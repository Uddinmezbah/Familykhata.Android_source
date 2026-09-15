package com.familykhata.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.familykhata.app.data.InventoryDatabase
import com.familykhata.app.data.ProductEntity
import com.familykhata.app.data.ProductStockSummary
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

    fun observeBatches(productId: Long): Flow<List<StockBatchEntity>> = dao.observeBatches(productId)

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
