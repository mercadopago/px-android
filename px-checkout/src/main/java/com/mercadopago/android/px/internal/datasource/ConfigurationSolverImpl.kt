package com.mercadopago.android.px.internal.datasource

import com.mercadopago.android.px.internal.repository.PayerPaymentMethodRepository
import com.mercadopago.android.px.internal.util.TextUtil
import com.mercadopago.android.px.model.AmountConfiguration
import com.mercadopago.android.px.model.CustomSearchItem

internal class ConfigurationSolverImpl(
    private val payerPaymentMethodRepository: PayerPaymentMethodRepository) : ConfigurationSolver {

    override fun getAmountConfigurationFor(customOptionId: String, paymentMethodType: String): AmountConfiguration? {
        return payerPaymentMethodRepository.value.firstOrNull { it.checkData(customOptionId, paymentMethodType) }
            ?.let { it.getAmountConfiguration(it.defaultAmountConfiguration) }
    }

    override fun getConfigurationHashFor(customOptionId: String, paymentMethodType: String): String {
        return payerPaymentMethodRepository.value.firstOrNull { it.checkData(customOptionId, paymentMethodType) }
            ?.defaultAmountConfiguration ?: TextUtil.EMPTY
    }

    private fun CustomSearchItem.checkData(customOptionId: String, paymentMethodType: String): Boolean {
        return id.equals(customOptionId, ignoreCase = true) && type.equals(paymentMethodType, ignoreCase = true)
    }
}