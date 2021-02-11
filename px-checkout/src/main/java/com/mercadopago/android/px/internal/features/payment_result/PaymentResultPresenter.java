package com.mercadopago.android.px.internal.features.payment_result;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.mercadopago.android.px.addons.FlowBehaviour;
import com.mercadopago.android.px.configuration.PaymentResultScreenConfiguration;
import com.mercadopago.android.px.core.MercadoPagoCheckout;
import com.mercadopago.android.px.internal.actions.ChangePaymentMethodAction;
import com.mercadopago.android.px.internal.actions.CopyAction;
import com.mercadopago.android.px.internal.actions.LinkAction;
import com.mercadopago.android.px.internal.actions.NextAction;
import com.mercadopago.android.px.internal.actions.RecoverPaymentAction;
import com.mercadopago.android.px.internal.base.BasePresenter;
import com.mercadopago.android.px.internal.callbacks.FailureRecovery;
import com.mercadopago.android.px.internal.features.PaymentResultViewModelFactory;
import com.mercadopago.android.px.internal.features.payment_congrats.model.PaymentCongratsModelMapper;
import com.mercadopago.android.px.internal.features.payment_result.mappers.PaymentResultViewModelMapper;
import com.mercadopago.android.px.internal.features.payment_result.presentation.PaymentResultButton;
import com.mercadopago.android.px.internal.features.payment_result.presentation.PaymentResultFooter;
import com.mercadopago.android.px.internal.features.payment_result.viewmodel.PaymentResultViewModel;
import com.mercadopago.android.px.internal.repository.InstructionsRepository;
import com.mercadopago.android.px.internal.repository.PaymentSettingRepository;
import com.mercadopago.android.px.internal.util.ApiUtil;
import com.mercadopago.android.px.internal.util.TextUtil;
import com.mercadopago.android.px.internal.view.ActionDispatcher;
import com.mercadopago.android.px.internal.viewmodel.PaymentModel;
import com.mercadopago.android.px.internal.mappers.FlowBehaviourResultMapper;
import com.mercadopago.android.px.model.Action;
import com.mercadopago.android.px.model.IPaymentDescriptor;
import com.mercadopago.android.px.model.Instruction;
import com.mercadopago.android.px.model.exceptions.ApiException;
import com.mercadopago.android.px.model.internal.CongratsResponse;
import com.mercadopago.android.px.services.Callback;
import com.mercadopago.android.px.tracking.internal.MPTracker;
import com.mercadopago.android.px.tracking.internal.events.AbortEvent;
import com.mercadopago.android.px.tracking.internal.events.ChangePaymentMethodEvent;
import com.mercadopago.android.px.tracking.internal.events.CongratsSuccessDeepLink;
import com.mercadopago.android.px.tracking.internal.events.ContinueEvent;
import com.mercadopago.android.px.tracking.internal.events.CrossSellingEvent;
import com.mercadopago.android.px.tracking.internal.events.DeepLinkType;
import com.mercadopago.android.px.tracking.internal.events.DiscountItemEvent;
import com.mercadopago.android.px.tracking.internal.events.DownloadAppEvent;
import com.mercadopago.android.px.tracking.internal.events.ScoreEvent;
import com.mercadopago.android.px.tracking.internal.events.SeeAllDiscountsEvent;
import com.mercadopago.android.px.tracking.internal.events.ViewReceiptEvent;
import com.mercadopago.android.px.tracking.internal.views.ResultViewTrack;
import java.util.List;

