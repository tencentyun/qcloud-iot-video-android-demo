package com.example.ivdemo

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.SurfaceTexture
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.TextureView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.ivdemo.popup.CustomCommandDialog
import com.example.ivdemo.popup.QualitySettingDialog
import com.tencent.iot.twcall.R
import com.tencent.iot.twcall.databinding.ActivityIpcBinding
import com.tencent.iot.video.device.VideoNativeInterface
import com.tencent.iot.video.device.annotations.CmFrameType
import com.tencent.iot.video.device.annotations.CsChannelType
import com.tencent.iot.video.device.annotations.StreamType
import com.tencent.iot.video.device.callback.IvCsInitCallback
import com.tencent.iot.video.device.model.AvDataInfo
import com.tencent.iot.video.device.model.CsBalanceInfo
import com.tencent.iot.video.device.model.CsChannelInfo
import com.tencent.iot.video.device.model.CsEventResultInfo
import com.tencent.iot.video.device.model.CsNotifyMsgData
import com.tencent.iotvideo.link.CameraRecorder
import com.tencent.iotvideo.link.SimplePlayer
import com.tencent.iotvideo.link.listener.OnEncodeListener
import com.tencent.iotvideo.link.util.copyTextToClipboard
import com.tencent.iotvideo.link.util.updateOperate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

private const val TAG = "IPCActivity"

class IPCActivity : BaseIPCActivity<ActivityIpcBinding>(), IvCsInitCallback, OnEncodeListener {

    private val player = SimplePlayer()
    private val cameraRecorder = CameraRecorder()
    private var lastClickTime = 0L
    private var localPreviewSurface: SurfaceTexture? = null
    private val handler = Handler(Looper.getMainLooper())
    private val UPDATE_P2P_INFO_TOKEN = "update_p2p_info_token"
    private val CHECK_PREPARE_CS_TOKEN = "check_prepare_cs_token"
    private var customCommandDialog: CustomCommandDialog? = null
    private var avDataInfo: AvDataInfo? = null
    private var shouldCsInit: Boolean = true
    private var csBalanceInfo: CsBalanceInfo? = null
    private var wareReportSate = false

