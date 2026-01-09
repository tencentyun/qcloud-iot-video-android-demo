package com.tencent.iotvideo.link;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;

import com.tencent.iotvideo.link.decoder.AudioDecoder;
import com.tencent.iotvideo.link.decoder.VideoDecoder;
import com.tencent.iotvideo.link.util.UtilsKt;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SimplePlayer {
    private static final String TAG = "SimplePlayer";
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    private String receiveH264FilePath = "/sdcard/wx_video.h264";
    private String receiveAacFilePath = "/sdcard/wx_audio.aac";
    private FileOutputStream h264Fos;
    private FileOutputStream aacFos;
    private boolean isSaveReceiveRecord = true;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

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

    public SimplePlayer() {
        isSaveReceiveRecord(false);
    }

    public void setContext(Context context) {
        if (audioDecoder != null) {
            audioDecoder.setContext(context);
        }
    }

    /**
     * 保存音视频数据
     *
     * @param isSaveReceiveRecord
     */
    public void isSaveReceiveRecord(boolean isSaveReceiveRecord) {
        this.isSaveReceiveRecord = isSaveReceiveRecord;
        recordSpeakH264(isSaveReceiveRecord);
        recordSpeakAac(isSaveReceiveRecord);
    }

    public void recordSpeakH264(boolean isRecord) {
        if (isRecord) {
            if (!TextUtils.isEmpty(receiveH264FilePath)) {
                try {
                    File file = UtilsKt.getFile(receiveH264FilePath);
                    h264Fos = new FileOutputStream(file);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, receiveH264FilePath + "临时缓存文件未找到");
                }
            }
        }
    }

    public void recordSpeakAac(boolean isRecord) {
        if (isRecord) {
            if (!TextUtils.isEmpty(receiveAacFilePath)) {
                try {
                    File file = UtilsKt.getFile(receiveAacFilePath);
                    aacFos = new FileOutputStream(file);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, receiveAacFilePath + "临时缓存文件未找到");
                }
            }
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
            if (videoDecoder != null) {
                videoDecoder.stopVideo();
            }
            return 0;
        } catch (Throwable t) {
            t.printStackTrace();
            return -1;
        }
    }

    public int stopAudioPlay(int visitor) {
        Log.d(TAG, "visitor " + visitor + " stop audio play");
        // 这里停止可能导致音频没有播放完就退出？
        if (audioDecoder != null) {
            audioDecoder.stopAudio();
        }
        return 0;
    }

    public int playVideoStream(int visitor, byte[] data, int len, long pts, long seq) {
//        Log.d(TAG, "video frame: visitor "+ visitor + " len " + len + " pts " + pts + " seq " + seq);
        if (videoDecoder != null) {
            int resCode = videoDecoder.decoderH264(data, len, pts);
            saveH264(data);
            return resCode;
        }
        return 0;
    }


    public int playAudioStream(int visitor, byte[] data, int len, long pts, long seq) {
//        Log.d(TAG, "audio frame: visitor "+ visitor + " len " + len + " pts " + pts + " seq " + seq);
        if (audioDecoder != null) {
            if (videoDecoder != null) {
                audioDecoder.setCurrentVideoPts(videoDecoder.getCurrentVideoPts());
            }
            int resCode = audioDecoder.decoderAAC(data, len, pts);
            saveAac(data);
            return resCode;
        }
        return 0;
    }

    public void saveH264(byte[] datas) {
        if (isSaveReceiveRecord) {
            if (executor.isShutdown()) return;
            executor.submit(() -> {
                if (h264Fos != null) {
                    try {
                        h264Fos.write(datas);
                        h264Fos.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    public void saveAac(byte[] datas) {
        if (isSaveReceiveRecord) {
            if (executor.isShutdown()) return;
            executor.submit(() -> {
                if (aacFos != null) {
                    try {
                        aacFos.write(datas);
                        aacFos.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }
}
