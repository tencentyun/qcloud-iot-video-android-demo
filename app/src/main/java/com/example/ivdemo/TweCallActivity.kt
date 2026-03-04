package com.example.ivdemo

import android.app.ProgressDialog
import android.content.Intent
import android.graphics.SurfaceTexture
import android.text.TextUtils
import android.util.Log
import android.view.Surface
import android.view.TextureView.SurfaceTextureListener
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.ivdemo.adapter.UserListAdapter
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.tencent.iot.twcall.R
import com.tencent.iot.twcall.databinding.ActivityTweCallBinding
import com.tencent.iot.video.device.VideoNativeInterface
import com.tencent.iot.video.device.annotations.AudioEncType
import com.tencent.iot.video.device.annotations.CallType
import com.tencent.iot.video.device.annotations.PixelType
import com.tencent.iot.video.device.annotations.StreamType
import com.tencent.iot.video.device.annotations.VideoEncType
import com.tencent.iot.video.device.annotations.VoipActivateType
import com.tencent.iot.video.device.annotations.VoipCalledStatus
import com.tencent.iot.video.device.annotations.VoipRecvVFpsType
import com.tencent.iot.video.device.annotations.VoipRecvVRotateType
import com.tencent.iot.video.device.callback.IvUCCallback
import com.tencent.iot.video.device.callback.IvVoipCallback
import com.tencent.iot.video.device.consts.CommandType
import com.tencent.iot.video.device.consts.IvErrCode
import com.tencent.iot.video.device.model.AvDataInfo
import com.tencent.iot.video.device.model.VoipVideoInfo
import com.tencent.iotvideo.link.CameraRecorder
import com.tencent.iotvideo.link.SimplePlayer
import com.tencent.iotvideo.link.consts.CallState
import com.tencent.iotvideo.link.entity.UserEntity
import com.tencent.iotvideo.link.util.DeviceSetting
import com.tencent.iotvideo.link.util.QualitySetting
import com.tencent.iotvideo.link.util.adjustAspectRatio
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

private val TAG: String = TweCallActivity::class.java.simpleName
private const val DATA_PATH = "/storage/emulated/0/"
private const val INCOMING_CALL_TIMEOUT = 55 * 1000L
private const val IS_DEBUG = true

class TweCallActivity : BaseIPCActivity<ActivityTweCallBinding>(), IvVoipCallback, IvUCCallback {

    @Volatile
    private var initStatus = -1 // 未初始化 -1， 初始化成功 0， 其他

    @Volatile
    private var callState = CallState.IDLE
    private var condition1 = false
    private var condition2 = false
    private val lock = Any()

    private var type = 0
    private var height = 0
    private var width = 0
    private val player = SimplePlayer()

    private val cameraRecorder = CameraRecorder()

    private var localPreviewSurface: SurfaceTexture? = null

    private var remotePreviewSurface: SurfaceTexture? = null
    private var dialog: ProgressDialog? = null
    private var mIncomingCallJob: Job? = null

    // wx twe call init
    private var modelId: String? = null
    private var deviceId: String? = null
    private var wxaAppId: String? = null

    private var roomId: String? = null
    private var openId: String = ""
    private val miniProgramVersion by lazy {
        intent.getIntExtra(
            "miniProgramVersion", 0
        )
    } //0 "正式版", 1  "开发版", 2 "体验版"

    private val deviceSetting by lazy { DeviceSetting.getInstance(this@TweCallActivity) }

    private val userListAdapter = UserListAdapter()

