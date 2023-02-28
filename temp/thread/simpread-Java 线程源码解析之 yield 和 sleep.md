# yield与sleep源码解析
> [原文地址](https://www.jianshu.com/p/0964124ae822)

# 概述

由于 Thread 的 yield 和 sleep 有一定的相似性，因此放在一起进行分析。yield 会释放 CPU 资源，让优先级更高（至少是相同）的线程获得执行机会；sleep 当传入参数为 0 时，和 yield 相同；当传入参数大于 0 时，也是释放 CPU 资源，当可以让其它任何优先级的线程获得执行机会；

假设当前进程只有 main 线程，当调用 yield 之后，main 线程会继续运行，因为没有比它优先级更高的线程；而调用 sleep 之后，mian 线程会进入 TIMED_WAITING 状态，不会继续运行；

# yield

Thread.sleep 底层是通过 JVM_Yield 方法实现的（见 jvm.cpp）:
```cpp
JVM_ENTRY(void, JVM_Yield(JNIEnv *env, jclass threadClass))
  JVMWrapper("JVM_Yield");
  //检查是否设置了DontYieldALot参数,默认为fasle
  //如果设置为true,直接返回
  if (os::dont_yield()) return;
 //如果ConvertYieldToSleep=true（默认为false）,调用os::sleep,否则调用os::yield
  if (ConvertYieldToSleep) {
    os::sleep(thread, MinSleepInterval, false);//sleep 1ms
  } else {
    os::yield();
  }
JVM_END
```

从上面知道，实际上调用的是 os::yield:

```cpp
//sched_yield是linux kernel提供的API,它会使调用线程放弃CPU使用权，加入到同等优先级队列的末尾；
//如果调用线程是优先级最高的唯一线程，yield方法返回后，调用线程会继续运行；
//因此可以知道，对于和调用线程相同或更高优先级的线程来说，yield方法会给予了它们一次运行的机会；
void os::yield() {
  sched_yield();
}

```

# sleep

Thread.sleep 最终调用 JVM_Sleep 方法：

```cpp
JVM_ENTRY(void, JVM_Sleep(JNIEnv* env, jclass threadClass, jlong millis))
  JVMWrapper("JVM_Sleep");

  if (millis < 0) {//参数校验
    THROW_MSG(vmSymbols::java_lang_IllegalArgumentException(), "timeout value is negative");
  }

  //如果线程已经中断，抛出中断异常,关于中断的实现，在另一篇文章中会讲解
  if (Thread::is_interrupted (THREAD, true) && !HAS_PENDING_EXCEPTION) {
    THROW_MSG(vmSymbols::java_lang_InterruptedException(), "sleep interrupted");
  }
  //设置线程状态为SLEEPING
  JavaThreadSleepState jtss(thread);

  EventThreadSleep event;

  if (millis == 0) {
    //如果设置了ConvertSleepToYield(默认为true),和yield效果相同
    if (ConvertSleepToYield) {
      os::yield();
    } else {//否则调用os::sleep方法
      ThreadState old_state = thread->osthread()->get_state();
      thread->osthread()->set_state(SLEEPING);
      os::sleep(thread, MinSleepInterval, false);//sleep 1ms
      thread->osthread()->set_state(old_state);
    }
  } else {//参数大于0
   //保存初始状态，返回时恢复原状态
    ThreadState old_state = thread->osthread()->get_state();
    //osthread->thread status mapping:
    // NEW->NEW
    //RUNNABLE->RUNNABLE
    //BLOCKED_ON_MONITOR_ENTER->BLOCKED
    //IN_OBJECT_WAIT,PARKED->WAITING
    //SLEEPING,IN_OBJECT_WAIT_TIMED,PARKED_TIMED->TIMED_WAITING
    //TERMINATED->TERMINATED
    thread->osthread()->set_state(SLEEPING);
    //调用os::sleep方法，如果发生中断，抛出异常
    if (os::sleep(thread, millis, true) == OS_INTRPT) {
      if (!HAS_PENDING_EXCEPTION) {
        if (event.should_commit()) {
          event.set_time(millis);
          event.commit();
        }
        THROW_MSG(vmSymbols::java_lang_InterruptedException(), "sleep interrupted");
      }
    }
    thread->osthread()->set_state(old_state);//恢复osThread状态
  }
  if (event.should_commit()) {
    event.set_time(millis);
    event.commit();
  }
JVM_END

```

os::sleep 的源码如下：

```cpp
int os::sleep(Thread* thread, jlong millis, bool interruptible) {
  assert(thread == Thread::current(),  "thread consistency check");
  //线程有如下几个成员变量:
  //ParkEvent * _ParkEvent ;          // for synchronized()
  //ParkEvent * _SleepEvent ;        // for Thread.sleep
  //ParkEvent * _MutexEvent ;      // for native internal Mutex/Monitor
  //ParkEvent * _MuxEvent ;         // for low-level muxAcquire-muxRelease
  ParkEvent * const slp = thread->_SleepEvent ;
  slp->reset() ;
  OrderAccess::fence() ;

//如果millis>0,传入interruptible＝true,否则为false
  if (interruptible) {
    jlong prevtime = javaTimeNanos();

    for (;;) {
      if (os::is_interrupted(thread, true)) {//判断是否中断
        return OS_INTRPT;
      }

      jlong newtime = javaTimeNanos();//获取当前时间
      //如果linux不支持monotonic lock,有可能出现newtime<prevtime
      if (newtime - prevtime < 0) {
        assert(!Linux::supports_monotonic_clock(), "time moving backwards");
      } else {
        millis -= (newtime - prevtime) / NANOSECS_PER_MILLISEC;
      }

      if(millis <= 0) {
        return OS_OK;
      }

      prevtime = newtime;
      {
        assert(thread->is_Java_thread(), "sanity check");
        JavaThread *jt = (JavaThread *) thread;
        ThreadBlockInVM tbivm(jt);
        OSThreadWaitState osts(jt->osthread(), false );

        jt->set_suspend_equivalent();
        slp->park(millis);
        jt->check_and_wait_while_suspended();
      }
    }
  } else {//如果interruptible=false
   //设置osthread的状态为CONDVAR_WAIT
    OSThreadWaitState osts(thread->osthread(), false );
    jlong prevtime = javaTimeNanos();

    for (;;) {
      jlong newtime = javaTimeNanos();

      if (newtime - prevtime < 0) {
        assert(!Linux::supports_monotonic_clock(), "time moving backwards");
      } else {
        millis -= (newtime - prevtime) / NANOSECS_PER_MILLISEC;
      }

      if(millis <= 0) break ;

      prevtime = newtime;
      slp->park(millis);//底层调用pthread_cond_timedwait实现
    }
    return OS_OK ;
  }
}

```

通过阅读源码知道，原来 sleep 是通过 pthread_cond_timedwait 实现的，那么为什么不通过 linux 的 sleep 实现呢？

- pthread_cond_timedwait 既可以堵塞在某个条件变量上，也可以设置超时时间；
- sleep 不能及时唤醒线程, 最小精度为秒；

可以看出 pthread_cond_timedwait 使用灵活，而且时间精度更高；

## 例子
通过 strace 可以查看代码的系统调用情况，建立两个类，一个调用 Thread.sleep(), 一个调用 Thread.yield(), 查看其系统调用情况:

### Thread.sleep(0)

```cpp
Thread.sleep(0);
System.out.println("hello");
```

![](http://upload-images.jianshu.io/upload_images/2592685-2a84bc0961dafbbe.png) sleep0.png

可以看到 sched_yield 的系统调用

### Thread.sleep(nonzero)

```cpp
Thread.sleep(1000);
System.out.println("hello");
```

![](http://upload-images.jianshu.io/upload_images/2592685-0ffa149115b2ab35.png) sleep2.png

在其中并没有看到 pthread_cond_timedwait 的调用，其实 Java 的线程有可两种实现方式：

1.  LinuxThreads
2.  NPTL(Native POSIX Thread Library)

```cpp
// NPTL or LinuxThreads?
  static bool is_LinuxThreads()               { return !_is_NPTL; }
  static bool is_NPTL()                       { return _is_NPTL;  }
```

可以通过如下命令查看到底是使用哪种线程实现:

```cpp
getconf GNU_LIBPTHREAD_VERSION
```

![](http://upload-images.jianshu.io/upload_images/2592685-5dbef871d6a7adb0.png) nptl.png

关于两者之间的区别，请查看 [wiki](https://link.jianshu.com?t=https://zh.wikipedia.org/wiki/Native_POSIX_Thread_Library)。由于我的机器上采用的是 2，因此无法看到 ppthread_cond_timedwait 的调用；
ppthread_cond_timedwait 采用 futex（Fast Userspace muTEXes）实现，因而可以看到对 futex 的调用；

关于 JVM 是如何决定采用哪种实现方式，可以查看如下方法 (os_linux.cpp):

```cpp
// detecting pthread library

void os::Linux::libpthread_init() {
  // Save glibc and pthread version strings. Note that _CS_GNU_LIBC_VERSION
  // and _CS_GNU_LIBPTHREAD_VERSION are supported in glibc >= 2.3.2\. Use a
  // generic name for earlier versions.
  // Define macros here so we can build HotSpot on old systems.
# ifndef _CS_GNU_LIBC_VERSION
# define _CS_GNU_LIBC_VERSION 2
# endif
# ifndef _CS_GNU_LIBPTHREAD_VERSION
# define _CS_GNU_LIBPTHREAD_VERSION 3
# endif

  size_t n = confstr(_CS_GNU_LIBC_VERSION, NULL, 0);
  if (n > 0) {
     char *str = (char *)malloc(n, mtInternal);
     confstr(_CS_GNU_LIBC_VERSION, str, n);
     os::Linux::set_glibc_version(str);
  } else {
     // _CS_GNU_LIBC_VERSION is not supported, try gnu_get_libc_version()
     static char _gnu_libc_version[32];
     jio_snprintf(_gnu_libc_version, sizeof(_gnu_libc_version),
              "glibc %s %s", gnu_get_libc_version(), gnu_get_libc_release());
     os::Linux::set_glibc_version(_gnu_libc_version);
  }
  //系统函数confstr获取C库信息
  n = confstr(_CS_GNU_LIBPTHREAD_VERSION, NULL, 0);
  if (n > 0) {
     char *str = (char *)malloc(n, mtInternal);
     confstr(_CS_GNU_LIBPTHREAD_VERSION, str, n);
     // Vanilla RH-9 (glibc 2.3.2) has a bug that confstr() always tells
     // us "NPTL-0.29" even we are running with LinuxThreads. Check if this
     // is the case. LinuxThreads has a hard limit on max number of threads.
     // So sysconf(_SC_THREAD_THREADS_MAX) will return a positive value.
     // On the other hand, NPTL does not have such a limit, sysconf()
     // will return -1 and errno is not changed. Check if it is really NPTL.
     if (strcmp(os::Linux::glibc_version(), "glibc 2.3.2") == 0 &&
         strstr(str, "NPTL") &&
         sysconf(_SC_THREAD_THREADS_MAX) > 0) {
       free(str);
       os::Linux::set_libpthread_version("linuxthreads");
     } else {
       os::Linux::set_libpthread_version(str);
     }
  } else {
    // glibc before 2.3.2 only has LinuxThreads.
    os::Linux::set_libpthread_version("linuxthreads");
  }

  if (strstr(libpthread_version(), "NPTL")) {
     os::Linux::set_is_NPTL();
  } else {
     os::Linux::set_is_LinuxThreads();
  }

  // LinuxThreads have two flavors: floating-stack mode, which allows variable
  // stack size; and fixed-stack mode. NPTL is always floating-stack.
  if (os::Linux::is_NPTL() || os::Linux::supports_variable_stack_size()) {
     os::Linux::set_is_floating_stack();
  }
}

```

### Thread.yield

```cpp
Thread.yield();
System.out.println("hello");
```

![](http://upload-images.jianshu.io/upload_images/2592685-613fc525333ad775.png) Paste_Image.png

和 Thread.sleep(0) 相同；

# 参考资料

1.  [Linux 线程模型的比较：LinuxThreads 和 NPTL](https://link.jianshu.com?t=http://www.ibm.com/developerworks/cn/linux/l-threading.html)
