package com.mercadopago.android.px.internal.features.pay_button

import com.mercadopago.android.px.R
import com.mercadopago.android.px.addons.model.SecurityValidationData
import com.mercadopago.android.px.internal.audio.AudioPlayer
import com.mercadopago.android.px.internal.features.explode.ExplodeDecorator
import com.mercadopago.android.px.internal.features.payment_congrats.model.PaymentCongratsModel
import com.mercadopago.android.px.internal.viewmodel.PaymentModel
import com.mercadopago.android.px.internal.viewmodel.PayButtonViewModel as ButtonConfig

internal sealed class PayButtonUiState

internal open class UIProgress : PayButtonUiState() {
    data class FingerprintRequired(val validationData: SecurityValidationData) : UIProgress()
    data class ButtonLoadingStarted(val timeOut: Int, val buttonConfig: ButtonConfig) : UIProgress()
    data class ButtonLoadingFinished(val explodeDecorator: ExplodeDecorator) : UIProgress()
    data class PlayResultAudio(val sound: AudioPlayer.Sound) : UIProgress()
    object ButtonLoadingCanceled : UIProgress()
}

internal open class UIResult : PayButtonUiState() {
    object VisualProcessorResult : UIResult()
    data class PaymentResult(val model: PaymentModel) : UIResult()
    data class CongratsPaymentModel(val model: PaymentCongratsModel) : UIResult()
    data class NoCongratsResult(val model : PaymentModel) : UIResult()
}

internal open class UIError : PayButtonUiState() {
    class ConnectionError(retriesCount: Int) : UIError() {
        private val maxRetries = 3
        val message = if (retriesCount <= maxRetries) R.string.px_connectivity_neutral_error else R.string.px_connectivity_error
        val actionMessage = if (retriesCount > maxRetries) R.string.px_snackbar_error_action else null
    }
    object BusinessError : UIError()
}