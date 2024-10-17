package com.example.ivdemo.popup

import android.content.Context
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import com.tencent.iot.twcall.databinding.PopupDeviceSettingLayoutBinding
import com.tencent.iotvideo.link.util.VoipSetting

class DeviceSettingDialog(private val context: Context) :
    IosCenterStyleDialog<PopupDeviceSettingLayoutBinding>(context) {
    private val voipSetting by lazy { VoipSetting.getInstance(context) }
    override fun getViewBinding(): PopupDeviceSettingLayoutBinding =
        PopupDeviceSettingLayoutBinding.inflate(layoutInflater)

    public override fun initView() {
        with(binding) {
            etLoginProductId.setText(voipSetting.productId)
            etLoginDeviceName.setText(voipSetting.deviceName)
            etLoginDeviceKey.setText(voipSetting.deviceKey)

            btnConfirm.setOnClickListener(View.OnClickListener {
                if (!checkDeviceInfo()) return@OnClickListener
                saveDeviceInfo()
                dismiss()
            })

            etJsonCopy.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    charSequence: CharSequence, i: Int, i1: Int, i2: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence, i: Int, i1: Int, i2: Int) {
                    val inputText = s.toString()
                    if (!TextUtils.isEmpty(inputText)) {
                        if (VoipSetting.isJSONString(inputText)) {
                            voipSetting.saveData(inputText)
                            voipSetting.loadValueToMemory()
                            etLoginProductId.setText(voipSetting.productId)
                            etLoginDeviceName.setText(voipSetting.deviceName)
                            etLoginDeviceKey.setText(voipSetting.deviceKey)
                        } else {
                            Toast.makeText(context, "输入的json非法！", Toast.LENGTH_LONG).show()
                        }
                    }
                }

                override fun afterTextChanged(editable: Editable) {
                }
            })
        }
    }

    private fun checkDeviceInfo(): Boolean {
        val productId = binding.etLoginProductId.text.toString()
        val deviceName = binding.etLoginDeviceName.text.toString()
        val deviceKey = binding.etLoginDeviceKey.text.toString()
        if (productId.isEmpty() || deviceName.isEmpty() || deviceKey.isEmpty()) {
            Toast.makeText(context, "请输入设备信息！", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun saveDeviceInfo() {
        voipSetting.setProductId(binding.etLoginProductId.text.toString())
        voipSetting.setDeviceName(binding.etLoginDeviceName.text.toString())
        voipSetting.setDeviceKey(binding.etLoginDeviceKey.text.toString())
    }

    fun show(manager: FragmentManager) {
        super.show(manager, "DeviceSettingDialog")
    }
}
