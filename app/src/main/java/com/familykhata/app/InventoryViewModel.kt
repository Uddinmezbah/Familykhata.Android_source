package com.familykhata.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.familykhata.app.data.AppDatabase
import com.familykhata.app.data.BakiEntryEntity
import com.familykhata.app.data.BakiPersonSummary
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
    private val bakiDatabase = AppDatabase.get(application)
    private val bakiDao = bakiDatabase.dao()
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

    val bakiPeople: StateFlow<List<BakiPersonSummary>> =
        workspace
            .flatMapLatest { workspaceValue ->
                bakiDao.observeBakiSummaries(
                    workspaceValue
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

            dao.getRetailSalesOnce(
                workspace = workspaceValue,
                businessKey = key
            ).forEach { sale ->
                runCatching {
                    reconcileRetailSaleBaki(
                        sale
                    )
                }
            }
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

    private suspend fun resolveExistingProductUnit(
        product: ProductEntity,
        requestedUnitName: String,
        requestedUnitFactor: Int
    ): Pair<String, Int> {
        require(requestedUnitFactor > 0)

        val baseName =
            product.unit
                .trim()
                .ifBlank { "pcs" }

        val baseKey =
            baseName.lowercase(
                Locale.ROOT
            )

        val requestedKey =
            requestedUnitName
                .trim()
                .lowercase(
                    Locale.ROOT
                )

        if (
            requestedKey.isBlank() ||
            requestedKey == baseKey
        ) {
            require(
                requestedUnitFactor == 1
            ) {
                "Invalid base unit factor"
            }

            return baseName to 1
        }

        val conversion =
            requireNotNull(
                dao.getProductUnitConversionsOnce(
                    product.id
                ).firstOrNull {
                    it.unitKey ==
                        requestedKey
                }
            ) {
                "Product unit not found"
            }

        require(
            conversion.baseQuantity ==
                requestedUnitFactor
        ) {
            "Product unit factor changed"
        }

        require(
            conversion.baseQuantity > 1
        )

        return conversion.unitName to
            conversion.baseQuantity
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
                emptyList(),
        initialUnitName: String = "",
        initialUnitFactor: Int = 1
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

        if (
            initialQuantity < 0 ||
            initialUnitFactor <= 0 ||
            !purchasePrice.isFinite() ||
            purchasePrice < 0.0
        ) {
            return
        }

        val initialStockValues =
            runCatching {
                val baseKey =
                    cleanUnit.lowercase(
                        Locale.ROOT
                    )

                val requestedKey =
                    initialUnitName
                        .trim()
                        .lowercase(
                            Locale.ROOT
                        )

                val factor =
                    if (
                        requestedKey.isBlank() ||
                        requestedKey == baseKey
                    ) {
                        require(
                            initialUnitFactor == 1
                        )
                        1
                    } else {
                        val conversion =
                            requireNotNull(
                                cleanedUnits
                                    .firstOrNull {
                                        it.unitName
                                            .lowercase(
                                                Locale.ROOT
                                            ) ==
                                            requestedKey
                                    }
                            )

                        require(
                            conversion.baseQuantity ==
                                initialUnitFactor
                        )

                        conversion.baseQuantity
                    }

                val baseQuantityLong =
                    initialQuantity.toLong() *
                        factor.toLong()

                require(
                    baseQuantityLong in
                        0L..
                        Int.MAX_VALUE.toLong()
                )

                val basePurchasePrice =
                    purchasePrice /
                        factor.toDouble()

                require(
                    basePurchasePrice.isFinite() &&
                        basePurchasePrice >= 0.0
                )

                baseQuantityLong.toInt() to
                    basePurchasePrice
            }.getOrNull()
                ?: return

        val initialBaseQuantity =
            initialStockValues.first

        val initialBasePurchasePrice =
            initialStockValues.second

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

                    if (initialBaseQuantity > 0) {
                        dao.insertBatch(
                            StockBatchEntity(
                                productId =
                                    productId,
                                batchNo =
                                    batchNo.trim(),
                                quantity =
                                    initialBaseQuantity,
                                purchasePrice =
                                    initialBasePurchasePrice,
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

        val existingBaseUnit =
            item.unit
                .trim()
                .ifBlank { "pcs" }

        if (
            !cleanUnit.equals(
                existingBaseUnit,
                ignoreCase = true
            )
        ) {
            return
        }

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
        batchNo: String,
        unitName: String = "",
        unitFactor: Int = 1
    ) {
        if (
            quantity <= 0 ||
            unitFactor <= 0 ||
            !purchasePrice.isFinite() ||
            purchasePrice < 0.0
        ) {
            return
        }

        val currentWorkspace =
            workspace.value

        val currentBusinessKey =
            businessKey.value

        viewModelScope.launch {
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
                    ) {
                        "Product context mismatch"
                    }

                    val resolvedUnit =
                        resolveExistingProductUnit(
                            product = product,
                            requestedUnitName =
                                unitName,
                            requestedUnitFactor =
                                unitFactor
                        )

                    val factor =
                        resolvedUnit.second

                    val baseQuantityLong =
                        quantity.toLong() *
                            factor.toLong()

                    require(
                        baseQuantityLong in
                            1L..
                            Int.MAX_VALUE.toLong()
                    )

                    val basePurchasePrice =
                        purchasePrice /
                            factor.toDouble()

                    require(
                        basePurchasePrice.isFinite() &&
                            basePurchasePrice >= 0.0
                    )

                    dao.insertBatch(
                        StockBatchEntity(
                            productId = productId,
                            batchNo = batchNo.trim(),
                            quantity =
                                baseQuantityLong.toInt(),
                            purchasePrice =
                                basePurchasePrice,
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

    private fun retailSaleDueSourceKey(
        saleId: Long
    ): String =
        "RETAIL_SALE_DUE:$saleId"

    private fun retailSalePaymentSourceKey(
        saleId: Long
    ): String =
        "RETAIL_SALE_PAYMENT:$saleId"

    private suspend fun reconcileRetailSaleBaki(
        sale: RetailSaleEntity
    ) {
        val dueKey =
            retailSaleDueSourceKey(
                sale.id
            )

        val paymentKey =
            retailSalePaymentSourceKey(
                sale.id
            )

        if (
            sale.status ==
            "CANCELLED"
        ) {
            bakiDao.deleteBakiEntryBySourceKey(
                dueKey
            )
            bakiDao.deleteBakiEntryBySourceKey(
                paymentKey
            )
            return
        }

        val personId =
            sale.bakiPersonId

        if (personId == null) {
            bakiDao.deleteBakiEntryBySourceKey(
                dueKey
            )
            bakiDao.deleteBakiEntryBySourceKey(
                paymentKey
            )
            return
        }

        val person =
            requireNotNull(
                bakiDao.getStatementPerson(
                    personId = personId,
                    workspace = sale.workspace
                )
            ) {
                "Baki person not found"
            }

        val currentDue =
            (
                sale.total -
                    sale.paid
            ).coerceAtLeast(
                0.0
            )

        var dueEntry =
            bakiDao.getBakiEntryBySourceKey(
                dueKey
            )

        /*
         * The first successful reconciliation freezes the
         * original remaining due. Future collections are
         * represented by one cumulative RECEIVED_BACK entry.
         */
        if (
            dueEntry == null &&
            currentDue > 0.0001
        ) {
            bakiDao.insertBakiEntryIgnore(
                BakiEntryEntity(
                    personId = person.id,
                    action = "GAVE",
                    amount = currentDue,
                    balanceDelta = currentDue,
                    note =
                        "Retail sale ${sale.invoiceNo}",
                    sourceKey = dueKey,
                    createdAt = sale.soldAt
                )
            )

            dueEntry =
                bakiDao.getBakiEntryBySourceKey(
                    dueKey
                )
        }

        /*
         * A sale that was fully paid from the beginning does
         * not need to create anything in Baki.
         */
        if (dueEntry == null) {
            bakiDao.deleteBakiEntryBySourceKey(
                paymentKey
            )
            return
        }

        require(
            dueEntry.personId ==
                person.id
        ) {
            "Retail sale Baki person mismatch"
        }

        val originalDue =
            dueEntry.amount

        require(
            originalDue > 0.0001
        ) {
            "Invalid retail sale due entry"
        }

        require(
            currentDue <=
                originalDue + 0.0001
        ) {
            "Retail sale due increased unexpectedly"
        }

        bakiDao.updateBakiEntry(
            entryId = dueEntry.id,
            action = "GAVE",
            amount = originalDue,
            balanceDelta = originalDue,
            note =
                "Retail sale ${sale.invoiceNo}",
            dueAt = dueEntry.dueAt
        )

        val collectedAfterSale =
            (
                originalDue -
                    currentDue
            ).coerceIn(
                0.0,
                originalDue
            )

        if (
            collectedAfterSale <=
            0.0001
        ) {
            bakiDao.deleteBakiEntryBySourceKey(
                paymentKey
            )
            return
        }

        var paymentEntry =
            bakiDao.getBakiEntryBySourceKey(
                paymentKey
            )

        if (paymentEntry == null) {
            bakiDao.insertBakiEntryIgnore(
                BakiEntryEntity(
                    personId = person.id,
                    action = "RECEIVED_BACK",
                    amount =
                        collectedAfterSale,
                    balanceDelta =
                        -collectedAfterSale,
                    note =
                        "Retail sale payment ${sale.invoiceNo}",
                    sourceKey =
                        paymentKey
                )
            )

            paymentEntry =
                bakiDao.getBakiEntryBySourceKey(
                    paymentKey
                )
        }

        paymentEntry?.let { entry ->
            require(
                entry.personId ==
                    person.id
            ) {
                "Retail payment Baki person mismatch"
            }

            bakiDao.updateBakiEntry(
                entryId = entry.id,
                action = "RECEIVED_BACK",
                amount =
                    collectedAfterSale,
                balanceDelta =
                    -collectedAfterSale,
                note =
                    "Retail sale payment ${sale.invoiceNo}",
                dueAt = null
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
                    val resolvedBakiPersonId =
                        bakiPersonId
                            ?.takeIf {
                                it > 0L
                            }

                    if (
                        resolvedBakiPersonId !=
                        null
                    ) {
                        requireNotNull(
                            bakiDao.getStatementPerson(
                                personId =
                                    resolvedBakiPersonId,
                                workspace =
                                    currentWorkspace
                            )
                        ) {
                            "Baki person not found"
                        }
                    }

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

                        val remainingDue =
                            (
                                total -
                                    finalPaid
                            ).coerceAtLeast(
                                0.0
                            )

                        if (
                            remainingDue >
                            0.0001
                        ) {
                            requireNotNull(
                                resolvedBakiPersonId
                            ) {
                                "Baki person required for due sale"
                            }
                        }

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
                                        resolvedBakiPersonId,
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

            saleId?.let { id ->
                dao.getRetailSaleOnce(
                    id
                )?.let { sale ->
                    runCatching {
                        reconcileRetailSaleBaki(
                            sale
                        )
                    }
                }
            }

            onDone(saleId)
        }
    }

    fun recordRetailSalePayment(
        saleId: Long,
        amount: Double,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            saleId <= 0L ||
            !amount.isFinite() ||
            amount <= 0.0
        ) {
            onDone(false)
            return
        }

        val currentWorkspace =
            workspace.value

        val currentBusinessKey =
            businessKey.value

        viewModelScope.launch {
            val updatedSale =
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
                                currentWorkspace &&
                                sale.businessKey ==
                                    currentBusinessKey
                        ) {
                            "Sale context mismatch"
                        }

                        require(
                            sale.status !=
                                "CANCELLED"
                        ) {
                            "Cancelled sale"
                        }

                        val remainingDue =
                            (
                                sale.total -
                                    sale.paid
                            ).coerceAtLeast(
                                0.0
                            )

                        require(
                            remainingDue >
                                0.0001
                        ) {
                            "Sale is already paid"
                        }

                        require(
                            amount <=
                                remainingDue +
                                    0.0001
                        ) {
                            "Payment exceeds due"
                        }

                        val newPaid =
                            (
                                sale.paid +
                                    amount
                            ).coerceAtMost(
                                sale.total
                            )

                        val newStatus =
                            if (
                                newPaid >=
                                sale.total -
                                    0.0001
                            ) {
                                "PAID"
                            } else {
                                "PARTIAL"
                            }

                        dao.updateRetailSalePayment(
                            saleId = sale.id,
                            paid = newPaid,
                            status = newStatus
                        )

                        requireNotNull(
                            dao.getRetailSaleOnce(
                                sale.id
                            )
                        )
                    }
                }.getOrNull()

            /*
             * Inventory payment is authoritative here.
             * Baki sync is idempotent and will retry when
             * this business context is opened again.
             */
            updatedSale?.let { sale ->
                runCatching {
                    reconcileRetailSaleBaki(
                        sale
                    )
                }
            }

            onDone(
                updatedSale != null
            )
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

            if (result) {
                dao.getRetailSaleOnce(
                    saleId
                )?.let { sale ->
                    runCatching {
                        reconcileRetailSaleBaki(
                            sale
                        )
                    }
                }
            }

            onDone(result)
        }
    }

    fun reduceStock(
        productId: Long,
        quantity: Int,
        unitName: String = "",
        unitFactor: Int = 1,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            quantity <= 0 ||
            unitFactor <= 0
        ) {
            onDone(false)
            return
        }

        val currentWorkspace =
            workspace.value

        val currentBusinessKey =
            businessKey.value

        viewModelScope.launch {
            val result =
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
                        ) {
                            "Product context mismatch"
                        }

                        val resolvedUnit =
                            resolveExistingProductUnit(
                                product = product,
                                requestedUnitName =
                                    unitName,
                                requestedUnitFactor =
                                    unitFactor
                            )

                        val baseQuantityLong =
                            quantity.toLong() *
                                resolvedUnit.second.toLong()

                        require(
                            baseQuantityLong in
                                1L..
                                Int.MAX_VALUE.toLong()
                        )

                        val baseQuantity =
                            baseQuantityLong.toInt()

                        val batches =
                            dao.getBatchesOnce(
                                productId
                            ).filter {
                                it.quantity > 0
                            }

                        val available =
                            batches.sumOf {
                                it.quantity.toLong()
                            }

                        require(
                            available >=
                                baseQuantityLong
                        ) {
                            "Not enough stock"
                        }

                        var remaining =
                            baseQuantity

                        for (batch in batches) {
                            if (remaining <= 0) break

                            val used =
                                minOf(
                                    batch.quantity,
                                    remaining
                                )

                            dao.updateBatchQuantity(
                                batch.id,
                                batch.quantity - used
                            )

                            remaining -= used
                        }

                        require(
                            remaining == 0
                        )
                    }
                }.isSuccess

            onDone(result)
        }
    }
}
