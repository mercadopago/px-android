package com.mercadopago.android.px.internal.features.express.slider;

import androidx.annotation.NonNull;
import com.mercadopago.android.px.internal.view.SummaryView;
import com.mercadopago.android.px.internal.viewmodel.GoingToModel;
import com.mercadopago.android.px.internal.viewmodel.SplitSelectionState;
import com.mercadopago.android.px.internal.viewmodel.SummaryModel;
import java.util.List;

public class SummaryViewAdapter extends HubableAdapter<List<SummaryModel>, SummaryView> {

    private static final int NO_SELECTED = -1;

    private int currentIndex = NO_SELECTED;
    private SummaryView.Model currentModel;

    public SummaryViewAdapter(@NonNull final SummaryView view) {
        super(view);
    }

    @Override
    public void updateData(
        final int index,
        final int payerCostSelected,
        @NonNull final SplitSelectionState splitSelectionState) {
        final SummaryView.Model nextModel = data.get(index).getSelectedModel();
        if (!nextModel.equals(currentModel)) {
            view.update(nextModel);
        }

        currentIndex = index;
        currentModel = nextModel;
    }

    @Override
    public void updateData(@NonNull final String key,
        final int payerCostSelected,
        @NonNull final SplitSelectionState splitSelectionState) {
        data.get(currentIndex).setCurrentSelection(key);
        final SummaryView.Model nextModel = data.get(currentIndex).getSelectedModel();

        if (!nextModel.equals(currentModel)) {
            view.update(nextModel);
        }

        currentModel = nextModel;
    }

    @Override
    public void updatePosition(float positionOffset, final int position) {
        if (positionOffset <= 0.0f || positionOffset > 1.0f) {
            return;
        }
        final GoingToModel goingTo = position < currentIndex ? GoingToModel.BACKWARDS : GoingToModel.FORWARD;
        final int nextIndex = goingTo == GoingToModel.BACKWARDS ? currentIndex - 1 : currentIndex + 1;
        //We need to check the index first because sometimes the pager says it's going beyond the available pages.
        if (nextIndex < 0 || nextIndex >= data.size()) {
            return;
        }
        //We only animate if the models are different
        if (!currentModel.equals(data.get(nextIndex).getSelectedModel())) {
            if (goingTo == GoingToModel.BACKWARDS) {
                positionOffset = 1.0f - positionOffset;
            }
            view.animateElementList(positionOffset);
        }
    }

    @Override
    public void update(@NonNull final List<SummaryModel> newData) {
        super.update(newData);
        view.setMaxElementsToShow(getMaxItemsInSummaryAvailable());
    }

    private int getMaxItemsInSummaryAvailable() {
        int maxItems = 0;
        for (final SummaryModel summaryModel : data) {
            for (final SummaryView.Model model : summaryModel.getSummaryViewModelMap().values()) {
                maxItems = Math.max(maxItems, model.getElementsSize());
            }
        }
        return maxItems;
    }

    @Override
    public List<SummaryModel> getNewModels(final HubAdapter.Model model) {
        return model.summaryViewModels;
    }
}