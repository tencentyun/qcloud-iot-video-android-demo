package com.example.ivdemo

import android.media.AudioFormat
import android.media.MediaRecorder
import android.widget.Button
import android.widget.RadioButton
import androidx.lifecycle.lifecycleScope
import com.tencent.iot.twcall.R
import com.tencent.iot.twcall.databinding.AcitivtyAiTestBinding
import com.tencent.iot.video.device.VideoNativeInterface
import com.tencent.iot.video.device.annotations.CmFrameType
import com.tencent.iot.video.device.annotations.CsChannelType
import com.tencent.iot.video.device.annotations.StreamType
import com.tencent.iot.video.device.annotations.VideoResType.IV_AVT_VIDEO_AUDIO
import com.tencent.iotvideo.link.SimplePlayer
import com.tencent.iotvideo.link.encoder.AudioEncoder
import com.tencent.iotvideo.link.listener.OnEncodeListener
import com.tencent.iotvideo.link.param.AudioEncodeParam
import com.tencent.iotvideo.link.param.MicParam
import com.tencent.iotvideo.link.util.updateOperate
import kotlinx.coroutines.launch

class AiTestActivity : BaseIPCActivity<AcitivtyAiTestBinding>(), OnEncodeListener {
    // 测试开启AI
    private val LLMCONFIG_TEST_STRING =
        "{\\\"LLMType\\\": \\\"openai\\\",\\\"Model\\\":\\\"ChatGPT\\\",\\\"APIKey\\\":\\\"12345678\\\",\\\"APIUrl\\\": \\\"https://api.xxx.com/chat/completions\\\", \\\"Streaming\\\": true}"
    private val TTSCONFIG_TEST_STRING =
        "{\\\"AppId\\\": 20241212, \\\"TTSType\\\": \\\"TTSType\\\",\\\"SecretId\\\": \\\"your_secret\\\",\\\"SecretKey\\\":  \\\"your_key\\\",\\\"VoiceType\\\": 101001,\\\"Speed\\\": 1.25,\\\"Volume\\\": 5,\\\"PrimaryLanguage\\\": \\\"zh-CN\\\"}"
    protected var isStartingAI = false
    private var mAudioEncoder: AudioEncoder? = null
    private var mPlayer: SimplePlayer? = null

    override fun getViewBinding(): AcitivtyAiTestBinding =
        AcitivtyAiTestBinding.inflate(layoutInflater)

    override fun initView() {
        with(binding) {
            titleLayout.tvTitle.text = getString(R.string.title_ai_test)
            titleLayout.ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
            textDevInfo.text = String.format((getString(R.string.text_device_info)), "${productId}_$deviceName")

            tvContent.text = "   观沧海\n" +
                    "   东汉末年/三国·曹操  \n" +
                    "  东临碣石，以观沧海。\n" +
                    "  水何澹澹，山岛竦峙。\n" +
                    "  树木丛生，百草丰茂。\n" +
                    "  秋风萧瑟，洪波涌起。\n" +
                    "  日月之行，若出其中；\n" +
                    "  星汉灿烂，若出其里。\n" +
                    "  幸甚至哉，歌以咏志。\n"

            tvText.text = LLMCONFIG_TEST_STRING

            btnStartAi.setOnClickListener {
                if (!isOnline && !isP2pReady) return@setOnClickListener

                if (isStartingAI) {
                    checkDefaultThreadActiveAndExecuteTask {
                        VideoNativeInterface.getInstance().stopAi()
                    }
                    isStartingAI = false
                    btnStartAi.text = "开始"
                }else {
                    checkDefaultThreadActiveAndExecuteTask {
//                    VideoNativeInterface.getInstance().startAi(LLMCONFIG_TEST_STRING, TTSCONFIG_TEST_STRING)
                        VideoNativeInterface.getInstance().startAi("", "")
                    }
                    isStartingAI = true
                    btnStartAi.text = "结束"
                }

            }
            btnEndAi.setOnClickListener {
                // TODO:
                if (isStartingAI)
                    checkDefaultThreadActiveAndExecuteTask {
                        VideoNativeInterface.getInstance().stopAi()
                    }
                isStartingAI = false
            }
        }
    }

    override fun onOnline(netDateTime: Long) {
        super.onOnline(netDateTime)
    }

    override fun onNotify(event: Int, visitor: Int, channel: Int, videoResType: Int) {
        super.onNotify(event, visitor, channel, videoResType)
        if (isP2pReady)
            lifecycleScope.launch {
                binding.btnStartAi.isEnabled = true
                binding.btnStartAi.updateOperate(true)
            }
    }

    override fun onStartRealPlay(visitor: Int, channel: Int, videoResType: Int) {
        super.onStartRealPlay(visitor, channel, videoResType)

        val micParam = MicParam()
        micParam.audioFormat = AudioFormat.ENCODING_PCM_16BIT
        micParam.channelConfig = AudioFormat.CHANNEL_IN_MONO
        micParam.sampleRateInHz = 16000
        micParam.audioSource = MediaRecorder.AudioSource.VOICE_COMMUNICATION
        val audioEncodeParam = AudioEncodeParam()
        audioEncodeParam.bitRate = 48000
        mAudioEncoder = AudioEncoder(micParam, audioEncodeParam, true, true)
        mAudioEncoder?.setOnEncodeListener(this)
        mAudioEncoder?.setMuted(false)
        mAudioEncoder?.start()

        mPlayer = SimplePlayer()
    }

    override fun onStopRealPlay(visitor: Int, channel: Int, videoResType: Int) {
        mAudioEncoder?.stop()
        mAudioEncoder = null

        mPlayer?.stopAudioPlay(visitor)
        mPlayer = null
    }

    override fun onStartRecvAudioStream(visitor: Int, channel: Int, type: Int, option: Int, mode: Int, width: Int, sample_rate: Int, sample_num: Int): Int {
        mPlayer?.startAudioPlay(visitor, type, option, mode, width, sample_rate, sample_num)
        return 0
    }

    override fun onStopRecvStream(visitor: Int, channel: Int, streamType: Int): Int {
        mPlayer?.stopAudioPlay(visitor)
        return 0
    }
    override fun onRecvStream(visitor: Int, streamType: Int, data: ByteArray?, len: Int, pts: Long, seq: Long): Int {
        mPlayer?.playAudioStream(visitor, data, len, pts, seq)
        return 0
    }

    override fun onDestroy() {
        checkDefaultThreadActiveAndExecuteTask {
            VideoNativeInterface.getInstance().stopAi()
        }
        super.onDestroy()
    }



    override fun onAudioEncoded(datas: ByteArray?, pts: Long, seq: Long) {
//        if (wareReportSate) {
        VideoNativeInterface.getInstance().sendAvtAudioData(datas, pts, seq, visitor, channel, videoResType)
//        }
    }

    override fun onVideoEncoded(datas: ByteArray?, pts: Long, seq: Long, isKeyFrame: Boolean) {
    }
}