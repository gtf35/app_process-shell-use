# 免Root实现静默安装和点击任意位置

## 0  前言

最近有了个需求：免 root 实现任意位置点击和静默安装。这个做过的小伙伴应该都知道正常情况下是不可能实现的。无障碍只能实现对已知控件的点击，并不能指定坐标。但是确实有人另辟蹊径做出来了，譬如做游戏手柄的飞智，他们是用一个激活器，手机开 usb 调试，然后插在激活器上并授权，飞智游戏厅就被「激活」了，然后可以实现任意位置点击。如果不了解的可以去他们官网了解下，在这里不多赘述了。无独有偶，黑域也使用了类似的手段，也可以用电脑的usb调试激活。我们知道，任意位置坐标xy点击是可以在 pc 上通过 shell 命令「input tap x  y」来实现的，也不需要 root 权限。但是在应用内通过「Runtime.getRuntime().exec」执行这个 shell 命令却提示「permission denied」也就是权限不足。但是飞智或者黑域却好像使用了某种魔法，提升了自己的权限，那么问题来了：如何用 usb 调试给 app 提权？

## 1 原理揭晓

如何用 usb 调试给 app 提权」这个问题乍一看确实没问题，但是知乎有个回答是「先问是不是，再问为什么」我觉得说的很好。我被这个问题给困扰了很久，最后发现我问错了。先放出结论「并不是给 app 提权，而是运行了一个由设立了权限的新程序」

刚才的问题先放一边，我来问大家个新问题，怎样让 app 获取 root 权限？这个问题答案已经有不少了，网上一查便可知其实是获取「Runtime.getRuntime().exec」的流，在里面用su提权，然后就可以执行需要 root 权限的 shell 命令，比如挂载 system 读写，访问 data 分区，用 shell 命令静默安装，等等。话说回来，是不是和我们今天的主题有点像，如何使 app 获取 shell 权限？嗯，其实差不多，思路也类似，因为本来 root 啦， shell 啦，根本就不是 Android 应用层的名词呀，他们本来就是 Linux 里的名词，只不过是 Android 框架运行于 Linux 层之上， 我们可以调用 shell 命令，也可以在shell 里调用 su 来使shell 获取 root 权限，来绕过 Android 层做一些被限制的事。然而在 app 里调用 shell 命令，其进程还是 app 的，权限还是受限。所以就不能在 app 里运行 shell 命令，那么问题来了，不在 app 里运行在哪运行？答案是在 pc 上运行。当然不可能是 pc 一直连着手机啦，而是 pc 上在 shell 里运行独立的一个 java 程序，这个程序因为是在 shell 里启动的，所以具有 shell 权限。我们想一下，这个 Java 程序在 shell 里运行，建立本地 socket 服务器，和 app 通信，远程执行 app 下发的代码。因为即使拔掉了数据线，这个 Java 程序也不会停止，只要不重启他就一直活着，执行我们的命令，这不就是看起来 app 有了 shell 权限？现在真相大白，飞智和黑域用 usb 调试激活的那一下，其实是启动那个 Java 程序，飞智是执行模拟按键，黑域是监听系统事件，你想干啥就任你开发了。「注：黑域和飞智由于进程管理的需要，其实是先用 shell 启动一个 so ，然后再用 so 做跳板启动 Java 程序，而且 so 也充当守护进程，当 Java 意外停止可以重新启动，读着有兴趣可以自行研究，在此不多做说明」

## 2 好耶！是 app_process

那么如何具体用 shell 运行 Java 程序呢？肯定不是「java xxx.jar」啦，Android 能运行的格式是 dex 。没错，就是apk 里那个 dex 。然后我们可以通过「app_process」开启动 Java 。app_process 的参数如下

```shell
app_process [vm-options] cmd-dir [options] start-class-name [main-options]
```

这个诡异又可怕的东西是没有 -help 的。我们要么看源码，要么看别人分析好的。本人水平有限，这里选择看别人分析好的：

```shell
vm-options – VM 选项
cmd-dir –父目录 (/system/bin)
options –运行的参数 :
    –zygote
    –start-system-server
    –application (api>=14)
    –nice-name=nice_proc_name (api>=14)
start-class-name –包含main方法的主类  (com.android.commands.am.Am)
main-options –启动时候传递到main方法中的参数
```

## 3 实践

因为是 dex 我们就直接在 as 里写吧，提取 dex 也方便。新建个空白项目，初始结构是这样：

