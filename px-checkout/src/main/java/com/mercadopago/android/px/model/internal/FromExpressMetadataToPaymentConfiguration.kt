package com.mercadopago.android.px.model.internal

import com.mercadopago.android.px.internal.mappers.Mapper
import com.mercadopago.android.px.internal.repository.AmountConfigurationRepository
import com.mercadopago.android.px.internal.repository.ApplicationSelectionRepository
import com.mercadopago.android.px.internal.repository.PayerCostSelectionRepository
import com.mercadopago.android.px.internal.viewmodel.SplitSelectionState
import com.mercadopago.android.px.model.PayerCost

internal class FromExpressMetadataToPaymentConfiguration(
    private val amountConfigurationRepository: AmountConfigurationRepository,
    private val splitSelectionState: SplitSelectionState,
    private val payerCostSelectionRepository: PayerCostSelectionRepository,
    private val applicationSelectionRepository: ApplicationSelectionRepository
) : Mapper<OneTapItem, PaymentConfiguration>() {

    override fun map(value: OneTapItem): PaymentConfiguration {

        val customOptionId = value.customOptionId
        val (paymentMethodId, paymentTypeId) = applicationSelectionRepository[customOptionId]?.let { application ->
            with(application.paymentMethod) { id to type }
        } ?: value.paymentMethodId to value.paymentTypeId

        var payerCost: PayerCost? = null
        val amountConfiguration = amountConfigurationRepository.getConfigurationFor(customOptionId, paymentTypeId)
        val splitPayment = splitSelectionState.userWantsToSplit() && amountConfiguration!!.allowSplit()

        if (value.isCard || value.isConsumerCredits) {
            payerCost = amountConfiguration!!.getCurrentPayerCost(splitSelectionState.userWantsToSplit(),
                payerCostSelectionRepository.get(customOptionId))
        }

        return PaymentConfiguration(paymentMethodId, paymentTypeId, customOptionId, value.isCard,
            splitPayment, payerCost)
    }
}
