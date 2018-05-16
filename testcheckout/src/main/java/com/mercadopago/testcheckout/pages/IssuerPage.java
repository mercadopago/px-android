package com.mercadopago.testcheckout.pages;

import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.contrib.RecyclerViewActions;

import com.mercadopago.testcheckout.assertions.Validator;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.contrib.RecyclerViewActions.scrollToPosition;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

public class IssuerPage extends PageObject {

    public IssuerPage() {
        // This constructor is intentionally empty. Nothing special is needed here.
    }

    public IssuerPage(Validator validator) {
        super(validator);
    }

    public ReviewAndConfirmPage enterBankOption(final int bankOption) {
        ViewInteraction recyclerView = onView(withId(com.mercadopago.R.id.mpsdkActivityIssuersView));
        recyclerView.perform(scrollToPosition(bankOption));
        recyclerView.perform(RecyclerViewActions.actionOnItemAtPosition(bankOption, click()));
        return new ReviewAndConfirmPage(validator);
    }

    @Override
    protected void validate() {
        validator.validate(this);
    }
}
