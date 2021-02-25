package com.mercadopago.android.px.securitycode

import com.mercadopago.android.px.internal.features.security_code.domain.model.BusinessCardDisplayInfo
import com.mercadopago.android.px.internal.mappers.CardUiMapper
import com.mercadopago.android.px.internal.util.JsonUtil
import com.mercadopago.android.px.internal.viewmodel.CardUiConfiguration
import com.mercadopago.android.px.model.CardDisplayInfo
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.internal.matchers.apachecommons.ReflectionEquals

class CardUiMapperTest {

    @Test
    fun whenMapBusinessCardDisplayInfoToCardUiConfiguration() {
        val businessCardDisplayInfo = mock(BusinessCardDisplayInfo::class.java)

        `when`(businessCardDisplayInfo.paymentMethodImage).thenReturn("paymentMethodImage")
        `when`(businessCardDisplayInfo.paymentMethodImageUrl).thenReturn("paymentMethodImageUrl")
        `when`(businessCardDisplayInfo.cardPattern).thenReturn(intArrayOf(1, 2, 3, 4))
        `when`(businessCardDisplayInfo.cardPatternMask).thenReturn("*****")
        `when`(businessCardDisplayInfo.cardholderName).thenReturn("cardholderName")
        `when`(businessCardDisplayInfo.color).thenReturn("color")
        `when`(businessCardDisplayInfo.expiration).thenReturn("expiration")
        `when`(businessCardDisplayInfo.fontColor).thenReturn("fontColor")
        `when`(businessCardDisplayInfo.fontType).thenReturn("fontType")
        `when`(businessCardDisplayInfo.issuerId).thenReturn(1234)
        `when`(businessCardDisplayInfo.issuerImage).thenReturn("issuerImage")
        `when`(businessCardDisplayInfo.issuerImageUrl).thenReturn("issuerImageUrl")
        `when`(businessCardDisplayInfo.lastFourDigits).thenReturn("7890")
        `when`(businessCardDisplayInfo.securityCodeLocation).thenReturn("back")
        `when`(businessCardDisplayInfo.securityCodeLength).thenReturn(3)

        val expectedResult = CardUiConfiguration(
            businessCardDisplayInfo.cardholderName,
            businessCardDisplayInfo.expiration,
            businessCardDisplayInfo.cardPatternMask,
            businessCardDisplayInfo.issuerImageUrl,
            businessCardDisplayInfo.paymentMethodImageUrl,
            businessCardDisplayInfo.fontType,
            businessCardDisplayInfo.cardPattern,
            businessCardDisplayInfo.color,
            businessCardDisplayInfo.fontColor,
            businessCardDisplayInfo.securityCodeLocation,
            businessCardDisplayInfo.securityCodeLength
        )

        val actualResult = CardUiMapper.map(businessCardDisplayInfo)

        assertTrue(ReflectionEquals(actualResult).matches(expectedResult))
    }

    @Test
    fun whenMapCardDisplayInfoToCardUiConfiguration() {
        val cardDisplayInfo = JsonUtil.fromJson("""{
            "payment_method_image": "paymentMethodImage",
            "payment_method_image_url": "paymentMethodImageUrl",
            "card_pattern": [1, 2, 3, 4],
            "cardholder_name": "cardholderName",
            "color": "color",
            "expiration": "expiration",
            "font_color": "fontColor",
            "font_type": "fontType",
            "issuer_id": 1234,
            "issuer_image": "issuerImage",
            "issuer_image_url": "issuerImageUrl",
            "last_four_digits": "7890",
            "security_code": {
                "card_location": "back",
                "length": "3"
            }
        }""".trimIndent(), CardDisplayInfo::class.java)

        val expectedResult = with(cardDisplayInfo!!) {
            CardUiConfiguration(
                cardholderName,
                expiration,
                getCardPattern(),
                issuerImageUrl,
                paymentMethodImageUrl,
                fontType,
                cardPattern,
                color,
                fontColor,
                securityCode.cardLocation,
                securityCode.length,
                null,
                null
            )
        }

        val actualResult = CardUiMapper.map(cardDisplayInfo)

        assertTrue(ReflectionEquals(actualResult).matches(expectedResult))
    }
}
