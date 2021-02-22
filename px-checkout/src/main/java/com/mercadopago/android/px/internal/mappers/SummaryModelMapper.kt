package com.mercadopago.android.px.internal.mappers

import com.mercadopago.android.px.internal.repository.PaymentMethodTypeSelectionRepository
import com.mercadopago.android.px.internal.view.SummaryView
import com.mercadopago.android.px.internal.viewmodel.SummaryModel
import com.mercadopago.android.px.model.internal.ExpressMetadataInternal

internal class SummaryModelMapper(
    private val paymentMethodTypeSelectionRepository: PaymentMethodTypeSelectionRepository,
    private val summaryViewModelMapList: List<Map<String, SummaryView.Model>>
) : Mapper<ExpressMetadataInternal, SummaryModel>() {

    private var currentIndex = 0

    override fun map(value: ExpressMetadataInternal): SummaryModel {
        return SummaryModel(
            paymentMethodTypeSelectionRepository.get(value.customOptionId),
            summaryViewModelMapList[currentIndex++]
        )
    }
}