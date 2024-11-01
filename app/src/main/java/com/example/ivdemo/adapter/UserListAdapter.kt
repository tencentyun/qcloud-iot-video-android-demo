package com.example.ivdemo.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tencent.iot.twcall.R
import com.tencent.iot.twcall.databinding.UserListItemBinding
import com.tencent.iotvideo.link.entity.UserEntity

class UserListAdapter : ListAdapter<UserEntity, UserListAdapter.UserViewHolder>(ItemCallback()) {

    private var selectedPosition = RecyclerView.NO_POSITION

    inner class UserViewHolder(private val binding: UserListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("NotifyDataSetChanged")
        fun bind(entity: UserEntity) {
            with(binding) {
                val resId = when (layoutPosition) {
                    0 -> R.drawable.dad
                    1 -> R.drawable.mom
                    else -> R.drawable.baby
                }
                ivHeadIcon.setImageResource(resId)
                tvOpenid.text = entity.openId
                entity.isSelect = selectedPosition == layoutPosition
                cbSelect.isChecked = entity.isSelect
                cbSelect.setOnClickListener {
                    selectedPosition = layoutPosition
                    notifyDataSetChanged()
                    onSelected?.invoke(layoutPosition, entity)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = UserListItemBinding.inflate(LayoutInflater.from(parent.context))
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(currentList[position])
    }

    internal class ItemCallback : DiffUtil.ItemCallback<UserEntity>() {
        override fun areItemsTheSame(oldItem: UserEntity, newItem: UserEntity): Boolean {
            return oldItem.openId == newItem.openId
        }

        override fun areContentsTheSame(oldItem: UserEntity, newItem: UserEntity): Boolean {
            return oldItem.openId == newItem.openId
        }
    }

    private var onSelected: ((Int, UserEntity) -> Unit)? = null

    fun setOnSelectListener(onSelect: (Int, UserEntity) -> Unit) {
        this.onSelected = onSelect
    }
}