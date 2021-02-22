package com.mercadopago.android.px.model.internal

import android.os.Parcel
import com.mercadopago.android.px.model.ExpressMetadata

class OneTapItem(parcel: Parcel?) : ExpressMetadata(parcel) {

    private lateinit var applications: List<Application>

    init {
        parcel?.readList(applications, Application::class.java.classLoader)
    }

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        super.writeToParcel(dest, flags)
        dest?.writeList(applications)
    }

    fun getApplications() = applications
}