package com.tencent.iotvideo.link.param;

import android.media.MediaFormat;

/**
 * 音频编码参数
 */
public class AudioEncodeParam ***REMOVED***

    private int bitRate = 96000; // 比特率
    private int maxInputSize = 1024 * 1024; // 最大输入数据大小
    private String mime = MediaFormat.MIMETYPE_AUDIO_AAC; // 编码格式: 默认 AAC

//    private AudioEncodeParam() ***REMOVED*** }

    public int getBitRate() ***REMOVED***
        return bitRate;
  ***REMOVED***

    public void setBitRate(int bitRate) ***REMOVED***
        this.bitRate = bitRate;
  ***REMOVED***

    public int getMaxInputSize() ***REMOVED***
        return maxInputSize;
  ***REMOVED***

    public void setMaxInputSize(int maxInputSize) ***REMOVED***
        this.maxInputSize = maxInputSize;
  ***REMOVED***

    public String getMime() ***REMOVED***
        return mime;
  ***REMOVED***

    public void setMime(String mime) ***REMOVED***
        this.mime = mime;
  ***REMOVED***

//    public static class Builder ***REMOVED***
//        private AudioEncodeParam audioEncodeParam;
//
//        public Builder() ***REMOVED***
//            audioEncodeParam = new AudioEncodeParam();
//      ***REMOVED***
//
//        public Builder setBitRate(int bitRate) ***REMOVED***
//            audioEncodeParam.setBitRate(bitRate);
//            return this;
//      ***REMOVED***
//
//        public Builder setMaxInputSize(int maxInputSize) ***REMOVED***
//            audioEncodeParam.setMaxInputSize(maxInputSize);
//            return this;
//      ***REMOVED***
//
//        public Builder setMime(String mime) ***REMOVED***
//            audioEncodeParam.setMime(mime);
//            return this;
//      ***REMOVED***
//
//        public AudioEncodeParam build() ***REMOVED***
//            return audioEncodeParam;
//      ***REMOVED***
//  ***REMOVED***

}
