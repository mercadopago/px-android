package com.mercadopago.android.px.internal.datasource;

import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.mercadopago.android.px.configuration.AdvancedConfiguration;
import com.mercadopago.android.px.configuration.CustomStringConfiguration;
import com.mercadopago.android.px.configuration.DiscountParamsConfiguration;
import com.mercadopago.android.px.configuration.PaymentConfiguration;
import com.mercadopago.android.px.internal.core.FileManager;
import com.mercadopago.android.px.internal.model.SecurityType;
import com.mercadopago.android.px.internal.repository.PaymentSettingRepository;
import com.mercadopago.android.px.internal.util.JsonUtil;
import com.mercadopago.android.px.model.Currency;
import com.mercadopago.android.px.model.Site;
import com.mercadopago.android.px.model.Token;
import com.mercadopago.android.px.model.commission.PaymentTypeChargeRule;
import com.mercadopago.android.px.model.internal.Configuration;
import com.mercadopago.android.px.preferences.CheckoutPreference;
import java.io.File;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class PaymentSettingService implements PaymentSettingRepository {

    private static final String PREF_CHECKOUT_PREF = "PREF_CHECKOUT_PREFERENCE";
    private static final String PREF_CHECKOUT_PREF_ID = "PREF_CHECKOUT_PREFERENCE_ID";
    private static final String PREF_PUBLIC_KEY = "PREF_PUBLIC_KEY";
    private static final String PREF_SITE = "PREF_SITE";
    private static final String PREF_CURRENCY = "PREF_CURRENCY";
    private static final String PREF_PRIVATE_KEY = "PREF_PRIVATE_KEY";
    private static final String PREF_TOKEN = "PREF_TOKEN";
    private static final String PREF_SECURITY_TYPE = "PREF_SECURITY_TYPE";
    private static final String PREF_PRODUCT_ID = "PREF_PRODUCT_ID";
    private static final String PREF_LABELS = "PREF_LABELS";
    private static final String PREF_DISCOUNT_PARAMS = "PREF_DISCOUNT_PARAMS";
    private static final String PREF_ONE_TAP_ENABLED = "PREF_ONE_TAP_ENABLED";
    private static final String PREF_CUSTOM_STRINGS = "PREF_CUSTOM_STRINGS";
    private static final String PREF_CONFIGURATION = "PREF_CONFIGURATION";
    private static final String FILE_PAYMENT_CONFIG = "px_payment_config";

    @NonNull private final SharedPreferences sharedPreferences;
    @NonNull private final FileManager fileManager;

    @Nullable private CheckoutPreference pref;
    private PaymentConfiguration paymentConfiguration;
    private AdvancedConfiguration advancedConfiguration;
    @NonNull private final File paymentConfigFile;

    public PaymentSettingService(@NonNull final SharedPreferences sharedPreferences,
        @NonNull final FileManager fileManager) {
        this.sharedPreferences = sharedPreferences;
        this.fileManager = fileManager;
        paymentConfigFile = fileManager.create(FILE_PAYMENT_CONFIG);
    }

    @Override
    public void reset() {
        final SharedPreferences.Editor edit = sharedPreferences.edit();
        edit.clear().apply();
        fileManager.removeFile(paymentConfigFile);
        pref = null;
        paymentConfiguration = null;
        advancedConfiguration = null;
    }

    @Override
    public void configure(@NonNull final Configuration configuration) {
        final SharedPreferences.Editor edit = sharedPreferences.edit();
        edit.putString(PREF_CONFIGURATION, JsonUtil.toJson(configuration));
        edit.apply();
    }

    @Override
    public void configure(@NonNull final Token token) {
        final SharedPreferences.Editor edit = sharedPreferences.edit();
        edit.putString(PREF_TOKEN, JsonUtil.toJson(token));
        edit.apply();
    }

    @Override
    public void configure(@NonNull final SecurityType secondFactor) {
        final SharedPreferences.Editor edit = sharedPreferences.edit();
        edit.putString(PREF_SECURITY_TYPE, secondFactor.name());
        edit.apply();
    }

    @Override
    public void clearToken() {
        sharedPreferences.edit().remove(PREF_TOKEN).apply();
    }

    @Override
    public void configurePreferenceId(@Nullable final String preferenceId) {
        final SharedPreferences.Editor edit = sharedPreferences.edit();
        edit.putString(PREF_CHECKOUT_PREF_ID, preferenceId).apply();
    }

    @Override
    public void configure(@NonNull final AdvancedConfiguration advancedConfiguration) {
        final SharedPreferences.Editor edit = sharedPreferences.edit();
        edit.putString(PREF_PRODUCT_ID, advancedConfiguration.getDiscountParamsConfiguration().getProductId());
        edit.putStringSet(PREF_LABELS, advancedConfiguration.getDiscountParamsConfiguration().getLabels());
        edit.putString(PREF_CUSTOM_STRINGS, JsonUtil.toJson(advancedConfiguration.getCustomStringConfiguration()));
        edit.putString(PREF_DISCOUNT_PARAMS,
            JsonUtil.toJson(advancedConfiguration.getDiscountParamsConfiguration().getAdditionalParams()));
        edit.putBoolean(PREF_ONE_TAP_ENABLED, advancedConfiguration.isExpressPaymentEnabled()).apply();

        this.advancedConfiguration = advancedConfiguration;
    }

    @Override
    public void configure(@NonNull final String publicKey) {
        final SharedPreferences.Editor edit = sharedPreferences.edit();
        edit.putString(PREF_PUBLIC_KEY, publicKey);
        edit.apply();
    }

    @Override
    public void configure(@NonNull final Site site) {
        final SharedPreferences.Editor edit = sharedPreferences.edit();
        edit.putString(PREF_SITE, JsonUtil.toJson(site));
        edit.apply();
    }

    @Override
    public void configure(@NonNull final Currency currency) {
        final SharedPreferences.Editor edit = sharedPreferences.edit();
        edit.putString(PREF_CURRENCY, JsonUtil.toJson(currency));
        edit.apply();
    }

    @Override
    public void configurePrivateKey(@Nullable final String privateKey) {
        final SharedPreferences.Editor edit = sharedPreferences.edit();
        edit.putString(PREF_PRIVATE_KEY, privateKey);
        edit.apply();
    }

    @Override
    public void configure(@NonNull final PaymentConfiguration paymentConfiguration) {
        this.paymentConfiguration = paymentConfiguration;
        fileManager.writeToFile(paymentConfigFile, paymentConfiguration);
    }

    @Override
    public void configure(@Nullable final CheckoutPreference checkoutPreference) {
        final SharedPreferences.Editor edit = sharedPreferences.edit();
        if (checkoutPreference == null) {
            edit.remove(PREF_CHECKOUT_PREF).apply();
        } else {
            edit.putString(PREF_CHECKOUT_PREF, JsonUtil.toJson(checkoutPreference));
            edit.apply();
        }
        pref = checkoutPreference;
    }

    @NonNull
    @Override
    public List<PaymentTypeChargeRule> chargeRules() {
        return getPaymentConfiguration().getCharges();
    }

    @NonNull
    @Override
    public PaymentConfiguration getPaymentConfiguration() {
        if (paymentConfiguration == null) {
            paymentConfiguration = fileManager.readParcelable(paymentConfigFile, PaymentConfiguration.CREATOR);
        }
        return paymentConfiguration;
    }

    @Nullable
    @Override
    public CheckoutPreference getCheckoutPreference() {
        if (pref == null) {
            pref = JsonUtil.fromJson(sharedPreferences.getString(PREF_CHECKOUT_PREF, ""), CheckoutPreference.class);
        }
        return pref;
    }

    @Nullable
    @Override
    public String getCheckoutPreferenceId() {
        return sharedPreferences.getString(PREF_CHECKOUT_PREF_ID, null);
    }

    @NonNull
    @Override
    public String getPublicKey() {
        return sharedPreferences.getString(PREF_PUBLIC_KEY, "");
    }

    @NonNull
    @Override
    public Site getSite() {
        final Site site = JsonUtil.fromJson(sharedPreferences.getString(PREF_SITE, null), Site.class);
        if (site == null) {
            throw new IllegalStateException("Unable to retrieve site from storage");
        }
        return site;
    }

    @NonNull
    @Override
    public Currency getCurrency() {
        final Currency currency = JsonUtil.fromJson(sharedPreferences.getString(PREF_CURRENCY, null), Currency.class);
        if (currency == null) {
            throw new IllegalStateException("Unable to retrieve currency from storage");
        }
        return currency;
    }

    @NonNull
    @Override
    public Configuration getConfiguration() {
        final Configuration configuration =
            JsonUtil.fromJson(sharedPreferences.getString(PREF_CONFIGURATION, null), Configuration.class);
        if (configuration == null) {
            throw new IllegalStateException("Unable to retrieve configuration from storage");
        }
        return configuration;
    }

    @Nullable
    @Override
    public Token getToken() {
        return JsonUtil.fromJson(sharedPreferences.getString(PREF_TOKEN, ""), Token.class);
    }

    @NonNull
    @Override
    public SecurityType getSecurityType() {
        return SecurityType.valueOf(sharedPreferences.getString(PREF_SECURITY_TYPE, SecurityType.NONE.name()));
    }

    @Override
    public boolean hasToken() {
        return getToken() != null;
    }

    @NonNull
    @Override
    public String getTransactionId() {
        return String.format(Locale.getDefault(), "%s%d", getPublicKey(), Calendar.getInstance().getTimeInMillis());
    }

    @NonNull
    @Override
    public AdvancedConfiguration getAdvancedConfiguration() {
        if (advancedConfiguration == null) {
            return new AdvancedConfiguration.Builder()
                .setExpressPaymentEnable(sharedPreferences.getBoolean(PREF_ONE_TAP_ENABLED, false))
                .setDiscountParamsConfiguration(new DiscountParamsConfiguration.Builder()
                    .setProductId(sharedPreferences.getString(PREF_PRODUCT_ID, ""))
                    .addAdditionalParams(
                        JsonUtil.getStringMapFromJson(sharedPreferences.getString(PREF_DISCOUNT_PARAMS, "")))
                    .setLabels(sharedPreferences.getStringSet(PREF_LABELS, Collections.emptySet())).build())
                .setCustomStringConfiguration(JsonUtil.fromJson(
                    sharedPreferences.getString(PREF_CUSTOM_STRINGS, null), CustomStringConfiguration.class))
                .build();
        }
        return advancedConfiguration;
    }

    @Nullable
    @Override
    public String getPrivateKey() {
        return sharedPreferences.getString(PREF_PRIVATE_KEY, null);
    }
}