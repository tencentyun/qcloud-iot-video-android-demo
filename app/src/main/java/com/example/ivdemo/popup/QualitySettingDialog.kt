package com.example.ivdemo.popup

import android.content.Context
import android.hardware.Camera
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.RadioButton
import androidx.fragment.app.FragmentManager
import com.tencent.iot.twcall.R
import com.tencent.iot.twcall.databinding.PopupQualitySettingLayoutBinding
import com.tencent.iotvideo.link.consts.CameraConstants
import com.tencent.iotvideo.link.entity.ResolutionEntity
import com.tencent.iotvideo.link.util.QualitySetting
import com.tencent.iotvideo.link.util.getSupportVideoEncoder

private val TAG: String = QualitySettingDialog::class.java.simpleName

class QualitySettingDialog(private val context: Context) :
    IosCenterStyleDialog<PopupQualitySettingLayoutBinding>(context), OnItemSelectedListener {

    private val qualitySetting by lazy { QualitySetting.getInstance(context) }

    private var encodeType = 0

    private var supportMediaCodeList: List<MediaCodecInfo>? = null

    private var selectMediaCodecInfo: MediaCodecInfo? = null

    private var localResolutionArray: ArrayList<ResolutionEntity>? = null

    private var selectResolutionEntity: ResolutionEntity? = null

    private var selectedWxResolution = 0

    private var selectedWxCameraSetting = true

    /**
     * 获取设备支持哪些分辨率
     */
    private fun getSupportPreviewSizes(): ArrayList<ResolutionEntity> {
        val localResolutionArray = arrayListOf<ResolutionEntity>()
        val camera = Camera.open(CameraConstants.facing.BACK)
        //获取相机参数
        val list = camera.parameters.supportedPreviewSizes
        for (size in list) {
            if (!checkSupportEncoderPixels(size.width, size.height)) {
                Log.d(TAG, "encoder no support,preview size:${size.height}x${size.width}")
                continue
            }
            if (!checkSupportLocationPixels(size.width, size.height)) {
                Log.d(TAG, "location no support,preview size:${size.height}x${size.width}")
                continue
            }
            Log.e(TAG, "****========== " + size.width + " " + size.height)
            val entity = ResolutionEntity(size.width, size.height, "${size.height}p")
            localResolutionArray.add(entity)
        }
        return localResolutionArray
    }

    private fun checkSupportEncoderPixels(height: Int, width: Int): Boolean {
        val videoCapabilities =
            selectMediaCodecInfo?.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC)?.videoCapabilities
        return videoCapabilities?.isSizeSupported(width, height)
            ?: false
    }

    private fun checkSupportLocationPixels(height: Int, width: Int): Boolean {
        val localPixels = resources.getStringArray(R.array.call_local_pixels_values)
        return localPixels.contains("${height}x$width")
    }

    override fun getViewBinding(): PopupQualitySettingLayoutBinding =
        PopupQualitySettingLayoutBinding.inflate(layoutInflater)

    public override fun initView() {
        //初始化缓存数据
        selectedWxResolution = qualitySetting.wxResolution
        selectedWxCameraSetting = qualitySetting.isWxCameraOn
        encodeType = qualitySetting.encodeType
        selectMediaCodecInfo = qualitySetting.mediaCodecInfo
        selectResolutionEntity = qualitySetting.resolutionEntity

        with(binding) {
            rbSoftEncode.isChecked = encodeType == 0
            rbHardEncode.isChecked = encodeType != 0
            rgSelectEncode.setOnCheckedChangeListener { group, checkedId ->
                val type = group.findViewById<RadioButton>(checkedId).tag.toString().toInt()
                encodeType = type
                supportMediaCodeList = getSupportVideoEncoder(encodeType)
                if (supportMediaCodeList?.isNotEmpty() == true) {
                    spSelectEncode.adapter = ArrayAdapter(
                        context,
                        android.R.layout.simple_spinner_item,
                        supportMediaCodeList?.map { it.name }!!.toList()
                    )
                }
            }

            spSelectEncode.onItemSelectedListener = this@QualitySettingDialog
            spTweCallLocalPixels.onItemSelectedListener = this@QualitySettingDialog
            spTweCallWxPixels.onItemSelectedListener = this@QualitySettingDialog

            swTweCallWxCameraIsOpen.setOnCheckedChangeListener { compoundButton, checked ->
                selectedWxCameraSetting = checked
            }
            btnConfirm.setOnClickListener {
                qualitySetting.encodeType = encodeType
                qualitySetting.mediaCodecInfo = selectMediaCodecInfo
                qualitySetting.resolutionEntity = selectResolutionEntity
                qualitySetting.wxResolution = selectedWxResolution
                qualitySetting.isWxCameraOn = selectedWxCameraSetting
                qualitySetting.saveData()

                Log.e(
                    TAG,
                    "****========== width:" + selectResolutionEntity?.width + "， height: " + selectResolutionEntity?.height + "， wxResolution:" + selectedWxResolution
                            + "， CameraSetting:" + selectedWxCameraSetting + "****========== "
                )
                dismiss()
                onDismiss?.invoke()
            }
        }


        binding.spTweCallWxPixels.setSelection(selectedWxResolution)
        binding.swTweCallWxCameraIsOpen.isChecked = selectedWxCameraSetting
        supportMediaCodeList = getSupportVideoEncoder(encodeType)
        if (supportMediaCodeList?.isNotEmpty() == true) {
            binding.spSelectEncode.adapter = ArrayAdapter(
                context,
                android.R.layout.simple_spinner_item,
                supportMediaCodeList?.map { it.name }!!.toList()
            )
            if (selectMediaCodecInfo != null) {
                supportMediaCodeList?.forEachIndexed { index, mediaCodecInfo ->
                    if (mediaCodecInfo.name == selectMediaCodecInfo?.name) {
                        binding.spSelectEncode.setSelection(index)
                    }
                }
            }
        }
    }

    fun show(manager: FragmentManager) {
        super.show(manager, "QualitySettingDialog")
    }

    private var onDismiss: (() -> Unit)? = null

    fun setOnDismissListener(onDismiss: () -> Unit) {
        this.onDismiss = onDismiss
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        when (parent) {
            binding.spSelectEncode -> {
                selectMediaCodecInfo = supportMediaCodeList?.get(position)
                localResolutionArray = getSupportPreviewSizes()
                binding.spTweCallLocalPixels.adapter = ArrayAdapter(
                    context,
                    android.R.layout.simple_spinner_item,
                    localResolutionArray!!
                )
                localResolutionArray!!.forEachIndexed { index, resolutionEntity ->
                    if (resolutionEntity.width == selectResolutionEntity?.width && resolutionEntity.height == selectResolutionEntity?.height) {
                        binding.spTweCallLocalPixels.setSelection(index)
                    }
                }
            }

            binding.spTweCallLocalPixels -> {
                selectResolutionEntity = localResolutionArray?.get(position)
            }

            binding.spTweCallWxPixels -> {
                selectedWxResolution = position
            }
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
    }
}
