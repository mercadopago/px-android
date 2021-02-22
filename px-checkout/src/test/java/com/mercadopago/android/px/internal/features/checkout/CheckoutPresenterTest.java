package com.mercadopago.android.px.internal.features.checkout;

import androidx.annotation.NonNull;
import com.mercadopago.android.px.internal.experiments.Variant;
import com.mercadopago.android.px.internal.repository.ExperimentsRepository;
import com.mercadopago.android.px.internal.repository.CheckoutRepository;
import com.mercadopago.android.px.internal.repository.PaymentRepository;
import com.mercadopago.android.px.internal.repository.PaymentSettingRepository;
import com.mercadopago.android.px.internal.repository.UserSelectionRepository;
import com.mercadopago.android.px.mocks.CheckoutResponseStub;
import com.mercadopago.android.px.model.BusinessPayment;
import com.mercadopago.android.px.model.Payment;
import com.mercadopago.android.px.model.Site;
import com.mercadopago.android.px.model.Sites;
import com.mercadopago.android.px.model.exceptions.ApiException;
import com.mercadopago.android.px.model.exceptions.MercadoPagoError;
import com.mercadopago.android.px.preferences.CheckoutPreference;
import com.mercadopago.android.px.tracking.internal.MPTracker;
import com.mercadopago.android.px.utils.StubFailMpCall;
import com.mercadopago.android.px.utils.StubSuccessMpCall;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CheckoutPresenterTest {

    public static final int CUSTOM_RESULT_CODE = 1;

    @Mock private Checkout.View checkoutView;
    @Mock private PaymentSettingRepository paymentSettingRepository;
    @Mock private UserSelectionRepository userSelectionRepository;
    @Mock private CheckoutRepository checkoutRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private ExperimentsRepository experimentsRepository;
    @Mock private PostPaymentUrlsMapper postPaymentUrlsMapper;

    private CheckoutPresenter presenter;

    @Before
    public void setUp() {
        final Site site = mock(Site.class);
        when(site.getId()).thenReturn(Sites.ARGENTINA.getId());
        when(paymentSettingRepository.getSite()).thenReturn(site);
        when(paymentSettingRepository.getCheckoutPreference()).thenReturn(mock(CheckoutPreference.class));
        when(postPaymentUrlsMapper.map(any(PostPaymentUrlsMapper.Model.class)))
            .thenReturn(mock(PostPaymentUrlsMapper.Response.class));
        presenter = getPresenter();
    }

    @Test
    public void whenCheckoutInitializedAndPaymentMethodSearchFailsThenShowError() {
        final ApiException apiException = mock(ApiException.class);
        when(checkoutRepository.checkout()).thenReturn(new StubFailMpCall<>(apiException));

        presenter.initialize();

        verify(checkoutView).showProgress();
        verify(checkoutView).showError(any(MercadoPagoError.class));
        verifyNoMoreInteractions(checkoutView);
    }

    @Test
    public void whenChoHasPreferenceAndPaymentMethodRetrievedShowOneTap() {
        when(checkoutRepository.checkout()).thenReturn(new StubSuccessMpCall<>(CheckoutResponseStub.FULL.get()));

        presenter.initialize();

        verify(checkoutRepository).checkout();
        verify(checkoutView).showProgress();
        verify(checkoutView).hideProgress();
        verify(checkoutView).showOneTap(any(Variant.class));
        verifyNoMoreInteractions(checkoutView);
        verifyNoMoreInteractions(checkoutRepository);
    }

    @Test
    public void whenPaymentResultWithCreatedPaymentThenFinishCheckoutWithPaymentResult() {
        final Payment payment = mock(Payment.class);
        when(paymentRepository.getPayment()).thenReturn(payment);

        presenter.onPaymentResultResponse(null, null, null);

        verify(checkoutView).finishWithPaymentResult(null, payment);
    }

    @Test
    public void whenPaymentResultWithoutCreatedPaymentThenFinishCheckoutWithoutPaymentResult() {
        presenter.onPaymentResultResponse(null, null, null);
        verify(checkoutView).finishWithPaymentResult(null, null);
    }

    @Test
    public void whenErrorShownAndValidIdentificationThenCancelCheckout() {
        final ApiException apiException = mock(ApiException.class);
        final MercadoPagoError mpException = new MercadoPagoError(apiException, "");

        presenter.onErrorCancel(mpException);

        verify(checkoutView).cancelCheckout();
        verifyNoMoreInteractions(checkoutView);
    }

    @Test
    public void whenCustomPaymentResultResponseHasPaymentThenFinishWithPaymentResult() {
        final Payment payment = mock(Payment.class);
        when(paymentRepository.getPayment()).thenReturn(payment);

        presenter.onPaymentResultResponse(CUSTOM_RESULT_CODE, null, null);

        verify(checkoutView).finishWithPaymentResult(CUSTOM_RESULT_CODE, payment);
        verifyNoMoreInteractions(checkoutView);
    }

    @Test
    public void whenCustomPaymentResultResponseHasNotPaymentThenFinishWithPaymentResult() {
        presenter.onPaymentResultResponse(CUSTOM_RESULT_CODE, null, null);

        verify(checkoutView).finishWithPaymentResult(CUSTOM_RESULT_CODE, null);
        verifyNoMoreInteractions(checkoutView);
    }

    @Test
    public void whenCustomPaymentResultResponseHasBusinessPaymentThenFinishWithPaymentResult() {
        final BusinessPayment payment = mock(BusinessPayment.class);
        when(paymentRepository.getPayment()).thenReturn(payment);

        presenter.onPaymentResultResponse(CUSTOM_RESULT_CODE, null, null);

        verify(checkoutView).finishWithPaymentResult(CUSTOM_RESULT_CODE, null);
        verifyNoMoreInteractions(checkoutView);
    }

// --------- Helper methods ----------- //

    @NonNull
    private CheckoutPresenter getBasePresenter(final Checkout.View view) {

        presenter = new CheckoutPresenter(paymentSettingRepository, userSelectionRepository,
            checkoutRepository, paymentRepository, experimentsRepository, postPaymentUrlsMapper, mock(MPTracker.class),
            false);

        presenter.attachView(view);
        return presenter;
    }

    @NonNull
    private CheckoutPresenter getPresenter() {
        return getBasePresenter(checkoutView);
    }
}
