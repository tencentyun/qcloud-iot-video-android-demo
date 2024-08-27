# qcloud-iot-video-android-demo

**目录**

<!-- TOC -->

- [1. 功能介绍](#1-功能介绍)
- [2. 接入方式](#2-接入方式)
    - [2.1 引用稳定版:](#21-引用稳定版)
    - [2.2 引用SNAPSHOT版：](#22-引用snapshot版)
- [3. 使用流程](#3-使用流程)
    - [基本使用流程](#基本使用流程)
- [4. 接口参考](#4-接口参考)
    - [接口列表](#接口列表)
- [5. 注意事项](#5-注意事项)

<!-- /TOC -->
--------

# 1. 功能介绍

本demo是实现voip sdk，借助微信小程序音视频通话的能力，开发者可以通过小程序框架，实现设备和手机微信端的一对一音视频通话，满足实时触达场景，提升通话体验。

# 2. 接入方式

使用Android aar库

## 2.1 引用稳定版：

在应用模块的build.gradle中配置

```
dependencies ***REMOVED***
    implementation 'com.tencent.iot.video:video-device-android:x.x.x'
}
```

具体版本号可参考[版本号列表](https://central.sonatype.com/search?q=video-device-android)

## 2.2 引用SNAPSHOT版：

(1). 在工程的build.gradle中配置仓库url

```
allprojects ***REMOVED***
    repositories ***REMOVED***
        google()
        jcenter()
        maven ***REMOVED***
            url "https://oss.sonatype.org/content/repositories/snapshots"
      ***REMOVED***
  ***REMOVED***
}
```

(2). 在应用模块的build.gradle中配置

```
dependencies ***REMOVED***
    implementation 'com.tencent.iot.video:video-device-android:x.x.x-SNAPSHOT'
}
```

**注：建议使用稳定版本，SNAPSHOT版仅供开发自测使用**

# 3. 使用流程

* 使用前请适配 `include\wmpf\wxvoip_os_impl.h` 中的相关接口，并确保所有接口均可正常工作
* 文件系统要求提供8KB的可用空间用于存放密钥（以FAT32文件系统，簇大小4KB估算，不含FAT表）
* 文件系统要求断电不丢失可持久化存储，不得使用 RAMFS 等断电丢失的文件系统
* 本模块依赖 av 模块，需在 av 模块后初始化，iv_avt_notify_cb 回调函数收到 IV_AVT_EVENT_P2P_PEER_READY
  事件后方可进行呼叫
* 协议相关文档请参阅 https://cloud.tencent.com/document/product/1081/107667

## 基本使用流程

```
// 依次按顺序初始化必要的功能模块
VoipNativeInterface.getInstance().initIvSystem(...)
VoipNativeInterface.getInstance().initIvDm()
VoipNativeInterface.getInstance().initIvAvt(...)
VoipNativeInterface.getInstance().initWxCloudVoip(...)

// -------- 呼叫流程开始 --------

// onNotify() 回调函数收到 IV_AVT_EVENT_P2P_PEER_READY 事件后方可进行后续呼叫流程
if (!iv_avt_voip_is_busy()) ***REMOVED***
    // 非占线情况下即可开始呼叫，该接口为阻塞接口，呼叫成功后微信会弹出语音通话界面
    doWxCloudVoipCall(...)
}
else ***REMOVED***
    // 如果本机已在通话中可以主动挂断；如有其他用户占线请稍后重试
    doWxCloudVoipHangUp(...)
}

// 呼叫成功后会触发 onStartRealPlay(...) 等多个回调函数，之后便可以收发音视频数据
sendAudioData()

// 通话结束后请主动挂断，对方挂断会收到 onRecvCommand(...) 回调函数发来的信令
VoipNativeInterface.getInstance().doWxCloudVoipHangUp()
// -------- 呼叫流程结束 --------

// 不再使用请依次销毁相关模块，长带电设备不必销毁，重复上述呼叫流程即可
VoipNativeInterface.getInstance().exitWxCloudVoip()
VoipNativeInterface.getInstance().exitIvAvt()
VoipNativeInterface.getInstance().exitIvDm()
VoipNativeInterface.getInstance().exitIvSys()
```

# 4. 接口参考

## 接口列表

**该功能模块提供以下接口**

* initIvSystem() iv system
* initIvDm() 初始化 data model
* initIvAvt() 初始化 audio and video transmission
* initWxCloudVoip() voip功能关闭
* registerAvtVoip() voip 注册
* isAvtVoipRegistered()  检测设备是否已经注册
* sendMsgNotice() 发送消息(通知)
* doWxCloudVoipCall() voip呼叫
* doWxCloudVoipAudioCall() voip呼叫
* doWxCloudVoipHangUp() voip挂断
* isWxCloudVoipBusy() voip是否占线
* sendFinishStream() 设备端主动结束P2P视频传输
* sendVideoData() 发送视频流
* sendAudioData() 发送音频和视频流
* getSendStreamStatus() 获取发送流真实状态
* getSendStreamBuf()  获取相关P2P视频流传输通道的发送缓冲区大小，用于用户自定义的P2P链路拥塞控制
* exitWxCloudVoip() 退出 voip功能
* exitIvAvt() 退出 audio and video transmission
* exitIvDm() 退出 data model
* exitIvSys() 退出 iv system

# 5. 注意事项

1. 本模块依赖av模块内的音视频传输接口，请按顺序初始化 initIvSystem, initIvDm, initIvAvt,
   initWxCloudVoip
2. 呼叫前前检查是否占线，占线时无法发起呼叫。如果本机已在通话中可以主动挂断后发起新的呼叫；如有其他用户占线请稍后重试。
3. doWxCloudVoipCall(...) 呼叫成功后会触发 av 模块中的接口，详细内容请参考 av 模块文档
4. 当对方出现挂断、占线等情况时，onRecvCommand() 回调函数内会收到 IV_AVT_COMMAND_CALL_XXX
   的信令，用户可根据实际情况处理相关信令
5. 对方正常挂断设备端会先通过 onRecvCommand() 回调收到 IV_AVT_COMMAND_CALL_HANG_UP 信令，之后触发
   onStopRealPlay() 回调
6. 当网络异常导致挂断时设备端无法收到 IV_AVT_COMMAND_CALL_HANG_UP 信令，只会触发 onStopRealPlay() 回调