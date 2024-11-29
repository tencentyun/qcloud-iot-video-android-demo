package com.tencent.iotvideo.link.decoder;

import static com.tencent.iotvideo.link.util.UtilsKt.getFile;

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
    private MediaCodec mVideoCodec;
    private ExecutorService mVideoExecutor;
    private long currentVideoPts = 0;
    private final ExecutorService decoderH264executor = Executors.newSingleThreadExecutor();
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
        synchronized (codecLock) {
            if (mVideoCodec != null) {
                mVideoCodec.stop();
                mVideoCodec.release();
                mVideoCodec = null;
            }
            if (mVideoExecutor != null && !mVideoExecutor.isShutdown()) {
                mVideoExecutor.shutdownNow();
            }
            initVideo(width, height, surface);
        }
    }

    private void initVideo(int width, int height, Surface surface) throws IOException {
        mVideoExecutor = Executors.newSingleThreadExecutor();
        mVideoCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        MediaFormat mFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
        mFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 1024 * 1024);
        mFormat.setInteger(MediaFormat.KEY_ROTATION, 0);
        mFormat.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline);
        mFormat.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel31);
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
    }

    public int decoderH264(byte[] data, int len, long pts) {
        currentVideoPts = pts;
        if (mVideoExecutor == null || mVideoExecutor.isShutdown()) return -1;
        if (mVideoCodec == null) return -2;

        mVideoExecutor.submit(() -> {
            try {
                ByteBuffer[] inputBuffers = mVideoCodec.getInputBuffers();
                // queue and decode
                int inputBufferIndex = mVideoCodec.dequeueInputBuffer(-1);
                if (inputBufferIndex >= 0) {
                    ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                    inputBuffer.clear();
                    inputBuffer.put(data, 0, len);
                    mVideoCodec.queueInputBuffer(inputBufferIndex, 0, len, 0, 0);
                } else {
                    Log.e(TAG, "video inputBufferIndex invalid: " + inputBufferIndex);
                }

                // dequeue and render
                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                int outputBufferIndex = mVideoCodec.dequeueOutputBuffer(bufferInfo, 0);
                while (outputBufferIndex >= 0) {
                    mVideoCodec.releaseOutputBuffer(outputBufferIndex, true);
                    outputBufferIndex = mVideoCodec.dequeueOutputBuffer(bufferInfo, 0);
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        });
        if (isRecord) {
            saveRawDataStream(data);
        }
        return 0;
    }

    private void saveRawDataStream(byte[] data) {
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
        if (mVideoExecutor != null) {
            mVideoExecutor.shutdown();
            mVideoExecutor = null;
        }

        if (mVideoCodec != null) {
            mVideoCodec.stop();
            mVideoCodec.release();
            mVideoCodec = null;
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
