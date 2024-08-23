package com.tencent.iotvideo.link;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SimplePlayer ***REMOVED***
    private static final String TAG = "SimplePlayer";
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes, int length) ***REMOVED***
        char[] hexChars = new char[length * 2];
        for (int j = 0; j < length; j++) ***REMOVED***
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
      ***REMOVED***
        return new String(hexChars);
  ***REMOVED***

    private MediaCodec mVideoCodec;
    private ExecutorService mVideoExecutor;

    private AudioTrack mAudioTrack;
    private MediaCodec mAudioCodec;
    private ExecutorService mAudioExecutor;
    private Thread mAudioPlayThread;

    // player config and control
    private int frameCount = 0;
    private int audioChannelConfig;
    private int audioWidth;
    private int audioSampleRate;
    private long currentVideoPts = 0;
    private static final int AV_PTS_GAP_MS = 1500;


    public int startVideoPlay(Surface surface, int visitor, int type, int height, int width) ***REMOVED***
        Log.d(TAG, "video input from visitor "+ visitor + " height "+ height + " width " + width + ", model:" + Build.MODEL);
        String model = Build.MODEL;

        // type == 0: h.264/avc; type == 1: h.265/hevc
        // currently only support h.264
        if (type == 0 && height > 0 && width > 0) ***REMOVED***
            try ***REMOVED***
                mVideoExecutor = Executors.newSingleThreadExecutor();
                mVideoCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
                MediaFormat mFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
                mFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE,1024*1024);
                mFormat.setInteger(MediaFormat.KEY_ROTATION, 90);

                if (model.contains("KONKA") && model.contains("9652") || model.contains("KONKA") && model.contains("9653"))***REMOVED*** // 康佳 MTK的一个SoC 型号
                    mFormat.setInteger("low-latency", 1);
                    Log.d(TAG, "qudiao mFormat set low-latency 1" + ", model:" + Build.MODEL);
              ***REMOVED***

                if (model.contains("KONKA") && model.contains("V811")) ***REMOVED*** // 康佳 海思的一个SoC 型号
                    mVideoCodec.configure(mFormat, surface, null, 0x2);
                    Log.d(TAG, "mVideoCodec configure flags 0x2" + ", model:" + Build.MODEL);
              ***REMOVED*** else ***REMOVED***
                    mVideoCodec.configure(mFormat, surface, null, 0);
                    Log.d(TAG, "mVideoCodec configure flags 0" + ", model:" + Build.MODEL);
              ***REMOVED***
                mVideoCodec.start();
                frameCount = 0;

          ***REMOVED*** catch (IOException e) ***REMOVED***
                e.printStackTrace();
          ***REMOVED***
      ***REMOVED***
        return 0;
  ***REMOVED***

    public int startAudioPlay(int visitor, int type, int option, int mode, int width, int sample_rate, int sample_num) ***REMOVED***
        Log.d(TAG, "audio input from visitor "+ visitor + " type " + type + " option " + option);
        // type 0: PCM; type 4 option 2:aac-lc
        // currently only support AAC or PCM
        if (type != 4 && type != 0) ***REMOVED***
            Log.e(TAG, "unsupported audio format " + type);
            return -1;
      ***REMOVED***

        audioChannelConfig = mode == 1 ? AudioFormat.CHANNEL_OUT_STEREO : AudioFormat.CHANNEL_OUT_MONO;
        audioWidth = width == 1 ? AudioFormat.ENCODING_PCM_16BIT : AudioFormat.ENCODING_PCM_8BIT;
        // weird mapping
        if (sample_rate > 8000 && sample_rate < 16000)
            audioSampleRate = 16000;
        else if (sample_rate > 16000 && sample_rate < 44100)
            audioSampleRate = 44100;
        else if (sample_rate > 44100 && sample_rate < 48000)
            audioSampleRate = 48000;
        else
            audioSampleRate = sample_rate;

        int minBufSize = AudioTrack.getMinBufferSize(audioSampleRate, audioChannelConfig, audioWidth);
        try ***REMOVED***
            mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, audioSampleRate, audioChannelConfig, audioWidth, minBufSize, AudioTrack.MODE_STREAM);
            mAudioTrack.setVolume(1.5f);
            mAudioTrack.play();
            mAudioExecutor = Executors.newSingleThreadExecutor();
            Log.d(TAG, "start audio track");
      ***REMOVED*** catch (Exception e) ***REMOVED***
            e.printStackTrace();
      ***REMOVED***

        // create audio decoder
        if (type == 4 ) ***REMOVED***
            try ***REMOVED***
                int channel = mode + 1;
                mAudioCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
                MediaFormat audioFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, audioSampleRate, channel);
                audioFormat.setInteger(MediaFormat.KEY_PCM_ENCODING, audioWidth);
                audioFormat.setInteger(MediaFormat.KEY_IS_ADTS,1);
                audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE,256*1024);
                int profile = MediaCodecInfo.CodecProfileLevel.AACObjectLC;
                audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, profile);
                // profile(5bits)|sample_rate_idx(4bits)|channel(4bits)|other(3bits)
                int sample_rate_idx = 0x08;
                switch(audioSampleRate) ***REMOVED***
                    case 16000: sample_rate_idx = 0x08; break;
                    case 8000: sample_rate_idx = 0x0B; break;
                    case 44100: sample_rate_idx = 0x04; break;
                    case 48000: sample_rate_idx = 0x03; break;
                    default: break;
              ***REMOVED***
                byte[] adts_data = new byte[2];
                adts_data[0] = (byte) ((profile << 3) | (sample_rate_idx >> 1));
                adts_data[1] = (byte) ((byte) ((sample_rate_idx << 7) & 0x80) | (channel << 3));
                ByteBuffer csd_0 = ByteBuffer.wrap(adts_data);
                audioFormat.setByteBuffer("csd-0", csd_0);
                mAudioCodec.configure(audioFormat, null, null, 0);
                mAudioCodec.start();
                Log.d(TAG, "start audio codec " + adts_data[0] + " " + adts_data[1]);

                // aac场景下，启动多一个线程用于播放，避免阻塞导致音频延迟
                Runnable runnable = new PlayTask();
                mAudioPlayThread = new Thread(runnable);
                mAudioPlayThread.start();
          ***REMOVED*** catch (Exception e) ***REMOVED***
                e.printStackTrace();
          ***REMOVED***
      ***REMOVED***
        return 0;
  ***REMOVED***

    public int stopVideoPlay(int visitor) ***REMOVED***
        try ***REMOVED***
            Log.d(TAG, "visitor "+ visitor + " stop video play");
            if (mVideoExecutor != null) ***REMOVED***
                mVideoExecutor.shutdown();
                mVideoExecutor = null;
          ***REMOVED***

            if (mVideoCodec != null) ***REMOVED***
                mVideoCodec.stop();
                mVideoCodec.release();
                mVideoCodec = null;
          ***REMOVED***
            return 0;

      ***REMOVED*** catch (Throwable t) ***REMOVED***
            t.printStackTrace();
            return -1;
      ***REMOVED***
  ***REMOVED***

    public int stopAudioPlay(int visitor) ***REMOVED***
        Log.d(TAG, "visitor "+ visitor + " stop audio play");
        // 这里停止可能导致音频没有播放完就退出？
        if (mAudioExecutor != null) ***REMOVED***
            mAudioExecutor.shutdown();
            mAudioExecutor = null;
      ***REMOVED***
        if (mAudioTrack != null) ***REMOVED***
            mAudioTrack.stop();
            mAudioTrack.release();
            mAudioTrack = null;
      ***REMOVED***
        if (mAudioCodec != null) ***REMOVED***
            mAudioCodec.stop();
            mAudioCodec.release();
            mAudioCodec = null;
      ***REMOVED***
        if (mAudioPlayThread != null) ***REMOVED***
            try ***REMOVED***
                mAudioPlayThread.join(10);
                mAudioPlayThread = null;
          ***REMOVED*** catch (Throwable t) ***REMOVED***
                t.printStackTrace();
          ***REMOVED***
      ***REMOVED***
        return 0;
  ***REMOVED***

    public int playVideoStream(int visitor, byte[] data, int len, long pts, long seq) ***REMOVED***
