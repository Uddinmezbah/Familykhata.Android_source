package com.familykhata.app.dealerbusiness

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.familykhata.app.businessDataKey
import com.familykhata.app.baseWorkspaceKey
import com.familykhata.app.businessIdFromWorkspaceKey
import com.familykhata.app.data.InventoryDatabase
import com.familykhata.app.data.ProductEntity
import com.familykhata.app.data.ProductUnitConversionEntity
import com.familykhata.app.data.StockBatchEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

data class DealerPurchaseLineInput(
    val productId: Long,
    val quantity: Int,
    val unitCost: Double,
    val batchNo: String = ""
)

data class DealerSaleLineInput(
    val productId: Long,
    val quantity: Int,
    val unitPrice: Double
)

data class DealerDeliveryChallanLineInput(
    val productId: Long,
    val boxCount: Int = 0,
    val sheetCount: Int = 0,
    val loosePieces: Int = 0,
    val unitName: String = "",
    val unitFactor: Int = 1,
    val quantity: Int = 0
)

data class DealerDeliverySaleLineInput(
    val challanLineId: Long,
    val quantityPieces: Int,
    val unitPrice: Double
)

data class DealerDeliverySettlementLineInput(
    val challanLineId: Long,
    val returnedPieces: Int = 0,
    val damagedPieces: Int = 0,
    val note: String = ""
)

data class DealerDeliveryLineStatus(
    val line: DealerDeliveryChallanLineEntity,
    val soldPieces: Int
) {
    val remainingPieces: Int
        get() =
            (
                line.quantityPieces -
                    soldPieces
            ).coerceAtLeast(0)
}

