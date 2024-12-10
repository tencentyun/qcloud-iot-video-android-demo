package com.tencent.iotvideo.link;

import static com.tencent.iotvideo.link.util.UtilsKt.getBitRateIntervalByPixel;
import static com.tencent.iotvideo.link.util.UtilsKt.getInfo;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.text.TextUtils;
import android.util.Log;
import android.util.Range;
import android.view.TextureView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.example.ivdemo.annotations.DynamicBitRateType;
import com.tencent.iot.video.device.VideoNativeInterface;
import com.tencent.iot.video.device.annotations.VideoResType;
import com.tencent.iot.video.device.model.IvP2pSendInfo;
import com.tencent.iotvideo.link.encoder.AudioEncoder;
import com.tencent.iotvideo.link.encoder.VideoEncoder;
import com.tencent.iotvideo.link.listener.OnEncodeListener;
import com.tencent.iotvideo.link.param.AudioEncodeParam;
import com.tencent.iotvideo.link.param.MicParam;
import com.tencent.iotvideo.link.param.VideoEncodeParam;
import com.tencent.iotvideo.link.util.CameraUtils;
import com.tencent.iotvideo.link.util.QualitySetting;

public class CameraRecorder implements Camera.PreviewCallback, OnEncodeListener {
    private static final String TAG = "CameraEncoder";

    private Camera mCamera;
    private int mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
    public int mVideoWidth = 640;
    public int mVideoHeight = 480;
    private int mVideoFrameRate = 15;
    private int mVideoBitRate = 13000000; // about width*height*4

    private int mAudioSampleRate = 16000;
    private int mAudioBitRate = 48000;

    private VideoEncoder mVideoEncoder = null;
    private AudioEncoder mAudioEncoder = null;
    private boolean isMuted = false;
    private boolean mIsRecording = false;
    private static final int MaxVisitors = 4;
    private Map<Integer, Integer> mVisitorInfo = new HashMap<Integer, Integer>(MaxVisitors);
    private Activity mActivity = null;
    private static Timer bitRateTimer;

    @VideoResType
    private int videoResType;

    private int visitor;

    public boolean isRunning = false;

    // for test only
    private boolean isSaveRecord = false;

    private FileOutputStream fos;
    private FileOutputStream nv21fos;

    private String speakH264FilePath = "/sdcard/video.h264";
    private String speakNv21FilePath = "/sdcard/video.nv21";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public void isSaveRecord(boolean isSaveRecord) {
        this.isSaveRecord = isSaveRecord;
        recordSpeakH264(isSaveRecord);
    }

