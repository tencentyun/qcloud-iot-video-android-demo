package com.example.ivdemo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.tencent.iot.twcall.R
import com.tencent.iot.twcall.databinding.ActivityLogBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class LogActivity : AppCompatActivity() {

    private val binding by lazy { ActivityLogBinding.inflate(layoutInflater) }
    protected val defaultScope = CoroutineScope(Dispatchers.Default)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        with(binding) {
            titleLayout.tvTitle.text = getString(R.string.title_log)
            titleLayout.ivBack.setOnClickListener { onBackPressed() }
            defaultScope.launch(Dispatchers.Default) {
                val logContent = LogcatHelper.getInstance(this@LogActivity.applicationContext)
                    .readLogFileContent()
                defaultScope.launch(Dispatchers.Main) {
                    tvLog.text = logContent
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        defaultScope.cancel()
    }
}