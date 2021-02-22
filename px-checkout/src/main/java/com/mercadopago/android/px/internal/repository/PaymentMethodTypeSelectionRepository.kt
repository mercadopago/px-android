package com.mercadopago.android.px.internal.repository

interface PaymentMethodTypeSelectionRepository {

    fun get(paymentMethodId: String): String

    fun save(paymentMethodId: String, paymentMethodType: String)

    fun reset()

    fun isEmpty(): Boolean
}