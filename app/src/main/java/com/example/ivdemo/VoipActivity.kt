package com.example.ivdemo

import android.app.ProgressDialog
import android.content.Intent
import android.graphics.SurfaceTexture
import android.text.TextUtils
import android.util.Log
import android.view.Surface
import android.view.TextureView.SurfaceTextureListener
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.tencent.iot.twcall.R
import com.tencent.iot.twcall.databinding.ActivityVoipBinding
import com.tencent.iot.twcall.databinding.SettingLayoutBinding
import com.tencent.iot.video.device.VideoNativeInterface
import com.tencent.iot.video.device.annotations.StreamType
import com.tencent.iotvideo.link.CameraRecorder
import com.tencent.iotvideo.link.SimplePlayer
import com.tencent.iotvideo.link.adapter.UserListAdapter
import com.tencent.iotvideo.link.entity.UserEntity
import com.tencent.iotvideo.link.util.QualitySetting
import com.tencent.iotvideo.link.util.VoipSetting
import com.tencent.iotvideo.link.util.showPopupWindow
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private val TAG: String = VoipActivity::class.java.simpleName

class VoipActivity : BaseIPCActivity<ActivityVoipBinding>() {

    private var userListAdapter: UserListAdapter? = null
    private var usersData: ArrayList<UserEntity>? = null

    private var initStatus = -1 // 未初始化 -1， 初始化成功 0， 其他
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private var selectedPosition = RecyclerView.NO_POSITION

    private var condition1 = false
    private var condition2 = false
    private val lock = Any()

    private var visitor = 0
    private var type = 0
    private var height = 0
    private var width = 0

    private val player = SimplePlayer()

    private val cameraRecorder = CameraRecorder()

    private var localPreviewSurface: SurfaceTexture? = null

    private var remotePreviewSurface: SurfaceTexture? = null
    private var dialog: ProgressDialog? = null

    // wx voip init
    private val modelId by lazy { intent.getStringExtra("voip_model_id") }
    private val voipDeviceId by lazy { intent.getStringExtra("voip_device_id") }
    private val wxaAppId by lazy { intent.getStringExtra("voip_wxa_appid") }
    private var openId: String = ""
    private val sNTicket by lazy { intent.getStringExtra("voip_sn_ticket") }
    private val miniProgramVersion by lazy {
        intent.getIntExtra("miniprogramVersion", 0)
    } //0 "正式版", 1  "开发版", 2 "体验版"
    private val voipSetting by lazy { VoipSetting.getInstance(this) }

