package com.example.ivdemo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaCodecList
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import com.tencent.iot.twcall.databinding.ActivityDeviceInfoBinding
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileReader
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


private const val REQUEST_STORAGE_PERMISSION = 100

class DeviceInfoActivity : AppCompatActivity() {

    private val binding by lazy { ActivityDeviceInfoBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        with(binding) {
            titleLayout.tvTitle.text = "DeviceInfo"
            titleLayout.ivBack.setOnClickListener { onBackPressed() }
            titleLayout.tvRightBtn.isVisible = true
            titleLayout.tvRightBtn.text = "分享日志"
            titleLayout.tvRightBtn.setOnClickListener {
                checkStoragePermission()
            }
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

    private fun checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_STORAGE_PERMISSION
            )
        } else {
            compressAndShareP2PLogs()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                compressAndShareP2PLogs()
            } else {
                Toast.makeText(
                    this,
                    "Storage permission is required to access files.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun compressAndShareP2PLogs() {
        val p2pLogsFolder: File = getP2PLogsFolder()
        if (!p2pLogsFolder.exists() || !p2pLogsFolder.isDirectory) {
            Toast.makeText(this, "p2p_logs folder not found.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val zipFile = File(getExternalFilesDir(null), "p2p_logs.zip")
            val fos = FileOutputStream(zipFile)
            val zos = ZipOutputStream(fos)
            zipFolder(p2pLogsFolder, p2pLogsFolder.name, zos)
            zos.close()
            fos.close()
            shareFile(zipFile)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @Throws(IOException::class)
    private fun zipFolder(folder: File, parentFolder: String, zos: ZipOutputStream) {
        val files = folder.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                zipFolder(file, parentFolder + "/" + file.name, zos)
                continue
            }
            val fis = FileInputStream(file)
            zos.putNextEntry(ZipEntry(parentFolder + "/" + file.name))
            val buffer = ByteArray(1024)
            var length: Int
            while ((fis.read(buffer).also { length = it }) > 0) {
                zos.write(buffer, 0, length)
            }
            zos.closeEntry()
            fis.close()
        }
    }

    private fun getP2PLogsFolder(): File {
        val sdCard = Environment.getExternalStorageDirectory()
        return File(sdCard, "p2p_logs")
    }

    private fun shareFile(file: File) {
        val fileUri = FileProvider.getUriForFile(this, "$packageName.provider", file)
        val intent = Intent(Intent.ACTION_SEND)
        intent.setType("application/zip")
        intent.putExtra(Intent.EXTRA_STREAM, fileUri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(intent, "Share File"))
    }
}