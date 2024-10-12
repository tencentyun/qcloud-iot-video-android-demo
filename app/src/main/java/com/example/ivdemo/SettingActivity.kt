package com.example.ivdemo

import android.hardware.Camera
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.tencent.iot.twcall.R
import com.tencent.iot.twcall.databinding.ActivityQualitySettingBinding
import com.tencent.iot.twcall.databinding.ActivityWxSettingBinding
import com.tencent.iotvideo.link.consts.CameraConstants
import com.tencent.iotvideo.link.entity.ResolutionEntity
import com.tencent.iotvideo.link.util.QualitySetting
import com.tencent.iotvideo.link.util.VoipSetting

class SettingActivity : AppCompatActivity() {
    private val TAG: String = SettingActivity::class.java.simpleName

    private val wxSettingLayoutBinding by lazy { ActivityWxSettingBinding.inflate(layoutInflater) }
    private val qualitySettingBinding by lazy { ActivityQualitySettingBinding.inflate(layoutInflater) }

    private val localResolutionArray = ArrayList<ResolutionEntity>()

    private var selectedLocalResolution = 0

    private var selectedFrameRate = 15

    private var minBitRateSbValue = 0
    private var selectedBitRate = 0

    private val wxResolutionArray =
        arrayOf("可变自适应", "240x320", "320x240", "480x352", "480x640")

    private var selectedWxResolution = 0

    private var selectedWxCameraSetting = true

    private var needShowDefaultValue = true

    private val voipSetting by lazy { VoipSetting.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val type = intent.getStringExtra("type")
        when (type) {
            "wx_setting" -> initWxSettingView()

            "quality_setting" -> {
                getSupportedPreviewSizes()
                initQualitySettingView()
            }
        }
    }

    private fun initWxSettingView() {
        with(wxSettingLayoutBinding) {
            setContentView(root)
            titleLayout.tvTitle.text = getString(R.string.setting)
            titleLayout.ivBack.setOnClickListener {
                onBackPressed()
            }
            etVoipModelId.setText(voipSetting.modelId)
            etVoipSn.setText(voipSetting.sn)
            etVoipSnTicket.setText(voipSetting.snTicket)
            etVoipWxAppId.setText(voipSetting.appId)
            btnConfirm.setOnClickListener(View.OnClickListener {
                saveWxAppInfo()
                if (!checkWxAppInfo()) return@OnClickListener
                Toast.makeText(this@SettingActivity, "保存成功！", Toast.LENGTH_LONG).show()
                it.postDelayed({
                    onBackPressed()
                }, 500)
            })
        }
    }

