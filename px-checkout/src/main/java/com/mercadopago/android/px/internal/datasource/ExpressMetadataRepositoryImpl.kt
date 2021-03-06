package com.mercadopago.android.px.internal.datasource

import com.mercadopago.android.px.internal.core.FileManager
import com.mercadopago.android.px.internal.repository.DisabledPaymentMethodRepository
import com.mercadopago.android.px.internal.repository.ExpressMetadataRepository
import com.mercadopago.android.px.model.ExpressMetadata
import com.mercadopago.android.px.model.internal.DisabledPaymentMethod
import java.io.File

private const val EXPRESS_METADATA = "express_metadata_repository"

internal class ExpressMetadataRepositoryImpl(private val fileManager: FileManager,
    private val disabledPaymentMethodRepository: DisabledPaymentMethodRepository) :
    AbstractLocalRepository<List<ExpressMetadata>>(fileManager), ExpressMetadataRepository {

    override val file: File = fileManager.create(EXPRESS_METADATA)

    override fun readFromStorage() = fileManager.readAnyList(file, ExpressMetadata::class.java)

    override fun sortByState() {
        val disabledPaymentMethodMap: Map<String, DisabledPaymentMethod> = disabledPaymentMethodRepository.disabledPaymentMethods
        ExpressMetadataSorter(value, disabledPaymentMethodMap).sort()
        configure(value)
    }
}