package com.tencent.iotvideo.ivdemo

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
import com.tencent.iot.voipdemo.databinding.ActivityMainBinding
import com.tencent.iotvideo.link.popup.DeviceSettingDialog
import com.tencent.iotvideo.link.util.VoipSetting
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class MainActivity : AppCompatActivity() ***REMOVED***

    private val TAG: String = MainActivity::class.java.getSimpleName()
    private val PERMISSIONS_REQUEST_CODE = 1


    private val binding: ActivityMainBinding by lazy ***REMOVED***
        ActivityMainBinding.inflate(layoutInflater)
  ***REMOVED***

    override fun onCreate(savedInstanceState: Bundle?) ***REMOVED***
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        with(binding) ***REMOVED***
            btnLoginVoip.setOnClickListener ***REMOVED***
                if (!checkDeviceInfo()) return@setOnClickListener
                startVoipActivity()
          ***REMOVED***
            btnSettingDevice.setOnClickListener ***REMOVED***
                val dialog = DeviceSettingDialog(this@MainActivity)
                dialog.show()
          ***REMOVED***
      ***REMOVED***

        // Request permissions
        if (!hasPermissions()) ***REMOVED***
            ActivityCompat.requestPermissions(
                this, arrayOf<String>(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ), PERMISSIONS_REQUEST_CODE
            )
      ***REMOVED*** else ***REMOVED***
            checkFilesToastAfterPermissions()
            runOnUiThread ***REMOVED*** LogcatHelper.getInstance(this@MainActivity).start() }
      ***REMOVED***
  ***REMOVED***

    private fun hasPermissions(): Boolean ***REMOVED***
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED && (ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED)
  ***REMOVED***

    private fun checkFilesToastAfterPermissions() ***REMOVED***
        val preferences = getSharedPreferences("InstallConfig", MODE_PRIVATE)
        val installFlag = preferences.getBoolean("installFlag", false)
        Log.d(
            TAG,
            "is first install or reinstall: $installFlag"
        )
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
                VoipSetting.getInstance(
                    this
                ).loadData()
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

        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null

        try ***REMOVED***
            inputStream = assetManager.open(fileName)
            val outFile = File(Environment.getExternalStorageDirectory(), fileName)
            outputStream = FileOutputStream(outFile)
            copyFile(inputStream, outputStream)
      ***REMOVED*** catch (e: IOException) ***REMOVED***
            Log.e(TAG, "Failed to copy asset file: $fileName", e)
      ***REMOVED*** finally ***REMOVED***
            if (inputStream != null) ***REMOVED***
                try ***REMOVED***
                    inputStream.close()
              ***REMOVED*** catch (e: IOException) ***REMOVED***
                    // NOOP
                    Log.e(TAG, "in.close Failed to copy asset file: $fileName", e)
              ***REMOVED***
          ***REMOVED***
            if (outputStream != null) ***REMOVED***
                try ***REMOVED***
                    outputStream.close()
              ***REMOVED*** catch (e: IOException) ***REMOVED***
                    // NOOP
                    Log.e(TAG, "out.close Failed to copy asset file: $fileName", e)
              ***REMOVED***
          ***REMOVED***
      ***REMOVED***
  ***REMOVED***

    @Throws(IOException::class)
    private fun copyFile(`in`: InputStream, out: OutputStream) ***REMOVED***
        val buffer = ByteArray(1024)
        var read: Int
        while ((`in`.read(buffer).also ***REMOVED*** read = it }) != -1) ***REMOVED***
            out.write(buffer, 0, read)
      ***REMOVED***
  ***REMOVED***


    private fun startVoipActivity() ***REMOVED***
        val productId = VoipSetting.getInstance(this).productId
        val deviceName = VoipSetting.getInstance(this).deviceName
        val deviceKey = VoipSetting.getInstance(this).deviceKey
        val intent = Intent(this, VoipLoginActivity::class.java)
        intent.putExtra("productId", productId)
        intent.putExtra("deviceName", deviceName)
        intent.putExtra("deviceKey", deviceKey)
        startActivity(intent)
  ***REMOVED***

    private fun checkDeviceInfo(): Boolean ***REMOVED***
        val productId: String = VoipSetting.getInstance(this).productId
        val deviceName: String = VoipSetting.getInstance(this).deviceName
        val deviceKey: String = VoipSetting.getInstance(this).deviceKey
        if (productId.isEmpty() || deviceName.isEmpty() || deviceKey.isEmpty()) ***REMOVED***
            Toast.makeText(this@MainActivity, "请输入设备信息！", Toast.LENGTH_LONG).show()
            return false
      ***REMOVED***
        return true
  ***REMOVED***
}