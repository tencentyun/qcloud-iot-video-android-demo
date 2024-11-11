package com.tencent.iotvideo.link.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tencent.iotvideo.link.entity.UserEntity

class DeviceSetting private constructor(context: Context) {

    companion object {
        private val TAG: String = DeviceSetting::class.java.simpleName
        private var instance: DeviceSetting? = null
        private const val PREFERENCES_NAME = "DeviceSetting"


        @Synchronized
        fun getInstance(context: Context): DeviceSetting {
            if (instance == null) instance = DeviceSetting(context)
            return instance!!
        }
    }

    private var preferences: SharedPreferences? = null
    private var editor: SharedPreferences.Editor? = null
    private val openIds by lazy { ArrayList<UserEntity>(3) }
    private val gson by lazy { Gson() }
    private var isLoadOpenIds = false

    var ipcType: Int
        set(value) {
            Log.d(TAG, "editor? save date key:ipcType  value:$value")
            editor?.putInt("ipcType", value)
            editor?.apply()
        }
        get() {
            val value = preferences?.getInt("ipcType", 2) ?: 2
            Log.d(TAG, "editor? get date key:ipcType  value:$value")
            return value
        }
    var productId: String
        set(value) {
            Log.d(TAG, "editor? save date key:productId  value:$value")
            editor?.putString("productId", value)
            editor?.apply()
        }
        get() {
            val value = preferences?.getString("productId", "") ?: ""
            Log.d(TAG, "editor? get date key:productId  value:$value")
            return value
        }

    var deviceName: String
        set(value) {
            Log.d(TAG, "editor? save date key:deviceName  value:$value")
            editor?.putString("deviceName", value)
            editor?.apply()
        }
        get() {
            val value = preferences?.getString("deviceName", "") ?: ""
            Log.d(TAG, "editor? get date key:deviceName  value:$value")
            return value
        }

    var deviceKey: String
        set(value) {
            Log.d(TAG, "editor? save date key:deviceKey  value:$value")
            editor?.putString("deviceKey", value)
            editor?.apply()
        }
        get() {
            val value = preferences?.getString("deviceKey", "") ?: ""
            Log.d(TAG, "editor? get date key:deviceKey  value:$value")
            return value
        }

    var modelId: String
        set(value) {
            Log.d(TAG, "editor? save date key:modelId  value:$value")
            editor?.putString("modelId", value)
            editor?.apply()
        }
        get() {
            val value = preferences?.getString("modelId", "") ?: ""
            Log.d(TAG, "editor? get date key:modelId  value:$value")
            return value
        }

    var appId: String
        set(value) {
            Log.d(TAG, "editor? save date key:appId  value:$value")
            editor?.putString("appId", value)
            editor?.apply()
        }
        get() {
            val value = preferences?.getString("appId", "") ?: ""
            Log.d(TAG, "editor? get date key:appId  value:$value")
            return value
        }

    var openIdList: ArrayList<UserEntity>
        private set(value) {

        }
        get() {
            if (!isLoadOpenIds) {
                isLoadOpenIds = true
                val value = preferences?.getString("openIdList", "") ?: ""
                Log.d(TAG, "editor? get date key:openIdList  value:$value")
                if (value.isNotEmpty()) {
                    kotlin.runCatching {
                        val listType = object : TypeToken<ArrayList<UserEntity>>() {}.type
                        val arrayList: ArrayList<UserEntity> = gson.fromJson(value, listType)
                        openIds.addAll(arrayList)
                    }
                }
            }
            return openIds
        }

    fun addOnlyEntity(userEntity: UserEntity) {
        val entity = openIds.find { it.openId == userEntity.openId }
        if (entity == null) openIds.add(0, userEntity) else {
            openIds.remove(entity)
            openIds.add(0, userEntity)
        }
        if (openIds.size > 3) {
            openIds.removeLast()
        }
        val openIdsStr = gson.toJson(openIds)
        Log.d(TAG, "editor? save date key:openIdList  value:$openIdsStr")
        editor?.putString("openIdList", openIdsStr)
        editor?.apply()
    }

    init {
        this.preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        this.editor = preferences?.edit()
    }
}