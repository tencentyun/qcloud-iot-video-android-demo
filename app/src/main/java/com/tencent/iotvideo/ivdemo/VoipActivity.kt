package com.tencent.iotvideo.ivdemo

import android.app.ProgressDialog
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.os.Environment
import android.text.TextUtils
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tencent.iot.voip.device.VoipNativeInterface
import com.tencent.iot.voipdemo.databinding.ActivityVoipBinding
import com.tencent.iotvideo.link.adapter.UserListAdapter
import com.tencent.iotvideo.link.entity.UserEntity
import com.tencent.iotvideo.link.util.QualitySetting
import com.tencent.iotvideo.link.util.VoipSetting
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class VoipActivity : IPCActivity(), TextureView.SurfaceTextureListener ***REMOVED***
    private val binding by lazy ***REMOVED*** ActivityVoipBinding.inflate(layoutInflater) }
    private var mModelId: String? = null
    private var mVoipDeviceId: String? = null
    private var mWxaAppId: String? = null
    private var mOpenId: String? = null
    private var mSNTicket: String? = null
    private var mUsersData: ArrayList<UserEntity>? = null
    
    private var mInitStatus = -1 // 未初始化 -1， 初始化成功 0， 其他
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private var mDialog: ProgressDialog? = null
    private var mMiniprogramVersion = 0 //0 "正式版", 1  "开发版", 2 "体验版"
    private var selectedPosition: Int = RecyclerView.NO_POSITION

    private var condition1 = false
    private var condition2 = false
    private val lock = Any()

    private var visitor = 0
    private var type = 0
    private var height = 0
    private var width = 0

    private fun getUsersData(): ArrayList<UserEntity>? ***REMOVED***
        if (mUsersData == null) ***REMOVED***
            mUsersData = ArrayList()
            val user1 = UserEntity()
            user1.openId = VoipSetting.getInstance(this).openId1
            mUsersData!!.add(user1)
            val user2 = UserEntity()
            user2.openId = VoipSetting.getInstance(this).openId2
            mUsersData!!.add(user2)
            val user3 = UserEntity()
            user3.openId = VoipSetting.getInstance(this).openId3
            mUsersData!!.add(user3)
      ***REMOVED***
        return mUsersData
  ***REMOVED***

    override fun initWidget() ***REMOVED***
        setContentView(binding.root)
        with(binding)***REMOVED***
            // 设置管理器
            val layoutManager = LinearLayoutManager(this@VoipActivity)
            rvUserList.setLayoutManager(layoutManager)
            // 如果可以确定每个item的高度是固定的，设置这个选项可以提高性能
            rvUserList.setHasFixedSize(true)
            // 设置适配器，刷新展示用户列表
            val userListAdapter = UserListAdapter(this@VoipActivity, getUsersData())
            userListAdapter.setOnSelectedListener ***REMOVED*** position -> selectedPosition = position }
            rvUserList.adapter = userListAdapter
            tvTips.isVisible = false
            ivAudio.isVisible = false
            textVoipIvDevinfo.isVisible = false
            textureViewVoip.surfaceTextureListener = this@VoipActivity
            textureViewVoip.isVisible = false
            surfaceViewVoip.surfaceTextureListener = this@VoipActivity
            surfaceViewVoip.isVisible = false
            surfaceViewVoipBg.isVisible= false
      ***REMOVED***

        // wx voip init
        mModelId = intent.getStringExtra("voip_model_id")
        mVoipDeviceId = intent.getStringExtra("voip_device_id")
        mWxaAppId = intent.getStringExtra("voip_wxa_appid")
        mOpenId = intent.getStringExtra("voip_open_id")
        mSNTicket = intent.getStringExtra("voip_sn_ticket")
        mMiniprogramVersion = intent.getIntExtra("miniprogramVersion", 0)

        try ***REMOVED***
            val dir = File(Environment.getExternalStorageDirectory(), "ivdemo")
            if (!dir.exists()) ***REMOVED***
                dir.mkdir()
          ***REMOVED***
      ***REMOVED*** catch (e: Exception) ***REMOVED***
            e.printStackTrace()
            Toast.makeText(this@VoipActivity, "创建文件夹失败！", Toast.LENGTH_LONG).show()
            return
      ***REMOVED***
        val baseDir: String = Environment.getExternalStorageDirectory().absolutePath
        val path = "$baseDir/ivdemo/"
        // 证书文件需要保存在相同的路径
        val fileOps: AssetFileOps = AssetFileOps()
        val certFileName = "cacert.pem"
        val absCertFileName = "$path/$certFileName"
        fileOps.copyFileFromAssets(applicationContext, certFileName, absCertFileName)

        mDialog = ProgressDialog.show(this@VoipActivity, "", "正在加载初始化initWxCloudVoip", true)

        if (!executor.isShutdown) ***REMOVED***
            executor.submit ***REMOVED***

