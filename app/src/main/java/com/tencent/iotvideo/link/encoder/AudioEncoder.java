package com.tencent.iotvideo.link.encoder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.iot.gvoice.interfaces.GvoiceJNIBridge;
import com.tencent.iotvideo.link.listener.OnEncodeListener;
import com.tencent.iotvideo.link.param.AudioEncodeParam;
import com.tencent.iotvideo.link.param.MicParam;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

public class AudioEncoder {

    /**
     * 采样频率对照表
     */
    private static final Map<Integer, Integer> samplingFrequencyIndexMap = new HashMap<>();

    private static final int GVOICE_AEC_USAGE_CONDITION = 640;

    static {
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
    }

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
    private boolean isMuted = false;

    private boolean enableAEC;

    private boolean enableAGC;
    private Context context;
    private boolean enableGvoiceAEC = true;
    private LinkedBlockingQueue<byte[]> playPcmData = new LinkedBlockingQueue<>(1920*5);
    private long queueOverflowCount = 0;  // 队列溢出计数
    private long queueUnderflowCount = 0;  // 队列欠载计数

    private static final int SAVE_PCM_DATA = 1;
    private boolean isRecordPcm = true;
    private String speakPcmFilePath = "/storage/emulated/0/speak_pcm_";

    private FileOutputStream fosNear;  // 保存麦克风原始数据
    private FileOutputStream fosFar;   // 保存播放器参考数据
    private FileOutputStream fosAec;   // 保存回声消除后数据
    private final Handler mHandler = new SavePcmHandler();

    public AudioEncoder(MicParam micParam, AudioEncodeParam audioEncodeParam) {
        this(micParam, audioEncodeParam, false, false);
    }


    public AudioEncoder(MicParam micParam, AudioEncodeParam audioEncodeParam, boolean enableAEC, boolean enableAGC) {
        this(micParam, audioEncodeParam, enableAEC, enableAGC, null);
    }

    /**
     * 构造函数，支持gvoice回声消除
     *
     * @param micParam         麦克风参数
     * @param audioEncodeParam 音频编码参数
     * @param enableAEC        是否启用系统AEC
     * @param enableAGC        是否启用系统AGC
     */
    public AudioEncoder(MicParam micParam, AudioEncodeParam audioEncodeParam, boolean enableAEC, boolean enableAGC, Context context) {
        this.micParam = micParam;
        this.audioEncodeParam = audioEncodeParam;
        this.enableAEC = enableAEC;
        this.enableAGC = enableAGC;
        this.context = context;
        if (context != null) {
            GvoiceJNIBridge.init(context);
            Log.d(TAG, "GvoiceJNIBridge initialized for AEC");
            Log.i(TAG, "=== Gvoice AEC Configuration ===");
            Log.i(TAG, "Mic Sample Rate: " + micParam.getSampleRateInHz() + " Hz");
            Log.i(TAG, "Mic Channel Config: " + micParam.getChannelConfig());
            Log.i(TAG, "Mic Audio Format: " + micParam.getAudioFormat());
            Log.i(TAG, "Buffer Size: " + (2 * AudioRecord.getMinBufferSize(micParam.getSampleRateInHz(), micParam.getChannelConfig(), micParam.getAudioFormat())) + " bytes");
            Log.i(TAG, "⚠️ IMPORTANT: Ensure AudioDecoder output format matches AudioEncoder input format!");
            Log.i(TAG, "================================");
        }
        init();
    }

    private void init() {
        initAudio();
        int audioSessionId = audioRecord.getAudioSessionId();
        if (enableAEC && audioSessionId != 0) {
            Log.e(TAG, "=====initAEC result: " + initAEC(audioSessionId));
        }
        if (enableAGC && audioSessionId != 0) {
            Log.e(TAG, "=====initAGC result: " + initAGC(audioSessionId));
        }
    }

    public void setOnEncodeListener(OnEncodeListener listener) {
        this.encodeListener = listener;
    }

    @SuppressLint("MissingPermission")
    private void initAudio() {
        // 计算基础缓冲区大小（20ms数据长度）
        int baseBufferSize = (micParam.getSampleRateInHz() * micParam.getChannelConfig() * micParam.getAudioFormat() / 8) / 1000 * 20;

        // 当启用Gvoice AEC时，必须使用640字节的倍数
        if (context != null && !enableAEC && !enableAGC) {
            // 确保是640的倍数（1920 = 640 * 3）
            bufferSizeInBytes = GVOICE_AEC_USAGE_CONDITION*3;
        } else {
            bufferSizeInBytes = baseBufferSize;
            Log.d(TAG, "=====bufferSizeInBytes: " + bufferSizeInBytes);
        }
        audioRecord = new AudioRecord(micParam.getAudioSource(), micParam.getSampleRateInHz(), micParam.getChannelConfig(), micParam.getAudioFormat(), bufferSizeInBytes);
        try {
            audioCodec = MediaCodec.createEncoderByType(audioEncodeParam.getMime());
            MediaFormat format = MediaFormat.createAudioFormat(audioEncodeParam.getMime(), micParam.getSampleRateInHz(), 1);
            format.setInteger(MediaFormat.KEY_BIT_RATE, audioEncodeParam.getBitRate());
            format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, audioEncodeParam.getMaxInputSize());
            audioCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (IOException e) {
            e.printStackTrace();
            audioRecord = null;
            audioCodec = null;
        }
    }

