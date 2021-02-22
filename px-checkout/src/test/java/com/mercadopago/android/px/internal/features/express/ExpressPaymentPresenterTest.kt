package com.mercadopago.android.px.internal.features.express

import com.mercadopago.android.px.addons.ESCManagerBehaviour
import com.mercadopago.android.px.configuration.AdvancedConfiguration
import com.mercadopago.android.px.configuration.DynamicDialogConfiguration
import com.mercadopago.android.px.core.DynamicDialogCreator
import com.mercadopago.android.px.internal.callbacks.MPCall
import com.mercadopago.android.px.internal.features.express.slider.HubAdapter
import com.mercadopago.android.px.internal.mappers.AmountDescriptorMapper
import com.mercadopago.android.px.internal.mappers.PaymentMethodDescriptorMapper
import com.mercadopago.android.px.internal.repository.*
import com.mercadopago.android.px.internal.tracking.TrackingRepository
import com.mercadopago.android.px.internal.view.ElementDescriptorView
import com.mercadopago.android.px.internal.viewmodel.SplitSelectionState
import com.mercadopago.android.px.internal.viewmodel.drawables.DrawableFragmentItem
import com.mercadopago.android.px.internal.viewmodel.drawables.PaymentMethodDrawableItemMapper
import com.mercadopago.android.px.mocks.CurrencyStub
import com.mercadopago.android.px.mocks.SiteStub
import com.mercadopago.android.px.model.*
import com.mercadopago.android.px.model.exceptions.ApiException
import com.mercadopago.android.px.model.exceptions.MercadoPagoError
import com.mercadopago.android.px.model.internal.DisabledPaymentMethod
import com.mercadopago.android.px.model.internal.CheckoutResponse
import com.mercadopago.android.px.model.internal.OneTapItem
import com.mercadopago.android.px.preferences.CheckoutPreference
import com.mercadopago.android.px.tracking.internal.MPTracker
import com.mercadopago.android.px.tracking.internal.events.AbortEvent
import com.mercadopago.android.px.tracking.internal.events.BackEvent
import com.mercadopago.android.px.tracking.internal.events.InstallmentsEventTrack
import com.mercadopago.android.px.tracking.internal.views.OneTapViewTracker
import com.mercadopago.android.px.utils.StubFailMpCall
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class ExpressPaymentPresenterTest {

    @Mock
    private lateinit var view: ExpressPayment.View

    @Mock
    private lateinit var paymentRepository: PaymentRepository

    @Mock
    private lateinit var paymentSettingRepository: PaymentSettingRepository

    @Mock
    private lateinit var disabledPaymentMethodRepository: DisabledPaymentMethodRepository

    @Mock
    private lateinit var payerCostSelectionRepository: PayerCostSelectionRepository

    @Mock
    private lateinit var checkoutRepository: CheckoutRepository

    @Mock
    private lateinit var discountRepository: DiscountRepository

    @Mock
    private lateinit var amountConfigurationRepository: AmountConfigurationRepository

    @Mock
    private lateinit var amountRepository: AmountRepository

    @Mock
    private lateinit var oneTapItem: OneTapItem

    @Mock
    private lateinit var amountConfiguration: AmountConfiguration

    @Mock
    private lateinit var discountConfigurationModel: DiscountConfigurationModel

    @Mock
    private lateinit var advancedConfiguration: AdvancedConfiguration

    @Mock
    private lateinit var dynamicDialogConfiguration: DynamicDialogConfiguration

    @Mock
    private lateinit var chargeRepository: ChargeRepository

    @Mock
    private lateinit var escManagerBehaviour: ESCManagerBehaviour

    @Mock
    private lateinit var paymentMethodDrawableItemMapper: PaymentMethodDrawableItemMapper

    @Mock
    private lateinit var experimentsRepository: ExperimentsRepository

    @Mock
    private lateinit var payerComplianceRepository: PayerComplianceRepository

    @Mock
    private lateinit var trackingRepository: TrackingRepository

    @Mock
    private lateinit var cardMetadata: CardMetadata

    @Mock
    private lateinit var tracker: MPTracker

    @Mock
    private lateinit var expressMetadataRepository: ExpressMetadataRepository

    @Mock
    private lateinit var payerPaymentMethodRepository: PayerPaymentMethodRepository

    @Mock
    private lateinit var modalRepository: ModalRepository

    private lateinit var expressPaymentPresenter: ExpressPaymentPresenter

    @Before
    fun setUp() {
        //This is needed for the presenter constructor
        val preference = mock(CheckoutPreference::class.java)
        val item = mock(Item::class.java)
        `when`(item.title).thenReturn("Item title")
        `when`(preference.items).thenReturn(listOf(item))
        `when`(paymentSettingRepository.site).thenReturn(SiteStub.MLA.get())
        `when`(paymentSettingRepository.currency).thenReturn(CurrencyStub.MLA.get())
        `when`(paymentSettingRepository.checkoutPreference).thenReturn(preference)
        `when`(paymentSettingRepository.advancedConfiguration).thenReturn(advancedConfiguration)
        `when`(advancedConfiguration.dynamicDialogConfiguration).thenReturn(dynamicDialogConfiguration)
        `when`(oneTapItem.isCard).thenReturn(true)
        `when`(oneTapItem.card).thenReturn(cardMetadata)
        `when`(cardMetadata.displayInfo).thenReturn(mock(CardDisplayInfo::class.java))
        `when`(oneTapItem.customOptionId).thenReturn("123")
        `when`(oneTapItem.status).thenReturn(mock(StatusMetadata::class.java))
        `when`(discountRepository.getCurrentConfiguration()).thenReturn(discountConfigurationModel)
        `when`(discountRepository.getConfigurationFor("123")).thenReturn(discountConfigurationModel)
        `when`(amountConfigurationRepository.getConfigurationFor("123")).thenReturn(amountConfiguration)
        `when`(expressMetadataRepository.value).thenReturn(listOf(oneTapItem))
        expressPaymentPresenter = ExpressPaymentPresenter(paymentSettingRepository, disabledPaymentMethodRepository,
            payerCostSelectionRepository, discountRepository, amountRepository, checkoutRepository,
            amountConfigurationRepository, chargeRepository, escManagerBehaviour, paymentMethodDrawableItemMapper,
            experimentsRepository, payerComplianceRepository, trackingRepository,
            mock(PaymentMethodDescriptorMapper::class.java), mock(CustomTextsRepository::class.java),
            mock(AmountDescriptorMapper::class.java), tracker, expressMetadataRepository, payerPaymentMethodRepository,
            modalRepository)
        verifyAttachView()
    }

    @Test
    fun whenFailToRetrieveCheckoutThenShowError() {
        `when`<MPCall<CheckoutResponse>>(checkoutRepository.checkout()).thenReturn(StubFailMpCall(mock(ApiException::class.java)))
        expressPaymentPresenter.handleDeepLink()
        verify(view).showError(any(MercadoPagoError::class.java))
    }

    @Test
    fun whenBackThenTrackAbort() {
        expressPaymentPresenter.onFreshStart()
        expressPaymentPresenter.onBack()

        verify(tracker).track(any(OneTapViewTracker::class.java))
        verify(tracker).track(any(AbortEvent::class.java))
        verifyNoMoreInteractions(tracker)
    }

    @Test
    fun whenCanceledThenCancelAndTrack() {
        expressPaymentPresenter.onFreshStart()
        expressPaymentPresenter.cancel()

        verify(view).cancel()
        verifyNoMoreInteractions(view)
        verify(tracker).track(any(OneTapViewTracker::class.java))
        verify(tracker).track(any(BackEvent::class.java))
        verifyNoMoreInteractions(tracker)
    }

    @Test
    fun whenInstallmentsRowPressedShowInstallments() {
        val selectedPayerCostIndex = 2
        `when`(amountConfiguration.getCurrentPayerCostIndex(anyBoolean(), anyInt())).thenReturn(selectedPayerCostIndex)

        expressPaymentPresenter.onInstallmentsRowPressed()

        verify(view).updateInstallmentsList(eq(selectedPayerCostIndex), anyList())
        verify(view).animateInstallmentsList()
        verify(tracker).track(any(InstallmentsEventTrack::class.java))
        verifyNoMoreInteractions(tracker)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun whenInstallmentsSelectionCancelledThenCollapseInstallments() {
        val paymentMethodIndex = 0
        val splitSelectionState = mock(SplitSelectionState::class.java)
        val state = mock(ExpressPaymentState::class.java)
        `when`(state.paymentMethodIndex).thenReturn(paymentMethodIndex)
        `when`(state.splitSelectionState).thenReturn(splitSelectionState)
        val payerCostIndex = 2
        `when`(payerCostSelectionRepository.get(oneTapItem.customOptionId)).thenReturn(payerCostIndex)

        expressPaymentPresenter.restoreState(state)
        expressPaymentPresenter.onInstallmentSelectionCanceled()

        verify(view).updateViewForPosition(paymentMethodIndex, payerCostIndex, splitSelectionState)
        verify(view).collapseInstallmentsSelection()
    }

    @Test
    fun whenViewIsResumedThenPaymentRepositoryIsAttached() {
        verifyNoMoreInteractions(paymentRepository)
        verifyNoMoreInteractions(view)
        verifyNoMoreInteractions(dynamicDialogConfiguration)
    }

    @Test
    fun whenElementDescriptorViewClickedAndHasCreatorThenShowDynamicDialog() {
        val dynamicDialogCreatorMock = mock(DynamicDialogCreator::class.java)
        `when`(dynamicDialogConfiguration.hasCreatorFor(DynamicDialogConfiguration.DialogLocation.TAP_ONE_TAP_HEADER))
            .thenReturn(true)
        `when`(dynamicDialogConfiguration.getCreatorFor(DynamicDialogConfiguration.DialogLocation.TAP_ONE_TAP_HEADER))
            .thenReturn(dynamicDialogCreatorMock)

        expressPaymentPresenter.onHeaderClicked()

        verify(dynamicDialogConfiguration).hasCreatorFor(DynamicDialogConfiguration.DialogLocation.TAP_ONE_TAP_HEADER)
        verify(dynamicDialogConfiguration).getCreatorFor(DynamicDialogConfiguration.DialogLocation.TAP_ONE_TAP_HEADER)
        verify(view).showDynamicDialog(eq(dynamicDialogCreatorMock), any(DynamicDialogCreator.CheckoutData::class.java))
        verifyNoMoreInteractions(view)
        verifyNoMoreInteractions(dynamicDialogConfiguration)
    }

    @Test
    fun whenElementDescriptorViewClickedAndHasNotCreatorThenDoNotShowDynamicDialog() {
        expressPaymentPresenter.onHeaderClicked()

        verify(dynamicDialogConfiguration).hasCreatorFor(DynamicDialogConfiguration.DialogLocation.TAP_ONE_TAP_HEADER)
        verifyNoMoreInteractions(view)
        verifyNoMoreInteractions(dynamicDialogConfiguration)
    }

    @Test
    fun whenDisabledDescriptorViewClickThenShowDisabledDialog() {
        val disabledPaymentMethod = mock(DisabledPaymentMethod::class.java)
        val statusMetadata = mock(StatusMetadata::class.java)
        `when`(disabledPaymentMethodRepository.getDisabledPaymentMethod(anyString())).thenReturn(disabledPaymentMethod)
        `when`(oneTapItem.status).thenReturn(statusMetadata)

        expressPaymentPresenter.onDisabledDescriptorViewClick()

        verify(view).showDisabledPaymentMethodDetailDialog(disabledPaymentMethod, statusMetadata)
    }

    @Test
    fun whenSliderOptionSelectedThenShowInstallmentsRow() {
        `when`(payerCostSelectionRepository[anyString()]).thenReturn(PayerCost.NO_SELECTED)
        val currentElementPosition = 0

        expressPaymentPresenter.onSliderOptionSelected(currentElementPosition)

        verify(view).updateViewForPosition(eq(currentElementPosition), eq(PayerCost.NO_SELECTED), any())
        verifyNoMoreInteractions(view)
    }

    @Test
    fun whenPayerCostSelectedThenItsReflectedOnView() {
        val paymentMethodIndex = 0
        val selectedPayerCostIndex = 1
        val payerCostList = mockPayerCosts(selectedPayerCostIndex)

        expressPaymentPresenter.onPayerCostSelected(payerCostList[selectedPayerCostIndex])

        verify(view).updateViewForPosition(eq(paymentMethodIndex), eq(selectedPayerCostIndex), any())
        verify(view).collapseInstallmentsSelection()
        verifyNoMoreInteractions(view)
    }

    private fun mockPayerCosts(selectedPayerCostIndex: Int): List<PayerCost> {
        `when`(payerCostSelectionRepository[anyString()]).thenReturn(selectedPayerCostIndex)
        val firstPayerCost = mock(PayerCost::class.java)
        val payerCostList = listOf(mock(PayerCost::class.java), firstPayerCost, mock(PayerCost::class.java))
        `when`(amountConfiguration.getAppliedPayerCost(false)).thenReturn(payerCostList)
        return payerCostList
    }

    private fun verifyAttachView() {
        expressPaymentPresenter.attachView(view)
        verify(view).configurePayButton(any())
        verify(view).configurePaymentMethodHeader(anyList())
        verify(view).showToolbarElementDescriptor(any(ElementDescriptorView.Model::class.java))
        verify(view).updateAdapters(any(HubAdapter.Model::class.java))
        verify(view).updateViewForPosition(anyInt(), anyInt(), any(SplitSelectionState::class.java))
        verify(view).configureRenderMode(any())
        verify(view).configureAdapters(any(Site::class.java), any(Currency::class.java))
        verify(view).updatePaymentMethods(anyListOf(DrawableFragmentItem::class.java))
    }
}
