package com.mercadopago.android.px.internal.features.pay_button

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.mercadolibre.android.andesui.snackbar.action.AndesSnackbarAction
import com.mercadolibre.android.andesui.snackbar.duration.AndesSnackbarDuration
import com.mercadolibre.android.andesui.snackbar.type.AndesSnackbarType
import com.mercadolibre.android.ui.widgets.MeliButton
import com.mercadopago.android.px.R
import com.mercadopago.android.px.addons.BehaviourProvider
import com.mercadopago.android.px.addons.internal.SecurityValidationHandler
import com.mercadopago.android.px.addons.model.SecurityValidationData
import com.mercadopago.android.px.internal.audio.AudioPlayer
import com.mercadopago.android.px.internal.base.BaseFragment
import com.mercadopago.android.px.internal.di.viewModel
import com.mercadopago.android.px.internal.extensions.runIfNull
import com.mercadopago.android.px.internal.extensions.showSnackBar
import com.mercadopago.android.px.internal.features.Constants
import com.mercadopago.android.px.internal.features.dummy_result.DummyResultActivity
import com.mercadopago.android.px.internal.features.explode.ExplodeDecorator
import com.mercadopago.android.px.internal.features.explode.ExplodingFragment
import com.mercadopago.android.px.internal.features.payment_congrats.PaymentCongrats
import com.mercadopago.android.px.internal.features.payment_result.PaymentResultActivity
import com.mercadopago.android.px.internal.features.plugins.PaymentProcessorActivity
import com.mercadopago.android.px.internal.features.security_code.SecurityCodeActivity
import com.mercadopago.android.px.internal.features.security_code.SecurityCodeFragment
import com.mercadopago.android.px.internal.features.security_code.model.SecurityCodeParams
import com.mercadopago.android.px.internal.util.FragmentUtil
import com.mercadopago.android.px.internal.util.ViewUtils
import com.mercadopago.android.px.internal.util.nonNullObserve
import com.mercadopago.android.px.internal.view.OnSingleClickListener
import com.mercadopago.android.px.internal.viewmodel.PostPaymentAction
import com.mercadopago.android.px.tracking.internal.events.FrictionEventTracker
import com.mercadopago.android.px.internal.viewmodel.PayButtonViewModel as ButtonConfig

private const val REQ_CODE_CONGRATS = 300
private const val REQ_CODE_PAYMENT_PROCESSOR = 302
private const val REQ_CODE_BIOMETRICS = 303
private const val REQ_CODE_SECURITY_CODE = 304
private const val EXTRA_STATE = "extra_state"
private const val EXTRA_VISIBILITY = "extra_visibility"
private const val EXTRA_OBSERVING = "extra_observing"

class PayButtonFragment : BaseFragment(), PayButton.View, SecurityValidationHandler {

