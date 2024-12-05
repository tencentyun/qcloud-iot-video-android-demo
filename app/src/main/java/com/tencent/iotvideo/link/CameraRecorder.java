package com.tencent.iotvideo.link;

import static com.tencent.iotvideo.link.util.UtilsKt.getBitRateIntervalByPixel;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
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
    private final Map<Integer, Pair<Integer, Integer>> mVisitorInfo = new HashMap<>(MaxVisitors);
    private static Timer bitRateTimer;

    public boolean isRunning = false;

    // for test only
    private boolean isSaveRecord = false;

    private FileOutputStream fos;
    private FileOutputStream nv21fos;

    private String speakH264FilePath = "/sdcard/video.h264";
    private String speakNv21FilePath = "/sdcard/video.nv21";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private OnEncodeListener encodeListener;

    public void setOnEncodeListener(OnEncodeListener listener) {
        this.encodeListener = listener;
    }

    public void isSaveRecord(boolean isSaveRecord) {
        this.isSaveRecord = isSaveRecord;
        recordSpeakH264(isSaveRecord);
    }

    public void openCamera(TextureView textureView, Activity activity) {
        try {
            mVideoWidth = QualitySetting.getInstance(activity.getApplicationContext()).getWidth();
            mVideoHeight = QualitySetting.getInstance(activity.getApplicationContext()).getHeight();
            mVideoFrameRate = QualitySetting.getInstance(activity.getApplicationContext()).getFrameRate();
//            adjustAspectRatio(mVideoWidth, mVideoHeight, textureView, null, null);
            Range<Double> range = getBitRateIntervalByPixel(mVideoWidth, mVideoHeight);
            mVideoBitRate = (int) ((range.getUpper() + range.getLower()) / 2);
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
            parameters.setPreviewFormat(ImageFormat.NV21);
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

    public void startRecording(int visitor, int channel, int res_type) {
        if (mIsRecording) {
            mVisitorInfo.put(visitor, new Pair<>(channel, res_type));
            return;
        }
        mVisitorInfo.put(visitor, new Pair<>(channel, res_type));
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
        startBitRateAdapter(visitor, channel, res_type);
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
            for (Map.Entry<Integer, Pair<Integer, Integer>> entry : mVisitorInfo.entrySet()) {
                int visitor = entry.getKey().intValue();
                int channel = entry.getValue().first;
                int res_type = entry.getValue().second;
                int ret = VideoNativeInterface.getInstance().sendAvtAudioData(datas, pts, seq, visitor, channel, res_type);
                if (ret != 0) Log.e(TAG, "sendAudioData to visitor " + visitor + " failed: " + ret);
            }
            if (encodeListener != null) {
                encodeListener.onAudioEncoded(datas, pts, seq);
            }
        }
    }

    private int stat_cnt = 0;

    @Override
    public void onVideoEncoded(byte[] datas, long pts, long seq, boolean isKeyFrame) {
//        Log.d(TAG, "encoded video data len " + datas.length + " pts " + pts);
        if (mIsRecording) {
            for (Map.Entry<Integer, Pair<Integer, Integer>> entry : mVisitorInfo.entrySet()) {
                int visitor = entry.getKey().intValue();
                int channel = entry.getValue().first;
                int res_type = entry.getValue().second;
                VideoNativeInterface iv = VideoNativeInterface.getInstance();
                int ret = iv.sendAvtVideoData(datas, pts, seq, isKeyFrame, visitor, channel, res_type);
                if (ret != 0) {
                    int buf_size = iv.getSendStreamBuf(visitor, channel, res_type);
                    Log.e(TAG, "sendVideoData to visitor " + visitor + " failed: " + ret + " buf size " + buf_size);
                }

                if ((stat_cnt++ % 50) == 0) {
                    int buf_size = iv.getSendStreamBuf(visitor, channel, res_type);
                    IvP2pSendInfo ivP2pSendInfo = iv.getSendStreamStatus(visitor, channel, res_type);
                    Log.d(TAG, "visitor " + visitor + " buf size " + buf_size + " link mode " + ivP2pSendInfo.getLinkMode() + "  instNetRate:" + ivP2pSendInfo.getInstNetRate() + "   aveSentRate:" + ivP2pSendInfo.getAveSentRate() + "   sumSentAcked:" + ivP2pSendInfo.getSumSentAcked());
                }
            }
            if (encodeListener != null) {
                encodeListener.onVideoEncoded(datas, pts, seq, isKeyFrame);
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

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (!mIsRecording || mVideoEncoder == null) {
            return;
        }
        mVideoEncoder.encoderH264(data, false);
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

    private void startBitRateAdapter(int visitor, int channel, int res_type) {
        VideoNativeInterface.getInstance().resetAvg();
        bitRateTimer = new Timer();
        AdapterBitRateTask task = new AdapterBitRateTask();
        task.setVideoEncoder(mVideoEncoder);
        task.setDynamicBitRateType(DynamicBitRateType.INTERNET_SPEED_TYPE);
        task.setInfo(visitor, channel, res_type);
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