//        Log.d(TAG, "video frame: visitor "+ visitor + " len " + len + " pts " + pts + " seq " + seq);
        currentVideoPts = pts;
        if (mVideoExecutor == null || mVideoExecutor.isShutdown()) return -1;
        if (mVideoCodec == null) return -2;

        mVideoExecutor.submit(() -> ***REMOVED***
            try ***REMOVED***
                // queue and decode
                int inputBufferIndex = mVideoCodec.dequeueInputBuffer(10000);
                if (inputBufferIndex >= 0) ***REMOVED***
                    ByteBuffer inputBuffer = mVideoCodec.getInputBuffer(inputBufferIndex);
                    inputBuffer.clear();
                    inputBuffer.put(data, 0, len).rewind();
                    // first frame
                    if (frameCount == 0) ***REMOVED***
                        mVideoCodec.queueInputBuffer(inputBufferIndex, 0, len, pts * 1000, MediaCodec.BUFFER_FLAG_CODEC_CONFIG);
                        frameCount = 1;
                  ***REMOVED*** else ***REMOVED***
                        mVideoCodec.queueInputBuffer(inputBufferIndex, 0, len, pts * 1000, 0);
                  ***REMOVED***
              ***REMOVED*** else ***REMOVED***
                    Log.e(TAG, "video inputBufferIndex invalid: " + inputBufferIndex);
              ***REMOVED***

                // dequeue and render
                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                int outputBufId = mVideoCodec.dequeueOutputBuffer(info, 100000);
                while (outputBufId >= 0) ***REMOVED***
                    mVideoCodec.releaseOutputBuffer(outputBufId, true);
                    outputBufId = mVideoCodec.dequeueOutputBuffer(info, 1000);
              ***REMOVED***
          ***REMOVED*** catch (Throwable t) ***REMOVED***
                t.printStackTrace();
          ***REMOVED***
            //        Log.d(TAG, "end of frame handle");
      ***REMOVED***);
        return 0;
  ***REMOVED***

    public int playAudioStream(int visitor, byte[] data, int len, long pts, long seq) ***REMOVED***
