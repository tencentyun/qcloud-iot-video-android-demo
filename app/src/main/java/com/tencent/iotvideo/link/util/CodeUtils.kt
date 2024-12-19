package com.tencent.iotvideo.link.util

object CodeUtils {
    /**
     * 旋转270，要进行宽高对调
     *
     * @param nv21Data
     * @param width
     * @param height
     * @return
     */
    private var nv21Rotated: ByteArray? = null

    fun rotateNV21Data270(nv21Data: ByteArray, width: Int, height: Int): ByteArray {
        val frameSize = width * height
        val bufferSize = frameSize * 3 / 2
        if (nv21Rotated == null) {
            nv21Rotated = ByteArray(bufferSize)
        }
        var i = 0

        // Rotate the Y luma
        for (x in width - 1 downTo 0) {
            var offset = 0
            for (y in 0 until height) {
                nv21Rotated!![i] = nv21Data[offset + x]
                i++
                offset += width
            }
        }

        // Rotate the U and V color components
        i = frameSize
        var x = width - 1
        while (x > 0) {
            var offset = frameSize
            for (y in 0 until height / 2) {
                nv21Rotated!![i] = nv21Data[offset + (x - 1)]
                i++
                nv21Rotated!![i] = nv21Data[offset + x]
                i++
                offset += width
            }
            x = x - 2
        }
        return nv21Rotated!!
    }

    /**
     * 旋转90，要进行宽高对调
     *
     * @param nv21Data
     * @param width
     * @param height
     * @return
     */
    fun rotateNV21Data90(nv21Data: ByteArray, width: Int, height: Int): ByteArray {
        val frameSize = width * height
        val bufferSize = frameSize * 3 / 2
        if (nv21Rotated == null) {
            nv21Rotated = ByteArray(bufferSize)
        }
        // Rotate the Y luma
        var i = 0
        val startPos = (height - 1) * width
        for (x in 0 until width) {
            var offset = startPos
            for (y in height - 1 downTo 0) {
                nv21Rotated!![i] = nv21Data[offset + x]
                i++
                offset -= width
            }
        }

        // Rotate the U and V color components
        i = bufferSize - 1
        var x = width - 1
        while (x > 0) {
            var offset = frameSize
            for (y in 0 until height / 2) {
                nv21Rotated!![i] = nv21Data[offset + x]
                i--
                nv21Rotated!![i] = nv21Data[offset + (x - 1)]
                i--
                offset += width
            }
            x = x - 2
        }
        return nv21Rotated!!
    }

    /**
     * 旋转180
     *
     * @param nv21Data
     * @param width
     * @param height
     * @return
     */
    fun rotateNV21Data180(nv21Data: ByteArray, width: Int, height: Int): ByteArray {
        val frameSize = width * height
        val bufferSize = frameSize * 3 / 2
        if (nv21Rotated == null) {
            nv21Rotated = ByteArray(bufferSize)
        }
        var count = 0

        for (i in frameSize - 1 downTo 0) {
            nv21Rotated!![count] = nv21Data[i]
            count++
        }

        var i = bufferSize - 1
        while (i >= frameSize) {
            nv21Rotated!![count++] = nv21Data[i - 1]
            nv21Rotated!![count++] = nv21Data[i]
            i -= 2
        }
        return nv21Rotated!!
    }

    /**
     * NV21 转换 nv12
     */
    private var nv12: ByteArray? = null
    fun convertNV21ToNV12(nv21: ByteArray, width: Int, height: Int): ByteArray {
        if (nv12 == null) {
            nv12 = ByteArray(width * height * 3 / 2)
        }
        val frameSize = width * height
        System.arraycopy(nv21, 0, nv12, 0, frameSize)
        var i = 0
        while (i < frameSize) {
            nv12!![i] = nv21[i]
            i++
        }
        var j = 0
        while (j < frameSize / 2) {
            nv12!![frameSize + j - 1] = nv21[j + frameSize]
            j += 2
        }
        j = 0
        while (j < frameSize / 2) {
            nv12!![frameSize + j] = nv21[j + frameSize - 1]
            j += 2
        }
        return nv12!!
    }

    /**
     * nv21 转换 yuv420
     */
    private var yuv420: ByteArray? = null
    fun convertNV21ToYUV420(nv21: ByteArray, width: Int, height: Int): ByteArray {
        val frameSize = width * height
        if (yuv420 == null) {
            yuv420 = ByteArray(frameSize * 3 / 2)
        }
        // Copy Y values
        System.arraycopy(nv21, 0, yuv420, 0, frameSize)
        // Copy U and V values
        for (i in 0 until height / 2) {
            for (j in 0 until width / 2) {
                yuv420!![frameSize + i * width / 2 + j] =
                    nv21[frameSize + 2 * (i * width / 2 + j) + 1] // U
                yuv420!![frameSize + frameSize / 4 + i * width / 2 + j] =
                    nv21[frameSize + 2 * (i * width / 2 + j)] // V
            }
        }
        return yuv420!!
    }
}