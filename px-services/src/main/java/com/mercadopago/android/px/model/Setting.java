package com.mercadopago.android.px.model;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.Nullable;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public class Setting implements Parcelable, Serializable {

    public static final Creator<Setting> CREATOR = new Creator<Setting>() {
        @Override
        public Setting createFromParcel(final Parcel in) {
            return new Setting(in);
        }

        @Override
        public Setting[] newArray(final int size) {
            return new Setting[size];
        }
    };
    private Bin bin;
    private CardNumber cardNumber;
    private SecurityCode securityCode;

    private Setting(final Parcel in) {
        bin = in.readParcelable(Bin.class.getClassLoader());
        cardNumber = in.readParcelable(CardNumber.class.getClassLoader());
        securityCode = in.readParcelable(SecurityCode.class.getClassLoader());
    }

    @Nullable
    public static Setting getSettingByBin(@Nullable final Collection<Setting> settings, @Nullable final String bin) {

        Setting selectedSetting = null;

        if (settings != null && !settings.isEmpty()) {

            for (final Setting setting : settings) {
                if (bin != null && !bin.isEmpty() && bin.matches(setting.getBin().getPattern() + ".*") &&
                    (setting.getBin().getExclusionPattern() == null || setting.getBin().getExclusionPattern().isEmpty()
                        || !bin.matches(setting.getBin().getExclusionPattern() + ".*"))) {
                    selectedSetting = setting;
                }
            }
        }
        return selectedSetting;
    }

    @Nullable
    public static Setting getSettingByPaymentMethodAndBin(final PaymentMethod paymentMethod, final String bin) {
        Setting setting = null;
        if (bin == null) {
            if (paymentMethod.getSettings() != null && !paymentMethod.getSettings().isEmpty()) {
                setting = paymentMethod.getSettings().get(0);
            }
        } else {
            final List<Setting> settings = paymentMethod.getSettings();
            setting = Setting.getSettingByBin(settings, bin);
        }
        return setting;
    }

    public Bin getBin() {
        return bin;
    }

    public void setBin(final Bin bin) {
        this.bin = bin;
    }

    public CardNumber getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(final CardNumber cardNumber) {
        this.cardNumber = cardNumber;
    }

    public SecurityCode getSecurityCode() {
        return securityCode;
    }

    public void setSecurityCode(final SecurityCode securityCode) {
        this.securityCode = securityCode;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeParcelable(bin, flags);
        dest.writeParcelable(cardNumber, flags);
        dest.writeParcelable(securityCode, flags);
    }
}
