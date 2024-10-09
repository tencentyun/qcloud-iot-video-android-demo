package com.example.ivdemo;

import android.app.ProgressDialog;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
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

import com.tencent.iot.video.device.VideoNativeInterface;
import com.tencent.iot.video.device.annotations.StreamType;
import com.tencent.iot.voipdemo.R;
import com.tencent.iotvideo.link.CameraRecorder;
import com.tencent.iotvideo.link.adapter.UserListAdapter;
import com.tencent.iotvideo.link.entity.UserEntity;
import com.tencent.iotvideo.link.util.QualitySetting;
import com.tencent.iotvideo.link.util.VoipSetting;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class VoipActivity extends IPCActivity implements TextureView.SurfaceTextureListener {
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

    public ArrayList<UserEntity> getmUsersData() {
        if (mUsersData == null) {
            mUsersData = new ArrayList<>();
            UserEntity user1 = new UserEntity();
            user1.setOpenId(VoipSetting.getInstance(this).openId1);
            mUsersData.add(user1);
            UserEntity user2 = new UserEntity();
            user2.setOpenId(VoipSetting.getInstance(this).openId2);
            mUsersData.add(user2);
            UserEntity user3 = new UserEntity();
            user3.setOpenId(VoipSetting.getInstance(this).openId3);
            mUsersData.add(user3);
        }
        return mUsersData;
    }

    @Override
    protected void initWidget() {
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
        mUserListAdapter.setOnSelectedListener(position -> selectedPosition = position);

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

        mDialog = ProgressDialog.show(VoipActivity.this, "", "正在加载初始化initWxCloudVoip", true);

        if (!executor.isShutdown()) {
            executor.submit(() -> {
                mInitStatus = initWxCloudVoip();
                if (mInitStatus == 19) {
                    //把device_key文件删掉
                    deleteDeviceKeyFile();
                    mInitStatus = initWxCloudVoip();
                }
            });
        }

        Button callButton = findViewById(R.id.btn_voip_video_call);
        callButton.setOnClickListener(v -> {
            mTextureView.setVisibility(View.VISIBLE);
            if (selectedPosition == RecyclerView.NO_POSITION) {
                Toast.makeText(VoipActivity.this, "请勾选被呼叫的用户！", Toast.LENGTH_SHORT).show();
                return;
            }
            setOpenId();
            if (TextUtils.isEmpty(mOpenId)) {
                Toast.makeText(VoipActivity.this, "请输入被呼叫的用户openid！", Toast.LENGTH_SHORT).show();
                return;
            }
            if (mInitStatus == -1) {
                Toast.makeText(VoipActivity.this, "initWxCloudVoip还未完成初始化", Toast.LENGTH_SHORT).show();
                return;
            }
            if (mInitStatus != 0) {
                Toast.makeText(VoipActivity.this, "initWxCloudVoip初始化失败：" + mInitStatus, Toast.LENGTH_SHORT).show();
                return;
            }

            mDialog = ProgressDialog.show(VoipActivity.this, "", "呼叫中doWxCloudVoipCall", true);
            if (!executor.isShutdown()) {
                executor.submit(() -> {
                    // voip call
                    String result = "";
                    int recvPixel = QualitySetting.getInstance(VoipActivity.this).getWxResolution();
                    boolean calleeCameraSwitch = QualitySetting.getInstance(VoipActivity.this).isWxCameraOn();
                    int ret = VideoNativeInterface.getInstance().doWxCloudVoipCall(
                            mModelId, mWxaAppId, mOpenId, mVoipDeviceId, recvPixel, calleeCameraSwitch);
                    if (ret == -2) {
                        result = "通话中";
                    } else if (ret != 0) {
                        result = "呼叫失败";
                    } else {
                        result = "呼叫成功";
                    }
                    Log.i(TAG, "VOIP call result: " + result + ", ret: " + ret);
                    String finalResult = result;
                    runOnUiThread(() -> {
                        if (mDialog != null) {
                            mDialog.dismiss();
                        }
                        Toast.makeText(VoipActivity.this, finalResult, Toast.LENGTH_SHORT).show();
                        updateVideoUI(true);
                    });
                });
            }
        });

        Button audioCallButton = findViewById(R.id.btn_voip_audio_call);
        audioCallButton.setOnClickListener(view -> {

            mTextureView.setVisibility(View.INVISIBLE);
            if (selectedPosition == RecyclerView.NO_POSITION) {
                Toast.makeText(VoipActivity.this, "请勾选被呼叫的用户！", Toast.LENGTH_SHORT).show();
                return;
            }
            setOpenId();
            if (TextUtils.isEmpty(mOpenId)) {
                Toast.makeText(VoipActivity.this, "请输入被呼叫的用户openid！", Toast.LENGTH_SHORT).show();
                return;
            }
            if (mInitStatus == -1) {
                Toast.makeText(VoipActivity.this, "initWxCloudVoip还未完成初始化", Toast.LENGTH_SHORT).show();
                return;
            }
            if (mInitStatus != 0) {
                Toast.makeText(VoipActivity.this, "initWxCloudVoip初始化失败：" + mInitStatus, Toast.LENGTH_SHORT).show();
                return;
            }

            mDialog = ProgressDialog.show(VoipActivity.this, "", "呼叫中doWxCloudVoipAudioCall", true);
            if (!executor.isShutdown()) {
                executor.submit(() -> {
                    // voip call
                    String result = "";
                    int ret = VideoNativeInterface.getInstance().doWxCloudVoipAudioCall(
                            mModelId, mWxaAppId, mOpenId, mVoipDeviceId);
                    if (ret == -2) {
                        result = "通话中";
                    } else if (ret != 0) {
                        result = "呼叫失败";
                    } else {
                        result = "呼叫成功";
                    }
                    Log.i(TAG, "VOIP call result: " + result + ", ret: " + ret);
                    String finalResult = result;
                    runOnUiThread(() -> {
                        if (mDialog != null) {
                            mDialog.dismiss();
                        }
                        Toast.makeText(VoipActivity.this, finalResult, Toast.LENGTH_SHORT).show();
                        mTipsTv.setText(finalResult);
                        updateAudioUI(true);
                    });
                });
            }
        });

        mHangUpButton = findViewById(R.id.btn_voip_hang_up);
        mHangUpButton.setOnClickListener(v -> {
            if (mInitStatus == -1) {
                Toast.makeText(VoipActivity.this, "initWxCloudVoip还未完成初始化", Toast.LENGTH_SHORT).show();
                return;
            }
            if (mInitStatus != 0) {
                Toast.makeText(VoipActivity.this, "initWxCloudVoip初始化失败：" + mInitStatus, Toast.LENGTH_SHORT).show();
                return;
            }

            mDialog = ProgressDialog.show(VoipActivity.this, "", "挂断doWxCloudVoipHangUp", true);
            if (!executor.isShutdown()) {
                executor.submit(() -> {
                    String result = "";
                    int ret = VideoNativeInterface.getInstance().doWxCloudVoipHangUp(
                            mProductId, mDeviceName, mOpenId, mVoipDeviceId);
                    if (ret == 0) {
                        result = "已挂断";
                    } else {
                        result = "挂断失败";
                    }
                    Log.i(TAG, "VOIP call result: " + result);
                    String finalResult = result;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (mDialog != null) {
                                mDialog.dismiss();
                            }
                            Toast.makeText(VoipActivity.this, finalResult, Toast.LENGTH_SHORT).show();
                            mTipsTv.setText(finalResult);
                            updateVideoUI(false);
                        }
                    });
                });
            }
        });
        mHangUpButton.setVisibility(View.INVISIBLE);

        mTextureView.requestFocus();
        mTextVoipDevice = findViewById(R.id.text_voip_device);
        mTextVoipDevice.setVisibility(View.INVISIBLE);
