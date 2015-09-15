# AirPlay-Receiver-on-Android
在Android中实现Airplay的接收端

## 目前实现的功能
1、iphone 推送图片到android，支持上一张、下一张  
2、iphone中的优酷播放器通过airplay推送视频到android，支持暂停、播放、seek和在随意时间点推送播放  
3、当android客户端推出播放后，ipone就退出airplay模式。  
4、airplay发现android设备，你可以随便修改自己的名称，例如显示：我的电视 等  
5、视频播放时，android和ipone是同步的，时间一致。 

##注意
本项目只是针对IOS8.4.1之前的版本，最新的IOS9 apple对airplay进行了修改，具体的可以看一下xmbc等开源实现对其的实现情况，或者自己抓包研究最新的airplay协议。   


未实现：  
1、腾讯视频推送airplay与优酷不一样，里面牵扯到了RTSP，不是单纯的HTTP。下一步继续研究。  
2、android客户端先启动，iphone后启动，iphone怎样发现android，由于服务注册发现使用的第三方jmdns。因此还需要进一步研究M_DNS和DNS_SD。

# 项目介绍
目前是移动互联网的时代，小屏幕已经占领了我们生活的大部分时间，然而在家庭内的另一个屏幕就是电视屏幕，如果让两个屏幕连接起来，
几年前已经成为了一个热门的话题（最近似乎不是很热了），但是如何占领家庭内部的市场仍是一个重点和热点，如近期出现的智能路由器。
在媒体共享方面，出现了DLNA和apple的Airplay两个比较好的东西。
### DLNA
DLNA全开放，目前各个部分已经有很好的实现，如Cling和CyberGarage等实现了DLNA的协议库。
### airplay
aiplay是apple的东西，比较封闭，仅仅用于ihone（ipad）与apple自己tv：apple-tv之间进行交互，而且不同的IOS版本可能还会有变化，如果你用apple的官方接口应该没有问题，但是网络上对airplay的抓包和分析，不同的版本可能还不一样，在国内，虽然iphone是在移动端的比例很大，而appletv在国内始终用户很少，国内大部分是android的智能电视，或者普通的电视加上一个androidd盒子。因此iphone和android之间实现airplay就很有必要了。目前国内的盒子几乎全部支持了，如小米盒子、funbox等。这些支持airplay的盒子有没有申请apple的东西，谁也不知道，如果apple要搞一下，估计许多都不能用了，或者不停的针对apple的版本进行破解、升级。

此项目基于几年前我所在的项目组的研究，但是随着ios版本的升级，原来的研究有些不管用了，基于个人爱好，开始了此项目。
实现iphone6 IOS8.4上的图片或者视频推送到我的android手机nubia上，国内的一些app store上的应用，如：优酷等客户端等是支持airplay的。

## 服务注册
airplay的服务发现是与M_DNS 和 DNS_SD协议的，目前开源的java实现为jmdns，百度搜索即可。苹果视频和图片的推送服务名称为._airplay._tcp.local，airplay注册服务的时候需要用到。

### 推送时显示自定义名称的方法
具体的怎样在airplay推送的时候，在你的手机上看到的是你自己定义的名字呢？经过抓包分析，解决方案如下：
音频raop服务和airplay的服务同时注册，并且注册的时候有一定的技巧，具体的代码如下所示：

```JAVA
 private void registerTcpLocal() throws IOException
    {
        airplayService = ServiceInfo.create(airplayName + "._airplay._tcp.local",
            airplayName, RequestListenerThread.port, 0, 0, values);
        jmdnsAirplay = JmDNS.create(localAddress);//create的必须绑定ip地址 android 4.0以上
        jmdnsAirplay.registerService(airplayService);
    }

    private void registerRaopLocal() throws IOException
    {
        String raopName = preMac + "@" + airplayName;
        raopService = ServiceInfo.create(raopName + "._raop._tcp.local", raopName,
            RequestListenerThread.port - 1,
            "tp=UDP sm=false sv=false ek=1 et=0,1 cn=0,1 ch=2 ss=16 "
                + "sr=44100 pw=false vn=3 da=true md=0,1,2 vs=103.14 txtvers=1");
        jmdnsRaop = JmDNS.create(localAddress);
        jmdnsRaop.registerService(raopService);
    }
```

