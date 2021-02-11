package com.mercadopago.android.px.internal.features.business_result;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.mercadopago.android.px.addons.FlowBehaviour;
import com.mercadopago.android.px.internal.base.BasePresenter;
import com.mercadopago.android.px.internal.features.payment_congrats.model.FlowBehaviourResultMapper;
import com.mercadopago.android.px.internal.features.payment_congrats.model.PaymentCongratsModel;
import com.mercadopago.android.px.internal.features.payment_congrats.model.PaymentCongratsResponse;
import com.mercadopago.android.px.internal.features.payment_result.CongratsAutoReturn;
import com.mercadopago.android.px.internal.features.payment_result.presentation.PaymentResultFooter;
import com.mercadopago.android.px.internal.util.TextUtil;
import com.mercadopago.android.px.internal.view.PaymentResultBody;
import com.mercadopago.android.px.model.ExitAction;
import com.mercadopago.android.px.model.internal.PrimaryExitAction;
import com.mercadopago.android.px.model.internal.SecondaryExitAction;
import com.mercadopago.android.px.tracking.internal.MPTracker;
import com.mercadopago.android.px.tracking.internal.events.AbortEvent;
import com.mercadopago.android.px.tracking.internal.events.CongratsSuccessDeepLink;
import com.mercadopago.android.px.tracking.internal.events.CrossSellingEvent;
import com.mercadopago.android.px.tracking.internal.events.DeepLinkType;
import com.mercadopago.android.px.tracking.internal.events.DiscountItemEvent;
import com.mercadopago.android.px.tracking.internal.events.DownloadAppEvent;
import com.mercadopago.android.px.tracking.internal.events.PrimaryActionEvent;
import com.mercadopago.android.px.tracking.internal.events.ScoreEvent;
import com.mercadopago.android.px.tracking.internal.events.SecondaryActionEvent;
import com.mercadopago.android.px.tracking.internal.events.SeeAllDiscountsEvent;
import com.mercadopago.android.px.tracking.internal.events.ViewReceiptEvent;
import com.mercadopago.android.px.tracking.internal.views.ResultViewTrack;

/* default */ class BusinessPaymentResultPresenter extends BasePresenter<BusinessPaymentResult.View>
    implements BusinessPaymentResult.Presenter, PaymentResultBody.Listener {

    @NonNull /* default */ final PaymentCongratsModel model;
    /* default */ final ResultViewTrack viewTracker;
    private final FlowBehaviour flowBehaviour;
    @Nullable /* default */ CongratsAutoReturn autoReturnTimer;

    /* default */ BusinessPaymentResultPresenter(@NonNull final PaymentCongratsModel model,
        @NonNull final FlowBehaviour flowBehaviour, final boolean isMP, @NonNull final MPTracker tracker) {
        super(tracker);
        this.model = model;
        this.flowBehaviour = flowBehaviour;
        viewTracker = new ResultViewTrack(model, isMP);
    }

    @Override
    public void attachView(@NonNull final BusinessPaymentResult.View view) {
        super.attachView(view);
        configureView();
    }

    @Override
    public void onFreshStart() {
        track(viewTracker);
        flowBehaviour.trackConversion(new FlowBehaviourResultMapper().map(model.getCongratsType()));
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
        track(new AbortEvent(viewTracker));
        final PaymentCongratsResponse congratsResponse = model.getPaymentCongratsResponse();
        getView().processCustomExit(congratsResponse.getBackUrl(), congratsResponse.getRedirectUrl());
    }

    private void configureView() {
        final BusinessPaymentResultViewModel viewModel = new BusinessPaymentResultMapper(getTracker()).map(model);
        getView().configureViews(viewModel, this, new PaymentResultFooter.Listener() {
            @Override
            public void onClick(@NonNull final ExitAction action) {
                if (action instanceof PrimaryExitAction) {
                    track(new PrimaryActionEvent(viewTracker));
                } else if (action instanceof SecondaryExitAction) {
                    track(new SecondaryActionEvent(viewTracker));
                }
                final PaymentCongratsResponse congratsResponse = model.getPaymentCongratsResponse();
                getView().processCustomExit(action, congratsResponse.getBackUrl(), congratsResponse.getRedirectUrl());
            }

            @Override
            public void onClick(@NonNull final String target) {
                getView().launchDeepLink(target);
            }
        });
        getView().setStatusBarColor(viewModel.getHeaderModel().getBackgroundColor());
        final CongratsAutoReturn.Model autoReturnModel = viewModel.getAutoReturnModel();
        if (autoReturnModel != null) {
            initAutoReturn(autoReturnModel);
        }
    }

    private void initAutoReturn(@NonNull final CongratsAutoReturn.Model model) {
        autoReturnTimer = new CongratsAutoReturn(model, new CongratsAutoReturn.Listener() {
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

    @Override
    public void OnClickDownloadAppButton(@NonNull final String deepLink) {
        track(new DownloadAppEvent(viewTracker));
        getView().launchDeepLink(deepLink);
    }

    @Override
    public void OnClickCrossSellingButton(@NonNull final String deepLink) {
        track(new CrossSellingEvent(viewTracker));
        getView().processCrossSellingBusinessAction(deepLink);
    }

    @Override
    public void onClickLoyaltyButton(@NonNull final String deepLink) {
        track(new ScoreEvent(viewTracker));
        getView().launchDeepLink(deepLink);
    }

    @Override
    public void onClickShowAllDiscounts(@NonNull final String deepLink) {
        track(new SeeAllDiscountsEvent(viewTracker));
        getView().launchDeepLink(deepLink);
    }

    @Override
    public void onClickViewReceipt(@NonNull final String deepLink) {
        track(new ViewReceiptEvent(viewTracker));
        getView().launchDeepLink(deepLink);
    }

    @Override
    public void onClickTouchPoint(@Nullable final String deepLink) {
        track(new DiscountItemEvent(viewTracker, 0, TextUtil.EMPTY));
        if (deepLink != null) {
            getView().launchDeepLink(deepLink);
        }
    }

    @Override
    public void onClickDiscountItem(final int index, @Nullable final String deepLink, @Nullable final String trackId) {
        track(new DiscountItemEvent(viewTracker, index, trackId));
        if (deepLink != null) {
            getView().launchDeepLink(deepLink);
        }
    }

    @Override
    public void onClickMoneySplit() {
        final PaymentCongratsResponse.ExpenseSplit moneySplit = model.getPaymentCongratsResponse().getExpenseSplit();
        final String deepLink;
        if (moneySplit != null && (deepLink = moneySplit.getAction().getTarget()) != null) {
            track(new CongratsSuccessDeepLink(DeepLinkType.MONEY_SPLIT_TYPE, deepLink));
            getView().launchDeepLink(deepLink);
        }
    }
}
