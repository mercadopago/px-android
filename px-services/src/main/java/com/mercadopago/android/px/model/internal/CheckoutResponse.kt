package com.mercadopago.android.px.model.internal

import com.google.gson.annotations.SerializedName
import com.mercadopago.android.px.addons.model.internal.Experiment
import com.mercadopago.android.px.model.*
import com.mercadopago.android.px.preferences.CheckoutPreference

data class CheckoutResponse(
    val checkoutPreference: CheckoutPreference? = null,
    val site: Site,
    val currency: Currency,
    val experiments: List<Experiment>? = null,
    val payerCompliance: PayerCompliance? = null,
    @SerializedName("configurations")
    val configuration: Configuration,
    val modals: Map<String, Modal>,
    val oneTap: List<ExpressMetadataInternal>? = null,
    val availablePaymentMethods: List<PaymentMethod>,
    val payerPaymentMethods: List<CustomSearchItem>,
    @SerializedName("general_coupon")
    val defaultAmountConfiguration: String,
    @SerializedName("coupons")
    val discountsConfigurations: Map<String, DiscountConfigurationModel>
)