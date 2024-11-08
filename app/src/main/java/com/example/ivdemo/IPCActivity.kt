package com.example.ivdemo

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.SurfaceTexture
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.TextureView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.ivdemo.popup.CustomCommandDialog
import com.example.ivdemo.popup.QualitySettingDialog
import com.tencent.iot.twcall.R
import com.tencent.iot.twcall.databinding.ActivityIpcBinding
import com.tencent.iot.video.device.VideoNativeInterface
import com.tencent.iot.video.device.annotations.CsChannelType
import com.tencent.iot.video.device.annotations.StreamType
import com.tencent.iot.video.device.model.AvDataInfo
import com.tencent.iotvideo.link.CameraRecorder
import com.tencent.iotvideo.link.SimplePlayer
import com.tencent.iotvideo.link.util.copyTextToClipboard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class IPCActivity : BaseIPCActivity<ActivityIpcBinding>() {

    private val player = SimplePlayer()
    private val cameraRecorder = CameraRecorder()
    private var lastClickTime = 0L
    private var localPreviewSurface: SurfaceTexture? = null
    private val handler = Handler(Looper.getMainLooper())
    private val UPDATE_P2P_INFO_TOKEN = "update_p2p_info_token"
    private var customCommandDialog: CustomCommandDialog? = null

    private val listener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            if (surface == binding.textureViewIpc.surfaceTexture) {
                // Initialize the SurfaceTexture object
                localPreviewSurface = surface

                // Start the camera encoder
                cameraRecorder.openCamera(localPreviewSurface, this@IPCActivity)
                Log.d("IPCActivity", "onSurfaceTextureAvailable")
            }
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            // Not used in this example
            Log.d("IPCActivity", "onSurfaceTextureSizeChanged")
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            Log.d("IPCActivity", "onSurfaceTextureDestroyed")
            if (surface == binding.textureViewIpc.surfaceTexture) {
                // Stop the camera encoder
                cameraRecorder.closeCamera()
            }
            return true
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            // Not used in this example
            Log.d("IPCActivity", "onSurfaceTextureUpdated")
        }
    }

    override fun getViewBinding(): ActivityIpcBinding = ActivityIpcBinding.inflate(layoutInflater)
    override fun initView() {
        with(binding) {
            titleLayout.tvTitle.text = getString(R.string.title_ipc)
            titleLayout.ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
            binding.titleLayout.ivRightBtn.isVisible = true
            binding.titleLayout.ivRightBtn.setOnClickListener {
                val dialog = QualitySettingDialog(this@IPCActivity)
                dialog.setOnDismissListener {
                    cameraRecorder.closeCamera()
                    cameraRecorder.openCamera(localPreviewSurface, this@IPCActivity)
                }
                dialog.show(supportFragmentManager)
            }
            textDevInfo.text =
                String.format((getString(R.string.text_device_info)), "${productId}_$deviceName")
            textureViewIpc.surfaceTextureListener = listener
            tvCopy.setOnClickListener {
                copyTextToClipboard(this@IPCActivity, tvP2pInfo.text.toString().substringAfter(":"))
            }
            btnCloudStorageReport.setOnClickListener {
                if (!isOnline) {
                    showToast("设备未上线")
                    return@setOnClickListener
                }
                VideoNativeInterface.getInstance()
                    .startCsEvent(CsChannelType.CS_SINGLE_CH, 1, "report test cs info")
            }
            btnCloudWareReport.setOnClickListener {
                if (!isOnline) {
                    showToast("设备未上线")
                    return@setOnClickListener
                }
                val drawable = ContextCompat.getDrawable(this@IPCActivity, R.drawable.mom)
                drawable?.let {
                    VideoNativeInterface.getInstance().reportCsEventDirectly(
                        CsChannelType.CS_SINGLE_CH,
                        1,
                        "report test cs info",
                        0,
                        bitmapToByteArray(drawableToBitmap(it))
                    )
                }
            }
            btnCustomCommand.setOnClickListener {
                if (!isOnline) {
                    showToast("设备未上线")
                    return@setOnClickListener
                }
                customCommandDialog = CustomCommandDialog(this@IPCActivity, visitor)
                customCommandDialog?.show(supportFragmentManager)
            }
//            btnIpcCall.setOnClickListener {
//                val time = System.currentTimeMillis()
//                val timeD = time - lastClickTime
//                //防止频繁点击
//                if (timeD in 1..999) {
//                    Toast.makeText(this@IPCActivity, "频繁点击！", Toast.LENGTH_SHORT).show()
//                    return@setOnClickListener
//                }
//                lastClickTime = time
//                // msg_id 6: 按门铃
//                VideoNativeInterface.getInstance().sendMsgNotice(6)
//            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun updateP2pInfo() {
        val p2pInfo = VideoNativeInterface.getInstance().p2pInfo
        if (!binding.tvP2pInfo.text.toString().contains(p2pInfo)) {
            showToast("P2PInfo 已更新")
        }
        binding.tvP2pInfo.text = String.format(getString(R.string.text_p2p_info), p2pInfo)
        handler.postDelayed(taskRunnable, UPDATE_P2P_INFO_TOKEN, 10000)
    }

    private var shouldGetCs = true
    private fun updateCsState() {
        if (shouldGetCs) {
            val info =
                VideoNativeInterface.getInstance().getCsBalanceInfo(CsChannelType.CS_SINGLE_CH, 10)
            if (info != null) {
                shouldGetCs = false
                binding.tvCloudStorageState.text = String.format(
                    getString(R.string.text_cloud_storage_state),
                    if (info.csSwitch) "已开通" else "未开通"
                )
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private val taskRunnable = Runnable {
        updateP2pInfo()
        updateCsState()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(UPDATE_P2P_INFO_TOKEN)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onOnline(netDateTime: Long) {
        super.onOnline(netDateTime)
        lifecycleScope.launch(Dispatchers.Main) {
            updateP2pInfo()
            handler.postDelayed(taskRunnable, UPDATE_P2P_INFO_TOKEN, 60000)
        }
    }

    override fun onGetAvEncInfo(visitor: Int, channel: Int, videoResType: Int): AvDataInfo {
        return AvDataInfo.createDefaultAvDataInfo(videoResType)
    }

    override fun onStartRealPlay(visitor: Int, channel: Int, videoResType: Int) {
        super.onStartRealPlay(visitor, channel, videoResType)
        cameraRecorder.startRecording(visitor, videoResType)
    }

    override fun onStopRealPlay(visitor: Int, channel: Int, videoResType: Int) {
        cameraRecorder.stopRecording(visitor, videoResType)
    }

    override fun onStartRecvAudioStream(
        visitor: Int,
        channel: Int,
        type: Int,
        option: Int,
        mode: Int,
        width: Int,
        sample_rate: Int,
        sample_num: Int
    ): Int {
        return player.startAudioPlay(visitor, type, option, mode, width, sample_rate, sample_num)
    }

    override fun onStartRecvVideoStream(
        visitor: Int,
        channel: Int,
        type: Int,
        height: Int,
        width: Int,
        frameRate: Int
    ): Int {
        return super.onStartRecvVideoStream(visitor, channel, type, height, width, frameRate)
    }

    override fun onStopRecvStream(visitor: Int, channel: Int, streamType: Int): Int {
        return if (streamType == StreamType.IV_AVT_STREAM_TYPE_VIDEO) {
            player.stopVideoPlay(visitor)
        } else {
            player.stopAudioPlay(visitor)
        }
    }

    override fun onRecvStream(
        visitor: Int,
        streamType: Int,
        data: ByteArray?,
        len: Int,
        pts: Long,
        seq: Long
    ): Int {
        if (streamType == 1) {
            return player.playVideoStream(visitor, data, len, pts, seq)
        } else if (streamType == 0) {
            return player.playAudioStream(visitor, data, len, pts, seq)
        }
        return 0
    }

    override fun onRecvCommand(
        command: Int,
        visitor: Int,
        channel: Int,
        videoResType: Int,
        args: String?
    ): String {
        return customCommandDialog?.receiveCommand(args ?: "null").toString()
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }

        val width = drawable.intrinsicWidth
        val height = drawable.intrinsicHeight
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    // 将Bitmap转换为byte数组
    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
}