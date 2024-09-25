package com.tencent.iotvideo.link;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.graphics.SurfaceTexture;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.tencent.iot.video.device.VideoNativeInterface;
import com.tencent.iotvideo.link.encoder.AudioEncoder;
import com.tencent.iotvideo.link.encoder.VideoEncoder;
import com.tencent.iotvideo.link.listener.OnEncodeListener;
import com.tencent.iotvideo.link.param.AudioEncodeParam;
import com.tencent.iotvideo.link.param.MicParam;
import com.tencent.iotvideo.link.param.VideoEncodeParam;
import com.tencent.iotvideo.link.util.CameraUtils;
import com.tencent.iotvideo.link.util.QualitySetting;

public class CameraRecorder implements Camera.PreviewCallback, OnEncodeListener ***REMOVED***
    private static final String TAG = "CameraEncoder";

    private Camera mCamera;
    private int mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
    private int mVideoWidth = 640;
    private int mVideoHeight = 480;
    private int mVideoFrameRate = 15;
    private int mVideoBitRate = 13000000; // about width*height*4

    private int mAudioSampleRate = 16000;
    private int mAudioBitRate = 48000;

    private VideoEncoder mVideoEncoder = null;
    private AudioEncoder mAudioEncoder = null;
    private boolean mIsRecording = false;
    private static final int MaxVisitors = 4;
    private Map<Integer, Integer> mVisitorInfo = new HashMap<Integer, Integer>(MaxVisitors);
    private Activity mActivity = null;
    private static Timer bitRateTimer;

    // for test only
//    private FileOutputStream mOutputStream = null;

    public void openCamera(SurfaceTexture surfaceTexture, Activity activity) ***REMOVED***
        try ***REMOVED***
            mActivity = activity;
            mVideoWidth = QualitySetting.getInstance(activity.getApplicationContext()).getWidth();
            mVideoHeight = QualitySetting.getInstance(activity.getApplicationContext()).getHeight();
            mVideoFrameRate = QualitySetting.getInstance(activity.getApplicationContext()).getFrameRate();
            mVideoBitRate = QualitySetting.getInstance(activity.getApplicationContext()).getBitRate() * 1000;
            // Configure and start the camera
            mCamera = Camera.open(mCameraId);
            mCamera.setDisplayOrientation(CameraUtils.getDisplayOrientation(activity, mCameraId));
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewSize(mVideoWidth, mVideoHeight);
            parameters.setPreviewFormat(ImageFormat.NV21);
            parameters.setPreviewFrameRate(mVideoFrameRate);
            mCamera.setParameters(parameters);
            mCamera.setPreviewTexture(surfaceTexture);
            mCamera.setPreviewCallback(this);
            mCamera.startPreview();
      ***REMOVED*** catch (RuntimeException | IOException e) ***REMOVED***
            e.printStackTrace();
      ***REMOVED***
  ***REMOVED***

    public void closeCamera() ***REMOVED***
        try ***REMOVED***
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
      ***REMOVED*** catch (RuntimeException e) ***REMOVED***
            e.printStackTrace();
      ***REMOVED***
  ***REMOVED***

    public void startRecording(int visitor, int res_type) ***REMOVED***
        if (mIsRecording) ***REMOVED***
            mVisitorInfo.put(new Integer(visitor), new Integer(res_type));
            return;
      ***REMOVED***
        mVisitorInfo.put(new Integer(visitor), new Integer(res_type));
        VideoEncodeParam encodeParam = new VideoEncodeParam();
        encodeParam.setHeight(mVideoHeight);
        encodeParam.setWidth(mVideoWidth);
        encodeParam.setFrameRate(mVideoFrameRate);
        encodeParam.setBitRate(mVideoBitRate);
        mVideoEncoder = new VideoEncoder(encodeParam, mActivity);
        mVideoEncoder.setEncoderListener(this);

        MicParam micParam = new MicParam();
        micParam.setAudioFormat(AudioFormat.ENCODING_PCM_16BIT);
        micParam.setChannelConfig(AudioFormat.CHANNEL_IN_MONO);
        micParam.setSampleRateInHz(mAudioSampleRate);
        micParam.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION);
        AudioEncodeParam audioEncodeParam = new AudioEncodeParam();
        audioEncodeParam.setBitRate(mAudioBitRate);
        mAudioEncoder = new AudioEncoder(micParam, audioEncodeParam, true, true);
        mAudioEncoder.setOnEncodeListener(this);
        mAudioEncoder.start();
        mIsRecording = true;
        Log.d(TAG, "start camera recording");
        startBitRateAdapter();
  ***REMOVED***

    public void stopRecording(int visitor, int res_type) ***REMOVED***
        if (!mIsRecording) ***REMOVED***
            return;
      ***REMOVED***

        mVisitorInfo.remove(Integer.valueOf(visitor));
        if (!mVisitorInfo.isEmpty())
            return;

        mIsRecording = false;
        mVideoEncoder.stop();
        mVideoEncoder = null;

        mAudioEncoder.stop();
        mAudioEncoder = null;
        Log.d(TAG, "stop camera recording");
        stopBitRateAdapter();
  ***REMOVED***

    @Override
    public void onAudioEncoded(byte[] datas, long pts, long seq) ***REMOVED***
