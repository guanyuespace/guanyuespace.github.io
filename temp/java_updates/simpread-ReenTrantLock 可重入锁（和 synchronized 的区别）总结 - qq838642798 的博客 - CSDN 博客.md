# ReenTrantLock 可重入锁（和 synchronized 的区别）总结
>[原文地址](https://blog.csdn.net/qq838642798/article/details/65441415)

## 可重入性

从名字上理解，ReenTrantLock 的字面意思就是再进入的锁，**其实 synchronized 关键字所使用的锁也是可重入的，两者关于这个的区别不大**。两者都是同一个线程每进入一次，锁的计数器都自增 1，所以要等到锁的计数器下降为 0 时才能释放锁。

## 锁的实现

*`Synchronized` 是依赖于 `JVM` 实现的，而 `ReenTrantLock` 是 `JDK` 实现的*，有什么区别，说白了就类似于操作系统来控制实现和用户自己敲代码实现的区别。前者的实现是比较难见到的，后者有直接的源码可供阅读。

## 性能的区别

在 `Synchronized` 优化以前，`synchronized` 的性能是比 `ReenTrantLock` 差很多的，***但是自从 Synchronized 引入了偏向锁，轻量级锁（自旋锁）后，两者的性能就差不多了***，*在两种方法都可用的情况下，官方甚至建议使用 synchronized*，其实 synchronized 的优化我感觉就借鉴了 `ReenTrantLock` 中的 `CAS` 技术。都是试图在用户态就把加锁问题解决，避免进入内核态的线程阻塞。

## 功能区别

- 便利性：很明显 `Synchronized` 的使用比较方便简洁，并且由编译器去保证锁的加锁和释放，而 `ReenTrantLock` 需要手工声明来加锁和释放锁，为了避免忘记手工释放锁造成死锁，所以最好在 `finally` 中声明释放锁。

- 锁的细粒度和灵活度：很明显 ReenTrantLock 优于 Synchronized

### ReenTrantLock 独有的能力

1. `ReenTrantLock` 可以指定是公平锁还是非公平锁。而 `synchronized` 只能是非公平锁。所谓的公平锁就是先等待的线程先获得锁。

2. `ReenTrantLock` 提供了一个 `Condition`（条件）类，用来实现分组唤醒需要唤醒的线程们，而不是像 `synchronized` 要么随机唤醒一个线程要么唤醒全部线程。

3. `ReenTrantLock` 提供了一种能够中断等待锁的线程的机制，通过 `lock.lockInterruptibly()` 来实现这个机制。

## ReenTrantLock 实现的原理

在网上看到相关的源码分析，本来这块应该是本文的核心，但是感觉比较复杂就不一一详解了，简单来说，`ReenTrantLock` 的实现是一种自旋锁，通过循环调用 `CAS` 操作来实现加锁。**它的性能比较好也是因为避免了使线程进入内核态的阻塞状态。***想尽办法避免线程进入内核的阻塞状态是我们去分析和理解锁设计的关键钥匙。

## 什么情况下使用 ReenTrantLock

答案是，如果你需要实现 ReenTrantLock 的三个独有功能时。
