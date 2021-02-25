package com.mercadopago.android.px.internal.mappers

import com.mercadopago.android.px.internal.repository.ApplicationSelectionRepository
import com.mercadopago.android.px.internal.util.ChargeRuleHelper
import com.mercadopago.android.px.internal.view.SummaryView
import com.mercadopago.android.px.internal.viewmodel.SummaryModel
import com.mercadopago.android.px.model.DiscountConfigurationModel
import com.mercadopago.android.px.model.commission.PaymentTypeChargeRule
import com.mercadopago.android.px.model.internal.OneTapItem

internal abstract class ApplicationSummaryCachedMapper(
    private val applicationSelectionRepository: ApplicationSelectionRepository
) : Mapper<OneTapItem, SummaryModel>() {

    private val cache = mutableMapOf<Key, SummaryView.Model>()

    override fun map(values: Iterable<OneTapItem>): List<SummaryModel> {
        return mapToSummaryViewModelList(values)
    }

    override fun map(value: OneTapItem): SummaryModel {
        return SummaryModel(value.getDefaultPaymentMethodType(), mapToSummaryViewModel(value))
    }

    private fun mapToSummaryViewModelList(values: Iterable<OneTapItem>) = mutableListOf<SummaryModel>().also {
        values.forEach { value ->
            val currentPmTypeSelection = applicationSelectionRepository.getPaymentMethodTypeId(value.customOptionId)
            it.add(SummaryModel(currentPmTypeSelection, mapToSummaryViewModel(value)))
        }
    }

    private fun mapToSummaryViewModel(value: OneTapItem): Map<String, SummaryView.Model> {
        val map = hashMapOf<String, SummaryView.Model>()
        val customOptionId = value.customOptionId
        val currentPmTypeSelection = applicationSelectionRepository.getPaymentMethodTypeId(customOptionId)
        value.getApplications()?.takeIf { it.isNotEmpty() }?.forEach { application ->
            val paymentMethodTypeId = application.paymentMethod.type
            map[paymentMethodTypeId] = mapWithCache(customOptionId, paymentMethodTypeId)
        } ?: let { map[currentPmTypeSelection] = mapWithCache(customOptionId, currentPmTypeSelection) }

        return map
    }

    private fun mapWithCache(customOptionId: String, paymentMethodTypeId: String): SummaryView.Model {
        val key = getKey(customOptionId, paymentMethodTypeId)
        return if (cache.containsKey(key)) {
            cache[key]!!
        } else {
            map(customOptionId, paymentMethodTypeId).also { cache[key] = it }
        }
    }

    protected abstract fun map(customOptionId: String, paymentMethodTypeId: String): SummaryView.Model

    abstract fun getKey(customOptionId: String, paymentMethodTypeId: String): Key

    internal class Key(
        discountConfigurationModel: DiscountConfigurationModel,
        paymentTypeChargeRule: PaymentTypeChargeRule?,
        hasSplit: Boolean) {
        private val discountConfigurationModel: DiscountConfigurationModel?
        private val paymentTypeChargeRule: PaymentTypeChargeRule?
        private val hasSplit: Boolean?
        override fun hashCode(): Int {
            return (discountConfigurationModel?.hashCode() ?: 0) xor
                (paymentTypeChargeRule?.hashCode() ?: 0) xor
                (hasSplit?.hashCode() ?: 0)
        }

        override fun equals(other: Any?): Boolean {
            if (other !is Key) {
                return false
            }
            return (other.discountConfigurationModel == discountConfigurationModel
                && other.paymentTypeChargeRule == paymentTypeChargeRule
                && other.hasSplit == hasSplit)
        }

        init {
            this.discountConfigurationModel = discountConfigurationModel
            this.paymentTypeChargeRule = if (ChargeRuleHelper.isHighlightCharge(paymentTypeChargeRule))
                null else paymentTypeChargeRule
            this.hasSplit = hasSplit
        }
    }
}