> 本文由 [简悦 SimpRead](http://ksria.com/simpread/) 转码， 原文地址 https://blog.csdn.net/u010852160/article/details/78491084

HTTP 2.0 是在 SPDY（An experimental protocol for a faster web, The Chromium Projects）基础上形成的下一代互联网通信协议。HTTP/2 的目的是通过支持请求与响应的多路复用来较少延迟，通过压缩 HTTPS 首部字段将协议开销降低，同时增加请求优先级和服务器端推送的支持。 
本文目的是学习 HTTP 2.0 的原理并研究其通信的详细细节。大部分知识点源于《Web 性能权威指南》。

# 1. 二进制分帧层

二进制分帧层，是 HTTP 2.0 性能增强的核心。 
HTTP 1.x 在应用层以纯文本的形式进行通信，而 HTTP 2.0 将所有的传输信息分割为更小的消息和帧，并对它们采用二进制格式编码。这样，客户端和服务端都需要引入新的二进制编码和解码的机制。 
如下图所示，HTTP 2.0 并没有改变 HTTP 1.x 的语义，只是在应用层使用二进制分帧方式传输。 
![](https://img-blog.csdn.net/20170405172818292) 
因此，也引入了新的通信单位：

## 1.1 帧（frame）

HTTP 2.0 通信的最小单位，包括帧首部、流标识符、优先值和帧净荷等。 
![](https://img-blog.csdn.net/20170405153816267)

其中，帧类型又可以分为：

*   DATA：用于传输 HTTP 消息体；
*   HEADERS：用于传输首部字段；
*   SETTINGS：用于约定客户端和服务端的配置数据。比如设置初识的双向流量控制窗口大小；
*   WINDOW_UPDATE：用于调整个别流或个别连接的流量
*   PRIORITY： 用于指定或重新指定引用资源的优先级。
*   RST_STREAM： 用于通知流的非正常终止。
*   PUSH_ PROMISE： 服务端推送许可。
*   PING： 用于计算往返时间，执行 “活性” 检活。
*   GOAWAY： 用于通知对端停止在当前连接中创建流。

标志位用于不同的帧类型定义特定的消息标志。比如 DATA 帧就可以使用`End Stream: true`表示该条消息通信完毕。流标识位表示帧所属的流 ID。优先值用于 HEADERS 帧，表示请求优先级。R 表示保留位。 
下面是 Wireshark 抓包的一个 DATA 帧： 
![](https://img-blog.csdn.net/20170405170457247)

## 1.2 消息（message）

消息是指逻辑上的 HTTP 消息（请求 / 响应）。一系列数据帧组成了一个完整的消息。比如一系列 DATA 帧和一个 HEADERS 帧组成了请求消息。

## 1.3 流（stream）

流是连接中的一个虚拟信道，可以承载双向消息传输。每个流有唯一整数标识符。为了防止两端流 ID 冲突，客户端发起的流具有奇数 ID，服务器端发起的流具有偶数 ID。 
所有 HTTP 2. 0 通信都在一个 TCP 连接上完成， 这个连接可以承载任意数量的双向数据流 Stream。 相应地， 每个数据流以 消息的形式发送， 而消息由一 或多个帧组成， 这些帧可以乱序发送， 然后根据每个帧首部的流标识符重新组装。 
![](https://img-blog.csdn.net/20170405172729697) 
二进制分帧层保留了 HTTP 的语义不受影响，包括首部、方法等，在应用层来看，和 HTTP 1.x 没有差别。同时，所有同主机的通信能够在一个 TCP 连接上完成。

# 2. 多路复用共享连接

基于二进制分帧层，HTTP 2.0 可以在共享 TCP 连接的基础上，同时发送请求和响应。HTTP 消息被分解为独立的帧，而不破坏消息本身的语义，交错发送出去，最后在另一端根据流 ID 和首部将它们重新组合起来。 
我们来对比下 HTTP 1.x 和 HTTP 2.0，假设不考虑 1.x 的 pipeline 机制，双方四层都是一个 TCP 连接。客户端向服务度发起三个图片请求 / image1.jpg，/image2.jpg，/image3.jpg。 
HTTP 1.x 发起请求是串行的，image1 返回后才能再发起 image2，image2 返回后才能再发起 image3。 
![](https://img-blog.csdn.net/20170406101003201) 
HTTP 2.0 建立一条 TCP 连接后，并行传输着 3 个数据流，客户端向服务端乱序发送 stream1~3 的一系列的 DATA 帧，与此同时，服务端已经在返回 stream 1 的 DATA 帧 
![](https://img-blog.csdn.net/20170406101019438) 
性能对比，高下立见。HTTP 2.0 成功解决了 HTTP 1.x 的队首阻塞问题（TCP 层的阻塞仍无法解决），同时，也不需要通过 pipeline 机制多条 TCP 连接来实现并行请求与响应。减少了 TCP 连接数对服务器性能也有很大的提升。

# 3. 请求优先级

流可以带有一个 31bit 的优先级：

*   0：表示最高优先级
*   2<sup>31</sup>-1：表示最低优先级

客户端明确指定优先级，服务端可以根据这个优先级作为依据交互数据，比如客户端优先级设置为. css>.js>.jpg（具体可参见《高性能网站建设指南》）， 服务端按优先级返回结果有利于高效利用底层连接，提高用户体验。 
然而，也不能过分迷信请求优先级，仍然要注意以下问题：

*   服务端是否支持请求优先级
*   会否引起队首阻塞问题，比如高优先级的慢响应请求会阻塞其他资源的交互。

# 4. 服务端推送

HTTP 2.0 增加了服务端推送功能，服务端可以根据客户端的请求，提前返回多个响应，推送额外的资源给客户端。如下图所示，客户端请求 stream 1，/page.html。服务端在返回 stream 1 消息的同时推送了 stream 2（/script.js）和 stream 4（/style.css）。 
![](https://img-blog.csdn.net/20170406105233944) 
PUSH_PROMISE 帧是服务端向客户端有意推送资源的信号。

*   如果客户端不需要服务端 Push，可在 SETTINGS 帧中设定服务端流的值为 0，禁用此功能
*   PUSH_PROMISE 帧中只包含预推送资源的首部。如果客户端对 PUSH_PROMISE 帧没有意见，服务端在 PUSH_PROMISE 帧后发送响应的 DATA 帧开始推送资源。如果客户端已经缓存该资源，不需要再推送，可以选择拒绝 PUSH_PROMISE 帧。
*   PUSH_PROMISE 必须遵循请求 - 响应原则，只能借着对请求的响应推送资源。 
    目前，Apache 的 [`mod_http2`](http://httpd.apache.org/docs/2.4/mod/mod_http2.html)能够开启 `H2Push on`服务端推送 Push。Nginx 的`ngx_http_v2_module`还不支持服务端 Push。

<pre>Apache mod_headers example
<Location /index.html>
    Header add Link "</css/site.css>;rel=preload"
    Header add Link "</images/logo.jpg>;rel=preload"
</Location>
Apache mod_headers example
<Location /index.html>
    Header add Link "</css/site.css>;rel=preload"
    Header add Link "</images/logo.jpg>;rel=preload"
</Location>
</pre>


# 5. 首部压缩

HTTP 1.x 每一次通信（请求 / 响应）都会携带首部信息用于描述资源属性。HTTP 2.0 在客户端和服务端之间使用 “首部表” 来跟踪和存储之前发送的键 - 值对。首部表在连接过程中始终存在，新增的键 - 值对会更新到表尾，因此，不需要每次通信都需要再携带首部。 
![](https://img-blog.csdn.net/20170406151823031) 
另外，HTTP 2.0 使用了首部压缩技术，压缩算法使用 HPACK。可让报头更紧凑，更快速传输，有利于移动网络环境。 
需要注意的是，HTTP 2.0 关注的是首部压缩，而我们常用的 gzip 等是报文内容（body）的压缩。二者不仅不冲突，且能够一起达到更好的压缩效果。

# 6. 一个完整的 HTTP 2.0 通信过程

考虑一个问题，客户端如何知道服务端是否支持 HTTP 2.0？是否支持对二进制分帧层的编码和解码？所以，在两端使用 HTTP 2.0 通信之前，必然存在协议协商的过程。

## 6.1 基于 ALPN 的协商过程

支持 HTTP 2.0 的浏览器可以在 TLS 会话层自发完成和服务端的协议协商以确定是否使用 HTTP 2.0 通信。其原理是 TLS 1.2 中引入了扩展字段，以允许协议的扩展，其中 ALPN 协议（Application Layer Protocol Negotiation, 应用层协议协商, 前身是 NPN）用于客户端和服务端的协议协商过程。 
服务端使用 ALPN，监听 443 端口默认提高 HTTP 1.1，并允许协商其他协议，比如 SPDY 和 HTTP 2.0。 
比如，客户端在 TLS 握手 Client Hello 阶段表明自身支持 HTTP 2.0 
![](https://img-blog.csdn.net/20170406154122927) 
服务端收到后，响应 Server Hello，表示自己也支持 HTTP 2.0。双方开始 HTTP 2.0 通信。 
![](https://img-blog.csdn.net/20170406154450854)

## 6.2 基于 HTTP 的协商过程

然而，HTTP 2.0 一定是 HTTPS（TLS 1.2）的特权吗？ 
当然不是，客户端使用 HTTP 也可以开启 HTTP 2.0 通信。只不过因为 HTTP 1. 0 和 HTTP 2. 0 都使用同一个 端口（80）， 又没有服务器是否支持 HTTP 2. 0 的其他任何 信息，此时 客户端只能使用 HTTP Upgrade 机制（OkHttp, [nghttp2](https://nghttp2.org/documentation/nghttp.1.html) 等组件均可实现，也可以自己编码完成）通过协调确定适当的协议：

<pre>HTTP Upgrade request
GET / HTTP/1.1
host: nghttp2.org
connection: Upgrade, HTTP2-Settings
upgrade: h2c        /*发起带有HTTP2.0 Upgrade头部的请求*/
http2-settings: AAMAAABkAAQAAP__   /*客户端SETTINGS净荷*/
user-agent: nghttp2/1.9.0-DEV
HTTP Upgrade response
HTTP/1.1 101 Switching Protocols   /*服务端同意升级到HTTP 2.0*/
Connection: Upgrade
Upgrade: h2c
HTTP Upgrade success               /*协商完成*/
HTTP Upgrade request
GET / HTTP/1.1
host: nghttp2.org
connection: Upgrade, HTTP2-Settings
upgrade: h2c        /*发起带有HTTP2.0 Upgrade头部的请求*/
http2-settings: AAMAAABkAAQAAP__   /*客户端SETTINGS净荷*/
user-agent: nghttp2/1.9.0-DEV

HTTP Upgrade response
HTTP/1.1 101 Switching Protocols   /*服务端同意升级到HTTP 2.0*/
Connection: Upgrade
Upgrade: h2c

HTTP Upgrade success               /*协商完成*/
</pre>


## 6.3 完整通信过程

TCP 连接建立： 
![](https://img-blog.csdn.net/20170406160432663) 
TLS 握手和 HTTP 2.0 通信过程： 
![](https://img-blog.csdn.net/20170406160245818) 
另外，在 chrome 中通过`chrome://net-internals/#http2`命令也能捕获 HTTP 2.0 通信过程：

<pre>42072: HTTP2_SESSION
textlink.simba.taobao.com:443 (PROXY 10.19.110.55:8080)
Start Time: 2017-04-05 11:39:11.459
t=370225 [st=    0] +HTTP2_SESSION  [dt=32475+]
                     --> host = "textlink.simba.taobao.com:443"
                     --> proxy = "PROXY 10.19.110.55:8080"
t=370225 [st=    0]    HTTP2_SESSION_INITIALIZED
                       --> protocol = "h2"
                       --> source_dependency = 42027 (PROXY_CLIENT_SOCKET_WRAPPER)
t=370225 [st=    0]    HTTP2_SESSION_SEND_SETTINGS
                       --> settings = ["[id:3 flags:0 value:1000]","[id:4 flags:0 value:6291456]","[id:1 flags:0 value:65536]"]
t=370225 [st=    0]    HTTP2_STREAM_UPDATE_RECV_WINDOW
                       --> delta = 15663105
                       --> window_size = 15728640
t=370225 [st=    0]    HTTP2_SESSION_SENT_WINDOW_UPDATE_FRAME
                       --> delta = 15663105
                       --> stream_id = 0
t=370225 [st=    0]    HTTP2_SESSION_SEND_HEADERS
                       --> exclusive = true
                       --> fin = true
                       --> has_priority = true
                       --> :method: GET
                           :authority: textlink.simba.taobao.com
                           :scheme: https
                           :path: /?name=tbhs&cna=IAj9EOy3fngCAXBQ5kJ9yusH&nn=&count=13&pid=430266_1006&_ksTS=1491363551394_94&callback=jsonp95
                           user-agent: Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36
                           accept: */*
                           referer: https://www.taobao.com/
                           accept-encoding: gzip, deflate, sdch, br
                           accept-language: zh-CN,zh;q=0.8
                           cookie: [382 bytes were stripped]
                       --> parent_stream_id = 0
                       --> stream_id = 1
                       --> weight = 147
t=370256 [st=   31]    HTTP2_SESSION_RECV_SETTINGS
                       --> host = "textlink.simba.taobao.com:443"
t=370256 [st=   31]    HTTP2_SESSION_RECV_SETTING
                       --> flags = 0
                       --> id = 3
                       --> value = 128
t=370256 [st=   31]    HTTP2_SESSION_UPDATE_STREAMS_SEND_WINDOW_SIZE
                       --> delta_window_size = 2147418112
t=370256 [st=   31]    HTTP2_SESSION_RECV_SETTING
                       --> flags = 0
                       --> id = 4
                       --> value = 2147483647
t=370256 [st=   31]    HTTP2_SESSION_RECV_SETTING
                       --> flags = 0
                       --> id = 5
                       --> value = 16777215
t=370256 [st=   31]    HTTP2_SESSION_RECEIVED_WINDOW_UPDATE_FRAME
                       --> delta = 2147418112
                       --> stream_id = 0
t=370256 [st=   31]    HTTP2_SESSION_UPDATE_SEND_WINDOW
                       --> delta = 2147418112
                       --> window_size = 2147483647
t=370261 [st=   36]    HTTP2_SESSION_RECV_HEADERS
                       --> fin = false
                       --> :status: 200
                           date: Wed, 05 Apr 2017 03:39:11 GMT
                           content-type: text/html; charset=ISO-8859-1
                           vary: Accept-Encoding
                           server: Tengine
                           expires: Wed, 05 Apr 2017 03:39:11 GMT
                           cache-control: max-age=0
                           strict-transport-security: max-age=0
                           timing-allow-origin: *
                           content-encoding: gzip
                       --> stream_id = 1
t=370261 [st=   36]    HTTP2_SESSION_RECV_DATA
                       --> fin = false
                       --> size = 58
                       --> stream_id = 1
t=370261 [st=   36]    HTTP2_SESSION_UPDATE_RECV_WINDOW
                       --> delta = -58
                       --> window_size = 15728582
t=370261 [st=   36]    HTTP2_SESSION_RECV_DATA
                       --> fin = true
                       --> size = 0
                       --> stream_id = 1
t=370295 [st=   70]    HTTP2_STREAM_UPDATE_RECV_WINDOW
                       --> delta = 58
                       --> window_size = 15728640
t=402700 [st=32475]
42072: HTTP2_SESSION
textlink.simba.taobao.com:443 (PROXY 10.19.110.55:8080)
Start Time: 2017-04-05 11:39:11.459

t=370225 [st=    0] +HTTP2_SESSION  [dt=32475+]
                     --> host = "textlink.simba.taobao.com:443"
                     --> proxy = "PROXY 10.19.110.55:8080"
t=370225 [st=    0]    HTTP2_SESSION_INITIALIZED
                       --> protocol = "h2"
                       --> source_dependency = 42027 (PROXY_CLIENT_SOCKET_WRAPPER)
t=370225 [st=    0]    HTTP2_SESSION_SEND_SETTINGS
                       --> settings = ["[id:3 flags:0 value:1000]","[id:4 flags:0 value:6291456]","[id:1 flags:0 value:65536]"]
t=370225 [st=    0]    HTTP2_STREAM_UPDATE_RECV_WINDOW
                       --> delta = 15663105
                       --> window_size = 15728640
t=370225 [st=    0]    HTTP2_SESSION_SENT_WINDOW_UPDATE_FRAME
                       --> delta = 15663105
                       --> stream_id = 0
t=370225 [st=    0]    HTTP2_SESSION_SEND_HEADERS
                       --> exclusive = true
                       --> fin = true
                       --> has_priority = true
                       --> :method: GET
                           :authority: textlink.simba.taobao.com
                           :scheme: https
                           :path: /?name=tbhs&cna=IAj9EOy3fngCAXBQ5kJ9yusH&nn=&count=13&pid=430266_1006&_ksTS=1491363551394_94&callback=jsonp95
                           user-agent: Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36
                           accept: */*
                           referer: https://www.taobao.com/
                           accept-encoding: gzip, deflate, sdch, br
                           accept-language: zh-CN,zh;q=0.8
                           cookie: [382 bytes were stripped]
                       --> parent_stream_id = 0
                       --> stream_id = 1
                       --> weight = 147
t=370256 [st=   31]    HTTP2_SESSION_RECV_SETTINGS
                       --> host = "textlink.simba.taobao.com:443"
t=370256 [st=   31]    HTTP2_SESSION_RECV_SETTING
                       --> flags = 0
                       --> id = 3
                       --> value = 128
t=370256 [st=   31]    HTTP2_SESSION_UPDATE_STREAMS_SEND_WINDOW_SIZE
                       --> delta_window_size = 2147418112
t=370256 [st=   31]    HTTP2_SESSION_RECV_SETTING
                       --> flags = 0
                       --> id = 4
                       --> value = 2147483647
t=370256 [st=   31]    HTTP2_SESSION_RECV_SETTING
                       --> flags = 0
                       --> id = 5
                       --> value = 16777215
t=370256 [st=   31]    HTTP2_SESSION_RECEIVED_WINDOW_UPDATE_FRAME
                       --> delta = 2147418112
                       --> stream_id = 0
t=370256 [st=   31]    HTTP2_SESSION_UPDATE_SEND_WINDOW
                       --> delta = 2147418112
                       --> window_size = 2147483647
t=370261 [st=   36]    HTTP2_SESSION_RECV_HEADERS
                       --> fin = false
                       --> :status: 200
                           date: Wed, 05 Apr 2017 03:39:11 GMT
                           content-type: text/html; charset=ISO-8859-1
                           vary: Accept-Encoding
                           server: Tengine
                           expires: Wed, 05 Apr 2017 03:39:11 GMT
                           cache-control: max-age=0
                           strict-transport-security: max-age=0
                           timing-allow-origin: *
                           content-encoding: gzip
                       --> stream_id = 1
t=370261 [st=   36]    HTTP2_SESSION_RECV_DATA
                       --> fin = false
                       --> size = 58
                       --> stream_id = 1
t=370261 [st=   36]    HTTP2_SESSION_UPDATE_RECV_WINDOW
                       --> delta = -58
                       --> window_size = 15728582
t=370261 [st=   36]    HTTP2_SESSION_RECV_DATA
                       --> fin = true
                       --> size = 0
                       --> stream_id = 1
t=370295 [st=   70]    HTTP2_STREAM_UPDATE_RECV_WINDOW
                       --> delta = 58
                       --> window_size = 15728640
t=402700 [st=32475]
</pre>


# 7. HTTP 2.0 性能瓶颈

是不是启用 HTTP 2.0 后性能必然提升了？任何事情都不是绝对的，虽然总体而言性能肯定是能提升的。 
我想 HTTP 2.0 会带来新的性能瓶颈。因为现在所有的压力集中在底层一个 TCP 连接之上，TCP 很可能就是下一个性能瓶颈，比如 TCP 分组的队首阻塞问题，单个 TCP packet 丢失导致整个连接阻塞，无法逃避，此时所有消息都会受到影响。未来，服务器端针对 HTTP 2.0 下的 TCP 配置优化至关重要，有机会我们再跟进详述。
