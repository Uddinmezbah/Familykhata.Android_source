package com.familykhata.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.familykhata.app.data.InventoryDatabase
import com.familykhata.app.data.ProductEntity
import com.familykhata.app.data.ProductUnitConversionEntity
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
import java.util.Locale

data class ProductUnitInput(
    val unitName: String,
    val baseQuantity: Int,
    val sortOrder: Int
)

data class RetailSaleLineInput(
    val productId: Long,
    val quantity: Int,
    val unitPrice: Double,
    val unitName: String = "",
    val unitFactor: Int = 1
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

    fun observeProductUnitConversions(
        productId: Long
    ): Flow<List<ProductUnitConversionEntity>> =
        dao.observeProductUnitConversions(
            productId
        )

    private fun normalizedProductUnitInputs(
        baseUnit: String,
        units: List<ProductUnitInput>
    ): List<ProductUnitInput> {
        val baseKey =
            baseUnit
                .trim()
                .ifBlank { "pcs" }
                .lowercase(Locale.ROOT)

        val cleaned =
            units
                .sortedBy { it.sortOrder }
                .mapIndexed { index, item ->
                    ProductUnitInput(
                        unitName =
                            item.unitName.trim(),
                        baseQuantity =
                            item.baseQuantity,
                        sortOrder =
                            index + 1
                    )
                }

        require(
            cleaned.none {
                it.unitName.isBlank() ||
                    it.baseQuantity <= 1
            }
        )

        val keys =
            cleaned.map {
                it.unitName.lowercase(
                    Locale.ROOT
                )
            }

        require(
            keys.distinct().size ==
                keys.size
        )

        require(
            keys.none {
                it == baseKey
            }
        )

        return cleaned
    }

    private suspend fun replaceProductUnitConversions(
        productId: Long,
        baseUnit: String,
        units: List<ProductUnitInput>
    ) {
        val cleaned =
            normalizedProductUnitInputs(
                baseUnit = baseUnit,
                units = units
            )

        dao.deleteProductUnitConversions(
            productId
        )

        cleaned.forEach { unit ->
            val inserted =
                dao.insertProductUnitConversion(
                    ProductUnitConversionEntity(
                        productId =
                            productId,
                        unitName =
                            unit.unitName,
                        unitKey =
                            unit.unitName
                                .lowercase(
                                    Locale.ROOT
                                ),
                        baseQuantity =
                            unit.baseQuantity,
                        sortOrder =
                            unit.sortOrder
                    )
                )

            require(inserted > 0L)
        }
    }

    fun saveProductUnitConversions(
        productId: Long,
        units: List<ProductUnitInput>,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (productId <= 0L) {
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
                                dao.getProductOnce(
                                    productId
                                )
                            )

                        require(
                            product.workspace ==
                                currentWorkspace &&
                                product.businessKey ==
                                    currentBusinessKey
                        )

                        replaceProductUnitConversions(
                            productId =
                                product.id,
                            baseUnit =
                                product.unit,
                            units =
                                units
                        )
                    }
                }.isSuccess

            onDone(success)
        }
    }

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
        batchNo: String,
        unitConversions:
            List<ProductUnitInput> =
                emptyList()
    ) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return

        val cleanUnit =
            unit.trim()
                .ifBlank { "pcs" }

        val cleanedUnits =
            runCatching {
                normalizedProductUnitInputs(
                    baseUnit = cleanUnit,
                    units = unitConversions
                )
            }.getOrNull()
                ?: return

        viewModelScope.launch {
            runCatching {
                database.withTransaction {
                    val productId =
                        dao.insertProduct(
                            ProductEntity(
                                name = cleanName,
                                category =
                                    category.trim(),
                                sku =
                                    sku.trim(),
                                unit =
                                    cleanUnit,
                                brand =
                                    brand.trim(),
                                genericName =
                                    genericName.trim(),
                                modelName =
                                    modelName.trim(),
                                serialOrImei =
                                    serialOrImei.trim(),
                                size =
                                    size.trim(),
                                color =
                                    color.trim(),
                                warrantyMonths =
                                    warrantyMonths
                                        .coerceAtLeast(0),
                                sellingPrice =
                                    sellingPrice
                                        .coerceAtLeast(0.0),
                                mrp =
                                    mrp.coerceAtLeast(0.0),
                                rackLocation =
                                    rackLocation.trim(),
                                lowStockLevel =
                                    lowStockLevel
                                        .coerceAtLeast(0),
                                note =
                                    note.trim(),
                                workspace =
                                    workspace,
                                businessKey =
                                    businessKey.value
                            )
                        )

                    require(productId > 0L)

                    replaceProductUnitConversions(
                        productId =
                            productId,
                        baseUnit =
                            cleanUnit,
                        units =
                            cleanedUnits
                    )

                    if (initialQuantity > 0) {
                        dao.insertBatch(
                            StockBatchEntity(
                                productId =
                                    productId,
                                batchNo =
                                    batchNo.trim(),
                                quantity =
                                    initialQuantity,
                                purchasePrice =
                                    purchasePrice
                                        .coerceAtLeast(0.0),
                                purchaseDate =
                                    purchaseDate,
                                expiryDate =
                                    expiryDate
                            )
                        )
                    }
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
        note: String,
        unitConversions:
            List<ProductUnitInput> =
                emptyList()
    ) {
        if (name.isBlank()) return

        val cleanUnit =
            unit.trim()
                .ifBlank { "pcs" }

        val cleanedUnits =
            runCatching {
                normalizedProductUnitInputs(
                    baseUnit = cleanUnit,
                    units = unitConversions
                )
            }.getOrNull()
                ?: return

        viewModelScope.launch {
            runCatching {
                database.withTransaction {
                    dao.updateProduct(
                        productId = item.id,
                        name = name.trim(),
                        category =
                            category.trim(),
                        sku =
                            sku.trim(),
                        unit =
                            cleanUnit,
                        brand =
                            brand.trim(),
                        genericName =
                            genericName.trim(),
                        modelName =
                            modelName.trim(),
                        serialOrImei =
                            serialOrImei.trim(),
                        size =
                            size.trim(),
                        color =
                            color.trim(),
                        warrantyMonths =
                            warrantyMonths
                                .coerceAtLeast(0),
                        sellingPrice =
                            sellingPrice
                                .coerceAtLeast(0.0),
                        mrp =
                            mrp.coerceAtLeast(0.0),
                        rackLocation =
                            rackLocation.trim(),
                        lowStockLevel =
                            lowStockLevel
                                .coerceAtLeast(0),
                        note =
                            note.trim()
                    )

                    replaceProductUnitConversions(
                        productId =
                            item.id,
                        baseUnit =
                            cleanUnit,
                        units =
                            cleanedUnits
                    )
                }
            }
        }
    }

    fun deleteProduct(
        productId: Long,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (productId <= 0L) {
            onDone(false)
            return
        }

        viewModelScope.launch {
            val deleted =
                runCatching {
                    database.withTransaction {
                        require(
                            dao.activeRetailSaleCountForProduct(
                                productId
                            ) == 0
                        ) {
                            "Product has active sales"
                        }

                        dao.deleteProductById(
                            productId
                        )
                    }
                }.isSuccess

            onDone(deleted)
        }
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
                    it.unitFactor > 0 &&
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
                            val unitName: String,
                            val unitFactor: Int,
                            val baseQuantity: Int,
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

                                val baseUnitKey =
                                    product.unit
                                        .trim()
                                        .ifBlank {
                                            "pcs"
                                        }
                                        .lowercase(
                                            Locale.ROOT
                                        )

                                val requestedUnitKey =
                                    input.unitName
                                        .trim()
                                        .lowercase(
                                            Locale.ROOT
                                        )

                                val resolvedUnitName:
                                    String

                                val resolvedUnitFactor:
                                    Int

                                if (
                                    requestedUnitKey
                                        .isBlank() ||
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
                                        product.unit
                                            .trim()
                                            .ifBlank {
                                                "pcs"
                                            }

                                    resolvedUnitFactor =
                                        1
                                } else {
                                    val conversion =
                                        requireNotNull(
                                            dao
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
                                        conversion
                                            .baseQuantity ==
                                            input.unitFactor
                                    ) {
                                        "Product unit factor changed"
                                    }

                                    require(
                                        conversion
                                            .baseQuantity >
                                            1
                                    )

                                    resolvedUnitName =
                                        conversion.unitName

                                    resolvedUnitFactor =
                                        conversion.baseQuantity
                                }

                                val baseQuantityLong =
                                    input.quantity
                                        .toLong() *
                                        resolvedUnitFactor
                                            .toLong()

                                require(
                                    baseQuantityLong in
                                        1L..
                                        Int.MAX_VALUE
                                            .toLong()
                                ) {
                                    "Sale quantity overflow"
                                }

                                val baseQuantity =
                                    baseQuantityLong
                                        .toInt()

                                val batches =
                                    dao.getBatchesOnce(
                                        product.id
                                    )
                                        .filter {
                                            it.quantity > 0
                                        }

                                require(
                                    batches.sumOf {
                                        it.quantity
                                            .toLong()
                                    } >=
                                        baseQuantity
                                            .toLong()
                                ) {
                                    "Not enough stock"
                                }

                                var remaining =
                                    baseQuantity

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

                                require(
                                    remaining == 0
                                )

                                val totalCost =
                                    allocations.sumOf {
                                        it.quantity *
                                            it.batch
                                                .purchasePrice
                                    }

                                /*
                                 * unitCost is the cost of one SOLD unit,
                                 * while allocations stay in Base Unit.
                                 */
                                val unitCost =
                                    totalCost /
                                        input.quantity
                                            .toDouble()

                                ResolvedLine(
                                    input = input,
                                    product = product,
                                    unitName =
                                        resolvedUnitName,
                                    unitFactor =
                                        resolvedUnitFactor,
                                    baseQuantity =
                                        baseQuantity,
                                    allocations =
                                        allocations,
                                    unitCost =
                                        unitCost,
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
                                            resolved.unitName,
                                        unitFactor =
                                            resolved.unitFactor,
                                        quantity =
                                            resolved.input.quantity,
                                        baseQuantity =
                                            resolved.baseQuantity,
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

    fun cancelRetailSale(
        saleId: Long,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (saleId <= 0L) {
            onDone(false)
            return
        }

        val currentWorkspace = workspace.value
        val currentBusinessKey = businessKey.value

        viewModelScope.launch {
            val result =
                runCatching {
                    database.withTransaction {
                        val sale =
                            requireNotNull(
                                dao.getRetailSaleOnce(
                                    saleId
                                )
                            ) {
                                "Sale not found"
                            }

                        require(
                            sale.workspace ==
                                currentWorkspace
                        ) {
                            "Sale belongs to another workspace"
                        }

                        require(
                            sale.businessKey ==
                                currentBusinessKey
                        ) {
                            "Sale belongs to another business"
                        }

                        if (
                            sale.status ==
                            "CANCELLED"
                        ) {
                            return@withTransaction true
                        }

                        val lines =
                            dao.getRetailSaleLinesOnce(
                                saleId
                            )

                        for (line in lines) {
                            val product =
                                dao.getProductOnce(
                                    line.productId
                                )

                            val allocations =
                                dao.getRetailSaleStockAllocationsOnce(
                                    line.id
                                )

                            /*
                             * Older/test data may already have had its
                             * product hard-deleted. In that case Room's
                             * product -> batch cascade removed the stock
                             * batches, so there is nowhere valid to restore
                             * stock. We still allow the historical sale to
                             * be cancelled.
                             */
                            if (product == null) {
                                continue
                            }

                            require(
                                product.workspace ==
                                    currentWorkspace &&
                                    product.businessKey ==
                                        currentBusinessKey
                            ) {
                                "Product context mismatch"
                            }

                            val expectedBaseQuantity =
                                if (
                                    line.baseQuantity >
                                    0
                                ) {
                                    line.baseQuantity
                                } else {
                                    line.quantity
                                }

                            require(
                                allocations.sumOf {
                                    it.quantity
                                        .toLong()
                                } ==
                                    expectedBaseQuantity
                                        .toLong()
                            ) {
                                "Sale stock allocation mismatch"
                            }

                            for (
                                allocation in
                                allocations
                            ) {
                                val batch =
                                    requireNotNull(
                                        dao.getBatchOnce(
                                            allocation.batchId
                                        )
                                    ) {
                                        "Stock batch missing"
                                    }

                                val restored =
                                    batch.quantity.toLong() +
                                        allocation.quantity.toLong()

                                require(
                                    restored <=
                                        Int.MAX_VALUE
                                ) {
                                    "Stock quantity overflow"
                                }

                                dao.updateBatchQuantity(
                                    batchId =
                                        batch.id,
                                    quantity =
                                        restored.toInt()
                                )
                            }
                        }

                        dao.updateRetailSaleStatus(
                            saleId = saleId,
                            status = "CANCELLED"
                        )

                        true
                    }
                }.getOrDefault(false)

            onDone(result)
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
