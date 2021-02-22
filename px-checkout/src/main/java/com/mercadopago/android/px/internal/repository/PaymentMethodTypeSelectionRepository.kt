package com.mercadopago.android.px.internal.repository

internal interface PaymentMethodTypeSelectionRepository: LocalRepository<HashMap<String, String>> {

    fun get(paymentMethodId: String): String

    fun save(paymentMethodId: String, paymentMethodType: String)
}