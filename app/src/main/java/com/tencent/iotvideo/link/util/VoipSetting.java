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

public class VoipSetting ***REMOVED***
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

    private VoipSetting(Context context) ***REMOVED***
        this.context = context.getApplicationContext();
  ***REMOVED***

    public static synchronized VoipSetting getInstance(Context context) ***REMOVED***
        if (instance == null) ***REMOVED***
            instance = new VoipSetting(context);
            instance.loadValueToMemory();
      ***REMOVED***
        return instance;
  ***REMOVED***

    public void saveData(String json) ***REMOVED***
        if (TextUtils.isEmpty(json)) ***REMOVED***
            Log.e(TAG, "saveData :" + json + ", is empty");
            return;
      ***REMOVED***
        if (!isJSONString(json)) ***REMOVED***
            Log.e(TAG, "saveData :" + json + ", is not json string");
            return;
      ***REMOVED***

        if (hasFilePermission()) ***REMOVED***
            saveToFile(json);
      ***REMOVED***

        saveToSharedPreferences(json);
  ***REMOVED***

    public String loadData() ***REMOVED***
        String json = null;

        if (hasFilePermission()) ***REMOVED***
            json = loadFromFile();
            if (!TextUtils.isEmpty(json)) ***REMOVED***
                if (!isJSONString(json)) ***REMOVED***
                    Log.e(TAG, "loadData :" + json + ", is not json string");
              ***REMOVED*** else ***REMOVED***
                    saveToSharedPreferences(json);
              ***REMOVED***
          ***REMOVED***
      ***REMOVED***

        if (TextUtils.isEmpty(json)) ***REMOVED***
            json = loadFromSharedPreferences();
      ***REMOVED***

        return json;
  ***REMOVED***

    public void updateValueForKey(String key, String value) ***REMOVED***
        if (TextUtils.isEmpty(value)) ***REMOVED***
            Log.e(TAG, "updateValueForKey key: " + key + ", value: " + value + " value is empty!");
            return;
      ***REMOVED***
        String json = loadData();

        try ***REMOVED***
            JSONObject jsonObject;
            if (TextUtils.isEmpty(json)) ***REMOVED***
                jsonObject = new JSONObject();
          ***REMOVED*** else ***REMOVED***
                jsonObject = new JSONObject(json);
          ***REMOVED***
            jsonObject.put(key, value);
            saveData(jsonObject.toString());
      ***REMOVED*** catch (JSONException e) ***REMOVED***
            e.printStackTrace();
      ***REMOVED***
  ***REMOVED***

    private boolean hasFilePermission() ***REMOVED***
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
  ***REMOVED***

    private void saveToFile(String json) ***REMOVED***
        File file = new File(Environment.getExternalStorageDirectory(), JSON_FILE_NAME);
        try ***REMOVED***
            FileWriter writer = new FileWriter(file);
            writer.write(json);
            writer.flush();
            writer.close();
      ***REMOVED*** catch (IOException e) ***REMOVED***
            e.printStackTrace();
      ***REMOVED***
  ***REMOVED***

    public void loadValueToMemory() ***REMOVED***
        String json = loadData();
        if (TextUtils.isEmpty(json)) ***REMOVED***
            json = readVoipSettingJsonStringFromAssets();
      ***REMOVED*** else ***REMOVED***
            if (isJSONString(json)) ***REMOVED***
                Log.e(TAG, "loadValueToMemory :" + json + ", is not json string");
          ***REMOVED***
      ***REMOVED***

        try ***REMOVED***
            JSONObject jsonObject = new JSONObject(json);
            if (jsonObject.has("productId")) ***REMOVED***
                this.productId = jsonObject.optString("productId");
          ***REMOVED***
            if (jsonObject.has("deviceName")) ***REMOVED***
                this.deviceName = jsonObject.optString("deviceName");
          ***REMOVED***
            if (jsonObject.has("deviceKey")) ***REMOVED***
                this.deviceKey = jsonObject.optString("deviceKey");
          ***REMOVED***
            if (jsonObject.has("voip_model_id")) ***REMOVED***
                this.modelId = jsonObject.optString("voip_model_id");
          ***REMOVED***
            if (jsonObject.has("voip_sn")) ***REMOVED***
                this.sn = jsonObject.optString("voip_sn");
          ***REMOVED***
            if (jsonObject.has("voip_sn_ticket")) ***REMOVED***
                this.snTicket = jsonObject.optString("voip_sn_ticket");
          ***REMOVED***
            if (jsonObject.has("voip_wxa_appid")) ***REMOVED***
                this.appId = jsonObject.optString("voip_wxa_appid");
          ***REMOVED***
            if (jsonObject.has("voip_openid1")) ***REMOVED***
                this.openId1 = jsonObject.optString("voip_openid1");
          ***REMOVED***
            if (jsonObject.has("voip_openid2")) ***REMOVED***
                this.openId2 = jsonObject.optString("voip_openid2");
          ***REMOVED***
            if (jsonObject.has("voip_openid3")) ***REMOVED***
                this.openId3 = jsonObject.optString("voip_openid3");
          ***REMOVED***
      ***REMOVED*** catch (JSONException e) ***REMOVED***
            e.printStackTrace();
      ***REMOVED***

  ***REMOVED***

    private String loadFromFile() ***REMOVED***
        File file = new File(Environment.getExternalStorageDirectory(), JSON_FILE_NAME);
        StringBuilder stringBuilder = new StringBuilder();

        if (file.exists()) ***REMOVED***
            try ***REMOVED***
                BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
                String line;
                while ((line = bufferedReader.readLine()) != null) ***REMOVED***
                    stringBuilder.append(line);
              ***REMOVED***
                bufferedReader.close();
          ***REMOVED*** catch (IOException e) ***REMOVED***
                e.printStackTrace();
          ***REMOVED***
      ***REMOVED***

        return stringBuilder.toString();
  ***REMOVED***

    private void saveToSharedPreferences(String json) ***REMOVED***
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(JSON_FILE_NAME, json);
        editor.apply();
  ***REMOVED***

    private String loadFromSharedPreferences() ***REMOVED***
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        return preferences.getString(JSON_FILE_NAME, null);
  ***REMOVED***

    public void setProductId(String productId) ***REMOVED***
        if (!TextUtils.isEmpty(productId)) ***REMOVED***
            this.productId = productId;
            updateValueForKey("productId", productId);
      ***REMOVED***
  ***REMOVED***

    public void setDeviceName(String deviceName) ***REMOVED***
        if (!TextUtils.isEmpty(deviceName)) ***REMOVED***
            this.deviceName = deviceName;
            updateValueForKey("deviceName", deviceName);
      ***REMOVED***
  ***REMOVED***

    public void setDeviceKey(String deviceKey) ***REMOVED***
        if (!TextUtils.isEmpty(deviceKey)) ***REMOVED***
            this.deviceKey = deviceKey;
            updateValueForKey("deviceKey", deviceKey);
      ***REMOVED***
  ***REMOVED***

    public void setModelId(String modelId) ***REMOVED***
        if (!TextUtils.isEmpty(modelId)) ***REMOVED***
            this.modelId = modelId;
            updateValueForKey("voip_model_id", modelId);
      ***REMOVED***
  ***REMOVED***

    public void setSn(String sn) ***REMOVED***
        if (!TextUtils.isEmpty(sn)) ***REMOVED***
            this.sn = sn;
            updateValueForKey("voip_sn", sn);
      ***REMOVED***
  ***REMOVED***

    public void setSnTicket(String snTicket) ***REMOVED***
        if (!TextUtils.isEmpty(snTicket)) ***REMOVED***
            this.snTicket = snTicket;
            updateValueForKey("voip_sn_ticket", snTicket);
      ***REMOVED***
  ***REMOVED***

    public void setAppId(String appId) ***REMOVED***
        if (!TextUtils.isEmpty(appId)) ***REMOVED***
            this.appId = appId;
            updateValueForKey("voip_wxa_appid", appId);
      ***REMOVED***
  ***REMOVED***

    public void setOpenId1(String openId1) ***REMOVED***
        if (!TextUtils.isEmpty(openId1)) ***REMOVED***
            this.openId1 = openId1;
            updateValueForKey("voip_openid1", openId1);
      ***REMOVED***
  ***REMOVED***

    public void setOpenId2(String openId2) ***REMOVED***
        if (!TextUtils.isEmpty(openId2)) ***REMOVED***
            this.openId2 = openId2;
            updateValueForKey("voip_openid2", openId2);
      ***REMOVED***
  ***REMOVED***

    public void setOpenId3(String openId3) ***REMOVED***
        if (!TextUtils.isEmpty(openId3)) ***REMOVED***
            this.openId3 = openId3;
            updateValueForKey("voip_openid3", openId3);
      ***REMOVED***
  ***REMOVED***

    private String readVoipSettingJsonStringFromAssets() ***REMOVED***
        AssetManager assetManager = context.getAssets();
        StringBuilder stringBuilder = new StringBuilder();

        try ***REMOVED***
            InputStream inputStream = assetManager.open(JSON_FILE_NAME);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = bufferedReader.readLine()) != null) ***REMOVED***
                stringBuilder.append(line);
          ***REMOVED***
            bufferedReader.close();
      ***REMOVED*** catch (IOException e) ***REMOVED***
            e.printStackTrace();
      ***REMOVED***
        return stringBuilder.toString();
  ***REMOVED***

    public static boolean isJSONString(String jsonString) ***REMOVED***
        if (!TextUtils.isEmpty(jsonString)) ***REMOVED***
            try ***REMOVED***
                new JSONObject(jsonString);
                return true;
          ***REMOVED*** catch (JSONException e) ***REMOVED***
                return false;
          ***REMOVED***
      ***REMOVED*** else ***REMOVED***
            return false;
      ***REMOVED***
  ***REMOVED***

}
