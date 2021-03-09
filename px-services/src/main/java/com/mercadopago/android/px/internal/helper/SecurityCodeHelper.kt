package com.mercadopago.android.px.internal.helper

import com.mercadopago.android.px.model.SecurityCode

private const val MANDATORY = "mandatory"

internal object SecurityCodeHelper {
    fun isRequired(securityCode: SecurityCode?): Boolean {
        return securityCode?.let { it.length != 0 && it.mode.equals(MANDATORY, true) } ?: false
    }
}
