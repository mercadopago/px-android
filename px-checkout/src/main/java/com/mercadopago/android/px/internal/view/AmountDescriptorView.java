package com.mercadopago.android.px.internal.view;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import android.text.SpannableStringBuilder;
import android.util.AttributeSet;
import android.content.res.Configuration;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.mercadolibre.android.picassodiskcache.PicassoDiskLoader;
import com.mercadopago.android.px.R;
import com.mercadopago.android.px.core.DynamicDialogCreator;
import com.mercadopago.android.px.internal.font.PxFont;
import com.mercadopago.android.px.internal.util.TextUtil;
import com.mercadopago.android.px.internal.util.ViewUtils;
import com.mercadopago.android.px.internal.viewmodel.AmountDescriptor;
import com.mercadopago.android.px.internal.viewmodel.DiscountBriefColor;
import com.mercadopago.android.px.internal.viewmodel.EmptyLocalized;
import com.mercadopago.android.px.internal.viewmodel.IDetailColor;
import com.mercadopago.android.px.internal.viewmodel.IDetailDrawable;
import com.mercadopago.android.px.internal.viewmodel.ILocalizedCharSequence;
import com.mercadopago.android.px.model.DiscountConfigurationModel;
import com.mercadopago.android.px.model.internal.Text;

import static com.mercadopago.android.px.internal.util.AccessibilityUtilsKt.executeIfAccessibilityTalkBackEnable;
import static com.mercadopago.android.px.internal.util.TextUtil.isEmpty;

public class AmountDescriptorView extends ConstraintLayout {

    private MPTextView descriptor;
    private MPTextView brief;
    private View descriptorContainer;
    private MPTextView amount;
    private ImageView iconDescriptor;
    private boolean rightLabelSemiBold;
    private boolean leftLabelSemiBold;

    public static int getDesiredHeight(@NonNull final Context context) {
        final View view = inflate(context, R.layout.px_viewholder_amountdescription, null);
        view.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
        return view.getMeasuredHeight();
    }

    public AmountDescriptorView(final Context context) {
        this(context, null);
    }

