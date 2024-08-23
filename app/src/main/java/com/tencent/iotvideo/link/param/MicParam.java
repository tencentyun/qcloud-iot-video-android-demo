package com.tencent.iotvideo.link.param;

/**
 * Mic 参数
 */
public class MicParam ***REMOVED***

    private int audioSource = -1; // 音频输入源
    private int sampleRateInHz = -1; // 采样率
    private int channelConfig = -1; // 声道格式
    private int audioFormat = -1; // 音频格式
    private int streamType = -1; // 音频流类型
    private int mode = -1; // 音频流形式

//    private MicParam() ***REMOVED*** }

    public int getAudioSource() ***REMOVED***
        return audioSource;
  ***REMOVED***

    public void setAudioSource(int audioSource) ***REMOVED***
        this.audioSource = audioSource;
  ***REMOVED***

    public int getSampleRateInHz() ***REMOVED***
        return sampleRateInHz;
  ***REMOVED***

    public void setSampleRateInHz(int sampleRateInHz) ***REMOVED***
        this.sampleRateInHz = sampleRateInHz;
  ***REMOVED***

    public int getChannelConfig() ***REMOVED***
        return channelConfig;
  ***REMOVED***

    public void setChannelConfig(int channelConfig) ***REMOVED***
        this.channelConfig = channelConfig;
  ***REMOVED***

    public int getAudioFormat() ***REMOVED***
        return audioFormat;
  ***REMOVED***

    public void setAudioFormat(int audioFormat) ***REMOVED***
        this.audioFormat = audioFormat;
  ***REMOVED***

    public int getStreamType() ***REMOVED***
        return streamType;
  ***REMOVED***

    public void setStreamType(int streamType) ***REMOVED***
        this.streamType = streamType;
  ***REMOVED***

    public int getMode() ***REMOVED***
        return mode;
  ***REMOVED***

    public void setMode(int mode) ***REMOVED***
        this.mode = mode;
  ***REMOVED***

//    public static class Builder ***REMOVED***
//        private MicParam micParam;
//
//        public Builder() ***REMOVED***
//            micParam = new MicParam();
//      ***REMOVED***
//
//        public Builder setAudioSource(int audioSource) ***REMOVED***
//            micParam.setAudioSource(audioSource);
//            return this;
//      ***REMOVED***
//
//        public Builder setSampleRateInHz(int sampleRateInHz) ***REMOVED***
//            micParam.setSampleRateInHz(sampleRateInHz);
//            return this;
//      ***REMOVED***
//
//        public Builder setChannelConfig(int channelConfig) ***REMOVED***
//            micParam.setChannelConfig(channelConfig);
//            return this;
//      ***REMOVED***
//
//        public Builder setAudioFormat(int audioFormat) ***REMOVED***
//            micParam.setAudioFormat(audioFormat);
//            return this;
//      ***REMOVED***
//
//        public Builder setStreamType(int streamType) ***REMOVED***
//            micParam.setStreamType(streamType);
//            return this;
//      ***REMOVED***
//
//        public Builder setMode(int mode) ***REMOVED***
//            micParam.setMode(mode);
//            return this;
//      ***REMOVED***
//
//        public MicParam build() ***REMOVED***
//            return micParam;
//      ***REMOVED***
//  ***REMOVED***

    public boolean isEmpty() ***REMOVED***
        return sampleRateInHz == -1
                || channelConfig == -1
                || audioFormat == -1;
  ***REMOVED***
}
