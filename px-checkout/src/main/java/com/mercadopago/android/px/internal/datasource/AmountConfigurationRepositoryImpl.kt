package com.mercadopago.android.px.internal.datasource

import com.mercadopago.android.px.internal.core.FileManager
import com.mercadopago.android.px.internal.repository.AmountConfigurationRepository
import com.mercadopago.android.px.internal.repository.UserSelectionRepository
import com.mercadopago.android.px.model.AmountConfiguration
import com.mercadopago.android.px.model.PaymentTypes.*
import java.io.File

private const val AMOUNT_CONFIGURATION = "amount_configuration_repository"

internal class AmountConfigurationRepositoryImpl(private val fileManager: FileManager,
    private val userSelectionRepository: UserSelectionRepository,
    private val configurationSolver: ConfigurationSolver) :
    AbstractLocalRepository<String>(fileManager), AmountConfigurationRepository {

    override val file: File = fileManager.create(AMOUNT_CONFIGURATION)

    @Throws(IllegalStateException::class)
    override fun getCurrentConfiguration(): AmountConfiguration {
        val paymentMethod = userSelectionRepository.paymentMethod
            ?: throw IllegalStateException("Payer costs shouldn't be requested without a selected payment method")
        return when {
            isCardPaymentType(paymentMethod.paymentTypeId) -> {
                userSelectionRepository.card?.id?.let { configurationSolver.getAmountConfigurationFor(it, paymentMethod.paymentTypeId) }
            }
            isAccountMoney(paymentMethod.paymentTypeId) || isDigitalCurrency(paymentMethod.paymentTypeId) -> {
                configurationSolver.getAmountConfigurationFor(paymentMethod.id, paymentMethod.paymentTypeId)
            }
            else -> null
        } ?: throw IllegalStateException("Couldn't find valid current configuration")
    }

    override fun getConfigurationFor(customOptionId: String, paymentMethodType: String): AmountConfiguration? {
        return configurationSolver.getAmountConfigurationFor(customOptionId, paymentMethodType)
    }

    override fun readFromStorage() = fileManager.readText(file)
}