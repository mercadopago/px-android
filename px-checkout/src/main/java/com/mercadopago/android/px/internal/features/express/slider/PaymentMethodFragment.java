package com.mercadopago.android.px.internal.features.express.slider;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;
import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import com.meli.android.carddrawer.model.CardDrawerSwitchView;
import com.meli.android.carddrawer.model.CardDrawerView;
import com.meli.android.carddrawer.model.SwitchModel;
import com.meli.android.carddrawer.model.CardDrawerSwitchView;
import com.meli.android.carddrawer.model.CardDrawerView;
import com.meli.android.carddrawer.model.SwitchModel;
import com.mercadopago.android.px.R;
import com.mercadopago.android.px.internal.base.BasePagerFragment;
import com.mercadopago.android.px.internal.di.Session;
import com.mercadopago.android.px.internal.features.disable_payment_method.DisabledPaymentMethodDetailDialog;
import com.mercadopago.android.px.internal.features.express.animations.BottomSlideAnimationSet;
import com.mercadopago.android.px.internal.features.generic_modal.GenericDialog;
import com.mercadopago.android.px.internal.features.generic_modal.GenericDialogAction;
import com.mercadopago.android.px.internal.features.generic_modal.GenericDialogItem;
import com.mercadopago.android.px.internal.util.TextUtil;
import com.mercadopago.android.px.internal.util.ViewUtils;
import com.mercadopago.android.px.internal.view.DynamicHeightViewPager;
import com.mercadopago.android.px.internal.view.MPTextView;
import com.mercadopago.android.px.internal.viewmodel.drawables.DrawableFragmentItem;
import com.mercadopago.android.px.model.internal.DisabledPaymentMethod;
import java.util.Arrays;

import static com.mercadopago.android.px.internal.util.AccessibilityUtilsKt.executeIfAccessibilityTalkBackEnable;

