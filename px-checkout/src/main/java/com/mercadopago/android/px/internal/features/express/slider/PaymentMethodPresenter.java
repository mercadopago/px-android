package com.mercadopago.android.px.internal.features.express.slider;

import androidx.annotation.NonNull;
import com.mercadopago.android.px.internal.base.BasePresenter;
import com.mercadopago.android.px.internal.repository.AmountConfigurationRepository;
import com.mercadopago.android.px.internal.repository.PayerCostSelectionRepository;
import com.mercadopago.android.px.internal.viewmodel.drawables.DrawableFragmentItem;
import com.mercadopago.android.px.model.AmountConfiguration;
import com.mercadopago.android.px.tracking.internal.MPTracker;
import org.jetbrains.annotations.Nullable;

class PaymentMethodPresenter extends BasePresenter<PaymentMethod.View> implements PaymentMethod.Action {
    private final PayerCostSelectionRepository payerCostSelectionRepository;
    private final AmountConfigurationRepository amountConfigurationRepository;
    private final DrawableFragmentItem item;

    /* default */ PaymentMethodPresenter(@NonNull final PayerCostSelectionRepository payerCostSelectionRepository,
        @NonNull final AmountConfigurationRepository amountConfigurationRepository,
        @NonNull final DrawableFragmentItem item, @NonNull final MPTracker tracker) {
        super(tracker);
        this.payerCostSelectionRepository = payerCostSelectionRepository;
        this.amountConfigurationRepository = amountConfigurationRepository;
        this.item = item;
    }

    @Nullable
    private String getHighlightText() {
        final int payerCostIndex = payerCostSelectionRepository.get(item.getId(), item.getType());
        final AmountConfiguration configuration = amountConfigurationRepository.getConfigurationFor(item.getId(), item.getType());
        final int installments = configuration == null || configuration.getPayerCosts().isEmpty() ?
            -1 : configuration.getCurrentPayerCost(false, payerCostIndex).getInstallments();
        final boolean hasReimbursement =
            item.getReimbursement() != null && item.getReimbursement().hasAppliedInstallment(installments);
        final String reimbursementMessage = hasReimbursement ? item.getReimbursement().getCard().getMessage() : null;
        return item.getChargeMessage() != null ? item.getChargeMessage() : reimbursementMessage;
    }

    @Override
    public void onFocusIn() {
        if (item.shouldHighlightBottomDescription()) {
            getView().updateHighlightText(getHighlightText());
            getView().animateHighlightMessageIn();
        }
    }

    @Override
    public void onFocusOut() {
        if (item.shouldHighlightBottomDescription()) {
            getView().animateHighlightMessageOut();
        }
    }
}