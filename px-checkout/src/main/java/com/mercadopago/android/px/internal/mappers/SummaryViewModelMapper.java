package com.mercadopago.android.px.internal.mappers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.mercadopago.android.px.internal.repository.AmountConfigurationRepository;
import com.mercadopago.android.px.internal.repository.AmountRepository;
import com.mercadopago.android.px.internal.repository.ChargeRepository;
import com.mercadopago.android.px.internal.repository.CustomTextsRepository;
import com.mercadopago.android.px.internal.repository.DiscountRepository;
import com.mercadopago.android.px.internal.repository.PaymentMethodTypeSelectionRepository;
import com.mercadopago.android.px.internal.util.ChargeRuleHelper;
import com.mercadopago.android.px.internal.view.AmountDescriptorView;
import com.mercadopago.android.px.internal.view.ElementDescriptorView;
import com.mercadopago.android.px.internal.view.SummaryDetailDescriptorFactory;
import com.mercadopago.android.px.internal.view.SummaryView;
import com.mercadopago.android.px.internal.viewmodel.AmountLocalized;
import com.mercadopago.android.px.internal.viewmodel.SummaryModel;
import com.mercadopago.android.px.internal.viewmodel.SummaryViewDefaultColor;
import com.mercadopago.android.px.internal.viewmodel.TotalLocalized;
import com.mercadopago.android.px.model.AmountConfiguration;
import com.mercadopago.android.px.model.Currency;
import com.mercadopago.android.px.model.DiscountConfigurationModel;
import com.mercadopago.android.px.model.commission.PaymentTypeChargeRule;
import com.mercadopago.android.px.model.internal.Application;
import com.mercadopago.android.px.model.internal.OneTapItem;
import com.mercadopago.android.px.model.internal.SummaryInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SummaryViewModelMapper extends CacheableMapper<OneTapItem, SummaryModel,
    SummaryViewModelMapper.Key> {

    @NonNull private final Currency currency;
    @NonNull private final DiscountRepository discountRepository;
    @NonNull private final AmountRepository amountRepository;
    @NonNull private final ElementDescriptorView.Model elementDescriptorModel;
    @NonNull private final AmountDescriptorView.OnClickListener listener;
    @NonNull private final SummaryInfo summaryInfo;
    @NonNull private final ChargeRepository chargeRepository;
    @NonNull private final AmountConfigurationRepository amountConfigurationRepository;
    @NonNull private final PaymentMethodTypeSelectionRepository paymentMethodTypeSelectionRepository;
    @NonNull private final CustomTextsRepository customTextsRepository;
    @NonNull private final AmountDescriptorMapper amountDescriptorMapper;

    public SummaryViewModelMapper(@NonNull final Currency currency,
        @NonNull final DiscountRepository discountRepository, @NonNull final AmountRepository amountRepository,
        @NonNull final ElementDescriptorView.Model elementDescriptorModel,
        @NonNull final AmountDescriptorView.OnClickListener listener,
        @NonNull final SummaryInfo summaryInfo,
        @NonNull final ChargeRepository chargeRepository,
        @NonNull final AmountConfigurationRepository amountConfigurationRepository,
        @NonNull final CustomTextsRepository customTextsRepository,
        @NonNull final AmountDescriptorMapper amountDescriptorMapper,
        @NonNull final PaymentMethodTypeSelectionRepository paymentMethodTypeSelectionRepository) {
        this.currency = currency;
        this.discountRepository = discountRepository;
        this.amountRepository = amountRepository;
        this.elementDescriptorModel = elementDescriptorModel;
        this.listener = listener;
        this.summaryInfo = summaryInfo;
        this.chargeRepository = chargeRepository;
        this.amountConfigurationRepository = amountConfigurationRepository;
        this.customTextsRepository = customTextsRepository;
        this.amountDescriptorMapper = amountDescriptorMapper;
        this.paymentMethodTypeSelectionRepository = paymentMethodTypeSelectionRepository;
    }

    @Override
    public SummaryModel map(@NonNull final OneTapItem expressPaymentMethod) {
        return createModel(expressPaymentMethod);
    }

    @Nullable
    private AmountConfiguration getAmountConfiguration(
        @NonNull final String customOptionId,
        @NonNull final String paymentMethodTypeId) {
        return amountConfigurationRepository.getConfigurationFor(customOptionId, paymentMethodTypeId);
    }

    @NonNull
    private SummaryModel createModel(@NonNull final OneTapItem oneTapItem) {
        final String customOptionId = oneTapItem.getCustomOptionId();
        final List<Application> applications = oneTapItem.getApplications();
        final Map<String, SummaryView.Model> map = new HashMap<>();

        if (applications != null) {
            for (final Application application : applications) {
                summaryViewModelToMap(
                    customOptionId,
                    application.getPaymentMethod().getType(),
                    map);
            }
        } else {
            summaryViewModelToMap(
                customOptionId,
                oneTapItem.getDefaultPaymentMethodType(),
                map);
        }

        return new SummaryModel(paymentMethodTypeSelectionRepository.get(customOptionId), map);
    }

    private void summaryViewModelToMap(
        @NonNull final String customOptionId,
        @NonNull final String paymentTypeId,
        @NonNull final Map<String, SummaryView.Model> map) {

        final AmountConfiguration amountConfiguration = getAmountConfiguration(customOptionId, paymentTypeId);
        final PaymentTypeChargeRule chargeRule = chargeRepository.getChargeRule(paymentTypeId);
        final DiscountConfigurationModel discountModel =
            discountRepository.getConfigurationFor(customOptionId, paymentTypeId);
        final List<AmountDescriptorView.Model> summaryDetailList =
            new SummaryDetailDescriptorFactory(
                listener,
                discountModel,
                amountRepository,
                summaryInfo,
                currency,
                chargeRule,
                amountConfiguration,
                amountDescriptorMapper).create();

        final AmountDescriptorView.Model totalRow = new AmountDescriptorView.Model(
            new TotalLocalized(customTextsRepository),
            new AmountLocalized(amountRepository.getAmountToPay(paymentTypeId, discountModel), currency),
            new SummaryViewDefaultColor());
        map.put(paymentTypeId, new SummaryView.Model(elementDescriptorModel, summaryDetailList, totalRow));
    }

    private void addKeyDataList(
        final List<PaymentTypeChargeRule> chargeRules,
        final List<DiscountConfigurationModel> discountConfigurationModels,
        final List<Boolean> hasSplits,
        final String customOptionId,
        final String paymentMethodType,
        final AmountConfiguration amountConfiguration
    ) {
        final PaymentTypeChargeRule chargeRule;
        final DiscountConfigurationModel discountConfigurationModel;

        chargeRule = chargeRepository.getChargeRule(paymentMethodType);
        discountConfigurationModel = discountRepository.getConfigurationFor(customOptionId, paymentMethodType);
        if (chargeRule != null) {
            chargeRules.add(chargeRule);
        }
        if(discountConfigurationModel != null) {
            discountConfigurationModels
                .add(discountConfigurationModel);
        }

        chargeRules.add(chargeRule);
        discountConfigurationModels.add(discountConfigurationModel);
        hasSplits.add(amountConfiguration != null && amountConfiguration.allowSplit());
    }

    @Override
    protected Key getKey(
        @NonNull final OneTapItem oneTapItem) {
        final String customOptionId = oneTapItem.getCustomOptionId();

        final List<PaymentTypeChargeRule> chargeRules = new ArrayList<>();
        final List<DiscountConfigurationModel> discountConfigurationModels = new ArrayList<>();
        final List<Boolean> hasSplits = new ArrayList<>();
        String paymentMethodType = oneTapItem.getDefaultPaymentMethodType();
        AmountConfiguration amountConfiguration;

        if (oneTapItem.getApplications() != null && !oneTapItem.getApplications().isEmpty()) {
            for (final Application application : oneTapItem.getApplications()) {
                paymentMethodType = application.getPaymentMethod().getType();
                amountConfiguration = getAmountConfiguration(customOptionId, paymentMethodType);
                addKeyDataList(
                    chargeRules,
                    discountConfigurationModels,
                    hasSplits,
                    customOptionId,
                    paymentMethodType,
                    amountConfiguration);
            }
        } else {
            amountConfiguration = getAmountConfiguration(customOptionId, paymentMethodType);
            addKeyDataList(
                chargeRules,
                discountConfigurationModels,
                hasSplits,
                customOptionId,
                paymentMethodType,
                amountConfiguration);
        }

        return new Key(discountConfigurationModels, chargeRules, hasSplits);
    }

    static final class Key {
        private final List<DiscountConfigurationModel> discountConfigurationModels;
        private final List<PaymentTypeChargeRule> paymentTypeChargeRules = new ArrayList<>();
        private final List<Boolean> hasSplits;

        Key(@NonNull final List<DiscountConfigurationModel> discountConfigurationModels,
            final List<PaymentTypeChargeRule> paymentTypeChargeRules,
            final List<Boolean> hasSplits) {
            this.discountConfigurationModels = discountConfigurationModels;
            for (final PaymentTypeChargeRule paymentTypeChargeRule : paymentTypeChargeRules) {
                this.paymentTypeChargeRules.add(
                    ChargeRuleHelper.isHighlightCharge(paymentTypeChargeRule) ? null : paymentTypeChargeRule);
            }
            this.hasSplits = hasSplits;
        }

        @Override
        public int hashCode() {
            return (discountConfigurationModels == null ? 0 : discountConfigurationModels.hashCode()) ^
                (paymentTypeChargeRules == null ? 0 : paymentTypeChargeRules.hashCode()) ^
                (hasSplits == null ? 0 : hasSplits.hashCode());
        }

        @Override
        public boolean equals(@Nullable final Object obj) {
            if (!(obj instanceof Key)) {
                return false;
            }
            final Key k = (Key) obj;
            return Objects.equals(k.discountConfigurationModels, discountConfigurationModels)
                && Objects.equals(k.paymentTypeChargeRules, paymentTypeChargeRules)
                && Objects.equals(k.hasSplits, hasSplits);
        }
    }
}