package com.mercadopago.android.px.model.internal

import android.os.Parcel
import com.mercadopago.android.px.model.ExpressMetadata
import java.lang.UnsupportedOperationException

class OneTapItem(parcel: Parcel?) : ExpressMetadata(parcel) {

    private var applications: List<Application>? = null

    init {
        throw UnsupportedOperationException("Parcelable implementation not available")
    }

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        super.writeToParcel(dest, flags)
        throw UnsupportedOperationException("Parcelable implementation not available")
    }

    fun getApplications() = applications

    fun getDefaultPaymentMethodType(): String = displayInfo
        ?.cardDrawerSwitch
        ?.default
        ?: paymentTypeId ?: paymentMethodId
}