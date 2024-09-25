package com.example.ivdemo;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.tencent.iot.video.device.VideoNativeInterface;
import com.tencent.iot.video.device.callback.IvAvtCallback;
import com.tencent.iot.video.device.callback.IvDeviceCallback;
import com.tencent.iot.video.device.consts.CommandType;
import com.tencent.iot.video.device.consts.P2pEventType;
import com.tencent.iot.video.device.consts.StreamType;
import com.tencent.iot.video.device.model.AvDataInfo;
import com.tencent.iot.video.device.model.AvtInitInfo;
import com.tencent.iot.video.device.model.SysInitInfo;
import com.tencent.iot.voipdemo.R;
import com.tencent.iotvideo.link.CameraRecorder;


public class IPCActivity extends AppCompatActivity implements IvAvtCallback {
    private static final String TAG = IPCActivity.class.getSimpleName();

    protected SimplePlayer mPlayer;
    protected CameraRecorder mCameraRecorder;
    // view for remote video
    protected TextureView mRemoteView;
    // view for local camera preview
    protected SurfaceTexture mLocalPreviewSurface;
    // view for remote preview
    protected SurfaceTexture mRemotePreviewSurface;
    protected TextureView mTextureView;
    // display device info
    protected TextView mTextDevinfo;
    protected String mProductId;
    protected String mDeviceName;

    private static long lastClickTime;

