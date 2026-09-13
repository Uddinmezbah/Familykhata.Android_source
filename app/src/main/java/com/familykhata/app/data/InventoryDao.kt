package com.familykhata.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(item: ProductEntity): Long

    @Query(
        """
        UPDATE inventory_products
        SET name = :name,
            category = :category,
            sku = :sku,
            sellingPrice = :sellingPrice,
            lowStockLevel = :lowStockLevel,
            note = :note
        WHERE id = :productId
        """
    )
    suspend fun updateProduct(
        productId: Long,
        name: String,
        category: String,
        sku: String,
        sellingPrice: Double,
        lowStockLevel: Int,
        note: String
    )

    @Query("DELETE FROM inventory_products WHERE id = :productId")
    suspend fun deleteProductById(productId: Long)

    @Query(
        """
        SELECT p.id AS id,
               p.name AS name,
               p.category AS category,
               p.sku AS sku,
               p.sellingPrice AS sellingPrice,
               p.lowStockLevel AS lowStockLevel,
               p.note AS note,
               p.workspace AS workspace,
               COALESCE(SUM(b.quantity), 0) AS totalStock,
               COALESCE(SUM(CASE WHEN b.quantity > 0 THEN b.quantity * b.purchasePrice ELSE 0 END), 0) AS stockValue,
               MIN(CASE WHEN b.quantity > 0 AND b.expiryDate IS NOT NULL THEN b.expiryDate END) AS nextExpiry
        FROM inventory_products p
        LEFT JOIN inventory_batches b ON b.productId = p.id
        WHERE p.workspace = :workspace
        GROUP BY p.id
        ORDER BY p.name COLLATE NOCASE ASC
        """
    )
    fun observeProductSummaries(workspace: String): Flow<List<ProductStockSummary>>

    @Query("SELECT * FROM inventory_products WHERE workspace = :workspace ORDER BY name COLLATE NOCASE ASC")
    fun observeProducts(workspace: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM inventory_batches WHERE productId = :productId ORDER BY purchaseDate ASC, id ASC")
    fun observeBatches(productId: Long): Flow<List<StockBatchEntity>>

    @Query("SELECT * FROM inventory_batches WHERE productId = :productId ORDER BY purchaseDate ASC, id ASC")
    suspend fun getBatchesOnce(productId: Long): List<StockBatchEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(item: StockBatchEntity): Long

    @Query("UPDATE inventory_batches SET quantity = :quantity WHERE id = :batchId")
    suspend fun updateBatchQuantity(batchId: Long, quantity: Int)

    @Delete
    suspend fun deleteBatch(item: StockBatchEntity)

    @Query("SELECT * FROM inventory_products ORDER BY id ASC")
    suspend fun getAllProducts(): List<ProductEntity>

    @Query("SELECT * FROM inventory_batches ORDER BY id ASC")
    suspend fun getAllBatches(): List<StockBatchEntity>

    @Query("DELETE FROM inventory_batches")
    suspend fun clearBatches()

    @Query("DELETE FROM inventory_products")
    suspend fun clearProducts()
}
