package com.tencent.iotvideo.link.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Resources
import android.graphics.Matrix
import android.util.DisplayMetrics
import android.util.Log
import android.util.Range
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import com.tencent.iot.twcall.R
import java.io.File


fun showPopupWindow(anchorView: View, popupViewGroup: View): PopupWindow {
    val popupWindow = PopupWindow(
        popupViewGroup,
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )
    popupWindow.isOutsideTouchable = true
    popupWindow.isFocusable = true

    val displayMetrics: DisplayMetrics = anchorView.context.resources.displayMetrics
    val screenWidth = displayMetrics.widthPixels
    val location = IntArray(2)
    anchorView.getLocationOnScreen(location)
    val xOffset =
        screenWidth - location[0] - anchorView.width - (8 * displayMetrics.density).toInt()

    popupWindow.showAsDropDown(anchorView, xOffset, 0)
    return popupWindow
}

fun TextView.updateOperate(
    isInoperable: Boolean,
    @ColorRes ableTextColorRes: Int = R.color.blue_0052D9,
    @ColorRes enableTextColorRes: Int = R.color.white_0052D9
) {
    if (isInoperable) {
        setTextColor(ContextCompat.getColor(this.context, ableTextColorRes))
        setBackgroundResource(R.drawable.background_blue_cell_btn)
    } else {
        setTextColor(ContextCompat.getColor(this.context, enableTextColorRes))
        setBackgroundResource(R.drawable.background_grey_cell_btn)
    }
}

fun copyTextToClipboard(context: Context, text: String) {
    // 获取剪贴板管理器
    val clipboard: ClipboardManager? = getSystemService(context, ClipboardManager::class.java)
    // 创建一个剪贴板数据对象
    val clip = ClipData.newPlainText("label", text)
    // 将剪贴板数据设置到剪贴板管理器
    clipboard?.setPrimaryClip(clip)
}

fun getFile(path: String): File {
    val file = File(path)
    if (file.exists()) {
        file.delete()
    }
    file.createNewFile()
    return file
}

fun dip2px(context: Context, dpValue: Float): Int {
    val scale = context.resources.displayMetrics.density
    return (dpValue * scale + 0.5f).toInt()
}

fun getScreenWidth(resource: Resources): Int {
    return resource.displayMetrics.widthPixels
}

fun getScreenHeight(resource: Resources): Int {
    return resource.displayMetrics.heightPixels
}

fun adjustAspectRatio(
    videoWidth: Int,
    videoHeight: Int,
    textureView: TextureView,
    screenWidth: Int? = null,
    screenHeight: Int? = null
) {
    val viewLayoutParams = textureView.layoutParams
    val viewWidth = screenWidth ?: viewLayoutParams.width
    val viewHeight = screenHeight ?: viewLayoutParams.height
    Log.d(
        "utils",
        "screenWidth:$screenWidth  screenHeight:$screenHeight   videoWidth:$videoWidth  videoHeight:$videoHeight"
    )
    if (videoWidth == 0 || videoHeight == 0) {
        return
    }
    val viewAspectRatio = viewWidth.toFloat() / viewHeight
    val videoAspectRatio = videoWidth.toFloat() / videoHeight

    var scaleX = 1.0f
    var scaleY = 1.0f

    if (videoAspectRatio > viewAspectRatio) {
        // Video is wider than view, scale by height
        scaleX = videoAspectRatio / viewAspectRatio
    } else {
        // Video is taller than view, scale by width
        scaleY = viewAspectRatio / videoAspectRatio
    }

    val pivotPointX = viewWidth / 2f
    val pivotPointY = viewHeight / 2f

    val matrix = Matrix()
    matrix.setScale(scaleX, scaleY, pivotPointX, pivotPointY)

    textureView.setTransform(matrix)
    textureView.invalidate()
}

fun getBitRateIntervalByPixel(width: Int, height: Int): Array<Double> {
    return arrayOf(width * height * 0.5, width * height * 2.0)
}

val list = listOf(
    10 to 400 * 1000,
    11 to 450 * 1000,
    12 to 490 * 1000,
    13 to 500 * 1000,
    15 to 550 * 1000,
    17 to 610 * 1000,
)

var index = list.size - 1
fun getInfo(isUp: Boolean): Array<Int> {
    if (index < list.size - 1 && index > 0) {
        if (isUp) {
            index++
        } else {
            index--
        }
    }
    return arrayOf(list[index].first, list[index].second)
}