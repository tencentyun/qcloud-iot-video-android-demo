package com.tencent.iotvideo.link.entity;

import androidx.annotation.NonNull;

public class ResolutionEntity ***REMOVED***
    public static String TAG = ResolutionEntity.class.getSimpleName();
    /**
     * 分辨率宽
     */
    private int width;
    /**
     * 分辨率高
     */
    private int height;
    /**
     * 分辨率简称 如 360p, 540p, 720p, 1080p
     */
    private String simpleName = "";

    public ResolutionEntity()***REMOVED***

  ***REMOVED***

    public ResolutionEntity(int width, int height) ***REMOVED***
        this.width = width;
        this.height = height;
  ***REMOVED***

    public ResolutionEntity(int width, int height, String simpleName) ***REMOVED***
        this.width = width;
        this.height = height;
        this.simpleName = simpleName;
  ***REMOVED***

    public int getWidth() ***REMOVED***
        return width;
  ***REMOVED***

    public void setWidth(int width) ***REMOVED***
        this.width = width;
  ***REMOVED***

    public int getHeight() ***REMOVED***
        return height;
  ***REMOVED***

    public void setHeight(int height) ***REMOVED***
        this.height = height;
  ***REMOVED***

    public String getSimpleName() ***REMOVED***
        return simpleName;
  ***REMOVED***

    public void setSimpleName(String simpleName) ***REMOVED***
        this.simpleName = simpleName;
  ***REMOVED***

    @NonNull
    @Override
    public String toString() ***REMOVED***
        return simpleName;
  ***REMOVED***
}
