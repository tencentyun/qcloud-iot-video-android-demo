package com.example.ivdemo

import android.graphics.SurfaceTexture
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.TextureView.SurfaceTextureListener
import com.tencent.iot.twcall.R
import com.tencent.iot.twcall.databinding.ActivityDuplexVideoBinding
import com.tencent.iotvideo.link.CameraRecorder
import com.tencent.iotvideo.link.SimplePlayer

private val TAG: String = DuplexVideoActivity::class.java.simpleName

class DuplexVideoActivity : BaseIPCActivity<ActivityDuplexVideoBinding>() {

    private val player = SimplePlayer()

    private val cameraRecorder = CameraRecorder()

    private var localPreviewSurface: SurfaceTexture? = null

    private var remotePreviewSurface: SurfaceTexture? = null


    private val listener = object : SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            if (surface == binding.textureViewDuplex.surfaceTexture) {
                // Initialize the SurfaceTexture object
                localPreviewSurface = surface

                // Start the camera encoder
                cameraRecorder.openCamera(localPreviewSurface, this@DuplexVideoActivity)
            } else if (surface == binding.surfaceViewDuplex.surfaceTexture) {
                remotePreviewSurface = surface
            }
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            // Not used in this example
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            if (surface == binding.textureViewDuplex.surfaceTexture) {
                // Stop the camera encoder
                cameraRecorder.closeCamera()
            }
            return true
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            // Not used in this example
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "start create")
        super.onCreate(savedInstanceState)
    }

    override fun getViewBinding(): ActivityDuplexVideoBinding =
        ActivityDuplexVideoBinding.inflate(layoutInflater)

    override fun initView() {
        binding.textureViewDuplex.surfaceTextureListener = listener
        binding.titleLayout.tvTitle.text = getString(R.string.title_audio_video_call)
        binding.titleLayout.ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.textDevinfo.text =
            String.format((getString(R.string.text_device_info)), "$productId/$deviceName")
    }

    override fun onStartRecvVideoStream(
        visitor: Int, channel: Int, type: Int, height: Int, width: Int, frameRate: Int
    ): Int {
        return if (remotePreviewSurface != null) {
            player.startVideoPlay(Surface(remotePreviewSurface), visitor, type, height, width)
        } else {
            Log.d(TAG, "IvStartRecvVideoStream mRemotePreviewSurface is null visitor $visitor")
            -1
        }
    }
}