/* default */ class PaymentResultPresenter extends BasePresenter<PaymentResult.View>
    implements ActionDispatcher, PaymentResult.Presenter, PaymentResult.Listener {

    @NonNull private final PaymentSettingRepository paymentSettings;
    private final PaymentModel paymentModel;
    private final InstructionsRepository instructionsRepository;
    private final ResultViewTrack resultViewTrack;
    private final PaymentResultScreenConfiguration screenConfiguration;
    @NonNull private final PaymentResultViewModelFactory factory;
    @NonNull /* default */ final PaymentCongratsModelMapper paymentCongratsMapper;
    private final FlowBehaviour flowBehaviour;

    private FailureRecovery failureRecovery;
    @Nullable /* default */ CongratsAutoReturn autoReturnTimer;

    /* default */ PaymentResultPresenter(@NonNull final PaymentSettingRepository paymentSettings,
        @NonNull final InstructionsRepository instructionsRepository, @NonNull final PaymentModel paymentModel,
        @NonNull final FlowBehaviour flowBehaviour, final boolean isMP,
        @NonNull final PaymentCongratsModelMapper paymentCongratsMapper,
        @NonNull final PaymentResultViewModelFactory factory,
        @NonNull final MPTracker tracker) {
        super(tracker);
        this.paymentSettings = paymentSettings;
        this.paymentModel = paymentModel;
        this.instructionsRepository = instructionsRepository;
        this.flowBehaviour = flowBehaviour;
        this.paymentCongratsMapper = paymentCongratsMapper;

        screenConfiguration = paymentSettings.getAdvancedConfiguration().getPaymentResultScreenConfiguration();
        this.factory = factory;
        resultViewTrack = new ResultViewTrack(paymentModel, screenConfiguration, paymentSettings, isMP);
    }

    @Override
    public void attachView(@NonNull final PaymentResult.View view) {
        super.attachView(view);

        if (paymentModel.getPaymentResult().isOffPayment()) {
            getInstructions();
        } else {
            configureView(null);
        }
    }

    @Override
    public void onFreshStart() {
        setCurrentViewTracker(resultViewTrack);
        final IPaymentDescriptor payment = paymentModel.getPayment();
        if (payment != null) {
            flowBehaviour.trackConversion(new FlowBehaviourResultMapper().map(payment));
        } else {
            flowBehaviour.trackConversion();
        }
    }

    @Override
    public void onStart() {
        if (autoReturnTimer != null) {
            autoReturnTimer.start();
        }
    }

    @Override
    public void onStop() {
        if (autoReturnTimer != null) {
            autoReturnTimer.cancel();
        }
    }

    @Override
    public void onAbort() {
        track(new AbortEvent(resultViewTrack));
        finishWithResult(MercadoPagoCheckout.PAYMENT_RESULT_CODE);
    }

    /* default */ void getInstructions() {
        instructionsRepository.getInstructions(paymentModel.getPaymentResult()).enqueue(
            new Callback<List<Instruction>>() {
                @Override
                public void success(final List<Instruction> instructions) {
                    resolveInstructions(instructions);
                }

                @Override
                public void failure(final ApiException apiException) {
                    if (isViewAttached()) {
                        getView().showApiExceptionError(apiException, ApiUtil.RequestOrigin.GET_INSTRUCTIONS);
                        setFailureRecovery(() -> getInstructions());
                    }
                }
            });
    }

    public void recoverFromFailure() {
        if (failureRecovery != null) {
            failureRecovery.recover();
        }
    }

    /* default */ void setFailureRecovery(final FailureRecovery failureRecovery) {
        this.failureRecovery = failureRecovery;
    }

    /* default */ void resolveInstructions(final List<Instruction> instructions) {
        final Instruction instruction = getInstruction(instructions);
        if (isViewAttached() && instruction == null) {
            getView().showInstructionsError();
        } else {
            configureView(instruction);
        }
    }

    @Nullable
    private Instruction getInstruction(@NonNull final List<Instruction> instructions) {
        if (instructions.size() == 1) {
            return instructions.get(0);
        } else {
            return getInstructionForType(instructions,
                paymentModel.getPaymentResult().getPaymentData().getPaymentMethod().getPaymentTypeId());
        }
    }

    @Nullable
    private Instruction getInstructionForType(final Iterable<Instruction> instructions, final String paymentTypeId) {
        for (final Instruction instruction : instructions) {
            if (instruction.getType().equals(paymentTypeId)) {
                return instruction;
            }
        }
        return null;
    }

    private void configureView(@Nullable final Instruction instruction) {
        if (isViewAttached()) {
            final PaymentResultViewModel viewModel = new PaymentResultViewModelMapper(screenConfiguration, factory,
                getTracker(), instruction, paymentSettings.getCheckoutPreference().getAutoReturn()).map(paymentModel);
            getView().configureViews(viewModel, paymentModel, this, new PaymentResultFooter.Listener() {
                @Override
                public void onClick(@NonNull final PaymentResultButton.Action action) {
                    //Only known action at this moment.
                    if (action == PaymentResultButton.Action.CONTINUE) {
                        dispatch(new NextAction());
                    }
                }

                @Override
                public void onClick(@NonNull final String target) {
                    getView().launchDeepLink(target);
                }
            });
            getView().setStatusBarColor(viewModel.getHeaderModel().getBackgroundColor());
            final CongratsAutoReturn.Model autoReturnModel = viewModel.getAutoReturnModel();
            if (autoReturnModel != null) {
                autoReturnTimer = new CongratsAutoReturn(autoReturnModel, new CongratsAutoReturn.Listener() {
                    @Override
                    public void onFinish() {
                        autoReturnTimer = null;
                        onAbort();
                    }

                    @Override
                    public void updateView(@NonNull final String label) {
                        getView().updateAutoReturnLabel(label);
                    }
                });
            }
        }
    }

    @Override
    public void dispatch(@NonNull final Action action) {
        if (!isViewAttached()) {
            return;
        }

        if (action instanceof NextAction) {
            track(new ContinueEvent(resultViewTrack));
            finishWithResult(MercadoPagoCheckout.PAYMENT_RESULT_CODE);
        } else if (action instanceof ChangePaymentMethodAction) {
            track(new ChangePaymentMethodEvent(resultViewTrack));
            getView().changePaymentMethod();
        } else if (action instanceof RecoverPaymentAction) {
            getView().recoverPayment();
        } else if (action instanceof LinkAction) {
            getView().openLink(((LinkAction) action).url);
        } else if (action instanceof CopyAction) {
            getView().copyToClipboard(((CopyAction) action).content);
        }
    }

    @Override
    public void OnClickDownloadAppButton(@NonNull final String deepLink) {
        track(new DownloadAppEvent(resultViewTrack));
        getView().launchDeepLink(deepLink);
    }

    @Override
    public void OnClickCrossSellingButton(@NonNull final String deepLink) {
        track(new CrossSellingEvent(resultViewTrack));
        getView().processCrossSellingBusinessAction(deepLink);
    }

    @Override
    public void onClickLoyaltyButton(@NonNull final String deepLink) {
        track(new ScoreEvent(resultViewTrack));
        getView().launchDeepLink(deepLink);
    }

    @Override
    public void onClickShowAllDiscounts(@NonNull final String deepLink) {
        track(new SeeAllDiscountsEvent(resultViewTrack));
        getView().launchDeepLink(deepLink);
    }

    @Override
    public void onClickViewReceipt(@NonNull final String deeLink) {
        track(new ViewReceiptEvent(resultViewTrack));
        getView().launchDeepLink(deeLink);
    }

    @Override
    public void onClickTouchPoint(@Nullable final String deepLink) {
        track(new DiscountItemEvent(resultViewTrack, 0, TextUtil.EMPTY));
        if (deepLink != null) {
            getView().launchDeepLink(deepLink);
        }
    }

    @Override
    public void onClickDiscountItem(final int index, @Nullable final String deepLink, @Nullable final String trackId) {
        track(new DiscountItemEvent(resultViewTrack, index, trackId));
        if (deepLink != null) {
            getView().launchDeepLink(deepLink);
        }
    }

    @Override
    public void onClickMoneySplit() {
        final CongratsResponse.MoneySplit moneySplit = paymentModel.getCongratsResponse().getMoneySplit();
        final String deepLink;
        if (moneySplit != null && (deepLink = moneySplit.getAction().getTarget()) != null) {
            track(new CongratsSuccessDeepLink(DeepLinkType.MONEY_SPLIT_TYPE, deepLink));
            getView().launchDeepLink(deepLink);
        }
    }

    private void finishWithResult(final int resultCode) {
        final CongratsResponse congratsResponse = paymentModel.getCongratsResponse();
        getView().finishWithResult(resultCode, congratsResponse.getBackUrl(), congratsResponse.getRedirectUrl());
    }
}