    public void start() {
        init();
        // 如果需要保存PCM数据，创建文件输出流
        if (isRecordPcm) {
            fosNear = createPcmFile("near");
            fosFar = createPcmFile("far");
            fosAec = createPcmFile("aec");
        }
        new Thread(this::record).start();
    }

    public void stop() {
        stopEncode = true;
    }

    public boolean isDevicesSupportAEC() {
        return AcousticEchoCanceler.isAvailable();
    }

    private boolean initAEC(int audioSession) {

        boolean isDevicesSupportAEC = isDevicesSupportAEC();
        Log.e(TAG, "isDevicesSupportAEC: " + isDevicesSupportAEC);
        if (!isDevicesSupportAEC) {
            return false;
        }
        if (canceler != null) {
            return false;
        }
        canceler = AcousticEchoCanceler.create(audioSession);
        if (canceler == null) return false;
        canceler.setEnabled(true);
        return canceler.getEnabled();
    }

    public boolean isDevicesSupportAGC() {
        return AutomaticGainControl.isAvailable();
    }

    private boolean initAGC(int audioSession) {

        boolean isDevicesSupportAGC = isDevicesSupportAGC();
        Log.e(TAG, "isDevicesSupportAGC: " + isDevicesSupportAGC);
        if (!isDevicesSupportAGC) {
            return false;
        }
        if (control != null) {
            return false;
        }
        control = AutomaticGainControl.create(audioSession);
        if (control == null) return false;
        control.setEnabled(true);
        return control.getEnabled();
    }

    public void setMuted(boolean muted) {
        isMuted = muted;
    }

    public boolean isMuted() {
        return isMuted;
    }

    /**
     * 设置是否保存PCM数据到文件
     *
     * @param isRecord 是否保存
     */
    public void setRecordPcm(boolean isRecord) {
        this.isRecordPcm = isRecord;
    }

    /**
     * 设置PCM文件保存路径
     *
     * @param path 文件路径前缀
     */
    public void setSpeakPcmFilePath(String path) {
        Log.e(TAG, "setSpeakPcmFilePath is: " + path);
        this.speakPcmFilePath = path;
    }

