package com.example.ivdemo

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.tencent.iot.twcall.R
import com.tencent.iot.twcall.databinding.ActivityClientBinding
import com.tencent.iot.video.device.VideoNativeInterface
import com.tencent.iot.video.device.callback.IvClientCallback
import com.tencent.iot.video.device.model.IvPeerInfo
import java.nio.ByteBuffer

class ClientActivity : AppCompatActivity(), IvClientCallback {
    private val binding by lazy { ActivityClientBinding.inflate(layoutInflater) }
    private val TAG = ClientActivity::class.simpleName
    private var clientId: Long = 0
    private var isOnline = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with(binding) {
            setContentView(root)
            titleLayout.tvTitle.text = getString(R.string.title_log)
            titleLayout.ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
            clientId = VideoNativeInterface.getInstance()
                .createClient(this@ClientActivity, ByteBuffer.allocateDirect(1000))
            connectClient.setOnClickListener {
                val ivPeerInfo = IvPeerInfo()
                ivPeerInfo.productId = etProductId.text.toString()
                ivPeerInfo.deviceName = etDeviceName.text.toString()
                ivPeerInfo.xp2pInfo = etXp2pInfo.text.toString()
                Log.d(TAG, "ivPeerInfo:$ivPeerInfo" + " clientId:$clientId")
                VideoNativeInterface.getInstance().setupConnectClient(clientId, ivPeerInfo)
                isOnline = true
            }
            sendCommand.setOnClickListener {
                val command = etCommand.text.toString()
                VideoNativeInterface.getInstance()
                    .sendClientCommand(clientId, command.toByteArray(), 0)
            }

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        VideoNativeInterface.getInstance().destroyClient(clientId)
    }

    override fun printLog(msg: String?) {
        Log.d(TAG, "printLog msg:$msg")
    }

    override fun onStartVideoStream(
        connId: String?,
        type: Int,
        height: Int,
        width: Int,
        frameRate: Int,
        param: ByteBuffer?
    ) {
        Log.d(
            TAG,
            "onStartVideoStream connId:$connId  type:$type height:$height width:$width  frameRate:$frameRate param:$param"
        )
    }

    override fun onStartAudioStream(
        connId: String?,
        type: Int,
        option: Int,
        mode: Int,
        width: Int,
        sample_rate: Int,
        sample_num: Int,
        param: ByteBuffer?
    ) {
        Log.d(
            TAG,
            "onStartAudioStream connId:$connId  type:$type sample_rate:$sample_rate width:$width  sample_num:$sample_num param:$param"
        )
    }

    override fun onStopStream(connId: String?, streamType: Int, param: ByteBuffer?) {
        Log.d(
            TAG,
            "onStopStream connId:$connId  streamType:$streamType param:$param"
        )
    }

    override fun onStreamPacket(
        connId: String?,
        streamType: Int,
        data: ByteArray?,
        pts: Long,
        seq: Long,
        param: ByteBuffer?
    ) {
        Log.d(
            TAG,
            "onStopStream connId:$connId  streamType:$streamType param:$param"
        )
    }

    override fun onEventNotify(connId: String?, event: Int, param: ByteBuffer?) {
        Log.d(
            TAG,
            "onStopStream connId:$connId  event:$event param:$param"
        )
    }

    override fun onPeerFeedback(connId: String?, msg: ByteArray?, param: ByteBuffer?): ByteArray {
        Log.d(
            TAG,
            "onPeerFeedback connId:$connId  msg:$msg param:$param"
        )
        return ByteArray(0)
    }
}
