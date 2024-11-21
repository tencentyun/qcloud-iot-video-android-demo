package com.example.ivdemo

import android.app.ProgressDialog
import android.content.Intent
import android.graphics.SurfaceTexture
import android.text.TextUtils
import android.util.Log
import android.view.Surface
import android.view.TextureView.SurfaceTextureListener
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.ivdemo.adapter.UserListAdapter
import com.tencent.iot.twcall.R
import com.tencent.iot.twcall.databinding.ActivityTweCallBinding
import com.tencent.iot.video.device.VideoNativeInterface
import com.tencent.iot.video.device.annotations.CallType
import com.tencent.iot.video.device.annotations.PixelType
import com.tencent.iot.video.device.annotations.PixelType.IV_CM_PIXEL_240x320
import com.tencent.iot.video.device.annotations.PixelType.IV_CM_PIXEL_320x240
import com.tencent.iot.video.device.annotations.PixelType.IV_CM_PIXEL_480x352
import com.tencent.iot.video.device.annotations.StreamType
import com.tencent.iot.video.device.callback.IvVoipCallback
import com.tencent.iot.video.device.model.AvDataInfo
import com.tencent.iotvideo.link.CameraRecorder
import com.tencent.iotvideo.link.SimplePlayer
import com.tencent.iotvideo.link.entity.UserEntity
import com.tencent.iotvideo.link.util.DeviceSetting
import com.tencent.iotvideo.link.util.QualitySetting
import com.tencent.iotvideo.link.util.adjustAspectRatio
import com.tencent.iotvideo.link.util.dip2px
import com.tencent.iotvideo.link.util.getScreenHeight
import com.tencent.iotvideo.link.util.getScreenWidth
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private val TAG: String = TweCallActivity::class.java.simpleName
private const val DATA_PATH = "/storage/emulated/0/"

class TweCallActivity : BaseIPCActivity<ActivityTweCallBinding>(), IvVoipCallback {

    private var initStatus = -1 // 未初始化 -1， 初始化成功 0， 其他
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

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

