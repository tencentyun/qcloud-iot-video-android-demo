package com.tencent.iotvideo.link;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import com.tencent.iotvideo.link.decoder.AudioDecoder;
import com.tencent.iotvideo.link.decoder.VideoDecoder;

import java.io.IOException;

public class SimplePlayer {
    private static final String TAG = "SimplePlayer";
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes, int length) {
        char[] hexChars = new char[length * 2];
        for (int j = 0; j < length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    private VideoDecoder videoDecoder;

    private AudioDecoder audioDecoder;

    public void setContext(Context context) {
        if (audioDecoder != null) {
            audioDecoder.setContext(context);
        }
    }

    public int startVideoPlay(Surface surface, int visitor, int type, int height, int width) {
        Log.d(TAG, "video input from visitor " + visitor + " height " + height + " width " + width + ", model:" + Build.MODEL);
        // type == 0: h.264/avc; type == 1: h.265/hevc
        // currently only support h.264
        if (type == 0 && height > 0 && width > 0) {
            if (videoDecoder == null) {
                videoDecoder = new VideoDecoder();
            }
            try {
                videoDecoder.startVideo(width, height, surface);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    public int startAudioPlay(int visitor, int type, int option, int mode, int width, int sample_rate, int sample_num) {
        Log.d(TAG, "audio input from visitor " + visitor + " type " + type + " option " + option);
        // type 0: PCM; type 4 option 2:aac-lc
        // currently only support AAC or PCM
        if (type != 4 && type != 0) {
            Log.e(TAG, "unsupported audio format " + type);
            return -1;
        }
        try {
            if (audioDecoder == null) {
                audioDecoder = new AudioDecoder();
            }
            audioDecoder.startAudio(type, mode, width, sample_rate);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public boolean isSpeakerOn() {
        return audioDecoder.isSpeakerOn();
    }

    public int stopVideoPlay(int visitor) {
        try {
            Log.d(TAG, "visitor " + visitor + " stop video play");
            videoDecoder.stopVideo();
            return 0;
        } catch (Throwable t) {
            t.printStackTrace();
            return -1;
        }
    }

    public int stopAudioPlay(int visitor) {
        Log.d(TAG, "visitor " + visitor + " stop audio play");
        // 这里停止可能导致音频没有播放完就退出？
        audioDecoder.stopAudio();
        return 0;
    }

    public int playVideoStream(int visitor, byte[] data, int len, long pts, long seq) {
//        Log.d(TAG, "video frame: visitor "+ visitor + " len " + len + " pts " + pts + " seq " + seq);
        return videoDecoder.decoderH264(data, len, pts);
    }


    public int playAudioStream(int visitor, byte[] data, int len, long pts, long seq) {
//        Log.d(TAG, "audio frame: visitor "+ visitor + " len " + len + " pts " + pts + " seq " + seq);
        audioDecoder.setCurrentVideoPts(videoDecoder.getCurrentVideoPts());
        return audioDecoder.decoderAAC(data, len, pts);
    }
}
