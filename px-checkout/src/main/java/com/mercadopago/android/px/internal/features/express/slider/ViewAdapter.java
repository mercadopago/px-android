package com.mercadopago.android.px.internal.features.express.slider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
import com.mercadopago.android.px.internal.viewmodel.SplitSelectionState;

public abstract class ViewAdapter<T, V extends View> {

    @Nullable protected T data;
    @NonNull protected V view;

    /* default */ ViewAdapter(@NonNull final V view) {
        this.view = view;
    }

    public void update(@NonNull final T newData) {
        data = newData;
    }

    public abstract void updateData(final int currentIndex, final int payerCostSelected,
        @NonNull final SplitSelectionState splitSelectionState);

    public void updateViewsOrder(@NonNull final View previousView,
        @NonNull final View currentView,
        @NonNull final View nextView) {
        // Do nothing
    }

    public void updateData(@NonNull final String key, final int payerCostSelected,
        @NonNull final SplitSelectionState splitSelectionState) {
        // Do nothing
    }

    public void updatePosition(final float positionOffset, final int position) {
        // Do nothing
    }

    public void showInstallmentsList() {
        // Do nothing
    }
}