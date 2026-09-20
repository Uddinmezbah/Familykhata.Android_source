package com.familykhata.app.dealership

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.familykhata.app.businessDataKey
import com.familykhata.app.baseWorkspaceKey
import com.familykhata.app.businessIdFromWorkspaceKey
import com.familykhata.app.data.InventoryDatabase
import com.familykhata.app.data.ProductEntity
import com.familykhata.app.data.StockBatchEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.min

data class DealershipSaleLineInput(
    val productId: Long,
    val quantity: Int,
    val unitPrice: Double
)

@OptIn(ExperimentalCoroutinesApi::class)
class DealershipViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val database =
        InventoryDatabase.get(application)

    private val inventoryDao =
        database.dao()

    private val dao =
        database.dealershipDao()

    private val workspace =
        MutableStateFlow("SHOP")

    private val businessKey =
        MutableStateFlow("legacy")

    private val productContext =
        combine(
            workspace,
            businessKey
        ) { workspaceValue, businessValue ->
            workspaceValue to businessValue
        }


    val suppliers:
        StateFlow<List<DealershipSupplierEntity>> =
        workspace.flatMapLatest {
            dao.observeSuppliers(it)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    val territories:
        StateFlow<List<DealershipTerritoryEntity>> =
        workspace.flatMapLatest {
            dao.observeTerritories(it)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    val dealers:
        StateFlow<List<DealershipDealerEntity>> =
        workspace.flatMapLatest {
            dao.observeDealers(it)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    val products:
        StateFlow<List<ProductEntity>> =
        productContext.flatMapLatest { context ->
            inventoryDao.observeProductsForBusiness(
                workspace = baseWorkspaceKey(context.first),
                businessKey = context.second
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    val productPolicies:
        StateFlow<List<DealershipProductPolicyEntity>> =
        workspace.flatMapLatest {
            dao.observeProductPolicies(it)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    val stockReceipts:
        StateFlow<List<DealershipStockReceiptEntity>> =
        workspace.flatMapLatest {
            dao.observeStockReceipts(it)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    val invoices:
        StateFlow<List<DealershipInvoiceSummary>> =
        workspace.flatMapLatest {
            dao.observeInvoiceSummaries(it)
        }.stateIn(
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

        if (workspace.value != workspaceValue) {
            workspace.value = workspaceValue
        }

        if (businessKey.value != key) {
            businessKey.value = key
        }

}

    fun setWorkspace(value: String) {
        if (workspace.value != value) {
            workspace.value = value
        }
    }

    fun addSupplier(
        name: String,
        phone: String,
        contactPerson: String,
        address: String,
        note: String
    ) {
        if (name.isBlank()) return

        val currentWorkspace = workspace.value

        viewModelScope.launch {
            dao.insertSupplier(
                DealershipSupplierEntity(
                    name = name.trim(),
                    phone = phone.trim(),
                    contactPerson =
                        contactPerson.trim(),
                    address = address.trim(),
                    note = note.trim(),
                    workspace = currentWorkspace
                )
            )
        }
    }

    fun addTerritory(
        name: String,
        code: String,
        note: String
    ) {
        if (name.isBlank()) return

        val currentWorkspace = workspace.value

        viewModelScope.launch {
            dao.insertTerritory(
                DealershipTerritoryEntity(
                    name = name.trim(),
                    code = code.trim(),
                    note = note.trim(),
                    workspace = currentWorkspace
                )
            )
        }
    }

    fun addDealer(
        territoryId: Long?,
        name: String,
        dealerCode: String,
        phone: String,
        address: String,
        creditLimit: Double,
        note: String
    ) {
        if (name.isBlank()) return

        val currentWorkspace = workspace.value

        viewModelScope.launch {
            dao.insertDealer(
                DealershipDealerEntity(
                    territoryId =
                        territoryId?.takeIf { it > 0 },
                    name = name.trim(),
                    dealerCode =
                        dealerCode.trim(),
                    phone = phone.trim(),
                    address = address.trim(),
                    creditLimit =
                        creditLimit.coerceAtLeast(0.0),
                    note = note.trim(),
                    workspace = currentWorkspace
                )
            )
        }
    }


    fun updateSupplier(
        supplier: DealershipSupplierEntity,
        name: String,
        phone: String,
        contactPerson: String,
        address: String,
        note: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            supplier.id <= 0 ||
            name.isBlank()
        ) {
            onDone(false)
            return
        }

        val currentWorkspace = workspace.value

        viewModelScope.launch {
            val success =
                runCatching {
                    require(
                        supplier.workspace ==
                            currentWorkspace
                    )

                    dao.updateSupplier(
                        supplier.copy(
                            name = name.trim(),
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

    fun updateTerritory(
        territory: DealershipTerritoryEntity,
        name: String,
        code: String,
        note: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            territory.id <= 0 ||
            name.isBlank()
        ) {
            onDone(false)
            return
        }

        val currentWorkspace = workspace.value

        viewModelScope.launch {
            val success =
                runCatching {
                    require(
                        territory.workspace ==
                            currentWorkspace
                    )

                    dao.updateTerritory(
                        territory.copy(
                            name = name.trim(),
                            code = code.trim(),
                            note = note.trim()
                        )
                    ) == 1
                }.getOrDefault(false)

            onDone(success)
        }
    }

    fun updateDealer(
        dealer: DealershipDealerEntity,
        territoryId: Long?,
        name: String,
        dealerCode: String,
        phone: String,
        address: String,
        creditLimit: Double,
        note: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            dealer.id <= 0 ||
            name.isBlank()
        ) {
            onDone(false)
            return
        }

        val currentWorkspace = workspace.value

        viewModelScope.launch {
            val success =
                runCatching {
                    require(
                        dealer.workspace ==
                            currentWorkspace
                    )

                    dao.updateDealer(
                        dealer.copy(
                            territoryId =
                                territoryId
                                    ?.takeIf { it > 0 },
                            name = name.trim(),
                            dealerCode =
                                dealerCode.trim(),
                            phone = phone.trim(),
                            address = address.trim(),
                            creditLimit =
                                creditLimit.coerceAtLeast(
                                    0.0
                                ),
                            note = note.trim()
                        )
                    ) == 1
                }.getOrDefault(false)

            onDone(success)
        }
    }

    fun setProductPolicy(
        productId: Long,
        dealerPrice: Double,
        marginPercent: Double,
        note: String
    ) {
        if (
            productId <= 0 ||
            dealerPrice < 0 ||
            marginPercent < 0
        ) return

        val currentWorkspace = workspace.value

        viewModelScope.launch {
            val product =
                inventoryDao.getProductOnce(
                    productId
                ) ?: return@launch

            if (
                product.workspace != baseWorkspaceKey(currentWorkspace)
            ) {
                return@launch
            }

            dao.upsertProductPolicy(
                DealershipProductPolicyEntity(
                    productId = productId,
                    dealerPrice =
                        dealerPrice.coerceAtLeast(0.0),
                    marginPercent =
                        marginPercent.coerceAtLeast(0.0),
                    note = note.trim(),
                    workspace = currentWorkspace
                )
            )
        }
    }

    fun receiveStock(
        supplierId: Long?,
        productId: Long,
        quantity: Int,
        unitCost: Double,
        batchNo: String,
        invoiceReference: String,
        note: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            productId <= 0 ||
            quantity <= 0 ||
            unitCost < 0
        ) {
            onDone(false)
            return
        }

        val currentWorkspace = workspace.value

        viewModelScope.launch {
            val success =
                runCatching {
                    database.withTransaction {

                        val product =
                            inventoryDao
                                .getProductOnce(
                                    productId
                                )
                                ?: error(
                                    "Product not found"
                                )

                        require(
                            product.workspace == baseWorkspaceKey(currentWorkspace)
                        )

                        val supplier =
                            supplierId
                                ?.takeIf { it > 0 }
                                ?.let {
                                    dao.getSupplierOnce(
                                        it
                                    )
                                }

                        if (
                            supplierId != null &&
                            supplierId > 0
                        ) {
                            require(
                                supplier != null &&
                                supplier.workspace ==
                                    currentWorkspace
                            )
                        }

                        val now =
                            System.currentTimeMillis()

                        val inventoryBatchId =
                            inventoryDao.insertBatch(
                                StockBatchEntity(
                                    productId =
                                        product.id,
                                    batchNo =
                                        batchNo.trim(),
                                    quantity =
                                        quantity,
                                    purchasePrice =
                                        unitCost,
                                    purchaseDate =
                                        now
                                )
                            )

                        require(
                            inventoryBatchId > 0
                        )

                        dao.insertStockReceipt(
                            DealershipStockReceiptEntity(
                                supplierId =
                                    supplier?.id,
                                productId =
                                    product.id,
                                inventoryBatchId =
                                    inventoryBatchId,
                                supplierNameSnapshot =
                                    supplier?.name
                                        ?: "",
                                productNameSnapshot =
                                    product.name,
                                invoiceReference =
                                    invoiceReference
                                        .trim(),
                                quantity =
                                    quantity,
                                unitCost =
                                    unitCost,
                                receivedAt =
                                    now,
                                note =
                                    note.trim(),
                                workspace =
                                    currentWorkspace
                            )
                        )
                    }
                }.isSuccess

            onDone(success)
        }
    }

    fun createInvoice(
        dealerId: Long,
        invoiceNo: String,
        lines: List<DealershipSaleLineInput>,
        initialPayment: Double,
        note: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        val validLines =
            lines.filter {
                it.productId > 0 &&
                it.quantity > 0 &&
                it.unitPrice > 0
            }

        if (
            dealerId <= 0 ||
            validLines.isEmpty() ||
            validLines.size != lines.size ||
            initialPayment < 0
        ) {
            onDone(false)
            return
        }

        val currentWorkspace = workspace.value

        viewModelScope.launch {
            val success =
                runCatching {
                    database.withTransaction {

                        val dealer =
                            dao.getDealerOnce(
                                dealerId
                            ) ?: error(
                                "Dealer not found"
                            )

                        require(
                            dealer.workspace ==
                                currentWorkspace
                        )

                        val invoiceTotal =
                            validLines.sumOf {
                                it.quantity *
                                    it.unitPrice
                            }

                        require(
                            initialPayment <=
                                invoiceTotal
                        )

                        val oldOutstanding =
                            dao.getDealerOutstanding(
                                dealerId =
                                    dealer.id,
                                workspace =
                                    currentWorkspace
                            ).coerceAtLeast(0.0)

                        val newCredit =
                            invoiceTotal -
                                initialPayment

                        if (
                            dealer.creditLimit > 0
                        ) {
                            require(
                                oldOutstanding +
                                    newCredit <=
                                    dealer.creditLimit
                            )
                        }

                        val now =
                            System.currentTimeMillis()

                        val finalInvoiceNo =
                            invoiceNo.trim()
                                .ifBlank {
                                    "D-$now"
                                }

                        val invoiceId =
                            dao.insertInvoice(
                                DealershipInvoiceEntity(
                                    dealerId =
                                        dealer.id,
                                    dealerNameSnapshot =
                                        dealer.name,
                                    invoiceNo =
                                        finalInvoiceNo,
                                    status =
                                        "OPEN",
                                    soldAt =
                                        now,
                                    note =
                                        note.trim(),
                                    workspace =
                                        currentWorkspace
                                )
                            )

                        require(invoiceId > 0)

                        validLines.forEach { input ->

                            val product =
                                inventoryDao
                                    .getProductOnce(
                                        input.productId
                                    )
                                    ?: error(
                                        "Product not found"
                                    )

                            require(
                                product.workspace == baseWorkspaceKey(currentWorkspace)
                            )

                            val batches =
                                inventoryDao
                                    .getBatchesOnce(
                                        product.id
                                    )
                                    .filter {
                                        it.quantity > 0
                                    }

                            val available =
                                batches.sumOf {
                                    it.quantity
                                }

                            require(
                                available >=
                                    input.quantity
                            )

                            val invoiceLineId =
                                dao.insertInvoiceLine(
                                    DealershipInvoiceLineEntity(
                                        invoiceId =
                                            invoiceId,
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

                            require(
                                invoiceLineId > 0
                            )

                            var remaining =
                                input.quantity

                            for (batch in batches) {
                                if (remaining <= 0) {
                                    break
                                }

                                val used =
                                    min(
                                        remaining,
                                        batch.quantity
                                    )

                                inventoryDao
                                    .updateBatchQuantity(
                                        batchId =
                                            batch.id,
                                        quantity =
                                            batch.quantity -
                                                used
                                    )

                                dao.insertStockAllocation(
                                    DealershipStockAllocationEntity(
                                        invoiceLineId =
                                            invoiceLineId,
                                        sourceStockBatchId =
                                            batch.id,
                                        sourceBatchNoSnapshot =
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

                                remaining -= used
                            }

                            require(remaining == 0)
                        }

                        if (initialPayment > 0) {
                            dao.insertPayment(
                                DealershipPaymentEntity(
                                    invoiceId =
                                        invoiceId,
                                    amount =
                                        initialPayment,
                                    paidAt =
                                        now,
                                    note =
                                        "Initial payment"
                                )
                            )
                        }

                        if (
                            invoiceTotal -
                                initialPayment <=
                            0.009
                        ) {
                            dao.updateInvoiceStatus(
                                invoiceId =
                                    invoiceId,
                                status =
                                    "PAID"
                            )
                        }
                    }
                }.isSuccess

            onDone(success)
        }
    }

    fun recordReturn(
        invoiceId: Long,
        invoiceLineId: Long,
        quantity: Int,
        returnType: String,
        note: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        val normalizedType =
            returnType
                .trim()
                .uppercase()

        if (
            invoiceId <= 0 ||
            invoiceLineId <= 0 ||
            quantity <= 0 ||
            normalizedType !in
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
                        val invoice =
                            dao.getInvoiceOnce(
                                invoiceId
                            ) ?: error(
                                "Invoice not found"
                            )

                        require(
                            invoice.workspace ==
                                currentWorkspace
                        )

                        require(
                            invoice.status !=
                                "CANCELLED"
                        )

                        val line =
                            dao.getInvoiceLineOnce(
                                invoiceLineId
                            ) ?: error(
                                "Invoice line not found"
                            )

                        require(
                            line.invoiceId ==
                                invoice.id
                        )

                        val alreadyReturned =
                            dao.getReturnedQuantity(
                                invoiceLineId
                            ).coerceAtLeast(0)

                        require(
                            alreadyReturned +
                                quantity <=
                                line.quantity
                        )

                        val allocations =
                            dao.getStockAllocationsForLineOnce(
                                invoiceLineId
                            )

                        require(
                            allocations.sumOf {
                                it.quantity
                            } >=
                                line.quantity
                        )

                        var skip =
                            alreadyReturned

                        var remaining =
                            quantity

                        var returnCost =
                            0.0

                        val restockByBatch =
                            linkedMapOf<Long, Int>()

                        for (
                            allocation in
                                allocations
                        ) {
                            if (remaining <= 0) {
                                break
                            }

                            if (
                                skip >=
                                    allocation.quantity
                            ) {
                                skip -=
                                    allocation.quantity
                                continue
                            }

                            val available =
                                allocation.quantity -
                                    skip

                            val used =
                                min(
                                    remaining,
                                    available
                                )

                            returnCost +=
                                used *
                                    allocation.unitCost

                            if (
                                normalizedType ==
                                    "RESTOCK"
                            ) {
                                val batchId =
                                    allocation
                                        .sourceStockBatchId
                                        ?: error(
                                            "Original stock batch is unavailable"
                                        )

                                restockByBatch[
                                    batchId
                                ] =
                                    (
                                        restockByBatch[
                                            batchId
                                        ] ?: 0
                                    ) + used
                            }

                            remaining -= used
                            skip = 0
                        }

                        require(
                            remaining == 0
                        )

                        val refundAmount =
                            quantity *
                                line.unitPrice

                        val originalTotal =
                            dao.getInvoiceTotal(
                                invoice.id
                            )

                        val previouslyReturnedTotal =
                            dao.getInvoiceReturnTotal(
                                invoice.id
                            )

                        val paidTotal =
                            dao.getInvoicePaid(
                                invoice.id
                            )

                        val proposedAdjustedTotal =
                            (
                                originalTotal -
                                    previouslyReturnedTotal -
                                    refundAmount
                            ).coerceAtLeast(
                                0.0
                            )

                        require(
                            proposedAdjustedTotal +
                                0.009 >=
                                paidTotal
                        ) {
                            "Return value exceeds unpaid invoice balance"
                        }

                        if (
                            normalizedType ==
                                "RESTOCK"
                        ) {
                            val productId =
                                line.productId
                                    ?: error(
                                        "Returned product is unavailable"
                                    )

                            val currentBatches =
                                inventoryDao
                                    .getBatchesOnce(
                                        productId
                                    )
                                    .associateBy {
                                        it.id
                                    }

                            restockByBatch
                                .forEach {
                                    (
                                        batchId,
                                        returnedQuantity
                                    ) ->

                                    val batch =
                                        currentBatches[
                                            batchId
                                        ] ?: error(
                                            "Original stock batch is unavailable"
                                        )

                                    inventoryDao
                                        .updateBatchQuantity(
                                            batchId =
                                                batch.id,
                                            quantity =
                                                batch.quantity +
                                                    returnedQuantity
                                        )
                                }
                        }

                        val returnId =
                            dao.insertReturn(
                                DealershipReturnEntity(
                                    invoiceId =
                                        invoice.id,
                                    invoiceLineId =
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
                                        refundAmount,
                                    totalCost =
                                        returnCost,
                                    returnType =
                                        normalizedType,
                                    note =
                                        note.trim(),
                                    workspace =
                                        currentWorkspace
                                )
                            )

                        require(
                            returnId > 0
                        )

                        val adjustedTotal =
                            proposedAdjustedTotal

                        dao.updateInvoiceStatus(
                            invoiceId =
                                invoice.id,
                            status =
                                if (
                                    adjustedTotal -
                                        paidTotal <=
                                        0.009
                                ) {
                                    "PAID"
                                } else {
                                    "OPEN"
                                }
                        )
                    }
                }.isSuccess

            onDone(success)
        }
    }


    fun addPayment(
        invoiceId: Long,
        amount: Double,
        note: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            invoiceId <= 0 ||
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

                        val invoice =
                            dao.getInvoiceOnce(
                                invoiceId
                            ) ?: error(
                                "Invoice not found"
                            )

                        require(
                            invoice.workspace ==
                                currentWorkspace
                        )

                        require(
                            invoice.status !=
                                "CANCELLED"
                        )

                        val originalTotal =
                            dao.getInvoiceTotal(
                                invoiceId
                            )

                        val returnedTotal =
                            dao.getInvoiceReturnTotal(
                                invoiceId
                            )

                        val paid =
                            dao.getInvoicePaid(
                                invoiceId
                            )

                        val adjustedTotal =
                            (
                                originalTotal -
                                    returnedTotal
                                ).coerceAtLeast(0.0)

                        val remaining =
                            (
                                adjustedTotal -
                                    paid
                                ).coerceAtLeast(0.0)

                        require(
                            remaining > 0.009
                        )

                        require(
                            amount <=
                                remaining + 0.009
                        )

                        dao.insertPayment(
                            DealershipPaymentEntity(
                                invoiceId =
                                    invoiceId,
                                amount =
                                    amount.coerceAtMost(
                                        remaining
                                    ),
                                note =
                                    note.trim()
                            )
                        )

                        if (
                            remaining -
                                amount <=
                            0.009
                        ) {
                            dao.updateInvoiceStatus(
                                invoiceId =
                                    invoiceId,
                                status =
                                    "PAID"
                            )
                        }
                    }
                }.isSuccess

            onDone(success)
        }
    }

    fun observeInvoiceLines(
        invoiceId: Long
    ): Flow<List<DealershipInvoiceLineEntity>> =
        dao.observeInvoiceLines(
            invoiceId
        )

    fun observePayments(
        invoiceId: Long
    ): Flow<List<DealershipPaymentEntity>> =
        dao.observePayments(
            invoiceId
        )


    fun observeReturns(
        invoiceId: Long
    ): Flow<List<DealershipReturnEntity>> =
        dao.observeReturns(
            invoiceId
        )
}
