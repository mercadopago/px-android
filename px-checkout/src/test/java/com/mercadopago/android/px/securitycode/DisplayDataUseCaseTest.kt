package com.mercadopago.android.px.securitycode

import com.mercadopago.android.px.CallbackTest
import com.mercadopago.android.px.TestContextProvider
import com.mercadopago.android.px.any
import com.mercadopago.android.px.argumentCaptor
import com.mercadopago.android.px.internal.features.security_code.data.SecurityCodeDisplayData
import com.mercadopago.android.px.internal.features.security_code.domain.model.BusinessSecurityCodeDisplayData
import com.mercadopago.android.px.internal.features.security_code.domain.use_case.DisplayDataUseCase
import com.mercadopago.android.px.internal.features.security_code.mapper.BusinessSecurityCodeDisplayDataMapper
import com.mercadopago.android.px.internal.repository.OneTapItemRepository
import com.mercadopago.android.px.internal.util.JsonUtil
import com.mercadopago.android.px.internal.viewmodel.LazyString
import com.mercadopago.android.px.model.CvvInfo
import com.mercadopago.android.px.model.exceptions.MercadoPagoError
import com.mercadopago.android.px.model.internal.CheckoutResponse
import com.mercadopago.android.px.tracking.internal.MPTracker
import com.mercadopago.android.px.utils.ResourcesUtil
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.internal.matchers.apachecommons.ReflectionEquals
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
internal class DisplayDataUseCaseTest {

    @Mock
    private lateinit var success: CallbackTest<BusinessSecurityCodeDisplayData>

    @Mock
    private lateinit var failure: CallbackTest<MercadoPagoError>

    @Mock
    private lateinit var oneTapItemRepository: OneTapItemRepository

    private lateinit var displayDataUseCase: DisplayDataUseCase
    private val securityCodeDisplayDataMapper = BusinessSecurityCodeDisplayDataMapper()

    @Before
    fun setUp() {
        val contextProvider = TestContextProvider()

        displayDataUseCase = DisplayDataUseCase(
            securityCodeDisplayDataMapper,
            mock(MPTracker::class.java),
            oneTapItemRepository,
            contextProvider
        )
    }

    @Test
    fun whenIsVirtualCard() {
        val cardParams = mock(DisplayDataUseCase.CardParams::class.java)
        val cvvInfo = mock(CvvInfo::class.java)
        val resultBusinessCaptor = argumentCaptor<BusinessSecurityCodeDisplayData>()
        `when`(cvvInfo.title).thenReturn("title")
        `when`(cvvInfo.message).thenReturn("message")
        `when`(cardParams.cvvInfo).thenReturn(cvvInfo)
        `when`(cardParams.securityCodeLength).thenReturn(3)
        val expectedResult = BusinessSecurityCodeDisplayData(
            LazyString(cvvInfo.title),
            LazyString(cvvInfo.message),
            cardParams.securityCodeLength!!,
            null
        )

        displayDataUseCase.execute(
            cardParams,
            success::invoke,
            failure::invoke
        )

        verify(success).invoke(resultBusinessCaptor.capture())
        verifyZeroInteractions(failure)
        with(resultBusinessCaptor.value) {
            assertTrue(ReflectionEquals(title).matches(expectedResult.title))
            assertTrue(ReflectionEquals(message).matches(expectedResult.message))
            assertTrue(ReflectionEquals(securityCodeLength).matches(expectedResult.securityCodeLength))
            assertTrue(ReflectionEquals(cardDisplayInfo).matches(expectedResult.cardDisplayInfo))
        }
    }

    @Test
    fun whenIsCardWithOneTap() = runBlocking {
        val cardParams = mock(DisplayDataUseCase.CardParams::class.java)
        val resultBusinessCaptor = argumentCaptor<BusinessSecurityCodeDisplayData>()
        val cardId = "268434496"
        val checkoutResponse = loadInitResponseWithOneTap()
        val displayInfo = checkoutResponse.oneTapItems?.find { it.isCard && it.card.id == cardId }?.card?.displayInfo
        `when`(cardParams.id).thenReturn(cardId)
        `when`(cardParams.securityCodeLength).thenReturn(3)
        `when`(cardParams.securityCodeLocation).thenReturn("back")
        `when`(oneTapItemRepository.value).thenReturn(checkoutResponse.oneTapItems)
        val expectedResult = SecurityCodeDisplayData(
            LazyString(0),
            LazyString(0, cardParams.securityCodeLength.toString()),
            cardParams.securityCodeLength!!,
            displayInfo).let {
            BusinessSecurityCodeDisplayDataMapper().map(it)
        }

        displayDataUseCase.execute(
            cardParams,
            success::invoke,
            failure::invoke
        )

        verify(success).invoke(resultBusinessCaptor.capture())
        verifyZeroInteractions(failure)
        with(resultBusinessCaptor.value) {
            assertTrue(ReflectionEquals(title, "resId").matches(expectedResult.title))
            assertTrue(ReflectionEquals(message, "resId").matches(expectedResult.message))
            assertTrue(ReflectionEquals(securityCodeLength).matches(expectedResult.securityCodeLength))
            assertTrue(ReflectionEquals(cardDisplayInfo).matches(expectedResult.cardDisplayInfo))
        }
    }

    @Test
    fun whenUseCaseFail() = runBlocking {
        val cardParams = mock(DisplayDataUseCase.CardParams::class.java)

        `when`(cardParams.securityCodeLength).thenReturn(0)

        displayDataUseCase.execute(
            cardParams,
            success::invoke,
            failure::invoke
        )

        verify(failure).invoke(any())
        verifyZeroInteractions(success)
    }

    private fun loadInitResponseWithOneTap() = JsonUtil
        .fromJson(
            ResourcesUtil.getStringResource("init_response_one_tap.json"),
            CheckoutResponse::class.java
        )!!
}
