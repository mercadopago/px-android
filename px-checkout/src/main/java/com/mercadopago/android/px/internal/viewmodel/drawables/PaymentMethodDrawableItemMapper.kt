package com.mercadopago.android.px.internal.viewmodel.drawables

import com.meli.android.carddrawer.configuration.CardDrawerStyle
import com.mercadopago.android.px.internal.features.generic_modal.ActionType
import com.mercadopago.android.px.internal.features.generic_modal.FromModalToGenericDialogItem
import com.mercadopago.android.px.internal.features.generic_modal.GenericDialogItem
import com.mercadopago.android.px.internal.mappers.CardUiMapper
import com.mercadopago.android.px.internal.mappers.NonNullMapper
import com.mercadopago.android.px.internal.repository.ChargeRepository
import com.mercadopago.android.px.internal.repository.DisabledPaymentMethodRepository
import com.mercadopago.android.px.internal.repository.ModalRepository
import com.mercadopago.android.px.internal.repository.PayerPaymentMethodRepository
import com.mercadopago.android.px.internal.util.TextUtil
import com.mercadopago.android.px.internal.viewmodel.mappers.CardDrawerCustomViewModelMapper
import com.mercadopago.android.px.internal.viewmodel.drawables.DrawableFragmentItem.Parameters
import com.mercadopago.android.px.model.AccountMoneyDisplayInfo
import com.mercadopago.android.px.model.CustomSearchItem
import com.mercadopago.android.px.model.ExpressMetadata
import com.mercadopago.android.px.model.one_tap.CheckoutBehaviour

internal class PaymentMethodDrawableItemMapper(
    private val chargeRepository: ChargeRepository,
    private val disabledPaymentMethodRepository: DisabledPaymentMethodRepository,
    private val cardUiMapper: CardUiMapper,
    private val cardDrawerCustomViewModelMapper: CardDrawerCustomViewModelMapper,
    private val payerPaymentMethodRepository: PayerPaymentMethodRepository,
    private val modalRepository: ModalRepository
) : NonNullMapper<ExpressMetadata, DrawableFragmentItem?>() {


    override fun map(value: ExpressMetadata): DrawableFragmentItem? {
        val genericDialogItem = value.getBehaviour(CheckoutBehaviour.Type.TAP_CARD)?.modal?.let { modal ->
            modalRepository.value[modal]?.let {
                FromModalToGenericDialogItem(ActionType.DISMISS, modal).map(it)
            }
        }
        val parameters = getParameters(value, payerPaymentMethodRepository.value, genericDialogItem)
        with(value) {
            return when {
                isCard -> SavedCardDrawableFragmentItem(parameters, paymentMethodId,
                    cardUiMapper.map(card.displayInfo))
                isAccountMoney -> getAccountMoneyFragmentItem(parameters, accountMoney.displayInfo)
                isConsumerCredits -> ConsumerCreditsDrawableFragmentItem(parameters, consumerCredits)
                isNewCard || isOfflineMethods -> OtherPaymentMethodFragmentItem(parameters, newCard, offlineMethods)
                else -> null
            }
        }
    }

    private fun getAccountMoneyFragmentItem(parameters: Parameters, displayInfo: AccountMoneyDisplayInfo) =
        displayInfo.takeIf { it.type != null }?.let {
            AccountMoneyDrawableFragmentItem(parameters, cardUiMapper.map(it))
        } ?: AccountMoneyDrawableFragmentItem(parameters, CardDrawerStyle.ACCOUNT_MONEY_DEFAULT)

    private fun getParameters(
        expressMetadata: ExpressMetadata,
        customSearchItems: List<CustomSearchItem>,
        genericDialogItem: GenericDialogItem?
    ): Parameters {
        val displayInfo = expressMetadata.displayInfo
        val charge = chargeRepository.getChargeRule(expressMetadata.paymentTypeId)
        val customOptionId = expressMetadata.customOptionId
        val (description, issuerName) = customSearchItems.firstOrNull { c -> c.id == customOptionId }?.let {
            Pair(it.description.orEmpty(), it.issuer?.name.orEmpty())
        } ?: Pair(TextUtil.EMPTY, TextUtil.EMPTY)

        return Parameters(
            customOptionId,
            expressMetadata.status,
            displayInfo?.bottomDescription,
            charge?.message,
            expressMetadata.benefits?.reimbursement,
            disabledPaymentMethodRepository.getDisabledPaymentMethod(customOptionId),
            description,
            issuerName,
            genericDialogItem,
            cardDrawerCustomViewModelMapper.mapToSwitchModel(displayInfo?.cardDrawerSwitch)
        )
    }
}
