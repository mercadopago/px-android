package com.mercadopago.android.px.model.internal

import android.os.Parcel
import com.mercadopago.android.px.model.ExpressMetadata

class ExpressMetadataInternal(parcel: Parcel?) : ExpressMetadata(parcel) {

    private var applications: List<Application>? = null

    init {
        applications?.let { parcel?.readList(it, Application::class.java.classLoader) }
    }

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        super.writeToParcel(dest, flags)
        dest?.writeList(applications)
    }

    fun getApplications() = applications

    fun getDefaultPaymentMethodType() = displayInfo?.cardDrawerSwitch?.default ?: paymentTypeId
}