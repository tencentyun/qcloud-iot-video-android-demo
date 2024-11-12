# qcloud-iot-video-android-demo

本demo提供的功能文档说明：

* [公共数据结构](docs/公共数据结构)
* [TWECALL文档说明](docs/TWECALLV2文档说明)
* [OTA升级说明](docs/OTA升级说明.md)
* [云存储模块](docs/云存储模块)
* [ERROR信息说明](docs/ERROR信息说明)
* [常见问题解答](docs/常见问题解答)
* [错误码](docs/错误码)

**目录**

<!-- TOC -->

- [1. DEMO使用](#1-DEMO使用)
    - [1.1 准备工作](#11-准备工作)
    - [1.2 使用步骤](#12-使用步骤)
- [2. 接入方式](#2-接入方式)
    - [2.1 引用稳定版:](#21-引用稳定版)
    - [2.2 引用SNAPSHOT版：](#22-引用snapshot版)

<!-- /TOC -->
--------

# 1. DEMO使用

以下是demo使用教程

## 1.1 准备工作

1、android 11及以上需要给访问所有文件权限。
2、需要从控制台获取productId、deviceName、deviceKey

## 1.2 使用步骤

一、打开demo app，点击配置设备，将productId、deviceName、deviceKey填入对应的位置，
如果使用设备端直播，小程序 or App 查看设备端画面，则视频链路选择单向；
如果使用小程序与设备端双向音视频通信，则选择双向视频；
如果使用小程序呼叫设备端双向音视频通信，则使用呼叫接听双向；
如果使用TWE CALL,或者ota可以随便选择。

/***注：双向和呼叫接听双向在小程序的入口不同。***/

点击确定

### 1.2.1 单向视频链路

执行完以上步骤以后点击音视频按钮，进入单向IPC页面，等待弹出设备上线提示以后，小程序或app可以查看

### 1.2.2 双向视频链路（小程序拨打设备端）

执行完以上步骤以后点击音视频按钮，进入双向音视频页面，等待弹出设备上线提示以后，小程序或app可以查看

### 1.2.3 呼叫接听双向视频链路（小程序拨打设备端）

执行完以上步骤以后点击音视频按钮，进入呼叫接听双向视频链路页面，等待弹出设备上线提示以后，小程序或app可以查看

### 1.2.4 TWE CALL（设备端拨打小程序）

执行完以上步骤以后点击TWE CALL按钮，进入Login 页面后，选择要通信的小程序版本，点击WX
SETTING，配置appid和modelid
点击LOGIN按钮，进入TWE CALL页面，填入openid以后，并弹出初始化完成以后，点击视频通话或者音频通话进行呼叫小程序。
/***注：呼叫失败返回-9，一版情况下是小程序未订阅，请先在小程序进行订阅***/

### 1.2.5 OTA

执行完以上步骤以后点击ota按钮，进入ota 页面后，等待初始化完成，如果有新版本则会在当前状态中展示出来新版本号与版本大小，并且ota升级按钮会变为可点击状态
点击进行升级

# 2. 接入方式

使用Android aar库

## 2.1 引用稳定版：

在应用模块的build.gradle中配置

```
dependencies {
    implementation 'com.tencent.iot.video:video-device-android:x.x.x'
}
```

具体版本号可参考[版本号列表](https://central.sonatype.com/search?q=video-device-android)

## 2.2 引用SNAPSHOT版：

(1). 在工程的build.gradle中配置仓库url

```
allprojects {
    repositories {
        google()
        jcenter()
        maven {
            url "https://oss.sonatype.org/content/repositories/snapshots"
        }
    }
}
```

(2). 在应用模块的build.gradle中配置

```
dependencies {
    implementation 'com.tencent.iot.video:video-device-android:x.x.x-SNAPSHOT'
}
```

**注：建议使用稳定版本，SNAPSHOT版仅供开发自测使用**