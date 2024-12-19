package com

import android.app.Application
import com.tencent.iot.video.device.VideoNativeInterface
import com.tencent.iot.video.device.annotations.LogLevelType

class IvDemoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        VideoNativeInterface.getInstance().initLog(LogLevelType.IV_eLOG_DEBUG)
    }
}
