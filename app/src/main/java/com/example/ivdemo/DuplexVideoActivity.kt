package com.example.ivdemo

import android.graphics.SurfaceTexture
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Surface
import android.view.TextureView.SurfaceTextureListener
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
import com.tencent.iotvideo.link.util.adjustAspectRatio
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
                cameraRecorder.setPreviewView(binding.textureViewDuplex)
                cameraRecorder.openCamera()
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

    override fun getViewBinding(): ActivityDuplexVideoBinding =
        ActivityDuplexVideoBinding.inflate(layoutInflater)

    override fun initView() {
        cameraRecorder.init(this)
        player.setContext(this)
        with(binding) {
            textureViewDuplex.surfaceTextureListener = listener
            surfaceViewDuplex.surfaceTextureListener = listener
            titleLayout.tvTitle.text = getString(R.string.title_audio_video_call)
            titleLayout.ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
            titleLayout.ivRightBtn.isVisible = true
            titleLayout.ivRightBtn.setOnClickListener {
                val dialog = QualitySettingDialog(this@DuplexVideoActivity)
                dialog.setOnDismissListener {
                    cameraRecorder.closeCamera()
                    cameraRecorder.openCamera()
                }
                dialog.show(supportFragmentManager)
            }
            textDevInfo.text =
                String.format((getString(R.string.text_device_info)), "${productId}_$deviceName")
            tvCopy.setOnClickListener {
                copyTextToClipboard(
                    this@DuplexVideoActivity,
                    tvP2pInfo.text.toString().substringAfter(":")
                )
                showToast("已复制p2p信息")
            }
            llMike.setOnClickListener {
                cameraRecorder.isMuted = !cameraRecorder.isMuted
                if (cameraRecorder.isMuted) {
                    ivMike.setImageResource(R.mipmap.icon_mike_close)
                    btnMike.text = "麦克风关"
                } else {
                    ivMike.setImageResource(R.mipmap.icon_mike_open)
                    btnMike.text = "麦克风开"
                }
            }
            llSpeaker.setOnClickListener {
//                player.isSpeakerOn = !player.isSpeakerOn
//                if (player.isSpeakerOn) {
//                    ivSpeaker.setImageResource(R.mipmap.icon_speaker_open)
//                    btnSpeaker.text = "扬声器开"
//                } else {
//                    ivSpeaker.setImageResource(R.mipmap.icon_speaker_close)
//                    btnSpeaker.text = "扬声器关"
//                }
            }
            llVideo.setOnClickListener {
                if (cameraRecorder.isRunning) {
                    cameraRecorder.closeCamera()
                    binding.textureViewDuplex.isVisible = false
                    ivVideo.setImageResource(R.mipmap.icon_video_close)
                    btnVideo.text = "摄像头关"
                } else {
                    binding.textureViewDuplex.isVisible = true
                    ivVideo.setImageResource(R.mipmap.icon_video_open)
                    btnVideo.text = "摄像头开"
                    cameraRecorder.openCamera()
                }
            }
        }
    }

    private fun updateP2pInfo() {
        val p2pInfo = VideoNativeInterface.getInstance().p2pInfo
        if (p2pInfo?.isNotEmpty() == true) {
            if (!binding.tvP2pInfo.text.toString().contains(p2pInfo)) {
                showToast("P2PInfo 已更新")
            }
            binding.tvP2pInfo.text = String.format(getString(R.string.text_p2p_info), p2pInfo)
            if (Build.VERSION.SDK_INT >= 28) {
                handler.postDelayed(taskRunnable, UPDATE_P2P_INFO_TOKEN, 10000)
            } else {
                handler.postDelayed(taskRunnable, 10000)
            }
        }
    }

    private val taskRunnable = Runnable {
        updateP2pInfo()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(UPDATE_P2P_INFO_TOKEN)
    }

    override fun onOnline(netDateTime: Long) {
        super.onOnline(netDateTime)
        lifecycleScope.launch(Dispatchers.Main) {
            updateP2pInfo()
            if (Build.VERSION.SDK_INT >= 28) {
                handler.postDelayed(taskRunnable, UPDATE_P2P_INFO_TOKEN, 10000)
            } else {
                handler.postDelayed(taskRunnable, 10000)
            }
        }
    }

    override fun onGetAvEncInfo(visitor: Int, channel: Int, videoResType: Int): AvDataInfo {
        return AvDataInfo.createDefaultAvDataInfo(videoResType)
    }

    override fun onStartRealPlay(visitor: Int, channel: Int, videoResType: Int) {
        super.onStartRealPlay(visitor, channel, videoResType)
        cameraRecorder.startRecording(visitor, channel, videoResType)
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
            lifecycleScope.launch {
                adjustAspectRatio(width, height, binding.surfaceViewDuplex)
            }
            player.startVideoPlay(Surface(remotePreviewSurface), visitor, type, height, width)
        } else {
            Log.d(TAG, "IvStartRecvVideoStream mRemotePreviewSurface is null visitor $visitor")
            -1
        }
    }
}