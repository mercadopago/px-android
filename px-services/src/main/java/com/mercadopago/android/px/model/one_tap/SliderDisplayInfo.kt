package com.mercadopago.android.px.model.one_tap

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.mercadopago.android.px.model.internal.CardDrawerSwitch
import com.mercadopago.android.px.model.internal.Text
import kotlinx.android.parcel.Parcelize

@Parcelize
data class SliderDisplayInfo(
    val bottomDescription: Text,
    @SerializedName("switch")
    val cardDrawerSwitch: CardDrawerSwitch? = null
) : Parcelable