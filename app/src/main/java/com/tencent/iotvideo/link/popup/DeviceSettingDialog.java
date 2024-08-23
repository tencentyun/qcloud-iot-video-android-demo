package com.tencent.iotvideo.link.popup;

import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.tencent.iot.voipdemo.R;
import com.tencent.iotvideo.link.util.VoipSetting;

public class DeviceSettingDialog extends IosCenterStyleDialog ***REMOVED***

    private EditText mJsonCopyEt;
    private EditText mProductEt;
    private EditText mDeviceNameEt;
    private EditText mDeviceKeyEt;
    private Button mConfirmBtn;

    public DeviceSettingDialog(Context context) ***REMOVED***
        super(context, R.layout.popup_device_setting_layout);
  ***REMOVED***

    @Override
    public void initView() ***REMOVED***
        mJsonCopyEt = view.findViewById(R.id.et_json_copy);
        mProductEt = view.findViewById(R.id.et_login_product_id);
        mDeviceNameEt = view.findViewById(R.id.et_login_device_name);
        mDeviceKeyEt = view.findViewById(R.id.et_login_device_key);
        mConfirmBtn = view.findViewById(R.id.btn_confirm);

        mProductEt.setText(VoipSetting.getInstance(getContext()).productId);
        mDeviceNameEt.setText(VoipSetting.getInstance(getContext()).deviceName);
        mDeviceKeyEt.setText(VoipSetting.getInstance(getContext()).deviceKey);

        mConfirmBtn.setOnClickListener(new View.OnClickListener() ***REMOVED***
            @Override
            public void onClick(View view) ***REMOVED***
                if (!checkDeviceInfo()) return;
                saveDeviceInfo();
                dismiss();
          ***REMOVED***
      ***REMOVED***);

        mJsonCopyEt.addTextChangedListener(new TextWatcher() ***REMOVED***
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) ***REMOVED***

          ***REMOVED***

            @Override
            public void onTextChanged(CharSequence s, int i, int i1, int i2) ***REMOVED***
                String inputText = s.toString();
                if (!TextUtils.isEmpty(inputText)) ***REMOVED***
                    if (VoipSetting.isJSONString(inputText)) ***REMOVED***
                        VoipSetting.getInstance(getContext()).saveData(inputText);
                        VoipSetting.getInstance(getContext()).loadValueToMemory();
                        mProductEt.setText(VoipSetting.getInstance(getContext()).productId);
                        mDeviceNameEt.setText(VoipSetting.getInstance(getContext()).deviceName);
                        mDeviceKeyEt.setText(VoipSetting.getInstance(getContext()).deviceKey);
                  ***REMOVED*** else ***REMOVED***
                        Toast.makeText(getContext(), "输入的json非法！", Toast.LENGTH_LONG).show();
                  ***REMOVED***
              ***REMOVED***
          ***REMOVED***

            @Override
            public void afterTextChanged(Editable editable) ***REMOVED***

          ***REMOVED***
      ***REMOVED***);
  ***REMOVED***

    private boolean checkDeviceInfo() ***REMOVED***
        String productId = mProductEt.getText().toString();
        String deviceName = mDeviceNameEt.getText().toString();
        String deviceKey = mDeviceKeyEt.getText().toString();
        if (productId.isEmpty() || deviceName.isEmpty() || deviceKey.isEmpty()) ***REMOVED***
            Toast.makeText(getContext(), "请输入设备信息！", Toast.LENGTH_LONG).show();
            return false;
      ***REMOVED***
        return true;
  ***REMOVED***

    private void saveDeviceInfo() ***REMOVED***
        VoipSetting.getInstance(getContext()).setProductId(mProductEt.getText().toString());
        VoipSetting.getInstance(getContext()).setDeviceName(mDeviceNameEt.getText().toString());
        VoipSetting.getInstance(getContext()).setDeviceKey(mDeviceKeyEt.getText().toString());
  ***REMOVED***
}
