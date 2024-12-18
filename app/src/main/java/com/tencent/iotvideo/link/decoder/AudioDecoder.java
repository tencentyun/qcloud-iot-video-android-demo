package com.tencent.iotvideo.link.decoder;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AudioDecoder {
    private static final String TAG = "AudioDecoder";
    private static final int AV_PTS_GAP_MS = 1500;
    private AudioTrack mAudioTrack;
    private MediaCodec mAudioCodec;
    private ExecutorService mAudioExecutor;
    private Thread mAudioPlayThread;

    private int audioChannelConfig;
    private int audioPcmFormat;
    private int audioSampleRate;
    private long currentVideoPts;
    private AudioManager audioManager;
    private boolean isSpeakerOn = true;

    public void setContext(Context context) {
        audioManager = ContextCompat.getSystemService(context, AudioManager.class);
    }

    public void setCurrentVideoPts(long currentVideoPts) {
        this.currentVideoPts = currentVideoPts;
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

    public void startAudio(int type, int mode, int width, int sample_rate) throws IOException {
        audioChannelConfig = mode == 1 ? AudioFormat.CHANNEL_OUT_STEREO : AudioFormat.CHANNEL_OUT_MONO;
        audioPcmFormat = width == 1 ? AudioFormat.ENCODING_PCM_16BIT : AudioFormat.ENCODING_PCM_8BIT;
        audioSampleRate = getSampleRate(sample_rate);
        int channel = mode + 1;
        initAudio(type, channel);
    }

    private int getSampleRate(int sample_rate) {
        // weird mapping
        if (sample_rate > 8000 && sample_rate < 16000)
            return 16000;
        else if (sample_rate > 16000 && sample_rate < 44100)
            return 44100;
        else if (sample_rate > 44100 && sample_rate < 48000)
            return 48000;
        else
            return sample_rate;
    }

    private void initAudio(int type, int channel) throws IOException {
        mAudioExecutor = Executors.newSingleThreadExecutor();
        int minBufSize = AudioTrack.getMinBufferSize(audioSampleRate, audioChannelConfig, audioPcmFormat);
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, audioSampleRate, audioChannelConfig, audioPcmFormat, minBufSize, AudioTrack.MODE_STREAM);
        mAudioTrack.setVolume(1.5f);
        mAudioTrack.play();
        Log.d(TAG, "start audio track");

        // create audio decoder
        if (type == 4) {
            mAudioCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
            MediaFormat audioFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, audioSampleRate, channel);
            audioFormat.setInteger(MediaFormat.KEY_PCM_ENCODING, audioPcmFormat);
            audioFormat.setInteger(MediaFormat.KEY_IS_ADTS, 1);
            audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 256 * 1024);
            int profile = MediaCodecInfo.CodecProfileLevel.AACObjectLC;
            audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, profile);
            // profile(5bits)|sample_rate_idx(4bits)|channel(4bits)|other(3bits)
            int sample_rate_idx = 0x08;
            switch (audioSampleRate) {
                case 16000:
                    sample_rate_idx = 0x08;
                    break;
                case 8000:
                    sample_rate_idx = 0x0B;
                    break;
                case 44100:
                    sample_rate_idx = 0x04;
                    break;
                case 48000:
                    sample_rate_idx = 0x03;
                    break;
                default:
                    break;
            }
            byte[] adts_data = new byte[2];
            adts_data[0] = (byte) ((profile << 3) | (sample_rate_idx >> 1));
            adts_data[1] = (byte) ((byte) ((sample_rate_idx << 7) & 0x80) | (channel << 3));
            ByteBuffer csd_0 = ByteBuffer.wrap(adts_data);
            audioFormat.setByteBuffer("csd-0", csd_0);
            mAudioCodec.configure(audioFormat, null, null, 0);
            mAudioCodec.start();
            Log.d(TAG, "start audio codec " + adts_data[0] + " " + adts_data[1]);
