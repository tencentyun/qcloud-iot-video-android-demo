package com.example.ivdemo;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;


public class LogcatHelper {
    private static final String TAG = LogcatHelper.class.getName();
    private static LogcatHelper INSTANCE = null;
    private static String PATH_LOGCAT;
    private LogDumper mLogDumper = null;
    private final int mPId;

    public void init(Context context) {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {// 优先保存到SD卡中
            PATH_LOGCAT = Environment.getExternalStorageDirectory()
                    .getAbsolutePath() + File.separator + "p2p_logs";
        } else {// 如果SD卡不存在，就保存到本应用的目录下
            PATH_LOGCAT = context.getFilesDir().getAbsolutePath() + File.separator + "p2p_logs";
        }
        File file = new File(PATH_LOGCAT);
        if (!file.exists()) {
            if (file.mkdirs()) {
                Log.e(TAG, "创建日志目录成功");
            } else {
                Log.e(TAG, "创建日志目录失败");
            }
        }
    }

    public static LogcatHelper getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new LogcatHelper(context);
        }
        return INSTANCE;
    }

    private LogcatHelper(Context context) {
        init(context);
        mPId = android.os.Process.myPid();
    }

    public void start() {
        if (mLogDumper == null) {
            mLogDumper = new LogDumper(String.valueOf(mPId), PATH_LOGCAT);
        }
        mLogDumper.start();
    }

    public void stop() {
        if (mLogDumper != null) {
            mLogDumper.stopLogs();
            mLogDumper = null;
        }
    }

    public String readLogFileContent() {
        StringBuilder content = new StringBuilder();
        BufferedReader reader = null;

        try {
            if (mLogDumper.logFilePath.isEmpty()) {
                reader = new BufferedReader(new FileReader(getLatestFile(PATH_LOGCAT)));
            } else {
                reader = new BufferedReader(new FileReader(mLogDumper.getLogFilePath()));
            }
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
                content.append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return content.toString();
    }

    private File getLatestFile(String directoryPath) {
        File directory = new File(directoryPath);
        File[] files = directory.listFiles();

        if (files != null && files.length > 0) {
            Arrays.sort(files, (file1, file2) -> Long.compare(file2.lastModified(), file1.lastModified()));
            return files[0];
        }
        return null;
    }

    private static class LogDumper extends Thread {

        private Process logcatProc;
        private BufferedReader mReader = null;
        private boolean mRunning = true;
        private final String cmds;
        private final String mPID;
        private FileOutputStream out = null;

        private String logFilePath = null;

        public LogDumper(String pid, String dir) {
            mPID = pid;
            try {
                File logFile = new File(dir, "log_" + getFileName() + ".log");
                logFilePath = logFile.getPath();
                out = new FileOutputStream(logFile, true);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            cmds = "logcat | grep \"(" + mPID + ")\"";
        }

        public void stopLogs() {
            mRunning = false;
        }

        @Override
        public void run() {
            try {
                logcatProc = Runtime.getRuntime().exec(cmds);
                mReader = new BufferedReader(new InputStreamReader(logcatProc.getInputStream()), 1024);
                String line;
                while (mRunning && (line = mReader.readLine()) != null) {
                    if (!mRunning) break;
                    if (line.length() == 0) continue;
                    if (out != null && line.contains(mPID)) {
                        out.write((line + "\n").getBytes());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (logcatProc != null) {
                    logcatProc.destroy();
                    logcatProc = null;
                }
                if (mReader != null) {
                    try {
                        mReader.close();
                        mReader = null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    out = null;
                }
            }
        }

        private String getFileName() {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm");
            return format.format(new Date(System.currentTimeMillis()));
        }

        public String getLogFilePath() {
            return logFilePath;
        }
    }
}
