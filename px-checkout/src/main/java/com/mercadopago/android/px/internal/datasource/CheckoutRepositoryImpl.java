package com.mercadopago.android.px.internal.datasource;

import android.os.Handler;
import android.os.HandlerThread;
import androidx.annotation.NonNull;
import com.mercadopago.android.px.addons.ESCManagerBehaviour;
import com.mercadopago.android.px.configuration.AdvancedConfiguration;
import com.mercadopago.android.px.configuration.DiscountParamsConfiguration;
import com.mercadopago.android.px.configuration.PaymentConfiguration;
import com.mercadopago.android.px.internal.callbacks.MPCall;
import com.mercadopago.android.px.internal.mappers.ExpressMetadataToDisabledIdMapper;
import com.mercadopago.android.px.internal.repository.AmountConfigurationRepository;
import com.mercadopago.android.px.internal.repository.DisabledPaymentMethodRepository;
import com.mercadopago.android.px.internal.repository.DiscountRepository;
import com.mercadopago.android.px.internal.repository.ExperimentsRepository;
import com.mercadopago.android.px.internal.repository.ExpressMetadataRepository;
import com.mercadopago.android.px.internal.repository.CheckoutRepository;
import com.mercadopago.android.px.internal.repository.ModalRepository;
import com.mercadopago.android.px.internal.repository.PayerComplianceRepository;
import com.mercadopago.android.px.internal.repository.PayerPaymentMethodRepository;
import com.mercadopago.android.px.internal.repository.PaymentMethodRepository;
import com.mercadopago.android.px.internal.repository.PaymentSettingRepository;
import com.mercadopago.android.px.internal.services.CheckoutService;
import com.mercadopago.android.px.internal.tracking.TrackingRepository;
import com.mercadopago.android.px.internal.util.JsonUtil;
import com.mercadopago.android.px.model.ExpressMetadata;
import com.mercadopago.android.px.model.exceptions.ApiException;
import com.mercadopago.android.px.model.internal.CheckoutFeatures;
import com.mercadopago.android.px.model.internal.DisabledPaymentMethod;
import com.mercadopago.android.px.model.internal.OneTapItem;
import com.mercadopago.android.px.model.internal.InitRequest;
import com.mercadopago.android.px.model.internal.CheckoutResponse;
import com.mercadopago.android.px.preferences.CheckoutPreference;
import com.mercadopago.android.px.services.Callback;
import com.mercadopago.android.px.tracking.internal.MPTracker;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CheckoutRepositoryImpl implements CheckoutRepository {

    private static final int MAX_REFRESH_RETRIES = 4;
    private static final int RETRY_DELAY = 500;

    @NonNull private final ESCManagerBehaviour escManagerBehaviour;
    @NonNull private final CheckoutService checkoutService;
    @NonNull /* default */ final PaymentSettingRepository paymentSettingRepository;
    @NonNull /* default */ final ExperimentsRepository experimentsRepository;
    @NonNull /* default */ DisabledPaymentMethodRepository disabledPaymentMethodRepository;
    @NonNull private final MPTracker tracker;
    @NonNull private final PayerPaymentMethodRepository payerPaymentMethodRepository;
    @NonNull private final ExpressMetadataRepository expressMetadataRepository;
    @NonNull private final PaymentMethodRepository paymentMethodRepository;
    @NonNull private final ModalRepository modalRepository;
    @NonNull private PayerComplianceRepository payerComplianceRepository;
    @NonNull private AmountConfigurationRepository amountConfigurationRepository;
    @NonNull private DiscountRepository discountRepository;
    @NonNull private final TrackingRepository trackingRepository;
    /* default */ int refreshRetriesAvailable = MAX_REFRESH_RETRIES;
    /* default */ Handler retryHandler;

    public CheckoutRepositoryImpl(@NonNull final PaymentSettingRepository paymentSettingRepository,
        @NonNull final ExperimentsRepository experimentsRepository,
        @NonNull final DisabledPaymentMethodRepository disabledPaymentMethodRepository,
        @NonNull final ESCManagerBehaviour escManagerBehaviour, @NonNull final CheckoutService checkoutService,
        @NonNull final TrackingRepository trackingRepository, @NonNull final MPTracker tracker,
        @NonNull final PayerPaymentMethodRepository payerPaymentMethodRepository,
        @NonNull final ExpressMetadataRepository expressMetadataRepository,
        @NonNull final PaymentMethodRepository paymentMethodRepository,
        @NonNull final ModalRepository modalRepository,
        @NonNull final PayerComplianceRepository payerComplianceRepository,
        @NonNull final AmountConfigurationRepository amountConfigurationRepository,
        @NonNull final DiscountRepository discountRepository) {
        this.paymentSettingRepository = paymentSettingRepository;
        this.experimentsRepository = experimentsRepository;
        this.disabledPaymentMethodRepository = disabledPaymentMethodRepository;
        this.escManagerBehaviour = escManagerBehaviour;
        this.checkoutService = checkoutService;
        this.trackingRepository = trackingRepository;
        this.tracker = tracker;
        this.payerPaymentMethodRepository = payerPaymentMethodRepository;
        this.expressMetadataRepository = expressMetadataRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.modalRepository = modalRepository;
        this.payerComplianceRepository = payerComplianceRepository;
        this.amountConfigurationRepository = amountConfigurationRepository;
        this.discountRepository = discountRepository;
    }

    @NonNull
    @Override
    public MPCall<CheckoutResponse> checkout() {
        return newCall(getPostResponse());
    }

    protected void configure(@NonNull final CheckoutResponse checkoutResponse) {
        if (checkoutResponse.getPreference() != null) {
            paymentSettingRepository.configure(checkoutResponse.getPreference());
        }
        paymentSettingRepository.configure(checkoutResponse.getSite());
        paymentSettingRepository.configure(checkoutResponse.getCurrency());
        paymentSettingRepository.configure(checkoutResponse.getConfiguration());
        experimentsRepository.configure(checkoutResponse.getExperiments());
        payerPaymentMethodRepository.configure(checkoutResponse.getPayerPaymentMethods());
        expressMetadataRepository.configure(checkoutResponse.getOneTapItems());
        paymentMethodRepository.configure(checkoutResponse.getAvailablePaymentMethods());
        modalRepository.configure(checkoutResponse.getModals());
        payerComplianceRepository.configure(checkoutResponse.getPayerCompliance());
        amountConfigurationRepository.configure(checkoutResponse.getDefaultAmountConfiguration());
        discountRepository.configure(checkoutResponse.getDiscountsConfigurations());

        disabledPaymentMethodRepository.storeDisabledPaymentMethodsIds(
            new ExpressMetadataToDisabledIdMapper().map(checkoutResponse.getOneTapItems()));

        tracker.setExperiments(experimentsRepository.getExperiments());
    }

    @Override
    public void lazyConfigure(@NonNull final CheckoutResponse checkoutResponse) {
        getPostResponse().call(checkoutResponse);
    }

    interface PostResponse {
        void call(CheckoutResponse checkoutResponse);
    }

    /* default */ PostResponse getPostResponse() {
        return this::configure;
    }

    private PostResponse noPostResponse() {
        return initResponse -> {
        };
    }

    @NonNull
    private MPCall<CheckoutResponse> newCall(@NonNull final PostResponse postResponse) {
        return new MPCall<CheckoutResponse>() {

            @Override
            public void enqueue(final Callback<CheckoutResponse> callback) {
                newRequest().enqueue(getInternalCallback(callback));
            }

            @NonNull /* default */ Callback<CheckoutResponse> getInternalCallback(
                final Callback<CheckoutResponse> callback) {
                return new Callback<CheckoutResponse>() {
                    @Override
                    public void success(final CheckoutResponse checkoutResponse) {
                        postResponse.call(checkoutResponse);
                        callback.success(checkoutResponse);
                    }

                    @Override
                    public void failure(final ApiException apiException) {
                        callback.failure(apiException);
                    }
                };
            }
        };
    }

    @NonNull /* default */ MPCall<CheckoutResponse> newRequest() {
        final CheckoutPreference checkoutPreference = paymentSettingRepository.getCheckoutPreference();
        final PaymentConfiguration paymentConfiguration = paymentSettingRepository.getPaymentConfiguration();

        final AdvancedConfiguration advancedConfiguration = paymentSettingRepository.getAdvancedConfiguration();
        final DiscountParamsConfiguration discountParamsConfiguration =
            advancedConfiguration.getDiscountParamsConfiguration();

        final CheckoutFeatures features = new CheckoutFeatures.Builder()
            .setSplit(paymentConfiguration.getPaymentProcessor().supportsSplitPayment(checkoutPreference))
            .setExpress(advancedConfiguration.isExpressPaymentEnabled())
            .setOdrFlag(true)
            .build();

        final Map<String, Object> body = JsonUtil.getMapFromObject(
            new InitRequest.Builder(paymentSettingRepository.getPublicKey())
                .setCardWithEsc(new ArrayList<>(escManagerBehaviour.getESCCardIds()))
                .setCharges(paymentConfiguration.getCharges())
                .setDiscountParamsConfiguration(discountParamsConfiguration)
                .setCheckoutFeatures(features)
                .setCheckoutPreference(checkoutPreference)
                .setFlow(trackingRepository.getFlowId())
                .build());

        final String preferenceId = paymentSettingRepository.getCheckoutPreferenceId();
        if (preferenceId != null) {
            return checkoutService.checkout(preferenceId, paymentSettingRepository.getPrivateKey(), body);
        } else {
            return checkoutService.checkout(paymentSettingRepository.getPrivateKey(), body);
        }
    }

    @NonNull
    @Override
    public MPCall<CheckoutResponse> refreshWithNewCard(@NonNull final String cardId) {
        return callback -> newCall(noPostResponse()).enqueue(getRefreshWithNewCardCallback(cardId, callback));
    }

    /* default */ Callback<CheckoutResponse> getRefreshWithNewCardCallback(@NonNull final String cardId,
        @NonNull final Callback<CheckoutResponse> callback) {
        final Map<String, DisabledPaymentMethod> disabledPaymentMethodMap =
            disabledPaymentMethodRepository.getDisabledPaymentMethods();
        return new Callback<CheckoutResponse>() {
            @Override
            public void success(final CheckoutResponse checkoutResponse) {
                refreshRetriesAvailable--;
                final List<OneTapItem> oneTap = checkoutResponse.getOneTapItems();
                for (final ExpressMetadata node : oneTap) {
                    if (node.isCard() && node.getCard().getId().equals(cardId)) {
                        refreshRetriesAvailable = MAX_REFRESH_RETRIES;
                        new ExpressMetadataSorter(oneTap, disabledPaymentMethodMap)
                            .setPrioritizedCardId(cardId).sort();
                        getPostResponse().call(checkoutResponse);
                        callback.success(checkoutResponse);
                        return;
                    }
                }
                if (refreshRetriesAvailable > 0) {
                    if (retryHandler == null) {
                        final HandlerThread thread = new HandlerThread("MyInitRetryThread");
                        thread.start();
                        retryHandler = new Handler(thread.getLooper());
                    }
                    retryHandler.postDelayed(() -> refreshWithNewCard(cardId).enqueue(callback), RETRY_DELAY);
                } else {
                    callback.failure(new ApiException());
                }
            }

            @Override
            public void failure(final ApiException apiException) {
                callback.failure(apiException);
            }
        };
    }
}