public abstract class PaymentMethodFragment<T extends DrawableFragmentItem>
    extends BasePagerFragment<PaymentMethodPresenter, T>
    implements PaymentMethod.View, Focusable, GenericDialog.Listener {

    private CardView card;
    private BottomSlideAnimationSet animation;
    private boolean focused;
    private MPTextView bottomDescription;
    private Handler handler;
    private PaymentMethodPagerListener listener = paymentTypeId -> {
    };

    @Override
    public void onAttach(@NonNull final Context context) {
        super.onAttach(context);
        if (getParentFragment() instanceof PaymentMethodPagerListener) {
            listener = (PaymentMethodPagerListener) getParentFragment();
        }
    }

    @Override
    protected PaymentMethodPresenter createPresenter() {
        return new PaymentMethodPresenter(
            Session.getInstance().getConfigurationModule().getPayerCostSelectionRepository(),
            Session.getInstance().getAmountConfigurationRepository(),
            model, Session.getInstance().getTracker());
    }

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        animation = new BottomSlideAnimationSet();
        handler = new Handler();
    }

    @CallSuper
    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeViews(view);
        final GenericDialogItem genericDialogItem = model.getGenericDialogItem();
        if (isDisableMethod()) {
            disable();
        } else if (genericDialogItem != null) {
            card.setOnClickListener(v -> GenericDialog.showDialog(getChildFragmentManager(), genericDialogItem));
        }
    }

    protected boolean isDisableMethod() {
        return model.getDisabledPaymentMethod() != null;
    }

    @CallSuper
    public void initializeViews(@NonNull final View view) {
        card = view.findViewById(R.id.payment_method);
        bottomDescription = view.findViewById(R.id.bottom_description);
        if (model.shouldHighlightBottomDescription()) {
            final View highlightContainer = view.findViewById(R.id.bottom_description_container);
            highlightContainer.setVisibility(View.INVISIBLE);
            bottomDescription.setVisibility(View.INVISIBLE);
            animation.initialize(Arrays.asList(highlightContainer, bottomDescription));
        } else {
            ViewUtils.setBackgroundColor(view.findViewById(R.id.bottom_description_background),
                model.getBottomDescription().getBackgroundColor());
            ViewUtils.loadOrHide(View.INVISIBLE, model.getBottomDescription(), bottomDescription);
            view.findViewById(R.id.bottom_description_shadow).setVisibility(View.INVISIBLE);
        }
        if (hasFocus()) {
            onFocusIn();
        }
        final CardDrawerView cardDrawerView = view.findViewById(R.id.card);
        if (cardDrawerView != null) {
            setUpCardDrawerView(cardDrawerView);
        }
    }

    @CallSuper
    protected void setUpCardDrawerView(@NonNull final CardDrawerView cardDrawerView) {
        setUpCardDrawerCustomView(cardDrawerView);
    }

    private void setUpCardDrawerCustomView(@NonNull final CardDrawerView cardDrawerView) {
        final SwitchModel switchModel = model.getSwitchModel();
        if (switchModel != null) {
            final CardDrawerSwitchView cardDrawerSwitch = new CardDrawerSwitchView(getContext());
            cardDrawerSwitch.setSwitchModel(switchModel);
            cardDrawerView.setCustomView(cardDrawerSwitch);
            cardDrawerSwitch.setSwitchListener(paymentMethodId -> listener.onApplicationChanged(paymentMethodId));
        }
    }

    protected String getAccessibilityContentDescription() {
        return TextUtil.EMPTY;
    }

    @Override
    public void updateHighlightText(@Nullable final String text) {
        bottomDescription.setText(text);
    }

    @Override
    public void setUserVisibleHint(final boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            onFocusIn();
        } else {
            onFocusOut();
        }
    }

    @Override
    public void onFocusIn() {
        focused = true;
        if (presenter != null) {
            presenter.onFocusIn();
            executeIfAccessibilityTalkBackEnable(getContext(), () -> {
                updateForAccessibility();
                return null;
            });
        }
    }

    @Override
    public void onFocusOut() {
        focused = false;
        if (presenter != null) {
            presenter.onFocusOut();
            executeIfAccessibilityTalkBackEnable(getContext(), () -> {
                clearForAccessibility();
                return null;
            });
        }
    }

    private void updateForAccessibility() {
        final String description = getAccessibilityContentDescription();

        if (TextUtil.isNotEmpty(description)) {
            setDescriptionForAccessibility(description);
        }

        if (isDisableMethod()) {
            String statusMessage = model.getStatus().getMainMessage().getMessage();
            statusMessage = TextUtil.isNotEmpty(statusMessage) ? statusMessage : TextUtil.EMPTY;
            if (TextUtil.isNotEmpty(statusMessage)) {
                setDescriptionForAccessibility(statusMessage);
            }
        }
    }

    private void setDescriptionForAccessibility(@NonNull final String description) {
        final View rootView = getView();
        final DynamicHeightViewPager parent;
        if (rootView != null && rootView.getParent() instanceof DynamicHeightViewPager &&
            (parent = (DynamicHeightViewPager) rootView.getParent()).hasAccessibilityFocus()) {
            parent.announceForAccessibility(description);
        }
        if (handler != null) {
            handler.postDelayed(() -> card.setContentDescription(description), 800);
        }
    }

    private void clearForAccessibility() {
        card.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED);
        card.setContentDescription(TextUtil.SPACE);
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    public boolean hasFocus() {
        return focused;
    }

    @Override
    public void animateHighlightMessageIn() {
        if (shouldAnimate()) {
            animation.slideUp();
        }
    }

    @Override
    public void animateHighlightMessageOut() {
        if (shouldAnimate()) {
            animation.slideDown();
        }
    }

    private boolean shouldAnimate() {
        return animation != null && TextUtil.isNotEmpty(bottomDescription.getText());
    }

    @Override
    public void disable() {
        final Fragment parentFragment = getParentFragment();
        final DisabledPaymentMethod disabledPaymentMethod = model.getDisabledPaymentMethod();

        if (!(parentFragment instanceof DisabledDetailDialogLauncher)) {
            throw new IllegalStateException(
                "Parent fragment should implement " + DisabledDetailDialogLauncher.class.getSimpleName());
        }
        if (disabledPaymentMethod == null) {
            throw new IllegalStateException(
                "Should have a disabledPaymentMethod to disable");
        }

        card.setOnClickListener(
            v -> DisabledPaymentMethodDetailDialog
                .showDialog(parentFragment, ((DisabledDetailDialogLauncher) parentFragment).getRequestCode(),
                    disabledPaymentMethod.getPaymentStatusDetail(), model.getStatus()));
    }

    @Override
    public void onAction(@NonNull final GenericDialogAction genericDialogAction) {
        //Do nothing
    }

    protected void tintBackground(@NonNull final ImageView background, @NonNull final String color) {
        final int backgroundColor = Color.parseColor(color);

        final int alpha = Color.alpha(backgroundColor);
        final int blue = Color.blue(backgroundColor);
        final int green = Color.green(backgroundColor);
        final int red = Color.red(backgroundColor);

        final int lighterBackgroundColor =
            Color.argb((int) (alpha * 0.7f), (int) (red * 0.8f), (int) (green * 0.8f), (int) (blue * 0.8f));
        Color.argb(0, 0, 0, 0);
        final int[] ints = { backgroundColor, lighterBackgroundColor };
        final GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.BL_TR,
            ints);

        gradientDrawable.setCornerRadius(getResources().getDimensionPixelSize(R.dimen.px_xs_margin));
        gradientDrawable.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        gradientDrawable.setShape(GradientDrawable.RECTANGLE);
        gradientDrawable.setDither(true);

        background.setImageDrawable(gradientDrawable);
    }

    public interface DisabledDetailDialogLauncher {
        default int getRequestCode() {
            return 0;
        }
    }

    public interface PaymentMethodPagerListener {
        void onApplicationChanged(@NonNull final String paymentTypeId);
    }
}
