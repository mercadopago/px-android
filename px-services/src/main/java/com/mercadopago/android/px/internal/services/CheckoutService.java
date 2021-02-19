package com.mercadopago.android.px.internal.services;

import androidx.annotation.Nullable;
import com.mercadopago.android.px.internal.callbacks.MPCall;
import com.mercadopago.android.px.model.PaymentMethod;
import com.mercadopago.android.px.model.PaymentMethodSearch;
import com.mercadopago.android.px.model.internal.CheckoutResponse;
import com.mercadopago.android.px.services.BuildConfig;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface CheckoutService {

    String CHECKOUT_VERSION = "v2";
    String ENVIRONMENT = BuildConfig.API_ENVIRONMENT_NEW;

    @POST("https://run.mocky.io/v3/0dd94609-c590-4fb2-8a41-87ae61b3dd30/" + ENVIRONMENT + "/px_mobile/" + CHECKOUT_VERSION + "/checkout")
    MPCall<CheckoutResponse> checkout(
        @Query("access_token") String privateKey,
        @Body Map<String, Object> body);

    @POST("https://run.mocky.io/v3/0dd94609-c590-4fb2-8a41-87ae61b3dd30/" + ENVIRONMENT + "/px_mobile/" + CHECKOUT_VERSION + "/checkout/{preference_id}")
    MPCall<CheckoutResponse> checkout(
        @Path(value = "preference_id", encoded = true) String preferenceId,
        @Query("access_token") String privateKey,
        @Body Map<String, Object> body);

    /**
     * Old api call version ; used by MercadoPagoServices.
     *
     * @param publicKey
     * @param amount
     * @param excludedPaymentTypes
     * @param excludedPaymentMethods
     * @param siteId
     * @param processingMode
     * @param cardsWithEsc
     * @param differentialPricingId
     * @param defaultInstallments
     * @param expressEnabled
     * @param accessToken
     * @return payment method search
     */
    @GET("{environment}/px_mobile_api/payment_methods?api_version=1.8")
    MPCall<PaymentMethodSearch> getPaymentMethodSearch(
        @Path(value = "environment", encoded = true) String environment,
        @Query("public_key") String publicKey,
        @Query("amount") BigDecimal amount,
        @Query("excluded_payment_types") String excludedPaymentTypes,
        @Query("excluded_payment_methods") String excludedPaymentMethods,
        @Query("site_id") String siteId,
        @Query("processing_mode") String processingMode,
        @Query("cards_esc") String cardsWithEsc,
        @Nullable @Query("differential_pricing_id") Integer differentialPricingId,
        @Nullable @Query("default_installments") final Integer defaultInstallments,
        @Query("express_enabled") final boolean expressEnabled,
        @Nullable @Query("access_token") String accessToken);

    @GET("{environment}/payment_methods")
    MPCall<List<PaymentMethod>> getPaymentMethods(
        @Path(value = "environment", encoded = true) String environment,
        @Query("public_key") String publicKey,
        @Query("access_token") String privateKey);
}