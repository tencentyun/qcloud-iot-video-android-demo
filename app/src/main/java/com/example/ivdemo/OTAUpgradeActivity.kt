package com.example.ivdemo

import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import com.google.errorprone.annotations.CheckReturnValue
import com.tencent.iot.twcall.R
import com.tencent.iot.twcall.databinding.ActivityOtaUpgradeBinding
import com.tencent.iot.video.device.VideoNativeInterface
import com.tencent.iot.video.device.annotations.OTAFailType
import com.tencent.iot.video.device.annotations.OTAProgressType
import com.tencent.iot.video.device.callback.IvOTACallback
import com.tencent.iotvideo.link.util.updateOperate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.IOException

class OTAUpgradeActivity : BaseIPCActivity<ActivityOtaUpgradeBinding>(), IvOTACallback {

    private var newFirmwareSize: Int = 0
    private var isUpgrade = false

    override fun getViewBinding(): ActivityOtaUpgradeBinding =
        ActivityOtaUpgradeBinding.inflate(layoutInflater)

    override fun initView() {
        with(binding) {
            titleLayout.tvTitle.text = getString(R.string.title_ota_upgrade)
            titleLayout.ivBack.setOnClickListener { onBackPressed() }
            textDevInfo.text =
                String.format((getString(R.string.text_device_info)), "${productId}_$deviceName")
            updateState(getString(R.string.text_ota_prepare))
            btnOtaUpgrade.setOnClickListener(View.OnClickListener {
                if (!isOnline) {
                    showToast("设备未上线")
                    return@OnClickListener
                }
                if (newFirmwareSize == 0) {
                    showToast("暂未检查到新固件")
                    return@OnClickListener
                }
                startOTAUpgrade()
                binding.btnOtaUpgrade.isEnabled = false
                binding.btnOtaUpgrade.updateOperate(false)
            })
            btnExitOta.setOnClickListener(View.OnClickListener {
                if (!isOnline) {
                    showToast("设备未上线")
                    return@OnClickListener
                }
                exitOTAUpgrade()
                binding.btnExitOta.isEnabled = false
                binding.btnExitOta.updateOperate(false)
            })
        }
    }

    private fun updateState(value: String) {
        defaultScope.launch(Dispatchers.Main) {
            binding.tvStateValue.text = value
        }
    }

    private fun checkForNewFirmware() {
        val hasDir = checkAndCreateDirectory(OTA_FIRMWARE_PATH)
        if (hasDir) {
            VideoNativeInterface.getInstance()
                .initOTA(OTA_FIRMWARE_PATH, OTA_FIRMWARE_VERSION, this)
        }
    }

    private fun startOTAUpgrade() {
        isUpgrade = true
        binding.pbUpgrade.isVisible = true
        binding.tvShowContent.isVisible = true
        binding.tvShowContent.text = "0%"
    }

    private fun exitOTAUpgrade() {
        VideoNativeInterface.getInstance().exitOTA()
    }

    override fun onOnline(netDateTime: Long) {
        super.onOnline(netDateTime)
        updateState(getString(R.string.text_ota_check))
        checkForNewFirmware()
        binding.btnExitOta.updateOperate(true)
    }

    override fun onOffline(status: Int) {
        super.onOffline(status)
    }

    override fun onFirmwareUpdate(firmwareName: String, firmwareLen: Int) {
        Log.d(TAG, "onFirmwareUpdate   firmwareName:$firmwareName  firmwareLen:$firmwareLen")
//        Toast.makeText(this@OTAUpgradeActivity, "onFirmwareUpdate", Toast.LENGTH_SHORT)
//            .show()
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

    override fun onPrepare(newFirmwareVersion: String?, newFirmwareSize: Int): Int {
        Log.d(TAG, "newFirmwareVersion:$newFirmwareVersion   newFirmwareSize:$newFirmwareSize")
        this.newFirmwareSize = newFirmwareSize
        updateState(
            String.format(
                getString(R.string.text_new_firmware), newFirmwareVersion, newFirmwareSize
            )
        )
        defaultScope.launch(Dispatchers.Main) {
            binding.btnOtaUpgrade.updateOperate(!isUpgrade)
        }
        return if (isUpgrade) 0 else 1
    }

    override fun onDownloadSize(size: Int) {
        Log.d(TAG, "current download progress:$size")
        defaultScope.launch(Dispatchers.Main) {
            val progress = (size.toDouble() / newFirmwareSize.toDouble()) * 100
            binding.pbUpgrade.progress = progress.toInt()
            binding.tvShowContent.text = if (progress.toInt() == 100) {
                "下载完成${String.format("%.2f", progress)}%"
            } else {
                "下载中${String.format("%.2f", progress)}%"
            }
        }
    }


    override fun onDestroy() {
        defaultScope.launch { exitOTAUpgrade() }
        super.onDestroy()
    }

    private fun checkAndCreateDirectory(path: String): Boolean {
        val dir = File(path)
        if (!dir.exists()) {
            return dir.mkdirs()
        }
        return true
    }

    companion object {
        private val TAG: String = OTAUpgradeActivity::class.java.simpleName

        private const val OTA_FIRMWARE_PATH = "/sdcard/temp"
        private const val OTA_FIRMWARE_VERSION = "3.0.0"
    }
}
