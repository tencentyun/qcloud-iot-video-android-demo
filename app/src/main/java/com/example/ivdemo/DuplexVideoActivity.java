package com.example.ivdemo;

import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import com.tencent.iot.voipdemo.R;
import com.tencent.iotvideo.link.CameraRecorder;


public class DuplexVideoActivity extends IPCActivity implements TextureView.SurfaceTextureListener ***REMOVED***
    private static final String TAG = DuplexVideoActivity.class.getSimpleName();

    @Override
    protected void initWidget() ***REMOVED***
        setContentView(R.layout.activity_duplex_video);

        mTextDevinfo = findViewById(R.id.text_duplex_devinfo);
        mTextureView = findViewById(R.id.textureView_duplex);
        // Set the SurfaceTextureListener on the TextureView
        mTextureView.setSurfaceTextureListener(this);

        mRemoteView = findViewById(R.id.surfaceView_duplex);
        mPlayer = new SimplePlayer();
        mCameraRecorder = new CameraRecorder();
  ***REMOVED***

    @Override
    protected void onCreate(Bundle savedInstanceState) ***REMOVED***
        Log.d(TAG, "start create");
        super.onCreate(savedInstanceState);
  ***REMOVED***

    @Override
    public int onStartRecvVideoStream(int visitor, int channel, int type, int height, int width, int frameRate) ***REMOVED***
        if (mRemotePreviewSurface != null) ***REMOVED***
            return mPlayer.startVideoPlay(new Surface(mRemotePreviewSurface), visitor, type, height, width);
      ***REMOVED*** else ***REMOVED***
            Log.d(TAG, "IvStartRecvVideoStream mRemotePreviewSurface is null visitor " + visitor);
            return -1;
      ***REMOVED***
  ***REMOVED***

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) ***REMOVED***

        if (surfaceTexture.equals(mTextureView.getSurfaceTexture())) ***REMOVED***
            // Initialize the SurfaceTexture object
            mLocalPreviewSurface = surfaceTexture;

            // Start the camera encoder
            mCameraRecorder.openCamera(mLocalPreviewSurface, this);
      ***REMOVED*** else if (surfaceTexture.equals(mRemoteView.getSurfaceTexture())) ***REMOVED***
            mRemotePreviewSurface = surfaceTexture;
      ***REMOVED***
  ***REMOVED***

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) ***REMOVED***
        // Not used in this example
  ***REMOVED***

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) ***REMOVED***

        if (surfaceTexture.equals(mTextureView.getSurfaceTexture())) ***REMOVED***
            // Stop the camera encoder
            mCameraRecorder.closeCamera();
      ***REMOVED***

        return true;
  ***REMOVED***

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) ***REMOVED***
        // Not used in this example
  ***REMOVED***
}