package com.tencent.iotvideo.link.param;

import android.media.MediaFormat;

/**
 * 视频编码参数
 */
public class VideoEncodeParam ***REMOVED***

    private int width = -1; //录制宽度
    private int height = -1; //录制高度
    private int frameRate = 15; // 帧率
    private int iFrameInterval = 2; // I帧间隔: 默认一秒一个 I 帧
    private int bitRate = 125000; //码率
    private String mime = MediaFormat.MIMETYPE_VIDEO_AVC; // 编码格式: 默认 H264

//    private VideoEncodeParam() ***REMOVED*** }

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

    public int getFrameRate() ***REMOVED***
        return frameRate;
  ***REMOVED***

    public void setFrameRate(int frameRate) ***REMOVED***
        this.frameRate = frameRate;
  ***REMOVED***

    public int getiFrameInterval() ***REMOVED***
        return iFrameInterval;
  ***REMOVED***

    public void setiFrameInterval(int iFrameInterval) ***REMOVED***
        this.iFrameInterval = iFrameInterval;
  ***REMOVED***

    public String getMime() ***REMOVED***
        return mime;
  ***REMOVED***

    public void setMime(String mime) ***REMOVED***
        this.mime = mime;
  ***REMOVED***

    public int getBitRate() ***REMOVED***
        return bitRate;
  ***REMOVED***

    public void setBitRate(int bitRate) ***REMOVED***
        this.bitRate = bitRate;
  ***REMOVED***

//    public static class Builder ***REMOVED***
//        private VideoEncodeParam videoEncodeParam;
//
//        public Builder() ***REMOVED***
//            videoEncodeParam = new VideoEncodeParam();
//      ***REMOVED***
//
//        public Builder setSize(int width, int height) ***REMOVED***
//            videoEncodeParam.setWidth(width);
//            videoEncodeParam.setHeight(height);
//            return this;
//      ***REMOVED***
//
//        public Builder setFrameRate(int frameRate) ***REMOVED***
//            videoEncodeParam.setFrameRate(frameRate);
//            return this;
//      ***REMOVED***
//
//        public Builder setIFrameInterval(int iFrameInterval) ***REMOVED***
//            videoEncodeParam.setiFrameInterval(iFrameInterval);
//            return this;
//      ***REMOVED***
//
//        public Builder setMime(String mime) ***REMOVED***
//            videoEncodeParam.setMime(mime);
//            return this;
//      ***REMOVED***
//
//        public Builder setBitRate(int bitRate) ***REMOVED***
//            videoEncodeParam.setBitRate(bitRate);
//            return this;
//      ***REMOVED***
//
//        public VideoEncodeParam build() ***REMOVED***
//            return videoEncodeParam;
//      ***REMOVED***
//  ***REMOVED***
}
