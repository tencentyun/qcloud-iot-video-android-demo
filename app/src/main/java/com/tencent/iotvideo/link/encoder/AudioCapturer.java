package com.tencent.iotvideo.link.encoder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioRecord;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.util.Log;

import com.tencent.iotvideo.link.param.MicParam;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

/**
 * 音频采集器
 *
 * <p>职责：
 * <ul>
 *   <li>初始化 {@link AudioRecord}，可选启用系统 AEC / AGC</li>
 *   <li>在独立线程中循环读取麦克风 PCM 数据</li>
 *   <li>通过 {@link CaptureCallback} 将采集到的数据回调给调用方</li>
 * </ul>
 *
 * <p>线程模型：
 * <pre>
 *   AudioRecord ──► 采集线程 ──► CaptureCallback.onCaptured()
 * </pre>
 */
public class AudioCapturer {

    private static final String TAG = "AudioCapturer";

    /** GVoice AEC 每次处理的固定帧大小（字节），对应 16kHz 单声道 16bit 的 20ms */
    private static final int GVOICE_AEC_USAGE_CONDITION = 640;
    /** 每次读取 1280 字节（40ms = 2帧），是 GVOICE_AEC_USAGE_CONDITION 的整数倍，AEC 可正常处理 */
    private static final int MIC_READ_SIZE = 1280;

    /** 采集数据回调接口 */
    public interface CaptureCallback {
        /**
         * 采集到一帧 PCM 数据时回调
         *
         * @param pcmData  采集到的 PCM 字节数组（已 clone，可安全持有）
         * @param readSize 实际有效字节数
         */
        void onCaptured(byte[] pcmData, int readSize);

        /**
         * 采集线程退出时回调（可选实现）
         * 用于通知外部采集已结束，可投入结束标记帧
         */
        default void onStopped() {}
    }

    private final MicParam micParam;
    private final boolean enableAEC;
    private final boolean enableAGC;

    private AudioRecord audioRecord;
    private AcousticEchoCanceler canceler;
    private AutomaticGainControl agcControl;

    private int bufferSizeInBytes;
    private volatile boolean stopped = false;
    private volatile boolean muted = false;

    // ===== 采集统计 =====
    private long captureTotalBytes = 0;
    private long captureFrameCount = 0;

    private CaptureCallback captureCallback;

    /** 获取当前时间戳字符串，格式：HH:mm:ss.SSS */
    private static String ts() {
        return new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(new Date());
    }

    /**
     * 构造函数
     *
     * @param micParam  麦克风参数
     * @param enableAEC 是否启用系统 AEC
     * @param enableAGC 是否启用系统 AGC
     */
    public AudioCapturer(MicParam micParam, boolean enableAEC, boolean enableAGC) {
        this.micParam = micParam;
        this.enableAEC = enableAEC;
        this.enableAGC = enableAGC;
        initAudio();
    }

    @SuppressLint("MissingPermission")
    private void initAudio() {
        if (!enableAGC) {
            bufferSizeInBytes = MIC_READ_SIZE;
        } else {
            bufferSizeInBytes = (micParam.getSampleRateInHz() * micParam.getChannelConfig()
                    * micParam.getAudioFormat() / 8) / 1000 * 20;
        }
        audioRecord = new AudioRecord(
                micParam.getAudioSource(),
                micParam.getSampleRateInHz(),
                micParam.getChannelConfig(),
                micParam.getAudioFormat(),
                bufferSizeInBytes);

        int audioSessionId = audioRecord.getAudioSessionId();
        if (enableAEC && audioSessionId != 0) {
            initAEC(audioSessionId);
        }
        if (enableAGC && audioSessionId != 0) {
            initAGC(audioSessionId);
        }
    }

    private boolean initAEC(int audioSession) {
        if (!AcousticEchoCanceler.isAvailable()) return false;
        if (canceler != null) return false;
        canceler = AcousticEchoCanceler.create(audioSession);
        if (canceler == null) return false;
        canceler.setEnabled(true);
        return canceler.getEnabled();
    }

