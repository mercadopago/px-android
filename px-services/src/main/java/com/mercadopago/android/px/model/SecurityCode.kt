package com.mercadopago.android.px.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.io.Serializable

@Parcelize
data class SecurityCode(
    var cardLocation: String,
    var length: Int,
    var mode: String?) : Parcelable, Serializable
