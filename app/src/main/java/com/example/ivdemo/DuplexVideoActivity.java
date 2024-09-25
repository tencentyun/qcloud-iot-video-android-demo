package com.example.ivdemo;

import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import com.tencent.iot.voipdemo.R;
import com.tencent.iotvideo.link.CameraRecorder;


public class DuplexVideoActivity extends IPCActivity implements TextureView.SurfaceTextureListener {
    private static final String TAG = DuplexVideoActivity.class.getSimpleName();

    @Override
    protected void initWidget() {
        setContentView(R.layout.activity_duplex_video);

        mTextDevinfo = findViewById(R.id.text_duplex_devinfo);
        mTextureView = findViewById(R.id.textureView_duplex);
        // Set the SurfaceTextureListener on the TextureView
        mTextureView.setSurfaceTextureListener(this);

        mRemoteView = findViewById(R.id.surfaceView_duplex);
        mPlayer = new SimplePlayer();
        mCameraRecorder = new CameraRecorder();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "start create");
        super.onCreate(savedInstanceState);
    }

    @Override
    public int onStartRecvVideoStream(int visitor, int channel, int type, int height, int width, int frameRate) {
        if (mRemotePreviewSurface != null) {
            return mPlayer.startVideoPlay(new Surface(mRemotePreviewSurface), visitor, type, height, width);
        } else {
            Log.d(TAG, "IvStartRecvVideoStream mRemotePreviewSurface is null visitor " + visitor);
            return -1;
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {

        if (surfaceTexture.equals(mTextureView.getSurfaceTexture())) {
            // Initialize the SurfaceTexture object
            mLocalPreviewSurface = surfaceTexture;

            // Start the camera encoder
            mCameraRecorder.openCamera(mLocalPreviewSurface, this);
        } else if (surfaceTexture.equals(mRemoteView.getSurfaceTexture())) {
            mRemotePreviewSurface = surfaceTexture;
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
        // Not used in this example
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {

        if (surfaceTexture.equals(mTextureView.getSurfaceTexture())) {
            // Stop the camera encoder
            mCameraRecorder.closeCamera();
        }

        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        // Not used in this example
    }
}