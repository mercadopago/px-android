package com.mercadopago.android.px.internal.repository;

import androidx.annotation.NonNull;

public interface PayerCostSelectionRepository {

    int get(@NonNull final String paymentMethodId, @NonNull final String paymentMethodType);

    void save(@NonNull final String paymentMethodId, @NonNull final String paymentMethodType, final int selectedPayerCost);

    void reset();
}