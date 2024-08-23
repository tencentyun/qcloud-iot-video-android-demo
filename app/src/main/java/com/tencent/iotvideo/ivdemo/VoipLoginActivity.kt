package com.tencent.iotvideo.ivdemo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.tencent.iot.voipdemo.databinding.ActivityVoipLoginBinding
import com.tencent.iotvideo.link.popup.QualitySettingDialog
import com.tencent.iotvideo.link.popup.WxSettingDialog
import com.tencent.iotvideo.link.util.VoipSetting

class VoipLoginActivity : AppCompatActivity() ***REMOVED***

    private val binding by lazy ***REMOVED*** ActivityVoipLoginBinding.inflate(layoutInflater) }

    private val mProductId by lazy ***REMOVED*** intent.getStringExtra("productId") }
    private val mDeviceName by lazy ***REMOVED*** intent.getStringExtra("deviceName") }
    private val mDeviceKey by lazy ***REMOVED*** intent.getStringExtra("deviceKey") }

    override fun onCreate(savedInstanceState: Bundle?) ***REMOVED***
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        with(binding) ***REMOVED***
            // Set button click listeners
            btnLoginVoip.setOnClickListener(View.OnClickListener ***REMOVED***
                if (!checkWxAppInfo()) return@OnClickListener
                startVoipActivity()
          ***REMOVED***)

            btnWxSetting.setOnClickListener ***REMOVED***
                val dialog = WxSettingDialog(this@VoipLoginActivity)
                dialog.show()
                dialog.setOnDismisListener ***REMOVED***
                    txWelcomeSn.text = String.format(
                        "Welcome: %s", VoipSetting.getInstance(this@VoipLoginActivity).sn
                    )
              ***REMOVED***
          ***REMOVED***


            btnQualitySetting.setOnClickListener ***REMOVED***
                val dialog = QualitySettingDialog(this@VoipLoginActivity)
                dialog.show()
//                dialog.setOnDismisListener ***REMOVED***
//                    txWelcomeSn.text = String.format(
//                        "Welcome: %s", VoipSetting.getInstance(this@VoipLoginActivity).sn
//                    );
//              ***REMOVED***
          ***REMOVED***
      ***REMOVED***
  ***REMOVED***

    override fun onResume() ***REMOVED***
        super.onResume()
        binding.txWelcomeSn.text = String.format("Welcome: %s", VoipSetting.getInstance(this).sn)
  ***REMOVED***

    private fun checkWxAppInfo(): Boolean ***REMOVED***
        val modelId = VoipSetting.getInstance(this).modelId
        val sn = VoipSetting.getInstance(this).sn
        val snTicket = VoipSetting.getInstance(this).snTicket
        val appId = VoipSetting.getInstance(this).appId
        if (modelId.isEmpty() || sn.isEmpty() || snTicket.isEmpty() || appId.isEmpty()) ***REMOVED***
            Toast.makeText(this, "请输入小程序信息！", Toast.LENGTH_LONG).show()
            return false
      ***REMOVED***
        return true
  ***REMOVED***

    private fun startVoipActivity() ***REMOVED***
        val intent = Intent(this, VoipActivity::class.java)
        intent.putExtra("voip_model_id", VoipSetting.getInstance(this).modelId)
        intent.putExtra("voip_device_id", VoipSetting.getInstance(this).sn)
        intent.putExtra("voip_wxa_appid", VoipSetting.getInstance(this).appId)
        intent.putExtra("voip_sn_ticket", VoipSetting.getInstance(this).snTicket)
        intent.putExtra("productId", mProductId)
        intent.putExtra("deviceName", mDeviceName)
        intent.putExtra("deviceKey", mDeviceKey)
        intent.putExtra("miniprogramVersion", 0)
        startActivity(intent)
  ***REMOVED***
}