package com.mercadopago.android.px.internal.mappers;

import androidx.annotation.NonNull;
import com.mercadopago.android.px.internal.repository.AmountConfigurationRepository;
import com.mercadopago.android.px.internal.repository.AmountRepository;
import com.mercadopago.android.px.internal.repository.DisabledPaymentMethodRepository;
import com.mercadopago.android.px.internal.repository.PaymentMethodTypeSelectionRepository;
import com.mercadopago.android.px.internal.repository.PaymentSettingRepository;
import com.mercadopago.android.px.internal.view.PaymentMethodDescriptorView;
import com.mercadopago.android.px.internal.viewmodel.AccountMoneyDescriptorModel;
import com.mercadopago.android.px.internal.viewmodel.CreditCardDescriptorModel;
import com.mercadopago.android.px.internal.viewmodel.DebitCardDescriptorModel;
import com.mercadopago.android.px.internal.viewmodel.DisabledPaymentMethodDescriptorModel;
import com.mercadopago.android.px.internal.viewmodel.EmptyInstallmentsDescriptorModel;
import com.mercadopago.android.px.model.Currency;
import com.mercadopago.android.px.model.InterestFree;
import com.mercadopago.android.px.model.PayerCost;
import com.mercadopago.android.px.model.PaymentTypes;
import com.mercadopago.android.px.model.internal.OneTapItem;
import com.mercadopago.android.px.model.internal.Text;

public class PaymentMethodDescriptorMapper extends Mapper<OneTapItem, PaymentMethodDescriptorView.Model> {

    @NonNull private final PaymentSettingRepository paymentSettings;
    @NonNull private final AmountConfigurationRepository amountConfigurationRepository;
    @NonNull private final DisabledPaymentMethodRepository disabledPaymentMethodRepository;
    @NonNull private final PaymentMethodTypeSelectionRepository paymentMethodTypeSelectionRepository;
    @NonNull private final AmountRepository amountRepository;

    public PaymentMethodDescriptorMapper(@NonNull final PaymentSettingRepository paymentSettings,
        @NonNull final AmountConfigurationRepository amountConfigurationRepository,
        @NonNull final DisabledPaymentMethodRepository disabledPaymentMethodRepository,
        @NonNull final PaymentMethodTypeSelectionRepository paymentMethodTypeSelectionRepository,
        @NonNull final AmountRepository amountRepository) {
        this.paymentSettings = paymentSettings;
        this.amountConfigurationRepository = amountConfigurationRepository;
        this.disabledPaymentMethodRepository = disabledPaymentMethodRepository;
        this.paymentMethodTypeSelectionRepository = paymentMethodTypeSelectionRepository;
        this.amountRepository = amountRepository;
    }

    @Override
    public PaymentMethodDescriptorView.Model map(@NonNull final OneTapItem oneTapItem) {
        final String customOptionId = oneTapItem.getCustomOptionId();
        final String paymentTypeId = paymentMethodTypeSelectionRepository.get(customOptionId);
        final Currency currency = paymentSettings.getCurrency();

        if (disabledPaymentMethodRepository.hasPaymentMethodId(customOptionId)) {
            return DisabledPaymentMethodDescriptorModel.createFrom(oneTapItem.getStatus().getMainMessage());
        } else if (PaymentTypes.isCreditCardPaymentType(paymentTypeId) || oneTapItem.isConsumerCredits()) {
            return mapCredit(oneTapItem);
        } else if (PaymentTypes.isCardPaymentType(paymentTypeId)) {
            return DebitCardDescriptorModel
                .createFrom(currency, amountConfigurationRepository.getConfigurationFor(customOptionId, paymentTypeId));
        } else if (PaymentTypes.isAccountMoney(oneTapItem.getPaymentMethodId())) {
            return AccountMoneyDescriptorModel.createFrom(oneTapItem.getAccountMoney(), currency,
                amountRepository.getAmountToPay(oneTapItem.getPaymentTypeId(), (PayerCost) null));
        } else {
            return EmptyInstallmentsDescriptorModel.create();
        }
    }

    private PaymentMethodDescriptorView.Model mapCredit(@NonNull final OneTapItem oneTapItem) {
        //This model is useful for Credit Card and Consumer Credits
        // FIXME change model to represent more than just credit cards.
        final Text installmentsRightHeader =
            oneTapItem.hasBenefits() ? oneTapItem.getBenefits().getInstallmentsHeader() : null;
        final InterestFree interestFree =
            oneTapItem.hasBenefits() ? oneTapItem.getBenefits().getInterestFree() : null;
        final String customOptionId = oneTapItem.getCustomOptionId();
        final String paymentTypeId = paymentMethodTypeSelectionRepository.get(customOptionId);
        return CreditCardDescriptorModel
            .createFrom(paymentSettings.getCurrency(), installmentsRightHeader, interestFree,
                amountConfigurationRepository.getConfigurationFor(customOptionId, paymentTypeId));
    }
}