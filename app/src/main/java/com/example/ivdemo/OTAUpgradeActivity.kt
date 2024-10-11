package com.example.ivdemo

import android.util.Log
import android.view.View
import android.widget.Toast
import com.tencent.iot.twcall.R
import com.tencent.iot.twcall.databinding.ActivityOtaUpgradeBinding
import com.tencent.iot.video.device.VideoNativeInterface
import com.tencent.iot.video.device.annotations.OTAFailType
import com.tencent.iot.video.device.annotations.OTAProgressType
import com.tencent.iot.video.device.callback.IvOTACallback
import java.io.File
import java.io.FileInputStream
import java.io.IOException

class OTAUpgradeActivity : BaseIPCActivity<ActivityOtaUpgradeBinding>(), IvOTACallback {

    private var isOnline = false

    private fun initOTAUpgrade() {
        VideoNativeInterface.getInstance()
            .initOTA(OTA_FIRMWARE_PATH, OTA_FIRMWARE_VERSION, this)
    }

    private fun exitOTAUpgrade() {
        VideoNativeInterface.getInstance().exitOTA()
    }

    override fun getViewBinding(): ActivityOtaUpgradeBinding =
        ActivityOtaUpgradeBinding.inflate(layoutInflater)

    override fun initView() {
        with(binding) {
            titleLayout.tvTitle.text = getString(R.string.title_ota_upgrade)
            titleLayout.ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
            textDevinfo.text =
                String.format((getString(R.string.text_device_info)), "$productId/$deviceName")
            btnOtaUpgrade.setOnClickListener(View.OnClickListener {
                if (!isOnline) {
                    Toast.makeText(this@OTAUpgradeActivity, "设备未上线", Toast.LENGTH_SHORT).show()
                    return@OnClickListener
                }
                initOTAUpgrade()
            })
            btnExitOta.setOnClickListener(View.OnClickListener {
                if (!isOnline) {
                    Toast.makeText(this@OTAUpgradeActivity, "设备未上线", Toast.LENGTH_SHORT).show()
                    return@OnClickListener
                }
                exitOTAUpgrade()
            })
        }
    }

    override fun onOnline(netDateTime: Long) {
        super.onOnline(netDateTime)
        isOnline = true
    }

    override fun onOffline(status: Int) {
        super.onOffline(status)
        isOnline = false
    }

    override fun onFirmwareUpdate(firmwareName: String, firmwareLen: Int) {
        Toast.makeText(this@OTAUpgradeActivity, "onFirmwareUpdate", Toast.LENGTH_SHORT)
            .show()
        var fileInputStream: FileInputStream? = null
        val firmwareFile = File(firmwareName)

        try {
            if (!firmwareFile.exists()) {
                Log.d(TAG, "Open firmware file: $firmwareName failed")
                VideoNativeInterface.getInstance().updateOTAProgress(
                    OTAProgressType.IV_OTA_PROGRESS_TYPE_FAIL,
                    OTAFailType.IV_OTA_FAIL_TYPE_OPEN_FILE
                )
                exitOTAUpgrade()
                return
            }

            fileInputStream = FileInputStream(firmwareFile)
            val fileLength = firmwareFile.length()

            if (firmwareLen.toLong() != fileLength) {
                Log.d(
                    TAG,
                    "$firmwareName real size is wrong, firmwareLen: $firmwareLen, fileLen: $fileLength."
                )
                VideoNativeInterface.getInstance().updateOTAProgress(
                    OTAProgressType.IV_OTA_PROGRESS_TYPE_FAIL,
                    OTAFailType.IV_OTA_FAIL_TYPE_WRONG_SIZE
                )
                exitOTAUpgrade()
                return
            }
            VideoNativeInterface.getInstance()
                .updateOTAProgress(OTAProgressType.IV_OTA_PROGRESS_TYPE_WRITE_FLASH, 0)
            Thread.sleep(1000) //todo 实现升级
            VideoNativeInterface.getInstance()
                .updateOTAProgress(OTAProgressType.IV_OTA_PROGRESS_TYPE_SUCCESS, 0)
            exitOTAUpgrade()
        } catch (e: Exception) {
            VideoNativeInterface.getInstance().updateOTAProgress(
                OTAProgressType.IV_OTA_PROGRESS_TYPE_FAIL,
                OTAFailType.IV_OTA_FAIL_TYPE_OPEN_FILE
            )
            exitOTAUpgrade()
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onOTAPrepare(): Int {
        return 0
    }

    companion object {
        private val TAG: String = OTAUpgradeActivity::class.java.simpleName

        private const val OTA_FIRMWARE_PATH = "/tmp"
        private const val OTA_FIRMWARE_VERSION = "2.0.0"
    }
}
