package com.mercadopago.android.px.internal.repository

import com.mercadopago.android.px.internal.callbacks.MPCall
import com.mercadopago.android.px.model.internal.CheckoutResponse

interface CheckoutRepository {
    fun checkout(): MPCall<CheckoutResponse>
    fun refreshWithNewCard(cardId: String): MPCall<CheckoutResponse>
    fun lazyConfigure(checkoutResponse: CheckoutResponse)
}