    private val listener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            if (surface == binding.textureViewIpc.surfaceTexture) {
                // Initialize the SurfaceTexture object
                localPreviewSurface = surface

                // Start the camera encoder
                cameraRecorder.openCamera(binding.textureViewIpc, this@IPCActivity)
                Log.d("IPCActivity", "onSurfaceTextureAvailable")
            }
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            // Not used in this example
            Log.d("IPCActivity", "onSurfaceTextureSizeChanged")
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            Log.d("IPCActivity", "onSurfaceTextureDestroyed")
            if (surface == binding.textureViewIpc.surfaceTexture) {
                // Stop the camera encoder
                cameraRecorder.closeCamera()
            }
            return true
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            // Not used in this example
        }
    }

    override fun getViewBinding(): ActivityIpcBinding = ActivityIpcBinding.inflate(layoutInflater)
    override fun initView() {
        prepareCs()
        cameraRecorder.setOnEncodeListener(this)
        with(binding) {
            titleLayout.tvTitle.text = getString(R.string.title_ipc)
            titleLayout.ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
            binding.titleLayout.ivRightBtn.isVisible = true
            binding.titleLayout.ivRightBtn.setOnClickListener {
                val dialog = QualitySettingDialog(this@IPCActivity)
                dialog.setOnDismissListener {
                    cameraRecorder.closeCamera()
                    cameraRecorder.openCamera(binding.textureViewIpc, this@IPCActivity)
                }
                dialog.show(supportFragmentManager)
            }
            textDevInfo.text =
                String.format((getString(R.string.text_device_info)), "${productId}_$deviceName")
            textureViewIpc.surfaceTextureListener = listener
            tvCopy.setOnClickListener {
                copyTextToClipboard(this@IPCActivity, tvP2pInfo.text.toString().substringAfter(":"))
                showToast("已复制p2p信息")
            }
            btnCloudStorageReport.setOnClickListener {
                if (!checkCsInfo()) return@setOnClickListener
                VideoNativeInterface.getInstance()
                    .stopCsEvent(CsChannelType.CS_SINGLE_CH, 1, "report test cs info")
                val csEventRes = VideoNativeInterface.getInstance()
                    .startCsEvent(CsChannelType.CS_SINGLE_CH, 1, "report test cs info")
                if (csEventRes != 0) {
                    showToast("触发事件失败,res:$csEventRes")
                } else {
                    showToast("触发事件成功")
                }
            }
            btnCloudPicWareReport.setOnClickListener {
                if (!checkCsInfo()) return@setOnClickListener
                val bitmap = binding.textureViewIpc.getBitmap(
                    cameraRecorder.mVideoWidth,
                    cameraRecorder.mVideoHeight
                )
                if (bitmap != null) {
                    val reportRes = VideoNativeInterface.getInstance().reportCsEventDirectly(
                        CsChannelType.CS_SINGLE_CH,
                        1,
                        "report test cs info",
                        0,
                        bitmapToByteArray(bitmap)
                    )
                    if (reportRes == 0) {
                        showToast("告警事件上报成功")
                    } else {
                        showToast("告警事件上报失败，res:$reportRes")
                    }
                }
            }
            btnCloudVideoWareReport.setOnClickListener {
                if (!checkCsInfo()) return@setOnClickListener
                if (!wareReportSate) {
                    VideoNativeInterface.getInstance().startCsEvent(
                        CsChannelType.CS_SINGLE_CH, 2, "start report test cs video info"
                    )
                } else {
                    VideoNativeInterface.getInstance().stopCsEvent(
                        CsChannelType.CS_SINGLE_CH, 2, "stop report test cs video info"
                    )
                }
            }
            btnCustomCommand.setOnClickListener {
                if (!isOnline) {
                    showToast("设备未上线")
                    return@setOnClickListener
                }
                customCommandDialog = CustomCommandDialog(this@IPCActivity, visitor)
                customCommandDialog?.show(supportFragmentManager)
            }
//            btnIpcCall.setOnClickListener {
//                val time = System.currentTimeMillis()
//                val timeD = time - lastClickTime
//                //防止频繁点击
//                if (timeD in 1..999) {
//                    Toast.makeText(this@IPCActivity, "频繁点击！", Toast.LENGTH_SHORT).show()
//                    return@setOnClickListener
//                }
//                lastClickTime = time
//                // msg_id 6: 按门铃
//                VideoNativeInterface.getInstance().sendMsgNotice(6)
//            }
        }
    }

    private val taskInitCsRunnable = Runnable {
        prepareCs()
    }

    private fun prepareCs() {
        if (isOnline) {
            initCs()
        } else {
            if (Build.VERSION.SDK_INT >= 28) {
                handler.postDelayed(taskInitCsRunnable, CHECK_PREPARE_CS_TOKEN, 1000)
            } else {
                handler.postDelayed(taskInitCsRunnable, 1000)
            }
        }
    }

    private fun checkCsInfo(): Boolean {
        if (!isOnline) {
            showToast("设备未上线")
            return false
        }
        if (csBalanceInfo == null) {
            showToast("设备未获取到云存套餐")
            return false
        } else if (csBalanceInfo?.csSwitch == 0) {
            showToast("设备未开通云存套餐")
            return false
        }
        if (shouldCsInit) {
            showToast("未初始化云存")
            return false
        }
        return true
    }

    private fun initCs(): Int {
        if (shouldCsInit) {
            val csChannelInfo = arrayOfNulls<CsChannelInfo>(1)
            for (i in csChannelInfo.indices) {
                val channelInfo = CsChannelInfo()
                channelInfo.channelId = i
                channelInfo.u32MaxGopSize = 512 * 1024
                channelInfo.csFormat = 0
                if (avDataInfo != null) {
                    channelInfo.avDataInfo = avDataInfo
                } else {
                    channelInfo.avDataInfo = AvDataInfo.createDefaultAvDataInfo(videoResType)
                }
                channelInfo.eventReportOpt = 0
                csChannelInfo[i] = channelInfo
            }
            shouldCsInit = false
            return VideoNativeInterface.getInstance().initCs(csChannelInfo, this)
        }
        return -1
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

    private var shouldGetCs = true
    private fun updateCsState() {
        if (shouldGetCs) {
            csBalanceInfo =
                VideoNativeInterface.getInstance().getCsBalanceInfo(CsChannelType.CS_SINGLE_CH, 0)
            if (csBalanceInfo != null) {
                shouldGetCs = false
                binding.tvCloudStorageState.text = String.format(
                    getString(R.string.text_cloud_storage_state),
                    if (csBalanceInfo?.csSwitch == 1) {
                        binding.btnCloudPicWareReport.updateOperate(true)
                        binding.btnCloudVideoWareReport.updateOperate(true)
                        binding.btnCloudStorageReport.updateOperate(true)
                        "已开通"
                    } else "未开通"
                )
            }
        }
    }

    private val taskRunnable = Runnable {
        updateP2pInfo()
        updateCsState()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (Build.VERSION.SDK_INT >= 28) {
            handler.removeCallbacksAndMessages(UPDATE_P2P_INFO_TOKEN)
        } else {
            handler.removeCallbacks(taskRunnable)
        }
        if (Build.VERSION.SDK_INT >= 28) {
            handler.removeCallbacksAndMessages(CHECK_PREPARE_CS_TOKEN)
        } else {
            handler.removeCallbacks(taskInitCsRunnable)
        }
        if (!shouldCsInit) {
            VideoNativeInterface.getInstance().exitCs()
        }
    }

    override fun onOnline(netDateTime: Long) {
        super.onOnline(netDateTime)
        lifecycleScope.launch(Dispatchers.Main) {
            updateP2pInfo()
            if (Build.VERSION.SDK_INT >= 28) {
                handler.postDelayed(taskRunnable, UPDATE_P2P_INFO_TOKEN, 5000)
            } else {
                handler.postDelayed(taskRunnable, 5000)
            }
            binding.btnCustomCommand.updateOperate(true)
        }
    }

    override fun onGetAvEncInfo(visitor: Int, channel: Int, videoResType: Int): AvDataInfo {
        avDataInfo = AvDataInfo.createDefaultAvDataInfo(videoResType)
        return avDataInfo!!
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

    override fun onRecvCommand(
        command: Int,
        visitor: Int,
        channel: Int,
        videoResType: Int,
        args: String?
    ): String {
        return customCommandDialog?.receiveCommand(args ?: "null").toString()
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }

        val width = drawable.intrinsicWidth
        val height = drawable.intrinsicHeight
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    // 将Bitmap转换为byte数组
    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    override fun onStartPushStream(channel: Int): Int {
        Log.d(TAG, "onStartPushStream  channel:$channel")
        wareReportSate = true
        lifecycleScope.launch(Dispatchers.Main) {
            binding.btnCloudVideoWareReport.text = "正在上报告警中..."
        }
        return 0
    }

    override fun onStopPushStream(channel: Int): Int {
        Log.d(TAG, "onStopPushStream  channel:$channel")
        wareReportSate = false
        lifecycleScope.launch(Dispatchers.Main) {
            binding.btnCloudVideoWareReport.text = getString(R.string.text_cloud_video_ware)
        }
        return 0
    }

    override fun onAiServiceNotify(channel: Int, aiServerType: Int, utcExpire: Long) {
        Log.d(TAG, "onAiServiceNotify  channel:$channel")
    }

    override fun onEventCapturePicture(channel: Int, eventId: Int): ByteArray {
        Log.d(TAG, "onEventCapturePicture  channel:$channel")
        val bitmap = binding.textureViewIpc.getBitmap(
            cameraRecorder.mVideoWidth,
            cameraRecorder.mVideoHeight
        )
        if (bitmap != null) {
            return bitmapToByteArray(bitmap)
        }
        return ByteArray(0)
    }

    override fun onEventPictureResult(channel: Int, errCode: Int): Int {
        Log.d(TAG, "onEventPictureResult  channel:$channel")
        return 0
    }

    override fun onEventReportResult(channel: Int, resultInfo: CsEventResultInfo?): Int {
        Log.d(TAG, "onEventReportResult  channel:$channel")
        return 0
    }

    override fun onNotify(channel: Int, notifyMsgType: Int, notifyData: CsNotifyMsgData?): Int {
        Log.d(TAG, "onNotify  channel:$channel")
        return 0
    }

    override fun onGetBalance(
        channel: Int,
        isValid: Boolean,
        balanceInfo: CsBalanceInfo?,
        timeoutMs: Int
    ) {
        Log.d(TAG, "onGetBalance  channel:$channel")
    }

    override fun onDumpFile(
        channel: Int,
        state: Int,
        startTs: Long,
        endTs: Long,
        fileName: String?,
        buf: ByteArray?,
        len: Int
    ): Int {
        Log.d(TAG, "onDumpFile  channel:$channel")
        return 0
    }

    override fun onAudioEncoded(datas: ByteArray?, pts: Long, seq: Long) {
        if (wareReportSate) {
            VideoNativeInterface.getInstance().pushCsAudioStream(
                CsChannelType.CS_SINGLE_CH, datas, pts, seq.toInt()
            )
        }
    }

    override fun onVideoEncoded(datas: ByteArray?, pts: Long, seq: Long, isKeyFrame: Boolean) {
        if (wareReportSate) {
            val type = if (isKeyFrame) {
                CmFrameType.IV_CM_FRAME_TYPE_I
            } else {
                CmFrameType.IV_CM_FRAME_TYPE_P
            }
            VideoNativeInterface.getInstance().pushCsVideoStream(
                CsChannelType.CS_SINGLE_CH, datas, pts, type, seq.toInt()
            )
        }
    }
}