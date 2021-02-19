package com.mercadopago.android.px.mocks;

import androidx.annotation.NonNull;
import com.mercadopago.android.px.internal.util.JsonUtil;
import com.mercadopago.android.px.model.internal.CheckoutResponse;
import com.mercadopago.android.px.utils.ResourcesUtil;
import java.util.Arrays;

public enum CheckoutResponseStub implements JsonStub<CheckoutResponse> {
    FULL(SiteStub.MLA,
        CurrencyStub.MLA,
        CheckoutPreferenceStub.DEFAULT,
        PaymentMethodStub.values(),
        PaymentMethodSearchItemStub.values(),
        CustomSearchItemStub.values(),
        ExpressMetadataStub.values()),

    NO_CUSTOM_OPTIONS(SiteStub.MLA,
        CurrencyStub.MLA,
        CheckoutPreferenceStub.DEFAULT,
        PaymentMethodStub.values(),
        PaymentMethodSearchItemStub.values(),
        new CustomSearchItemStub[0],
        ExpressMetadataStub.values()),

    CREDIT_CARD_AND_PAGOFACIL(SiteStub.MLA,
        CurrencyStub.MLA,
        CheckoutPreferenceStub.DEFAULT,
        PaymentMethodStub.values(),
        PaymentMethodSearchItemStub.CREDIT_CARD_AND_PAGO_FACIL,
        CustomSearchItemStub.values(),
        ExpressMetadataStub.values()),

    ONLY_TICKET_MLA(SiteStub.MLA,
        CurrencyStub.MLA,
        CheckoutPreferenceStub.DEFAULT,
        PaymentMethodStub.MEDIOS_OFF_MLA,
        PaymentMethodSearchItemStub.ONLY_TICKETS_MLA,
        new CustomSearchItemStub[0],
        new ExpressMetadataStub[0]),

    ONLY_TICKET_AND_AM_MLA(SiteStub.MLA,
        CurrencyStub.MLA,
        CheckoutPreferenceStub.DEFAULT,
        PaymentMethodStub.MEDIOS_OFF_MLA,
        PaymentMethodSearchItemStub.ONLY_TICKETS_MLA,
        CustomSearchItemStub.ONLY_ACCOUNT_MONEY,
        new ExpressMetadataStub[0]),

    ONLY_ACCOUNT_MONEY_MLA(SiteStub.MLA,
        CurrencyStub.MLA,
        CheckoutPreferenceStub.DEFAULT,
        new PaymentMethodStub[0],
        new PaymentMethodSearchItemStub[0],
        CustomSearchItemStub.ONLY_ACCOUNT_MONEY,
        new ExpressMetadataStub[0]),

    ONLY_NEW_CREDIT_CARD(SiteStub.MLA,
        CurrencyStub.MLA,
        CheckoutPreferenceStub.DEFAULT,
        PaymentMethodStub.ONLY_CREDIT,
        PaymentMethodSearchItemStub.ONLY_CREDIT_CARD,
        new CustomSearchItemStub[0],
        new ExpressMetadataStub[0]),

    ONLY_BOLBRADESCO_MLB(SiteStub.MLB,
        CurrencyStub.MLB,
        CheckoutPreferenceStub.DEFAULT,
        PaymentMethodStub.MEDIOS_OFF_MLB,
        PaymentMethodSearchItemStub.ONLY_BOLBRADESCO_MLB,
        new CustomSearchItemStub[0],
        new ExpressMetadataStub[0]),

    ONE_GROUP_ONE_PPM(SiteStub.MLA,
        CurrencyStub.MLA,
        CheckoutPreferenceStub.DEFAULT,
        PaymentMethodStub.values(),
        PaymentMethodSearchItemStub.ONLY_CREDIT_CARD,
        CustomSearchItemStub.ONLY_ACCOUNT_MONEY,
        ExpressMetadataStub.values());

    @NonNull private final String json;

    @SuppressWarnings("TypeMayBeWeakened")
    CheckoutResponseStub(@NonNull final SiteStub siteStub,
        @NonNull final CurrencyStub currencyStub,
        @NonNull final CheckoutPreferenceStub checkoutPreferenceStub,
        @NonNull final PaymentMethodStub[] paymentMethodStubs,
        @NonNull final PaymentMethodSearchItemStub[] paymentMethodSearchItemStubs,
        @NonNull final CustomSearchItemStub[] customSearchItemStubs,
        @NonNull final ExpressMetadataStub[] expressMetadataStubs) {

        final StringBuilder jsonContainer =
            new StringBuilder(ResourcesUtil.getStringResource("init_response_template.json"));

        siteStub.inject(jsonContainer);
        currencyStub.inject(jsonContainer);
        checkoutPreferenceStub.inject(jsonContainer);
        ListJsonInjector.injectAll(Arrays.asList(paymentMethodStubs).iterator(), jsonContainer);
        ListJsonInjector.injectAll(Arrays.asList(paymentMethodSearchItemStubs).iterator(), jsonContainer);
        ListJsonInjector.injectAll(Arrays.asList(customSearchItemStubs).iterator(), jsonContainer);
        ListJsonInjector.injectAll(Arrays.asList(expressMetadataStubs).iterator(), jsonContainer);

        final String dirtyJson = jsonContainer.toString();
        json = dirtyJson.replaceAll("%(.*?)%", "");
    }

    @NonNull
    @Override
    public CheckoutResponse get() {
        return JsonUtil.fromJson(json, CheckoutResponse.class);
    }

    @NonNull
    @Override
    public String getJson() {
        return json;
    }

    @NonNull
    @Override
    public String getType() {
        return "%INIT_RESPONSE%";
    }
}