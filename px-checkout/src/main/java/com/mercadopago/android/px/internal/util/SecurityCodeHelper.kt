package com.mercadopago.android.px.internal.util

import com.mercadopago.android.px.model.PaymentMethod
import com.mercadopago.android.px.model.SecurityCode
import com.mercadopago.android.px.model.Setting

internal object SecurityCodeHelper {
    fun get(paymentMethod: PaymentMethod?, bin: String?): SecurityCode? {
        return Setting.getSettingByBin(paymentMethod?.settings, bin)?.securityCode
    }
}