    public void openCamera(TextureView textureView, Activity activity) {
        try {
            mActivity = activity;
            mVideoWidth = QualitySetting.getInstance(activity.getApplicationContext()).getWidth();
            mVideoHeight = QualitySetting.getInstance(activity.getApplicationContext()).getHeight();
            mVideoFrameRate = QualitySetting.getInstance(activity.getApplicationContext()).getFrameRate();
//            adjustAspectRatio(mVideoWidth, mVideoHeight, textureView, null, null);
            Double[] range = getBitRateIntervalByPixel(mVideoWidth, mVideoHeight);
            mVideoBitRate = (int) ((range[0] + range[1]) / 2);
            // Configure and start the camera
            mCamera = Camera.open(mCameraId);
            mCamera.setDisplayOrientation(CameraUtils.getDisplayOrientation(activity, mCameraId));
            Camera.Parameters parameters = mCamera.getParameters();
            if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            } else if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }
            parameters.setPreviewSize(mVideoWidth, mVideoHeight);
            parameters.setPreviewFormat(ImageFormat.YV12);
            parameters.setPreviewFrameRate(mVideoFrameRate);
            mCamera.setParameters(parameters);
            mCamera.setPreviewTexture(textureView.getSurfaceTexture());
            mCamera.setPreviewCallback(this);
            mCamera.startPreview();
            isRunning = true;
        } catch (RuntimeException | IOException e) {
            e.printStackTrace();
        }
    }

    public void switchCamera(TextureView textureView, Activity activity) {
        if (mCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
            mCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
        } else {
            mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
        }
        closeCamera();
        openCamera(textureView, activity);
    }

    public void closeCamera() {
        try {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            isRunning = false;
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    public void startRecording(int visitor, int res_type) {
        this.videoResType = res_type;
        this.visitor = visitor;
        if (mIsRecording) {
            mVisitorInfo.put(new Integer(visitor), new Integer(res_type));
            return;
        }
        mVisitorInfo.put(new Integer(visitor), new Integer(res_type));
        VideoEncodeParam encodeParam = new VideoEncodeParam();
        encodeParam.setHeight(mVideoHeight);
        encodeParam.setWidth(mVideoWidth);
        encodeParam.setFrameRate(mVideoFrameRate);
        encodeParam.setBitRate(mVideoBitRate);
        mVideoEncoder = new VideoEncoder(encodeParam);
        mVideoEncoder.setEncoderListener(this);
        mVideoEncoder.start();
        MicParam micParam = new MicParam();
        micParam.setAudioFormat(AudioFormat.ENCODING_PCM_16BIT);
        micParam.setChannelConfig(AudioFormat.CHANNEL_IN_MONO);
        micParam.setSampleRateInHz(mAudioSampleRate);
        micParam.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION);
        AudioEncodeParam audioEncodeParam = new AudioEncodeParam();
        audioEncodeParam.setBitRate(mAudioBitRate);
        mAudioEncoder = new AudioEncoder(micParam, audioEncodeParam, true, true);
        mAudioEncoder.setOnEncodeListener(this);
        mAudioEncoder.setMuted(isMuted);
        mAudioEncoder.start();
        mIsRecording = true;
        Log.d(TAG, "start camera recording");
        startBitRateAdapter();
    }

    public void setMuted(boolean muted) {
        isMuted = muted;
        if (mAudioEncoder != null) {
            mAudioEncoder.setMuted(isMuted);
        }
    }

    public boolean isMuted() {
        if (mAudioEncoder != null) {
            return mAudioEncoder.isMuted();
        }
        return isMuted;
    }

    public void stopRecording(int visitor, int res_type) {
        if (!mIsRecording) {
            return;
        }

        mVisitorInfo.remove(Integer.valueOf(visitor));
        if (!mVisitorInfo.isEmpty()) return;

        mIsRecording = false;
        mVideoEncoder.stop();
        mVideoEncoder = null;

        mAudioEncoder.stop();
        mAudioEncoder = null;
        Log.d(TAG, "stop camera recording");
        stopBitRateAdapter();
    }

    @Override
    public void onAudioEncoded(byte[] datas, long pts, long seq) {
//        Log.d(TAG, "encoded audio data len " + datas.length + " pts " + pts);
        if (mIsRecording) {
            for (Map.Entry<Integer, Integer> entry : mVisitorInfo.entrySet()) {
                int visitor = entry.getKey().intValue();
                int res_type = entry.getValue().intValue();
                int ret = VideoNativeInterface.getInstance().sendAvtAudioData(datas, pts, seq, visitor, 0, res_type);
                if (ret != 0) Log.e(TAG, "sendAudioData to visitor " + visitor + " failed: " + ret);
            }
        }
    }

    private int stat_cnt = 0;

    @Override
    public void onVideoEncoded(byte[] datas, long pts, long seq, boolean isKeyFrame) {
//        Log.d(TAG, "encoded video data len " + datas.length + " pts " + pts);

        if (mIsRecording) {
            for (Map.Entry<Integer, Integer> entry : mVisitorInfo.entrySet()) {
                int visitor = entry.getKey().intValue();
                int res_type = entry.getValue().intValue();
                VideoNativeInterface iv = VideoNativeInterface.getInstance();
                int ret = iv.sendAvtVideoData(datas, pts, seq, isKeyFrame, visitor, 0, res_type);
                if (ret != 0) {
                    int buf_size = iv.getSendStreamBuf(visitor, 0, res_type);
                    Log.e(TAG, "sendVideoData to visitor " + visitor + " failed: " + ret + " buf size " + buf_size);
                }

                if ((stat_cnt++ % 50) == 0) {
                    int buf_size = iv.getSendStreamBuf(visitor, 0, res_type);
                    IvP2pSendInfo ivP2pSendInfo = iv.getSendStreamStatus(visitor, 0, res_type);
                    Log.d(TAG, "visitor " + visitor + " buf size " + buf_size + " link mode " + ivP2pSendInfo.getLinkMode() + "  instNetRate:" + ivP2pSendInfo.getInstNetRate() + "   aveSentRate:" + ivP2pSendInfo.getAveSentRate() + "   sumSentAcked:" + ivP2pSendInfo.getSumSentAcked());
                }
                if (isSaveRecord) {
                    if (executor.isShutdown()) return;
                    executor.submit(() -> {
                        if (fos != null) {
                            try {
                                fos.write(datas);
                                fos.flush();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (!mIsRecording || mVideoEncoder == null) {
            return;
        }
        mVideoEncoder.encoderH264(data, mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT);
        if (isSaveRecord) {
            if (executor.isShutdown()) return;
            executor.submit(() -> {
                if (nv21fos != null) {
                    try {
                        nv21fos.write(data);
                        nv21fos.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    @DynamicBitRateType
    private int dynamicBitRateType = DynamicBitRateType.INTERNET_SPEED_TYPE;


    public class AdapterBitRateTask extends TimerTask {
        private boolean exceedLowMark = false;

        @Override
        public void run() {
            System.out.println("检测时间到:" + System.currentTimeMillis());
//                int visitor = entry.getKey().intValue();
//                int res_type = entry.getValue().intValue();
            if (mVideoEncoder != null) {
                if (dynamicBitRateType == DynamicBitRateType.WATER_LEVEL_TYPE) {
                    int bufSize = VideoNativeInterface.getInstance().getSendStreamBuf(visitor, 0, videoResType);
                    int p2p_wl_avg = VideoNativeInterface.getInstance().getAvgMaxMin(bufSize);
                    int now_video_rate = mVideoEncoder.getVideoBitRate();
                    int now_frame_rate = mVideoEncoder.getVideoFrameRate();
                    Log.e(TAG, "WATER_LEVEL_TYPE send_bufsize==" + bufSize + ",now_video_rate==" + now_video_rate + ",avg_index==" + p2p_wl_avg + ",now_frame_rate==" + now_frame_rate);
                    // 降码率
                    // 当发现p2p的水线超过一定值时，降低视频码率，这是一个经验值，一般来说要大于 [视频码率/2]
                    // 实测设置为 80%视频码率 到 120%视频码率 比较理想
                    // 在10组数据中，获取到平均值，并将平均水位与当前码率比对。

                    int video_rate_byte = (now_video_rate / 8) * 3 / 4;
                    if (p2p_wl_avg > video_rate_byte) {

                        mVideoEncoder.setVideoBitRate(now_video_rate / 2);
                        mVideoEncoder.setVideoFrameRate(now_frame_rate / 3);

                    } else if (p2p_wl_avg < (now_video_rate / 8) / 3) {

                        // 升码率
                        // 测试发现升码率的速度慢一些效果更好
                        // p2p水线经验值一般小于[视频码率/2]，网络良好的情况会小于 [视频码率/3] 甚至更低
                        mVideoEncoder.setVideoBitRate(now_video_rate + (now_video_rate - p2p_wl_avg * 8) / 5);
                        mVideoEncoder.setVideoFrameRate(now_frame_rate * 5 / 4);
                    }
                } else if (dynamicBitRateType == DynamicBitRateType.INTERNET_SPEED_TYPE) {
                    IvP2pSendInfo ivP2pSendInfo = VideoNativeInterface.getInstance().getSendStreamStatus(visitor, 0, videoResType);
                    int bufSize = VideoNativeInterface.getInstance().getSendStreamBuf(visitor, 0, videoResType);
                    int now_video_rate = mVideoEncoder.getVideoBitRate();
                    int now_frame_rate = mVideoEncoder.getVideoFrameRate();
                    Double[] nowBitRateInterval = mVideoEncoder.getBitRateInterval();
                    Log.d(TAG, "INTERNET_SPEED_TYPE bufsize=" + bufSize + "video_rate/8*0.8=" + now_video_rate / 8 * 0.8 + "  video_rate=" + now_video_rate + "  frame_rate=" + now_frame_rate);
                    int new_video_rate = 0;
                    int new_frame_rate = 0;
                    //判断当前码率/8和网速，如果码率/8大于当前网速，并且两次水位值都大于20k，开始降码率
                    if (ivP2pSendInfo == null) return;
                    if (ivP2pSendInfo.getAveSentRate() < (double) now_video_rate / 8 * 0.9 && exceedLowMark && (exceedLowMark = bufSize > 20 * 1024)) {
                        // 降码率
                        new_video_rate = (int) (now_video_rate * 0.75);
                        new_frame_rate = now_frame_rate * 4 / 5;
//                        Integer[] res = getInfo(false);
//                        new_frame_rate = res[0];
//                        new_video_rate = res[1];
                    } else if (bufSize < 20 * 1024) { //当前水位值小于20k，开始升码率
                        if (now_video_rate < nowBitRateInterval[1] / 2) {
                            new_video_rate = (int) (now_video_rate * 1.1);
                            new_frame_rate = now_frame_rate * 5 / 4;
                        }
//                        Integer[] res = getInfo(true);
//                        new_frame_rate = res[0];
//                        new_video_rate = res[1];
                    } else {
                        return;
                    }
                    if (new_video_rate < nowBitRateInterval[0] && now_video_rate > nowBitRateInterval[0]) {
                        new_video_rate = (int) (now_video_rate * 0.8f);
                    } else if (new_video_rate > nowBitRateInterval[1] && now_video_rate < nowBitRateInterval[0]) {
                        new_video_rate = (int) (now_video_rate * 1.1f);
                    }
                    if (new_video_rate != 0) {
                        mVideoEncoder.setVideoBitRate(new_video_rate);
                    }
                    if (new_frame_rate != 0) {
                        mVideoEncoder.setVideoFrameRate(new_frame_rate);
                    }
                    Log.d(TAG, "new_video_rate:" + new_video_rate + "  VideoBitRate:" + mVideoEncoder.getVideoBitRate());
                }
            }
        }
    }


    List<Integer> list = new ArrayList<>(10);

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


    private void startBitRateAdapter() {

//        IvNativeInterface.getInstance().resetAvg();
        bitRateTimer = new Timer();
        bitRateTimer.schedule(new AdapterBitRateTask(), 3000, 1000);
    }

    private void stopBitRateAdapter() {
        if (bitRateTimer != null) {
            bitRateTimer.cancel();
        }
    }

    public void recordSpeakH264(boolean isRecord) {
        if (isRecord) {
            if (!TextUtils.isEmpty(speakH264FilePath)) {
                try {
                    File file = getFile(speakH264FilePath);
                    fos = new FileOutputStream(file);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, speakH264FilePath + "临时缓存文件未找到");
                }
            }
            if (!TextUtils.isEmpty(speakNv21FilePath)) {
                try {
                    File file = getFile(speakNv21FilePath);
                    nv21fos = new FileOutputStream(file);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, speakNv21FilePath + "临时缓存文件未找到");
                }
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
}
