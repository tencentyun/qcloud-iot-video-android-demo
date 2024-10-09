package com.example.ivdemo;

import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.tencent.iot.video.device.VideoNativeInterface;
import com.tencent.iot.voipdemo.R;
import com.tencent.iotvideo.link.CameraRecorder;

import org.json.JSONException;
import org.json.JSONObject;

public class CallAnswerVideoActivity extends IPCActivity implements TextureView.SurfaceTextureListener {
    private static final String TAG = DuplexVideoActivity.class.getSimpleName();
    private static final String COMMAND_WX_CALL_START = "wx_call_start"; //小程序发起请求
    private static final String COMMAND_WX_CALL_CANCEL = "wx_call_cancel"; //小程序取消呼叫
    private static final String COMMAND_WX_CALL_HANGUP = "wx_call_hangup"; //小程序挂断


    private ConstraintLayout clCall = null;

    private Button btnRejectListen = null;
    private TextView tip = null;
    private Button btnHangUp = null;
    private Button btnAnswer = null;
    private volatile boolean isCalling = false;

    private boolean condition1 = false;
    private boolean condition2 = false;
    private final Object lock = new Object();

    private int type;
    private int height;
    private int width;

    private Surface surface;

    @Override
    protected void initWidget() {
        setContentView(R.layout.activity_call_answer_video);

        mTextDevinfo = findViewById(R.id.text_duplex_devinfo);
        mTextureView = findViewById(R.id.textureView_duplex);

        // Set the SurfaceTextureListener on the TextureView
        mTextureView.setSurfaceTextureListener(this);

        mRemoteView = findViewById(R.id.surfaceView_duplex);
        mRemoteView.setSurfaceTextureListener(this);
        mPlayer = new SimplePlayer();
        mCameraRecorder = new CameraRecorder();

        clCall = findViewById(R.id.cl_call);
        btnRejectListen = findViewById(R.id.btn_reject_listen);
        tip = findViewById(R.id.tip_text);
        btnHangUp = findViewById(R.id.btn_hang_up);
        btnAnswer = findViewById(R.id.btn_answer);
        btnRejectListen.setOnClickListener(v -> {
            sendCommand("call_reject");
            finishStream();
            updateCancelCallUI();
        });
        btnHangUp.setOnClickListener(v -> {
            sendCommand("call_hang_up");
            finishStream();
            isCalling = false;
            updateCancelCallUI();
        });

        btnAnswer.setOnClickListener(v -> {
            sendCommand("call_answer");
            isCalling = true;
            btnRejectListen.setVisibility(View.GONE);
            btnAnswer.setVisibility(View.GONE);
            btnHangUp.setVisibility(View.VISIBLE);
            tip.setVisibility(View.VISIBLE);
            updateAnswerUI();
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "start create");
        super.onCreate(savedInstanceState);
    }

    @Override
    public int onStartRecvVideoStream(int visitor, int channel, int type, int height, int width, int frameRate) {
        Log.d(TAG, "start video visitor " + visitor + " h: " + height + " w: " + width);
        this.type = type;
        this.height = height;
        this.width = width;
        synchronized (lock) {
            condition2 = true;
            checkConditions();
        }
        if (mRemotePreviewSurface != null) {
            return 0;
        } else {
            Log.d(TAG, "IvStartRecvVideoStream mRemotePreviewSurface is null visitor " + visitor);
            return -1;
        }
    }

    private void checkConditions() {
        if (condition1 && condition2 && mRemotePreviewSurface != null && surface == null) {
            surface = new Surface(mRemotePreviewSurface);
            mPlayer.startVideoPlay(surface, visitor, type, height, width);
        }
    }

    @Override
    public int onRecvStream(int visitor, int streamType, byte[] data, int len, long pts, long seq) {
        if (!isCalling) return 0;
        Log.d(TAG, "onRecvStream visitor " + visitor + " stream_type " + streamType + " data" + data + "  len" + len + "   pts" + pts + "   seq" + seq);
        if (streamType == 1) {
            return mPlayer.playVideoStream(visitor, data, len, pts, seq);

        } else if (streamType == 0) {
            return mPlayer.playAudioStream(visitor, data, len, pts, seq);
        }
        return 0;
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
            synchronized (lock) {
                condition1 = true;
                checkConditions();
            }
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


    @Override
    public String onRecvCommand(int command, int visitor, int channel, int videoResType, String args) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (args) {
                    case COMMAND_WX_CALL_START:
                        updateCallUI();
                        break;
                    case COMMAND_WX_CALL_CANCEL:
                        updateCancelCallUI();
                        break;
                    case COMMAND_WX_CALL_HANGUP:
                        isCalling = false;
                        updateCancelCallUI();
                        break;
                    case "wx_call_timeout":
                        break;
                }
            }
        });
        JSONObject resJson = new JSONObject();
        try {
            resJson.put("code", 0);
            resJson.put("errMsg", "");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return resJson.toString();
    }

    private void finishStream() {
        VideoNativeInterface.getInstance().sendFinishStreamMsg(visitor, channel, videoResType);
        VideoNativeInterface.getInstance().sendFinishStream(visitor, channel, videoResType);
    }

    private boolean sendCommand(String order) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("iv_private_cmd", order);
        } catch (Exception e) {
            e.printStackTrace();
        }
        String res = VideoNativeInterface.getInstance().sendCommand(visitor, jsonObject.toString(), 1 * 1000);
        Toast.makeText(this, "发送发送:" + jsonObject + "   信令发送结果：" + res, Toast.LENGTH_SHORT).show();
        return !TextUtils.isEmpty(res);
    }

    public void updateCallUI() {
        btnRejectListen.setVisibility(View.VISIBLE);
        btnAnswer.setVisibility(View.VISIBLE);
        btnHangUp.setVisibility(View.GONE);
        clCall.setVisibility(View.VISIBLE);
        tip.setVisibility(View.VISIBLE);
        tip.bringToFront();
        mTextureView.setVisibility(View.VISIBLE);
    }

    public void updateCancelCallUI() {
        clCall.setVisibility(View.GONE);
        mRemoteView.setVisibility(View.INVISIBLE);
        mTextureView.setVisibility(View.INVISIBLE);
    }

    public void updateAnswerUI() {
        mRemoteView.setVisibility(View.VISIBLE);
        mRemoteView.bringToFront();
        mTextureView.setVisibility(View.VISIBLE);
        mTextureView.bringToFront();
    }
}
