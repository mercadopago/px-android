package com.mercadopago.android.px.internal.features.express.slider;

import androidx.annotation.NonNull;
import android.view.View;
import com.mercadopago.android.px.internal.view.PaymentMethodDescriptorView;
import com.mercadopago.android.px.internal.view.TitlePager;
import com.mercadopago.android.px.internal.viewmodel.GoingToModel;
import com.mercadopago.android.px.internal.viewmodel.PaymentMethodDescriptorModel;
import com.mercadopago.android.px.internal.viewmodel.SplitSelectionState;
import java.util.List;

import static com.mercadopago.android.px.internal.util.AccessibilityUtilsKt.executeIfAccessibilityTalkBackEnable;

public class TitlePagerAdapter extends HubableAdapter<List<PaymentMethodDescriptorModel>, TitlePager> {

    private static final int NO_SELECTED = -1;

    private PaymentMethodDescriptorView previousView;
    private PaymentMethodDescriptorView currentView;
    private PaymentMethodDescriptorView nextView;
    private int currentIndex = NO_SELECTED;
    private final InstallmentChanged installmentChanged;

    public interface InstallmentChanged {
        void installmentSelectedChanged(final int installment);
    }

    public TitlePagerAdapter(@NonNull final TitlePager titlePager, @NonNull final InstallmentChanged installmentChanged) {
        super(titlePager);
        this.installmentChanged = installmentChanged;
    }

    @Override
    public void updateData(final int currentIndex, final int payerCostSelected,
        @NonNull final SplitSelectionState splitSelectionState) {
        if (this.currentIndex != currentIndex) {
            final GoingToModel goingTo =
                this.currentIndex < currentIndex ? GoingToModel.BACKWARDS : GoingToModel.FORWARD;
            view.orderViews(goingTo);
            this.currentIndex = currentIndex;
        }
        refreshData(currentIndex, payerCostSelected, splitSelectionState);
    }

    @Override
    public void updateData(@NonNull final String key, final int payerCostSelected,
        @NonNull final SplitSelectionState splitSelectionState) {
        data.get(currentIndex).setCurrentSelection(key);

        final PaymentMethodDescriptorView.Model currentModel = data.get(currentIndex).getSelectedModel();

        currentModel.setCurrentPayerCost(payerCostSelected);
        currentModel.setSplit(splitSelectionState.userWantsToSplit());
        currentView.update(currentModel);
        executeIfAccessibilityTalkBackEnable(currentView.getContext(), () -> {
            currentView.updateContentDescription(currentModel);
            return null;
        });

        if (installmentChanged != null) {
            installmentChanged.installmentSelectedChanged(currentModel.getCurrentInstalment());
        }
    }

    @Override
    public void updatePosition(final float positionOffset, final int position) {
        final GoingToModel goingTo = position < currentIndex ? GoingToModel.BACKWARDS : GoingToModel.FORWARD;
        view.updatePosition(positionOffset, goingTo);
    }

    @Override
    public void updateViewsOrder(@NonNull final View previousView,
        @NonNull final View currentView,
        @NonNull final View nextView) {
        this.previousView = (PaymentMethodDescriptorView) previousView;
        this.currentView = (PaymentMethodDescriptorView) currentView;
        this.nextView = (PaymentMethodDescriptorView) nextView;
    }

    private void refreshData(final int currentIndex, final int payerCostSelected,
        @NonNull final SplitSelectionState splitSelectionState) {
        if (currentIndex > 0) {
            final PaymentMethodDescriptorView.Model previousModel = data.get(currentIndex - 1).getSelectedModel();
            previousView.update(previousModel);
        }

        final PaymentMethodDescriptorView.Model currentModel = data.get(currentIndex).getSelectedModel();

        currentModel.setCurrentPayerCost(payerCostSelected);
        currentModel.setSplit(splitSelectionState.userWantsToSplit());
        currentView.update(currentModel);
        executeIfAccessibilityTalkBackEnable(currentView.getContext(), () -> {
            currentView.updateContentDescription(currentModel);
            return null;
        });

        if (installmentChanged != null) {
            installmentChanged.installmentSelectedChanged(currentModel.getCurrentInstalment());
        }

        if (currentIndex + 1 < data.size()) {
            final PaymentMethodDescriptorView.Model nextModel = data.get(currentIndex + 1).getSelectedModel();
            nextView.update(nextModel);
        }
    }

    @Override
    public List<PaymentMethodDescriptorModel> getNewModels(final HubAdapter.Model model) {
        return model.paymentMethodDescriptorModels;
    }
}