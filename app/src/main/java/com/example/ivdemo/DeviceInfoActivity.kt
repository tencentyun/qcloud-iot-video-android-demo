package com.example.ivdemo

import android.content.Intent
import android.media.MediaCodecList
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.tencent.iot.twcall.databinding.ActivityDeviceInfoBinding
import com.tencent.iotvideo.link.util.QualitySetting
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException


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
            val device = "Android Version: $versionName\n" +
                    "API Level: $sdkVersion\n" +
                    "Codename: $versionCodename\n" +
                    "Incremental: $versionIncremental"
            Log.d("DeviceInfoActivity", device)
            deviceInfo.text = device
            val encoder = "support encoder:${selectCodec()}"
            Log.d("DeviceInfoActivity", encoder)
            encoderInfo.text = encoder
            val cpu = getCpuInfo() + "\n cpu架构：" + Build.SUPPORTED_ABIS[0]
            Log.d("DeviceInfoActivity", cpu)
            cpuInfo.text = cpu
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
            if (!codecInfo.isEncoder){
                Log.d("Decoder", "Decoder name: " + codecInfo.getName());
                val supportedTypes = codecInfo.supportedTypes
                for (type in supportedTypes) {
                    Log.d("Decoder", "Supported type: $type")
                }
                continue;
            }
            val types = codecInfo.supportedTypes
            for (type in types) {
                Log.d("DeviceInfoActivity", "Encoder name: " + codecInfo.name + ", type: " + type)
                if (type.startsWith("video/")) {
                    val capabilities = codecInfo.getCapabilitiesForType(type)
                    val videoCapabilities = capabilities.videoCapabilities
                    val videoWidth = QualitySetting.getInstance(applicationContext).width
                    val videoHeight = QualitySetting.getInstance(applicationContext).height
                    str.append(
                        "name:${codecInfo.name};  type:${type};  colorFormats:${capabilities.colorFormats.contentToString()};  currentPixel:${videoWidth}x${videoHeight}  bitRateRange:${videoCapabilities.bitrateRange} frameRange:${videoCapabilities.supportedFrameRates}  isSupportedPixel:${
                            videoCapabilities.isSizeSupported(
                                videoWidth,
                                videoHeight
                            )
                        } \n"
                    )
                } else {
                    str.append("name:${codecInfo.name};  type:${type} \n")
                }
            }
        }
        return str.toString()
    }

    private fun getCpuInfo(): String {
        val cpuInfo = StringBuilder()
        try {
            val br = BufferedReader(FileReader("/proc/cpuinfo"))
            var line: String?
            while ((br.readLine().also { line = it }) != null) {
                cpuInfo.append(line).append("\n")
            }
            br.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return cpuInfo.toString()
    }
}