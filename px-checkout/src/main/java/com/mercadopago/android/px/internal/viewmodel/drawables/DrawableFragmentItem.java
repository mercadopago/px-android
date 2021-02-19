package com.mercadopago.android.px.internal.viewmodel.drawables;

import android.os.Parcel;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.meli.android.carddrawer.model.SwitchModel;
import com.mercadopago.android.px.internal.features.generic_modal.GenericDialogItem;
import com.mercadopago.android.px.internal.util.KParcelable;
import com.mercadopago.android.px.model.Reimbursement;
import com.mercadopago.android.px.model.StatusMetadata;
import com.mercadopago.android.px.model.internal.DisabledPaymentMethod;
import com.mercadopago.android.px.model.internal.Text;
import java.io.Serializable;

public abstract class DrawableFragmentItem implements KParcelable, Serializable {

    private final String id;
    private final StatusMetadata status;
    private final Text bottomDescription;
    private final String chargeMessage;
    private final Reimbursement reimbursement;
    private final DisabledPaymentMethod disabledPaymentMethod;
    private final String description;
    private final String issuerName;
    private final GenericDialogItem genericDialogItem;
    private final SwitchModel switchModel;

    protected DrawableFragmentItem(@NonNull final Parameters parameters) {
        id = parameters.id;
        status = parameters.status;
        bottomDescription = parameters.bottomDescription;
        chargeMessage = parameters.chargeMessage;
        reimbursement = parameters.reimbursement;
        disabledPaymentMethod = parameters.disabledPaymentMethod;
        description = parameters.description;
        issuerName = parameters.issuerName;
        genericDialogItem = parameters.genericDialogItem;
        switchModel = parameters.switchModel;
    }

    protected DrawableFragmentItem(final Parcel in) {
        id = in.readString();
        status = in.readParcelable(StatusMetadata.class.getClassLoader());
        bottomDescription = in.readParcelable(Text.class.getClassLoader());
        chargeMessage = in.readString();
        reimbursement = in.readParcelable(Reimbursement.class.getClassLoader());
        disabledPaymentMethod = in.readParcelable(DisabledPaymentMethod.class.getClassLoader());
        description = in.readString();
        issuerName = in.readString();
        genericDialogItem = in.readParcelable(GenericDialogItem.class.getClassLoader());
        switchModel = in.readParcelable(SwitchModel.class.getClassLoader());
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeString(id);
        dest.writeParcelable(status, flags);
        dest.writeParcelable(bottomDescription, flags);
        dest.writeString(chargeMessage);
        dest.writeParcelable(reimbursement, flags);
        dest.writeParcelable(disabledPaymentMethod, flags);
        dest.writeString(description);
        dest.writeString(issuerName);
        dest.writeParcelable(genericDialogItem, flags);
        dest.writeParcelable(switchModel, flags);
    }

    public abstract Fragment draw(@NonNull final PaymentMethodFragmentDrawer drawer);

    public String getId() {
        return id;
    }

    public StatusMetadata getStatus() {
        return status;
    }

    @Nullable
    public Text getBottomDescription() {
        return bottomDescription;
    }

    @Nullable
    public String getChargeMessage() {
        return chargeMessage;
    }

    @Nullable
    public Reimbursement getReimbursement() {
        return reimbursement;
    }

    @Nullable
    public DisabledPaymentMethod getDisabledPaymentMethod() {
        return disabledPaymentMethod;
    }

    @NonNull
    public String getDescription() {
        return description;
    }

    @NonNull
    public String getIssuerName() {
        return issuerName;
    }

    public boolean shouldHighlightBottomDescription() {
        return bottomDescription == null;
    }

    @Nullable
    public GenericDialogItem getGenericDialogItem() {
        return genericDialogItem;
    }

    @Nullable
    public SwitchModel getSwitchModel() {
        return switchModel;
    }

    public static final class Parameters {
        /* default */ @NonNull final String id;
        /* default */ @NonNull final StatusMetadata status;
        /* default */ @Nullable private Text bottomDescription;
        /* default */ @Nullable final String chargeMessage;
        /* default */ @Nullable final Reimbursement reimbursement;
        /* default */ @Nullable final DisabledPaymentMethod disabledPaymentMethod;
        /* default */ @NonNull final String description;
        /* default */ @NonNull final String issuerName;
        /* default */ @NonNull final GenericDialogItem genericDialogItem;
        /* default */ @Nullable final SwitchModel switchModel;

        /* default */ Parameters(@NonNull final String id, @NonNull final StatusMetadata status,
            @Nullable final Text bottomDescription, @Nullable final String chargeMessage,
            @Nullable final Reimbursement reimbursement, @Nullable final DisabledPaymentMethod disabledPaymentMethod,
            @NonNull final String description, @NonNull final String issuerName,
            @Nullable final GenericDialogItem genericDialogItem, @Nullable final SwitchModel switchModel) {
            this.id = id;
            this.status = status;
            this.bottomDescription = bottomDescription;
            this.chargeMessage = chargeMessage;
            this.reimbursement = reimbursement;
            this.disabledPaymentMethod = disabledPaymentMethod;
            this.description = description;
            this.issuerName = issuerName;
            this.genericDialogItem = genericDialogItem;
            this.switchModel = switchModel;
        }
    }
}
