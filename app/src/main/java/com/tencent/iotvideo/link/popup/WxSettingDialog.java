package com.tencent.iotvideo.link.popup;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.tencent.iot.voipdemo.R;
import com.tencent.iotvideo.link.util.VoipSetting;

public class WxSettingDialog extends IosCenterStyleDialog ***REMOVED***

    private EditText mModelIdEt;
    private EditText mSNEt;
    private EditText mSNTicketEt;
    private EditText mAppIDEt;
    private Button mConfirmBtn;

    public WxSettingDialog(Context context) ***REMOVED***
        super(context, R.layout.popup_wx_setting_layout);
  ***REMOVED***

    @Override
    public void initView() ***REMOVED***
        mModelIdEt = view.findViewById(R.id.et_voip_model_id);
        mSNEt = view.findViewById(R.id.et_voip_sn);
        mSNTicketEt = view.findViewById(R.id.et_voip_sn_ticket);
        mAppIDEt = view.findViewById(R.id.et_voip_wx_app_id);
        mConfirmBtn = view.findViewById(R.id.btn_confirm);

        mModelIdEt.setText(VoipSetting.getInstance(getContext()).modelId);
        mSNEt.setText(VoipSetting.getInstance(getContext()).sn);
        mSNTicketEt.setText(VoipSetting.getInstance(getContext()).snTicket);
        mAppIDEt.setText(VoipSetting.getInstance(getContext()).appId);

        mConfirmBtn.setOnClickListener(new View.OnClickListener() ***REMOVED***
            @Override
            public void onClick(View view) ***REMOVED***
                if (!checkWxAppInfo()) return;
                saveWxAppInfo();
                dismiss();
                if (onDismisListener != null) ***REMOVED***
                    onDismisListener.onDismised();
              ***REMOVED***
          ***REMOVED***
      ***REMOVED***);
  ***REMOVED***

    private boolean checkWxAppInfo() ***REMOVED***
        String modelId = mModelIdEt.getText().toString();
        String sn = mSNEt.getText().toString();
        String snTicket = mSNTicketEt.getText().toString();
        String appId = mAppIDEt.getText().toString();
        if (modelId.isEmpty() || sn.isEmpty() || snTicket.isEmpty() || appId.isEmpty()) ***REMOVED***
            Toast.makeText(getContext(), "请输入小程序信息！", Toast.LENGTH_LONG).show();
            return false;
      ***REMOVED***
        return true;
  ***REMOVED***

    private void saveWxAppInfo() ***REMOVED***
        VoipSetting.getInstance(getContext()).setModelId(mModelIdEt.getText().toString());
        VoipSetting.getInstance(getContext()).setSn(mSNEt.getText().toString());
        VoipSetting.getInstance(getContext()).setSnTicket(mSNTicketEt.getText().toString());
        VoipSetting.getInstance(getContext()).setAppId(mAppIDEt.getText().toString());
  ***REMOVED***

    private volatile OnDismisListener onDismisListener;

    public interface OnDismisListener ***REMOVED***
        void onDismised();
  ***REMOVED***

    public void setOnDismisListener(OnDismisListener onDismisListener) ***REMOVED***
        this.onDismisListener = onDismisListener;
  ***REMOVED***
}
