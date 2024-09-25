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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.tencent.bugly.crashreport.CrashReport;
import com.tencent.iot.voipdemo.R;
import com.tencent.iotvideo.link.popup.DeviceSettingDialog;
import com.tencent.iotvideo.link.util.VoipSetting;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity ***REMOVED***
    private static final int PERMISSIONS_REQUEST_CODE = 1;
    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) ***REMOVED***
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        CrashReport.initCrashReport(getApplicationContext(), "e1c50561ac", false);

        // Get UI elements
        Button ipcButton = findViewById(R.id.btn_login_IPC);
        Button duplexButton = findViewById(R.id.btn_login_duplex_video);
        Button voipButton = findViewById(R.id.btn_login_voip);
        Button settingDeviceButton = findViewById(R.id.btn_setting_device);

        // Request permissions
        if (!hasPermissions()) ***REMOVED***
            ActivityCompat.requestPermissions(this, new String[]***REMOVED***
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
          ***REMOVED***, PERMISSIONS_REQUEST_CODE);
      ***REMOVED*** else ***REMOVED***
            checkFilesToastAfterPermissions();
            runOnUiThread(new Runnable() ***REMOVED***
                @Override
                public void run() ***REMOVED***
                    LogcatHelper.getInstance(MainActivity.this).start();
              ***REMOVED***
          ***REMOVED***);
      ***REMOVED***

        // Set button click listeners
        ipcButton.setOnClickListener(new View.OnClickListener() ***REMOVED***
            @Override
            public void onClick(View v) ***REMOVED***
                if (!checkDeviceInfo()) return;
                startIpcActivity();
          ***REMOVED***
      ***REMOVED***);
        duplexButton.setOnClickListener(new View.OnClickListener() ***REMOVED***
            @Override
            public void onClick(View v) ***REMOVED***
                if (!checkDeviceInfo()) return;
                startDuplexActivity();
          ***REMOVED***
      ***REMOVED***);
        voipButton.setOnClickListener(new View.OnClickListener() ***REMOVED***
            @Override
            public void onClick(View v) ***REMOVED***
                if (!checkDeviceInfo()) return;
                startVoipActivity();
          ***REMOVED***
      ***REMOVED***);
        settingDeviceButton.setOnClickListener(new View.OnClickListener() ***REMOVED***
            @Override
            public void onClick(View view) ***REMOVED***
                DeviceSettingDialog dialog = new DeviceSettingDialog(MainActivity.this);
                dialog.show();
          ***REMOVED***
      ***REMOVED***);
  ***REMOVED***

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) ***REMOVED***
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_CODE) ***REMOVED***
            if (hasPermissions()) ***REMOVED***
                checkFilesToastAfterPermissions();
                LogcatHelper.getInstance(this).start();
          ***REMOVED*** else ***REMOVED***
                Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show();
          ***REMOVED***
      ***REMOVED***
  ***REMOVED***

    private boolean hasPermissions() ***REMOVED***
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
  ***REMOVED***

    private void startIpcActivity() ***REMOVED***
        String productId = VoipSetting.getInstance(this).productId;
        String deviceName = VoipSetting.getInstance(this).deviceName;
        String deviceKey = VoipSetting.getInstance(this).deviceKey;
        Intent intent = new Intent(this, IPCActivity.class);
        intent.putExtra("productId", productId);
        intent.putExtra("deviceName", deviceName);
        intent.putExtra("deviceKey", deviceKey);
        startActivity(intent);
  ***REMOVED***

    private void startDuplexActivity() ***REMOVED***
        String productId = VoipSetting.getInstance(this).productId;
        String deviceName = VoipSetting.getInstance(this).deviceName;
        String deviceKey = VoipSetting.getInstance(this).deviceKey;
        Intent intent = new Intent(this, DuplexVideoActivity.class);
        intent.putExtra("productId", productId);
        intent.putExtra("deviceName", deviceName);
        intent.putExtra("deviceKey", deviceKey);
        startActivity(intent);
  ***REMOVED***

    private void startVoipActivity() ***REMOVED***
        String productId = VoipSetting.getInstance(this).productId;
        String deviceName = VoipSetting.getInstance(this).deviceName;
        String deviceKey = VoipSetting.getInstance(this).deviceKey;
        Intent intent = new Intent(this, VoipLoginActivity.class);
        intent.putExtra("productId", productId);
        intent.putExtra("deviceName", deviceName);
        intent.putExtra("deviceKey", deviceKey);
        startActivity(intent);
  ***REMOVED***

    private boolean checkDeviceInfo() ***REMOVED***
        String productId = VoipSetting.getInstance(this).productId;
        String deviceName = VoipSetting.getInstance(this).deviceName;
        String deviceKey = VoipSetting.getInstance(this).deviceKey;
        if (productId.isEmpty() || deviceName.isEmpty() || deviceKey.isEmpty()) ***REMOVED***
            Toast.makeText(MainActivity.this, "请输入设备信息！", Toast.LENGTH_LONG).show();
            return false;
      ***REMOVED***

        return true;
  ***REMOVED***

    private void checkFilesToastAfterPermissions() ***REMOVED***
        SharedPreferences preferences = getSharedPreferences("InstallConfig", Context.MODE_PRIVATE);
        boolean installFlag = preferences.getBoolean("installFlag", false);
        Log.d(TAG, "is first install or reinstall: " + installFlag);
        if (!installFlag) ***REMOVED***
            saveFileFromAssertToSDCard("device_key");
            saveFileFromAssertToSDCard("voip_setting.json");
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("installFlag", true);
            editor.apply();
      ***REMOVED*** else ***REMOVED***
            if (!isFileExists("device_key")) ***REMOVED***
                saveFileFromAssertToSDCard("device_key");
          ***REMOVED***
            if (!isFileExists("voip_setting.json")) ***REMOVED***
                saveFileFromAssertToSDCard("voip_setting.json");
          ***REMOVED***
      ***REMOVED***
        Toast.makeText(this, "voip_setting.json是否在sdcard下：" + isFileExists("voip_setting.json") + ", json是否合法：" + VoipSetting.isJSONString(VoipSetting.getInstance(this).loadData()), Toast.LENGTH_SHORT).show();

  ***REMOVED***

    private boolean isFileExists(String fileName) ***REMOVED***
        File file = new File(Environment.getExternalStorageDirectory(), fileName);
        if (file.exists()) ***REMOVED***
            Log.d(TAG, fileName + "File exists");
            return true;
      ***REMOVED*** else ***REMOVED***
            Log.d(TAG, fileName + "File does not exist");
            return false;
      ***REMOVED***
  ***REMOVED***

    private void saveFileFromAssertToSDCard(String fileName) ***REMOVED***
        AssetManager assetManager = getAssets();

        InputStream in = null;
        OutputStream out = null;

        try ***REMOVED***
            in = assetManager.open(fileName);
            File outFile = new File(Environment.getExternalStorageDirectory(), fileName);
            out = new FileOutputStream(outFile);
            copyFile(in, out);
      ***REMOVED*** catch (IOException e) ***REMOVED***
            Log.e(TAG, "Failed to copy asset file: " + fileName, e);
      ***REMOVED*** finally ***REMOVED***
            if (in != null) ***REMOVED***
                try ***REMOVED***
                    in.close();
              ***REMOVED*** catch (IOException e) ***REMOVED***
                    // NOOP
                    Log.e(TAG, "in.close Failed to copy asset file: " + fileName, e);
              ***REMOVED***
          ***REMOVED***
            if (out != null) ***REMOVED***
                try ***REMOVED***
                    out.close();
              ***REMOVED*** catch (IOException e) ***REMOVED***
                    // NOOP
                    Log.e(TAG, "out.close Failed to copy asset file: " + fileName, e);
              ***REMOVED***
          ***REMOVED***
      ***REMOVED***
  ***REMOVED***

    private void copyFile(InputStream in, OutputStream out) throws IOException ***REMOVED***
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) ***REMOVED***
            out.write(buffer, 0, read);
      ***REMOVED***
  ***REMOVED***
}