![](http://article.gtf35.top/app_process/as%E9%BB%98%E8%AE%A4%E6%A6%82%E8%A7%88.JPG)

我们新建个包，存放我们要在 shell 下运行的 Java 代码：

![](http://article.gtf35.top/app_process/%E7%AC%AC%E4%B8%80%E6%AC%A1%E6%B5%8B%E8%AF%95.JPG)

这里我们补全 Main 方法，因为我们这个不是个 Android 程序，只是编译成 dex 的纯 Java 程序，所以我们这个的入口是 Main :

```java
package shellService;

public class Main {
    public static void main(String[] args){
        System.out.println("我是在 shell 里运行的！！！");
    }
}
```

我们在代码里只是打印一行「我是在 shell 里运行的！！！」，因为这里是纯 Java 所以也用的 println。现在编译 apk：

![](http://article.gtf35.top/app_process/%E7%BC%96%E8%AF%91%E5%87%BA%E6%9D%A5%E7%9A%84apk.JPG)

因为 apk 就是 zip 所以我们直接解压出 apk 文件，然后执行 ：

```shell
adb push classes.dex /data/local/tmp
cd /data/local/tmp
app_process -Djava.class.path=/data/local/tmp/classes.dex /system/bin shellService.Main
```

这时就能看到已经成功运行啦：

![](http://article.gtf35.top/app_process/%E8%BF%90%E8%A1%8C%E7%BB%93%E6%9E%9C.JPG)

这里因为 utf8 在 Windows shell 里有问题，所以乱码了，但是还是说明我们成功了。

##4 具有实用性

只能输出肯定是不行的，不具有实用性。我们之前说过，我们应该建立个本地 socket 服务器来接受命令并执行，这里的「Service」类实现了这个功能，因为如何建立 socket 不是文章的重点，所以大家只要知道这个类内部实现了一个「ServiceGetText」接口，在收到命令之后会把命令内容作为参数回掉 getText 方法，然后我们执行 shell 命令之后，吧结果作为字符串返回即可，具体实现可以看查看源码[Service](https://github.com/gtf35/app_process-shell-use/blob/master/app/src/main/java/shellService/Service.java)。

我们新建一个「[ServiceThread](https://github.com/gtf35/app_process-shell-use/blob/master/app/src/main/java/shellService/ServiceThread.java)」来运行「Service」服务和执行设立了命令：

```java
public class ServiceThread extends Thread {
    private static int ShellPORT = 4521;

    @Override
    public void run() {
        System.out.println(">>>>>>Shell服务端程序被调用<<<<<<");
        new Service(new Service.ServiceGetText() {
            @Override
            public String getText(String text) {
                if (text.startsWith("###AreYouOK")){
                    return "###IamOK#";
                }
                try{
                    ServiceShellUtils.ServiceShellCommandResult sr =  ServiceShellUtils.execCommand(text, false);
                    if (sr.result == 0){
                        return "###ShellOK#" + sr.successMsg;
                    } else {
                        return "###ShellError#" + sr.errorMsg;
                    }
                }catch (Exception e){
                    return "###CodeError#" + e.toString();
                }
            }
        }, ShellPORT);
    }
}
```

其中 ServiceShellUtils 用到了开源项目 ShellUtils 在此感谢。这个类用来执行 shell 命令。

然后在 Main 中调用这个线程：

```java
public class Main {

    public static void main(String[] args){
        new ServiceThread().start();
        while (true);
    }

}
```

这样，我们服务端就准备好了，我们来写控制服务端的 app 。我们新建类「SocketClient」用来和服务端进行通信，并在活动里调用他（完整代码请参看[SocketClient](https://github.com/gtf35/app_process-shell-use/blob/master/app/src/main/java/top/gtf35/shellapplicatontest/SocketClient.java)和[MainActivity](https://github.com/gtf35/app_process-shell-use/blob/master/app/src/main/java/top/gtf35/shellapplicatontest/MainActivity.java)）：

```java
private void runShell(final String cmd){
        if (TextUtils.isEmpty(cmd)) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
              new SocketClient(cmd, new SocketClient.onServiceSend() {
                  @Override
                  public void getSend(String result) {
                      showTextOnTextView(result);
                  }
              });
            }
        }).start();
    }
```

然后重复 3 小节的操作，运行这个服务端:

![](http://article.gtf35.top/app_process/%E6%9C%80%E5%90%8Edemo%E7%9A%84shell.JPG)

然后安装 apk ，运行：

```java
input text HelloWord
```

![](http://article.gtf35.top/app_process/%E6%89%8B%E6%9C%BA%E8%BF%90%E8%A1%8C.gif)

可以看到，在不 root 的情况下，成功的执行了需要 shell 权限的命令

##5 最可爱的人

最后，我真的是要由衷的感谢各种技术分析文章和开源项目，真的太感谢了，没有无条件的奉献就没有互联网这么快的进步。

我对 app_process 利用方法的研究离不开以下项目和前辈的汗水:

[Brevent](https://github.com/brevent/Brevent) 最早利用app_process进程实现无 root 权限使用的开源应用（虽然已经闭源，仍然尊重并感谢 [liudongmiao](https://github.com/liudongmiao)）

[Android system log viewer on Android phone without root.](https://github.com/Zane96/Fairy) 利用app_process进程实现无 root 权限使用的优秀开源应用

[Android上app_process启动java进程](https://blog.csdn.net/u010651541/article/details/53163542) 通俗易懂的教程

[使用 app_process 来调用高权限 API](https://haruue.moe/blog/2017/08/30/call-privileged-api-with-app-process/) 分析的很深刻的教程

本文的项目可以在[GitHub上获取](https://github.com/gtf35/app_process-shell-use)：https://github.com/gtf35/app_process-shell-use