package com.mercadopago.android.px.internal.repository

import com.mercadopago.android.px.model.CustomSearchItem

internal interface PayerPaymentMethodRepository : LocalRepository<List<@kotlin.jvm.JvmSuppressWildcards CustomSearchItem>> {
    operator fun get(key: Key): CustomSearchItem?
    fun getIdsWithSplitAllowed(): Set<String>

    data class Key(
        val payerPaymentMethodId: String,
        val paymentMethodId: String,
        val paymentTypeId: String
    )
}