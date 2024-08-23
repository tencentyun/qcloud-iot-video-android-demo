package com.tencent.iotvideo.ivdemo

import android.content.Context
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.util.Log
import android.view.TextureView
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.tencent.iot.voip.device.VoipNativeInterface
import com.tencent.iot.voip.device.callback.IvAvtCallback
import com.tencent.iot.voip.device.callback.IvDeviceCallback
import com.tencent.iot.voip.device.consts.CommandType
import com.tencent.iot.voip.device.consts.P2pEventType
import com.tencent.iot.voip.device.model.AvtInitInfo
import com.tencent.iot.voip.device.model.DeviceInfo
import com.tencent.iot.voip.device.model.SysInitInfo
import com.tencent.iot.voipdemo.databinding.ActivityIpcBinding
import com.tencent.iotvideo.link.CameraRecorder
import com.tencent.iotvideo.link.SimplePlayer

open class IPCActivity : AppCompatActivity(), IvAvtCallback ***REMOVED***
    private val binding: ActivityIpcBinding by lazy ***REMOVED***
        ActivityIpcBinding.inflate(layoutInflater)
  ***REMOVED***
    protected val mPlayer: SimplePlayer by lazy ***REMOVED*** SimplePlayer() }
    protected val mCameraRecorder: CameraRecorder by lazy ***REMOVED*** CameraRecorder() }

    // view for remote video
    protected var mRemoteView: TextureView? = null

    // view for local camera preview
    protected var mLocalPreviewSurface: SurfaceTexture? = null

    // view for remote preview
    protected var mRemotePreviewSurface: SurfaceTexture? = null

    // set device info
    protected val mProductId: String? by lazy ***REMOVED*** intent.getStringExtra("productId") }
    protected val mDeviceName: String? by lazy ***REMOVED*** intent.getStringExtra("deviceName") }
    protected val deviceKey: String? by lazy ***REMOVED*** intent.getStringExtra("deviceKey") }

    protected open fun initWidget() ***REMOVED***
        setContentView(binding.root)

        //        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        with(binding) ***REMOVED***
            // Find the TextureView in the layout
            btnIpcCall.setOnClickListener(View.OnClickListener ***REMOVED***
                val time = System.currentTimeMillis()
                val timeD = time - lastClickTime
                //防止频繁点击
                if (timeD in 1..999) ***REMOVED***
                    Toast.makeText(this@IPCActivity, "频繁点击！", Toast.LENGTH_SHORT).show()
                    return@OnClickListener
              ***REMOVED***
                lastClickTime = time
                // msg_id 6: 按门铃
//                IVoipJNIBridge.getInstance().sendMsgNotice(6);
          ***REMOVED***)
      ***REMOVED***
  ***REMOVED***


    override fun onCreate(savedInstanceState: Bundle?) ***REMOVED***
        Log.d(TAG, "start create")
        super.onCreate(savedInstanceState)
        val region = "china"
        val devinfo = "$mProductId/$mDeviceName"
        // local AV files for playback
        val fileOps = AssetFileOps()
        val path = filesDir.absolutePath
        Log.d(TAG, "path is $path")
        val audioFileName = "audio_sample16000_stereo_64kbps.aac"
        val absAudioFileName = "$path/$audioFileName"
        val videoFileName = "video_size320x180_gop50_fps25.h264"
        val absVideoFileName = "$path/$videoFileName"
        fileOps.copyFileFromAssets(applicationContext, audioFileName, absAudioFileName)
        fileOps.copyFileFromAssets(applicationContext, videoFileName, absVideoFileName)

        // start run JNI iot_video_demo
        val info = SysInitInfo.createDefaultSysInitInfo(
            DeviceInfo(mProductId, mDeviceName, deviceKey, region)
        )
        VoipNativeInterface.getInstance().initIvSystem(info, object : IvDeviceCallback ***REMOVED***
            override fun onOnline(netDateTime: Long) ***REMOVED***
                Log.d(TAG, "netDateTime--->$netDateTime")
          ***REMOVED***

            override fun onOffline(status: Int) ***REMOVED***
                Log.d(TAG, "status--->$status")
          ***REMOVED***

            override fun onModuleStatus(moduleStatus: Int) ***REMOVED***
                Log.d(TAG, "moduleStatus--->$moduleStatus")
          ***REMOVED***
      ***REMOVED***)
        VoipNativeInterface.getInstance().initIvDm()
        val avtInitInfo = AvtInitInfo.createDefaultAvtInitInfo()
        VoipNativeInterface.getInstance().initIvAvt(avtInitInfo, this)
        Log.d(TAG, "run iot_video_demo for $devinfo")
        initWidget()
        binding.textIpcDevinfo.text = devinfo
  ***REMOVED***

    override fun onDestroy() ***REMOVED***
        VoipNativeInterface.getInstance().ivAvtExit()
        VoipNativeInterface.getInstance().ivDmExit()
        VoipNativeInterface.getInstance().ivSysExit()
        super.onDestroy()
  ***REMOVED***

    override fun onStartRealPlay(visitor: Int, channel: Int, res_type: Int, `object`: Any) ***REMOVED***
        Log.d(TAG, "start send for visitor $visitor channel $channel res_type $res_type")
        mCameraRecorder.startRecording(visitor, res_type)
  ***REMOVED***

    override fun onStopRealPlay(visitor: Int, channel: Int, res_type: Int) ***REMOVED***
        Log.d(TAG, "stop send for visitor $visitor channel $channel res_type $res_type")
        mCameraRecorder.stopRecording(visitor, res_type)
  ***REMOVED***

    override fun onStartRecvAudioStream(
        visitor: Int,
        channel: Int,
        type: Int,
        option: Int,
        mode: Int,
        width: Int,
        sample_rate: Int,
        sample_num: Int
    ): Int ***REMOVED***
        Log.d(TAG, "IvStartRecvAudioStream visitor $visitor")
        return mPlayer.startAudioPlay(visitor, type, option, mode, width, sample_rate, sample_num)
  ***REMOVED***

    override fun onStartRecvVideoStream(
        visitor: Int,
        channel: Int,
        type: Int,
        height: Int,
        width: Int
    ): Int ***REMOVED***
        Log.w(TAG, "video stream is not supported in this activity")
        return 0
  ***REMOVED***

    override fun onNotify(event: Int, visitor: Int, channel: Int, videoResType: Int) ***REMOVED***
        var msg = ""
        when (event) ***REMOVED***
            P2pEventType.IV_AVT_EVENT_P2P_PEER_CONNECT_FAIL, P2pEventType.IV_AVT_EVENT_P2P_PEER_ERROR -> ***REMOVED***
                Log.d(TAG, "receive event: peer error")
                msg = "network err"
          ***REMOVED***

            P2pEventType.IV_AVT_EVENT_P2P_PEER_ADDR_CHANGED -> ***REMOVED***
                Log.d(TAG, "receive event: peer addr change")
                msg = "peer change"
          ***REMOVED***

            P2pEventType.IV_AVT_EVENT_P2P_PEER_READY, P2pEventType.IV_AVT_EVENT_P2P_WATERMARK_LOW, P2pEventType.IV_AVT_EVENT_P2P_WATERMARK_WARN, P2pEventType.IV_AVT_EVENT_P2P_WATERMARK_HIGH, P2pEventType.IV_AVT_EVENT_P2P_LOCAL_NET_READY -> ***REMOVED***}
            else -> ***REMOVED***
                Log.d(TAG, "not support event")
                msg = "unsupport event type $event"
          ***REMOVED***
      ***REMOVED***
        updateUI(this, msg)
  ***REMOVED***

    override fun onStopRecvStream(visitor: Int, channel: Int, streamType: Int): Int ***REMOVED***
        Log.d(TAG, "visitor $visitor stream_type $streamType stopped")
        return if (streamType == 1) ***REMOVED***
            mPlayer.stopVideoPlay(visitor)
      ***REMOVED*** else ***REMOVED***
            mPlayer.stopAudioPlay(visitor)
      ***REMOVED***
  ***REMOVED***

    override fun onRecvStream(
        visitor: Int,
        streamType: Int,
        data: ByteArray,
        len: Int,
        pts: Long,
        seq: Long
    ): Int ***REMOVED***
        if (streamType == 1) ***REMOVED***
            return mPlayer.playVideoStream(visitor, data, len, pts, seq)
      ***REMOVED*** else if (streamType == 0) ***REMOVED***
            return mPlayer.playAudioStream(visitor, data, len, pts, seq)
      ***REMOVED***
        return 0
  ***REMOVED***

    override fun onRecvCommand(
        command: Int,
        visitor: Int,
        channel: Int,
        videoResType: Int,
        args: Any
    ): Int ***REMOVED***
        var msg = ""
        when (command) ***REMOVED***
            CommandType.IV_AVT_COMMAND_USR_DATA -> ***REMOVED***
                Log.d(TAG, "receive command: user data")
                msg = "user data"
          ***REMOVED***

            CommandType.IV_AVT_COMMAND_REQ_STREAM -> ***REMOVED***
                Log.d(TAG, "receive command: request stream")
                msg = "request stream"
          ***REMOVED***

            CommandType.IV_AVT_COMMAND_CHN_NAME -> ***REMOVED***
                Log.d(TAG, "receive command: get channel name")
                msg = "get channel name"
          ***REMOVED***

            CommandType.IV_AVT_COMMAND_REQ_IFRAME -> ***REMOVED***
                Log.d(TAG, "receive command: request I frame")
                msg = "request I frame"
          ***REMOVED***

            CommandType.IV_AVT_COMMAND_PLAYBACK_PAUSE -> ***REMOVED***
                Log.d(TAG, "receive command: playback pause")
                msg = "playback pause"
          ***REMOVED***

            CommandType.IV_AVT_COMMAND_PLAYBACK_RESUME -> ***REMOVED***
                Log.d(TAG, "receive command: playback resume")
                msg = "playback resume"
          ***REMOVED***

            CommandType.IV_AVT_COMMAND_PLAYBACK_QUERY_MONTH -> ***REMOVED***
                Log.d(TAG, "receive command: playback query month")
                msg = "playback query month"
          ***REMOVED***

            CommandType.IV_AVT_COMMAND_PLAYBACK_QUERY_DAY -> ***REMOVED***
                Log.d(TAG, "receive command: playback query day")
                msg = "playback query day"
          ***REMOVED***

            CommandType.IV_AVT_COMMAND_PLAYBACK_SEEK -> ***REMOVED***
                Log.d(TAG, "receive command: playback seek")
                msg = "playback seek"
          ***REMOVED***

            CommandType.IV_AVT_COMMAND_PLAYBACK_FF -> ***REMOVED***
                Log.d(TAG, "receive command: playback fast forward")
                msg = "playback fast forward"
          ***REMOVED***

            CommandType.IV_AVT_COMMAND_PLAYBACK_SPEED -> ***REMOVED***
                Log.d(TAG, "receive command: playback speed")
                msg = "playback speed"
          ***REMOVED***

            CommandType.IV_AVT_COMMAND_PLAYBACK_REWIND -> ***REMOVED***
                Log.d(TAG, "receive command: playback rewind")
                msg = "playback rewind"
          ***REMOVED***

            CommandType.IV_AVT_COMMAND_PLAYBACK_PROGRESS -> ***REMOVED***
                Log.d(TAG, "receive command: playback progress")
                msg = "playback progress"
          ***REMOVED***

            CommandType.IV_AVT_COMMAND_QUERY_FILE_LIST -> ***REMOVED***
                Log.d(TAG, "receive command: get file list")
                msg = "get file list"
          ***REMOVED***

            CommandType.IV_AVT_COMMAND_CALL_ANSWER -> ***REMOVED***
                Log.d(TAG, "receive command: call answer")
                msg = "call answer"
          ***REMOVED***

            CommandType.IV_AVT_COMMAND_CALL_HANG_UP -> ***REMOVED***
                Log.d(TAG, "receive command: call hang up")
                msg = "call hang up"
          ***REMOVED***

            CommandType.IV_AVT_COMMAND_CALL_REJECT -> ***REMOVED***
                Log.d(TAG, "receive command: call reject")
                msg = "call reject"
          ***REMOVED***

            CommandType.IV_AVT_COMMAND_CALL_CANCEL -> ***REMOVED***
                Log.d(TAG, "receive command: call cancel")
                msg = "call cancel"
          ***REMOVED***

            CommandType.IV_AVT_COMMAND_CALL_BUSY -> ***REMOVED***
                Log.d(TAG, "receive command: call busy")
                msg = "call busy"
          ***REMOVED***

            CommandType.IV_AVT_COMMAND_CALL_TIMEOUT -> ***REMOVED***
                Log.d(TAG, "receive command: call timeout")
                msg = "call timeout"
          ***REMOVED***

            else -> ***REMOVED***
                Log.d(TAG, "not support command")
                msg = "unsupport cmd type $command"
          ***REMOVED***
      ***REMOVED***
        // Toast.makeText(IPCActivity.this, msg, Toast.LENGTH_SHORT).show();
        updateUI(this, msg)
        return 0
  ***REMOVED***

    override fun onDownloadFile(status: Int, visitor: Int, channel: Int, args: Any): Int ***REMOVED***
        return 0
  ***REMOVED***

    override fun onGetPeerOuterNet(visitor: Int, channel: Int, netInfo: String) ***REMOVED***
  ***REMOVED***

    fun updateUI(context: Context, msg: String?) ***REMOVED***
        (context as IPCActivity).runOnUiThread ***REMOVED*** //此时已在主线程中，可以更新UI了
            Toast.makeText(this@IPCActivity, msg, Toast.LENGTH_SHORT).show()
      ***REMOVED***
  ***REMOVED***

    companion object ***REMOVED***
        private val TAG: String = IPCActivity::class.java.simpleName

        private var lastClickTime: Long = 0
  ***REMOVED***
}