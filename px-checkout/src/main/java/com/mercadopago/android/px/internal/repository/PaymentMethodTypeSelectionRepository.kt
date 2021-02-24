package com.mercadopago.android.px.internal.repository

import com.mercadopago.android.px.model.internal.OneTapItem

internal interface PaymentMethodTypeSelectionRepository: LocalRepository<HashMap<String, String>> {

    fun get(paymentMethodId: String): String

    fun save(paymentMethodId: String, paymentMethodType: String)

    fun configure(oneTapItems: List<OneTapItem>)
}