package com.mercadopago.android.px.model.internal

import com.mercadopago.android.px.internal.repository.AmountConfigurationRepository
import com.mercadopago.android.px.internal.repository.PayerCostSelectionRepository
import com.mercadopago.android.px.internal.viewmodel.SplitSelectionState
import com.mercadopago.android.px.internal.mappers.Mapper
import com.mercadopago.android.px.internal.repository.PaymentMethodTypeSelectionRepository
import com.mercadopago.android.px.model.PayerCost

internal class FromExpressMetadataToPaymentConfiguration(
    private val amountConfigurationRepository: AmountConfigurationRepository,
    private val splitSelectionState: SplitSelectionState,
    private val payerCostSelectionRepository: PayerCostSelectionRepository,
    private val paymentMethodTypeSelectionRepository: PaymentMethodTypeSelectionRepository
) : Mapper<OneTapItem, PaymentConfiguration>() {

    override fun map(oneTapItem: OneTapItem): PaymentConfiguration {
        var payerCost: PayerCost? = null

        val customOptionId = oneTapItem.customOptionId
        val paymentMethodSelected = paymentMethodTypeSelectionRepository.get(customOptionId);
        val amountConfiguration = amountConfigurationRepository.getConfigurationFor(
            customOptionId,
            paymentMethodSelected)
        val splitPayment = splitSelectionState.userWantsToSplit() && amountConfiguration!!.allowSplit()

        if (oneTapItem.isCard || oneTapItem.isConsumerCredits) {
            payerCost = amountConfiguration!!.getCurrentPayerCost(splitSelectionState.userWantsToSplit(),
                payerCostSelectionRepository.get(customOptionId, paymentMethodSelected))
        }

        return PaymentConfiguration(
            oneTapItem.paymentMethodId,
            paymentMethodSelected,
            customOptionId,
            oneTapItem.isCard,
            splitPayment,
            payerCost)
    }
}
