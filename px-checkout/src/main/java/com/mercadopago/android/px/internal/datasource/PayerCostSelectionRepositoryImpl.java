package com.mercadopago.android.px.internal.datasource;

import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.mercadopago.android.px.internal.repository.PayerCostSelectionRepository;
import com.mercadopago.android.px.internal.util.JsonUtil;
import com.mercadopago.android.px.model.PayerCost;
import java.util.Map;

public class PayerCostSelectionRepositoryImpl implements PayerCostSelectionRepository {

    private static final String PREF_SELECTED_PAYER_COSTS = "PREF_SELECTED_PAYER_COSTS";

    @NonNull private final SharedPreferences sharedPreferences;
    @Nullable private Map<String, Integer> selectedPayerCosts;

    public PayerCostSelectionRepositoryImpl(@NonNull final SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    @Override
    public int get(@NonNull final String paymentMethodId) {
        final Integer selectedPayerCost = getSelectedPayerCosts().get(paymentMethodId);
        return selectedPayerCost != null ? selectedPayerCost : PayerCost.NO_SELECTED;
    }

    @Override
    public void save(@NonNull final String paymentMethodId, final int selectedPayerCost) {
        final Map<String, Integer> selectedPayerCosts = getSelectedPayerCosts();
        selectedPayerCosts.put(paymentMethodId, selectedPayerCost);
        sharedPreferences.edit().putString(PREF_SELECTED_PAYER_COSTS, JsonUtil.toJson(selectedPayerCosts))
            .apply();
    }

    @Override
    public void reset() {
        sharedPreferences.edit().remove(PREF_SELECTED_PAYER_COSTS).apply();
        selectedPayerCosts = null;
    }

    @NonNull
    private Map<String, Integer> getSelectedPayerCosts() {
        if (selectedPayerCosts == null) {
            final String selectedPayerCostsJson = sharedPreferences.getString(PREF_SELECTED_PAYER_COSTS, null);
            selectedPayerCosts = JsonUtil.getCustomMapFromJson(selectedPayerCostsJson, String.class, Integer.class);
        }
        return selectedPayerCosts;
    }
}