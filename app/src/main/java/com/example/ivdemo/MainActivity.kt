package com.example.ivdemo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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
import com.tencent.iotvideo.link.util.VoipSetting
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
    private val voipSetting by lazy { VoipSetting.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashReport.initCrashReport(applicationContext, "e1c50561ac", false)
        setContentView(binding.root)

        // Request permissions
        if (!hasPermissions()) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
        } else {
            checkFilesToastAfterPermissions()
            lifecycleScope.launch(Dispatchers.Main) {
                LogcatHelper.getInstance(this@MainActivity).start()
            }
        }
        with(binding) {
            // Set button click listeners
            btnLoginIPC.setOnClickListener {
                if (!checkDeviceInfo()) return@setOnClickListener
                startActivity(IPCActivity::class.java)
            }
            btnLoginDuplexVideo.setOnClickListener {
                if (!checkDeviceInfo()) return@setOnClickListener
                startActivity(DuplexVideoActivity::class.java)
            }
            btnLoginVoip.setOnClickListener {
                if (!checkDeviceInfo()) return@setOnClickListener
                startActivity(VoipLoginActivity::class.java)
            }
            btnOtaUpgrade.setOnClickListener {
                if (!checkDeviceInfo()) return@setOnClickListener
                startActivity(OTAUpgradeActivity::class.java)
            }
            btnSettingDevice.setOnClickListener {
                DeviceSettingDialog(this@MainActivity).show(supportFragmentManager)
            }
        }
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
                LogcatHelper.getInstance(this).start()
            } else {
                Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun hasPermissions(): Boolean {
        return (ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED) && ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startActivity(clazz: Class<*>) {
        val productId = voipSetting.productId
        val deviceName = voipSetting.deviceName
        val deviceKey = voipSetting.deviceKey
        val intent = Intent(this, clazz)
        intent.putExtra("productId", productId)
        intent.putExtra("deviceName", deviceName)
        intent.putExtra("deviceKey", deviceKey)
        if (clazz.simpleName == VoipActivity::class.java.simpleName) {
            intent.putExtra("voip_model_id", voipSetting.modelId)
            intent.putExtra("voip_device_id", voipSetting.sn)
            intent.putExtra("voip_wxa_appid", voipSetting.appId)
            intent.putExtra("voip_sn_ticket", voipSetting.snTicket)
            intent.putExtra("miniprogramVersion", 0)
        }
        startActivity(intent)
    }

    private fun checkDeviceInfo(): Boolean {
        val productId = voipSetting.productId
        val deviceName = voipSetting.deviceName
        val deviceKey = voipSetting.deviceKey
        if (productId.isEmpty() || deviceName.isEmpty() || deviceKey.isEmpty()) {
            Toast.makeText(this@MainActivity, "请输入设备信息！", Toast.LENGTH_SHORT).show()
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
            saveFileFromAssertToSDCard("voip_setting.json")
            val editor = preferences.edit()
            editor.putBoolean("installFlag", true)
            editor.apply()
        } else {
            if (!isFileExists("device_key")) {
                saveFileFromAssertToSDCard("device_key")
            }
            if (!isFileExists("voip_setting.json")) {
                saveFileFromAssertToSDCard("voip_setting.json")
            }
        }
        Toast.makeText(
            this,
            "voip_setting.json是否在sdcard下：" + isFileExists("voip_setting.json") + ", json是否合法：" + VoipSetting.isJSONString(
                voipSetting.loadData()
            ),
            Toast.LENGTH_SHORT
        ).show()
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