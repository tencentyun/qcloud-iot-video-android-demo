package com.example.ivdemo;

import android.app.ProgressDialog;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tencent.iot.voip.device.VoipNativeInterface;
import com.tencent.iot.voip.device.consts.StreamType;
import com.tencent.iotvideo.link.CameraRecorder;
import com.tencent.iotvideo.link.adapter.UserListAdapter;
import com.tencent.iotvideo.link.entity.UserEntity;
import com.tencent.iotvideo.link.util.QualitySetting;
import com.tencent.iotvideo.link.util.VoipSetting;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.tencent.iot.voipdemo.R;


public class VoipActivity extends IPCActivity implements TextureView.SurfaceTextureListener ***REMOVED***
    private static final String TAG = VoipActivity.class.getSimpleName();

    private String mModelId;
    private String mVoipDeviceId;
    private String mWxaAppId;
    private String mOpenId;
    private String mSNTicket;
    private TextView mTextVoipDevice;
    private RecyclerView mUserListRv;
    private UserListAdapter mUserListAdapter;
    private ArrayList<UserEntity> mUsersData;

    private TextView mTipsTv;
    private ImageView mAudioIv;
    private View mSurfaceBgView;
    private Button mHangUpButton;
    private LinearLayout mButtonsLL;

    private int mInitStatus = -1; // 未初始化 -1， 初始化成功 0， 其他
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private ProgressDialog mDialog;
    private int mMiniprogramVersion;   //0 "正式版", 1  "开发版", 2 "体验版"
    private int selectedPosition = RecyclerView.NO_POSITION;

    private boolean condition1 = false;
    private boolean condition2 = false;
    private final Object lock = new Object();

    private int visitor;
    private int type;
    private int height;
    private int width;

    public ArrayList<UserEntity> getmUsersData() ***REMOVED***
        if (mUsersData == null) ***REMOVED***
            mUsersData = new ArrayList<UserEntity>();
            UserEntity user1 = new UserEntity();
            user1.setOpenId(VoipSetting.getInstance(this).openId1);
            mUsersData.add(user1);
            UserEntity user2 = new UserEntity();
            user2.setOpenId(VoipSetting.getInstance(this).openId2);
            mUsersData.add(user2);
            UserEntity user3 = new UserEntity();
            user3.setOpenId(VoipSetting.getInstance(this).openId3);
            mUsersData.add(user3);
      ***REMOVED***
        return mUsersData;
  ***REMOVED***

    @Override
    protected void initWidget() ***REMOVED***
        setContentView(R.layout.activity_voip);

        mUserListRv = findViewById(R.id.rv_user_list);
        // 设置管理器
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mUserListRv.setLayoutManager(layoutManager);
        // 如果可以确定每个item的高度是固定的，设置这个选项可以提高性能
        mUserListRv.setHasFixedSize(true);
        // 设置适配器，刷新展示用户列表
        mUserListAdapter = new UserListAdapter(this, getmUsersData());
        mUserListRv.setAdapter(mUserListAdapter);
        mUserListAdapter.setOnSelectedListener(new UserListAdapter.OnSelectedListener() ***REMOVED***
            @Override
            public void onSelected(int position) ***REMOVED***
                selectedPosition = position;
          ***REMOVED***
      ***REMOVED***);

        mTipsTv = findViewById(R.id.tv_tips);
        mTipsTv.setVisibility(View.INVISIBLE);
        mAudioIv = findViewById(R.id.iv_audio);
        mAudioIv.setVisibility(View.INVISIBLE);

        mTextDevinfo = findViewById(R.id.text_voip_iv_devinfo);
        mTextDevinfo.setVisibility(View.INVISIBLE);
        mTextureView = findViewById(R.id.textureView_voip);
        mTextureView.setSurfaceTextureListener(this);
        mTextureView.setVisibility(View.INVISIBLE);

        mRemoteView = findViewById(R.id.surfaceView_voip);
        mRemoteView.setSurfaceTextureListener(this);
        mRemoteView.setVisibility(View.INVISIBLE);

        mSurfaceBgView = findViewById(R.id.surfaceView_voip_bg);
        mSurfaceBgView.setVisibility(View.INVISIBLE);

        mButtonsLL = findViewById(R.id.ll_buttons);

        mPlayer = new SimplePlayer();
        mCameraRecorder = new CameraRecorder();

        // wx voip init
        mModelId = getIntent().getStringExtra("voip_model_id");
        mVoipDeviceId = getIntent().getStringExtra("voip_device_id");
        mWxaAppId = getIntent().getStringExtra("voip_wxa_appid");
        mOpenId = getIntent().getStringExtra("voip_open_id");
        mSNTicket = getIntent().getStringExtra("voip_sn_ticket");
        mMiniprogramVersion = getIntent().getIntExtra("miniprogramVersion", 0);

        try ***REMOVED***
            File dir = new File(Environment.getExternalStorageDirectory(), "ivdemo");
            if (!dir.exists()) ***REMOVED***
                dir.mkdir();
          ***REMOVED***

      ***REMOVED*** catch (Exception e) ***REMOVED***
            e.printStackTrace();
            Toast.makeText(VoipActivity.this, "创建文件夹失败！", Toast.LENGTH_LONG).show();
            return;
      ***REMOVED***
        String baseDir = Environment.getExternalStorageDirectory().getAbsolutePath();
        String path = baseDir + "/ivdemo/";
        // 证书文件需要保存在相同的路径
        AssetFileOps fileOps = new AssetFileOps();
        String certFileName = "cacert.pem";
        String absCertFileName = path + "/" + certFileName;
        fileOps.copyFileFromAssets(getApplicationContext(), certFileName, absCertFileName);

        mDialog = ProgressDialog.show(VoipActivity.this, "", "正在加载初始化initWxCloudVoip", true);

        if (!executor.isShutdown()) ***REMOVED***
            executor.submit(() -> ***REMOVED***
//                int ret = VoipNativeInterface.getInstance().initWxCloudVoip(path, "mHostAppId", mModelId,
//                        "mVoipProductId", mVoipDeviceId, "mVoipDeviceSign", mWxaAppId, mSNTicket, mMiniprogramVersion);
                int ret = VoipNativeInterface.getInstance().initWxCloudVoip(mModelId, mVoipDeviceId, mWxaAppId, mSNTicket, mMiniprogramVersion);
                Log.i(TAG, "initWxCloudVoip ret: " + ret);
                mInitStatus = ret;
                if (ret == 19) ***REMOVED***
                    //把device_key文件删掉
                    deleteDeviceKeyFile();
                    mInitStatus = VoipNativeInterface.getInstance().initWxCloudVoip(mModelId, mVoipDeviceId, mWxaAppId, mSNTicket, mMiniprogramVersion);
                    Log.i(TAG, "reInitWxCloudVoip ret: " + ret);
              ***REMOVED***
                runOnUiThread(new Runnable() ***REMOVED***
                    @Override
                    public void run() ***REMOVED***
                        if (mDialog != null) ***REMOVED***
                            mDialog.dismiss();
                      ***REMOVED***
                  ***REMOVED***
              ***REMOVED***);
          ***REMOVED***);
      ***REMOVED***

        Button callButton = findViewById(R.id.btn_voip_video_call);
        callButton.setOnClickListener(new View.OnClickListener() ***REMOVED***
            @Override
            public void onClick(View v) ***REMOVED***
                mTextureView.setVisibility(View.VISIBLE);
                if (selectedPosition == RecyclerView.NO_POSITION) ***REMOVED***
                    Toast.makeText(VoipActivity.this, "请勾选被呼叫的用户！", Toast.LENGTH_SHORT).show();
                    return;
              ***REMOVED***
                setOpenId();
                if (TextUtils.isEmpty(mOpenId)) ***REMOVED***
                    Toast.makeText(VoipActivity.this, "请输入被呼叫的用户openid！", Toast.LENGTH_SHORT).show();
                    return;
              ***REMOVED***
                if (mInitStatus == -1) ***REMOVED***
                    Toast.makeText(VoipActivity.this, "initWxCloudVoip还未完成初始化", Toast.LENGTH_SHORT).show();
                    return;
              ***REMOVED***
                if (mInitStatus != 0) ***REMOVED***
                    Toast.makeText(VoipActivity.this, "initWxCloudVoip初始化失败：" + mInitStatus, Toast.LENGTH_SHORT).show();
                    return;
              ***REMOVED***

                mDialog = ProgressDialog.show(VoipActivity.this, "", "呼叫中doWxCloudVoipCall", true);
                if (!executor.isShutdown()) ***REMOVED***
                    executor.submit(() -> ***REMOVED***
                        // voip call
                        String result = "";
                        int recvPixel = QualitySetting.getInstance(VoipActivity.this).getWxResolution();
                        boolean calleeCameraSwitch = QualitySetting.getInstance(VoipActivity.this).isWxCameraOn();
                        int ret = VoipNativeInterface.getInstance().doWxCloudVoipCall(
                                mModelId, mWxaAppId, mOpenId, mVoipDeviceId, recvPixel, calleeCameraSwitch);
                        if (ret == -2) ***REMOVED***
                            result = "通话中";
                      ***REMOVED*** else if (ret != 0) ***REMOVED***
                            result = "呼叫失败";
                      ***REMOVED*** else ***REMOVED***
                            result = "呼叫成功";
                      ***REMOVED***
                        Log.i(TAG, "VOIP call result: " + result + ", ret: " + ret);
                        String finalResult = result;
                        runOnUiThread(new Runnable() ***REMOVED***
                            @Override
                            public void run() ***REMOVED***
                                if (mDialog != null) ***REMOVED***
                                    mDialog.dismiss();
                              ***REMOVED***
                                Toast.makeText(VoipActivity.this, finalResult, Toast.LENGTH_SHORT).show();
                                updateVideoUI(true);
                          ***REMOVED***
                      ***REMOVED***);
                  ***REMOVED***);
              ***REMOVED***
          ***REMOVED***
      ***REMOVED***);

        Button audioCallButton = findViewById(R.id.btn_voip_audio_call);
        audioCallButton.setOnClickListener(new View.OnClickListener() ***REMOVED***
            @Override
            public void onClick(View view) ***REMOVED***

                mTextureView.setVisibility(View.INVISIBLE);
                if (selectedPosition == RecyclerView.NO_POSITION) ***REMOVED***
                    Toast.makeText(VoipActivity.this, "请勾选被呼叫的用户！", Toast.LENGTH_SHORT).show();
                    return;
              ***REMOVED***
                setOpenId();
                if (TextUtils.isEmpty(mOpenId)) ***REMOVED***
                    Toast.makeText(VoipActivity.this, "请输入被呼叫的用户openid！", Toast.LENGTH_SHORT).show();
                    return;
              ***REMOVED***
                if (mInitStatus == -1) ***REMOVED***
                    Toast.makeText(VoipActivity.this, "initWxCloudVoip还未完成初始化", Toast.LENGTH_SHORT).show();
                    return;
              ***REMOVED***
                if (mInitStatus != 0) ***REMOVED***
                    Toast.makeText(VoipActivity.this, "initWxCloudVoip初始化失败：" + mInitStatus, Toast.LENGTH_SHORT).show();
                    return;
              ***REMOVED***

                mDialog = ProgressDialog.show(VoipActivity.this, "", "呼叫中doWxCloudVoipAudioCall", true);
                if (!executor.isShutdown()) ***REMOVED***
                    executor.submit(() -> ***REMOVED***
                        // voip call
                        String result = "";
                        int ret = VoipNativeInterface.getInstance().doWxCloudVoipAudioCall(
                                mModelId, mWxaAppId, mOpenId, mVoipDeviceId);
                        if (ret == -2) ***REMOVED***
                            result = "通话中";
                      ***REMOVED*** else if (ret != 0) ***REMOVED***
                            result = "呼叫失败";
                      ***REMOVED*** else ***REMOVED***
                            result = "呼叫成功";
                      ***REMOVED***
                        Log.i(TAG, "VOIP call result: " + result + ", ret: " + ret);
                        String finalResult = result;
                        runOnUiThread(new Runnable() ***REMOVED***
                            @Override
                            public void run() ***REMOVED***
                                if (mDialog != null) ***REMOVED***
                                    mDialog.dismiss();
                              ***REMOVED***
                                Toast.makeText(VoipActivity.this, finalResult, Toast.LENGTH_SHORT).show();
                                mTipsTv.setText(finalResult);
                                updateAudioUI(true);
                          ***REMOVED***
                      ***REMOVED***);
                  ***REMOVED***);
              ***REMOVED***
          ***REMOVED***
      ***REMOVED***);

        mHangUpButton = findViewById(R.id.btn_voip_hang_up);
        mHangUpButton.setOnClickListener(new View.OnClickListener() ***REMOVED***
            @Override
            public void onClick(View v) ***REMOVED***
                if (mInitStatus == -1) ***REMOVED***
                    Toast.makeText(VoipActivity.this, "initWxCloudVoip还未完成初始化", Toast.LENGTH_SHORT).show();
                    return;
              ***REMOVED***
                if (mInitStatus != 0) ***REMOVED***
                    Toast.makeText(VoipActivity.this, "initWxCloudVoip初始化失败：" + mInitStatus, Toast.LENGTH_SHORT).show();
                    return;
              ***REMOVED***

                mDialog = ProgressDialog.show(VoipActivity.this, "", "挂断doWxCloudVoipHangUp", true);
                if (!executor.isShutdown()) ***REMOVED***
                    executor.submit(() -> ***REMOVED***
                        String result = "";
                        int ret = VoipNativeInterface.getInstance().doWxCloudVoipHangUp(
                                mProductId, mDeviceName, mOpenId, mVoipDeviceId);
                        if (ret == 0) ***REMOVED***
                            result = "已挂断";
                      ***REMOVED*** else ***REMOVED***
                            result = "挂断失败";
                      ***REMOVED***
                        Log.i(TAG, "VOIP call result: " + result);
                        String finalResult = result;
                        runOnUiThread(new Runnable() ***REMOVED***
                            @Override
                            public void run() ***REMOVED***
                                if (mDialog != null) ***REMOVED***
                                    mDialog.dismiss();
                              ***REMOVED***
                                Toast.makeText(VoipActivity.this, finalResult, Toast.LENGTH_SHORT).show();
                                mTipsTv.setText(finalResult);
                                updateVideoUI(false);
                          ***REMOVED***
                      ***REMOVED***);
                  ***REMOVED***);
              ***REMOVED***
          ***REMOVED***
      ***REMOVED***);
        mHangUpButton.setVisibility(View.INVISIBLE);

        mTextureView.requestFocus();
        mTextVoipDevice = findViewById(R.id.text_voip_device);
        mTextVoipDevice.setVisibility(View.INVISIBLE);
