package com.example.ivdemo;

import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Context;
import com.tencent.iot.voipdemo.R;

import androidx.appcompat.app.AppCompatActivity;

import com.tencent.iot.voip.device.VoipNativeInterface;
import com.tencent.iot.voip.device.callback.IvAvtCallback;
import com.tencent.iot.voip.device.callback.IvDeviceCallback;
import com.tencent.iot.voip.device.consts.CommandType;
import com.tencent.iot.voip.device.consts.P2pEventType;
import com.tencent.iot.voip.device.consts.StreamType;
import com.tencent.iot.voip.device.model.AvtInitInfo;
import com.tencent.iot.voip.device.model.DeviceInfo;
import com.tencent.iot.voip.device.model.SysInitInfo;
import com.tencent.iotvideo.link.CameraRecorder;


public class IPCActivity extends AppCompatActivity implements IvAvtCallback ***REMOVED***
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

    protected void initWidget() ***REMOVED***
        setContentView(R.layout.activity_ipc);
//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)

        mTextDevinfo = findViewById(R.id.text_ipc_devinfo);
        // Find the TextureView in the layout
        mTextureView = findViewById(R.id.textureView_ipc);

        mPlayer = new SimplePlayer();
        mCameraRecorder = new CameraRecorder();

        Button callButton = findViewById(R.id.btn_ipc_call);
        callButton.setOnClickListener(new View.OnClickListener() ***REMOVED***
            @Override
            public void onClick(View v) ***REMOVED***
                long time = System.currentTimeMillis();
                long timeD = time - lastClickTime;
                //防止频繁点击
                if (0 < timeD && timeD < 1000) ***REMOVED***
                    Toast.makeText(IPCActivity.this, "频繁点击！", Toast.LENGTH_SHORT).show();
                    return;
              ***REMOVED***
                lastClickTime = time;
                // msg_id 6: 按门铃
//                IVoipJNIBridge.getInstance().sendMsgNotice(6);
          ***REMOVED***
      ***REMOVED***);
  ***REMOVED***


    @Override
    protected void onCreate(Bundle savedInstanceState) ***REMOVED***
        Log.d(TAG, "start create");
        super.onCreate(savedInstanceState);

        // set device info
        mProductId = getIntent().getStringExtra("productId");
        mDeviceName = getIntent().getStringExtra("deviceName");
        String deviceKey = getIntent().getStringExtra("deviceKey");
        String region = "china";
        String devinfo = mProductId + "/" + mDeviceName;

        // local AV files for playback
        AssetFileOps fileOps = new AssetFileOps();
        String path = getFilesDir().getAbsolutePath();
        Log.d(TAG, "path is " + path);
        String audioFileName = "audio_sample16000_stereo_64kbps.aac";
        String absAudioFileName = path + "/" + audioFileName;
        String videoFileName = "video_size320x180_gop50_fps25.h264";
        String absVideoFileName = path + "/" + videoFileName;
        fileOps.copyFileFromAssets(getApplicationContext(), audioFileName, absAudioFileName);
        fileOps.copyFileFromAssets(getApplicationContext(), videoFileName, absVideoFileName);

        // start run JNI iot_video_demo
        SysInitInfo info = SysInitInfo.createDefaultSysInitInfo(new DeviceInfo(mProductId, mDeviceName, deviceKey, region));
        VoipNativeInterface.getInstance().initIvSystem(info, new IvDeviceCallback() ***REMOVED***
            @Override
            public void onOnline(long netDateTime) ***REMOVED***
                Log.d(TAG, "onOnline  netDateTime--->" + netDateTime);
          ***REMOVED***

            @Override
            public void onOffline(int status) ***REMOVED***
                Log.d(TAG, "onOffline  status--->" + status);
          ***REMOVED***

            @Override
            public void onModuleStatus(int moduleStatus) ***REMOVED***
                Log.d(TAG, "moduleStatus--->" + moduleStatus);
          ***REMOVED***
      ***REMOVED***);
        VoipNativeInterface.getInstance().initIvDm();
        AvtInitInfo avtInitInfo = AvtInitInfo.createDefaultAvtInitInfo();
        VoipNativeInterface.getInstance().initIvAvt(avtInitInfo, this);
        Log.d(TAG, "run iot_video_demo for " + devinfo);

        initWidget();
        mTextDevinfo.setText(devinfo);
  ***REMOVED***

    @Override
    protected void onDestroy() ***REMOVED***
        VoipNativeInterface.getInstance().exitWxCloudVoip();
        VoipNativeInterface.getInstance().exitIvAvt();
        VoipNativeInterface.getInstance().exitIvDm();
        VoipNativeInterface.getInstance().exitIvSys();
        super.onDestroy();
  ***REMOVED***

    @Override
    public void onStartRealPlay(int visitor, int channel, int res_type) ***REMOVED***
        Log.d(TAG, "onStartRealPlay  visitor " + visitor + " channel " + channel + " res_type " + res_type);
        mCameraRecorder.startRecording(visitor, res_type);
  ***REMOVED***

    @Override
    public void onStopRealPlay(int visitor, int channel, int res_type) ***REMOVED***
        Log.d(TAG, "onStopRealPlay  visitor " + visitor + " channel " + channel + " res_type " + res_type);
        mCameraRecorder.stopRecording(visitor, res_type);
  ***REMOVED***

    @Override
    public int onStartRecvAudioStream(int visitor, int channel, int type, int option, int mode, int width, int sample_rate, int sample_num) ***REMOVED***
        Log.d(TAG, "onStartRecvAudioStream visitor " + visitor);
        return mPlayer.startAudioPlay(visitor, type, option, mode, width, sample_rate, sample_num);
  ***REMOVED***

    @Override
    public int onStartRecvVideoStream(int visitor, int channel, int type, int height, int width) ***REMOVED***
        Log.w(TAG, "onStartRecvVideoStream  video stream is not supported in this activity");
        return 0;
  ***REMOVED***

    @Override
    public void onNotify(int event, int visitor, int channel, int videoResType) ***REMOVED***
        Log.w(TAG, "onNotify()");
        String msg = "";
        switch (event)***REMOVED***
            case P2pEventType.IV_AVT_EVENT_P2P_PEER_CONNECT_FAIL:
            case P2pEventType.IV_AVT_EVENT_P2P_PEER_ERROR:
            ***REMOVED***
                Log.d(TAG,"receive event: peer error");
                msg = "network err";
          ***REMOVED***
            break;
            case P2pEventType.IV_AVT_EVENT_P2P_PEER_ADDR_CHANGED: ***REMOVED***
                Log.d(TAG,"receive event: peer addr change");
                msg = "peer change";
          ***REMOVED***
            break;
            case P2pEventType.IV_AVT_EVENT_P2P_PEER_READY:
            case P2pEventType.IV_AVT_EVENT_P2P_WATERMARK_LOW:
            case P2pEventType.IV_AVT_EVENT_P2P_WATERMARK_WARN:
            case P2pEventType.IV_AVT_EVENT_P2P_WATERMARK_HIGH:
            case P2pEventType.IV_AVT_EVENT_P2P_LOCAL_NET_READY:
                // do nothing
                break;

            default:
                Log.d(TAG,"not support event");
                msg = "unsupport event type " + event;
                break;
      ***REMOVED***
        if (!msg.isEmpty())***REMOVED***
            updateUI(this, msg);
      ***REMOVED***
  ***REMOVED***

    public int onStopRecvStream(int visitor, int channel, int streamType) ***REMOVED***
        Log.d(TAG, "onStopRecvStream visitor " + visitor + " stream_type " + streamType + " stopped");
        if (streamType == StreamType.IV_AVT_STREAM_TYPE_VIDEO) ***REMOVED***
            return mPlayer.stopVideoPlay(visitor);
      ***REMOVED*** else ***REMOVED***
            return mPlayer.stopAudioPlay(visitor);
      ***REMOVED***
  ***REMOVED***

    @Override
    public int onRecvStream(int visitor, int streamType, byte[] data, int len, long pts, long seq) ***REMOVED***
        Log.d(TAG, "onRecvStream visitor " + visitor + " stream_type " + streamType + " data" + data + "  len" + len + "   pts" + pts + "   seq" + seq);
        if (streamType == 1) ***REMOVED***
            return mPlayer.playVideoStream(visitor, data, len, pts, seq);

      ***REMOVED*** else if (streamType == 0) ***REMOVED***
            return mPlayer.playAudioStream(visitor, data, len, pts, seq);
      ***REMOVED***
        return 0;
  ***REMOVED***

    @Override
    public int onRecvCommand(int command, int visitor, int channel, int videoResType, Object args) ***REMOVED***
        Log.d(TAG, "onRecvCommand command " + command + " visitor " + visitor + " channel" + channel + "   videoResType" + videoResType + "   args" + args);
        String msg = "";
        switch (command) ***REMOVED***
            case CommandType.IV_AVT_COMMAND_USR_DATA: ***REMOVED***
                Log.d(TAG, "receive command: user data");
                msg = "user data";
          ***REMOVED***
            break;
            case CommandType.IV_AVT_COMMAND_REQ_STREAM: ***REMOVED***
                Log.d(TAG, "receive command: request stream");
                msg = "request stream";
          ***REMOVED***
            break;
            case CommandType.IV_AVT_COMMAND_CHN_NAME: ***REMOVED***
                Log.d(TAG, "receive command: get channel name");
                msg = "get channel name";
          ***REMOVED***
            break;
            case CommandType.IV_AVT_COMMAND_REQ_IFRAME: ***REMOVED***
                Log.d(TAG, "receive command: request I frame");
                msg = "request I frame";
          ***REMOVED***
            break;
            case CommandType.IV_AVT_COMMAND_PLAYBACK_PAUSE: ***REMOVED***
                Log.d(TAG, "receive command: playback pause");
                msg = "playback pause";
          ***REMOVED***
            break;
            case CommandType.IV_AVT_COMMAND_PLAYBACK_RESUME: ***REMOVED***
                Log.d(TAG, "receive command: playback resume");
                msg = "playback resume";
          ***REMOVED***
            break;
            case CommandType.IV_AVT_COMMAND_PLAYBACK_QUERY_MONTH: ***REMOVED***
                Log.d(TAG, "receive command: playback query month");
                msg = "playback query month";
          ***REMOVED***
            break;
            case CommandType.IV_AVT_COMMAND_PLAYBACK_QUERY_DAY: ***REMOVED***
                Log.d(TAG, "receive command: playback query day");
                msg = "playback query day";
          ***REMOVED***
            break;
            case CommandType.IV_AVT_COMMAND_PLAYBACK_SEEK: ***REMOVED***
                Log.d(TAG, "receive command: playback seek");
                msg = "playback seek";
          ***REMOVED***
            break;
            case CommandType.IV_AVT_COMMAND_PLAYBACK_FF: ***REMOVED***
                Log.d(TAG, "receive command: playback fast forward");
                msg = "playback fast forward";
          ***REMOVED***
            break;
            case CommandType.IV_AVT_COMMAND_PLAYBACK_SPEED: ***REMOVED***
                Log.d(TAG, "receive command: playback speed");
                msg = "playback speed";
          ***REMOVED***
            break;
            case CommandType.IV_AVT_COMMAND_PLAYBACK_REWIND: ***REMOVED***
                Log.d(TAG, "receive command: playback rewind");
                msg = "playback rewind";
          ***REMOVED***
            break;
            case CommandType.IV_AVT_COMMAND_PLAYBACK_PROGRESS: ***REMOVED***
                Log.d(TAG, "receive command: playback progress");
                msg = "playback progress";
          ***REMOVED***
            break;
            case CommandType.IV_AVT_COMMAND_QUERY_FILE_LIST: ***REMOVED***
                Log.d(TAG, "receive command: get file list");
                msg = "get file list";
          ***REMOVED***
            break;
            case CommandType.IV_AVT_COMMAND_CALL_ANSWER: ***REMOVED***
                Log.d(TAG, "receive command: call answer");
                msg = "call answer";
          ***REMOVED***
            break;
            case CommandType.IV_AVT_COMMAND_CALL_HANG_UP: ***REMOVED***
                Log.d(TAG, "receive command: call hang up");
                msg = "call hang up";
          ***REMOVED***
            break;
            case CommandType.IV_AVT_COMMAND_CALL_REJECT: ***REMOVED***
                Log.d(TAG, "receive command: call reject");
                msg = "call reject";
          ***REMOVED***
            break;
            case CommandType.IV_AVT_COMMAND_CALL_CANCEL: ***REMOVED***
                Log.d(TAG, "receive command: call cancel");
                msg = "call cancel";
          ***REMOVED***
            break;
            case CommandType.IV_AVT_COMMAND_CALL_BUSY: ***REMOVED***
                Log.d(TAG, "receive command: call busy");
                msg = "call busy";
          ***REMOVED***
            break;
            case CommandType.IV_AVT_COMMAND_CALL_TIMEOUT: ***REMOVED***
                Log.d(TAG, "receive command: call timeout");
                msg = "call timeout";
          ***REMOVED***
            break;

            default:
                Log.d(TAG, "not support command");
                msg = "unsupport cmd type " + command;
                break;
      ***REMOVED***
        // Toast.makeText(IPCActivity.this, msg, Toast.LENGTH_SHORT).show();
        updateUI(this, msg);
        return 0;
  ***REMOVED***

    @Override
    public int onDownloadFile(int status, int visitor, int channel, Object args) ***REMOVED***
        Log.d(TAG, "onDownloadFile status " + status + " visitor " + visitor + " channel"+channel+"   args"+args);
        return 0;
  ***REMOVED***

    @Override
    public void onGetPeerOuterNet(int visitor, int channel, String netInfo) ***REMOVED***
        Log.d(TAG, "onGetPeerOuterNet visitor " + visitor + " channel " + channel + " netInfo"+netInfo);
  ***REMOVED***

    public void updateUI(Context context, String msg) ***REMOVED***
        ((IPCActivity) context).runOnUiThread(new Runnable() ***REMOVED***
            @Override
            public void run() ***REMOVED***
                //此时已在主线程中，可以更新UI了
                Toast.makeText(IPCActivity.this, msg, Toast.LENGTH_SHORT).show();
          ***REMOVED***
      ***REMOVED***);
  ***REMOVED***
}