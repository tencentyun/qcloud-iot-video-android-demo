package com.example.ivdemo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.tencent.bugly.crashreport.CrashReport
import com.tencent.iot.twcall.databinding.ActivityMainBinding
import com.example.ivdemo.popup.DeviceSettingDialog
import com.tencent.iotvideo.link.util.DeviceSetting
import com.tencent.iotvideo.link.util.updateOperate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

private const val PERMISSION_REQUEST_CODE = 1
private val TAG: String = MainActivity::class.java.simpleName

class MainActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val permissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    private val moreThan30permissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    private val deviceSetting by lazy { DeviceSetting.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashReport.initCrashReport(applicationContext, "e1c50561ac", false)
        setContentView(binding.root)

        // Request permissions
        if (!hasPermissions()) {
            ActivityCompat.requestPermissions(
                this,
                if (Build.VERSION.SDK_INT >= 30) moreThan30permissions else permissions,
                PERMISSION_REQUEST_CODE
            )
        } else {
            checkFilesToastAfterPermissions()
            lifecycleScope.launch(Dispatchers.Main) {
                LogcatHelper.getInstance(this@MainActivity).start()
            }
        }
        with(binding) {
            updateBtnState()
            // Set button click listeners
            btnLoginDuplexVideo.setOnClickListener {
                if (!checkDeviceInfo()) return@setOnClickListener
                if (deviceSetting.ipcType == 2) {
                    startActivity(DuplexVideoActivity::class.java)
                } else {
                    startActivity(IPCActivity::class.java)
                }
            }
            btnTweCall.setOnClickListener {
                if (!checkDeviceInfo()) return@setOnClickListener
                startActivity(TweCallLoginActivity::class.java)
            }
            btnOtaUpgrade.setOnClickListener {
                if (!checkDeviceInfo()) return@setOnClickListener
                startActivity(OTAUpgradeActivity::class.java)
            }
            btnSettingDevice.setOnClickListener {
                val dialog = DeviceSettingDialog(this@MainActivity)
                dialog.setDismissListener {
                    updateBtnState()
                }
                dialog.show(supportFragmentManager)
            }
        }
    }

    private fun updateBtnState() {
        val isOperate =
            !(deviceSetting.productId.isEmpty() || deviceSetting.deviceName.isEmpty() || deviceSetting.deviceKey.isEmpty())
        binding.btnLoginDuplexVideo.updateOperate(isOperate)
        binding.btnTweCall.updateOperate(isOperate)
        binding.btnOtaUpgrade.updateOperate(isOperate)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (hasPermissions()) {
                checkFilesToastAfterPermissions()
                LogcatHelper.getInstance(this.applicationContext).start()
            } else {
                Toast.makeText(this.applicationContext, "Permissions denied", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun hasPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= 30) {
            ContextCompat.checkSelfPermission(
                this.applicationContext,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                this.applicationContext,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                this.applicationContext,
                Manifest.permission.MANAGE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            (ContextCompat.checkSelfPermission(
                this.applicationContext,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED) && ContextCompat.checkSelfPermission(
                this.applicationContext,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                this.applicationContext,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun startActivity(clazz: Class<*>) {
        val productId = deviceSetting.productId
        val deviceName = deviceSetting.deviceName
        val deviceKey = deviceSetting.deviceKey
        val intent = Intent(this, clazz)
        intent.putExtra("productId", productId)
        intent.putExtra("deviceName", deviceName)
        intent.putExtra("deviceKey", deviceKey)
        startActivity(intent)
    }

    private fun checkDeviceInfo(): Boolean {
        val productId = deviceSetting.productId
        val deviceName = deviceSetting.deviceName
        val deviceKey = deviceSetting.deviceKey
        if (productId.isEmpty() || deviceName.isEmpty() || deviceKey.isEmpty()) {
            Toast.makeText(
                this@MainActivity.applicationContext,
                "请先配置设备信息！",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }
        return true
    }

    private fun checkFilesToastAfterPermissions() {
        val preferences = getSharedPreferences("InstallConfig", MODE_PRIVATE)
        val installFlag = preferences.getBoolean("installFlag", false)
        Log.d(TAG, "is first install or reinstall: $installFlag")
        if (!installFlag) {
            saveFileFromAssertToSDCard("device_key")
            val editor = preferences.edit()
            editor.putBoolean("installFlag", true)
            editor.apply()
        } else {
            if (!isFileExists("device_key")) {
                saveFileFromAssertToSDCard("device_key")
            }
        }
    }

    private fun isFileExists(fileName: String): Boolean {
        val file = File(Environment.getExternalStorageDirectory(), fileName)
        if (file.exists()) {
            Log.d(TAG, fileName + "File exists")
            return true
        } else {
            Log.d(TAG, fileName + "File does not exist")
            return false
        }
    }

    private fun saveFileFromAssertToSDCard(fileName: String) {
        val assetManager = assets
        var input: InputStream? = null
        var output: OutputStream? = null

        try {
            input = assetManager.open(fileName)
            val outFile = File(Environment.getExternalStorageDirectory(), fileName)
            output = FileOutputStream(outFile)
            copyFile(input, output)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to copy asset file: $fileName", e)
        } finally {
            if (input != null) {
                try {
                    input.close()
                } catch (e: IOException) {
                    // NOOP
                    Log.e(TAG, "in.close Failed to copy asset file: $fileName", e)
                }
            }
            if (output != null) {
                try {
                    output.close()
                } catch (e: IOException) {
                    // NOOP
                    Log.e(TAG, "out.close Failed to copy asset file: $fileName", e)
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun copyFile(input: InputStream, output: OutputStream) {
        val buffer = ByteArray(1024)
        var read: Int
        while ((input.read(buffer).also { read = it }) != -1) {
            output.write(buffer, 0, read)
        }
    }
}