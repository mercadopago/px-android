package com.mercadopago.android.px.utils;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import com.mercadopago.SamplePaymentProcessor;
import com.mercadopago.SampleTopFragment;
import com.mercadopago.android.px.configuration.AdvancedConfiguration;
import com.mercadopago.android.px.configuration.DiscountConfiguration;
import com.mercadopago.android.px.configuration.DynamicDialogConfiguration;
import com.mercadopago.android.px.configuration.PaymentConfiguration;
import com.mercadopago.android.px.configuration.PaymentResultScreenConfiguration;
import com.mercadopago.android.px.configuration.TrackingConfiguration;
import com.mercadopago.android.px.core.MercadoPagoCheckout;
import com.mercadopago.android.px.core.SplitPaymentProcessor;
import com.mercadopago.android.px.model.GenericPayment;
import com.mercadopago.android.px.model.IParcelablePaymentDescriptor;
import com.mercadopago.android.px.model.IPaymentDescriptor;
import com.mercadopago.android.px.model.Item;
import com.mercadopago.android.px.model.Payment;
import com.mercadopago.android.px.model.PaymentTypes;
import com.mercadopago.android.px.model.Sites;
import com.mercadopago.android.px.model.commission.PaymentTypeChargeRule;
import com.mercadopago.android.px.preferences.CheckoutPreference;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.mercadopago.android.px.utils.PaymentUtils.getBusinessPaymentApproved;
import static com.mercadopago.android.px.utils.PaymentUtils.getGenericPaymentApproved;
import static com.mercadopago.android.px.utils.PaymentUtils.getGenericPaymentRejected;

public final class OneTapSamples {

    // Product id for show credits
    private static final String PRODUCT_ID = "bh31umv10flg01nmhg60";

    private OneTapSamples() {
        //Do nothing
    }

    public static void addAll(final Collection<Pair<String, MercadoPagoCheckout.Builder>> options) {
        int i = 1;
        options.add(new Pair<>("Saved cards with default installments", startSavedCardsDefaultInstallments()));
        options.add(new Pair<>(i++ + " - One tap - Should suggest account money (no cards)",
            startOneTapWithAccountMoneyNoCards()));
        options.add(new Pair<>(i++ + " - One tap - Should suggest account money (debit and credit cards)",
            startOneTapWithAccountMoneyAndCardsDebitCredit()));
        options.add(new Pair<>(i++ + " - One tap - Should suggest account money (debit and credit cards) and rejected",
            startOneTapWithAccountMoneyAndCardsDebitCreditAndRejectedPayment()));
        options.add(new Pair<>(i++ + " - One tap - Should suggest debit card (excluded account money)",
            startOneTapWithAccountMoneyAndCardsDebitCreditAndExcludedAccountMoney()));
        options.add(new Pair<>(i++ + " - One tap - Should suggest credit card (excluded account money and debit)",
            startOneTapWithAccountMoneyAndCardsDebitCreditAndExcludedAccountMoneyAndDebit()));
        options.add(new Pair<>(i++ + " - One tap - Should suggest credit card (no account money)",
            startOneTapNoAccountMoneyWithCreditCard()));
        options.add(new Pair<>(i++ + " - One tap - Shouldn't suggest one tap (no cards no account money)",
            startOneTapNoAccountMoneyNoCards()));
        options.add(new Pair<>(i++ + " - One tap - Should suggest credit card (no account money)",
            startOneTapNoAccountMoneyWithCredit()));
        options.add(new Pair<>(i++ + " - One tap - Should suggest credit card (account money with second factor auth",
            startOneTapWithAccountMoneyAndSecondFactorAuthWithCredit()));
        options.add(new Pair<>(i++ + " - One tap - Shouldn't suggest one tap (second factor and excuded credit card)",
            startOneTapWithAccountMoneyAndSecondFactorAuthWithExcludedCreditCard()));
        options.add(new Pair<>(i++ + " - One tap - Should suggest account money (credit card)",
            startOneTapWithAccountMoneyWithCreditCard()));
        options.add(new Pair<>(i++ + " - One tap - Should suggest account money (amount lower than cap)",
            startOneTapWithAccountMoneyLowerThanCap()));
        options.add(new Pair<>(i++ + " - One tap - Shouldn't suggest one tap (amount greater than cap)",
            startOneTapWithAmountGreaterThanCap()));
        options.add(new Pair<>(i++ + " - One tap - Should suggest account money (low account money)",
            startOneTapWithLowAccountMoneyWithLowerAmount()));
        options
            .add(new Pair<>(i++ + " - One tap - Should suggest credit card (low account money, amount lower than cap)",
                startOneTapWithLowAccountMoneyWithLowerAmountAndLowerCap()));
        options
            .add(new Pair<>(i++ + " - One tap - Shouldn't suggest one tap (low account money, amount greater than cap)",
                startOneTapWithLowAccountMoneyWithLowerAmountAndGreaterCap()));
        options.add(new Pair<>(i++ + " - One tap - Should suggest credit card (no account money) with direct discount",
            startOneTapNoAccountMoneyWithCreditCardAndDirectDiscount()));
        options.add(
            new Pair<>(i++ + " - One tap - Should suggest credit card (no account money) with not available discount",
                startOneTapNoAccountMoneyWithCreditCardAndNoAvailableDiscount()));
        options.add(new Pair<>(i++ + " - One tap - Should suggest credit card and get call for authorize result",
            startOneTapWithCreditCardAndShowCallForAuthorize()));
        options.add(new Pair<>(i++ + " - One tap - Should suggest consumer credits",
            startOneTapWithConsumerCredits()));
        options.add(new Pair<>(i++ + " - One tap - Should suggest consumer credits with charges",
            startOneTapWithConsumerCreditsWithCharges()));
        options.add(new Pair<>(i++ + " - One tap - Should suggest consumer credits with charges in brazil",
            startOneTapWithConsumerCreditsWithChargesInBrazil()));
        options.add(new Pair<>(i++ + " - One tap - Should suggest consumer credits and rejected",
            startOneTapWithConsumerCreditsAndRejectedPayment()));
        options.add(new Pair<>(i++ + " - One tap - Should suggest one tap with offline methods",
            startOneTapWithOfflineMethods()));
        options.add(new Pair<>(i++ + " - One tap - Should suggest one tap with debit card in chile",
            startOneTapWithDebitCardInChile()));
    }

