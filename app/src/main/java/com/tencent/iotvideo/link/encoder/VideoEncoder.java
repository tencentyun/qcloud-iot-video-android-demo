package com.tencent.iotvideo.link.encoder;

import android.annotation.TargetApi;
import android.app.Activity;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;

import com.tencent.iotvideo.link.listener.OnEncodeListener;
import com.tencent.iotvideo.link.param.VideoEncodeParam;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class VideoEncoder ***REMOVED***

    private final VideoEncodeParam videoEncodeParam;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private MediaCodec mediaCodec;
    private MediaFormat mediaFormat;
    private OnEncodeListener encoderListener;
    private long seq = 0L;
    private int MAX_BITRATE_LENGTH = 5500000;
    private int MIN_BITRATE_LENGTH = 1000000;
    private int beginBitRate = 0;
    private int MAX_FRAMERATE_LENGTH = 20;
    private int MIN_FRAMERATE_LENGTH = 5;
    private int beginFrameRate = 0;
    private boolean isPad = false;

    public VideoEncoder(VideoEncodeParam param, Activity activity) ***REMOVED***
        if (activity != null) ***REMOVED***
            DisplayMetrics displayMetrics = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int height = displayMetrics.heightPixels;
            int width = displayMetrics.widthPixels;
            Log.e("TAG", "height: " + height + ", width: " + width);
            isPad = !(height > width);
      ***REMOVED***
        this.videoEncodeParam = param;
        initMediaCodec();
  ***REMOVED***

    private void initMediaCodec() ***REMOVED***
        try ***REMOVED***
            mediaCodec = MediaCodec.createEncoderByType("video/avc");
            //height和width一般都是照相机的height和width。
            //TODO 因为获取到的视频帧数据是逆时针旋转了90度的，所以这里宽高需要对调
            MediaFormat mediaFormat;
            if (isPad) ***REMOVED***
                mediaFormat = MediaFormat.createVideoFormat("video/avc", videoEncodeParam.getHeight(), videoEncodeParam.getWidth());
          ***REMOVED*** else ***REMOVED***
                mediaFormat = MediaFormat.createVideoFormat("video/avc", videoEncodeParam.getWidth(), videoEncodeParam.getHeight());
          ***REMOVED***
            //描述平均位速率（以位/秒为单位）的键。 关联的值是一个整数
            int bitRate = videoEncodeParam.getBitRate();
            if (bitRate > MAX_BITRATE_LENGTH) ***REMOVED***
                bitRate = MAX_BITRATE_LENGTH;
          ***REMOVED***
            beginBitRate = bitRate;
            int frameRate = videoEncodeParam.getFrameRate();
            if (frameRate > MAX_FRAMERATE_LENGTH) ***REMOVED***
                frameRate = MAX_FRAMERATE_LENGTH;
          ***REMOVED***
            beginFrameRate = frameRate;
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
            //描述视频格式的帧速率（以帧/秒为单位）的键。帧率，一般在15至30之内，太小容易造成视频卡顿。
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
            //色彩格式，具体查看相关API，不同设备支持的色彩格式不尽相同
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
            //关键帧间隔时间，单位是秒
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, videoEncodeParam.getiFrameInterval());
            mediaFormat.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
            //设置压缩等级  默认是 baseline
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) ***REMOVED***
                mediaFormat.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel3);
          ***REMOVED*** else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) ***REMOVED***
                mediaFormat.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileMain);
          ***REMOVED***
            mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            //开始编码
            mediaCodec.start();
      ***REMOVED*** catch (Exception e) ***REMOVED***
            e.printStackTrace();
      ***REMOVED***
  ***REMOVED***

    //描述平均位速率（以位/秒为单位）的键。 关联的值是一个整数
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void setVideoBitRate(int bitRate) ***REMOVED***
        int nowBitrate = videoEncodeParam.getBitRate();

        if ((bitRate > beginBitRate) || (bitRate < MIN_BITRATE_LENGTH) || (nowBitrate == bitRate) || (bitRate > MAX_BITRATE_LENGTH)) ***REMOVED***
            return;
      ***REMOVED***

        videoEncodeParam.setBitRate(bitRate);

        try ***REMOVED***
            Bundle params = new Bundle();
            params.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, bitRate);
            mediaCodec.setParameters(params);

      ***REMOVED*** catch (IllegalStateException e) ***REMOVED***
            Log.e("TAG", "updateBitrate failed", e);
      ***REMOVED***
  ***REMOVED***

    public int getVideoBitRate() ***REMOVED***
        return videoEncodeParam.getBitRate();
  ***REMOVED***

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void setVideoFrameRate(int frameRate) ***REMOVED***
        int nowFrameRate = videoEncodeParam.getFrameRate();

        if ((frameRate > beginFrameRate) || (frameRate < MIN_FRAMERATE_LENGTH) || (nowFrameRate == frameRate) || (frameRate > MAX_FRAMERATE_LENGTH)) ***REMOVED***
            return;
      ***REMOVED***

        videoEncodeParam.setFrameRate(frameRate);

        try ***REMOVED***
            Bundle params = new Bundle();
            params.putInt(MediaFormat.KEY_FRAME_RATE, frameRate);
            mediaCodec.setParameters(params);

      ***REMOVED*** catch (IllegalStateException e) ***REMOVED***
            Log.e("TAG", "updateFramerate failed", e);
      ***REMOVED***
  ***REMOVED***

    public int getVideoFrameRate() ***REMOVED***
        return videoEncodeParam.getFrameRate();
  ***REMOVED***
    /**
     * 将NV21编码成H264
     */
    public void encoderH264(byte[] data, boolean mirror) ***REMOVED***
        if (executor.isShutdown()) return;
        executor.submit(() -> ***REMOVED***
            //将NV21编码成NV12
            byte[] bytes = NV21ToNV12(data, videoEncodeParam.getWidth(), videoEncodeParam.getHeight());
            //视频顺时针旋转90度
            byte[] nv12 ;
            if (isPad) ***REMOVED***
                nv12 = rotateNV270(bytes, videoEncodeParam.getWidth(), videoEncodeParam.getHeight());
          ***REMOVED*** else ***REMOVED***
                nv12 = bytes;
          ***REMOVED***

            if (mirror) ***REMOVED***
                verticalMirror(nv12, videoEncodeParam.getHeight(), videoEncodeParam.getWidth());
          ***REMOVED***

            try ***REMOVED***
                //拿到输入缓冲区,用于传送数据进行编码
                ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
                //拿到输出缓冲区,用于取到编码后的数据
                ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();
                int inputBufferIndex = mediaCodec.dequeueInputBuffer(-1);
                //当输入缓冲区有效时,就是>=0
                if (inputBufferIndex >= 0) ***REMOVED***
                    ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                    inputBuffer.clear();
                    //往输入缓冲区写入数据
                    inputBuffer.put(nv12);
                    //五个参数，第一个是输入缓冲区的索引，第二个数据是输入缓冲区起始索引，第三个是放入的数据大小，第四个是时间戳，保证递增就是
                    mediaCodec.queueInputBuffer(inputBufferIndex, 0, nv12.length, System.nanoTime() / 1000, 0);
              ***REMOVED***
                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                //拿到输出缓冲区的索引
                int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
                while (outputBufferIndex >= 0) ***REMOVED***
                    ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];

                    outputBuffer.position(bufferInfo.offset);
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size);

                    byte[] outData = new byte[outputBuffer.remaining()];
                    outputBuffer.get(outData, 0, outData.length);

                    if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME) ***REMOVED***
                        // I 帧的处理逻辑
