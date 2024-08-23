package com.tencent.iotvideo.link.adapter;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import com.tencent.iot.voipdemo.R;
import com.tencent.iotvideo.link.entity.UserEntity;
import com.tencent.iotvideo.link.util.VoipSetting;

import java.util.ArrayList;

public class UserListAdapter extends RecyclerView.Adapter<UserListAdapter.ViewHolder> ***REMOVED***
    private static String TAG = UserListAdapter.class.getSimpleName();
    private ArrayList<UserEntity> mDatas = null;
    private LayoutInflater mInflater = null;
    private int selectedPosition = RecyclerView.NO_POSITION;

    public UserListAdapter(Context context, ArrayList<UserEntity> datas) ***REMOVED***
        this.mDatas = datas;
        this.mInflater = LayoutInflater.from(context);
  ***REMOVED***

    // 创建新View，被LayoutManager所调用
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) ***REMOVED***
        View view = mInflater.inflate(R.layout.user_list_item, parent, false);
        ViewHolder vewHolder = new ViewHolder(view);
        return vewHolder;
  ***REMOVED***

    // 将数据与界面进行绑定的操作
    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) ***REMOVED***
        final UserEntity user = mDatas.get(position);
        switch (position%3) ***REMOVED***
            case 0:
                holder.headIconIv.setImageResource(R.drawable.dad);
                break;
            case 1:
                holder.headIconIv.setImageResource(R.drawable.mom);
                break;
            default:
                holder.headIconIv.setImageResource(R.drawable.baby);
                break;
      ***REMOVED***
        holder.openidEt.removeTextChangedListener(holder.textWatcher);
        holder.openidEt.setText(user.getOpenId());
        holder.textWatcher = new TextWatcher() ***REMOVED***
            @Override
            public void beforeTextChanged(CharSequence s, int i, int i1, int i2) ***REMOVED***

          ***REMOVED***

            @Override
            public void onTextChanged(CharSequence s, int i, int i1, int i2) ***REMOVED***
                String inputText = s.toString();
                Log.d(TAG, "onTextChanged: s: " + inputText + ", position: " + position);
                user.setOpenId(inputText);
                switch (position) ***REMOVED***
                    case 0:
                        VoipSetting.getInstance(mInflater.getContext()).setOpenId1(user.getOpenId());
                        break;
                    case 1:
                        VoipSetting.getInstance(mInflater.getContext()).setOpenId2(user.getOpenId());
                        break;
                    case 2:
                        VoipSetting.getInstance(mInflater.getContext()).setOpenId3(user.getOpenId());
                        break;
              ***REMOVED***
          ***REMOVED***

            @Override
            public void afterTextChanged(Editable editable) ***REMOVED***

          ***REMOVED***
      ***REMOVED***;
        holder.openidEt.addTextChangedListener(holder.textWatcher);
        holder.selectCb.setChecked(selectedPosition == position);
        user.setIsSelect(selectedPosition == position);
        holder.selectCb.setOnClickListener(new View.OnClickListener() ***REMOVED***
            @Override
            public void onClick(View v) ***REMOVED***
                selectedPosition = position;
                notifyDataSetChanged();
                if (onSelectedListener != null) ***REMOVED***
                    onSelectedListener.onSelected(position);
              ***REMOVED***
          ***REMOVED***
      ***REMOVED***);
  ***REMOVED***

    // 获取数据的数量
    @Override
    public int getItemCount() ***REMOVED***
        return mDatas == null ? 0 : mDatas.size();
  ***REMOVED***

    // 自定义的ViewHolder，持有每个Item的的所有界面组件
    public class ViewHolder extends RecyclerView.ViewHolder ***REMOVED***
        public ImageView headIconIv = null;
        public CheckBox selectCb = null;
        public EditText openidEt = null;
        TextWatcher textWatcher;

        public ViewHolder(View itemView) ***REMOVED***
            super(itemView);
            headIconIv = (ImageView) itemView.findViewById(R.id.iv_head_icon);
            selectCb = (CheckBox) itemView.findViewById(R.id.cb_select);
            openidEt = (EditText) itemView.findViewById(R.id.et_openid);
      ***REMOVED***
  ***REMOVED***

    private volatile OnSelectedListener onSelectedListener;

    public interface OnSelectedListener ***REMOVED***
        void onSelected(int position);
  ***REMOVED***

    public void setOnSelectedListener(OnSelectedListener onSelectedListener) ***REMOVED***
        this.onSelectedListener = onSelectedListener;
  ***REMOVED***
}