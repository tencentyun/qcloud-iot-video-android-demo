package com.tencent.iotvideo.ivdemo;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;


public class LogcatHelper ***REMOVED***
    private static final String TAG = LogcatHelper.class.getName();
    private static LogcatHelper INSTANCE = null;
    private static String PATH_LOGCAT;
    private LogDumper mLogDumper = null;
    private final int mPId;

    public void init(Context context) ***REMOVED***
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) ***REMOVED***// 优先保存到SD卡中
            PATH_LOGCAT = Environment.getExternalStorageDirectory()
                    .getAbsolutePath() + File.separator + "p2p_logs";
      ***REMOVED*** else ***REMOVED***// 如果SD卡不存在，就保存到本应用的目录下
            PATH_LOGCAT = context.getFilesDir().getAbsolutePath() + File.separator + "p2p_logs";
      ***REMOVED***
        File file = new File(PATH_LOGCAT);
        if (!file.exists()) ***REMOVED***
            if (file.mkdirs()) ***REMOVED***
                Log.e(TAG, "创建日志目录成功");
          ***REMOVED*** else ***REMOVED***
                Log.e(TAG, "创建日志目录失败");
          ***REMOVED***
      ***REMOVED***
  ***REMOVED***

    public static LogcatHelper getInstance(Context context) ***REMOVED***
        if (INSTANCE == null) ***REMOVED***
            INSTANCE = new LogcatHelper(context);
      ***REMOVED***
        return INSTANCE;
  ***REMOVED***

    private LogcatHelper(Context context) ***REMOVED***
        init(context);
        mPId = android.os.Process.myPid();
  ***REMOVED***

    public void start() ***REMOVED***
        if (mLogDumper == null) ***REMOVED***
            mLogDumper = new LogDumper(String.valueOf(mPId), PATH_LOGCAT);
      ***REMOVED***
        mLogDumper.start();
  ***REMOVED***

    public void stop() ***REMOVED***
        if (mLogDumper != null) ***REMOVED***
            mLogDumper.stopLogs();
            mLogDumper = null;
      ***REMOVED***
  ***REMOVED***

    private static class LogDumper extends Thread ***REMOVED***

        private Process logcatProc;
        private BufferedReader mReader = null;
        private boolean mRunning = true;
        private final String cmds;
        private final String mPID;
        private FileOutputStream out = null;

        public LogDumper(String pid, String dir) ***REMOVED***
            mPID = pid;
            try ***REMOVED***
                out = new FileOutputStream(new File(dir, "log_" + getFileName() + ".log"),
                        true);
          ***REMOVED*** catch (FileNotFoundException e) ***REMOVED***
                e.printStackTrace();
          ***REMOVED***
            cmds = "logcat | grep \"(" + mPID + ")\"";
      ***REMOVED***

        public void stopLogs() ***REMOVED***
            mRunning = false;
      ***REMOVED***

        @Override
        public void run() ***REMOVED***
            try ***REMOVED***
                logcatProc = Runtime.getRuntime().exec(cmds);
                mReader = new BufferedReader(new InputStreamReader(logcatProc.getInputStream()), 1024);
                String line;
                while (mRunning && (line = mReader.readLine()) != null) ***REMOVED***
                    if (!mRunning) break;
                    if (line.length() == 0) continue;
                    if (out != null && line.contains(mPID)) ***REMOVED***
                        out.write((line + "\n").getBytes());
                  ***REMOVED***
              ***REMOVED***
          ***REMOVED*** catch (IOException e) ***REMOVED***
                e.printStackTrace();
          ***REMOVED*** finally ***REMOVED***
                if (logcatProc != null) ***REMOVED***
                    logcatProc.destroy();
                    logcatProc = null;
              ***REMOVED***
                if (mReader != null) ***REMOVED***
                    try ***REMOVED***
                        mReader.close();
                        mReader = null;
                  ***REMOVED*** catch (IOException e) ***REMOVED***
                        e.printStackTrace();
                  ***REMOVED***
              ***REMOVED***
                if (out != null) ***REMOVED***
                    try ***REMOVED***
                        out.close();
                  ***REMOVED*** catch (IOException e) ***REMOVED***
                        e.printStackTrace();
                  ***REMOVED***
                    out = null;
              ***REMOVED***
          ***REMOVED***
      ***REMOVED***
        private String getFileName() ***REMOVED***
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm");
            return format.format(new Date(System.currentTimeMillis()));
      ***REMOVED***
  ***REMOVED***
}
