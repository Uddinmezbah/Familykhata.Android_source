package com.familykhata.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.familykhata.app.data.AppDatabase
import com.familykhata.app.data.BakiEntryEntity
import com.familykhata.app.data.BakiPersonSummary
import com.familykhata.app.data.InventoryDatabase
import com.familykhata.app.data.FinancialAccountEntity
import com.familykhata.app.data.FinancialAccountEntryEntity
import com.familykhata.app.data.FinancialAccountSummary
import com.familykhata.app.data.ProductEntity
import com.familykhata.app.data.ProductUnitConversionEntity
import com.familykhata.app.data.ProductStockSummary
import com.familykhata.app.data.PurchaseBillEntity
import com.familykhata.app.data.PurchaseBillLineEntity
import com.familykhata.app.data.PurchaseBillSummary
import com.familykhata.app.data.PurchasePaymentEntity
import com.familykhata.app.data.PurchaseSupplierEntity
import com.familykhata.app.data.PurchaseSupplierSummary
import com.familykhata.app.data.RetailSaleEntity
import com.familykhata.app.data.RetailSaleLineEntity
import com.familykhata.app.data.RetailSalePaymentEntity
import com.familykhata.app.data.RetailSaleReturnEntity
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
import java.util.UUID

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

data class PurchaseLineInput(
    val productId: Long,
    val quantity: Int,
    val unitCost: Double,
    val unitName: String = "",
    val unitFactor: Int = 1,
    val batchNo: String = "",
    val expiryDate: Long? = null
)

private data class ResolvedPurchaseLine(
    val input: PurchaseLineInput,
    val product: ProductEntity,
    val unitName: String,
    val unitFactor: Int,
    val baseQuantity: Int,
    val basePurchasePrice: Double,
    val lineTotal: Double
)

@OptIn(ExperimentalCoroutinesApi::class)
class InventoryViewModel(application: Application) : AndroidViewModel(application) {
    private val database = InventoryDatabase.get(application)
    private val dao = database.dao()
    private val bakiDatabase = AppDatabase.get(application)
    private val bakiDao = bakiDatabase.dao()
    private val retailSaleReturnService =
        RetailSaleReturnService(
            inventoryDb = database,
            appDb = bakiDatabase
        )
    private val purchaseReturnService =
        PurchaseReturnService(
            inventoryDb = database,
            appDb = bakiDatabase
        )
    private val appPreferences = application.getSharedPreferences(
        "hisabi_khata_preferences",
        android.content.Context.MODE_PRIVATE
    )
    private val initialBusinessId =
        appPreferences.getString(
            "selected_business_id",
            ""
        ).orEmpty().trim()
    private val workspace =
        MutableStateFlow("SHOP")

    private val businessKey =
        MutableStateFlow(
            "__NO_BUSINESS__"
        )

    private val businessId =
        MutableStateFlow(initialBusinessId)

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

