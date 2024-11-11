package com.example.ivdemo

import android.graphics.SurfaceTexture
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.ivdemo.popup.QualitySettingDialog
import com.tencent.iot.twcall.R
import com.tencent.iot.twcall.databinding.ActivityCustomDuplexVideoBinding
import com.tencent.iot.video.device.VideoNativeInterface
import com.tencent.iotvideo.link.CameraRecorder
import com.tencent.iotvideo.link.SimplePlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject

private const val COMMAND_WX_CALL_START = "wx_call_start" //小程序发起请求
private const val COMMAND_WX_CALL_CANCEL = "wx_call_cancel" //小程序取消呼叫
private const val COMMAND_WX_CALL_HANGUP = "wx_call_hangup" //小程序挂断
private val TAG: String = CustomDuplexVideoActivity::class.java.simpleName

class CustomDuplexVideoActivity : BaseIPCActivity<ActivityCustomDuplexVideoBinding>() {

    @Volatile
    private var isCalling = false

    private var condition1 = false
    private var condition2 = false
    private val lock = Any()

    private var type = 0
    private var height = 0
    private var width = 0

    private var surface: Surface? = null

    private var localPreviewSurface: SurfaceTexture? = null
    private var remotePreviewSurface: SurfaceTexture? = null
    private var player = SimplePlayer()
    private val cameraRecorder = CameraRecorder()
    private val handler = Handler(Looper.getMainLooper())
    private val UPDATE_P2P_INFO_TOKEN = "update_p2p_info_token"


    private val listener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            if (surface == binding.textureViewDuplex.surfaceTexture) {
                // Initialize the SurfaceTexture object
                localPreviewSurface = surface

                // Start the camera encoder
                cameraRecorder.openCamera(localPreviewSurface, this@CustomDuplexVideoActivity)
            } else if (surface == binding.surfaceViewDuplex.surfaceTexture) {
                remotePreviewSurface = surface
                synchronized(lock) {
                    condition1 = true
                    checkConditions()
                }
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

    override fun getViewBinding(): ActivityCustomDuplexVideoBinding =
        ActivityCustomDuplexVideoBinding.inflate(layoutInflater)

    override fun initView() {
        with(binding) {
            // Set the SurfaceTextureListener on the TextureView
            textureViewDuplex.surfaceTextureListener = listener
            surfaceViewDuplex.surfaceTextureListener = listener
            titleLayout.tvTitle.text = getString(R.string.title_custom_audio_video_call)
            titleLayout.ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
            titleLayout.ivRightBtn.isVisible = true
            titleLayout.ivRightBtn.setOnClickListener {
                val dialog = QualitySettingDialog(this@CustomDuplexVideoActivity)
                dialog.setOnDismissListener {
                    cameraRecorder.closeCamera()
                    cameraRecorder.openCamera(localPreviewSurface, this@CustomDuplexVideoActivity)
                }
                dialog.show(supportFragmentManager)
            }
            textDevInfo.text =
                String.format((getString(R.string.text_device_info)), "${productId}_$deviceName")


            btnRejectListen.setOnClickListener {
                sendCommand("call_reject")
                finishStream()
                updateCancelCallUI()
            }
            btnHangUp.setOnClickListener {
                sendCommand("call_hang_up")
                finishStream()
                isCalling = false
                updateCancelCallUI()
            }

            btnAnswer.setOnClickListener {
                sendCommand("call_answer")
                isCalling = true
                btnRejectListen.visibility = View.GONE
                btnAnswer.visibility = View.GONE
                btnHangUp.visibility = View.VISIBLE
                tipText.visibility = View.VISIBLE
                updateAnswerUI()
            }
        }
    }

    private fun updateP2pInfo() {
        val p2pInfo = VideoNativeInterface.getInstance().p2pInfo
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

    override fun onStartRecvVideoStream(
        visitor: Int,
        channel: Int,
        type: Int,
        height: Int,
        width: Int,
        frameRate: Int
    ): Int {
        Log.d(TAG, "start video visitor $visitor h: $height w: $width")
        this.type = type
        this.height = height
        this.width = width
        synchronized(lock) {
            condition2 = true
            checkConditions()
        }
        if (remotePreviewSurface != null) {
            return 0
        } else {
            Log.d(
                TAG,
                "IvStartRecvVideoStream mRemotePreviewSurface is null visitor $visitor"
            )
            return -1
        }
    }

    private fun checkConditions() {
        if (condition1 && condition2 && remotePreviewSurface != null && surface == null) {
            surface = Surface(remotePreviewSurface)
            player.startVideoPlay(surface, visitor, type, height, width)
        }
    }

    override fun onStartRealPlay(visitor: Int, channel: Int, videoResType: Int) {
        super.onStartRealPlay(visitor, channel, videoResType)
        cameraRecorder.startRecording(visitor, videoResType)
    }

    override fun onStopRealPlay(visitor: Int, channel: Int, videoResType: Int) {
        cameraRecorder.stopRecording(visitor, videoResType)
    }

    override fun onRecvStream(
        visitor: Int,
        streamType: Int,
        data: ByteArray?,
        len: Int,
        pts: Long,
        seq: Long
    ): Int {
        if (!isCalling) return 0
        Log.d(
            TAG,
            "onRecvStream visitor $visitor stream_type $streamType data$data  len$len   pts$pts   seq$seq"
        )
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
        runOnUiThread {
            when (args) {
                COMMAND_WX_CALL_START -> updateCallUI()
                COMMAND_WX_CALL_CANCEL -> updateCancelCallUI()
                COMMAND_WX_CALL_HANGUP -> {
                    isCalling = false
                    updateCancelCallUI()
                }

                "wx_call_timeout" -> {}
            }
        }
        val resJson = JSONObject()
        try {
            resJson.put("code", 0)
            resJson.put("errMsg", "")
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return resJson.toString()
    }

    private fun finishStream() {
        VideoNativeInterface.getInstance().sendFinishStreamMsg(visitor, channel, videoResType)
        VideoNativeInterface.getInstance().sendFinishStream(visitor, channel, videoResType)
    }

    private fun sendCommand(order: String): Boolean {
        val jsonObject = JSONObject()
        try {
            jsonObject.put("iv_private_cmd", order)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val res =
            VideoNativeInterface.getInstance().sendCommand(visitor, jsonObject.toString(), 1 * 1000)
        Toast.makeText(this, "发送发送:$jsonObject   信令发送结果：$res", Toast.LENGTH_SHORT).show()
        return !TextUtils.isEmpty(res)
    }

    private fun updateCallUI() {
        with(binding) {
            btnRejectListen.visibility = View.VISIBLE
            btnAnswer.visibility = View.VISIBLE
            btnHangUp.visibility = View.GONE
            clCall.visibility = View.VISIBLE
            tipText.visibility = View.VISIBLE
            tipText.bringToFront()
            textureViewDuplex.visibility = View.VISIBLE
        }
    }

    private fun updateCancelCallUI() {
        with(binding) {
            clCall.visibility = View.GONE
            surfaceViewDuplex.visibility = View.INVISIBLE
            textureViewDuplex.visibility = View.INVISIBLE
        }
    }

    private fun updateAnswerUI() {
        with(binding) {
            surfaceViewDuplex.visibility = View.VISIBLE
            surfaceViewDuplex.bringToFront()
            textureViewDuplex.visibility = View.VISIBLE
            textureViewDuplex.bringToFront()
        }
    }
}