    // It should suggest one tap with credit card, call for authorize
    private static MercadoPagoCheckout.Builder startOneTapWithCreditCardAndShowCallForAuthorize() {
        final GenericPayment payment = new GenericPayment.Builder(Payment.StatusCodes.STATUS_REJECTED,
            Payment.StatusDetail.STATUS_DETAIL_CC_REJECTED_CALL_FOR_AUTHORIZE).setPaymentId(123L)
            .createGenericPayment();
        final SplitPaymentProcessor samplePaymentProcessor = new SamplePaymentProcessor(payment);
        final Collection<String> excludedPaymentTypes = new ArrayList<>();
        excludedPaymentTypes.add("account_money");
        excludedPaymentTypes.add("debit_card");
        final CheckoutPreference checkoutPreferenceWithPayerEmail =
            getCheckoutPreferenceWithPayerEmail(excludedPaymentTypes, 120);
        return new MercadoPagoCheckout.Builder(Credentials.ONE_TAP_MERCHANT_PUBLIC_KEY, checkoutPreferenceWithPayerEmail,
            PaymentConfigurationUtils.create(samplePaymentProcessor))
            .setAdvancedConfiguration(new AdvancedConfiguration.Builder().setEscEnabled(true).build())
            .setPrivateKey(Credentials.ONE_TAP_PAYER_2_ACCESS_TOKEN)
            .setAdvancedConfiguration(new AdvancedConfiguration.Builder().setExpressPaymentEnable(true).build());
    }

