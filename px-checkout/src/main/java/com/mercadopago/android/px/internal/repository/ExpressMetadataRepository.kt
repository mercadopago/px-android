package com.mercadopago.android.px.internal.repository

import com.mercadopago.android.px.model.internal.ExpressMetadataInternal

internal interface ExpressMetadataRepository : LocalRepository<List<@JvmSuppressWildcards ExpressMetadataInternal>> {
    fun sortByState()
}