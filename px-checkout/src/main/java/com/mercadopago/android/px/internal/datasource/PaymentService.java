package com.mercadopago.android.px.internal.datasource;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.mercadopago.android.px.addons.ESCManagerBehaviour;
import com.mercadopago.android.px.core.SplitPaymentProcessor;
import com.mercadopago.android.px.core.internal.PaymentWrapper;
import com.mercadopago.android.px.internal.callbacks.PaymentServiceEventHandler;
import com.mercadopago.android.px.internal.callbacks.PaymentServiceHandlerWrapper;
import com.mercadopago.android.px.internal.core.FileManager;
import com.mercadopago.android.px.internal.datasource.mapper.FromPayerPaymentMethodToCardMapper;
import com.mercadopago.android.px.internal.mappers.PaymentMethodMapper;
import com.mercadopago.android.px.internal.model.EscStatus;
import com.mercadopago.android.px.internal.repository.AmountConfigurationRepository;
import com.mercadopago.android.px.internal.repository.AmountRepository;
import com.mercadopago.android.px.internal.repository.ApplicationSelectionRepository;
import com.mercadopago.android.px.internal.repository.CongratsRepository;
import com.mercadopago.android.px.internal.repository.DisabledPaymentMethodRepository;
import com.mercadopago.android.px.internal.repository.DiscountRepository;
import com.mercadopago.android.px.internal.repository.EscPaymentManager;
import com.mercadopago.android.px.internal.repository.InstructionsRepository;
import com.mercadopago.android.px.internal.repository.PayerPaymentMethodRepository;
import com.mercadopago.android.px.internal.repository.PaymentMethodRepository;
import com.mercadopago.android.px.internal.repository.PaymentRepository;
import com.mercadopago.android.px.internal.repository.PaymentSettingRepository;
import com.mercadopago.android.px.internal.repository.TokenRepository;
import com.mercadopago.android.px.internal.repository.UserSelectionRepository;
import com.mercadopago.android.px.internal.util.TokenErrorWrapper;
import com.mercadopago.android.px.model.AmountConfiguration;
import com.mercadopago.android.px.model.Card;
import com.mercadopago.android.px.model.CardInformation;
import com.mercadopago.android.px.model.Discount;
import com.mercadopago.android.px.model.DiscountConfigurationModel;
import com.mercadopago.android.px.model.IPaymentDescriptor;
import com.mercadopago.android.px.model.PayerCost;
import com.mercadopago.android.px.model.Payment;
import com.mercadopago.android.px.model.PaymentData;
import com.mercadopago.android.px.model.PaymentMethod;
import com.mercadopago.android.px.model.PaymentRecovery;
import com.mercadopago.android.px.model.PaymentResult;
import com.mercadopago.android.px.model.PaymentTypes;
import com.mercadopago.android.px.model.Split;
import com.mercadopago.android.px.model.Token;
import com.mercadopago.android.px.model.exceptions.ApiException;
import com.mercadopago.android.px.model.exceptions.MercadoPagoError;
import com.mercadopago.android.px.model.internal.PaymentConfiguration;
import com.mercadopago.android.px.preferences.CheckoutPreference;
import com.mercadopago.android.px.services.Callback;
import com.mercadopago.android.px.tracking.internal.model.Reason;
import java.io.File;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import kotlin.Pair;

public class PaymentService implements PaymentRepository {

    private static final String FILE_PAYMENT = "file_payment";

    @NonNull private final PaymentSettingRepository paymentSettingRepository;
    @NonNull private final DiscountRepository discountRepository;
    @NonNull private final AmountRepository amountRepository;
    @NonNull private final Context context;
    @NonNull private final TokenRepository tokenRepository;
    @NonNull private final FileManager fileManager;
    @NonNull private final EscPaymentManager escPaymentManager;
    @NonNull private final ESCManagerBehaviour escManagerBehaviour;

    @NonNull /* default */ final PaymentServiceHandlerWrapper handlerWrapper;
    @NonNull /* default */ final AmountConfigurationRepository amountConfigurationRepository;
    @NonNull /* default */ final UserSelectionRepository userSelectionRepository;

    @Nullable private PaymentWrapper payment;
    @NonNull private final File paymentFile;
    @NonNull private final FromPayerPaymentMethodToCardMapper fromPayerPaymentMethodToCardMapper;
    @NonNull private final PaymentMethodMapper paymentMethodMapper;
    @NonNull private final PaymentMethodRepository paymentMethodRepository;
    @NonNull private final ApplicationSelectionRepository applicationSelectionRepository;

