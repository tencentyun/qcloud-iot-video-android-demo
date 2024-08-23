package com.tencent.iotvideo.link.util.audio;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class G711Code ***REMOVED***
    private final static int SIGN_BIT = 0x80;
    private final static int QUANT_MASK = 0xf;
    private final static int SEG_SHIFT = 4;
    private final static int SEG_MASK = 0x70;
    private final static int BIAS = 0x84;
    private final static int CLIP = 8159;

    static short[] seg_end = ***REMOVED***0xFF, 0x1FF, 0x3FF, 0x7FF, 0xFFF, 0x1FFF, 0x3FFF, 0x7FFF};
    static short[] seg_uend = ***REMOVED***0x3F, 0x7F, 0xFF, 0x1FF, 0x3FF, 0x7FF, 0xFFF, 0x1FFF};

    static short search(short val, short[] table, short size) ***REMOVED***
        for (short i = 0; i < size; i++) ***REMOVED***
            if (val <= table[i]) ***REMOVED***
                return i;
          ***REMOVED***
      ***REMOVED***
        return size;
  ***REMOVED***

    /**
     * pcm 转 a-law
     */
    static byte linear2alaw(short pcm_val) ***REMOVED***
        short mask;
        short seg;
        char aval;
        if (pcm_val >= 0) ***REMOVED***
            mask = 0xD5;
      ***REMOVED*** else ***REMOVED***
            mask = 0x55;
            pcm_val = (short) (-pcm_val - 1);
            if (pcm_val < 0) ***REMOVED***
                pcm_val = 32767;
          ***REMOVED***
      ***REMOVED***
        seg = search(pcm_val, seg_end, (short) 8); // Convert the scaled magnitude to segment number.
        // Combine the sign, segment, and quantization bits.
        if (seg >= 8)  // out of range, return maximum value.
            return (byte) (0x7F ^ mask);
        else ***REMOVED***
            aval = (char) (seg << SEG_SHIFT);
            if (seg < 2)
                aval |= (pcm_val >> 4) & QUANT_MASK;
            else
                aval |= (pcm_val >> (seg + 3)) & QUANT_MASK;
            return (byte) (aval ^ mask);
      ***REMOVED***
  ***REMOVED***

    /**
     * pcm 转 u-law
     */
    static byte linear2ulaw(short pcm_val) ***REMOVED***
        short mask;
        short seg;
        char uval;
        if (pcm_val >= 0) ***REMOVED***
            mask = 0xFF;
      ***REMOVED*** else ***REMOVED***
            mask = 0x7F;
            pcm_val = (short) (-pcm_val);
            if (pcm_val < 0) ***REMOVED***
                pcm_val = 32767;
          ***REMOVED***
      ***REMOVED***
        if ( pcm_val > CLIP ) pcm_val = (short)CLIP; // clip the magnitude 削波
        pcm_val += (BIAS >> 2);
        seg = search(pcm_val, seg_uend, (short) 8); // Convert the scaled magnitude to segment number.
        // Combine the sign, segment, and quantization bits.
        if (seg >= 8)  // out of range, return maximum value.
            return (byte) (0x7F ^ mask);
        else ***REMOVED***
            uval = (char) ((seg << 4) | ((pcm_val >> (seg + 1)) & 0xF));
            return (byte) (uval ^ mask);
      ***REMOVED***
  ***REMOVED***

    /**
     * a-law 转 pcm
     */
    static short alaw2linear(byte a_val) ***REMOVED***
        short t;
        short seg;
        a_val ^= 0x55;

        t = (short) ((a_val & QUANT_MASK) << 4);
        seg = (short) ((a_val & SEG_MASK) >> SEG_SHIFT);
        switch (seg) ***REMOVED***
            case 0:
                t += 8;
                break;
            case 1:
                t += 0x108;
                break;
            default:
                t += 0x108;
                t <<= seg - 1;
      ***REMOVED***
        return (a_val & SIGN_BIT) != 0 ? t : (short) -t;
  ***REMOVED***

    /**
     * u-law 转 pcm
     */
    static short ulaw2linear(byte u_val) ***REMOVED***
        short t;
        u_val = (byte) (~u_val); // Complement to obtain normal u-law value.
        /*
         * Extract and bias the quantization bits. Then
         * shift up by the segment number and subtract out the bias.
         */
        t = (short) (((short) ((u_val & QUANT_MASK) << 3)) + BIAS);
        t <<= (u_val & SEG_MASK) >> SEG_SHIFT;
        return ((u_val & SIGN_BIT) != 0 ? (short) (BIAS - t) : (short) (t - BIAS));
  ***REMOVED***

    /**
     * pcm 转 G711 a率
     */
    public static byte[] encodeToALaw(byte[] pcm) ***REMOVED***
        short sample = 0;
        int j = 0;
        byte[] g711 = new byte[pcm.length / 2];
        for (int i = 0; i < pcm.length / 2; i++) ***REMOVED***
            sample = (short) ((pcm[j++] & 0xff) | (pcm[j++]) << 8) ;
            g711[i] = linear2alaw(sample);
      ***REMOVED***
        return g711;
  ***REMOVED***

    /**
     * pcm 转 G711 u率
     */
    public static byte[] encodeToULaw(byte[] pcm) ***REMOVED***
        short sample = 0;
        int j = 0;
        byte[] g711 = new byte[pcm.length / 2];
        for (int i = 0; i < pcm.length / 2; i++) ***REMOVED***
            sample = (short) ((pcm[j++] & 0xff) | (pcm[j++]) << 8) ;
            g711[i] = linear2ulaw(sample);
      ***REMOVED***
        return g711;
  ***REMOVED***

    /**
     * G.711 a-law 转 PCM
     */
    public static short[] decodeFromAlaw(byte[] code) ***REMOVED***
        short[] raw = new short[code.length];
        for (int i = 0; i < code.length; i++) ***REMOVED***
            raw[i] = alaw2linear(code[i]);
      ***REMOVED***
        return raw;
  ***REMOVED***

    /**
     * G.711 u-law 转 PCM
     */
    public static short[] decodeFromUlaw(byte[] code) ***REMOVED***
        short[] raw = new short[code.length];
        for (int i = 0; i < code.length; i++) ***REMOVED***
            raw[i] = ulaw2linear(code[i]);
      ***REMOVED***
        return raw;
  ***REMOVED***

    /**
     * short[] to byte[]
     */
    public static byte[] shortToBytes(short[] shorts) ***REMOVED***
        if(shorts==null)***REMOVED***
            return null;
      ***REMOVED***
        byte[] bytes = new byte[shorts.length * 2];
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(shorts);

        return bytes;
  ***REMOVED***

    /**
     * byte[] to short[]
     */
    public short[] bytesToShort(byte[] bytes) ***REMOVED***
        if(bytes==null)***REMOVED***
            return null;
      ***REMOVED***
        short[] shorts = new short[bytes.length/2];
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
        return shorts;
  ***REMOVED***
}
