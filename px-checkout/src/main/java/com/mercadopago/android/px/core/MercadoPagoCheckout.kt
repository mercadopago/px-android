package com.mercadopago.android.px.core

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.mercadopago.android.px.configuration.AdvancedConfiguration
import com.mercadopago.android.px.configuration.PaymentConfiguration
import com.mercadopago.android.px.configuration.TrackingConfiguration
import com.mercadopago.android.px.core.internal.PrefetchService
import com.mercadopago.android.px.core.internal.PrefetchService.Companion.onCheckoutStarted
import com.mercadopago.android.px.internal.datasource.MercadoPagoPaymentConfiguration
import com.mercadopago.android.px.internal.di.Session
import com.mercadopago.android.px.internal.features.checkout.CheckoutActivity
import com.mercadopago.android.px.preferences.CheckoutPreference
import com.mercadopago.android.px.tracking.internal.events.InitEvent

/**
 * Main class of this project. It provides access to most of the checkout experience.
 */
class MercadoPagoCheckout internal constructor(builder: Builder) {
    val publicKey: String
    val advancedConfiguration: AdvancedConfiguration
    val preferenceId: String?
    val paymentConfiguration: PaymentConfiguration
    val checkoutPreference: CheckoutPreference?
    val trackingConfiguration: TrackingConfiguration
    val privateKey: String

    internal var prefetch: PrefetchService? = null

    init {
        publicKey = builder.publicKey
        advancedConfiguration = builder.advancedConfiguration
        preferenceId = builder.preferenceId
        privateKey = builder.privateKey.orEmpty()
        paymentConfiguration = builder.paymentConfiguration
        checkoutPreference = builder.checkoutPreference
        trackingConfiguration = builder.trackingConfiguration
    }

    /**
     * Starts checkout experience. When the flows ends it returns a [PaymentResult] object that will be returned
     * on [Activity.onActivityResult] if success or [com.mercadopago.android.px.model.exceptions.MercadoPagoError]
     *
     *
     * will return on [Activity.onActivityResult]
     *
     * @param context context needed to start checkout.
     * @param requestCode it's the number that identifies the checkout flow request for [ ][Activity.onActivityResult]
     */
    fun startPayment(context: Context, requestCode: Int) {
        startIntent(context, CheckoutActivity.getIntent(context, prefetch != null), requestCode)
    }

    private fun startIntent(context: Context, checkoutIntent: Intent, requestCode: Int) {
        val session = Session.getInstance()
        session.init(this)
        prefetch?.checkoutResponse?.let {
            session.checkoutRepository.lazyConfigure(it)
        }
        onCheckoutStarted()
        with(session.tracker) {
            initializeSessionTime()
            track(InitEvent(session.configurationModule.paymentSettings))
        }
        if (context is Activity) {
            context.startActivityForResult(checkoutIntent, requestCode)
        } else {
            // Since android 9, we are forced to startActivities from an Activity context or use NEW_TASK flag.
            //https://developer.android.com/about/versions/pie/android-9.0-changes-all#fant-required
            checkoutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(checkoutIntent)
        }
    }

    class Builder {
        val publicKey: String
        val preferenceId: String?
        val checkoutPreference: CheckoutPreference?
        internal var advancedConfiguration = AdvancedConfiguration.Builder().build()
        internal var paymentConfiguration = MercadoPagoPaymentConfiguration.create()
        internal var privateKey: String? = null
        internal var trackingConfiguration = TrackingConfiguration.Builder().build()

        /**
         * Checkout builder allow you to create a [MercadoPagoCheckout] {@see  [our developers site](http://developers.mercadopago.com/)}
         *
         * @param publicKey merchant public key / collector public key {@see [credentials](https://www.mercadopago.com/mla/account/credentials)}
         * @param paymentConfiguration the payment configuration for this checkout.
         * @param checkoutPreference the preference that represents the payment information.
         */
        constructor(publicKey: String,
            checkoutPreference: CheckoutPreference,
            paymentConfiguration: PaymentConfiguration) {
            this.publicKey = publicKey
            this.paymentConfiguration = paymentConfiguration
            this.checkoutPreference = checkoutPreference
            preferenceId = null
        }

        /**
         * Checkout builder allow you to create a [MercadoPagoCheckout] {@see  [our developers site](http://developers.mercadopago.com/)}
         *
         * @param publicKey merchant public key / collector public key {@see [credentials](https://www.mercadopago.com/mla/account/credentials)}
         * @param paymentConfiguration the payment configuration for this checkout.
         * @param preferenceId the preference id that represents the payment information.
         */
        constructor(publicKey: String,
            preferenceId: String,
            paymentConfiguration: PaymentConfiguration) {
            this.publicKey = publicKey
            this.paymentConfiguration = paymentConfiguration
            this.preferenceId = preferenceId
            checkoutPreference = null
        }

        /**
         * Checkout builder allow you to create a [MercadoPagoCheckout] For more information check the following
         * links {@see [credentials](https://www.mercadopago.com/mla/account/credentials)} {@see [create
         * preference](https://www.mercadopago.com.ar/developers/es/reference/preferences/_checkout_preferences/post/)}
         *
         * @param publicKey merchant public key / collector public key
         * @param preferenceId the preference id that represents the payment information.
         */
        constructor(publicKey: String, preferenceId: String) {
            this.publicKey = publicKey
            this.preferenceId = preferenceId
            checkoutPreference = null
        }

        /**
         * Private key provides save card capabilities and account money balance.
         *
         * @param privateKey the user private key
         * @return builder to keep operating
         */
        fun setPrivateKey(privateKey: String) = apply {
            this.privateKey = privateKey
        }

        /**
         * It provides support for custom checkout functionality/ configure special behaviour You can enable/disable
         * several functionality.
         *
         * @param advancedConfiguration your configuration.
         * @return builder to keep operating
         */
        fun setAdvancedConfiguration(advancedConfiguration: AdvancedConfiguration) = apply {
            this.advancedConfiguration = advancedConfiguration
        }

        /**
         * It provides additional configurations to modify tracking and session data.
         *
         * @param trackingConfiguration your configuration.
         * @return builder to keep operating
         */
        fun setTrackingConfiguration(trackingConfiguration: TrackingConfiguration) = apply {
            this.trackingConfiguration = trackingConfiguration
        }

        /**
         * @return [MercadoPagoCheckout] instance
         */
        fun build() = MercadoPagoCheckout(this)
    }

    companion object {
        const val PAYMENT_RESULT_CODE = 7
        const val SESSION_EXPIRED_RESULT_CODE = 666
        const val EXTRA_PAYMENT_RESULT = "EXTRA_PAYMENT_RESULT"
        const val EXTRA_ERROR = "EXTRA_ERROR"
    }
}
