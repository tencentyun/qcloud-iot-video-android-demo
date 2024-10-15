# qcloud-iot-video-android-demo

本demo提供的功能文档说明：

* [voip文档说明](docs/voip文档说明.md)
* [OTA升级说明](docs/OTA升级说明.md)
* [error信息说明](docs/error信息说明.md)

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

准备以下json字符串，并将对应值填上：

```
{"productId":"","deviceName":"","deviceKey":"","voip_model_id":"","voip_sn":"","voip_sn_ticket":"","voip_wxa_appid":"","voip_openid1":""}
```

## 1.2 使用步骤

一、打开demo app，点击配置设备，将准备的json字符串复制进底部弹窗内，点击确定
![1728983404300.jpg](..%2F..%2FDownloads%2F1728983404300.jpg)![WechatIMG47.jpg](..%2F..%2FDownloads%2FWechatIMG47.jpg)
二、点击VOIP,进入VOIP页面，此页面中右上角设置按钮点击选择WX SETTING可单独设置准备好json字符串中的某些值
![1728983715014.jpg](..%2F..%2FDownloads%2F1728983715014.jpg)
三、点击LOGIN,进入选择对应填入的voip_openid1以后，点击视频通过或者音频通过就可以使用了

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