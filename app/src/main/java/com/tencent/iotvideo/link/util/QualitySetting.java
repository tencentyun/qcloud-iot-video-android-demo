package com.tencent.iotvideo.link.util;

import android.content.Context;
import android.content.SharedPreferences;

public class QualitySetting ***REMOVED***
    private static String TAG = QualitySetting.class.getSimpleName();
    private static final String PREFERENCES_NAME = "QualitySetting";
    private Context context;

    private static QualitySetting instance;

    private int width = 640;
    private int height = 360;
    private int frameRate = 15;
    private int bitRate = 800;
    private int wxResolution = 0;  //***REMOVED***"可变自适应" : 0, "240x320": 1, "320x240": 2, "480x352" : 3, "480x640" : 4};
    private boolean wxCameraOn = true;

    private QualitySetting(Context context) ***REMOVED***
        this.context = context.getApplicationContext();
  ***REMOVED***

    public static synchronized QualitySetting getInstance(Context context) ***REMOVED***
        if (instance == null) ***REMOVED***
            instance = new QualitySetting(context);
            instance.loadData();
      ***REMOVED***
        return instance;
  ***REMOVED***

    public void saveData() ***REMOVED***
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("width", this.width);
        editor.putInt("height", this.height);
        editor.putInt("frameRate", this.frameRate);
        editor.putInt("bitRate", this.bitRate);
        editor.putInt("wxResolution", this.wxResolution);
        editor.putBoolean("wxCameraOn", this.wxCameraOn);
        editor.apply();
  ***REMOVED***

    public void loadData() ***REMOVED***
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        this.width = preferences.getInt("width", 0);
        this.height = preferences.getInt("height", 0);
        this.frameRate = preferences.getInt("frameRate", 0);
        this.bitRate = preferences.getInt("bitRate", 0);
        this.wxResolution = preferences.getInt("wxResolution", 0);
        this.wxCameraOn = preferences.getBoolean("wxCameraOn", true);
  ***REMOVED***

    public int getWidth() ***REMOVED***
        return width;
  ***REMOVED***

    public int getHeight() ***REMOVED***
        return height;
  ***REMOVED***

    public int getFrameRate() ***REMOVED***
        return frameRate;
  ***REMOVED***

    public int getBitRate() ***REMOVED***
        return bitRate;
  ***REMOVED***

    public int getWxResolution() ***REMOVED***
        return wxResolution;
  ***REMOVED***

    public boolean isWxCameraOn() ***REMOVED***
        return wxCameraOn;
  ***REMOVED***

    public void setWidth(int width) ***REMOVED***
        this.width = width;
  ***REMOVED***

    public void setHeight(int height) ***REMOVED***
        this.height = height;
  ***REMOVED***

    public void setFrameRate(int frameRate) ***REMOVED***
        this.frameRate = frameRate;
  ***REMOVED***

    public void setBitRate(int bitRate) ***REMOVED***
        this.bitRate = bitRate;
  ***REMOVED***

    public void setWxResolution(int wxResolution) ***REMOVED***
        this.wxResolution = wxResolution;
  ***REMOVED***

    public void setWxCameraOn(boolean wxCameraOn) ***REMOVED***
        this.wxCameraOn = wxCameraOn;
  ***REMOVED***

}
