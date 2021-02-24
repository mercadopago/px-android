package com.mercadopago.android.px.internal.datasource

import com.mercadopago.android.px.internal.core.FileManager
import com.mercadopago.android.px.internal.repository.PaymentMethodTypeSelectionRepository
import com.mercadopago.android.px.model.internal.OneTapItem

private const val PM_TYPE_SELECTED_REPOSITORY = "PM_TYPE_SELECTED_REPOSITORY"

internal class PaymentMethodTypeSelectionRepositoryImpl(
    private val fileManager: FileManager
) : AbstractLocalRepository<HashMap<String, String>>(fileManager), PaymentMethodTypeSelectionRepository {

    override val file = fileManager.create(PM_TYPE_SELECTED_REPOSITORY)

    override fun readFromStorage() = fileManager
        .readAnyMap(
            file,
            String::class.java,
            String::class.java)
        as HashMap<String, String>

    override fun get(paymentMethodId: String): String = value[paymentMethodId]
        ?: throw IllegalStateException("Payment method type must not be null")

    override fun save(paymentMethodId: String, paymentMethodType: String) {
        value[paymentMethodId] = paymentMethodType
        configure(value)
    }

    override fun configure(oneTapItems: List<OneTapItem>) {
        hashMapOf<String, String>().also { map ->
            oneTapItems.forEach { map[it.customOptionId] = it.getDefaultPaymentMethodType() }
            configure(map)
        }
    }
}