    // wx twe call init
    private var modelId: String? = null
    private var deviceId: String? = null
    private var wxaAppId: String? = null

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
                cameraRecorder.openCamera(binding.textureViewTweCall, this@TweCallActivity)
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
                textureViewTweCall.isVisible = true
                if (!checkCallCondition()) return@setOnClickListener
                dialog =
                    ProgressDialog.show(this@TweCallActivity, "", "呼叫中doWxCloudTweCall", true)
                executeCallV2(true)
            }
            btnTweCallAudioCall.setOnClickListener {
                textureViewTweCall.isVisible = false
                if (!checkCallCondition()) return@setOnClickListener
                dialog =
                    ProgressDialog.show(
                        this@TweCallActivity,
                        "",
                        "呼叫中doWxCloudTweCallAudioCall",
                        true
                    )
                executeCallV2(false)
            }
            llTweCallHangUp.setOnClickListener {
                if (initStatus == -1) {
                    showToast("initWxCloudTweCall还未完成初始化")
                    return@setOnClickListener
                }
                if (initStatus != 0) {
                    showToast("initWxCloudTweCall初始化失败：$initStatus")
                    return@setOnClickListener
                }
                dialog =
                    ProgressDialog.show(
                        this@TweCallActivity,
                        "",
                        "挂断doWxCloudTweCallHangUp",
                        true
                    )
                hangUpV2()
            }
        }
    }

    private fun initTweCall() {
        if (!executor.isShutdown) {
            executor.submit {
                initStatus = initWxCloudTweCallV2()
                if (initStatus == 19) {
                    //把device_key文件删掉
                    deleteDeviceKeyFile(DATA_PATH)
                    initStatus = initWxCloudTweCallV2()
                }
                if (initStatus != 0) {
                    dismissDialog()
                    showToast("初始化失败，resCode:$initStatus")
                } else {
                    showToast("twecall初始化成功")
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
        if (!executor.isShutdown) {
            executor.submit {
                // call
                @PixelType val recvPixel =
                    QualitySetting.getInstance(this@TweCallActivity).wxResolution
                val calleeCameraSwitch =
                    if (isVideo) QualitySetting.getInstance(this@TweCallActivity).isWxCameraOn else true
                val callType =
                    if (isVideo) CallType.IV_CM_STREAM_TYPE_VIDEO else CallType.IV_CM_STREAM_TYPE_AUDIO
                val res = VideoNativeInterface.getInstance().doWxCloudVoipCall(
                    modelId, wxaAppId, openId, deviceId,
                    callType, recvPixel, true, calleeCameraSwitch
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
    }

    /**
     * 呼叫
     * @param isVideo
     */
    private fun executeCallV2(isVideo: Boolean) {
        if (!executor.isShutdown) {
            executor.submit {
                // call
                @PixelType val recvPixel =
                    if (isVideo) QualitySetting.getInstance(this@TweCallActivity).wxResolution else PixelType.IV_CM_PIXEL_VARIABLE
                val calleeCameraSwitch =
                    if (isVideo) QualitySetting.getInstance(this@TweCallActivity).isWxCameraOn else true
                val callType =
                    if (isVideo) CallType.IV_CM_STREAM_TYPE_VIDEO else CallType.IV_CM_STREAM_TYPE_AUDIO
                val res = VideoNativeInterface.getInstance()
                    .doWxCloudVoipCallV2(openId, callType, recvPixel, true, calleeCameraSwitch)
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
    }

    /**
     * 挂断，暂停维护，请使用v2接口
     */
    private fun hangUp() {
        if (!executor.isShutdown) {
            executor.submit {
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
    }

    /**
     * 挂断
     */
    private fun hangUpV2() {
        if (!executor.isShutdown) {
            executor.submit {
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
    }

    private fun dismissDialog(block: (() -> Unit)? = null) {
        lifecycleScope.launch {
            dialog?.dismiss()
            block?.invoke()
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "destory")
        defaultScope.launch {
            //        VideoNativeInterface.getInstance().exitWxCloudVoip()
            val exitWxCloudVoipV2 = VideoNativeInterface.getInstance().exitWxCloudVoipV2()
            Log.d(TAG, "exit twecall v2 resCode:$exitWxCloudVoipV2")
        }
        super.onDestroy()
        executor.shutdown()
    }

    private fun checkConditions() {
        if (condition1 && condition2 && remotePreviewSurface != null) {
            player.startVideoPlay(Surface(remotePreviewSurface), visitor, type, height, width)
        }
    }

    override fun onOnline(netDateTime: Long) {
        super.onOnline(netDateTime)
        initTweCall()
    }

    override fun onGetAvEncInfo(visitor: Int, channel: Int, videoResType: Int): AvDataInfo {
        return AvDataInfo.createDefaultAvDataInfo(videoResType)
    }

    override fun onStartRealPlay(visitor: Int, channel: Int, videoResType: Int) {
        super.onStartRealPlay(visitor, channel, videoResType)
        cameraRecorder.startRecording(visitor, videoResType)
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
            val pixel = getPixel()
            adjustAspectRatio(
                pixel[0],
                pixel[1],
                binding.surfaceViewTweCall,
                getScreenWidth(resources) - dip2px(this@TweCallActivity, 10f),
                getScreenHeight(resources) - dip2px(this@TweCallActivity, 115f)
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
        Log.d(
            TAG,
            "onRecvStream visitor:$visitor  streamType:$streamType  data:$data  len:$len  pts:$pts  seq:$seq"
        )
        if (streamType == 1) {
            return player.playVideoStream(visitor, data, len, pts, seq)
        } else if (streamType == 0) {
            return player.playAudioStream(visitor, data, len, pts, seq)
        }
        return 0
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
                llTweCallHangUp.bringToFront()
            }
            surfaceViewTweCallBg.isVisible = isCalling
            surfaceViewTweCall.isVisible = isCalling
            textureViewTweCall.isVisible = isCalling
            llTweCallHangUp.isVisible = isCalling
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
            llTweCallHangUp.isVisible = isCalling
            llButtons.isVisible = !isCalling
            rvUserList.isVisible = !isCalling
            llOpenid.isVisible = !isCalling
            tvUserList.isVisible = !isCalling
        }
    }

    //获取激活设备信息
    override fun onUpdateAuthorizeStatus(openId: String?, status: Int): Int {
        Log.d(TAG, "onUpdateAuthorizeStatus   penId:${openId}  status:$status")
        return 0
    }
}