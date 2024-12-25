package com.example.ivdemo

import com.tencent.iot.twcall.R
import com.tencent.iot.twcall.databinding.AcitivtyAiTestBinding
import com.tencent.iot.video.device.VideoNativeInterface
import com.tencent.iot.video.device.consts.P2pEventType

class AiTestActivity : BaseIPCActivity<AcitivtyAiTestBinding>() {
    // 测试开启AI
    private val LLMCONFIG_TEST_STRING =
        "{\\\"LLMType\\\": \\\"openai\\\",\\\"Model\\\":\\\"ChatGPT\\\",\\\"APIKey\\\":\\\"12345678\\\",\\\"APIUrl\\\": \\\"https://api.xxx.com/chat/completions\\\", \\\"Streaming\\\": true}"
    private val TTSCONFIG_TEST_STRING =
        "{\\\"AppId\\\": 20241212, \\\"TTSType\\\": \\\"TTSType\\\",\\\"SecretId\\\": \\\"your_secret\\\",\\\"SecretKey\\\":  \\\"your_key\\\",\\\"VoiceType\\\": 101001,\\\"Speed\\\": 1.25,\\\"Volume\\\": 5,\\\"PrimaryLanguage\\\": \\\"zh-CN\\\"}"

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
                    VideoNativeInterface.getInstance()
                        .startAi(LLMCONFIG_TEST_STRING, TTSCONFIG_TEST_STRING)
                }
            }
            btnEndAi.setOnClickListener {
                // TODO:
            }
        }
    }

    override fun onOnline(netDateTime: Long) {
        super.onOnline(netDateTime)
    }

    override fun onStartRealPlay(visitor: Int, channel: Int, videoResType: Int) {
        super.onStartRealPlay(visitor, channel, videoResType)
        // TODO:  
    }

    override fun onDestroy() {
        checkDefaultThreadActiveAndExecuteTask {
            VideoNativeInterface.getInstance().stopAi()
        }
        super.onDestroy()
    }
}