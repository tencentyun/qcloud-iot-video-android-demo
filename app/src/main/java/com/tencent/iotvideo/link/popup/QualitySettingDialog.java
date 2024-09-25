package com.tencent.iotvideo.link.popup;

import android.content.Context;
import android.hardware.Camera;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.tencent.iot.voipdemo.R;
import com.tencent.iotvideo.link.consts.CameraConstants;
import com.tencent.iotvideo.link.entity.ResolutionEntity;
import com.tencent.iotvideo.link.util.QualitySetting;
import com.tencent.iotvideo.link.util.VoipSetting;

import java.util.ArrayList;
import java.util.List;

public class QualitySettingDialog extends IosCenterStyleDialog {

    private static final String TAG = QualitySettingDialog.class.getSimpleName();

    private Spinner mLocalResolutionSp;

    private ArrayList<ResolutionEntity> localResolutionArray = new ArrayList<ResolutionEntity>();

    private int selectedLocalResolution = 0;
    private SeekBar mFrameRateSb;

    private int selectedFrameRate = 15;

    private TextView mFrameRateTv;
    private SeekBar mBitRateSb;
    private int minBitRateSbValue;
    private int selectedBitRate = 0;

    private TextView mBitRateTv;

    private Spinner mWxResolutionSp;

    private final String[] wxResolutionArray = {"可变自适应", "240x320", "320x240", "480x352", "480x640"};

    private int selectedWxResolution = 0;

    private Switch mWxCameraSettingSw;

    private boolean selectedWxCameraSetting = true;

    private boolean needShowDefaultValue = true;
    private Button mConfirmBtn;

    public QualitySettingDialog(Context context) {
        super(context, R.layout.popup_quality_setting_layout);
        getSupportedPreviewSizes();
    }

    @Override
    public void initView() {
        super.initView();
        //获取控件
        mLocalResolutionSp = view.findViewById(R.id.sp_voip_local_resolution);
        mFrameRateSb = view.findViewById(R.id.sb_voip_local_frame_rate);
        mBitRateSb = view.findViewById(R.id.sb_voip_local_bit_rate);
        mWxResolutionSp = view.findViewById(R.id.sp_voip_wx_resolution);
        mWxCameraSettingSw = view.findViewById(R.id.sw_voip_wx_camera_is_open);
        mFrameRateTv = view.findViewById(R.id.tv_voip_local_frame_rate_tip);
        mBitRateTv = view.findViewById(R.id.tv_voip_local_frame_bit_tip);
        mConfirmBtn = view.findViewById(R.id.btn_confirm);

        // 控件初始值设定
        if (localResolutionArray != null && mLocalResolutionSp != null) {
            ArrayAdapter<ResolutionEntity> adapter1 = new ArrayAdapter<ResolutionEntity>(getContext(), android.R.layout.simple_spinner_item, localResolutionArray);
            mLocalResolutionSp.setAdapter(adapter1);
        }

        ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_item, wxResolutionArray);
        mWxResolutionSp.setAdapter(adapter2);


        if (QualitySetting.getInstance(getContext()).getWidth() != 0) {
            setSpinnerDefaultValueFromDisk(QualitySetting.getInstance(getContext()).getWidth(), QualitySetting.getInstance(getContext()).getHeight());
            selectedFrameRate = QualitySetting.getInstance(getContext()).getFrameRate();
            selectedBitRate = QualitySetting.getInstance(getContext()).getBitRate();
            selectedWxResolution = QualitySetting.getInstance(getContext()).getWxResolution();
            selectedWxCameraSetting = QualitySetting.getInstance(getContext()).isWxCameraOn();
        } else {
            if (!setSpinnerDefaultValue("360p")) {
                setSelectedLocalResolution(0);
            }
        }
        mLocalResolutionSp.setSelection(selectedLocalResolution);

        mFrameRateSb.setMax(24 - 10);
        mFrameRateSb.setProgress(selectedFrameRate - 10);
        setSelectedFrameRate(selectedFrameRate);
        mWxResolutionSp.setSelection(selectedWxResolution);
        mWxCameraSettingSw.setChecked(selectedWxCameraSetting);

