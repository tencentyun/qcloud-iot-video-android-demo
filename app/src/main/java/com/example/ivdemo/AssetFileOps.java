package com.example.ivdemo;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class AssetFileOps {
    private static final String TAG = "AssetFileOps";
    public void copyFilesFromAssets2(Context context, String assetsPath, String savePath){
        try {
            String fileNames[] = context.getAssets().list(assetsPath);// 获取assets目录下的所有文件及目录名
            if (fileNames.length > 0) {// 如果是目录
                File file = new File(savePath);
                file.mkdirs();// 如果文件夹不存在，则递归
                for (String fileName : fileNames) {
                    copyFilesFromAssets2(context, assetsPath + "/" + fileName,
                            savePath + "/" + fileName);
                }
            } else {// 如果是文件
                InputStream is = context.getAssets().open(assetsPath);
                FileOutputStream fos = new FileOutputStream(new File(savePath));
                byte[] buffer = new byte[1024];
                int byteCount = 0;
                while ((byteCount = is.read(buffer)) != -1) {// 循环从输入流读取
                    // buffer字节
                    fos.write(buffer, 0, byteCount);// 将读取的输入流写入到输出流
                }
                fos.flush();// 刷新缓冲区
                is.close();
                fos.close();
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void copyDirFromAssets(Context context, String folder){
        String filesDir = context.getFilesDir().getPath();
        filesDir = filesDir + "/" + folder;
        copyFilesFromAssets2(context,folder,filesDir);
    }

    public void copyFileFromAssets(Context context, String fileName, String absFileName) {
        Log.d(TAG, "Create file " + absFileName);

        File f = new File(absFileName);
        if (f.exists()) {
            Log.d(TAG, "file " + absFileName + " exist");
            return;
        }

        try {
            InputStream is = context.getAssets().open(fileName);
            byte[] bytes = new byte[4096];
            int bt = 0;
            FileOutputStream fos = new FileOutputStream(f);
            while ((bt = is.read(bytes)) != -1) {
                fos.write(bytes, 0, bt);
            }
            fos.flush();
            is.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
