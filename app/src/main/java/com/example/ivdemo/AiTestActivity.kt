package com.example.ivdemo

import com.tencent.iot.twcall.R
import com.tencent.iot.twcall.databinding.AcitivtyAiTestBinding
import com.tencent.iot.video.device.VideoNativeInterface

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
        }
    }

    override fun onOnline(netDateTime: Long) {
        super.onOnline(netDateTime)
        VideoNativeInterface.getInstance().startAi(LLMCONFIG_TEST_STRING, TTSCONFIG_TEST_STRING)
    }

    override fun onDestroy() {
        checkDefaultThreadActiveAndExecuteTask {
            VideoNativeInterface.getInstance().stopAi()
        }
        super.onDestroy()
    }
}