//                        Log.i("TAG", "==========I帧==============="+seq);
                        ByteBuffer spsb = mediaCodec.getOutputFormat().getByteBuffer("csd-0");
                        byte[] sps = new byte[spsb.remaining()];
                        spsb.get(sps, 0, sps.length);
                        ByteBuffer ppsb = mediaCodec.getOutputFormat().getByteBuffer("csd-1");
                        byte[] pps = new byte[ppsb.remaining()];
                        ppsb.get(pps, 0, pps.length);

                        byte[] dataBytes = new byte[sps.length + pps.length + outData.length];
                        System.arraycopy(sps, 0, dataBytes, 0, sps.length);
                        System.arraycopy(pps, 0, dataBytes, sps.length, pps.length);
                        System.arraycopy(outData, 0, dataBytes, pps.length + sps.length, outData.length);
                        if (encoderListener != null) ***REMOVED***
                            encoderListener.onVideoEncoded(dataBytes, System.currentTimeMillis(), seq, true);
                            seq++;
                      ***REMOVED***
                  ***REMOVED*** else ***REMOVED***
                        //outData就是输出的h264数据
//                        Log.e("TAG", "==========P帧===============" + seq);
                        if (encoderListener != null) ***REMOVED***
                            encoderListener.onVideoEncoded(outData, System.currentTimeMillis(), seq, false);
                            seq++;
                      ***REMOVED***
                  ***REMOVED***

                    mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                    outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
              ***REMOVED***
          ***REMOVED*** catch (Throwable t) ***REMOVED***
                t.printStackTrace();
          ***REMOVED***
      ***REMOVED***);
  ***REMOVED***

    /**
     * 因为从MediaCodec不支持NV21的数据编码，所以需要先讲NV21的数据转码为NV12
     */
    private byte[] NV21ToNV12(byte[] nv21, int width, int height) ***REMOVED***
        byte[] nv12 = new byte[width * height * 3 / 2];
        int frameSize = width * height;
        int i, j;
        System.arraycopy(nv21, 0, nv12, 0, frameSize);
        for (i = 0; i < frameSize; i++) ***REMOVED***
            nv12[i] = nv21[i];
      ***REMOVED***
        for (j = 0; j < frameSize / 2; j += 2) ***REMOVED***
            nv12[frameSize + j - 1] = nv21[j + frameSize];
      ***REMOVED***
        for (j = 0; j < frameSize / 2; j += 2) ***REMOVED***
            nv12[frameSize + j] = nv21[j + frameSize - 1];
      ***REMOVED***
        return nv12;
  ***REMOVED***

    /**
     * 此处为顺时针旋转旋转90度
     *
     * @param data        旋转前的数据
     * @param imageWidth  旋转前数据的宽
     * @param imageHeight 旋转前数据的高
     * @return 旋转后的数据
     */
    private byte[] rotateNV290(byte[] data, int imageWidth, int imageHeight) ***REMOVED***
        byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
        // Rotate the Y luma
        int i = 0;
        for (int x = 0; x < imageWidth; x++) ***REMOVED***
            for (int y = imageHeight - 1; y >= 0; y--) ***REMOVED***
                yuv[i] = data[y * imageWidth + x];
                i++;
          ***REMOVED***
      ***REMOVED***
        // Rotate the U and V color components
        i = imageWidth * imageHeight * 3 / 2 - 1;
        for (int x = imageWidth - 1; x > 0; x = x - 2) ***REMOVED***
            for (int y = 0; y < imageHeight / 2; y++) ***REMOVED***
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + x];
                i--;
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + (x - 1)];
                i--;
          ***REMOVED***
      ***REMOVED***
        return yuv;
  ***REMOVED***

    /**
     * 此处为逆时针旋转旋转90度
     *
     * @param data        旋转前的数据
     * @param imageWidth  旋转前数据的宽
     * @param imageHeight 旋转前数据的高
     * @return 旋转后的数据
     */
    private byte[] rotateNV270(byte[] data, int imageWidth, int imageHeight) ***REMOVED***
        byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
        // Rotate the Y luma
        int i = 0;
        for (int x = imageWidth - 1; x >= 0; x--) ***REMOVED***
            for (int y = 0; y < imageHeight; y++) ***REMOVED***
                yuv[i] = data[y * imageWidth + x];
                i++;
          ***REMOVED***
      ***REMOVED***
        // Rotate the U and V color components
        i = imageWidth * imageHeight;
        for (int x = imageWidth - 1; x > 0; x = x - 2) ***REMOVED***
            for (int y = 0; y < imageHeight / 2; y++) ***REMOVED***
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + (x - 1)];
                i++;
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + x];
                i++;
          ***REMOVED***
      ***REMOVED***
        return yuv;
  ***REMOVED***

    private void verticalMirror(byte[] src, int w, int h) ***REMOVED*** //src是原始yuv数组
        int i;
        int index;
        byte temp;
        int a, b;
        //mirror y
        for (i = 0; i < w; i++) ***REMOVED***
            a = i;
            b = (h - 1) * w + i;
            while (a < b) ***REMOVED***
                temp = src[a];
                src[a] = src[b];
                src[b] = temp;
                a += w;
                b -= w;
          ***REMOVED***
      ***REMOVED***

        // mirror u and v
        index = w * h;
        for (i = 0; i < w; i++) ***REMOVED***
            a = i;
            b = (h / 2 - 1) * w + i;
            while (a < b) ***REMOVED***
                temp = src[a + index];
                src[a + index] = src[b + index];
                src[b + index] = temp;
                a += w;
                b -= w;
          ***REMOVED***
      ***REMOVED***
  ***REMOVED***

    /**
     * 设置编码成功后数据回调
     */
    public void setEncoderListener(OnEncodeListener listener) ***REMOVED***
        encoderListener = listener;
  ***REMOVED***

    public void stop() ***REMOVED***
        executor.shutdown();
  ***REMOVED***
}
