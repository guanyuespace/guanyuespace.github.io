# JAVA--AQS详解
> [原文地址](https://www.jianshu.com/p/da9d051dcc3d)

 `AQS`是 `AbstractQueuedSynchronizer` 的简称。`AQS` 提供了一种实现阻塞锁和一系列依赖 `FIFO` 等待队列的同步器的框架，如下图所示。`AQS` 为一系列同步器依赖于一个单独的原子变量（state）的同步器提供了一个非常有用的基础。子类们必须定义改变 `state` 变量的 `protected` 方法，这些方法定义了 `state` 是如何被获取或释放的。鉴于此，本类中的其他方法执行所有的排队和阻塞机制。子类也可以维护其他的 state 变量，但是为了保证同步，必须原子地操作这些变量。

![AQS.png](http://upload-images.jianshu.io/upload_images/53727-ae36db58241c256b.png "AQS.png")

`AbstractQueuedSynchronizer` 中对 `state` 的操作是原子的，且不能被继承。**所有的同步机制的实现均依赖于对改变量的原子操作**。为了实现不同的同步机制，我们需要创建一个非共有的（non-public internal）扩展了 `AQS` 类的内部辅助类来实现相应的同步逻辑。`AbstractQueuedSynchronizer` 并不实现任何同步接口，它提供了一些可以被具体实现类直接调用的一些原子操作方法来重写相应的同步逻辑。`AQS` 同时提供了互斥模式（`exclusive`）和共享模式（`shared`）两种不同的同步逻辑。一般情况下，子类只需要根据需求实现其中一种模式，当然也有同时实现两种模式的同步类，如`ReadWriteLock`。接下来将详细介绍 `AbstractQueuedSynchronizer` 的提供的一些具体实现方法。

## state 状态

 `AbstractQueuedSynchronizer` 维护了一个 `volatile int` 类型的变量，用户表示当前同步状态。`volatile` 虽然不能保证操作的原子性，但是保证了当前变量 `state` 的可见性。至于 [volatile](https://www.jianshu.com/p/14fc9d34de33) 的具体语义，可以参考相关文章。`state` 的访问方式有三种:
- getState()
- setState()
- compareAndSetState()

这三种叫做均是原子操作，其中 `compareAndSetState` 的实现依赖于 `Unsafe` 的 `compareAndSwapInt()` 方法。代码实现如下：

```java
/**
 * The synchronization state.
 */
private volatile int state;

/**
 * Returns the current value of synchronization state.
 * This operation has memory semantics of a {@code volatile} read.
 * @return current state value
 */
protected final int getState() {
    return state;
}

/**
 * Sets the value of synchronization state.
 * This operation has memory semantics of a {@code volatile} write.
 * @param newState the new state value
 */
protected final void setState(int newState) {
    state = newState;
}

/**
 * Atomically sets synchronization state to the given updated
 * value if the current state value equals the expected value.
 * This operation has memory semantics of a {@code volatile} read
 * and write.
 *
 * @param expect the expected value
 * @param update the new value
 * @return {@code true} if successful. False return indicates that the actual
 *         value was not equal to the expected value.
 */
protected final boolean compareAndSetState(int expect, int update) {
    // See below for intrinsics setup to support this
    return unsafe.compareAndSwapInt(this, stateOffset, expect, update);
}
```

## 自定义资源共享方式

`AQS` 定义两种资源共享方式：`Exclusive`（独占，只有一个线程能执行，如 `ReentrantLock`）和 `Share`（共享，多个线程可同时执行，如 `Semaphore/CountDownLatch`）。

**不同的自定义同步器争用共享资源的方式也不同。自定义同步器在实现时只需要实现共享资源 `state` 的获取与释放方式即可，至于具体线程等待队列的维护（如获取资源失败入队 / 唤醒出队等），AQS 已经在顶层实现好了**。

自定义同步器实现时主要实现以下几种方法：
- isHeldExclusively()：该线程是否正在独占资源。只有用到 `condition` 才需要去实现它。
- tryAcquire(int)：独占方式。尝试获取资源，成功则返回 `true`，失败则返回 `false`。
- tryRelease(int)：独占方式。尝试释放资源，成功则返回 `true`，失败则返回 `false`。
- tryAcquireShared(int)：共享方式。尝试获取资源。负数表示失败；0 表示成功，但没有剩余可用资源；正数表示成功，且有剩余资源。
- tryReleaseShared(int)：共享方式。尝试释放资源，如果释放后允许唤醒后续等待结点返回 `true`，否则返回 `false`。

## 源码实现

接下来我们开始开始讲解 `AQS` 的源码实现。依照 `acquire-release`、`acquireShared-releaseShared` 的次序来。

### 1. acquire(int)

`acquire` 是一种以独占方式获取资源，如果获取到资源，线程直接返回，否则进入等待队列，直到获取到资源为止，且整个过程忽略中断的影响。该方法是独占模式下线程获取共享资源的顶层入口。获取到资源后，线程就可以去执行其临界区代码了。下面是 `acquire()` 的源码：

```java
/**
 * Acquires in exclusive mode, ignoring interrupts.  Implemented
 * by invoking at least once {@link #tryAcquire},
 * returning on success.  Otherwise the thread is queued, possibly
 * repeatedly blocking and unblocking, invoking {@link
 * #tryAcquire} until success.  This method can be used
 * to implement method {@link Lock#lock}.
 *
 * @param arg the acquire argument.  This value is conveyed to
 *        {@link #tryAcquire} but is otherwise uninterpreted and
 *        can represent anything you like.
 */
public final void acquire(int arg) {
    if (!tryAcquire(arg) &&
        acquireQueued(addWaiter(Node.EXCLUSIVE), arg));
        selfInterrupt();
}
```

  通过注释我们知道，`acquire` 方法是一种互斥模式，且忽略中断。该方法至少执行一次`tryAcquire(int)`方法，如果 `tryAcquire`(int) 方法返回 `true`，则 `acquire` 直接返回，否则当前线程需要进入队列进行排队。函数流程如下：

1. tryAcquire() 尝试直接去获取资源，如果成功则直接返回；
2. addWaiter() 将该线程加入等待队列的尾部，并标记为独占模式；
3. acquireQueued() 使线程在等待队列中获取资源，一直获取到资源后才返回。**如果在整个等待过程中被中断过，则返回 `true`，否则返回 false**。
4. 如果线程在等待过程中被中断过，它是不响应的。只是获取资源后才再进行自我中断 `selfInterrupt()`，将中断补上。

接下来一次介绍相关方法。

### 1.1 tryAcquire(int)
>子类自定义Method--&gt;获取资源

`tryAcquire` 尝试以独占的方式获取资源，如果获取成功，则直接返回 `true`，否则直接返回 `false`。该方法可以用于实现 Lock 中的 tryLock() 方法。该方法的默认实现是抛出`UnsupportedOperationException`，具体实现由自定义的扩展了 AQS 的同步类来实现。AQS 在这里只负责定义了一个公共的方法框架。这里之所以没有定义成 abstract，是因为独占模式下只用实现 tryAcquire-tryRelease，而共享模式下只用实现 tryAcquireShared-tryReleaseShared。如果都定义成 abstract，那么每个模式也要去实现另一模式下的接口。

```java
/**
 * Attempts to acquire in exclusive mode. This method should query
 * if the state of the object permits it to be acquired in the
 * exclusive mode, and if so to acquire it.
 *
 * <p>This method is always invoked by the thread performing
 * acquire.  If this method reports failure, the acquire method
 * may queue the thread, if it is not already queued, until it is
 * signalled by a release from some other thread. This can be used
 * to implement method {@link Lock#tryLock()}.
 *
 * <p>The default
 * implementation throws {@link UnsupportedOperationException}.
 *
 * @param arg the acquire argument. This value is always the one
 *        passed to an acquire method, or is the value saved on entry
 *        to a condition wait.  The value is otherwise uninterpreted
 *        and can represent anything you like.
 * @return {@code true} if successful. Upon success, this object has
 *         been acquired.
 * @throws IllegalMonitorStateException if acquiring would place this
 *         synchronizer in an illegal state. This exception must be
 *         thrown in a consistent fashion for synchronization to work
 *         correctly.
 * @throws UnsupportedOperationException if exclusive mode is not supported
 */
protected boolean tryAcquire(int arg) {
    throw new UnsupportedOperationException();
}

```

### 1.2 addWaiter(Node)
>加入等待队列

该方法用于将当前线程根据不同的模式（`Node.EXCLUSIVE`互斥模式、`Node.SHARED`共享模式）加入到等待队列的队尾，并返回当前线程所在的结点。如果队列不为空，则以通过`compareAndSetTail`方法以 CAS 的方式将当前线程节点加入到等待队列的末尾。<!-- maybe set tail failed. -->否则，通过 `enq(node)` 方法初始化一个等待队列，并返回当前节点。源码如下：

```java
/**
 * Creates and enqueues node for current thread and given mode.
 *
 * @param mode Node.EXCLUSIVE for exclusive, Node.SHARED for shared
 * @return the new node
 */
private Node addWaiter(Node mode) {
    Node node = new Node(Thread.currentThread(), mode);
    // Try the fast path of enq; backup to full enq on failure
    Node pred = tail;
    if (pred != null) {
        node.prev = pred;
        if (compareAndSetTail(pred, node)) {
            pred.next = node;
            return node;
        }
    }
    enq(node);
    return node;
}

```

### 1.2.1 enq(node)
>初始化队列&入队

`enq(node)`用于将当前节点插入等待队列，如果队列为空，则初始化当前队列。整个过程以 CAS 自旋的方式进行，直到成功加入队尾为止。源码如下：

```java
/**
 * Inserts node into queue, initializing if necessary. See picture above.
 * @param node the node to insert
 * @return node's predecessor
 */
private Node enq(final Node node) {
    for (;;) {
        Node t = tail;
        if (t == null) { // Must initialize
            if (compareAndSetHead(new Node()))
                tail = head;
        } else {
            node.prev = t;
            if (compareAndSetTail(t, node)) {
                t.next = node;
                return t;
            }
        }
    }
}
```

### **1.3 acquireQueued(Node, int)**
>`acquireQueued()` **使线程在等待队列中获取资源**，一直获取到资源后才返回。如果在整个等待过程中被中断过，则返回 true，否则返回 false。

`acquireQueued()`用于队列中的线程自旋地以独占且不可中断的方式获取同步状态（acquire），直到拿到锁之后再返回。
该方法的实现分成两部分：
1. 如果当前节点已经成为头结点，尝试获取锁（tryAcquire）成功，然后返回；
2. 否则检查当前节点是否应该被 park，然后将该线程 park 并且检查当前线程是否被可以被中断。

```java
/**
 * Acquires in exclusive uninterruptible mode for thread already in queue.
 * Used by condition wait methods as well as acquire.
 * @param node the node
 * @param arg the acquire argument
 * @return {@code true} if interrupted while waiting
 */
final boolean acquireQueued(final Node node, int arg) {
    //标记是否成功拿到资源，默认false
    boolean failed = true;
    try {
        boolean interrupted = false;//标记等待过程中是否被中断过
        for (;;) {//loop
            final Node p = node.predecessor();
            if (p == head && tryAcquire(arg)) {//在等待队列中熬成了首部，此时尝试获取资源
                setHead(node);
                p.next = null; // help GC
                failed = false;
                return interrupted;
            }
            ////////////////////等待//////////////////////
            if (shouldParkAfterFailedAcquire(p, node) &&
                parkAndCheckInterrupt())
                interrupted = true;
        }
    } finally {
        if (failed)
            cancelAcquire(node);//failed...
    }
}
```

### 1.3.1 shouldParkAfterFailedAcquire(Node, Node)
>等待队列中节点状态的检查与更新

`shouldParkAfterFailedAcquire` 方法通过对当前节点的前一个节点的状态进行判断，对当前节点做出不同的操作，至于每个 Node 的状态表示，可以参考接口文档。

```java
/**
 * Checks and updates status for a node that failed to acquire.
 * Returns true if thread should block.
 * This is the main signal control in all acquire loops.
 * Requires that pred == node.prev.
 *
 * @param pred node's predecessor holding status
 * @param node the node
 * @return {@code true} if thread should block
 */
private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
    int ws = pred.waitStatus;
    if (ws == Node.SIGNAL)
        // This node has already set status asking a release to signal it, so it can safely park.
        return true;
    if (ws > 0) {//remove CANCELED nodes in the waiting queue.
        // Predecessor was cancelled. Skip over predecessors and indicate retry.
        do {
            node.prev = pred = pred.prev;
        } while (pred.waitStatus > 0);//find the non-canceled node
        pred.next = node;
    } else {
        /*
         * waitStatus must be 0 or PROPAGATE.
         * Indicate that we need a signal, but don't park yet.
         * Caller will need to retry to make sure it cannot acquire before parking.
         */
        compareAndSetWaitStatus(pred, ws, Node.SIGNAL);//want to be signalled ,in other words want to acquire
    }
    return false;
}
```

### 1.3.2 parkAndCheckInterrupt()
> just like sleep(), wait here. 节省CPU资源

该方法让线程去休息，真正进入等待状态。
`park()` 会让当前线程进入 `waiting` 状态。
在此状态下，有两种途径可以唤醒该线程：
1. 被 unpark()；
2. 被 interrupt()。
>需要注意的是，Thread.interrupted() 会清除当前线程的中断标记位。

```java
/**
 * Convenience method to park and then check if interrupted
 *
 * @return {@code true} if interrupted
 */
private final boolean parkAndCheckInterrupt() {
    LockSupport.park(this);
    return Thread.interrupted();
}
```

### acquireQueued()小节

1. 结点进入队尾后，检查状态，找到安全休息点；
2. 调用 `park()` 进入 `waiting` 状态，等待 `unpark()` 或 `interrupt()` 唤醒自己；
3. 被唤醒后，看自己是不是有资格能拿到号。如果拿到，`head` 指向当前结点，并返回从入队到拿到号的整个过程中是否被中断过；如果没拿到，继续流程 1。

### acquire()小节--独占性获取锁

1. 调用自定义同步器的 `tryAcquire()` 尝试直接去获取资源，如果成功则直接返回；
2. 没成功，则 `addWaiter()` 将该线程加入等待队列的尾部，并标记为独占模式；
3. `acquireQueued()` 使线程在等待队列中休息，有机会时（轮到自己，会被 `unpark()`）会去尝试获取资源。获取到资源后才返回。如果在整个等待过程中被中断过，则返回 `true`，否则返回 `false`。
4. 如果线程在等待过程中被中断过，它是不响应的。只是获取资源后才再进行自我中断 `selfInterrupt()`，将中断补上。


### 2. release(int)
>独占方式释放

`release(int)`方法是独占模式下线程释放共享资源的顶层入口。它会释放指定量的资源，如果彻底释放了（即 state=0）, 它会唤醒等待队列里的其他线程来获取资源。这也正是 `unlock()` 的语义，当然不仅仅只限于 `unlock()`。下面是 `release()` 的源码：

```java
/**
 * Releases in exclusive mode.
 * Implemented by unblocking one or  more threads if {@link #tryRelease} returns true.
 * This method can be used to implement method {@link Lock#unlock}.
 *
 * @param arg the release argument.  This value is conveyed to
 *        {@link #tryRelease} but is otherwise uninterpreted and
 *        can represent anything you like.
 * @return the value returned from {@link #tryRelease}
 */
public final boolean release(int arg) {
    if (tryRelease(arg)) {//try ok
        Node h = head;
        if (h != null && h.waitStatus != 0)
            unparkSuccessor(h);//唤醒后继
        return true;
    }
    return false;
}

/**
 * Attempts to set the state to reflect a release in exclusive mode.
 *
 * <p>This method is always invoked by the thread performing release.
 *
 * <p>The default implementation throws {@link UnsupportedOperationException}.
 *
 * @param arg the release argument. This value is always the one
 *        passed to a release method, or the current state value upon
 *        entry to a condition wait.  The value is otherwise
 *        uninterpreted and can represent anything you like.
 * @return {@code true} if this object is now in a fully released
 *         state, so that any waiting threads may attempt to acquire;
 *         and {@code false} otherwise.
 * @throws IllegalMonitorStateException if releasing would place this
 *         synchronizer in an illegal state. This exception must be
 *         thrown in a consistent fashion for synchronization to work
 *         correctly.
 * @throws UnsupportedOperationException if exclusive mode is not supported
 */
protected boolean tryRelease(int arg) {
    throw new UnsupportedOperationException();
}

/**
 * Wakes up node's successor, if one exists.
 * @param node the node
 */
private void unparkSuccessor(Node node) {
    /*
     * If status is negative (i.e., possibly needing signal) try to clear in anticipation of signalling.
     * It is OK if this fails or if status is changed by waiting thread.
     */
    int ws = node.waitStatus;
    if (ws < 0)
        compareAndSetWaitStatus(node, ws, 0);//当前节点状态更新，无所谓了吧

    /*
     * Thread to unpark is held in successor, which is normally just the next node.
     * But if cancelled or apparently null, traverse backwards from tail to find the actual non-cancelled successor.
     */
    Node s = node.next;
    if (s == null || s.waitStatus > 0) {
        s = null;
        for (Node t = tail; t != null && t != node; t = t.prev)//ok
            if (t.waitStatus <= 0)//non-canceled
                s = t;
    }
    if (s != null)
        LockSupport.unpark(s.thread);//唤醒后继节点
}
```

与 `acquire()`方法中的 `tryAcquire()`类似，`tryRelease()`方法也是需要独占模式的自定义同步器去实现的。

正常来说，`tryRelease()`都会成功的，因为这是独占模式，该线程来释放资源，那么它肯定已经拿到独占资源了，直接减掉相应量的资源即可 (state-=arg)，也不需要考虑线程安全的问题。

但要注意它的返回值，上面已经提到了，`release()` 是根据 `tryRelease()`的返回值来判断该线程是否已经完成释放掉资源了！所以自义定同步器在实现时，如果已经彻底释放资源(state=0)，要返回 `true`，否则返回 `false`。

`unparkSuccessor(Node)`方法用于唤醒等待队列中下一个线程。这里要注意的是，下一个线程并不一定是当前节点的 `next` 节点，而是下一个可以用来唤醒的线程，如果这个节点存在，调用`unpark()`方法唤醒。

总之，`release()` 是独占模式下线程释放共享资源的顶层入口。它会释放指定量的资源，如果彻底释放了（即 state=0）, 它会唤醒等待队列里的其他线程来获取资源。

### 3. acquireShared(int)
>共享模式获取

`acquireShared(int)`方法是共享模式下线程获取共享资源的顶层入口。它会获取指定量的资源，获取成功则直接返回，获取失败则进入等待队列，直到获取到资源为止，整个过程忽略中断。下面是 acquireShared() 的源码：

```java
/**
 * Acquires in shared mode, ignoring interrupts.
 * Implemented by first invoking at least once {@link #tryAcquireShared}, returning on success.
 * Otherwise the thread is queued, possibly repeatedly blocking and unblocking, invoking {@link #tryAcquireShared} until success.
 *
 * @param arg the acquire argument.  This value is conveyed to
 *        {@link #tryAcquireShared} but is otherwise uninterpreted
 *        and can represent anything you like.
 */
public final void acquireShared(int arg) {
    if (tryAcquireShared(arg) < 0)
        doAcquireShared(arg);
}
```

### **3.1 doAcquireShared(int)**
>加入等待队列  联系acquireQueued

将当前线程加入等待队列尾部休息，直到其他线程释放资源唤醒自己，自己成功拿到相应量的资源后才返回。源码如下：

```java
 /**
 * Acquires in shared uninterruptible mode.
 * @param arg the acquire argument
 */
private void doAcquireShared(int arg) {
    final Node node = addWaiter(Node.SHARED);
    boolean failed = true;
    try {
        boolean interrupted = false;
        for (;;) {//loop
            final Node p = node.predecessor();
            if (p == head) {//首部节点尝试获取
                int r = tryAcquireShared(arg);
                if (r >= 0) {//获取成功
                    setHeadAndPropagate(node, r);
                    p.next = null; // help GC
                    if (interrupted)
                        selfInterrupt();//发生过中断则唤醒当前线程
                    failed = false;
                    return;
                }
            }
            //////////////等待//////////////////////the below code just be the same as the exclusive acquire method
            if (shouldParkAfterFailedAcquire(p, node) &&
                parkAndCheckInterrupt())
                interrupted = true;
        }
    } finally {
        if (failed)
            cancelAcquire(node);
    }
}

```

跟独占模式比，还有一点需要注意的是，这里只有线程是 head.next 时（“老二”），才会去尝试获取资源，有剩余的话还会唤醒之后的队友。

那么问题就来了，假如老大用完后释放了 5 个资源，而老二需要 6 个，老三需要 1 个，老四需要 2 个。老大先唤醒老二，老二一看资源不够，他是把资源让给老三呢，还是不让？答案是否定的！

老二会继续 park() 等待其他线程释放资源，也更不会去唤醒老三和老四了。

独占模式，同一时刻只有一个线程去执行，这样做未尝不可；

但共享模式下，多个线程是可以同时执行的，现在因为老二的资源需求量大，而把后面量小的老三和老四也都卡住了。当然，这并不是问题，只是 AQS 保证严格按照入队顺序唤醒罢了（保证公平，但降低了并发）。实现如下：

```java
/**
 * Sets head of queue, and checks if successor may be waiting in shared mode, if so propagating if either propagate > 0 or PROPAGATE status was set.
 *
 * @param node the node
 * @param propagate the return value from a tryAcquireShared
 */
private void setHeadAndPropagate(Node node, int propagate) {
    Node h = head; // Record old head for check below
    setHead(node);
    /*
     * Try to signal next queued node if:
     *   Propagation was indicated by caller,
     *     or was recorded (as h.waitStatus either before
     *     or after setHead) by a previous operation
     *     (note: this uses sign-check of waitStatus because
     *      PROPAGATE status may transition to SIGNAL.)
     * and
     *   The next node is waiting in shared mode,
     *     or we don't know, because it appears null
     *
     * The conservatism in both of these checks may cause unnecessary wake-ups, but only when there are multiple racing acquires/releases, so most need signals now or soon anyway.
     */
     //释放前驱结点的资源。。。
    if (propagate > 0 || h == null || h.waitStatus < 0 ||
        (h = head) == null || h.waitStatus < 0) {
        Node s = node.next;
        if (s == null || s.isShared())
            doReleaseShared();//释放资源
    }
}
```

此方法在 `setHead()` 的基础上多了一步，就是自己苏醒的同时，如果条件符合（比如还有剩余资源），还会去唤醒后继结点，毕竟是共享模式！至此，`acquireShared()` 也要告一段落了。让我们再梳理一下它的流程：

1. `tryAcquireShared()` 尝试获取资源，成功则直接返回；
2. 失败则通过 `doAcquireShared()` 进入等待队列 `park()`，直到被 `unpark()`/`interrupt()` 并成功获取到资源才返回。整个等待过程也是忽略中断的。

### 4. releaseShared(int)
>共享模式释放资源

`releaseShared(int)`方法是共享模式下线程释放共享资源的顶层入口。它会释放指定量的资源，如果成功释放且允许唤醒等待线程，它会唤醒等待队列里的其他线程来获取资源。下面是 `releaseShared()` 的源码：

```java
/**
 * Releases in shared mode.  Implemented by unblocking one or more
 * threads if {@link #tryReleaseShared} returns true.
 *
 * @param arg the release argument.  This value is conveyed to
 *        {@link #tryReleaseShared} but is otherwise uninterpreted
 *        and can represent anything you like.
 * @return the value returned from {@link #tryReleaseShared}
 */
public final boolean releaseShared(int arg) {
    if (tryReleaseShared(arg)) {
        doReleaseShared();//释放。。。
        return true;
    }
    return false;
}
```

此方法的流程也比较简单，一句话：**释放掉资源后，唤醒后继**。跟独占模式下的 release() 相似，但有一点稍微需要注意：**独占模式下的 tryRelease() 在完全释放掉资源（state=0）后，才会返回 true 去唤醒其他线程，这主要是基于独占下可重入的考量；而共享模式下的 releaseShared() 则没有这种要求，共享模式实质就是控制一定量的线程并发执行，那么拥有资源的线程在释放掉部分资源时就可以唤醒后继等待结点**。

```java
/**
 * Release action for shared mode -- signals successor and ensures propagation.
 * (Note: For exclusive mode, release just amounts to calling unparkSuccessor of head if it needs signal.)
 */
private void doReleaseShared() {
    /*
     * Ensure that a release propagates, even if there are other
     * in-progress acquires/releases.  This proceeds in the usual
     * way of trying to unparkSuccessor of head if it needs
     * signal. But if it does not, status is set to PROPAGATE to
     * ensure that upon release, propagation continues.
     * Additionally, we must loop in case a new node is added
     * while we are doing this. Also, unlike other uses of
     * unparkSuccessor, we need to know if CAS to reset status
     * fails, if so rechecking.
     */
    for (;;) {
        Node h = head;
        if (h != null && h != tail) {
            int ws = h.waitStatus;
            if (ws == Node.SIGNAL) {//唤醒后继
                if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0))
                    continue;            // loop to recheck cases
                unparkSuccessor(h);
            }
            else if (ws == 0 &&
                     !compareAndSetWaitStatus(h, 0, Node.PROPAGATE))
                continue;                // loop on failed CAS
        }
        if (h == head)                   // loop if head changed
            break;
    }
}
```

参考：[https://www.cnblogs.com/waterystone/p/4920797.html](https://link.jianshu.com?t=https%3A%2F%2Fwww.cnblogs.com%2Fwaterystone%2Fp%2F4920797.html)
