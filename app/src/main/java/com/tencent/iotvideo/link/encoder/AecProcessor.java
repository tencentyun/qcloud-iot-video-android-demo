package com.tencent.iotvideo.link.encoder;

import android.util.Log;

import com.iot.gvoice.interfaces.GvoiceJNIBridge;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * AEC（回声消除）独立线程处理器
 *
 * <p>职责：
 * <ul>
 *   <li>接收采集线程投入的近端（麦克风）PCM 帧</li>
 *   <li>接收播放器投入的远端（扬声器）PCM 参考帧</li>
 *   <li>在独立线程中调用 {@link GvoiceJNIBridge#cancellation} 完成 AEC</li>
 *   <li>将处理结果写入输出队列，供编码线程消费</li>
 * </ul>
 *
 * <p>线程模型：
 * <pre>
 *   采集线程  ──near──►  nearQueue  ──┐
 *                                    ├──► AEC线程 ──► outputQueue ──► 编码线程
 *   播放线程  ──far───►  farQueue   ──┘
 * </pre>
 *
 * <p>当 AEC 变慢时，只会导致输出队列积压，不会阻塞采集循环。
 */
public class AecProcessor {

    private static final String TAG = "AecProcessor";

    /** GVoice AEC 每次处理的固定帧大小（字节），对应 16kHz 单声道 16bit 的 20ms */
    private static final int FRAME_SIZE = 640;

    /** 近端输入队列容量 */
    private static final int NEAR_QUEUE_CAPACITY = 20;
    /** 远端参考队列容量 */
    private static final int FAR_QUEUE_CAPACITY = 20;
    /** AEC 输出队列容量 */
    private static final int OUTPUT_QUEUE_CAPACITY = 20;

    /** 近端（麦克风）PCM 输入队列 */
    private final LinkedBlockingQueue<byte[]> nearQueue = new LinkedBlockingQueue<>(NEAR_QUEUE_CAPACITY);
    /** 远端（播放器）PCM 参考队列 */
    private final LinkedBlockingQueue<byte[]> farQueue = new LinkedBlockingQueue<>(FAR_QUEUE_CAPACITY);
    /** AEC 处理结果输出队列 */
    private final LinkedBlockingQueue<byte[]> outputQueue = new LinkedBlockingQueue<>(OUTPUT_QUEUE_CAPACITY);

    private volatile boolean stopped = false;
    private Thread aecThread;

    // ===== 统计字段 =====
    private long nearOverflowCount = 0;
    private long farOverflowCount = 0;
    private long outputOverflowCount = 0;
    private long aecFrameCount = 0;
    private long aecTotalCostMs = 0;
    private long aecMaxCostMs = 0;
    private long aecTotalBytes = 0;

    // ===== 远端帧缓冲（用于拼接不对齐的远端数据）=====
    private byte[] farFrameBuffer = null;
    private int farFrameBufferOffset = 0;
    private byte[] lastFarFrame = null;

    /** 获取当前时间戳字符串，格式：HH:mm:ss.SSS */
    private static String ts() {
        return new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(new Date());
    }

    /**
     * 启动 AEC 处理线程
     */
    public void start() {
        stopped = false;
        aecThread = new Thread(this::processLoop, "AecProcessor-Thread");
        aecThread.setDaemon(true);
        aecThread.start();
        Log.i(TAG, "[" + ts() + "][AEC] AEC独立线程已启动");
    }

    /**
     * 停止 AEC 处理线程
     */
    public void stop() {
        stopped = true;
        if (aecThread != null) {
            aecThread.interrupt();
            aecThread = null;
        }
        nearQueue.clear();
        farQueue.clear();
        outputQueue.clear();
        farFrameBuffer = null;
        farFrameBufferOffset = 0;
        lastFarFrame = null;
        Log.i(TAG, "[" + ts() + "][AEC] AEC独立线程已停止"
                + " aecFrames=" + aecFrameCount
                + " totalBytes=" + aecTotalBytes
                + " avgCostMs=" + (aecFrameCount > 0 ? aecTotalCostMs / aecFrameCount : 0)
                + " maxCostMs=" + aecMaxCostMs
                + " nearOverflow=" + nearOverflowCount
                + " farOverflow=" + farOverflowCount
                + " outputOverflow=" + outputOverflowCount);
    }

    /**
     * 投入近端（麦克风）PCM 数据
     * 由采集线程调用，非阻塞
     *
     * @param nearData 近端 PCM 数据，长度必须是 {@link #FRAME_SIZE} 的倍数
     */
    public void putNearData(byte[] nearData) {
        if (nearData == null || nearData.length == 0) return;
        if (!nearQueue.offer(nearData)) {
            nearOverflowCount++;
            Log.w(TAG, "[" + ts() + "][AEC] 近端队列溢出! overflow=" + nearOverflowCount
                    + " queueSize=" + nearQueue.size());
        }
    }

    /**
     * 投入远端（播放器）PCM 参考数据
     * 由播放线程调用，非阻塞
     *
     * @param farData 远端 PCM 数据
     */
    public void putFarData(byte[] farData) {
        if (farData == null || farData.length == 0) return;
        if (!farQueue.offer(farData)) {
            farOverflowCount++;
            Log.w(TAG, "[" + ts() + "][AEC] 远端队列溢出! overflow=" + farOverflowCount
                    + " queueSize=" + farQueue.size());
        }
    }

    /**
     * 从输出队列取出 AEC 处理结果
     * 由编码线程调用，非阻塞
     *
     * @return AEC 处理后的 PCM 数据，队列为空时返回 null
     */
    public byte[] pollOutput() {
        return outputQueue.poll();
    }

    /**
     * 从输出队列取出 AEC 处理结果（带超时）
     * 由编码线程调用
     *
     * @param timeoutMs 超时时间（毫秒）
     * @return AEC 处理后的 PCM 数据，超时或队列为空时返回 null
     */
    public byte[] pollOutput(long timeoutMs) {
        try {
            return outputQueue.poll(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * 获取输出队列当前大小
     */
    public int getOutputQueueSize() {
        return outputQueue.size();
    }

    /**
     * AEC 处理主循环，运行在独立线程中
     */
    private void processLoop() {
        Log.i(TAG, "[" + ts() + "][AEC] processLoop 开始");
        while (!stopped) {
            try {
                // 阻塞等待近端数据（最多 100ms）
                byte[] nearData = nearQueue.poll(100, TimeUnit.MILLISECONDS);
                if (nearData == null) {
                    continue;
                }

                // 近端数据必须是 FRAME_SIZE 的倍数才能送入 GVoice
                if (nearData.length % FRAME_SIZE != 0) {
                    Log.w(TAG, "[" + ts() + "][AEC] 近端数据长度不对齐，跳过 AEC，直接输出"
                            + " len=" + nearData.length);
                    putOutput(nearData);
                    continue;
                }

                // 读取与近端等长的远端参考数据
                byte[] farData = readFarData(nearData.length);

                if (farData == null) {
                    // 远端数据不足，直接输出原始近端数据
                    Log.w(TAG, "[" + ts() + "][AEC] 远端数据不足，跳过 AEC，直接输出"
                            + " nearLen=" + nearData.length);
                    putOutput(nearData);
                    continue;
                }

                // 执行 AEC
                long aecStart = System.currentTimeMillis();
                byte[] aecResult = GvoiceJNIBridge.cancellation(nearData, farData);
                long aecCostMs = System.currentTimeMillis() - aecStart;

                // 更新统计
                aecTotalCostMs += aecCostMs;
                if (aecCostMs > aecMaxCostMs) aecMaxCostMs = aecCostMs;

                byte[] output;
                if (aecResult != null && aecResult.length == nearData.length) {
                    output = aecResult;
                    aecTotalBytes += nearData.length;
                    aecFrameCount++;
                    Log.i(TAG, "[" + ts() + "][AEC] AEC处理完成"
                            + " 本帧=" + nearData.length + "B"
                            + " 累计=" + aecTotalBytes + "B"
                            + " 帧数=" + aecFrameCount
                            + " 本次耗时=" + aecCostMs + "ms"
                            + " 累计耗时=" + aecTotalCostMs + "ms"
                            + " 最大耗时=" + aecMaxCostMs + "ms"
                            + " 近端队列=" + nearQueue.size()
                            + " 输出队列=" + outputQueue.size());
                } else {
                    // AEC 失败，输出原始近端数据
                    output = nearData;
                    Log.w(TAG, "[" + ts() + "][AEC] AEC结果无效，使用原始近端数据"
                            + " aecResult=" + (aecResult == null ? "null" : aecResult.length + "B")
                            + " nearLen=" + nearData.length + "B");
                }

                putOutput(output);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.i(TAG, "[" + ts() + "][AEC] processLoop 被中断，退出");
                break;
            } catch (Exception e) {
                Log.e(TAG, "[" + ts() + "][AEC] processLoop 异常: " + e.getMessage(), e);
            }
        }
        Log.i(TAG, "[" + ts() + "][AEC] processLoop 结束");
    }

    /**
     * 将输出数据放入输出队列
     */
    private void putOutput(byte[] data) {
        if (!outputQueue.offer(data)) {
            outputOverflowCount++;
            Log.w(TAG, "[" + ts() + "][AEC] 输出队列溢出，丢弃最旧帧! overflow=" + outputOverflowCount
                    + " queueSize=" + outputQueue.size());
            // 丢弃最旧的一帧，腾出空间
            outputQueue.poll();
            outputQueue.offer(data);
        }
    }

    /**
     * 从远端队列中读取指定长度的数据（支持帧拼接）
     *
     * @param length 需要读取的字节数，必须是 {@link #FRAME_SIZE} 的倍数
     * @return 远端 PCM 数据，数据不足时返回 null
     */
    private byte[] readFarData(int length) {
        byte[] result = new byte[length];
        int offset = 0;

        // 第一步：从帧缓冲区读取剩余数据
        if (farFrameBuffer != null && farFrameBufferOffset < farFrameBuffer.length) {
            int available = farFrameBuffer.length - farFrameBufferOffset;
            int copyLen = Math.min(available, length);
            System.arraycopy(farFrameBuffer, farFrameBufferOffset, result, offset, copyLen);
            offset += copyLen;
            farFrameBufferOffset += copyLen;
            if (farFrameBufferOffset >= farFrameBuffer.length) {
                farFrameBuffer = null;
                farFrameBufferOffset = 0;
            }
            if (offset >= length) {
                lastFarFrame = Arrays.copyOf(result, result.length);
                return result;
            }
        }

        // 第二步：从队列中读取新帧并拼接
        while (offset < length) {
            byte[] frame = farQueue.poll();
            if (frame == null) {
                // 远端数据不足，用上一帧填充（舒适噪声）
                if (lastFarFrame != null) {
                    while (offset < length) {
                        int remaining = length - offset;
                        int copyLen = Math.min(lastFarFrame.length, remaining);
                        System.arraycopy(lastFarFrame, 0, result, offset, copyLen);
                        offset += copyLen;
                    }
                    Log.d(TAG, "[" + ts() + "][AEC] 远端数据不足，使用上一帧填充"
                            + " farQueueSize=" + farQueue.size());
                    return result;
                }
                // 没有历史帧，返回 null 跳过 AEC
                return null;
            }

            int remaining = length - offset;
            if (frame.length <= remaining) {
                System.arraycopy(frame, 0, result, offset, frame.length);
                offset += frame.length;
            } else {
                System.arraycopy(frame, 0, result, offset, remaining);
                offset += remaining;
                // 将帧的剩余部分存入缓冲区
                int leftover = frame.length - remaining;
                farFrameBuffer = new byte[leftover];
                System.arraycopy(frame, remaining, farFrameBuffer, 0, leftover);
                farFrameBufferOffset = 0;
                break;
            }
        }

        lastFarFrame = Arrays.copyOf(result, result.length);
        return result;
    }
}