//                int ret = VoipNativeInterface.getInstance().initWxCloudVoip(path, "mHostAppId", mModelId,
//                        "mVoipProductId", mVoipDeviceId, "mVoipDeviceSign", mWxaAppId, mSNTicket, mMiniprogramVersion);
                val ret = VoipNativeInterface.getInstance().initWxCloudVoip(mModelId, mVoipDeviceId, mWxaAppId, mSNTicket, mMiniprogramVersion)
                Log.i(TAG, "initWxCloudVoip ret: $ret")
                mInitStatus = ret
                if (ret == 19) ***REMOVED***
                    //把device_key文件删掉
                    deleteDeviceKeyFile()
                    mInitStatus = VoipNativeInterface.getInstance().initWxCloudVoip(mModelId, mVoipDeviceId, mWxaAppId, mSNTicket, mMiniprogramVersion)
                    Log.i(TAG, "reInitWxCloudVoip ret: $ret")
              ***REMOVED***
                runOnUiThread ***REMOVED***
                    mDialog?.dismiss()
              ***REMOVED***
          ***REMOVED***
      ***REMOVED***

        binding.btnVoipVideoCall.setOnClickListener(View.OnClickListener ***REMOVED***
            binding.textureViewVoip.visibility = View.VISIBLE
            if (selectedPosition == RecyclerView.NO_POSITION) ***REMOVED***
                Toast.makeText(this@VoipActivity, "请勾选被呼叫的用户！", Toast.LENGTH_SHORT).show()
                return@OnClickListener
          ***REMOVED***
            setOpenId()
            if (TextUtils.isEmpty(mOpenId)) ***REMOVED***
                Toast.makeText(this@VoipActivity, "请输入被呼叫的用户openid！", Toast.LENGTH_SHORT).show()
                return@OnClickListener
          ***REMOVED***
            if (mInitStatus == -1) ***REMOVED***
                Toast.makeText(this@VoipActivity, "initWxCloudVoip还未完成初始化", Toast.LENGTH_SHORT).show()
                return@OnClickListener
          ***REMOVED***
            if (mInitStatus != 0) ***REMOVED***
                Toast.makeText(this@VoipActivity, "initWxCloudVoip初始化失败：$mInitStatus", Toast.LENGTH_SHORT).show()
                return@OnClickListener
          ***REMOVED***

            mDialog = ProgressDialog.show(this@VoipActivity, "", "呼叫中doWxCloudVoipCall", true)
            if (!executor.isShutdown) ***REMOVED***
                executor.submit ***REMOVED***
                    // voip call
                    var result = ""
                    val recvPixel: Int =
                        QualitySetting.getInstance(this@VoipActivity).getWxResolution()
                    val calleeCameraSwitch: Boolean =
                        QualitySetting.getInstance(this@VoipActivity).isWxCameraOn()
                    val ret: Int = VoipNativeInterface.getInstance().doWxCloudVoipCall(
                        mModelId, mWxaAppId, mOpenId, mVoipDeviceId, recvPixel, calleeCameraSwitch
                    )
                    result = if (ret == -2) ***REMOVED***
                        "通话中"
                  ***REMOVED*** else if (ret != 0) ***REMOVED***
                        "呼叫失败"
                  ***REMOVED*** else ***REMOVED***
                        "呼叫成功"
                  ***REMOVED***
                    Log.i(TAG, "VOIP call result: $result, ret: $ret")
                    val finalResult = result
                    runOnUiThread ***REMOVED***
                        mDialog?.dismiss()
                        Toast.makeText(this@VoipActivity, finalResult, Toast.LENGTH_SHORT).show()
                        binding.btnVoipHangUp.visibility = View.VISIBLE
                        binding.llButtons.visibility = View.INVISIBLE
                        binding.rvUserList.visibility = View.INVISIBLE
                        binding.surfaceViewVoipBg.visibility = View.VISIBLE
                        binding.surfaceViewVoipBg.bringToFront()
                        binding.textureViewVoip.bringToFront()
                  ***REMOVED***
              ***REMOVED***
          ***REMOVED***
      ***REMOVED***)

        binding.btnVoipAudioCall.setOnClickListener(View.OnClickListener ***REMOVED***
            binding.textureViewVoip.visibility = View.INVISIBLE
            if (selectedPosition == RecyclerView.NO_POSITION) ***REMOVED***
                Toast.makeText(this@VoipActivity, "请勾选被呼叫的用户！", Toast.LENGTH_SHORT).show()
                return@OnClickListener
          ***REMOVED***
            setOpenId()
            if (TextUtils.isEmpty(mOpenId)) ***REMOVED***
                Toast.makeText(this@VoipActivity, "请输入被呼叫的用户openid！", Toast.LENGTH_SHORT)
                    .show()
                return@OnClickListener
          ***REMOVED***
            if (mInitStatus == -1) ***REMOVED***
                Toast.makeText(
                    this@VoipActivity,
                    "initWxCloudVoip还未完成初始化",
                    Toast.LENGTH_SHORT
                ).show()
                return@OnClickListener
          ***REMOVED***
            if (mInitStatus != 0) ***REMOVED***
                Toast.makeText(
                    this@VoipActivity,
                    "initWxCloudVoip初始化失败：$mInitStatus",
                    Toast.LENGTH_SHORT
                ).show()
                return@OnClickListener
          ***REMOVED***

            mDialog =
                ProgressDialog.show(this@VoipActivity, "", "呼叫中doWxCloudVoipAudioCall", true)
            if (!executor.isShutdown) ***REMOVED***
                executor.submit ***REMOVED***
                    // voip call
                    var result = ""
                    val ret: Int = VoipNativeInterface.getInstance().doWxCloudVoipAudioCall(
                        mModelId, mWxaAppId, mOpenId, mVoipDeviceId
                    )
                    result = if (ret == -2) ***REMOVED***
                        "通话中"
                  ***REMOVED*** else if (ret != 0) ***REMOVED***
                        "呼叫失败"
                  ***REMOVED*** else ***REMOVED***
                        "呼叫成功"
                  ***REMOVED***
                    Log.i(TAG, "VOIP call result: $result, ret: $ret")
                    val finalResult = result
                    runOnUiThread ***REMOVED***
                        mDialog?.dismiss()
                        Toast.makeText(this@VoipActivity, finalResult, Toast.LENGTH_SHORT).show()
                        binding.tvTips.visibility = View.VISIBLE
                        binding.tvTips.text = finalResult
                        binding.ivAudio.visibility = View.VISIBLE
                        binding.btnVoipHangUp.visibility = View.VISIBLE
                        binding.llButtons.visibility = View.INVISIBLE
                        binding.rvUserList.visibility = View.INVISIBLE
                  ***REMOVED***
              ***REMOVED***
          ***REMOVED***
      ***REMOVED***)

        binding.btnVoipHangUp.setOnClickListener(View.OnClickListener ***REMOVED***
            if (mInitStatus == -1) ***REMOVED***
                Toast.makeText(
                    this@VoipActivity,
                    "initWxCloudVoip还未完成初始化",
                    Toast.LENGTH_SHORT
                ).show()
                return@OnClickListener
          ***REMOVED***
            if (mInitStatus != 0) ***REMOVED***
                Toast.makeText(
                    this@VoipActivity,
                    "initWxCloudVoip初始化失败：$mInitStatus",
                    Toast.LENGTH_SHORT
                ).show()
                return@OnClickListener
          ***REMOVED***

            mDialog = ProgressDialog.show(this@VoipActivity, "", "挂断doWxCloudVoipHangUp", true)
            if (!executor.isShutdown) ***REMOVED***
                executor.submit ***REMOVED***
                    var result = ""
                    val ret: Int = VoipNativeInterface.getInstance().doWxCloudVoipHangUp(
                        mProductId, mDeviceName, mOpenId, mVoipDeviceId
                    )
                    result = if (ret == 0) ***REMOVED***
                        "已挂断"
                  ***REMOVED*** else ***REMOVED***
                        "挂断失败"
                  ***REMOVED***
                    Log.i(TAG, "VOIP call result: $result")
                    val finalResult = result
                    runOnUiThread ***REMOVED***
                        mDialog?.dismiss()
                        Toast.makeText(this@VoipActivity, finalResult, Toast.LENGTH_SHORT).show()
                        binding.tvTips.text = finalResult
                        binding.tvTips.visibility = View.INVISIBLE
                        binding.ivAudio.visibility = View.INVISIBLE
                        binding.btnVoipHangUp.visibility = View.INVISIBLE
                        binding.textureViewVoip.visibility = View.INVISIBLE
                        //                                mRemoteSurfaceView.setVisibility(View.INVISIBLE);
                        binding.llButtons.visibility = View.VISIBLE
                        binding.rvUserList.visibility = View.VISIBLE
                  ***REMOVED***
              ***REMOVED***
          ***REMOVED***
      ***REMOVED***)
        binding.btnVoipHangUp.visibility = View.INVISIBLE

        binding.textureViewVoip.requestFocus()
        binding.textVoipDevice.isVisible = false
        //        String devinfo = "VOIP device: " + mVoipProductId + "/" + mVoipDeviceId;