    // It should suggest one tap with account money
    private static MercadoPagoCheckout.Builder startOneTapWithAccountMoneyNoCards() {
        final CheckoutPreference preference =
            getCheckoutPreferenceWithPayerEmail(new ArrayList<>(), 12000);
        final PaymentConfiguration paymentConfiguration =
            new PaymentConfiguration.Builder(new SamplePaymentProcessor(
                PaymentUtils.getGenericPaymentRejected(),
                PaymentUtils.getGenericPaymentApproved()))
                .addChargeRules(
                    Collections.singletonList(PaymentTypeChargeRule.createChargeFreeRule(
                        PaymentTypes.CREDIT_CARD, "Mensaje de prueba")))
                .build();

        final TrackingConfiguration trackingConfiguration =
            new TrackingConfiguration.Builder().flowId("example_app").build();

        final DynamicDialogConfiguration dynamicDialogConfiguration = new DynamicDialogConfiguration.Builder()
            .addDynamicCreator(DynamicDialogConfiguration.DialogLocation.ENTER_REVIEW_AND_CONFIRM,
                DialogSamples.INSTANCE.getDynamicDialog())
            .addDynamicCreator(DynamicDialogConfiguration.DialogLocation.TAP_ONE_TAP_HEADER,
                DialogSamples.INSTANCE.getDynamicDialog())
            .build();

        final PaymentResultScreenConfiguration resultScreenConfig = new PaymentResultScreenConfiguration.Builder()
            .setTopFragment(SampleTopFragment.class,
                DialogSamples.getSampleFragmentArgs("Your custom top fragment"))
            .setBottomFragment(SampleTopFragment.class,
                DialogSamples.getSampleFragmentArgs("Your custom bottom fragment"))
            .build();

        final AdvancedConfiguration advancedConfiguration = new AdvancedConfiguration.Builder()
            .setPaymentResultScreenConfiguration(resultScreenConfig)
            .setDynamicDialogConfiguration(dynamicDialogConfiguration)
            .setExpressPaymentEnable(true)
            .build();

        return new MercadoPagoCheckout.Builder(Credentials.ONE_TAP_DIRECT_DISCOUNT_MERCHANT_PUBLIC_KEY, preference,
            paymentConfiguration)
            .setPrivateKey(Credentials.ONE_TAP_PAYER_1_ACCESS_TOKEN)
            .setAdvancedConfiguration(advancedConfiguration)
            .setTrackingConfiguration(trackingConfiguration);
    }

    // It should suggest one tap with account money
    private static MercadoPagoCheckout.Builder startOneTapWithAccountMoneyAndCardsDebitCredit() {

        final IPaymentDescriptor payment = getGenericPaymentApproved();
        final SplitPaymentProcessor samplePaymentProcessor = new SamplePaymentProcessor(payment);
        final CheckoutPreference preference = getCheckoutPreferenceWithPayerEmail(120);
        return new MercadoPagoCheckout.Builder(Credentials.ONE_TAP_MERCHANT_PUBLIC_KEY, preference,
            PaymentConfigurationUtils
                .create(samplePaymentProcessor))
            .setPrivateKey(Credentials.ONE_TAP_PAYER_2_ACCESS_TOKEN)
            .setAdvancedConfiguration(new AdvancedConfiguration.Builder().setExpressPaymentEnable(true).build());
    }

    // It should suggest one tap with account money
    private static MercadoPagoCheckout.Builder startOneTapWithAccountMoneyAndCardsDebitCreditAndRejectedPayment() {

        final IParcelablePaymentDescriptor payment = getGenericPaymentRejected();
        final SplitPaymentProcessor samplePaymentProcessor = new SamplePaymentProcessor(payment);
        final CheckoutPreference preference = getCheckoutPreferenceWithPayerEmail(120);
        return new MercadoPagoCheckout.Builder(Credentials.ONE_TAP_MERCHANT_PUBLIC_KEY, preference,
            PaymentConfigurationUtils
                .create(samplePaymentProcessor))
            .setPrivateKey(Credentials.YELLOW_PRIVATE_KEY)
            .setAdvancedConfiguration(new AdvancedConfiguration.Builder().setExpressPaymentEnable(true).build());
    }

    // It should suggest one tap with debit card
    private static MercadoPagoCheckout.Builder startOneTapWithAccountMoneyAndCardsDebitCreditAndExcludedAccountMoney() {
        final IPaymentDescriptor payment = getGenericPaymentApproved();
        final SplitPaymentProcessor samplePaymentProcessor = new SamplePaymentProcessor(payment);
        final Collection<String> excludedPaymentTypes = new ArrayList<>();
        excludedPaymentTypes.add("account_money");
        final CheckoutPreference checkoutPreferenceWithPayerEmail =
            getCheckoutPreferenceWithPayerEmail(excludedPaymentTypes, 120);
        return new MercadoPagoCheckout.Builder(Credentials.ONE_TAP_MERCHANT_PUBLIC_KEY, checkoutPreferenceWithPayerEmail,
            PaymentConfigurationUtils.create(samplePaymentProcessor))
            .setPrivateKey(Credentials.ONE_TAP_PAYER_2_ACCESS_TOKEN)
            .setAdvancedConfiguration(new AdvancedConfiguration.Builder().setExpressPaymentEnable(true).build());
    }

