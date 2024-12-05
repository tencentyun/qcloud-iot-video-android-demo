package com.tencent.iotvideo.link;

import android.util.Log;
import android.util.Range;

import com.example.ivdemo.annotations.DynamicBitRateType;
import com.tencent.iot.video.device.VideoNativeInterface;
import com.tencent.iot.video.device.model.IvP2pSendInfo;
import com.tencent.iotvideo.link.encoder.VideoEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

public class AdapterBitRateTask extends TimerTask {
    private final String TAG = AdapterBitRateTask.class.getSimpleName();
    private boolean exceedLowMark = false;
    private VideoEncoder videoEncoder;
    @DynamicBitRateType
    private int dynamicBitRateType;

    private int visitor;
    private int channel;
    private int videoResType;

    public void setInfo(int visitor, int channel, int videoResType) {
        this.visitor = visitor;
        this.channel = channel;
        this.videoResType = videoResType;
    }

    public void setVideoEncoder(VideoEncoder videoEncoder) {
        this.videoEncoder = videoEncoder;
    }

    public void setDynamicBitRateType(int dynamicBitRateType) {
        this.dynamicBitRateType = dynamicBitRateType;
    }

    @Override
    public void run() {
        System.out.println("检测时间到:" + System.currentTimeMillis());
//                int visitor = entry.getKey().intValue();
//                int res_type = entry.getValue().intValue();
        if (videoEncoder != null) {
            if (dynamicBitRateType == DynamicBitRateType.WATER_LEVEL_TYPE) {
                int bufSize = VideoNativeInterface.getInstance().getSendStreamBuf(visitor, channel, videoResType);
                int p2p_wl_avg = VideoNativeInterface.getInstance().getAvgMaxMin(bufSize);
                int now_video_rate = videoEncoder.getVideoBitRate();
                int now_frame_rate = videoEncoder.getVideoFrameRate();
                Log.e(TAG, "WATER_LEVEL_TYPE send_bufsize==" + bufSize + ",now_video_rate==" + now_video_rate + ",avg_index==" + p2p_wl_avg + ",now_frame_rate==" + now_frame_rate);
                // 降码率
                // 当发现p2p的水线超过一定值时，降低视频码率，这是一个经验值，一般来说要大于 [视频码率/2]
                // 实测设置为 80%视频码率 到 120%视频码率 比较理想
                // 在10组数据中，获取到平均值，并将平均水位与当前码率比对。

                int video_rate_byte = (now_video_rate / 8) * 3 / 4;
                if (p2p_wl_avg > video_rate_byte) {

                    videoEncoder.setVideoBitRate(now_video_rate / 2);
                    videoEncoder.setVideoFrameRate(now_frame_rate / 3);

                } else if (p2p_wl_avg < (now_video_rate / 8) / 3) {

                    // 升码率
                    // 测试发现升码率的速度慢一些效果更好
                    // p2p水线经验值一般小于[视频码率/2]，网络良好的情况会小于 [视频码率/3] 甚至更低
                    videoEncoder.setVideoBitRate(now_video_rate + (now_video_rate - p2p_wl_avg * 8) / 5);
                    videoEncoder.setVideoFrameRate(now_frame_rate * 5 / 4);
                }
            } else if (dynamicBitRateType == DynamicBitRateType.INTERNET_SPEED_TYPE) {
                IvP2pSendInfo ivP2pSendInfo = VideoNativeInterface.getInstance().getSendStreamStatus(visitor, channel, videoResType);
                int bufSize = VideoNativeInterface.getInstance().getSendStreamBuf(visitor, channel, videoResType);
                int p2p_wl_avg = getAvgMaxMin(ivP2pSendInfo.getAveSentRate());
                int now_video_rate = videoEncoder.getVideoBitRate();
                int now_frame_rate = videoEncoder.getVideoFrameRate();
                Range<Double> nowBitRateInterval = videoEncoder.getBitRateInterval();
                Log.e(TAG, "INTERNET_SPEED_TYPE now_video_rate==" + now_video_rate + ",avg_index==" + p2p_wl_avg + ",now_frame_rate==" + now_frame_rate + " link mode " + ivP2pSendInfo.getLinkMode() + "  instNetRate:" + ivP2pSendInfo.getInstNetRate() + "   aveSentRate:" + ivP2pSendInfo.getAveSentRate() + "   sumSentAcked:" + ivP2pSendInfo.getSumSentAcked());
                int new_video_rate = 0;
                int new_frame_rate = 0;
                Log.e(TAG, "AveSentRate:" + ivP2pSendInfo.getAveSentRate() + "   now_video_rate/8:" + now_video_rate / 8);
                //判断当前码率/8和网速，如果码率/8大于当前网速，并且两次水位值都大于20k，开始降码率
                if (ivP2pSendInfo.getAveSentRate() < (double) now_video_rate / 8 && exceedLowMark && (exceedLowMark = bufSize > 20 * 1024)) {
                    // 降码率
                    new_video_rate = (int) (now_video_rate * 0.75);
                    new_frame_rate = now_frame_rate * 4 / 5;
                } else if (bufSize < 20 * 1024) { //当前水位值小于20k，开始升码率
                    if (now_video_rate < nowBitRateInterval.getUpper() / 2) {
                        new_video_rate = (int) (now_video_rate * 1.1);
                        new_frame_rate = now_frame_rate * 5 / 4;
                    }
                }
                if (new_video_rate < nowBitRateInterval.getLower() && now_video_rate > nowBitRateInterval.getLower()) {
                    new_video_rate = (int) (now_video_rate * 0.8f);
                } else if (new_video_rate > nowBitRateInterval.getUpper() && now_video_rate < nowBitRateInterval.getLower()) {
                    new_video_rate = (int) (now_video_rate * 1.1f);
                }
                if (new_video_rate != 0) {
                    videoEncoder.setVideoBitRate(new_video_rate);
                }
                if (new_frame_rate != 0) {
                    videoEncoder.setVideoFrameRate(new_frame_rate);
                }
                Log.d(TAG, "new_video_rate:" + new_video_rate + "  VideoBitRate:" + videoEncoder.getVideoBitRate());
            }
        }
    }

    private List<Integer> list = new ArrayList<>(10);

    /**
     * 存入队列同时删除队列内最旧的一个数值，去掉一个最高值去掉一个最低值，计算平均值，算出的平均值可用于控制码率，一般而言此数值与视频码率相近，当发现平均网速低于视频码率时主动降低视频码率到一个比平均网速更低的值。
     *
     * @param bufSize
     * @return
     */
    private int getAvgMaxMin(int bufSize) {
        int sum = 0;
        int max = Integer.MIN_VALUE;
        int min = Integer.MAX_VALUE;

        if (list.size() >= 10) {
            list.remove(0);
        }
        list.add(bufSize);
        if (list.size() == 1) return bufSize;
        if (list.size() == 2) return (list.get(0) + list.get(1)) / list.size();
        for (int item : list) {
            sum += item;
            max = Math.max(max, item);
            min = Math.min(min, item);
        }
        sum = sum - max - min;

        return sum / (list.size() - 2);
    }
}
