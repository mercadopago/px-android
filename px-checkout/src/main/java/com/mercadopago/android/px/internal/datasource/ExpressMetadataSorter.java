package com.mercadopago.android.px.internal.datasource;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.mercadopago.android.px.model.internal.DisabledPaymentMethod;
import com.mercadopago.android.px.model.internal.OneTapItem;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/* default */ public final class ExpressMetadataSorter {
    @NonNull private final List<OneTapItem> expressMetadataList;
    @NonNull private final Map<String, DisabledPaymentMethod> disabledPaymentMethodMap;
    @Nullable private String prioritizedCardId;

    /* default */ ExpressMetadataSorter(@NonNull final List<OneTapItem> expressMetadataList,
        @NonNull final Map<String, DisabledPaymentMethod> disabledPaymentMethodMap) {
        this.expressMetadataList = expressMetadataList;
        this.disabledPaymentMethodMap = disabledPaymentMethodMap;
    }

    public ExpressMetadataSorter setPrioritizedCardId(@NonNull final String prioritizedCardId) {
        this.prioritizedCardId = prioritizedCardId;
        return this;
    }

    /* default */ void sort() {
        final Iterator<OneTapItem> expressMetadataIterator = expressMetadataList.iterator();
        final Collection<OneTapItem> disabledExpressMetadataList = new ArrayList<>();
        OneTapItem prioritizedCard = null;
        while (expressMetadataIterator.hasNext()) {
            final OneTapItem expressMetadata = expressMetadataIterator.next();
            if (expressMetadata.isNewCard()) {
                break;
            } else if(disabledPaymentMethodMap.containsKey(expressMetadata.getCustomOptionId())) {
                expressMetadataIterator.remove();
                disabledExpressMetadataList.add(expressMetadata);
            } else if(expressMetadata.isCard() && expressMetadata.getCard().getId().equals(prioritizedCardId)) {
                expressMetadataIterator.remove();
                prioritizedCard = expressMetadata;
            }
        }
        expressMetadataList.addAll(disabledExpressMetadataList);
        if (prioritizedCard != null) {
            expressMetadataList.add(0, prioritizedCard);
        }
    }
}