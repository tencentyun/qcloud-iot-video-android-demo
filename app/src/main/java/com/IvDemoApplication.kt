package com

import android.app.Application
import com.tencent.mmkv.MMKV

class IvDemoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MMKV.initialize(this);
    }
}