    // It should suggest one tap with credit card
    private static MercadoPagoCheckout.Builder startOneTapWithAccountMoneyAndCardsDebitCreditAndExcludedAccountMoneyAndDebit() {
        final IPaymentDescriptor payment = getGenericPaymentApproved();
        final SplitPaymentProcessor samplePaymentProcessor = new SamplePaymentProcessor(payment);
        final Collection<String> excludedPaymentTypes = new ArrayList<>();
        excludedPaymentTypes.add("account_money");
        excludedPaymentTypes.add("debit_card");
        final CheckoutPreference checkoutPreferenceWithPayerEmail =
            getCheckoutPreferenceWithPayerEmail(excludedPaymentTypes, 120);
        return new MercadoPagoCheckout.Builder(Credentials.ONE_TAP_MERCHANT_PUBLIC_KEY, checkoutPreferenceWithPayerEmail,
            PaymentConfigurationUtils.create(samplePaymentProcessor))
            .setAdvancedConfiguration(new AdvancedConfiguration.Builder().setEscEnabled(true).build())
            .setPrivateKey(Credentials.ONE_TAP_PAYER_2_ACCESS_TOKEN)
            .setAdvancedConfiguration(new AdvancedConfiguration.Builder().setExpressPaymentEnable(true).build());
    }

    // It should suggest one tap with credit card
    private static MercadoPagoCheckout.Builder startOneTapNoAccountMoneyWithCreditCard() {
        final SplitPaymentProcessor samplePaymentProcessor =
            new SamplePaymentProcessor(getBusinessPaymentApproved());
        return new MercadoPagoCheckout.Builder(Credentials.ONE_TAP_MERCHANT_PUBLIC_KEY, getCheckoutPreferenceWithPayerEmail(120),
            PaymentConfigurationUtils
                .create(
                    samplePaymentProcessor))
            .setPrivateKey(Credentials.ONE_TAP_PAYER_3_ACCESS_TOKEN)
            .setAdvancedConfiguration(new AdvancedConfiguration.Builder().setExpressPaymentEnable(true).build());
    }

    // It shouldn't suggest one tap
    private static MercadoPagoCheckout.Builder startOneTapNoAccountMoneyNoCards() {
        final SplitPaymentProcessor samplePaymentProcessor =
            new SamplePaymentProcessor(getBusinessPaymentApproved());
        return new MercadoPagoCheckout.Builder(Credentials.ONE_TAP_MERCHANT_PUBLIC_KEY, getCheckoutPreferenceWithPayerEmail(120),
            PaymentConfigurationUtils
                .create(
                    samplePaymentProcessor))
            .setPrivateKey(Credentials.ONE_TAP_PAYER_4_ACCESS_TOKEN)
            .setAdvancedConfiguration(new AdvancedConfiguration.Builder().setExpressPaymentEnable(true).build());
    }

    // It should suggest one tap with credit card
    private static MercadoPagoCheckout.Builder startOneTapNoAccountMoneyWithCredit() {
        final SplitPaymentProcessor samplePaymentProcessor =
            new SamplePaymentProcessor(getBusinessPaymentApproved());
        return new MercadoPagoCheckout.Builder(Credentials.ONE_TAP_MERCHANT_PUBLIC_KEY, getCheckoutPreferenceWithPayerEmail(120),
            PaymentConfigurationUtils
                .create(
                    samplePaymentProcessor))
            .setPrivateKey(Credentials.ONE_TAP_PAYER_5_ACCESS_TOKEN)
            .setAdvancedConfiguration(new AdvancedConfiguration.Builder().setExpressPaymentEnable(true).build());
    }

    // It should suggest one tap with credit card
    private static MercadoPagoCheckout.Builder startOneTapWithAccountMoneyAndSecondFactorAuthWithCredit() {

        final SplitPaymentProcessor samplePaymentProcessor =
            new SamplePaymentProcessor(getBusinessPaymentApproved());
        return new MercadoPagoCheckout.Builder(Credentials.ONE_TAP_MERCHANT_PUBLIC_KEY,
            getCheckoutPreferenceWithPayerEmail(120),
            PaymentConfigurationUtils
                .create(
                    samplePaymentProcessor))
            .setPrivateKey(Credentials.ONE_TAP_PAYER_6_ACCESS_TOKEN)
            .setAdvancedConfiguration(new AdvancedConfiguration.Builder().setExpressPaymentEnable(true).build());
    }

