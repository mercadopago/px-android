package com.mercadopago.android.px.model.internal

import com.mercadopago.android.px.internal.repository.AmountConfigurationRepository
import com.mercadopago.android.px.internal.repository.PayerCostSelectionRepository
import com.mercadopago.android.px.internal.viewmodel.SplitSelectionState
import com.mercadopago.android.px.internal.mappers.Mapper
import com.mercadopago.android.px.internal.repository.PaymentMethodTypeSelectionRepository
import com.mercadopago.android.px.model.ExpressMetadata
import com.mercadopago.android.px.model.PayerCost

internal class FromExpressMetadataToPaymentConfiguration(
    private val amountConfigurationRepository: AmountConfigurationRepository,
    private val splitSelectionState: SplitSelectionState,
    private val payerCostSelectionRepository: PayerCostSelectionRepository,
    private val paymentMethodTypeSelectionRepository: PaymentMethodTypeSelectionRepository
) : Mapper<ExpressMetadata, PaymentConfiguration>() {

    override fun map(expressMetadata: ExpressMetadata): PaymentConfiguration {
        var payerCost: PayerCost? = null

        val customOptionId = expressMetadata.customOptionId
        val amountConfiguration = amountConfigurationRepository.getConfigurationFor(
            customOptionId,
            paymentMethodTypeSelectionRepository.get(customOptionId))
        val splitPayment = splitSelectionState.userWantsToSplit() && amountConfiguration!!.allowSplit()

        if (expressMetadata.isCard || expressMetadata.isConsumerCredits) {
            payerCost = amountConfiguration!!.getCurrentPayerCost(splitSelectionState.userWantsToSplit(),
                payerCostSelectionRepository.get(customOptionId))
        }

        return PaymentConfiguration(expressMetadata.paymentMethodId, expressMetadata.paymentTypeId, customOptionId, expressMetadata.isCard,
            splitPayment, payerCost)
    }
}
