# Java8读写锁的改进--StampedLock的使用示例
> 本文由 [原文地址](https://www.cnblogs.com/ten951/p/6590579.html)

## 前言
`StampedLock` 是 Java8 引入的一种新的所机制, 简单的理解, 可以认为它是读写锁的一个改进版本, **读写锁虽然分离了读和写的功能, 使得读与读之间可以完全并发, 但是读和写之间依然是冲突的, 读锁会完全阻塞写锁, 它使用的依然是悲观的锁策略**. ***如果有大量的读线程, 他也有可能引起写线程的饥饿,而 `StampedLock` 则提供了一种乐观的读策略, 这种乐观策略的锁非常类似于无锁的操作, 使得乐观锁完全不会阻塞写线程***。

### StampedLock 的使用实例

```java
public class Point {
    private double x, y;//内部定义表示坐标点
    private final StampedLock s1 = new StampedLock();//定义了StampedLock锁,

    void move(double deltaX, double deltaY) {
        long stamp = s1.writeLock();//这里的含义和distanceFormOrigin方法中 s1.readLock()是类似的
        try {
            x += deltaX;
            y += deltaY;
        } finally {
            s1.unlockWrite(stamp);//退出临界区,释放写锁
        }
    }

    double distanceFormOrigin() {//只读方法
        long stamp = s1.tryOptimisticRead();  //试图尝试一次乐观读 返回一个类似于时间戳的邮戳整数stamp 这个stamp就可以作为这一个所获取的凭证
        double currentX = x, currentY = y;//读取x和y的值,这时候我们并不确定x和y是否是一致的
        if (!s1.validate(stamp)) {//判断这个stamp是否在读过程发生期间被修改过,如果stamp没有被修改过,则认为这次读取时有效的,因此就可以直接return了,反之,如果stamp是不可用的,则意味着在读取的过程中,可能被其他线程改写了数据,因此,有可能出现脏读,如果如果出现这种情况,我们可以像CAS操作那样在一个死循环中一直使用乐观锁,知道成功为止
            stamp = s1.readLock();//也可以升级锁的级别,这里我们升级乐观锁的级别,将乐观锁变为悲观锁, 如果当前对象正在被修改,则读锁的申请可能导致线程挂起.
            try {
                currentX = x;
                currentY = y;
            } finally {
                s1.unlockRead(stamp);//退出临界区,释放读锁
            }
        }
        return Math.sqrt(currentX * currentX + currentY * currentY);
    }
}
```


## Thinking
- StampedLock 的小陷阱
- 有关 StampedLock 的实现思想

`StampedLock` 的内部实现是基于 CLH 锁的, CLH 锁是一种自旋锁, 它**保证没有饥饿的发生, 并且可以保证 FIFO(先进先出) 的服务顺序**.***CLH 锁的基本思想如下: 锁维护一个等待线程队列, 所有申请锁, 但是没有成功的线程都记录在这个队列中, 每一个节点代表一个线程, 保存一个标记位 (locked). 用与判断当前线程是否已经释放锁;*** `locked=true` 没有获取到锁, `false` 已经成功释放了锁,当一个线程视图获得锁时, 取得等待队列的尾部节点作为其前序节点. 并使用类似如下代码判断前序节点是否已经成功释放锁:

```java
while (pred.locked) {

}
```

只要前序节点 (pred) 没有释放锁, 则表示当前线程还不能继续执行, 因此会自旋等待,     
反之, 如果前序线程已经释放锁, 则当前线程可以继续执行.     
释放锁时, 也遵循这个逻辑, 线程会将自身节点的 `locked` 位置标记位 `false`, 那么后续等待的线程就能继续执行了