    // It shouldn't suggest one tap
    private static MercadoPagoCheckout.Builder startOneTapWithAccountMoneyAndSecondFactorAuthWithExcludedCreditCard() {
        final SplitPaymentProcessor samplePaymentProcessor =
            new SamplePaymentProcessor(getBusinessPaymentApproved());
        final Collection<String> excludedPaymentTypes = new ArrayList<>();
        excludedPaymentTypes.add("credit_card");
        return new MercadoPagoCheckout.Builder(Credentials.ONE_TAP_MERCHANT_PUBLIC_KEY,
            getCheckoutPreferenceWithPayerEmail(excludedPaymentTypes, 120), PaymentConfigurationUtils
            .create(
                samplePaymentProcessor))
            .setPrivateKey(Credentials.ONE_TAP_PAYER_6_ACCESS_TOKEN)
            .setAdvancedConfiguration(new AdvancedConfiguration.Builder().setExpressPaymentEnable(true).build());
    }

    // It should suggest one tap with acount money
    private static MercadoPagoCheckout.Builder startOneTapWithAccountMoneyWithCreditCard() {
        final SplitPaymentProcessor samplePaymentProcessor =
            new SamplePaymentProcessor(getBusinessPaymentApproved());
        return new MercadoPagoCheckout.Builder(Credentials.ONE_TAP_MERCHANT_PUBLIC_KEY, getCheckoutPreferenceWithPayerEmail(120),
            PaymentConfigurationUtils
                .create(
                    samplePaymentProcessor))
            .setPrivateKey(Credentials.ONE_TAP_PAYER_7_ACCESS_TOKEN)
            .setAdvancedConfiguration(new AdvancedConfiguration.Builder().setExpressPaymentEnable(true).build());
    }

    // It should suggest one tap with acount money
    private static MercadoPagoCheckout.Builder startOneTapWithAccountMoneyLowerThanCap() {
        final SplitPaymentProcessor samplePaymentProcessor =
            new SamplePaymentProcessor(getBusinessPaymentApproved());
        return new MercadoPagoCheckout.Builder(Credentials.ONE_TAP_MERCHANT_PUBLIC_KEY,
            getCheckoutPreferenceWithPayerEmail(120),
            PaymentConfigurationUtils
                .create(
                    samplePaymentProcessor))
            .setPrivateKey(Credentials.ONE_TAP_PAYER_8_ACCESS_TOKEN)
            .setAdvancedConfiguration(new AdvancedConfiguration.Builder().setExpressPaymentEnable(true).build());
    }

    // It shouldn't suggest one tap
    private static MercadoPagoCheckout.Builder startOneTapWithAmountGreaterThanCap() {
        final SplitPaymentProcessor samplePaymentProcessor =
            new SamplePaymentProcessor(getBusinessPaymentApproved());
        return new MercadoPagoCheckout.Builder(Credentials.ONE_TAP_MERCHANT_PUBLIC_KEY, getCheckoutPreferenceWithPayerEmail(800),
            PaymentConfigurationUtils
                .create(
                    samplePaymentProcessor))
            .setPrivateKey(Credentials.ONE_TAP_PAYER_8_ACCESS_TOKEN)
            .setAdvancedConfiguration(new AdvancedConfiguration.Builder().setExpressPaymentEnable(true).build());
    }

    // It should suggest one tap with acount money
    private static MercadoPagoCheckout.Builder startOneTapWithLowAccountMoneyWithLowerAmount() {

        final SplitPaymentProcessor samplePaymentProcessor =
            new SamplePaymentProcessor(getBusinessPaymentApproved());
        return new MercadoPagoCheckout.Builder(Credentials.ONE_TAP_MERCHANT_PUBLIC_KEY,
            getCheckoutPreferenceWithPayerEmail(120),
            PaymentConfigurationUtils
                .create(
                    samplePaymentProcessor))
            .setPrivateKey(Credentials.ONE_TAP_PAYER_9_ACCESS_TOKEN)
            .setAdvancedConfiguration(new AdvancedConfiguration.Builder().setExpressPaymentEnable(true).build());
    }

    // It should suggest one tap with credit card
    private static MercadoPagoCheckout.Builder startOneTapWithLowAccountMoneyWithLowerAmountAndLowerCap() {

        final SplitPaymentProcessor samplePaymentProcessor =
            new SamplePaymentProcessor(getBusinessPaymentApproved());
        return new MercadoPagoCheckout.Builder(Credentials.ONE_TAP_MERCHANT_PUBLIC_KEY,
            getCheckoutPreferenceWithPayerEmail(500),
            PaymentConfigurationUtils
                .create(
                    samplePaymentProcessor))
            .setPrivateKey(Credentials.ONE_TAP_PAYER_9_ACCESS_TOKEN)
            .setAdvancedConfiguration(new AdvancedConfiguration.Builder().setExpressPaymentEnable(true).build());
    }

