# 微信小程序音视频通话

**目录**

注：此旧接口已停止维护，请使用新v2接口，文档地址：[TWECALL V2文档说明](TWECALLV2文档说明.md)

<!-- TOC -->

- [1. 功能介绍](#1-功能介绍)
- [2. 使用流程](#2-使用流程)
    - [基本使用流程](#基本使用流程)
- [3. 接口参考](#3-接口参考)
    - [接口列表](#接口列表)
    - [接口描述](#接口描述)
    - [回调接口介绍](#回调接口介绍)
- [4. 注意事项](#4-注意事项)

<!-- /TOC -->
--------

# 1. 功能介绍

本模块借助微信小程序音视频通话的能力，开发者可以通过小程序框架，实现设备和手机微信端的一对一音视频通话，满足实时触达场景，提升通话体验。

# 2. 使用流程

* 使用前请适配 `include\wmpf\wxvoip_os_impl.h` 中的相关接口，并确保所有接口均可正常工作
* 文件系统要求提供8KB的可用空间用于存放密钥（以FAT32文件系统，簇大小4KB估算，不含FAT表）
* 文件系统要求断电不丢失可持久化存储，不得使用 RAMFS 等断电丢失的文件系统
* 本模块依赖 av 模块，需在 av 模块后初始化，onNotify() 回调函数收到 IV_AVT_EVENT_P2P_PEER_READY
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
if (isRegistered == 0) { //表示未注册
    VideoNativeInterface.getInstance().registerAvtVoip(mSNTicket); //执行注册
}

// -------- 呼叫流程开始 --------

// onNotify() 回调函数收到 IV_AVT_EVENT_P2P_PEER_READY 事件后方可进行后续呼叫流程
if (!isWxCloudVoipBusy()) {
    // 非占线情况下即可开始呼叫，该接口为阻塞接口，呼叫成功后微信会弹出语音通话界面
    doWxCloudVoipCall(...)
}
else {
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

# 3. 接口参考

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
| 参数名称 | 类型 | 描述 | 输入/输出 |
| --------- | ------------------ | ------------------------------- | --------- |
| type | voip_wxa_type_t * | 小程序类型 | 输入 |
| data_path | const char * | voip 自动生成的设置文件存储路径 | 输入 |
| model_id | const char * | voip model_id | 输入 |
| device_id | const char * | voip device_id | 输入 |
| wxa_appid | const char * | voip 微信小程序 wxa_appid | 输入 |

**返回值**  
| 返回值 | 描述 |
| ---------- | ------------------------------------ |
| WXERROR_OK | 成功 |
| WXERROR_*  | 失败，请参考 wx_error_t 对应的错误码 |
| 9800001 | sn长度不能超过128字节 |
| 9800002 | sn包含非法字符 |
| 9800003 | model_id检查不通过 |

### isAvtVoipRegistered()  检测设备是否已经注册

**功能描述**  
检测设备是否已经注册。  
需要在 iv_avt_voip_init 调用完成之后才能调用本函数。

**参数说明**  
| 参数名称 | 类型 | 描述 | 输入/输出 |
| -------- | ----- | -------------------------- | --------- |
| is_reg | int * | voip 微信小程序绑定 ticket | 输出 |

**返回值**  
| 返回值 | 描述 |
| ---------- | ------------------------------------ |
| WXERROR_OK | 成功 |
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
| 参数名称 | 类型 | 描述 | 输入/输出 |
| -------- | ------------ | --------------------------------------------------------- | --------- |
| ticket | const char * | voip 微信小程序绑定 ticket，有效期5分钟，获取后请尽快注册 | 输入 |

**返回值**  
| 返回值 | 描述 |
| ---------- | ----------------------------------------- |
| WXERROR_OK | 成功 |
| WXERROR_*  | 失败，请参考 wx_error_t 对应的错误码 |
| -10008 | snticket 有问题，常见原因是 snticket 过期 |

### exitWxCloudVoip() 退出 voip功能

**功能描述**  
voip模块退出，释放资源。

**参数说明**  
| 参数名称 | 类型 | 描述 | 输入/输出 |
| -------- | ---- | ---- | --------- |
| 无 | 无 | 无 | 无 |

**返回值**  
| 返回值 | 描述 |
| ------ | ---- |
| 无 | 无 |

### doWxCloudVoipCall() voip呼叫

**功能描述**  
发起 voip 呼叫，该接口为阻塞接口，阻塞时长视网络情况而定。

**参数说明**  
| 参数名称 | 类型 | 描述 | 输入/输出 |
| -------------------- | ------------------- | -------------------------------------------------------------------------------------------------- | --------- |
| open_id | const char * | voip open_id | 输入 |
| device_id | const char * | voip device_id | 输入 |
| model_id | const char * | voip model_id | 输入 |
| wxa_appid | const char * | voip 微信小程序 wxa_appid | 输入 |
| call_type | int | voip 呼叫类型 | 输入 |
| v_info | voip_video_info_s | 设备端指定收发视频格式信息 | 输入 |
| caller_camera_switch | uint32_t |
主叫端摄像头开关，0关闭，1开启，如果设备端不具备摄像头或不需要开启摄像头，请设置为关闭 | 输入 |
| callee_camera_switch | uint32_t |
被叫端摄像头开关，0关闭，1开启，如果设备端不具备屏幕或不需要查看微信用户的摄像头内容，请设置为关闭 |
输入 |

**返回值**  
| 返回值 | 描述 |
| ------------------------------ | ---------------------------------------------------------------------------------- |
| -1 | groupId 错误 |
| -2 | 设备 deviceId 错误 |
| -3 | voip_id 错误 |
| -4 | 校园场景支付刷脸模式，voipToken 错误 |
| -5 | 生成 voip 房间错误 |
| -7 | openId 错误 |
| -8 | openId 未授权 |
| -9 | 校园场景支付刷脸模式：openId 不是 userId 的联系人；硬件设备模式：openId 未绑定设备 |
| -12 | 小程序音视频能力审核未完成，正式版中暂时无法使用 |
| -13 | 硬件设备拨打手机微信模式，voipToken 错误 |
| -14 | 手机微信拨打硬件设备模式，voipToken 错误 |
| -15 | 音视频费用包欠费 |
| -17 | voipToken 对应 modelId 错误 |
| -19 | openId 与小程序 appId 不匹配。请注意同一个用户在不同小程序的 openId 是不同的 |
| -20 | openId 无效 |
| WXERROR_OK | 成功 |
| WXERROR_*                      | 正数错误码，请参考 wx_error_t 对应的错误码 |
| IV_ERR_AVT_REQ_CHN_BUSY | 占线 |
| IV_ERR_AVT_INPUT_PARAM_INVAILD | 初始化失败或未初始化 |
| IV_ERR_AVT_FAILED | 其他错误 |

### doWxCloudVoipHangUp() voip挂断

**功能描述**  
用于本机主动挂断 voip 呼叫
对方主动挂断通话时不需要调用此接口

**参数说明**  
| 参数名称 | 类型 | 描述 | 输入/输出 |
| -------- | ---- | ---- | --------- |
| 无 | 无 | 无 | 无 |

**返回值**  
| 返回值 | 描述 |
| ----------- | -------------------- |
| IV_ERR_NONE | 成功 |
| IV_ERR_*    | 失败，对应相应错误码 |

### isWxCloudVoipBusy() voip是否占线

**功能描述**  
检查是否占线

**参数说明**  
| 参数名称 | 类型 | 描述 | 输入/输出 |
| -------- | ---- | ---- | --------- |
| 无 | 无 | 无 | 无 |

**返回值**  
| 返回值 | 描述 |
| ------ | ------ |
| 0 | 无占线 |
| 1 | 占线 |

## 回调接口介绍

**IvDeviceCallback接口介绍**

- onOnline 设备上线通知回调
- onOffline 设备离线通知回调
- onModuleStatus 功能模块状态回调

### onOnline

**功能描述**  
设备上线回调，通知用户设备已经上线，并会带入网络时间参数，该回调中不要做耗时太长的操作。
网络异常时该时间参数不保证可靠，用户如需对设备进行校时，建议使用 iv_sys_get_time 接口
当sdk不支持mqtt通信时(根据配置FEATURE_DM_CONFIG_USE_MQTT)，该接口无效

**参数说明**  
| 参数名称 | 类型 | 描述 | 输入/输出 |
| -------------- | -------- | ------------------------ | --------- |
| u64NetDateTime | uint64_t | 网络时间，距1970年毫秒数 | 输入 |

**返回值**  
| 返回值 | 描述 |
| ------ | ---- |
| void | 无 |

### onOffline

**功能描述**  
设备失去与 IoT Video 服务器的连接后，通过此回调通知用户，该回调中不要做耗时太长的操作
当sdk不支持mqtt通信时(根据配置FEATURE_DM_CONFIG_USE_MQTT)，该接口无效

**参数说明**  
| 参数名称 | 类型 | 描述 | 输入/输出 |
| -------- | ---------------------------- | ------------ | --------- |
| status | iv_sys_offline_status_type_e | 离线时的状态 | 输入 |

**返回值**  
| 返回值 | 描述 |
| ------ | ---- |
| void | 无 |

### onModuleStatus

**功能描述**  
功能模块在后台服务开启状态的回调通知

**参数说明**

| 参数名称          | 类型       | 描述     | 输入/输出 |
|---------------|----------|--------|-------|
| module_status | uint32_t | 功能模块状态 | 输入    |

**返回值**

| 返回值  | 描述 |
|------|----|
| void | 无  |

**使用说明**  
该接口将通知各功能模块是否在后台开启服务，使用功能模块状态参数通知，
按位表示，0 未开启，1 开启； bit 0: 云存模块 bit 1: Ai模块

**IvAvtCallback接口介绍**

- onGetAvEncInfo 获取音视频编码参数信息
- onStartRealPlay 现场音视频开始播放回调
- onStopRealPlay 现场音视频停止播放回调
- onStartRecvAudioStream 开始接收数据流回调
- onStartRecvVideoStream 开始接收数据流回调
- onStopRecvStream 停止接收数据流回调
- onRecvStream 接收数据回调
- onNotify 事件通知回调
- onRecvCommand 接收信令回调
- onDownloadFile 文件下载请求回调
- onGetPeerOuterNet 获取对端外网IP信息回调

### onGetAvEncInfo

**功能描述**  
现场音视频开始播放回调。用于观看端发起现场监控时，SDK 回调设备端以取得此次监控通路的相应音视频信息。

**参数说明**  
| 参数名称 | 类型 | 描述 | 输入/输出 |
| -------------- | ----------------------- | -------------- | --------- |
| visitor | uint32_t | 访问者ID | 输入 |
| channel | uint32_t | 通道号 | 输入 |
| video_res_type | iv_avt_video_res_type_e | 视频流类型 | 输入 |
| av_data_info | iv_cm_av_data_info_s * | 音视频数据信息 | 输出 |
| args | void * | 当使用用户回放通道时, 会传输些时间参数 | 输入 |

**返回值**  
无

### onStartRealPlay

**功能描述**  
现场音视频开始播放回调。用于观看端发起现场监控时，SDK 回调通知设备端启动相关音视频业务。

**参数说明**  
| 参数名称 | 类型 | 描述 | 输入/输出 |
| -------------- | ----------------------- | --------------------------------------------------------------- | --------- |
| visitor | uint32_t | 访问者ID | 输入 |
| channel | uint32_t | 通道号 | 输入 |
| video_res_type | iv_avt_video_res_type_e | 视频流类型 | 输入 |
| args | void * | 实时监控或回放时由于传输某些参数，参见 iv_avt_req_stream_info_s | 输入 |

**返回值**  
无

**注意**  
当每次请求数据流时都会触发一次该回调，即使是不同的对端请求相同的channel和类型的数据流时，也会触发多次该回调，详细使用方法参照`demo`；`visitor`
表示对端的ID，表明请求者的身份；`channel`表示摄像头的ID，针对单摄像头设备，该值为0；`video_res_type`
表示一个摄像头传输的视频流的类型(类似IPC的主码流,子码流概念)，分辨率不强制一一对应

### onStopRealPlay

**功能描述**  
现场音视频停止播放回调，用于观看端停止现场监控时，SDK 回调通知设备端关闭相关音视频业务。

**参数说明**  
| 参数名称 | 类型 | 描述 | 输入/输出 |
| -------------- | ----------------------- | ---------- | --------- |
| visitor | uint32_t | 访问者ID | 输入 |
| channel | uint32_t | 通道号 | 输入 |
| video_res_type | iv_avt_video_res_type_e | 视频流类型 | 输入 |

**返回值**  
无

**注意**  
当每次结束数据流时都会触发一次该回调，即使是不同的对端结束相同的channel和类型的数据流时，也会触发多次该回调，详细使用方法参照`demo`

### onStartRecvVideoStream、onStartRecvAudioStream

**功能描述**  
通知设备开始接收对向数据流回调，并输出数据流的编码信息，在收到第一个音频帧和第一个视频帧时，该回调会被分别调用，即可能被调用两次

**参数说明**  
| 参数名称 | 类型 | 描述 | 输入/输出 |
| ------------- | ---------------------- | ------------------ | --------- |
| visitor | uint32_t | 访问者ID | 输入 |
| channel | uint32_t | 通道号 | 输入 |
| stream_type | iv_avt_stream_type_e | 音视频标识 | 输入 |
| pstAvDataInfo | iv_cm_av_data_info_s * | 音视频解码信息参数 | 输入 |

**返回值**  
| 返回值 | 描述 |
| ----------- | -------------------- |
| IV_ERR_NONE | 成功 |
| IV_ERR_*    | 失败，对应相应错误码 |

### onStopRecvStream

**功能描述**  
通知设备停止接收对向数据流的回调，结束收流的回调只会被调用一次

**参数说明**  
| 参数名称 | 类型 | 描述 | 输入/输出 |
| ----------- | -------------------- | ---------- | --------- |
| visitor | uint32_t | 访问者ID | 输入 |
| channel | uint32_t | 通道号 | 输入 |
| stream_type | iv_avt_stream_type_e | 音视频标识 | 输入 |

**返回值**  
| 返回值 | 描述 |
| ----------- | -------------------- |
| IV_ERR_NONE | 成功 |
| IV_ERR_*    | 失败，对应相应错误码 |

### onRecvStream

**功能描述**  
接收对向的数据流回调。用于观看端向设备发送音视频数据时，设备端的接收数据回调，由用户实现对数据进行处理播放。

**参数说明**  
| 参数名称 | 类型 | 描述 | 输入/输出 |
| ----------- | -------------------- | ---------------------- | --------- |
| visitor | uint32_t | 访问者ID | 输入 |
| channel | uint32_t | 通道号 | 输入 |
| stream_type | iv_avt_stream_type_e | 音视频标识 | 输入 |
| pStream | void * | 每次接收的音频数据内容 | 输入 |

**返回值**  
| 返回值 | 描述 |
| ----------- | -------------------- |
| IV_ERR_NONE | 成功 |
| IV_ERR_*    | 失败，对应相应错误码 |

### onNotify

**功能描述**  
监控过程中，事件通知接口，用户在该回调中可以根据事件类型做相应的操作。当前版本主要是拥塞控制的事件回调;

**参数说明**  
| 参数名称 | 类型 | 描述 | 输入/输出 |
| ----------- | -------------------- | ---------------- | --------- |
| event | iv_avt_event_e | 事件类型 | 输入 |
| visitor | uint32_t | 访问者ID | 输入 |
| channel | uint32_t | 通道号 | 输入 |
| stream_type | iv_avt_stream_type_e | 音视频流数据类型 | 输入 |

**返回值**  
| 返回值 | 描述 |
| ----------- | -------------------- |
| IV_ERR_NONE | 成功 |
| IV_ERR_*    | 失败，对应相应错误码 |

### onRecvCommand

**功能描述**  
接收信令处理回调

**参数说明**  
| 参数名称 | 类型 | 描述 | 输入/输出 |
| -------------- | ----------------------- | ---------- | --------- |
| command | iv_avt_command_type_e | 信令类型 | 输入 |
| visitor | uint32_t | 访问者ID | 输入 |
| channel | uint32_t | 通道号 | 输入 |
| video_res_type | iv_avt_video_res_type_e | stream类型 | 输入 |
| args | void * | 信令参数 | 输入输出 |

**返回值**  
| 返回值 | 描述 |
| ----------- | -------------------- |
| IV_ERR_NONE | 成功 |
| IV_ERR_*    | 失败，对应相应错误码 |

**说明**  
不同的command，参数args不同，具体使用方式参照`iot_video_demo`，回调中切勿做耗时较长的操作

### onDownloadFile

**功能描述**  
文件下载回调

**参数说明**  
| 参数名称 | 类型 | 描述 | 输入/输出 |
| -------- | ------------------------ | -------- | --------- |
| status | iv_avt_download_status_e | 下载状态 | 输入 |
| visitor | uint32_t | 访问者ID | 输入 |
| channel | uint32_t | 通道号 | 输入 |
| args | void * | 信令参数 | 输入输出 |

**返回值**  
| 返回值 | 描述 |
| ----------- | -------------------- |
| IV_ERR_NONE | 成功 |
| IV_ERR_*    | 失败，对应相应错误码 |

**说明**  
不同的status，参数args不同，具体使用方式参照`iot_video_demo`，回调中切勿做耗时较长的操作

### onGetPeerOuterNet

**功能描述**  
获取对端外网IP信息回调

**参数说明**  
| 参数名称 | 类型 | 描述 | 输入/输出 |
| -------- | -------- | -------- | --------- |
| visitor | uint32_t | 访问者ID | 输入 |
| channel | uint32_t | 通道号 | 输入 |
| net_info | char * | 网络信息 | 输入 |

**返回值**  
无

**说明**  
当前net_info信息输出为对端外网IP

# 4. 注意事项

1. 本模块依赖av模块内的音视频传输接口，请按顺序初始化 initIvSystem(), initIvDm(), initIvAvt(),
   initWxCloudVoip()
2. 呼叫前前检查是否占线，占线时无法发起呼叫。如果本机已在通话中可以主动挂断后发起新的呼叫；如有其他用户占线请稍后重试。
3. doWxCloudVoipCall(...) 呼叫成功后会触发 av 模块中的接口，详细内容请参考 av 模块文档
4. 当对方出现挂断、占线等情况时，onRecvCommand() 回调函数内会收到 IV_AVT_COMMAND_CALL_XXX
   的信令，用户可根据实际情况处理相关信令
5. 对方正常挂断设备端会先通过 onRecvCommand() 回调收到 IV_AVT_COMMAND_CALL_HANG_UP 信令，之后触发
   onStopRealPlay() 回调
6. 当网络异常导致挂断时设备端无法收到 IV_AVT_COMMAND_CALL_HANG_UP 信令，只会触发 onStopRealPlay() 回调