//            startAudioPlayThread();
        }
    }

    private void startAudioPlayThread() {
        // aac场景下，启动多一个线程用于播放，避免阻塞导致音频延迟
        Runnable runnable = new PlayTask();
        mAudioPlayThread = new Thread(runnable);
        mAudioPlayThread.start();
    }

    public int decoderAAC(byte[] data, int len, long pts) {
        if (mAudioExecutor == null || mAudioExecutor.isShutdown()) return -1;
        if (mAudioTrack == null) return -2;

        // one thread for audio decode
        mAudioExecutor.submit(() -> {
            try {
                byte[] readyToProcessBytes = data;
                // PCM format data, no need to decode, just play
                if (mAudioCodec == null && mAudioTrack != null) {
                    mAudioTrack.write(data, 0, len);
                    return;
                }

//                Log.d(TAG, ">>>> queue audio aac data " + len + " pts " + pts);
                // queue aac data and decode
                int inputBufferIndex = mAudioCodec.dequeueInputBuffer(0);
                if (inputBufferIndex >= 0) {
                    ByteBuffer inputBuffer = mAudioCodec.getInputBuffer(inputBufferIndex);
                    if (inputBuffer != null) {
                        inputBuffer.clear();
                        inputBuffer.put(readyToProcessBytes);
                        mAudioCodec.queueInputBuffer(inputBufferIndex, 0, readyToProcessBytes.length, System.nanoTime() / 1000, 0);
                    }
                } else {
                    Log.e(TAG, "audio inputBufferIndex invalid: " + inputBufferIndex);
                }

                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                int outputBufferIndex = mAudioCodec.dequeueOutputBuffer(bufferInfo, 0);
                while (outputBufferIndex >= 0) {
                    ByteBuffer outputBuffer = mAudioCodec.getOutputBuffer(outputBufferIndex);
                    if (outputBuffer != null) {
                        byte[] outData = new byte[bufferInfo.size];
                        outputBuffer.get(outData);
                        long decode_pts = bufferInfo.presentationTimeUs / 1000;
//                        Log.d(TAG, ">>>>> audio decoder output size " + info.size + " pts " + decode_pts + " current video pts " + currentVideoPts);
                        // 简单音画同步处理，如果音频帧滞后超过一定时间，直接丢弃
                        if ((decode_pts + AV_PTS_GAP_MS) < currentVideoPts) {
                            Log.i(TAG, "drop audio frame as audio pts " + decode_pts + " < video pts " + currentVideoPts);
                        } else {
                            if (audioManager != null) {
                                audioManager.setSpeakerphoneOn(isSpeakerOn);
                            }
                            if (mAudioTrack.getState() != AudioTrack.STATE_UNINITIALIZED) {
                                mAudioTrack.write(outData, 0, outData.length);
                            }
                        }
                        mAudioCodec.releaseOutputBuffer(outputBufferIndex, false);
                    }
                    outputBufferIndex = mAudioCodec.dequeueOutputBuffer(bufferInfo, 0);
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        });
        return 0;
    }

    public void stopAudio() {
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
    }

    // one thread for audio play
    private class PlayTask implements Runnable {
        @Override
        public void run() {
            Log.i(TAG, "start audio play thread");
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
//          Log.d(TAG, "audio outputBufferIndex: " + outputBufId);
            int outputBufId = mAudioCodec.dequeueOutputBuffer(bufferInfo, 0);
            while (mAudioCodec != null && mAudioTrack != null && outputBufId >= 0) {
                // dequeue and play
                if (outputBufId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    Log.i(TAG, "audio format changed");
                    mAudioTrack.stop();
                    mAudioTrack.release();
                    int minBufSize = AudioTrack.getMinBufferSize(audioSampleRate, audioChannelConfig, audioPcmFormat);
                    mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, audioSampleRate, audioChannelConfig, audioPcmFormat, 2 * minBufSize, AudioTrack.MODE_STREAM);
                    mAudioTrack.setVolume(1.5f);
                    mAudioTrack.play();
                } else if (outputBufId == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    continue;
                } else {
                    ByteBuffer outputBuffer = mAudioCodec.getOutputBuffer(outputBufId);
                    if (outputBuffer != null) {
                        byte[] outData = new byte[bufferInfo.size];
                        outputBuffer.get(outData);
                        long decode_pts = bufferInfo.presentationTimeUs / 1000;
//                        Log.d(TAG, ">>>>> audio decoder output size " + info.size + " pts " + decode_pts + " current video pts " + currentVideoPts);
                        // 简单音画同步处理，如果音频帧滞后超过一定时间，直接丢弃
                        if ((decode_pts + AV_PTS_GAP_MS) < currentVideoPts) {
                            Log.i(TAG, "drop audio frame as audio pts " + decode_pts + " < video pts " + currentVideoPts);
                        } else {
                            if (audioManager != null) {
                                audioManager.setSpeakerphoneOn(isSpeakerOn);
                            }
                            if (mAudioTrack.getState() != AudioTrack.STATE_UNINITIALIZED) {
                                mAudioTrack.write(outData, 0, outData.length);
                            }
                        }
                        mAudioCodec.releaseOutputBuffer(outputBufId, false);
                    }
                    outputBufId = mAudioCodec.dequeueOutputBuffer(bufferInfo, 0);
                }
            }
            Log.i(TAG, "quit audio play thread");
        }
    }
}
