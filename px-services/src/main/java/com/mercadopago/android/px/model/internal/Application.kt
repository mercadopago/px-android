package com.mercadopago.android.px.model.internal

import com.mercadopago.android.px.model.StatusMetadata

data class Application(
    val paymentMethod: PaymentMethod,
    val validationPrograms: List<ValidationProgram>,
    val status: StatusMetadata
) {

    data class PaymentMethod(
        val id: String,
        val type: String
    )

    data class ValidationProgram(
        val id: String,
        val mandatory: Boolean
    )
}