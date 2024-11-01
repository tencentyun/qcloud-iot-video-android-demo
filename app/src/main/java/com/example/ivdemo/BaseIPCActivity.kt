package com.example.ivdemo

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.tencent.iot.video.device.VideoNativeInterface
import com.tencent.iot.video.device.annotations.LogLevelType
import com.tencent.iot.video.device.callback.IvAvtCallback
import com.tencent.iot.video.device.callback.IvDeviceCallback
import com.tencent.iot.video.device.consts.CommandType
import com.tencent.iot.video.device.consts.P2pEventType
import com.tencent.iot.video.device.model.AvDataInfo
import com.tencent.iot.video.device.model.AvtInitInfo
import com.tencent.iot.video.device.model.SysInitInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

private val TAG = IPCActivity::class.java.simpleName

abstract class BaseIPCActivity<VB : ViewBinding> : AppCompatActivity(), IvDeviceCallback,
    IvAvtCallback {
    protected val productId: String? by lazy { intent.getStringExtra("productId") }
    protected val deviceName: String? by lazy { intent.getStringExtra("deviceName") }
    protected val deviceKey: String? by lazy { intent.getStringExtra("deviceKey") }
    protected val binding by lazy { getViewBinding() }
    private val region = "china"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!checkPerformCreate()) return
        // set device info
        onPerformCreate()
    }

    protected open fun checkPerformCreate() = true

    protected fun onPerformCreate() {
        Log.d(TAG, "run iot_video_demo for ${productId}/${deviceName}")
        initVideoNative()
        setContentView(binding.root)
        initView()
    }

    /**
     * init jni iot video
     */
    private fun initVideoNative() {
        // start run JNI iot_video_demo
        VideoNativeInterface.getInstance().initLog(LogLevelType.IV_eLOG_DEBUG)
        val sysInitInfo = SysInitInfo(productId, deviceName, deviceKey, region)
        VideoNativeInterface.getInstance().initIvSystem(sysInitInfo, this)
        VideoNativeInterface.getInstance().initIvDm()
        val avtInitInfo = AvtInitInfo()
        VideoNativeInterface.getInstance().initIvAvt(avtInitInfo, this)
    }

    protected abstract fun getViewBinding(): VB

    protected abstract fun initView()

    override fun onDestroy() {
        lifecycleScope.launch(Dispatchers.IO) {
            val exitIvAvt = VideoNativeInterface.getInstance().exitIvAvt()
            Log.d(TAG, "exit avt res:$exitIvAvt")
            val exitIvDm = VideoNativeInterface.getInstance().exitIvDm()
            Log.d(TAG, "exit dm res:$exitIvDm")
            val exitIvSys = VideoNativeInterface.getInstance().exitIvSys()
            Log.d(TAG, "exit sys res:$exitIvSys")
        }
        super.onDestroy()
    }

    override fun onOnline(netDateTime: Long) {
        Log.d(TAG, "onOnline  netDateTime--->$netDateTime")
        showToast("设备上线")
    }

    override fun onOffline(status: Int) {
        Log.d(TAG, "onOffline  status--->$status")
        showToast("设备下线")
    }

    override fun onModuleStatus(moduleStatus: Int) {
        Log.d(TAG, "moduleStatus--->$moduleStatus")
    }

    override fun onGetAvEncInfo(visitor: Int, channel: Int, videoResType: Int): AvDataInfo {
        return AvDataInfo.createDefaultAvDataInfo(videoResType)
    }

    override fun onStartRealPlay(visitor: Int, channel: Int, videoResType: Int) {
        Log.d(TAG, "onStartRealPlay  visitor $visitor channel $channel res_type $videoResType")
    }

    override fun onStopRealPlay(visitor: Int, channel: Int, videoResType: Int) {
        Log.d(TAG, "onStopRealPlay  visitor $visitor channel $channel res_type $videoResType")
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
        Log.d(TAG, "onStartRecvAudioStream visitor $visitor")
        return 0
    }

    override fun onStartRecvVideoStream(
        visitor: Int,
        channel: Int,
        type: Int,
        height: Int,
        width: Int,
        frameRate: Int
    ): Int {
        Log.d(TAG, "onStartRecvVideoStream  video stream is not supported in this activity")
        return 0
    }

    override fun onStopRecvStream(visitor: Int, channel: Int, streamType: Int): Int {
        Log.d(TAG, "onStopRecvStream visitor $visitor stream_type $streamType stopped")
        return 0
    }

    override fun onRecvStream(
        visitor: Int,
        streamType: Int,
        data: ByteArray?,
        len: Int,
        pts: Long,
        seq: Long
    ): Int {
        Log.d(
            TAG,
            "onRecvStream visitor $visitor stream_type $streamType data$data  len$len   pts$pts   seq$seq"
        )
        return 0
    }

    override fun onNotify(event: Int, visitor: Int, channel: Int, videoResType: Int) {
        Log.w(TAG, "onNotify()")
        var msg = ""
        when (event) {
            P2pEventType.IV_AVT_EVENT_P2P_PEER_CONNECT_FAIL, P2pEventType.IV_AVT_EVENT_P2P_PEER_ERROR -> {
                Log.d(TAG, "receive event: peer error")
                msg = "network err"
            }

            P2pEventType.IV_AVT_EVENT_P2P_PEER_ADDR_CHANGED -> {
                Log.d(TAG, "receive event: peer addr change")
                msg = "peer change"
            }

            P2pEventType.IV_AVT_EVENT_P2P_PEER_READY, P2pEventType.IV_AVT_EVENT_P2P_WATERMARK_LOW, P2pEventType.IV_AVT_EVENT_P2P_WATERMARK_WARN, P2pEventType.IV_AVT_EVENT_P2P_WATERMARK_HIGH, P2pEventType.IV_AVT_EVENT_P2P_LOCAL_NET_READY -> {}
            else -> {
                Log.d(TAG, "not support event")
                msg = "unsupport event type $event"
            }
        }
        if (msg.isNotEmpty()) {
            showToast(msg)
        }
    }

    override fun onRecvCommand(
        command: Int,
        visitor: Int,
        channel: Int,
        videoResType: Int,
        args: String?
    ): String {
        Log.d(
            TAG,
            "onRecvCommand command $command visitor $visitor channel$channel   videoResType$videoResType   args$args"
        )
        val msg: String = when (command) {
            CommandType.IV_AVT_COMMAND_USR_DATA -> {
                Log.d(TAG, "receive command: user data")
                "user data"
            }

            CommandType.IV_AVT_COMMAND_REQ_STREAM -> {
                Log.d(TAG, "receive command: request stream")
                "request stream"
            }

            CommandType.IV_AVT_COMMAND_CHN_NAME -> {
                Log.d(TAG, "receive command: get channel name")
                "get channel name"
            }

            CommandType.IV_AVT_COMMAND_REQ_IFRAME -> {
                Log.d(TAG, "receive command: request I frame")
                "request I frame"
            }

            CommandType.IV_AVT_COMMAND_PLAYBACK_PAUSE -> {
                Log.d(TAG, "receive command: playback pause")
                "playback pause"
            }

            CommandType.IV_AVT_COMMAND_PLAYBACK_RESUME -> {
                Log.d(TAG, "receive command: playback resume")
                "playback resume"
            }

            CommandType.IV_AVT_COMMAND_PLAYBACK_QUERY_MONTH -> {
                Log.d(TAG, "receive command: playback query month")
                "playback query month"
            }

            CommandType.IV_AVT_COMMAND_PLAYBACK_QUERY_DAY -> {
                Log.d(TAG, "receive command: playback query day")
                "playback query day"
            }

            CommandType.IV_AVT_COMMAND_PLAYBACK_SEEK -> {
                Log.d(TAG, "receive command: playback seek")
                "playback seek"
            }

            CommandType.IV_AVT_COMMAND_PLAYBACK_FF -> {
                Log.d(TAG, "receive command: playback fast forward")
                "playback fast forward"
            }

            CommandType.IV_AVT_COMMAND_PLAYBACK_SPEED -> {
                Log.d(TAG, "receive command: playback speed")
                "playback speed"
            }

            CommandType.IV_AVT_COMMAND_PLAYBACK_REWIND -> {
                Log.d(TAG, "receive command: playback rewind")
                "playback rewind"
            }

            CommandType.IV_AVT_COMMAND_PLAYBACK_PROGRESS -> {
                Log.d(TAG, "receive command: playback progress")
                "playback progress"
            }

            CommandType.IV_AVT_COMMAND_QUERY_FILE_LIST -> {
                Log.d(TAG, "receive command: get file list")
                "get file list"
            }

            CommandType.IV_AVT_COMMAND_CALL_ANSWER -> {
                Log.d(TAG, "receive command: call answer")
                "call answer"
            }

            CommandType.IV_AVT_COMMAND_CALL_HANG_UP -> {
                Log.d(TAG, "receive command: call hang up")
                "call hang up"
            }

            CommandType.IV_AVT_COMMAND_CALL_REJECT -> {
                Log.d(TAG, "receive command: call reject")
                "call reject"
            }

            CommandType.IV_AVT_COMMAND_CALL_CANCEL -> {
                Log.d(TAG, "receive command: call cancel")
                "call cancel"
            }

            CommandType.IV_AVT_COMMAND_CALL_BUSY -> {
                Log.d(TAG, "receive command: call busy")
                "call busy"
            }

            CommandType.IV_AVT_COMMAND_CALL_TIMEOUT -> {
                Log.d(TAG, "receive command: call timeout")
                "call timeout"
            }

            else -> {
                Log.d(TAG, "not support command")
                "unsupport cmd type $command"
            }
        }
        showToast(msg)
        val res = JSONObject()
        res.put("code", 0)
        res.put("msg", "success")
        return res.toString()
    }

    override fun onDownloadFile(status: Int, visitor: Int, channel: Int): Int {
        Log.d(TAG, "onDownloadFile status $status visitor $visitor channel$channel")
        return 0
    }

    override fun onGetPeerOuterNet(visitor: Int, channel: Int, netInfo: String?) {
        Log.d(TAG, "onGetPeerOuterNet visitor $visitor channel $channel netInfo$netInfo")
    }

    fun showToast(msg: String) {
        lifecycleScope.launch {
            Toast.makeText(this@BaseIPCActivity.applicationContext, msg, Toast.LENGTH_SHORT).show();
        }
    }
}