//        Log.d(TAG, "audio frame: visitor "+ visitor + " len " + len + " pts " + pts + " seq " + seq);
        if (mAudioExecutor == null || mAudioExecutor.isShutdown()) return -1;
        if (mAudioTrack == null) return -2;

        // one thread for audio decode
        mAudioExecutor.submit(() -> ***REMOVED***
            try ***REMOVED***
                // PCM format data, no need to decode, just play
                if (mAudioCodec == null && mAudioTrack != null) ***REMOVED***
                    mAudioTrack.write(data, 0, len);
                    return;
              ***REMOVED***

                if (mAudioCodec != null) ***REMOVED***
//                Log.d(TAG, ">>>> queue audio aac data " + len + " pts " + pts);
                    // queue aac data and decode
                    int inputBufferIndex = mAudioCodec.dequeueInputBuffer(100000);
                    if (inputBufferIndex >= 0) ***REMOVED***
                        ByteBuffer inputBuffer = mAudioCodec.getInputBuffer(inputBufferIndex);
                        inputBuffer.clear();
//                  Log.d(TAG, "aac input: " + bytesToHex(data, len));
                        inputBuffer.put(data, 0, len).rewind();
                        mAudioCodec.queueInputBuffer(inputBufferIndex, 0, len, pts * 1000, 0);
                  ***REMOVED*** else ***REMOVED***
                        Log.e(TAG, "audio inputBufferIndex invalid: " + inputBufferIndex);
                  ***REMOVED***
              ***REMOVED***
          ***REMOVED*** catch (Throwable t) ***REMOVED***
                t.printStackTrace();
          ***REMOVED***
      ***REMOVED***);

        return 0;
  ***REMOVED***

    // one thread for audio play
    private class PlayTask implements Runnable ***REMOVED***
        @Override
        public void run() ***REMOVED***
            Log.i(TAG, "start audio play thread");
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            while (mAudioCodec != null && mAudioTrack != null) ***REMOVED***
                try ***REMOVED***
                    // dequeue and play
                    int outputBufId = mAudioCodec.dequeueOutputBuffer(info, 100000);
//                    Log.d(TAG, "audio outputBufferIndex: " + outputBufId);

                    if (outputBufId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) ***REMOVED***
                        Log.i(TAG, "audio format changed");
                        mAudioTrack.stop();
                        mAudioTrack.release();
                        int minBufSize = AudioTrack.getMinBufferSize(audioSampleRate, audioChannelConfig, audioWidth);
                        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, audioSampleRate, audioChannelConfig, audioWidth, 2*minBufSize, AudioTrack.MODE_STREAM);
                        mAudioTrack.setVolume(1.5f);
                        mAudioTrack.play();
                        outputBufId = mAudioCodec.dequeueOutputBuffer(info, 10000);

                  ***REMOVED*** else if (outputBufId == MediaCodec.INFO_TRY_AGAIN_LATER) ***REMOVED***
                        continue;
                  ***REMOVED*** else ***REMOVED***
                        ByteBuffer outputBuf = mAudioCodec.getOutputBuffer(outputBufId);
                        byte[] playBuf = new byte[info.size];
                        outputBuf.get(playBuf);
                        outputBuf.rewind();
                        outputBuf.clear();
                        mAudioCodec.releaseOutputBuffer(outputBufId, false);
                        long decode_pts = info.presentationTimeUs/1000;
//                        Log.d(TAG, ">>>>> audio decoder output size " + info.size + " pts " + decode_pts + " current video pts " + currentVideoPts);
                        // 简单音画同步处理，如果音频帧滞后超过一定时间，直接丢弃
                        if ((decode_pts + AV_PTS_GAP_MS) < currentVideoPts) ***REMOVED***
                            Log.i(TAG, "drop audio frame as audio pts " + decode_pts + " < video pts " + currentVideoPts);
                      ***REMOVED*** else ***REMOVED***
                            mAudioTrack.write(playBuf, 0, info.size);
                      ***REMOVED***
                  ***REMOVED***
              ***REMOVED*** catch (Throwable t) ***REMOVED***
                    t.printStackTrace();
              ***REMOVED***
          ***REMOVED***
            Log.i(TAG, "quit audio play thread");
      ***REMOVED***
  ***REMOVED***
}
