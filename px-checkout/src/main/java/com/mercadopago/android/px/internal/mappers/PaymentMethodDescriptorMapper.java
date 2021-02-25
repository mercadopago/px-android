package com.mercadopago.android.px.internal.mappers;

import androidx.annotation.NonNull;
import com.mercadopago.android.px.internal.repository.AmountConfigurationRepository;
import com.mercadopago.android.px.internal.repository.AmountRepository;
import com.mercadopago.android.px.internal.repository.ApplicationSelectionRepository;
import com.mercadopago.android.px.internal.repository.DisabledPaymentMethodRepository;
import com.mercadopago.android.px.internal.repository.PaymentSettingRepository;
import com.mercadopago.android.px.internal.view.PaymentMethodDescriptorView;
import com.mercadopago.android.px.internal.viewmodel.AccountMoneyDescriptorModel;
import com.mercadopago.android.px.internal.viewmodel.CreditCardDescriptorModel;
import com.mercadopago.android.px.internal.viewmodel.DebitCardDescriptorModel;
import com.mercadopago.android.px.internal.viewmodel.DisabledPaymentMethodDescriptorModel;
import com.mercadopago.android.px.internal.viewmodel.EmptyInstallmentsDescriptorModel;
import com.mercadopago.android.px.internal.viewmodel.PaymentMethodDescriptorModel;
import com.mercadopago.android.px.model.BenefitsMetadata;
import com.mercadopago.android.px.model.Currency;
import com.mercadopago.android.px.model.InterestFree;
import com.mercadopago.android.px.model.PayerCost;
import com.mercadopago.android.px.model.PaymentTypes;
import com.mercadopago.android.px.model.internal.Application;
import com.mercadopago.android.px.model.internal.OneTapItem;
import com.mercadopago.android.px.model.internal.Text;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PaymentMethodDescriptorMapper
    extends Mapper<OneTapItem, PaymentMethodDescriptorModel> {

    @NonNull private final PaymentSettingRepository paymentSettings;
    @NonNull private final AmountConfigurationRepository amountConfigurationRepository;
    @NonNull private final DisabledPaymentMethodRepository disabledPaymentMethodRepository;
    @NonNull private final ApplicationSelectionRepository applicationSelectionRepository;
    @NonNull private final AmountRepository amountRepository;

    public PaymentMethodDescriptorMapper(@NonNull final PaymentSettingRepository paymentSettings,
        @NonNull final AmountConfigurationRepository amountConfigurationRepository,
        @NonNull final DisabledPaymentMethodRepository disabledPaymentMethodRepository,
        @NonNull final ApplicationSelectionRepository applicationSelectionRepository,
        @NonNull final AmountRepository amountRepository) {
        this.paymentSettings = paymentSettings;
        this.amountConfigurationRepository = amountConfigurationRepository;
        this.disabledPaymentMethodRepository = disabledPaymentMethodRepository;
        this.applicationSelectionRepository = applicationSelectionRepository;
        this.amountRepository = amountRepository;
    }

    @Override
    public PaymentMethodDescriptorModel map(@NonNull final OneTapItem oneTapItem) {
        final Map<String, PaymentMethodDescriptorView.Model> paymentMethodDescriptorModelMap = new HashMap<>();
        final List<Application> applications = oneTapItem.getApplications();
        final String defaultPaymentMethodType = applicationSelectionRepository.getPaymentMethodTypeId(oneTapItem.getCustomOptionId());
        String paymentMethodType = defaultPaymentMethodType;

        if (applications != null && !applications.isEmpty()) {
            for (final Application application : applications) {
                paymentMethodType = application.getPaymentMethod().getType();
                paymentMethodDescriptorModelMap.put(
                    paymentMethodType,
                    createPaymentMethodDescriptorModel(oneTapItem, paymentMethodType));
            }
        } else {
            paymentMethodDescriptorModelMap.put(
                paymentMethodType,
                createPaymentMethodDescriptorModel(oneTapItem, paymentMethodType));
        }

        return new PaymentMethodDescriptorModel(defaultPaymentMethodType, paymentMethodDescriptorModelMap);
    }

    private PaymentMethodDescriptorView.Model createPaymentMethodDescriptorModel(
        @NonNull final OneTapItem oneTapItem, @NonNull final String paymentMethodType) {
        final String customOptionId = oneTapItem.getCustomOptionId();
        final Currency currency = paymentSettings.getCurrency();

        if (disabledPaymentMethodRepository.hasPaymentMethodId(customOptionId)) {
            return DisabledPaymentMethodDescriptorModel.createFrom(oneTapItem.getStatus().getMainMessage());
        } else if (PaymentTypes.isCreditCardPaymentType(paymentMethodType) || oneTapItem.isConsumerCredits()) {
            return mapCredit(oneTapItem);
        } else if (PaymentTypes.isCardPaymentType(paymentMethodType)) {
            return DebitCardDescriptorModel
                .createFrom(currency, Objects.requireNonNull(amountConfigurationRepository
                    .getConfigurationFor(customOptionId, paymentMethodType)));
        } else if (PaymentTypes.isAccountMoney(paymentMethodType)) {
            return AccountMoneyDescriptorModel.createFrom(oneTapItem.getAccountMoney(), currency,
                amountRepository.getAmountToPay(paymentMethodType, (PayerCost) null));
        } else {
            return EmptyInstallmentsDescriptorModel.create();
        }
    }

    private PaymentMethodDescriptorView.Model mapCredit(@NonNull final OneTapItem oneTapItem) {
        //This model is useful for Credit Card and Consumer Credits
        // FIXME change model to represent more than just credit cards.
        final BenefitsMetadata benefitsMetadata = oneTapItem.getBenefits();
        final boolean hasBenefits = oneTapItem.hasBenefits();
        final Text installmentsRightHeader = hasBenefits ? benefitsMetadata.getInstallmentsHeader() : null;
        final InterestFree interestFree = hasBenefits ? benefitsMetadata.getInterestFree() : null;
        final String customOptionId = oneTapItem.getCustomOptionId();
        final String paymentTypeId = oneTapItem.getPaymentTypeId();

        return CreditCardDescriptorModel
            .createFrom(paymentSettings.getCurrency(), installmentsRightHeader, interestFree,
                Objects.requireNonNull(amountConfigurationRepository
                    .getConfigurationFor(customOptionId, paymentTypeId)));
    }
}