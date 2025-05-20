**目录**  

<!-- TOC -->

- [1. 功能介绍](#1-功能介绍)
- [2. 使用流程](#2-使用流程)
- [3. 接口参考](#3-接口参考)
  - [接口列表](#接口列表)
  - [接口描述](#接口描述)
    - [iv\_ota\_init](#iv_ota_init)
    - [iv\_ota\_exit](#iv_ota_exit)
    - [iv\_ota\_update\_progress](#iv_ota_update_progress)
    - [iv\_ota\_firmware\_update\_cb](#iv_ota_firmware_update_cb)
    - [iv\_ota\_prepare\_cb](#iv_ota_prepare_cb)
    - [iv\_ota\_download\_size\_cb](#iv_ota_download_size_cb)
- [4. 数据结构](#4-数据结构)
  - [数据结构列表](#数据结构列表)
  - [数据结构描述](#数据结构描述)
    - [iv\_ota\_progress\_type\_e](#iv_ota_progress_type_e)
    - [iv\_ota\_fail\_type\_e](#iv_ota_fail_type_e)
    - [iv\_ota\_init\_parm\_s](#iv_ota_init_parm_s)
- [5. 注意事项](#5-注意事项)

<!-- /TOC -->
--------

# 1. 功能介绍
本模块提供设备的OTA升级能力。


# 2. 使用流程
1. 在控制台上传固件，填写版本号等信息，并推送固件
2. 调用 `iv_ota_init` 后，SDK会启动单独的OTA任务线程，并上报当前设备内正在运行的固件版本号
3. 设备端发现有新版本固件后会调用`iv_ota_prepare_cb`通知用户，并告知新固件版本与大小，如果准备好升级请返回0，如未准备好或希望下次升级请返回非零值
4. 设备端SDK在用户准备好升级后开始下载固件，下载的过程中会调用`iv_ota_download_size_cb`告知用户已下载的固件大小
5. 当设备端SDK下载完固件并校验MD5正确之后，会通过回调 `iv_ota_firmware_update_cb` 通知用户，并告知固件保存地址及相关信息
6. 如仅下载固件，暂不升级，可调用 `iv_ota_update_progress` 上报 IV_OTA_PROGRESS_TYPE_FAIL 消息
7. 设备开始烧录前，请使用 `iv_ota_update_progress` 上报 IV_OTA_PROGRESS_TYPE_WRITE_FLASH 消息
8. 烧录完成后，重启之前，请使用 `iv_ota_update_progress` 上报 IV_OTA_PROGRESS_TYPE_REBOOT 消息
9.  重启完成且升级成功后，请使用 `iv_ota_update_progress` 上报 IV_OTA_PROGRESS_TYPE_SUCCESS 消息
10. 固件升级过程中如发生错误，可使用 `iv_ota_update_progress` 上报 IV_OTA_PROGRESS_TYPE_FAIL 消息
11. 当固件升级结束之后，或者发生异常需要退出的时候，请调用 `iv_ota_exit` 退出OTA模块，否则会循环上报版本并进行OTA过程

详细使用流程请参考例程代码。  


# 3. 接口参考

## 接口列表
**该功能模块提供以下接口**  
* iv_ota_init() OTA模块初始化
* iv_ota_exit() OTA模块销毁
* iv_ota_update_progress() OTA状态上报

**用户需注册以下回调函数**  
* (*iv_ota_firmware_update_cb)() OTA升级烧录通知回调
* (*iv_ota_prepare_cb)() OTA升级准备状态回调
* (*iv_ota_download_size_cb)() OTA已下载固件大小回调

## 接口描述

### iv_ota_init

**功能描述**  
OTA模块初始化，函数内部会执行查询固件版本等操作。  
该接口为阻塞接口，阻塞时长视网络情况而定。  

**函数原型**  
```
int iv_ota_init(iv_ota_init_parm_s *pstInitParm)
```

**参数说明**  
| 参数名称    | 类型                 | 描述          | 输入/输出 |
| ----------- | -------------------- | ------------- | --------- |
| pstInitParm | iv_ota_init_parm_s * | OTA初始化参数 | 输入      |

**返回值**  
| 返回值      | 描述                 |
| ----------- | -------------------- |
| IV_ERR_NONE | 成功                 |
| IV_ERR_*    | 失败，对应相应错误码 |


### iv_ota_exit

**功能描述**  
退出并销毁OTA模块。  

**函数原型**  
```
int iv_ota_exit(void)
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


### iv_ota_update_progress

**功能描述**  
上报OTA升级的进度。  
使用本接口可以向后台上报当前设备处于OTA的何种状态，例如烧录中、重启中、升级成功、升级失败。  

**函数原型**  
```
int iv_ota_update_progress(iv_ota_progress_type_e type, int progress_value)
```

**参数说明**  
| 参数名称       | 类型                   | 描述                                     | 输入/输出 |
| -------------- | ---------------------- | ---------------------------------------- | --------- |
| type           | iv_ota_progress_type_e | 当前的升级状态                           | 输入      |
| progress_value | int                    | 当前的升级状态的进度（仅预留，暂不支持） | 输入      |

**返回值**  
| 返回值      | 描述                 |
| ----------- | -------------------- |
| IV_ERR_NONE | 成功                 |
| IV_ERR_*    | 失败，对应相应错误码 |


### iv_ota_firmware_update_cb

**功能描述**  
当固件下载完成时，SDK使用此回调函数告知用户固件保存的路径和大小，之后用户可以开始进行固件升级。  

**函数原型**  
```
void (* iv_ota_firmware_update_cb)(char * firmware_name, uint32_t firmware_len);
```

**参数说明**  
| 参数名称      | 类型     | 描述         | 输入/输出 |
| ------------- | -------- | ------------ | --------- |
| firmware_name | char *   | 固件保存路径 | 输出      |
| firmware_len  | uint32_t | 固件大小     | 输出      |

**返回值**  
| 返回值 | 描述 |
| ------ | ---- |
| 无     | 无   |


### iv_ota_prepare_cb

**功能描述**  
OTA升级时，SDK使用此回调函数向用户查询是否准备好进行升级。  

**函数原型**  
```
int (* iv_ota_prepare_cb)(char *new_firmware_version, uint32_t new_firmware_size);
```

**参数说明**  
| 参数名称             | 类型     | 描述       | 输入/输出 |
| -------------------- | -------- | ---------- | --------- |
| new_firmware_version | char*    | 新固件名   | 输入      |
| new_firmware_size    | uint32_t | 新固件大小 | 输入      |


**返回值**  
| 返回值 | 描述             |
| ------ | ---------------- |
| 0      | 已准备好进行升级 |
| 非0    | 未准备好进行升级 |


### iv_ota_download_size_cb

**功能描述**  
OTA下载固件过程中，SDK使用此回调函数向用户告知已下载固件大小。  

**函数原型**  
```
void (* iv_ota_download_size_cb)(uint32_t size);
```

**参数说明**  
| 参数名称 | 类型     | 描述       | 输入/输出 |
| -------- | -------- | ---------- | --------- |
| size     | uint32_t | 已下载大小 | 输入      |


**返回值**  
| 返回值 | 描述 |
| ------ | ---- |
| 无     | 无   |


# 4. 数据结构

## 数据结构列表
**云存模块涉及以下数据结构**  
* iv_ota_progress_type_e OTA进度状态枚举
* iv_ota_fail_type_e OTA失败原因枚举
* iv_ota_init_parm_s OTA模块初始化参数


## 数据结构描述

### iv_ota_progress_type_e

**功能描述**  
OTA进度状态枚举  

**结构原型**  
```
typedef enum
{
    IV_OTA_PROGRESS_TYPE_WRITE_FLASH = 0,
    IV_OTA_PROGRESS_TYPE_REBOOT      = 1,
    IV_OTA_PROGRESS_TYPE_SUCCESS     = 2,
    IV_OTA_PROGRESS_TYPE_FAIL        = 3,
    IV_OTA_PROGRESS_TYPE_BUTT
}iv_ota_progress_type_e;
```

**参数说明**  
| 成员名称                         | 描述     | 取值 |
| -------------------------------- | -------- | ---- |
| IV_OTA_PROGRESS_TYPE_WRITE_FLASH | 烧录状态 | 0    |
| IV_OTA_PROGRESS_TYPE_REBOOT      | 重启状态 | 1    |
| IV_OTA_PROGRESS_TYPE_SUCCESS     | 升级成功 | 2    |
| IV_OTA_PROGRESS_TYPE_FAIL        | 升级失败 | 3    |
| IV_OTA_PROGRESS_TYPE_BUTT        | 枚举总数 | -    |


### iv_ota_fail_type_e

**功能描述**  
OTA失败原因枚举

**结构原型**  
```
typedef enum
{
    IV_OTA_FAIL_TYPE_OPEN_FILE   = 0,
    IV_OTA_FAIL_TYPE_WRONG_SIZE  = 1,
    IV_OTA_FAIL_TYPE_WRITE_FLASH = 2,
    IV_OTA_FAIL_TYPE_WRITE_BUTT
}iv_ota_fail_type_e;
```

**参数说明**  
| 成员名称                     | 描述         | 取值 |
| ---------------------------- | ------------ | ---- |
| IV_OTA_FAIL_TYPE_OPEN_FILE   | 打开文件失败 | 0    |
| IV_OTA_FAIL_TYPE_WRONG_SIZE  | 固件大小错误 | 1    |
| IV_OTA_FAIL_TYPE_WRITE_FLASH | 固件烧录错误 | 2    |
| IV_OTA_FAIL_TYPE_WRITE_BUTT  | 枚举总数     | -    |


### iv_ota_init_parm_s

**功能描述**  
OTA模块初始化参数

**结构原型**  
```
typedef struct iv_ota_init_parm_s
{
    void (* iv_ota_firmware_update_cb)(char * firmware_name, uint32_t firmware_len);
    char firmware_path[IV_OTA_LOCAL_FILE_NAME_MAX_LEN];
    char firmware_version[IV_OTA_FIRMWARE_VERSION_MAX_LEN];
    int (* iv_ota_prepare_cb)(char *new_firmware_version, uint32_t new_firmware_size);
    void (* iv_ota_download_size_cb)(uint32_t size);
}iv_ota_init_parm_s;
```

**参数说明**  
| 成员名称                  | 描述                                                       | 取值 |
| ------------------------- | ---------------------------------------------------------- | ---- |
| iv_ota_firmware_update_cb | OTA升级烧录通知回调                                        | -    |
| firmware_path             | 保存固件的路径，不含文件名，该路径下需要足够的空间         | -    |
| firmware_version          | 设备当前正在运行的固件版本号，该版本号会上报至后台以便管理 | -    |
| iv_ota_prepare_cb         | OTA升级准备状态回调                                        | -    |
| iv_ota_download_size_cb   | OTA已下载固件大小回调                                      | -    |


# 5. 注意事项
1. SDK会在用户提供的固件下载目录下面创建包含固件版本号的固件文件，某些平台对路径及文件名的长度有限制，在云端控制台创建固件版本号不宜过长。
2. OTA功能支持断点续传，OTA模块初始化时若存在未下载完的固件则会继续下载。
3. SDK会在用户提供的固件下载路径下创建配置文件保存OTA进度，如果固件下载路径下除了固件文件还存在一个文本文件，则说明固件下载还未完成，在固件下载完成并校验无误之后，SDK会自动删除文本描述文件。
