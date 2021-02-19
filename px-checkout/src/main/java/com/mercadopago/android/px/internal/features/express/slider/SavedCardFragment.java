package com.mercadopago.android.px.internal.features.express.slider;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.meli.android.carddrawer.model.CardDrawerView;
import com.mercadopago.android.px.R;
import com.mercadopago.android.px.internal.util.TextUtil;
import com.mercadopago.android.px.internal.viewmodel.CardUiConfiguration;
import com.mercadopago.android.px.internal.viewmodel.drawables.SavedCardDrawableFragmentItem;

public class SavedCardFragment extends PaymentMethodFragment<SavedCardDrawableFragmentItem> {

    private CardDrawerView cardView;

    @NonNull
    public static Fragment getInstance(final SavedCardDrawableFragmentItem model) {
        final SavedCardFragment instance = new SavedCardFragment();
        instance.storeModel(model);
        return instance;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable final ViewGroup container,
        @Nullable final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.px_fragment_saved_card, container, false);
    }

    @Override
    protected void setUpCardDrawerView(@NonNull final CardDrawerView cardDrawerView) {
        super.setUpCardDrawerView(cardDrawerView);
        final CardUiConfiguration card = model.card;
        cardView = cardDrawerView;

        cardView.getCard().setName(card.getName());
        cardView.getCard().setExpiration(card.getDate());
        cardView.getCard().setNumber(card.getNumber());
        cardView.show(card);
        cardView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
    }

    @Override
    protected String getAccessibilityContentDescription() {
        final SpannableStringBuilder builder = new SpannableStringBuilder();
        builder
            .append(model.paymentMethodId)
            .append(TextUtil.SPACE)
            .append(model.getIssuerName())
            .append(TextUtil.SPACE)
            .append(model.getDescription())
            .append(TextUtil.SPACE)
            .append(getString(R.string.px_date_divider))
            .append(TextUtil.SPACE)
            .append(model.card.getName());

        return builder.toString();
    }

    @Override
    public void disable() {
        super.disable();
        cardView.setEnabled(false);
    }
}
