package com.tencent.iotvideo.link.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.tencent.iot.video.device.annotations.PixelType;

public class QualitySetting {
    private static String TAG = QualitySetting.class.getSimpleName();
    private static final String PREFERENCES_NAME = "QualitySetting";
    private Context context;

    private static QualitySetting instance;

    private int width = 640;
    private int height = 360;
    private int frameRate = 15;
    private int bitRate = 800;
    @PixelType
    private int wxResolution = 0;  //{"可变自适应" : 0, "240x320": 1, "320x240": 2, "480x352" : 3, "480x640" : 4};
    private boolean wxCameraOn = true;

    private QualitySetting(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized QualitySetting getInstance(Context context) {
        if (instance == null) {
            instance = new QualitySetting(context);
            instance.loadData();
        }
        return instance;
    }

    public void saveData() {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("width", this.width);
        editor.putInt("height", this.height);
        editor.putInt("frameRate", this.frameRate);
        editor.putInt("bitRate", this.bitRate);
        editor.putInt("wxResolution", this.wxResolution);
        editor.putBoolean("wxCameraOn", this.wxCameraOn);
        editor.apply();
    }

    public void loadData() {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        this.width = preferences.getInt("width", 0);
        this.height = preferences.getInt("height", 0);
        this.frameRate = preferences.getInt("frameRate", 0);
        this.bitRate = preferences.getInt("bitRate", 0);
        this.wxResolution = preferences.getInt("wxResolution", 0);
        this.wxCameraOn = preferences.getBoolean("wxCameraOn", true);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getFrameRate() {
        return frameRate;
    }

    public int getBitRate() {
        return bitRate;
    }

    public int getWxResolution() {
        return wxResolution;
    }

    public boolean isWxCameraOn() {
        return wxCameraOn;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setFrameRate(int frameRate) {
        this.frameRate = frameRate;
    }

    public void setBitRate(int bitRate) {
        this.bitRate = bitRate;
    }

    public void setWxResolution(int wxResolution) {
        this.wxResolution = wxResolution;
    }

    public void setWxCameraOn(boolean wxCameraOn) {
        this.wxCameraOn = wxCameraOn;
    }

}
