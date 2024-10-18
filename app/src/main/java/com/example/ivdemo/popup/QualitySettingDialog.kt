package com.example.ivdemo.popup

import android.content.Context
import android.hardware.Camera
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.fragment.app.FragmentManager
import com.tencent.iot.twcall.R
import com.tencent.iot.twcall.databinding.PopupQualitySettingLayoutBinding
import com.tencent.iotvideo.link.consts.CameraConstants
import com.tencent.iotvideo.link.entity.ResolutionEntity
import com.tencent.iotvideo.link.util.QualitySetting

class QualitySettingDialog(private val context: Context) :
    IosCenterStyleDialog<PopupQualitySettingLayoutBinding>(context) {

    private val localResolutionArray = arrayListOf<ResolutionEntity>()

    private var selectedLocalResolution = 0

    private var selectedFrameRate = 15

    private var minBitRateSbValue = 0
    private var selectedBitRate = 0

    private val wxResolutionArray =
        arrayOf("可变自适应", "240x320", "320x240", "480x352", "480x640")

    private var selectedWxResolution = 0

    private var selectedWxCameraSetting = true

    private var needShowDefaultValue = true
    override fun getViewBinding(): PopupQualitySettingLayoutBinding =
        PopupQualitySettingLayoutBinding.inflate(layoutInflater)

    public override fun initView() {
        with(binding) {

            // 控件初始值设定
            spVoipLocalResolution.adapter =
                ArrayAdapter(context, android.R.layout.simple_spinner_item, localResolutionArray)
            spVoipWxResolution.adapter =
                ArrayAdapter(context, android.R.layout.simple_spinner_item, wxResolutionArray)

            if (QualitySetting.getInstance(context).width != 0) {
                setSpinnerDefaultValueFromDisk(
                    QualitySetting.getInstance(context).width, QualitySetting.getInstance(
                        context
                    ).height
                )
                selectedFrameRate = QualitySetting.getInstance(context).frameRate
                selectedBitRate = QualitySetting.getInstance(context).bitRate
                selectedWxResolution = QualitySetting.getInstance(context).wxResolution
                selectedWxCameraSetting = QualitySetting.getInstance(context).isWxCameraOn
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
            spVoipLocalResolution.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
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

            swVoipWxCameraIsOpen.setOnCheckedChangeListener { compoundButton, checked ->
                selectedWxCameraSetting = checked
            }
            btnConfirm.setOnClickListener {
                val entity = localResolutionArray!![selectedLocalResolution]
                QualitySetting.getInstance(context).width = entity.width
                QualitySetting.getInstance(context).height = entity.height
                QualitySetting.getInstance(context).frameRate = selectedFrameRate
                QualitySetting.getInstance(context).bitRate = selectedBitRate
                QualitySetting.getInstance(context).wxResolution = selectedWxResolution
                QualitySetting.getInstance(context).isWxCameraOn = selectedWxCameraSetting
                QualitySetting.getInstance(context).saveData()

                Log.e(
                    TAG,
                    "****========== width:" + entity.width + "， height: " + entity.height + "， frameRate:" +
                            selectedFrameRate + "， bitRate:" + selectedBitRate + "， wxResolution:" + selectedWxResolution
                            + "， CameraSetting:" + selectedWxCameraSetting + "****========== "
                )
                dismiss()
                onDismiss?.invoke()
            }
        }
    }

    private val supportedPreviewSizes: Unit
        /**
         * 获取设备支持哪些分辨率
         */
        get() {
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

    private fun setSpinnerDefaultValue(defaultValue: String): Boolean {
        localResolutionArray.forEachIndexed { index, resolutionEntity ->
            if (localResolutionArray[index].simpleName == defaultValue) {
                setSelectedLocalResolution(index)
                binding.spVoipLocalResolution.setSelection(index)
                return true
            }
        }
        return false
    }

    private fun setSpinnerDefaultValueFromDisk(width: Int, height: Int): Boolean {
        localResolutionArray.forEachIndexed { index, resolutionEntity ->
            if (resolutionEntity.width == width && resolutionEntity.height == height) {
                setSelectedLocalResolution(index)
                binding.spVoipLocalResolution.setSelection(index)
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
                binding.sbVoipLocalBitRate.max = 1000 - minBitRateSbValue
                if (needShowDefaultValue && QualitySetting.getInstance(context).bitRate != 0) {
                    setSelectedBitRate(QualitySetting.getInstance(context).bitRate)
                    binding.sbVoipLocalBitRate.progress = selectedBitRate - minBitRateSbValue
                } else {
                    binding.sbVoipLocalBitRate.progress = 800 - minBitRateSbValue
                    setSelectedBitRate(800)
                }
            }

            "540p" -> {
                minBitRateSbValue = 400
                binding.sbVoipLocalBitRate.max = 1600 - minBitRateSbValue
                if (needShowDefaultValue && QualitySetting.getInstance(context).bitRate != 0) {
                    setSelectedBitRate(QualitySetting.getInstance(context).bitRate)
                    binding.sbVoipLocalBitRate.progress = selectedBitRate - minBitRateSbValue
                } else {
                    binding.sbVoipLocalBitRate.progress = 900 - minBitRateSbValue
                    setSelectedBitRate(900)
                }
            }

            "720p" -> {
                minBitRateSbValue = 500
                binding.sbVoipLocalBitRate.max = 2000 - minBitRateSbValue
                if (needShowDefaultValue && QualitySetting.getInstance(context).bitRate != 0) {
                    setSelectedBitRate(QualitySetting.getInstance(context).bitRate)
                    binding.sbVoipLocalBitRate.progress = selectedBitRate - minBitRateSbValue
                } else {
                    binding.sbVoipLocalBitRate.progress = 1250 - minBitRateSbValue
                    setSelectedBitRate(1250)
                }
            }

            "1080p" -> {
                minBitRateSbValue = 800
                binding.sbVoipLocalBitRate.max = 3000 - minBitRateSbValue
                if (needShowDefaultValue && QualitySetting.getInstance(context).bitRate != 0) {
                    setSelectedBitRate(QualitySetting.getInstance(context).bitRate)
                    binding.sbVoipLocalBitRate.progress = selectedBitRate - minBitRateSbValue
                } else {
                    binding.sbVoipLocalBitRate.progress = 1900 - minBitRateSbValue
                    setSelectedBitRate(1900)
                }
            }
        }
    }

    fun setSelectedFrameRate(selectedFrameRate: Int) {
        this.selectedFrameRate = selectedFrameRate
        binding.tvVoipLocalFrameRateTip.text =
            String.format(context.getString(R.string.text_fps), selectedFrameRate)
    }

    fun setSelectedBitRate(selectedBitRate: Int) {
        this.selectedBitRate = selectedBitRate
        binding.tvVoipLocalFrameBitTip.text =
            String.format(context.getString(R.string.text_kbps), selectedBitRate)
    }

    fun show(manager: FragmentManager) {
        super.show(manager, "QualitySettingDialog")
    }

    private var onDismiss: (()->Unit)? = null

    init {
        supportedPreviewSizes
    }

    fun setOnDismissListener(onDismiss: ()->Unit) {
        this.onDismiss = onDismiss
    }

    companion object {
        private val TAG: String = QualitySettingDialog::class.java.simpleName
    }
}