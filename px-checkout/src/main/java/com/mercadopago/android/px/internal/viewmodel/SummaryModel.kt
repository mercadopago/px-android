package com.mercadopago.android.px.internal.viewmodel

import com.mercadopago.android.px.internal.view.SummaryView

internal data class SummaryModel(
    var currentSelection: String?,
    val summaryViewModelMap: Map<String, SummaryView.Model>
) {
    fun getSelectionModel() = currentSelection?.let { summaryViewModelMap[it] } ?: summaryViewModelMap.values.first()
}