    // It shouldn't suggest one tap
    private static MercadoPagoCheckout.Builder startOneTapWithLowAccountMoneyWithLowerAmountAndGreaterCap() {
        final SplitPaymentProcessor samplePaymentProcessor =
            new SamplePaymentProcessor(getBusinessPaymentApproved());
        return new MercadoPagoCheckout.Builder(Credentials.ONE_TAP_MERCHANT_PUBLIC_KEY, getCheckoutPreferenceWithPayerEmail(701),
            PaymentConfigurationUtils
                .create(
                    samplePaymentProcessor))
            .setPrivateKey(Credentials.ONE_TAP_PAYER_9_ACCESS_TOKEN)
            .setAdvancedConfiguration(new AdvancedConfiguration.Builder().setExpressPaymentEnable(true).build());
    }

    // It should suggest one tap with credit card
    private static MercadoPagoCheckout.Builder startOneTapNoAccountMoneyWithCreditCardAndDirectDiscount() {
        return new MercadoPagoCheckout.Builder(Credentials.ONE_TAP_DIRECT_DISCOUNT_MERCHANT_PUBLIC_KEY,
            getCheckoutPreferenceWithPayerEmail(120),
            PaymentConfigurationUtils.create())
            .setPrivateKey(Credentials.ONE_TAP_PAYER_3_ACCESS_TOKEN)
            .setAdvancedConfiguration(new AdvancedConfiguration.Builder().setExpressPaymentEnable(true).build());
    }

    // It should suggest one tap with credit card and not available discount
    private static MercadoPagoCheckout.Builder startOneTapNoAccountMoneyWithCreditCardAndNoAvailableDiscount() {
        final SamplePaymentProcessor samplePaymentProcessor =
            new SamplePaymentProcessor(getBusinessPaymentApproved());
        final CheckoutPreference preference = getCheckoutPreferenceWithPayerEmail(new ArrayList<>(), 120);
        return new MercadoPagoCheckout.Builder(Credentials.ONE_TAP_DIRECT_DISCOUNT_MERCHANT_PUBLIC_KEY, preference,
            new PaymentConfiguration.Builder(samplePaymentProcessor)
                .setDiscountConfiguration(DiscountConfiguration.forNotAvailableDiscount()).build())
            .setPrivateKey(Credentials.ONE_TAP_PAYER_3_ACCESS_TOKEN)
            .setAdvancedConfiguration(new AdvancedConfiguration.Builder().setExpressPaymentEnable(true).build());
    }

    private static CheckoutPreference getCheckoutPreferenceWithPayerEmail(final int amount) {
        return getCheckoutPreferenceWithPayerEmail(new ArrayList<>(), amount);
    }

    private static CheckoutPreference getCheckoutPreferenceWithPayerEmail(
        @NonNull final Collection<String> excludedPaymentTypes, final int amount) {
        final List<Item> items = new ArrayList<>();
        final Item item = new Item.Builder("Android", 1, new BigDecimal(amount))
            .setDescription("Androide")
            .setPictureUrl("https://www.androidsis.com/wp-content/uploads/2015/08/marshmallow.png")
            .setId("1234")
            .build();
        items.add(item);
        return new CheckoutPreference.Builder(Sites.ARGENTINA,
            Credentials.PAYER_EMAIL_DUMMY, items)
            .addExcludedPaymentTypes(excludedPaymentTypes)
            .build();
    }

    private static CheckoutPreference getCheckoutPreferenceWithPayerEmailInBrazil(
        @NonNull final Collection<String> excludedPaymentTypes, final int amount) {
        final List<Item> items = new ArrayList<>();
        final Item item = new Item.Builder("Android", 1, new BigDecimal(amount))
            .setDescription("Androide")
            .setPictureUrl("https://www.androidsis.com/wp-content/uploads/2015/08/marshmallow.png")
            .setId("1234")
            .build();
        items.add(item);
        return new CheckoutPreference.Builder(Sites.BRASIL,
            Credentials.PAYER_EMAIL_DUMMY, items)
            .addExcludedPaymentTypes(excludedPaymentTypes)
            .build();
    }

