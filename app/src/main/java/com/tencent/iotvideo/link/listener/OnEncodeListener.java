package com.tencent.iotvideo.link.listener;


public interface OnEncodeListener ***REMOVED***
    void onAudioEncoded(byte[] datas, long pts, long seq);
    void onVideoEncoded(byte[] datas, long pts, long seq, boolean isKeyFrame);
}
