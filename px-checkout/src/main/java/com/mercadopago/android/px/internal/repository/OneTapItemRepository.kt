package com.mercadopago.android.px.internal.repository

import com.mercadopago.android.px.model.internal.OneTapItem

internal interface OneTapItemRepository : LocalRepository<List<@JvmSuppressWildcards OneTapItem>> {
    fun sortByState()
}