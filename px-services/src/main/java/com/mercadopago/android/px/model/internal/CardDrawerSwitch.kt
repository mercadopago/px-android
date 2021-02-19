package com.mercadopago.android.px.model.internal

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class CardDrawerSwitch(
    val states: SwitchStates,
    val default: String,
    val backgroundColor: String,
    val options: List<Option>
): Parcelable {

    @Parcelize
    data class SwitchStates(
        val checkedState: State,
        val uncheckedState: State,
        val disabledState: State
    ): Parcelable {

        @Parcelize
        data class State(
            val backgroundColor: String,
            val textColor: String,
            val weight: String
        ): Parcelable

    }

    @Parcelize
    data class Option(
        val id: String,
        val name: String
    ): Parcelable
}