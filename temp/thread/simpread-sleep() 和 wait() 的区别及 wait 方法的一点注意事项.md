> 本文由 [简悦 SimpRead](http://ksria.com/simpread/) 转码， 原文地址 https://www.cnblogs.com/lyx210019/p/9427146.html

一. 查看 API

sleep 是 Thread 类的方法，导致此线程暂停执行指定时间，给其他线程执行机会，但是依然保持着监控状态，过了指定时间会自动恢复，调用 sleep 方法不会释放锁对象。

当调用 sleep 方法后，当前线程进入阻塞状态。目的是让出 CPU 给其他线程运行的机会。但是由于 sleep 方法不会释放锁对象，所以在一个同步代码块中调用这个方法后，线程虽然休眠了，但其他线程无法访问它的锁对象。这是因为 sleep 方法拥有 CPU 的执行权，它可以自动醒来无需唤醒。而当 sleep() 结束指定休眠时间后，这个线程不一定立即执行，因为此时其他线程可能正在运行。

wait 方法是 Object 类里的方法，当一个线程执行到 wait() 方法时，它就进入到一个和该对象相关的等待池中，同时释放了锁对象，等待期间可以调用里面的同步方法，其他线程可以访问，等待时不拥有 CPU 的执行权，否则其他线程无法获取执行权。当一个线程执行了 wait 方法后，必须调用 notify 或者 notifyAll 方法才能唤醒，而且是随机唤醒，若是被其他线程抢到了 CPU 执行权，该线程会继续进入等待状态。由于锁对象可以时任意对象，所以 wait 方法必须定义在 Object 类中，因为 Obeject 类是所有类的基类。

二. 是否可以传入参数

sleep() 方法必须传入参数，参数就是休眠时间，时间到了就会自动醒来。

wait() 方法可以传入参数也可以不传入参数，传入参数就是在参数结束的时间后开始等待，不穿如参数就是直接等待。

三. 是否需要捕获异常

sleep 方法必须要捕获异常，而 wait 方法不需要捕获异常。sleep 方法属于 Thread 类中方法，表示让一个线程进入睡眠状态，等待一定的时间之后，自动醒来进入到可运行状态，不会马上进入运行状态，因为线程调度机制恢复线程的运行也需要时间，一个线程对象调用了 sleep 方法之后，并不会释放他所持有的所有对象锁，所以也就不会影响其他进程对象的运行。但在 sleep 的过程中过程中有可能被其他对象调用它的 interrupt(), 产生 InterruptedException 异常，如果你的程序不捕获这个异常，线程就会异常终止，进入 TERMINATED 状态，如果你的程序捕获了这个异常，那么程序就会继续执行 catch 语句块 (可能还有 finally 语句块) 以及以后的代码。

wait 属于 Object 的成员方法，一旦一个对象调用了 wait 方法，必须要采用 notify() 和 notifyAll() 方法唤醒该进程; 如果线程拥有某个或某些对象的同步锁，那么在调用了 wait() 后，这个线程就会释放它持有的所有同步资源，而不限于这个被调用了 wait() 方法的对象。wait() 方法也同样会在 wait 的过程中有可能被其他对象调用 interrupt() 方法而产生。

四. 作用范围

wait、notify 和 notifyAll 方法只能在同步方法或者同步代码块中使用，而 sleep 方法可以在任何地方使用。但是注意 sleep 是静态方法，也就是说它只对当前对象有效。通过对象名. sleep() 想让该对象线程进入休眠是无效的，它只会让当前线程进入休眠。

五. 调用者的区别

首先为什么 wait、notify 和 notifyAll 方法要和 synchronized 关键字一起使用？

因为 wait 方法是使一个线程进入等待状态，并且释放其所持有的锁对象，notify 方法是通知等待该锁对象的线程重新获得锁对象，然而如果没有获得锁对象，wait 方法和 notify 方法都是没有意义的，因此必须先获得锁对象再对锁对象进行进一步操作于是才要把 wait 方法和 notify 方法写到同步方法和同步代码块中了。

由此可知，wait 和 notify、notifyAll 方法是由确定的对象即锁对象来调用的，锁对象就像一个传话的人，他对某个线程说停下来等待，然后对另一个线程说你可以执行了（实质上是被捕获了），这一过程是线程通信。sleep 方法是让某个线程暂停运行一段时间，其控制范围是由当前线程决定，运行的主动权是由当前线程来控制（拥有 CPU 的执行权）。

其实两者的区别都是让线程暂停运行一段时间，但本质的区别一个是线程的运行状态控制，一个是线程间的通信。

六. wait 方法（线程通信）的一些注意事项

例如以下代码：

当线程 print1 获得了锁对象（this）后进行判断，如果不满足条件（flag == 1）无法继续执行下面的代码，于是进入 wait 状态。在另一个线程 print3 中，由于改变了 flag，使得线程 print1 的判断条件满足了，线程 print1 就会被唤醒。

[![](http://common.cnblogs.com/images/copycode.gif)](javascript:void(0); "复制代码")

```java
class Printer {
    private int flag = 1;
    public void print1() throws Exception {
        synchronized(this) {
            while(flag != 1) {
                this.wait();
            }
            System.out.print("1");
            System.out.print("2");
            System.out.print("3");
            System.out.print("4");
            System.out.println();
            flag = 2;
            this.notifyAll();
        }
    }
    public void print2() throws Exception {
        synchronized(this) {
            while(flag != 2) {
                this.wait();
            }
            System.out.print("5");
            System.out.print("6");
            System.out.print("7");
            System.out.print("8");
            System.out.println();
            flag = 3;
            this.notifyAll();
        }
    }
    public void print3() throws Exception {
        synchronized(this) {
            while(flag != 3) {
                this.wait();
            }
            System.out.print("A");
            System.out.print("B");
            System.out.print("C");
            System.out.print("D");
            System.out.println();
            flag = 1;
            this.notifyAll();
        }
    }
}
```


注意事项：

1. 调用 wait 方法和 notify、notifyAll 方法前必须获得对象锁，也就是必须写在 synchronized(锁对象){......} 代码块中。

2. 当线程 print1 调用了 wait 方法后就释放了对象锁，否则其他线程无法获得对象锁，也就无法唤醒线程 print1。

3. 当 this.wait() 方法返回后，线程必须再次获得对象锁后才能继续执行。

4. 如果另外两个线程都在 wait，则正在执行的线程调用 notify 方法只能唤醒一个正在 wait 的线程（公平竞争，由 JVM 决定）。

5. 当使用 notifyAll 方法后，所有 wait 状态的线程都会被唤醒，但是只有一个线程能获得锁对象，必须执行完 while(condition){this.wait();} 后才能获得对象锁。其余的需要等待该获得对象锁的线程执行完释放对象锁后才能继续执行。

6. 当某个线程调用 notifyAll 方法后，虽然其他线程被唤醒了，但是该线程依然持有着对象锁，必须等该同步代码块执行完（右大括号结束）后才算正式释放了锁对象，另外两个线程才有机会执行。
