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

import com.tencent.iotvideo.link.listener.OnDecodeListener;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AudioDecoder {
    private static final String TAG = "AudioDecoder";
    private static final int AV_PTS_GAP_MS = 1500;
    private final Object codecLock = new Object();
    /** 保证 startAudio/stopAudio 串行化，避免并发启停导致 codec/track 双重 release 或泄漏 */
    private final Object lifecycleLock = new Object();
    private volatile boolean running = false;
    private MediaCodec mAudioCodec;
    private AudioTrack mAudioTrack;
    private ExecutorService mAudioExecutor;
    private Thread mAudioPlayThread;

    private int audioChannelConfig;
    private int audioPcmFormat;
    private int audioSampleRate;
    private long currentVideoPts;
    private AudioManager audioManager;
    private boolean isSpeakerOn = true;
    private OnDecodeListener onDecodeListener;

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

    /**
     * 设置音频解码监听器
     * @param listener 解码监听器
     */
    public void setOnDecodeListener(OnDecodeListener listener) {
        this.onDecodeListener = listener;
    }

    public void startAudio(int type, int mode, int width, int sample_rate) throws IOException {
        synchronized (lifecycleLock) {
            // 若上一次尚未完全停止，先内部 stop 一次，避免旧 codec/track/executor 泄漏或与新实例并存
            stopAudioLocked();
            audioChannelConfig = mode == 1 ? AudioFormat.CHANNEL_OUT_STEREO : AudioFormat.CHANNEL_OUT_MONO;
            audioPcmFormat = width == 1 ? AudioFormat.ENCODING_PCM_16BIT : AudioFormat.ENCODING_PCM_8BIT;
            audioSampleRate = getSampleRate(sample_rate);
            int channel = mode + 1;
            initAudio(type, channel);
        }
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
            running = true;
            Log.d(TAG, "start audio codec " + adts_data[0] + " " + adts_data[1]);
            startAudioPlayThread();
        }
    }

    private void startAudioPlayThread() {
        // aac场景下，启动多一个线程用于播放，避免阻塞导致音频延迟
        Runnable runnable = new PlayTask();
        mAudioPlayThread = new Thread(runnable);
        mAudioPlayThread.start();
    }

    public int decoderAAC(byte[] data, int len, long pts) {
        // 快照，避免与 stopAudio 并发时读到 null
        final ExecutorService executor = mAudioExecutor;
        if (executor == null || executor.isShutdown()) return -1;
        if (!running) return -2;

        // one thread for audio decode
        try {
            executor.submit(() -> {
            try {
                if (!running) {
                    return;
                }
                synchronized (codecLock) {
                    if (!running) {
                        return;
                    }
                    // PCM format data, no need to decode, just play
                    if (mAudioCodec == null && mAudioTrack != null) {
                        try {
                            mAudioTrack.write(data, 0, len);
                        } catch (IllegalStateException ise) {
                            Log.w(TAG, "audio track state invalid: " + ise.getMessage());
                        }
                        return;
                    }
                    if (mAudioCodec == null) {
                        return;
                    }
//                Log.d(TAG, ">>>> queue audio aac data " + len + " pts " + pts);
                    // queue aac data and decode
                    try {
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
                    } catch (IllegalStateException ise) {
                        Log.w(TAG, "audio codec state invalid: " + ise.getMessage());
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
            });
        } catch (java.util.concurrent.RejectedExecutionException e) {
            // executor 已在两步检查之间被关闭，忽略即可
            return -1;
        }
        return 0;
    }

    public void stopAudio() {
        synchronized (lifecycleLock) {
            stopAudioLocked();
        }
    }

    /**
     * 实际的停止逻辑，调用方需已持有 lifecycleLock。
     * <p>
     * 顺序：running=false → 中断播放线程 → shutdownNow executor 并等待退出
     * → join 播放线程 → 锁内 release codec/track。
     */
    private void stopAudioLocked() {
        // 这里停止可能导致音频没有播放完就退出？
        running = false;

        // 先中断播放线程，使其尽快退出 dequeue 阻塞
        Thread playThread = mAudioPlayThread;
        if (playThread != null) {
            playThread.interrupt();
        }

        // 终止解码 executor 并等待任务完全退出，避免与 release 并发
        ExecutorService executor = mAudioExecutor;
        mAudioExecutor = null;
        if (executor != null) {
            executor.shutdownNow();
            try {
                executor.awaitTermination(500, java.util.concurrent.TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // 等待播放线程退出，避免其仍在 codecLock 内操作旧 codec/track
        if (playThread != null && playThread != Thread.currentThread()) {
            try {
                playThread.join(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        mAudioPlayThread = null;

        // 在锁内释放 AudioTrack 与 MediaCodec，避免与播放/解码线程并发释放
        synchronized (codecLock) {
            if (mAudioTrack != null) {
                try {
                    mAudioTrack.stop();
                } catch (Exception ignore) {
                }
                try {
                    mAudioTrack.release();
                } catch (Exception ignore) {
                }
                mAudioTrack = null;
            }
            if (mAudioCodec != null) {
                try {
                    mAudioCodec.stop();
                } catch (Exception ignore) {
                }
                try {
                    mAudioCodec.release();
                } catch (Exception ignore) {
                }
                mAudioCodec = null;
            }
        }
    }

    // one thread for audio play
    private class PlayTask implements Runnable {
        @Override
        public void run() {
            Log.i(TAG, "start audio play thread");
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

            try {
                while (!Thread.currentThread().isInterrupted()) {
                    byte[] playBuf = null;
                    long audioPts = 0;

                    synchronized (codecLock) {
                        if (!running || mAudioCodec == null || mAudioTrack == null) {
                            Log.w(TAG, "The audio component is not initialized, exit the playback thread");
                            break;
                        }

                        try {
                            // 从解码器获取输出缓冲区
                            int outputBufId = mAudioCodec.dequeueOutputBuffer(info, 100_000);

                            if (outputBufId == MediaCodec.INFO_TRY_AGAIN_LATER) {
                                continue;
                            } else if (outputBufId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                                handleFormatChange();
                                continue;
                            } else if (outputBufId == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                                Log.i(TAG, "The audio output buffer changes");
                                // 在API 21以下需要调用getOutputBuffers()更新缓冲区
                                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
                                    try {
                                        mAudioCodec.getOutputBuffers(); // 更新输出缓冲区
                                    } catch (IllegalStateException e) {
                                        Log.e(TAG, "Failed to get output buffer", e);
                                    }
                                }
                                continue;
                            } else if (outputBufId < 0) {
                                Log.w(TAG, "Invalid output buffer ID: " + outputBufId);
                                continue;
                            }

                            ByteBuffer outputBuf = mAudioCodec.getOutputBuffer(outputBufId);
                            if (outputBuf == null || info.size <= 0) {
                                mAudioCodec.releaseOutputBuffer(outputBufId, false);
                                continue;
                            }

                            playBuf = new byte[info.size];
                            outputBuf.get(playBuf);
                            mAudioCodec.releaseOutputBuffer(outputBufId, false);
                            audioPts = info.presentationTimeUs / 1000;
                        } catch (Exception e) {
                            Log.i(TAG, "Abnormal audio playback: " + e.getMessage(), e);
                            break;
                        }
                    }

                    // 锁外处理：丢帧判断、回调；播放写入仍需回到锁内，避免与 stop() 释放并发
                    if (playBuf != null && running) {
                        if (shouldDropAudioFrame(audioPts)) {
                            Log.i(TAG, String.format("丢弃音频帧(pts: %d < 视频pts: %d)", audioPts, currentVideoPts));
                            continue;
                        }
                        // 回调解码后的PCM数据
                        if (onDecodeListener != null) {
                            try {
                                Log.d(TAG, "audio playBuf: " + playBuf.length);
                                onDecodeListener.onAudioDecoded(playBuf, playBuf.length, System.currentTimeMillis());
                            } catch (Exception e) {
                                Log.e(TAG, "音频解码回调异常: " + e.getMessage(), e);
                            }
                        }
                        // 将写入放回锁内执行，避免 mAudioTrack 在锁外被 release 后发生 native 崩溃
                        synchronized (codecLock) {
                            if (running && mAudioTrack != null) {
                                playAudioData(playBuf);
                            }
                        }
                    }
                }
            } finally {
                Log.i(TAG, "Audio playback thread exits");
            }
        }

        private void handleFormatChange() {
            Log.i(TAG, "The audio format has changed, reinitialize AudioTrack");
            // 已在 PlayTask 的 codecLock 内调用；需保证旧 track 安全 release + 新 track 引用已发布
            if (mAudioTrack != null) {
                try {
                    mAudioTrack.stop();
                } catch (Exception ignore) {
                }
                try {
                    mAudioTrack.release();
                } catch (Exception ignore) {
                }
                mAudioTrack = null;
            }

            int minBufSize = AudioTrack.getMinBufferSize(audioSampleRate, audioChannelConfig, audioPcmFormat);
            AudioTrack newTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                    audioSampleRate, audioChannelConfig,
                    audioPcmFormat, 2 * minBufSize, AudioTrack.MODE_STREAM);
            newTrack.setVolume(1.5f);
            newTrack.play();
            mAudioTrack = newTrack;
        }

        private boolean shouldDropAudioFrame(long audioPts) {
            return (audioPts + AV_PTS_GAP_MS) < currentVideoPts;
        }

        private void playAudioData(byte[] audioData) {
            if (audioManager != null) {
                audioManager.setSpeakerphoneOn(isSpeakerOn);
            }

            if (mAudioTrack != null && mAudioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
                try {
                    mAudioTrack.write(audioData, 0, audioData.length);
                } catch (IllegalStateException e) {
                    Log.e(TAG, "AudioTrack写入失败: " + e.getMessage());
                }
            }
        }
    }
}