    private val surfaceTextureListener = object : SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            if (surface == binding.textureViewTweCall.surfaceTexture) {
                // Initialize the SurfaceTexture object
                localPreviewSurface = surface

                // Start the camera encoder
                cameraRecorder.setPreviewView(binding.textureViewTweCall)
                cameraRecorder.openCamera()
            } else if (surface == binding.surfaceViewTweCall.surfaceTexture) {
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
            if (surface == binding.textureViewTweCall.surfaceTexture) {
                // Stop the camera encoder
                cameraRecorder.closeCamera()
            }
            return true
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            // Not used in this example
        }

    }

    override fun checkPerformCreate(): Boolean {
        parseIntent(intent)
        return true
    }

    private fun parseIntent(intent: Intent) {
        modelId = intent.getStringExtra("model_id")
        deviceId = intent.getStringExtra("device_id")
        wxaAppId = intent.getStringExtra("app_id")
    }

    override fun getViewBinding(): ActivityTweCallBinding =
        ActivityTweCallBinding.inflate(layoutInflater)

    override fun initView() {
        cameraRecorder.init(this)
        with(binding) {
            titleLayout.tvTitle.text = getString(R.string.title_tweCall)
            titleLayout.ivRightBtn.isVisible = true
            titleLayout.ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
            titleLayout.ivRightBtn.isVisible = false
            textDevInfo.text = String.format(getString(R.string.text_device_info), deviceId)
            // 如果可以确定每个item的高度是固定的，设置这个选项可以提高性能
            rvUserList.setHasFixedSize(true)
            // 设置适配器，刷新展示用户列表
            userListAdapter.setOnSelectListener { position: Int, userEntity: UserEntity ->
                etOpenid.setText(userEntity.openId)
            }
            userListAdapter.submitList(deviceSetting.openIdList)
            rvUserList.setAdapter(userListAdapter)
            textureViewTweCall.requestFocus()
            textureViewTweCall.surfaceTextureListener = surfaceTextureListener
            surfaceViewTweCall.surfaceTextureListener = surfaceTextureListener
            dialog =
                ProgressDialog.show(
                    this@TweCallActivity,
                    "",
                    "正在加载初始化initWxCloudTweCall",
                    true
                )
            btnTweCallVideoCall.setOnClickListener {
                if (!checkCallCondition()) return@setOnClickListener
                textureViewTweCall.isVisible = true
                dialog =
                    ProgressDialog.show(this@TweCallActivity, "", "呼叫中doWxCloudTweCall", true)
                executeCallV2(true)
            }
            btnTweCallAudioCall.setOnClickListener {
                if (!checkCallCondition()) return@setOnClickListener
                textureViewTweCall.isVisible = false
                dialog =
                    ProgressDialog.show(
                        this@TweCallActivity,
                        "",
                        "呼叫中doWxCloudTweCallAudioCall",
                        true
                    )
                executeCallV2(false)
            }
            btnTweCallHangUp.setOnClickListener {
                if (initStatus == -1) {
                    showToast("initWxCloudTweCall还未完成初始化")
                    return@setOnClickListener
                }
                if (initStatus != 0) {
                    showToast("initWxCloudTweCall初始化失败：$initStatus")
                    return@setOnClickListener
                }

                if (callState == CallState.IDLE) {
                    showToast("未开启通话")
                    return@setOnClickListener
                }

                if (roomId != null && callState == CallState.INCOMING_CALL) {
                    Log.d(TAG, "reject incoming call, roomId: $roomId")
                    dialog = ProgressDialog.show(
                        this@TweCallActivity,
                        "",
                        "拒接来电，roomId: $roomId",
                        true
                    )
                    replyRoomCall(VoipCalledStatus.VOIP_CALLED_STATUS_REFUSE)
                    tvBeCallStatus.text = getString(R.string.wx_voip_refuse)
                    updateBeCallUI(false)
                    roomId = null
                } else {
                    dialog =
                        ProgressDialog.show(
                            this@TweCallActivity,
                            "",
                            "挂断doWxCloudTweCallHangUp",
                            true
                        )
                    hangUpV2()
                }

                callState = CallState.IDLE
            }

            btnTweCallAnswer.setOnClickListener {
                if (initStatus == -1) {
                    showToast("initWxCloudTweCall还未完成初始化")
                    return@setOnClickListener
                }

                if (initStatus != 0) {
                    showToast("initWxCloudTweCall初始化失败：$initStatus")
                    return@setOnClickListener
                }

                if (roomId == null) {
                    showToast("roomId 为空，无法接听通话")
                    return@setOnClickListener
                }

                dialog = ProgressDialog.show(this@TweCallActivity, "", "正在加入通话", true)
                replyRoomCall(VoipCalledStatus.VOIP_CALLED_STATUS_ACCEPT)
            }

            btnAiTalk.setOnClickListener {
                showToast("查询 AI 连接 Url")
                queryWebSocketUrl()
            }

            if (IS_DEBUG) {
                btnTweCallBusy.setOnClickListener {
                    if (initStatus == -1) {
                        showToast("initWxCloudTweCall还未完成初始化")
                        return@setOnClickListener
                    }

                    if (initStatus != 0) {
                        showToast("initWxCloudTweCall初始化失败：$initStatus")
                        return@setOnClickListener
                    }

                    if (roomId == null) {
                        showToast("roomId 为空，无法进行通话占线操作")
                        return@setOnClickListener
                    }

                    if (roomId != null && callState == CallState.INCOMING_CALL) {
                        Log.d(TAG, "busy action for incoming call, roomId: $roomId")
                        dialog = ProgressDialog.show(
                            this@TweCallActivity,
                            "",
                            "占线来电，roomId: $roomId",
                            true
                        )
                        replyRoomCall(VoipCalledStatus.VOIP_CALLED_STATUS_BUSY)
                        tvBeCallStatus.text = getString(R.string.wx_voip_busy)
                        updateBeCallUI(false)
                        callState = CallState.IDLE
                        roomId = null
                    }
                }
            }
        }
    }

    private fun initTweCall() {
        checkDefaultThreadActiveAndExecuteTask {
            initStatus = initWxCloudTweCallV2()
            if (initStatus == 19 || initStatus == -379) {                //把device_key文件删掉
                deleteDeviceKeyFile(DATA_PATH)
                initStatus = initWxCloudTweCallV2()
            }
            if (initStatus != 0) {
                dismissDialog()
                showToast("初始化失败，resCode:$initStatus")
            } else {
                showToast("twecall初始化成功")
                val activeDeviceInfo = VideoNativeInterface.getInstance().voipActiveDeviceInfoV2
                if (activeDeviceInfo == null || activeDeviceInfo.expireTime < System.currentTimeMillis() / 1000) {
                    val selectActiveType =
                        intent.getIntExtra("activeType", VoipActivateType.VOIP_ACT_IPC)
                    Log.i(TAG, "激活类型:$selectActiveType")
                    val activateRes = VideoNativeInterface.getInstance()
                        .activateVoipLicenseV2(selectActiveType)
                    if (activateRes == 0) {
                        showToast("检查设备过期，激活结果成功，resCode:$activateRes")
                    } else {
                        showToast("检查设备过期，激活结果失败，resCode:$activateRes")
                    }
                }
            }
        }
    }

    /**
     * 初始化 TweCall,暂停维护，请使用v2接口
     *
     * @return 初始化状态值
     */
    private fun initWxCloudTweCall(): Int {
        val initStatus = VideoNativeInterface.getInstance()
            .initWxCloudVoip(DATA_PATH, modelId, deviceId, wxaAppId, miniProgramVersion)
        Log.i(TAG, "reInitWxCloudTweCall initStatus: $initStatus")
        if (initStatus == 0) {
            val registeredState = VideoNativeInterface.getInstance().isAvtVoipRegistered()
            Log.i(TAG, "isAvtTweCallRegistered: $registeredState")
            if (registeredState == 0) {
                val registerRes = VideoNativeInterface.getInstance().registerAvtVoip("")
                Log.i(TAG, "registerAvtTweCall registerRes: $registerRes")
            }
            dismissDialog()
        }
        return initStatus
    }

    /**
     * 初始化 TweCall
     *
     * @return 初始化状态值
     */
    private fun initWxCloudTweCallV2(): Int {
        val initStatus = VideoNativeInterface.getInstance()
            .initWxCloudVoipV2(DATA_PATH, modelId, wxaAppId, miniProgramVersion, this)
        Log.i(TAG, "initWxCloudVoipV2 initStatus: $initStatus")
        if (initStatus == 0) {
            dismissDialog()
        }
        return initStatus
    }

    private fun checkCallCondition(): Boolean {
        openId = binding.etOpenid.text.toString()
        if (TextUtils.isEmpty(openId)) {
            showToast("请输入被呼叫的用户openid！")
            return false
        } else {
            deviceSetting.addOnlyEntity(UserEntity(openId, true))
            userListAdapter.notifyDataSetChanged()
        }
        if (initStatus == -1) {
            showToast("initWxCloudTweCall还未完成初始化")
            return false
        } else if (initStatus != 0) {
            showToast("initWxCloudTweCall初始化失败：$initStatus")
            return false
        }
        return true
    }

    /**
     * 呼叫,暂停维护，请使用v2接口
     * @param isVideo
     */
    private fun executeCall(isVideo: Boolean) {
        checkDefaultThreadActiveAndExecuteTask {
            // call
            @PixelType val recvPixel =
                QualitySetting.getInstance(this@TweCallActivity).wxResolution
            val calleeCameraSwitch =
                if (isVideo) QualitySetting.getInstance(this@TweCallActivity).isWxCameraOn else true
            val callType =
                if (isVideo) CallType.IV_CM_STREAM_TYPE_VIDEO else CallType.IV_CM_STREAM_TYPE_AUDIO
            val videoInfo = VoipVideoInfo(
                VideoEncType.IV_CM_VENC_TYPE_H264,
                VideoEncType.IV_CM_VENC_TYPE_H264,
                recvPixel,
                AudioEncType.IV_CM_AENC_TYPE_AAC,
                VoipRecvVFpsType.VOIP_RECV_V_FPS_MAX,
                VoipRecvVRotateType.VOIP_RECV_V_ROTATE_NONE,
                0, 0
            )
            val customMsg = ""
            val res = VideoNativeInterface.getInstance().doWxCloudVoipCall(
                modelId, wxaAppId, openId, deviceId, customMsg,
                callType, videoInfo, true, calleeCameraSwitch
            )
            val result = when (res) {
                -2 -> "通话中"
                0 -> "呼叫成功"
                else -> "呼叫失败"
            }
            Log.i(TAG, " call result: $result, resCode: $res")
            dismissDialog {
                showToast("$result,resCode:$res")
                if (isVideo) updateVideoUI(true) else updateAudioUI(true)
            }
        }
    }

    /**
     * 呼叫
     * @param isVideo
     */
    private fun executeCallV2(isVideo: Boolean) {
        checkDefaultThreadActiveAndExecuteTask {
            // call
            @PixelType val recvPixel =
                if (isVideo) QualitySetting.getInstance(this@TweCallActivity).wxResolution else PixelType.IV_CM_PIXEL_VARIABLE
            val calleeCameraSwitch =
                if (isVideo) QualitySetting.getInstance(this@TweCallActivity).isWxCameraOn else true
            val callType =
                if (isVideo) CallType.IV_CM_STREAM_TYPE_VIDEO else CallType.IV_CM_STREAM_TYPE_AUDIO
            val videoInfo = VoipVideoInfo(
                VideoEncType.IV_CM_VENC_TYPE_H264,
                VideoEncType.IV_CM_VENC_TYPE_H264,
                recvPixel,
                AudioEncType.IV_CM_AENC_TYPE_AAC,
                VoipRecvVFpsType.VOIP_RECV_V_FPS_MAX,
                VoipRecvVRotateType.VOIP_RECV_V_ROTATE_NONE,
                0, 0
            )
            val customMsg = ""
            val res = VideoNativeInterface.getInstance()
                .doWxCloudVoipCallV2(
                    openId, customMsg, callType, videoInfo, true, calleeCameraSwitch
                )
            val result = when (res) {
                -2 -> "通话中"
                0 -> {
                    callState = CallState.IS_CALLING
                    "呼叫成功"
                }

                else -> "呼叫失败"
            }
            Log.i(TAG, " call result: $result, resCode: $res")
            dismissDialog {
                showToast("$result,resCode:$res")
                if (isVideo) updateVideoUI(true) else updateAudioUI(true)
            }
        }
    }

    /**
     * 挂断，暂停维护，请使用v2接口
     */
    private fun hangUp() {
        checkDefaultThreadActiveAndExecuteTask {
            val res = VideoNativeInterface.getInstance().doWxCloudVoipHangUp(
                productId, deviceName, openId, deviceId
            )
            val result = if (res == 0) "已挂断" else "挂断失败"
            Log.i(TAG, "TweCall call result: $result  resCode:$res")
            dismissDialog {
                showToast("$result,resCode:$res")
                binding.tvTips.text = result
                updateVideoUI(false)
                updateAudioUI(false)
            }
        }
    }

    /**
     * 挂断
     */
    private fun hangUpV2() {
        checkDefaultThreadActiveAndExecuteTask {
            val res = VideoNativeInterface.getInstance().doWxCloudVoipHangUpV2()
            val result = if (res == 0) "已挂断" else "挂断失败"
            Log.i(TAG, "TweCall call result: $result  resCode:$res")
            dismissDialog {
                showToast("$result,resCode:$res")
                binding.tvTips.text = result
                updateVideoUI(false)
                updateAudioUI(false)
            }
        }
    }

    private fun replyRoomCall(@VoipCalledStatus reply: Int) {
        cancelIncomingCallJob()

        checkDefaultThreadActiveAndExecuteTask {
            @PixelType val recvPixel = QualitySetting.getInstance(this@TweCallActivity).wxResolution
            val videoInfo = VoipVideoInfo(
                VideoEncType.IV_CM_VENC_TYPE_H264,
                VideoEncType.IV_CM_VENC_TYPE_H264,
                recvPixel,
                AudioEncType.IV_CM_AENC_TYPE_AAC,
                VoipRecvVFpsType.VOIP_RECV_V_FPS_MAX,
                VoipRecvVRotateType.VOIP_RECV_V_ROTATE_NONE,
                0, 0
            )
            val res =
                VideoNativeInterface.getInstance().doWxCloudVoipJoinV2(roomId, videoInfo, reply)

            val result = when (res) {
                IvErrCode.IV_ERR_NONE -> "响应房间成功"
                IvErrCode.IV_ERR_AVT_REQ_CHN_BUSY -> "占线"
                IvErrCode.IV_ERR_AVT_VOIP_EXPIRED -> "服务到期"
                IvErrCode.IV_ERR_AVT_INPUT_PARAM_INVAILD -> "初始化失败或未初始化"
                IvErrCode.IV_ERR_AVT_FAILED -> "其他错误"
                else -> "响应房间失败"
            }

            Log.i(TAG, "replyRoomCall: result: $result, res: $res")

            dismissDialog {
                showToast("$result, resCode: $res")

                if (res == IvErrCode.IV_ERR_NONE) {
                    binding.btnTweCallAnswer.visibility = View.GONE

                    if (IS_DEBUG) {
                        binding.btnTweCallBusy.visibility = View.GONE
                    }

                    if (reply == VoipCalledStatus.VOIP_CALLED_STATUS_ACCEPT) {
                        binding.tvBeCallStatus.isVisible = false
                        callState = CallState.ON_CALL
                    }
                }
            }
        }
    }

    private fun dismissDialog(block: (() -> Unit)? = null) {
        lifecycleScope.launch {
            dialog?.dismiss()
            block?.invoke()
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "destory")
        cancelIncomingCallJob()

        checkDefaultThreadActiveAndExecuteTask {
//            VideoNativeInterface.getInstance().exitWxCloudVoip()
            if (initStatus == 0) {
                VideoNativeInterface.getInstance().exitWxCloudVoipV2()
                Log.d(TAG, "exit twecall v2")
            }

            VideoNativeInterface.getInstance().exitUc()
        }
        super.onDestroy()
    }

    private fun checkConditions() {
        if (condition1 && condition2 && remotePreviewSurface != null) {
            player.startVideoPlay(Surface(remotePreviewSurface), visitor, type, height, width)
        }
    }

    override fun onOnline(netDateTime: Long) {
        super.onOnline(netDateTime)
        initTweCall()
        initUcModule()
    }

    override fun onGetAvEncInfo(visitor: Int, channel: Int, videoResType: Int): AvDataInfo {
        return AvDataInfo.createDefaultAvDataInfo(videoResType)
    }

    override fun onStartRealPlay(visitor: Int, channel: Int, videoResType: Int) {
        super.onStartRealPlay(visitor, channel, videoResType)
        callState = CallState.ON_CALL
        cameraRecorder.startRecording(visitor, channel, videoResType)
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
        Log.d(TAG, "IvStartRecvAudioStream visitor $visitor")
        lifecycleScope.launch { binding.tvTips.text = "通话中" }
        return player.startAudioPlay(visitor, type, option, mode, width, sample_rate, sample_num)
    }

    override fun onStartRecvVideoStream(
        visitor: Int, channel: Int, type: Int, height: Int, width: Int, frameRate: Int
    ): Int {
        Log.d(TAG, "start video visitor $visitor h: $height w: $width")
        this.type = type
        this.height = height
        this.width = width
        lifecycleScope.launch {
            adjustAspectRatio(
                width,
                height,
                binding.surfaceViewTweCall,
                binding.surfaceViewTweCallBg.measuredWidth,
                binding.surfaceViewTweCallBg.measuredHeight
            )
        }
        if (remotePreviewSurface != null) {
            synchronized(lock) {
                condition2 = true
                checkConditions()
            }
            return 0
        } else {
            synchronized(lock) {
                condition2 = true
                checkConditions()
            }
            Log.d(TAG, "IvStartRecvVideoStream mRemotePreviewSurface is null visitor $visitor")
            return -1
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
//        Log.d(
//            TAG,
//            "onRecvStream visitor:$visitor  streamType:$streamType  data:$data  len:$len  pts:$pts  seq:$seq"
//        )
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
        Log.d(
            TAG,
            "onRecvCommand command $command visitor $visitor channel$channel   videoResType$videoResType   args$args"
        )

        if (command == CommandType.IV_AVT_COMMAND_CALL_CANCEL || command == CommandType.IV_AVT_COMMAND_CALL_TIMEOUT) {
            lifecycleScope.launch {
                updateBeCallUI(false)
                callState = CallState.IDLE
                roomId = null

                when (command) {
                    CommandType.IV_AVT_COMMAND_CALL_CANCEL -> {
                        binding.tvBeCallStatus.text = getString(R.string.wx_voip_peer_cancel)
                    }

                    CommandType.IV_AVT_COMMAND_CALL_TIMEOUT -> {
                        binding.tvBeCallStatus.text = getString(R.string.wx_voip_timeout)
                    }
                }
            }
        }

        return super.onRecvCommand(command, visitor, channel, videoResType, args)
    }

    override fun onStopRecvStream(visitor: Int, channel: Int, streamType: Int): Int {
        super.onStopRecvStream(visitor, channel, streamType)
        if (streamType == StreamType.IV_AVT_STREAM_TYPE_VIDEO || streamType == StreamType.IV_AVT_STREAM_TYPE_AV) {
            if (streamType == StreamType.IV_AVT_STREAM_TYPE_VIDEO) {
                player.stopVideoPlay(visitor)
            } else {
                player.stopAudioPlay(visitor)
            }
        }
        return 0
    }

    override fun onStopRealPlay(visitor: Int, channel: Int, videoResType: Int) {
        super.onStopRealPlay(visitor, channel, videoResType)
        cameraRecorder.stopRecording(visitor, videoResType)
        lifecycleScope.launch {
            updateVideoUI(false)
            updateAudioUI(false)
            callState = CallState.IDLE
        }
    }

    private fun deleteDeviceKeyFile(path: String) {
        val fileName = "device_key"

        // 创建一个File对象，表示device_key文件
        val deviceKeyFile = File(path, fileName)

        // 检查文件是否存在
        if (deviceKeyFile.exists()) {
            // 文件存在，尝试删除
            if (deviceKeyFile.delete()) {
                Log.i(TAG, "device_key文件已成功删除。")
            } else {
                Log.i(TAG, "删除device_key文件失败。")
            }
        } else {
            Log.i(TAG, "device_key文件不存在。")
        }
    }

    private fun updateVideoUI(isCalling: Boolean) {
        with(binding) {
            if (isCalling) {
                surfaceViewTweCall.bringToFront()
                textureViewTweCall.bringToFront()
                btnTweCallHangUp.bringToFront()
            }
            surfaceViewTweCallBg.isVisible = isCalling
            surfaceViewTweCall.isVisible = isCalling
            textureViewTweCall.isVisible = isCalling
            btnTweCallHangUp.isVisible = isCalling
            llButtons.isVisible = !isCalling
            llOpenid.isVisible = !isCalling
            tvUserList.isVisible = !isCalling
            rvUserList.isVisible = !isCalling
        }
    }

    private fun updateAudioUI(isCalling: Boolean) {
        with(binding) {
            tvTips.isVisible = isCalling
            ivAudio.isVisible = isCalling
            btnTweCallHangUp.isVisible = isCalling
            llButtons.isVisible = !isCalling
            rvUserList.isVisible = !isCalling
            llOpenid.isVisible = !isCalling
            tvUserList.isVisible = !isCalling
        }
    }

    private fun updateBeCallUI(isShow: Boolean) {
        with(binding) {
            if (isShow) {
                tvBeCallStatus.text = getString(R.string.wx_voip_incoming_call)
                surfaceViewTweCall.bringToFront()
                textureViewTweCall.bringToFront()
                btnTweCallHangUp.bringToFront()
                btnTweCallAnswer.bringToFront()

                if (IS_DEBUG) {
                    btnTweCallBusy.bringToFront()
                }
            }

            llButtons.isVisible = !isShow
            llOpenid.isVisible = !isShow
            tvUserList.isVisible = !isShow
            rvUserList.isVisible = !isShow
            surfaceViewTweCallBg.isVisible = isShow
            surfaceViewTweCall.isVisible = isShow
            textureViewTweCall.isVisible = isShow
            tvBeCallStatus.isVisible = isShow
            btnTweCallAnswer.visibility = if (isShow) View.VISIBLE else View.GONE
            btnTweCallHangUp.isVisible = isShow

            if (IS_DEBUG) {
                btnTweCallBusy.visibility = if (isShow) View.VISIBLE else View.GONE
            }
        }
    }

    private fun cancelIncomingCallJob() {
        mIncomingCallJob?.cancel()
        mIncomingCallJob = null
    }

    //获取激活设备信息
    override fun onUpdateAuthorizeStatus(openId: String?, status: Int): Int {
        Log.d(TAG, "onUpdateAuthorizeStatus   penId:${openId}  status:$status")
        return 0
    }

    override fun onJoinNotify(roomId: String?): Int {
        Log.d(TAG, "onJoinNotify: roomId: $roomId")

        if (callState != CallState.IDLE) {
            replyRoomCall(VoipCalledStatus.VOIP_CALLED_STATUS_BUSY)
            return 0
        }

        this.roomId = roomId

        mIncomingCallJob = lifecycleScope.launch {
            callState = CallState.INCOMING_CALL
            updateBeCallUI(true)
            delay(INCOMING_CALL_TIMEOUT)

            if (callState == CallState.INCOMING_CALL) {
                binding.tvBeCallStatus.text = getString(R.string.wx_voip_timeout)
                showToast("超时无应答")
                callState = CallState.IDLE
                this@TweCallActivity.roomId = null
                delay(1000L)
                updateBeCallUI(false)
            }
        }

        return 0
    }

    override fun onCancelNotify(roomId: String?): Int {
        Log.d(TAG, "onCancelNotify: roomId: $roomId")

        lifecycleScope.launch {
            if (this@TweCallActivity.roomId == roomId) {
                cancelIncomingCallJob()
                binding.tvBeCallStatus.text = getString(R.string.wx_voip_peer_cancel)
                showToast("对方取消呼叫")
                callState = CallState.IDLE
                this@TweCallActivity.roomId = null
                delay(1000L)
                updateBeCallUI(false)
            } else {
                Log.w(TAG, "onCancelNotify: room id is different!")
            }
        }

        return 0;
    }

    /*********** UC *************/

    val serviceDownTopic: String? by lazy {
        "\$twecall/down/service/$productId/$deviceName"
    }

    val serviceUpTopic: String? by lazy {
        "\$twecall/up/service/$productId/$deviceName"
    }

    fun initUcModule() {
        checkDefaultThreadActiveAndExecuteTask {
            val status = VideoNativeInterface.getInstance().initUc(this)

            if (status < 0) {
                showToast("初始化自定义信令模块失败：$status")
                Log.e(TAG, "initUcModule, init uc module error: $status")
                return@checkDefaultThreadActiveAndExecuteTask
            }

            if (isOnline) {
                val subscribeResCode =
                    VideoNativeInterface.getInstance().ucMqttSubscribe(serviceDownTopic)

                if (subscribeResCode != 0) {
                    Log.e(TAG, "initUcModule, subscribe $serviceDownTopic error, res: $subscribeResCode")
                }
            }
        }
    }

    fun queryWebSocketUrl() {
        if (isOnline) {
            val data = JsonObject().apply {
                addProperty("method", "query_websocket_url")
                addProperty("clientToken", "${productId}_${deviceName}")

                val param = JsonObject()
                param.addProperty("connect_type", "talk")

                add("param", param)
            }

            val publishResCode = VideoNativeInterface.getInstance()
                .ucMqttPublish(serviceUpTopic, Gson().toJson(data))

            if (publishResCode != 0) {
                Log.e(TAG, "queryWebSocketUrl, error: $publishResCode")
            }
        }
    }

    override fun onRecvMsg(data: ByteArray?, dataLen: Int) {
        val dataStr = data?.let { String(it) }
        Log.d(TAG, "onRecvMsg, dataStr: $dataStr, dataLen: $dataLen")
    }

    override fun onMqttMsg(payload: String?, payloadLen: Int) {
        if (payload.isNullOrEmpty()) return

        try {
            val jsonObject = Gson().fromJson(payload, JsonObject::class.java)
            val method = jsonObject.get("method")?.asString

            if (method == "query_websocket_url_reply") {
                val clientToken = jsonObject.get("clientToken")?.asString
                val code = jsonObject.get("code")?.asInt
                val status = jsonObject.get("status")?.asString

                val params = jsonObject.getAsJsonObject("params")
                val token = params?.get("token")?.asString
                val websocketUrl = params?.get("websocket_url")?.asString
                val websocketPort = params?.get("websocket_port")?.asInt

                Log.d(TAG, "query_websocket_url_reply 返回结果:")
                Log.d(TAG, "  clientToken: $clientToken")
                Log.d(TAG, "  code: $code")
                Log.d(TAG, "  status: $status")
                Log.d(TAG, "  token: $token")
                Log.d(TAG, "  websocket_url: $websocketUrl")
                Log.d(TAG, "  websocket_port: $websocketPort")
            }
        } catch (e: Exception) {
            // do nothing (其它消息类型)
        }
    }
}