package com.tencent.iotvideo.link;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;

import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    private AudioManager audioManager;
    private boolean isSpeakerOn = true;

    private FileOutputStream fos;

    private String speakH264FilePath = "/sdcard/simplePlayer.h264";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public void setContext(Context context) {
        audioManager = ContextCompat.getSystemService(context, AudioManager.class);
    }

    public int startVideoPlay(Surface surface, int visitor, int type, int height, int width) {
        recordSpeakH264(false);
        Log.d(TAG, "video input from visitor " + visitor + " height " + height + " width " + width + ", model:" + Build.MODEL);
        String model = Build.MODEL;

        // type == 0: h.264/avc; type == 1: h.265/hevc
        // currently only support h.264
        if (type == 0 && height > 0 && width > 0) {
            try {
                mVideoExecutor = Executors.newSingleThreadExecutor();
                mVideoCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
                MediaFormat mFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
                mFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 1024 * 1024);
                mFormat.setInteger(MediaFormat.KEY_ROTATION, 90);
                mFormat.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline);
                mFormat.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel31);

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
                frameCount = 0;

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
        try {
            mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, audioSampleRate, audioChannelConfig, audioWidth, minBufSize, AudioTrack.MODE_STREAM);
            mAudioTrack.setVolume(1.5f);
            mAudioTrack.play();
            mAudioExecutor = Executors.newSingleThreadExecutor();
            Log.d(TAG, "start audio track");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // create audio decoder
        if (type == 4) {
            try {
                int channel = mode + 1;
                mAudioCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
                MediaFormat audioFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, audioSampleRate, channel);
                audioFormat.setInteger(MediaFormat.KEY_PCM_ENCODING, audioWidth);
                audioFormat.setInteger(MediaFormat.KEY_IS_ADTS, 1);
                audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 256 * 1024);
                int profile = MediaCodecInfo.CodecProfileLevel.AACObjectLC;
                audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, profile);
                // profile(5bits)|sample_rate_idx(4bits)|channel(4bits)|other(3bits)
                int sample_rate_idx = 0x08;
                switch(audioSampleRate) {
                    case 16000: sample_rate_idx = 0x08; break;
                    case 8000: sample_rate_idx = 0x0B; break;
                    case 44100: sample_rate_idx = 0x04; break;
                    case 48000: sample_rate_idx = 0x03; break;
                    default: break;
                }
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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    public void setSpeakerOn(boolean speakerOn) {
        isSpeakerOn = speakerOn;
        if (audioManager != null) {
            audioManager.setSpeakerphoneOn(speakerOn);
        }
    }

    public boolean isSpeakerOn() {
        return isSpeakerOn;
    }

    public int stopVideoPlay(int visitor) {
        try {
            Log.d(TAG, "visitor " + visitor + " stop video play");
            if (mVideoExecutor != null) {
                mVideoExecutor.shutdown();
                mVideoExecutor = null;
            }

            if (mVideoCodec != null) {
                mVideoCodec.stop();
                mVideoCodec.release();
                mVideoCodec = null;
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
        if (mAudioExecutor != null) {
            mAudioExecutor.shutdown();
            mAudioExecutor = null;
        }
        if (mAudioTrack != null) {
            mAudioTrack.stop();
            mAudioTrack.release();
            mAudioTrack = null;
        }
        if (mAudioCodec != null) {
            mAudioCodec.stop();
            mAudioCodec.release();
            mAudioCodec = null;
        }
        if (mAudioPlayThread != null) {
            try {
                mAudioPlayThread.join(10);
                mAudioPlayThread = null;
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        return 0;
    }

    public int playVideoStream(int visitor, byte[] data, int len, long pts, long seq) {
//        Log.d(TAG, "video frame: visitor "+ visitor + " len " + len + " pts " + pts + " seq " + seq);
        currentVideoPts = pts;
        if (mVideoExecutor == null || mVideoExecutor.isShutdown()) return -1;
        if (mVideoCodec == null) return -2;

        mVideoExecutor.submit(() -> {
            try {
                ByteBuffer[] inputBuffers = mVideoCodec.getInputBuffers();
                ByteBuffer[] outputBuffers = mVideoCodec.getOutputBuffers();
                // queue and decode
                int inputBufferIndex = mVideoCodec.dequeueInputBuffer(-1);
                if (inputBufferIndex >= 0) {
                    ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                    inputBuffer.clear();
                    inputBuffer.put(data, 0, len);
                    // first frame
//                    if (frameCount == 0) {
//                        mVideoCodec.queueInputBuffer(inputBufferIndex, 0, len, pts * 1000, MediaCodec.BUFFER_FLAG_CODEC_CONFIG);
//                        frameCount = 1;
//                    } else {
//                        mVideoCodec.queueInputBuffer(inputBufferIndex, 0, len, pts * 1000, 0);
//
//                    }
                    mVideoCodec.queueInputBuffer(inputBufferIndex, 0, len, 0, 0);
                } else {
                    Log.e(TAG, "video inputBufferIndex invalid: " + inputBufferIndex);
                }

                // dequeue and render
                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                int outputBufferIndex = mVideoCodec.dequeueOutputBuffer(bufferInfo, 0);
                while (outputBufferIndex >= 0) {
//                    ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
//                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) {
//                        Log.d(TAG, "I帧");
//                    } else {
//                        Log.d(TAG, "P帧");
//                    }
                    mVideoCodec.releaseOutputBuffer(outputBufferIndex, true);
                    outputBufferIndex = mVideoCodec.dequeueOutputBuffer(bufferInfo, 0);
                }
//                if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
//                    MediaFormat newFormat = mVideoCodec.getOutputFormat();
//                    Log.d(TAG, "Output format changed: " + newFormat);
//                } else if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
//                    Log.d(TAG, "No output buffer available, try again later");
//                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
            //        Log.d(TAG, "end of frame handle");
        });

        executor.submit(() -> {
            if (fos != null) {
                try {
                    fos.write(data);
                    fos.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        return 0;
    }

    public void recordSpeakH264(boolean isRecord) {

        if (!TextUtils.isEmpty(speakH264FilePath)) {
            try {
                File file = getFile(speakH264FilePath);
                fos = new FileOutputStream(file);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, speakH264FilePath + "临时缓存文件未找到");
            }
        }
    }

    public File getFile(String path) throws IOException {
        File file = new File(path);
        Log.i(TAG, "speak cache h264 file path:" + path);
        if (file.exists()) {
            file.delete();
        }
        file.createNewFile();
        return file;
    }

    public int playAudioStream(int visitor, byte[] data, int len, long pts, long seq) {
//        Log.d(TAG, "audio frame: visitor "+ visitor + " len " + len + " pts " + pts + " seq " + seq);
        if (mAudioExecutor == null || mAudioExecutor.isShutdown()) return -1;
        if (mAudioTrack == null) return -2;

        // one thread for audio decode
        mAudioExecutor.submit(() -> {
            try {
                // PCM format data, no need to decode, just play
                if (mAudioCodec == null && mAudioTrack != null) {
                    mAudioTrack.write(data, 0, len);
                    return;
                }

                if (mAudioCodec != null) {
//                Log.d(TAG, ">>>> queue audio aac data " + len + " pts " + pts);
                    // queue aac data and decode
                    int inputBufferIndex = mAudioCodec.dequeueInputBuffer(100000);
                    if (inputBufferIndex >= 0) {
                        ByteBuffer inputBuffer = mAudioCodec.getInputBuffer(inputBufferIndex);
                        inputBuffer.clear();
//                  Log.d(TAG, "aac input: " + bytesToHex(data, len));
                        inputBuffer.put(data, 0, len).rewind();
                        mAudioCodec.queueInputBuffer(inputBufferIndex, 0, len, pts * 1000, 0);
                    } else {
                        Log.e(TAG, "audio inputBufferIndex invalid: " + inputBufferIndex);
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        });

        return 0;
    }

    // one thread for audio play
    private class PlayTask implements Runnable {
        @Override
        public void run() {
            Log.i(TAG, "start audio play thread");
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            while (mAudioCodec != null && mAudioTrack != null) {
                try {
                    // dequeue and play
                    int outputBufId = mAudioCodec.dequeueOutputBuffer(info, 100000);
//                    Log.d(TAG, "audio outputBufferIndex: " + outputBufId);

                    if (outputBufId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        Log.i(TAG, "audio format changed");
                        mAudioTrack.stop();
                        mAudioTrack.release();
                        int minBufSize = AudioTrack.getMinBufferSize(audioSampleRate, audioChannelConfig, audioWidth);
                        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, audioSampleRate, audioChannelConfig, audioWidth, 2 * minBufSize, AudioTrack.MODE_STREAM);
                        mAudioTrack.setVolume(1.5f);
                        mAudioTrack.play();
                        outputBufId = mAudioCodec.dequeueOutputBuffer(info, 10000);

                    } else if (outputBufId == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        continue;
                    } else {
                        ByteBuffer outputBuf = mAudioCodec.getOutputBuffer(outputBufId);
                        byte[] playBuf = new byte[info.size];
                        outputBuf.get(playBuf);
                        outputBuf.rewind();
                        outputBuf.clear();
                        mAudioCodec.releaseOutputBuffer(outputBufId, false);
                        long decode_pts = info.presentationTimeUs / 1000;
//                        Log.d(TAG, ">>>>> audio decoder output size " + info.size + " pts " + decode_pts + " current video pts " + currentVideoPts);
                        // 简单音画同步处理，如果音频帧滞后超过一定时间，直接丢弃
                        if ((decode_pts + AV_PTS_GAP_MS) < currentVideoPts) {
                            Log.i(TAG, "drop audio frame as audio pts " + decode_pts + " < video pts " + currentVideoPts);
                        } else {
                            if (audioManager != null) {
                                audioManager.setSpeakerphoneOn(isSpeakerOn);
                            }
                            if (mAudioTrack.getState() != AudioTrack.STATE_UNINITIALIZED) {
                                mAudioTrack.write(playBuf, 0, info.size);
                            }
                        }
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
            Log.i(TAG, "quit audio play thread");
        }
    }
}
