package com.mercadopago.android.px.internal.repository

import com.mercadopago.android.px.model.AmountConfiguration

internal interface AmountConfigurationRepository : LocalRepository<String> {
    /**
     * Obtains the payer costs configuration that applies in a particular moment of the flow
     * <p>
     * In the future, with a discount selector feature, the selected discount will define the associated payer cost.
     * <p>
     * @return The current dominant configuration.
     */
    @Throws(IllegalStateException::class)
    fun getCurrentConfiguration(): AmountConfiguration

    /**
     * Obtains the complete payer cost configuration for a specif custom option.
     *
     * @param customOptionId The [com.mercadopago.android.px.model.CustomSearchItem] ID.
     * @return The payer cost configuration, returns null if don't have a configuration or ID is invalid.
     */
    fun getConfigurationFor(customOptionId: String, paymentMethodType: String): AmountConfiguration?

}