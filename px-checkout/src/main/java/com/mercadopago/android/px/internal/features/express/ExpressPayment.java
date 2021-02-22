package com.mercadopago.android.px.internal.features.express;

import androidx.annotation.NonNull;
import com.mercadolibre.android.cardform.internal.LifecycleListener;
import com.mercadopago.android.px.core.DynamicDialogCreator;
import com.mercadopago.android.px.internal.base.MvpView;
import com.mercadopago.android.px.internal.experiments.Variant;
import com.mercadopago.android.px.internal.features.express.installments.InstallmentRowHolder;
import com.mercadopago.android.px.internal.features.express.slider.HubAdapter;
import com.mercadopago.android.px.internal.features.generic_modal.ActionType;
import com.mercadopago.android.px.internal.features.generic_modal.GenericDialogItem;
import com.mercadopago.android.px.internal.features.pay_button.PayButton;
import com.mercadopago.android.px.internal.util.CardFormWrapper;
import com.mercadopago.android.px.internal.view.ElementDescriptorView;
import com.mercadopago.android.px.internal.viewmodel.PostPaymentAction;
import com.mercadopago.android.px.internal.viewmodel.SplitSelectionState;
import com.mercadopago.android.px.internal.viewmodel.drawables.DrawableFragmentItem;
import com.mercadopago.android.px.model.Currency;
import com.mercadopago.android.px.model.DiscountConfigurationModel;
import com.mercadopago.android.px.model.PayerCost;
import com.mercadopago.android.px.model.Site;
import com.mercadopago.android.px.model.StatusMetadata;
import com.mercadopago.android.px.model.exceptions.MercadoPagoError;
import com.mercadopago.android.px.model.internal.DisabledPaymentMethod;
import com.mercadopago.android.px.model.internal.PaymentConfiguration;
import java.util.List;

public interface ExpressPayment {

    interface View extends MvpView {

        void configurePayButton(@NonNull PayButton.StateChange listener);

        void clearAdapters();

        void configureRenderMode(@NonNull List<Variant> variants);

        void configureAdapters(@NonNull final Site site, @NonNull final Currency currency);

        void updateAdapters(@NonNull HubAdapter.Model model);

        void updatePaymentMethods(@NonNull List<DrawableFragmentItem> items);

        void cancel();

        void updateViewForPosition(final int paymentMethodIndex,
            final int payerCostSelected,
            @NonNull final SplitSelectionState splitSelectionState);

        void updateViewForPaymentMethodType(@NonNull final String paymentMethodType,
            final int payerCostSelected,
            @NonNull final SplitSelectionState splitSelectionState);

        void updateInstallmentsList(final int selectedIndex, @NonNull List<InstallmentRowHolder.Model> models);

        void animateInstallmentsList();

        void showToolbarElementDescriptor(@NonNull final ElementDescriptorView.Model elementDescriptorModel);

        void collapseInstallmentsSelection();

        void showDiscountDetailDialog(@NonNull final Currency currency,
            @NonNull final DiscountConfigurationModel discountModel);

        void showDisabledPaymentMethodDetailDialog(@NonNull final DisabledPaymentMethod disabledPaymentMethod,
            @NonNull final StatusMetadata currentStatus);

        void setPagerIndex(final int index);

        void showDynamicDialog(@NonNull final DynamicDialogCreator creatorFor,
            @NonNull final DynamicDialogCreator.CheckoutData checkoutData);

        void showOfflineMethodsExpanded();

        void showOfflineMethodsCollapsed();

        void showGenericDialog(@NonNull GenericDialogItem item);

        void startAddNewCardFlow(
            final CardFormWrapper cardFormWrapper);

        void startDeepLink(@NonNull String deepLink);

        void onDeepLinkReceived();

        void showLoading();

        void hideLoading();

        void configurePaymentMethodHeader(@NonNull final List<Variant> variant);

        void showError(@NonNull final MercadoPagoError mercadoPagoError);
    }

    interface Actions {

        void onFreshStart();

        void cancel();

        void onBack();

        void loadViewModel();

        void onInstallmentsRowPressed();

        void updateInstallments();

        void onInstallmentSelectionCanceled();

        void onSliderOptionSelected(final int paymentMethodIndex);

        void onPayerCostSelected(final PayerCost payerCostSelected);

        void onPaymentMethodTypeChanged(@NonNull final String paymentMethodType);

        void onSplitChanged(boolean isChecked);

        void onHeaderClicked();

        void onOtherPaymentMethodClicked();

        void handlePrePaymentAction(@NonNull final PayButton.OnReadyForPaymentCallback callback);

        void handleGenericDialogAction(@NonNull final ActionType type);

        void onPaymentExecuted(@NonNull final PaymentConfiguration paymentConfiguration);

        void handleDeepLink();

        void onPostPaymentAction(@NonNull final PostPaymentAction postPaymentAction);

        void onCardAdded(@NonNull final String cardId, @NonNull final LifecycleListener.Callback callback);

        void onCardFormResult();
    }

    enum NavigationState {
        NONE, CARD_FORM, SECURITY_CODE
    }
}
