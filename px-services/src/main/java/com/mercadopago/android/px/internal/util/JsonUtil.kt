package com.mercadopago.android.px.internal.util

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.mercadopago.android.px.internal.util.ObjectMapTypeAdapter.ObjectMapType

object JsonUtil {
    @JvmStatic
    val gson: Gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .serializeNulls()
        .registerTypeAdapterFactory(ObjectMapTypeAdapter.FACTORY)
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
        .create()

    @JvmStatic
    fun <T> getListFromJson(json: String?, classOfT: Class<T>): List<T> {
        val typeOfT = TypeToken.getParameterized(MutableList::class.java, classOfT).type
        return gson.fromJson(json, typeOfT) ?: mutableListOf()
    }

    @JvmStatic
    fun <K, V> getCustomMapFromJson(json: String?, classOfK: Class<K>, classOfV: Class<V>): Map<K, V> {
        val typeOfT = TypeToken.getParameterized(MutableMap::class.java, classOfK, classOfV).type
        return gson.fromJson(json, typeOfT) ?: mutableMapOf()
    }

    @JvmStatic
    fun <T> fromJson(json: String?, classOfT: Class<T>): T? {
        return gson.fromJson(json, classOfT)
    }

    @JvmStatic
    fun getMapFromJson(json: String?): Map<String, Any> {
        return gson.fromJson(
            json, object : TypeToken<ObjectMapType>() {}.type
        ) ?: mutableMapOf()
    }

    @JvmStatic
    fun getStringMapFromJson(json: String?): Map<String, String> {
        return gson.fromJson(json, object : TypeToken<Map<String, String>>() {}.type) ?: mutableMapOf()
    }

    @JvmStatic
    fun getMapFromObject(src: Any?): Map<String, Any> {
        return getMapFromJson(gson.toJson(src))
    }

    @JvmStatic
    fun toJson(src: Any?): String {
        return gson.toJson(src)
    }
}