    public PaymentService(@NonNull final UserSelectionRepository userSelectionRepository,
        @NonNull final PaymentSettingRepository paymentSettingRepository,
        @NonNull final DisabledPaymentMethodRepository disabledPaymentMethodRepository,
        @NonNull final DiscountRepository discountRepository,
        @NonNull final AmountRepository amountRepository,
        @NonNull final Context context,
        @NonNull final EscPaymentManager escPaymentManager,
        @NonNull final ESCManagerBehaviour escManagerBehaviour,
        @NonNull final TokenRepository tokenRepository,
        @NonNull final InstructionsRepository instructionsRepository,
        @NonNull final AmountConfigurationRepository amountConfigurationRepository,
        @NonNull final CongratsRepository congratsRepository,
        @NonNull final FileManager fileManager,
        @NonNull final FromPayerPaymentMethodToCardMapper fromPayerPaymentMethodToCardMapper,
        @NonNull final PaymentMethodMapper paymentMethodMapper,
        @NonNull final PaymentMethodRepository paymentMethodRepository,
        @NonNull final ApplicationSelectionRepository applicationSelectionRepository) {
        this.amountConfigurationRepository = amountConfigurationRepository;
        this.escPaymentManager = escPaymentManager;
        this.escManagerBehaviour = escManagerBehaviour;
        this.userSelectionRepository = userSelectionRepository;
        this.paymentSettingRepository = paymentSettingRepository;
        this.discountRepository = discountRepository;
        this.amountRepository = amountRepository;
        this.context = context;
        this.tokenRepository = tokenRepository;
        this.fileManager = fileManager;

        paymentFile = fileManager.create(FILE_PAYMENT);
        this.fromPayerPaymentMethodToCardMapper = fromPayerPaymentMethodToCardMapper;
        this.paymentMethodMapper = paymentMethodMapper;
        this.paymentMethodRepository = paymentMethodRepository;
        this.applicationSelectionRepository = applicationSelectionRepository;

        handlerWrapper =
            new PaymentServiceHandlerWrapper(this, disabledPaymentMethodRepository, escPaymentManager,
                instructionsRepository, congratsRepository, userSelectionRepository);
    }

    @Nullable
    @Override
    public PaymentServiceEventHandler getObservableEvents() {
        return handlerWrapper.getObservableEvents();
    }

    @Override
    public void reset() {
        fileManager.removeFile(paymentFile);
    }

    @Override
    public void storePayment(@NonNull final IPaymentDescriptor payment) {
        this.payment = new PaymentWrapper(payment);
        fileManager.writeToFile(paymentFile, this.payment);
    }

    @Nullable
    @Override
    public IPaymentDescriptor getPayment() {
        final PaymentWrapper paymentWrapper = getPaymentWrapper();
        return paymentWrapper != null ? paymentWrapper.get() : null;
    }

    @Nullable
    private PaymentWrapper getPaymentWrapper() {
        if (payment == null) {
            payment = fileManager.readParcelable(paymentFile, PaymentWrapper.CREATOR);
        }
        return payment;
    }

    @NonNull
    @Override
    public PaymentRecovery createRecoveryForInvalidESC() {
        return createPaymentRecovery(Payment.StatusDetail.STATUS_DETAIL_INVALID_ESC);
    }

    @NonNull
    @Override
    public PaymentRecovery createPaymentRecovery() {
        return createPaymentRecovery(getPayment().getPaymentStatusDetail());
    }

    @NonNull
    private PaymentRecovery createPaymentRecovery(@NonNull final String statusDetail) {
        final Token token = paymentSettingRepository.getToken();
        final Card card = userSelectionRepository.getCard();
        final PaymentMethod paymentMethod = userSelectionRepository.getPaymentMethod();
        return new PaymentRecovery(statusDetail, token, card, paymentMethod);
    }

    /**
     * This method presets all user information ahead before the payment is processed.
     */
    @Override
    public void startExpressPayment(@NonNull final PaymentConfiguration configuration) {
        handlerWrapper.createTransactionLiveData();

        final Pair<String, String> pair = new Pair<>(configuration.getPaymentMethodId(),
            configuration.getPaymentTypeId());
        final PaymentMethod paymentMethod = paymentMethodMapper.map(pair);
        userSelectionRepository.select(paymentMethod, null);
        if (PaymentTypes.isCardPaymentType(paymentMethod.getPaymentTypeId())) {
            // cards
            final Card card = fromPayerPaymentMethodToCardMapper.map(
                new PayerPaymentMethodRepository.Key(configuration.getCustomOptionId(),
                    paymentMethod.getId(), paymentMethod.getPaymentTypeId()));
            if(card == null) {
                throw new IllegalStateException("Cannot find selected card");
            }
            if (configuration.getSplitPayment()) {
                //TODO refactor
                final String secondaryPaymentMethodId =
                    amountConfigurationRepository.getConfigurationFor(card.getId(), applicationSelectionRepository.getPaymentMethodTypeId(card.getId()))
                        .getSplitConfiguration().secondaryPaymentMethod.paymentMethodId;
                userSelectionRepository
                    .select(card, paymentMethodRepository.getPaymentMethodById(secondaryPaymentMethodId));
            } else if (card != null) {
                userSelectionRepository.select(card, null);
            }
        }
        userSelectionRepository.select(configuration.getPayerCost());

        startPayment();
    }