    private static CheckoutPreference getCheckoutPreferenceWithPayerEmail(
        @NonNull final Collection<String> excludedPaymentTypes, final int amount, final int defaultInstallments) {
        final List<Item> items = new ArrayList<>();
        final Item item =
            new Item.Builder("Product title", 1, new BigDecimal(amount))
                .build();
        items.add(item);
        return new CheckoutPreference.Builder(Sites.ARGENTINA,
            Credentials.PAYER_EMAIL_DUMMY, items)
            .addExcludedPaymentTypes(excludedPaymentTypes)
            .setDefaultInstallments(defaultInstallments)
            .build();
    }

    // It should suggest one tap with debit card
    private static MercadoPagoCheckout.Builder startSavedCardsDefaultInstallments() {
        final IPaymentDescriptor payment = getGenericPaymentApproved();
        final SplitPaymentProcessor samplePaymentProcessor = new SamplePaymentProcessor(payment);
        final Collection<String> excludedPaymentTypes = new ArrayList<>();
        excludedPaymentTypes.add("account_money");
        final CheckoutPreference checkoutPreferenceWithPayerEmail =
            getCheckoutPreferenceWithPayerEmail(excludedPaymentTypes, 120, 1);
        return new MercadoPagoCheckout.Builder(Credentials.SAVED_CARD_MERCHANT_PUBLIC_KEY_1, checkoutPreferenceWithPayerEmail,
            PaymentConfigurationUtils.create(samplePaymentProcessor))
            .setPrivateKey(Credentials.SAVED_CARD_PAYER_PRIVATE_KEY_1)
            .setAdvancedConfiguration(new AdvancedConfiguration.Builder().setExpressPaymentEnable(true).build());
    }

    // It should suggest one tap with credits
    private static MercadoPagoCheckout.Builder startOneTapWithConsumerCredits() {

        final IPaymentDescriptor payment = getGenericPaymentApproved();

        final Collection<String> excludedPaymentTypes = new ArrayList<>();
        excludedPaymentTypes.add("ticket");
        final CheckoutPreference preference =
            getCheckoutPreferenceWithPayerEmail(excludedPaymentTypes, 120);
        final PaymentConfiguration paymentConfiguration =
            PaymentConfigurationUtils.create(new SamplePaymentProcessor(payment));

        return new MercadoPagoCheckout.Builder(Credentials.ONE_TAP_MERCHANT_PUBLIC_KEY, preference, paymentConfiguration)
            .setPrivateKey(Credentials.ONE_TAP_PAYER_1_ACCESS_TOKEN)
            .setAdvancedConfiguration(
                new AdvancedConfiguration.Builder().setProductId(PRODUCT_ID).setExpressPaymentEnable(true)
                    .build());
    }

    // It should suggest one tap with credits with charges
    private static MercadoPagoCheckout.Builder startOneTapWithConsumerCreditsWithCharges() {

        final IPaymentDescriptor payment = getGenericPaymentApproved();

        final Collection<String> excludedPaymentTypes = new ArrayList<>();
        excludedPaymentTypes.add("ticket");
        final CheckoutPreference preference =
            getCheckoutPreferenceWithPayerEmail(excludedPaymentTypes, 120);

        final ArrayList<PaymentTypeChargeRule> chargeRules = new ArrayList<>();
        chargeRules.add(PaymentTypeChargeRule.createChargeFreeRule(PaymentTypes.DIGITAL_CURRENCY, "consumer credits"));
        chargeRules.add(PaymentTypeChargeRule.createChargeFreeRule(PaymentTypes.ACCOUNT_MONEY, "account money"));

        final PaymentConfiguration paymentConfiguration = new PaymentConfiguration
            .Builder(new SamplePaymentProcessor(payment))
            .addChargeRules(chargeRules)
            .build();

        return new MercadoPagoCheckout.Builder(Credentials.ONE_TAP_MERCHANT_PUBLIC_KEY, preference, paymentConfiguration)
            .setPrivateKey(Credentials.ONE_TAP_PAYER_1_ACCESS_TOKEN)
            .setAdvancedConfiguration(
                new AdvancedConfiguration.Builder().setProductId(PRODUCT_ID).setExpressPaymentEnable(true)
                    .build());
    }

