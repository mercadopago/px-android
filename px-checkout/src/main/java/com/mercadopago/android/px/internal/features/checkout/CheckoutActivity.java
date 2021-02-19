package com.mercadopago.android.px.internal.features.checkout;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.mercadolibre.android.cardform.internal.CardFormWithFragment;
import com.mercadolibre.android.cardform.internal.LifecycleListener;
import com.mercadopago.android.px.R;
import com.mercadopago.android.px.core.BackHandler;
import com.mercadopago.android.px.internal.base.PXActivity;
import com.mercadopago.android.px.internal.di.CheckoutConfigurationModule;
import com.mercadopago.android.px.internal.di.MapperProvider;
import com.mercadopago.android.px.internal.di.Session;
import com.mercadopago.android.px.internal.experiments.Variant;
import com.mercadopago.android.px.internal.features.Constants;
import com.mercadopago.android.px.internal.features.express.ExpressPayment;
import com.mercadopago.android.px.internal.features.express.ExpressPaymentFragment;
import com.mercadopago.android.px.internal.features.security_code.SecurityCodeFragment;
import com.mercadopago.android.px.internal.repository.PaymentSettingRepository;
import com.mercadopago.android.px.internal.util.ErrorUtil;
import com.mercadopago.android.px.internal.util.FragmentUtil;
import com.mercadopago.android.px.internal.util.MercadoPagoUtil;
import com.mercadopago.android.px.model.Payment;
import com.mercadopago.android.px.model.exceptions.MercadoPagoError;

import static com.mercadopago.android.px.core.MercadoPagoCheckout.EXTRA_ERROR;
import static com.mercadopago.android.px.core.MercadoPagoCheckout.EXTRA_PAYMENT_RESULT;
import static com.mercadopago.android.px.core.MercadoPagoCheckout.PAYMENT_RESULT_CODE;
import static com.mercadopago.android.px.internal.features.Constants.RESULT_CUSTOM_EXIT;
import static com.mercadopago.android.px.internal.features.payment_result.PaymentResultActivity.EXTRA_RESULT_CODE;
import static com.mercadopago.android.px.internal.util.ErrorUtil.isErrorResult;
import static com.mercadopago.android.px.model.ExitAction.EXTRA_CLIENT_RES_CODE;

