package com.mercadopago.android.px.internal.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mercadopago.android.px.internal.base.FragmentCommunicationViewModel
import com.mercadopago.android.px.internal.base.use_case.TokenizeUseCase
import com.mercadopago.android.px.internal.core.ConnectionHelper
import com.mercadopago.android.px.internal.features.express.offline_methods.OfflineMethodsViewModel
import com.mercadopago.android.px.internal.features.pay_button.PayButtonViewModel
import com.mercadopago.android.px.internal.features.security_code.SecurityCodeViewModel
import com.mercadopago.android.px.internal.features.security_code.domain.use_case.DisplayDataUseCase
import com.mercadopago.android.px.internal.features.security_code.domain.use_case.SecurityTrackModelUseCase
import com.mercadopago.android.px.internal.features.security_code.mapper.BusinessSecurityCodeDisplayDataMapper
import com.mercadopago.android.px.internal.features.security_code.mapper.SecurityCodeDisplayModelMapper
import com.mercadopago.android.px.internal.features.security_code.mapper.TrackingParamModelMapper
import com.mercadopago.android.px.internal.mappers.CardUiMapper
import com.mercadopago.android.px.internal.mappers.PayButtonViewModelMapper

internal class ViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        val session = Session.getInstance()
        val configurationModule = session.configurationModule
        val paymentSetting = configurationModule.paymentSettings

        return when {
            modelClass.isAssignableFrom(PayButtonViewModel::class.java) -> {
                PayButtonViewModel(session.paymentRepository,
                    configurationModule.productIdProvider,
                    ConnectionHelper.instance,
                    paymentSetting,
                    configurationModule.customTextsRepository,
                    PayButtonViewModelMapper(),
                    MapperProvider.getPaymentCongratsMapper(),
                    MapperProvider.getPostPaymentUrlsMapper(),
                    session.paymentResultViewModelFactory,
                    session.tracker
                )
            }
            modelClass.isAssignableFrom(OfflineMethodsViewModel::class.java) -> {
                OfflineMethodsViewModel(paymentSetting,
                    session.amountRepository,
                    session.discountRepository,
                    session.oneTapItemRepository,
                    session.configurationModule.payerComplianceRepository,
                    session.tracker)
            }
            modelClass.isAssignableFrom(SecurityCodeViewModel::class.java) -> {
                val tokenizeUseCase = TokenizeUseCase(
                    session.cardTokenRepository,
                    session.mercadoPagoESC,
                    paymentSetting,
                    session.tracker
                )

                val displayDataUseCase = DisplayDataUseCase(
                    BusinessSecurityCodeDisplayDataMapper(),
                    session.tracker,
                    session.oneTapItemRepository)

                SecurityCodeViewModel(
                    tokenizeUseCase,
                    displayDataUseCase,
                    SecurityTrackModelUseCase(session.tracker),
                    TrackingParamModelMapper(),
                    SecurityCodeDisplayModelMapper(CardUiMapper),
                    session.tracker
                )
            }
            modelClass.isAssignableFrom(FragmentCommunicationViewModel::class.java) -> {
                FragmentCommunicationViewModel(session.tracker)
            }
            else -> {
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        } as T
    }
}
