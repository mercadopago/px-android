package com.mercadopago.android.px.internal.viewmodel

import com.mercadopago.android.px.internal.view.PaymentMethodDescriptorView

class PaymentMethodDescriptorModel(
    var currentSelection: String?,
    val paymentMethodDescriptorViewMap: Map<String, PaymentMethodDescriptorView.Model>
    ) {
        fun getSelectedModel() = currentSelection?.let { paymentMethodDescriptorViewMap[it] } ?: paymentMethodDescriptorViewMap.values.first()
    }