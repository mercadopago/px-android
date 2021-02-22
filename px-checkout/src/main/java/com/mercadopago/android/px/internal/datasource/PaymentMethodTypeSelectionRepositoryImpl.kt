package com.mercadopago.android.px.internal.datasource

import android.content.SharedPreferences
import com.google.gson.reflect.TypeToken
import com.mercadopago.android.px.internal.repository.PaymentMethodTypeSelectionRepository
import com.mercadopago.android.px.internal.util.JsonUtil

private const val PREF_PM_TYPE_SELECTED = "PREF_PM_TYPE_SELECTED"

internal class PaymentMethodTypeSelectionRepositoryImpl(
    private val sharedPreferences: SharedPreferences
) : PaymentMethodTypeSelectionRepository {

    private var paymentMethodTypeSelectedMap: HashMap<String, String> = hashMapOf()

    override fun get(paymentMethodId: String): String = paymentMethodTypeSelectedMap
        .getOrElse(paymentMethodId) { getPaymentMethodTypeSelectedFromPreference(paymentMethodId) }

    override fun save(paymentMethodId: String, paymentMethodType: String) {
        paymentMethodTypeSelectedMap[paymentMethodId] = paymentMethodType
        sharedPreferences
            .edit()
            .putString(PREF_PM_TYPE_SELECTED, JsonUtil.toJson(paymentMethodTypeSelectedMap))
            .apply()
    }

    override fun reset() {
        sharedPreferences.edit().remove(PREF_PM_TYPE_SELECTED).apply()
        paymentMethodTypeSelectedMap.clear()
    }

    override fun isEmpty() = paymentMethodTypeSelectedMap.isEmpty()

    private fun getPaymentMethodTypeSelectedFromPreference(paymentMethodId: String) = sharedPreferences
        .getString(PREF_PM_TYPE_SELECTED, null)
        ?.let(::getMapFromJson)
        ?.also(paymentMethodTypeSelectedMap::putAll)
        ?.get(paymentMethodId)
        ?: throw IllegalStateException("Shared preference payment method type must not be null")

    private fun getMapFromJson(jsonMapPaymentMethodTypeSelected: String): HashMap<String, String> {
        val type = object : TypeToken<HashMap<String, String>>() {}.type
        return JsonUtil.fromJson<HashMap<String, String>>(jsonMapPaymentMethodTypeSelected, type)
    }
}