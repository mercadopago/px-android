package com.mercadopago.android.px.internal.datasource

import com.mercadopago.android.px.internal.core.FileManager
import com.mercadopago.android.px.internal.repository.AmountConfigurationRepository
import com.mercadopago.android.px.internal.util.TextUtil
import com.mercadopago.android.px.model.Campaign
import com.mercadopago.android.px.model.Card
import com.mercadopago.android.px.model.DiscountConfigurationModel
import com.mercadopago.android.px.model.PaymentMethod
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class DiscountServiceTest {

    @Mock
    private lateinit var fileManager: FileManager

    @Mock
    private lateinit var userSelectionService: UserSelectionService

    @Mock
    private lateinit var amountConfigurationRepository: AmountConfigurationRepository

    @Mock
    private lateinit var configurationSolver: ConfigurationSolver

    @Mock
    private lateinit var expectedDiscountModel: DiscountConfigurationModel

    @Mock
    private lateinit var campaign: Campaign

    private val validCampaignId = "VALID_CAMPAIGN_ID"
    private val invalidCustomOptionId = "INVALID_CUSTOM_OPTION_ID"
    private val validCustomOptionId = "VALID_CUSTOM_OPTION_ID"

    private val discountServiceImpl: DiscountServiceImpl by lazy {
        DiscountServiceImpl(
            fileManager,
            userSelectionService,
            amountConfigurationRepository,
            configurationSolver
        )
    }

    @Before
    fun setUp() {
        `when`(expectedDiscountModel.campaign).thenReturn(campaign)
        `when`(campaign.id).thenReturn(validCampaignId)
        val discounts = HashMap<String, DiscountConfigurationModel>().also {
            it[expectedDiscountModel.campaign.id] = expectedDiscountModel
        }
        discountServiceImpl.configure(discounts)
    }

    @Test
    fun testGetConfigurationFor_whenCustomOptionIdIsNotValidAndHasNoDefault_thenReturnDiscountModelNone() {
        `when`(configurationSolver.getConfigurationHashFor(invalidCustomOptionId, )).thenReturn(TextUtil.EMPTY)
        val discountConfigurationModel = discountServiceImpl.getConfigurationFor(invalidCustomOptionId, )
        Assert.assertEquals(DiscountConfigurationModel.NONE, discountConfigurationModel)
    }

    @Test
    fun testGetConfigurationFor_whenCustomOptionIdIsNotValidAndHasDefault_thenReturnDefaultDiscountModel() {
        `when`(configurationSolver.getConfigurationHashFor(invalidCustomOptionId, )).thenReturn(TextUtil.EMPTY)
        `when`(amountConfigurationRepository.value).thenReturn(validCampaignId)
        val actualDiscountModel = discountServiceImpl.getConfigurationFor(invalidCustomOptionId, )
        Assert.assertEquals(this.expectedDiscountModel, actualDiscountModel)
    }

    @Test
    fun testGetConfigurationFor_whenCustomOptionIdIsValid_thenReturnCustomOptionDiscountModel() {
        `when`(configurationSolver.getConfigurationHashFor(validCustomOptionId, )).thenReturn(validCampaignId)
        val actualDiscountModel = discountServiceImpl.getConfigurationFor(validCustomOptionId, )
        Assert.assertEquals(this.expectedDiscountModel, actualDiscountModel)
    }

    @Test
    fun testGetCurrentConfiguration_whenMethodSelectedIsCard_thenReturnCardDiscountModel() {
        val card = mock(Card::class.java)
        `when`(configurationSolver.getConfigurationHashFor(validCustomOptionId, )).thenReturn(validCampaignId)
        `when`(card.id).thenReturn(validCustomOptionId)
        `when`(userSelectionService.card).thenReturn(card)
        val actualDiscountModel = discountServiceImpl.getCurrentConfiguration()
        Assert.assertEquals(this.expectedDiscountModel, actualDiscountModel)
    }

    @Test
    fun testGetCurrentConfiguration_whenMethodSelectedIsNotCard_thenReturnPaymentMethodDiscountModel() {
        val paymentMethod = mock(PaymentMethod::class.java)
        `when`(configurationSolver.getConfigurationHashFor(validCustomOptionId, )).thenReturn(validCampaignId)
        `when`(paymentMethod.id).thenReturn(validCustomOptionId)
        `when`(userSelectionService.paymentMethod).thenReturn(paymentMethod)
        val actualDiscountModel = discountServiceImpl.getCurrentConfiguration()
        Assert.assertEquals(this.expectedDiscountModel, actualDiscountModel)
    }

    @Test
    fun testGetCurrentConfiguration_whenMethodIsNotSelected_thenReturnDefaultDiscountModel() {
        `when`(amountConfigurationRepository.value).thenReturn(validCampaignId)
        val actualDiscountModel = discountServiceImpl.getCurrentConfiguration()
        Assert.assertEquals(this.expectedDiscountModel, actualDiscountModel)
    }
}