    @Override
    public void startPayment() {
        //Wrapping the callback is important to assure ESC handling.
        processPaymentMethod();
    }

    /* default */ void processPaymentMethod() {
        if (PaymentTypes.isCardPaymentType(userSelectionRepository.getPaymentMethod().getPaymentTypeId())) {
            payWithCard();
        } else {
            pay();
        }
    }

    private boolean shouldPayWithCvv(@NonNull final CardInformation card) {
        final int securityCodeLength = card.getSecurityCodeLength() != null ? card.getSecurityCodeLength() : 0;
        return securityCodeLength > 0;
    }

    private void payWithoutCvv(@NonNull final Card card) {
        tokenRepository.createTokenWithoutCvv(card).enqueue(new Callback<Token>() {
            @Override
            public void success(final Token token) {
                pay();
            }

            @Override
            public void failure(final ApiException apiException) {
                // No start CVV screen if fail
                final String tokenError = new TokenErrorWrapper(apiException).getValue();
                handlerWrapper.onPaymentError(MercadoPagoError.createRecoverable(tokenError));
            }
        });
    }

    private void payWithCard() {
        final Card card = userSelectionRepository.getCard();

        if (card != null && hasPayerCost()) {
            if (paymentSettingRepository.hasToken()) { // Paying with saved card with token
                pay();
            } else if (shouldPayWithCvv(card)) { // Token does not exists - must generate one or ask for CVV.
                checkEscAvailability(card);
            } else {
                payWithoutCvv(card); // Tokenize and pay with card without cvv (WebPay for example)
            }
        } else if (hasValidNewCardInfo()) { // New card payment
            pay();
        } else { // Guessing card could not tokenize or obtain card information.
            handlerWrapper.onPaymentError(getError());
        }
    }

    private boolean shouldInvalidateEsc(final String escStatus) {
        return EscStatus.REJECTED.equals(escStatus);
    }

    private void checkEscAvailability(@NonNull final Card card) {
        //Paying with saved card without token

        final boolean shouldInvalidateEsc = shouldInvalidateEsc(card.getEscStatus());
        if (escManagerBehaviour.isESCEnabled() && escPaymentManager.hasEsc(card) && !shouldInvalidateEsc) {
            //Saved card has ESC - Try to tokenize
            tokenRepository.createToken(card).enqueue(new Callback<Token>() {
                @Override
                public void success(final Token token) {
                    processPaymentMethod();
                }

                @Override
                public void failure(final ApiException apiException) {
                    //Start CVV screen if fail
                    handlerWrapper.onCvvRequired(card, new TokenErrorWrapper(apiException).toReason());
                }
            });
        } else {
            final Reason reason =
                escManagerBehaviour.isESCEnabled() ? (shouldInvalidateEsc ? Reason.ESC_CAP : Reason.SAVED_CARD)
                    : Reason.ESC_DISABLED;
            handlerWrapper.onCvvRequired(card, reason);
        }
    }

    private boolean hasPayerCost() {
        return userSelectionRepository.getPayerCost() != null;
    }

    private boolean hasValidNewCardInfo() {
        return userSelectionRepository.getPaymentMethod() != null
            && userSelectionRepository.getIssuer() != null
            && userSelectionRepository.getPayerCost() != null
            && paymentSettingRepository.hasToken();
    }

    private void pay() {
        final CheckoutPreference checkoutPreference = paymentSettingRepository.getCheckoutPreference();
        final String securityType = paymentSettingRepository.getSecurityType().getValue();
        if (getPaymentProcessor().shouldShowFragmentOnPayment(checkoutPreference)) {
            handlerWrapper.onVisualPayment();
        } else {
            final SplitPaymentProcessor.CheckoutData checkoutData =
                new SplitPaymentProcessor.CheckoutData(getPaymentDataList(), checkoutPreference, securityType);
            getPaymentProcessor().startPayment(context, checkoutData, handlerWrapper);
        }
    }

