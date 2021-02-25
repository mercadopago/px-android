package com.mercadopago.android.px.internal.mappers

import com.mercadopago.android.px.internal.repository.*
import com.mercadopago.android.px.internal.view.AmountDescriptorView
import com.mercadopago.android.px.internal.view.ElementDescriptorView
import com.mercadopago.android.px.internal.view.SummaryDetailDescriptorFactory
import com.mercadopago.android.px.internal.view.SummaryView
import com.mercadopago.android.px.internal.viewmodel.AmountLocalized
import com.mercadopago.android.px.internal.viewmodel.SummaryViewDefaultColor
import com.mercadopago.android.px.internal.viewmodel.TotalLocalized
import com.mercadopago.android.px.model.AmountConfiguration
import com.mercadopago.android.px.model.Currency
import com.mercadopago.android.px.model.DiscountConfigurationModel
import com.mercadopago.android.px.model.internal.SummaryInfo

internal class ApplicationSummaryMapper(
    private val currency: Currency,
    private val discountRepository: DiscountRepository,
    private val amountRepository: AmountRepository,
    private val elementDescriptorViewModel: ElementDescriptorView.Model,
    private val onClickListener: AmountDescriptorView.OnClickListener,
    private val summaryInfo: SummaryInfo,
    private val chargeRepository: ChargeRepository,
    private val amountConfigurationRepository: AmountConfigurationRepository,
    private val customTextsRepository: CustomTextsRepository,
    private val amountDescriptorMapper: AmountDescriptorMapper,
    applicationSelectionRepository: ApplicationSelectionRepository
): ApplicationSummaryCachedMapper(applicationSelectionRepository) {

    override fun map(customOptionId: String, paymentMethodTypeId: String): SummaryView.Model {
        return createModel(paymentMethodTypeId,
            getDiscountConfiguration(customOptionId, paymentMethodTypeId),
            getAmountConfiguration(customOptionId, paymentMethodTypeId))
    }

    private fun createModel(
        paymentTypeId: String,
        discountModel: DiscountConfigurationModel,
        amountConfiguration: AmountConfiguration?): SummaryView.Model {
        val chargeRule = chargeRepository.getChargeRule(paymentTypeId)
        val summaryDetailList = SummaryDetailDescriptorFactory(
            onClickListener,
            discountModel,
            amountRepository,
            summaryInfo,
            currency,
            chargeRule,
            amountConfiguration,
            amountDescriptorMapper).create()
        val totalRow = AmountDescriptorView.Model(
            TotalLocalized(customTextsRepository),
            AmountLocalized(amountRepository.getAmountToPay(paymentTypeId, discountModel), currency),
            SummaryViewDefaultColor())
        return SummaryView.Model(elementDescriptorViewModel, summaryDetailList, totalRow)
    }

    override fun getKey(customOptionId: String, paymentMethodTypeId: String): Key {
        val chargeRule = chargeRepository.getChargeRule(paymentMethodTypeId)
        val amountConfiguration: AmountConfiguration? = getAmountConfiguration(customOptionId, paymentMethodTypeId)
        val hasSplit = amountConfiguration != null && amountConfiguration.allowSplit()

        return Key(getDiscountConfiguration(customOptionId, paymentMethodTypeId), chargeRule, hasSplit)
    }

    private fun getDiscountConfiguration(customOptionId: String, paymentMethodId: String): DiscountConfigurationModel {
        return discountRepository.getConfigurationFor(customOptionId, paymentMethodId)
    }

    private fun getAmountConfiguration(customOptionId: String, paymentMethodId: String): AmountConfiguration? {
        return amountConfigurationRepository.getConfigurationFor(customOptionId, paymentMethodId)
    }
}