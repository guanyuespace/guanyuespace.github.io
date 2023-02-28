>翻译自 [How To Do In Java](http://howtodoinjava.com/2014/06/03/difference-between-yield-and-join-in-threads-in-java/)。欢迎加入[翻译小组](http://group.jobbole.com/category/feedback/trans-team/)。转载请见文末要求。

### Java 线程调度的一点背景

在各种各样的线程中，Java 虚拟机必须实现一个有优先权的、基于优先级的调度程序。这意味着 Java 程序中的每一个线程被分配到一定的优先权，使用定义好的范围内的一个正整数表示。优先级可以被开发者改变。即使线程已经运行了一定时间，Java 虚拟机也不会改变其优先级

优先级的值很重要，因为 Java 虚拟机和下层的操作系统之间的约定是操作系统必须选择有最高优先权的 Java 线程运行。所以我们说 Java 实现了一个[基于优先权的调度程序](http://en.wikipedia.org/wiki/Fixed-priority_pre-emptive_scheduling)。该调度程序使用一种有优先权的方式实现，这意味着当一个有更高优先权的线程到来时，无论低优先级的线程是否在运行，都会中断 (抢占) 它。这个约定对于操作系统来说并不总是这样，这意味着操作系统有时可能会选择运行一个更低优先级的线程。(我憎恨多线程的这一点，因为这不能保证任何事情)

> **注意**
>Java并不限定线程是以时间片运行，但是大多数操作系统却有这样的要求。在术语中经常引起混淆：抢占经常与时间片混淆。事实上，抢占意味着只有拥有高优先级的线程可以优先于低优先级的线程执行，但是当线程拥有相同优先级的时候，他们不能相互抢占。它们通常受时间片管制，但这并不是 Java 的要求。

### **理解线程的优先权**

接下来，理解线程优先级是多线程学习很重要的一步，尤其是了解 yield() 函数的工作过程。

1.  记住当线程的优先级没有指定时，所有线程都携带普通优先级。
2.  优先级可以用从 1 到 10 的范围指定。10 表示最高优先级，1 表示最低优先级，5 是普通优先级。
3.  记住优先级最高的线程在执行时被给予优先。但是不能保证线程在启动时就进入运行状态。
4.  与在线程池中等待运行机会的线程相比， **当前正在运行的线程可能总是拥有更高的优先级。**
5.  由调度程序决定哪一个线程被执行。
6.  t.setPriority() 用来设定线程的优先级。
7.  记住在线程开始方法被调用之前，线程的优先级应该被设定。
8.  你可以使用常量，如 MIN_PRIORITY,MAX_PRIORITY，NORM_PRIORITY 来设定优先级

现在，当我们对线程调度和线程优先级有一定理解后，让我们进入主题。

### **yield() 方法**

理论上，yield 意味着放手，放弃，投降。
一个调用 yield() 方法的线程告诉虚拟机它乐意让其他线程占用自己的位置。这表明该线程没有在做一些紧急的事情。
注意，这仅是一个暗示，并不能保证不会产生任何影响。

在 Thread.java 中 yield() 定义如下：

```java
/**
 * A hint to the scheduler that the current thread is willing to yield its current use of a processor.
 * The scheduler is free to ignore this hint.
 * Yield is a heuristic attempt to improve relative progression between threads that would otherwise over-utilize a CPU.
 * Its use should be combined with detailed profiling and benchmarking to ensure that it actually has the desired effect.
 */
 public static native void yield();
```

让我们列举一下关于以上定义重要的几点：

*   Yield 是一个静态的原生 (native) 方法
*   Yield 告诉当前正在执行的线程把运行机会交给线程池中拥有相同优先级的线程。
*   Yield 不能保证使得当前正在运行的线程迅速转换到可运行的状态
*   它仅能使一个线程从运行状态转到可运行状态，而不是等待或阻塞状态

### **yield() 方法使用示例**

在下面的示例程序中，我随意的创建了名为生产者和消费者的两个线程。生产者设定为最小优先级，消费者设定为最高优先级。在 Thread.yield() 注释和非注释的情况下我将分别运行该程序。没有调用 yield() 方法时，虽然输出有时改变，但是通常消费者行先打印出来，然后事生产者。

调用 yield() 方法时，两个线程依次打印，然后将执行机会交给对方，一直这样进行下去。

```java
package test.core.threads; 
public class YieldExample{
    public static void main(String[] args){
        Thread producer = new Producer();
        Thread consumer = new Consumer();
        producer.setPriority(Thread.MIN_PRIORITY); //Min Priority
        consumer.setPriority(Thread.MAX_PRIORITY); //Max Priority 
        producer.start();consumer.start();
    }
} 
class Producer extends Thread{
    public void run(){
        for (int i = 0; i < 5; i++){
            System.out.println("I am Producer : Produced Item " + i);
            Thread.yield();
        }
    }
} 
class Consumer extends Thread{
    public void run(){
        for (int i = 0; i < 5; i++){
            System.out.println("I am Consumer : Consumed Item " + i);
            Thread.yield();
        }
    }
}
```

#### **上述程序在没有调用 yield() 方法情况下的输出：**

```
I am Consumer : Consumed Item 0
I am Consumer : Consumed Item 1
I am Consumer : Consumed Item 2
I am Consumer : Consumed Item 3
I am Consumer : Consumed Item 4
I am Producer : Produced Item 0
I am Producer : Produced Item 1
I am Producer : Produced Item 2
I am Producer : Produced Item 3
I am Producer : Produced Item 4
```

#### **上述程序在调用 yield() 方法情况下的输出：**

```
I am Producer : Produced Item 0
I am Consumer : Consumed Item 0
I am Producer : Produced Item 1
I am Consumer : Consumed Item 1
I am Producer : Produced Item 2
I am Consumer : Consumed Item 2
I am Producer : Produced Item 3
I am Consumer : Consumed Item 3
I am Producer : Produced Item 4
I am Consumer : Consumed Item 4
```

### **join() 方法**

线程实例的方法 join() 方法可以使得一个线程在另一个线程结束后再执行。如果 join() 方法在一个线程实例上调用，当前运行着的线程将阻塞直到这个线程实例完成了执行。

```java
//Waits for this thread to die. 
public final synchronized void join() throws InterruptedException {... wait();}
````

在 join() 方法内设定超时，使得 join() 方法的影响在特定超时后无效。
当超时时，主方法和任务线程申请运行的时候是平等的。然而，当涉及 sleep 时，join() 方法依靠操作系统计时，所以你不应该假定 join() 方法将会等待你指定的时间。

像 sleep,join 通过抛出 InterruptedException 对中断做出回应。

### **join() 方法使用示例**

```java
package test.core.threads; 
public class JoinExample{
    public static void main(String[] args) throws InterruptedException{
        Thread t = new Thread(new Runnable(){
            public void run(){
                System.out.println("First task started");
                System.out.println("Sleeping for 2 seconds");
                try{
                    Thread.sleep(2000);
                } catch (InterruptedException e){
                    e.printStackTrace();
                }
                System.out.println("First task completed");
            }
        });

        Thread t1 = new Thread(new Runnable(){
            public void run(){
                System.out.println("Second task completed");
            }
        });

        t.start(); // Line 15
        t.join();  // Line 16
        t1.start();
    }
} 
```
Output: 

```
First task started
Sleeping for 2 seconds
First task completed
Second task completed
```

这是一些很小却很重要的概念。在评论部分让我知道你的想法。

### 原文链接
[How To Do In Java](http://howtodoinjava.com/2014/06/03/difference-between-yield-and-join-in-threads-in-java/)

### 翻译
[ImportNew.com](http://www.importnew.com)

[Calarence](http://www.importnew.com/author/calarencce)

[译文链接](http://www.importnew.com/14958.html)


### 关于作者
[Calarence](http://www.importnew.com/author/calarencce)
