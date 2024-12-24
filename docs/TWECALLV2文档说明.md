**目录**

<!-- TOC -->

- [1. 功能介绍](#1-功能介绍)
- [2. 使用流程](#2-使用流程)
    - [基本使用流程](#基本使用流程)
- [3. 接口参考](#3-接口参考)
    - [接口列表](#接口列表)
    - [接口描述](#接口描述)
        - [initWxCloudVoip](#initWxCloudVoip)
        - [isAvtVoipRegistered](#isAvtVoipRegistered)
        - [registerAvtVoip](#registerAvtVoip)
        - [exitWxCloudVoip](#exitWxCloudVoip)
        - [doWxCloudVoipCall](#doWxCloudVoipCall)
        - [doWxCloudVoipHangUp](#doWxCloudVoipHangUp)
        - [isWxCloudVoipBusy](#isWxCloudVoipBusy)
        - [initWxCloudVoipV2](#initWxCloudVoipV2)
        - [exitWxCloudVoipV2](#exitWxCloudVoipV2)
        - [doWxCloudVoipCallV2](#doWxCloudVoipCallV2)
        - [doWxCloudVoipHangUpV2](#doWxCloudVoipHangUpV2)
        - [isWxCloudVoipBusyV2](#isWxCloudVoipBusyV2)
        - [activateVoipLicenseV2](#activateVoipLicenseV2)
        - [getVoipActiveDeviceInfoV2](#getVoipActiveDeviceInfoV2)
        - [getVoipUserListV2](#getVoipUserListV2)
        - [onUpdateAuthorizeStatus](#onUpdateAuthorizeStatus)
- [4. 数据结构](#4-数据结构)
    - [数据结构列表](#数据结构列表)
- [5. 注意事项](#5-注意事项)

<!-- /TOC -->
--------

# 1. 功能介绍

借助微信小程序音视频通话的能力，开发者可以通过小程序框架，实现设备和手机微信端的一对一音视频通话，满足实时触达场景，提升通话体验。

# 2. 使用流程

* 使用前请适配 `include\wmpf\wxvoip_os_impl.h` 中的相关接口，并确保所有接口均可正常工作
* 文件系统要求提供8KB的可用空间用于存放密钥（以FAT32文件系统，簇大小4KB估算，不含FAT表）
* 文件系统要求断电不丢失可持久化存储，不得使用 RAMFS 等断电丢失的文件系统
* 本模块依赖 av 模块，需在 av 模块后初始化，onNotify() 回调函数收到 IV_AVT_EVENT_P2P_PEER_READY
  事件后方可进行呼叫
* 设备端使用前请调用 activateVoipLicenseV2()
  （或云API）进行激活，未激活情况下使用会返回-9或其他错误码（量产设备建议调用云API进行批量激活，云API说明 https://cloud.tencent.com/document/product/1081/106587）
* 设备激活后请在微信小程序中订阅，未订阅情况下使用会返回-9或其他错误码

## 基本使用流程

旧接口已停止维护，此处以v2接口为例

```
int code = 0;
// 依次初始化必要的功能模块
VideoNativeInterface.getInstance().initIvSystem(...);
VideoNativeInterface.getInstance().initIvDm();
VideoNativeInterface.getInstance().initIvAvt(...);
code = VideoNativeInterface.getInstance().initWxCloudVoipV2(...)；
if (code) {
    return code;
}

// 若未使用云API激活设备，可调用 VideoNativeInterface.getInstance().activateVoipLicenseV2(...) 进行激活
code = VideoNativeInterface.getInstance().activateVoipLicenseV2(type);
if (code) {
    return code;
}

// -------- 呼叫流程开始 --------

// IvAvtCallback的onNotify(...) 回调函数收到 IV_AVT_EVENT_P2P_PEER_READY 事件后方可进行后续呼叫流程
if (!VideoNativeInterface.getInstance().isWxCloudVoipBusyV2()) {
    // 非占线情况下即可开始呼叫，该接口为阻塞接口，呼叫成功后微信会弹出语音通话界面
    VideoNativeInterface.getInstance().doWxCloudVoipCallV2(openId,callType, pixelType, callerCameraSwitch, calleeCameraSwitch);
}
else {
    // 如果本机已在通话中可以主动挂断；如有其他用户占线请稍后重试
    VideoNativeInterface.getInstance().doWxCloudVoipHangUpV2();
}

while (1) {
    // 呼叫成功后会触发IvAvtCallback的 onStartRealPlay(...) 等多个回调函数，之后便可以收发音视频数据
    // 详细使用方法请查阅音视频传输及对讲模块文档
    VideoNativeInterface.getInstance().sendAvtVideoData(...);
    VideoNativeInterface.getInstance().sendAvtAudioData(...);
}
// 通话结束后请主动挂断，对方挂断会收到 IvAvtCallback的onRecvCommand() 回调函数发来的信令
 VideoNativeInterface.getInstance().doWxCloudVoipHangUpV2();
// -------- 呼叫流程结束 --------

// 不再使用请依次销毁相关模块，长带电设备不必销毁，重复上述呼叫流程即可
VideoNativeInterface.getInstance().exitWxCloudVoipV2();
VideoNativeInterface.getInstance().exitIvAvt();
VideoNativeInterface.getInstance().exitIvDm();
VideoNativeInterface.getInstance().exitIvSys();
```

# 3. 接口参考

## 接口列表

**该功能模块提供以下接口**

* initWxCloudVoip() voip功能初始化（停止维护，请使用v2接口）
* isAvtVoipRegistered() 检查设备是否注册（停止维护，请使用v2接口）
* registerAvtVoip() 设备注册（停止维护，请使用v2接口）
* exitWxCloudVoip() voip功能关闭（停止维护，请使用v2接口）
* doWxCloudVoipCall() voip呼叫（停止维护，请使用v2接口）
* doWxCloudVoipHangUp() voip挂断（停止维护，请使用v2接口）
* isWxCloudVoipBusy() voip是否占线（停止维护，请使用v2接口）
* initWxCloudVoipV2() voip功能初始化
* exitWxCloudVoipV2() voip功能关闭
* doWxCloudVoipCallV2() voip呼叫
* doWxCloudVoipHangUpV2() voip挂断
* isWxCloudVoipBusyV2() voip是否占线
* activateVoipLicenseV2() 激活设备
* getVoipActiveDeviceInfoV2() 获取激活设备信息
* getVoipUserListV2() 获取微信用户列表

**用户需注册以下回调函数**

* IvVoipCallback.onUpdateAuthorizeStatus() 更新用户授权订阅状态通知

## 接口描述

### initWxCloudVoip()

**功能描述**  
voip模块初始化，函数内部会执行鉴权等操作  
该接口为阻塞接口，阻塞时长视网络情况而定

**函数原型**

```
int iv_avt_voip_init(voip_wxa_type_e type,
                     const char *data_path, const char *model_id,
                     const char *device_id, const char *wxa_appid);
```

**参数说明**  
| 参数名称 | 类型 | 描述 | 输入/输出 |
| --------- | ----------------- | ------------------------------- | --------- |
| type | voip_wxa_type_e * | 小程序类型 | 输入 |
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

### isAvtVoipRegistered()

**功能描述**  
检测设备是否已经注册  
需要在 initWxCloudVoip 调用完成之后才能调用本函数

**函数原型**

```
int iv_avt_voip_is_registered(int *is_reg);
```

**参数说明**  
| 参数名称 | 类型 | 描述 | 输入/输出 |
| -------- | ----- | -------------------------- | --------- |
| is_reg | int * | voip 微信小程序绑定 ticket | 输出 |

**返回值**  
| 返回值 | 描述 |
| ---------- | ------------------------------------ |
| WXERROR_OK | 成功 |
| WXERROR_*  | 失败，请参考 wx_error_t 对应的错误码 |

### registerAvtVoip()

**功能描述**  
注册设备  
该接口为阻塞接口，阻塞时长视网络情况而定  
新设备初次使用或清理 data_path 后需要调用一次此接口进行注册  
绑定成功后 data_path 路径下会产生大小不为0的密钥文件，请勿删除  
如需重新绑定（或恢复出厂设置等情况）请删除 data_path 路径下所有文件，并使用新的 ticket 重新绑定  
若设备已经注册，则立即返回  
若设备未注册或注册数据错误，会再次注册

**函数原型**

```
int iv_avt_voip_register(const char *ticket);
```

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

### exitWxCloudVoip()

**功能描述**  
voip模块去初始化，释放资源

**函数原型**

```
void iv_avt_voip_exit();
```

**参数说明**  
| 参数名称 | 类型 | 描述 | 输入/输出 |
| -------- | ---- | ---- | --------- |
| 无 | 无 | 无 | 无 |

**返回值**  
| 返回值 | 描述 |
| ------ | ---- |
| 无 | 无 |

### doWxCloudVoipCall()

**功能描述**  
发起 voip 呼叫，该接口为阻塞接口，阻塞时长视网络情况而定

**函数原型**

```
int iv_avt_voip_call(iv_cm_stream_type_e type, const char *open_id, const char *device_id,
                     const char *model_id, const char *wxa_appid, voip_video_info_s v_info,
                     uint32_t caller_camera_switch, uint32_t callee_camera_switch)
```

**参数说明**  
| 参数名称 | 类型 | 描述 | 输入/输出 |
| -------------------- | ------------------- | -------------------------------------------------------------------------------------------------- | --------- |
| type | iv_cm_stream_type_e | 呼叫类型，支持音视频、或仅音频 | 输入 |
| open_id | const char * | voip open_id | 输入 |
| device_id | const char * | voip device_id | 输入 |
| model_id | const char * | voip model_id | 输入 |
| wxa_appid | const char * | voip 微信小程序 wxa_appid | 输入 |
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

### doWxCloudVoipHangUp()

**功能描述**  
用于本机主动挂断 voip 呼叫  
对方主动挂断通话时不需要调用此接口

**函数原型**

```
int iv_avt_voip_hang_up();
```

**参数说明**  
| 参数名称 | 类型 | 描述 | 输入/输出 |
| -------- | ---- | ---- | --------- |
| 无 | 无 | 无 | 无 |

**返回值**  
| 返回值 | 描述 |
| ----------- | -------------------- |
| IV_ERR_NONE | 成功 |
| IV_ERR_*    | 失败，对应相应错误码 |

### isWxCloudVoipBusy()

**功能描述**  
检查是否占线

**函数原型**

```
int iv_avt_voip_is_busy()
```

**参数说明**  
| 参数名称 | 类型 | 描述 | 输入/输出 |
| -------- | ---- | ---- | --------- |
| 无 | 无 | 无 | 无 |

**返回值**  
| 返回值 | 描述 |
| ------ | ------ |
| 0 | 无占线 |
| 1 | 占线 |

### initWxCloudVoipV2()

**功能描述**  
voip模块初始化，函数内部会执行鉴权等操作  
该接口为阻塞接口，阻塞时长视网络情况而定  
新设备初次使用或清理 data_path 目录后调用此接口会在 data_path
路径下会产生大小不为0的配置文件，请勿删除  
若 data_path 路径下已有配置文件，调用此接口不会重复生成配置文件  
如需重新绑定（或开发调试期间更换设备信息、恢复出厂设置等情况）请退出此模块，然后删除 data_path
路径下所有文件，并重新初始化

**函数原型**

```
int iv_avt_voip_init_v2(voip_wxa_type_e type, const char *data_path, const char *model_id,
                        const char *wxa_appid, voip_callback_func_s functions);
```

**参数说明**  
| 参数名称 | 类型 | 描述 | 输入/输出 |
| --------- | -------------------- | ------------------------------- | --------- |
| type | voip_wxa_type_e * | 小程序类型 | 输入 |
| data_path | const char * | voip 自动生成的设置文件存储路径 | 输入 |
| model_id | const char * | voip model_id | 输入 |
| wxa_appid | const char * | voip 微信小程序 wxa_appid | 输入 |
| functions | voip_callback_func_s | 回调函数 | 输入 |

**返回值**  
| 返回值 | 描述 |
| ---------- | ------------------------------------ |
| WXERROR_OK | 成功 |
| WXERROR_*  | 失败，请参考 wx_error_t 对应的错误码 |
| 9800001 | sn长度不能超过128字节 |
| 9800002 | sn包含非法字符 |
| 9800003 | model_id检查不通过 |

### exitWxCloudVoipV2()

**功能描述**  
voip模块去初始化，释放资源

**函数原型**

```
void iv_avt_voip_exit_v2();
```

**参数说明**  
| 参数名称 | 类型 | 描述 | 输入/输出 |
| -------- | ---- | ---- | --------- |
| 无 | 无 | 无 | 无 |

**返回值**  
| 返回值 | 描述 |
| ------ | ---- |
| 无 | 无 |

### doWxCloudVoipCallV2()

**功能描述**  
发起 voip 呼叫，该接口为阻塞接口，阻塞时长视网络情况而定

**函数原型**

```
int iv_avt_voip_call_v2(iv_cm_stream_type_e type, const char *open_id, voip_video_info_s v_info,
                        uint32_t caller_camera_switch, uint32_t callee_camera_switch)
```

**参数说明**  
| 参数名称 | 类型 | 描述 | 输入/输出 |
| -------------------- | ------------------- | -------------------------------------------------------------------------------------------------- | --------- |
| type | iv_cm_stream_type_e | 呼叫类型，支持音视频、或仅音频 | 输入 |
| open_id | const char * | 被叫微信用户的 open_id | 输入 |
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
| -15 | 音视频费用包欠费（请购买激活码，并正确分配到对应的产品中） |
| -17 | voipToken 对应 modelId 错误 |
| -19 | openId 与小程序 appId 不匹配。请注意同一个用户在不同小程序的 openId 是不同的 |
| -20 | openId 无效 |
| WXERROR_OK | 成功 |
| WXERROR_*                      | 正数错误码，请参考 wx_error_t 对应的错误码 |
| IV_ERR_AVT_REQ_CHN_BUSY | 占线 |
| IV_ERR_AVT_INPUT_PARAM_INVAILD | 初始化失败或未初始化 |
| IV_ERR_AVT_FAILED | 其他错误 |

### doWxCloudVoipHangUpV2()

**功能描述**  
用于本机主动挂断 voip 呼叫  
对方主动挂断通话时不需要调用此接口

**函数原型**

```
int iv_avt_voip_hang_up_v2();
```

**参数说明**  
| 参数名称 | 类型 | 描述 | 输入/输出 |
| -------- | ---- | ---- | --------- |
| 无 | 无 | 无 | 无 |

**返回值**  
| 返回值 | 描述 |
| ----------- | -------------------- |
| IV_ERR_NONE | 成功 |
| IV_ERR_*    | 失败，对应相应错误码 |

### isWxCloudVoipBusyV2()

**功能描述**  
检查是否占线

**函数原型**

```
int iv_avt_voip_is_busy_v2()
```

**参数说明**  
| 参数名称 | 类型 | 描述 | 输入/输出 |
| -------- | ---- | ---- | --------- |
| 无 | 无 | 无 | 无 |

**返回值**  
| 返回值 | 描述 |
| ------ | ------ |
| 0 | 无占线 |
| 1 | 占线 |

### activateVoipLicenseV2()

**功能描述**  
激活设备  
若未使用云API激活设备，可调用此接口进行激活  
云API激活接口 https://cloud.tencent.com/document/product/1081/106587
（其中sn由`<ProductId>_<DeviceName>`格式拼接而成）

**函数原型**

```
int iv_avt_voip_activate_license_v2(voip_activate_type_e type)
```

**参数说明**  
| 参数名称 | 类型 | 描述 | 输入/输出 |
| -------- | -------------------- | -------- | --------- |
| type | voip_activate_type_e | 设备类型 | 输入 |

**返回值**  
| 返回值 | 描述 |
| ------ | ---- |
| 0 | 正常 |
| 非0值 | 异常 |

### getVoipActiveDeviceInfoV2()

**功能描述**  
获取设备激活信息

**函数原型**

```
int iv_avt_voip_get_active_device_info_v2(voip_activate_info_s *info)
```

**参数说明**  
| 参数名称 | 类型 | 描述 | 输入/输出 |
| -------- | ---------------------- | ------------ | --------- |
| info | voip_activate_type_e * | 设备激活信息 | 输出 |

**返回值**  
| 返回值 | 描述 |
| ------ | ---- |
| 0 | 正常 |
| 非0值 | 异常 |

### getVoipUserListV2()

**功能描述**  
获取已授权订阅微信用户列表  
单次最多获取10个用户，超过10个用户请指定 offset 并分多次获取  
当设备被分享给多个用户，且其他用户也授权订阅成功后，该接口可查询到所有授权订阅用户的
open_id，设备端可对某个指定用户发起呼叫

**函数原型**

```
int iv_avt_voip_get_user_list_v2(int32_t offset, int32_t limit, char openid_list[][VOIP_MAX_ID_LEN])
```

**参数说明**  
| 参数名称 | 类型 | 描述 | 输入/输出 |
| ----------- | ------------------------- | --------------------------------- | --------- |
| offset | int32_t | 偏移量 | 输出 |
| limit | int32_t | 单次获取用户数 | 输出 |
| openid_list | char (*)[VOIP_MAX_ID_LEN] | 用户openid列表，必须大于等于limit | 输出 |

**返回值**  
| 返回值 | 描述 |
| --------- | -------- |
| 大于等于0 | 用户总数 |
| 小于0 | 异常 |

### onUpdateAuthorizeStatus()

**功能描述**  
更新用户授权订阅状态通知，当用户在微信小程序中订阅此设备时，会通过此回调下发当前订阅用户的
open_id，设备端可使用该 open_id 呼叫对应的用户  
当设备被分享其他用户，且其他用户也授权订阅成功后，该接会收到其他用户的 open_id

**函数原型**

```
int (*iv_avt_voip_update_authorize_status_cb)(char *open_id, int status)
```

**参数说明**  
| 参数名称 | 类型 | 描述 | 输入/输出 |
| -------- | ------ | ------------------------ | --------- |
| open_id | char * | 订阅用户的open_id | 输入 |
| status | int | 0未授权订阅，1已授权订阅 | 输入 |

**返回值**  
| 返回值 | 描述 |
| ------ | ---- |
| 0 | 正常 |
| 非0值 | 异常 |

# 4. 数据结构

## 数据结构列表

**云存模块涉及以下数据结构**

* iv_cm_* 请参考音视频结构公共模块
* wxvoip_os_impl_t HAL接口
* voip_wxa_type_e 小程序版本
* voip_video_info_s voip呼叫的视频格式
* VoipActivateType 设备激活类型
* VoipActivateInfo 设备激活信息
* IvVoipCallback 回调函数

# 5. 注意事项

1. 本模块依赖av模块内的音视频传输接口，请按顺序初始化 initIvSystem(), initIvDm(), initIvAvt(),
   initWxCloudVoip()
2. 呼叫前前检查是否占线，占线时无法发起呼叫。如果本机已在通话中可以主动挂断后发起新的呼叫；如有其他用户占线请稍后重试
3. doWxCloudVoipCall() 呼叫成功后会触发 av 模块中的接口，详细内容请参考 av 模块文档
4. 当对方出现挂断、占线等情况时，onRecvCommand() 回调函数内会收到 IV_AVT_COMMAND_CALL_XXX
   的信令，用户可根据实际情况处理相关信令
5. 对方正常挂断设备端会先通过 onRecvCommand() 回调收到 IV_AVT_COMMAND_CALL_HANG_UP 信令，之后触发
   onStopRealPlay() 回调
6. 当网络异常导致挂断时设备端无法收到 IV_AVT_COMMAND_CALL_HANG_UP 信令，只会触发
   onStopRealPlay() 回调