//        Log.d(TAG, "encoded audio data len " + datas.length + " pts " + pts);
        if (mIsRecording) ***REMOVED***
            for (Map.Entry<Integer, Integer> entry : mVisitorInfo.entrySet()) ***REMOVED***
                int visitor = entry.getKey().intValue();
                int res_type = entry.getValue().intValue();
                int ret = VideoNativeInterface.getInstance().sendAvtAudioData(datas, pts, seq, visitor, res_type);
                if (ret != 0)
                    Log.e(TAG, "sendAudioData to visitor " + visitor + " failed: " + ret);
          ***REMOVED***
      ***REMOVED***
  ***REMOVED***

    private int stat_cnt = 0;

    @Override
    public void onVideoEncoded(byte[] datas, long pts, long seq, boolean isKeyFrame) ***REMOVED***
//        Log.d(TAG, "encoded video data len " + datas.length + " pts " + pts);

        if (mIsRecording) ***REMOVED***
            for (Map.Entry<Integer, Integer> entry : mVisitorInfo.entrySet()) ***REMOVED***
                int visitor = entry.getKey().intValue();
                int res_type = entry.getValue().intValue();
                VideoNativeInterface iv = VideoNativeInterface.getInstance();
                int ret = iv.sendAvtVideoData(datas, pts, seq, isKeyFrame, visitor, res_type);
                if (ret != 0) ***REMOVED***
                    int buf_size = iv.getSendStreamBuf(visitor, res_type);
                    Log.e(TAG, "sendVideoData to visitor " + visitor + " failed: " + ret + " buf size " + buf_size);
              ***REMOVED***

                if ((stat_cnt++ % 50) == 0) ***REMOVED***
                    int buf_size = iv.getSendStreamBuf(visitor, res_type);
                    int link_mode = iv.getSendStreamStatus(visitor, res_type);
                    Log.d(TAG, "visitor " + visitor + " buf size " + buf_size + " link mode " + link_mode);
              ***REMOVED***
          ***REMOVED***
      ***REMOVED***
  ***REMOVED***

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) ***REMOVED***
        if (!mIsRecording || mVideoEncoder == null) ***REMOVED***
            return;
      ***REMOVED***
        mVideoEncoder.encoderH264(data, false);
  ***REMOVED***


    public class AdapterBitRateTask extends TimerTask ***REMOVED***
        @Override
        public void run() ***REMOVED***
            System.out.println("检测时间到:" + new Date());

            if (mVideoEncoder != null) ***REMOVED***


                int bufsize = VideoNativeInterface.getInstance().getSendStreamBuf(0, 1);
                int p2p_wl_avg = VideoNativeInterface.getInstance().getAvgMaxMin(bufsize);
                int now_video_rate = mVideoEncoder.getVideoBitRate();
                int now_frame_rate = mVideoEncoder.getVideoFrameRate();

                Log.e(TAG, "send_bufsize==" + bufsize + ",now_video_rate==" + now_video_rate + ",avg_index==" + p2p_wl_avg + ",now_frame_rate==" + now_frame_rate);

                // 降码率
                // 当发现p2p的水线超过一定值时，降低视频码率，这是一个经验值，一般来说要大于 [视频码率/2]
                // 实测设置为 80%视频码率 到 120%视频码率 比较理想
                // 在10组数据中，获取到平均值，并将平均水位与当前码率比对。

                int video_rate_byte = (now_video_rate / 8) * 3 / 4;
                if (p2p_wl_avg > video_rate_byte) ***REMOVED***

                    mVideoEncoder.setVideoBitRate(now_video_rate / 2);
                    mVideoEncoder.setVideoFrameRate(now_frame_rate / 3);

              ***REMOVED*** else if (p2p_wl_avg < (now_video_rate / 8) / 3) ***REMOVED***

                    // 升码率
                    // 测试发现升码率的速度慢一些效果更好
                    // p2p水线经验值一般小于[视频码率/2]，网络良好的情况会小于 [视频码率/3] 甚至更低
                    mVideoEncoder.setVideoBitRate(now_video_rate + (now_video_rate - p2p_wl_avg * 8) / 5);
                    mVideoEncoder.setVideoFrameRate(now_frame_rate * 5 / 4);
              ***REMOVED***
          ***REMOVED***
      ***REMOVED***
  ***REMOVED***


    private void startBitRateAdapter() ***REMOVED***

//        IvNativeInterface.getInstance().resetAvg();
        bitRateTimer = new Timer();
        bitRateTimer.schedule(new AdapterBitRateTask(), 3000, 1000);
  ***REMOVED***

    private void stopBitRateAdapter() ***REMOVED***
        if (bitRateTimer != null) ***REMOVED***
            bitRateTimer.cancel();
      ***REMOVED***
  ***REMOVED***
}
