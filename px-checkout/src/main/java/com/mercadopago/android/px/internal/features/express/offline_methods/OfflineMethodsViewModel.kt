package com.mercadopago.android.px.internal.features.express.offline_methods

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.mercadopago.android.px.internal.base.BaseViewModel
import com.mercadopago.android.px.internal.extensions.orIfEmpty
import com.mercadopago.android.px.internal.features.pay_button.PayButton.OnReadyForPaymentCallback
import com.mercadopago.android.px.internal.livedata.MutableSingleLiveData
import com.mercadopago.android.px.internal.repository.*
import com.mercadopago.android.px.internal.util.TextUtil
import com.mercadopago.android.px.internal.viewmodel.AmountLocalized
import com.mercadopago.android.px.model.OfflineMethodsCompliance
import com.mercadopago.android.px.model.SensitiveInformation
import com.mercadopago.android.px.model.internal.PaymentConfiguration
import com.mercadopago.android.px.tracking.internal.MPTracker
import com.mercadopago.android.px.tracking.internal.events.BackEvent
import com.mercadopago.android.px.tracking.internal.events.ConfirmEvent
import com.mercadopago.android.px.tracking.internal.events.KnowYourCustomerFlowEvent
import com.mercadopago.android.px.tracking.internal.model.ConfirmData
import com.mercadopago.android.px.tracking.internal.views.OfflineMethodsViewTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class OfflineMethodsViewModel(
    private val paymentSettingRepository: PaymentSettingRepository,
    private val amountRepository: AmountRepository,
    private val discountRepository: DiscountRepository,
    private val oneTapItemRepository: OneTapItemRepository,
    private val payerComplianceRepository: PayerComplianceRepository,
    tracker: MPTracker) : BaseViewModel(tracker), OfflineMethods.ViewModel {

    private lateinit var viewTracker: OfflineMethodsViewTracker
    private var payerCompliance: OfflineMethodsCompliance? = null
    private var selectedItem: OfflineMethodItem? = null
    private val observableDeepLink = MutableSingleLiveData<String>()
    override val deepLinkLiveData: LiveData<String>
        get() = observableDeepLink

    override fun onViewLoaded(): LiveData<OfflineMethods.Model> {
        val liveData = MutableLiveData<OfflineMethods.Model>()
        CoroutineScope(Dispatchers.IO).launch {
            val offlineMethods = oneTapItemRepository.value
                .firstOrNull { express -> express.isOfflineMethods }?.offlineMethods
            val bottomDescription = offlineMethods?.displayInfo?.bottomDescription
            val defaultPaymentTypeId = offlineMethods?.paymentTypes?.firstOrNull()?.id ?: TextUtil.EMPTY
            val amountLocalized = AmountLocalized(
                amountRepository.getAmountToPay(defaultPaymentTypeId, discountRepository.getCurrentConfiguration()),
                paymentSettingRepository.currency)
            payerCompliance = payerComplianceRepository.value?.offlineMethods
            val offlinePaymentTypes = offlineMethods?.paymentTypes.orEmpty()
            viewTracker = OfflineMethodsViewTracker(offlinePaymentTypes)
            liveData.postValue(OfflineMethods.Model(bottomDescription, amountLocalized, offlinePaymentTypes))
        }
        return liveData
    }

    override fun onSheetShowed() {
        track(viewTracker)
    }

    override fun onMethodSelected(selectedItem: OfflineMethodItem) {
        this.selectedItem = selectedItem
    }

    override fun onPrePayment(callback: OnReadyForPaymentCallback) {
        selectedItem?.let { item ->
            payerCompliance?.let {
                if (item.isAdditionalInfoNeeded && it.isCompliant) {
                    completePayerInformation(it.sensitiveInformation)
                } else if (item.isAdditionalInfoNeeded) {
                    track(KnowYourCustomerFlowEvent(viewTracker))
                    observableDeepLink.value = it.turnComplianceDeepLink
                    return
                }
            }
            requireCurrentConfiguration(item, callback)
        }
    }

    private fun requireCurrentConfiguration(item: OfflineMethodItem, callback: OnReadyForPaymentCallback) {
        val paymentMethodId = item.paymentMethodId.orIfEmpty(TextUtil.EMPTY)
        val paymentConfiguration = PaymentConfiguration(paymentMethodId, item.paymentTypeId.orIfEmpty(TextUtil.EMPTY),
            paymentMethodId, isCard = false, splitPayment = false, payerCost = null)
        callback.call(paymentConfiguration)
    }

    override fun onPaymentExecuted(configuration: PaymentConfiguration) {
        val confirmData = ConfirmData.from(configuration.paymentTypeId, configuration.paymentMethodId,
            payerCompliance?.isCompliant == true, selectedItem?.isAdditionalInfoNeeded == true)
        track(ConfirmEvent(confirmData))
    }

    private fun completePayerInformation(sensitiveInformation: SensitiveInformation) {
        val checkoutPreference = paymentSettingRepository.checkoutPreference
        val payer = checkoutPreference!!.payer
        payer.firstName = sensitiveInformation.firstName
        payer.lastName = sensitiveInformation.lastName
        payer.identification = sensitiveInformation.identification
        paymentSettingRepository.configure(checkoutPreference)
    }

    override fun onBack() {
        track(BackEvent(viewTracker))
    }
}
