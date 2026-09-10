package com.tencent.iotvideo.link.decoder;

import static com.tencent.iotvideo.link.util.UtilsKt.getFile;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VideoDecoder {
    private static final String TAG = "VideoDecoder";

    private final Object codecLock = new Object();
    /** 保证 start/stop 串行化，避免并发导致 codec 双重 release */
    private final Object lifecycleLock = new Object();
    private volatile boolean running = false;
    private MediaCodec mVideoCodec;
    private ExecutorService mVideoExecutor;
    private long currentVideoPts = 0;
    private ExecutorService decoderH264executor = Executors.newSingleThreadExecutor();
    private String decoderH264FilePath = "/sdcard/videoDecoder.h264";
    private FileOutputStream fos;

    private boolean isRecord = false;

    public void setRecord(boolean isRecord) {
        this.isRecord = isRecord;
        recordDecoderH264();
    }

    public long getCurrentVideoPts() {
        return currentVideoPts;
    }

    public void startVideo(int width, int height, Surface surface) throws IOException {
        synchronized (lifecycleLock) {
            // 先完整地 stop（含 executor 终止等待），避免旧线程任务与新 codec 交叉
            stopVideoLocked();
            initVideo(width, height, surface);
        }
    }

    @SuppressLint("WrongConstant")
    private void initVideo(int width, int height, Surface surface) throws IOException {
        mVideoExecutor = Executors.newSingleThreadExecutor();
        mVideoCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        MediaFormat mFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
        mFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 1024 * 1024);
        mFormat.setInteger(MediaFormat.KEY_ROTATION, 0);
        mFormat.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline);
        mFormat.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel31);
        mFormat.setInteger(MediaFormat.KEY_PRIORITY, 0); // 设置低优先级
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            mFormat.setInteger(MediaFormat.KEY_LOW_LATENCY, 1); // 启用低延迟模式
//        }
        String model = Build.MODEL;
        if (model.contains("KONKA") && model.contains("9652") || model.contains("KONKA") && model.contains("9653") || model.contains("XY01")) { // 康佳 MTK的一个SoC 型号
            mFormat.setInteger("low-latency", 1);
            Log.d(TAG, "qudiao mFormat set low-latency 1" + ", model:" + Build.MODEL);
        }

        if (model.contains("KONKA") && model.contains("V811")) { // 康佳 海思的一个SoC 型号
            mVideoCodec.configure(mFormat, surface, null, 0x2);
            Log.d(TAG, "mVideoCodec configure flags 0x2" + ", model:" + Build.MODEL);
        } else {
            mVideoCodec.configure(mFormat, surface, null, 0);
            Log.d(TAG, "mVideoCodec configure flags 0" + ", model:" + Build.MODEL);
        }
        mVideoCodec.start();
        running = true;
    }

    public int decoderH264(byte[] data, int len, long pts) {
        currentVideoPts = pts;
        // 快照，避免与 stopVideo 并发时读到 null
        final ExecutorService executor = mVideoExecutor;
        if (executor == null || executor.isShutdown()) return -1;
        if (!running) return -2;

        try {
            executor.submit(() -> {
                synchronized (codecLock) {
                    if (!running || mVideoCodec == null) {
                        return;
                    }
                    try {
                        ByteBuffer[] inputBuffers = mVideoCodec.getInputBuffers();
                        // queue and decode
                        int inputBufferIndex = mVideoCodec.dequeueInputBuffer(10000);
                        if (inputBufferIndex >= 0) {
                            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                            inputBuffer.clear();
                            inputBuffer.put(data, 0, len);
                            mVideoCodec.queueInputBuffer(inputBufferIndex, 0, len, pts * 1000, 0);
                        } else {
                            Log.e(TAG, "video inputBufferIndex invalid: " + inputBufferIndex);
                        }

                        // dequeue and render
                        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                        int outputBufferIndex = mVideoCodec.dequeueOutputBuffer(bufferInfo, 10000);
                        while (outputBufferIndex >= 0) {
                            if (!running || mVideoCodec == null) break;
                            mVideoCodec.releaseOutputBuffer(outputBufferIndex, true);
                            if (!running || mVideoCodec == null) break;
                            outputBufferIndex = mVideoCodec.dequeueOutputBuffer(bufferInfo, 10000);
                        }
                    } catch (IllegalStateException ise) {
                        Log.w(TAG, "video codec state invalid: " + ise.getMessage());
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            });
        } catch (java.util.concurrent.RejectedExecutionException e) {
            // executor 已在两步检查之间被关闭，忽略即可
            return -1;
        }
        if (isRecord) {
            saveRawDataStream(data);
        }
        return 0;
    }

    private void saveRawDataStream(byte[] data) {
        if (decoderH264executor.isShutdown()){
            decoderH264executor = Executors.newSingleThreadExecutor();
        }
        decoderH264executor.submit(() -> {
            if (fos != null) {
                try {
                    fos.write(data);
                    fos.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void stopVideo() {
        synchronized (lifecycleLock) {
            stopVideoLocked();
        }
    }

    /**
     * 实际的停止逻辑，调用方需已持有 lifecycleLock。
     * <p>
     * 顺序：先 running=false → shutdownNow executor 并 awaitTermination →
     * 再在 codecLock 内 stop+release codec。保证旧任务完全退出后才 release codec，
     * 避免 native use-after-free（RefBase::decStrong）。
     */
    private void stopVideoLocked() {
        running = false;

        // 1) 先终止 executor，并等待旧任务退出（它们可能正在持 codecLock 操作 codec）
        ExecutorService executor = mVideoExecutor;
        mVideoExecutor = null;
        if (executor != null) {
            executor.shutdownNow();
            try {
                executor.awaitTermination(1000, java.util.concurrent.TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // 2) executor 已终止，现在安全地 release codec
        synchronized (codecLock) {
            if (mVideoCodec != null) {
                try {
                    mVideoCodec.stop();
                } catch (Exception ignore) {
                }
                try {
                    mVideoCodec.release();
                } catch (Exception ignore) {
                }
                mVideoCodec = null;
            }
        }

        // 3) 顺带关闭录制 executor 与文件流，避免线程与句柄泄漏
        if (decoderH264executor != null && !decoderH264executor.isShutdown()) {
            decoderH264executor.shutdownNow();
        }
        if (fos != null) {
            try {
                fos.close();
            } catch (IOException ignore) {
            }
            fos = null;
        }
    }

    private void recordDecoderH264() {
        if (!TextUtils.isEmpty(decoderH264FilePath) && isRecord) {
            try {
                File file = getFile(decoderH264FilePath);
                fos = new FileOutputStream(file);
            } catch (Exception e) {
                Log.e(TAG, decoderH264FilePath + "临时缓存文件未找到");
                e.printStackTrace();
            }
        }
    }
}
