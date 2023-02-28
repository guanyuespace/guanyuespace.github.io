> 本文由 [简悦 SimpRead](http://ksria.com/simpread/) 转码， 原文地址 https://www.jianshu.com/p/4ec914c7e013

## 属性键值对
> 属性系统是 android 的一个重要特性。每个属性是一个键值对（key/value pair），其类型都是字符串。但给它的缺点也很明显，只能传输简单的参数。
> **可以通过命令 adb shell ：**
> getprop 查看 android 设备上所有属性状态值。或者使用 setprop 设置某个属性的状态。


### 特别属性
- 如果属性名称以 “ro.” 开头，那么这个属性被视为只读属性。一旦设置，属性值不能改变。
- 如果属性名称以 “persist.” 开头，当设置这个属性时，其值也将写入 / data/property。
- 如果属性名称以 “net.” 开头，当设置这个属性时，“net.change”属性将会自动设置，以加入到最后修改的属性名。
<!-- （这是很巧妙的。 netresolve 模块的使用这个属性来追踪在 net.* 属性上的任何变化。） -->
- 属性 **“ctrl.start”** 和 **“ctrl.stop”** 是用来启动和停止服务。

每一项服务必须在 `init.rc` 中定义. 系统启动时，与 `init` 守护进程将解析 `init.rc` 和启动属性服务。一旦收到设置 “ctrl.start” 属性的请求，属性服务将使用该属性值作为服务名找到该服务，启动该服务。这项服务的启动结果将会放入“ init.svc.<服务名“属性中。客户端应用程序可以轮询那个属性值，以确定结果。<!--开机动画就是这么实现的。-->

## framework 访问系统属性

framework 通过 `SystemProperties` 接口操作系统属性，所以在 `java` 层大家都可以使用该类来获取或者设置系统属性。`SystemProperties` 通过 `JNI` 调用访问系统属性。

代码路径 frameworks\base\core\java\android\os\ SystemProperties.java：

```java
public class SystemProperties {
//JNI
  private static native String native_get(String key, String def);
  private static native void native_set(String key, String def);

  public static String get(String key, String def) {
   return native_get(key, def); 
  }

  public static void set(String key, String val) {
   native_set(key, val); 
  } 
}
```

**Jni代码位置：**
\frameworks\base\core\jni\android_os_SystemProperties.cpp

### 读取系统属性

实现是在 **\bionic\libc\bionic\system_properties.c 中：**
```c
int __system_property_get(const char* name,char* value) {
  // 数据已经存储在内存中__system_property_area__ 等待读取完返回
  const prop_info* pi = __system_property_find(name);
  return __system_property_read(pi,0, value); 
}
```
进程启动后数据已经将系统属性数据读取到相应的共享内存中，保存在全局变量`__system_property_area__`;
<!-- 进程之间都是独立的，系统属性数据是如何读取到当前进程空间中的呢？后续介绍。 -->


### 设置系统属性
>设置属性异步 `socket` 通信

```c
int __system_property_set(const char* key,const char* value) {
    msg.cmd = PROP_MSG_SETPROP; 
    strlcpy(msg.name, key,sizeofmsg.name); 
    strlcpy(msg.value, value,sizeofmsg.value); 
    err = send_prop_msg(&msg);///
}
static int send_prop_msg(prop_msg *msg) {
    //sokcet 通信 --->  对方:/dev/socket/property_service
    s = socket(AF_LOCAL, SOCK_STREAM,0); 
    connect(s, (structsockaddr *) &addr, alen) 
    send(s, msg,sizeof(prop_msg),0)
    close(s);
}
```
**通过 `socket` 向 `property_service` 发送消息**
<!-- property_service 运行在哪里呢？ -->

# 通信对方property_service

## Property Service 创建服务端 socket

Property Service 在 init 进程中，init进程启动监听过程中：\system\core\init\Init.c

![init property code](http://upload-images.jianshu.io/upload_images/7626001-215e3623aef62f73.png "init property code")

**Property Service 是运行在 init 守护进程中。** 先看看 Property Service 接收到消息后的处理。

## Property Service 监听 socket 处理

![Property Service 监听 socket 消息的处理过程](http://upload-images.jianshu.io/upload_images/7626001-235fa7c1d5501c50.png "Property Service 监听 socket 消息的处理过程")

通过设置系统属性启动/关闭Service：

![权限判断](http://upload-images.jianshu.io/upload_images/7626001-f47acff2fd47c45d.png "权限判断")

所以如果想要应用有权限启动/关闭某 Native Service：
需要具有 system/root 权限, 找到对应应用 uid gid，将应用名称加入到 control_perms 列表中

处理消息可以通过设置系统属性 改变服务的执行状态 start/stop：

![ctrl属性相关控制](http://upload-images.jianshu.io/upload_images/7626001-1553c94fd766263b.png "ctrl属性相关控制")

连着前面就是 ctr.start 和 ctr.stop 系统属性：用来启动和停止服务的。

例如：
```c
// start boot animation
property_set("ctl.start", "bootanim");
```
在 init.rc 中表明服务是否在开机时启动：
```
service adbd /sbin/adbd
classcore
disabled// 不自动启动
```

启动服务的时候会判断:
![](http://upload-images.jianshu.io/upload_images/7626001-a1a600200a7c6cdb.png)

修改系统属性值：
![](http://upload-images.jianshu.io/upload_images/7626001-e34a3ac4eebe8072.png)

看这个修改系统属性权限表，配置系统权限可以查看 system/core/include/private/android_filesystem_config.h：

![](http://upload-images.jianshu.io/upload_images/7626001-da447b843a148236.png)

指定了特定的用户有用修改 带有某些前缀的系统属性值。

到这里基本就是 Property 对外的基本工作流程，Property Service 内部具体如何实现，操作运行，

跨进程空想内存等问题仍未清除是如何处理的。

# 属性系统设计

![](http://upload-images.jianshu.io/upload_images/7626001-96621048b3486636.png)

**Property Service** 运行在 init 进程中，开机从属性文件中加载到共享内存中；设置系统属性通过 socket 与 Property Service 通信。

**Property Consumer** 进程将存储系统属性值的共享内存，加载到当前进程虚拟空间中，实现对系统属性值的读取。

**Property Setter** 进程修改系统属性，通过 socket 向 Property Service 发送消息，更改系统属性值。

# 属性系统实现

属性系统设计的关键就是：跨进程共享内存的实现。

**下面将看看属性系统实现具体过程：**

**Init进程执行：**

![](http://upload-images.jianshu.io/upload_images/7626001-ec9dd81d150191e6.png)

**初始化Property Service:\system\core\init\property_service.c**

![](http://upload-images.jianshu.io/upload_images/7626001-573edefc71118d97.png)

**初始化共享内存空间：**

![](http://upload-images.jianshu.io/upload_images/7626001-21b4b164340bd06f.png)

**__system_property_area__**：

每个进程都会使用此变量，指向系统属性共享内存区域，访问系统属性，很重要。

位于：\bionic\libc\bionic\system_properties.c 中，属于 bionic 库。后面将介绍各进程如何加载共享内存。

**将文件作为共享内存映射到进程空间内存使用：**

![](http://upload-images.jianshu.io/upload_images/7626001-e1821b476c131a4d.png)

**加载系统属性默认数据文件：**

![](http://upload-images.jianshu.io/upload_images/7626001-049786fb12f42313.png)

加上上面所述：Property Service Socket 资源的创建，来监听 socket 通信连接设置系统属性，

在 Init 进程中 Property Service 完成了初始化。

**将得到该内存区域数据结构：**

![](http://upload-images.jianshu.io/upload_images/7626001-7a18b3c2ac26a919.png)

**进程共享系统属性内存空间实现**

Property Service 运行于 init 进程中，将文件映射为创建一块共享内存空间，但在整个系统中，

其他进程也能够读取这块内存映射到当前进程空间中，是如何实现的呢？

**Service 进程启动：**将共享内存空间 fd size 作为环境变量传递给新创建进程

![](http://upload-images.jianshu.io/upload_images/7626001-3b002abf74226a53.png)

共享内存空间 fd size 作为环境变量传递给新创建进程后，将在何处使用呢？

**将系统属性内存空间映射到当前进程虚拟空间：进程在启动时，会加载动态库 bionic libc库：**
\bionic\libc\bionic\libc_init_dynamic.c 中：
```c
void __attribute__((constructor)) __libc_preinit(void);
```
**根据GCC的constructor/destructor属性：**

给一个函数赋予 constructor 或 destructor，其中 constructor 在 main 开始运行之前被调用，

destructor 在 main 函数结束后被调用。如果有多个 constructor 或 destructor，可以给每个 constructor

或 destructor 赋予优先级，对于 constructor，优先级数值越小，运行越早。destructor 则相反。

**多个constructor需要加优先级：**

![](http://upload-images.jianshu.io/upload_images/7626001-57917818bcbb34fb.png)

**__libc_preinit在bionic libc库加载的时候会被调用：**

![](http://upload-images.jianshu.io/upload_images/7626001-93abf1fb06e7d739.png)   
