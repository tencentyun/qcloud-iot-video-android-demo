package com.tencent.iotvideo.link.util

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tencent.iotvideo.link.entity.UserEntity
import com.tencent.mmkv.MMKV
import org.json.JSONArray

class DeviceSetting private constructor() {

    companion object {
        private val TAG: String = DeviceSetting::class.java.simpleName
        private var instance: DeviceSetting? = null

        @Synchronized
        fun getInstance(): DeviceSetting {
            if (instance == null) instance = DeviceSetting()
            return instance!!
        }
    }

    private val mmkv by lazy { MMKV.defaultMMKV() }
    private val openIds by lazy { ArrayList<UserEntity>(3) }
    private val gson by lazy { Gson() }
    private var isLoadOpenIds = false

    var ipcType: Int
        set(value) {
            Log.d(TAG, "mmkv save date key:ipcType  value:$value")
            mmkv.encode("ipcType", value)
        }
        get() {
            val value = mmkv.decodeInt("ipcType", 2)
            Log.d(TAG, "mmkv get date key:ipcType  value:$value")
            return value
        }
    var productId: String
        set(value) {
            Log.d(TAG, "mmkv save date key:productId  value:$value")
            mmkv.encode("productId", value)
        }
        get() {
            val value = mmkv.decodeString("productId", "") ?: ""
            Log.d(TAG, "mmkv get date key:productId  value:$value")
            return value
        }

    var deviceName: String
        set(value) {
            Log.d(TAG, "mmkv save date key:deviceName  value:$value")
            mmkv.encode("deviceName", value)
        }
        get() {
            val value = mmkv.decodeString("deviceName", "") ?: ""
            Log.d(TAG, "mmkv get date key:deviceName  value:$value")
            return value
        }

    var deviceKey: String
        set(value) {
            Log.d(TAG, "mmkv save date key:deviceKey  value:$value")
            mmkv.encode("deviceKey", value)
        }
        get() {
            val value = mmkv.decodeString("deviceKey", "") ?: ""
            Log.d(TAG, "mmkv get date key:deviceKey  value:$value")
            return value
        }

    var modelId: String
        set(value) {
            Log.d(TAG, "mmkv save date key:modelId  value:$value")
            mmkv.encode("modelId", value)
        }
        get() {
            val value = mmkv.decodeString("modelId", "") ?: ""
            Log.d(TAG, "mmkv get date key:modelId  value:$value")
            return value
        }

    var appId: String
        set(value) {
            Log.d(TAG, "mmkv save date key:appId  value:$value")
            mmkv.encode("appId", value)
        }
        get() {
            val value = mmkv.decodeString("appId", "") ?: ""
            Log.d(TAG, "mmkv get date key:appId  value:$value")
            return value
        }

    var openIdList: ArrayList<UserEntity>
        private set(value) {

        }
        get() {
            if (!isLoadOpenIds) {
                isLoadOpenIds = true
                val value = mmkv.decodeString("openIdList", "") ?: ""
                Log.d(TAG, "mmkv get date key:openIdList  value:$value")
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
        Log.d(TAG, "mmkv save date key:openIdList  value:$openIdsStr")
        mmkv.encode("openIdList", openIdsStr)
    }
}