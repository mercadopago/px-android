package com.mercadopago.android.px.internal.mapper

import com.mercadopago.android.px.internal.repository.PaymentMethodRepository
import com.mercadopago.android.px.internal.mappers.PaymentMethodMapper
import com.mercadopago.android.px.mocks.CheckoutResponseStub
import com.mercadopago.android.px.mocks.PaymentMethodStub
import com.mercadopago.android.px.model.internal.CheckoutResponse
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class PaymentMethodMapperTest {

    private lateinit var checkoutResponse: CheckoutResponse

    @Mock
    private lateinit var paymentMethodRepository: PaymentMethodRepository

    @Test(expected = IllegalStateException::class)
    fun whenPaymentMethodNotFound() {
        checkoutResponse = CheckoutResponseStub.ONLY_TICKET_MLA.get()
        val paymentMethodMock = PaymentMethodStub.VISA_CREDIT.get()
        PaymentMethodMapper(paymentMethodRepository).map(Pair(paymentMethodMock.id, paymentMethodMock.paymentTypeId))
    }

    @Test
    fun whenPaymentMethodIdIsNotNull() {
        checkoutResponse = CheckoutResponseStub.FULL.get()
        val paymentMethodMock = PaymentMethodStub.VISA_CREDIT.get()
        `when`(paymentMethodRepository.getPaymentMethodById(paymentMethodMock.id)).thenReturn(paymentMethodMock)
        val actual = PaymentMethodMapper(paymentMethodRepository).map(Pair(paymentMethodMock.id, paymentMethodMock.paymentTypeId))

        assertEquals(actual.id, paymentMethodMock.id)
        assertEquals(actual.paymentTypeId, paymentMethodMock.paymentTypeId)
    }
}