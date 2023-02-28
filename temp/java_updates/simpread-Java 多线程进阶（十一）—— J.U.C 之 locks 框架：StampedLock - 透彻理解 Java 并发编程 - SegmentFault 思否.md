# StampedLock分析
> 
> [原文地址](https://segmentfault.com/a/1190000015808032?utm_source=tag-newest)
> 本文首发于一世流云的专栏：[https://segmentfault.com/blog...](https://segmentfault.com/blog/ressmix_multithread)

## 一、StampedLock 类简介

`StampedLock` 类，在 JDK1.8 时引入，是对读写锁 `ReentrantReadWriteLock` 的增强，该类提供了一些功能，优化了读锁、写锁的访问，同时使读写锁之间可以互相转换，更细粒度控制并发。

**首先明确下，该类的设计初衷是作为一个内部工具类，用于辅助开发其它线程安全组件，用得好，该类可以提升系统性能，用不好，容易产生死锁和其它莫名其妙的问题**。

### 1.1 StampedLock 的引入
>为什么有了 ReentrantReadWriteLock，还要引入 StampedLock？

ReentrantReadWriteLock 使得多个读线程同时持有读锁（只要写锁未被占用），而写锁是独占的。

但是，读写锁如果使用不当，很容易产生 “饥饿” 问题：

比如在读线程非常多，写线程很少的情况下，很容易导致写线程 “饥饿”，虽然使用“公平” 策略可以一定程度上缓解这个问题，但是 “公平” 策略是以牺牲系统吞吐量为代价的。（在 [ReentrantLock 类](https://segmentfault.com/a/1190000015562293)的介绍章节中，介绍过这种情况）

### 1.2 StampedLock 的特点

`StampedLock` 的主要特点概括一下，有以下几点：

1. 所有获取锁的方法，都返回一个邮戳（Stamp），`Stamp` 为 0 表示获取失败，其余都表示成功；
2. 所有释放锁的方法，都需要一个邮戳（Stamp），这个 `Stamp` 必须是和成功获取锁时得到的 Stamp 一致；
3. StampedLock 是不可重入的；（如果一个线程已经持有了写锁，再去获取写锁的话就会造成死锁）
4. StampedLock 有三种访问模式：
    - ①Reading（读模式）：功能和 `ReentrantReadWriteLock` 的读锁类似
    - ②Writing（写模式）：功能和 `ReentrantReadWriteLock` 的写锁类似
    - ③Optimistic reading（乐观读模式）：这是一种优化的读模式。
5. StampedLock 支持读锁和写锁的相互转换
    我们知道 `ReentrantReadWriteLock` 中，当线程获取到写锁后，可以降级为读锁，但是读锁是不能直接升级为写锁的。
    `StampedLock` 提供了读锁和写锁相互转换的功能，使得该类支持更多的应用场景。
6. 无论写锁还是读锁，都不支持 `Conditon` 等待

我们知道，在 ReentrantReadWriteLock 中，当读锁被使用时，如果有线程尝试获取写锁，该写线程会阻塞。
但是，在 Optimistic reading 中，**即使读线程获取到了读锁，写线程尝试获取写锁也不会阻塞，这相当于对读模式的优化，但是可能会导致数据不一致的问题**。所以，当使用 Optimistic reading 获取到读锁时，必须对获取结果进行校验。

## 二、StampedLock 使用示例

先来看一个 Oracle 官方的例子：

```java
class Point {
    private double x, y;
    private final StampedLock sl = new StampedLock();

    void move(double deltaX, double deltaY) {
        long stamp = sl.writeLock();    //涉及对共享资源的修改，使用写锁-独占操作
        try {
            x += deltaX;
            y += deltaY;
        } finally {
            sl.unlockWrite(stamp);
        }
    }

    /**
     * 使用乐观读锁访问共享资源
     * 注意：乐观读锁在保证数据一致性上需要拷贝一份要操作的变量到方法栈，并且在操作数据时候可能其他写线程已经修改了数据，
     * 而我们操作的是方法栈里面的数据，也就是一个快照，所以最多返回的不是最新的数据，但是一致性还是得到保障的。
     *
     * @return
     */
    double distanceFromOrigin() {
        long stamp = sl.tryOptimisticRead();    // 使用乐观读锁
        double currentX = x, currentY = y;      // 拷贝共享资源到本地方法栈中
        if (!sl.validate(stamp)) {              // 如果有写锁被占用，可能造成数据不一致，所以要切换到普通读锁模式
            stamp = sl.readLock();              // 乐观锁失效，采用共享读锁
            try {
                currentX = x;
                currentY = y;
            } finally {
                sl.unlockRead(stamp);
            }
        }
        return Math.sqrt(currentX * currentX + currentY * currentY);
    }

    void moveIfAtOrigin(double newX, double newY) { // upgrade
        // Could instead start with optimistic, not read mode
        long stamp = sl.readLock();
        try {
            while (x == 0.0 &amp;&amp; y == 0.0) {
                long ws = sl.tryConvertToWriteLock(stamp);  //读锁转换为写锁
                if (ws != 0L) {
                    stamp = ws;
                    x = newX;
                    y = newY;
                    break;
                } else {
                    sl.unlockRead(stamp);
                    stamp = sl.writeLock();
                }
            }
        } finally {
            sl.unlock(stamp);
        }
    }
}
```

可以看到，上述示例最特殊的其实是 **distanceFromOrigin** 方法，这个方法中使用了 “Optimistic reading” 乐观读锁，使得读写可以并发执行，但是 “Optimistic reading” 的使用必须遵循以下模式：

```java
// static StamedLock lock=new StamedLock();

long stamp = lock.tryOptimisticRead();  // 非阻塞获取版本信息
copyVaraibale2ThreadMemory();           // 拷贝变量到线程本地堆栈
if(!lock.validate(stamp)){              // 校验
  long stamp = lock.readLock();         // 获取读锁
  try {
    copyVaraibale2ThreadMemory();       // 拷贝变量到线程本地堆栈
  } finally {
    lock.unlock(stamp);                 // 释放悲观锁
  }
}
useThreadMemoryVarables();              // 使用线程本地堆栈里面的数据进行操作
```

## 三、StampedLock 原理

### 3.1 StampedLock 的内部常量

`StampedLock` 虽然不像其它锁一样定义了内部类来实现 `AQS` 框架，但是 `StampedLock` 的基本实现思路还是利用 `CLH` 队列进行线程的管理，通过同步状态值来表示锁的状态和类型。

`StampedLock` 内部定义了很多常量，定义这些常量的根本目的还是和 `ReentrantReadWriteLock` 一样，对同步状态值按位切分，以通过位运算对 `State` 进行操作：

> 对于 `StampedLock` 来说，写锁被占用的标志是第 8 位为 1，读锁使用 0-7 位，正常情况下读锁数目为 1-126，超过 126 时，使用一个名为`readerOverflow`的 `int` 整型保存超出数。

![](https://segmentfault.com/img/bVbeuzV?w=993&h=442)

部分常量的比特位表示如下：
![](https://segmentfault.com/img/bVbeuzX?w=934&h=330)

另外，StampedLock 相比 ReentrantReadWriteLock，对多核 CPU 进行了优化，可以看到，当 CPU 核数超过 1 时，会有一些自旋操作:
![](https://segmentfault.com/img/bVbeuz4?w=810&h=426)

### 3.2 示例分析

> 假设现在有三个线程：ThreadA、ThreadB、ThreadC、ThreadD。操作如下：

```
ThreadA调用writeLock, 获取写锁
ThreadB调用readLock, 获取读锁
ThreadC调用readLock, 获取读锁
ThreadD调用writeLock, 获取写锁
ThreadE调用readLock, 获取读锁
```

### 1. StampedLock 对象的创建

StampedLock 的构造器很简单，构造时设置下同步状态值：
![](https://segmentfault.com/img/bVbeuAn?w=639&h=142)

另外，StamedLock 提供了三类视图：
![](https://segmentfault.com/img/bVbeuAp?w=582&h=105)

这些视图其实是对 StamedLock 方法的封装，便于习惯了 ReentrantReadWriteLock 的用户使用：
例如，**ReadLockView** 其实相当于`ReentrantReadWriteLock.readLock()`返回的读锁;
![](https://segmentfault.com/img/bVbeuAw?w=970&h=523)

### 2. ThreadA 调用 writeLock 获取写锁

来看下 **writeLock** 方法：
![](https://segmentfault.com/img/bVbeuAz?w=1202&h=244)

StampedLock 中大量运用了位运算，这里`(s = state) & ABITS == 0L` 表示读锁和写锁都未被使用，这里写锁可以立即获取成功，然后 CAS 操作更新同步状态值 State。

操作完成后，等待队列的结构如下：
![](https://segmentfault.com/img/bVbeuAI?w=145&h=201)

> 注意：StampedLock 中，等待队列的结点要比 AQS 中简单些，仅仅三种状态。
> 0：初始状态
> -1：等待中
> 1：取消

另外，结点的定义中有个`cowait`字段，该字段指向一个栈，用于保存读线程，这个后续会讲到。
![](https://segmentfault.com/img/bVbeuAX?w=722&h=714)

### 3. ThreadB 调用 readLock 获取读锁

来看下 **readLock** 方法：
由于 ThreadA 此时持有写锁，所以 ThreadB 获取读锁失败，将调用 **acquireRead** 方法，加入等待队列：
![](https://segmentfault.com/img/bVbeuAZ?w=1129&h=286)

**acquireRead** 方法非常复杂，用到了大量自旋操作：

```java
/**
 * 尝试自旋的获取读锁, 获取不到则加入等待队列, 并阻塞线程
 *
 * @param interruptible true 表示检测中断, 如果线程被中断过, 则最终返回INTERRUPTED
 * @param deadline      如果非0, 则表示限时获取
 * @return 非0表示获取成功, INTERRUPTED表示中途被中断过
 */
private long acquireRead(boolean interruptible, long deadline) {
    WNode node = null, p;   // node指向入队结点, p指向入队前的队尾结点

    /**
     * 自旋入队操作
     * 如果写锁未被占用, 则立即尝试获取读锁, 获取成功则返回.
     * 如果写锁被占用, 则将当前读线程包装成结点, 并插入等待队列（如果队尾是写结点,直接链接到队尾;否则,链接到队尾读结点的栈中）
     */
    for (int spins = -1; ; ) {
        WNode h;
        if ((h = whead) == (p = wtail)) {   // 如果队列为空或只有头结点, 则会立即尝试获取读锁
            for (long m, s, ns; ; ) {
                if ((m = (s = state) &amp; ABITS) < RFULL ?     // 判断写锁是否被占用
                    U.compareAndSwapLong(this, STATE, s, ns = s + RUNIT) :  //写锁未占用,且读锁数量未超限, 则更新同步状态
                    (m < WBIT &amp;&amp; (ns = tryIncReaderOverflow(s)) != 0L))        //写锁未占用,但读锁数量超限, 超出部分放到readerOverflow字段中
                    return ns;          // 获取成功后, 直接返回
                else if (m >= WBIT) {   // 写锁被占用,以随机方式探测是否要退出自旋
                    if (spins > 0) {
                        if (LockSupport.nextSecondarySeed() >= 0)
                            --spins;
                    } else {
                        if (spins == 0) {
                            WNode nh = whead, np = wtail;
                            if ((nh == h &amp;&amp; np == p) || (h = nh) != (p = np))
                                break;
                        }
                        spins = SPINS;
                    }
                }
            }
        }
        if (p == null) {                            // p == null表示队列为空, 则初始化队列(构造头结点)
            WNode hd = new WNode(WMODE, null);
            if (U.compareAndSwapObject(this, WHEAD, null, hd))
                wtail = hd;
        } else if (node == null) {                  // 将当前线程包装成读结点
            node = new WNode(RMODE, p);
        } else if (h == p || p.mode != RMODE) {     // 如果队列只有一个头结点, 或队尾结点不是读结点, 则直接将结点链接到队尾, 链接完成后退出自旋
            if (node.prev != p)
                node.prev = p;
            else if (U.compareAndSwapObject(this, WTAIL, p, node)) {
                p.next = node;
                break;
            }
        }
        // 队列不为空, 且队尾是读结点, 则将添加当前结点链接到队尾结点的cowait链中（实际上构成一个栈, p是栈顶指针 ）
        else if (!U.compareAndSwapObject(p, WCOWAIT, node.cowait = p.cowait, node)) {    // CAS操作队尾结点p的cowait字段,实际上就是头插法插入结点
            node.cowait = null;
        } else {
            for (; ; ) {
                WNode pp, c;
                Thread w;
                // 尝试唤醒头结点的cowait中的第一个元素, 假如是读锁会通过循环释放cowait链
                if ((h = whead) != null &amp;&amp; (c = h.cowait) != null &amp;&amp;
                    U.compareAndSwapObject(h, WCOWAIT, c, c.cowait) &amp;&amp;
                    (w = c.thread) != null) // help release
                    U.unpark(w);
                if (h == (pp = p.prev) || h == p || pp == null) {
                    long m, s, ns;
                    do {
                        if ((m = (s = state) &amp; ABITS) < RFULL ?
                            U.compareAndSwapLong(this, STATE, s,
                                ns = s + RUNIT) :
                            (m < WBIT &amp;&amp;
                                (ns = tryIncReaderOverflow(s)) != 0L))
                            return ns;
                    } while (m < WBIT);
                }
                if (whead == h &amp;&amp; p.prev == pp) {
                    long time;
                    if (pp == null || h == p || p.status > 0) {
                        node = null; // throw away
                        break;
                    }
                    if (deadline == 0L)
                        time = 0L;
                    else if ((time = deadline - System.nanoTime()) <= 0L)
                        return cancelWaiter(node, p, false);
                    Thread wt = Thread.currentThread();
                    U.putObject(wt, PARKBLOCKER, this);
                    node.thread = wt;
                    if ((h != pp || (state &amp; ABITS) == WBIT) &amp;&amp; whead == h &amp;&amp; p.prev == pp) {
                        // 写锁被占用, 且当前结点不是队首结点, 则阻塞当前线程
                        U.park(false, time);
                    }
                    node.thread = null;
                    U.putObject(wt, PARKBLOCKER, null);
                    if (interruptible &amp;&amp; Thread.interrupted())
                        return cancelWaiter(node, p, true);
                }
            }
        }
    }

    for (int spins = -1; ; ) {
        WNode h, np, pp;
        int ps;
        if ((h = whead) == p) {     // 如果当前线程是队首结点, 则尝试获取读锁
            if (spins < 0)
                spins = HEAD_SPINS;
            else if (spins < MAX_HEAD_SPINS)
                spins <<= 1;
            for (int k = spins; ; ) { // spin at head
                long m, s, ns;
                if ((m = (s = state) &amp; ABITS) < RFULL ?     // 判断写锁是否被占用
                    U.compareAndSwapLong(this, STATE, s, ns = s + RUNIT) :  //写锁未占用,且读锁数量未超限, 则更新同步状态
                    (m < WBIT &amp;&amp; (ns = tryIncReaderOverflow(s)) != 0L)) {      //写锁未占用,但读锁数量超限, 超出部分放到readerOverflow字段中
                    // 获取读锁成功, 释放cowait链中的所有读结点
                    WNode c;
                    Thread w;

                    // 释放头结点, 当前队首结点成为新的头结点
                    whead = node;
                    node.prev = null;

                    // 从栈顶开始(node.cowait指向的结点), 依次唤醒所有读结点, 最终node.cowait==null, node成为新的头结点
                    while ((c = node.cowait) != null) {
                        if (U.compareAndSwapObject(node, WCOWAIT, c, c.cowait) &amp;&amp; (w = c.thread) != null)
                            U.unpark(w);
                    }
                    return ns;
                } else if (m >= WBIT &amp;&amp;
                    LockSupport.nextSecondarySeed() >= 0 &amp;&amp; --k <= 0)
                    break;
            }
        } else if (h != null) {     // 如果头结点存在cowait链, 则唤醒链中所有读线程
            WNode c;
            Thread w;
            while ((c = h.cowait) != null) {
                if (U.compareAndSwapObject(h, WCOWAIT, c, c.cowait) &amp;&amp;
                    (w = c.thread) != null)
                    U.unpark(w);
            }
        }
        if (whead == h) {
            if ((np = node.prev) != p) {
                if (np != null)
                    (p = np).next = node;   // stale
            } else if ((ps = p.status) == 0)        // 将前驱结点的等待状态置为WAITING, 表示之后将唤醒当前结点
                U.compareAndSwapInt(p, WSTATUS, 0, WAITING);
            else if (ps == CANCELLED) {
                if ((pp = p.prev) != null) {
                    node.prev = pp;
                    pp.next = node;
                }
            } else {        // 阻塞当前读线程
                long time;
                if (deadline == 0L)
                    time = 0L;
                else if ((time = deadline - System.nanoTime()) <= 0L)   //限时等待超时, 取消等待
                    return cancelWaiter(node, node, false);

                Thread wt = Thread.currentThread();
                U.putObject(wt, PARKBLOCKER, this);
                node.thread = wt;
                if (p.status < 0 &amp;&amp; (p != h || (state &amp; ABITS) == WBIT) &amp;&amp; whead == h &amp;&amp; node.prev == p) {
                    // 如果前驱的等待状态为WAITING, 且写锁被占用, 则阻塞当前调用线程
                    U.park(false, time);
                }
                node.thread = null;
                U.putObject(wt, PARKBLOCKER, null);
                if (interruptible &amp;&amp; Thread.interrupted())
                    return cancelWaiter(node, node, true);
            }
        }
    }
}
```

我们来分析下这个方法。
该方法会首先自旋的尝试获取读锁，获取成功后，就直接返回；否则，会将当前线程包装成一个读结点，插入到等待队列。
由于，目前等待队列还是空，所以 ThreadB 会初始化队列，然后将自身包装成一个读结点，插入队尾，然后在下面这个地方跳出自旋：
![](https://segmentfault.com/img/bVbeuBF?w=1233&h=315)

此时，等待队列的结构如下：
![](https://segmentfault.com/img/bVbeuIU?w=621&h=332)

跳出自旋后，ThreadB 会继续向下执行，进入下一个自旋，在下一个自旋中，依然会再次尝试获取读锁，如果这次再获取不到，就会将前驱的等待状态置为 WAITING, 表示我（当前线程）要去睡了（阻塞），到时记得叫醒我：
![](https://segmentfault.com/img/bVbeuB3?w=1042&h=254)

![](https://segmentfault.com/img/bVbeuIT?w=685&h=352)

最终, ThreadB 进入阻塞状态:
![](https://segmentfault.com/img/bVbeuCj?w=1063&h=673)

最终，等待队列的结构如下：

![](https://segmentfault.com/img/bVbeuII?w=643&h=350)

### 4. ThreadC 调用 readLock 获取读锁

这个过程和 ThreadB 获取读锁一样，区别在于 ThreadC 被包装成结点加入等待队列后，是链接到 ThreadB 结点的栈指针中的。调用完下面这段代码后，ThreadC 会链接到以 Thread B 为栈顶指针的栈中：
![](https://segmentfault.com/img/bVbeuHf?w=1304&h=69)

![](https://segmentfault.com/img/bVbeuIB?w=760&h=554)

> 注意：读结点的 cowait 字段其实构成了一个栈，入栈的过程其实是个 “头插法” 插入单链表的过程。比如，再来个 ThreadX 读结点，则 cowait 链表结构为：`ThreadB - > ThreadX -> ThreadC`。最终唤醒读结点时，将从栈顶开始。

然后会在下一次自旋中，阻塞当前读线程：
![](https://segmentfault.com/img/bVbeuHn?w=888&h=489)

最终，等待队列的结构如下：
![](https://segmentfault.com/img/bVbeuIv?w=696&h=550)

可以看到，此时 ThreadC 结点并没有把它的前驱的等待状态置为 - 1，因为 ThreadC 是链接到栈中的，当写锁释放的时候，会从栈底元素开始，唤醒栈中所有读结点。

### 5. ThreadD 调用 writeLock 获取写锁

ThreadD 调用 **writeLock** 方法获取写锁失败后（ThreadA 依然占用着写锁），会调用 **acquireWrite** 方法，该方法整体逻辑和 **acquireRead** 差不多，首先自旋的尝试获取写锁，获取成功后，就直接返回；否则，会将当前线程包装成一个写结点，插入到等待队列。

![](https://segmentfault.com/img/bVbeuAz?w=1202&h=244)

_acquireWrite 源码：_

```java
/**
 * 尝试自旋的获取写锁, 获取不到则阻塞线程
 *
 * @param interruptible true 表示检测中断, 如果线程被中断过, 则最终返回INTERRUPTED
 * @param deadline      如果非0, 则表示限时获取
 * @return 非0表示获取成功, INTERRUPTED表示中途被中断过
 */
private long acquireWrite(boolean interruptible, long deadline) {
    WNode node = null, p;

    /**
     * 自旋入队操作
     * 如果没有任何锁被占用, 则立即尝试获取写锁, 获取成功则返回.
     * 如果存在锁被使用, 则将当前线程包装成独占结点, 并插入等待队列尾部
     */
    for (int spins = -1; ; ) {
        long m, s, ns;
        if ((m = (s = state) &amp; ABITS) == 0L) {      // 没有任何锁被占用
            if (U.compareAndSwapLong(this, STATE, s, ns = s + WBIT))    // 尝试立即获取写锁
                return ns;                                                 // 获取成功直接返回
        } else if (spins < 0)
            spins = (m == WBIT &amp;&amp; wtail == whead) ? SPINS : 0;
        else if (spins > 0) {
            if (LockSupport.nextSecondarySeed() >= 0)
                --spins;
        } else if ((p = wtail) == null) {       // 队列为空, 则初始化队列, 构造队列的头结点
            WNode hd = new WNode(WMODE, null);
            if (U.compareAndSwapObject(this, WHEAD, null, hd))
                wtail = hd;
        } else if (node == null)               // 将当前线程包装成写结点
            node = new WNode(WMODE, p);
        else if (node.prev != p)
            node.prev = p;
        else if (U.compareAndSwapObject(this, WTAIL, p, node)) {    // 链接结点至队尾
            p.next = node;
            break;
        }
    }

    for (int spins = -1; ; ) {
        WNode h, np, pp;
        int ps;
        if ((h = whead) == p) {     // 如果当前结点是队首结点, 则立即尝试获取写锁
            if (spins < 0)
                spins = HEAD_SPINS;
            else if (spins < MAX_HEAD_SPINS)
                spins <<= 1;
            for (int k = spins; ; ) { // spin at head
                long s, ns;
                if (((s = state) &amp; ABITS) == 0L) {      // 写锁未被占用
                    if (U.compareAndSwapLong(this, STATE, s,
                        ns = s + WBIT)) {               // CAS修改State: 占用写锁
                        // 将队首结点从队列移除
                        whead = node;
                        node.prev = null;
                        return ns;
                    }
                } else if (LockSupport.nextSecondarySeed() >= 0 &amp;&amp;
                    --k <= 0)
                    break;
            }
        } else if (h != null) {  // 唤醒头结点的栈中的所有读线程
            WNode c;
            Thread w;
            while ((c = h.cowait) != null) {
                if (U.compareAndSwapObject(h, WCOWAIT, c, c.cowait) &amp;&amp; (w = c.thread) != null)
                    U.unpark(w);
            }
        }
        if (whead == h) {
            if ((np = node.prev) != p) {
                if (np != null)
                    (p = np).next = node;   // stale
            } else if ((ps = p.status) == 0)        // 将当前结点的前驱置为WAITING, 表示当前结点会进入阻塞, 前驱将来需要唤醒我
                U.compareAndSwapInt(p, WSTATUS, 0, WAITING);
            else if (ps == CANCELLED) {
                if ((pp = p.prev) != null) {
                    node.prev = pp;
                    pp.next = node;
                }
            } else {        // 阻塞当前调用线程
                long time;  // 0 argument to park means no timeout
                if (deadline == 0L)
                    time = 0L;
                else if ((time = deadline - System.nanoTime()) <= 0L)
                    return cancelWaiter(node, node, false);
                Thread wt = Thread.currentThread();
                U.putObject(wt, PARKBLOCKER, this);
                node.thread = wt;
                if (p.status < 0 &amp;&amp; (p != h || (state &amp; ABITS) != 0L) &amp;&amp; whead == h &amp;&amp; node.prev == p)
                    U.park(false, time);    // emulate LockSupport.park
                node.thread = null;
                U.putObject(wt, PARKBLOCKER, null);
                if (interruptible &amp;&amp; Thread.interrupted())
                    return cancelWaiter(node, node, true);
            }
        }
    }
}
```

**acquireWrite** 中的下面这个自旋操作，用于将线程包装成写结点，插入队尾：
![](https://segmentfault.com/img/bVbeuH2?w=903&h=610)

插入完成后，队列结构如下：
![](https://segmentfault.com/img/bVbeuIg?w=931&h=549)

然后，进入下一个自旋，并在下一个自旋中阻塞 ThreadD，最终队列结构如下：
![](https://segmentfault.com/img/bVbeuI3?w=967&h=545)

### 6. ThreadE 调用 readLock 获取读锁

同样，由于写锁被 ThreadA 占用着，所以最终会调用 **acquireRead** 方法，在该方法的第一个自旋中，会将 ThreadE 加入等待队列：
![](https://segmentfault.com/img/bVbeuI8?w=946&h=430)

> 注意，由于队尾结点是写结点，所以当前读结点会直接链接到队尾；如果队尾是读结点，则会链接到队尾读结点的 cowait 链中。

然后进入第二个自旋，阻塞 ThreadE，最终队列结构如下：
![](https://segmentfault.com/img/bVbeuJr?w=946&h=430)

### 7. ThreadA 调用 unlockWrite 释放写锁

通过 CAS 操作，修改 State 成功后，会调用 **release** 方法唤醒等待队列的队首结点：
![](https://segmentfault.com/img/bVbeuJv?w=1230&h=248)

**release** 方法非常简单，先将头结点的等待状态置为 0，表示即将唤醒后继结点，然后立即唤醒队首结点：
![](https://segmentfault.com/img/bVbeuJw?w=1129&h=417)

此时，等待队列的结构如下：
![](https://segmentfault.com/img/bVbeuJA?w=946&h=430)

### 8. ThreadB 被唤醒后继续向下执行

ThreadB 被唤醒后，会从原阻塞处继续向下执行，然后开始下一次自旋：
![](https://segmentfault.com/img/bVbeuJB?w=1026&h=668)

第二次自旋时，ThreadB 发现写锁未被占用，则成功获取到读锁，然后从栈顶（ThreadB 的 cowait 指针指向的结点）开始唤醒栈中所有线程，
最后返回：
![](https://segmentfault.com/img/bVbeuJC?w=1220&h=493)

最终，等待队列的结构如下：
![](https://segmentfault.com/img/bVbeuJK?w=777&h=459)

### 9. ThreadC 被唤醒后继续向下执行

ThreadC 被唤醒后，继续执行，并进入下一次自旋，下一次自旋时，会成功获取到读锁。
![](https://segmentfault.com/img/bVbeuJQ?w=815&h=886)

注意，此时 ThreadB 和 ThreadC 已经拿到了读锁，ThreadD（写线程）和 ThreadE（读线程）依然阻塞中，原来 ThreadC 对应的结点是个孤立结点，会被 GC 回收。

最终，等待队列的结构如下：
![](https://segmentfault.com/img/bVbeuJT?w=798&h=291)

### 10. ThreadB 和 ThreadC 释放读锁

ThreadB 和 ThreadC 调用 **unlockRead** 方法释放读锁，CAS 操作 State 将读锁数量减 1：
![](https://segmentfault.com/img/bVbeuJY?w=1143&h=432)

注意，当读锁的数量变为 0 时才会调用 **release** 方法，唤醒队首结点：
![](https://segmentfault.com/img/bVbeuJw?w=1129&h=417)

队首结点（ThreadD 写结点被唤醒），最终等待队列的结构如下：
![](https://segmentfault.com/img/bVbeuJ2?w=791&h=308)

### 11. ThreadD 被唤醒后继续向下执行

ThreadD 会从原阻塞处继续向下执行，并在下一次自旋中获取到写锁，然后返回:
![](https://segmentfault.com/img/bVbeuKh?w=882&h=987)

最终，等待队列的结构如下：
![](https://segmentfault.com/img/bVbeuKk?w=506&h=291)

### 12. ThreadD 调用 unlockWrite 释放写锁

ThreadD 释放写锁的过程和[步骤 7](https://segmentfault.com/a/1190000015808032#articleHeader13) 完全相同，会调用 **unlockWrite** 唤醒队首结点（ThreadE）。

![](https://segmentfault.com/img/bVbeuKu?w=487&h=288)

ThreadE 被唤醒后会从原阻塞处继续向下执行，但由于 ThreadE 是个读结点，所以同时会唤醒 cowait 栈中的所有读结点，过程和[步骤 8](https://segmentfault.com/a/1190000015808032#articleHeader14) 完全一样。最终，等待队列的结构如下：
![](https://segmentfault.com/img/bVbeuKP?w=215&h=258)

至此，全部执行完成。

## 四、StampedLock 类 / 方法声明

参考 Oracle 官方文档：[https://docs.oracle.com/javas...](https://docs.oracle.com/javase/8/docs/api/)
**_类声明：_**
![](https://segmentfault.com/img/bVbeuKZ?w=278&h=82)

**_方法声明：_**
![](https://segmentfault.com/img/bVbeuKX?w=1406&h=1828)

## 五、StampedLock 总结

StampedLock 的等待队列与 RRW 的 CLH 队列相比，有以下特点：

1.  当入队一个线程时，如果队尾是读结点，不会直接链接到队尾，而是链接到该读结点的 cowait 链中，cowait 链本质是一个栈；
2.  当入队一个线程时，如果队尾是写结点，则直接链接到队尾；
3.  唤醒线程的规则和 AQS 类似，都是首先唤醒队首结点。区别是 StampedLock 中，当唤醒的结点是读结点时，会唤醒该读结点的 cowait 链中的所有读结点（顺序和入栈顺序相反，也就是后进先出）。

另外，StampedLock 使用时要特别小心，避免锁重入的操作，在使用乐观读锁时也需要遵循相应的调用模板，防止出现数据不一致的问题。