//        String devinfo = "VOIP device: " + mVoipProductId + "/" + mVoipDeviceId;
//        mTextVoipDevice.setText(devinfo);
  ***REMOVED***

    private void setOpenId() ***REMOVED***
        switch (selectedPosition) ***REMOVED***
            case 0:
                mOpenId = VoipSetting.getInstance(this).openId1;
                break;
            case 1:
                mOpenId = VoipSetting.getInstance(this).openId2;
                break;
            case 2:
                mOpenId = VoipSetting.getInstance(this).openId3;
                break;
      ***REMOVED***
  ***REMOVED***

    @Override
    protected void onCreate(Bundle savedInstanceState) ***REMOVED***
        Log.d(TAG, "start create");
        super.onCreate(savedInstanceState);
  ***REMOVED***

    @Override
    protected void onDestroy() ***REMOVED***
        Log.d(TAG, "destory");
        super.onDestroy();
        executor.shutdown();
  ***REMOVED***

    private void checkConditions() ***REMOVED***
        if (condition1 && condition2 && mRemotePreviewSurface != null) ***REMOVED***
            mPlayer.startVideoPlay(new Surface(mRemotePreviewSurface), visitor, type, height, width);
      ***REMOVED***
  ***REMOVED***

    @Override
    public int onStartRecvAudioStream(int visitor, int channel, int type, int option, int mode, int width, int sample_rate, int sample_num) ***REMOVED***
        Log.d(TAG, "IvStartRecvAudioStream visitor " + visitor);
        mTipsTv.setText("通话中");
        return super.onStartRecvAudioStream(visitor, channel, type, option, mode, width, sample_rate, sample_num);
  ***REMOVED***

    @Override
    public int onStartRecvVideoStream(int visitor, int channel, int type, int height, int width) ***REMOVED***
        Log.d(TAG, "start video visitor " + visitor + " h: " + height + " w: " + width);
        runOnUiThread(new Runnable() ***REMOVED***
            @Override
            public void run() ***REMOVED***
                updateVideoUI(true);
          ***REMOVED***
      ***REMOVED***);
        this.visitor = visitor;
        this.type = type;
        this.height = height;
        this.width = width;
        if (mRemotePreviewSurface != null) ***REMOVED***
            synchronized (lock) ***REMOVED***
                condition2 = true;
                checkConditions();
          ***REMOVED***
            return 0;
      ***REMOVED*** else ***REMOVED***
            synchronized (lock) ***REMOVED***
                condition2 = true;
                checkConditions();
          ***REMOVED***
            Log.d(TAG, "IvStartRecvVideoStream mRemotePreviewSurface is null visitor " + visitor);
            return -1;
      ***REMOVED***
  ***REMOVED***

    @Override
    public int onStopRecvStream(int visitor, int channel, int streamType) ***REMOVED***
        super.onStopRecvStream(visitor, channel, streamType);
        if (streamType == StreamType.IV_AVT_STREAM_TYPE_VIDEO || streamType == StreamType.IV_AVT_STREAM_TYPE_AV) ***REMOVED***
            runOnUiThread(new Runnable() ***REMOVED***
                @Override
                public void run() ***REMOVED***
                    updateVideoUI(false);
              ***REMOVED***
          ***REMOVED***);
      ***REMOVED***
        return 0;
  ***REMOVED***

    @Override
    public void onStopRealPlay(int visitor, int channel, int res_type) ***REMOVED***
        super.onStopRealPlay(visitor, channel, res_type);
        runOnUiThread(new Runnable() ***REMOVED***
            @Override
            public void run() ***REMOVED***
                updateVideoUI(false);
          ***REMOVED***
      ***REMOVED***);
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
            synchronized (lock) ***REMOVED***
                condition1 = true;
                checkConditions();
          ***REMOVED***
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

    private void deleteDeviceKeyFile() ***REMOVED***
        // 假设SD卡的路径是 /sdcard
        String sdCardPath = "/sdcard";
        String fileName = "device_key";

        // 创建一个File对象，表示device_key文件
        File deviceKeyFile = new File(sdCardPath, fileName);

        // 检查文件是否存在
        if (deviceKeyFile.exists()) ***REMOVED***
            // 文件存在，尝试删除
            if (deviceKeyFile.delete()) ***REMOVED***
                Log.i(TAG, "device_key文件已成功删除。");
          ***REMOVED*** else ***REMOVED***
                Log.i(TAG, "删除device_key文件失败。");
          ***REMOVED***
      ***REMOVED*** else ***REMOVED***
            Log.i(TAG, "device_key文件不存在。");
      ***REMOVED***
  ***REMOVED***

    public void updateVideoUI(boolean isCalling) ***REMOVED***
        if (isCalling) ***REMOVED***
            mSurfaceBgView.setVisibility(View.VISIBLE);
            mRemoteView.setVisibility(View.VISIBLE);
            mRemoteView.bringToFront();
            mTextureView.setVisibility(View.VISIBLE);
            mTextureView.bringToFront();
            mHangUpButton.setVisibility(View.VISIBLE);
            mButtonsLL.setVisibility(View.INVISIBLE);
            mUserListRv.setVisibility(View.INVISIBLE);
      ***REMOVED*** else ***REMOVED***
            mSurfaceBgView.setVisibility(View.INVISIBLE);
            mRemoteView.setVisibility(View.INVISIBLE);
            mHangUpButton.setVisibility(View.INVISIBLE);
            mTipsTv.setVisibility(View.INVISIBLE);
            mAudioIv.setVisibility(View.INVISIBLE);
            mTextureView.setVisibility(View.INVISIBLE);
            mButtonsLL.setVisibility(View.VISIBLE);
            mUserListRv.setVisibility(View.VISIBLE);
      ***REMOVED***
  ***REMOVED***

    public void updateAudioUI(boolean isCalling) ***REMOVED***
        if (isCalling) ***REMOVED***
            mTipsTv.setVisibility(View.VISIBLE);
            mAudioIv.setVisibility(View.VISIBLE);
            mHangUpButton.setVisibility(View.VISIBLE);
            mButtonsLL.setVisibility(View.INVISIBLE);
            mUserListRv.setVisibility(View.INVISIBLE);
      ***REMOVED*** else ***REMOVED***
            mTipsTv.setVisibility(View.INVISIBLE);
            mAudioIv.setVisibility(View.INVISIBLE);
            mHangUpButton.setVisibility(View.INVISIBLE);
            mButtonsLL.setVisibility(View.VISIBLE);
            mUserListRv.setVisibility(View.VISIBLE);
      ***REMOVED***
  ***REMOVED***
}