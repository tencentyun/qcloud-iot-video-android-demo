package com.example.ivdemo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tencent.iot.twcall.R
import com.tencent.iot.twcall.databinding.ActivityLogBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LogActivity : AppCompatActivity() {

    private val binding by lazy { ActivityLogBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        with(binding) {
            titleLayout.tvTitle.text = getString(R.string.title_log)
            titleLayout.ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
            lifecycleScope.launch(Dispatchers.Default) {
                val logContent = LogcatHelper.getInstance(this@LogActivity.applicationContext)
                    .readLogFileContent()
                lifecycleScope.launch(Dispatchers.Main) {
                    tvLog.text = logContent
                }
            }
        }
    }
}