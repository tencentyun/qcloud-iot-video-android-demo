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
import com.tencent.iot.voipdemo.databinding.ActivityMainBinding
import com.tencent.iotvideo.link.popup.DeviceSettingDialog
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

class MainActivity : AppCompatActivity() ***REMOVED***

    private val binding = ActivityMainBinding.inflate(layoutInflater)
    private val permissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    private val voipSetting = VoipSetting.getInstance(this)

    override fun onCreate(savedInstanceState: Bundle?) ***REMOVED***
        super.onCreate(savedInstanceState)
        CrashReport.initCrashReport(applicationContext, "e1c50561ac", false)
        setContentView(binding.root)

        // Request permissions
        if (!hasPermissions()) ***REMOVED***
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
      ***REMOVED*** else ***REMOVED***
            checkFilesToastAfterPermissions()
            lifecycleScope.launch(Dispatchers.Main) ***REMOVED***
                LogcatHelper.getInstance(this@MainActivity).start()
          ***REMOVED***
      ***REMOVED***
        with(binding) ***REMOVED***
            // Set button click listeners
            btnLoginIPC.setOnClickListener ***REMOVED***
                if (!checkDeviceInfo()) return@setOnClickListener
                startActivity(IPCActivity::class.java)
          ***REMOVED***
            btnLoginDuplexVideo.setOnClickListener ***REMOVED***
                if (!checkDeviceInfo()) return@setOnClickListener
                startActivity(DuplexVideoActivity::class.java)
          ***REMOVED***
            btnLoginVoip.setOnClickListener ***REMOVED***
                if (!checkDeviceInfo()) return@setOnClickListener
                startActivity(VoipLoginActivity::class.java)
          ***REMOVED***
            btnSettingDevice.setOnClickListener ***REMOVED***
                DeviceSettingDialog(this@MainActivity).show()
          ***REMOVED***
      ***REMOVED***
  ***REMOVED***

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) ***REMOVED***
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) ***REMOVED***
            if (hasPermissions()) ***REMOVED***
                checkFilesToastAfterPermissions()
                LogcatHelper.getInstance(this).start()
          ***REMOVED*** else ***REMOVED***
                Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show()
          ***REMOVED***
      ***REMOVED***
  ***REMOVED***

    private fun hasPermissions(): Boolean ***REMOVED***
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
  ***REMOVED***

    private fun startActivity(clazz: Class<*>) ***REMOVED***
        val productId = voipSetting.productId
        val deviceName = voipSetting.deviceName
        val deviceKey = voipSetting.deviceKey
        val intent = Intent(this, clazz)
        intent.putExtra("productId", productId)
        intent.putExtra("deviceName", deviceName)
        intent.putExtra("deviceKey", deviceKey)
        startActivity(intent)
  ***REMOVED***

    private fun checkDeviceInfo(): Boolean ***REMOVED***
        val productId = voipSetting.productId
        val deviceName = voipSetting.deviceName
        val deviceKey = voipSetting.deviceKey
        if (productId.isEmpty() || deviceName.isEmpty() || deviceKey.isEmpty()) ***REMOVED***
            Toast.makeText(this@MainActivity, "请输入设备信息！", Toast.LENGTH_LONG).show()
            return false
      ***REMOVED***
        return true
  ***REMOVED***

    private fun checkFilesToastAfterPermissions() ***REMOVED***
        val preferences = getSharedPreferences("InstallConfig", MODE_PRIVATE)
        val installFlag = preferences.getBoolean("installFlag", false)
        Log.d(TAG, "is first install or reinstall: $installFlag")
        if (!installFlag) ***REMOVED***
            saveFileFromAssertToSDCard("device_key")
            saveFileFromAssertToSDCard("voip_setting.json")
            val editor = preferences.edit()
            editor.putBoolean("installFlag", true)
            editor.apply()
      ***REMOVED*** else ***REMOVED***
            if (!isFileExists("device_key")) ***REMOVED***
                saveFileFromAssertToSDCard("device_key")
          ***REMOVED***
            if (!isFileExists("voip_setting.json")) ***REMOVED***
                saveFileFromAssertToSDCard("voip_setting.json")
          ***REMOVED***
      ***REMOVED***
        Toast.makeText(
            this,
            "voip_setting.json是否在sdcard下：" + isFileExists("voip_setting.json") + ", json是否合法：" + VoipSetting.isJSONString(
                voipSetting.loadData()
            ),
            Toast.LENGTH_SHORT
        ).show()
  ***REMOVED***

    private fun isFileExists(fileName: String): Boolean ***REMOVED***
        val file = File(Environment.getExternalStorageDirectory(), fileName)
        if (file.exists()) ***REMOVED***
            Log.d(TAG, fileName + "File exists")
            return true
      ***REMOVED*** else ***REMOVED***
            Log.d(TAG, fileName + "File does not exist")
            return false
      ***REMOVED***
  ***REMOVED***

    private fun saveFileFromAssertToSDCard(fileName: String) ***REMOVED***
        val assetManager = assets
        var input: InputStream? = null
        var output: OutputStream? = null

        try ***REMOVED***
            input = assetManager.open(fileName)
            val outFile = File(Environment.getExternalStorageDirectory(), fileName)
            output = FileOutputStream(outFile)
            copyFile(input, output)
      ***REMOVED*** catch (e: IOException) ***REMOVED***
            Log.e(TAG, "Failed to copy asset file: $fileName", e)
      ***REMOVED*** finally ***REMOVED***
            if (input != null) ***REMOVED***
                try ***REMOVED***
                    input.close()
              ***REMOVED*** catch (e: IOException) ***REMOVED***
                    // NOOP
                    Log.e(TAG, "in.close Failed to copy asset file: $fileName", e)
              ***REMOVED***
          ***REMOVED***
            if (output != null) ***REMOVED***
                try ***REMOVED***
                    output.close()
              ***REMOVED*** catch (e: IOException) ***REMOVED***
                    // NOOP
                    Log.e(TAG, "out.close Failed to copy asset file: $fileName", e)
              ***REMOVED***
          ***REMOVED***
      ***REMOVED***
  ***REMOVED***

    @Throws(IOException::class)
    private fun copyFile(input: InputStream, output: OutputStream) ***REMOVED***
        val buffer = ByteArray(1024)
        var read: Int
        while ((input.read(buffer).also ***REMOVED*** read = it }) != -1) ***REMOVED***
            output.write(buffer, 0, read)
      ***REMOVED***
  ***REMOVED***
}