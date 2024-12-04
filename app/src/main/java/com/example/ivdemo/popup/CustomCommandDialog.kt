package com.example.ivdemo.popup

import android.content.Context
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import com.tencent.iot.twcall.databinding.PopupCustomCommandLayoutBinding
import com.tencent.iot.video.device.VideoNativeInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject


class CustomCommandDialog(context: Context, private val visitor: Int) :
    IosCenterStyleDialog<PopupCustomCommandLayoutBinding>(context) {

    protected val defaultScope = CoroutineScope(Dispatchers.Main)

    override fun getViewBinding(): PopupCustomCommandLayoutBinding =
        PopupCustomCommandLayoutBinding.inflate(layoutInflater)

    public override fun initView() {
        with(binding) {
            tvClose.setOnClickListener {
                dismiss()
            }
            btnConfirm.setOnClickListener {
                if (tvCommand.text.isEmpty()) {
                    showToast("发送的自定义信令为空")
                    return@setOnClickListener
                }
                val msg = tvCommand.text.toString()
                val msgJson = JSONObject()
                try {
                    msgJson.put("sendMsg", msg)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
                tvResult.append("发送信令 ==> $msgJson\n\n")
                val res = VideoNativeInterface.getInstance()
                    .sendCommand(visitor, msgJson.toString(), 1000)
                tvResult.append("发送信令结果 ==> ${if (res.isEmpty()) "失败" else "成功"}  res:$res\n\n")
            }
        }
    }

    fun receiveCommand(msg: String): JSONObject {
        binding.tvResult.append("接受信令 ==> $msg\n\n")
        var replyMsg = binding.tvCommand.text.toString()
        if (replyMsg.isEmpty()) {
            showToast("回复信令内容不能为空，已取默认值success")
            replyMsg = "success"
        }
        val resJson = JSONObject()
        try {
            resJson.put("code", 0)
            resJson.put("errMsg", "")
            resJson.put("Msg", replyMsg)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        binding.tvResult.append("回复信令 ==> $resJson\n\n")
        return resJson
    }

    fun show(manager: FragmentManager) {
        super.show(manager, "CustomCommandDialog")
    }

    private fun showToast(msg: String) {
        defaultScope.launch {
            Toast.makeText(requireContext().applicationContext, msg, Toast.LENGTH_SHORT).show();
        }
    }

    override fun dismiss() {
        super.dismiss()
        defaultScope.cancel()
    }
}
