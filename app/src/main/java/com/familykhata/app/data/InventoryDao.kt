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
            unit = :unit,
            brand = :brand,
            genericName = :genericName,
            modelName = :modelName,
            serialOrImei = :serialOrImei,
            size = :size,
            color = :color,
            warrantyMonths = :warrantyMonths,
            sellingPrice = :sellingPrice,
            mrp = :mrp,
            rackLocation = :rackLocation,
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
    )

    @Query("DELETE FROM inventory_products WHERE id = :productId")
    suspend fun deleteProductById(productId: Long)

    @Query(
        """
        SELECT p.id AS id,
               p.name AS name,
               p.category AS category,
               p.sku AS sku,
               p.unit AS unit,
               p.brand AS brand,
               p.genericName AS genericName,
               p.modelName AS modelName,
               p.serialOrImei AS serialOrImei,
               p.size AS size,
               p.color AS color,
               p.warrantyMonths AS warrantyMonths,
               p.sellingPrice AS sellingPrice,
               p.mrp AS mrp,
               p.rackLocation AS rackLocation,
               p.lowStockLevel AS lowStockLevel,
               p.note AS note,
               p.workspace AS workspace,
               p.businessKey AS businessKey,
               COALESCE(SUM(b.quantity), 0) AS totalStock,
               COALESCE(SUM(CASE WHEN b.quantity > 0 THEN b.quantity * b.purchasePrice ELSE 0 END), 0) AS stockValue,
               CASE
                   WHEN COALESCE(SUM(b.quantity), 0) > 0
                   THEN COALESCE(SUM(CASE WHEN b.quantity > 0 THEN b.quantity * b.purchasePrice ELSE 0 END), 0) * 1.0
                        / COALESCE(SUM(b.quantity), 0)
                   ELSE 0
               END AS avgPurchasePrice,
               COALESCE(SUM(b.quantity), 0) * p.sellingPrice AS saleValue,
               (COALESCE(SUM(b.quantity), 0) * p.sellingPrice)
                   - COALESCE(SUM(CASE WHEN b.quantity > 0 THEN b.quantity * b.purchasePrice ELSE 0 END), 0)
                   AS potentialProfit,
               MIN(CASE WHEN b.quantity > 0 AND b.expiryDate IS NOT NULL THEN b.expiryDate END) AS nextExpiry
        FROM inventory_products p
        LEFT JOIN inventory_batches b ON b.productId = p.id
        WHERE p.workspace = :workspace
          AND p.businessKey = :businessKey
        GROUP BY p.id
        ORDER BY p.name COLLATE NOCASE ASC
        """
    )
    fun observeProductSummaries(
        workspace: String,
        businessKey: String
    ): Flow<List<ProductStockSummary>>

    @Query("SELECT * FROM inventory_products WHERE workspace = :workspace ORDER BY name COLLATE NOCASE ASC")
    fun observeProducts(workspace: String): Flow<List<ProductEntity>>

    @Query(
        """
        SELECT *
        FROM inventory_products
        WHERE workspace = :workspace
          AND businessKey = :businessKey
        ORDER BY name COLLATE NOCASE ASC
        """
    )
    fun observeProductsForBusiness(
        workspace: String,
        businessKey: String
    ): Flow<List<ProductEntity>>

    @Query(
        """
        UPDATE inventory_products
        SET businessKey = :businessKey
        WHERE workspace = :workspace
          AND businessKey = 'legacy'
        """
    )
    suspend fun claimLegacyProducts(
        workspace: String,
        businessKey: String
    ): Int


    @Query("SELECT * FROM inventory_products WHERE id = :productId LIMIT 1")
    suspend fun getProductOnce(productId: Long): ProductEntity?

    @Query("SELECT * FROM inventory_batches WHERE productId = :productId ORDER BY purchaseDate ASC, id ASC")
    fun observeBatches(productId: Long): Flow<List<StockBatchEntity>>

    @Query("SELECT * FROM inventory_batches WHERE productId = :productId ORDER BY purchaseDate ASC, id ASC")
    suspend fun getBatchesOnce(productId: Long): List<StockBatchEntity>

    @Query("SELECT * FROM inventory_batches WHERE id = :batchId LIMIT 1")
    suspend fun getBatchOnce(batchId: Long): StockBatchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(item: StockBatchEntity): Long

    @Query("UPDATE inventory_batches SET quantity = :quantity WHERE id = :batchId")
    suspend fun updateBatchQuantity(batchId: Long, quantity: Int)

    @Query(
        """
        UPDATE inventory_batches
        SET quantity = :quantity,
            purchasePrice = :purchasePrice,
            purchaseDate = :purchaseDate,
            expiryDate = :expiryDate,
            batchNo = :batchNo
        WHERE id = :batchId
        """
    )
    suspend fun updateBatch(
        batchId: Long,
        quantity: Int,
        purchasePrice: Double,
        purchaseDate: Long,
        expiryDate: Long?,
        batchNo: String
    )

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