    @Override
    public boolean isExplodingAnimationCompatible() {
        return !getPaymentProcessor().shouldShowFragmentOnPayment(paymentSettingRepository.getCheckoutPreference());
    }

    /**
     * Payment data is a dynamic non-mutable object that represents the payment state of the checkout exp.
     *
     * @return payment data at the moment is called.
     */
    @NonNull
    @Override
    public List<PaymentData> getPaymentDataList() {
        final DiscountConfigurationModel discountModel = discountRepository.getCurrentConfiguration();
        final PaymentMethod secondaryPaymentMethod = userSelectionRepository.getSecondaryPaymentMethod();
        final PaymentMethod paymentMethod = userSelectionRepository.getPaymentMethod();
        final PayerCost payerCost = userSelectionRepository.getPayerCost();
        final BigDecimal amountToPay = amountRepository.getAmountToPay(paymentMethod.getPaymentTypeId(), payerCost);

        if (secondaryPaymentMethod != null) { // is split payment
            final AmountConfiguration currentConfiguration = amountConfigurationRepository.getCurrentConfiguration();
            final Split splitConfiguration = currentConfiguration.getSplitConfiguration();

            final PaymentData paymentData = new PaymentData.Builder()
                .setPaymentMethod(paymentMethod)
                .setPayerCost(payerCost)
                .setToken(paymentSettingRepository.getToken())
                .setIssuer(userSelectionRepository.getIssuer())
                .setPayer(paymentSettingRepository.getCheckoutPreference().getPayer())
                .setTransactionAmount(amountToPay)
                .setCampaign(discountModel.getCampaign())
                .setDiscount(splitConfiguration.primaryPaymentMethod.discount)
                .setRawAmount(splitConfiguration.primaryPaymentMethod.amount)
                .createPaymentData();

            final PaymentData secondaryPaymentData = new PaymentData.Builder()
                .setTransactionAmount(amountToPay)
                .setPayer(paymentSettingRepository.getCheckoutPreference().getPayer())
                .setPaymentMethod(secondaryPaymentMethod)
                .setCampaign(discountModel.getCampaign())
                .setDiscount(splitConfiguration.secondaryPaymentMethod.discount)
                .setRawAmount(splitConfiguration.secondaryPaymentMethod.amount)
                .createPaymentData();

            return Arrays.asList(paymentData, secondaryPaymentData);
        } else { // is regular 1 pm payment
            Discount discount;

            try {
                discount = Discount.replaceWith(discountModel.getDiscount(),
                    amountConfigurationRepository.getCurrentConfiguration().getDiscountToken());
            } catch (final IllegalStateException e) {
                discount = discountModel.getDiscount();
            }

            final PaymentData paymentData = new PaymentData.Builder()
                .setPaymentMethod(paymentMethod)
                .setPayerCost(payerCost)
                .setToken(paymentSettingRepository.getToken())
                .setIssuer(userSelectionRepository.getIssuer())
                .setDiscount(discount)
                .setPayer(paymentSettingRepository.getCheckoutPreference().getPayer())
                .setTransactionAmount(amountToPay)
                .setCampaign(discountModel.getCampaign())
                .setRawAmount(amountRepository.getAmountWithoutDiscount(paymentMethod.getPaymentTypeId(), payerCost))
                .createPaymentData();

            return Collections.singletonList(paymentData);
        }
    }

    /**
     * Transforms IPayment into a {@link PaymentResult}
     *
     * @param payment The payment model
     * @return The transformed {@link PaymentResult}
     */
    @NonNull
    @Override
    public PaymentResult createPaymentResult(@NonNull final IPaymentDescriptor payment) {
        return new PaymentResult.Builder()
            .setPaymentData(getPaymentDataList())
            .setPaymentId(payment.getId())
            .setPaymentMethodId(payment.getPaymentMethodId())
            .setPaymentStatus(payment.getPaymentStatus())
            .setStatementDescription(payment.getStatementDescription())
            .setPaymentStatusDetail(payment.getPaymentStatusDetail())
            .build();
    }

    private SplitPaymentProcessor getPaymentProcessor() {
        return paymentSettingRepository.getPaymentConfiguration().getPaymentProcessor();
    }

    @Override
    public int getPaymentTimeout() {
        return getPaymentProcessor().getPaymentTimeout(paymentSettingRepository.getCheckoutPreference());
    }

    public MercadoPagoError getError() {
        return new MercadoPagoError("Something went wrong", false);
    }
}