    protected void initWidget() {
        setContentView(R.layout.activity_ipc);
//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)

        mTextDevinfo = findViewById(R.id.text_ipc_devinfo);
        // Find the TextureView in the layout
        mTextureView = findViewById(R.id.textureView_ipc);

        mPlayer = new SimplePlayer();
        mCameraRecorder = new CameraRecorder();

        Button callButton = findViewById(R.id.btn_ipc_call);
        callButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long time = System.currentTimeMillis();
                long timeD = time - lastClickTime;
                //防止频繁点击
                if (0 < timeD && timeD < 1000) {
                    Toast.makeText(IPCActivity.this, "频繁点击！", Toast.LENGTH_SHORT).show();
                    return;
                }
                lastClickTime = time;
                // msg_id 6: 按门铃
                VideoNativeInterface.getInstance().sendMsgNotice(6);
            }
        });
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "start create");
        super.onCreate(savedInstanceState);

        // set device info
        mProductId = getIntent().getStringExtra("productId");
        mDeviceName = getIntent().getStringExtra("deviceName");
        String deviceKey = getIntent().getStringExtra("deviceKey");
        String region = "china";
        String devinfo = mProductId + "/" + mDeviceName;
        Log.d(TAG, "run iot_video_demo for " + devinfo);

        // start run JNI iot_video_demo
        SysInitInfo info = new SysInitInfo(mProductId, mDeviceName, deviceKey, region);
        VideoNativeInterface.getInstance().initIvSystem(info, new IvDeviceCallback() {
            @Override
            public void onOnline(long netDateTime) {
                Log.d(TAG, "onOnline  netDateTime--->" + netDateTime);
            }

            @Override
            public void onOffline(int status) {
                Log.d(TAG, "onOffline  status--->" + status);
            }

            @Override
            public void onModuleStatus(int moduleStatus) {
                Log.d(TAG, "moduleStatus--->" + moduleStatus);
            }
        });
        VideoNativeInterface.getInstance().initIvDm();
        AvtInitInfo avtInitInfo = new AvtInitInfo();
        VideoNativeInterface.getInstance().initIvAvt(avtInitInfo, this);
        initWidget();
        mTextDevinfo.setText(devinfo);
    }

    @Override
    protected void onDestroy() {
        VideoNativeInterface.getInstance().exitWxCloudVoip();
        VideoNativeInterface.getInstance().exitIvAvt();
        VideoNativeInterface.getInstance().exitIvDm();
        VideoNativeInterface.getInstance().exitIvSys();
        super.onDestroy();
    }

    @Override
    public AvDataInfo onGetAvEncInfo(int visitor, int channel, int videoResType) {
        return AvDataInfo.createDefaultAvDataInfo(videoResType);
    }

    @Override
    public void onStartRealPlay(int visitor, int channel, int res_type) {
        Log.d(TAG, "onStartRealPlay  visitor " + visitor + " channel " + channel + " res_type " + res_type);
        mCameraRecorder.startRecording(visitor, res_type);
    }

    @Override
    public void onStopRealPlay(int visitor, int channel, int res_type) {
        Log.d(TAG, "onStopRealPlay  visitor " + visitor + " channel " + channel + " res_type " + res_type);
        mCameraRecorder.stopRecording(visitor, res_type);
    }

    @Override
    public int onStartRecvAudioStream(int visitor, int channel, int type, int option, int mode, int width, int sample_rate, int sample_num) {
        Log.d(TAG, "onStartRecvAudioStream visitor " + visitor);
        return mPlayer.startAudioPlay(visitor, type, option, mode, width, sample_rate, sample_num);
    }

    @Override
    public int onStartRecvVideoStream(int visitor, int channel, int type, int height, int width, int frameRate) {
        Log.w(TAG, "onStartRecvVideoStream  video stream is not supported in this activity");
        return 0;
    }

    @Override
    public void onNotify(int event, int visitor, int channel, int videoResType) {
        Log.w(TAG, "onNotify()");
        String msg = "";
        switch (event) {
            case P2pEventType.IV_AVT_EVENT_P2P_PEER_CONNECT_FAIL:
            case P2pEventType.IV_AVT_EVENT_P2P_PEER_ERROR: {
                Log.d(TAG, "receive event: peer error");
                msg = "network err";
            }
            break;
            case P2pEventType.IV_AVT_EVENT_P2P_PEER_ADDR_CHANGED: {
                Log.d(TAG, "receive event: peer addr change");
                msg = "peer change";
            }
            break;
            case P2pEventType.IV_AVT_EVENT_P2P_PEER_READY:
            case P2pEventType.IV_AVT_EVENT_P2P_WATERMARK_LOW:
            case P2pEventType.IV_AVT_EVENT_P2P_WATERMARK_WARN:
            case P2pEventType.IV_AVT_EVENT_P2P_WATERMARK_HIGH:
            case P2pEventType.IV_AVT_EVENT_P2P_LOCAL_NET_READY:
                // do nothing
                break;

            default:
                Log.d(TAG, "not support event");
                msg = "unsupport event type " + event;
                break;
        }
        if (!msg.isEmpty()) {
            updateUI(this, msg);
        }
    }

    public int onStopRecvStream(int visitor, int channel, int streamType) {
        Log.d(TAG, "onStopRecvStream visitor " + visitor + " stream_type " + streamType + " stopped");
        if (streamType == StreamType.IV_AVT_STREAM_TYPE_VIDEO) {
            return mPlayer.stopVideoPlay(visitor);
        } else {
            return mPlayer.stopAudioPlay(visitor);
        }
    }

    @Override
    public int onRecvStream(int visitor, int streamType, byte[] data, int len, long pts, long seq) {
        Log.d(TAG, "onRecvStream visitor " + visitor + " stream_type " + streamType + " data" + data + "  len" + len + "   pts" + pts + "   seq" + seq);
        if (streamType == 1) {
            return mPlayer.playVideoStream(visitor, data, len, pts, seq);

        } else if (streamType == 0) {
            return mPlayer.playAudioStream(visitor, data, len, pts, seq);
        }
        return 0;
    }

    @Override
    public int onRecvCommand(int command, int visitor, int channel, int videoResType, Object args) {
        Log.d(TAG, "onRecvCommand command " + command + " visitor " + visitor + " channel" + channel + "   videoResType" + videoResType + "   args" + args);
        String msg = "";
        switch (command) {
            case CommandType.IV_AVT_COMMAND_USR_DATA: {
                Log.d(TAG, "receive command: user data");
                msg = "user data";
            }
            break;
            case CommandType.IV_AVT_COMMAND_REQ_STREAM: {
                Log.d(TAG, "receive command: request stream");
                msg = "request stream";
            }
            break;
            case CommandType.IV_AVT_COMMAND_CHN_NAME: {
                Log.d(TAG, "receive command: get channel name");
                msg = "get channel name";
            }
            break;
            case CommandType.IV_AVT_COMMAND_REQ_IFRAME: {
                Log.d(TAG, "receive command: request I frame");
                msg = "request I frame";
            }
            break;
            case CommandType.IV_AVT_COMMAND_PLAYBACK_PAUSE: {
                Log.d(TAG, "receive command: playback pause");
                msg = "playback pause";
            }
            break;
            case CommandType.IV_AVT_COMMAND_PLAYBACK_RESUME: {
                Log.d(TAG, "receive command: playback resume");
                msg = "playback resume";
            }
            break;
            case CommandType.IV_AVT_COMMAND_PLAYBACK_QUERY_MONTH: {
                Log.d(TAG, "receive command: playback query month");
                msg = "playback query month";
            }
            break;
            case CommandType.IV_AVT_COMMAND_PLAYBACK_QUERY_DAY: {
                Log.d(TAG, "receive command: playback query day");
                msg = "playback query day";
            }
            break;
            case CommandType.IV_AVT_COMMAND_PLAYBACK_SEEK: {
                Log.d(TAG, "receive command: playback seek");
                msg = "playback seek";
            }
            break;
            case CommandType.IV_AVT_COMMAND_PLAYBACK_FF: {
                Log.d(TAG, "receive command: playback fast forward");
                msg = "playback fast forward";
            }
            break;
            case CommandType.IV_AVT_COMMAND_PLAYBACK_SPEED: {
                Log.d(TAG, "receive command: playback speed");
                msg = "playback speed";
            }
            break;
            case CommandType.IV_AVT_COMMAND_PLAYBACK_REWIND: {
                Log.d(TAG, "receive command: playback rewind");
                msg = "playback rewind";
            }
            break;
            case CommandType.IV_AVT_COMMAND_PLAYBACK_PROGRESS: {
                Log.d(TAG, "receive command: playback progress");
                msg = "playback progress";
            }
            break;
            case CommandType.IV_AVT_COMMAND_QUERY_FILE_LIST: {
                Log.d(TAG, "receive command: get file list");
                msg = "get file list";
            }
            break;
            case CommandType.IV_AVT_COMMAND_CALL_ANSWER: {
                Log.d(TAG, "receive command: call answer");
                msg = "call answer";
            }
            break;
            case CommandType.IV_AVT_COMMAND_CALL_HANG_UP: {
                Log.d(TAG, "receive command: call hang up");
                msg = "call hang up";
            }
            break;
            case CommandType.IV_AVT_COMMAND_CALL_REJECT: {
                Log.d(TAG, "receive command: call reject");
                msg = "call reject";
            }
            break;
            case CommandType.IV_AVT_COMMAND_CALL_CANCEL: {
                Log.d(TAG, "receive command: call cancel");
                msg = "call cancel";
            }
            break;
            case CommandType.IV_AVT_COMMAND_CALL_BUSY: {
                Log.d(TAG, "receive command: call busy");
                msg = "call busy";
            }
            break;
            case CommandType.IV_AVT_COMMAND_CALL_TIMEOUT: {
                Log.d(TAG, "receive command: call timeout");
                msg = "call timeout";
            }
            break;

            default:
                Log.d(TAG, "not support command");
                msg = "unsupport cmd type " + command;
                break;
        }
        // Toast.makeText(IPCActivity.this, msg, Toast.LENGTH_SHORT).show();
        updateUI(this, msg);
        return 0;
    }

    @Override
    public int onDownloadFile(int status, int visitor, int channel, Object args) {
        Log.d(TAG, "onDownloadFile status " + status + " visitor " + visitor + " channel" + channel + "   args" + args);
        return 0;
    }

    @Override
    public void onGetPeerOuterNet(int visitor, int channel, String netInfo) {
        Log.d(TAG, "onGetPeerOuterNet visitor " + visitor + " channel " + channel + " netInfo" + netInfo);
    }

    public void updateUI(Context context, String msg) {
        ((IPCActivity) context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //此时已在主线程中，可以更新UI了
                Toast.makeText(IPCActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}