    public AmountDescriptorView(final Context context, @Nullable final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AmountDescriptorView(final Context context, @Nullable final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public interface OnClickListener {
        void onDiscountAmountDescriptorClicked(@NonNull final DiscountConfigurationModel discountModel);

        void onChargesAmountDescriptorClicked(@NonNull final DynamicDialogCreator dynamicDialogCreator);
    }

    private void init() {
        inflate(getContext(), R.layout.px_view_amount_descriptor, this);
        descriptorContainer = findViewById(R.id.descriptor_container);
        descriptor = descriptorContainer.findViewById(R.id.descriptor);
        brief = descriptorContainer.findViewById(R.id.brief);
        amount = findViewById(R.id.amount);
        iconDescriptor = findViewById(R.id.icon_descriptor);
    }

    public void animateEnter() {
        final Animation slideLeft = AnimationUtils.loadAnimation(getContext(), R.anim.px_summary_slide_left_in);
        final Animation slideRight = AnimationUtils.loadAnimation(getContext(), R.anim.px_summary_slide_right_in);
        descriptorContainer.startAnimation(slideRight);
        amount.startAnimation(slideLeft);
    }

    public void update(@NonNull final AmountDescriptorView.Model model) {
        if (model.amountDescriptor != null) {
            updateAmountDescriptor(model.amountDescriptor, model.detailColor, model.briefColor, model.hasSplit);
        } else {
            updateTextColor(model.detailColor);
            updateLeftLabel(model);
            updateRightLabel(model);
            updateDrawable(model.detailDrawable, model.detailDrawableColor);
            brief.setVisibility(View.GONE);
        }

        setOnClickListener(model.listener);

        executeIfAccessibilityTalkBackEnable(getContext(), () -> {
            final SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
            spannableStringBuilder.append(descriptor.getText()).append(TextUtil.SPACE);
            final String textAmount = getTextAmount(model);
            final String[] listAmount = textAmount.split(TextUtil.SPACE);
            if (listAmount.length > 0) {
                spannableStringBuilder
                    .append(listAmount[listAmount.length - 1])
                    .append(getResources().getString(R.string.px_money));
            }
            setContentDescription(spannableStringBuilder.toString());
            return null;
        });
    }

    @NonNull
    private String getTextAmount(@NonNull final AmountDescriptorView.Model model) {
        final ILocalizedCharSequence amount = model.right;
        final AmountDescriptor amountDescriptor = model.amountDescriptor;
        if (amount != null) {
            return amount.get(getContext()).toString();
        } else if (amountDescriptor != null) {
            return amountDescriptor.getAmount().getMessage();
        } else {
            return TextUtil.EMPTY;
        }
    }

    private void updateAmountDescriptor(@NonNull final AmountDescriptor amountDescriptor,
        @NonNull final IDetailColor descriptorColor, @NonNull final IDetailColor briefColor, final boolean hasSplit) {
        ViewUtils
            .loadTextListOrGone(descriptor, amountDescriptor.getDescription(), descriptorColor.getColor(getContext()));

        if (!hasSplit || ViewUtils.isScreenSize(getContext(), Configuration.SCREENLAYOUT_SIZE_LARGE)) {
            ViewUtils.loadTextListOrGone(brief, amountDescriptor.getBrief(), briefColor.getColor(getContext()));
        } else {
            brief.setVisibility(View.GONE);
        }

        amount.setText(amountDescriptor.getAmount());

        if (TextUtil.isNotEmpty(amountDescriptor.getUrl())) {
            PicassoDiskLoader.get(getContext()).load(amountDescriptor.getUrl()).into(iconDescriptor);
        }
    }

    private void updateRightLabel(@NonNull final AmountDescriptorView.Model model) {
        updateLabel(model.right.get(getContext()).toString(), amount, rightLabelSemiBold);
    }

    private void updateLeftLabel(@NonNull final AmountDescriptorView.Model model) {
        if (model.leftText != null) {
            updateLabel(descriptor, model.leftText);
        } else {
            updateLabel(model.left.get(getContext()), descriptor, leftLabelSemiBold);
        }
    }

    private void updateLabel(@NonNull final CharSequence charSequence, @NonNull final TextView textView,
        final boolean isSemiBold) {
        final SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
        spannableStringBuilder.append(charSequence);

        if (isSemiBold) {
            ViewUtils.setFontInSpannable(getContext(), PxFont.SEMI_BOLD, spannableStringBuilder);
        }

        if (isEmpty(charSequence)) {
            textView.setVisibility(GONE);
        }

        textView.setText(spannableStringBuilder);
    }

    private void updateLabel(@NonNull final MPTextView textView, @NonNull final Text text) {
        ViewUtils.loadOrHide(View.GONE, text, textView);
    }

    public void setBold(@NonNull final Position label) {
        if (Position.LEFT == label) {
            leftLabelSemiBold = true;
        } else if (Position.RIGHT == label) {
            rightLabelSemiBold = true;
        }
    }

    public void setTextSize(final int dimen) {
        final int size = (int) getContext().getResources().getDimension(dimen);
        descriptor.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        amount.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
    }

    public void setTextColor(final int color) {
        descriptor.setTextColor(color);
        amount.setTextColor(color);
    }

    private void updateTextColor(@NonNull final IDetailColor detailColor) {
        descriptor.setTextColor(detailColor.getColor(getContext()));
        amount.setTextColor(detailColor.getColor(getContext()));
    }

    private void updateDrawable(@Nullable final IDetailDrawable detailDrawable,
        @Nullable final IDetailColor detailColor) {
        if (detailDrawable != null) {
            iconDescriptor.setVisibility(VISIBLE);
            iconDescriptor.setImageDrawable(detailDrawable.getDrawable(getContext()));
        } else {
            iconDescriptor.setVisibility(INVISIBLE);
        }

        if (detailColor != null) {
            iconDescriptor.setColorFilter(detailColor.getColor(getContext()));
        }
    }

    public static class Model {
        /* default */ @Nullable final ILocalizedCharSequence left;
        /* default */ @Nullable final ILocalizedCharSequence right;
        /* default */ @NonNull final IDetailColor detailColor;
        /* default */ @NonNull final IDetailColor briefColor;
        /* default */ @Nullable final Text leftText;
        /* default */ @Nullable final AmountDescriptor amountDescriptor;
        /* default */ @Nullable IDetailDrawable detailDrawable;
        /* default */ @Nullable IDetailColor detailDrawableColor;
        /* default */ @Nullable View.OnClickListener listener;
        /* default */ boolean hasSplit = false;

        public Model(@NonNull final AmountDescriptor amountDescriptor, @NonNull final IDetailColor detailColor,
            final boolean hasSplit) {
            this.amountDescriptor = amountDescriptor;
            this.detailColor = detailColor;
            this.briefColor = new DiscountBriefColor();
            this.hasSplit = hasSplit;
            this.leftText = null;
            this.left = null;
            this.right = null;
        }

        public Model(@NonNull final ILocalizedCharSequence left, @NonNull final ILocalizedCharSequence right,
            @NonNull final IDetailColor detailColor) {
            this.left = left;
            this.right = right;
            this.detailColor = detailColor;
            this.briefColor = new DiscountBriefColor();
            this.leftText = null;
            this.amountDescriptor = null;
        }

        public Model(@NonNull final ILocalizedCharSequence left, @NonNull final IDetailColor detailColor) {
            this.left = left;
            this.detailColor = detailColor;
            this.briefColor = new DiscountBriefColor();
            this.right = new EmptyLocalized();
            this.leftText = null;
            this.amountDescriptor = null;
        }

        public Model(@NonNull final Text leftText, @NonNull final IDetailColor detailColor) {
            this.leftText = leftText;
            this.detailColor = detailColor;
            this.briefColor = new DiscountBriefColor();
            this.left = new EmptyLocalized();
            this.right = new EmptyLocalized();
            this.amountDescriptor = null;
        }

        AmountDescriptorView.Model setDetailDrawable(@Nullable final IDetailDrawable detailDrawable,
            @Nullable final IDetailColor detailDrawableColor) {
            this.detailDrawable = detailDrawable;
            this.detailDrawableColor = detailDrawableColor;
            return this;
        }

        public AmountDescriptorView.Model setListener(@NonNull final View.OnClickListener listener) {
            this.listener = listener;
            return this;
        }
    }

    public enum Position {LEFT, RIGHT}
}
