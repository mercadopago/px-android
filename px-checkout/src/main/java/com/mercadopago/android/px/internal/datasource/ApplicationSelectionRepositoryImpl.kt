package com.mercadopago.android.px.internal.datasource

import com.mercadopago.android.px.internal.core.FileManager
import com.mercadopago.android.px.internal.repository.ApplicationSelectionRepository
import com.mercadopago.android.px.internal.repository.OneTapItemRepository
import com.mercadopago.android.px.model.internal.Application
import java.io.File
import java.lang.IllegalStateException

private const val SELECTED_APPLICATIONS = "selected_applications_repository"

internal class ApplicationSelectionRepositoryImpl(private val fileManager: FileManager,
    private val oneTapItemRepository: OneTapItemRepository) :
    AbstractLocalRepository<HashMap<String, Application>>(fileManager), ApplicationSelectionRepository {

    override val file: File = fileManager.create(SELECTED_APPLICATIONS)

    override fun get(payerPaymentMethodId: String): Application? {
        val selectedApplication = value[payerPaymentMethodId]
        return selectedApplication ?: resolveDefault(payerPaymentMethodId)
    }

    override fun getPaymentMethodTypeId(payerPaymentMethodId: String): String = get(payerPaymentMethodId)
        ?.paymentMethod?.type
        ?: oneTapItemRepository
            .value.firstOrNull { it.customOptionId == payerPaymentMethodId }
            ?.getDefaultPaymentMethodType()
        ?: throw IllegalStateException("Payment method type id must not be null")

    private fun resolveDefault(payerPaymentMethodId: String): Application? {
        return oneTapItemRepository.value.firstOrNull { it.customOptionId == payerPaymentMethodId }
            ?.let { oneTapItem ->
                oneTapItem.getApplications()?.firstOrNull {
                    oneTapItem.displayInfo?.cardDrawerSwitch?.default == it.paymentMethod.type
                }?.also { set(payerPaymentMethodId, it) }
            }
    }

    override fun set(payerPaymentMethodId: String, application: Application) {
        value[payerPaymentMethodId] = application
        configure(value)
    }

    override fun readFromStorage() = fileManager.readAnyMap(file, String::class.java, Application::class.java) as HashMap<String, Application>
}
