package com.example.ivdemo

import android.content.Intent
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.tencent.iot.twcall.databinding.ActivityDeviceInfoBinding
import java.util.Arrays

class DeviceInfoActivity : AppCompatActivity() {

    private val binding by lazy { ActivityDeviceInfoBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        with(binding) {
            titleLayout.tvTitle.text = "DeviceInfo"
            titleLayout.ivBack.setOnClickListener { onBackPressed() }
            // 获取当前设备的 Android 版本号和 API 级别
            val sdkVersion = Build.VERSION.SDK_INT
            val versionName = Build.VERSION.RELEASE
            val versionCodename = Build.VERSION.CODENAME
            val versionIncremental = Build.VERSION.INCREMENTAL
            deviceInfo.text = "Android Version: $versionName\n" +
                    "API Level: $sdkVersion\n" +
                    "Codename: $versionCodename\n" +
                    "Incremental: $versionIncremental"
            val info = selectCodec()
            encoderInfo.text = "support encoder:${info}"
            log.setOnClickListener {
                startActivity(Intent(this@DeviceInfoActivity, LogActivity::class.java))
            }
        }
    }

    private fun selectCodec(): String {
        val str = StringBuilder()
        val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
        val codecInfos = codecList.codecInfos
        for (codecInfo in codecInfos) {
            if (!codecInfo.isEncoder) continue
            val types = codecInfo.supportedTypes
            for (type in types) {
                Log.d("DeviceInfoActivity", "Encoder name: " + codecInfo.name + ", type: " + type)
                if (type.startsWith("video/")) {
                    val capabilities = codecInfo.getCapabilitiesForType(type)
                    str.append("name:${codecInfo.name};  type:${type};  colorFormats:${capabilities.colorFormats.contentToString()} \n")
                } else {
                    str.append("name:${codecInfo.name};  type:${type} \n")
                }
            }
        }
        return str.toString()
    }
}