public class CheckoutActivity extends PXActivity<CheckoutPresenter>
    implements Checkout.View, ExpressPaymentFragment.CallBack, LifecycleListener {

    private static final String ARGS_WITH_PREFETCH = "args_with_prefetch";
    private static final String EXTRA_PRIVATE_KEY = "extra_private_key";
    private static final String EXTRA_PUBLIC_KEY = "extra_public_key";
    private static final String TAG_ONETAP_FRAGMENT = "TAG_ONETAP";

    private CheckoutPresenter presenter;

    private String merchantPublicKey;
    @Nullable private String privateKey;
    private Intent customDataBundle;
    private View progress;

    public static Intent getIntent(@NonNull final Context context, final boolean withPrefetch) {
        return new Intent(context, CheckoutActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(ARGS_WITH_PREFETCH, withPrefetch);
    }

    @Override
    protected void onHalted() {
        presenter.onHalted();
    }

    @Override
    protected void onCreated(@Nullable final Bundle savedInstanceState) {
        setContentView(R.layout.px_activity_checkout);
        progress = findViewById(R.id.mpsdkProgressLayout);
        if (savedInstanceState == null) {
            initPresenter();
        }
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        if (intent.getData() != null) {
            final ExpressPayment.View fragment =
                (ExpressPayment.View) getSupportFragmentManager().findFragmentByTag(TAG_ONETAP_FRAGMENT);
            if (fragment != null) {
                fragment.onDeepLinkReceived();
            }
        } else {
            FragmentUtil.tryRemoveNow(getSupportFragmentManager(), TAG_ONETAP_FRAGMENT);
            // if onNewIntent is called, means that we are initialized twice, so we need to detach previews presenter
            if (presenter != null) {
                presenter.detachView();
            }
            initPresenter();
        }
    }

    private void initPresenter() {
        presenter = getActivityParameters();
        presenter.attachView(this);
        presenter.initialize();
    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(EXTRA_PRIVATE_KEY, privateKey);
        outState.putString(EXTRA_PUBLIC_KEY, merchantPublicKey);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull final Bundle savedInstanceState) {
        final Session session = Session.getInstance();
        final CheckoutConfigurationModule configurationModule = session.getConfigurationModule();

        presenter =
            new CheckoutPresenter(configurationModule.getPaymentSettings(),
                configurationModule.getUserSelectionRepository(),
                session.getCheckoutRepository(),
                session.getPaymentRepository(),
                session.getExperimentsRepository(),
                MapperProvider.INSTANCE.getPostPaymentUrlsMapper(),
                session.getTracker(),
                getIntent().getBooleanExtra(ARGS_WITH_PREFETCH, false));

        privateKey = savedInstanceState.getString(EXTRA_PRIVATE_KEY);
        merchantPublicKey = savedInstanceState.getString(EXTRA_PUBLIC_KEY);
        presenter.attachView(this);
        presenter.onRestore();
    }

    @Override
    public void onBackPressed() {
        final FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager != null) {
            final int backStackEntryCount = fragmentManager.getBackStackEntryCount();

            final Fragment fragment = fragmentManager.findFragmentByTag(CardFormWithFragment.TAG);
            if (fragment != null && fragment.getChildFragmentManager().getBackStackEntryCount() > 0) {
                fragment.getChildFragmentManager().popBackStack();
                return;
            }

            if (earlyExitFromBackHandler(fragmentManager.findFragmentByTag(SecurityCodeFragment.TAG))) {
                return;
            }

            if (earlyExitFromBackHandler(fragmentManager.findFragmentByTag(TAG_ONETAP_FRAGMENT))) {
                return;
            }

            if (backStackEntryCount > 0) {
                fragmentManager.popBackStack();
                return;
            }

            super.onBackPressed();
        }
    }

    private boolean earlyExitFromBackHandler(@Nullable final Fragment fragment) {
        if (fragment instanceof BackHandler && fragment.isAdded()) {
            return ((BackHandler) fragment).handleBack();
        }
        return false;
    }

    protected CheckoutPresenter getActivityParameters() {
        final Session session = Session.getInstance();
        final CheckoutConfigurationModule configurationModule = session.getConfigurationModule();
        final PaymentSettingRepository configuration = configurationModule.getPaymentSettings();

        privateKey = configuration.getPrivateKey();

        merchantPublicKey = configuration.getPublicKey();

        return new CheckoutPresenter(configuration,
            configurationModule.getUserSelectionRepository(),
            session.getCheckoutRepository(),
            session.getPaymentRepository(),
            session.getExperimentsRepository(),
            MapperProvider.INSTANCE.getPostPaymentUrlsMapper(),
            session.getTracker(),
            getIntent().getBooleanExtra(ARGS_WITH_PREFETCH, false)
        );
    }

    @Override
    public void showOneTap(@NonNull final Variant variant) {
        final FragmentManager supportFragmentManager = getSupportFragmentManager();
        if (supportFragmentManager.findFragmentByTag(TAG_ONETAP_FRAGMENT) == null) {
            supportFragmentManager
                .beginTransaction()
                .setCustomAnimations(R.anim.px_slide_right_to_left_in, R.anim.px_slide_right_to_left_out)
                .replace(R.id.one_tap_fragment, ExpressPaymentFragment.getInstance(variant), TAG_ONETAP_FRAGMENT)
                .commitNowAllowingStateLoss();
        }
    }

    @Override
    public void hideProgress() {
        progress.setVisibility(View.GONE);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ErrorUtil.ERROR_REQUEST_CODE) {
            resolveErrorRequest(resultCode, data);
        } else if (resultCode == RESULT_CUSTOM_EXIT) {
            handleCustomExit(data);
        }
    }

    private void handleCustomExit(final Intent data) {
        if (data != null) {
            final String backUrl = data.getStringExtra(Constants.EXTRA_BACK_URL);
            final String redirectUrl = data.getStringExtra(Constants.EXTRA_REDIRECT_URL);

            if (data.hasExtra(EXTRA_CLIENT_RES_CODE)) {
                //Business custom exit
                final int resCode = data.getIntExtra(EXTRA_CLIENT_RES_CODE, RESULT_OK);
                presenter.onPaymentResultResponse(resCode, backUrl, redirectUrl);
            } else if (data != null && data.hasExtra(EXTRA_RESULT_CODE)) {
                //Custom exit  - Result screen.
                final Integer finalResultCode = data.getIntExtra(EXTRA_RESULT_CODE, PAYMENT_RESULT_CODE);
                customDataBundle = data;
                presenter.onPaymentResultResponse(finalResultCode, backUrl, redirectUrl);
            } else {
                //Normal exit - Result screen.
                presenter.onPaymentResultResponse(null, backUrl, redirectUrl);
            }
        } else {
            //Normal exit - Result screen.
            presenter.onPaymentResultResponse(null, null, null);
        }
    }

    private void resolveErrorRequest(final int resultCode, final Intent data) {
        if (resultCode == RESULT_OK) {
            presenter.recoverFromFailure();
        } else {
            final MercadoPagoError mercadoPagoError =
                isErrorResult(data) ? (MercadoPagoError) data.getSerializableExtra(EXTRA_ERROR) : null;
            presenter.onErrorCancel(mercadoPagoError);
        }
    }

    @Override
    public void showError(final MercadoPagoError error) {
        ErrorUtil.startErrorActivity(this, error);
    }

    @Override
    public void showProgress() {
        progress.setVisibility(View.VISIBLE);
    }

    @Override
    public void goToLink(@NonNull final String link) {
        final Intent intent = MercadoPagoUtil.getIntent(this, link);
        if (intent != null) {
            startActivity(intent);
        }
    }

    @Override
    public void openInWebView(@NonNull final String link) {
        final Intent intent = MercadoPagoUtil.getNativeOrWebViewIntent(this, link);
        if (intent != null) {
            startActivity(intent);
        }
    }

    @Override
    public void finishWithPaymentResult(@Nullable final Integer resultCode, @Nullable final Payment payment) {
        int defaultResultCode = RESULT_OK;
        final Intent intent = new Intent();
        if (customDataBundle != null) {
            intent.putExtras(customDataBundle);
        }
        if (payment != null) {
            defaultResultCode = PAYMENT_RESULT_CODE;
            intent.putExtra(EXTRA_PAYMENT_RESULT, payment);
        }
        setResult(resultCode != null ? resultCode : defaultResultCode, intent);
        finish();
        overrideTransitionOut();
    }

    @Override
    public void onOneTapCanceled() {
        cancelCheckout();
    }

    @Override
    public void cancelCheckout() {
        setResult(RESULT_CANCELED);
        finish();
        overrideTransitionOut();
    }

    @Override
    public void onCardAdded(@NonNull final String cardId, @NonNull final LifecycleListener.Callback callback) {
        presenter.onCardAdded(cardId, callback);
    }
}