//        mTextVoipDevice.setText(devinfo);
  ***REMOVED***

    private fun setOpenId() ***REMOVED***
        when (selectedPosition) ***REMOVED***
            0 -> mOpenId = VoipSetting.getInstance(this).openId1
            1 -> mOpenId = VoipSetting.getInstance(this).openId2
            2 -> mOpenId = VoipSetting.getInstance(this).openId3
      ***REMOVED***
  ***REMOVED***

    override fun onCreate(savedInstanceState: Bundle?) ***REMOVED***
        Log.d(TAG, "start create")
        super.onCreate(savedInstanceState)
  ***REMOVED***

    override fun onDestroy() ***REMOVED***
        Log.d(TAG, "destory")
        super.onDestroy()
        executor.shutdown()
  ***REMOVED***

    private fun checkConditions() ***REMOVED***
        if (condition1 && condition2 && mRemotePreviewSurface != null) ***REMOVED***
            mPlayer.startVideoPlay(Surface(mRemotePreviewSurface), visitor, type, height, width)
      ***REMOVED***
  ***REMOVED***

    override fun onStartRecvVideoStream(visitor: Int, channel: Int, type: Int, height: Int, width: Int
    ): Int ***REMOVED***
        Log.d(TAG, "start video visitor $visitor h: $height w: $width")
        runOnUiThread ***REMOVED***
            binding.surfaceViewVoipBg.visibility = View.VISIBLE
            mRemoteView?.visibility = View.VISIBLE
            mRemoteView?.bringToFront()
            binding.textureViewVoip.bringToFront()
            binding.btnVoipHangUp.visibility = View.VISIBLE
            binding.llButtons.visibility = View.INVISIBLE
            binding.rvUserList.visibility = View.INVISIBLE
      ***REMOVED***
        this.visitor = visitor
        this.type = type
        this.height = height
        this.width = width
        if (mRemotePreviewSurface != null) ***REMOVED***
            synchronized(lock) ***REMOVED***
                condition2 = true
                checkConditions()
          ***REMOVED***
            return 0
      ***REMOVED*** else ***REMOVED***
            synchronized(lock) ***REMOVED***
                condition2 = true
                checkConditions()
          ***REMOVED***
            Log.d(TAG, "IvStartRecvVideoStream mRemotePreviewSurface is null visitor $visitor")
            return -1
      ***REMOVED***
  ***REMOVED***

    override fun onStartRecvAudioStream(visitor: Int, channel: Int, type: Int, option: Int, mode: Int, width: Int, sample_rate: Int, sample_num: Int): Int ***REMOVED***
        Log.d(TAG, "IvStartRecvAudioStream visitor $visitor")
        binding.tvTips.text = "通话中"
        return super.onStartRecvAudioStream(visitor, channel, type, option, mode, width, sample_rate, sample_num)
  ***REMOVED***

    override fun onStopRecvStream(visitor: Int, channel: Int, streamType: Int): Int ***REMOVED***
        super.onStopRecvStream(visitor, channel, streamType)
        if (streamType == 1) ***REMOVED***
            runOnUiThread(Runnable ***REMOVED***
                binding.surfaceViewVoipBg.visibility = View.INVISIBLE
                mRemoteView?.visibility = View.INVISIBLE
                binding.btnVoipHangUp.visibility = View.INVISIBLE
                binding.tvTips.visibility = View.INVISIBLE
                binding.ivAudio.visibility = View.INVISIBLE
                binding.textureViewVoip.visibility = View.INVISIBLE
                binding.llButtons.visibility = View.VISIBLE
                binding.rvUserList.visibility = View.VISIBLE
          ***REMOVED***)
      ***REMOVED***
        return 0
  ***REMOVED***

    override fun onSurfaceTextureAvailable(
        surfaceTexture: SurfaceTexture,
        width: Int,
        height: Int
    ) ***REMOVED***
        if (surfaceTexture == binding.textureViewVoip.surfaceTexture) ***REMOVED***
            // Initialize the SurfaceTexture object
            mLocalPreviewSurface = surfaceTexture

            // Start the camera encoder
            mCameraRecorder.openCamera(mLocalPreviewSurface, this)
      ***REMOVED*** else if (surfaceTexture == mRemoteView?.surfaceTexture) ***REMOVED***
            mRemotePreviewSurface = surfaceTexture
            synchronized(lock) ***REMOVED***
                condition1 = true
                checkConditions()
          ***REMOVED***
      ***REMOVED***
  ***REMOVED***

    override fun onSurfaceTextureSizeChanged(
        surfaceTexture: SurfaceTexture,
        width: Int,
        height: Int
    ) ***REMOVED***
        // Not used in this example
  ***REMOVED***

    override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean ***REMOVED***
        if (surfaceTexture == binding.textureViewVoip.surfaceTexture) ***REMOVED***
            // Stop the camera encoder
            mCameraRecorder.closeCamera()
      ***REMOVED***

        return true
  ***REMOVED***

    override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) ***REMOVED***
        // Not used in this example
  ***REMOVED***

    private fun deleteDeviceKeyFile() ***REMOVED***
        // 假设SD卡的路径是 /sdcard
        val sdCardPath = "/sdcard"
        val fileName = "device_key"

        // 创建一个File对象，表示device_key文件
        val deviceKeyFile = File(sdCardPath, fileName)

        // 检查文件是否存在
        if (deviceKeyFile.exists()) ***REMOVED***
            // 文件存在，尝试删除
            if (deviceKeyFile.delete()) ***REMOVED***
                Log.i(TAG, "device_key文件已成功删除。")
          ***REMOVED*** else ***REMOVED***
                Log.i(TAG, "删除device_key文件失败。")
          ***REMOVED***
      ***REMOVED*** else ***REMOVED***
            Log.i(TAG, "device_key文件不存在。")
      ***REMOVED***
  ***REMOVED***

    companion object ***REMOVED***
        private val TAG: String = VoipActivity::class.java.simpleName
  ***REMOVED***
}