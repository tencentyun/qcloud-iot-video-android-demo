package com.tencent.iotvideo.link.encoder;

import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.os.Build;
import android.util.Log;

import com.tencent.iotvideo.link.listener.OnEncodeListener;
import com.tencent.iotvideo.link.param.AudioEncodeParam;
import com.tencent.iotvideo.link.param.MicParam;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class AudioEncoder ***REMOVED***

    /**
     * 采样频率对照表
     */
    private static final Map<Integer, Integer> samplingFrequencyIndexMap = new HashMap<>();

    static ***REMOVED***
        samplingFrequencyIndexMap.put(96000, 0);
        samplingFrequencyIndexMap.put(88200, 1);
        samplingFrequencyIndexMap.put(64000, 2);
        samplingFrequencyIndexMap.put(48000, 3);
        samplingFrequencyIndexMap.put(44100, 4);
        samplingFrequencyIndexMap.put(32000, 5);
        samplingFrequencyIndexMap.put(24000, 6);
        samplingFrequencyIndexMap.put(22050, 7);
        samplingFrequencyIndexMap.put(16000, 8);
        samplingFrequencyIndexMap.put(12000, 9);
        samplingFrequencyIndexMap.put(11025, 10);
        samplingFrequencyIndexMap.put(8000, 11);
  ***REMOVED***

    private final String TAG = AudioEncoder.class.getSimpleName();
    private MediaCodec audioCodec;
    private AudioRecord audioRecord;
    private AcousticEchoCanceler canceler;
    private AutomaticGainControl control;

    private final MicParam micParam;
    private final AudioEncodeParam audioEncodeParam;
    private OnEncodeListener encodeListener;

    private volatile boolean stopEncode = false;
    private long seq = 0L;
    private int bufferSizeInBytes;


    public AudioEncoder(MicParam micParam, AudioEncodeParam audioEncodeParam) ***REMOVED***
        this(micParam, audioEncodeParam, false, false);
  ***REMOVED***


    public AudioEncoder(MicParam micParam, AudioEncodeParam audioEncodeParam, boolean enableAEC, boolean enableAGC) ***REMOVED***
        this.micParam = micParam;
        this.audioEncodeParam = audioEncodeParam;
        initAudio();
        int audioSessionId = audioRecord.getAudioSessionId();
        if (enableAEC && audioSessionId != 0) ***REMOVED***
            Log.e(TAG, "=====initAEC result: " + initAEC(audioSessionId));
      ***REMOVED***
        if (enableAGC && audioSessionId != 0) ***REMOVED***
            Log.e(TAG, "=====initAGC result: " + initAGC(audioSessionId));
      ***REMOVED***
  ***REMOVED***

    public void setOnEncodeListener(OnEncodeListener listener) ***REMOVED***
        this.encodeListener = listener;
  ***REMOVED***

    private void initAudio() ***REMOVED***
        bufferSizeInBytes = 2 * AudioRecord.getMinBufferSize(micParam.getSampleRateInHz(), micParam.getChannelConfig(), micParam.getAudioFormat());
        Log.d(TAG, "=====bufferSizeInBytes: " + bufferSizeInBytes);
        audioRecord = new AudioRecord(micParam.getAudioSource(), micParam.getSampleRateInHz(), micParam.getChannelConfig(), micParam.getAudioFormat(), bufferSizeInBytes);
        try ***REMOVED***
            audioCodec = MediaCodec.createEncoderByType(audioEncodeParam.getMime());
            MediaFormat format = MediaFormat.createAudioFormat(audioEncodeParam.getMime(), micParam.getSampleRateInHz(), 1);
            format.setInteger(MediaFormat.KEY_BIT_RATE, audioEncodeParam.getBitRate());
            format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, audioEncodeParam.getMaxInputSize());
            audioCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
      ***REMOVED*** catch (IOException e) ***REMOVED***
            e.printStackTrace();
            audioRecord = null;
            audioCodec = null;
      ***REMOVED***
  ***REMOVED***

    public void start() ***REMOVED***
        new Thread(this::record).start();
  ***REMOVED***

    public void stop() ***REMOVED***
        stopEncode = true;
  ***REMOVED***

    public boolean isDevicesSupportAEC() ***REMOVED***
        return AcousticEchoCanceler.isAvailable();
  ***REMOVED***

    private boolean initAEC(int audioSession) ***REMOVED***

        boolean isDevicesSupportAEC = isDevicesSupportAEC();
        Log.e(TAG, "isDevicesSupportAEC: " + isDevicesSupportAEC);
        if (!isDevicesSupportAEC) ***REMOVED***
            return false;
      ***REMOVED***
        if (canceler != null) ***REMOVED***
            return false;
      ***REMOVED***
        canceler = AcousticEchoCanceler.create(audioSession);
        canceler.setEnabled(true);
        return canceler.getEnabled();
  ***REMOVED***

    public boolean isDevicesSupportAGC() ***REMOVED***
        return AutomaticGainControl.isAvailable();
  ***REMOVED***

    private boolean initAGC(int audioSession) ***REMOVED***

        boolean isDevicesSupportAGC = isDevicesSupportAGC();
        Log.e(TAG, "isDevicesSupportAGC: " + isDevicesSupportAGC);
        if (!isDevicesSupportAGC) ***REMOVED***
            return false;
      ***REMOVED***
        if (control != null) ***REMOVED***
            return false;
      ***REMOVED***
        control = AutomaticGainControl.create(audioSession);
        control.setEnabled(true);
        return control.getEnabled();
  ***REMOVED***

    private void release() ***REMOVED***
        if (audioRecord != null) ***REMOVED***
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
      ***REMOVED***

        if (audioCodec != null) ***REMOVED***
            audioCodec.stop();
            audioCodec.release();
            audioCodec = null;
      ***REMOVED***

        if (canceler != null) ***REMOVED***
            canceler.setEnabled(false);
            canceler.release();
            canceler = null;
      ***REMOVED***

        if (control != null) ***REMOVED***
            control.setEnabled(false);
            control.release();
            control = null;
      ***REMOVED***
  ***REMOVED***

    private void addADTStoPacket(ByteBuffer outputBuffer) ***REMOVED***
        byte[] bytes = new byte[outputBuffer.remaining()];
        outputBuffer.get(bytes, 0, bytes.length);
        byte[] dataBytes = new byte[bytes.length + 7];
        System.arraycopy(bytes, 0, dataBytes, 7, bytes.length);
        addADTStoPacket(dataBytes, dataBytes.length);
        if (stopEncode) ***REMOVED***
            return;
      ***REMOVED***
        if (encodeListener != null) ***REMOVED***
            encodeListener.onAudioEncoded(dataBytes, System.currentTimeMillis(), seq);
            seq++;
      ***REMOVED*** else ***REMOVED***
            Log.e(TAG, "Encode listener is null, please set encode listener.");
      ***REMOVED***
  ***REMOVED***

    private void addADTStoPacket(byte[] packet, int packetLen) ***REMOVED***
        // AAC LC
        int profile = 2;
        // CPE
        int chanCfg = 1;
        int freqIdx = samplingFrequencyIndexMap.get(micParam.getSampleRateInHz());
        // filled in ADTS data
        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF9;
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
  ***REMOVED***

    private void record() ***REMOVED***
        if (audioCodec == null) ***REMOVED***
            return;
      ***REMOVED***
        stopEncode = false;
        audioRecord.startRecording();
        audioCodec.start();
        MediaCodec.BufferInfo audioInfo = new MediaCodec.BufferInfo();
        while (true) ***REMOVED***
            if (stopEncode) ***REMOVED***
                release();
                break;
          ***REMOVED***

            // 将 AudioRecord 获取的 PCM 原始数据送入编码器
            int audioInputBufferId = audioCodec.dequeueInputBuffer(0);
            if (audioInputBufferId >= 0) ***REMOVED***
                ByteBuffer inputBuffer = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) ***REMOVED***
                    inputBuffer = audioCodec.getInputBuffer(audioInputBufferId);
              ***REMOVED*** else ***REMOVED***
                    inputBuffer = audioCodec.getInputBuffers()[audioInputBufferId];
              ***REMOVED***
                int readSize = -1;
                if (inputBuffer != null) ***REMOVED***
                    readSize = audioRecord.read(inputBuffer, bufferSizeInBytes);
              ***REMOVED***
                if (readSize >= 0) ***REMOVED***
                    audioCodec.queueInputBuffer(audioInputBufferId, 0, readSize, System.nanoTime() / 1000, 0);
              ***REMOVED***
          ***REMOVED***

            int audioOutputBufferId = audioCodec.dequeueOutputBuffer(audioInfo, 0);
            while (audioOutputBufferId >= 0) ***REMOVED***
                ByteBuffer outputBuffer = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) ***REMOVED***
                    outputBuffer = audioCodec.getOutputBuffer(audioOutputBufferId);
              ***REMOVED*** else ***REMOVED***
                    outputBuffer = audioCodec.getOutputBuffers()[audioOutputBufferId];
              ***REMOVED***
                if (audioInfo.size > 2) ***REMOVED***
                    outputBuffer.position(audioInfo.offset);
                    outputBuffer.limit(audioInfo.offset + audioInfo.size);
                    addADTStoPacket(outputBuffer);
              ***REMOVED***
                audioCodec.releaseOutputBuffer(audioOutputBufferId, false);
                audioOutputBufferId = audioCodec.dequeueOutputBuffer(audioInfo, 0);
          ***REMOVED***
      ***REMOVED***
  ***REMOVED***
}
