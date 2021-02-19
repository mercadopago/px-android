package com.mercadopago.android.px.model.internal

import android.os.Parcelable
import com.mercadopago.android.px.model.StatusMetadata
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Application(
    val paymentMethod: PaymentMethod,
    val validationPrograms: List<ValidationProgram>,
    val status: StatusMetadata
): Parcelable {

    @Parcelize
    data class PaymentMethod(
        val id: String,
        val type: String
    ): Parcelable

    @Parcelize
    data class ValidationProgram(
        val id: String,
        val mandatory: Boolean
    ): Parcelable
}