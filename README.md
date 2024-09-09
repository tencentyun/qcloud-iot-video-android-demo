# qcloud-iot-video-android-demo

初次体验demo请查看demo使用教程：
* [demo使用教程](app)

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
    - [接口描述](#接口描述)
- [5. 注意事项](#5-注意事项)
- [6. error信息](#6-error信息)

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
VideoNativeInterface.getInstance().initIvSystem(...)
VideoNativeInterface.getInstance().initIvDm()
VideoNativeInterface.getInstance().initIvAvt(...)
VideoNativeInterface.getInstance().initWxCloudVoip(...)

//检测注册
int isRegistered = VideoNativeInterface.getInstance().isAvtVoipRegistered();
if (isRegistered == 0) ***REMOVED*** //表示未注册
    VideoNativeInterface.getInstance().registerAvtVoip(mSNTicket); //执行注册
}

// -------- 呼叫流程开始 --------

// onNotify() 回调函数收到 IV_AVT_EVENT_P2P_PEER_READY 事件后方可进行后续呼叫流程
if (!isWxCloudVoipBusy()) ***REMOVED***
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
VideoNativeInterface.getInstance().doWxCloudVoipHangUp()
// -------- 呼叫流程结束 --------

// 不再使用请依次销毁相关模块，长带电设备不必销毁，重复上述呼叫流程即可
VideoNativeInterface.getInstance().exitWxCloudVoip()
VideoNativeInterface.getInstance().exitIvAvt()
VideoNativeInterface.getInstance().exitIvDm()
VideoNativeInterface.getInstance().exitIvSys()
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

## 接口描述

### initWxCloudVoip() voip功能关闭

**功能描述**  
voip模块初始化，函数内部会执行鉴权等操作。  
该接口为阻塞接口，阻塞时长视网络情况而定。

**参数说明**  
| 参数名称  | 类型               | 描述                            | 输入/输出 |
| --------- | ------------------ | ------------------------------- | --------- |
| type      | voip_wxa_type_t *  | 小程序类型                      | 输入      |
| data_path | const char *       | voip 自动生成的设置文件存储路径 | 输入      |
| model_id  | const char *       | voip model_id                   | 输入      |
| device_id | const char *       | voip device_id                  | 输入      |
| wxa_appid | const char *       | voip 微信小程序 wxa_appid       | 输入      |

**返回值**  
| 返回值     | 描述                                 |
| ---------- | ------------------------------------ |
| WXERROR_OK | 成功                                 |
| WXERROR_*  | 失败，请参考 wx_error_t 对应的错误码 |
| 9800001    | sn长度不能超过128字节                |
| 9800002    | sn包含非法字符                       |
| 9800003    | model_id检查不通过                   |

### isAvtVoipRegistered()  检测设备是否已经注册

**功能描述**  
检测设备是否已经注册。  
需要在 iv_avt_voip_init 调用完成之后才能调用本函数。

**参数说明**  
| 参数名称 | 类型  | 描述                       | 输入/输出 |
| -------- | ----- | -------------------------- | --------- |
| is_reg   | int * | voip 微信小程序绑定 ticket | 输出      |

**返回值**  
| 返回值     | 描述                                 |
| ---------- | ------------------------------------ |
| WXERROR_OK | 成功                                 |
| WXERROR_*  | 失败，请参考 wx_error_t 对应的错误码 |

### registerAvtVoip() voip 注册

**功能描述**  
注册设备。  
该接口为阻塞接口，阻塞时长视网络情况而定。  
新设备初次使用或清理 data_path 后需要调用一次此接口进行注册。  
绑定成功后 data_path 路径下会产生大小不为0的密钥文件，请勿删除。  
如需重新绑定（或恢复出厂设置等情况）请删除 data_path 路径下所有文件，并使用新的 ticket 重新绑定。  
若设备已经注册，则立即返回。  
若设备未注册或注册数据错误，会再次注册。

**参数说明**  
| 参数名称 | 类型         | 描述                                                      | 输入/输出 |
| -------- | ------------ | --------------------------------------------------------- | --------- |
| ticket   | const char * | voip 微信小程序绑定 ticket，有效期5分钟，获取后请尽快注册 | 输入      |

**返回值**  
| 返回值     | 描述                                      |
| ---------- | ----------------------------------------- |
| WXERROR_OK | 成功                                      |
| WXERROR_*  | 失败，请参考 wx_error_t 对应的错误码      |
| -10008     | snticket 有问题，常见原因是 snticket 过期 |

### exitWxCloudVoip() 退出 voip功能

**功能描述**  
voip模块退出，释放资源。

**参数说明**  
| 参数名称 | 类型 | 描述 | 输入/输出 |
| -------- | ---- | ---- | --------- |
| 无       | 无   | 无   | 无        |

**返回值**  
| 返回值 | 描述 |
| ------ | ---- |
| 无     | 无   |

### doWxCloudVoipCall() \ doWxCloudVoipAudioCall() voip呼叫

**功能描述**  
发起 voip 呼叫，该接口为阻塞接口，阻塞时长视网络情况而定。

**参数说明**  
| 参数名称             | 类型                | 描述                                                                                               | 输入/输出 |
| -------------------- | ------------------- | -------------------------------------------------------------------------------------------------- | --------- |
| type                 | iv_cm_stream_type_e | 呼叫类型，支持音视频、或仅音频                                                                     | 输入      |
| open_id              | const char *        | voip open_id                                                                                       | 输入      |
| device_id            | const char *        | voip device_id                                                                                     | 输入      |
| model_id             | const char *        | voip model_id                                                                                      | 输入      |
| wxa_appid            | const char *        | voip 微信小程序 wxa_appid                                                                          | 输入      |
| v_info               | voip_video_info_s   | 设备端指定收发视频格式信息                                                                         | 输入      |
| caller_camera_switch | uint32_t            | 主叫端摄像头开关，0关闭，1开启，如果设备端不具备摄像头或不需要开启摄像头，请设置为关闭             | 输入      |
| callee_camera_switch | uint32_t            | 被叫端摄像头开关，0关闭，1开启，如果设备端不具备屏幕或不需要查看微信用户的摄像头内容，请设置为关闭 | 输入      |

**返回值**  
| 返回值                         | 描述                                                                               |
| ------------------------------ | ---------------------------------------------------------------------------------- |
| -1                             | groupId 错误                                                                       |
| -2                             | 设备 deviceId 错误                                                                 |
| -3                             | voip_id 错误                                                                       |
| -4                             | 校园场景支付刷脸模式，voipToken 错误                                               |
| -5                             | 生成 voip 房间错误                                                                 |
| -7                             | openId 错误                                                                        |
| -8                             | openId 未授权                                                                      |
| -9                             | 校园场景支付刷脸模式：openId 不是 userId 的联系人；硬件设备模式：openId 未绑定设备 |
| -12                            | 小程序音视频能力审核未完成，正式版中暂时无法使用                                   |
| -13                            | 硬件设备拨打手机微信模式，voipToken 错误                                           |
| -14                            | 手机微信拨打硬件设备模式，voipToken 错误                                           |
| -15                            | 音视频费用包欠费                                                                   |
| -17                            | voipToken 对应 modelId 错误                                                        |
| -19                            | openId 与小程序 appId 不匹配。请注意同一个用户在不同小程序的 openId 是不同的       |
| -20                            | openId 无效                                                                        |
| WXERROR_OK                     | 成功                                                                               |
| WXERROR_*                      | 正数错误码，请参考 wx_error_t 对应的错误码                                         |
| IV_ERR_AVT_REQ_CHN_BUSY        | 占线                                                                               |
| IV_ERR_AVT_INPUT_PARAM_INVAILD | 初始化失败或未初始化                                                               |
| IV_ERR_AVT_FAILED              | 其他错误                                                                           |

### doWxCloudVoipHangUp() voip挂断

**功能描述**  
用于本机主动挂断 voip 呼叫
对方主动挂断通话时不需要调用此接口

**参数说明**  
| 参数名称 | 类型 | 描述 | 输入/输出 |
| -------- | ---- | ---- | --------- |
| 无       | 无   | 无   | 无        |

**返回值**  
| 返回值      | 描述                 |
| ----------- | -------------------- |
| IV_ERR_NONE | 成功                 |
| IV_ERR_*    | 失败，对应相应错误码 |

### isWxCloudVoipBusy() voip是否占线

**功能描述**  
检查是否占线

**参数说明**  
| 参数名称 | 类型 | 描述 | 输入/输出 |
| -------- | ---- | ---- | --------- |
| 无       | 无   | 无   | 无        |

**返回值**  
| 返回值 | 描述   |
| ------ | ------ |
| 0      | 无占线 |
| 1      | 占线   |

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

# 6. error信息

* @brief 操作成功
WXERROR_OK = 0

* @brief 操作被取消
* 通常是被调用方取消.
WXERROR_CANCELLED = 1

* @brief 未知错误
* 通常，你应该尽可能返回其他更加详细的错误码。如果实在没有相关可以使用的错误码，使用
* WXERROR_UNKNOWN.
WXERROR_UNKNOWN = 2

* @brief 非法参数
* 这表示方法调用方传递了非法参数，如需要接受一个非空指针，但是调用方实际传递了个空指针；或者调用方传递了一个错误的文件路径.
* 请注意，该错误仅应用于参数能被简单地校验失败的情况下.
* 对于传入的参数不符合系统状态的情况，你应该返回
* WXERROR_FAILED_PRECONDITION.
WXERROR_INVALID_ARGUMENT = 3

* @brief 调用超时
* 这表示操作应该在指定时间内完成，但并未完成.
WXERROR_TIMEOUT = 4

* @brief 被请求的实体不存在
* 如文件不存在.
WXERROR_NOT_FOUND = 5

* @brief 希望创建的实体已存在
* 如要创建的文件已经存在.
WXERROR_ALREADY_EXISTS = 6

* @brief 没有权限
* 这表示调用方无权调用指定的操作.
* 需要与 WXERROR_UNAUTHENTICATED 进行区分.
WXERROR_PERMISSION_DENIED = 7

* @brief 资源不足
* 可能表示磁盘空间已满, 或网络不连通.
WXERROR_RESOURCE_EXHAUSTED = 8

* @brief 前置条件不满足
* 这表示当前操作因系统不在可以运行的状态中而被拒绝执行.
* 比如要删除文件夹时，指定的路径上实际是个普通文件.
WXERROR_FAILED_PRECONDITION = 9

* @brief 操作被中断
WXERROR_ABORTED = 10

* @brief 超出范围
* 这表示当前操作超出了允许的范围，如读取文件时超出了文件长度.
WXERROR_OUT_OF_RANGE = 11

* @brief 操作未实现
* 这表示当前操作尚未被实现/被支持. 因此当前操作不应该重试.
WXERROR_UNIMPLEMENTED = 12

* @brief 内部错误
* 这表示当前操作因为系统状态不正常而失败. 这通常表示系统内部有 bug.
WXERROR_INTERNAL = 13

* @brief 服务不可用
* 这表示操作尚不可用.
* 比如要播放音频时，音频设备未连接；或者要采集音频时，话筒设备未连接.
WXERROR_UNAVAILABLE = 14

* @brief 数据丢失
WXERROR_DATA_LOSS = 15

* @brief
WXERROR_UNAUTHENTICATED = 16

* @brief IO 错误
* 一般是文件创建、读、写。
WXERROR_IO = 17

* @brief 回复错误
* 一般是网络返回内容不对
WXERROR_RESPONSE = 18

* @brief 设备信息不匹配
* 一般是传入的 sn、modelid 与原注册信息不匹配
WXERROR_INVALID_DEVICEID = 19