package com.example.ivdemo.popup

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import com.tencent.iot.twcall.databinding.PopupWxSettingLayoutBinding
import com.tencent.iotvideo.link.util.DeviceSetting
import com.tencent.iotvideo.link.util.updateOperate

class WxSettingDialog(context: Context) :
    IosCenterStyleDialog<PopupWxSettingLayoutBinding>(context) {
    private val deviceSetting by lazy { DeviceSetting.getInstance() }
    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(
            s: CharSequence?,
            start: Int,
            count: Int,
            after: Int
        ) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            val isOperate = checkWxAppInfo()
            binding.btnConfirm.updateOperate(isOperate)
        }

        override fun afterTextChanged(s: Editable?) {
        }

    }

    override fun getViewBinding(): PopupWxSettingLayoutBinding =
        PopupWxSettingLayoutBinding.inflate(layoutInflater)

    public override fun initView() {
        with(binding) {
            if (deviceSetting.modelId.isEmpty() || deviceSetting.appId.isEmpty()) {
                btnConfirm.updateOperate(false)
            }
            etTweCallModelId.addTextChangedListener(textWatcher)
            etTweCallModelId.setText(deviceSetting.modelId)
            etTweCallWxAppId.addTextChangedListener(textWatcher)
            etTweCallWxAppId.setText(deviceSetting.appId)
            btnConfirm.setOnClickListener(View.OnClickListener {
                if (!checkWxAppInfo()) {
                    Toast.makeText(context?.applicationContext, "请输入小程序信息！", Toast.LENGTH_SHORT).show()
                    return@OnClickListener
                }
                saveWxAppInfo()
                dismiss()
                onDismiss?.invoke()
            })
        }
    }

    private fun checkWxAppInfo(): Boolean {
        val modelId = binding.etTweCallModelId.text.toString()
        val appId = binding.etTweCallWxAppId.text.toString()
        return !(modelId.isEmpty() || appId.isEmpty())
    }

    private fun saveWxAppInfo() {
        deviceSetting.modelId = binding.etTweCallModelId.text.toString()
        deviceSetting.appId = binding.etTweCallWxAppId.text.toString()
    }

    fun show(manager: FragmentManager) {
        super.show(manager, "WxSettingDialog")
    }

    @Volatile
    private var onDismiss: (() -> Unit)? = null

    fun setOnDismissListener(onDismiss: () -> Unit) {
        this.onDismiss = onDismiss
    }
}