    private fun initQualitySettingView() {
        with(qualitySettingBinding) {
            setContentView(root)
            titleLayout.tvTitle.text = getString(R.string.setting)
            titleLayout.ivBack.setOnClickListener {
                onBackPressed()
            }
            // 控件初始值设定
            if (spVoipLocalResolution != null) {
                val adapter1: ArrayAdapter<ResolutionEntity> = ArrayAdapter<ResolutionEntity>(
                    this@SettingActivity,
                    android.R.layout.simple_spinner_item,
                    localResolutionArray
                )
                spVoipLocalResolution.setAdapter(adapter1)
            }

            val adapter2: ArrayAdapter<String> = ArrayAdapter<String>(
                this@SettingActivity,
                android.R.layout.simple_spinner_item,
                wxResolutionArray
            )
            spVoipWxResolution.setAdapter(adapter2)


            if (QualitySetting.getInstance(this@SettingActivity).width != 0) {
                setSpinnerDefaultValueFromDisk(
                    QualitySetting.getInstance(this@SettingActivity).width,
                    QualitySetting.getInstance(this@SettingActivity).height
                )
                selectedFrameRate = QualitySetting.getInstance(this@SettingActivity).frameRate
                selectedBitRate = QualitySetting.getInstance(this@SettingActivity).bitRate
                selectedWxResolution = QualitySetting.getInstance(this@SettingActivity).wxResolution
                selectedWxCameraSetting =
                    QualitySetting.getInstance(this@SettingActivity).isWxCameraOn
            } else {
                if (!setSpinnerDefaultValue("360p")) {
                    setSelectedLocalResolution(0)
                }
            }
            spVoipLocalResolution.setSelection(selectedLocalResolution)
            sbVoipLocalFrameRate.max = 24 - 10
            sbVoipLocalFrameRate.progress = selectedFrameRate - 10
            setSelectedFrameRate(selectedFrameRate)
            spVoipWxResolution.setSelection(selectedWxResolution)
            swVoipWxCameraIsOpen.isChecked = selectedWxCameraSetting


            //控件事件
            spVoipLocalResolution.onItemSelectedListener = object :
                AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>?,
                    view: View,
                    position: Int,
                    l: Long
                ) {
                    setSelectedLocalResolution(position)
                    needShowDefaultValue = false
                }

                override fun onNothingSelected(adapterView: AdapterView<*>?) {}
            }
            sbVoipLocalFrameRate.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        setSelectedFrameRate(progress + 10)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {}
                override fun onStopTrackingTouch(seekBar: SeekBar) {}
            })

            sbVoipLocalBitRate.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        setSelectedBitRate(progress + minBitRateSbValue)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {}
                override fun onStopTrackingTouch(seekBar: SeekBar) {}
            })

            spVoipWxResolution.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        adapterView: AdapterView<*>?,
                        view: View,
                        position: Int,
                        l: Long
                    ) {
                        selectedWxResolution = position
                    }

                    override fun onNothingSelected(adapterView: AdapterView<*>?) {
                    }
                }

            swVoipWxCameraIsOpen.setOnCheckedChangeListener { _, checked ->
                selectedWxCameraSetting = checked
            }
            btnConfirm.setOnClickListener {
                val entity: ResolutionEntity = localResolutionArray[selectedLocalResolution]
                QualitySetting.getInstance(this@SettingActivity).width = entity.width
                QualitySetting.getInstance(this@SettingActivity).height = entity.height
                QualitySetting.getInstance(this@SettingActivity).frameRate = selectedFrameRate
                QualitySetting.getInstance(this@SettingActivity).bitRate = selectedBitRate
                QualitySetting.getInstance(this@SettingActivity).wxResolution = selectedWxResolution
                QualitySetting.getInstance(this@SettingActivity).isWxCameraOn =
                    selectedWxCameraSetting
                QualitySetting.getInstance(this@SettingActivity).saveData()

                Log.e(
                    TAG,
                    "****========== width:" + entity.width + "， height: " + entity.height + "， frameRate:" +
                            selectedFrameRate + "， bitRate:" + selectedBitRate + "， wxResolution:" + selectedWxResolution
                            + "， CameraSetting:" + selectedWxCameraSetting + "****========== "
                )
                it.postDelayed({
                    onBackPressed()
                }, 500)
            }
        }
    }

    private fun checkWxAppInfo(): Boolean {
        if (voipSetting.modelId.isEmpty() || voipSetting.sn.isEmpty() || voipSetting.snTicket.isEmpty() || voipSetting.appId.isEmpty()) {
            Toast.makeText(this, "请输入小程序信息！", Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }

    private fun saveWxAppInfo() {
        voipSetting
            .setModelId(wxSettingLayoutBinding.etVoipModelId.getText().toString())
        voipSetting
            .setSn(wxSettingLayoutBinding.etVoipSn.getText().toString())
        voipSetting
            .setSnTicket(wxSettingLayoutBinding.etVoipSnTicket.getText().toString())
        voipSetting
            .setAppId(wxSettingLayoutBinding.etVoipWxAppId.getText().toString())
    }

    private fun setSpinnerDefaultValue(defaultValue: String): Boolean {
        for (i in localResolutionArray.indices) {
            if (localResolutionArray[i].simpleName == defaultValue) {
                setSelectedLocalResolution(i)
                qualitySettingBinding.spVoipLocalResolution.setSelection(i)
                return true
            }
        }
        return false
    }

    private fun setSpinnerDefaultValueFromDisk(width: Int, height: Int): Boolean {
        for (i in localResolutionArray.indices) {
            val entity = localResolutionArray[i]
            if (entity.width == width && entity.height == height) {
                setSelectedLocalResolution(i)
                qualitySettingBinding.spVoipLocalResolution.setSelection(i)
                return true
            }
        }
        return false
    }

    fun setSelectedLocalResolution(selectedLocalResolution: Int) {
        this.selectedLocalResolution = selectedLocalResolution
        val entity = localResolutionArray[selectedLocalResolution]
        when (entity.simpleName) {
            "360p" -> {
                minBitRateSbValue = 200
                qualitySettingBinding.sbVoipLocalBitRate.max = 1000 - minBitRateSbValue
                if (needShowDefaultValue && QualitySetting.getInstance(this).bitRate != 0) {
                    setSelectedBitRate(QualitySetting.getInstance(this).bitRate)
                    qualitySettingBinding.sbVoipLocalBitRate.progress =
                        selectedBitRate - minBitRateSbValue
                } else {
                    qualitySettingBinding.sbVoipLocalBitRate.progress = 800 - minBitRateSbValue
                    setSelectedBitRate(800)
                }
            }

            "540p" -> {
                minBitRateSbValue = 400
                qualitySettingBinding.sbVoipLocalBitRate.max = 1600 - minBitRateSbValue
                if (needShowDefaultValue && QualitySetting.getInstance(this).bitRate != 0) {
                    setSelectedBitRate(QualitySetting.getInstance(this).bitRate)
                    qualitySettingBinding.sbVoipLocalBitRate.progress =
                        selectedBitRate - minBitRateSbValue
                } else {
                    qualitySettingBinding.sbVoipLocalBitRate.progress = 900 - minBitRateSbValue
                    setSelectedBitRate(900)
                }
            }

            "720p" -> {
                minBitRateSbValue = 500
                qualitySettingBinding.sbVoipLocalBitRate.max = 2000 - minBitRateSbValue
                if (needShowDefaultValue && QualitySetting.getInstance(this).bitRate != 0) {
                    setSelectedBitRate(QualitySetting.getInstance(this).bitRate)
                    qualitySettingBinding.sbVoipLocalBitRate.progress =
                        selectedBitRate - minBitRateSbValue
                } else {
                    qualitySettingBinding.sbVoipLocalBitRate.progress = 1250 - minBitRateSbValue
                    setSelectedBitRate(1250)
                }
            }

            "1080p" -> {
                minBitRateSbValue = 800
                qualitySettingBinding.sbVoipLocalBitRate.max = 3000 - minBitRateSbValue
                if (needShowDefaultValue && QualitySetting.getInstance(this).bitRate != 0) {
                    setSelectedBitRate(QualitySetting.getInstance(this).bitRate)
                    qualitySettingBinding.sbVoipLocalBitRate.progress =
                        selectedBitRate - minBitRateSbValue
                } else {
                    qualitySettingBinding.sbVoipLocalBitRate.progress = 1900 - minBitRateSbValue
                    setSelectedBitRate(1900)
                }
            }
        }
    }

    fun setSelectedFrameRate(selectedFrameRate: Int) {
        this.selectedFrameRate = selectedFrameRate
        qualitySettingBinding.tvVoipLocalFrameRateTip.text =
            String.format("%d fps", selectedFrameRate)
    }

    fun setSelectedBitRate(selectedBitRate: Int) {
        this.selectedBitRate = selectedBitRate
        qualitySettingBinding.tvVoipLocalFrameBitTip.text =
            String.format("%d kbps", selectedBitRate)
    }

    /**
     * 获取设备支持哪些分辨率
     */
    private fun getSupportedPreviewSizes() {
        val camera = Camera.open(CameraConstants.facing.BACK)
        //获取相机参数
        val parameters = camera.parameters
        val list = parameters.supportedPreviewSizes
        for (size in list) {
            Log.e(TAG, "****========== " + size.width + " " + size.height)
            if (size.width == 640 && size.height == 360) {
                val entity = ResolutionEntity(size.width, size.height, "360p")
                localResolutionArray.add(entity)
            } else if (size.width == 960 && size.height == 540) {
                val entity = ResolutionEntity(size.width, size.height, "540p")
                localResolutionArray.add(entity)
            } else if (size.width == 1280 && size.height == 720) {
                val entity = ResolutionEntity(size.width, size.height, "720p")
                localResolutionArray.add(entity)
            } else if (size.width == 1920 && size.height == 1080) {
                val entity = ResolutionEntity(size.width, size.height, "1080p")
                localResolutionArray.add(entity)
            }
        }
        camera.setPreviewCallback(null)
        camera.stopPreview()
        camera.release()
    }

    override fun onBackPressed() {
        if (intent.getStringExtra("type") == "wx_setting") {
            if (!checkWxAppInfo()) {
                setResult(-1)
            } else {
                intent.putExtra("voip_model_id", voipSetting.modelId)
                intent.putExtra("voip_device_id", voipSetting.sn)
                intent.putExtra("voip_wxa_appid", voipSetting.appId)
                intent.putExtra("voip_sn_ticket", voipSetting.snTicket)
                setResult(0,intent)
            }
        }
        super.onBackPressed()
    }
}