    private var buttonStatus = MeliButton.State.NORMAL
    private lateinit var button: MeliButton
    private val viewModel: PayButtonViewModel by viewModel()
    private var observingPostPaymentAction = false
    private var payButtonStateChange: PayButton.StateChange = object : PayButton.StateChange {
        override fun overrideStateChange(uiState: PayButton.State): Boolean {
            return false
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.px_fragment_pay_button, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        when {
            targetFragment is PayButton.Handler -> viewModel.attach(targetFragment as PayButton.Handler)
            parentFragment is PayButton.Handler -> viewModel.attach(parentFragment as PayButton.Handler)
            context is PayButton.Handler -> viewModel.attach(context as PayButton.Handler)
            else -> throw IllegalStateException("Parent should implement ${PayButton.Handler::class.java.simpleName}")
        }

        button = view.findViewById(R.id.confirm_button)
        button.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View?) {
                viewModel.preparePayment()
            }
        })
        savedInstanceState?.let {
            buttonStatus = it.getInt(EXTRA_STATE, MeliButton.State.NORMAL)
            button.visibility = it.getInt(EXTRA_VISIBILITY, VISIBLE)
            viewModel.restoreState(it.getParcelable(BUNDLE_STATE)!!)
            observingPostPaymentAction = it.getBoolean(EXTRA_OBSERVING)
        }

        if (observingPostPaymentAction) {
            observePostPaymentAction()
        }

        updateButtonState()

        with(viewModel) {
            buttonTextLiveData.observe(viewLifecycleOwner,
                Observer { buttonConfig -> button.text = buttonConfig!!.getButtonText(this@PayButtonFragment.context!!) })
            cvvRequiredLiveData.observe(viewLifecycleOwner,
                Observer { params -> params?.let { showSecurityCodeScreen(it) } })
            stateUILiveData.observe(viewLifecycleOwner, Observer { state -> state?.let { onStateUIChanged(it) } })
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(EXTRA_STATE, buttonStatus)
        outState.putInt(EXTRA_VISIBILITY, button.visibility)
        outState.putBoolean(EXTRA_OBSERVING, observingPostPaymentAction)
        outState.putParcelable(BUNDLE_STATE, viewModel.state)
    }

    private fun onStateUIChanged(stateUI: PayButtonUiState) {
        when (stateUI) {
            is UIProgress.FingerprintRequired -> startBiometricsValidation(stateUI.validationData)
            is UIProgress.ButtonLoadingStarted -> startLoadingButton(stateUI.timeOut, stateUI.buttonConfig)
            is UIProgress.ButtonLoadingFinished -> finishLoading(stateUI.explodeDecorator)
            is UIProgress.ButtonLoadingCanceled -> cancelLoading()
            is UIResult.VisualProcessorResult -> PaymentProcessorActivity.start(this, REQ_CODE_PAYMENT_PROCESSOR)
            is UIError -> resolveError(stateUI)
            is UIResult.PaymentResult -> PaymentResultActivity.start(this, REQ_CODE_CONGRATS, stateUI.model)
            is UIResult.NoCongratsResult -> DummyResultActivity.start(this, REQ_CODE_CONGRATS, stateUI.model)
            is UIResult.CongratsPaymentModel -> PaymentCongrats.show(stateUI.model, this, REQ_CODE_CONGRATS)
            is UIProgress.PlayResultAudio -> context?.let { AudioPlayer.play(it, stateUI.sound) }
        }
    }

    override fun stimulate() {
        viewModel.preparePayment()
    }

    override fun enable() {
        val handleState = payButtonStateChange.overrideStateChange(PayButton.State.ENABLE)
        if (!handleState) {
            buttonStatus = MeliButton.State.NORMAL
            updateButtonState()
        }
    }

    override fun disable() {
        val handleState = payButtonStateChange.overrideStateChange(PayButton.State.DISABLE)
        if (!handleState) {
            buttonStatus = MeliButton.State.DISABLED
            updateButtonState()
        }
    }

    private fun updateButtonState() {
        if (::button.isInitialized) {
            button.state = buttonStatus
        }
    }

    private fun startBiometricsValidation(validationData: SecurityValidationData) {
        disable()
        BehaviourProvider.getSecurityBehaviour().startValidation(this, validationData, REQ_CODE_BIOMETRICS)
    }

    private fun resolveConnectionError(uiError: UIError.ConnectionError) {
        var action: AndesSnackbarAction? = null
        var type = AndesSnackbarType.NEUTRAL
        var duration = AndesSnackbarDuration.SHORT

        uiError.actionMessage?.also {
            action = AndesSnackbarAction(getString(it), View.OnClickListener { activity?.onBackPressed() })
            type = AndesSnackbarType.ERROR
            duration = AndesSnackbarDuration.LONG
        }

        view.showSnackBar(getString(uiError.message), type, duration, action)
    }

    private fun resolveError(uiError: UIError) {
        when (uiError) {
            is UIError.ConnectionError -> resolveConnectionError(uiError)
            else -> {
                val action = AndesSnackbarAction(
                    getString(R.string.px_snackbar_error_action), View.OnClickListener {
                    activity?.onBackPressed()
                })
                view.showSnackBar(getString(R.string.px_error_title), andesSnackbarAction = action)
            }
        }
    }

    override fun onResultIconAnimation() {
        viewModel.onResultIconAnimation()
    }

    override fun onAnimationFinished() {
        viewModel.hasFinishPaymentAnimation()
    }

    override fun onSecurityValidated(isSuccess: Boolean, securityValidated: Boolean) {
        viewModel.handleBiometricsResult(isSuccess, securityValidated)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQ_CODE_BIOMETRICS) {
            val securityRequested = data?.getBooleanExtra(
                BehaviourProvider.getSecurityBehaviour().extraResultKey, false) ?: false
            enable()
            onSecurityValidated(resultCode == Activity.RESULT_OK, securityRequested)
        } else if (requestCode == REQ_CODE_CONGRATS) {
            if (resultCode == Constants.RESULT_ACTION) {
                handleAction(data)
            } else {
                viewModel.handleCongratsResult(resultCode, data)
            }
        } else if (requestCode == REQ_CODE_SECURITY_CODE) {
            if (resultCode == Constants.RESULT_ACTION) {
                handleAction(data)
            } else {
                viewModel.handleSecurityCodeResult(resultCode, data)
            }
        } else if (resultCode == Constants.RESULT_PAYMENT) {
            viewModel.onPostPayment(PaymentProcessorActivity.getPaymentModel(data))
        } else if (resultCode == Constants.RESULT_FAIL_ESC) {
            viewModel.onRecoverPaymentEscInvalid(PaymentProcessorActivity.getPaymentRecovery(data)!!)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun handleAction(data: Intent?) {
        data?.extras?.let { viewModel.onPostPaymentAction(PostPaymentAction.fromBundle(it)) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.detach()
    }

    override fun onDestroy() {
        FragmentUtil.tryRemoveNow(childFragmentManager, ExplodingFragment.TAG)
        super.onDestroy()
    }

    private fun finishLoading(params: ExplodeDecorator) {
        ViewUtils.hideKeyboard(activity)
        childFragmentManager.findFragmentByTag(ExplodingFragment.TAG)
            ?.let { (it as ExplodingFragment).finishLoading(params) }
            ?: viewModel.hasFinishPaymentAnimation()
    }

    private fun startLoadingButton(paymentTimeout: Int, buttonConfig: ButtonConfig) {
        context?.let {
            button.post {
                if (!isAdded) {
                    FrictionEventTracker.with("/px_checkout/pay_button_loading", FrictionEventTracker.Id.GENERIC,
                        FrictionEventTracker.Style.SCREEN, emptyMap<String, String>())
                } else {
                    val handleState = payButtonStateChange.overrideStateChange(PayButton.State.IN_PROGRESS)
                    if (!handleState) {
                        val explodingFragment = ExplodingFragment.newInstance(
                            buttonConfig.getButtonProgressText(it), paymentTimeout)
                        childFragmentManager.beginTransaction()
                            .add(R.id.exploding_frame, explodingFragment, ExplodingFragment.TAG)
                            .commitNowAllowingStateLoss()
                        hideConfirmButton()
                    }
                }
            }
        }
    }

    private fun cancelLoading() {
        showConfirmButton()
        val fragment = childFragmentManager.findFragmentByTag(ExplodingFragment.TAG) as ExplodingFragment?
        if (fragment != null && fragment.isAdded && fragment.hasFinished()) {
            childFragmentManager
                .beginTransaction()
                .remove(fragment)
                .commitNowAllowingStateLoss()
        }
        restoreStatusBar()
        enable()
    }

    private fun restoreStatusBar() {
        activity?.let { ViewUtils.setStatusBarColor(ContextCompat.getColor(it, R.color.px_colorPrimaryDark), it.window) }
    }

    private fun hideConfirmButton() {
        with(button) {
            clearAnimation()
            visibility = INVISIBLE
        }
    }

    private fun showConfirmButton() {
        with(button) {
            clearAnimation()
            visibility = VISIBLE
        }
    }

    private fun showSecurityCodeScreen(params: SecurityCodeParams) {
        if (params.fragmentContainer == 0) {
            SecurityCodeActivity.start(this, params, REQ_CODE_SECURITY_CODE)
        } else {
            activity?.supportFragmentManager?.apply {
                findFragmentByTag(SecurityCodeFragment.TAG).runIfNull {
                    beginTransaction()
                        .setCustomAnimations(0, R.animator.px_onetap_cvv_dummy, 0, R.animator.px_onetap_cvv_dummy)
                        .replace(params.fragmentContainer, SecurityCodeFragment.newInstance(params), SecurityCodeFragment.TAG)
                        .addToBackStack(SecurityCodeFragment.TAG)
                        .commit()
                    if (!observingPostPaymentAction) observePostPaymentAction()
                }
            }
        }
    }

    private fun observePostPaymentAction() {
        fragmentCommunicationViewModel?.apply {
            observingPostPaymentAction = true
            postPaymentActionLiveData.nonNullObserve(viewLifecycleOwner) {
                viewModel.onPostPaymentAction(it)
            }
        }
    }

    override fun isExploding(): Boolean {
        return FragmentUtil.isFragmentVisible(childFragmentManager, ExplodingFragment.TAG)
    }

    override fun getParentView() = button

    fun addOnStateChange(stateChange: PayButton.StateChange) {
        this.payButtonStateChange = stateChange
    }
}