如上面的代码:   
airplayName就是你的自定义的名字，音频raop注册必须是mac@airplayName._raop._tcp.local, airplay注册必须是airplayName.    
假如你的airplayName="我的电视"，则显示在你手机上的就是我的电视。  
![image](https://github.com/gpfduoduo/AirPlay-Receiver-on-Android/blob/master/protocol/show%20self%20define%20name.png)

## 具体的协议分析
  简单的来说需要你的android 实现一个httpserver，然后apple设备（手机，pad）作为client将内容推送到你的server上，然后server（android）设备根据不同的内容进行显示，client（苹果）设备可以对推送的内容进行控制：推送下一张图片、视频的暂停、seek和推送结束等。

### 对于图片  
  首先你会受到一个http get /server-info的请求   
  
  然后收到一个http post /reverse请求  
  
  最后就有收到 http put /photo请求，请求的httpbody中就含有实际的jpeg格式的图片二进制文件信息，在android中你decode就可以直接显示。  
  
  具体的日志如下：  
  
airplay  incoming HTTP  method = GET; target = /server-info;   

airplay  incoming HTTP  method = POST; target = /reverse;   

airplay  incoming HTTP  method = PUT; target = /photo;    
而且每一个图片都对应这个一个 唯一的id：assetKey.
  结束推送的时候：
  airplay  incoming HTTP  method = POST; target = /stop 

  具体的忘了抓包如下所示：     
  收到请求Server基本信息的请求 必须返回相应内容  
  ![iamge](https://github.com/gpfduoduo/AirPlay-Receiver-on-Android/blob/master/protocol/airplay%20photo%20server-info.png)    收到reverse请求  
  ![image](https://github.com/gpfduoduo/AirPlay-Receiver-on-Android/blob/master/protocol/airplay%20photo%20reverse.png)  
  接收到真正的图片数据：http body就是图片的二进制数据  
  ![image](https://github.com/gpfduoduo/AirPlay-Receiver-on-Android/blob/master/protocol/airplay%20photo%20put%20photo.png)  
  收到结束推送的消息     
  ![image](https://github.com/gpfduoduo/AirPlay-Receiver-on-Android/blob/master/protocol/airplay%20photo%20stop.png)   
  收到stop消息之后还要发送：    
  ![image](https://github.com/gpfduoduo/AirPlay-Receiver-on-Android/blob/master/protocol/airplay%20photo%20send%20reverse%20msg.png)  
  
  airplay推送图片的时候，会有一个缓存的操作，即：将缓存图片一并推送过来，这样可以较快的进行下一张图片的显示，提高用户体现。具体的第一次推送put /photo的时候，会推送三种图片，然后当你在apple客户端滑动显示图片的时候，会推送当前显示的一样和下一张的cache。具体的日志如下：
  


推送第一张图片  
08-25 13:32:48.508  14608-15497/com.guo.duoduo.airplayreceiver D/WebServiceHandler﹕ airplay cached image, assetKey = 78A1BB2D-5488-4372-95EA-FF32737B563C 缓存 左边  

08-25 13:32:48.523  14608-15497/com.guo.duoduo.airplayreceiver D/MyLineParse
08-25 13:32:48.568  14608-15497/com.guo.duoduo.airplayreceiver D/WebServiceHandler﹕ airplay cached image, assetKey = F6BC486E-821B-4D74-B257-80AF280C6E5C 缓存 右边  

08-25 13:32:48.725  14608-15497/com.guo.duoduo.airplayreceiver D/MyLineParser﹕
08-25 13:32:48.752  14608-15497/com.guo.duoduo.airplayreceiver D/WebServiceHandler﹕ airplay display image; assetKey = X-Apple-AssetKey: 8B792485-B6B6-4CF4-91D9-A14734E9E790 显示 当前    

右滑动   
08-25 13:35:46.201  14608-15497/com.guo.duoduo.airplayreceiver D/WebServiceHandler﹕ airplay display cached image, assetKey = 78A1BB2D-5488-4372-95EA-FF32737B563C 原来左边变为当前显示  

08-25 13:35:46.345  14608-15497/com.guo.duoduo.airplayreceiver D/WebServiceHandler﹕ airplay cached image, assetKey = B6711879-7539-4980-8213-98FA76FDD11A  缓存左边  

右滑动   
08-25 13:38:01.586  14608-15497/com.guo.duoduo.airplayreceiver D/WebServiceHandler﹕ airplay display cached image, assetKey = B6711879-7539-4980-8213-98FA76FDD11A 上一个的左边变为当前  

08-25 13:38:01.642  14608-15497/com.guo.duoduo.airplayreceiver D/WebServiceHandler﹕ airplay cached image, assetKey = 15FE410D-84D3-4ED8-A741-673CD2DFD0F4 缓存左边  

左滑动  
08-25 13:42:04.883  14608-15497/com.guo.duoduo.airplayreceiver D/WebServiceHandler﹕ airplay display cached image, assetKey = 78A1BB2D-5488-4372-95EA-FF32737B563C  

08-25 13:42:04.980  14608-15497/com.guo.duoduo.airplayreceiver D/WebServiceHandler﹕ airplay cached image, assetKey = 8B792485-B6B6-4CF4-91D9-A14734E9E790   

左滑动  
08-25 13:47:06.468  14608-15497/com.guo.duoduo.airplayreceiver D/WebServiceHandler﹕ airplay display cached image, assetKey = 8B792485-B6B6-4CF4-91D9-A14734E9E790  

08-25 13:47:06.542  14608-15497/com.guo.duoduo.airplayreceiver D/WebServiceHandler﹕ airplay cached image, assetKey = F6BC486E-821B-4D74-B257-80AF280C6E5C  
  
### 推送效果
以下为ipad推送，有时候会没有反应，具体还要查证    
![image](https://github.com/gpfduoduo/AirPlay-Receiver-on-Android/blob/master/push_image.gif "推送图片")

### 视频推送
  视频推送是通过优酷客户端进行的。   
#### 与之前的研究不同的地方
1、推送视频的时候，目前IOS8.4.1不发送 "/playback-info" 请求了。    
2、在你播放时的结束的地方，要通过reverse的socket发送stop状态给iphone，这样你android推出，iphone才能推出airplay模式    
3、iphone向你请求播放时间和长度的时候，你返回如下格式    

  ```JAVA
   "duration: " + strDuration + "\nposition: " + strCurPos
  ```
  记住duration:之后有一个空格，否则iphone 进入 airplay模式，时间就不东了，不会和android播放, 具体的参考代码。  
  
#### 具体的协议分析   
以iphone6上的优酷客户端为例，向我的android设备推送视频  
1、iphone发送 http post /play http 1.1 消息 ，里面包含了推送的视频的url地址（http链接，优酷为，m3u8文件），字段为Content-Location，和起始的播放时间点，字段为Start-Position。   
android设备回复http 200 ok    

2、iphone紧接着发送http get /scrub http 1.1 消息，get方法是用来获取当前你的android设备的播放器获取的该链接的总体播放时间和当前播放时间。   
android设备需要回复你的播放duration: 和 position两个字段   

3、iphone发送 http post /reverse 消息   
android设备需要回复 switch protocols 并且保存该长链接，该链接后续用于向iphone设备发送你的android设备播放停止的消息。  

4、媒体在android客户端播放后   
iphone不停的发送 http get /scrub消息，用于获取当前android设备的播放duration和position   
android设备回复你当前的播放duration和position。  

5、当你的iphone点击暂停后   
iphone发送http post /rate消息，其中包含字段value，如果value字段为0说明为暂停播放了   
android设备收到该指令就要暂停播放了   

6、当你的 iphone重新播放后   
iphone发送http post /rate消息，字段value 为 1   
android设备重新开始播放   

7、当你的iphone退出airplay后    
iphone向android设备推送 http post /stop消息   
android收到后，退出播放，同时使用reverse长链接向iphone发送post消息，报告自己的状态为stopped。  

8、当你的android设备主动退出播放   
android设备需要主动通过reverse长链接向iphone发送post消息，报告自己stoppe的   
iphone收到后退出airplay模式  

具体的协议交互截图就不发上传了，可以自行抓包分析  

#### 推送效果
gif图有点大需要等待。     
![image](https://github.com/gpfduoduo/AirPlay-Receiver-on-Android/blob/master/push_video.gif "推送视频")

##  感谢与推荐
1 源视频播放器]](https://www.vitamio.org/)   
2 使用google最新的android studio更是方便，不像eclipse那样将vitamio作为library还需要搞一堆的res资源，在android manifest.xml中添加许多的类。android studio将其作为module，之后什么不需要，直接调用即可。
