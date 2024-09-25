package com.example.ivdemo

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.tencent.iot.voipdemo.databinding.ActivityVoipLoginBinding
import com.tencent.iotvideo.link.popup.QualitySettingDialog
import com.tencent.iotvideo.link.popup.WxSettingDialog
import com.tencent.iotvideo.link.util.VoipSetting

class VoipLoginActivity : AppCompatActivity() ***REMOVED***

    private val binding by lazy ***REMOVED*** ActivityVoipLoginBinding.inflate(layoutInflater) }
    private val mProductId by lazy ***REMOVED*** intent.getStringExtra("productId") ?: "" }
    private val mDeviceName by lazy ***REMOVED*** intent.getStringExtra("deviceName") ?: "" }
    private val mDeviceKey by lazy ***REMOVED*** intent.getStringExtra("deviceKey") ?: "" }
    private val voipSetting = VoipSetting.getInstance(this)

    override fun onCreate(savedInstanceState: Bundle?) ***REMOVED***
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        with(binding) ***REMOVED***
            // Set button click listeners
            btnLoginVoip.setOnClickListener ***REMOVED***
                if (!checkWxAppInfo()) return@setOnClickListener
                startVoipActivity()
          ***REMOVED***
            btnWxSetting.setOnClickListener ***REMOVED***
                val dialog = WxSettingDialog(this@VoipLoginActivity)
                dialog.show()
                dialog.setOnDismisListener ***REMOVED***
                    txWelcomeSn.text = String.format("Welcome: %s", voipSetting.sn)
              ***REMOVED***
          ***REMOVED***
            btnQualitySetting.setOnClickListener ***REMOVED***
                QualitySettingDialog(this@VoipLoginActivity).show()
          ***REMOVED***
      ***REMOVED***
  ***REMOVED***

    override fun onResume() ***REMOVED***
        super.onResume()
        binding.txWelcomeSn.text = String.format("Welcome: %s", voipSetting.sn)
  ***REMOVED***

    private fun checkWxAppInfo(): Boolean ***REMOVED***
        val modelId = voipSetting.modelId
        val sn = voipSetting.sn
        val snTicket = voipSetting.snTicket
        val appId = voipSetting.appId
        if (modelId.isEmpty() || sn.isEmpty() || snTicket.isEmpty() || appId.isEmpty()) ***REMOVED***
            Toast.makeText(this, "请输入小程序信息！", Toast.LENGTH_LONG).show()
            return false
      ***REMOVED***
        return true
  ***REMOVED***

    private fun startVoipActivity() ***REMOVED***
        val intent = Intent(this, VoipActivity::class.java)
        intent.putExtra("voip_model_id", voipSetting.modelId)
        intent.putExtra("voip_device_id", voipSetting.sn)
        intent.putExtra("voip_wxa_appid", voipSetting.appId)
        intent.putExtra("voip_sn_ticket", voipSetting.snTicket)
        intent.putExtra("productId", mProductId)
        intent.putExtra("deviceName", mDeviceName)
        intent.putExtra("deviceKey", mDeviceKey)
        intent.putExtra("miniprogramVersion", 0)
        startActivity(intent)
  ***REMOVED***
}