        //控件事件
        mLocalResolutionSp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                setSelectedLocalResolution(position);
                needShowDefaultValue = false;
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        mFrameRateSb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    setSelectedFrameRate(progress + 10);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        mBitRateSb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    setSelectedBitRate(progress + minBitRateSbValue);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        mWxResolutionSp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                selectedWxResolution = position;
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        mWxCameraSettingSw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                selectedWxCameraSetting = checked;
            }
        });
        mConfirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                ResolutionEntity entity = localResolutionArray.get(selectedLocalResolution);
                QualitySetting.getInstance(getContext()).setWidth(entity.getWidth());
                QualitySetting.getInstance(getContext()).setHeight(entity.getHeight());
                QualitySetting.getInstance(getContext()).setFrameRate(selectedFrameRate);
                QualitySetting.getInstance(getContext()).setBitRate(selectedBitRate);
                QualitySetting.getInstance(getContext()).setWxResolution(selectedWxResolution);
                QualitySetting.getInstance(getContext()).setWxCameraOn(selectedWxCameraSetting);
                QualitySetting.getInstance(getContext()).saveData();

                Log.e(TAG, "****========== width:" + entity.getWidth() + "， height: " +entity.getHeight()  + "， frameRate:" +
                        selectedFrameRate + "， bitRate:" + selectedBitRate + "， wxResolution:" + selectedWxResolution
                        + "， CameraSetting:" + selectedWxCameraSetting + "****========== ");
                dismiss();
                if (onDismisListener != null) {
                    onDismisListener.onDismised();
                }
            }
        });
    }

    /**
     * 获取设备支持哪些分辨率
     */
    private void getSupportedPreviewSizes() {
        Camera camera = Camera.open(CameraConstants.facing.BACK);
        //获取相机参数
        Camera.Parameters parameters = camera.getParameters();
        List<Camera.Size> list = parameters.getSupportedPreviewSizes();
        for (Camera.Size size : list) {
            Log.e(TAG, "****========== " + size.width + " " + size.height);
            if (size.width == 640 && size.height == 360) {
                ResolutionEntity entity = new ResolutionEntity(size.width, size.height, "360p");
                localResolutionArray.add(entity);
            } else if (size.width == 960 && size.height == 540) {
                ResolutionEntity entity = new ResolutionEntity(size.width, size.height, "540p");
                localResolutionArray.add(entity);
            } else if (size.width == 1280 && size.height == 720) {
                ResolutionEntity entity = new ResolutionEntity(size.width, size.height, "720p");
                localResolutionArray.add(entity);
            } else if (size.width == 1920 && size.height == 1080) {
                ResolutionEntity entity = new ResolutionEntity(size.width, size.height, "1080p");
                localResolutionArray.add(entity);
            }
        }
        camera.setPreviewCallback(null);
        camera.stopPreview();
        camera.release();
    }

    private boolean setSpinnerDefaultValue(String defaultValue) {
        for (int i = 0; i < localResolutionArray.size(); i++) {
            if (localResolutionArray.get(i).getSimpleName().equals(defaultValue)) {
                setSelectedLocalResolution(i);
                mLocalResolutionSp.setSelection(i);
                return true;
            }
        }
        return false;
    }

    private boolean setSpinnerDefaultValueFromDisk(int width, int height) {
        for (int i = 0; i < localResolutionArray.size(); i++) {
            ResolutionEntity entity = localResolutionArray.get(i);
            if (entity.getWidth() == width && entity.getHeight() == height) {
                setSelectedLocalResolution(i);
                mLocalResolutionSp.setSelection(i);
                return true;
            }
        }
        return false;
    }

    public void setSelectedLocalResolution(int selectedLocalResolution) {
        this.selectedLocalResolution = selectedLocalResolution;
        ResolutionEntity entity = localResolutionArray.get(selectedLocalResolution);
        switch (entity.getSimpleName()) {
            case "360p":

                minBitRateSbValue = 200;
                mBitRateSb.setMax(1000 - minBitRateSbValue);
                if (needShowDefaultValue && QualitySetting.getInstance(getContext()).getBitRate() != 0) {
                    setSelectedBitRate(QualitySetting.getInstance(getContext()).getBitRate());
                    mBitRateSb.setProgress(selectedBitRate - minBitRateSbValue);
                } else {
                    mBitRateSb.setProgress(800 - minBitRateSbValue);
                    setSelectedBitRate(800);
                }
                break;
            case "540p":
                minBitRateSbValue = 400;
                mBitRateSb.setMax(1600 - minBitRateSbValue);
                if (needShowDefaultValue && QualitySetting.getInstance(getContext()).getBitRate() != 0) {
                    setSelectedBitRate(QualitySetting.getInstance(getContext()).getBitRate());
                    mBitRateSb.setProgress(selectedBitRate - minBitRateSbValue);
                } else {
                    mBitRateSb.setProgress(900 - minBitRateSbValue);
                    setSelectedBitRate(900);
                }
                break;
            case "720p":
                minBitRateSbValue = 500;
                mBitRateSb.setMax(2000 - minBitRateSbValue);
                if (needShowDefaultValue && QualitySetting.getInstance(getContext()).getBitRate() != 0) {
                    setSelectedBitRate(QualitySetting.getInstance(getContext()).getBitRate());
                    mBitRateSb.setProgress(selectedBitRate - minBitRateSbValue);
                } else {
                    mBitRateSb.setProgress(1250 - minBitRateSbValue);
                    setSelectedBitRate(1250);
                }
                break;
            case "1080p":
                minBitRateSbValue = 800;
                mBitRateSb.setMax(3000 - minBitRateSbValue);
                if (needShowDefaultValue && QualitySetting.getInstance(getContext()).getBitRate() != 0) {
                    setSelectedBitRate(QualitySetting.getInstance(getContext()).getBitRate());
                    mBitRateSb.setProgress(selectedBitRate - minBitRateSbValue);
                } else {
                    mBitRateSb.setProgress(1900 - minBitRateSbValue);
                    setSelectedBitRate(1900);
                }
                break;
        }
    }

    public void setSelectedFrameRate(int selectedFrameRate) {
        this.selectedFrameRate = selectedFrameRate;
        mFrameRateTv.setText(String.format("%d fps", selectedFrameRate));
    }

    public void setSelectedBitRate(int selectedBitRate) {
        this.selectedBitRate = selectedBitRate;
        mBitRateTv.setText(String.format("%d kbps", selectedBitRate));
    }

    private volatile WxSettingDialog.OnDismisListener onDismisListener;

    public interface OnDismisListener {
        void onDismised();
    }

    public void setOnDismisListener(WxSettingDialog.OnDismisListener onDismisListener) {
        this.onDismisListener = onDismisListener;
    }
}