    private boolean initAGC(int audioSession) {
        if (!AutomaticGainControl.isAvailable()) return false;
        if (agcControl != null) return false;
        agcControl = AutomaticGainControl.create(audioSession);
        if (agcControl == null) return false;
        agcControl.setEnabled(true);
        return agcControl.getEnabled();
    }

    /**
     * 设置采集数据回调
     */
    public void setCaptureCallback(CaptureCallback callback) {
        this.captureCallback = callback;
    }

    /**
     * 获取 AudioRecord 的 AudioSession ID（供外部 AEC/AGC 使用）
     */
    public int getAudioSessionId() {
        return audioRecord != null ? audioRecord.getAudioSessionId() : 0;
    }

    /**
     * 获取每次读取的缓冲区大小（字节）
     */
    public int getBufferSizeInBytes() {
        return bufferSizeInBytes;
    }

    /**
     * 获取底层 AudioRecord 对象
     */
    public AudioRecord getAudioRecord() {
        return audioRecord;
    }

    /**
     * 设置是否静音（静音时输出全零数据）
     */
    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public boolean isMuted() {
        return muted;
    }

    /**
     * 是否支持系统 AEC
     */
    public boolean isDevicesSupportAEC() {
        return AcousticEchoCanceler.isAvailable();
    }

    /**
     * 是否支持系统 AGC
     */
    public boolean isDevicesSupportAGC() {
        return AutomaticGainControl.isAvailable();
    }

    /**
     * 启动采集线程
     */
    public void start() {
        stopped = false;
        new Thread(this::captureLoop, "AudioCaptureThread").start();
    }

    /**
     * 停止采集
     */
    public void stop() {
        stopped = true;
    }

    /**
     * 释放资源
     */
    public void release() {
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        if (canceler != null) {
            canceler.setEnabled(false);
            canceler.release();
            canceler = null;
        }
        if (agcControl != null) {
            agcControl.setEnabled(false);
            agcControl.release();
            agcControl = null;
        }
    }

    /**
     * 采集线程主循环
     */
    private void captureLoop() {
        if (audioRecord == null) {
            Log.e(TAG, "[" + ts() + "][CAPTURE] AudioRecord 未初始化，退出采集线程");
            return;
        }
        audioRecord.startRecording();
        byte[] nearBuffer = new byte[bufferSizeInBytes];
        Log.i(TAG, "[" + ts() + "][CAPTURE] 采集线程已启动 bufferSize=" + bufferSizeInBytes);

        while (!stopped) {
            int readSize = audioRecord.read(nearBuffer, 0, bufferSizeInBytes);
            if (readSize < 0) {
                continue;
            }

            // ===== 采集统计（每帧打印）=====
            if (readSize > 0) {
                captureTotalBytes += readSize;
                captureFrameCount++;
                Log.i(TAG, "[" + ts() + "][CAPTURE] 本地采集"
                        + " 本帧=" + readSize + "B"
                        + " 累计=" + captureTotalBytes + "B"
                        + " 帧数=" + captureFrameCount);
            }

            byte[] pcmData = (readSize == nearBuffer.length)
                    ? nearBuffer.clone()
                    : Arrays.copyOf(nearBuffer, readSize > 0 ? readSize : nearBuffer.length);

            if (muted) {
                Arrays.fill(pcmData, (byte) 0);
            }

            if (captureCallback != null) {
                captureCallback.onCaptured(pcmData, readSize > 0 ? readSize : pcmData.length);
            }
        }

        Log.i(TAG, "[" + ts() + "][CAPTURE] 采集线程已退出"
                + " totalBytes=" + captureTotalBytes
                + " frames=" + captureFrameCount);
        if (captureCallback != null) {
            captureCallback.onStopped();
        }
    }
}
