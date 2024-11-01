package com.tencent.iotvideo.link.util

import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.tencent.iot.twcall.R


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