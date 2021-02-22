package com.mercadopago.android.px.internal.datasource;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.mercadopago.android.px.model.AmountConfiguration;

public interface ConfigurationSolver {

    /**
     * Retrieves the dominant discount hash for a custom option.
     *
     * @param customOptionId    The custom option ID.
     * @param paymentMethodType
     * @return The hash associated to the discount configuration.
     */
    @NonNull
    String getConfigurationHashFor(@NonNull final String customOptionId, @NonNull final String paymentMethodType);

    /**
     * Retrieves the dominant payer cost model for a custom option.
     *
     * @param customOptionId The custom option ID.
     * @return The payer cost model associated to the custom option ID.
     */
    @Nullable
    AmountConfiguration getAmountConfigurationFor(@NonNull final String customOptionId,
        @NonNull final String paymentMethodType);
}
