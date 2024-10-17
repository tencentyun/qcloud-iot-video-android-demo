package com.example.ivdemo.popup

import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import com.tencent.iot.twcall.databinding.PopupWxSettingLayoutBinding
import com.tencent.iotvideo.link.util.VoipSetting

class WxSettingDialog(context: Context) :
    IosCenterStyleDialog<PopupWxSettingLayoutBinding>(context) {
    private val voipSetting by lazy { VoipSetting.getInstance(context) }
    override fun getViewBinding(): PopupWxSettingLayoutBinding =
        PopupWxSettingLayoutBinding.inflate(layoutInflater)

    public override fun initView() {
        with(binding) {
            etVoipModelId.setText(voipSetting.modelId)
            etVoipSn.setText(voipSetting.sn)
            etVoipSnTicket.setText(voipSetting.snTicket)
            etVoipWxAppId.setText(voipSetting.appId)
            btnConfirm.setOnClickListener(View.OnClickListener {
                if (!checkWxAppInfo()) return@OnClickListener
                saveWxAppInfo()
                dismiss()
                onDismiss?.invoke()
            })
        }
    }

    private fun checkWxAppInfo(): Boolean {
        val modelId = binding.etVoipModelId.text.toString()
        val sn = binding.etVoipSn.text.toString()
        val snTicket = binding.etVoipSnTicket.text.toString()
        val appId = binding.etVoipWxAppId.text.toString()
        if (modelId.isEmpty() || sn.isEmpty() || snTicket.isEmpty() || appId.isEmpty()) {
            Toast.makeText(context, "请输入小程序信息！", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun saveWxAppInfo() {
        voipSetting.setModelId(binding.etVoipModelId.text.toString())
        voipSetting.setSn(binding.etVoipSn.text.toString())
        voipSetting.setSnTicket(binding.etVoipSnTicket.text.toString())
        voipSetting.setAppId(binding.etVoipWxAppId.text.toString())
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
