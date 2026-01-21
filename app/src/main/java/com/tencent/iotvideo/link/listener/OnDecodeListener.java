package com.tencent.iotvideo.link.listener;

/**
 * 音频解码监听器
 */
public interface OnDecodeListener {
    /**
     * 音频解码后的PCM数据回调
     * @param pcmData PCM音频数据
     * @param length 数据长度
     * @param pts 时间戳
     */
    void onAudioDecoded(byte[] pcmData, int length, long pts);
}
