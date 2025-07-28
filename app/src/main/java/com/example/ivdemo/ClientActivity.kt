package com.example.ivdemo

import android.content.Context
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.tencent.iot.twcall.BuildConfig
import com.tencent.iot.twcall.R
import com.tencent.iot.twcall.databinding.ActivityClientBinding
import com.tencent.iot.video.device.VideoNativeInterface
import com.tencent.iot.video.device.annotations.CmFrameType
import com.tencent.iot.video.device.annotations.StreamType
import com.tencent.iot.video.device.annotations.VideoResType
import com.tencent.iot.video.device.callback.IvClientCallback
import com.tencent.iot.video.device.consts.P2pEventType
import com.tencent.iot.video.device.model.AvDataInfo
import com.tencent.iot.video.device.model.IvPeerInfo
import com.tencent.iot.video.device.model.IvStreamInfo
import com.tencent.iotvideo.link.CameraRecorder
import com.tencent.iotvideo.link.SimplePlayer
import com.tencent.iotvideo.link.listener.OnEncodeListener
import com.tencent.iotvideo.link.util.adjustAspectRatio
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.nio.ByteBuffer
import kotlin.random.Random

private const val COMMAND_WX_CALL_START = "wx_call_start" //小程序发起请求
private const val COMMAND_WX_CALL_CANCEL = "wx_call_cancel" //小程序取消呼叫
private const val COMMAND_WX_CALL_HANGUP = "wx_call_hangup" //小程序挂断
private const val RESPONSE_DEVICE_CALL_ANSWER = "call_answer" // 对端接听
private const val RESPONSE_DEVICE_CALL_REJECT = "call_reject" // 对端拒听
private const val RESPONSE_DEVICE_CALL_HANGUP = "call_hang_up" // 对端挂断

class ClientActivity : AppCompatActivity(), IvClientCallback, OnEncodeListener {
    private val binding by lazy { ActivityClientBinding.inflate(layoutInflater) }
    private val TAG = ClientActivity::class.simpleName
    private var clientId: Long = 0
    
    @Volatile
    private var onCall = false    // 在通话中
    @Volatile
    private var isCalling = false     // 正在呼叫
    private var isClientReady = false
    private var isConnected = false
    private val cameraRecorder = CameraRecorder()
    private var localPreviewSurface: SurfaceTexture? = null
    private var remotePreviewSurface: SurfaceTexture? = null
    private var player = SimplePlayer()
    private val mVideoNativeInterface by lazy { VideoNativeInterface.getInstance() }

    private var condition1 = false
    private var condition2 = false
    private val lock = Any()
    private var surface: Surface? = null

    private var type = 0
    private var height = 0
    private var width = 0

    private val listener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            if (surface == binding.tvLocalVideo.surfaceTexture) {
                Log.d(TAG, "onSurfaceTextureAvailable: local preview")
                // Initialize the SurfaceTexture object
                localPreviewSurface = surface

                // Start the camera encoder
                cameraRecorder.setPreviewView(binding.tvLocalVideo)
                cameraRecorder.openCamera()
            } else if (surface == binding.tvRemoteVideo.surfaceTexture) {
                Log.d(TAG, "onSurfaceTextureAvailable: remote preview")
                remotePreviewSurface = surface
                
                synchronized(lock) {
                    condition1 = true
                    checkConditions()
                }
            }
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            if (surface == binding.tvLocalVideo.surfaceTexture) {
                // Stop the camera encoder
                cameraRecorder.closeCamera()
            }

