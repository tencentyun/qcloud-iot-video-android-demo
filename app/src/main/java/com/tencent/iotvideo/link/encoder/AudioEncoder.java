package com.tencent.iotvideo.link.encoder;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.tencent.iotvideo.link.listener.OnEncodeListener;
import com.tencent.iotvideo.link.param.AudioEncodeParam;
import com.tencent.iotvideo.link.param.MicParam;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 音频编码器（协调器）
 *
 * <p>职责：
 * <ul>
 *   <li>协调 {@link AudioCapturer}（采集）、{@link AecProcessor}（回声消除）和 MediaCodec（编码）</li>
 *   <li>将编码后的 AAC 数据通过 {@link OnEncodeListener} 回调给上层</li>
 * </ul>
 *
 * <p>线程模型：
 * <pre>
 *   AudioCapturer ──► [AecProcessor] ──► encodeQueue ──► 编码线程 ──► OnEncodeListener
 * </pre>
 */
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

    private final Object codecLock = new Object();
    private volatile boolean running = false;
    private MediaCodec audioCodec;
    private final MicParam micParam;
    private final AudioEncodeParam audioEncodeParam;
    private OnEncodeListener encodeListener;

    private volatile boolean stopEncode = false;
    private long seq = 0L;

    /** 编码线程引用，用于 stop() 时 join 等待其真正退出，避免与新的 start() 并发操作同一 codec */
    private Thread encodeThread;
    /** 保证 start/stop 串行化，避免并发 stop() 导致多次 release */
    private final Object lifecycleLock = new Object();

    /** 编码队列：采集/AEC线程投入，编码线程消费 */
    private final LinkedBlockingQueue<byte[]> encodeQueue = new LinkedBlockingQueue<>(40);

    // ===== 远端PCM（setPlayerPcmData）统计 =====
    private long farTotalBytesReceived = 0;
    private long farFrameCount = 0;

    // ===== 发送PTS统计 =====
    private long lastSendPts = 0;
    private long sendFrameCount = 0;

    private boolean enableGvoiceAEC = false;
    /** AEC 独立线程处理器，enableGvoiceAEC=true 时使用 */
    private AecProcessor aecProcessor = null;

    /** 音频采集器 */
    private final AudioCapturer audioCapturer;

    private static final int SAVE_PCM_DATA = 1;
    private boolean isRecordPcm = false;
    private String speakPcmFilePath = "/storage/emulated/0/speak_pcm_";

    private FileOutputStream fosNear;
    private FileOutputStream fosFar;
    private FileOutputStream fosAec;
    private final Handler mHandler = new SavePcmHandler();

    /** 获取当前时间戳字符串，格式：HH:mm:ss.SSS */
    private static String ts() {
        return new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(new Date());
    }

    public AudioEncoder(MicParam micParam, AudioEncodeParam audioEncodeParam) {
        this(micParam, audioEncodeParam, false, false);
    }

    public AudioEncoder(MicParam micParam, AudioEncodeParam audioEncodeParam, boolean enableAEC, boolean enableAGC) {
        this(micParam, audioEncodeParam, enableAEC, enableAGC, null);
    }

    /**
     * 构造函数，支持 gvoice 回声消除
     *
     * @param micParam         麦克风参数
     * @param audioEncodeParam 音频编码参数
     * @param enableAEC        是否启用系统 AEC
     * @param enableAGC        是否启用系统 AGC
     * @param context          Context，非空时初始化 GVoice
     */
    public AudioEncoder(MicParam micParam, AudioEncodeParam audioEncodeParam,
                        boolean enableAEC, boolean enableAGC, Context context) {
        this.micParam = micParam;
        this.audioEncodeParam = audioEncodeParam;
        // 初始化采集器
        this.audioCapturer = new AudioCapturer(micParam, enableAEC, enableAGC);
        // 初始化编码器
        initCodec();
        if (context != null) {
            com.iot.gvoice.interfaces.GvoiceJNIBridge.init(context);
        }
    }

    private void initCodec() {
        try {
            audioCodec = MediaCodec.createEncoderByType(audioEncodeParam.getMime());
            MediaFormat format = MediaFormat.createAudioFormat(
                    audioEncodeParam.getMime(), micParam.getSampleRateInHz(), 1);
            format.setInteger(MediaFormat.KEY_BIT_RATE, audioEncodeParam.getBitRate());
            format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, audioEncodeParam.getMaxInputSize());
            audioCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (IOException e) {
            e.printStackTrace();
            audioCodec = null;
        }
    }

    public void setOnEncodeListener(OnEncodeListener listener) {
        this.encodeListener = listener;
    }

    public void setMuted(boolean muted) {
        audioCapturer.setMuted(muted);
    }

    public boolean isMuted() {
        return audioCapturer.isMuted();
    }

    public boolean isDevicesSupportAEC() {
        return audioCapturer.isDevicesSupportAEC();
    }

    public boolean isDevicesSupportAGC() {
        return audioCapturer.isDevicesSupportAGC();
    }

    /**
     * 设置是否保存 PCM 数据到文件
     */
    public void setRecordPcm(boolean isRecord) {
        this.isRecordPcm = isRecord;
    }

    /**
     * 设置 PCM 文件保存路径
     */
    public void setSpeakPcmFilePath(String path) {
        this.speakPcmFilePath = path;
    }

    /**
     * 创建 PCM 文件输出流
     */
    private FileOutputStream createPcmFile(String format) {
        if (!TextUtils.isEmpty(speakPcmFilePath)) {
            File file = new File(speakPcmFilePath + format + ".pcm");
            if (file.exists()) {
                file.delete();
            }
            try {
                file.createNewFile();
                return new FileOutputStream(file);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    /**
     * Handler 用于异步保存 PCM 数据
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
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 写入 PCM 数据到文件
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

    /**
     * 动态开关 GVoice AEC
     * <p>
     * - 开启时：若编码器已启动且 AEC 尚未运行，则立即创建并启动 {@link AecProcessor} 和转发线程
     * - 关闭时：停止 {@link AecProcessor}，采集数据切换为直通模式（直接入编码队列）
     * <p>
     * 使用 lifecycleLock 串行化，避免并发调用产生多个 AecProcessor / 转发线程
     *
     * @param enable 是否开启 AEC
     */
    public void setEnableGvoiceAEC(boolean enable) {
        synchronized (lifecycleLock) {
            if (this.enableGvoiceAEC == enable) return;
            this.enableGvoiceAEC = enable;

            if (enable) {
                // 开启 AEC：若编码器已在运行且 AecProcessor 尚未启动，则立即启动
                if (!stopEncode && aecProcessor == null) {
                    aecProcessor = new AecProcessor();
                    aecProcessor.start();
                    // 启动 AEC 转发线程
                    new Thread(this::aecForwardLoop, "AecForwardThread").start();
                    Log.i(TAG, "[" + ts() + "][AEC] 动态开启 AEC");
                }
            } else {
                // 关闭 AEC：停止 AecProcessor，采集回调会自动切换为直通模式
                if (aecProcessor != null) {
                    aecProcessor.stop();
                    aecProcessor = null;
                    Log.i(TAG, "[" + ts() + "][AEC] 动态关闭 AEC");
                }
            }
        }
    }

    /**
     * 设置播放器 PCM 数据（用于 AEC 参考）
     *
     * @param pcmData 播放器输出的 PCM 数据
     */
    public void setPlayerPcmData(byte[] pcmData) {
        if (pcmData == null || pcmData.length == 0) return;

        farTotalBytesReceived += pcmData.length;
        farFrameCount++;
        Log.i(TAG, "[" + ts() + "][FAR-IN] 远端PCM接收"
                + " 本帧=" + pcmData.length + "B"
                + " 累计=" + farTotalBytesReceived + "B"
                + " 帧数=" + farFrameCount);

        // 快照，避免与 setEnableGvoiceAEC(false) 并发时读到已 stop 的 processor
        AecProcessor snapshot = aecProcessor;
        if (snapshot != null) {
            snapshot.putFarData(pcmData);
        }
    }

    /**
     * 启动采集、AEC（可选）和编码线程
     * <p>
     * 支持在 stop() 之后重新调用 start()：会等待上一次的编码线程完全退出，并重建 MediaCodec。
     */
    public void start() {
        synchronized (lifecycleLock) {
            // 若上一次的编码线程仍在运行，先等待其退出，避免两条编码线程并发操作同一 MediaCodec
            if (encodeThread != null && encodeThread.isAlive()) {
                Log.w(TAG, "[" + ts() + "] 上次编码线程仍在运行，等待其退出后再启动");
                try {
                    encodeThread.join(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            encodeThread = null;

            // 若 codec 已在上次 release 中置 null，则重新初始化，支持实例复用
            synchronized (codecLock) {
                if (audioCodec == null) {
                    initCodec();
                }
            }
            if (audioCodec == null) {
                Log.e(TAG, "[" + ts() + "] MediaCodec 未初始化，无法启动");
                return;
            }
            stopEncode = false;

        if (isRecordPcm) {
            fosNear = createPcmFile("near");
            fosFar = createPcmFile("far");
            fosAec = createPcmFile("aec");
        }

        // 注意：AEC 在 start() 时不再预先启动，由 setEnableGvoiceAEC(true) 动态启动
        // 若调用 start() 前已调用 setEnableGvoiceAEC(true)，则在此处启动
        if (enableGvoiceAEC && aecProcessor == null) {
            aecProcessor = new AecProcessor();
            aecProcessor.start();
            // 启动 AEC 转发线程：将 AEC 输出结果转发到编码队列
            new Thread(this::aecForwardLoop, "AecForwardThread").start();
        }

        // 设置采集回调：将采集到的数据路由到 AEC 或直接入编码队列
        // 注意：enableGvoiceAEC 和 aecProcessor 均为 volatile/动态判断，支持运行时切换
        audioCapturer.setCaptureCallback(new AudioCapturer.CaptureCallback() {
            @Override
            public void onCaptured(byte[] pcmData, int readSize) {
                AecProcessor currentAec = aecProcessor;
                if (enableGvoiceAEC && currentAec != null
                        && readSize % GVOICE_AEC_USAGE_CONDITION == 0) {
                    // 将近端数据投入 AEC 处理器（非阻塞）
                    currentAec.putNearData(pcmData);
                    // AEC 结果由 AEC 转发线程写入 encodeQueue
                } else {
                    // 直接入编码队列（直通模式）
                    if (!encodeQueue.offer(pcmData)) {
                        Log.w(TAG, "[" + ts() + "][CAPTURE] 编码队列已满，丢弃本帧 size=" + pcmData.length);
                    }
                }
            }

            @Override
            public void onStopped() {
                // AEC 模式下，由 AEC 转发线程负责投入空标记帧
                // 非 AEC 模式下，由采集线程退出时投入空标记帧通知编码线程退出
                if (!enableGvoiceAEC || aecProcessor == null) {
                    encodeQueue.offer(new byte[0]);
                }
            }
        });

            // 启动编码线程（保存引用以便 stop() 时 join）
            encodeThread = new Thread(this::encodeLoop, "AudioEncodeThread");
            encodeThread.start();
            // 启动采集线程
            audioCapturer.start();
        }
    }

    /**
     * 停止编码和采集
     * <p>
     * 该方法会阻塞等待编码线程完全退出后再返回，以保证：
     * 1) MediaCodec 的 release 只发生一次；
     * 2) 返回后再次 start() 时不会与旧线程并发操作同一 codec，避免 native use-after-free
     *    导致 RefBase::decStrong 崩溃。
     */
    public void stop() {
        synchronized (lifecycleLock) {
            stopEncode = true;
            audioCapturer.stop();
            // 投入一个空标记帧，唤醒可能在 encodeQueue.poll 上等待的编码线程，加速退出
            encodeQueue.offer(new byte[0]);

            Thread t = encodeThread;
            if (t != null && t != Thread.currentThread()) {
                try {
                    t.join(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                if (t.isAlive()) {
                    Log.w(TAG, "[" + ts() + "] 编码线程未在 2s 内退出，强制中断");
                    t.interrupt();
                }
            }
            encodeThread = null;

            // 兜底：若编码线程未成功执行到 release()，此处强制释放 codec，保证 stop() 语义
            synchronized (codecLock) {
                if (audioCodec != null) {
                    try {
                        audioCodec.stop();
                    } catch (Exception ignore) {
                    }
                    try {
                        audioCodec.release();
                    } catch (Exception ignore) {
                    }
                    audioCodec = null;
                }
            }
        }
    }

    private void release() {
        running = false;
        audioCapturer.release();

        synchronized (codecLock) {
            if (audioCodec != null) {
                try {
                    audioCodec.stop();
                } catch (Exception ignore) {
                }
                try {
                    audioCodec.release();
                } catch (Exception ignore) {
                }
                audioCodec = null;
            }
        }

        // aecProcessor 使用快照方式释放，避免与 setEnableGvoiceAEC 并发时二次释放
        AecProcessor aecSnapshot = aecProcessor;
        aecProcessor = null;
        if (aecSnapshot != null) {
            try {
                aecSnapshot.stop();
            } catch (Exception ignore) {
            }
        }

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
        }
    }

    private void addADTStoPacket(byte[] packet, int packetLen) {
        int profile = 2;
        int chanCfg = 1;
        int freqIdx = samplingFrequencyIndexMap.get(micParam.getSampleRateInHz());
        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF9;
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }

    /**
     * AEC 转发线程：将 AEC 输出队列的数据转发到编码队列
     * 仅在 enableGvoiceAEC=true 时运行。
     * <p>
     * 退出条件：
     * 1. 编码器停止（stopEncode=true）
     * 2. AEC 被动态关闭（aecProcessor 被置为 null）
     */
    private void aecForwardLoop() {
        // 记录本次转发线程绑定的 AecProcessor 实例，避免被动态替换后错误操作
        AecProcessor boundAec = aecProcessor;
        if (boundAec == null) {
            Log.w(TAG, "[" + ts() + "][AEC-FWD] boundAec 为空，转发线程直接退出");
            return;
        }
        Log.i(TAG, "[" + ts() + "][AEC-FWD] AEC转发线程已启动");
        while (!stopEncode && aecProcessor == boundAec) {
            byte[] aecOutput = boundAec.pollOutput(20);
            if (aecOutput != null && aecOutput.length > 0) {
                if (audioCapturer.isMuted()) {
                    Arrays.fill(aecOutput, (byte) 0);
                }
                if (!encodeQueue.offer(aecOutput)) {
                    Log.w(TAG, "[" + ts() + "][AEC-FWD] 编码队列已满，丢弃AEC帧 size=" + aecOutput.length);
                }
            }
        }
        // 将剩余 AEC 输出全部转发（仅在编码器停止时才投入结束标记帧）
        byte[] remaining;
        while ((remaining = boundAec.pollOutput()) != null) {
            if (remaining.length > 0) {
                encodeQueue.offer(remaining);
            }
        }
        // 仅在编码器停止时投入空标记帧，通知编码线程退出
        // 若是 AEC 被动态关闭，采集线程的直通模式会继续供数据，不需要投入结束标记
        if (stopEncode) {
            encodeQueue.offer(new byte[0]);
        }
        Log.i(TAG, "[" + ts() + "][AEC-FWD] AEC转发线程已退出 stopEncode=" + stopEncode);
    }

    /**
     * 编码线程主循环：从编码队列取数据，送入 MediaCodec 编码，回调输出
     * <p>
     * 所有对 audioCodec 的调用均在 codecLock 内进行，并在锁内二次判 null，
     * 避免与 stop()/release() 并发导致 native use-after-free（RefBase::decStrong 崩溃）。
     */
    private void encodeLoop() {
        synchronized (codecLock) {
            if (audioCodec == null) {
                return;
            }
            try {
                audioCodec.start();
            } catch (Exception e) {
                Log.e(TAG, "[" + ts() + "] audioCodec.start 失败: " + e.getMessage(), e);
                return;
            }
            running = true;
        }
        MediaCodec.BufferInfo audioInfo = new MediaCodec.BufferInfo();
        int bufferSizeInBytes = audioCapturer.getBufferSizeInBytes();

        while (true) {
            if (stopEncode && encodeQueue.isEmpty()) {
                release();
                break;
            }

            // 从编码队列取数据（最多等待 50ms）
            byte[] processedData;
            try {
                processedData = encodeQueue.poll(50, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            // 空标记帧或 null 均表示无数据，继续循环检查退出条件
            if (processedData == null || processedData.length == 0) {
                if (stopEncode) {
                    release();
                    break;
                }
                continue;
            }

            int readSize = processedData.length;

            // ===== 编码入队与取出：全部在 codecLock 内进行，二次判 null =====
            synchronized (codecLock) {
                if (!running || audioCodec == null) {
                    // codec 已被并发释放，直接退出循环
                    break;
                }
                try {
                    int audioInputBufferId = audioCodec.dequeueInputBuffer(50000);
                    if (audioInputBufferId >= 0) {
                        ByteBuffer inputBuffer;
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                            inputBuffer = audioCodec.getInputBuffer(audioInputBufferId);
                        } else {
                            inputBuffer = audioCodec.getInputBuffers()[audioInputBufferId];
                        }
                        if (inputBuffer != null) {
                            inputBuffer.clear();
                            inputBuffer.put(processedData, 0, readSize);
                        }
                        long pts = System.nanoTime() / 1000;

                        // ===== PTS 信息打印（仅当 PTS 间隔偏差超过预期的 50% 时打印）=====
                        if (lastSendPts > 0) {
                            long ptsGapUs = pts - lastSendPts;
                            long expectedPtsGapUs = (long) bufferSizeInBytes * 1_000_000L
                                    / (micParam.getSampleRateInHz() * 2);
                            long deviation = ptsGapUs - expectedPtsGapUs;
                            if (Math.abs(deviation) > expectedPtsGapUs / 2) {
                                Log.w(TAG, "[" + ts() + "][PTS] ⚠️ PTS间隔异常!"
                                        + " seq=" + seq
                                        + " pts=" + pts + "us"
                                        + " ptsGap=" + ptsGapUs + "us"
                                        + " expected=" + expectedPtsGapUs + "us"
                                        + " deviation=" + deviation + "us"
                                        + " frameSize=" + readSize + "B");
                            }
                        }
                        lastSendPts = pts;
                        audioCodec.queueInputBuffer(audioInputBufferId, 0, readSize, pts, 0);
                    }

                    int audioOutputBufferId = audioCodec.dequeueOutputBuffer(audioInfo, 0);
                    while (audioOutputBufferId >= 0) {
                        ByteBuffer outputBuffer;
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                            outputBuffer = audioCodec.getOutputBuffer(audioOutputBufferId);
                        } else {
                            outputBuffer = audioCodec.getOutputBuffers()[audioOutputBufferId];
                        }
                        if (outputBuffer != null && audioInfo.size > 2) {
                            outputBuffer.position(audioInfo.offset);
                            outputBuffer.limit(audioInfo.offset + audioInfo.size);
                            int aacFrameSize = audioInfo.size + 7;
                            // ===== 发送 PTS 详细信息（每帧打印）=====
                            Log.d(TAG, "[" + ts() + "][SEND-PTS]"
                                    + " seq=" + seq
                                    + " pts=" + audioInfo.presentationTimeUs + "us"
                                    + " aacSize=" + aacFrameSize + "B"
                                    + " flags=" + audioInfo.flags
                                    + " sendFrames=" + sendFrameCount);
                            addADTStoPacket(outputBuffer);
                            sendFrameCount++;
                        }
                        audioCodec.releaseOutputBuffer(audioOutputBufferId, false);
                        // 再次判 null 后继续 dequeue
                        if (audioCodec == null) break;
                        audioOutputBufferId = audioCodec.dequeueOutputBuffer(audioInfo, 0);
                    }
                } catch (IllegalStateException ise) {
                    // codec 已被释放或处于非法状态，退出循环
                    Log.w(TAG, "[" + ts() + "] audioCodec 状态异常，退出编码循环: " + ise.getMessage());
                    break;
                } catch (Exception e) {
                    Log.e(TAG, "[" + ts() + "] 编码循环异常: " + e.getMessage(), e);
                    break;
                }
            }
        }

        // 兜底：无论从哪种途径退出，都保证 release 被调用一次
        release();
    }
}