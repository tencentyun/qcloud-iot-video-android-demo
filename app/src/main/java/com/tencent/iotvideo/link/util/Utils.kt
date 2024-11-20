package com.tencent.iotvideo.link.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.DisplayMetrics
import android.util.Log
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

fun adjustAspectRatio(textureView: TextureView, videoWidth: Int, videoHeight: Int) {


    val viewLayoutParams = textureView.layoutParams
    val screenWidth = viewLayoutParams.width
    val screenHeight = viewLayoutParams.height
    Log.d(
        "utils",
        "screenWidth:$screenWidth  screenHeight:$screenHeight   videoWidth:$videoWidth  videoHeight:$videoHeight"
    )
    if (videoWidth == 0 || videoHeight == 0) {
        return
    }
    val screenAspectRatio = screenWidth.toFloat() / screenHeight
    val videoAspectRatio = videoWidth.toFloat() / videoHeight

    val newWidth: Int
    val newHeight: Int
    if (videoAspectRatio > screenAspectRatio) {
        newWidth = screenWidth
        newHeight = (screenWidth / videoAspectRatio).toInt()
    } else {
        newHeight = screenHeight
        newWidth = (screenHeight * videoAspectRatio).toInt()
    }

    viewLayoutParams.width = newWidth
    viewLayoutParams.height = newHeight
    Log.d(
        "utils",
        "end newWidth:$newWidth  newHeight:$newHeight"
    )
    textureView.layoutParams = viewLayoutParams
}