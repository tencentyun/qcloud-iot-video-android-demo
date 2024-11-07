package com.example.ivdemo

import android.graphics.SurfaceTexture
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Surface
import android.view.TextureView.SurfaceTextureListener
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.ivdemo.popup.QualitySettingDialog
import com.tencent.iot.twcall.R
import com.tencent.iot.twcall.databinding.ActivityDuplexVideoBinding
import com.tencent.iot.video.device.VideoNativeInterface
import com.tencent.iot.video.device.annotations.StreamType
import com.tencent.iot.video.device.model.AvDataInfo
import com.tencent.iotvideo.link.CameraRecorder
import com.tencent.iotvideo.link.SimplePlayer
import com.tencent.iotvideo.link.util.copyTextToClipboard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val TAG: String = DuplexVideoActivity::class.java.simpleName

class DuplexVideoActivity : BaseIPCActivity<ActivityDuplexVideoBinding>() {

    private val player = SimplePlayer()

    private val cameraRecorder = CameraRecorder()

    private var localPreviewSurface: SurfaceTexture? = null

    private var remotePreviewSurface: SurfaceTexture? = null

    private val handler = Handler(Looper.getMainLooper())
    private val UPDATE_P2P_INFO_TOKEN = "update_p2p_info_token"

    private val listener = object : SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            if (surface == binding.textureViewDuplex.surfaceTexture) {
                // Initialize the SurfaceTexture object
                localPreviewSurface = surface

                // Start the camera encoder
                cameraRecorder.openCamera(localPreviewSurface, this@DuplexVideoActivity)
            } else if (surface == binding.surfaceViewDuplex.surfaceTexture) {
                remotePreviewSurface = surface
            }
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            // Not used in this example
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            if (surface == binding.textureViewDuplex.surfaceTexture) {
                // Stop the camera encoder
                cameraRecorder.closeCamera()
            }
            return true
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            // Not used in this example
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "start create")
        super.onCreate(savedInstanceState)
    }

    override fun getViewBinding(): ActivityDuplexVideoBinding =
        ActivityDuplexVideoBinding.inflate(layoutInflater)

    override fun initView() {
        binding.textureViewDuplex.surfaceTextureListener = listener
        binding.surfaceViewDuplex.surfaceTextureListener = listener
        binding.titleLayout.tvTitle.text = getString(R.string.title_audio_video_call)
        binding.titleLayout.ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.titleLayout.ivRightBtn.isVisible = true
        binding.titleLayout.ivRightBtn.setOnClickListener {
            val dialog = QualitySettingDialog(this@DuplexVideoActivity)
            dialog.setOnDismissListener {
                cameraRecorder.closeCamera()
                cameraRecorder.openCamera(localPreviewSurface, this@DuplexVideoActivity)
            }
            dialog.show(supportFragmentManager)
        }
        binding.textDevInfo.text =
            String.format((getString(R.string.text_device_info)), "${productId}_$deviceName")
        binding.tvCopy.setOnClickListener {
            copyTextToClipboard(
                this@DuplexVideoActivity,
                binding.tvP2pInfo.text.toString().substringAfter(":")
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun updateP2pInfo() {
        val p2pInfo = VideoNativeInterface.getInstance().p2pInfo
        if (!binding.tvP2pInfo.text.toString().contains(p2pInfo)) {
            showToast("P2PInfo 已更新")
        }
        binding.tvP2pInfo.text = String.format(getString(R.string.text_p2p_info), p2pInfo)
        handler.postDelayed(taskRunnable, UPDATE_P2P_INFO_TOKEN, 60000)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private val taskRunnable = Runnable {
        updateP2pInfo()
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

    override fun onStartRecvVideoStream(
        visitor: Int, channel: Int, type: Int, height: Int, width: Int, frameRate: Int
    ): Int {
        return if (remotePreviewSurface != null) {
            player.startVideoPlay(Surface(remotePreviewSurface), visitor, type, height, width)
        } else {
            Log.d(TAG, "IvStartRecvVideoStream mRemotePreviewSurface is null visitor $visitor")
            -1
        }
    }
}