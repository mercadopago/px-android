package com.mercadopago.android.px.internal.di

import android.content.Context
import com.mercadopago.android.px.internal.datasource.*
import com.mercadopago.android.px.internal.repository.*

internal class CheckoutConfigurationModule(context: Context) : ConfigurationModule(context) {

    val userSelectionRepository: UserSelectionRepository by lazy { UserSelectionService(sharedPreferences, fileManager) }
    val paymentSettings: PaymentSettingRepository by lazy { PaymentSettingService(sharedPreferences, fileManager) }
    val disabledPaymentMethodRepository: DisabledPaymentMethodRepository by lazy {
        DisabledPaymentMethodService(sharedPreferences)
    }
    val payerCostSelectionRepository: PayerCostSelectionRepository by lazy {
        PayerCostSelectionRepositoryImpl(sharedPreferences)
    }
    val payerComplianceRepository: PayerComplianceRepository by lazy { PayerComplianceRepositoryImpl(sharedPreferences, fileManager) }
    private var internalChargeRepository: ChargeRepository? = null
    val chargeRepository: ChargeRepository
        get() {
            if (internalChargeRepository == null) {
                internalChargeRepository = ChargeService(paymentSettings)
            }
            return internalChargeRepository!!
        }

    private var internalCustomTextsRepository: CustomTextsRepository? = null
    val customTextsRepository: CustomTextsRepository
        get() {
            if (internalCustomTextsRepository == null) {
                internalCustomTextsRepository = CustomTextsRepositoryImpl(paymentSettings)
            }
            return internalCustomTextsRepository!!
        }

    private var internalApplicationSelectionRepository: ApplicationSelectionRepository? = null
    val applicationSelectionRepository: ApplicationSelectionRepository
        get() {
            return internalApplicationSelectionRepository ?: ApplicationSelectionRepositoryImpl(
                fileManager, Session.getInstance().oneTapItemRepository).also {
                internalApplicationSelectionRepository = it
            }
        }

    override fun reset() {
        super.reset()
        userSelectionRepository.reset()
        paymentSettings.reset()
        disabledPaymentMethodRepository.reset()
        payerCostSelectionRepository.reset()
        payerComplianceRepository.reset()
        applicationSelectionRepository.reset()
        internalChargeRepository = null
        internalCustomTextsRepository = null
        internalApplicationSelectionRepository = null;
    }
}