@OptIn(ExperimentalCoroutinesApi::class)
class DealerBusinessViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val database =
        InventoryDatabase.get(application)

    private val dao =
        database.dealerBusinessDao()

    private val inventoryDao =
        database.dao()

    private val workspace =
        MutableStateFlow("SHOP")

    private val businessKey =
        MutableStateFlow("dealer_business")

    val companies: StateFlow<List<DealerCompanyEntity>> =
        workspace
            .flatMapLatest {
                dao.observeCompanies(it)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    val areas: StateFlow<List<DealerAreaEntity>> =
        workspace
            .flatMapLatest {
                dao.observeAreas(it)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    val customers: StateFlow<List<DealerCustomerEntity>> =
        workspace
            .flatMapLatest {
                dao.observeCustomers(it)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    val customerLedgers:
        StateFlow<List<DealerCustomerLedgerSummary>> =
        workspace
            .flatMapLatest {
                dao.observeCustomerLedgerSummaries(
                    it
                )
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    val purchases: StateFlow<List<DealerPurchaseEntity>> =
        workspace
            .flatMapLatest {
                dao.observePurchases(it)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    val sales: StateFlow<List<DealerSaleEntity>> =
        workspace
            .flatMapLatest {
                dao.observeSales(it)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    val expenses: StateFlow<List<DealerExpenseEntity>> =
        workspace
            .flatMapLatest {
                dao.observeExpenses(it)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    val collections:
        StateFlow<List<DealerCollectionEntity>> =
        workspace
            .flatMapLatest {
                dao.observeAllCollections(it)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    val supplierPayments:
        StateFlow<List<DealerSupplierPaymentEntity>> =
        workspace
            .flatMapLatest {
                dao.observeAllSupplierPayments(it)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    val productPacks:
        StateFlow<List<DealerProductPackEntity>> =
        workspace
            .flatMapLatest {
                dao.observeProductPacks(it)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    val deliveryPeople:
        StateFlow<List<DealerDeliveryPersonEntity>> =
        workspace
            .flatMapLatest {
                dao.observeDeliveryPeople(it)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    val deliveryChallans:
        StateFlow<List<DealerDeliveryChallanEntity>> =
        workspace
            .flatMapLatest {
                dao.observeDeliveryChallans(it)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    val damages:
        StateFlow<List<DealerDamageEntity>> =
        workspace
            .flatMapLatest {
                dao.observeDamages(it)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    val products: StateFlow<List<ProductEntity>> =
        workspace
            .flatMapLatest { workspaceValue ->
                inventoryDao.observeProductsForBusiness(
                    workspace = baseWorkspaceKey(workspaceValue),
                    businessKey = businessKey.value
                )
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    fun setBusinessContext(
        workspaceValue: String,
        shopType: String
    ) {
        val legacyKey =
            businessDataKey(shopType)
        val key =
            businessIdFromWorkspaceKey(
                workspaceValue
            ) ?: legacyKey

        workspace.value = workspaceValue
        businessKey.value = key

}

    fun observeProductBatches(
        productId: Long
    ): Flow<List<StockBatchEntity>> =
        inventoryDao.observeBatches(
            productId
        )

    fun observeProductUnitConversions(
        productId: Long
    ): Flow<List<ProductUnitConversionEntity>> =
        inventoryDao.observeProductUnitConversions(
            productId
        )

    suspend fun loadDeliveryLineStatuses(
        challanId: Long
    ): List<DealerDeliveryLineStatus> {
        if (challanId <= 0) {
            return emptyList()
        }

        val challan =
            dao.getDeliveryChallanOnce(
                challanId
            ) ?: return emptyList()

        if (
            challan.workspace !=
            workspace.value
        ) {
            return emptyList()
        }

        return dao
            .getDeliveryChallanLinesOnce(
                challanId
            )
            .map { line ->
                DealerDeliveryLineStatus(
                    line = line,
                    soldPieces =
                        dao.getDeliverySoldQuantityForLine(
                            line.id
                        )
                )
            }
    }

    fun observePurchaseLines(
        purchaseId: Long
    ): Flow<List<DealerPurchaseLineEntity>> =
        dao.observePurchaseLines(purchaseId)

    fun observeSaleLines(
        saleId: Long
    ): Flow<List<DealerSaleLineEntity>> =
        dao.observeSaleLines(saleId)

    fun observeCollections(
        customerId: Long
    ): Flow<List<DealerCollectionEntity>> =
        dao.observeCollections(customerId)

    fun observeSupplierPayments(
        companyId: Long
    ): Flow<List<DealerSupplierPaymentEntity>> =
        dao.observeSupplierPayments(companyId)

    fun observeSalesReturns(
        saleId: Long
    ): Flow<List<DealerSalesReturnEntity>> =
        dao.observeSalesReturns(saleId)

    fun observePurchaseReturns(
        purchaseId: Long
    ): Flow<List<DealerPurchaseReturnEntity>> =
        dao.observePurchaseReturns(purchaseId)

    private fun packedQuantityPieces(
        boxCount: Int,
        sheetCount: Int,
        loosePieces: Int,
        pack: DealerProductPackEntity
    ): Int {
        require(boxCount >= 0)
        require(sheetCount >= 0)
        require(loosePieces >= 0)

        if (boxCount > 0) {
            require(
                pack.piecesPerBox > 0
            )
        }

        if (sheetCount > 0) {
            require(
                pack.piecesPerSheet > 0
            )
        }

        return (
            boxCount *
                pack.piecesPerBox
            ) +
            (
                sheetCount *
                    pack.piecesPerSheet
            ) +
            loosePieces
    }

    fun saveProductPack(
        productId: Long,
        piecesPerBox: Int,
        piecesPerSheet: Int,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            productId <= 0 ||
            piecesPerBox < 0 ||
            piecesPerSheet < 0
        ) {
            onDone(false)
            return
        }

        val currentWorkspace =
            workspace.value

        val currentBusinessKey =
            businessKey.value

        viewModelScope.launch {
            val success =
                runCatching {
                    database.withTransaction {
                        val product =
                            requireNotNull(
                                inventoryDao
                                    .getProductOnce(
                                        productId
                                    )
                            )

                        require(
                            product.workspace == baseWorkspaceKey(currentWorkspace)
                        )

                        require(
                            product.businessKey ==
                                currentBusinessKey
                        )

                        val old =
                            dao.getProductPackOnce(
                                product.id
                            )

                        dao.upsertProductPack(
                            DealerProductPackEntity(
                                productId =
                                    product.id,
                                piecesPerBox =
                                    piecesPerBox,
                                piecesPerSheet =
                                    piecesPerSheet,
                                workspace =
                                    currentWorkspace,
                                createdAt =
                                    old?.createdAt
                                        ?: System.currentTimeMillis(),
                                updatedAt =
                                    System.currentTimeMillis()
                            )
                        )
                    }
                }.isSuccess

            onDone(success)
        }
    }

    fun addDeliveryPerson(
        name: String,
        phone: String = "",
        note: String = "",
        onDone: (Boolean) -> Unit = {}
    ) {
        if (name.isBlank()) {
            onDone(false)
            return
        }

        val currentWorkspace =
            workspace.value

        viewModelScope.launch {
            val success =
                runCatching {
                    dao.insertDeliveryPerson(
                        DealerDeliveryPersonEntity(
                            name = name.trim(),
                            phone = phone.trim(),
                            note = note.trim(),
                            workspace =
                                currentWorkspace
                        )
                    ) > 0
                }.getOrDefault(false)

            onDone(success)
        }
    }

    fun updateDeliveryPerson(
        item: DealerDeliveryPersonEntity,
        name: String,
        phone: String = "",
        note: String = "",
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            item.id <= 0 ||
            name.isBlank() ||
            item.workspace != workspace.value
        ) {
            onDone(false)
            return
        }

        viewModelScope.launch {
            val success =
                runCatching {
                    dao.updateDeliveryPerson(
                        item.copy(
                            name = name.trim(),
                            phone = phone.trim(),
                            note = note.trim()
                        )
                    ) == 1
                }.getOrDefault(false)

            onDone(success)
        }
    }

    fun createDeliveryChallan(
        deliveryPersonId: Long,
        challanNo: String,
        lines: List<DealerDeliveryChallanLineInput>,
        note: String = "",
        issuedAt: Long =
            System.currentTimeMillis(),
        onDone: (Boolean) -> Unit = {}
    ) {
        val cleanLines =
            lines.filter {
                val legacyQuantityEntered =
                    it.boxCount > 0 ||
                        it.sheetCount > 0 ||
                        it.loosePieces > 0

                val genericQuantityEntered =
                    it.quantity > 0

                it.productId > 0 &&
                    it.boxCount >= 0 &&
                    it.sheetCount >= 0 &&
                    it.loosePieces >= 0 &&
                    it.quantity >= 0 &&
                    it.unitFactor > 0 &&
                    (
                        legacyQuantityEntered xor
                            genericQuantityEntered
                    )
            }

        if (
            deliveryPersonId <= 0 ||
            cleanLines.isEmpty()
        ) {
            onDone(false)
            return
        }

        val currentWorkspace =
            workspace.value

        val currentBusinessKey =
            businessKey.value

        viewModelScope.launch {
            val success =
                runCatching {
                    database.withTransaction {
                        val deliveryPerson =
                            requireNotNull(
                                dao.getDeliveryPersonOnce(
                                    deliveryPersonId
                                )
                            )

                        require(
                            deliveryPerson.workspace ==
                                currentWorkspace
                        )

                        val challanId =
                            dao.insertDeliveryChallan(
                                DealerDeliveryChallanEntity(
                                    deliveryPersonId =
                                        deliveryPerson.id,
                                    deliveryPersonNameSnapshot =
                                        deliveryPerson.name,
                                    challanNo =
                                        challanNo
                                            .trim()
                                            .ifBlank {
                                                "DC-$issuedAt"
                                            },
                                    issuedAt =
                                        issuedAt,
                                    note =
                                        note.trim(),
                                    workspace =
                                        currentWorkspace
                                )
                            )

                        for (input in cleanLines) {
                            val product =
                                requireNotNull(
                                    inventoryDao
                                        .getProductOnce(
                                            input.productId
                                        )
                                )

                            require(
                                product.workspace == baseWorkspaceKey(currentWorkspace)
                            )

                            require(
                                product.businessKey ==
                                    currentBusinessKey
                            )

                            val pack =
                                dao.getProductPackOnce(
                                    product.id
                                )
                                    ?: DealerProductPackEntity(
                                        productId =
                                            product.id,
                                        workspace =
                                            currentWorkspace
                                    )

                            val legacyRequested =
                                input.boxCount > 0 ||
                                    input.sheetCount > 0 ||
                                    input.loosePieces > 0

                            val genericRequested =
                                input.quantity > 0

                            require(
                                legacyRequested xor
                                    genericRequested
                            )

                            val baseUnitName =
                                product.unit
                                    .trim()
                                    .ifBlank {
                                        "pcs"
                                    }

                            val baseUnitKey =
                                baseUnitName.lowercase(
                                    Locale.ROOT
                                )

                            var resolvedUnitName =
                                baseUnitName

                            var resolvedUnitFactor =
                                1

                            val enteredQuantity: Int
                            val pieces: Int

                            if (genericRequested) {
                                val requestedUnitKey =
                                    input.unitName
                                        .trim()
                                        .lowercase(
                                            Locale.ROOT
                                        )

                                if (
                                    requestedUnitKey.isBlank() ||
                                    requestedUnitKey ==
                                        baseUnitKey
                                ) {
                                    require(
                                        input.unitFactor ==
                                            1
                                    ) {
                                        "Invalid base unit factor"
                                    }

                                    resolvedUnitName =
                                        baseUnitName

                                    resolvedUnitFactor =
                                        1
                                } else {
                                    val conversion =
                                        requireNotNull(
                                            inventoryDao
                                                .getProductUnitConversionsOnce(
                                                    product.id
                                                )
                                                .firstOrNull {
                                                    it.unitKey ==
                                                        requestedUnitKey
                                                }
                                        ) {
                                            "Product unit not found"
                                        }

                                    require(
                                        conversion.baseQuantity ==
                                            input.unitFactor
                                    ) {
                                        "Product unit factor changed"
                                    }

                                    require(
                                        conversion.baseQuantity >
                                            1
                                    )

                                    resolvedUnitName =
                                        conversion.unitName

                                    resolvedUnitFactor =
                                        conversion.baseQuantity
                                }

                                val baseQuantityLong =
                                    input.quantity.toLong() *
                                        resolvedUnitFactor
                                            .toLong()

                                require(
                                    baseQuantityLong in
                                        1L..
                                        Int.MAX_VALUE.toLong()
                                ) {
                                    "Delivery quantity overflow"
                                }

                                enteredQuantity =
                                    input.quantity

                                pieces =
                                    baseQuantityLong.toInt()
                            } else {
                                pieces =
                                    packedQuantityPieces(
                                        boxCount =
                                            input.boxCount,
                                        sheetCount =
                                            input.sheetCount,
                                        loosePieces =
                                            input.loosePieces,
                                        pack =
                                            pack
                                    )

                                enteredQuantity =
                                    pieces

                                resolvedUnitName =
                                    baseUnitName

                                resolvedUnitFactor =
                                    1
                            }

                            require(pieces > 0)

                            val batches =
                                inventoryDao
                                    .getBatchesOnce(
                                        product.id
                                    )
                                    .filter {
                                        it.quantity > 0
                                    }

                            require(
                                batches.sumOf {
                                    it.quantity
                                } >= pieces
                            )

                            val lineId =
                                dao.insertDeliveryChallanLine(
                                    DealerDeliveryChallanLineEntity(
                                        challanId =
                                            challanId,
                                        productId =
                                            product.id,
                                        productNameSnapshot =
                                            product.name,
                                        boxCount =
                                            if (
                                                genericRequested
                                            ) {
                                                0
                                            } else {
                                                input.boxCount
                                            },
                                        sheetCount =
                                            if (
                                                genericRequested
                                            ) {
                                                0
                                            } else {
                                                input.sheetCount
                                            },
                                        loosePieces =
                                            if (
                                                genericRequested
                                            ) {
                                                0
                                            } else {
                                                input.loosePieces
                                            },
                                        piecesPerBoxSnapshot =
                                            if (
                                                genericRequested
                                            ) {
                                                0
                                            } else {
                                                pack.piecesPerBox
                                            },
                                        piecesPerSheetSnapshot =
                                            if (
                                                genericRequested
                                            ) {
                                                0
                                            } else {
                                                pack.piecesPerSheet
                                            },
                                        quantityPieces =
                                            pieces,
                                        unitSnapshot =
                                            resolvedUnitName,
                                        unitFactor =
                                            resolvedUnitFactor,
                                        enteredQuantity =
                                            enteredQuantity
                                    )
                                )

                            var remaining =
                                pieces

                            for (batch in batches) {
                                if (remaining <= 0) {
                                    break
                                }

                                val used =
                                    minOf(
                                        batch.quantity,
                                        remaining
                                    )

                                dao.insertDeliveryChallanAllocation(
                                    DealerDeliveryChallanAllocationEntity(
                                        challanLineId =
                                            lineId,
                                        sourceStockBatchId =
                                            batch.id,
                                        batchNoSnapshot =
                                            batch.batchNo,
                                        quantityPieces =
                                            used,
                                        unitCost =
                                            batch.purchasePrice,
                                        totalCost =
                                            used *
                                                batch.purchasePrice
                                    )
                                )

                                inventoryDao
                                    .updateBatchQuantity(
                                        batch.id,
                                        batch.quantity -
                                            used
                                    )

                                remaining -= used
                            }

                            require(
                                remaining == 0
                            )
                        }
                    }
                }.isSuccess

            onDone(success)
        }
    }

    fun updateDeliveryChallanMeta(
        item: DealerDeliveryChallanEntity,
        deliveryPersonId: Long,
        challanNo: String,
        issuedAt: Long,
        note: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            item.id <= 0 ||
            deliveryPersonId <= 0 ||
            item.workspace != workspace.value
        ) {
            onDone(false)
            return
        }

        val currentWorkspace =
            workspace.value

        viewModelScope.launch {
            val success =
                runCatching {
                    database.withTransaction {
                        val fresh =
                            requireNotNull(
                                dao.getDeliveryChallanOnce(
                                    item.id
                                )
                            )

                        require(
                            fresh.workspace ==
                                currentWorkspace
                        )

                        val person =
                            requireNotNull(
                                dao.getDeliveryPersonOnce(
                                    deliveryPersonId
                                )
                            )

                        require(
                            person.workspace ==
                                currentWorkspace
                        )

                        require(
                            dao.updateDeliveryChallan(
                                fresh.copy(
                                    deliveryPersonId =
                                        person.id,
                                    deliveryPersonNameSnapshot =
                                        person.name,
                                    challanNo =
                                        challanNo
                                            .trim()
                                            .ifBlank {
                                                fresh.challanNo
                                            },
                                    issuedAt =
                                        issuedAt,
                                    note =
                                        note.trim()
                                )
                            ) == 1
                        )
                    }
                }.isSuccess

            onDone(success)
        }
    }

    fun deleteDeliveryChallan(
        item: DealerDeliveryChallanEntity,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            item.id <= 0 ||
            item.workspace != workspace.value
        ) {
            onDone(false)
            return
        }

        val currentWorkspace =
            workspace.value

        viewModelScope.launch {
            val success =
                runCatching {
                    database.withTransaction {
                        val challan =
                            requireNotNull(
                                dao.getDeliveryChallanOnce(
                                    item.id
                                )
                            )

                        require(
                            challan.workspace ==
                                currentWorkspace
                        )

                        /*
                         * Once a challan has sale or settlement,
                         * use sale correction / settlement reopen.
                         */
                        require(
                            challan.status ==
                                "OPEN"
                        )

                        require(
                            dao.getDeliveryChallanSalesOnce(
                                challan.id
                            ).isEmpty()
                        )

                        require(
                            dao.getDeliverySettlementOnce(
                                challan.id
                            ) == null
                        )

                        val lines =
                            dao.getDeliveryChallanLinesOnce(
                                challan.id
                            ).associateBy {
                                it.id
                            }

                        val allocations =
                            dao
                                .getDeliveryChallanAllocationsOnce(
                                    challan.id
                                )

                        for (
                            allocation in
                            allocations
                        ) {
                            val line =
                                requireNotNull(
                                    lines[
                                        allocation
                                            .challanLineId
                                    ]
                                )

                            restoreDeliveryStock(
                                line = line,
                                allocation =
                                    allocation,
                                quantity =
                                    allocation
                                        .quantityPieces,
                                receivedAt =
                                    System
                                        .currentTimeMillis()
                            )
                        }

                        require(
                            dao.deleteDeliveryChallanById(
                                challan.id,
                                currentWorkspace
                            ) == 1
                        )
                    }
                }.isSuccess

            onDone(success)
        }
    }

    suspend fun loadDeliverySettlement(
        challanId: Long
    ): DealerDeliverySettlementEntity? {
        if (challanId <= 0) {
            return null
        }

        val challan =
            dao.getDeliveryChallanOnce(
                challanId
            ) ?: return null

        if (
            challan.workspace !=
                workspace.value
        ) {
            return null
        }

        return dao.getDeliverySettlementOnce(
            challanId
        )
    }

    fun createDeliverySale(
        challanId: Long,
        customerId: Long,
        invoiceNo: String,
        lines: List<DealerDeliverySaleLineInput>,
        collectedNow: Double = 0.0,
        note: String = "",
        soldAt: Long =
            System.currentTimeMillis(),
        onDone: (Boolean) -> Unit = {}
    ) {
        val cleanLines =
            lines.filter {
                it.challanLineId > 0 &&
                    it.quantityPieces > 0 &&
                    it.unitPrice > 0
            }

        if (
            challanId <= 0 ||
            customerId <= 0 ||
            cleanLines.isEmpty() ||
            collectedNow < 0
        ) {
            onDone(false)
            return
        }

        val currentWorkspace =
            workspace.value

        val currentBusinessKey =
            businessKey.value

        viewModelScope.launch {
            val success =
                runCatching {
                    database.withTransaction {
                        val challan =
                            requireNotNull(
                                dao.getDeliveryChallanOnce(
                                    challanId
                                )
                            )

                        require(
                            challan.workspace ==
                                currentWorkspace
                        )

                        require(
                            challan.status == "OPEN"
                        )

                        require(
                            dao.getDeliverySettlementOnce(
                                challan.id
                            ) == null
                        )

                        val customer =
                            requireNotNull(
                                dao.getCustomerOnce(
                                    customerId
                                )
                            )

                        require(
                            customer.workspace ==
                                currentWorkspace
                        )

                        val resolved =
                            cleanLines.map { input ->
                                val challanLine =
                                    requireNotNull(
                                        dao.getDeliveryChallanLineOnce(
                                            input.challanLineId
                                        )
                                    )

                                require(
                                    challanLine.challanId ==
                                        challan.id
                                )

                                val productId =
                                    requireNotNull(
                                        challanLine.productId
                                    )

                                val product =
                                    requireNotNull(
                                        inventoryDao
                                            .getProductOnce(
                                                productId
                                            )
                                    )

                                require(
                                    product.workspace == baseWorkspaceKey(currentWorkspace)
                                )

                                require(
                                    product.businessKey ==
                                        currentBusinessKey
                                )

                                val alreadySold =
                                    dao.getDeliverySoldQuantityForLine(
                                        challanLine.id
                                    )

                                require(
                                    alreadySold +
                                        input.quantityPieces <=
                                        challanLine.quantityPieces
                                )

                                Triple(
                                    input,
                                    challanLine,
                                    product
                                )
                            }

                        val total =
                            resolved.sumOf {
                                it.first.quantityPieces *
                                    it.first.unitPrice
                            }

                        val existingSales =
                            dao.getSalesForCustomerOnce(
                                customer.id,
                                currentWorkspace
                            )

                        var existingDue = 0.0

                        for (sale in existingSales) {
                            val saleTotal =
                                dao.getSaleTotal(
                                    sale.id
                                )

                            val returnTotal =
                                dao.getSaleReturnTotal(
                                    sale.id
                                )

                            val collected =
                                dao.getSaleCollected(
                                    sale.id
                                )

                            existingDue +=
                                (
                                    saleTotal -
                                        returnTotal -
                                        collected
                                ).coerceAtLeast(0.0)
                        }

                        val oldCollections =
                            dao.getCollectionsForCustomerOnce(
                                customer.id,
                                currentWorkspace
                            )

                        var availableAdvance = 0.0

                        for (
                            collection in
                            oldCollections
                        ) {
                            val allocated =
                                dao.getCollectionAllocated(
                                    collection.id
                                )

                            availableAdvance +=
                                (
                                    collection.amount -
                                        allocated
                                ).coerceAtLeast(0.0)
                        }

                        val newDue =
                            (
                                total -
                                    availableAdvance -
                                    collectedNow
                            ).coerceAtLeast(0.0)

                        if (
                            customer.creditLimit > 0.0
                        ) {
                            require(
                                existingDue +
                                    newDue <=
                                    customer.creditLimit +
                                        0.0001
                            )
                        }

                        val saleId =
                            dao.insertSale(
                                DealerSaleEntity(
                                    customerId =
                                        customer.id,
                                    customerNameSnapshot =
                                        customer.name,
                                    invoiceNo =
                                        invoiceNo
                                            .trim(),
                                    soldAt =
                                        soldAt,
                                    note =
                                        note.trim(),
                                    workspace =
                                        currentWorkspace
                                )
                            )

                        dao.insertDeliveryChallanSale(
                            DealerDeliveryChallanSaleEntity(
                                challanId =
                                    challan.id,
                                saleId =
                                    saleId
                            )
                        )

                        for (
                            resolvedLine in
                            resolved
                        ) {
                            val input =
                                resolvedLine.first

                            val challanLine =
                                resolvedLine.second

                            val product =
                                resolvedLine.third

                            val saleLineId =
                                dao.insertSaleLine(
                                    DealerSaleLineEntity(
                                        saleId =
                                            saleId,
                                        productId =
                                            product.id,
                                        productNameSnapshot =
                                            product.name,
                                        quantity =
                                            input.quantityPieces,
                                        unitPrice =
                                            input.unitPrice,
                                        lineTotal =
                                            input.quantityPieces *
                                                input.unitPrice
                                    )
                                )

                            val allocations =
                                dao.getDeliveryAllocationsForLineOnce(
                                    challanLine.id
                                )

                            var remaining =
                                input.quantityPieces

                            for (
                                allocation in
                                allocations
                            ) {
                                if (remaining <= 0) {
                                    break
                                }

                                val usedBefore =
                                    dao.getDeliverySoldQuantityForAllocation(
                                        allocation.id
                                    )

                                val free =
                                    (
                                        allocation.quantityPieces -
                                            usedBefore
                                    ).coerceAtLeast(0)

                                if (free <= 0) {
                                    continue
                                }

                                val used =
                                    minOf(
                                        free,
                                        remaining
                                    )

                                dao.insertDeliverySaleAllocation(
                                    DealerDeliverySaleAllocationEntity(
                                        challanLineId =
                                            challanLine.id,
                                        challanAllocationId =
                                            allocation.id,
                                        saleLineId =
                                            saleLineId,
                                        quantityPieces =
                                            used,
                                        unitCost =
                                            allocation.unitCost,
                                        totalCost =
                                            used *
                                                allocation.unitCost
                                    )
                                )

                                /*
                                 * Existing sale-return / COGS logic
                                 * reads DealerStockAllocationEntity.
                                 * Stock itself is NOT reduced here:
                                 * it was already removed when the
                                 * delivery challan was issued.
                                 */
                                dao.insertStockAllocation(
                                    DealerStockAllocationEntity(
                                        saleLineId =
                                            saleLineId,
                                        sourceStockBatchId =
                                            allocation.sourceStockBatchId,
                                        batchNoSnapshot =
                                            allocation.batchNoSnapshot,
                                        quantity =
                                            used,
                                        unitCost =
                                            allocation.unitCost,
                                        totalCost =
                                            used *
                                                allocation.unitCost
                                    )
                                )

                                remaining -= used
                            }

                            require(
                                remaining == 0
                            )
                        }

                        var due = total

                        for (
                            collection in
                            oldCollections
                        ) {
                            if (due <= 0.0001) {
                                break
                            }

                            val allocated =
                                dao.getCollectionAllocated(
                                    collection.id
                                )

                            val free =
                                (
                                    collection.amount -
                                        allocated
                                ).coerceAtLeast(0.0)

                            val use =
                                minOf(
                                    free,
                                    due
                                )

                            if (use > 0.0001) {
                                dao.insertCollectionAllocation(
                                    DealerCollectionAllocationEntity(
                                        collectionId =
                                            collection.id,
                                        saleId =
                                            saleId,
                                        amount =
                                            use
                                    )
                                )

                                due -= use
                            }
                        }

                        if (
                            collectedNow > 0.0001
                        ) {
                            val collectionId =
                                dao.insertCollection(
                                    DealerCollectionEntity(
                                        customerId =
                                            customer.id,
                                        customerNameSnapshot =
                                            customer.name,
                                        amount =
                                            collectedNow,
                                        collectedAt =
                                            soldAt,
                                        note =
                                            "Delivery ${challan.challanNo}",
                                        workspace =
                                            currentWorkspace
                                    )
                                )

                            val use =
                                minOf(
                                    collectedNow,
                                    due.coerceAtLeast(
                                        0.0
                                    )
                                )

                            if (use > 0.0001) {
                                dao.insertCollectionAllocation(
                                    DealerCollectionAllocationEntity(
                                        collectionId =
                                            collectionId,
                                        saleId =
                                            saleId,
                                        amount =
                                            use
                                    )
                                )

                                due -= use
                            }
                        }

                        val status =
                            when {
                                due <= 0.0001 ->
                                    "PAID"

                                due <
                                    total -
                                        0.0001 ->
                                    "PARTIAL"

                                else ->
                                    "OPEN"
                            }

                        dao.updateSaleStatus(
                            saleId,
                            status
                        )
                    }
                }.isSuccess

            onDone(success)
        }
    }

    private suspend fun restoreDeliveryStock(
        line: DealerDeliveryChallanLineEntity,
        allocation:
            DealerDeliveryChallanAllocationEntity,
        quantity: Int,
        receivedAt: Long
    ) {
        if (quantity <= 0) {
            return
        }

        val productId =
            requireNotNull(
                line.productId
            )

        val oldBatch =
            allocation.sourceStockBatchId
                ?.let {
                    inventoryDao
                        .getBatchOnce(it)
                }

        if (oldBatch != null) {
            require(
                oldBatch.productId ==
                    productId
            )

            inventoryDao.updateBatchQuantity(
                oldBatch.id,
                oldBatch.quantity +
                    quantity
            )
        } else {
            inventoryDao.insertBatch(
                StockBatchEntity(
                    productId =
                        productId,
                    batchNo =
                        allocation
                            .batchNoSnapshot,
                    quantity =
                        quantity,
                    purchasePrice =
                        allocation.unitCost,
                    purchaseDate =
                        receivedAt
                )
            )
        }
    }

    fun settleDeliveryChallan(
        challanId: Long,
        lines:
            List<DealerDeliverySettlementLineInput>,
        cashHandedOver: Double = 0.0,
        note: String = "",
        receivedAt: Long =
            System.currentTimeMillis(),
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            challanId <= 0 ||
            lines.isEmpty() ||
            cashHandedOver < 0
        ) {
            onDone(false)
            return
        }

        val currentWorkspace =
            workspace.value

        viewModelScope.launch {
            val success =
                runCatching {
                    database.withTransaction {
                        val challan =
                            requireNotNull(
                                dao.getDeliveryChallanOnce(
                                    challanId
                                )
                            )

                        require(
                            challan.workspace ==
                                currentWorkspace
                        )

                        require(
                            challan.status == "OPEN"
                        )

                        require(
                            dao.getDeliverySettlementOnce(
                                challan.id
                            ) == null
                        )

                        val challanLines =
                            dao.getDeliveryChallanLinesOnce(
                                challan.id
                            )

                        val inputMap =
                            lines.associateBy {
                                it.challanLineId
                            }

                        require(
                            inputMap.size ==
                                lines.size
                        )

                        require(
                            inputMap.keys ==
                                challanLines
                                    .map {
                                        it.id
                                    }
                                    .toSet()
                        )

                        val settlementId =
                            dao.insertDeliverySettlement(
                                DealerDeliverySettlementEntity(
                                    challanId =
                                        challan.id,
                                    cashHandedOver =
                                        cashHandedOver,
                                    receivedAt =
                                        receivedAt,
                                    note =
                                        note.trim()
                                )
                            )

                        for (
                            challanLine in
                            challanLines
                        ) {
                            val input =
                                requireNotNull(
                                    inputMap[
                                        challanLine.id
                                    ]
                                )

                            require(
                                input.returnedPieces >= 0
                            )

                            require(
                                input.damagedPieces >= 0
                            )

                            val soldPieces =
                                dao.getDeliverySoldQuantityForLine(
                                    challanLine.id
                                )

                            require(
                                soldPieces +
                                    input.returnedPieces +
                                    input.damagedPieces ==
                                    challanLine.quantityPieces
                            )

                            dao.insertDeliverySettlementLine(
                                DealerDeliverySettlementLineEntity(
                                    settlementId =
                                        settlementId,
                                    challanLineId =
                                        challanLine.id,
                                    soldPieces =
                                        soldPieces,
                                    returnedPieces =
                                        input.returnedPieces,
                                    damagedPieces =
                                        input.damagedPieces,
                                    note =
                                        input.note.trim()
                                )
                            )

                            var returnRemaining =
                                input.returnedPieces

                            var damageRemaining =
                                input.damagedPieces

                            val allocations =
                                dao.getDeliveryAllocationsForLineOnce(
                                    challanLine.id
                                )

                            for (
                                allocation in
                                allocations
                            ) {
                                val soldFromAllocation =
                                    dao.getDeliverySoldQuantityForAllocation(
                                        allocation.id
                                    )

                                var free =
                                    (
                                        allocation.quantityPieces -
                                            soldFromAllocation
                                    ).coerceAtLeast(0)

                                if (
                                    free <= 0
                                ) {
                                    continue
                                }

                                val returned =
                                    minOf(
                                        free,
                                        returnRemaining
                                    )

                                if (
                                    returned > 0
                                ) {
                                    restoreDeliveryStock(
                                        line =
                                            challanLine,
                                        allocation =
                                            allocation,
                                        quantity =
                                            returned,
                                        receivedAt =
                                            receivedAt
                                    )

                                    returnRemaining -=
                                        returned

                                    free -=
                                        returned
                                }

                                val damaged =
                                    minOf(
                                        free,
                                        damageRemaining
                                    )

                                if (
                                    damaged > 0
                                ) {
                                    dao.insertDamage(
                                        DealerDamageEntity(
                                            productId =
                                                challanLine.productId,
                                            sourceStockBatchId =
                                                allocation
                                                    .sourceStockBatchId,
                                            deliveryChallanId =
                                                challan.id,
                                            productNameSnapshot =
                                                challanLine
                                                    .productNameSnapshot,
                                            batchNoSnapshot =
                                                allocation
                                                    .batchNoSnapshot,
                                            quantityPieces =
                                                damaged,
                                            unitCost =
                                                allocation.unitCost,
                                            totalCost =
                                                damaged *
                                                    allocation.unitCost,
                                            sourceType =
                                                "DELIVERY",
                                            reason =
                                                "Night settlement damage",
                                            damagedAt =
                                                receivedAt,
                                            note =
                                                input.note.trim(),
                                            workspace =
                                                currentWorkspace
                                        )
                                    )

                                    damageRemaining -=
                                        damaged
                                }
                            }

                            require(
                                returnRemaining == 0
                            )

                            require(
                                damageRemaining == 0
                            )
                        }

                        dao.updateDeliveryChallan(
                            challan.copy(
                                settledAt =
                                    receivedAt,
                                status =
                                    "SETTLED"
                            )
                        )
                    }
                }.isSuccess

            onDone(success)
        }
    }

    fun updateDeliverySettlementMeta(
        challan: DealerDeliveryChallanEntity,
        item: DealerDeliverySettlementEntity,
        cashHandedOver: Double,
        receivedAt: Long,
        note: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            challan.id <= 0 ||
            item.id <= 0 ||
            cashHandedOver < 0 ||
            challan.workspace != workspace.value
        ) {
            onDone(false)
            return
        }

        val currentWorkspace =
            workspace.value

        viewModelScope.launch {
            val success =
                runCatching {
                    database.withTransaction {
                        val freshChallan =
                            requireNotNull(
                                dao.getDeliveryChallanOnce(
                                    challan.id
                                )
                            )

                        require(
                            freshChallan.workspace ==
                                currentWorkspace
                        )

                        require(
                            freshChallan.status ==
                                "SETTLED"
                        )

                        val freshSettlement =
                            requireNotNull(
                                dao.getDeliverySettlementOnce(
                                    freshChallan.id
                                )
                            )

                        require(
                            freshSettlement.id ==
                                item.id
                        )

                        require(
                            dao.updateDeliverySettlementCrud(
                                freshSettlement.copy(
                                    cashHandedOver =
                                        cashHandedOver,
                                    receivedAt =
                                        receivedAt,
                                    note =
                                        note.trim()
                                )
                            ) == 1
                        )

                        require(
                            dao.updateDeliveryChallan(
                                freshChallan.copy(
                                    settledAt =
                                        receivedAt
                                )
                            ) == 1
                        )
                    }
                }.isSuccess

            onDone(success)
        }
    }

    fun reopenDeliverySettlement(
        challanItem:
            DealerDeliveryChallanEntity,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            challanItem.id <= 0 ||
            challanItem.workspace != workspace.value
        ) {
            onDone(false)
            return
        }

        val currentWorkspace =
            workspace.value

        viewModelScope.launch {
            val success =
                runCatching {
                    database.withTransaction {
                        val challan =
                            requireNotNull(
                                dao.getDeliveryChallanOnce(
                                    challanItem.id
                                )
                            )

                        require(
                            challan.workspace ==
                                currentWorkspace
                        )

                        require(
                            challan.status ==
                                "SETTLED"
                        )

                        val settlement =
                            requireNotNull(
                                dao.getDeliverySettlementOnce(
                                    challan.id
                                )
                            )

                        val settlementLines =
                            dao.getDeliverySettlementLinesOnce(
                                settlement.id
                            )

                        val challanLines =
                            dao.getDeliveryChallanLinesOnce(
                                challan.id
                            ).associateBy {
                                it.id
                            }

                        require(
                            settlementLines.size ==
                                challanLines.size
                        )

                        /*
                         * First validate and reverse stock that
                         * was returned to warehouse at settlement.
                         */
                        for (
                            settlementLine in
                            settlementLines
                        ) {
                            val line =
                                requireNotNull(
                                    challanLines[
                                        settlementLine
                                            .challanLineId
                                    ]
                                )

                            val currentSold =
                                dao
                                    .getDeliverySoldQuantityForLine(
                                        line.id
                                    )

                            require(
                                currentSold ==
                                    settlementLine
                                        .soldPieces
                            )

                            require(
                                currentSold +
                                    settlementLine
                                        .returnedPieces +
                                    settlementLine
                                        .damagedPieces ==
                                    line.quantityPieces
                            )

                            var returnRemaining =
                                settlementLine
                                    .returnedPieces

                            val allocations =
                                dao
                                    .getDeliveryAllocationsForLineOnce(
                                        line.id
                                    )

                            for (
                                allocation in
                                allocations
                            ) {
                                if (
                                    returnRemaining <= 0
                                ) {
                                    break
                                }

                                val soldFromAllocation =
                                    dao
                                        .getDeliverySoldQuantityForAllocation(
                                            allocation.id
                                        )

                                val free =
                                    (
                                        allocation
                                            .quantityPieces -
                                            soldFromAllocation
                                    ).coerceAtLeast(0)

                                val returned =
                                    minOf(
                                        free,
                                        returnRemaining
                                    )

                                if (returned > 0) {
                                    val batchId =
                                        requireNotNull(
                                            allocation
                                                .sourceStockBatchId
                                        )

                                    val batch =
                                        requireNotNull(
                                            inventoryDao
                                                .getBatchOnce(
                                                    batchId
                                                )
                                        )

                                    /*
                                     * Returned goods are going
                                     * back to delivery custody.
                                     */
                                    require(
                                        batch.quantity >=
                                            returned
                                    )

                                    inventoryDao
                                        .updateBatchQuantity(
                                            batch.id,
                                            batch.quantity -
                                                returned
                                        )

                                    returnRemaining -=
                                        returned
                                }
                            }

                            require(
                                returnRemaining == 0
                            )
                        }

                        val deliveryDamages =
                            dao
                                .getDeliveryDamagesForChallanOnce(
                                    challan.id
                                )

                        val expectedDamage =
                            settlementLines.sumOf {
                                it.damagedPieces
                            }

                        require(
                            deliveryDamages.sumOf {
                                it.quantityPieces
                            } == expectedDamage
                        )

                        dao.deleteDeliveryDamagesForChallan(
                            challan.id
                        )

                        require(
                            dao.deleteDeliverySettlementById(
                                settlement.id
                            ) == 1
                        )

                        require(
                            dao.updateDeliveryChallan(
                                challan.copy(
                                    settledAt = null,
                                    status = "OPEN"
                                )
                            ) == 1
                        )
                    }
                }.isSuccess

            onDone(success)
        }
    }

    fun recordWarehouseDamage(
        batchId: Long,
        quantityPieces: Int,
        reason: String = "",
        note: String = "",
        damagedAt: Long =
            System.currentTimeMillis(),
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            batchId <= 0 ||
            quantityPieces <= 0
        ) {
            onDone(false)
            return
        }

        val currentWorkspace =
            workspace.value

        val currentBusinessKey =
            businessKey.value

        viewModelScope.launch {
            val success =
                runCatching {
                    database.withTransaction {
                        val batch =
                            requireNotNull(
                                inventoryDao
                                    .getBatchOnce(
                                        batchId
                                    )
                            )

                        val product =
                            requireNotNull(
                                inventoryDao
                                    .getProductOnce(
                                        batch.productId
                                    )
                            )

                        require(
                            product.workspace == baseWorkspaceKey(currentWorkspace)
                        )

                        require(
                            product.businessKey ==
                                currentBusinessKey
                        )

                        require(
                            quantityPieces <=
                                batch.quantity
                        )

                        dao.insertDamage(
                            DealerDamageEntity(
                                productId =
                                    product.id,
                                sourceStockBatchId =
                                    batch.id,
                                productNameSnapshot =
                                    product.name,
                                batchNoSnapshot =
                                    batch.batchNo,
                                quantityPieces =
                                    quantityPieces,
                                unitCost =
                                    batch.purchasePrice,
                                totalCost =
                                    quantityPieces *
                                        batch.purchasePrice,
                                sourceType =
                                    "WAREHOUSE",
                                reason =
                                    reason.trim(),
                                damagedAt =
                                    damagedAt,
                                note =
                                    note.trim(),
                                workspace =
                                    currentWorkspace
                            )
                        )

                        inventoryDao
                            .updateBatchQuantity(
                                batch.id,
                                batch.quantity -
                                    quantityPieces
                            )
                    }
                }.isSuccess

            onDone(success)
        }
    }

    fun updateWarehouseDamage(
        item: DealerDamageEntity,
        quantityPieces: Int,
        reason: String,
        note: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            item.id <= 0 ||
            quantityPieces <= 0 ||
            item.workspace != workspace.value ||
            item.sourceType != "WAREHOUSE"
        ) {
            onDone(false)
            return
        }

        val batchId =
            item.sourceStockBatchId

        if (batchId == null) {
            onDone(false)
            return
        }

        viewModelScope.launch {
            val success =
                runCatching {
                    database.withTransaction {
                        val batch =
                            requireNotNull(
                                inventoryDao.getBatchOnce(
                                    batchId
                                )
                            )

                        if (item.productId != null) {
                            require(
                                batch.productId ==
                                    item.productId
                            )
                        }

                        val delta =
                            quantityPieces -
                                item.quantityPieces

                        when {
                            delta > 0 -> {
                                require(
                                    batch.quantity >=
                                        delta
                                )

                                inventoryDao
                                    .updateBatchQuantity(
                                        batch.id,
                                        batch.quantity -
                                            delta
                                    )
                            }

                            delta < 0 -> {
                                inventoryDao
                                    .updateBatchQuantity(
                                        batch.id,
                                        batch.quantity +
                                            (-delta)
                                    )
                            }
                        }

                        require(
                            dao.updateDamage(
                                item.copy(
                                    quantityPieces =
                                        quantityPieces,
                                    totalCost =
                                        quantityPieces *
                                            item.unitCost,
                                    reason =
                                        reason.trim(),
                                    note =
                                        note.trim()
                                )
                            ) == 1
                        )
                    }
                }.isSuccess

            onDone(success)
        }
    }

    fun deleteWarehouseDamage(
        item: DealerDamageEntity,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            item.id <= 0 ||
            item.workspace != workspace.value ||
            item.sourceType != "WAREHOUSE"
        ) {
            onDone(false)
            return
        }

        val batchId =
            item.sourceStockBatchId

        if (batchId == null) {
            onDone(false)
            return
        }

        viewModelScope.launch {
            val success =
                runCatching {
                    database.withTransaction {
                        val batch =
                            requireNotNull(
                                inventoryDao.getBatchOnce(
                                    batchId
                                )
                            )

                        if (item.productId != null) {
                            require(
                                batch.productId ==
                                    item.productId
                            )
                        }

                        inventoryDao
                            .updateBatchQuantity(
                                batch.id,
                                batch.quantity +
                                    item.quantityPieces
                            )

                        require(
                            dao.deleteDamageById(
                                item.id,
                                item.workspace
                            ) == 1
                        )
                    }
                }.isSuccess

            onDone(success)
        }
    }

    fun addCompany(
        name: String,
        code: String,
        phone: String,
        contactPerson: String,
        address: String,
        note: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (name.isBlank()) {
            onDone(false)
            return
        }

        val currentWorkspace = workspace.value

        viewModelScope.launch {
            val success =
                runCatching {
                    dao.insertCompany(
                        DealerCompanyEntity(
                            name = name.trim(),
                            code = code.trim(),
                            phone = phone.trim(),
                            contactPerson =
                                contactPerson.trim(),
                            address = address.trim(),
                            note = note.trim(),
                            workspace = currentWorkspace
                        )
                    ) > 0
                }.getOrDefault(false)

            onDone(success)
        }
    }

    fun updateCompany(
        item: DealerCompanyEntity,
        name: String,
        code: String,
        phone: String,
        contactPerson: String,
        address: String,
        note: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            item.id <= 0 ||
            name.isBlank() ||
            item.workspace != workspace.value
        ) {
            onDone(false)
            return
        }

        viewModelScope.launch {
            val success =
                runCatching {
                    dao.updateCompany(
                        item.copy(
                            name = name.trim(),
                            code = code.trim(),
                            phone = phone.trim(),
                            contactPerson =
                                contactPerson.trim(),
                            address = address.trim(),
                            note = note.trim()
                        )
                    ) == 1
                }.getOrDefault(false)

            onDone(success)
        }
    }

    fun addArea(
        name: String,
        code: String,
        note: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (name.isBlank()) {
            onDone(false)
            return
        }

        val currentWorkspace = workspace.value

        viewModelScope.launch {
            val success =
                runCatching {
                    dao.insertArea(
                        DealerAreaEntity(
                            name = name.trim(),
                            code = code.trim(),
                            note = note.trim(),
                            workspace = currentWorkspace
                        )
                    ) > 0
                }.getOrDefault(false)

            onDone(success)
        }
    }

    fun updateArea(
        item: DealerAreaEntity,
        name: String,
        code: String,
        note: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            item.id <= 0 ||
            name.isBlank() ||
            item.workspace != workspace.value
        ) {
            onDone(false)
            return
        }

        viewModelScope.launch {
            val success =
                runCatching {
                    dao.updateArea(
                        item.copy(
                            name = name.trim(),
                            code = code.trim(),
                            note = note.trim()
                        )
                    ) == 1
                }.getOrDefault(false)

            onDone(success)
        }
    }

    fun addCustomer(
        areaId: Long?,
        name: String,
        customerCode: String,
        phone: String,
        address: String,
        creditLimit: Double,
        note: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (name.isBlank()) {
            onDone(false)
            return
        }

        val currentWorkspace = workspace.value

        viewModelScope.launch {
            val success =
                runCatching {
                    if (areaId != null) {
                        val area =
                            dao.getAreaOnce(areaId)

                        require(
                            area != null &&
                                area.workspace ==
                                    currentWorkspace
                        )
                    }

                    dao.insertCustomer(
                        DealerCustomerEntity(
                            areaId = areaId,
                            name = name.trim(),
                            customerCode =
                                customerCode.trim(),
                            phone = phone.trim(),
                            address = address.trim(),
                            creditLimit =
                                creditLimit.coerceAtLeast(0.0),
                            note = note.trim(),
                            workspace = currentWorkspace
                        )
                    ) > 0
                }.getOrDefault(false)

            onDone(success)
        }
    }

    fun updateCustomer(
        item: DealerCustomerEntity,
        areaId: Long?,
        name: String,
        customerCode: String,
        phone: String,
        address: String,
        creditLimit: Double,
        note: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            item.id <= 0 ||
            name.isBlank() ||
            item.workspace != workspace.value
        ) {
            onDone(false)
            return
        }

        val currentWorkspace = workspace.value

        viewModelScope.launch {
            val success =
                runCatching {
                    if (areaId != null) {
                        val area =
                            dao.getAreaOnce(areaId)

                        require(
                            area != null &&
                                area.workspace ==
                                    currentWorkspace
                        )
                    }

                    dao.updateCustomer(
                        item.copy(
                            areaId = areaId,
                            name = name.trim(),
                            customerCode =
                                customerCode.trim(),
                            phone = phone.trim(),
                            address = address.trim(),
                            creditLimit =
                                creditLimit.coerceAtLeast(0.0),
                            note = note.trim()
                        )
                    ) == 1
                }.getOrDefault(false)

            onDone(success)
        }
    }

    fun deleteCompany(
        item: DealerCompanyEntity,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            item.id <= 0 ||
            item.workspace != workspace.value
        ) {
            onDone(false)
            return
        }

        viewModelScope.launch {
            val success =
                runCatching {
                    dao.deleteCompanyById(
                        item.id,
                        item.workspace
                    ) == 1
                }.getOrDefault(false)

            onDone(success)
        }
    }

    fun deleteArea(
        item: DealerAreaEntity,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            item.id <= 0 ||
            item.workspace != workspace.value
        ) {
            onDone(false)
            return
        }

        viewModelScope.launch {
            val success =
                runCatching {
                    dao.deleteAreaById(
                        item.id,
                        item.workspace
                    ) == 1
                }.getOrDefault(false)

            onDone(success)
        }
    }

    fun deleteCustomer(
        item: DealerCustomerEntity,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            item.id <= 0 ||
            item.workspace != workspace.value
        ) {
            onDone(false)
            return
        }

        viewModelScope.launch {
            val success =
                runCatching {
                    dao.deleteCustomerById(
                        item.id,
                        item.workspace
                    ) == 1
                }.getOrDefault(false)

            onDone(success)
        }
    }

    fun deleteDeliveryPerson(
        item: DealerDeliveryPersonEntity,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            item.id <= 0 ||
            item.workspace != workspace.value
        ) {
            onDone(false)
            return
        }

        viewModelScope.launch {
            val success =
                runCatching {
                    dao.deleteDeliveryPersonById(
                        item.id,
                        item.workspace
                    ) == 1
                }.getOrDefault(false)

            onDone(success)
        }
    }

    fun deleteProductPack(
        item: DealerProductPackEntity,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            item.productId <= 0 ||
            item.workspace != workspace.value
        ) {
            onDone(false)
            return
        }

        viewModelScope.launch {
            val success =
                runCatching {
                    dao.deleteProductPackByProductId(
                        item.productId,
                        item.workspace
                    ) == 1
                }.getOrDefault(false)

            onDone(success)
        }
    }

    fun addExpense(
        category: String,
        amount: Double,
        note: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            category.isBlank() ||
            amount <= 0
        ) {
            onDone(false)
            return
        }

        val currentWorkspace = workspace.value

        viewModelScope.launch {
            val success =
                runCatching {
                    dao.insertExpense(
                        DealerExpenseEntity(
                            category = category.trim(),
                            amount = amount,
                            note = note.trim(),
                            workspace = currentWorkspace
                        )
                    ) > 0
                }.getOrDefault(false)

            onDone(success)
        }
    }

    fun updateExpense(
        item: DealerExpenseEntity,
        category: String,
        amount: Double,
        note: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            item.id <= 0 ||
            category.isBlank() ||
            amount <= 0 ||
            item.workspace != workspace.value
        ) {
            onDone(false)
            return
        }

        viewModelScope.launch {
            val success =
                runCatching {
                    dao.updateExpense(
                        item.copy(
                            category = category.trim(),
                            amount = amount,
                            note = note.trim()
                        )
                    ) == 1
                }.getOrDefault(false)

            onDone(success)
        }
    }

    fun deleteExpense(
        item: DealerExpenseEntity,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            item.id <= 0 ||
            item.workspace != workspace.value
        ) {
            onDone(false)
            return
        }

        viewModelScope.launch {
            val success =
                runCatching {
                    dao.deleteExpenseById(
                        item.id,
                        item.workspace
                    ) == 1
                }.getOrDefault(false)

            onDone(success)
        }
    }

    private suspend fun refreshSaleStatus(
        saleId: Long
    ) {
        val total =
            dao.getSaleTotal(saleId)

        val returned =
            dao.getSaleReturnTotal(saleId)

        val collected =
            dao.getSaleCollected(saleId)

        val net =
            (
                total -
                    returned
            ).coerceAtLeast(0.0)

        val due =
            (
                net -
                    collected
            ).coerceAtLeast(0.0)

        dao.updateSaleStatus(
            saleId,
            when {
                due <= 0.0001 ->
                    "PAID"

                collected > 0.0001 ->
                    "PARTIAL"

                else ->
                    "OPEN"
            }
        )
    }

    private suspend fun refreshPurchaseStatus(
        purchaseId: Long
    ) {
        val total =
            dao.getPurchaseTotal(purchaseId)

        val returned =
            dao.getPurchaseReturnTotal(
                purchaseId
            )

        val paid =
            dao.getPurchasePaid(purchaseId)

        val net =
            (
                total -
                    returned
            ).coerceAtLeast(0.0)

        val due =
            (
                net -
                    paid
            ).coerceAtLeast(0.0)

        dao.updatePurchaseStatus(
            purchaseId,
            when {
                due <= 0.0001 ->
                    "PAID"

                paid > 0.0001 ->
                    "PARTIAL"

                else ->
                    "OPEN"
            }
        )
    }

    private suspend fun allocateCollectionAmount(
        collectionId: Long,
        customerId: Long,
        amount: Double,
        workspaceValue: String
    ) {
        var remaining =
            amount.coerceAtLeast(0.0)

        val sales =
            dao.getSalesForCustomerOnce(
                customerId,
                workspaceValue
            )

        for (sale in sales) {
            if (remaining <= 0.0001) break

            val total =
                dao.getSaleTotal(sale.id)

            val returned =
                dao.getSaleReturnTotal(
                    sale.id
                )

            val collected =
                dao.getSaleCollected(
                    sale.id
                )

            val due =
                (
                    total -
                        returned -
                        collected
                ).coerceAtLeast(0.0)

            val use =
                minOf(
                    due,
                    remaining
                )

            if (use > 0.0001) {
                dao.insertCollectionAllocation(
                    DealerCollectionAllocationEntity(
                        collectionId =
                            collectionId,
                        saleId =
                            sale.id,
                        amount =
                            use
                    )
                )

                remaining -= use

                refreshSaleStatus(
                    sale.id
                )
            }
        }

        // Remaining money intentionally stays
        // unallocated as customer advance.
    }

    private suspend fun allocateSupplierPaymentAmount(
        paymentId: Long,
        companyId: Long,
        amount: Double,
        workspaceValue: String
    ) {
        var remaining =
            amount.coerceAtLeast(0.0)

        val purchases =
            dao.getPurchasesForCompanyOnce(
                companyId,
                workspaceValue
            )

        for (purchase in purchases) {
            if (remaining <= 0.0001) break

            val total =
                dao.getPurchaseTotal(
                    purchase.id
                )

            val returned =
                dao.getPurchaseReturnTotal(
                    purchase.id
                )

            val paid =
                dao.getPurchasePaid(
                    purchase.id
                )

            val due =
                (
                    total -
                        returned -
                        paid
                ).coerceAtLeast(0.0)

            val use =
                minOf(
                    due,
                    remaining
                )

            if (use > 0.0001) {
                dao.insertSupplierPaymentAllocation(
                    DealerSupplierPaymentAllocationEntity(
                        paymentId =
                            paymentId,
                        purchaseId =
                            purchase.id,
                        amount =
                            use
                    )
                )

                remaining -= use

                refreshPurchaseStatus(
                    purchase.id
                )
            }
        }

        // Remaining money intentionally stays
        // unallocated as supplier advance.
    }

    private suspend fun rebuildCustomerCollectionAllocations(
        customerId: Long,
        workspaceValue: String
    ) {
        val sales =
            dao.getSalesForCustomerOnce(
                customerId,
                workspaceValue
            )

        val collections =
            dao.getCollectionsForCustomerOnce(
                customerId,
                workspaceValue
            )

        /*
         * Remove every allocation for this customer first.
         * Collections are returned oldest-first, so rebuilding
         * below reproduces chronological allocation.
         */
        for (collection in collections) {
            dao.deleteCollectionAllocationsByCollection(
                collection.id
            )
        }

        for (sale in sales) {
            refreshSaleStatus(
                sale.id
            )
        }

        for (collection in collections) {
            allocateCollectionAmount(
                collectionId =
                    collection.id,
                customerId =
                    customerId,
                amount =
                    collection.amount,
                workspaceValue =
                    workspaceValue
            )
        }

        for (sale in sales) {
            refreshSaleStatus(
                sale.id
            )
        }
    }

    private suspend fun rebuildCompanyPaymentAllocations(
        companyId: Long,
        workspaceValue: String
    ) {
        val purchases =
            dao.getPurchasesForCompanyOnce(
                companyId,
                workspaceValue
            )

        val payments =
            dao.getSupplierPaymentsForCompanyOnce(
                companyId,
                workspaceValue
            )

        /*
         * Remove every allocation for this company first.
         * Payments are returned oldest-first, so rebuilding
         * below reproduces chronological allocation.
         */
        for (payment in payments) {
            dao.deleteSupplierPaymentAllocationsByPayment(
                payment.id
            )
        }

        for (purchase in purchases) {
            refreshPurchaseStatus(
                purchase.id
            )
        }

        for (payment in payments) {
            allocateSupplierPaymentAmount(
                paymentId =
                    payment.id,
                companyId =
                    companyId,
                amount =
                    payment.amount,
                workspaceValue =
                    workspaceValue
            )
        }

        for (purchase in purchases) {
            refreshPurchaseStatus(
                purchase.id
            )
        }
    }

    fun deletePurchase(
        item: DealerPurchaseEntity,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            item.id <= 0 ||
            item.workspace != workspace.value
        ) {
            onDone(false)
            return
        }

        val currentWorkspace =
            workspace.value

        viewModelScope.launch {
            val success =
                runCatching {
                    database.withTransaction {
                        val purchase =
                            requireNotNull(
                                dao.getPurchaseOnce(
                                    item.id
                                )
                            )

                        require(
                            purchase.workspace ==
                                currentWorkspace
                        )

                        val lines =
                            dao.getPurchaseLinesOnce(
                                purchase.id
                            )

                        val batches =
                            mutableListOf<
                                StockBatchEntity
                            >()

                        for (line in lines) {
                            val batchId =
                                requireNotNull(
                                    line.inventoryBatchId
                                )

                            /*
                             * A purchase can only be physically
                             * deleted while its batches have not
                             * been used by sale/delivery/damage.
                             */
                            require(
                                dao
                                    .getDealerDownstreamBatchReferenceCount(
                                        batchId
                                    ) == 0
                            )

                            val returned =
                                dao.getReturnedPurchaseQuantity(
                                    line.id
                                )

                            val batch =
                                requireNotNull(
                                    inventoryDao
                                        .getBatchOnce(
                                            batchId
                                        )
                                )

                            require(
                                batch.quantity ==
                                    (
                                        line.quantity -
                                            returned
                                    )
                                        .coerceAtLeast(0)
                            )

                            batches += batch
                        }

                        val companyId =
                            purchase.companyId

                        require(
                            dao.deletePurchaseById(
                                purchase.id,
                                currentWorkspace
                            ) == 1
                        )

                        /*
                         * Purchase lines are now gone, so their
                         * dedicated stock batches can be removed.
                         */
                        for (batch in batches) {
                            inventoryDao.deleteBatch(
                                batch
                            )
                        }

                        if (companyId != null) {
                            rebuildCompanyPaymentAllocations(
                                companyId =
                                    companyId,
                                workspaceValue =
                                    currentWorkspace
                            )
                        }
                    }
                }.isSuccess

            onDone(success)
        }
    }

    fun updatePurchaseMeta(
        item: DealerPurchaseEntity,
        invoiceNo: String,
        purchasedAt: Long,
        note: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            item.id <= 0 ||
            item.workspace != workspace.value
        ) {
            onDone(false)
            return
        }

        viewModelScope.launch {
            val success =
                runCatching {
                    dao.updatePurchase(
                        item.copy(
                            invoiceNo =
                                invoiceNo.trim(),
                            purchasedAt =
                                purchasedAt,
                            note =
                                note.trim()
                        )
                    ) == 1
                }.getOrDefault(false)

            onDone(success)
        }
    }

    fun updateSaleMeta(
        item: DealerSaleEntity,
        invoiceNo: String,
        soldAt: Long,
        note: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            item.id <= 0 ||
            item.workspace != workspace.value
        ) {
            onDone(false)
            return
        }

        viewModelScope.launch {
            val success =
                runCatching {
                    dao.updateSale(
                        item.copy(
                            invoiceNo =
                                invoiceNo.trim(),
                            soldAt =
                                soldAt,
                            note =
                                note.trim()
                        )
                    ) == 1
                }.getOrDefault(false)

            onDone(success)
        }
    }

    fun deleteSale(
        item: DealerSaleEntity,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            item.id <= 0 ||
            item.workspace != workspace.value
        ) {
            onDone(false)
            return
        }

        val currentWorkspace =
            workspace.value

        viewModelScope.launch {
            val success =
                runCatching {
                    database.withTransaction {
                        val sale =
                            requireNotNull(
                                dao.getSaleOnce(
                                    item.id
                                )
                            )

                        require(
                            sale.workspace ==
                                currentWorkspace
                        )

                        /*
                         * Old pre-allocation return data cannot
                         * be reversed safely.
                         */
                        require(
                            dao
                                .getUnmappedSalesReturnAllocationCount(
                                    sale.id
                                ) == 0
                        )

                        val deliveryLink =
                            dao
                                .getDeliveryChallanSaleForSaleOnce(
                                    sale.id
                                )

                        val saleLines =
                            dao.getSaleLinesOnce(
                                sale.id
                            )

                        if (deliveryLink == null) {
                            /*
                             * Normal sale:
                             * restore the part of each original
                             * allocation that is not already back
                             * in stock through RESTOCK returns.
                             */
                            for (line in saleLines) {
                                val allocations =
                                    dao.getStockAllocationsOnce(
                                        line.id
                                    )

                                for (
                                    allocation in
                                    allocations
                                ) {
                                    val alreadyRestocked =
                                        dao
                                            .getRestockedReturnedAllocationQuantity(
                                                allocation.id
                                            )

                                    require(
                                        alreadyRestocked <=
                                            allocation.quantity
                                    )

                                    val restore =
                                        allocation.quantity -
                                            alreadyRestocked

                                    if (restore > 0) {
                                        val batchId =
                                            requireNotNull(
                                                allocation
                                                    .sourceStockBatchId
                                            )

                                        val batch =
                                            requireNotNull(
                                                inventoryDao
                                                    .getBatchOnce(
                                                        batchId
                                                    )
                                            )

                                        inventoryDao
                                            .updateBatchQuantity(
                                                batch.id,
                                                batch.quantity +
                                                    restore
                                            )
                                    }
                                }
                            }
                        } else {
                            /*
                             * Delivery sale:
                             * challan issue already removed stock.
                             * Deleting the sale makes those units
                             * unsold challan stock again, so no
                             * warehouse restore is done here.
                             *
                             * But any retailer RESTOCK return
                             * previously added to warehouse must
                             * be removed again.
                             */
                            val challan =
                                requireNotNull(
                                    dao.getDeliveryChallanOnce(
                                        deliveryLink
                                            .challanId
                                    )
                                )

                            require(
                                challan.status ==
                                    "OPEN"
                            )

                            require(
                                dao.getDeliverySettlementOnce(
                                    challan.id
                                ) == null
                            )

                            val returnAllocations =
                                dao
                                    .getRestockReturnAllocationsForSaleOnce(
                                        sale.id
                                    )

                            for (
                                allocation in
                                returnAllocations
                            ) {
                                val batchId =
                                    requireNotNull(
                                        allocation
                                            .sourceStockBatchId
                                    )

                                val batch =
                                    requireNotNull(
                                        inventoryDao
                                            .getBatchOnce(
                                                batchId
                                            )
                                    )

                                require(
                                    batch.quantity >=
                                        allocation.quantity
                                )

                                inventoryDao
                                    .updateBatchQuantity(
                                        batch.id,
                                        batch.quantity -
                                            allocation.quantity
                                    )
                            }
                        }

                        val customerId =
                            sale.customerId

                        require(
                            dao.deleteSaleById(
                                sale.id,
                                currentWorkspace
                            ) == 1
                        )

                        if (customerId != null) {
                            rebuildCustomerCollectionAllocations(
                                customerId =
                                    customerId,
                                workspaceValue =
                                    currentWorkspace
                            )
                        }
                    }
                }.isSuccess

            onDone(success)
        }
    }

    fun correctCollection(
        item: DealerCollectionEntity,
        customerId: Long,
        amount: Double,
        collectedAt: Long,
        note: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            item.id <= 0 ||
            customerId <= 0 ||
            amount <= 0 ||
            item.workspace != workspace.value
        ) {
            onDone(false)
            return
        }

        val currentWorkspace =
            workspace.value

        viewModelScope.launch {
            val success =
                runCatching {
                    database.withTransaction {
                        val customer =
                            requireNotNull(
                                dao.getCustomerOnce(
                                    customerId
                                )
                            )

                        require(
                            customer.workspace ==
                                currentWorkspace
                        )

                        /*
                         * Critical when customer/date/amount changes:
                         * remove the edited receipt's old allocation
                         * before rebuilding either ledger.
                         */
                        dao.deleteCollectionAllocationsByCollection(
                            item.id
                        )

                        require(
                            dao.updateCollection(
                                item.copy(
                                    customerId =
                                        customer.id,
                                    customerNameSnapshot =
                                        customer.name,
                                    amount =
                                        amount,
                                    collectedAt =
                                        collectedAt,
                                    note =
                                        note.trim()
                                )
                            ) == 1
                        )

                        val affectedCustomers =
                            listOfNotNull(
                                item.customerId,
                                customer.id
                            ).distinct()

                        for (
                            affectedCustomerId in
                            affectedCustomers
                        ) {
                            rebuildCustomerCollectionAllocations(
                                customerId =
                                    affectedCustomerId,
                                workspaceValue =
                                    currentWorkspace
                            )
                        }
                    }
                }.isSuccess

            onDone(success)
        }
    }

    fun deleteCollection(
        item: DealerCollectionEntity,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            item.id <= 0 ||
            item.workspace != workspace.value
        ) {
            onDone(false)
            return
        }

        val currentWorkspace =
            workspace.value

        viewModelScope.launch {
            val success =
                runCatching {
                    database.withTransaction {
                        val customerId =
                            item.customerId

                        dao.deleteCollectionAllocationsByCollection(
                            item.id
                        )

                        require(
                            dao.deleteCollectionById(
                                item.id,
                                currentWorkspace
                            ) == 1
                        )

                        if (customerId != null) {
                            rebuildCustomerCollectionAllocations(
                                customerId =
                                    customerId,
                                workspaceValue =
                                    currentWorkspace
                            )
                        }
                    }
                }.isSuccess

            onDone(success)
        }
    }

    fun correctSupplierPayment(
        item: DealerSupplierPaymentEntity,
        companyId: Long,
        amount: Double,
        paidAt: Long,
        note: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            item.id <= 0 ||
            companyId <= 0 ||
            amount <= 0 ||
            item.workspace != workspace.value
        ) {
            onDone(false)
            return
        }

        val currentWorkspace =
            workspace.value

        viewModelScope.launch {
            val success =
                runCatching {
                    database.withTransaction {
                        val company =
                            requireNotNull(
                                dao.getCompanyOnce(
                                    companyId
                                )
                            )

                        require(
                            company.workspace ==
                                currentWorkspace
                        )

                        /*
                         * Critical when company/date/amount changes:
                         * remove the edited payment's old allocation
                         * before rebuilding either ledger.
                         */
                        dao.deleteSupplierPaymentAllocationsByPayment(
                            item.id
                        )

                        require(
                            dao.updateSupplierPayment(
                                item.copy(
                                    companyId =
                                        company.id,
                                    companyNameSnapshot =
                                        company.name,
                                    amount =
                                        amount,
                                    paidAt =
                                        paidAt,
                                    note =
                                        note.trim()
                                )
                            ) == 1
                        )

                        val affectedCompanies =
                            listOfNotNull(
                                item.companyId,
                                company.id
                            ).distinct()

                        for (
                            affectedCompanyId in
                            affectedCompanies
                        ) {
                            rebuildCompanyPaymentAllocations(
                                companyId =
                                    affectedCompanyId,
                                workspaceValue =
                                    currentWorkspace
                            )
                        }
                    }
                }.isSuccess

            onDone(success)
        }
    }

    fun deleteSupplierPayment(
        item: DealerSupplierPaymentEntity,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            item.id <= 0 ||
            item.workspace != workspace.value
        ) {
            onDone(false)
            return
        }

        val currentWorkspace =
            workspace.value

        viewModelScope.launch {
            val success =
                runCatching {
                    database.withTransaction {
                        val companyId =
                            item.companyId

                        dao.deleteSupplierPaymentAllocationsByPayment(
                            item.id
                        )

                        require(
                            dao.deleteSupplierPaymentById(
                                item.id,
                                currentWorkspace
                            ) == 1
                        )

                        if (companyId != null) {
                            rebuildCompanyPaymentAllocations(
                                companyId =
                                    companyId,
                                workspaceValue =
                                    currentWorkspace
                            )
                        }
                    }
                }.isSuccess

            onDone(success)
        }
    }

    fun updateSalesReturnMeta(
        item: DealerSalesReturnEntity,
        returnedAt: Long,
        note: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            item.id <= 0 ||
            item.workspace != workspace.value
        ) {
            onDone(false)
            return
        }

        viewModelScope.launch {
            val success =
                runCatching {
                    dao.updateSalesReturn(
                        item.copy(
                            returnedAt =
                                returnedAt,
                            note =
                                note.trim()
                        )
                    ) == 1
                }.getOrDefault(false)

            onDone(success)
        }
    }

    fun deleteSalesReturn(
        item: DealerSalesReturnEntity,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            item.id <= 0 ||
            item.workspace != workspace.value
        ) {
            onDone(false)
            return
        }

        val currentWorkspace =
            workspace.value

        viewModelScope.launch {
            val success =
                runCatching {
                    database.withTransaction {
                        val returnItem =
                            requireNotNull(
                                dao.getSalesReturnOnce(
                                    item.id
                                )
                            )

                        require(
                            returnItem.workspace ==
                                currentWorkspace
                        )

                        val sale =
                            requireNotNull(
                                dao.getSaleOnce(
                                    returnItem.saleId
                                )
                            )

                        val allocations =
                            dao
                                .getSalesReturnAllocationsForReturnOnce(
                                    returnItem.id
                                )

                        if (
                            returnItem.returnType ==
                                "RESTOCK"
                        ) {
                            /*
                             * Undo the stock that this return
                             * previously placed back in warehouse.
                             */
                            for (
                                allocation in
                                allocations
                            ) {
                                val batchId =
                                    requireNotNull(
                                        allocation
                                            .sourceStockBatchId
                                    )

                                val batch =
                                    requireNotNull(
                                        inventoryDao
                                            .getBatchOnce(
                                                batchId
                                            )
                                    )

                                require(
                                    batch.quantity >=
                                        allocation.quantity
                                )

                                inventoryDao
                                    .updateBatchQuantity(
                                        batch.id,
                                        batch.quantity -
                                            allocation.quantity
                                    )
                            }
                        }

                        require(
                            dao.deleteSalesReturnById(
                                returnItem.id,
                                currentWorkspace
                            ) == 1
                        )

                        val customerId =
                            sale.customerId

                        if (customerId != null) {
                            rebuildCustomerCollectionAllocations(
                                customerId =
                                    customerId,
                                workspaceValue =
                                    currentWorkspace
                            )
                        } else {
                            refreshSaleStatus(
                                sale.id
                            )
                        }
                    }
                }.isSuccess

            onDone(success)
        }
    }

    fun updatePurchaseReturnMeta(
        item: DealerPurchaseReturnEntity,
        returnedAt: Long,
        note: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            item.id <= 0 ||
            item.workspace != workspace.value
        ) {
            onDone(false)
            return
        }

        viewModelScope.launch {
            val success =
                runCatching {
                    dao.updatePurchaseReturn(
                        item.copy(
                            returnedAt =
                                returnedAt,
                            note =
                                note.trim()
                        )
                    ) == 1
                }.getOrDefault(false)

            onDone(success)
        }
    }

    fun deletePurchaseReturn(
        item: DealerPurchaseReturnEntity,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            item.id <= 0 ||
            item.workspace != workspace.value
        ) {
            onDone(false)
            return
        }

        val currentWorkspace =
            workspace.value

        viewModelScope.launch {
            val success =
                runCatching {
                    database.withTransaction {
                        val returnItem =
                            requireNotNull(
                                dao.getPurchaseReturnOnce(
                                    item.id
                                )
                            )

                        require(
                            returnItem.workspace ==
                                currentWorkspace
                        )

                        val purchase =
                            requireNotNull(
                                dao.getPurchaseOnce(
                                    returnItem.purchaseId
                                )
                            )

                        val line =
                            requireNotNull(
                                dao.getPurchaseLineOnce(
                                    returnItem
                                        .purchaseLineId
                                )
                            )

                        val batchId =
                            requireNotNull(
                                line.inventoryBatchId
                            )

                        val batch =
                            requireNotNull(
                                inventoryDao
                                    .getBatchOnce(
                                        batchId
                                    )
                            )

                        /*
                         * Deleting purchase return means the
                         * returned quantity belongs to stock again.
                         */
                        inventoryDao
                            .updateBatchQuantity(
                                batch.id,
                                batch.quantity +
                                    returnItem.quantity
                            )

                        require(
                            dao.deletePurchaseReturnById(
                                returnItem.id,
                                currentWorkspace
                            ) == 1
                        )

                        val companyId =
                            purchase.companyId

                        if (companyId != null) {
                            rebuildCompanyPaymentAllocations(
                                companyId =
                                    companyId,
                                workspaceValue =
                                    currentWorkspace
                            )
                        } else {
                            refreshPurchaseStatus(
                                purchase.id
                            )
                        }
                    }
                }.isSuccess

            onDone(success)
        }
    }

    fun createPurchase(
        companyId: Long,
        invoiceNo: String,
        lines: List<DealerPurchaseLineInput>,
        paidNow: Double = 0.0,
        note: String = "",
        purchasedAt: Long =
            System.currentTimeMillis(),
        onDone: (Boolean) -> Unit = {}
    ) {
        val cleanLines =
            lines.filter {
                it.productId > 0 &&
                    it.quantity > 0 &&
                    it.unitCost > 0
            }

        if (
            companyId <= 0 ||
            cleanLines.isEmpty() ||
            paidNow < 0
        ) {
            onDone(false)
            return
        }

        val currentWorkspace = workspace.value
        val currentBusinessKey = businessKey.value

        viewModelScope.launch {
            val success =
                runCatching {
                    database.withTransaction {
                        val company =
                            requireNotNull(
                                dao.getCompanyOnce(companyId)
                            )

                        require(
                            company.workspace ==
                                currentWorkspace
                        )

                        val resolved =
                            cleanLines.map { input ->
                                val product =
                                    requireNotNull(
                                        inventoryDao.getProductOnce(
                                            input.productId
                                        )
                                    )

                                require(
                                    product.workspace == baseWorkspaceKey(currentWorkspace)
                                )

                                require(
                                    product.businessKey ==
                                        currentBusinessKey
                                )

                                input to product
                            }

                        val total =
                            resolved.sumOf {
                                it.first.quantity *
                                    it.first.unitCost
                            }

                        val purchaseId =
                            dao.insertPurchase(
                                DealerPurchaseEntity(
                                    companyId = company.id,
                                    companyNameSnapshot =
                                        company.name,
                                    invoiceNo =
                                        invoiceNo.trim(),
                                    purchasedAt =
                                        purchasedAt,
                                    note = note.trim(),
                                    workspace =
                                        currentWorkspace
                                )
                            )

                        resolved.forEach {
                            val input = it.first
                            val product = it.second

                            val batchId =
                                inventoryDao.insertBatch(
                                    StockBatchEntity(
                                        productId =
                                            product.id,
                                        batchNo =
                                            input.batchNo.trim(),
                                        quantity =
                                            input.quantity,
                                        purchasePrice =
                                            input.unitCost,
                                        purchaseDate =
                                            purchasedAt
                                    )
                                )

                            dao.insertPurchaseLine(
                                DealerPurchaseLineEntity(
                                    purchaseId =
                                        purchaseId,
                                    productId =
                                        product.id,
                                    inventoryBatchId =
                                        batchId,
                                    productNameSnapshot =
                                        product.name,
                                    batchNoSnapshot =
                                        input.batchNo.trim(),
                                    quantity =
                                        input.quantity,
                                    unitCost =
                                        input.unitCost,
                                    lineTotal =
                                        input.quantity *
                                            input.unitCost
                                )
                            )
                        }

                        var due = total

                        val oldPayments =
                            dao.getSupplierPaymentsForCompanyOnce(
                                company.id,
                                currentWorkspace
                            )

                        for (payment in oldPayments) {
                            if (due <= 0.0001) break

                            val allocated =
                                dao.getSupplierPaymentAllocated(
                                    payment.id
                                )

                            val free =
                                (
                                    payment.amount -
                                        allocated
                                ).coerceAtLeast(0.0)

                            val use =
                                minOf(
                                    free,
                                    due
                                )

                            if (use > 0.0001) {
                                dao.insertSupplierPaymentAllocation(
                                    DealerSupplierPaymentAllocationEntity(
                                        paymentId =
                                            payment.id,
                                        purchaseId =
                                            purchaseId,
                                        amount = use
                                    )
                                )

                                due -= use
                            }
                        }

                        if (paidNow > 0.0001) {
                            val paymentId =
                                dao.insertSupplierPayment(
                                    DealerSupplierPaymentEntity(
                                        companyId =
                                            company.id,
                                        companyNameSnapshot =
                                            company.name,
                                        amount =
                                            paidNow,
                                        paidAt =
                                            purchasedAt,
                                        workspace =
                                            currentWorkspace
                                    )
                                )

                            val use =
                                minOf(
                                    paidNow,
                                    due.coerceAtLeast(0.0)
                                )

                            if (use > 0.0001) {
                                dao.insertSupplierPaymentAllocation(
                                    DealerSupplierPaymentAllocationEntity(
                                        paymentId =
                                            paymentId,
                                        purchaseId =
                                            purchaseId,
                                        amount =
                                            use
                                    )
                                )

                                due -= use
                            }
                        }

                        val status =
                            when {
                                due <= 0.0001 ->
                                    "PAID"

                                due < total - 0.0001 ->
                                    "PARTIAL"

                                else ->
                                    "OPEN"
                            }

                        dao.updatePurchaseStatus(
                            purchaseId,
                            status
                        )
                    }
                }.isSuccess

            onDone(success)
        }
    }

    fun createSale(
        customerId: Long,
        invoiceNo: String,
        lines: List<DealerSaleLineInput>,
        collectedNow: Double = 0.0,
        note: String = "",
        soldAt: Long =
            System.currentTimeMillis(),
        onDone: (Boolean) -> Unit = {}
    ) {
        val cleanLines =
            lines.filter {
                it.productId > 0 &&
                    it.quantity > 0 &&
                    it.unitPrice > 0
            }

        if (
            customerId <= 0 ||
            cleanLines.isEmpty() ||
            collectedNow < 0
        ) {
            onDone(false)
            return
        }

        val currentWorkspace = workspace.value
        val currentBusinessKey = businessKey.value

        viewModelScope.launch {
            val success =
                runCatching {
                    database.withTransaction {
                        val customer =
                            requireNotNull(
                                dao.getCustomerOnce(customerId)
                            )

                        require(
                            customer.workspace ==
                                currentWorkspace
                        )

                        val resolved =
                            cleanLines.map { input ->
                                val product =
                                    requireNotNull(
                                        inventoryDao.getProductOnce(
                                            input.productId
                                        )
                                    )

                                require(
                                    product.workspace == baseWorkspaceKey(currentWorkspace)
                                )

                                require(
                                    product.businessKey ==
                                        currentBusinessKey
                                )

                                val batches =
                                    inventoryDao
                                        .getBatchesOnce(product.id)
                                        .filter {
                                            it.quantity > 0
                                        }

                                require(
                                    batches.sumOf {
                                        it.quantity
                                    } >=
                                        input.quantity
                                )

                                Triple(
                                    input,
                                    product,
                                    batches
                                )
                            }

                        val total =
                            resolved.sumOf {
                                it.first.quantity *
                                    it.first.unitPrice
                            }

                        val existingSales =
                            dao.getSalesForCustomerOnce(
                                customer.id,
                                currentWorkspace
                            )

                        var existingDue = 0.0

                        for (sale in existingSales) {
                            val saleTotal =
                                dao.getSaleTotal(sale.id)

                            val returns =
                                dao.getSaleReturnTotal(
                                    sale.id
                                )

                            val collected =
                                dao.getSaleCollected(
                                    sale.id
                                )

                            existingDue +=
                                (
                                    saleTotal -
                                        returns -
                                        collected
                                ).coerceAtLeast(0.0)
                        }

                        var availableAdvance = 0.0

                        val oldCollections =
                            dao.getCollectionsForCustomerOnce(
                                customer.id,
                                currentWorkspace
                            )

                        for (collection in oldCollections) {
                            val allocated =
                                dao.getCollectionAllocated(
                                    collection.id
                                )

                            availableAdvance +=
                                (
                                    collection.amount -
                                        allocated
                                ).coerceAtLeast(0.0)
                        }

                        val newDueAfterKnownMoney =
                            (
                                total -
                                    availableAdvance -
                                    collectedNow
                            ).coerceAtLeast(0.0)

                        if (customer.creditLimit > 0.0) {
                            require(
                                existingDue +
                                    newDueAfterKnownMoney <=
                                    customer.creditLimit +
                                        0.0001
                            )
                        }

                        val saleId =
                            dao.insertSale(
                                DealerSaleEntity(
                                    customerId =
                                        customer.id,
                                    customerNameSnapshot =
                                        customer.name,
                                    invoiceNo =
                                        invoiceNo.trim(),
                                    soldAt =
                                        soldAt,
                                    note = note.trim(),
                                    workspace =
                                        currentWorkspace
                                )
                            )

                        for (
                            resolvedLine in resolved
                        ) {
                            val input =
                                resolvedLine.first

                            val product =
                                resolvedLine.second

                            val batches =
                                resolvedLine.third

                            val saleLineId =
                                dao.insertSaleLine(
                                    DealerSaleLineEntity(
                                        saleId =
                                            saleId,
                                        productId =
                                            product.id,
                                        productNameSnapshot =
                                            product.name,
                                        quantity =
                                            input.quantity,
                                        unitPrice =
                                            input.unitPrice,
                                        lineTotal =
                                            input.quantity *
                                                input.unitPrice
                                    )
                                )

                            var remaining =
                                input.quantity

                            for (batch in batches) {
                                if (remaining <= 0) break

                                val used =
                                    minOf(
                                        batch.quantity,
                                        remaining
                                    )

                                dao.insertStockAllocation(
                                    DealerStockAllocationEntity(
                                        saleLineId =
                                            saleLineId,
                                        sourceStockBatchId =
                                            batch.id,
                                        batchNoSnapshot =
                                            batch.batchNo,
                                        quantity =
                                            used,
                                        unitCost =
                                            batch.purchasePrice,
                                        totalCost =
                                            used *
                                                batch.purchasePrice
                                    )
                                )

                                inventoryDao.updateBatchQuantity(
                                    batch.id,
                                    batch.quantity -
                                        used
                                )

                                remaining -= used
                            }

                            require(remaining == 0)
                        }

                        var due = total

                        for (collection in oldCollections) {
                            if (due <= 0.0001) break

                            val allocated =
                                dao.getCollectionAllocated(
                                    collection.id
                                )

                            val free =
                                (
                                    collection.amount -
                                        allocated
                                ).coerceAtLeast(0.0)

                            val use =
                                minOf(
                                    free,
                                    due
                                )

                            if (use > 0.0001) {
                                dao.insertCollectionAllocation(
                                    DealerCollectionAllocationEntity(
                                        collectionId =
                                            collection.id,
                                        saleId =
                                            saleId,
                                        amount =
                                            use
                                    )
                                )

                                due -= use
                            }
                        }

                        if (collectedNow > 0.0001) {
                            val collectionId =
                                dao.insertCollection(
                                    DealerCollectionEntity(
                                        customerId =
                                            customer.id,
                                        customerNameSnapshot =
                                            customer.name,
                                        amount =
                                            collectedNow,
                                        collectedAt =
                                            soldAt,
                                        workspace =
                                            currentWorkspace
                                    )
                                )

                            val use =
                                minOf(
                                    collectedNow,
                                    due.coerceAtLeast(0.0)
                                )

                            if (use > 0.0001) {
                                dao.insertCollectionAllocation(
                                    DealerCollectionAllocationEntity(
                                        collectionId =
                                            collectionId,
                                        saleId =
                                            saleId,
                                        amount =
                                            use
                                    )
                                )

                                due -= use
                            }
                        }

                        val status =
                            when {
                                due <= 0.0001 ->
                                    "PAID"

                                due < total - 0.0001 ->
                                    "PARTIAL"

                                else ->
                                    "OPEN"
                            }

                        dao.updateSaleStatus(
                            saleId,
                            status
                        )
                    }
                }.isSuccess

            onDone(success)
        }
    }


    private suspend fun rebalanceSaleAfterReturn(
        saleId: Long
    ) {
        val originalTotal =
            dao.getSaleTotal(saleId)

        val returnTotal =
            dao.getSaleReturnTotal(saleId)

        val netTotal =
            (
                originalTotal -
                    returnTotal
            ).coerceAtLeast(0.0)

        var allocated =
            dao.getSaleCollected(saleId)

        var excess =
            (
                allocated -
                    netTotal
            ).coerceAtLeast(0.0)

        if (excess > 0.0001) {
            val allocations =
                dao.getCollectionAllocationsForSaleOnce(
                    saleId
                )

            for (allocation in allocations) {
                if (excess <= 0.0001) break

                val release =
                    minOf(
                        allocation.amount,
                        excess
                    )

                val remaining =
                    allocation.amount -
                        release

                if (remaining <= 0.0001) {
                    dao.deleteCollectionAllocationById(
                        allocation.id
                    )
                } else {
                    dao.updateCollectionAllocationAmount(
                        allocation.id,
                        remaining
                    )
                }

                excess -= release
            }

            allocated =
                dao.getSaleCollected(saleId)
        }

        val due =
            (
                netTotal -
                    allocated
            ).coerceAtLeast(0.0)

        val status =
            when {
                due <= 0.0001 ->
                    "PAID"

                allocated > 0.0001 ->
                    "PARTIAL"

                else ->
                    "OPEN"
            }

        dao.updateSaleStatus(
            saleId,
            status
        )
    }

    private suspend fun rebalancePurchaseAfterReturn(
        purchaseId: Long
    ) {
        val originalTotal =
            dao.getPurchaseTotal(purchaseId)

        val returnTotal =
            dao.getPurchaseReturnTotal(
                purchaseId
            )

        val netTotal =
            (
                originalTotal -
                    returnTotal
            ).coerceAtLeast(0.0)

        var allocated =
            dao.getPurchasePaid(purchaseId)

        var excess =
            (
                allocated -
                    netTotal
            ).coerceAtLeast(0.0)

        if (excess > 0.0001) {
            val allocations =
                dao.getSupplierPaymentAllocationsForPurchaseOnce(
                    purchaseId
                )

            for (allocation in allocations) {
                if (excess <= 0.0001) break

                val release =
                    minOf(
                        allocation.amount,
                        excess
                    )

                val remaining =
                    allocation.amount -
                        release

                if (remaining <= 0.0001) {
                    dao.deleteSupplierPaymentAllocationById(
                        allocation.id
                    )
                } else {
                    dao.updateSupplierPaymentAllocationAmount(
                        allocation.id,
                        remaining
                    )
                }

                excess -= release
            }

            allocated =
                dao.getPurchasePaid(purchaseId)
        }

        val due =
            (
                netTotal -
                    allocated
            ).coerceAtLeast(0.0)

        val status =
            when {
                due <= 0.0001 ->
                    "PAID"

                allocated > 0.0001 ->
                    "PARTIAL"

                else ->
                    "OPEN"
            }

        dao.updatePurchaseStatus(
            purchaseId,
            status
        )
    }

    fun recordSalesReturn(
        saleLineId: Long,
        quantity: Int,
        returnType: String,
        note: String = "",
        returnedAt: Long =
            System.currentTimeMillis(),
        onDone: (Boolean) -> Unit = {}
    ) {
        val cleanType =
            returnType
                .trim()
                .uppercase()

        if (
            saleLineId <= 0 ||
            quantity <= 0 ||
            cleanType !in
                setOf(
                    "RESTOCK",
                    "DAMAGED"
                )
        ) {
            onDone(false)
            return
        }

        val currentWorkspace =
            workspace.value

        viewModelScope.launch {
            val success =
                runCatching {
                    database.withTransaction {
                        val line =
                            requireNotNull(
                                dao.getSaleLineOnce(
                                    saleLineId
                                )
                            )

                        val sale =
                            requireNotNull(
                                dao.getSaleOnce(
                                    line.saleId
                                )
                            )

                        require(
                            sale.workspace ==
                                currentWorkspace
                        )

                        /*
                         * Delivery stock was already removed from
                         * warehouse when the challan was issued.
                         * Until night settlement closes the challan,
                         * a normal retailer return would otherwise
                         * restore warehouse stock while delivery
                         * allocation still counted it as sold.
                         */
                        val deliveryLink =
                            dao
                                .getDeliveryChallanSaleForSaleOnce(
                                    sale.id
                                )

                        if (deliveryLink != null) {
                            val deliveryChallan =
                                requireNotNull(
                                    dao.getDeliveryChallanOnce(
                                        deliveryLink
                                            .challanId
                                    )
                                )

                            require(
                                deliveryChallan.status !=
                                    "OPEN"
                            )
                        }

                        val alreadyReturned =
                            dao.getReturnedSaleQuantity(
                                line.id
                            )

                        require(
                            alreadyReturned +
                                quantity <=
                                line.quantity
                        )

                        val originalAllocations =
                            dao.getStockAllocationsOnce(
                                line.id
                            )

                        data class ReturnSlice(
                            val allocation:
                                DealerStockAllocationEntity,
                            val quantity: Int
                        )

                        val slices =
                            mutableListOf<ReturnSlice>()

                        var remaining =
                            quantity

                        for (
                            allocation in
                            originalAllocations
                        ) {
                            if (remaining <= 0) break

                            val usedBefore =
                                dao.getReturnedAllocationQuantity(
                                    saleLineId =
                                        line.id,
                                    saleAllocationId =
                                        allocation.id
                                )

                            val available =
                                (
                                    allocation.quantity -
                                        usedBefore
                                ).coerceAtLeast(0)

                            val use =
                                minOf(
                                    available,
                                    remaining
                                )

                            if (use > 0) {
                                slices +=
                                    ReturnSlice(
                                        allocation =
                                            allocation,
                                        quantity =
                                            use
                                    )

                                remaining -= use
                            }
                        }

                        require(remaining == 0)

                        val totalCost =
                            slices.sumOf {
                                it.quantity *
                                    it.allocation.unitCost
                            }

                        val returnId =
                            dao.insertSalesReturn(
                                DealerSalesReturnEntity(
                                    saleId =
                                        sale.id,
                                    saleLineId =
                                        line.id,
                                    productId =
                                        line.productId,
                                    productNameSnapshot =
                                        line.productNameSnapshot,
                                    quantity =
                                        quantity,
                                    unitPrice =
                                        line.unitPrice,
                                    totalRefund =
                                        quantity *
                                            line.unitPrice,
                                    totalCost =
                                        totalCost,
                                    returnType =
                                        cleanType,
                                    returnedAt =
                                        returnedAt,
                                    note =
                                        note.trim(),
                                    workspace =
                                        currentWorkspace
                                )
                            )

                        for (slice in slices) {
                            val allocation =
                                slice.allocation

                            var restoredBatchId =
                                allocation
                                    .sourceStockBatchId

                            if (
                                cleanType ==
                                    "RESTOCK"
                            ) {
                                val productId =
                                    requireNotNull(
                                        line.productId
                                    )

                                val currentBatch =
                                    allocation
                                        .sourceStockBatchId
                                        ?.let {
                                            inventoryDao
                                                .getBatchOnce(it)
                                        }

                                restoredBatchId =
                                    if (
                                        currentBatch != null
                                    ) {
                                        inventoryDao
                                            .updateBatchQuantity(
                                                currentBatch.id,
                                                currentBatch.quantity +
                                                    slice.quantity
                                            )

                                        currentBatch.id
                                    } else {
                                        inventoryDao.insertBatch(
                                            StockBatchEntity(
                                                productId =
                                                    productId,
                                                batchNo =
                                                    allocation
                                                        .batchNoSnapshot,
                                                quantity =
                                                    slice.quantity,
                                                purchasePrice =
                                                    allocation
                                                        .unitCost,
                                                purchaseDate =
                                                    returnedAt
                                            )
                                        )
                                    }
                            }

                            dao.insertSalesReturnAllocation(
                                DealerSalesReturnAllocationEntity(
                                    returnId =
                                        returnId,
                                    saleAllocationId =
                                        allocation.id,
                                    sourceStockBatchId =
                                        restoredBatchId,
                                    batchNoSnapshot =
                                        allocation
                                            .batchNoSnapshot,
                                    quantity =
                                        slice.quantity,
                                    unitCost =
                                        allocation.unitCost,
                                    totalCost =
                                        slice.quantity *
                                            allocation.unitCost
                                )
                            )
                        }

                        rebalanceSaleAfterReturn(
                            sale.id
                        )
                    }
                }.isSuccess

            onDone(success)
        }
    }

    fun recordPurchaseReturn(
        purchaseLineId: Long,
        quantity: Int,
        note: String = "",
        returnedAt: Long =
            System.currentTimeMillis(),
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            purchaseLineId <= 0 ||
            quantity <= 0
        ) {
            onDone(false)
            return
        }

        val currentWorkspace =
            workspace.value

        viewModelScope.launch {
            val success =
                runCatching {
                    database.withTransaction {
                        val line =
                            requireNotNull(
                                dao.getPurchaseLineOnce(
                                    purchaseLineId
                                )
                            )

                        val purchase =
                            requireNotNull(
                                dao.getPurchaseOnce(
                                    line.purchaseId
                                )
                            )

                        require(
                            purchase.workspace ==
                                currentWorkspace
                        )

                        val returnedBefore =
                            dao.getReturnedPurchaseQuantity(
                                line.id
                            )

                        require(
                            returnedBefore +
                                quantity <=
                                line.quantity
                        )

                        val batchId =
                            requireNotNull(
                                line.inventoryBatchId
                            )

                        val batch =
                            requireNotNull(
                                inventoryDao
                                    .getBatchOnce(batchId)
                            )

                        require(
                            batch.quantity >= quantity
                        )

                        inventoryDao.updateBatchQuantity(
                            batch.id,
                            batch.quantity -
                                quantity
                        )

                        dao.insertPurchaseReturn(
                            DealerPurchaseReturnEntity(
                                purchaseId =
                                    purchase.id,
                                purchaseLineId =
                                    line.id,
                                productId =
                                    line.productId,
                                productNameSnapshot =
                                    line.productNameSnapshot,
                                quantity =
                                    quantity,
                                unitCost =
                                    line.unitCost,
                                totalValue =
                                    quantity *
                                        line.unitCost,
                                returnedAt =
                                    returnedAt,
                                note =
                                    note.trim(),
                                workspace =
                                    currentWorkspace
                            )
                        )

                        rebalancePurchaseAfterReturn(
                            purchase.id
                        )
                    }
                }.isSuccess

            onDone(success)
        }
    }


    fun addCollection(
        customerId: Long,
        amount: Double,
        collectedAt: Long =
            System.currentTimeMillis(),
        note: String = "",
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            customerId <= 0 ||
            amount <= 0
        ) {
            onDone(false)
            return
        }

        val currentWorkspace = workspace.value

        viewModelScope.launch {
            val success =
                runCatching {
                    database.withTransaction {
                        val customer =
                            requireNotNull(
                                dao.getCustomerOnce(customerId)
                            )

                        require(
                            customer.workspace ==
                                currentWorkspace
                        )

                        val collectionId =
                            dao.insertCollection(
                                DealerCollectionEntity(
                                    customerId =
                                        customer.id,
                                    customerNameSnapshot =
                                        customer.name,
                                    amount = amount,
                                    collectedAt =
                                        collectedAt,
                                    note = note.trim(),
                                    workspace =
                                        currentWorkspace
                                )
                            )

                        var remaining = amount

                        val customerSales =
                            dao.getSalesForCustomerOnce(
                                customer.id,
                                currentWorkspace
                            )

                        for (sale in customerSales) {
                            if (remaining <= 0.0001) break

                            val total =
                                dao.getSaleTotal(sale.id)

                            val returns =
                                dao.getSaleReturnTotal(
                                    sale.id
                                )

                            val collected =
                                dao.getSaleCollected(
                                    sale.id
                                )

                            val due =
                                (
                                    total -
                                        returns -
                                        collected
                                ).coerceAtLeast(0.0)

                            val use =
                                minOf(
                                    due,
                                    remaining
                                )

                            if (use > 0.0001) {
                                dao.insertCollectionAllocation(
                                    DealerCollectionAllocationEntity(
                                        collectionId =
                                            collectionId,
                                        saleId =
                                            sale.id,
                                        amount =
                                            use
                                    )
                                )

                                remaining -= use

                                val left =
                                    due - use

                                dao.updateSaleStatus(
                                    sale.id,
                                    if (left <= 0.0001) {
                                        "PAID"
                                    } else {
                                        "PARTIAL"
                                    }
                                )
                            }
                        }

                        // Any remaining amount stays unallocated
                        // and therefore becomes customer advance.
                    }
                }.isSuccess

            onDone(success)
        }
    }

    fun addSupplierPayment(
        companyId: Long,
        amount: Double,
        paidAt: Long =
            System.currentTimeMillis(),
        note: String = "",
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            companyId <= 0 ||
            amount <= 0
        ) {
            onDone(false)
            return
        }

        val currentWorkspace = workspace.value

        viewModelScope.launch {
            val success =
                runCatching {
                    database.withTransaction {
                        val company =
                            requireNotNull(
                                dao.getCompanyOnce(companyId)
                            )

                        require(
                            company.workspace ==
                                currentWorkspace
                        )

                        val paymentId =
                            dao.insertSupplierPayment(
                                DealerSupplierPaymentEntity(
                                    companyId =
                                        company.id,
                                    companyNameSnapshot =
                                        company.name,
                                    amount = amount,
                                    paidAt = paidAt,
                                    note = note.trim(),
                                    workspace =
                                        currentWorkspace
                                )
                            )

                        var remaining = amount

                        val purchases =
                            dao.getPurchasesForCompanyOnce(
                                company.id,
                                currentWorkspace
                            )

                        for (purchase in purchases) {
                            if (remaining <= 0.0001) break

                            val total =
                                dao.getPurchaseTotal(
                                    purchase.id
                                )

                            val returns =
                                dao.getPurchaseReturnTotal(
                                    purchase.id
                                )

                            val paid =
                                dao.getPurchasePaid(
                                    purchase.id
                                )

                            val due =
                                (
                                    total -
                                        returns -
                                        paid
                                ).coerceAtLeast(0.0)

                            val use =
                                minOf(
                                    due,
                                    remaining
                                )

                            if (use > 0.0001) {
                                dao.insertSupplierPaymentAllocation(
                                    DealerSupplierPaymentAllocationEntity(
                                        paymentId =
                                            paymentId,
                                        purchaseId =
                                            purchase.id,
                                        amount =
                                            use
                                    )
                                )

                                remaining -= use

                                val left =
                                    due - use

                                dao.updatePurchaseStatus(
                                    purchase.id,
                                    if (left <= 0.0001) {
                                        "PAID"
                                    } else {
                                        "PARTIAL"
                                    }
                                )
                            }
                        }

                        // Any remaining amount stays as
                        // supplier/company advance.
                    }
                }.isSuccess

            onDone(success)
        }
    }
}
