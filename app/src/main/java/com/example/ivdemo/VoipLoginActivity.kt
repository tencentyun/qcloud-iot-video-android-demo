package com.example.ivdemo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.tencent.iot.voipdemo.R;
import com.tencent.iotvideo.link.popup.QualitySettingDialog;
import com.tencent.iotvideo.link.popup.WxSettingDialog;
import com.tencent.iotvideo.link.util.VoipSetting;

public class VoipLoginActivity extends AppCompatActivity {

    private String mProductId;
    private String mDeviceName;
    private String mDeviceKey;

    private TextView mWelcomeSnTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voip_login);

        mProductId = getIntent().getStringExtra("productId");
        mDeviceName = getIntent().getStringExtra("deviceName");
        mDeviceKey = getIntent().getStringExtra("deviceKey");

        mWelcomeSnTv = findViewById(R.id.tx_welcome_sn);

        // Set button click listeners
        Button voipButton = findViewById(R.id.btn_login_voip);
        voipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!checkWxAppInfo()) return;
                startVoipActivity();
            }
        });

        Button wxSettingButton = findViewById(R.id.btn_wx_setting);
        wxSettingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WxSettingDialog dialog = new WxSettingDialog(VoipLoginActivity.this);
                dialog.show();
                dialog.setOnDismisListener(new WxSettingDialog.OnDismisListener() {
                    @Override
                    public void onDismised() {
                        if (mWelcomeSnTv != null) {
                            mWelcomeSnTv.setText(String.format("Welcome: %s", VoipSetting.getInstance(VoipLoginActivity.this).sn));
                        }
                    }
                });
            }
        });



        Button qualitySettingButton = findViewById(R.id.btn_quality_setting);
        qualitySettingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                QualitySettingDialog dialog = new QualitySettingDialog(VoipLoginActivity.this);
                dialog.show();
//                dialog.setOnDismisListener(new WxSettingDialog.OnDismisListener() {
//                    @Override
//                    public void onDismised() {
//                        if (mWelcomeSnTv != null) {
//                            mWelcomeSnTv.setText(String.format("Welcome: %s", VoipSetting.getInstance(VoipLoginActivity.this).sn));
//                        }
//                    }
//                });
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mWelcomeSnTv != null) {
            mWelcomeSnTv.setText(String.format("Welcome: %s", VoipSetting.getInstance(this).sn));
        }
    }

    private boolean checkWxAppInfo() {
        String modelId = VoipSetting.getInstance(this).modelId;
        String sn = VoipSetting.getInstance(this).sn;
        String snTicket = VoipSetting.getInstance(this).snTicket;
        String appId = VoipSetting.getInstance(this).appId;
        if (modelId.isEmpty() || sn.isEmpty() || snTicket.isEmpty() || appId.isEmpty()) {
            Toast.makeText(this, "请输入小程序信息！", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    private void startVoipActivity() {
        Intent intent = new Intent(this, VoipActivity.class);
        intent.putExtra("voip_model_id", VoipSetting.getInstance(this).modelId);
        intent.putExtra("voip_device_id", VoipSetting.getInstance(this).sn);
        intent.putExtra("voip_wxa_appid", VoipSetting.getInstance(this).appId);
        intent.putExtra("voip_sn_ticket", VoipSetting.getInstance(this).snTicket);
        intent.putExtra("productId", mProductId);
        intent.putExtra("deviceName", mDeviceName);
        intent.putExtra("deviceKey", mDeviceKey);
        intent.putExtra("miniprogramVersion", 0);
        startActivity(intent);
    }
}