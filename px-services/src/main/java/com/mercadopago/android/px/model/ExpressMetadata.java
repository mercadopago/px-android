package com.mercadopago.android.px.model;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import com.mercadopago.android.px.model.internal.ExpressPaymentMethod;
import com.mercadopago.android.px.model.one_tap.CheckoutBehaviour;
import com.mercadopago.android.px.model.one_tap.SliderDisplayInfo;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class ExpressMetadata implements Parcelable, Serializable, ExpressPaymentMethod {

    private final String paymentMethodId;
    private final String paymentTypeId;
    private final CardMetadata card;
    private final AccountMoneyMetadata accountMoney;
    private final ConsumerCreditsMetadata consumerCredits;
    private final NewCardMetadata newCard;
    private final StatusMetadata status;
    private final OfflinePaymentTypesMetadata offlineMethods;
    private final BenefitsMetadata benefits;
    private final SliderDisplayInfo displayInfo;
    private final Map<String, CheckoutBehaviour> behaviours;

    public static final Creator<ExpressMetadata> CREATOR = new Creator<ExpressMetadata>() {
        @Override
        public ExpressMetadata createFromParcel(final Parcel in) {
            return new ExpressMetadata(in);
        }

        @Override
        public ExpressMetadata[] newArray(final int size) {
            return new ExpressMetadata[size];
        }
    };

    @Override
    public String getPaymentMethodId() {
        return paymentMethodId;
    }

    @Override
    public String getPaymentTypeId() {
        return paymentTypeId;
    }

    @Override
    public CardMetadata getCard() {
        return card;
    }

    public ConsumerCreditsMetadata getConsumerCredits() {
        return consumerCredits;
    }

    public AccountMoneyMetadata getAccountMoney() {
        return accountMoney;
    }

    public NewCardMetadata getNewCard() {
        return newCard;
    }

    @NonNull
    public StatusMetadata getStatus() {
        return status;
    }

    public OfflinePaymentTypesMetadata getOfflineMethods() {
        return offlineMethods;
    }

    public BenefitsMetadata getBenefits() {
        return benefits;
    }

    public SliderDisplayInfo getDisplayInfo() {
        return displayInfo;
    }

    public CheckoutBehaviour getBehaviour(@NonNull @CheckoutBehaviour.Type final String type) {
        return behaviours != null ? behaviours.get(type) : null;
    }

    public boolean isAccountMoney() {
        return accountMoney != null;
    }

    public boolean isConsumerCredits() {
        return consumerCredits != null;
    }

    @Override
    public boolean isCard() {
        return card != null;
    }

    public boolean isNewCard() {
        return newCard != null;
    }

    public boolean hasBenefits() {
        return benefits != null;
    }

    public boolean isOfflineMethods() {
        return offlineMethods != null;
    }

    @Override
    public String getCustomOptionId() {
        return isCard() ? getCard().getId() : getPaymentMethodId();
    }

    protected ExpressMetadata(final Parcel in) {
        paymentMethodId = in.readString();
        paymentTypeId = in.readString();
        card = in.readParcelable(CardMetadata.class.getClassLoader());
        accountMoney = in.readParcelable(AccountMoneyMetadata.class.getClassLoader());
        consumerCredits = in.readParcelable(ConsumerCreditsMetadata.class.getClassLoader());
        newCard = in.readParcelable(NewCardMetadata.class.getClassLoader());
        status = in.readParcelable(StatusMetadata.class.getClassLoader());
        offlineMethods = in.readParcelable(OfflinePaymentTypesMetadata.class.getClassLoader());
        benefits = in.readParcelable(BenefitsMetadata.class.getClassLoader());
        displayInfo = in.readParcelable(SliderDisplayInfo.class.getClassLoader());
        behaviours = new HashMap<>();
        in.readMap(behaviours, CheckoutBehaviour.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeString(paymentMethodId);
        dest.writeString(paymentTypeId);
        dest.writeParcelable(card, flags);
        dest.writeParcelable(accountMoney, flags);
        dest.writeParcelable(consumerCredits, flags);
        dest.writeParcelable(newCard, flags);
        dest.writeParcelable(status, flags);
        dest.writeParcelable(offlineMethods, flags);
        dest.writeParcelable(benefits, flags);
        dest.writeParcelable(displayInfo, flags);
        dest.writeMap(behaviours);
    }
}