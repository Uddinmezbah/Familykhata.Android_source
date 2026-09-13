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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class InventoryViewModel(application: Application) : AndroidViewModel(application) {
    private val database = InventoryDatabase.get(application)
    private val dao = database.dao()
    private val workspace = MutableStateFlow("SHOP")

    val products: StateFlow<List<ProductStockSummary>> = workspace
        .flatMapLatest { dao.observeProductSummaries(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setWorkspace(value: String) {
        if (workspace.value != value) workspace.value = value
    }

    fun observeBatches(productId: Long): Flow<List<StockBatchEntity>> = dao.observeBatches(productId)

    fun addProduct(
        name: String,
        category: String,
        sku: String,
        sellingPrice: Double,
        lowStockLevel: Int,
        note: String,
        workspace: String,
        initialQuantity: Int,
        purchasePrice: Double,
        purchaseDate: Long,
        expiryDate: Long?
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
                        sellingPrice = sellingPrice.coerceAtLeast(0.0),
                        lowStockLevel = lowStockLevel.coerceAtLeast(0),
                        note = note.trim(),
                        workspace = workspace
                    )
                )
                if (initialQuantity > 0) {
                    dao.insertBatch(
                        StockBatchEntity(
                            productId = productId,
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
        sellingPrice: Double,
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
                sellingPrice = sellingPrice.coerceAtLeast(0.0),
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
        expiryDate: Long?
    ) {
        if (quantity <= 0) return
        viewModelScope.launch {
            dao.insertBatch(
                StockBatchEntity(
                    productId = productId,
                    quantity = quantity,
                    purchasePrice = purchasePrice.coerceAtLeast(0.0),
                    purchaseDate = purchaseDate,
                    expiryDate = expiryDate
                )
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