            return false
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initView()
    }

    private fun initView() {
        cameraRecorder.init(this)
        cameraRecorder.setOnEncodeListener(this)
        player.setContext(this)

        with(binding) {
            // Set the SurfaceTextureListener on the TextureView
            tvLocalVideo.surfaceTextureListener = listener
            tvRemoteVideo.surfaceTextureListener = listener
            titleLayout.tvTitle.text = getString(R.string.title_call_device)
            titleLayout.ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

            val buffer = ByteBuffer.allocateDirect(1000)
//            var p2PConnStateInfo = P2PConnStateInfo() // 有需要用户可以自定义参数数据，需序列化
            clientId = mVideoNativeInterface
                .createClient(this@ClientActivity, buffer)
            cameraRecorder.setClientId(clientId)
            Log.d(TAG, "create client, clientId: $clientId")

            val prefs = getSharedPreferences("device_call_info", MODE_PRIVATE)
            etProductId.setText(prefs.getString("productId", ""))
            etDeviceName.setText(prefs.getString("deviceName", ""))

            btnConnectClient.setOnClickListener {
                val ivPeerInfo = IvPeerInfo()
                ivPeerInfo.productId = etProductId.text.toString()
                ivPeerInfo.deviceName = etDeviceName.text.toString()
                ivPeerInfo.xp2pInfo = etXp2pInfo.text.toString()
                val res = mVideoNativeInterface.setupConnectClient(clientId, ivPeerInfo)

                val result = when (res) {
                    0 -> {
                        updateConnectState(true)

                        getSharedPreferences("device_call_info", MODE_PRIVATE).edit {
                            putString("productId", ivPeerInfo.productId)
                            putString("deviceName", ivPeerInfo.deviceName)
                        }

                        "成功连接到设备"
                    }
                    else -> "连接失败"
                }

                Log.d(TAG,
                    "Connect to device, productId: ${ivPeerInfo.productId}, deviceName: ${ivPeerInfo.deviceName}, " +
                            "p2pInfo: ${if (BuildConfig.DEBUG) ivPeerInfo.xp2pInfo else "null"}, clientId: $clientId, result: $result")

                Toast.makeText(
                    this@ClientActivity,
                    "Connect to device, result: $res",
                    Toast.LENGTH_SHORT
                ).show()
            }

            sendCommand.setOnClickListener {
                val command = etCommand.text.toString()
                val res = mVideoNativeInterface.sendClientCommand(clientId, command.toByteArray(), 0)
                val response = String(res)

                Log.d(TAG, "sendCommand, command: $command, response: $response")
            }

            btnStartCall.setOnClickListener { callDevice() }

            btnCallHangUp.setOnClickListener {
                if (isCalling) {
                    cancelCall()
                }

                if (onCall) {
                    hangUp()
                }
            }

            btnDisconnectClient.setOnClickListener {
                if (isConnected) {
                    mVideoNativeInterface.teardownConnect(clientId)
                    updateConnectState(false)
                }
            }
        }
    }

    private fun callDevice() {
        // Send call request command
        val callCommand = getCommand(cmd = COMMAND_WX_CALL_START)
        val res = sendCommand(callCommand)

        // 必须先启动收流，否则接收不到信令
        if (isClientReady && isConnected && res == 0) {
            updateCallState(true)
            startClientRecvStream()
            startClientSendStream()
        } else {
            Toast.makeText(this, "呼叫失败，请检查连接", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startClientRecvStream() {
        // Start receive stream
        val info = IvStreamInfo()
        info.channel = 0
        info.isDisableCrypt = false
        info.resType = VideoResType.IV_AVT_VIDEO_RES_SD
//        info.maxFrameSize = 1000
        val rid = StringBuilder()
        createRandom(rid)
        val resId = rid.toString()
        val requester = "server-voip"
        val recvStreamRes =
            mVideoNativeInterface.startClientRecvStream(clientId, info, resId, requester)

        // Teardown connection when call failed
        if (recvStreamRes != 0) {
            Log.e(TAG, "callDevice: StartClientRecvStream failed: $recvStreamRes")
            Toast.makeText(
                this,
                "启动收流失败，请尝试重新连接，result: $recvStreamRes",
                Toast.LENGTH_SHORT
            ).show()

            mVideoNativeInterface.teardownConnect(clientId)
            isConnected = false

            return
        }

        Toast.makeText(this, "成功启动收流", Toast.LENGTH_SHORT).show()
    }

    private fun startClientSendStream() {
        val streamInfo = IvStreamInfo()
        streamInfo.channel = 0
        streamInfo.isDisableCrypt = false
        streamInfo.resType = VideoResType.IV_AVT_VIDEO_RES_SD
        streamInfo.maxFrameSize = 1024
        val dataInfo = AvDataInfo.createDefaultAvDataInfo(VideoResType.IV_AVT_VIDEO_RES_SD)
        val res =
            mVideoNativeInterface.startClientSendStream(clientId, streamInfo, dataInfo)
        val resMsg = when(res) {
            0 -> "成功启动推流"
            else -> "推流失败"
        }

        Toast.makeText(this, resMsg, Toast.LENGTH_SHORT).show()
        Log.d(TAG, "startClientSendStream: res: $res, client send buffer size: ${mVideoNativeInterface.getClientSendBufferSize(clientId)}")
    }

    private fun hangUp() {
        // Send call request command
        val hangUpCommand = getCommand(cmd = COMMAND_WX_CALL_HANGUP)
        sendCommand(hangUpCommand)

        // Stop receive and send stream
        cameraRecorder.stopRecording(0, type)
        mVideoNativeInterface.stopClientRecvStream(clientId)
        mVideoNativeInterface.stopClientSendStream(clientId)
        updateCallState(false)
        Toast.makeText(this, "已挂断", Toast.LENGTH_SHORT).show()
    }

    private fun cancelCall() {
        // Send call request command
        val cancelCommand = getCommand(cmd = COMMAND_WX_CALL_CANCEL)
        sendCommand(cancelCommand)

        // Stop receive and send stream
        mVideoNativeInterface.stopClientRecvStream(clientId)
        mVideoNativeInterface.stopClientSendStream(clientId)
        updateCallState(false)
        Toast.makeText(this, "已取消呼叫", Toast.LENGTH_SHORT).show()
    }

    private fun updateCallState(isCalling: Boolean) {
        if (this.isCalling != isCalling) {
            this.isCalling = isCalling
        }

        with(binding) {
            callLayout.visibility = if (isCalling) View.VISIBLE else View.GONE
            tvLocalVideo.bringToFront()
            tipText.visibility = if (isCalling) View.VISIBLE else View.GONE
            inputLayout.visibility = if (isCalling) View.INVISIBLE else View.VISIBLE
        }
    }

    private fun updateConnectState(connected: Boolean) {
        if (isConnected != connected) {
            isConnected = connected
        }

        with(binding) {
            btnStartCall.isEnabled = connected
            sendCommand.isEnabled = connected
            etCommand.isEnabled = connected
            btnDisconnectClient.isEnabled = connected
            btnConnectClient.isEnabled = !connected
            etProductId.isEnabled = !connected
            etDeviceName.isEnabled = !connected
            etXp2pInfo.isEnabled = !connected
        }
    }

    private fun checkConditions() {
        Log.d(
            TAG, "checkConditions: condition1: $condition1, condition2: $condition2, remote surface: $remotePreviewSurface, surface: $surface"
        )

        if (condition1 && condition2 && remotePreviewSurface != null && surface == null) {
            surface = Surface(remotePreviewSurface)
            player.startVideoPlay(surface, 0, type, height, width)
        }
    }

    override fun onDestroy() {
        with(mVideoNativeInterface) {
            if (onCall) {
                val hangUpCommand = getCommand(cmd = COMMAND_WX_CALL_HANGUP)
                sendClientCommand(clientId, hangUpCommand.toByteArray(), 0)
                stopClientSendStream(clientId)
                stopClientRecvStream(clientId)
            }

            if (isConnected) {
                teardownConnect(clientId)
            }

            destroyClient(clientId)
        }

        super.onDestroy()
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

        this.type = type
        this.height = height
        this.width = width

        lifecycleScope.launch {
//            adjustAspectRatio(width, height, binding.tvRemoteVideo, resources.displayMetrics.widthPixels, resources.displayMetrics.heightPixels)
            val params = binding.tvRemoteVideo.layoutParams as ConstraintLayout.LayoutParams
            params.dimensionRatio = "H,$height:$width"
            binding.tvRemoteVideo.layoutParams = params
        }

        synchronized(lock) {
            condition2 = true
            checkConditions()
        }
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
            "onStartAudioStream connId:$connId  type:$type option: $option mode: $mode sample_rate:$sample_rate width:$width  sample_num:$sample_num param:$param"
        )

        player.startAudioPlay(0, type, option, mode, width, sample_rate, sample_num)
    }

    override fun onStopStream(connId: String?, streamType: Int, param: ByteBuffer?) {
        Log.d(
            TAG,
            "onStopStream connId:$connId  streamType:$streamType param:$param"
        )
        cameraRecorder.closeCamera()
        player.stopVideoPlay(0)
        player.stopAudioPlay(0)
    }

    override fun onStreamPacket(
        connId: String?,
        streamType: Int,
        data: ByteArray?,
        pts: Long,
        seq: Long,
        param: ByteBuffer?
    ) {
        if (!onCall) return

        when (streamType) {
            StreamType.IV_AVT_STREAM_TYPE_VIDEO -> data?.let {
                player.playVideoStream(0, data, data.size, pts, seq)
            }

            StreamType.IV_AVT_STREAM_TYPE_AUDIO -> data?.let {
                player.playAudioStream(0, data, data.size, pts, seq)
            }

            StreamType.IV_AVT_STREAM_TYPE_AV -> {
            }

            StreamType.IV_AVT_STREAM_TYPE_MJPG -> {
            }
        }
    }

    override fun onEventNotify(connId: String?, event: Int, param: ByteBuffer?) {
        Log.d(
            TAG,
            "onEventNotify connId:$connId  event:$event param:$param"
        )

        when (event) {
            P2pEventType.IV_AVT_EVENT_P2P_PEER_READY -> {
                isClientReady = true
            }
            P2pEventType.IV_AVT_EVENT_P2P_PEER_ERROR -> {
                if (isClientReady) {
                    isClientReady = false
                }
            }
            P2pEventType.IV_AVT_EVENT_P2P_LINK_DISCONNECT -> {
                lifecycleScope.launch {
                    updateConnectState(false)
                    Toast.makeText(this@ClientActivity, "设备已断连，请重试", Toast.LENGTH_SHORT).show()
                }
                Log.w(TAG, "onEventNotify, p2p client link with remote peer is disconnected")
            }
            P2pEventType.IV_AVT_EVENT_P2P_CLI_SEND_CLOSE -> {}
            // ......
        }
    }

    override fun onPeerFeedback(connId: String?, msg: ByteArray?, param: ByteBuffer?): ByteArray {
        val m = msg?.let { String(it) }
        Log.e(
            TAG,
            "onPeerFeedback connId:$connId  msg:$m param:${param}"
        )

        lifecycleScope.launch {
            m?.let {
                val msgJson = JSONObject(m)

                when (msgJson.optString("iv_private_cmd", "")) {
                    RESPONSE_DEVICE_CALL_ANSWER -> {
                        if (isCalling) {
                            onCall = true
                            isCalling = false
                            binding.tipText.visibility = View.INVISIBLE
                            binding.tvRemoteVideo.bringToFront()
                            binding.tvLocalVideo.bringToFront()
                            cameraRecorder.startRecording(0, 0, type)
                            Toast.makeText(this@ClientActivity, "对方已接听", Toast.LENGTH_SHORT).show()
                        }
                    }

                    RESPONSE_DEVICE_CALL_REJECT -> {
                        if (isCalling) {
                            updateCallState(false)
                            Toast.makeText(this@ClientActivity, "对方已拒绝", Toast.LENGTH_SHORT).show()
                        }
                    }

                    RESPONSE_DEVICE_CALL_HANGUP -> {
                        if (onCall) {
                            onCall = false
                            updateCallState(false)
                            cameraRecorder.stopRecording(0, type)
                            // Stop receive and send stream
                            mVideoNativeInterface.stopClientRecvStream(clientId)
                            mVideoNativeInterface.stopClientSendStream(clientId)
                            Toast.makeText(this@ClientActivity, "对方已挂断", Toast.LENGTH_SHORT).show()
                        }
                    }

                   else ->
                        Toast.makeText(this@ClientActivity, "对端回发了一个消息 msg: $m param: $param", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val jsonResp = JSONObject()
        jsonResp.put("status", 0)

        return jsonResp.toString().toByteArray()
    }

    /**
     * 生成一个32位随机数，并将其16进制字符串写入传入的StringBuilder中
     * @param rid 用于存放16进制字符串的StringBuilder，长度应足够
     * @return 生成的随机数
     */
    private fun createRandom(rid: StringBuilder): Int {
        val randomNum = Random.nextInt()
        val hexStr = randomNum.toUInt().toString(16)
        rid.clear()
        rid.append(hexStr)

        return randomNum
    }

    /**
     * 拼接信令
     */
    private fun getCommand(
        action: String = "user_define",
        channel: Int = 0,
        cmd: String,
        type: String? = null,
        quality: String? = null
    ): String {
        val params = mutableListOf<String>()

        if (action.isNotEmpty()) {
            params.add("action=$action")
        }

        params.add("channel=$channel")
        params.add("cmd=$cmd")

        if (!type.isNullOrEmpty()) {
            params.add("type=$type")
        }

        if (!quality.isNullOrEmpty()) {
            params.add("quality=$quality")
        }

        return params.joinToString("&")
    }

    private fun getNewSize(width: Int, height: Int): Size? {
        if (width < 0 || height < 0) {
            Log.w(TAG, "getNewSize: width or height must >= 0!")
            return null
        }

        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        val ratio = height.toFloat() / width
        var newWidth = width
        var newHeight = height

        if (width < screenHeight) {
            newWidth = screenHeight
            newHeight = (screenWidth * ratio).toInt()
        }

        Log.d(
            TAG,
            "getNewSize: width: $width, height: $height, newWidth: $newWidth, newHeight: $newHeight"
        )

        return Size(newWidth, newHeight)
    }

    private fun sendCommand(cmd: String?, timeout: Long = 5000L): Int {
        if (!cmd.isNullOrEmpty()) {
            val responseResult = mVideoNativeInterface.sendClientCommand(clientId, cmd.toByteArray(), timeout)
            val res = String(responseResult)
            val jsonResult = JSONObject(res)
            val code = jsonResult.getInt("code")
            val msg = jsonResult.getString("errMsg")
            Log.d(TAG, "send client command: $cmd result: $res")

            if (code != 0) {
                Log.e(TAG, "send client command error, code: $code, msg: $msg")
            }

            return code
        } else {
            Log.w(TAG, "send client command error, cmd is null or empty")

            return -1
        }
    }

    override fun onAudioEncoded(datas: ByteArray?, pts: Long, seq: Long) {
        if (!onCall) return

        datas?.let {
            val res =
                mVideoNativeInterface.sendClientAudioStream(clientId, datas, pts, seq.toInt())

            if (res != 0) {
                Log.w(TAG, "onAudioEncoded: pts: $pts, seq: $seq, seq to int: ${seq.toInt()}, data size: ${datas.size}, res: $res")
            }
        }
    }

    override fun onVideoEncoded(datas: ByteArray?, pts: Long, seq: Long, isKeyFrame: Boolean) {
        if (!onCall) return

        datas?.let {
            val type = if (isKeyFrame) CmFrameType.IV_CM_FRAME_TYPE_I else CmFrameType.IV_CM_FRAME_TYPE_P
            val res =
                mVideoNativeInterface.sendClientVideoStream(clientId, datas, pts, type, seq.toInt())

            if (res != 0) {
                Log.w(TAG, "onVideoEncoded: pts: $pts, seq: $seq, seq to int: ${seq.toInt()}, type: $type, isKeyFrame: $isKeyFrame, data size: ${datas.size}, res: $res")
            }
        }
    }
}
