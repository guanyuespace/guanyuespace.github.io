> 本文由 [简悦 SimpRead](http://ksria.com/simpread/) 转码， 原文地址 https://www.cnblogs.com/xudilei/p/6867045.html

最近看多线程的时候发现对于 join 的理解有些错误，在网上查了不少资料，根据自己的理解整理了一下，这里之所以把 join 和 wait 放在一起，是因为 join 的底层实现就是基于 wait 的，一并讲解更容易理解。

## wait

了解 join 就先需要了解 wait，wait 是线程间通信常用的信号量，作用就是让线程暂时停止运行，等待其他线程使用 notify 来唤醒或者达到一定条件自己苏醒。
wait 是一个本地方法，属于 Object 类，其底层实现是 JVM 内部实现，是基于 monitor 对象监视锁。

```java
//本地方法
public final native void wait(long timeout) throws InterruptedException;
//参数有纳秒和毫秒
public final void wait(long timeout, int nanos) throws InterruptedException {
    if (timeout < 0) {
        throw new IllegalArgumentException("timeout value is negative");
    }

    if (nanos < 0 || nanos > 999999) {
        throw new IllegalArgumentException(
                            "nanosecond timeout value out of range");
    }
    //存在纳秒，默认加一毫秒
    if (nanos > 0) {
        timeout++;
    }

    wait(timeout);
}
//无参数，默认为0
public final void wait() throws InterruptedException {
    wait(0);
}
```

根据源码可以发现，虽然 wait 有三个重载的方法，但是主要的还是 wait(long timeout) 这个本地方法，其他两个都是基于这个来封装的，由 JVM 底层源码不太好看到，我就以流程的形式来描述。

```java
synchronized (this) {
        System.out.println("A begin " + System.currentTimeMillis());
        try {
            wait(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("A end " + System.currentTimeMillis());
}
```

- 上述代码在执行到 wait(5000) 时首先会释放当前占用的锁，并暂停线程。
- 在暂停的 5 秒内如果收到其他线程的 notify() 方法发来的信号，那么就再次尝试获取已经释放的锁
- 如果获取到那么就继续执行，没有就等待锁释放来竞争。
- 如果在 5 秒内未收到信号，那么到时间后就自动苏醒去尝试获取锁。

而对于时间的参数 **timeout** 需要注意的是，如果输入 0 不代表不暂停，而是需要特殊情况自己苏醒或者 notify 唤醒，这里有个特殊点，**wait(0) 是可以自己苏醒的**。

```java
public class Thread2 extends Thread{
    private Thread1 a;
    public Thread2(Thread1 a) {
        this.a = a;
    }

    @Override
    public void run() {
        synchronized (a) {
            System.out.println("B begin " + System.currentTimeMillis());
            try {
            a.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("B end " + System.currentTimeMillis());
        }
    }
}

public class Thread1 extends Thread{
    @Override
    public void run() {
        synchronized (this) {
            System.out.println("A begin " + System.currentTimeMillis());
            System.out.println("A end " + System.currentTimeMillis());
        }
    }
}

public class Main{
    public static void main(String[] args) {
        Thread1 thread = new Thread1();
        Thread2 thread2 = new Thread2(thread);
        thread2.start();
        thread.start();
        System.out.println("main end "+System.currentTimeMillis());
    }
}
```

这个例子运行结果存在以下情况

```
B begin 1494995803564
main end 1494995803565
A begin 1494995803565
A end 1494995803565
B end 1494995803565
```

wait() 在没有 notify() 情况下自动苏醒了，因此这里可以看到，当前情况下 Thread.wait() 等待过程中，如果 Thread 结束了，是可以自动唤醒的。这个会在 join 中被使用。

## join

了解了 wait 的实现原理之后就可以来看 join 了，join 是 Thread 类的方法，不是底层本地方法，这里可以看一下它的源码。

```java
public final synchronized void join(long millis)
throws InterruptedException {
    long base = System.currentTimeMillis();
    long now = 0;
    //参数判断<0,抛异常
    if (millis < 0) {
        throw new IllegalArgumentException("timeout value is negative");
    }
    //参数为0，即join()
    if (millis == 0) {
        //当前线程存活，就调用wait(0)，一直到调用join的线程结束再自动苏醒
        while (isAlive()) {
            wait(0);
        }
        //参数>0，调用wait(long millis)等待一段时间后自动唤醒
    } else {
        while (isAlive()) {
            long delay = millis - now;
            if (delay <= 0) {
                break;
            }
            wait(delay);
            now = System.currentTimeMillis() - base;
        }
    }
}

public final synchronized void join(long millis, int nanos)
throws InterruptedException {

    if (millis < 0) {
        throw new IllegalArgumentException("timeout value is negative");
    }

    if (nanos < 0 || nanos > 999999) {
        throw new IllegalArgumentException(
                            "nanosecond timeout value out of range");
    }

    if (nanos >= 500000 || (nanos != 0 && millis == 0)) {
        millis++;
    }

    join(millis);
}

public final void join() throws InterruptedException {
    join(0);
}
```

很明显，join 的三个重载方法主要还是基于 join(long millis) 方法，因此我们主要关注这个方法，方法的处理逻辑如下

- 判断参数时间参数，如果参数小于 0，抛出 `IllegalArgumentException("timeout value is negative")` 异常
- 参数等于 0，判断调用 `join` 的线程 (假设是 A) 是否存活，不存活就不执行操作，如果存活，就调用 `wait(0)`，阻塞 `join` 方法，等待 A 线程执行完在结束 `join` 方法。
- 参数大于 0，判断调用 `join` 的 A 线程是否存活，不存活就不执行操作，如果存活，就调用 `wait(long millis)`，阻塞 `join` 方法，等待时间结束再继续执行 `join` 方法。

由于 `join` 是 `synchronized` 修饰的同步方法，因此会出现 `join(long millis)` 阻塞时间超过了 `millis` 的值。

```java
public class Thread1 extends Thread{
    @Override
    public void run() {
        synchronized (this) {
            System.out.println("A begin " + System.currentTimeMillis());
            try {
                sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("A end " + System.currentTimeMillis());
        }
    }
}

public class Main{
    public static void main(String[] args) {
        Thread1 thread = new Thread1();
        thread.start();
        try {
            thread.join(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        System.out.println("main end "+System.currentTimeMillis());
    }
}
```

这个例子的运行结果是

```
A begin 1494996862054
A end 1494996867056
main end 1494996867056
```

`main` 线程一定是最后执行完的，按照流程来说，1 秒之后阻塞就结束了，`main` 线程应该就可以开始执行了，但是这里有一个注意点，`join(long millis)` 在执行 `millis>0` 的时候在 `wait(delay)` 之后还有一行代码，而上面代码1秒之后只是结束了 `wait` 方法，并没有执行完 `join` 方法。上面的例子，由于 `join` 的锁和 `thread` 的锁相同，在 `thread` 运行完之前，锁不会释放，那么导致 `join` 一直阻塞在最后一步无法结束，才会出现上面的情况。
