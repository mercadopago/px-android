package com.mercadopago.android.px.internal.mappers;

import androidx.annotation.NonNull;
import com.mercadopago.android.px.internal.features.express.slider.SplitPaymentHeaderAdapter;
import com.mercadopago.android.px.internal.repository.AmountConfigurationRepository;
import com.mercadopago.android.px.internal.repository.PaymentMethodTypeSelectionRepository;
import com.mercadopago.android.px.model.AmountConfiguration;
import com.mercadopago.android.px.model.Currency;
import com.mercadopago.android.px.model.ExpressMetadata;

public class SplitHeaderMapper extends Mapper<ExpressMetadata, SplitPaymentHeaderAdapter.Model> {

    @NonNull private final Currency currency;
    @NonNull private final AmountConfigurationRepository amountConfigurationRepository;
    @NonNull private final PaymentMethodTypeSelectionRepository paymentMethodTypeSelectionRepository;

    public SplitHeaderMapper(@NonNull final Currency currency,
        @NonNull final AmountConfigurationRepository amountConfigurationRepository,
        @NonNull final PaymentMethodTypeSelectionRepository paymentMethodTypeSelectionRepository) {
        this.currency = currency;
        this.amountConfigurationRepository = amountConfigurationRepository;
        this.paymentMethodTypeSelectionRepository = paymentMethodTypeSelectionRepository;
    }

    @Override
    public SplitPaymentHeaderAdapter.Model map(@NonNull final ExpressMetadata val) {
        if (val.isCard() && val.getStatus().isEnabled()) {
            final String cardId = val.getCard().getId();
            final AmountConfiguration config =
                amountConfigurationRepository.getConfigurationFor(cardId, paymentMethodTypeSelectionRepository.get(cardId));
            return config.allowSplit() ? new SplitPaymentHeaderAdapter.SplitModel(currency,
                config.getSplitConfiguration())
                : new SplitPaymentHeaderAdapter.Empty();
        }
        return new SplitPaymentHeaderAdapter.Empty();
    }
}