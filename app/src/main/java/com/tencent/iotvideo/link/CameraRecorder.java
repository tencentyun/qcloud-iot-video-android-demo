package com.tencent.iotvideo.link;

import static com.tencent.iotvideo.link.util.UtilsKt.getBitRateIntervalByPixel;

import android.app.Activity;
import android.hardware.Camera;
import android.media.AudioFormat;
import android.media.MediaCodecInfo;
import android.media.MediaRecorder;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.util.Range;
import android.view.TextureView;

import com.example.ivdemo.annotations.DynamicBitRateType;
import com.tencent.iot.video.device.VideoNativeInterface;
import com.tencent.iot.video.device.model.IvP2pSendInfo;
import com.tencent.iotvideo.link.encoder.AudioEncoder;
import com.tencent.iotvideo.link.encoder.VideoEncoder;
import com.tencent.iotvideo.link.listener.OnEncodeListener;
import com.tencent.iotvideo.link.param.AudioEncodeParam;
import com.tencent.iotvideo.link.param.MicParam;
import com.tencent.iotvideo.link.param.VideoEncodeParam;
import com.tencent.iotvideo.link.util.CameraUtils;
import com.tencent.iotvideo.link.util.QualitySetting;
import com.tencent.iotvideo.link.util.UtilsKt;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraRecorder implements Camera.PreviewCallback, OnEncodeListener {
    private static final String TAG = "CameraEncoder";

    private int cameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
    private Camera camera;
    public int mVideoWidth = 640;
    public int mVideoHeight = 480;

    private int mAudioSampleRate = 16000;
    private int mAudioBitRate = 48000;
    private VideoEncodeParam videoEncodeParam;
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
    private String speakH264FilePath = "/sdcard/video.h264";

    private Activity context;

    private TextureView previewView;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private OnEncodeListener encodeListener;

    public void setOnEncodeListener(OnEncodeListener listener) {
        this.encodeListener = listener;
    }

    public void init(Activity context) {
        this.context = context;
        QualitySetting setting = QualitySetting.getInstance(context.getApplicationContext());
        int videoWidth = setting.getResolutionEntity().getWidth();
        int videoHeight = setting.getResolutionEntity().getHeight();
        int videoFrameRate = setting.getFrameRate();
        int videoEncodeType = setting.getEncodeType();
        MediaCodecInfo info = setting.getMediaCodecInfo();
        Range<Double> range = getBitRateIntervalByPixel(videoWidth, videoHeight);
        int videoBitRate = (int) ((range.getUpper() + range.getLower()) / 2);
        videoEncodeParam = new VideoEncodeParam();
        videoEncodeParam.setHeight(videoHeight);
        videoEncodeParam.setWidth(videoWidth);
        videoEncodeParam.setFrameRate(videoFrameRate);
        videoEncodeParam.setBitRate(videoBitRate);
        videoEncodeParam.setEncodeType(videoEncodeType);
        videoEncodeParam.setCodecInfo(info);
        mVideoEncoder = new VideoEncoder(videoEncodeParam);
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
    }

    public void setPreviewView(TextureView textureView) {
        this.previewView = textureView;
    }

    public void openCamera() {
        try {
            // Configure and start the camera
            camera = Camera.open(cameraId);
            camera.setErrorCallback((error, camera) -> {
                if (error == Camera.CAMERA_ERROR_SERVER_DIED || error == Camera.CAMERA_ERROR_UNKNOWN) {
                    closeCamera();
                    openCamera();
                }
            });
            if (context != null) {
                camera.setDisplayOrientation(CameraUtils.getDisplayOrientation(context, cameraId));
            }
            if (previewView != null) {
                camera.setPreviewTexture(previewView.getSurfaceTexture());
            }
            Camera.Parameters parameters = getParameters();
            camera.setParameters(parameters);
            camera.setPreviewCallback(this);
            camera.startPreview();
            isRunning = true;
        } catch (RuntimeException | IOException e) {
            e.printStackTrace();
        }
    }

    private Camera.Parameters getParameters() {
        Camera.Parameters parameters = camera.getParameters();
        if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        } else if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }
        parameters.setPreviewSize(videoEncodeParam.getWidth(), videoEncodeParam.getHeight());
        parameters.setPreviewFormat(mVideoEncoder.getFormat());
        parameters.setPreviewFrameRate(videoEncodeParam.getFrameRate());
        return parameters;
    }

