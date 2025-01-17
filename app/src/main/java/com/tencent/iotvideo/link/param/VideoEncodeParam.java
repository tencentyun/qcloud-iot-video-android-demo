package com.tencent.iotvideo.link.param;

import android.media.MediaCodecInfo;
import android.media.MediaFormat;

/**
 * 视频编码参数
 */
public class VideoEncodeParam {

    private int width = -1; //录制宽度
    private int height = -1; //录制高度
    private int frameRate = 15; // 帧率
    private int iFrameInterval = 2; // I帧间隔: 默认一秒一个 I 帧
    private int bitRate = 125000; //码率
    private String mime = MediaFormat.MIMETYPE_VIDEO_AVC; // 编码格式: 默认 H264

    private int encodeType = 0;
    private MediaCodecInfo codecInfo;

//    private VideoEncodeParam() { }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getFrameRate() {
        return frameRate;
    }

    public void setFrameRate(int frameRate) {
        this.frameRate = frameRate;
    }

    public int getiFrameInterval() {
        return iFrameInterval;
    }

    public void setiFrameInterval(int iFrameInterval) {
        this.iFrameInterval = iFrameInterval;
    }

    public String getMime() {
        return mime;
    }

    public void setMime(String mime) {
        this.mime = mime;
    }

    public int getBitRate() {
        return bitRate;
    }

    public void setBitRate(int bitRate) {
        this.bitRate = bitRate;
    }

    public int getEncodeType() {
        return encodeType;
    }

    public void setEncodeType(int encodeType) {
        this.encodeType = encodeType;
    }

    public MediaCodecInfo getCodecInfo() {
        return codecInfo;
    }

    public void setCodecInfo(MediaCodecInfo codecInfo) {
        this.codecInfo = codecInfo;
    }

//    public static class Builder {
//        private VideoEncodeParam videoEncodeParam;
//
//        public Builder() {
//            videoEncodeParam = new VideoEncodeParam();
//        }
//
//        public Builder setSize(int width, int height) {
//            videoEncodeParam.setWidth(width);
//            videoEncodeParam.setHeight(height);
//            return this;
//        }
//
//        public Builder setFrameRate(int frameRate) {
//            videoEncodeParam.setFrameRate(frameRate);
//            return this;
//        }
//
//        public Builder setIFrameInterval(int iFrameInterval) {
//            videoEncodeParam.setiFrameInterval(iFrameInterval);
//            return this;
//        }
//
//        public Builder setMime(String mime) {
//            videoEncodeParam.setMime(mime);
//            return this;
//        }
//
//        public Builder setBitRate(int bitRate) {
//            videoEncodeParam.setBitRate(bitRate);
//            return this;
//        }
//
//        public VideoEncodeParam build() {
//            return videoEncodeParam;
//        }
//    }
}
