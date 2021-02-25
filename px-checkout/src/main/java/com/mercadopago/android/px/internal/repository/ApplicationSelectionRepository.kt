package com.mercadopago.android.px.internal.repository

import com.mercadopago.android.px.model.internal.Application

internal interface ApplicationSelectionRepository : LocalRepository<HashMap<String, Application>> {
    operator fun get(payerPaymentMethodId: String): Application?
    operator fun set(payerPaymentMethodId: String, application: Application)

    fun getPaymentMethodTypeId(payerPaymentMethodId: String): String
}