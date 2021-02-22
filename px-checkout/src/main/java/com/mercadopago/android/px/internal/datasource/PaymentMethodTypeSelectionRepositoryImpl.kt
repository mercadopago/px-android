package com.mercadopago.android.px.internal.datasource

import com.mercadopago.android.px.internal.core.FileManager
import com.mercadopago.android.px.internal.repository.PaymentMethodTypeSelectionRepository

private const val PM_TYPE_SELECTED = "PM_TYPE_SELECTED"

internal class PaymentMethodTypeSelectionRepositoryImpl(
    private val fileManager: FileManager
) : AbstractLocalRepository<HashMap<String, String>>(fileManager), PaymentMethodTypeSelectionRepository {

    override val file = fileManager.create(PM_TYPE_SELECTED)

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
}