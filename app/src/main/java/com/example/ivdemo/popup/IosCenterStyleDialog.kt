package com.example.ivdemo.popup

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.view.animation.TranslateAnimation
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.viewbinding.ViewBinding
import com.tencent.iot.twcall.R

abstract class IosCenterStyleDialog<VB : ViewBinding>(
    private var context: Context,
    private var showAnimation: Boolean = true
) : DialogFragment() {

    protected lateinit var binding: VB

    protected abstract fun getViewBinding(): VB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullScreenDialogStyle);
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = getViewBinding()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //设置view 弹出的平移动画，从底部-100% 平移到自身位置
        if (showAnimation) {
            val animation = TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF,
                0f, Animation.RELATIVE_TO_SELF, 1f, Animation.RELATIVE_TO_SELF, 0f
            )
            animation.interpolator = DecelerateInterpolator()
            animation.duration = 350
            animation.startOffset = 150
            binding.root.setAnimation(animation) //设置动画
        }
        initView()
    }

    protected abstract fun initView()

    override fun show(manager: FragmentManager, tag: String?) {
        if ((context as Activity).isFinishing || (context as Activity).isDestroyed) return
        if (dialog?.isShowing == true) return  // 已经处于显示状态，不再显示
        super.show(manager, tag)
        // 设置dialog的宽高是全屏，注意：一定要放在show的后面，否则不是全屏显示
        val params = dialog?.window?.attributes
        params?.width = ViewGroup.LayoutParams.MATCH_PARENT
        params?.height = ViewGroup.LayoutParams.MATCH_PARENT
        params?.gravity = Gravity.CENTER
        dialog?.window?.attributes = params
        dialog?.window?.setContentView(binding.root)
    }
}
