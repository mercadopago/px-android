package com.mercadopago.android.px.internal.datasource;

import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import com.mercadopago.android.px.internal.repository.DisabledPaymentMethodRepository;
import com.mercadopago.android.px.internal.util.JsonUtil;
import com.mercadopago.android.px.internal.util.TextUtil;
import com.mercadopago.android.px.model.Payment;
import com.mercadopago.android.px.model.PaymentResult;
import com.mercadopago.android.px.model.PaymentTypes;
import com.mercadopago.android.px.model.internal.DisabledPaymentMethod;
import java.util.Collection;
import java.util.Map;

import static com.mercadopago.android.px.internal.util.TextUtil.isNotEmpty;

public final class DisabledPaymentMethodService implements DisabledPaymentMethodRepository {

    private static final String PREF_DISABLED_PAYMENT_METHODS = "PREF_DISABLED_PAYMENT_METHODS";

    @NonNull private final SharedPreferences sharedPreferences;
    private Map<String, DisabledPaymentMethod> disabledPaymentMethods;

    public DisabledPaymentMethodService(@NonNull final SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    private void storeDisabledPaymentMethodId(@NonNull final String paymentMethodId,
        @NonNull final String paymentStatusDetail) {
        final Map<String, DisabledPaymentMethod> disabledPaymentMethods = getDisabledPaymentMethods();
        disabledPaymentMethods.put(paymentMethodId, new DisabledPaymentMethod(paymentMethodId, paymentStatusDetail));
        sharedPreferences.edit().putString(PREF_DISABLED_PAYMENT_METHODS, JsonUtil.toJson(disabledPaymentMethods))
            .apply();
    }

    @Override
    public void storeDisabledPaymentMethodsIds(@NonNull final Collection<String> paymentMethodsIds) {
        final Map<String, DisabledPaymentMethod> disabledPaymentMethods = getDisabledPaymentMethods();
        for (final String paymentMethodId : paymentMethodsIds) {
            disabledPaymentMethods.put(paymentMethodId, new DisabledPaymentMethod(paymentMethodId));
        }
        sharedPreferences.edit().putString(PREF_DISABLED_PAYMENT_METHODS, JsonUtil.toJson(disabledPaymentMethods))
            .apply();
    }

    @NonNull
    @Override
    public Map<String, DisabledPaymentMethod> getDisabledPaymentMethods() {
        if (disabledPaymentMethods == null) {
            final String disabledPaymentMethodsJson =
                sharedPreferences.getString(PREF_DISABLED_PAYMENT_METHODS, null);
            disabledPaymentMethods =
                JsonUtil.getCustomMapFromJson(disabledPaymentMethodsJson, String.class, DisabledPaymentMethod.class);
        }
        return disabledPaymentMethods;
    }

    @Override
    public DisabledPaymentMethod getDisabledPaymentMethod(@NonNull final String paymentMethodId) {
        return getDisabledPaymentMethods().get(paymentMethodId);
    }

    @Override
    public void reset() {
        disabledPaymentMethods = null;
        sharedPreferences.edit().remove(PREF_DISABLED_PAYMENT_METHODS).apply();
    }

    @Override
    public boolean hasPaymentMethodId(@NonNull final String paymentMethodId) {
        return getDisabledPaymentMethods().containsKey(paymentMethodId);
    }

    @Override
    public void handleDisableablePayment(@NonNull final PaymentResult paymentResult) {
        if (isDisableablePayment(paymentResult)) {
            final boolean isSplitPayment = paymentResult.getPaymentDataList().size() > 1;
            final String paymentMethodId;
            final String paymentTypeId = paymentResult.getPaymentData().getPaymentMethod().getPaymentTypeId();

            if (isSplitPayment && TextUtil.isNotEmpty(paymentResult.getPaymentMethodId())) {
                paymentMethodId = paymentResult.getPaymentMethodId();
            } else if (
                PaymentTypes.isCardPaymentType(paymentResult.getPaymentData().getPaymentMethod().getPaymentTypeId()) &&
                    paymentResult.getPaymentData().getToken() != null &&
                    TextUtil.isNotEmpty(paymentResult.getPaymentData().getToken().getCardId())) {
                paymentMethodId = paymentResult.getPaymentData().getToken().getCardId();
            } else {
                paymentMethodId = paymentResult.getPaymentData().getPaymentMethod().getId();
            }

            if (!PaymentTypes.isCardPaymentType(paymentTypeId) ||
                isNotEmpty(paymentResult.getPaymentData().getToken().getCardId())) {
                storeDisabledPaymentMethodId(paymentMethodId, paymentResult.getPaymentStatusDetail());
            }
        }
    }

    private boolean isDisableablePayment(@NonNull final PaymentResult paymentResult) {
        return Payment.StatusCodes.STATUS_REJECTED.equalsIgnoreCase(paymentResult.getPaymentStatus()) &&
            (Payment.StatusDetail.STATUS_DETAIL_REJECTED_HIGH_RISK
                .equalsIgnoreCase(paymentResult.getPaymentStatusDetail()) ||
                Payment.StatusDetail.STATUS_DETAIL_CC_REJECTED_HIGH_RISK
                    .equalsIgnoreCase(paymentResult.getPaymentStatusDetail()) ||
                Payment.StatusDetail.STATUS_DETAIL_CC_REJECTED_BLACKLIST
                    .equalsIgnoreCase(paymentResult.getPaymentStatusDetail()) ||
                Payment.StatusDetail.STATUS_DETAIL_CC_REJECTED_INSUFFICIENT_AMOUNT
                    .equalsIgnoreCase(paymentResult.getPaymentStatusDetail()));
    }
}