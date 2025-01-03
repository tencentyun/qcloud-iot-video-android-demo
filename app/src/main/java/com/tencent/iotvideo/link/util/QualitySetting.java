package com.tencent.iotvideo.link.util;

import static com.tencent.iotvideo.link.util.MediaCodeUtilsKt.getMediaCodecInfoByName;
import static com.tencent.iotvideo.link.util.MediaCodeUtilsKt.getSupportVideoEncoder;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaCodecInfo;
import android.util.Log;

import com.google.gson.Gson;
import com.tencent.iot.video.device.annotations.PixelType;
import com.tencent.iotvideo.link.entity.ResolutionEntity;

import org.json.JSONObject;

import java.util.List;

public class QualitySetting {
    private static String TAG = QualitySetting.class.getSimpleName();
    private static final String PREFERENCES_NAME = "QualitySetting";
    private Context context;

    private static QualitySetting instance;

    private int encodeType;//0 软编；1硬编

    private MediaCodecInfo mediaCodecInfo;

    private ResolutionEntity resolutionEntity;
    private int frameRate = 15;
    private int bitRate = 800;
    @PixelType
    private int wxResolution = 0;  //{"可变自适应" : 0, "240x320": 1, "320x240": 2, "480x352" : 3, "480x640" : 4};
    private boolean wxCameraOn = true;

    private Gson gson;

    private QualitySetting(Context context) {
        this.context = context.getApplicationContext();
        this.gson = new Gson();
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
        editor.putInt("encodeType", encodeType);
        editor.putString("resolutionEntity", gson.toJson(resolutionEntity));
        editor.putString("encoderName", mediaCodecInfo.getName());
        editor.putInt("frameRate", this.frameRate);
        editor.putInt("bitRate", this.bitRate);
        editor.putInt("wxResolution", this.wxResolution);
        editor.putBoolean("wxCameraOn", this.wxCameraOn);
        Log.d(TAG, "save data encodeType:" + encodeType + " resolutionEntity:" + gson.toJson(resolutionEntity) + " encoderName:" + mediaCodecInfo.getName() + " frameRate:" + frameRate + " bitRate:" + bitRate + " wxResolution:" + wxResolution + " wxCameraOn:" + wxCameraOn);
        editor.apply();
    }

    public void loadData() {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        this.encodeType = preferences.getInt("encodeType", 0);
        ResolutionEntity entity = gson.fromJson(preferences.getString("resolutionEntity", ""), ResolutionEntity.class);
        if (entity != null) {
            resolutionEntity = entity;
        } else {
            resolutionEntity = new ResolutionEntity(640, 480, "480p");
        }
        String encoderName = preferences.getString("encoderName", "");
        if (!encoderName.isEmpty()) {
            mediaCodecInfo = getMediaCodecInfoByName(encoderName);
        } else {
            List<MediaCodecInfo> list = MediaCodeUtilsKt.getSupportVideoEncoder(0);
            if (list.isEmpty()) {
                Log.d(TAG, "MediaCodecInfo list is empty");
                return;
            }
            mediaCodecInfo = list.get(0);
        }
        this.frameRate = preferences.getInt("frameRate", 15);
        this.bitRate = preferences.getInt("bitRate", 800);
        this.wxResolution = preferences.getInt("wxResolution", 0);
        this.wxCameraOn = preferences.getBoolean("wxCameraOn", true);
        Log.d(TAG, "load data encodeType:" + encodeType + " resolutionEntity:" + gson.toJson(resolutionEntity) + " mediaCodecInfo:" + mediaCodecInfo.getName() + " frameRate:" + frameRate + " bitRate:" + bitRate + " wxResolution:" + wxResolution + " wxCameraOn:" + wxCameraOn);
    }

    public int getEncodeType() {
        return encodeType;
    }

    public void setEncodeType(int encodeType) {
        this.encodeType = encodeType;
    }

    public MediaCodecInfo getMediaCodecInfo() {
        return mediaCodecInfo;
    }

    public void setMediaCodecInfo(MediaCodecInfo mediaCodecInfo) {
        this.mediaCodecInfo = mediaCodecInfo;
    }

    public ResolutionEntity getResolutionEntity() {
        return resolutionEntity;
    }

    public void setResolutionEntity(ResolutionEntity resolutionEntity) {
        this.resolutionEntity = resolutionEntity;
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
