package com.example.ivdemo;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.tencent.iot.voipdemo.R;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.tencent.bugly.crashreport.CrashReport;
import com.tencent.iotvideo.link.popup.DeviceSettingDialog;
import com.tencent.iotvideo.link.util.VoipSetting;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSIONS_REQUEST_CODE = 1;
    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mian);
        CrashReport.initCrashReport(getApplicationContext(), "e1c50561ac", false);

        // Get UI elements
        Button ipcButton = findViewById(R.id.btn_login_IPC);
        Button duplexButton = findViewById(R.id.btn_login_duplex_video);
        Button voipButton = findViewById(R.id.btn_login_voip);
        Button callAnswerButton = findViewById(R.id.btn_call_answer);
        Button settingDeviceButton = findViewById(R.id.btn_setting_device);

        // Request permissions
        if (!hasPermissions()) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, PERMISSIONS_REQUEST_CODE);
        } else {
            checkFilesToastAfterPermissions();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    LogcatHelper.getInstance(MainActivity.this).start();
                }
            });
        }

        // Set button click listeners
        ipcButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!checkDeviceInfo()) return;
                startIpcActivity();
            }
        });
        duplexButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!checkDeviceInfo()) return;
                startDuplexActivity();
            }
        });
        voipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!checkDeviceInfo()) return;
                startVoipActivity();
            }
        });
        callAnswerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!checkDeviceInfo()) return;
                startCallAnswerVideoActivity();
            }
        });
        settingDeviceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DeviceSettingDialog dialog = new DeviceSettingDialog(MainActivity.this);
                dialog.show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (hasPermissions()) {
                checkFilesToastAfterPermissions();
                LogcatHelper.getInstance(this).start();
            } else {
                Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean hasPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void startIpcActivity() {
        String productId = VoipSetting.getInstance(this).productId;
        String deviceName = VoipSetting.getInstance(this).deviceName;
        String deviceKey = VoipSetting.getInstance(this).deviceKey;
        Intent intent = new Intent(this, IPCActivity.class);
        intent.putExtra("productId", productId);
        intent.putExtra("deviceName", deviceName);
        intent.putExtra("deviceKey", deviceKey);
        startActivity(intent);
    }

    private void startDuplexActivity() {
        String productId = VoipSetting.getInstance(this).productId;
        String deviceName = VoipSetting.getInstance(this).deviceName;
        String deviceKey = VoipSetting.getInstance(this).deviceKey;
        Intent intent = new Intent(this, DuplexVideoActivity.class);
        intent.putExtra("productId", productId);
        intent.putExtra("deviceName", deviceName);
        intent.putExtra("deviceKey", deviceKey);
        startActivity(intent);
    }

    private void startVoipActivity() {
        String productId = VoipSetting.getInstance(this).productId;
        String deviceName = VoipSetting.getInstance(this).deviceName;
        String deviceKey = VoipSetting.getInstance(this).deviceKey;
        Intent intent = new Intent(this, VoipLoginActivity.class);
        intent.putExtra("productId", productId);
        intent.putExtra("deviceName", deviceName);
        intent.putExtra("deviceKey", deviceKey);
        startActivity(intent);
    }

    private void startCallAnswerVideoActivity() {
        String productId = VoipSetting.getInstance(this).productId;
        String deviceName = VoipSetting.getInstance(this).deviceName;
        String deviceKey = VoipSetting.getInstance(this).deviceKey;
        Intent intent = new Intent(this, CallAnswerVideoActivity.class);
        intent.putExtra("productId", productId);
        intent.putExtra("deviceName", deviceName);
        intent.putExtra("deviceKey", deviceKey);
        startActivity(intent);
    }

    private boolean checkDeviceInfo() {
        String productId = VoipSetting.getInstance(this).productId;
        String deviceName = VoipSetting.getInstance(this).deviceName;
        String deviceKey = VoipSetting.getInstance(this).deviceKey;
        if (productId.isEmpty() || deviceName.isEmpty() || deviceKey.isEmpty()) {
            Toast.makeText(MainActivity.this, "请输入设备信息！", Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }

    private void checkFilesToastAfterPermissions() {
        SharedPreferences preferences = getSharedPreferences("InstallConfig", Context.MODE_PRIVATE);
        boolean installFlag = preferences.getBoolean("installFlag", false);
        Log.d(TAG, "is first install or reinstall: " + installFlag);
        if (!installFlag) {
            saveFileFromAssertToSDCard("device_key");
            saveFileFromAssertToSDCard("voip_setting.json");
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("installFlag", true);
            editor.apply();
        } else {
            if (!isFileExists("device_key")) {
                saveFileFromAssertToSDCard("device_key");
            }
            if (!isFileExists("voip_setting.json")) {
                saveFileFromAssertToSDCard("voip_setting.json");
            }
        }
        Toast.makeText(this, "voip_setting.json是否在sdcard下：" + isFileExists("voip_setting.json") + ", json是否合法：" + VoipSetting.isJSONString(VoipSetting.getInstance(this).loadData()), Toast.LENGTH_SHORT).show();

    }

    private boolean isFileExists(String fileName) {
        File file = new File(Environment.getExternalStorageDirectory(), fileName);
        if (file.exists()) {
            Log.d(TAG, fileName + "File exists");
            return true;
        } else {
            Log.d(TAG, fileName + "File does not exist");
            return false;
        }
    }

    private void saveFileFromAssertToSDCard(String fileName) {
        AssetManager assetManager = getAssets();

        InputStream in = null;
        OutputStream out = null;

        try {
            in = assetManager.open(fileName);
            File outFile = new File(Environment.getExternalStorageDirectory(), fileName);
            out = new FileOutputStream(outFile);
            copyFile(in, out);
        } catch (IOException e) {
            Log.e(TAG, "Failed to copy asset file: " + fileName, e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // NOOP
                    Log.e(TAG, "in.close Failed to copy asset file: " + fileName, e);
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // NOOP
                    Log.e(TAG, "out.close Failed to copy asset file: " + fileName, e);
                }
            }
        }
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }
}