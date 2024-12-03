package com.tencent.iotvideo.link.encoder;

import static com.tencent.iotvideo.link.util.UtilsKt.getBitRateIntervalByPixel;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Range;

import com.tencent.iotvideo.link.listener.OnEncodeListener;
import com.tencent.iotvideo.link.param.VideoEncodeParam;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class VideoEncoder {

    private final String TAG = VideoEncoder.class.getSimpleName();
    private final String CODEC_NAME = "OMX.MTK.VIDEO.ENCODER.AVC";
    private final int COLOR_FORMAT = 0x7F000200;
    private final VideoEncodeParam videoEncodeParam;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private MediaCodec mediaCodec;
    private OnEncodeListener encoderListener;
    private Range<Double> bitRateInterval;
    private long seq = 0L;
    private final int MAX_FRAMERATE_LENGTH = 20;
    private final int MIN_FRAMERATE_LENGTH = 5;

    public VideoEncoder(VideoEncodeParam param) {
        this.videoEncodeParam = param;
    }

    public void start() {
        try {
            bitRateInterval = getBitRateIntervalByPixel(videoEncodeParam.getWidth(), videoEncodeParam.getHeight());
            initMediaCodec();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private MediaCodecInfo selectCodec() {
        MediaCodecList codecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
        MediaCodecInfo[] codecInfos = codecList.getCodecInfos();
        for (MediaCodecInfo codecInfo : codecInfos) {
            if (!codecInfo.isEncoder()) continue;
            String[] types = codecInfo.getSupportedTypes();
            for (String type : types) {
                Log.d(TAG, "Encoder name: " + codecInfo.getName() + ", type: " + type);
                if (type.equalsIgnoreCase(MediaFormat.MIMETYPE_VIDEO_AVC)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }


    private void initMediaCodec() throws IOException {
        MediaCodecInfo codecInfo = selectCodec();
        if (codecInfo == null) {
            Log.e(TAG, "No suitable codec found for MIME type: " + MediaFormat.MIMETYPE_VIDEO_AVC);
            return;
        }
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, videoEncodeParam.getWidth(), videoEncodeParam.getHeight());
        //描述平均位速率（以位/秒为单位）的键。 关联的值是一个整数
        int bitRate = videoEncodeParam.getBitRate();
        if (bitRateInterval.getLower() > bitRate || bitRateInterval.getUpper() < bitRate) {
            bitRate = (int) ((bitRateInterval.getUpper() + bitRateInterval.getLower()) / 2);
            videoEncodeParam.setBitRate(bitRate);
        }
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        //描述视频格式的帧速率（以帧/秒为单位）的键。帧率，一般在15至30之内，太小容易造成视频卡顿。
        int frameRate = videoEncodeParam.getFrameRate();
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
        //关键帧间隔时间，单位是秒
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, videoEncodeParam.getiFrameInterval());
        //色彩格式，具体查看相关API，不同设备支持的色彩格式不尽相同
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, COLOR_FORMAT);
        //设置编码器码率模式为可变
        mediaFormat.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
        mediaFormat.setInteger(MediaFormat.KEY_ROTATION, 90);
        //设置压缩等级  默认是 baseline
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mediaFormat.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel3);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediaFormat.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileMain);
        }
        // 创建 MediaCodec 编码器
        mediaCodec = MediaCodec.createByCodecName(CODEC_NAME);
        mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mediaCodec.start();
    }

    //描述平均位速率（以位/秒为单位）的键。 关联的值是一个整数
    public void setVideoBitRate(int bitRate) {
        int nowBitrate = videoEncodeParam.getBitRate();
        if (bitRateInterval.getLower() > bitRate || nowBitrate == bitRate || bitRateInterval.getUpper() < bitRate) {
            return;
        }
        videoEncodeParam.setBitRate(bitRate);
        Bundle params = new Bundle();
        params.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, bitRate);
        mediaCodec.setParameters(params);
    }

    public int getVideoBitRate() {
        return videoEncodeParam.getBitRate();
    }

    public void setVideoFrameRate(int frameRate) {
        int nowFrameRate = videoEncodeParam.getFrameRate();
        if ((frameRate < MIN_FRAMERATE_LENGTH) || (nowFrameRate == frameRate) || (frameRate > MAX_FRAMERATE_LENGTH)) {
            return;
        }
        videoEncodeParam.setFrameRate(frameRate);
        Bundle params = new Bundle();
        params.putInt(MediaFormat.KEY_FRAME_RATE, frameRate);
        mediaCodec.setParameters(params);
    }

    public int getVideoFrameRate() {
        return videoEncodeParam.getFrameRate();
    }

    public Range<Double> getBitRateInterval() {
        return bitRateInterval;
    }

    /**
     * 将NV21编码成H264
     */
    public void encoderH264(byte[] data, boolean mirror) {
        if (executor.isShutdown()) return;
        executor.submit(() -> {
            // 获取输入缓冲区
            ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
            ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();

            int inputBufferIndex = mediaCodec.dequeueInputBuffer(-1);
            if (inputBufferIndex >= 0) {
                ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                inputBuffer.clear();
                inputBuffer.put(data);

                // 将数据传递给编码器
                mediaCodec.queueInputBuffer(inputBufferIndex, 0, data.length, System.nanoTime() / 1000, 0);
            }

            // 获取输出缓冲区
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);

            while (outputBufferIndex >= 0) {
                ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];

                // 处理编码后的数据
                byte[] outData = new byte[bufferInfo.size];
                outputBuffer.get(outData);
                // 打印编码后的数据大小
                Log.d(TAG, "Encoded data size: " + outData.length);
                if (encoderListener != null) {
                    encoderListener.onVideoEncoded(outData, System.currentTimeMillis(), seq, true);
                    seq++;
                }

                // 释放输出缓冲区
                mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
            }
        });
    }

    /**
     * 设置编码成功后数据回调
     */
    public void setEncoderListener(OnEncodeListener listener) {
        encoderListener = listener;
    }

    public void stop() {
        executor.shutdown();
    }
}
