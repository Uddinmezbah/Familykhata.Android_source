package com.familykhata.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.familykhata.app.data.InventoryDatabase
import com.familykhata.app.data.ProductEntity
import com.familykhata.app.data.ProductStockSummary
import com.familykhata.app.data.RetailSaleEntity
import com.familykhata.app.data.RetailSaleLineEntity
import com.familykhata.app.data.RetailSaleStockAllocationEntity
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

data class RetailSaleLineInput(
    val productId: Long,
    val quantity: Int,
    val unitPrice: Double
)

@OptIn(ExperimentalCoroutinesApi::class)
class InventoryViewModel(application: Application) : AndroidViewModel(application) {
    private val database = InventoryDatabase.get(application)
    private val dao = database.dao()
    private val workspace =
        MutableStateFlow("SHOP")

    private val businessKey =
        MutableStateFlow("legacy")

    private val inventoryContext =
        combine(
            workspace,
            businessKey
        ) { workspaceValue, businessValue ->
            workspaceValue to businessValue
        }

    val products: StateFlow<List<ProductStockSummary>> =
        inventoryContext
        .flatMapLatest { context ->
            dao.observeProductSummaries(
                workspace = context.first,
                businessKey = context.second
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val retailSales: StateFlow<List<RetailSaleEntity>> =
        inventoryContext
            .flatMapLatest { context ->
                dao.observeRetailSales(
                    workspace = context.first,
                    businessKey = context.second
                )
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    fun setContext(
        workspaceValue: String,
        shopType: String
    ) {
        val key =
            businessDataKey(shopType)

        if (
            workspace.value !=
            workspaceValue
        ) {
            workspace.value =
                workspaceValue
        }

        if (
            businessKey.value !=
            key
        ) {
            businessKey.value =
                key
        }

        viewModelScope.launch {
            dao.claimLegacyProducts(
                workspace = workspaceValue,
                businessKey = key
            )
        }
    }

    fun setWorkspace(value: String) {
        if (workspace.value != value) workspace.value = value
    }

    fun observeBatches(productId: Long): Flow<List<StockBatchEntity>> =
        dao.observeBatches(productId)

    fun observeRetailSaleLines(
        saleId: Long
    ): Flow<List<RetailSaleLineEntity>> =
        dao.observeRetailSaleLines(saleId)

    fun addProduct(
        name: String,
        category: String,
        sku: String,
        unit: String,
        brand: String,
        genericName: String,
        modelName: String,
        serialOrImei: String,
        size: String,
        color: String,
        warrantyMonths: Int,
        sellingPrice: Double,
        mrp: Double,
        rackLocation: String,
        lowStockLevel: Int,
        note: String,
        workspace: String,
        initialQuantity: Int,
        purchasePrice: Double,
        purchaseDate: Long,
        expiryDate: Long?,
        batchNo: String
    ) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return

        viewModelScope.launch {
            database.withTransaction {
                val productId = dao.insertProduct(
                    ProductEntity(
                        name = cleanName,
                        category = category.trim(),
                        sku = sku.trim(),
                        unit = unit.trim().ifBlank { "pcs" },
                        brand = brand.trim(),
                        genericName = genericName.trim(),
                        modelName = modelName.trim(),
                        serialOrImei = serialOrImei.trim(),
                        size = size.trim(),
                        color = color.trim(),
                        warrantyMonths = warrantyMonths.coerceAtLeast(0),
                        sellingPrice = sellingPrice.coerceAtLeast(0.0),
                        mrp = mrp.coerceAtLeast(0.0),
                        rackLocation = rackLocation.trim(),
                        lowStockLevel = lowStockLevel.coerceAtLeast(0),
                        note = note.trim(),
                        workspace = workspace,
                        businessKey =
                            businessKey.value
                    )
                )

                if (initialQuantity > 0) {
                    dao.insertBatch(
                        StockBatchEntity(
                            productId = productId,
                            batchNo = batchNo.trim(),
                            quantity = initialQuantity,
                            purchasePrice = purchasePrice.coerceAtLeast(0.0),
                            purchaseDate = purchaseDate,
                            expiryDate = expiryDate
                        )
                    )
                }
            }
        }
    }

    fun updateProduct(
        item: ProductStockSummary,
        name: String,
        category: String,
        sku: String,
        unit: String,
        brand: String,
        genericName: String,
        modelName: String,
        serialOrImei: String,
        size: String,
        color: String,
        warrantyMonths: Int,
        sellingPrice: Double,
        mrp: Double,
        rackLocation: String,
        lowStockLevel: Int,
        note: String
    ) {
        if (name.isBlank()) return

        viewModelScope.launch {
            dao.updateProduct(
                productId = item.id,
                name = name.trim(),
                category = category.trim(),
                sku = sku.trim(),
                unit = unit.trim().ifBlank { "pcs" },
                brand = brand.trim(),
                genericName = genericName.trim(),
                modelName = modelName.trim(),
                serialOrImei = serialOrImei.trim(),
                size = size.trim(),
                color = color.trim(),
                warrantyMonths = warrantyMonths.coerceAtLeast(0),
                sellingPrice = sellingPrice.coerceAtLeast(0.0),
                mrp = mrp.coerceAtLeast(0.0),
                rackLocation = rackLocation.trim(),
                lowStockLevel = lowStockLevel.coerceAtLeast(0),
                note = note.trim()
            )
        }
    }

    fun deleteProduct(productId: Long) {
        viewModelScope.launch { dao.deleteProductById(productId) }
    }

    fun addBatch(
        productId: Long,
        quantity: Int,
        purchasePrice: Double,
        purchaseDate: Long,
        expiryDate: Long?,
        batchNo: String
    ) {
        if (quantity <= 0) return

        viewModelScope.launch {
            dao.insertBatch(
                StockBatchEntity(
                    productId = productId,
                    batchNo = batchNo.trim(),
                    quantity = quantity,
                    purchasePrice = purchasePrice.coerceAtLeast(0.0),
                    purchaseDate = purchaseDate,
                    expiryDate = expiryDate
                )
            )
        }
    }

    fun updateBatch(
        batchId: Long,
        quantity: Int,
        purchasePrice: Double,
        purchaseDate: Long,
        expiryDate: Long?,
        batchNo: String
    ) {
        if (quantity < 0 || purchasePrice < 0) return

        viewModelScope.launch {
            dao.updateBatch(
                batchId = batchId,
                quantity = quantity,
                purchasePrice =
                    purchasePrice.coerceAtLeast(0.0),
                purchaseDate = purchaseDate,
                expiryDate = expiryDate,
                batchNo = batchNo.trim()
            )
        }
    }

    fun createRetailSale(
        invoiceNo: String = "",
        lines: List<RetailSaleLineInput>,
        discount: Double = 0.0,
        paid: Double = 0.0,
        paymentMethod: String = "CASH",
        customerName: String = "",
        customerPhone: String = "",
        bakiPersonId: Long? = null,
        note: String = "",
        soldAt: Long = System.currentTimeMillis(),
        onDone: (Long?) -> Unit = {}
    ) {
        val cleanLines =
            lines.filter {
                it.productId > 0L &&
                    it.quantity > 0 &&
                    it.unitPrice.isFinite() &&
                    it.unitPrice >= 0.0
            }

        if (
            cleanLines.isEmpty() ||
            cleanLines.size != lines.size ||
            !discount.isFinite() ||
            !paid.isFinite() ||
            discount < 0.0 ||
            paid < 0.0 ||
            soldAt <= 0L
        ) {
            onDone(null)
            return
        }

        // One product may appear only once per sale.
        // The UI can increase quantity instead of adding duplicate rows.
        if (
            cleanLines.map { it.productId }
                .distinct()
                .size != cleanLines.size
        ) {
            onDone(null)
            return
        }

        val currentWorkspace = workspace.value
        val currentBusinessKey = businessKey.value

        viewModelScope.launch {
            val saleId =
                runCatching {
                    database.withTransaction {
                        data class BatchUse(
                            val batch: StockBatchEntity,
                            val quantity: Int
                        )

                        data class ResolvedLine(
                            val input: RetailSaleLineInput,
                            val product: ProductEntity,
                            val allocations: List<BatchUse>,
                            val unitCost: Double,
                            val lineTotal: Double
                        )

                        val resolvedLines =
                            cleanLines.map { input ->
                                val product =
                                    requireNotNull(
                                        dao.getProductOnce(
                                            input.productId
                                        )
                                    ) {
                                        "Product not found"
                                    }

                                require(
                                    product.workspace ==
                                        currentWorkspace
                                ) {
                                    "Product belongs to another workspace"
                                }

                                require(
                                    product.businessKey ==
                                        currentBusinessKey
                                ) {
                                    "Product belongs to another business"
                                }

                                val batches =
                                    dao.getBatchesOnce(product.id)
                                        .filter {
                                            it.quantity > 0
                                        }

                                require(
                                    batches.sumOf {
                                        it.quantity
                                    } >= input.quantity
                                ) {
                                    "Not enough stock"
                                }

                                var remaining =
                                    input.quantity

                                val allocations =
                                    mutableListOf<BatchUse>()

                                for (batch in batches) {
                                    if (remaining <= 0) break

                                    val used =
                                        minOf(
                                            batch.quantity,
                                            remaining
                                        )

                                    allocations +=
                                        BatchUse(
                                            batch = batch,
                                            quantity = used
                                        )

                                    remaining -= used
                                }

                                require(remaining == 0)

                                val totalCost =
                                    allocations.sumOf {
                                        it.quantity *
                                            it.batch.purchasePrice
                                    }

                                val unitCost =
                                    totalCost /
                                        input.quantity.toDouble()

                                ResolvedLine(
                                    input = input,
                                    product = product,
                                    allocations = allocations,
                                    unitCost = unitCost,
                                    lineTotal =
                                        input.quantity *
                                            input.unitPrice
                                )
                            }

                        val subtotal =
                            resolvedLines.sumOf {
                                it.lineTotal
                            }

                        require(subtotal.isFinite()) {
                            "Sale total is invalid"
                        }

                        require(
                            discount <=
                                subtotal + 0.0001
                        ) {
                            "Discount exceeds subtotal"
                        }

                        val total =
                            (
                                subtotal -
                                    discount
                            ).coerceAtLeast(0.0)

                        require(
                            paid <=
                                total + 0.0001
                        ) {
                            "Paid amount exceeds total"
                        }

                        val finalPaid =
                            paid.coerceAtMost(total)

                        val status =
                            when {
                                total <= 0.0001 ->
                                    "PAID"

                                finalPaid >=
                                    total - 0.0001 ->
                                    "PAID"

                                finalPaid > 0.0001 ->
                                    "PARTIAL"

                                else ->
                                    "DUE"
                            }

                        val requestedInvoice =
                            invoiceNo.trim()

                        val finalInvoiceNo =
                            if (requestedInvoice.isNotBlank()) {
                                require(
                                    dao.retailInvoiceNumberCount(
                                        workspace =
                                            currentWorkspace,
                                        businessKey =
                                            currentBusinessKey,
                                        invoiceNo =
                                            requestedInvoice
                                    ) == 0
                                ) {
                                    "Invoice number already exists"
                                }

                                requestedInvoice
                            } else {
                                var candidate =
                                    "R-$soldAt"

                                var suffix = 1

                                while (
                                    dao.retailInvoiceNumberCount(
                                        workspace =
                                            currentWorkspace,
                                        businessKey =
                                            currentBusinessKey,
                                        invoiceNo =
                                            candidate
                                    ) > 0
                                ) {
                                    candidate =
                                        "R-$soldAt-$suffix"
                                    suffix++
                                }

                                candidate
                            }

                        val createdSaleId =
                            dao.insertRetailSale(
                                RetailSaleEntity(
                                    invoiceNo =
                                        finalInvoiceNo,
                                    bakiPersonId =
                                        bakiPersonId
                                            ?.takeIf {
                                                it > 0L
                                            },
                                    customerName =
                                        customerName.trim(),
                                    customerPhone =
                                        customerPhone.trim(),
                                    subtotal = subtotal,
                                    discount = discount,
                                    total = total,
                                    paid = finalPaid,
                                    paymentMethod =
                                        paymentMethod
                                            .trim()
                                            .uppercase()
                                            .ifBlank {
                                                "CASH"
                                            },
                                    status = status,
                                    note = note.trim(),
                                    workspace =
                                        currentWorkspace,
                                    businessKey =
                                        currentBusinessKey,
                                    soldAt = soldAt
                                )
                            )

                        require(createdSaleId > 0L)

                        for (resolved in resolvedLines) {
                            val lineId =
                                dao.insertRetailSaleLine(
                                    RetailSaleLineEntity(
                                        saleId =
                                            createdSaleId,
                                        productId =
                                            resolved.product.id,
                                        productNameSnapshot =
                                            resolved.product.name,
                                        skuSnapshot =
                                            resolved.product.sku,
                                        unitSnapshot =
                                            resolved.product.unit,
                                        quantity =
                                            resolved.input.quantity,
                                        unitPrice =
                                            resolved.input.unitPrice,
                                        unitCost =
                                            resolved.unitCost,
                                        lineTotal =
                                            resolved.lineTotal
                                    )
                                )

                            require(lineId > 0L)

                            for (
                                allocation in
                                resolved.allocations
                            ) {
                                dao.updateBatchQuantity(
                                    batchId =
                                        allocation.batch.id,
                                    quantity =
                                        allocation.batch.quantity -
                                            allocation.quantity
                                )

                                val allocationId =
                                    dao.insertRetailSaleStockAllocation(
                                        RetailSaleStockAllocationEntity(
                                            saleLineId =
                                                lineId,
                                            batchId =
                                                allocation.batch.id,
                                            quantity =
                                                allocation.quantity,
                                            unitCost =
                                                allocation.batch
                                                    .purchasePrice
                                        )
                                    )

                                require(allocationId > 0L)
                            }
                        }

                        createdSaleId
                    }
                }.getOrNull()

            onDone(saleId)
        }
    }

    fun reduceStock(productId: Long, quantity: Int, onDone: (Boolean) -> Unit = {}) {
        if (quantity <= 0) {
            onDone(false)
            return
        }
        viewModelScope.launch {
            val result = runCatching {
                database.withTransaction {
                    val batches = dao.getBatchesOnce(productId).filter { it.quantity > 0 }
                    val available = batches.sumOf { it.quantity }
                    require(available >= quantity) { "Not enough stock" }
                    var remaining = quantity
                    for (batch in batches) {
                        if (remaining <= 0) break
                        val used = minOf(batch.quantity, remaining)
                        dao.updateBatchQuantity(batch.id, batch.quantity - used)
                        remaining -= used
                    }
                }
            }.isSuccess
            onDone(result)
        }
    }
}
