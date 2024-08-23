package com.tencent.iotvideo.link.util.audio;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class FLVPacker ***REMOVED***

    private long pts = 0;
    private volatile boolean isHead = false;

    public synchronized byte[] getFLV(byte[] data) ***REMOVED***
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try ***REMOVED***
            if (!isHead) ***REMOVED***
                baos.write(flvHeader());
          ***REMOVED***
            baos.write(aacToFlv(data));
            baos.flush();
            baos.close();
      ***REMOVED*** catch (IOException e) ***REMOVED***
            e.printStackTrace();
      ***REMOVED***
        return baos.toByteArray();
  ***REMOVED***

    public synchronized byte[] aacToFlv(byte[] date) ***REMOVED***
        byte[] data = Arrays.copyOfRange(date, 7, date.length);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try ***REMOVED***
            baos.write(0x08);
            // 长度
            baos.write(integerTo3Bytes(data.length + 2));
            if (pts == 0) ***REMOVED***
                // 时间戳
                baos.write(0x00);
                baos.write(0x00);
                baos.write(0x00);
                baos.write(0x00);
                pts = System.currentTimeMillis();
          ***REMOVED*** else ***REMOVED***
                byte[] b = integerTo4Bytes((int) (System.currentTimeMillis() - pts));
                baos.write(b[1]);
                baos.write(b[2]);
                baos.write(b[3]);
                baos.write(b[0]);
          ***REMOVED***
            // StreamID
            baos.write(0x00);
            baos.write(0x00);
            baos.write(0x00);
            baos.write(0xAF);
            if (data.length < 10) ***REMOVED***
                baos.write(0x00);
          ***REMOVED*** else ***REMOVED***
                baos.write(0x01);
          ***REMOVED***
            baos.write(data);

            int len = data.length + 13;
            byte[] bbDS = integerTo4Bytes(len);
            baos.write(bbDS[0]);
            baos.write(bbDS[1]);
            baos.write(bbDS[2]);
            baos.write(bbDS[3]);
            baos.flush();
            baos.close();
      ***REMOVED*** catch (IOException e) ***REMOVED***
            e.printStackTrace();
      ***REMOVED***

        return baos.toByteArray();
  ***REMOVED***

    private byte[] flvHeader() ***REMOVED***
        isHead = true;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try ***REMOVED***
            baos.write(0x46);
            baos.write(0x4C);
            baos.write(0x56);
            baos.write(0x01);
            baos.write(0x04); // 用来控制是否含有音频 音视频都有为0x05 纯视频是0x01 纯音频为 0x04

            baos.write(0x00);
            baos.write(0x00);
            baos.write(0x00);
            baos.write(0x09);

            baos.write(0x00);
            baos.write(0x00);
            baos.write(0x00);
            baos.write(0x00);
            baos.flush();
            baos.close();
      ***REMOVED*** catch (IOException e) ***REMOVED***
            e.printStackTrace();
      ***REMOVED***
        return baos.toByteArray();
  ***REMOVED***

    public static byte[] integerTo3Bytes(int value) ***REMOVED***
        byte[] result = new byte[3];
        result[0] = (byte) ((value >>> 16) & 0xFF);
        result[1] = (byte) ((value >>> 8) & 0xFF);
        result[2] = (byte) (value & 0xFF);
        return result;
  ***REMOVED***

    public static byte[] integerTo4Bytes(int value) ***REMOVED***
        byte[] result = new byte[4];
        result[0] = (byte) ((value >>> 24) & 0xFF);
        result[1] = (byte) ((value >>> 16) & 0xFF);
        result[2] = (byte) ((value >>> 8) & 0xFF);
        result[3] = (byte) (value & 0xFF);
        return result;
  ***REMOVED***

}

