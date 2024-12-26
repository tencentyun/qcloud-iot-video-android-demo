package com.example.ivdemo

import android.media.AudioFormat
import android.media.MediaRecorder
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

class AiTestActivity : BaseIPCActivity<AcitivtyAiTestBinding>(), OnEncodeListener {
    // 测试开启AI
    private val LLMCONFIG_TEST_STRING =
        "{\\\"LLMType\\\": \\\"openai\\\",\\\"Model\\\":\\\"ChatGPT\\\",\\\"APIKey\\\":\\\"12345678\\\",\\\"APIUrl\\\": \\\"https://api.xxx.com/chat/completions\\\", \\\"Streaming\\\": true}"
    private val TTSCONFIG_TEST_STRING =
        "{\\\"AppId\\\": 20241212, \\\"TTSType\\\": \\\"TTSType\\\",\\\"SecretId\\\": \\\"your_secret\\\",\\\"SecretKey\\\":  \\\"your_key\\\",\\\"VoiceType\\\": 101001,\\\"Speed\\\": 1.25,\\\"Volume\\\": 5,\\\"PrimaryLanguage\\\": \\\"zh-CN\\\"}"
    protected var isStartingAI = false
    private var mAudioEncoder: AudioEncoder? = null
    private val player = SimplePlayer()

    override fun getViewBinding(): AcitivtyAiTestBinding =
        AcitivtyAiTestBinding.inflate(layoutInflater)

    override fun initView() {
        with(binding) {
            titleLayout.tvTitle.text = getString(R.string.title_ai_test)
            titleLayout.ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
            tvContent.text = "   观沧海\n" +
                    "   东汉末年/三国·曹操  \n" +
                    "  东临碣石，以观沧海。\n" +
                    "  水何澹澹，山岛竦峙。\n" +
                    "  树木丛生，百草丰茂。\n" +
                    "  秋风萧瑟，洪波涌起。\n" +
                    "  日月之行，若出其中；\n" +
                    "  星汉灿烂，若出其里。\n" +
                    "  幸甚至哉，歌以咏志。\n"
            btnStartAi.setOnClickListener {
                if (!isOnline && !isP2pReady) return@setOnClickListener
                checkDefaultThreadActiveAndExecuteTask {
//                    VideoNativeInterface.getInstance().startAi(LLMCONFIG_TEST_STRING, TTSCONFIG_TEST_STRING)
                    VideoNativeInterface.getInstance().startAi("", "")
                }
                isStartingAI = true
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
    }

    override fun onStopRealPlay(visitor: Int, channel: Int, videoResType: Int) {
        mAudioEncoder?.stop()
        mAudioEncoder = null
    }

    override fun onStartRecvAudioStream(visitor: Int, channel: Int, type: Int, option: Int, mode: Int, width: Int, sample_rate: Int, sample_num: Int): Int {
        return player.startAudioPlay(visitor, type, option, mode, width, sample_rate, sample_num)
    }

    override fun onStopRecvStream(visitor: Int, channel: Int, streamType: Int): Int {
        return if (streamType == StreamType.IV_AVT_STREAM_TYPE_AUDIO) {
            player.stopAudioPlay(visitor)
        } else {
            player.stopVideoPlay(visitor)
        }
    }

    override fun onDestroy() {
        checkDefaultThreadActiveAndExecuteTask {
            VideoNativeInterface.getInstance().stopAi()
        }
        super.onDestroy()
    }



    override fun onAudioEncoded(datas: ByteArray?, pts: Long, seq: Long) {
//        if (wareReportSate) {
        VideoNativeInterface.getInstance().sendAvtAudioData(datas, pts, seq, visitor, channel, IV_AVT_VIDEO_AUDIO)
//        }
    }

    override fun onVideoEncoded(datas: ByteArray?, pts: Long, seq: Long, isKeyFrame: Boolean) {
    }
}