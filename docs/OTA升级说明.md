# OTA升级说明

**目录**

<!-- TOC -->
- [1. 功能介绍](#1-功能介绍)
- [2. 使用流程](#2-使用流程)
- [3. 接口参考](#3-接口参考)
    - [接口列表](#接口列表)
    - [接口描述](#接口描述)
        - [initOTA](#initOTA)
        - [exitOTA](#exitOTA)
        - [updateOTAProgress](#updateOTAProgress)
        - [onFirmwareUpdate](#onFirmwareUpdate)
        - [onOTAPrepare](#onOTAPrepare)
        - [onDownloadSize](#onDownloadSize)
- [4. 数据结构](#4-数据结构)
    - [数据结构列表](#数据结构列表)
    - [数据结构描述](#数据结构描述)
        - [OTAProgressType](#OTAProgressType)
        - [OTAFailType](#OTAFailType)
- [5. 注意事项](#5-注意事项)

<!-- /TOC -->
--------

# 1. 功能介绍

本模块提供设备的OTA升级能力。


# 2. 使用流程
1. 在控制台上传固件，填写版本号等信息，并推送固件
2. 调用 `initOTA` 后，SDK会启动单独的OTA任务线程，并上报当前设备内正在运行的固件版本号
3. 设备端SDK在收到固件升级的消息后开始下载固件，下载的过程中设备 SDK 会不断的上报下载进度
4. 当设备端SDK下载完固件并校验MD5正确之后，会通过`IvOTACallback`接口回调 `onFirmwareUpdate` 通知用户，并告知固件地址及相关信息
5. 回调 `onFirmwareUpdate` 的文件名包含固件的版本号，用户可以据此来检查是否需要升级
6. 如不需升级，可调用 `updateOTAProgress` 上报 IV_OTA_PROGRESS_TYPE_FAIL 消息
7. 设备开始烧录前，请使用 `updateOTAProgress` 上报 IV_OTA_PROGRESS_TYPE_WRITE_FLASH 消息
8. 烧录完成后，重启之前，请使用 `updateOTAProgress` 上报 IV_OTA_PROGRESS_TYPE_REBOOT 消息
9. 重启完成后，请使用 `updateOTAProgress` 上报 IV_OTA_PROGRESS_TYPE_SUCCESS 消息
10. 固件升级过程中如发生错误，可使用 `updateOTAProgress` 上报 IV_OTA_PROGRESS_TYPE_FAIL 消息
11. 当固件升级结束之后，或者发生异常需要退出的时候，请调用 `exitOTA` 退出OTA模块，否则会循环上报版本并进行OTA过程

详细使用流程请参考例程代码。

# 3. 接口参考

## 接口列表
**该功能模块提供以下接口**
* initOTA() OTA模块初始化
* exitOTA() OTA模块销毁
* updateOTAProgress() OTA状态上报

**用户需注册以下回调函数**
onFirmwareUpdate() OTA升级烧录通知回调 
onOTAPrepare() OTA升级准备状态回调

## 接口描述

### initOTA

**功能描述**  
OTA模块初始化，函数内部会执行查询固件版本等操作。  
该接口为阻塞接口，阻塞时长视网络情况而定。

**函数原型**
```
int initOTA(String firmwarePath, String firmwareVersion, IvOTACallback ivOTACallback)
```

**参数说明**  
| 参数名称    | 类型                 | 描述          | 输入/输出 |
| ----------- | -------------------- | ------------- | --------- |
| firmwarePath | String | 保存固件的路径，不含文件名，该路径下需要足够的空间 | 输入      |
| firmwareVersion | String | 设备当前正在运行的固件版本号，该版本号会上报至后台以便管理 | 输入      |
| ivOTACallback | IvOTACallback | OTA升级回调 | 输入      |

**返回值**  
| 返回值      | 描述                 |
| ----------- | -------------------- |
| IV_ERR_NONE | 成功                 |
| IV_ERR_*    | 失败，对应相应错误码 |


### exitOTA

**功能描述**  
退出并销毁OTA模块。

**函数原型**
```
int exitOTA()
```

**参数说明**  
| 参数名称 | 类型 | 描述 | 输入/输出 |
| -------- | ---- | ---- | --------- |
| 无       | 无   | 无   | 无        |

**返回值**  
| 返回值      | 描述                 |
| ----------- | -------------------- |
| IV_ERR_NONE | 成功                 |
| IV_ERR_*    | 失败，对应相应错误码 |


### updateOTAProgress

**功能描述**  
上报OTA升级的进度。  
使用本接口可以向后台上报当前设备处于OTA的何种状态，例如烧录中、重启中、升级成功、升级失败。

**函数原型**
```
int updateOTAProgress(@OTAProgressType int progressType, int progress)
```

**参数说明**  
| 参数名称       | 类型                   | 描述                                     | 输入/输出 |
| -------------- | ---------------------- | ---------------------------------------- | --------- |
| progressType           | int | 当前的升级状态                           | 输入      |
| progress | int                    | 当前的升级状态的进度（仅预留，暂不支持） | 输入      |

**返回值**  
| 返回值      | 描述                 |
| ----------- | -------------------- |
| IV_ERR_NONE | 成功                 |
| IV_ERR_*    | 失败，对应相应错误码 |


### onFirmwareUpdate

**功能描述**  
当固件下载完成时，SDK使用此回调函数告知用户固件保存的路径和大小，之后用户可以开始进行固件升级。

**函数原型**
```
void onFirmwareUpdate(String firmwareName, int firmwareLen);
```

**参数说明**  
| 参数名称      | 类型     | 描述         | 输入/输出 |
| ------------- | -------- | ------------ | --------- |
| firmware_name | String   | 固件保存路径 | 输出      |
| firmware_len  | int | 固件大小     | 输出      |

**返回值**  
| 返回值 | 描述 |
| ------ | ---- |
| 无     | 无   |


### onOTAPrepare

**功能描述**  
OTA升级时，SDK使用此回调函数向用户查询是否准备好进行升级。

**函数原型**
```
int onOTAPrepare();
```

**参数说明**  
| 参数名称 | 类型 | 描述 | 输入/输出 |
| -------- | ---- | ---- | --------- |
| newFirmwareVersion       | String   | 查询到新固件的版本号   | 无        |
| newFirmwareSize       | int   | 查询到新固件的版本大小   | 无        |

**返回值**  
| 返回值 | 描述             |
| ------ | ---------------- |
| 0      | 已准备好进行升级 |
| 非0    | 未准备好进行升级 |

### onDownloadSize

**功能描述**  
OTA升级时，SDK使用此回调函数向用户输出当前下载固件大小。

**函数原型**
```
void onDownloadSize();
```

**参数说明**  
| 参数名称 | 类型 | 描述 | 输入/输出 |
| -------- | ---- | ---- | --------- |
| size       | int   | 下载固件进度大小   | 无        |

**返回值**  
| 返回值 | 描述             |
| ------ | ---------------- |
| 无      | 无 |

# 4. 数据结构

## 数据结构列表
**云存模块涉及以下数据结构**
* OTAProgressType OTA进度状态枚举
* OTAFailType OTA失败原因枚举


## 数据结构描述

### OTAProgressType

**功能描述**  
OTA进度状态枚举

**参数说明**  
| 成员名称                         | 描述     | 取值 |
| -------------------------------- | -------- | ---- |
| IV_OTA_PROGRESS_TYPE_WRITE_FLASH | 烧录状态 | 0    |
| IV_OTA_PROGRESS_TYPE_REBOOT      | 重启状态 | 1    |
| IV_OTA_PROGRESS_TYPE_SUCCESS     | 升级成功 | 2    |
| IV_OTA_PROGRESS_TYPE_FAIL        | 升级失败 | 3    |
| IV_OTA_PROGRESS_TYPE_BUTT        | 枚举总数 | -    |


### OTAFailType

**功能描述**  
OTA失败原因枚举

**参数说明**  
| 成员名称                     | 描述         | 取值 |
| ---------------------------- | ------------ | ---- |
| IV_OTA_FAIL_TYPE_OPEN_FILE   | 打开文件失败 | 0    |
| IV_OTA_FAIL_TYPE_WRONG_SIZE  | 固件大小错误 | 1    |
| IV_OTA_FAIL_TYPE_WRITE_FLASH | 固件烧录错误 | 2    |
| IV_OTA_FAIL_TYPE_WRITE_BUTT  | 枚举总数     | -    |


# 5. 注意事项
1. SDK会在用户提供的固件下载目录下面创建包含固件版本号的固件文件，某些平台对路径及文件名的长度有限制，在云端控制台创建固件版本号不宜过长。
2. OTA功能支持断点续传，OTA模块初始化时若存在未下载完的固件则会继续下载。
3. SDK会在用户提供的固件下载路径下创建配置文件保存OTA进度，如果固件下载路径下除了固件文件还存在一个文本文件，则说明固件下载还未完成，在固件下载完成并校验无误之后，SDK会自动删除文本描述文件。