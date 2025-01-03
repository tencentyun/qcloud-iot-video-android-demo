package com.tencent.iotvideo.link.encoder;

import static com.tencent.iotvideo.link.util.UtilsKt.getBitRateIntervalByPixel;

import android.graphics.ImageFormat;
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
import com.tencent.iotvideo.link.util.CodeUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class VideoEncoder {

    private final String TAG = VideoEncoder.class.getSimpleName();
    private static final int OMX_QCOM_COLOR_FORMAT = 0x7FA30C00;// 高通特定的 YUV 4:2:0 打包半平面格式
    private static final int OMX_MTK_COLOR_FORMAT = 0x7F000200;//联发科特定的 YUV 4:2:0 YV12 格式。
    private static final int OMX_HISI_COLOR_FORMAT = 0x7F000789;//海思特定的 YUV 4:2:0 YV12 格式。

    //常见的h264硬件编码器名称
    private final List<String> encoderList = Arrays.asList("OMX.qcom.video.encoder.avc", "OMX.MTK.VIDEO.ENCODER.AVC", "OMX.hisi.video.encoder.avc", "c2.android.avc.encoder");
    private MediaCodecInfo mediaCodecInfo;
    private int colorFormat;
    private final VideoEncodeParam videoEncodeParam;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private MediaCodec mediaCodec;
    private OnEncodeListener encoderListener;
    private Range<Double> bitRateInterval;
    private long seq = 0L;
    private int MAX_FRAMERATE_LENGTH = 20;
    private int MIN_FRAMERATE_LENGTH = 5;

    public VideoEncoder(VideoEncodeParam param) {
        this.videoEncodeParam = param;
        mediaCodecInfo = getMediaCodecInfo();
        if (mediaCodecInfo != null) {
            colorFormat = getColorFormat(mediaCodecInfo, MediaFormat.MIMETYPE_VIDEO_AVC);
        }
    }

    public void start() {
        try {
            bitRateInterval = getBitRateIntervalByPixel(videoEncodeParam.getWidth(), videoEncodeParam.getHeight());
            initMediaCodec();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 此方法优先获取设备硬件编码器信息
     *
     * @return
     */
    private MediaCodecInfo getMediaCodecInfo() {
        MediaCodecInfo resInfo = videoEncodeParam.getCodecInfo();
        String[] types = resInfo.getSupportedTypes();
        for (String type : types) {
            if (!type.startsWith("video/")) continue;
            MediaCodecInfo.CodecCapabilities capabilities = resInfo.getCapabilitiesForType(type);
            Log.d(TAG, "using encoder name:" + resInfo.getName() + "  support colorFormats:" + Arrays.toString(capabilities.colorFormats));
        }
        return resInfo;
    }

    private void initMediaCodec() throws IOException {
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, videoEncodeParam.getWidth(), videoEncodeParam.getHeight());
        //描述平均位速率（以位/秒为单位）的键。 关联的值是一个整数
        int bitRate = videoEncodeParam.getBitRate();
        if (bitRateInterval.getLower() > bitRate || bitRateInterval.getUpper() < bitRate) {
            bitRate = (int) (bitRateInterval.getUpper() * 0.8);
            videoEncodeParam.setBitRate(bitRate);
        }
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        //描述视频格式的帧速率（以帧/秒为单位）的键。帧率，一般在15至30之内，太小容易造成视频卡顿。
        int frameRate = videoEncodeParam.getFrameRate();
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
        //关键帧间隔时间，单位是秒
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, videoEncodeParam.getiFrameInterval());
        //色彩格式，具体查看相关API，不同设备支持的色彩格式不尽相同
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, getColorFormat());
        //设置编码器码率模式为可变
        mediaFormat.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mediaFormat.setInteger(MediaFormat.KEY_ROTATION, 0);
        }
        //设置压缩等级  默认是 baseline
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mediaFormat.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel3);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediaFormat.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileMain);
        }
        // 创建 MediaCodec 编码器
        if (mediaCodecInfo != null) {
            mediaCodec = MediaCodec.createByCodecName(mediaCodecInfo.getName());
        } else {
            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        }
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
            byte[] readyToProcessBytes = convertData(data);
            // 获取输入缓冲区
            ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
            ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();

            int inputBufferIndex = mediaCodec.dequeueInputBuffer(-1);
            if (inputBufferIndex >= 0) {
                ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                inputBuffer.clear();
                inputBuffer.put(readyToProcessBytes);

                // 将数据传递给编码器
                mediaCodec.queueInputBuffer(inputBufferIndex, 0, readyToProcessBytes.length, System.nanoTime() / 1000, 0);
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

    private byte[] convertData(byte[] data) {
        if ("OMX.MTK.VIDEO.ENCODER.AVC".equals(mediaCodecInfo.getName()) || "OMX.hisi.video.encoder.avc".equals(mediaCodecInfo.getName()) || "OMX.qcom.video.encoder.avc".equals(mediaCodecInfo.getName())) {
            return data;
        } else if (isSupportNV21()) {
            return CodeUtils.INSTANCE.convertNV21ToNV12(data, videoEncodeParam.getWidth(), videoEncodeParam.getHeight());
        } else
            return CodeUtils.INSTANCE.convertNV21ToYUV420(data, videoEncodeParam.getWidth(), videoEncodeParam.getHeight());
    }

    /**
     * 设置编码成功后数据回调
     */
    public void setEncoderListener(OnEncodeListener listener) {
        encoderListener = listener;
    }

    public void stop() {
        try {
            if (mediaCodec != null) {
                mediaCodec.stop();
                mediaCodec.release();
                mediaCodec = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        executor.shutdown();
    }

    /**
     * 查找支持的colorformat
     *
     * @return
     */
    private int getColorFormat() {
        if ("OMX.qcom.video.encoder.avc".equals(mediaCodecInfo.getName())) {
            return OMX_QCOM_COLOR_FORMAT;
        } else if ("OMX.MTK.VIDEO.ENCODER.AVC".equals(mediaCodecInfo.getName())) {
            return OMX_MTK_COLOR_FORMAT;
        } else if ("OMX.hisi.video.encoder.avc".equals(mediaCodecInfo.getName())) {
            return OMX_HISI_COLOR_FORMAT;
        } else if (isSupportNV21()) {
            return MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;
        } else if (isSupportYV12()) {
            return MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar;
        } else {
            return colorFormat;
        }
    }

    /**
     * 获取编码器支持的格式
     *
     * @return
     */
    public int getFormat() {
        if ("OMX.MTK.VIDEO.ENCODER.AVC".equals(mediaCodecInfo.getName())) {
            return ImageFormat.YV12;
        } else if ("OMX.hisi.video.encoder.avc".equals(mediaCodecInfo.getName()) || "OMX.qcom.video.encoder.avc".equals(mediaCodecInfo.getName()) || isSupportNV21()) {
            return ImageFormat.NV21;
        } else if (isSupportYV12()) {
            return ImageFormat.YV12;
        }
        Log.d(TAG, "no get ImageFormat");
        return ImageFormat.NV21;
    }

    /**
     * 是否支持nv21
     *
     * @return
     */
    public boolean isSupportNV21() {
        return isColorFormatSupported(mediaCodecInfo, MediaFormat.MIMETYPE_VIDEO_AVC, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
    }

    /**
     * 是否支持yv12
     *
     * @return
     */
    public boolean isSupportYV12() {
        return isColorFormatSupported(mediaCodecInfo, MediaFormat.MIMETYPE_VIDEO_AVC, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar);
    }

    private boolean isColorFormatSupported(MediaCodecInfo codecInfo, String mimeType, int colorFormat) {
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(mimeType);
        for (int format : capabilities.colorFormats) {
            if (format == colorFormat) return true;
        }
        return false;
    }

    private int getColorFormat(MediaCodecInfo codecInfo, String mimeType) {
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(mimeType);
        for (int i = 0; i < capabilities.colorFormats.length; i++) {
            int colorFormat = capabilities.colorFormats[i];
            if (isRecognizedFormat(colorFormat)) {
                return colorFormat;
            }
        }
        return 0; // No suitable color format found
    }

    private boolean isRecognizedFormat(int colorFormat) {
        switch (colorFormat) {
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
            case OMX_QCOM_COLOR_FORMAT: // OMX_QCOM_COLOR_FormatYUV420PackedSemiPlanar32m
            case OMX_MTK_COLOR_FORMAT: // OMX_MTK_COLOR_FormatYV12
                return true;
            default:
                return false;
        }
    }
}