//    public void switchCamera() {
//        if (cameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
//            cameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
//        } else {
//            cameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
//        }
//        stopRecording(visitor, videoResType);
//        closeCamera();
//        openCamera(textureView, activity);
//        startRecording(visitor, videoResType);
//    }

    public void closeCamera() {
        try {
            if (camera != null) {
                camera.stopPreview();
                camera.setPreviewCallback(null);
                camera.release();
                camera = null;
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        isRunning = false;
    }


    public void startRecording(int visitor, int channel, int res_type) {
        if (mIsRecording) {
            mVisitorInfo.put(visitor, new Pair<>(channel, res_type));
            return;
        }
        mVisitorInfo.put(visitor, new Pair<>(channel, res_type));
        mVideoEncoder.start();
        mAudioEncoder.setMuted(isMuted);
        mAudioEncoder.start();
        mIsRecording = true;
        Log.d(TAG, "start camera recording");
        startBitRateAdapter(visitor, channel, res_type);
    }

    public void stopRecording(int visitor, int res_type) {
        if (!mIsRecording) {
            return;
        }
        if (mVideoEncoder != null) {
            mVideoEncoder.stop();
            mVideoEncoder = null;
        }
        if (mAudioEncoder != null) {
            mAudioEncoder.stop();
            mAudioEncoder = null;
        }
        mIsRecording = false;
        mVisitorInfo.remove(visitor);
        if (!mVisitorInfo.isEmpty()) return;
        Log.d(TAG, "stop camera recording");
        stopBitRateAdapter();
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

    @Override
    public void onAudioEncoded(byte[] datas, long pts, long seq) {
        if (encodeListener != null) {
            encodeListener.onAudioEncoded(datas, pts, seq);
        }
        if (mIsRecording) {
            for (Map.Entry<Integer, Pair<Integer, Integer>> entry : mVisitorInfo.entrySet()) {
                int visitor = entry.getKey().intValue();
                int channel = entry.getValue().first;
                int res_type = entry.getValue().second;
                int ret = VideoNativeInterface.getInstance().sendAvtAudioData(datas, pts, seq, visitor, channel, res_type);
                if (ret != 0) Log.e(TAG, "sendAudioData to visitor " + visitor + " failed: " + ret);
            }
        }
    }

    private int stat_cnt = 0;

    @Override
    public void onVideoEncoded(byte[] datas, long pts, long seq, boolean isKeyFrame) {
        if (encodeListener != null) {
            encodeListener.onVideoEncoded(datas, pts, seq, isKeyFrame);
        }
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
                }else {
                    Log.e(TAG, "sendVideoData to success");
                }

                if ((stat_cnt++ % 50) == 0) {
                    int buf_size = iv.getSendStreamBuf(visitor, channel, res_type);
                    IvP2pSendInfo ivP2pSendInfo = iv.getSendStreamStatus(visitor, channel, res_type);
                    Log.d(TAG, "visitor " + visitor + " buf size " + buf_size + " link mode " + ivP2pSendInfo.getLinkMode() + "  instNetRate:" + ivP2pSendInfo.getInstNetRate() + "   aveSentRate:" + ivP2pSendInfo.getAveSentRate() + "   sumSentAcked:" + ivP2pSendInfo.getSumSentAcked());
                }
            }
            saveH264(datas);
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (mVideoEncoder == null) return;
        mVideoEncoder.encoderH264(data, cameraId == Camera.CameraInfo.CAMERA_FACING_FRONT);
    }

    private void startBitRateAdapter(int visitor, int channel, int res_type) {
        VideoNativeInterface.getInstance().resetAvg();
        bitRateTimer = new Timer();
        AdapterBitRateTask task = new AdapterBitRateTask();
        task.setVideoEncoder(mVideoEncoder);
        task.setDynamicBitRateType(DynamicBitRateType.INTERNET_SPEED_TYPE);
        task.setInfo(visitor, channel, res_type);
        bitRateTimer.schedule(task, 3000, 1000);
    }

    private void stopBitRateAdapter() {
        if (bitRateTimer != null) {
            bitRateTimer.cancel();
        }
    }

    /**
     * 保存h264数据
     *
     * @param isSaveRecord
     */
    public void isSaveRecord(boolean isSaveRecord) {
        this.isSaveRecord = isSaveRecord;
        recordSpeakH264(isSaveRecord);
    }

    public void recordSpeakH264(boolean isRecord) {
        if (isRecord) {
            if (!TextUtils.isEmpty(speakH264FilePath)) {
                try {
                    File file = UtilsKt.getFile(speakH264FilePath);
                    fos = new FileOutputStream(file);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, speakH264FilePath + "临时缓存文件未找到");
                }
            }
        }
    }

    public void saveH264(byte[] datas) {
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
