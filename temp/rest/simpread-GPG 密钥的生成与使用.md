> 本文由 [简悦 SimpRead](http://ksria.com/simpread/) 转码， 原文地址 https://www.jianshu.com/p/7f19ceacf57c

先来看 PGP 和 GPG 程序的介绍。

PGP
---

PGP（英语：Pretty Good Privacy，中文含义 “良好隐私密码法”）是一套用于消息加密、验证的应用程序

Phil Zimmermann 于 1991 年将 PGP 在互联网上免费发布。**PGP 本身是商业应用程序**；对应的开源软件为 GPG（GnuPG）。如今 PGP 软件属于 Symantec (赛门铁克公司) 公司。  

![](http://upload-images.jianshu.io/upload_images/7667789-0f89c046ae47ee22.png) image

> [PGP.cn](https://link.jianshu.com?t=http%3A%2F%2Fwww.pgp.cn%2F) PGP 中國：创建一个在中国可以让人信任的公钥发布 / 查询 / 下载网站，我们承诺，公钥发布 / 查询 / 下载的功能将永远免费。

GPG
---

[GNU Privacy Guard](https://link.jianshu.com?t=https%3A%2F%2Fgnupg.org%2F) （简称 GnuPG 或 GPG）是一种加密软件，它是 PGP 加密软件的开源替代程序。GnuPG 是一个混合加密软件程序，可以使用多种非专利的算法。  

![](http://upload-images.jianshu.io/upload_images/7667789-8c400477e586ab8f.png)

安装
--

ubuntu 安装 gnupg：

```
$ sudo apt install gnupg


```

默认配置文件：

> 默认的配置文件是 `~/.gnupg/gpg.conf` 和 `~/.gnupg/dirmngr.conf`.

创建密钥对
-----

```
$ gpg --gen-key 


```

操作示例：

```
[fan 18:58:33]~$ gpg --gen-key 
gpg (GnuPG) 1.4.20; Copyright (C) 2015 Free Software Foundation, Inc.
This is free software: you are free to change and redistribute it.
There is NO WARRANTY, to the extent permitted by law.

请选择您要使用的密钥种类：
   (1) RSA and RSA (default)
   (2) DSA and Elgamal
   (3) DSA (仅用于签名)
   (4) RSA (仅用于签名)
您的选择？ 
RSA 密钥长度应在 1024 位与 4096 位之间。
您想要用多大的密钥尺寸？(2048) 4096
您所要求的密钥尺寸是 4096 位
请设定这把密钥的有效期限。
         0 = 密钥永不过期
      <n>  = 密钥在 n 天后过期
      <n>w = 密钥在 n 周后过期
      <n>m = 密钥在 n 月后过期
      <n>y = 密钥在 n 年后过期
密钥的有效期限是？(0) 
密钥永远不会过期
以上正确吗？(y/n) y

您需要一个用户标识来辨识您的密钥；本软件会用真实姓名、注释和电子邮件地址组合
成用户标识，如下所示：
    “Heinrich Heine (Der Dichter) <heinrichh@duesseldorf.de>”

真实姓名： Fan
电子邮件地址： fan@qq.com
注释： github
您选定了这个用户标识：
    “Fan(github) <fan@qq.com>”

更改姓名(N)、注释(C)、电子邮件地址(E)或确定(O)/退出(Q)？ O
您需要一个密码来保护您的私钥。

我们需要生成大量的随机字节。这个时候您可以多做些琐事(像是敲打键盘、移动
鼠标、读写硬盘之类的)，这会让随机数字发生器有更好的机会获得足够的熵数。
....+++++

随机字节不够多。请再做一些其他的琐事，以使操作系统能搜集到更多的熵！
(还需要184字节)
+++++
我们需要生成大量的随机字节。这个时候您可以多做些琐事(像是敲打键盘、移动
鼠标、读写硬盘之类的)，这会让随机数字发生器有更好的机会获得足够的熵数。

随机字节不够多。请再做一些其他的琐事，以使操作系统能搜集到更多的熵！
(还需要231字节)
...+++++

随机字节不够多。请再做一些其他的琐事，以使操作系统能搜集到更多的熵！
(还需要170字节)
+++++
gpg: 密钥 2DBA87CF 被标记为绝对信任
公钥和私钥已经生成并经签名。

gpg: 正在检查信任度数据库
gpg: 需要 3 份勉强信任和 1 份完全信任，PGP 信任模型
gpg: 深度：0 有效性：  1 已签名：  0 信任度：0-，0q，0n，0m，0f，1u
pub   4096R/2DBA87CF 2017-04-11
      密钥指纹 = 3C00 AC7B 3D06 E22E AEDE  72B0 B28F ACA4 2EBC 87DF
uid                  Fan (github) <fan@qq.com>
sub   4096R/873278A9 2017-04-11

gpg key已经生成
----------------------------------------------
# 后续操作
# 列出密钥
[fan 19:24:44]~$ gpg --list-secret-keys --keyid-format LONG
# 最好制作一张撤销证书，用于密钥作废，请求外部公钥服务器撤销你的公钥
# 将二进制的公钥(私钥)导出为ASSCII码
# 上传公钥到公钥服务器。这里使用阮一峰的命令有一些问题，但可以正常工作
gpg --send-keys [用户ID]
# 生成用于公布的公钥指纹（用于他人校验）
gpg --fingerpint [用户ID]
密钥指纹 = 3C00 AC7B 3D06 E22E AEDE  72B0 B28F ACA4 2EBC 87DF


```

> "用户 ID" 的 Hash 字符串，可以用来替代 "用户 ID"，比如这里`gpg: 密钥 2EBC87DF 被标记为绝对信任`的 2EBC87DF 就是我的用户 ID。  
> 上面是阮一峰的博客中得到的结论；用户 ID 应该就是 uid ，比如上面输出的 `uid Fan (github) <fan@qq.com>`这才是指用户 ID

> **而这里的 2DBA87CF 这应该是 密钥 ID， KEY_ID，或者更确切的说，这里它是 主 key id、MASTERKEYID。**  
> 另外还有 SUBKEYID。

> `用户邮箱` `MASTERKEYID` 在一些场合可以相互替换，但不是全部场合。被这个坑了。

*   用户名和电子邮件。可以给同样的密钥不同的身份，比如给同一个密钥关联多个电子邮件。
*   任何导入密钥的人都可以看到这里的用户名和电子邮件地址。

常用命令
----

### 查看公钥：

```
$ gpg --list-keys

$ gpg -k 


```

```
[fan 15:50:59]~/.gnupg$ gpg -k
/home/fan/.gnupg/pubring.gpg    # 公钥文件
----------------------------
pub   4096R/2DBA87CF 2017-04-11   # PUBlic key特征： 4096位，key id 和生成时间 
uid                  Fan (for github) <fan@qq.com>  # 用户ID
sub   4096R/873278A9 2017-04-11  # public SUBkey特征


```

**注意**： 输出中可以看到是 `.gnupg/pubring.gpg` 文件中的内容。

### 查看私钥：

```
$ gpg --list-secret-keys

$ gpg -K


```

```
[fan 15:35:25]~/.gnupg$ gpg -K
/home/fan/.gnupg/secring.gpg      # 私钥文件
----------------------------
sec   4096R/2DBA87CF 2017-04-11
uid                  Fan (for github) <fan@qq.com>
ssb   4096R/873278A9 2017-04-11


```

**注意**： 输出中可以看到是 `.gnupg/secring.gpg` 文件中的内容。

一些缩写介绍：

```
sec => 'SECret key'
ssb => 'Secret SuBkey'
pub => 'PUBlic key'
sub => 'public SUBkey'


```

下面的命令中的`[用户ID]`都直接替换成 邮箱地址吧。

### 生成和使用撤销证书

生成撤销证书

```
# 二进制证书 revocation-gmail.cert； 但会提示 “已强行使用 ASCII 封装过的输出”
$ gpg --output revocation-gmail.cert --gen-revoke MASTERKEYID

# -a (--armor)输出文件为 revocation-gmail-cert.txt 文本文件
$ gpg -a -o revocation-gmail-cert.txt --gen-revoke MASTERKEYID


```

### 导出密钥并备份

可使用 -a 代替 --armor，使用 -o 代替 --output。

导出公钥：

```
gpg --armor --output public-key-gmail.txt --export MASTERKEYID


```

导出私钥：

```
gpg --armor --output secret-key-gmail.txt --export-secret-keys MASTERKEYID


```

### 编辑命令

```
gpg --edit-key MASTERKEYID


```

当运行编辑命令时，出现的几个标记的介绍：

```
Constant           Character      Explanation
─────────────────────────────────────────────────────
PUBKEY_USAGE_SIG      S       key is good for signing
PUBKEY_USAGE_CERT     C       key is good for certifying other signatures
PUBKEY_USAGE_ENC      E       key is good for encryption
PUBKEY_USAGE_AUTH     A       key is good for authentication


```

可以看到最重要的主密钥显示：SC（“sign”&“certify”，代表可以签名和认证其它密钥）

第一个副密钥显示：E（“encrypt”，加密）

第二个副密钥显示：S（“sign”，签名

> 对于 Debian 来说，签名密钥就足够了。

将另一个电子邮件与您的 GPG 密钥相关联
---------------------

[GPG: Change email for key in PGP key servers (Example)](https://link.jianshu.com?t=https%3A%2F%2Fcoderwall.com%2Fp%2Ftx_1-g%2Fgpg-change-email-for-key-in-pgp-key-servers)

在您的 GPG 密钥中使用经过验证的电子邮件地址。如果您需要更新或添加电子邮件地址到您的 GPG 密钥，请参阅：  
[Associating an email with your GPG key - GitHub Enterprise 2.10 Documentation](https://link.jianshu.com?t=https%3A%2F%2Fhelp.github.com%2Fenterprise%2F2.10%2Fuser%2Farticles%2Fassociating-an-email-with-your-gpg-key%2F)

[Github GPG + Keybase PGP - Ahmad Nassri](https://link.jianshu.com?t=https%3A%2F%2Fwww.ahmadnassri.com%2Fblog%2Fgithub-gpg-keybase-pgp%2F)

将另一个电子邮件与您的 GPG 密钥相关联，示例：

```
  $ gpg --edit-key <key-id>
  gpg> adduid
  Real Name: <name>
  Email address: <email>
  Comment: <comment or Return to none>
  Change (N)ame, (C)omment, (E)mail or (O)kay/(Q)uit? O
  Enter passphrase: <password>
  gpg> uid <uid>
  gpg> trust
  Your decision? 5
  Do you really want to set this key to ultimate trust? (y/N) y
  gpg> save
$ gpg --send-keys <key-id>


```

gpg key 用途
----------

常见的两种用途： 加密、签名

> PGP 的一些作用：（利用公鑰進行加密和簽名）

*   對 Email 進行加密
*   加密一些文本信息，比如（註冊信息），使用 PGP 加密這些信息后可將加密后的密文發佈到自己的 Blog 上，什麼時候需要就取回密文進行解密。
*   簽名功能：可以利用 PGP 的簽名功能對你在網絡上的言論進行簽名，防止他人篡改你的原文（具體原理略）。

> 对文字内容及文件进行加密、解密、签名的方法見該網站。  
> OpenPGP  
> [Symantec Encryption (PGP) Documentation](https://link.jianshu.com?t=https%3A%2F%2Fsupport.symantec.com%2Fen_US%2Farticle.TECH202483.html) 客户端 / 服务器架构。

### 1. 加密

比如加密你的密码文件。

解密

### 2. 签名

验证签名

在 GitHub 和 GitLab 中的使用
----------------------

我的具体操作就是按照 [Authenticating to GitHub](https://link.jianshu.com?t=https%3A%2F%2Fhelp.github.com%2Fcategories%2Fauthenticating-to-github%2F) 所说的方法进行设置。

其中有一步， 是要告诉 Git 你的 GPG key id 是多少，步骤如下：

```
# 使用此命令列出我的key id，顾名思义LONG 这种形式的id 比一般的id要长
$ gpg --list-secret-keys --keyid-format LONG
# 我的是 B28FACA42EBC87DF
$ git config --global user.signingkey B28FACA42EBC87DF


```

而后面的两个命令，列出的 2EBC87DF 也都是 key id，只是比上面的短：

```
$ gpg -K fan@qq.com
sec   4096R/2EBC87DF 2017-04-11
...
# 或使用命令
$ gpg --list-keys
sec   4096R/2EBC87DF 2017-04-11


```

> 对提交签名和对 Tag 进行签名

GitHub 中可以使用 gpg key 进行 Signing commits using GPG 和 Signing Tags using GPG 这两项操作。

**注意：在 GitHub 中使用的 gpg key 的邮件地址必须是经过 GitHub 认证过的邮箱地址**。  
Your GPG key must be associated with a GitHub verified email that matches your committer identity.

> Tips 小贴士:
> 
> To set all commits for a repository to be signed by default, in Git versions 2.0.0 and above, run `git config commit.gpgsign true`. To set all commits in any local repository on your computer to be signed by default, run `git config --global commit.gpgsign true`.
> 
> To store your GPG key passphrase so you don't have to enter it every time you sign a commit, we recommend using the following tools:
> 
> For Mac users, the GPG Suite allows you to store your GPG key passphrase in the Mac OS Keychain.  
> For Windows users, the Gpg4win integrates with other Windows tools.  
> You can also manually configure gpg-agent to save your GPG key passphrase, but this doesn't integrate with Mac OS Keychain like ssh-agent and requires more setup.

### 设置 Signed commits

对单次提交进行签名： `git commit -S -m "-S选项表示对此次提交使用gpg进行签名"`

[Signed commits · Wiki · akwizgran / briar · GitLab](https://link.jianshu.com?t=https%3A%2F%2Fcode.briarproject.org%2Fakwizgran%2Fbriar%2Fwikis%2Fsigned-commits)

好吧，原来上面的小贴士中也有相关方法。

Steps for enabling GPG signed commits in **Android Studio**:

*   Find the ID of your signing key
    *   Run`gpg -K you@example.com`
    *   Look at the line starting with `sec`。（找到以 sec 开始的行）
    *   The hex digits after the slash are the key ID(斜杠后面的十六进制数字是 key ID), e.g. `ABCD0123`
*   Add the key ID to your global `.gitconfig`:
    
    ```
    [user]
          name = you
          email = you@example.com
          signingkey = ABCD0123
    
    
    ```
    
*   Add the following lines to your `.gnupg/gpg.conf`:
    
    ```
    use-agent
    no-tty
    
    
    ```
    
*   Enable signed commits in the project's `.git/config`: （对应于某个工程）
    
    ```
    [commit]
        gpgsign = true
    
    
    ```
    
    和这个命令作用一样： `git config commit.gpgsign true`

也可尝试在 git 的全局配置文件中进行配置，以用于所有提交。也可以直接使用命令  
`git config --global commit.gpgsign true`

> GPG 的配置文件 `.gnupg/gpg.conf`

gpg-agent
---------

[GnuPG (简体中文) - ArchWiki](https://link.jianshu.com?t=https%3A%2F%2Fwiki.archlinux.org%2Findex.php%2FGnuPG_%28%25E7%25AE%2580%25E4%25BD%2593%25E4%25B8%25AD%25E6%2596%2587%29) 推荐

需要备份的文件:

**gpg-agent.conf** 和 **trustlist.txt**（This is the list of trusted keys. You should backup this file.）

配置起来比较麻烦，这里只介绍，个人电脑个人用户的简单配置。

[Using the GNU Privacy Guard: Invoking GPG-AGENT](https://link.jianshu.com?t=https%3A%2F%2Fwww.gnupg.org%2Fdocumentation%2Fmanuals%2Fgnupg%2FInvoking-GPG_002dAGENT.html)，在此网站在下面的 “Option Index” 列表中列出了一些信息。

手动停止： `gpgconf --kill gpg-agent`

您应该总是将以下行添加到. bashrc：

```
export GPG_TTY=$(tty)


```

### 1. 配置

在配置文件`~/.gnupg/gpg-agent.conf`中添加：

```
# 3600 = 60秒 × 60分钟 设置缓存的有效时间，默认为600秒。每次访问都重新开始计时，前提是没有超出最大缓存时间，该时间通过 max-cache-ttl设置默认为2小时
default-cache-ttl 3600


```

[Using the GNU Privacy Guard: Agent Options](https://link.jianshu.com?t=https%3A%2F%2Fwww.gnupg.org%2Fdocumentation%2Fmanuals%2Fgnupg%2FAgent-Options.html%23index-default_002dcache_002dttl) 在这里查看配置文件中各配置的含义

> `ignore-cache-for-signing`  
> This option will let gpg-agent bypass the passphrase cache for all signing operation. Note that there is also a per-session option to control this behavior but this command line option takes precedence.

一个示例：

```
# 5小时  24小时
default-cache-ttl 18000
max-cache-ttl 86400
ignore-cache-for-signing


```

### 2. 重新加载 agent

更改配置后需要重新加载 agent。

```
$ gpg-connect-agent reloadagent /bye


```

该命令将会打印出 OK

### 3.pinentry

最后 agent 需要知道如何向用户索要密码，默认是使用一个 gtk dialog (gtk 对话框)。

在`~/.gnupg/gpg-agent.conf`配置文件中，可以通过`pinentry-program`配置你要采用的程序：

```
# PIN entry program
# pinentry-program /usr/bin/pinentry-curses
# pinentry-program /usr/bin/pinentry-qt
# pinentry-program /usr/bin/pinentry-kwallet

pinentry-program /usr/bin/pinentry-gtk-2


```

**仍然重新加载 agent**

### 4.Start gpg-agent with systemd user（可选）

这里使用的是 archlinux

Create a systemd unit file:

```
# 文件 ~/.config/systemd/user/gpg-agent.service 中
[Unit]
Description=GnuPG private key agent
IgnoreOnIsolate=true

[Service]
Type=forking
ExecStart=/usr/bin/gpg-agent --daemon
Restart=on-abort

[Install]
WantedBy=default.target


```

### 5. 无人值守的密码短语（可选）

为了具有与旧版本相同类型的功能，必须完成两件事情：

1.  edit the gpg-agent configuration to allow _loopback_ pinentry mode:
    
    ```
    # ~/.gnupg/gpg-agent.conf 
    allow-loopback-pinentry
    
    
    ```
    
    然后重启 gpg-agent，以生效。
    
2.  需要更新应用程序，最好使用命令行加参数的形式，来使用环回模式，如下：
    

```
$ gpg  --pinentry-mode loopback


```

如果这样不行，则尝试在配置文件中添加相应配置项：

```
# ~/.gnupg/gpg.conf
pinentry-mode loopback


```

> `gpg --pinentry-mode loopback`命令不能执行，没有这个选项。后面的没有做了。配置了前面的已经可以了。

### 6. SSH agent （可选）

如果你已经安装了 GnuPG 套件，你可能考虑使用 gpg-agent 去缓存你的 SSH key。一些用户可能更喜欢 GnuPG 代理提供的 PIN 输入对话框，作为其密码短语管理的一部分。

略

My PGP PUBLIC KEY
-----------------

```
Fingerprint=683D ABB1 ABD1 6E7B 04A4  1284 6C98 8F2F 8B35 D6D7


```

学习资料
----

推荐的文章:

*   [GPG 入门教程](https://link.jianshu.com?t=http%3A%2F%2Fwww.ruanyifeng.com%2Fblog%2F2013%2F07%2Fgpg.html)
*   [Signing commits with GPG](https://link.jianshu.com?t=https%3A%2F%2Fhelp.github.com%2Farticles%2Fsigning-commits-with-gpg%2F)
*   [Pro Git:7.4 Git 工具 - 签署工作](https://link.jianshu.com?t=https%3A%2F%2Fgit-scm.com%2Fbook%2Fzh%2Fv2%2FGit-%25E5%25B7%25A5%25E5%2585%25B7-%25E7%25AD%25BE%25E7%25BD%25B2%25E5%25B7%25A5%25E4%25BD%259C)
*   [GnuPG (简体中文) - ArchWiki](https://link.jianshu.com?t=https%3A%2F%2Fwiki.archlinux.org%2Findex.php%2FGnuPG_%28%25E7%25AE%2580%25E4%25BD%2593%25E4%25B8%25AD%25E6%2596%2587%29) 推荐
*   [GnuPG 快速使用指南 - MWB 日常笔记](https://link.jianshu.com?t=https%3A%2F%2Fwww.mawenbao.com%2Fnote%2Fgnupg.html) 推荐。
*   [二翔子的博客: 如何创建完美的 GPG 密钥对](https://link.jianshu.com?t=https%3A%2F%2F2xiangzi.blogspot.com%2F2016%2F09%2Fperfect-gpg-keypair.html) 文章中导出副密钥的语句有误。
*   [Creating a new GPG key with subkeys](https://link.jianshu.com?t=https%3A%2F%2Fwww.void.gr%2Fkargig%2Fblog%2F2013%2F12%2F02%2Fcreating-a-new-gpg-key-with-subkeys%2F) 推荐
*   [Creating the perfect GPG keypair - Alex Cabal](https://link.jianshu.com?t=https%3A%2F%2Falexcabal.com%2Fcreating-the-perfect-gpg-keypair%2F) 能够了解一些补充的概念。
*   [Subkeys - Debian Wiki](https://link.jianshu.com?t=https%3A%2F%2Fwiki.debian.org%2FSubkeys) 正解在这里。
*   [使用 PGP 保护代码完整性（三）：生成 PGP 子密钥](https://link.jianshu.com?t=https%3A%2F%2Flinux.cn%2Farticle-9607-1.html)

另参考：

*   [使用 GnuPG 实现电子邮件加密和数字签名——PGP 30 分钟简明教程 (1)](https://link.jianshu.com?t=http%3A%2F%2Farchboy.org%2F2013%2F04%2F18%2Fgnupg-pgp-encrypt-decrypt-message-and-email-and-digital-signing-easy-tutorial%2F)
    
*   [Gnu 隐私卫士 (GnuPG) 袖珍 HOWTO (中文版)](https://link.jianshu.com?t=https%3A%2F%2Fwww.gnupg.org%2Fhowtos%2Fzh%2Findex.html)
    
*   [RSA 算法原理（一）](https://link.jianshu.com?t=http%3A%2F%2Fwww.ruanyifeng.com%2Fblog%2F2013%2F06%2Frsa_algorithm_part_one.html)