    // It should suggest one tap with credits with charges in brazil
    private static MercadoPagoCheckout.Builder startOneTapWithConsumerCreditsWithChargesInBrazil() {

        final IPaymentDescriptor payment = getGenericPaymentApproved();

        final Collection<String> excludedPaymentTypes = new ArrayList<>();
        excludedPaymentTypes.add("ticket");
        final CheckoutPreference preference =
            getCheckoutPreferenceWithPayerEmailInBrazil(excludedPaymentTypes, 120);

        final ArrayList<PaymentTypeChargeRule> chargeRules = new ArrayList<>();
        chargeRules.add(new PaymentTypeChargeRule(PaymentTypes.DIGITAL_CURRENCY, BigDecimal.TEN));
        chargeRules.add(PaymentTypeChargeRule.createChargeFreeRule(PaymentTypes.ACCOUNT_MONEY, "account money"));

        final PaymentConfiguration paymentConfiguration = new PaymentConfiguration
            .Builder(new SamplePaymentProcessor(payment))
            .addChargeRules(chargeRules)
            .build();

        return new MercadoPagoCheckout.Builder(Credentials.MLB_PUBLIC_KEY, preference, paymentConfiguration)
            .setPrivateKey(Credentials.MLB_PRIVATE_KEY_CREDITS)
            .setAdvancedConfiguration(
                new AdvancedConfiguration.Builder().setProductId(PRODUCT_ID).setExpressPaymentEnable(true)
                    .build());
    }

    // It should suggest one tap with credits and rejected
    private static MercadoPagoCheckout.Builder startOneTapWithConsumerCreditsAndRejectedPayment() {

        final IPaymentDescriptor payment = getGenericPaymentRejected();

        final Collection<String> excludedPaymentTypes = new ArrayList<>();
        excludedPaymentTypes.add("ticket");
        final CheckoutPreference preference =
            getCheckoutPreferenceWithPayerEmail(excludedPaymentTypes, 120);
        final PaymentConfiguration paymentConfiguration =
            PaymentConfigurationUtils.create(new SamplePaymentProcessor(payment));

        return new MercadoPagoCheckout.Builder(Credentials.ONE_TAP_MERCHANT_PUBLIC_KEY, preference, paymentConfiguration)
            .setPrivateKey(Credentials.YELLOW_PRIVATE_KEY)
            .setAdvancedConfiguration(
                new AdvancedConfiguration.Builder().setProductId(PRODUCT_ID).setExpressPaymentEnable(true)
                    .build());
    }

    // It should suggest one tap with offline methods
    private static MercadoPagoCheckout.Builder startOneTapWithOfflineMethods() {

        final IPaymentDescriptor payment = getGenericPaymentApproved();

        final Collection<String> excludedPaymentTypes = new ArrayList<>();
        excludedPaymentTypes.add("ticket");
        final CheckoutPreference preference =
            getCheckoutPreferenceWithPayerEmail(excludedPaymentTypes, 120);

        final ArrayList<PaymentTypeChargeRule> chargeRules = new ArrayList<>();
        chargeRules.add(new PaymentTypeChargeRule(PaymentTypes.DIGITAL_CURRENCY, BigDecimal.TEN));
        chargeRules.add(PaymentTypeChargeRule.createChargeFreeRule(PaymentTypes.ACCOUNT_MONEY, "account money"));

        final PaymentConfiguration paymentConfiguration = new PaymentConfiguration
            .Builder(new SamplePaymentProcessor(payment))
            .addChargeRules(chargeRules)
            .build();

        return new MercadoPagoCheckout.Builder(Credentials.ONE_TAP_MERCHANT_PUBLIC_KEY, preference, paymentConfiguration)
            .setPrivateKey(Credentials.ONE_TAP_PAYER_10_ACCESS_TOKEN)
            .setAdvancedConfiguration(
                new AdvancedConfiguration.Builder().setProductId(PRODUCT_ID).setExpressPaymentEnable(true)
                    .build());
    }

    private static MercadoPagoCheckout.Builder startOneTapWithDebitCardInChile() {
        final SplitPaymentProcessor samplePaymentProcessor = new SamplePaymentProcessor(
            PaymentUtils.getGenericPaymentApproved());

        return new MercadoPagoCheckout.Builder(Credentials.ONE_TAP_MERCHANT_WEB_PAY_PUBLIC_KEY, Credentials.ONE_TAP_WEB_PAY_PREFERENCE_ID,
            PaymentConfigurationUtils.create(samplePaymentProcessor))
            .setPrivateKey(Credentials.ONE_TAP_PAYER_WEB_PAY_ACCESS_TOKEN)
            .setAdvancedConfiguration(new AdvancedConfiguration
                .Builder()
                .setExpressPaymentEnable(true)
                .build());
    }
}
