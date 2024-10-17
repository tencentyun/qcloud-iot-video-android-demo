package com.example.ivdemo

import android.content.Intent
import android.os.Bundle
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.tencent.iot.twcall.R
import com.tencent.iot.twcall.databinding.ActivityVoipLoginBinding
import com.example.ivdemo.popup.QualitySettingDialog
import com.example.ivdemo.popup.WxSettingDialog
import com.tencent.iotvideo.link.util.VoipSetting

class VoipLoginActivity : AppCompatActivity() {

    private val binding by lazy { ActivityVoipLoginBinding.inflate(layoutInflater) }
    private val mProductId by lazy { intent.getStringExtra("productId") ?: "" }
    private val mDeviceName by lazy { intent.getStringExtra("deviceName") ?: "" }
    private val mDeviceKey by lazy { intent.getStringExtra("deviceKey") ?: "" }
    private val voipSetting by lazy { VoipSetting.getInstance(this) }
    private var miniProgramVersion: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        with(binding) {
            titleLayout.tvTitle.text = getString(R.string.title_voip_login)
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
            btnLoginVoip.setOnClickListener {
                if (!checkWxAppInfo()) return@setOnClickListener
                startVoipActivity()
            }
            btnWxSetting.setOnClickListener {
                val dialog = WxSettingDialog(this@VoipLoginActivity)
                dialog.show(supportFragmentManager)
                dialog.setOnDismissListener {
                    txWelcomeSn.text = String.format("Welcome: %s", voipSetting.sn)
                }
            }
            btnQualitySetting.setOnClickListener {
                QualitySettingDialog(this@VoipLoginActivity).show(supportFragmentManager)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.txWelcomeSn.text = String.format("Welcome: %s", voipSetting.sn)
    }

    private fun checkWxAppInfo(): Boolean {
        val modelId = voipSetting.modelId
        val sn = voipSetting.sn
        val snTicket = voipSetting.snTicket
        val appId = voipSetting.appId
        if (modelId.isEmpty() || sn.isEmpty() || snTicket.isEmpty() || appId.isEmpty()) {
            Toast.makeText(this, "请输入小程序信息！", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun startVoipActivity() {
        val intent = Intent(this, VoipActivity::class.java)
        intent.putExtra("voip_model_id", voipSetting.modelId)
        intent.putExtra("voip_device_id", voipSetting.sn)
        intent.putExtra("voip_wxa_appid", voipSetting.appId)
        intent.putExtra("voip_sn_ticket", voipSetting.snTicket)
        intent.putExtra("productId", mProductId)
        intent.putExtra("deviceName", mDeviceName)
        intent.putExtra("deviceKey", mDeviceKey)
        intent.putExtra("miniProgramVersion", miniProgramVersion)
        startActivity(intent)
    }
}