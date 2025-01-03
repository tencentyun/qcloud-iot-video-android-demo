package com.tencent.iotvideo.link.util

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat

private val softEncoderList = arrayOf("OMX.google.h264.encoder")
private val hardEncoderList = mutableListOf(
    "OMX.qcom.video.encoder.avc",
    "OMX.MTK.VIDEO.ENCODER.AVC",
    "OMX.hisi.video.encoder.avc",
    "c2.android.avc.encoder"
)

/**
 * 获取支持的编码器
 * @param type 0，软编；1、硬编
 */
fun getSupportVideoEncoder(type: Int): List<MediaCodecInfo> {
    val mediaCodecList = arrayListOf<MediaCodecInfo>()
    val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
    val codecInfos = codecList.codecInfos
    for (codecInfo in codecInfos) {
        if (!codecInfo.isEncoder) continue
        codecInfo.supportedTypes.forEachIndexed { index, codecType ->
            if (codecType == MediaFormat.MIMETYPE_VIDEO_AVC) {
                if (type == 1 && hardEncoderList.contains(codecInfo.name)) {
                    mediaCodecList.add(codecInfo)
                } else if (type == 0 && softEncoderList.contains(codecInfo.name)) {
                    mediaCodecList.add(codecInfo)
                }
            }
        }
    }
    return mediaCodecList;
}