    val purchaseSuppliers:
        StateFlow<List<PurchaseSupplierSummary>> =
        inventoryContext
            .flatMapLatest { context ->
                dao.observePurchaseSupplierSummaries(
                    workspace = context.first,
                    businessKey = context.second
                )
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    val purchaseBills:
        StateFlow<List<PurchaseBillSummary>> =
        inventoryContext
            .flatMapLatest { context ->
                dao.observePurchaseBillSummaries(
                    workspace = context.first,
                    businessKey = context.second
                )
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    private val ledgerContext =
        combine(workspace, businessId) { workspaceValue, businessIdValue ->
            workspaceValue to businessIdValue
        }

    val bakiPeople: StateFlow<List<BakiPersonSummary>> =
        ledgerContext
            .flatMapLatest { context ->
                bakiDao.observeBakiSummaries(context.first, context.second)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val financialAccounts: StateFlow<List<FinancialAccountSummary>> =
        ledgerContext
            .flatMapLatest { context ->
                bakiDao.observeFinancialAccounts(context.first, context.second)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setContext(
        workspaceValue: String,
        shopType: String,
        businessIdValue: String
    ) {
        val legacyBusinessKey =
            businessDataKey(shopType)
        val cleanBusinessId =
            businessIdValue.trim()

        if (workspace.value != workspaceValue) {
            workspace.value = workspaceValue
        }

        if (
            workspaceValue == "SHOP" &&
            cleanBusinessId.isBlank()
        ) {
            businessId.value = ""
            businessKey.value = "__NO_BUSINESS__"
            return
        }

        val targetBusinessKey =
            if (workspaceValue == "SHOP") {
                inventoryBusinessKey(
                    cleanBusinessId,
                    shopType
                )
            } else {
                cleanBusinessId.ifBlank {
                    legacyBusinessKey
                }
            }

        if (businessId.value != cleanBusinessId) {
            businessId.value = cleanBusinessId
        }

        if (businessKey.value != targetBusinessKey) {
            businessKey.value = targetBusinessKey
        }

        viewModelScope.launch {
            if (
                workspaceValue == "SHOP" &&
                cleanBusinessId.isNotBlank()
            ) {
                dao.moveInventoryBusinessKey(
                    workspace = workspaceValue,
                    sourceBusinessKey = cleanBusinessId,
                    targetBusinessKey = targetBusinessKey
                )

                dao.moveRetailSalesBusinessKey(
                    workspace = workspaceValue,
                    sourceBusinessKey = cleanBusinessId,
                    targetBusinessKey = targetBusinessKey
                )

                val originalBusinessId =
                    appPreferences.getString(
                        "legacy_business_id",
                        ""
                    ).orEmpty()

                if (cleanBusinessId == originalBusinessId) {
                    dao.claimExistingBusinessProducts(
                        workspace = workspaceValue,
                        legacyBusinessKey = legacyBusinessKey,
                        targetBusinessId = targetBusinessKey
                    )

                    dao.claimExistingBusinessRetailSales(
                        workspace = workspaceValue,
                        legacyBusinessKey = legacyBusinessKey,
                        targetBusinessId = targetBusinessKey
                    )
                }
            }

            dao.getRetailSalesOnce(
                workspace = workspaceValue,
                businessKey = targetBusinessKey
            ).forEach { sale ->
                runCatching {
                    reconcileRetailSaleBaki(sale)
                }

                runCatching {
                    reconcileRetailSaleAccounts(sale)
                }
            }
        }
    }

    fun setWorkspace(value: String) {
        if (workspace.value != value) workspace.value = value
    }

    private fun ledgerBusinessIdForInventoryContext(
        workspaceValue: String,
        inventoryBusinessKeyValue: String
    ): String {
        if (workspaceValue != "SHOP") {
            return ""
        }

        val cleanKey =
            inventoryBusinessKeyValue.trim()

        if (
            cleanKey.isBlank() ||
            cleanKey == "__NO_BUSINESS__"
        ) {
            return ""
        }

        return cleanKey
            .substringBefore("::")
            .trim()
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

    fun observeRetailSaleReturns(
        saleId: Long
    ): Flow<List<RetailSaleReturnEntity>> =
        dao.observeRetailSaleReturns(saleId)

    fun recordRetailSaleReturn(
        saleId: Long,
        saleLineId: Long,
        quantity: Int,
        returnType: String = "RESTOCK",
        refundFinancialAccountId: Long? = null,
        note: String = "",
        returnedAt: Long = System.currentTimeMillis(),
        onDone: (Boolean) -> Unit = {}
    ) {
        val currentWorkspace = workspace.value
        val currentBusinessKey = businessKey.value
        val currentLedgerBusinessId =
            ledgerBusinessIdForInventoryContext(
                currentWorkspace,
                currentBusinessKey
            )

        viewModelScope.launch {
            val success =
                retailSaleReturnService.record(
                    saleId = saleId,
                    lineId = saleLineId,
                    quantity = quantity,
                    returnType = returnType,
                    refundAccountId = refundFinancialAccountId,
                    note = note,
                    returnedAt = returnedAt,
                    workspace = currentWorkspace,
                    businessKey = currentBusinessKey,
                    ledgerBusinessId = currentLedgerBusinessId
                )

            onDone(success)
        }
    }

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

        val cleanSku =
            sku.trim()

        val currentBusinessKey =
            businessKey.value

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
                    if (cleanSku.isNotBlank()) {
                        require(
                            dao.countProductSkuConflicts(
                                workspace = workspace,
                                businessKey =
                                    currentBusinessKey,
                                sku = cleanSku
                            ) == 0
                        ) {
                            "Duplicate Barcode / SKU"
                        }
                    }

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

        val cleanSku =
            sku.trim()

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
                    if (cleanSku.isNotBlank()) {
                        require(
                            dao.countProductSkuConflicts(
                                workspace =
                                    item.workspace,
                                businessKey =
                                    item.businessKey,
                                sku = cleanSku,
                                excludeProductId =
                                    item.id
                            ) == 0
                        ) {
                            "Duplicate Barcode / SKU"
                        }
                    }

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


    private fun retailSaleAccountSourceKey(
        eventKey: String
    ): String =
        "RETAIL_SALE_ACCOUNT:$eventKey"

    private fun retailPaymentMethodForAccount(
        account: FinancialAccountEntity
    ): String {
        val provider =
            account.provider
                .trim()
                .uppercase(
                    Locale.ROOT
                )

        return when {
            provider.contains("BKASH") ->
                "BKASH"

            provider.contains("NAGAD") ->
                "NAGAD"

            provider.contains("ROCKET") ->
                "ROCKET"

            account.type == "CASH" ->
                "CASH"

            account.type == "BANK" ->
                "BANK"

            account.type == "CARD" ->
                "CARD"

            account.type ==
                "MOBILE_WALLET" ->
                "WALLET"

            else ->
                "OTHER"
        }
    }

    private fun mergedRetailPaymentMethod(
        oldMethod: String,
        alreadyPaid: Double,
        incomingMethod: String
    ): String {
        if (
            alreadyPaid <= 0.0001 ||
            oldMethod.equals(
                "UNPAID",
                ignoreCase = true
            )
        ) {
            return incomingMethod
        }

        return if (
            oldMethod.equals(
                incomingMethod,
                ignoreCase = true
            )
        ) {
            incomingMethod
        } else {
            "MIXED"
        }
    }

    private suspend fun removeRetailSaleAccountCreditsSafely(
        sale: RetailSaleEntity,
        payments: List<RetailSalePaymentEntity>
    ) {
        val existingEntries =
            mutableListOf<FinancialAccountEntryEntity>()

        payments.forEach { payment ->
            val sourceKey =
                retailSaleAccountSourceKey(
                    payment.eventKey
                )

            val entry =
                bakiDao
                    .getFinancialAccountEntryBySourceKey(
                        sourceKey
                    )

            if (entry != null) {
                require(
                    entry.accountId ==
                        payment.financialAccountId &&
                        entry.entryType ==
                            "RETAIL_SALE_IN" &&
                        entry.workspace ==
                            sale.workspace &&
                        entry.businessId ==
                            ledgerBusinessIdForInventoryContext(
                                sale.workspace,
                                sale.businessKey
                            ) &&
                        kotlin.math.abs(
                            entry.balanceDelta -
                                payment.amount
                        ) < 0.0001
                ) {
                    "Retail account projection mismatch"
                }

                existingEntries += entry
            }
        }

        existingEntries
            .groupBy { it.accountId }
            .forEach {
                    (accountId, entries) ->

                val amountToRemove =
                    entries.sumOf {
                        it.balanceDelta
                    }

                val currentBalance =
                    requireNotNull(
                        bakiDao
                            .getFinancialAccountBalanceOnce(
                                accountId
                            )
                    ) {
                        "Financial account balance unavailable"
                    }

                require(
                    currentBalance -
                        amountToRemove >=
                        -0.0001
                ) {
                    "Insufficient financial account balance for sale cancellation"
                }
            }

        existingEntries.forEach { entry ->
            entry.sourceKey?.let { sourceKey ->
                bakiDao
                    .deleteFinancialAccountEntryBySourceKey(
                        sourceKey
                    )
            }
        }
    }

    private suspend fun reconcileRetailSaleAccounts(
        sale: RetailSaleEntity
    ) {
        val payments =
            dao.getRetailSalePaymentsOnce(
                sale.id
            )

        bakiDatabase.withTransaction {
            if (
                sale.status ==
                    "CANCELLED"
            ) {
                removeRetailSaleAccountCreditsSafely(
                    sale = sale,
                    payments = payments
                )
                return@withTransaction
            }

            payments.forEach {
                    payment ->

                require(
                    payment.saleId ==
                        sale.id &&
                        payment.amount
                            .isFinite() &&
                        payment.amount >
                            0.0001
                ) {
                    "Invalid retail sale payment"
                }

                val account =
                    requireNotNull(
                        bakiDao
                            .getFinancialAccountOnce(
                                payment
                                    .financialAccountId
                            )
                    ) {
                        "Retail payment account not found"
                    }

                require(
                    account.workspace ==
                        sale.workspace &&
                        (sale.workspace != "SHOP" || account.businessId ==
                                        ledgerBusinessIdForInventoryContext(
                                            sale.workspace,
                                            sale.businessKey
                                        ))
                ) {
                    "Retail payment account workspace mismatch"
                }

                val sourceKey =
                    retailSaleAccountSourceKey(
                        payment.eventKey
                    )

                val existing =
                    bakiDao
                        .getFinancialAccountEntryBySourceKey(
                            sourceKey
                        )

                val matches =
                    existing != null &&
                        existing.accountId ==
                            account.id &&
                        existing.entryType ==
                            "RETAIL_SALE_IN" &&
                        kotlin.math.abs(
                            existing.amount -
                                payment.amount
                        ) < 0.0001 &&
                        kotlin.math.abs(
                            existing.balanceDelta -
                                payment.amount
                        ) < 0.0001 &&
                        existing.relatedAccountId ==
                            null &&
                        existing.transferGroupId ==
                            null &&
                        existing.workspace ==
                            sale.workspace &&
                        existing.businessId ==
                            ledgerBusinessIdForInventoryContext(
                                sale.workspace,
                                sale.businessKey
                            )

                if (!matches) {
                    if (existing != null) {
                        bakiDao
                            .deleteFinancialAccountEntryBySourceKey(
                                sourceKey
                            )
                    }

                    val inserted =
                        bakiDao
                            .insertFinancialAccountEntry(
                                FinancialAccountEntryEntity(
                                    accountId =
                                        account.id,
                                    entryType =
                                        "RETAIL_SALE_IN",
                                    amount =
                                        payment.amount,
                                    balanceDelta =
                                        payment.amount,
                                    sourceKey =
                                        sourceKey,
                                    note =
                                        "Retail sale ${sale.invoiceNo} • ${payment.paymentMethod}",
                                    workspace =
                                        sale.workspace,
                                    businessId = account.businessId,
                                    createdAt =
                                        payment.paidAt
                                )
                            )

                    require(
                        inserted > 0L
                    )
                }
            }
        }
    }

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
                    workspace = sale.workspace,
                    businessId =
                        ledgerBusinessIdForInventoryContext(
                            sale.workspace,
                            sale.businessKey
                        )
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
        financialAccountId: Long? = null,
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
                                    currentWorkspace,
                                businessId =
                                    ledgerBusinessIdForInventoryContext(
                                        currentWorkspace,
                                        currentBusinessKey
                                    )
                            )
                        ) {
                            "Baki person not found"
                        }
                    }

                    val resolvedFinancialAccount =
                        financialAccountId
                            ?.takeIf {
                                it > 0L
                            }
                            ?.let {
                                    accountId ->

                                requireNotNull(
                                    bakiDao
                                        .getFinancialAccountOnce(
                                            accountId
                                        )
                                ).also {
                                        account ->

                                    require(
                                        account.workspace ==
                                            currentWorkspace &&
                                            (
                                                currentWorkspace != "SHOP" ||
                                                    account.businessId ==
                                                    ledgerBusinessIdForInventoryContext(
                                                        currentWorkspace,
                                                        currentBusinessKey
                                                    )
                                            ) &&
                                            account.isActive
                                    ) {
                                        "Invalid retail payment account"
                                    }
                                }
                            }

                    require(
                        paid <= 0.0001 ||
                            resolvedFinancialAccount != null
                    ) {
                        "Financial account required for paid retail sale"
                    }

                    val resolvedPaymentMethod =
                        resolvedFinancialAccount
                            ?.let {
                                retailPaymentMethodForAccount(
                                    it
                                )
                            }
                            ?: paymentMethod
                                .trim()
                                .uppercase(
                                    Locale.ROOT
                                )
                                .ifBlank {
                                    "CASH"
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
                                        if (
                                            finalPaid >
                                                0.0001
                                        ) {
                                            resolvedPaymentMethod
                                        } else {
                                            "UNPAID"
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

                        if (
                            finalPaid >
                                0.0001 &&
                            resolvedFinancialAccount !=
                                null
                        ) {
                            val paymentId =
                                dao.insertRetailSalePayment(
                                    RetailSalePaymentEntity(
                                        eventKey =
                                            UUID.randomUUID()
                                                .toString(),
                                        saleId =
                                            createdSaleId,
                                        financialAccountId =
                                            resolvedFinancialAccount
                                                .id,
                                        amount =
                                            finalPaid,
                                        paymentMethod =
                                            resolvedPaymentMethod,
                                        note =
                                            note.trim(),
                                        paidAt =
                                            soldAt
                                    )
                                )

                            require(
                                paymentId > 0L
                            )
                        }

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

                    runCatching {
                        reconcileRetailSaleAccounts(
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
        financialAccountId: Long? = null,
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
                    val resolvedFinancialAccount =
                        financialAccountId
                            ?.takeIf {
                                it > 0L
                            }
                            ?.let {
                                    accountId ->

                                requireNotNull(
                                    bakiDao
                                        .getFinancialAccountOnce(
                                            accountId
                                        )
                                ).also {
                                        account ->

                                    require(
                                        account.workspace ==
                                            currentWorkspace &&
                                            (
                                                currentWorkspace != "SHOP" ||
                                                    account.businessId ==
                                                    ledgerBusinessIdForInventoryContext(
                                                        currentWorkspace,
                                                        currentBusinessKey
                                                    )
                                            ) &&
                                            account.isActive
                                    ) {
                                        "Invalid retail payment account"
                                    }
                                }
                            }

                    requireNotNull(
                        resolvedFinancialAccount
                    ) {
                        "Financial account required for retail payment"
                    }

                    val incomingPaymentMethod =
                        resolvedFinancialAccount
                            ?.let {
                                retailPaymentMethodForAccount(
                                    it
                                )
                            }

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

                        val saleReturns =
                            dao.getRetailSaleReturnsOnce(
                                sale.id
                            )

                        val returnedAmount =
                            saleReturns.sumOf {
                                it.amount
                            }

                        val refundedAmount =
                            saleReturns.sumOf {
                                it.refundAmount
                            }

                        val effectiveTotal =
                            (
                                sale.total -
                                    returnedAmount
                            ).coerceAtLeast(
                                0.0
                            )

                        val netPaid =
                            (
                                sale.paid -
                                    refundedAmount
                            ).coerceAtLeast(
                                0.0
                            )

                        val remainingDue =
                            (
                                effectiveTotal -
                                    netPaid
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

                        val newNetPaid =
                            (
                                newPaid -
                                    refundedAmount
                            ).coerceAtLeast(
                                0.0
                            )

                        val newStatus =
                            if (
                                newNetPaid >=
                                    effectiveTotal -
                                        0.0001
                            ) {
                                "PAID"
                            } else {
                                "PARTIAL"
                            }

                        val newPaymentMethod =
                            incomingPaymentMethod
                                ?.let {
                                    mergedRetailPaymentMethod(
                                        oldMethod =
                                            sale.paymentMethod,
                                        alreadyPaid =
                                            sale.paid,
                                        incomingMethod =
                                            it
                                    )
                                }
                                ?: sale.paymentMethod

                        dao.updateRetailSalePayment(
                            saleId =
                                sale.id,
                            paid =
                                newPaid,
                            status =
                                newStatus,
                            paymentMethod =
                                newPaymentMethod
                        )

                        if (
                            resolvedFinancialAccount !=
                                null &&
                            incomingPaymentMethod !=
                                null
                        ) {
                            val paymentId =
                                dao.insertRetailSalePayment(
                                    RetailSalePaymentEntity(
                                        eventKey =
                                            UUID.randomUUID()
                                                .toString(),
                                        saleId =
                                            sale.id,
                                        financialAccountId =
                                            resolvedFinancialAccount
                                                .id,
                                        amount =
                                            amount,
                                        paymentMethod =
                                            incomingPaymentMethod,
                                        note =
                                            "Due collection ${sale.invoiceNo}",
                                        paidAt =
                                            System.currentTimeMillis()
                                    )
                                )

                            require(
                                paymentId > 0L
                            )
                        }

                        requireNotNull(
                            dao.getRetailSaleOnce(
                                sale.id
                            )
                        )
                    }
                }.getOrNull()

            /*
             * Inventory DB is authoritative.
             * Baki and account ledgers are idempotent
             * cross-database projections and retry when
             * this business context opens again.
             */
            updatedSale?.let {
                    sale ->

                runCatching {
                    reconcileRetailSaleBaki(
                        sale
                    )
                }

                runCatching {
                    reconcileRetailSaleAccounts(
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
            val saleBeforeCancellation =
                runCatching {
                    requireNotNull(
                        dao.getRetailSaleOnce(
                            saleId
                        )
                    ) {
                        "Sale not found"
                    }.also { sale ->
                        require(
                            sale.workspace ==
                                currentWorkspace &&
                                sale.businessKey ==
                                    currentBusinessKey
                        ) {
                            "Sale context mismatch"
                        }
                    }
                }.getOrNull()

            if (saleBeforeCancellation == null) {
                onDone(false)
                return@launch
            }

            // A sale with recorded returns cannot be cancelled; return the remaining items instead.
            if (
                dao.getRetailSaleReturnsOnce(
                    saleId
                ).isNotEmpty()
            ) {
                onDone(false)
                return@launch
            }

            if (
                saleBeforeCancellation.status ==
                "CANCELLED"
            ) {
                val projectionOk =
                    runCatching {
                        reconcileRetailSaleAccounts(
                            saleBeforeCancellation
                        )
                    }.isSuccess

                onDone(projectionOk)
                return@launch
            }

            /*
             * Inventory DB and Financial Account DB are separate.
             * Remove deterministic Retail credits first. If Inventory
             * cancellation fails, recreate projection as compensation.
             */
            val accountReversalReady =
                runCatching {
                    reconcileRetailSaleAccounts(
                        saleBeforeCancellation
                    )

                    val payments =
                        dao.getRetailSalePaymentsOnce(
                            saleId
                        )

                    bakiDatabase.withTransaction {
                        removeRetailSaleAccountCreditsSafely(
                            sale =
                                saleBeforeCancellation,
                            payments =
                                payments
                        )
                    }
                }.isSuccess

            if (!accountReversalReady) {
                onDone(false)
                return@launch
            }

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

            if (!result) {
                runCatching {
                    reconcileRetailSaleAccounts(
                        saleBeforeCancellation
                    )
                }
            }

            if (result) {
                dao.getRetailSaleOnce(
                    saleId
                )?.let { sale ->
                    runCatching {
                        reconcileRetailSaleBaki(
                            sale
                        )
                    }

                    runCatching {
                        reconcileRetailSaleAccounts(
                            sale
                        )
                    }
                }
            }

            onDone(result)
        }
    }

    fun addPurchaseSupplier(
        name: String,
        phone: String = "",
        address: String = "",
        note: String = "",
        onDone: (Long?) -> Unit = {}
    ) {
        val cleanName = name.trim()

        if (cleanName.isBlank()) {
            onDone(null)
            return
        }

        val currentWorkspace =
            workspace.value

        val currentBusinessKey =
            businessKey.value

        if (
            currentWorkspace == "SHOP" &&
            currentBusinessKey ==
                "__NO_BUSINESS__"
        ) {
            onDone(null)
            return
        }

        viewModelScope.launch {
            val supplierId =
                runCatching {
                    dao.insertPurchaseSupplier(
                        PurchaseSupplierEntity(
                            name = cleanName,
                            phone = phone.trim(),
                            address =
                                address.trim(),
                            note = note.trim(),
                            workspace =
                                currentWorkspace,
                            businessKey =
                                currentBusinessKey
                        )
                    )
                }.getOrNull()

            onDone(
                supplierId?.takeIf {
                    it > 0L
                }
            )
        }
    }

    private fun purchaseAccountSourceKey(
        eventKey: String
    ): String =
        "PURCHASE_ACCOUNT:$eventKey"

    private suspend fun reconcilePurchasePaymentAccount(
        bill: PurchaseBillEntity,
        payment: PurchasePaymentEntity
    ) {
        require(
            payment.billId == bill.id &&
                payment.amount.isFinite() &&
                payment.amount > 0.0001
        ) {
            "Invalid purchase payment"
        }

        val ledgerBusinessId =
            ledgerBusinessIdForInventoryContext(
                bill.workspace,
                bill.businessKey
            )

        bakiDatabase.withTransaction {
            val account =
                requireNotNull(
                    bakiDao.getFinancialAccountOnce(
                        payment.financialAccountId
                    )
                ) {
                    "Purchase payment account not found"
                }

            require(
                account.workspace ==
                    bill.workspace &&
                    (
                        bill.workspace != "SHOP" ||
                            account.businessId ==
                                ledgerBusinessId
                    )
            ) {
                "Purchase payment account scope mismatch"
            }

            val sourceKey =
                purchaseAccountSourceKey(
                    payment.eventKey
                )

            val existing =
                bakiDao
                    .getFinancialAccountEntryBySourceKey(
                        sourceKey
                    )

            if (existing != null) {
                require(
                    existing.accountId ==
                        account.id &&
                        existing.entryType ==
                            "PURCHASE_OUT" &&
                        kotlin.math.abs(
                            existing.amount -
                                payment.amount
                        ) < 0.0001 &&
                        kotlin.math.abs(
                            existing.balanceDelta +
                                payment.amount
                        ) < 0.0001 &&
                        existing.workspace ==
                            bill.workspace &&
                        existing.businessId ==
                            ledgerBusinessId
                ) {
                    "Purchase account projection mismatch"
                }

                return@withTransaction
            }

            val balance =
                requireNotNull(
                    bakiDao
                        .getFinancialAccountBalanceOnce(
                            account.id
                        )
                ) {
                    "Financial account balance unavailable"
                }

            require(
                balance - payment.amount >=
                    -0.0001
            ) {
                "Insufficient account balance"
            }

            val inserted =
                bakiDao.insertFinancialAccountEntry(
                    FinancialAccountEntryEntity(
                        accountId =
                            account.id,
                        entryType =
                            "PURCHASE_OUT",
                        amount =
                            payment.amount,
                        balanceDelta =
                            -payment.amount,
                        sourceKey =
                            sourceKey,
                        note =
                            "Purchase ${bill.purchaseNo} • ${payment.paymentMethod}",
                        workspace =
                            bill.workspace,
                        businessId =
                            ledgerBusinessId,
                        createdAt =
                            payment.paidAt
                    )
                )

            require(inserted > 0L)
        }
    }

    fun createPurchase(
        purchaseNo: String = "",
        supplierId: Long,
        lines: List<PurchaseLineInput>,
        discount: Double = 0.0,
        initialPaid: Double = 0.0,
        financialAccountId: Long? = null,
        note: String = "",
        purchasedAt: Long =
            System.currentTimeMillis(),
        onDone: (Long?) -> Unit = {}
    ) {
        if (
            supplierId <= 0L ||
            lines.isEmpty() ||
            lines.any {
                it.productId <= 0L ||
                    it.quantity <= 0 ||
                    it.unitFactor <= 0 ||
                    !it.unitCost.isFinite() ||
                    it.unitCost < 0.0
            } ||
            !discount.isFinite() ||
            discount < 0.0 ||
            !initialPaid.isFinite() ||
            initialPaid < 0.0 ||
            purchasedAt <= 0L
        ) {
            onDone(null)
            return
        }

        val currentWorkspace =
            workspace.value

        val currentBusinessKey =
            businessKey.value

        val currentLedgerBusinessId =
            ledgerBusinessIdForInventoryContext(
                currentWorkspace,
                currentBusinessKey
            )

        if (
            currentWorkspace == "SHOP" &&
            currentLedgerBusinessId.isBlank()
        ) {
            onDone(null)
            return
        }

        viewModelScope.launch {
            val created =
                runCatching {
                    val supplier =
                        requireNotNull(
                            dao.getPurchaseSupplierOnce(
                                supplierId
                            )
                        ) {
                            "Supplier not found"
                        }

                    require(
                        supplier.workspace ==
                            currentWorkspace &&
                            supplier.businessKey ==
                                currentBusinessKey &&
                            supplier.isActive
                    ) {
                        "Supplier scope mismatch"
                    }

                    val paymentAccount =
                        if (
                            initialPaid >
                            0.0001
                        ) {
                            requireNotNull(
                                financialAccountId
                                    ?.takeIf {
                                        it > 0L
                                    }
                                    ?.let {
                                            bakiDao
                                                .getFinancialAccountOnce(
                                                    it
                                                )
                                        }
                            ) {
                                "Financial account required"
                            }.also {
                                    account ->

                                require(
                                    account.workspace ==
                                        currentWorkspace &&
                                        (
                                            currentWorkspace !=
                                                "SHOP" ||
                                                account.businessId ==
                                                    currentLedgerBusinessId
                                        ) &&
                                        account.isActive
                                ) {
                                    "Invalid purchase payment account"
                                }

                                val balance =
                                    requireNotNull(
                                        bakiDao
                                            .getFinancialAccountBalanceOnce(
                                                account.id
                                            )
                                    )

                                require(
                                    balance -
                                        initialPaid >=
                                        -0.0001
                                ) {
                                    "Insufficient account balance"
                                }
                            }
                        } else {
                            null
                        }

                    database.withTransaction {
                        val freshSupplier =
                            requireNotNull(
                                dao.getPurchaseSupplierOnce(
                                    supplierId
                                )
                            )

                        require(
                            freshSupplier.workspace ==
                                currentWorkspace &&
                                freshSupplier.businessKey ==
                                    currentBusinessKey &&
                                freshSupplier.isActive
                        )

                        val resolvedLines =
                            lines.map { input ->
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
                                        currentWorkspace &&
                                        product.businessKey ==
                                            currentBusinessKey
                                ) {
                                    "Product scope mismatch"
                                }

                                val resolvedUnit =
                                    resolveExistingProductUnit(
                                        product =
                                            product,
                                        requestedUnitName =
                                            input.unitName,
                                        requestedUnitFactor =
                                            input.unitFactor
                                    )

                                val factor =
                                    resolvedUnit.second

                                val baseQuantityLong =
                                    input.quantity.toLong() *
                                        factor.toLong()

                                require(
                                    baseQuantityLong in
                                        1L..
                                        Int.MAX_VALUE
                                            .toLong()
                                )

                                val lineTotal =
                                    input.quantity
                                        .toDouble() *
                                        input.unitCost

                                require(
                                    lineTotal.isFinite() &&
                                        lineTotal >= 0.0
                                )

                                val basePurchasePrice =
                                    input.unitCost /
                                        factor.toDouble()

                                require(
                                    basePurchasePrice
                                        .isFinite() &&
                                        basePurchasePrice >=
                                            0.0
                                )

                                ResolvedPurchaseLine(
                                    input = input,
                                    product =
                                        product,
                                    unitName =
                                        resolvedUnit.first,
                                    unitFactor =
                                        factor,
                                    baseQuantity =
                                        baseQuantityLong
                                            .toInt(),
                                    basePurchasePrice =
                                        basePurchasePrice,
                                    lineTotal =
                                        lineTotal
                                )
                            }

                        val subtotal =
                            resolvedLines.sumOf {
                                it.lineTotal
                            }

                        require(
                            subtotal.isFinite() &&
                                subtotal >= 0.0
                        )

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
                            ).coerceAtLeast(
                                0.0
                            )

                        require(
                            initialPaid <=
                                total + 0.0001
                        ) {
                            "Payment exceeds purchase total"
                        }

                        val cleanPurchaseNo =
                            purchaseNo.trim()
                                .ifBlank {
                                    "PUR-${System.currentTimeMillis()}-${UUID.randomUUID().toString().take(4).uppercase(Locale.ROOT)}"
                                }

                        val bill =
                            PurchaseBillEntity(
                                purchaseNo =
                                    cleanPurchaseNo,
                                supplierId =
                                    freshSupplier.id,
                                subtotal =
                                    subtotal,
                                discount =
                                    discount,
                                total =
                                    total,
                                note =
                                    note.trim(),
                                workspace =
                                    currentWorkspace,
                                businessKey =
                                    currentBusinessKey,
                                purchasedAt =
                                    purchasedAt
                            )

                        val billId =
                            dao.insertPurchaseBill(
                                bill
                            )

                        require(billId > 0L)

                        resolvedLines.forEach {
                                line ->

                            val batchId =
                                dao.insertBatch(
                                    StockBatchEntity(
                                        productId =
                                            line.product.id,
                                        batchNo =
                                            line.input
                                                .batchNo
                                                .trim(),
                                        quantity =
                                            line.baseQuantity,
                                        purchasePrice =
                                            line.basePurchasePrice,
                                        purchaseDate =
                                            purchasedAt,
                                        expiryDate =
                                            line.input
                                                .expiryDate
                                    )
                                )

                            require(batchId > 0L)

                            val lineId =
                                dao.insertPurchaseBillLine(
                                    PurchaseBillLineEntity(
                                        billId =
                                            billId,
                                        productId =
                                            line.product.id,
                                        stockBatchId =
                                            batchId,
                                        productNameSnapshot =
                                            line.product.name,
                                        skuSnapshot =
                                            line.product.sku,
                                        unitSnapshot =
                                            line.unitName,
                                        unitFactor =
                                            line.unitFactor,
                                        quantity =
                                            line.input
                                                .quantity,
                                        baseQuantity =
                                            line.baseQuantity,
                                        unitCost =
                                            line.input
                                                .unitCost,
                                        lineTotal =
                                            line.lineTotal,
                                        batchNo =
                                            line.input
                                                .batchNo
                                                .trim(),
                                        expiryDate =
                                            line.input
                                                .expiryDate
                                    )
                                )

                            require(lineId > 0L)
                        }

                        var payment:
                            PurchasePaymentEntity? =
                            null

                        if (
                            initialPaid >
                            0.0001
                        ) {
                            val account =
                                requireNotNull(
                                    paymentAccount
                                )

                            val eventKey =
                                "PURCHASE_PAYMENT:${UUID.randomUUID()}"

                            val pendingPayment =
                                PurchasePaymentEntity(
                                    eventKey =
                                        eventKey,
                                    billId =
                                        billId,
                                    financialAccountId =
                                        account.id,
                                    amount =
                                        initialPaid,
                                    paymentMethod =
                                        retailPaymentMethodForAccount(
                                            account
                                        ),
                                    note =
                                        "Initial purchase payment",
                                    paidAt =
                                        purchasedAt
                                )

                            val paymentId =
                                dao.insertPurchasePayment(
                                    pendingPayment
                                )

                            require(paymentId > 0L)

                            payment =
                                pendingPayment.copy(
                                    id =
                                        paymentId
                                )
                        }

                        bill.copy(
                            id = billId
                        ) to payment
                    }
                }.getOrNull()

            if (created == null) {
                onDone(null)
                return@launch
            }

            val bill =
                created.first

            val payment =
                created.second

            if (payment != null) {
                val projected =
                    runCatching {
                        reconcilePurchasePaymentAccount(
                            bill = bill,
                            payment = payment
                        )
                    }.isSuccess

                if (!projected) {
                    runCatching {
                        dao.deletePurchasePaymentByEventKey(
                            payment.eventKey
                        )
                    }
                }
            }

            onDone(bill.id)
        }
    }

    fun recordPurchaseReturn(
        billId: Long,
        purchaseLineId: Long,
        quantity: Int,
        refundFinancialAccountId: Long? = null,
        note: String = "",
        returnedAt: Long =
            System.currentTimeMillis(),
        onDone: (Boolean) -> Unit = {}
    ) {
        val currentWorkspace =
            workspace.value

        val currentBusinessKey =
            businessKey.value

        val currentLedgerBusinessId =
            ledgerBusinessIdForInventoryContext(
                currentWorkspace,
                currentBusinessKey
            )

        viewModelScope.launch {
            val success =
                purchaseReturnService.record(
                    billId = billId,
                    lineId =
                        purchaseLineId,
                    quantity = quantity,
                    refundAccountId =
                        refundFinancialAccountId,
                    note = note,
                    returnedAt =
                        returnedAt,
                    workspace =
                        currentWorkspace,
                    businessKey =
                        currentBusinessKey,
                    ledgerBusinessId =
                        currentLedgerBusinessId
                )

            onDone(success)
        }
    }

    fun addPurchasePayment(
        billId: Long,
        amount: Double,
        financialAccountId: Long,
        note: String = "",
        paidAt: Long =
            System.currentTimeMillis(),
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            billId <= 0L ||
            financialAccountId <= 0L ||
            !amount.isFinite() ||
            amount <= 0.0001 ||
            paidAt <= 0L
        ) {
            onDone(false)
            return
        }

        val currentWorkspace =
            workspace.value

        val currentBusinessKey =
            businessKey.value

        val currentLedgerBusinessId =
            ledgerBusinessIdForInventoryContext(
                currentWorkspace,
                currentBusinessKey
            )

        viewModelScope.launch {
            val result =
                runCatching {
                    val bill =
                        requireNotNull(
                            dao.getPurchaseBillOnce(
                                billId
                            )
                        ) {
                            "Purchase not found"
                        }

                    require(
                        bill.workspace ==
                            currentWorkspace &&
                            bill.businessKey ==
                                currentBusinessKey &&
                            bill.status !=
                                "CANCELLED"
                    ) {
                        "Purchase scope mismatch"
                    }

                    val payments =
                        dao.getPurchasePaymentsOnce(
                            bill.id
                        )

                    val alreadyPaid =
                        payments.sumOf {
                            it.amount
                        }

                    val due =
                        (
                            bill.total -
                                alreadyPaid
                        ).coerceAtLeast(
                            0.0
                        )

                    require(
                        amount <=
                            due + 0.0001
                    ) {
                        "Payment exceeds due"
                    }

                    val account =
                        requireNotNull(
                            bakiDao
                                .getFinancialAccountOnce(
                                    financialAccountId
                                )
                        ) {
                            "Financial account not found"
                        }

                    require(
                        account.workspace ==
                            currentWorkspace &&
                            (
                                currentWorkspace !=
                                    "SHOP" ||
                                    account.businessId ==
                                        currentLedgerBusinessId
                            ) &&
                            account.isActive
                    ) {
                        "Invalid payment account"
                    }

                    val balance =
                        requireNotNull(
                            bakiDao
                                .getFinancialAccountBalanceOnce(
                                    account.id
                                )
                        )

                    require(
                        balance -
                            amount >=
                            -0.0001
                    ) {
                        "Insufficient account balance"
                    }

                    val payment =
                        PurchasePaymentEntity(
                            eventKey =
                                "PURCHASE_PAYMENT:${UUID.randomUUID()}",
                            billId =
                                bill.id,
                            financialAccountId =
                                account.id,
                            amount =
                                amount,
                            paymentMethod =
                                retailPaymentMethodForAccount(
                                    account
                                ),
                            note =
                                note.trim(),
                            paidAt =
                                paidAt
                        )

                    val paymentId =
                        dao.insertPurchasePayment(
                            payment
                        )

                    require(paymentId > 0L)

                    val insertedPayment =
                        payment.copy(
                            id =
                                paymentId
                        )

                    val projected =
                        runCatching {
                            reconcilePurchasePaymentAccount(
                                bill =
                                    bill,
                                payment =
                                    insertedPayment
                            )
                        }.isSuccess

                    if (!projected) {
                        dao.deletePurchasePaymentByEventKey(
                            payment.eventKey
                        )
                    }

                    require(projected)

                    true
                }.getOrDefault(false)

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
