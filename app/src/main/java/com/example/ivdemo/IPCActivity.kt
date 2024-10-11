package com.example.ivdemo

import android.widget.Toast
import com.tencent.iot.twcall.R
import com.tencent.iot.twcall.databinding.ActivityIpcBinding
import com.tencent.iot.video.device.VideoNativeInterface
import com.tencent.iot.video.device.annotations.StreamType
import com.tencent.iot.video.device.model.AvDataInfo
import com.tencent.iotvideo.link.CameraRecorder
import com.tencent.iotvideo.link.SimplePlayer

class IPCActivity : BaseIPCActivity<ActivityIpcBinding>() {

    private val player = SimplePlayer()
    private val cameraRecorder = CameraRecorder()
    private var lastClickTime = 0L

    override fun getViewBinding(): ActivityIpcBinding = ActivityIpcBinding.inflate(layoutInflater)
    override fun initView() {
        with(binding) {
            titleLayout.tvTitle.text = getString(R.string.title_ipc)
            titleLayout.ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
            textDevinfo.text =
                String.format((getString(R.string.text_device_info)), "$productId/$deviceName")
            btnIpcCall.setOnClickListener {
                val time = System.currentTimeMillis()
                val timeD = time - lastClickTime
                //防止频繁点击
                if (timeD in 1..999) {
                    Toast.makeText(this@IPCActivity, "频繁点击！", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                lastClickTime = time
                // msg_id 6: 按门铃
                VideoNativeInterface.getInstance().sendMsgNotice(6)
            }
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
}