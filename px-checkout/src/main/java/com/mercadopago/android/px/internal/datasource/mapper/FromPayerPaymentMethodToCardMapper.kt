package com.mercadopago.android.px.internal.datasource.mapper

import com.mercadopago.android.px.internal.mappers.NonNullMapper
import com.mercadopago.android.px.internal.repository.PayerPaymentMethodRepository
import com.mercadopago.android.px.internal.repository.PaymentMethodRepository
import com.mercadopago.android.px.model.Card
import com.mercadopago.android.px.model.PaymentTypes.isCardPaymentType

internal class FromPayerPaymentMethodToCardMapper(
    private val payerPaymentMethodRepository: PayerPaymentMethodRepository,
    private val paymentMethodRepository: PaymentMethodRepository
) : NonNullMapper<PayerPaymentMethodRepository.Key, Card>() {

    override fun map(value: PayerPaymentMethodRepository.Key): Card? {
        return payerPaymentMethodRepository[value]?.takeIf {
            isCardPaymentType(it.type) }?.let {
            payerPaymentMethod ->
            paymentMethodRepository.value.find { it.id == payerPaymentMethod.paymentMethodId }?.let { paymentMethod ->
                val card = Card()
                card.id = payerPaymentMethod.id
                card.securityCode = paymentMethod.securityCode
                card.paymentMethod = paymentMethod
                card.firstSixDigits = payerPaymentMethod.firstSixDigits
                card.lastFourDigits = payerPaymentMethod.lastFourDigits
                card.issuer = payerPaymentMethod.issuer
                card.escStatus = payerPaymentMethod.escStatus
                return card
            }
        }
    }
}