package com.example.ivdemo.popup

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import com.tencent.iot.twcall.databinding.PopupDeviceSettingLayoutBinding
import com.tencent.iotvideo.link.util.DeviceSetting
import com.tencent.iotvideo.link.util.updateOperate

class DeviceSettingDialog(private val context: Context) :
    IosCenterStyleDialog<PopupDeviceSettingLayoutBinding>(context) {

    private val deviceSetting by lazy { DeviceSetting.getInstance() }
    private var ipcType: Int = 2
    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(
            s: CharSequence?,
            start: Int,
            count: Int,
            after: Int
        ) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            val isOperate = checkDeviceInfo()
            binding.btnConfirm.updateOperate(isOperate)
        }

        override fun afterTextChanged(s: Editable?) {
        }

    }

    override fun getViewBinding(): PopupDeviceSettingLayoutBinding =
        PopupDeviceSettingLayoutBinding.inflate(layoutInflater)

    public override fun initView() {
        with(binding) {
            etLoginProductId.setText(deviceSetting.productId)
            etLoginDeviceName.setText(deviceSetting.deviceName)
            etLoginDeviceKey.setText(deviceSetting.deviceKey)
            val isOperate =
                !(deviceSetting.productId.isEmpty() || deviceSetting.deviceName.isEmpty() || deviceSetting.deviceKey.isEmpty())
            binding.btnConfirm.updateOperate(isOperate)

            etLoginProductId.addTextChangedListener(textWatcher)
            etLoginDeviceName.addTextChangedListener(textWatcher)
            etLoginDeviceKey.addTextChangedListener(textWatcher)
            ipcType = deviceSetting.ipcType
            rbOneWay.isChecked = ipcType == 1
            rbTwoWay.isChecked = ipcType == 2
            rgSelectWay.setOnCheckedChangeListener { group, checkedId ->
                ipcType = group.findViewById<RadioButton>(checkedId).tag.toString().toInt()
            }

            btnConfirm.setOnClickListener(View.OnClickListener {
                if (!checkDeviceInfo()) {
                    Toast.makeText(context.applicationContext, "请输入设备信息！", Toast.LENGTH_SHORT).show()
                    return@OnClickListener
                }
                saveDeviceInfo()
                dismiss()
                onDismiss?.invoke()
            })
        }
    }

    private fun checkDeviceInfo(): Boolean {
        val productId = binding.etLoginProductId.text.toString()
        val deviceName = binding.etLoginDeviceName.text.toString()
        val deviceKey = binding.etLoginDeviceKey.text.toString()
        return !(productId.isEmpty() || deviceName.isEmpty() || deviceKey.isEmpty())
    }

    private fun saveDeviceInfo() {
        with(binding) {
            deviceSetting.ipcType = ipcType
            deviceSetting.productId = etLoginProductId.text.toString()
            deviceSetting.deviceName = etLoginDeviceName.text.toString()
            deviceSetting.deviceKey = etLoginDeviceKey.text.toString()
        }
    }

    fun show(manager: FragmentManager) {
        super.show(manager, "DeviceSettingDialog")
    }

    private var onDismiss: (() -> Unit)? = null
    fun setDismissListener(onDismiss: () -> Unit) {
        this.onDismiss = onDismiss
    }
}
