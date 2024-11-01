package com.example.ivdemo

import android.content.Intent
import android.os.Bundle
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.tencent.iot.twcall.R
import com.example.ivdemo.popup.QualitySettingDialog
import com.example.ivdemo.popup.WxSettingDialog
import com.tencent.iot.twcall.databinding.ActivityTweCallLoginBinding
import com.tencent.iotvideo.link.util.DeviceSetting
import com.tencent.iotvideo.link.util.updateOperate

class TweCallLoginActivity : AppCompatActivity() {

    private val binding by lazy { ActivityTweCallLoginBinding.inflate(layoutInflater) }
    private val deviceSetting by lazy { DeviceSetting.getInstance() }
    private var miniProgramVersion: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        with(binding) {
            titleLayout.tvTitle.text = getString(R.string.title_twe_call_login)
            titleLayout.ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
            rgSelectVersion.setOnCheckedChangeListener { group, checkedId ->
                val text = group.findViewById<RadioButton>(checkedId).text
                miniProgramVersion = when (text) {
                    getString(R.string.text_develop_version) -> 1
                    getString(R.string.text_experience_version) -> 2
                    else -> 0
                }
            }
            // Set button click listeners
            if (deviceSetting.appId.isEmpty() || deviceSetting.modelId.isEmpty()) {
                btnLoginTweCall.updateOperate(false)
            }
            btnLoginTweCall.setOnClickListener {
                if (!checkWxAppInfo()) return@setOnClickListener
                startTweCallActivity()
            }
            btnWxSetting.setOnClickListener {
                val dialog = WxSettingDialog(this@TweCallLoginActivity)
                dialog.show(supportFragmentManager)
                dialog.setOnDismissListener {
                    val isInoperable =
                        !(deviceSetting.appId.isEmpty() || deviceSetting.modelId.isEmpty())
                    btnLoginTweCall.updateOperate(isInoperable)
                    tvDeviceInfo.text = String.format(
                        getString(R.string.text_device_info),
                        "${deviceSetting.productId}_${deviceSetting.deviceName}"
                    )
                }
            }
            btnQualitySetting.setOnClickListener {
                QualitySettingDialog(this@TweCallLoginActivity).show(supportFragmentManager)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.tvDeviceInfo.text =
            String.format(
                getString(R.string.text_device_info),
                "${deviceSetting.productId}_${deviceSetting.deviceName}"
            )
    }

    private fun checkWxAppInfo(): Boolean {
        val modelId = deviceSetting.modelId
        val appId = deviceSetting.appId
        if (modelId.isEmpty() || appId.isEmpty()) {
            Toast.makeText(this, "请输入小程序信息！", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun startTweCallActivity() {
        val intent = Intent(this, TweCallActivity::class.java)
        intent.putExtra("model_id", deviceSetting.modelId)
        intent.putExtra("device_id", "${deviceSetting.productId}_${deviceSetting.deviceName}")
        intent.putExtra("app_id", deviceSetting.appId)
        intent.putExtra("productId", deviceSetting.productId)
        intent.putExtra("deviceName", deviceSetting.deviceName)
        intent.putExtra("deviceKey", deviceSetting.deviceKey)
        intent.putExtra("miniProgramVersion", miniProgramVersion)
        startActivity(intent)
    }
}