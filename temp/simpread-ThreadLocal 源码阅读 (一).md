# ThreadLocal源码简要分析
> [原文地址](https://www.jianshu.com/p/529c03d9b67e)

## JDK ThreadLocal

JDK 的实现主要包括如下三个类:

1. Thread
2. ThreadLocal
3. ThreadLocal.ThreadLocalMap

### Thread

Thread 比较简单，只是定义了 ThreadLocalMap 类型的变量 threadLocals, 存储本线程中使用到的所有 ThreadLocal 及关联对象；

```java
ThreadLocal.ThreadLocalMap threadLocals = null;
```

### ThreadLocalMap

ThreadLocalMap 是一个内部类，从名字上猜测或许是一个 Map,key 是 ThreadLocal,value 是关联对象; 实际上该类的确类似 Map, 但和我们经常使用的 HashMap 又有差异；
大家都知道 HashMap 的存储方式是 Entry[]-> 链表，而 ThreadLocalMap 只是采用 Entry[]，而不使用链表; 那么 ThreadLocal 是如何解决 hashcode 冲突的呢？这部分代码在 set 方法中：

```java
private void set(ThreadLocal key, Object value) {
    Entry[] tab = table;
    int len = tab.length;
    int i = key.threadLocalHashCode & (len-1);
    for (Entry e = tab[i];
         e != null;
         e = tab[i = nextIndex(i, len)]) {//如果HASH冲突则寻找下一索引
        ThreadLocal k = e.get();

        if (k == key) {//如果已经存在，直接覆盖
            e.value = value;
            return;
        }

        if (k == null) {//如果ThreadLocal为空,替换
            replaceStaleEntry(key, value, i);
            return;
        }
    }
    tab[i] = new Entry(key, value);//新元素则加入
    int sz = ++size;
    if (!cleanSomeSlots(i, sz) && sz >= threshold)//判断是否需要生长
        rehash();
}
```

从上面的代码可以看到，set 方法的逻辑如下：

1. 首先根据 `ThreadLocal` 元素的 `hashCode` 计算出其数组下标；
2. 由于是采用和 `len-1` 进行按位与操作，因此要求 `len` 是 2 的 n 次方，这样计算出来的结果应该是 0~2^n-1；
3. 如果数组该下标并未存储内容，则将 `key` 和 `value` 组装为 `Entry` , 存储到该下标中；另外需要判断是否进行扩容；
4. 如果该数组下标中已经存储了内容，则需要判断其 `Entry` 中的 `key` 和传入的 `ThreadLocal` 是否相同，如果相同，则更新 `Entry` 的 `value`; 如果 `Entry` 中的 `key` 为空，则说明 `ThreadLocal` 已经被垃圾回收 **(Entry 是 WeakReference 类型，当垃圾回收时，如果没有其他的强引用，会被回收掉)** ，则调用 replaceStaleEntry 方法设值；*否则继续判断数组的下一元素*；

分析上面的源码发现，**和 `HashMap` 的实现不一样，当对应的数组下标存在元素时，并不是通过链表的方式存储；而是在数组中寻找下一个可用的位置**；因此当 `hash` 冲突加大时，`set` 的效率会比较低；`get` 方法也同样存在该问题；看到这，很好奇 JDK 是如何解决 `hash` 冲突问题的，问题的关键应该在 `ThreadLocal` 的 `hashcode` 上；

## ThreadLocal

`ThreadLoal` 中定义了 `int` 类型的 `threadLocalHashCode` 变量：

```java
private final int threadLocalHashCode = nextHashCode();
private static AtomicInteger nextHashCode =new AtomicInteger();
private static final int HASH_INCREMENT = 0x61c88647;

private static int nextHashCode() {
    return nextHashCode.getAndAdd(HASH_INCREMENT);
}
```

可以看到其 `hashcode` 的生成规则为从 `0` 开始，每次递增 `0x61c88647`；
为什么采用 `0x61c88647` 这个数字呢？

| 16 进制 | 10 进制 | 2 进制 |
| --- | --- | --- |
| 0x61c88647 | 1640531527 | 01100001110010001000011001000111 10011110001101110111100110111001(取反＋1) |

`10011110001101110111100110111001` 对应的 `10` 进制为 `2654435769`；

 `2654435769` 是一个 ***斐波那契散列乘数*** ，它的优点是**通过它散列出来的结果分布会比较均匀，可以很大程度上避免 hash 冲突**；

***斐波那契乘数***，斐波那契散列
<!-- 除法散列，平方散列，斐波那契散列 -->

### 举例说明：
1. ThreadLocal 的容量为 16，threshold 为 16*2/3=10，那么计算的结果为：

| 序号 | hashcode | 数组下标 |
| --- | --- | --- |
| 0 | 0x00 | 0 |
| 1 | 0x61c88647 | 7 |
| 2 | 0xc3910c8e | 14 |
| 3 | 0x255992d5 | 5 |
| 4 | 0x8722191c | 12 |
| 5 | 0xe8ea9f63 | 3 |
| 6 | 0x4ab325aa | 10 |
| 7 | 0xac7babf1 | 1 |
| 8 | 0xe443238 | 8 |
| 9 | 0x700cb87f | 15 |

2. ThreadLocal 的容量为 32，threshold 为 32*2/3=21，那么计算的结果为：

| 序号 | hashcode | 数组下标 |
| --- | --- | --- |
| 0 | 0x00 | 0 |
| 1 | 0x61c88647 | 7 |
| 2 | 0xc3910c8e | 14 |
| 3 | 0x255992d5 | 21 |
| 4 | 0x8722191c | 28 |
| 5 | 0xe8ea9f63 | 3 |
| 6 | 0x4ab325aa | 10 |
| 7 | 0xac7babf1 | 17 |
| 8 | 0xe443238 | 24 |
| 9 | 0x700cb87f | 31 |
| 10 | 0xd1d53ec6 | 6 |
| 11 | 0x339dc50d | 13 |
| 12 | 0x95664b54 | 20 |
| 13 | 0xf72ed19b | 27 |
| 14 | 0x58f757e2 | 2 |
| 15 | 0xbabfde29 | 9 |
| 16 | 0x1c886470 | 16 |
| 17 | 0x7e50eab7 | 23 |
| 18 | 0xe01970fe | 30 |
| 19 | 0x41e1f745 | 5 |
| 20 | 0xa3aa7d8c | 12 |

可以看到，该算法计算出的数组下标分散性还是很好的；
