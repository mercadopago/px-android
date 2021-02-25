package com.mercadopago.android.px.securitycode

import com.mercadopago.android.px.internal.features.security_code.data.SecurityCodeDisplayData
import com.mercadopago.android.px.internal.features.security_code.domain.model.BusinessCardDisplayInfo
import com.mercadopago.android.px.internal.features.security_code.domain.model.BusinessSecurityCodeDisplayData
import com.mercadopago.android.px.internal.features.security_code.mapper.BusinessSecurityCodeDisplayDataMapper
import com.mercadopago.android.px.internal.util.JsonUtil
import com.mercadopago.android.px.internal.viewmodel.LazyString
import com.mercadopago.android.px.model.CardDisplayInfo
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.internal.matchers.apachecommons.ReflectionEquals

class BusinessSecurityCodeDisplayDataMapperTest {
    
    @Test
    fun whenMapSecurityCodeDisplayDataToBusinessSecurityCodeDisplayData() {
        val securityCodeDisplayData = mock(SecurityCodeDisplayData::class.java)

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

        `when`(securityCodeDisplayData.title).thenReturn(mock(LazyString::class.java))
        `when`(securityCodeDisplayData.message).thenReturn(mock(LazyString::class.java))
        `when`(securityCodeDisplayData.securityCodeLength).thenReturn(3)
        `when`(securityCodeDisplayData.cardDisplayInfo).thenReturn(cardDisplayInfo)

        val businessCardDisplayInfo = with(cardDisplayInfo!!) {
            BusinessCardDisplayInfo(
                cardholderName,
                expiration,
                color,
                fontColor,
                issuerId,
                cardPattern,
                getCardPattern(),
                securityCode.cardLocation,
                securityCode.length,
                lastFourDigits,
                paymentMethodImage,
                issuerImage,
                fontType,
                paymentMethodImageUrl,
                issuerImageUrl
            )
        }
        
        
        val expectedResult = BusinessSecurityCodeDisplayData(
            securityCodeDisplayData.title,
            securityCodeDisplayData.message,
            securityCodeDisplayData.securityCodeLength,
            businessCardDisplayInfo
        )

        val mapper = BusinessSecurityCodeDisplayDataMapper()
        val actualResult = mapper.map(securityCodeDisplayData)
        
        assertTrue(ReflectionEquals(actualResult).matches(expectedResult))
    }
}
