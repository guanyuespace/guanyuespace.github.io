---
titile: 定时器
date: 2019-04-29 17:05:32
---
<!-- TOC -->

- [定时器](#%E5%AE%9A%E6%97%B6%E5%99%A8)
  - [创建一个定时器](#%E5%88%9B%E5%BB%BA%E4%B8%80%E4%B8%AA%E5%AE%9A%E6%97%B6%E5%99%A8)
  - [启动一个定时器](#%E5%90%AF%E5%8A%A8%E4%B8%80%E4%B8%AA%E5%AE%9A%E6%97%B6%E5%99%A8)
  - [获得一个活动定时器的剩余时间](#%E8%8E%B7%E5%BE%97%E4%B8%80%E4%B8%AA%E6%B4%BB%E5%8A%A8%E5%AE%9A%E6%97%B6%E5%99%A8%E7%9A%84%E5%89%A9%E4%BD%99%E6%97%B6%E9%97%B4)
  - [**取得一个定时器的超限运行次数**](#%E5%8F%96%E5%BE%97%E4%B8%80%E4%B8%AA%E5%AE%9A%E6%97%B6%E5%99%A8%E7%9A%84%E8%B6%85%E9%99%90%E8%BF%90%E8%A1%8C%E6%AC%A1%E6%95%B0)
  - [删除一个定时器](#%E5%88%A0%E9%99%A4%E4%B8%80%E4%B8%AA%E5%AE%9A%E6%97%B6%E5%99%A8)
- [add_ons](#add_ons)
  - [signal 函数](#signal-%E5%87%BD%E6%95%B0)
  - [信号列表](#%E4%BF%A1%E5%8F%B7%E5%88%97%E8%A1%A8)

<!-- /TOC -->
# 定时器:POSIX 时钟系列
>  [原文地址](http://www.360doc.com/content/14/0227/14/12424571_356129884.shtml)

最强大的定时器接口来自 POSIX 时钟系列，其创建、初始化以及删除一个定时器的行动被分为三个不同的函数：timer_create()(创建定时器)、timer_settime()(初始化定时器) 以及 timer_delete(销毁它)。

## 创建一个定时器
```c
int timer_create(clockid_t clock_id, struct sigevent *evp, timer_t *timerid)
```

进程可以通过调用 timer_create() 创建特定的定时器，定时器是每个进程自己的，不是在 fork 时继承的。clock_id 说明定时器是基于哪个时钟的，\*timerid 装载的是被创建的定时器的 ID。该函数创建了定时器，并将他的 ID 放入 timerid 指向的位置中。        

参数 evp 指定了定时器到期要产生的异步通知。如果 evp 为 NULL，那么定时器到期会产生默认的信号，对 CLOCK_REALTIMER 来说，默认信号就是 SIGALRM。  
如果要产生除默认信号之外的其它信号，程序必须将 evp->sigev_signo 设置为期望的信号码。


struct sigevent 结构中的成员 evp->sigev_notify 说明了定时器到期时应该采取的行动。通常，这个成员的值为 SIGEV_SIGNAL, 这个值说明在定时器到期时，会产生一个信号。程序可以将成员 evp->sigev_notify 设为 SIGEV_NONE 来防止定时器到期时产生信号。    


**如果几个定时器产生了同一个信号，处理程序可以用 evp->sigev_value 来区分是哪个定时器产生了信号。要实现这种功能，程序必须在为信号安装处理程序时，使用 *struct sigaction 的成员 sa_flags* 中的标志符 SA_SIGINFO。**

**clock_id 取值为以下：**   
CLOCK_REALTIME :Systemwide realtime clock.  
CLOCK_MONOTONIC:Represents monotonic time. Cannot be set.  
CLOCK_PROCESS_CPUTIME_ID :High resolution per-process timer.  
CLOCK_THREAD_CPUTIME_ID :Thread-specific timer.   
CLOCK_REALTIME_HR :High resolution version of CLOCK_REALTIME.   
CLOCK_MONOTONIC_HR :High resolution version of CLOCK_MONOTONIC.   

**struct sigevent 结构:**   
```c
struct sigevent
{
  int sigev_notify; //notification type
  int sigev_signo; //signal number
  union sigval   sigev_value; //signal value
  void (*sigev_notify_function)(union sigval);
  pthread_attr_t *sigev_notify_attributes;
}

union sigval
{
  int sival_int; //integer value
  void *sival_ptr; //pointer value
}
```
通过将 evp->sigev_notify 设定为如下值来定制定时器到期后的行为：    
SIGEV_NONE：什么都不做，只提供通过 timer_gettime 和 timer_getoverrun 查询超时信息。    
SIGEV_SIGNAL: 当定时器到期，内核会将 sigev_signo 所指定的信号传送给进程。在信号处理程序中，si_value 会被设定会 sigev_value。    
**SIGEV_THREAD: **  ***当定时器到期，内核会 (在此进程内) 以 sigev_notification_attributes 为线程属性创建一个线程，并且让它执行 sigev_notify_function，传入 sigev_value 作为为一个参数。***

## 启动一个定时器   
timer_create() 所创建的定时器并未启动。要将它关联到一个到期时间以及启动时钟周期，可以使用 timer_settime()。

```c
int timer_settime(timer_t timerid, int flags, const struct itimerspec *value, struct itimerspect *ovalue);

struct itimespec{
  struct timespec it_interval;
  struct timespec it_value; 
}; 
```

如同 settimer()，`it_value` 用于指定当前的定时器到期时间。当定时器到期，`it_value` 的值会被更新成 `it_interval` 的值。**如果 it_interval 的值为 0，则定时器不是一个时间间隔定时器，一旦 it_value 到期就会回到未启动状态。??????未启动状态 means????**

timespec 的结构提供了纳秒级分辨率：
```c
struct timespec{
  time_t tv_sec;
  long tv_nsec;
}
```
如果 flags 的值为 TIMER_ABSTIME，则 value 所指定的时间值会被解读成绝对值 (此值的默认的解读方式为相对于当前的时间)。这个经修改的行为可避免取得当前时间、计算“该时间” 与“所期望的未来时间”的相对差额以及启动定时器期间造成竞争条件。

**如果 ovalue 的值不是 NULL，则之前的定时器到期时间会被存入其所提供的 itimerspec。如果定时器之前处在未启动状态，则此结构的成员全都会被设定成 0。**

## 获得一个活动定时器的剩余时间
```c
int timer_gettime(timer_t timerid,struct itimerspec *value);
```

## **取得一个定时器的超限运行次数**
有可能一个定时器到期了，而同一定时器上一次到期时产生的信号还处于挂起状态。在这种情况下，其中的一个信号可能会丢失。这就是定时器超限。程序可以通过调用 timer_getoverrun 来确定一个特定的定时器出现这种超限的次数。定时器超限只能发生在同一个定时器产生的信号上。由多个定时器，甚至是那些使用相同的时钟和信号的定时器，所产生的信号都会排队而不会丢失。

```c
int timer_getoverrun(timer_t timerid);
```
执行成功时，timer_getoverrun()会返回定时器初次到期与通知进程 (例如通过信号) 定时器已到期之间额外发生的定时器到期次数。举例来说，在我们之前的例子中，一个 1ms 的定时器运行了 10ms，则此调用会返回 9。如果超限运行的次数等于或大于 DELAYTIMER_MAX，则此调用会返回 DELAYTIMER_MAX。

执行失败时，此函数会返回 - 1 并将 errno 设定会 EINVAL，这个唯一的错误情况代表 timerid 指定了无效的定时器。

## 删除一个定时器
```c
int timer_delete (timer_t timerid);
```

一次成功的 timer_delete() 调用会销毁关联到 timerid 的定时器并且返回 0。执行失败时，此调用会返回 - 1 并将 errno 设定会 EINVAL，这个唯一的错误情况代表 timerid 不是一个有效的定时器。

例 1：
```c
void  handle()
{
 time_t t;
 char p[32];

 time(&t);
 strftime(p, sizeof(p), "%T", localtime(&t));
 printf("time is %s/n", p);
}

int main()
{
 struct sigevent evp;
 struct itimerspec ts;
 timer_t timer;
 int ret;

 evp.sigev_value.sival_ptr = &timer;
 evp.sigev_notify = SIGEV_SIGNAL;//到期时触发SIGUSER1信号
 evp.sigev_signo = SIGUSR1;
 signal(SIGUSR1, handle);////////////SIGUSER1信号触发handle处理函数...

 ret = timer_create(CLOCK_REALTIME, &evp, &timer);
 if(ret)
  perror("timer_create");

 ts.it_interval.tv_sec = 1;
 ts.it_interval.tv_nsec = 0;
 ts.it_value.tv_sec = 3;
 ts.it_value.tv_nsec = 0;

 ret = timer_settime(timer, 0, &ts, NULL);
 if(ret)
  perror("timer_settime");

 sleep(10);
}
```
例 2：
```c
void  handle(union sigval v)
{
 time_t t;
 char p[32];

 time(&t);
 strftime(p, sizeof(p), "%T", localtime(&t));

 printf("%s thread %lu, val = %d, signal captured./n", p, pthread_self(), v.sival_int);
 return;
}

int main()
{
 struct sigevent evp;
 struct itimerspec ts;
 timer_t timer;
 int ret;

 memset   (&evp,   0,   sizeof   (evp));
 evp.sigev_value.sival_ptr = &timer;
 evp.sigev_notify = SIGEV_THREAD;///ok
 evp.sigev_notify_function = handle;
 evp.sigev_value.sival_int = 3;   // 作为 handle() 的参数

 ret = timer_create(CLOCK_REALTIME, &evp, &timer);
 if(ret)
  perror("timer_create");

 ts.it_interval.tv_sec = 1;
 ts.it_interval.tv_nsec = 0;
 ts.it_value.tv_sec = 3;
 ts.it_value.tv_nsec = 0;

 ret = timer_settime(timer, TIMER_ABSTIME, &ts, NULL);
 if(ret)
  perror("timer_settime");
 sleep(100);
}
```
---

# add_ons
## signal 函数

```c
typedef void (*sighandler_t)(int);//处理函数原型
sighandler_t signal(int signum, sighandler_t handler);//ok
signal函数
    作用1：站在应用程序的角度，注册一个信号处理函数
    作用2：忽略信号，设置信号默认处理 信号的安装和回复
参数
--signal是一个带signum和handler两个参数的函数，准备捕捉或屏蔽的信号由参数signum给出，接收到指定信号时将要调用的函数有handler给出
--handler这个函数必须有一个int类型的参数（即接收到的信号代码），它本身的类型是void
--handler也可以是下面两个特殊值：① SIG_IGN 屏蔽该信号        ② SIG_DFL 恢复默认行为
```

## Test
```c
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <errno.h>
#include <sys/types.h>
#include <signal.h>

//#define TEST1 1
//#define TEST2 1
#define TEST3 1

typedef void(* signal_handler)(int);

pid_t doFork(){
  pid_t pid=fork();
  if(pid==-1)
  {
      printf("fork() failed! error message:%s\n",strerror(errno));
      return -1;
  }
  return pid;
}

void test(pid_t pid){
  printf("pid: %d\t",pid);
  if(pid>0)
  {
      printf("father is runing !\n");
      sleep(10);
  }
  if(pid==0)
  {
      printf("i am child!\n");
      exit(0);
  }
  printf("game over!\n");

}

void myFunc(int param){
  printf("%s:%d\n","it is my func",param);//the param value is the value of signal
}

int main(int arg, char *args[])
{

    pid_t pid;
#ifdef TEST1
    pid=doFork();
    //进程 Terminate 或 Stop 的时候，SIGCHLD 会发送给它的父进程。缺省情况下该 Signal 会被忽略
    //注册信号，屏蔽SIGCHLD信号，子进程退出，将不会给父进程发送信号，因此也不会出现僵尸进程
    signal(SIGCHLD,SIG_IGN);//SIG_IGN:忽略创建子进程信号
    test(pid);
#endif

#ifdef TEST2
    signal(SIGCHLD,SIG_DFL);//恢复默认处理
    pid=doFork();
    test(pid);
#endif

#ifdef TEST3
    signal_handler m_handler=myFunc;
    signal(SIGCHLD,m_handler);//自定义信号处理函数
    pid=doFork();
    test(pid);
#endif
  return 0;
}
```
ok,

SIGCHLD: 进程 Terminate 或 Stop 的时候，SIGCHLD 会发送给它的父进程。**缺省情况下该 Signal 会被忽略,*坑坑坑坑坑***

```
118876 father is runing !
0 i am child!
game over!


119097 father is runing !
0 i am child!
game over!


///在执行TEST3时，ok... ...
119103 father is runing !
0 i am child!
it is my func:17
game over!
```

## 信号列表
> it can be listed when command: "kill -l"

```
SIGABRT        进程停止运行    6
SIGALRM        警告钟    
SIGFPE        算述运算例外
SIGHUP        系统挂断
SIGILL        非法指令
SIGINT        终端中断   2
SIGKILL        停止进程（此信号不能被忽略或捕获）
SIGPIPE        向没有读的管道写入数据
SIGSEGV        无效内存段访问
SIGQOUT        终端退出    3
SIGTERM        终止
SIGUSR1        用户定义信号1
SIGUSR2        用户定义信号2
SIGCHLD        子进程已经停止或退出
SIGCONT        如果被停止则继续执行
SIGSTOP        停止执行
SIGTSTP        终端停止信号
SIGTOUT        后台进程请求进行写操作
SIGTTIN        后台进程请求进行读操作
```


**一些常用的 Signal 如下：**   

| **Signal** | **Description** |
|---|---|
| SIGABRT | 由调用 abort 函数产生，进程非正常退出 |
| SIGALRM | 用 alarm 函数设置的 timer 超时或 setitimer 函数设置的 interval timer 超时 |
| SIGBUS | 某种特定的硬件异常，通常由内存访问引起 |
| SIGCANCEL | 由 Solaris Thread Library 内部使用，通常不会使用 |
| SIGCHLD | 进程 Terminate 或 Stop 的时候，SIGCHLD 会发送给它的父进程。缺省情况下该 Signal 会被忽略 |
| SIGCONT | 当被 stop 的进程恢复运行的时候，自动发送 |
| SIGEMT | 和实现相关的硬件异常 |
| SIGFPE | 数学相关的异常，如被 0 除，浮点溢出，等等 |
| SIGFREEZE | Solaris 专用，Hiberate 或者 Suspended 时候发送 |
| SIGHUP | 发送给具有 Terminal 的 Controlling Process，当 terminal 被 disconnect 时候发送 |
| SIGILL | 非法指令异常 |
| SIGINFO | BSD signal。由 Status Key 产生，通常是 CTRL+T。发送给所有 Foreground Group 的进程 |
| SIGINT | 由 Interrupt Key 产生，通常是 CTRL+C 或者 DELETE。发送给所有 ForeGround Group 的进程 |
| SIGIO | 异步 IO 事件 |
| SIGIOT | 实现相关的硬件异常，一般对应 SIGABRT |
| SIGKILL | 无法处理和忽略。中止某个进程 |
| SIGLWP | 由 Solaris Thread Libray 内部使用 |
| SIGPIPE | 在 reader 中止之后写 Pipe 的时候发送 |
| SIGPOLL | 当某个事件发送给 Pollable Device 的时候发送 |
| SIGPROF | Setitimer 指定的 Profiling Interval Timer 所产生 |
| SIGPWR | 和系统相关。和 UPS 相关。 |
| SIGQUIT | 输入 Quit Key 的时候（CTRL+\）发送给所有 Foreground Group 的进程 |
| SIGSEGV | 非法内存访问 |
| SIGSTKFLT | Linux 专用，数学协处理器的栈异常 |
| SIGSTOP | 中止进程。无法处理和忽略。 |
| SIGSYS | 非法系统调用 |
| SIGTERM | 请求中止进程，kill 命令缺省发送 |
| SIGTHAW | Solaris 专用，从 Suspend 恢复时候发送 |
| SIGTRAP | 实现相关的硬件异常。一般是调试异常 |
| SIGTSTP | Suspend Key，一般是 Ctrl+Z。发送给所有 Foreground Group 的进程 |
| SIGTTIN | 当 Background Group 的进程尝试读取 Terminal 的时候发送 |
| SIGTTOU | 当 Background Group 的进程尝试写 Terminal 的时候发送 |
| SIGURG | 当 out-of-band data 接收的时候可能发送 |
| SIGUSR1 | 用户自定义 signal 1 |
| SIGUSR2 | 用户自定义 signal 2 |
| SIGVTALRM | setitimer 函数设置的 Virtual Interval Timer 超时的时候 |
| SIGWAITING | Solaris Thread Library 内部实现专用 |
| SIGWINCH | 当 Terminal 的窗口大小改变的时候，发送给 Foreground Group 的所有进程 |
| SIGXCPU | 当 CPU 时间限制超时的时候 |
| SIGXFSZ | 进程超过文件大小限制 |
| SIGXRES | Solaris 专用，进程超过资源限制的时候发送 |
