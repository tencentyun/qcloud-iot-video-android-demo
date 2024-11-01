package com.example.ivdemo

import android.graphics.SurfaceTexture
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.TextureView.SurfaceTextureListener
import androidx.core.view.isVisible
import com.example.ivdemo.popup.QualitySettingDialog
import com.tencent.iot.twcall.R
import com.tencent.iot.twcall.databinding.ActivityDuplexVideoBinding
import com.tencent.iot.video.device.annotations.StreamType
import com.tencent.iot.video.device.model.AvDataInfo
import com.tencent.iotvideo.link.CameraRecorder
import com.tencent.iotvideo.link.SimplePlayer

private val TAG: String = DuplexVideoActivity::class.java.simpleName

class DuplexVideoActivity : BaseIPCActivity<ActivityDuplexVideoBinding>() {

    private val player = SimplePlayer()

    private val cameraRecorder = CameraRecorder()

    private var localPreviewSurface: SurfaceTexture? = null

    private var remotePreviewSurface: SurfaceTexture? = null


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