    /**
     * 创建PCM文件输出流
     *
     * @param format 文件名后缀（near/far/aec）
     * @return FileOutputStream
     */
    private FileOutputStream createPcmFile(String format) {
        if (!TextUtils.isEmpty(speakPcmFilePath)) {
            File file = new File(speakPcmFilePath + format + ".pcm");
            Log.i(TAG, "speak cache pcm file path: " + file.getAbsolutePath());
            if (file.exists()) {
                file.delete();
            }
            try {
                file.createNewFile();
                return new FileOutputStream(file);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "创建PCM文件失败: " + e.getMessage());
                return null;
            }
        }
        return null;
    }

    /**
     * Handler用于异步保存PCM数据
     */
    private class SavePcmHandler extends Handler {
        public SavePcmHandler() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            try {
                if (msg.what == SAVE_PCM_DATA && fosNear != null && fosFar != null && fosAec != null) {
                    JSONObject jsonObject = (JSONObject) msg.obj;
                    byte[] nearBytesData = (byte[]) jsonObject.get("nearPcmBytes");
                    fosNear.write(nearBytesData);
                    fosNear.flush();

                    byte[] farBytesData = (byte[]) jsonObject.get("farPcmBytes");
                    fosFar.write(farBytesData);
                    fosFar.flush();

                    byte[] aecBytesData = (byte[]) jsonObject.get("aecPcmBytes");
                    fosAec.write(aecBytesData);
                    fosAec.flush();
                }
            } catch (IOException e) {
                Log.e(TAG, "IOException while saving PCM: " + e);
                e.printStackTrace();
            } catch (JSONException e) {
                Log.e(TAG, "JSONException while saving PCM: " + e);
                e.printStackTrace();
            }
        }
    }

    /**
     * 写入PCM数据到文件
     *
     * @param nearPcmBytes 麦克风原始数据
     * @param farPcmBytes  播放器参考数据
     * @param aecPcmBytes  回声消除后数据
     */
    private void writePcmBytesToFile(byte[] nearPcmBytes, byte[] farPcmBytes, byte[] aecPcmBytes) {
        if (isRecordPcm) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("nearPcmBytes", nearPcmBytes);
                jsonObject.put("farPcmBytes", farPcmBytes);
                jsonObject.put("aecPcmBytes", aecPcmBytes);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Message message = mHandler.obtainMessage(SAVE_PCM_DATA, jsonObject);
            mHandler.sendMessage(message);
        }
    }

    public void setEnableGvoiceAEC(boolean enableGvoiceAEC) {
        this.enableGvoiceAEC = enableGvoiceAEC;
    }

    /**
     * 设置播放器PCM数据（用于AEC参考）
     * 采用完整帧存储策略，避免逐字节装箱开销
     * 
     * @param pcmData 播放器输出的PCM数据
     */
    public void setPlayerPcmData(byte[] pcmData) {
        if (pcmData == null || pcmData.length == 0) return;
        
        // 尝试将完整帧加入队列
        if (!playPcmData.offer(pcmData)) {
            // 队列满时丢弃新数据，避免破坏正在读取的旧帧
            queueOverflowCount++;
            if (queueOverflowCount % 100 == 1) {  // 每100次溢出输出一次日志
                Log.w(TAG, "⚠️ Player PCM queue overflow! Dropped " + queueOverflowCount + " frames. Consider increasing queue size or reducing latency.");
            }
        }
    }

    /**
     * 从播放器PCM队列中读取指定长度的数据
     * 优化策略：
     * 1. 使用完整帧读取，避免逐字节拆箱
     * 2. 队列欠载时返回null，由调用方处理
     * 3. 支持拼接多帧数据以满足所需长度（必须是640的倍数）
     *
     * @param length 需要读取的数据长度（必须是640的倍数）
     * @return 播放器PCM数据，如果队列数据不足则返回null
     */
    private byte[] onReadPlayerPlayPcm(int length) {
        // 验证长度是否为640的倍数
        if (length % 640 != 0) {
            Log.e(TAG, "❌ Invalid length for Gvoice AEC: " + length + " (must be multiple of 640)");
            return null;
        }

        byte[] result = new byte[length];
        int offset = 0;
        
        try {
            // 从队列中读取完整帧并拼接
            while (offset < length) {
                byte[] frame = playPcmData.poll();
                if (frame == null) {
                    // 队列为空，数据不足
                    queueUnderflowCount++;
                    if (queueUnderflowCount % 50 == 1) {
                        Log.w(TAG, "⚠️ Queue underflow #" + queueUnderflowCount + " - requested: " + length + "B, got: " + offset + "B");
                    }
                    return null;
                }
                
                // 拷贝帧数据到结果数组
                int copyLength = Math.min(frame.length, length - offset);
                System.arraycopy(frame, 0, result, offset, copyLength);
                offset += copyLength;
            }
            
            return result;
        } catch (Exception e) {
            Log.e(TAG, "onReadPlayerPlayPcm error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private void release() {
        // 输出队列统计信息
        if (queueOverflowCount > 0 || queueUnderflowCount > 0) {
            Log.i(TAG, "=== Player PCM Queue Statistics ===");
            Log.i(TAG, "Overflow count: " + queueOverflowCount + " (frames dropped due to queue full)");
            Log.i(TAG, "Underflow count: " + queueUnderflowCount + " (frames reused due to queue empty)");
            Log.i(TAG, "Final queue size: " + playPcmData.size() + " frames");
            Log.i(TAG, "===================================");
        }
        
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }

        if (audioCodec != null) {
            audioCodec.stop();
            audioCodec.release();
            audioCodec = null;
        }

        if (canceler != null) {
            canceler.setEnabled(false);
            canceler.release();
            canceler = null;
        }

        if (control != null) {
            control.setEnabled(false);
            control.release();
            control = null;
        }

        // 清理gvoice相关资源
        if (enableGvoiceAEC && !playPcmData.isEmpty()) {
            playPcmData.clear();
        }

        queueOverflowCount = 0;
        queueUnderflowCount = 0;

        // 关闭PCM文件输出流
        try {
            if (fosNear != null) {
                fosNear.close();
                fosNear = null;
            }
            if (fosFar != null) {
                fosFar.close();
                fosFar = null;
            }
            if (fosAec != null) {
                fosAec.close();
                fosAec = null;
            }
        } catch (IOException e) {
            Log.e(TAG, "关闭PCM文件流失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void addADTStoPacket(ByteBuffer outputBuffer) {
        byte[] bytes = new byte[outputBuffer.remaining()];
        outputBuffer.get(bytes, 0, bytes.length);
        byte[] dataBytes = new byte[bytes.length + 7];
        System.arraycopy(bytes, 0, dataBytes, 7, bytes.length);
        addADTStoPacket(dataBytes, dataBytes.length);
        if (stopEncode) {
            return;
        }
        if (encodeListener != null) {
            encodeListener.onAudioEncoded(dataBytes, System.currentTimeMillis(), seq);
            seq++;
        } else {
            Log.e(TAG, "Encode listener is null, please set encode listener.");
        }
    }

    private void addADTStoPacket(byte[] packet, int packetLen) {
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
    }

    private void record() {
        if (audioCodec == null) {
            return;
        }
        stopEncode = false;
        audioRecord.startRecording();
        audioCodec.start();
        MediaCodec.BufferInfo audioInfo = new MediaCodec.BufferInfo();
        while (true) {
            if (stopEncode) {
                release();
                break;
            }

            // 将 AudioRecord 获取的 PCM 原始数据送入编码器
            int audioInputBufferId = audioCodec.dequeueInputBuffer(0);
            if (audioInputBufferId >= 0) {
                ByteBuffer inputBuffer = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    inputBuffer = audioCodec.getInputBuffer(audioInputBufferId);
                } else {
                    inputBuffer = audioCodec.getInputBuffers()[audioInputBufferId];
                }
                int readSize = -1;
                if (inputBuffer != null) {
                    // 先读取原始PCM数据到临时缓冲区
                    byte[] tempBuffer = new byte[bufferSizeInBytes];
                    readSize = audioRecord.read(tempBuffer, 0, bufferSizeInBytes);

                    if (readSize > 0) {
                        byte[] processedData = tempBuffer;
                        byte[] playerPcmBytes = null;
                        if (enableGvoiceAEC) {
                            // 验证麦克风数据长度是否为640的倍数
                            if (readSize % GVOICE_AEC_USAGE_CONDITION != 0) {
                                Log.e(TAG, "❌ Mic data length not multiple of 640: " + readSize + ", AEC disabled for this frame");
                            } else {
                                playerPcmBytes = onReadPlayerPlayPcm(readSize);
                                // 验证播放器数据长度是否匹配
                                if (playerPcmBytes != null && playerPcmBytes.length == readSize) {
                                    // 使用gvoice进行回声消除
                                    processedData = GvoiceJNIBridge.cancellation(tempBuffer, playerPcmBytes);
                                    if (isRecordPcm) {
                                        writePcmBytesToFile(tempBuffer, playerPcmBytes, processedData);
                                    }
                                } else {
                                    // 播放器数据不足或长度不匹配时，传入空数组进行降噪
                                    byte[] emptyPlayerPcm = new byte[readSize];
                                    processedData = GvoiceJNIBridge.cancellation(tempBuffer, emptyPlayerPcm);

                                    if (playerPcmBytes != null) {
                                        Log.w(TAG, "⚠️ Gvoice AEC: player data length mismatch! Expected: " + readSize + ", Got: " + playerPcmBytes.length + ", using empty reference");
                                    }

                                    // 保存PCM数据到文件（使用空数组作为far）
                                    if (isRecordPcm) {
                                        writePcmBytesToFile(tempBuffer, emptyPlayerPcm, processedData);
                                    }
                                }
                            }
                        }

                        // 如果静音，将数据置零
                        if (isMuted) {
                            Arrays.fill(processedData, (byte) 0);
                        }

                        // 将处理后的数据写入编码器输入缓冲区
                        inputBuffer.clear();
                        inputBuffer.put(processedData, 0, processedData.length);
                        readSize = processedData.length;
                    }
                }
                if (readSize >= 0) {
                    audioCodec.queueInputBuffer(audioInputBufferId, 0, readSize, System.nanoTime() / 1000, 0);
                }
            }

            int audioOutputBufferId = audioCodec.dequeueOutputBuffer(audioInfo, 0);
            while (audioOutputBufferId >= 0) {
                ByteBuffer outputBuffer = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    outputBuffer = audioCodec.getOutputBuffer(audioOutputBufferId);
                } else {
                    outputBuffer = audioCodec.getOutputBuffers()[audioOutputBufferId];
                }
                if (audioInfo.size > 2) {
                    outputBuffer.position(audioInfo.offset);
                    outputBuffer.limit(audioInfo.offset + audioInfo.size);
                    addADTStoPacket(outputBuffer);
                }
                audioCodec.releaseOutputBuffer(audioOutputBufferId, false);
                audioOutputBufferId = audioCodec.dequeueOutputBuffer(audioInfo, 0);
            }
        }
    }
}
