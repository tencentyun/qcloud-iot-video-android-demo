# OTA升级功能接入指南 (Java/Android 版)

**目录**

<!-- TOC -->
- [1. 功能介绍](#1-功能介绍)
- [2. 快速开始](#2-快速开始)
  - [2.1 前置条件](#21-前置条件)
  - [2.2 基本流程](#22-基本流程)
- [3. 接口说明](#3-接口说明)
  - [3.1 初始化接口](#31-初始化接口)
  - [3.2 状态上报接口](#32-状态上报接口)
  - [3.3 退出接口](#33-退出接口)
  - [3.4 回调接口](#34-回调接口)
- [4. 完整示例](#4-完整示例)
  - [4.1 Activity 实现](#41-activity-实现)
  - [4.2 关键代码解析](#42-关键代码解析)
- [5. 数据结构](#5-数据结构)
  - [5.1 OTAProgressType](#51-otaprogresstype)
  - [5.2 OTAFailType](#52-otafailtype)
- [6. 最佳实践](#6-最佳实践)
- [7. 常见问题](#7-常见问题)
- [8. 注意事项](#8-注意事项)

<!-- /TOC -->

---

## 1. 功能介绍

OTA升级模块提供设备固件的远程升级能力，支持：

- ✅ 固件版本管理和上报
- ✅ 固件自动下载和校验
- ✅ 下载进度实时回调
- ✅ 断点续传
- ✅ 升级状态上报
- ✅ 灵活的升级时机控制

---

## 2. 快速开始

### 2.1 前置条件

1. **设备已上线**：确保设备已成功初始化并上线
2. **控制台配置**：在腾讯云 IoT Video 控制台上传固件并推送
3. **存储空间**：设备有足够的存储空间保存固件文件
4. **权限配置**：Android 应用需要文件读写权限

### 2.2 基本流程

```
┌─────────────────────────────────────────────────────────────┐
│                      OTA 升级流程                            │
└─────────────────────────────────────────────────────────────┘

1. 控制台上传固件
   ↓
2. 调用 initOTA() 初始化
   ↓
3. SDK 上报当前固件版本
   ↓
4. 收到升级消息，SDK 开始下载固件
   ↓
5. 下载过程中回调 onDownloadSize() 上报进度
   ↓
6. 下载完成，回调 onFirmwareUpdate() 通知固件路径
   ↓
7. 检查固件版本和大小
   ↓
8. 上报 IV_OTA_PROGRESS_TYPE_WRITE_FLASH 开始烧录
   ↓
9. 执行固件烧录
   ↓
10. 上报 IV_OTA_PROGRESS_TYPE_REBOOT 准备重启
   ↓
11. 设备重启
   ↓
12. 重启后上报 IV_OTA_PROGRESS_TYPE_SUCCESS 升级成功
   ↓
13. 调用 exitOTA() 退出 OTA 模块
```

---

## 3. 接口说明

### 3.1 初始化接口

#### initOTA

**功能描述**

初始化 OTA 模块，SDK 会启动独立的 OTA 任务线程，上报当前固件版本并查询是否有新固件。

**⚠️ 注意**：该接口为阻塞接口，建议在子线程中调用。

**方法签名**

```java
public native int initOTA(
    String firmwarePath, 
    String firmwareVersion, 
    IvOTACallback ivOTACallback
);
```

**参数说明**

| 参数名称 | 类型 | 描述 | 是否必填 |
|---------|------|------|---------|
| firmwarePath | String | 固件保存路径（不含文件名），需要足够的存储空间，长度不超过 128 字节 | 是 |
| firmwareVersion | String | 设备当前运行的固件版本号，长度不超过 32 字节 | 是 |
| ivOTACallback | IvOTACallback | OTA 升级回调接口 | 是 |

**返回值**

| 返回值 | 描述 |
|--------|------|
| 0 (IV_ERR_NONE) | 成功 |
| 非 0 | 失败，对应相应错误码 |

**示例代码**

```java
// 在子线程中调用
checkDefaultThreadActiveAndExecuteTask(() -> {
    int result = VideoNativeInterface.getInstance().initOTA(
        "/sdcard/temp",           // 固件保存路径
        "3.0.0",                  // 当前固件版本
        this                      // 回调接口
    );
    
    if (result == 0) {
        Log.d(TAG, "OTA 初始化成功");
    } else {
        Log.e(TAG, "OTA 初始化失败，错误码: " + result);
    }
});
```

---

### 3.2 状态上报接口

#### updateOTAProgress

**功能描述**

向云端上报 OTA 升级的当前状态，如烧录中、重启中、升级成功、升级失败等。

**方法签名**

```java
public native int updateOTAProgress(
    @OTAProgressType int progressType, 
    int progress
);
```

**参数说明**

| 参数名称 | 类型 | 描述 | 是否必填 |
|---------|------|------|---------|
| progressType | int | 当前的升级状态，详见 [OTAProgressType](#51-otaprogresstype) | 是 |
| progress | int | 当前的升级进度（预留参数，暂不支持，传 0 即可） | 是 |

**返回值**

| 返回值 | 描述 |
|--------|------|
| 0 (IV_ERR_NONE) | 成功 |
| 非 0 | 失败，对应相应错误码 |

**使用场景**

```java
// 场景 1: 开始烧录固件
VideoNativeInterface.getInstance().updateOTAProgress(
    OTAProgressType.IV_OTA_PROGRESS_TYPE_WRITE_FLASH, 
    0
);

// 场景 2: 准备重启
VideoNativeInterface.getInstance().updateOTAProgress(
    OTAProgressType.IV_OTA_PROGRESS_TYPE_REBOOT, 
    0
);

// 场景 3: 升级成功
VideoNativeInterface.getInstance().updateOTAProgress(
    OTAProgressType.IV_OTA_PROGRESS_TYPE_SUCCESS, 
    0
);

// 场景 4: 升级失败
VideoNativeInterface.getInstance().updateOTAProgress(
    OTAProgressType.IV_OTA_PROGRESS_TYPE_FAIL,
    OTAFailType.IV_OTA_FAIL_TYPE_OPEN_FILE  // 失败原因
);
```

---

### 3.3 退出接口

#### exitOTA

**功能描述**

退出并销毁 OTA 模块，释放资源。

**⚠️ 重要**：升级结束或发生异常时必须调用此接口，否则会循环上报版本并进行 OTA 过程。

**方法签名**

```java
public native int exitOTA();
```

**返回值**

| 返回值 | 描述 |
|--------|------|
| 0 (IV_ERR_NONE) | 成功 |
| 非 0 | 失败，对应相应错误码 |

**示例代码**

```java
@Override
protected void onDestroy() {
    checkDefaultThreadActiveAndExecuteTask(() -> {
        VideoNativeInterface.getInstance().exitOTA();
    });
    super.onDestroy();
}
```

---

### 3.4 回调接口

#### IvOTACallback 接口

OTA 模块需要实现 `IvOTACallback` 接口来接收 SDK 的回调通知。

```java
public interface IvOTACallback {
    /**
     * 固件下载完成回调
     */
    void onFirmwareUpdate(String firmwareName, int firmwareLen);
    
    /**
     * OTA 升级准备状态回调
     */
    int onPrepare(String newFirmwareVersion, int newFirmwareSize);
    
    /**
     * 固件下载进度回调
     */
    void onDownloadSize(int size);
    
    /**
     * OTA 线程退出回调
     */
    void onOtaThreadExit(int mqttOnline);
}
```

---

#### onFirmwareUpdate

**功能描述**

当固件下载完成并校验通过后，SDK 通过此回调告知固件保存路径和大小，用户可以开始进行固件升级。

**方法签名**

```java
void onFirmwareUpdate(String firmwareName, int firmwareLen);
```

**参数说明**

| 参数名称 | 类型 | 描述 |
|---------|------|------|
| firmwareName | String | 固件保存的完整路径（包含文件名） |
| firmwareLen | int | 固件文件大小（字节） |

**示例实现**

```java
@Override
public void onFirmwareUpdate(String firmwareName, int firmwareLen) {
    Log.d(TAG, "固件下载完成: " + firmwareName + ", 大小: " + firmwareLen);
    
    FileInputStream fileInputStream = null;
    File firmwareFile = new File(firmwareName);
    
    try {
        // 1. 检查文件是否存在
        if (!firmwareFile.exists()) {
            Log.e(TAG, "固件文件不存在: " + firmwareName);
            VideoNativeInterface.getInstance().updateOTAProgress(
                OTAProgressType.IV_OTA_PROGRESS_TYPE_FAIL,
                OTAFailType.IV_OTA_FAIL_TYPE_OPEN_FILE
            );
            exitOTAUpgrade();
            return;
        }
        
        // 2. 校验文件大小
        fileInputStream = new FileInputStream(firmwareFile);
        long fileLength = firmwareFile.length();
        
        if (firmwareLen != fileLength) {
            Log.e(TAG, "固件大小不匹配，期望: " + firmwareLen + ", 实际: " + fileLength);
            VideoNativeInterface.getInstance().updateOTAProgress(
                OTAProgressType.IV_OTA_PROGRESS_TYPE_FAIL,
                OTAFailType.IV_OTA_FAIL_TYPE_WRONG_SIZE
            );
            exitOTAUpgrade();
            return;
        }
        
        // 3. 上报开始烧录
        VideoNativeInterface.getInstance().updateOTAProgress(
            OTAProgressType.IV_OTA_PROGRESS_TYPE_WRITE_FLASH, 
            0
        );
        
        // 4. 执行固件烧录（此处为示例，实际需要根据硬件实现）
        performFirmwareUpgrade(fileInputStream, firmwareLen);
        
        // 5. 烧录完成，上报成功
        VideoNativeInterface.getInstance().updateOTAProgress(
            OTAProgressType.IV_OTA_PROGRESS_TYPE_SUCCESS, 
            0
        );
        
        // 6. 退出 OTA 模块
        exitOTAUpgrade();
        
    } catch (Exception e) {
        Log.e(TAG, "固件升级异常", e);
        VideoNativeInterface.getInstance().updateOTAProgress(
            OTAProgressType.IV_OTA_PROGRESS_TYPE_FAIL,
            OTAFailType.IV_OTA_FAIL_TYPE_WRITE_FLASH
        );
        exitOTAUpgrade();
    } finally {
        if (fileInputStream != null) {
            try {
                fileInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
```

---

#### onPrepare

**功能描述**

SDK 查询到新固件后，通过此回调询问设备是否准备好进行升级。

**方法签名**

```java
int onPrepare(String newFirmwareVersion, int newFirmwareSize);
```

**参数说明**

| 参数名称 | 类型 | 描述 |
|---------|------|------|
| newFirmwareVersion | String | 新固件的版本号 |
| newFirmwareSize | int | 新固件的大小（字节） |

**返回值**

| 返回值 | 描述 |
|--------|------|
| 0 | 已准备好，开始下载固件 |
| 非 0 | 未准备好，暂不下载 |

**示例实现**

```java
private boolean isUpgrade = false;  // 升级标志

@Override
public int onPrepare(String newFirmwareVersion, int newFirmwareSize) {
    Log.d(TAG, "发现新固件: " + newFirmwareVersion + ", 大小: " + newFirmwareSize);
    
    // 更新 UI 显示新固件信息
    runOnUiThread(() -> {
        binding.tvNewFirmware.setText(
            "新固件版本: " + newFirmwareVersion + 
            "\n大小: " + (newFirmwareSize / 1024) + " KB"
        );
        binding.btnUpgrade.setEnabled(!isUpgrade);
    });
    
    // 返回 0 表示准备好升级，返回非 0 表示暂不升级
    return isUpgrade ? 0 : 1;
}
```

---

#### onDownloadSize

**功能描述**

固件下载过程中，SDK 通过此回调实时上报已下载的大小。

**方法签名**

```java
void onDownloadSize(int size);
```

**参数说明**

| 参数名称 | 类型 | 描述 |
|---------|------|------|
| size | int | 当前已下载的固件大小（字节） |

**示例实现**

```java
private int newFirmwareSize = 0;  // 新固件总大小

@Override
public void onDownloadSize(int size) {
    Log.d(TAG, "下载进度: " + size + " / " + newFirmwareSize);
    
    // 计算下载百分比
    double progress = (size * 100.0) / newFirmwareSize;
    
    // 更新 UI 进度条
    runOnUiThread(() -> {
        binding.progressBar.setProgress((int) progress);
        binding.tvProgress.setText(
            String.format("下载中 %.2f%%", progress)
        );
    });
}
```

---

#### onOtaThreadExit

**功能描述**

OTA 线程退出时的回调通知。

**方法签名**

```java
void onOtaThreadExit(int mqttOnline);
```

**参数说明**

| 参数名称 | 类型 | 描述 |
|---------|------|------|
| mqttOnline | int | MQTT 在线状态 |

**示例实现**

```java
@Override
public void onOtaThreadExit(int mqttOnline) {
    Log.d(TAG, "OTA 线程退出，MQTT 状态: " + mqttOnline);
}
```

---

## 4. 完整示例

### 4.1 Activity 实现

以下是一个完整的 OTA 升级 Activity 实现示例：

---

### 4.2 关键代码解析

#### 1. 初始化时机

```java
@Override
public void onOnline(long netDateTime) {
    super.onOnline(netDateTime);
    // 设备上线后才初始化 OTA
    checkForNewFirmware();
}
```

**说明**：必须在设备上线后才能初始化 OTA 模块。

---

#### 2. 子线程调用

```java
checkDefaultThreadActiveAndExecuteTask(() -> {
    VideoNativeInterface.getInstance().initOTA(
        OTA_FIRMWARE_PATH,
        OTA_FIRMWARE_VERSION,
        this
    );
});
```

**说明**：`initOTA` 是阻塞接口，必须在子线程中调用。

---

#### 3. 文件校验

```java
// 检查文件存在
if (!firmwareFile.exists()) {
    // 上报失败
    VideoNativeInterface.getInstance().updateOTAProgress(
        OTAProgressType.IV_OTA_PROGRESS_TYPE_FAIL,
        OTAFailType.IV_OTA_FAIL_TYPE_OPEN_FILE
    );
    return;
}

// 校验文件大小
if (firmwareLen != fileLength) {
    // 上报失败
    VideoNativeInterface.getInstance().updateOTAProgress(
        OTAProgressType.IV_OTA_PROGRESS_TYPE_FAIL,
        OTAFailType.IV_OTA_FAIL_TYPE_WRONG_SIZE
    );
    return;
}
```

**说明**：在烧录前必须校验固件文件的存在性和大小。

---

#### 4. 状态上报流程

```java
// 1. 开始烧录
VideoNativeInterface.getInstance().updateOTAProgress(
    OTAProgressType.IV_OTA_PROGRESS_TYPE_WRITE_FLASH, 0
);

// 2. 执行烧录
performFirmwareUpgrade();

// 3. 烧录成功
VideoNativeInterface.getInstance().updateOTAProgress(
    OTAProgressType.IV_OTA_PROGRESS_TYPE_SUCCESS, 0
);

// 4. 退出 OTA
exitOTAUpgrade();
```

**说明**：按照标准流程上报状态，确保云端能正确跟踪升级进度。

---

## 5. 数据结构

### 5.1 OTAProgressType

**功能描述**

OTA 进度状态枚举，用于上报当前升级状态。

**枚举值**

| 枚举名称 | 值 | 描述 | 使用场景 |
|---------|---|------|---------|
| IV_OTA_PROGRESS_TYPE_WRITE_FLASH | 0 | 烧录状态 | 开始烧录固件前上报 |
| IV_OTA_PROGRESS_TYPE_REBOOT | 1 | 重启状态 | 准备重启设备前上报 |
| IV_OTA_PROGRESS_TYPE_SUCCESS | 2 | 升级成功 | 固件烧录成功后上报 |
| IV_OTA_PROGRESS_TYPE_FAIL | 3 | 升级失败 | 升级过程中发生错误时上报 |

**使用示例**

```java
// 开始烧录
VideoNativeInterface.getInstance().updateOTAProgress(
    OTAProgressType.IV_OTA_PROGRESS_TYPE_WRITE_FLASH, 0
);

// 准备重启
VideoNativeInterface.getInstance().updateOTAProgress(
    OTAProgressType.IV_OTA_PROGRESS_TYPE_REBOOT, 0
);

// 升级成功
VideoNativeInterface.getInstance().updateOTAProgress(
    OTAProgressType.IV_OTA_PROGRESS_TYPE_SUCCESS, 0
);

// 升级失败
VideoNativeInterface.getInstance().updateOTAProgress(
    OTAProgressType.IV_OTA_PROGRESS_TYPE_FAIL,
    OTAFailType.IV_OTA_FAIL_TYPE_WRITE_FLASH
);
```

---

### 5.2 OTAFailType

**功能描述**

OTA 失败原因枚举，当升级失败时用于指明具体原因。

**枚举值**

| 枚举名称 | 值 | 描述 | 使用场景 |
|---------|---|------|---------|
| IV_OTA_FAIL_TYPE_OPEN_FILE | 0 | 打开文件失败 | 固件文件不存在或无法打开 |
| IV_OTA_FAIL_TYPE_WRONG_SIZE | 1 | 固件大小错误 | 下载的固件大小与预期不符 |
| IV_OTA_FAIL_TYPE_WRITE_FLASH | 2 | 固件烧录错误 | 烧录过程中发生错误 |

**使用示例**

```java
// 文件打开失败
VideoNativeInterface.getInstance().updateOTAProgress(
    OTAProgressType.IV_OTA_PROGRESS_TYPE_FAIL,
    OTAFailType.IV_OTA_FAIL_TYPE_OPEN_FILE
);

// 文件大小错误
VideoNativeInterface.getInstance().updateOTAProgress(
    OTAProgressType.IV_OTA_PROGRESS_TYPE_FAIL,
    OTAFailType.IV_OTA_FAIL_TYPE_WRONG_SIZE
);

// 烧录失败
VideoNativeInterface.getInstance().updateOTAProgress(
    OTAProgressType.IV_OTA_PROGRESS_TYPE_FAIL,
    OTAFailType.IV_OTA_FAIL_TYPE_WRITE_FLASH
);
```

---

## 6. 最佳实践

### 6.1 线程管理

```java
// ✅ 推荐：在子线程中调用阻塞接口
checkDefaultThreadActiveAndExecuteTask(() -> {
    VideoNativeInterface.getInstance().initOTA(path, version, callback);
});

// ❌ 不推荐：在主线程中调用
VideoNativeInterface.getInstance().initOTA(path, version, callback);
```

---

### 6.2 错误处理

```java
@Override
public void onFirmwareUpdate(String firmwareName, int firmwareLen) {
    try {
        // 升级逻辑
        performUpgrade(firmwareName, firmwareLen);
        
        // 成功上报
        VideoNativeInterface.getInstance().updateOTAProgress(
            OTAProgressType.IV_OTA_PROGRESS_TYPE_SUCCESS, 0
        );
        
    } catch (Exception e) {
        Log.e(TAG, "升级失败", e);
        
        // 失败上报
        VideoNativeInterface.getInstance().updateOTAProgress(
            OTAProgressType.IV_OTA_PROGRESS_TYPE_FAIL,
            OTAFailType.IV_OTA_FAIL_TYPE_WRITE_FLASH
        );
        
    } finally {
        // 确保退出 OTA
        exitOTAUpgrade();
    }
}
```
---

### 6.3 资源释放

```java
@Override
protected void onDestroy() {
    // 确保在 Activity 销毁时退出 OTA
    checkDefaultThreadActiveAndExecuteTask(() -> {
        VideoNativeInterface.getInstance().exitOTA();
    });
    super.onDestroy();
}
```
---

## 7. 常见问题

### Q1: initOTA 调用后没有收到 onPrepare 回调？

**原因**：
- 设备未上线
- 控制台未推送固件
- 当前版本已是最新版本

**解决方案**：
1. 确保设备已上线（`isOnline == true`）
2. 在控制台检查固件是否已推送
3. 确认控制台固件版本号高于当前版本

---

### Q2: onFirmwareUpdate 回调中文件不存在？

**原因**：
- 存储路径没有写权限
- 存储空间不足
- 下载过程中断

**解决方案**：
```java
// 1. 检查并创建目录
File dir = new File(OTA_FIRMWARE_PATH);
if (!dir.exists()) {
    boolean created = dir.mkdirs();
    Log.d(TAG, "创建目录: " + created);
}

// 2. 检查存储空间
StatFs stat = new StatFs(OTA_FIRMWARE_PATH);
long availableBytes = stat.getAvailableBytes();
Log.d(TAG, "可用空间: " + availableBytes);

// 3. 检查权限
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
    if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        != PackageManager.PERMISSION_GRANTED) {
        requestPermissions(
            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 
            REQUEST_CODE
        );
    }
}
```

---

### Q3: 固件下载很慢或中断？

**原因**：
- 网络不稳定
- 固件文件过大

**解决方案**：
- SDK 支持断点续传，重新初始化 OTA 会继续下载
- 检查网络连接状态
- 在网络良好时进行升级

---

### Q4: 升级后版本号没有更新？

**原因**：
- 烧录后未重启设备
- 未上报 SUCCESS 状态
- 固件版本号未更新

**解决方案**：
```java
// 1. 烧录成功后上报
VideoNativeInterface.getInstance().updateOTAProgress(
    OTAProgressType.IV_OTA_PROGRESS_TYPE_SUCCESS, 0
);

// 2. 重启设备
VideoNativeInterface.getInstance().updateOTAProgress(
    OTAProgressType.IV_OTA_PROGRESS_TYPE_REBOOT, 0
);

// 3. 重启后使用新版本号初始化
VideoNativeInterface.getInstance().initOTA(
    path, 
    "3.1.0",  // 新版本号
    callback
);
```

---

### Q5: 如何取消正在进行的升级？

**解决方案**：
```java
// 调用 exitOTA 会停止下载和升级过程
VideoNativeInterface.getInstance().exitOTA();

// 如果需要清理已下载的固件文件
File firmwareDir = new File(OTA_FIRMWARE_PATH);
if (firmwareDir.exists()) {
    File[] files = firmwareDir.listFiles();
    if (files != null) {
        for (File file : files) {
            file.delete();
        }
    }
}
```

---

## 8. 注意事项

### ⚠️ 重要提示

1. **版本号长度限制**
   - 固件路径长度不超过 **128 字节**
   - 固件版本号长度不超过 **32 字节**
   - 某些平台对路径和文件名长度有限制，版本号不宜过长

2. **断点续传**
   - SDK 支持断点续传功能
   - OTA 初始化时若存在未下载完的固件会继续下载
   - SDK 会在固件路径下创建配置文件保存进度
   - 如果路径下存在文本文件，说明固件下载未完成
   - 固件下载完成并校验无误后，SDK 会自动删除配置文件

3. **文件管理**
   - SDK 会在固件路径下创建包含版本号的固件文件
   - 文件名格式：`firmware_<version>.bin`
   - 升级完成后建议清理旧固件文件

4. **线程安全**
   - `initOTA` 是阻塞接口，必须在子线程调用
   - 回调函数可能在子线程中执行，UI 更新需切换到主线程
   - `exitOTA` 建议在子线程中调用

5. **生命周期管理**
   - 必须在设备上线后才能初始化 OTA
   - Activity 销毁时必须调用 `exitOTA`
   - 升级结束或异常时必须调用 `exitOTA`，否则会循环上报版本

6. **状态上报顺序**
   ```
   WRITE_FLASH → (烧录中) → REBOOT → (重启) → SUCCESS
   ```
   或
   ```
   WRITE_FLASH → (烧录失败) → FAIL
   ```

7. **错误处理**
   - 所有异常情况都应上报 `FAIL` 状态
   - 上报失败时需指定具体的 `OTAFailType`
   - 失败后必须调用 `exitOTA`

8. **存储空间**
   - 确保固件路径有足够的存储空间
   - 建议预留固件大小 2 倍以上的空间
   - 下载前检查可用空间

9. **权限要求**
   - Android 6.0+ 需要动态申请存储权限
   - 确保应用有文件读写权限
   - 建议使用应用私有目录避免权限问题

10. **版本管理**
    - 使用语义化版本号
    - 版本号必须递增
    - 控制台固件版本必须高于设备当前版本

---

## 附录

### A. 完整的状态流转图

```
                    ┌─────────────┐
                    │  控制台推送  │
                    │   新固件     │
                    └──────┬──────┘
                           ↓
                    ┌─────────────┐
                    │  initOTA()  │
                    │  初始化模块  │
                    └──────┬──────┘
                           ↓
                    ┌─────────────┐
                    │ onPrepare() │
                    │  查询准备    │
                    └──────┬──────┘
                           ↓
                    ┌─────────────┐
                    │  开始下载    │
                    │   固件文件   │
                    └──────┬──────┘
                           ↓
                ┌──────────┴──────────┐
                │  onDownloadSize()   │
                │   实时上报进度       │
                └──────────┬──────────┘
                           ↓
                ┌──────────────────────┐
                │ onFirmwareUpdate()   │
                │   下载完成通知       │
                └──────────┬───────────┘
                           ↓
                ┌──────────────────────┐
                │  校验文件存在性和大小 │
                └──────────┬───────────┘
                           ↓
                ┌──────────────────────┐
                │ WRITE_FLASH 状态上报 │
                └──────────┬───────────┘
                           ↓
                ┌──────────────────────┐
                │   执行固件烧录       │
                └──────────┬───────────┘
                           ↓
                ┌──────────┴───────────┐
                │   烧录成功？         │
                └──┬────────────────┬──┘
                 是│                │否
                   ↓                ↓
        ┌──────────────┐   ┌──────────────┐
        │ REBOOT 上报  │   │  FAIL 上报   │
        └──────┬───────┘   └──────┬───────┘
               ↓                   ↓
        ┌──────────────┐   ┌──────────────┐
        │  设备重启    │   │  exitOTA()   │
        └──────┬───────┘   └──────────────┘
               ↓
        ┌──────────────┐
        │ SUCCESS 上报 │
        └──────┬───────┘
               ↓
        ┌──────────────┐
        │  exitOTA()   │
        └──────────────┘
```

---

**文档版本**: v1.0  
**最后更新**: 2025-12-25  
**适用 SDK 版本**: Android SDK v3.0+  
**维护者**: IoT Video Team