    private val surfaceTextureListener = object : SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            if (surface == binding.textureViewVoip.surfaceTexture) {
                // Initialize the SurfaceTexture object
                localPreviewSurface = surface

                // Start the camera encoder
                cameraRecorder.openCamera(localPreviewSurface, this@VoipActivity)
            } else if (surface == binding.surfaceViewVoip.surfaceTexture) {
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
            if (surface == binding.textureViewVoip.surfaceTexture) {
                // Stop the camera encoder
                cameraRecorder.closeCamera()
            }
            return true
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            // Not used in this example
        }

    }

    private fun getUsersData(): ArrayList<UserEntity> {
        if (usersData == null) {
            usersData = arrayListOf()
            val user1 = UserEntity()
            user1.openId = voipSetting.openId1
            usersData?.add(user1)
            val user2 = UserEntity()
            user2.openId = voipSetting.openId2
            usersData?.add(user2)
            val user3 = UserEntity()
            user3.openId = voipSetting.openId3
            usersData?.add(user3)
        }
        return usersData!!
    }


    override fun getViewBinding(): ActivityVoipBinding = ActivityVoipBinding.inflate(layoutInflater)

    override fun initView() {
        openId = intent.getStringExtra("voip_open_id") ?: ""
        with(binding) {
            titleLayout.tvTitle.text = getString(R.string.title_voip)
            titleLayout.ivRightBtn.isVisible = true
            titleLayout.ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
            titleLayout.ivRightBtn.setOnClickListener {
                val settingBinding = SettingLayoutBinding.inflate(layoutInflater)
                val dialog = showPopupWindow(it, settingBinding.root)
                settingBinding.tvWxSetting.setOnClickListener {
                    jumpSetting("wx_setting")
                    dialog.dismiss()
                }
                settingBinding.tvQualitySetting.setOnClickListener {
                    jumpSetting("quality_setting")
                    dialog.dismiss()
                }
            }
            // 如果可以确定每个item的高度是固定的，设置这个选项可以提高性能
            rvUserList.setHasFixedSize(true)
            // 设置适配器，刷新展示用户列表
            userListAdapter = UserListAdapter(this@VoipActivity, getUsersData())
            userListAdapter?.setOnSelectedListener { position: Int -> selectedPosition = position }
            rvUserList.setAdapter(userListAdapter)
            textureViewVoip.requestFocus()
            textureViewVoip.surfaceTextureListener = surfaceTextureListener
            surfaceViewVoip.surfaceTextureListener = surfaceTextureListener
            dialog =
                ProgressDialog.show(this@VoipActivity, "", "正在加载初始化initWxCloudVoip", true)
            btnVoipVideoCall.setOnClickListener {
                textureViewVoip.isVisible = true
                if (!checkCallCondition()) return@setOnClickListener
                dialog = ProgressDialog.show(this@VoipActivity, "", "呼叫中doWxCloudVoipCall", true)
                callVoip(true)
            }
            btnVoipAudioCall.setOnClickListener {
                textureViewVoip.isVisible = false
                if (!checkCallCondition()) return@setOnClickListener
                dialog =
                    ProgressDialog.show(this@VoipActivity, "", "呼叫中doWxCloudVoipAudioCall", true)
                callVoip(false)
            }
            btnVoipHangUp.setOnClickListener {
                if (initStatus == -1) {
                    showToast("initWxCloudVoip还未完成初始化")
                    return@setOnClickListener
                }
                if (initStatus != 0) {
                    showToast("initWxCloudVoip初始化失败：$initStatus")
                    return@setOnClickListener
                }
                dialog = ProgressDialog.show(this@VoipActivity, "", "挂断doWxCloudVoipHangUp", true)
                hangUp()
            }
            initVoip()
        }
    }

    private fun initVoip() {
        if (!executor.isShutdown) {
            executor.submit {
                initStatus = initWxCloudVoip()
                if (initStatus == 19) {
                    //把device_key文件删掉
                    deleteDeviceKeyFile()
                    initStatus = initWxCloudVoip()
                }
            }
        }
    }

    /**
     * 初始化 voip
     *
     * @return 初始化状态值
     */
    private fun initWxCloudVoip(): Int {
        val initStatus = VideoNativeInterface.getInstance()
            .initWxCloudVoip(modelId, voipDeviceId, wxaAppId, miniProgramVersion)
        Log.i(TAG, "reInitWxCloudVoip initStatus: $initStatus")
        if (initStatus == 0) {
            val registeredState = VideoNativeInterface.getInstance().isAvtVoipRegistered()
            Log.i(TAG, "isAvtVoipRegistered: $registeredState")
            if (registeredState == 0) {
                val registerRes = VideoNativeInterface.getInstance().registerAvtVoip(sNTicket)
                Log.i(TAG, "registerAvtVoip registerRes: $registerRes")
            }
            dismissDialog()
        }
        return initStatus
    }

    private fun checkCallCondition(): Boolean {
        if (selectedPosition == RecyclerView.NO_POSITION) {
            showToast("请勾选被呼叫的用户！")
            return false
        }
        setOpenId()
        if (TextUtils.isEmpty(openId)) {
            showToast("请输入被呼叫的用户openid！")
            return false
        }
        if (initStatus == -1) {
            showToast("initWxCloudVoip还未完成初始化")
            return false
        } else if (initStatus != 0) {
            showToast("initWxCloudVoip初始化失败：$initStatus")
            return false
        }
        return true
    }

    /**
     * 呼叫
     * @param isVideo
     */
    private fun callVoip(isVideo: Boolean) {
        if (!executor.isShutdown) {
            executor.submit {
                // voip call
                val recvPixel = QualitySetting.getInstance(this@VoipActivity).wxResolution
                val calleeCameraSwitch = QualitySetting.getInstance(this@VoipActivity).isWxCameraOn
                val ret = if (isVideo) {
                    VideoNativeInterface.getInstance().doWxCloudVoipCall(
                        modelId, wxaAppId, openId, voipDeviceId, recvPixel, calleeCameraSwitch
                    )
                } else {
                    VideoNativeInterface.getInstance().doWxCloudVoipAudioCall(
                        modelId, wxaAppId, openId, voipDeviceId
                    )
                }

                val result = when (ret) {
                    -2 -> "通话中"
                    0 -> "呼叫成功"
                    else -> "呼叫失败"
                }
                Log.i(TAG, "VOIP call result: $result, ret: $ret")
                dismissDialog {
                    showToast(result)
                    if (isVideo) updateVideoUI(true) else updateAudioUI(true)
                }
            }
        }
    }

    /**
     * 挂断
     */
    private fun hangUp() {
        if (!executor.isShutdown) {
            executor.submit {
                val ret = VideoNativeInterface.getInstance().doWxCloudVoipHangUp(
                    productId, deviceName, openId, voipDeviceId
                )
                val result = if (ret == 0) "已挂断" else "挂断失败"
                Log.i(TAG, "VOIP call result: $result")
                dismissDialog {
                    showToast(result)
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
        super.onDestroy()
        executor.shutdown()
    }

    private fun setOpenId() {
        when (selectedPosition) {
            0 -> openId = voipSetting.openId1
            1 -> openId = voipSetting.openId2
            2 -> openId = voipSetting.openId3
        }
    }

    private fun checkConditions() {
        if (condition1 && condition2 && remotePreviewSurface != null) {
            player.startVideoPlay(Surface(remotePreviewSurface), visitor, type, height, width)
        }
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
        binding.tvTips.text = "通话中"
        return super.onStartRecvAudioStream(
            visitor, channel, type, option, mode, width, sample_rate, sample_num
        )
    }

    override fun onStartRecvVideoStream(
        visitor: Int, channel: Int, type: Int, height: Int, width: Int, frameRate: Int
    ): Int {
        Log.d(TAG, "start video visitor $visitor h: $height w: $width")
        lifecycleScope.launch { updateVideoUI(true) }
        this.visitor = visitor
        this.type = type
        this.height = height
        this.width = width
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

    override fun onStopRecvStream(visitor: Int, channel: Int, streamType: Int): Int {
        super.onStopRecvStream(visitor, channel, streamType)
        if (streamType == StreamType.IV_AVT_STREAM_TYPE_VIDEO || streamType == StreamType.IV_AVT_STREAM_TYPE_AV) {
            lifecycleScope.launch { updateVideoUI(false) }
        }
        return 0
    }

    override fun onStopRealPlay(visitor: Int, channel: Int, videoResType: Int) {
        super.onStopRealPlay(visitor, channel, videoResType)
        lifecycleScope.launch { updateVideoUI(false) }
    }

    private fun deleteDeviceKeyFile() {
        // 假设SD卡的路径是 /sdcard
        val sdCardPath = "/sdcard"
        val fileName = "device_key"

        // 创建一个File对象，表示device_key文件
        val deviceKeyFile = File(sdCardPath, fileName)

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
                surfaceViewVoip.bringToFront()
                textureViewVoip.bringToFront()
            }
            surfaceViewVoipBg.isVisible = isCalling
            surfaceViewVoip.isVisible = isCalling
            textureViewVoip.isVisible = isCalling
            btnVoipHangUp.isVisible = isCalling
            llButtons.isVisible = !isCalling
            rvUserList.isVisible = !isCalling
        }
    }

    private fun updateAudioUI(isCalling: Boolean) {
        with(binding) {
            tvTips.isVisible = isCalling
            ivAudio.isVisible = isCalling
            btnVoipHangUp.isVisible = isCalling
            llButtons.isVisible = !isCalling
            rvUserList.isVisible = !isCalling
        }
    }

    private fun showToast(text: String) {
        Toast.makeText(this@VoipActivity, text, Toast.LENGTH_SHORT).show()
    }

    private fun jumpSetting(type: String) {
        val intent = Intent(this, SettingActivity::class.java)
        intent.putExtra("type", type)
        startActivity(intent)
    }
}