package com.tencent.iotvideo.link.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class VoipSetting {
    private static String TAG = VoipSetting.class.getSimpleName();
    private static final String PREFERENCES_NAME = "MyPreferences";
    private static final String JSON_FILE_NAME = "voip_setting.json";
    private Context context;

    private static VoipSetting instance;

    public String productId = "";
    public String deviceName = "";
    public String deviceKey = "";
    public String modelId = "";
    public String sn = "";
    public String snTicket = "";
    public String appId = "";
    public String openId1 = "";
    public String openId2 = "";
    public String openId3 = "";

    private VoipSetting(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized VoipSetting getInstance(Context context) {
        if (instance == null) {
            instance = new VoipSetting(context);
            instance.loadValueToMemory();
        }
        return instance;
    }

    public void saveData(String json) {
        if (TextUtils.isEmpty(json)) {
            Log.e(TAG, "saveData :" + json + ", is empty");
            return;
        }
        if (!isJSONString(json)) {
            Log.e(TAG, "saveData :" + json + ", is not json string");
            return;
        }

        if (hasFilePermission()) {
            saveToFile(json);
        }

        saveToSharedPreferences(json);
    }

    public String loadData() {
        String json = null;

        if (hasFilePermission()) {
            json = loadFromFile();
            if (!TextUtils.isEmpty(json)) {
                if (!isJSONString(json)) {
                    Log.e(TAG, "loadData :" + json + ", is not json string");
                } else {
                    saveToSharedPreferences(json);
                }
            }
        }

        if (TextUtils.isEmpty(json)) {
            json = loadFromSharedPreferences();
        }

        return json;
    }

    public void updateValueForKey(String key, String value) {
        if (TextUtils.isEmpty(value)) {
            Log.e(TAG, "updateValueForKey key: " + key + ", value: " + value + " value is empty!");
            return;
        }
        String json = loadData();

        try {
            JSONObject jsonObject;
            if (TextUtils.isEmpty(json)) {
                jsonObject = new JSONObject();
            } else {
                jsonObject = new JSONObject(json);
            }
            jsonObject.put(key, value);
            saveData(jsonObject.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private boolean hasFilePermission() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    private void saveToFile(String json) {
        File file = new File(Environment.getExternalStorageDirectory(), JSON_FILE_NAME);
        try {
            FileWriter writer = new FileWriter(file);
            writer.write(json);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadValueToMemory() {
        String json = loadData();
        if (TextUtils.isEmpty(json)) {
            json = readVoipSettingJsonStringFromAssets();
        } else {
            if (isJSONString(json)) {
                Log.e(TAG, "loadValueToMemory :" + json + ", is not json string");
            }
        }

        try {
            JSONObject jsonObject = new JSONObject(json);
            if (jsonObject.has("productId")) {
                this.productId = jsonObject.optString("productId");
            }
            if (jsonObject.has("deviceName")) {
                this.deviceName = jsonObject.optString("deviceName");
            }
            if (jsonObject.has("deviceKey")) {
                this.deviceKey = jsonObject.optString("deviceKey");
            }
            if (jsonObject.has("voip_model_id")) {
                this.modelId = jsonObject.optString("voip_model_id");
            }
            if (jsonObject.has("voip_sn")) {
                this.sn = jsonObject.optString("voip_sn");
            }
            if (jsonObject.has("voip_sn_ticket")) {
                this.snTicket = jsonObject.optString("voip_sn_ticket");
            }
            if (jsonObject.has("voip_wxa_appid")) {
                this.appId = jsonObject.optString("voip_wxa_appid");
            }
            if (jsonObject.has("voip_openid1")) {
                this.openId1 = jsonObject.optString("voip_openid1");
            }
            if (jsonObject.has("voip_openid2")) {
                this.openId2 = jsonObject.optString("voip_openid2");
            }
            if (jsonObject.has("voip_openid3")) {
                this.openId3 = jsonObject.optString("voip_openid3");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private String loadFromFile() {
        File file = new File(Environment.getExternalStorageDirectory(), JSON_FILE_NAME);
        StringBuilder stringBuilder = new StringBuilder();

        if (file.exists()) {
            try {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                bufferedReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return stringBuilder.toString();
    }

    private void saveToSharedPreferences(String json) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(JSON_FILE_NAME, json);
        editor.apply();
    }

    private String loadFromSharedPreferences() {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        return preferences.getString(JSON_FILE_NAME, null);
    }

    public void setProductId(String productId) {
        if (!TextUtils.isEmpty(productId)) {
            this.productId = productId;
            updateValueForKey("productId", productId);
        }
    }

    public void setDeviceName(String deviceName) {
        if (!TextUtils.isEmpty(deviceName)) {
            this.deviceName = deviceName;
            updateValueForKey("deviceName", deviceName);
        }
    }

    public void setDeviceKey(String deviceKey) {
        if (!TextUtils.isEmpty(deviceKey)) {
            this.deviceKey = deviceKey;
            updateValueForKey("deviceKey", deviceKey);
        }
    }

    public void setModelId(String modelId) {
        if (!TextUtils.isEmpty(modelId)) {
            this.modelId = modelId;
            updateValueForKey("voip_model_id", modelId);
        }
    }

    public void setSn(String sn) {
        if (!TextUtils.isEmpty(sn)) {
            this.sn = sn;
            updateValueForKey("voip_sn", sn);
        }
    }

    public void setSnTicket(String snTicket) {
        if (!TextUtils.isEmpty(snTicket)) {
            this.snTicket = snTicket;
            updateValueForKey("voip_sn_ticket", snTicket);
        }
    }

    public void setAppId(String appId) {
        if (!TextUtils.isEmpty(appId)) {
            this.appId = appId;
            updateValueForKey("voip_wxa_appid", appId);
        }
    }

    public void setOpenId1(String openId1) {
        if (!TextUtils.isEmpty(openId1)) {
            this.openId1 = openId1;
            updateValueForKey("voip_openid1", openId1);
        }
    }

    public void setOpenId2(String openId2) {
        if (!TextUtils.isEmpty(openId2)) {
            this.openId2 = openId2;
            updateValueForKey("voip_openid2", openId2);
        }
    }

    public void setOpenId3(String openId3) {
        if (!TextUtils.isEmpty(openId3)) {
            this.openId3 = openId3;
            updateValueForKey("voip_openid3", openId3);
        }
    }

    private String readVoipSettingJsonStringFromAssets() {
        AssetManager assetManager = context.getAssets();
        StringBuilder stringBuilder = new StringBuilder();

        try {
            InputStream inputStream = assetManager.open(JSON_FILE_NAME);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stringBuilder.toString();
    }

    public static boolean isJSONString(String jsonString) {
        if (!TextUtils.isEmpty(jsonString)) {
            try {
                new JSONObject(jsonString);
                return true;
            } catch (JSONException e) {
                return false;
            }
        } else {
            return false;
        }
    }

}
