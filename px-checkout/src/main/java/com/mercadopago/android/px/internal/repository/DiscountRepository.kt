package com.mercadopago.android.px.internal.repository

import com.mercadopago.android.px.model.DiscountConfigurationModel

internal interface DiscountRepository : LocalRepository<Map<String, DiscountConfigurationModel>> {

    /**
     * Obtains the discount configuration that applies in a particular moment of the flow
     * <p>
     * E.g. If the user did not select any payment method, the general discount is retrieved otherwise you will retrieve
     * the best discount between the general discount or the selected payment method.
     * <p>
     * In the future, with a discount selector feature, the selected discount will be dominant over the best one.
     *
     * @return The current dominant configuration
     */
    fun getCurrentConfiguration(): DiscountConfigurationModel

    /**
     * Obtains the complete discount configuration for a specif custom option.
     *
     * @param customOptionId The [com.mercadopago.android.px.model.CustomSearchItem] ID.
     * @return The discount configuration, returns null if the ID is invalid.
     */
    fun getConfigurationFor(customOptionId: String, paymentMethodType: String): DiscountConfigurationModel
}