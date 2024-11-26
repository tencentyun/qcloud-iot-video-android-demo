package com.tencent.iotvideo.link.encoder;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.tencent.iotvideo.link.listener.OnEncodeListener;
import com.tencent.iotvideo.link.param.VideoEncodeParam;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class VideoEncoder1 {

    private final String TAG = VideoEncoder1.class.getSimpleName();
    private final VideoEncodeParam videoEncodeParam;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private MediaCodec mediaCodec;
    private OnEncodeListener encoderListener;
    private long seq = 0L;
    private int MAX_BITRATE_LENGTH = 1000000;

    private int MAX_FRAMERATE_LENGTH = 20;
    private int MIN_FRAMERATE_LENGTH = 5;

    private String firstSupportColorFormatCodecName = "";  //  OMX.qcom.video.encoder.avc 和 c2.android.avc.encoder 过滤，这两个h264编码性能好一些。如果都不支持COLOR_FormatYUV420Planar，就用默认的方式。

    private boolean isSupportNV21 = false;

    public VideoEncoder1(VideoEncodeParam param) {
        this.videoEncodeParam = param;
    }

    public void start() {
        try {
            initMediaCodec();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private MediaCodecInfo selectCodec(String mimeType) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            MediaCodecList codecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
            MediaCodecInfo[] codecInfos = codecList.getCodecInfos();
            for (MediaCodecInfo codecInfo : codecInfos) {
                if (!codecInfo.isEncoder()) {
                    continue;
                }
                checkSupportedColorFormats(codecInfo);
                String[] types = codecInfo.getSupportedTypes();
                for (String type : types) {
                    if (type.equalsIgnoreCase(mimeType)) {
                        return codecInfo;
                    }
                }
            }
        }
        return null;
    }

    private int selectColorFormat(MediaCodecInfo codecInfo, String mimeType) {
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
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar: {
                isSupportNV21 = false;
                return true;
            }
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar: {
                isSupportNV21 = true;
                return true;
            }
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
                return true;
            default:
                return false;
        }
    }

    private void checkSupportedColorFormats(MediaCodecInfo codecInfo) {
        if (codecInfo.getName().equals("OMX.qcom.video.encoder.avc") || codecInfo.getName().equals("c2.android.avc.encoder")) {

            String[] supportedTypes = codecInfo.getSupportedTypes();
            for (String type : supportedTypes) {
                if (type.startsWith("video/")) {
                    MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(type);
                    int[] colorFormats = capabilities.colorFormats;
                    for (int colorFormat : colorFormats) {
                        if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar) {
                            Log.d(TAG, "Video encoder: " + codecInfo.getName() + ", supported color format: " + colorFormat);
                            firstSupportColorFormatCodecName = codecInfo.getName();
                            return;
                        }
                    }
                }
            }
        }


    }

    private void initMediaCodec() throws IOException {
        MediaFormat mediaFormat;
        mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, videoEncodeParam.getWidth(), videoEncodeParam.getHeight());
        //描述平均位速率（以位/秒为单位）的键。 关联的值是一个整数
        int bitRate = videoEncodeParam.getBitRate();
        if (bitRate > MAX_BITRATE_LENGTH) {
            bitRate = MAX_BITRATE_LENGTH;
        }
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        //描述视频格式的帧速率（以帧/秒为单位）的键。帧率，一般在15至30之内，太小容易造成视频卡顿。
        int frameRate = videoEncodeParam.getFrameRate();
        if (frameRate > MAX_FRAMERATE_LENGTH) {
            frameRate = MAX_FRAMERATE_LENGTH;
        }
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
        //关键帧间隔时间，单位是秒
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, videoEncodeParam.getiFrameInterval());
        //色彩格式，具体查看相关API，不同设备支持的色彩格式不尽相同
        // 查找支持的颜色格式
        MediaCodecInfo codecInfo = selectCodec(MediaFormat.MIMETYPE_VIDEO_AVC);
        if (codecInfo == null) {
            Log.e(TAG, "No suitable codec found for MIME type: " + MediaFormat.MIMETYPE_VIDEO_AVC);
            return;
        }
        int colorFormat = selectColorFormat(codecInfo, MediaFormat.MIMETYPE_VIDEO_AVC);

        if (isSupportNV21) {
            //色彩格式，具体查看相关API，不同设备支持的色彩格式不尽相同
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
        } else {
            //色彩格式，具体查看相关API，不同设备支持的色彩格式不尽相同
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat);
        }
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
        if (!firstSupportColorFormatCodecName.isEmpty()) {
            mediaCodec = MediaCodec.createByCodecName(firstSupportColorFormatCodecName);
        } else {
            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        }
        mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mediaCodec.start();
    }

    //描述平均位速率（以位/秒为单位）的键。 关联的值是一个整数
    public void setVideoBitRate(int bitRate) {
        int nowBitrate = videoEncodeParam.getBitRate();
        if ((bitRate < 10000) || (nowBitrate == bitRate) || (bitRate > MAX_BITRATE_LENGTH)) {
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

    /**
     * 将NV21编码成H264
     */
    public void encoderH264(byte[] data, boolean mirror) {
        if (executor.isShutdown()) return;
        executor.submit(() -> {
            byte[] nv21 = data;
            if (mirror) {
                nv21 = rotateNV21Data180(data, videoEncodeParam.getWidth(), videoEncodeParam.getHeight());
            }
            byte[] readyToProcessBytes;
            if (isSupportNV21) {
                readyToProcessBytes = convertNV21ToNV12(nv21, videoEncodeParam.getWidth(), videoEncodeParam.getHeight());
            } else {
                readyToProcessBytes = convertNV21ToYUV420(nv21, videoEncodeParam.getWidth(), videoEncodeParam.getHeight());
            }
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
     * 旋转90，270，要进行宽高对调
     *
     * @param nv21Data
     * @param width
     * @param height
     * @return
     */
    public static byte[] rotateNV21Data270(byte[] nv21Data, int width, int height) {
        int frameSize = width * height;
        int bufferSize = frameSize * 3 / 2;
        byte[] nv21Rotated = new byte[bufferSize];
        int i = 0;

        // Rotate the Y luma
        for (int x = width - 1; x >= 0; x--) {
            int offset = 0;
            for (int y = 0; y < height; y++) {
                nv21Rotated[i] = nv21Data[offset + x];
                i++;
                offset += width;
            }
        }

        // Rotate the U and V color components
        i = frameSize;
        for (int x = width - 1; x > 0; x = x - 2) {
            int offset = frameSize;
            for (int y = 0; y < height / 2; y++) {
                nv21Rotated[i] = nv21Data[offset + (x - 1)];
                i++;
                nv21Rotated[i] = nv21Data[offset + x];
                i++;
                offset += width;
            }
        }
        return nv21Rotated;
    }

    /**
     * 旋转90，270，要进行宽高对调
     *
     * @param nv21Data
     * @param width
     * @param height
     * @return
     */
    public byte[] rotateNV21Data90(byte[] nv21Data, int width, int height) {
        int frameSize = width * height;
        int bufferSize = frameSize * 3 / 2;
        byte[] nv21Rotated = new byte[bufferSize];
        // Rotate the Y luma
        int i = 0;
        int startPos = (height - 1) * width;
        for (int x = 0; x < width; x++) {
            int offset = startPos;
            for (int y = height - 1; y >= 0; y--) {
                nv21Rotated[i] = nv21Data[offset + x];
                i++;
                offset -= width;
            }
        }

        // Rotate the U and V color components
        i = bufferSize - 1;
        for (int x = width - 1; x > 0; x = x - 2) {
            int offset = frameSize;
            for (int y = 0; y < height / 2; y++) {
                nv21Rotated[i] = nv21Data[offset + x];
                i--;
                nv21Rotated[i] = nv21Data[offset + (x - 1)];
                i--;
                offset += width;
            }
        }
        return nv21Rotated;
    }

    public byte[] rotateNV21Data180(byte[] nv21Data, int width, int height) {
        int frameSize = width * height;
        int bufferSize = frameSize * 3 / 2;
        byte[] nv21Rotated = new byte[bufferSize];
        int count = 0;

        for (int i = frameSize - 1; i >= 0; i--) {
            nv21Rotated[count] = nv21Data[i];
            count++;
        }

        for (int i = bufferSize - 1; i >= frameSize; i -= 2) {
            nv21Rotated[count++] = nv21Data[i - 1];
            nv21Rotated[count++] = nv21Data[i];
        }
        return nv21Rotated;
    }

    private byte[] convertNV21ToNV12(byte[] nv21, int width, int height) {
        byte[] nv12 = new byte[width * height * 3 / 2];
        int frameSize = width * height;
        int i, j;
        System.arraycopy(nv21, 0, nv12, 0, frameSize);
        for (i = 0; i < frameSize; i++) {
            nv12[i] = nv21[i];
        }
        for (j = 0; j < frameSize / 2; j += 2) {
            nv12[frameSize + j - 1] = nv21[j + frameSize];
        }
        for (j = 0; j < frameSize / 2; j += 2) {
            nv12[frameSize + j] = nv21[j + frameSize - 1];
        }
        nv21 = null;
        return nv12;
    }

    private byte[] convertNV21ToYUV420(byte[] nv21, int width, int height) {
        int frameSize = width * height;
        byte[] yuv420 = new byte[frameSize * 3 / 2];
        // Copy Y values
        System.arraycopy(nv21, 0, yuv420, 0, frameSize);
        // Copy U and V values
        for (int i = 0; i < height / 2; i++) {
            for (int j = 0; j < width / 2; j++) {
                yuv420[frameSize + i * width / 2 + j] = nv21[frameSize + 2 * (i * width / 2 + j) + 1]; // U
                yuv420[frameSize + frameSize / 4 + i * width / 2 + j] = nv21[frameSize + 2 * (i * width / 2 + j)]; // V
            }
        }
        return yuv420;
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