//        String devinfo = "VOIP device: " + mVoipProductId + "/" + mVoipDeviceId;
//        mTextVoipDevice.setText(devinfo);
    }

    private void setOpenId() {
        switch (selectedPosition) {
            case 0:
                mOpenId = VoipSetting.getInstance(this).openId1;
                break;
            case 1:
                mOpenId = VoipSetting.getInstance(this).openId2;
                break;
            case 2:
                mOpenId = VoipSetting.getInstance(this).openId3;
                break;
        }
    }

    /**
     * 初始化 voip
     *
     * @return 初始化状态值
     */
    private int initWxCloudVoip() {
        int initStatus = VideoNativeInterface.getInstance().initWxCloudVoip(mModelId, mVoipDeviceId, mWxaAppId, mMiniprogramVersion);
        if (initStatus == 0) {
            Log.i(TAG, "reInitWxCloudVoip initStatus: " + initStatus);
            int registeredState = VideoNativeInterface.getInstance().isAvtVoipRegistered();
            Log.i(TAG, "isAvtVoipRegistered: " + registeredState);
            if (registeredState == 0) {
                int registerRes = VideoNativeInterface.getInstance().registerAvtVoip(mSNTicket);
                Log.i(TAG, "registerAvtVoip registerRes: " + registerRes);
            }
            runOnUiThread(() -> {
                if (mDialog != null) {
                    mDialog.dismiss();
                }
            });
        }
        return initStatus;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "start create");
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "destory");
        super.onDestroy();
        executor.shutdown();
    }

    private void checkConditions() {
        if (condition1 && condition2 && mRemotePreviewSurface != null) {
            mPlayer.startVideoPlay(new Surface(mRemotePreviewSurface), visitor, type, height, width);
        }
    }

    @Override
    public int onStartRecvAudioStream(int visitor, int channel, int type, int option, int mode, int width, int sample_rate, int sample_num) {
        Log.d(TAG, "IvStartRecvAudioStream visitor " + visitor);
        mTipsTv.setText("通话中");
        return super.onStartRecvAudioStream(visitor, channel, type, option, mode, width, sample_rate, sample_num);
    }

    @Override
    public int onStartRecvVideoStream(int visitor, int channel, int type, int height, int width, int frameRate) {
        Log.d(TAG, "start video visitor " + visitor + " h: " + height + " w: " + width);
        runOnUiThread(() -> updateVideoUI(true));
        this.visitor = visitor;
        this.type = type;
        this.height = height;
        this.width = width;
        if (mRemotePreviewSurface != null) {
            synchronized (lock) {
                condition2 = true;
                checkConditions();
            }
            return 0;
        } else {
            synchronized (lock) {
                condition2 = true;
                checkConditions();
            }
            Log.d(TAG, "IvStartRecvVideoStream mRemotePreviewSurface is null visitor " + visitor);
            return -1;
        }
    }

    @Override
    public int onStopRecvStream(int visitor, int channel, int streamType) {
        super.onStopRecvStream(visitor, channel, streamType);
        if (streamType == StreamType.IV_AVT_STREAM_TYPE_VIDEO || streamType == StreamType.IV_AVT_STREAM_TYPE_AV) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateVideoUI(false);
                }
            });
        }
        return 0;
    }

    @Override
    public void onStopRealPlay(int visitor, int channel, int res_type) {
        super.onStopRealPlay(visitor, channel, res_type);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateVideoUI(false);
            }
        });
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

    private void deleteDeviceKeyFile() {
        // 假设SD卡的路径是 /sdcard
        String sdCardPath = "/sdcard";
        String fileName = "device_key";

        // 创建一个File对象，表示device_key文件
        File deviceKeyFile = new File(sdCardPath, fileName);

        // 检查文件是否存在
        if (deviceKeyFile.exists()) {
            // 文件存在，尝试删除
            if (deviceKeyFile.delete()) {
                Log.i(TAG, "device_key文件已成功删除。");
            } else {
                Log.i(TAG, "删除device_key文件失败。");
            }
        } else {
            Log.i(TAG, "device_key文件不存在。");
        }
    }

    public void updateVideoUI(boolean isCalling) {
        if (isCalling) {
            mSurfaceBgView.setVisibility(View.VISIBLE);
            mRemoteView.setVisibility(View.VISIBLE);
            mRemoteView.bringToFront();
            mTextureView.setVisibility(View.VISIBLE);
            mTextureView.bringToFront();
            mHangUpButton.setVisibility(View.VISIBLE);
            mButtonsLL.setVisibility(View.INVISIBLE);
            mUserListRv.setVisibility(View.INVISIBLE);
        } else {
            mSurfaceBgView.setVisibility(View.INVISIBLE);
            mRemoteView.setVisibility(View.INVISIBLE);
            mHangUpButton.setVisibility(View.INVISIBLE);
            mTipsTv.setVisibility(View.INVISIBLE);
            mAudioIv.setVisibility(View.INVISIBLE);
            mTextureView.setVisibility(View.INVISIBLE);
            mButtonsLL.setVisibility(View.VISIBLE);
            mUserListRv.setVisibility(View.VISIBLE);
        }
    }

    public void updateAudioUI(boolean isCalling) {
        if (isCalling) {
            mTipsTv.setVisibility(View.VISIBLE);
            mAudioIv.setVisibility(View.VISIBLE);
            mHangUpButton.setVisibility(View.VISIBLE);
            mButtonsLL.setVisibility(View.INVISIBLE);
            mUserListRv.setVisibility(View.INVISIBLE);
        } else {
            mTipsTv.setVisibility(View.INVISIBLE);
            mAudioIv.setVisibility(View.INVISIBLE);
            mHangUpButton.setVisibility(View.INVISIBLE);
            mButtonsLL.setVisibility(View.VISIBLE);
            mUserListRv.setVisibility(View.VISIBLE);
        }
    }
}