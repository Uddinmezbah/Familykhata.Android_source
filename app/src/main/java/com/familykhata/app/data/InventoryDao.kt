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


    @Query(
        """
        UPDATE inventory_products
        SET businessKey = :targetBusinessId
        WHERE workspace = :workspace
          AND businessKey IN ('legacy', :legacyBusinessKey)
        """
    )
    suspend fun claimExistingBusinessProducts(
        workspace: String,
        legacyBusinessKey: String,
        targetBusinessId: String
    ): Int

    @Query(
        """
        UPDATE retail_sales
        SET businessKey = :targetBusinessId
        WHERE workspace = :workspace
          AND businessKey IN ('legacy', :legacyBusinessKey)
        """
    )
    suspend fun claimExistingBusinessRetailSales(
        workspace: String,
        legacyBusinessKey: String,
        targetBusinessId: String
    ): Int

    @Query(
        """
        UPDATE inventory_products
        SET businessKey = :targetBusinessId
        WHERE workspace = 'SHOP'
          AND businessKey = :sourceBusinessId
          AND createdAt < :createdBefore
        """
    )
    suspend fun moveProductsCreatedBefore(
        sourceBusinessId: String,
        createdBefore: Long,
        targetBusinessId: String
    ): Int

    @Query(
        """
        UPDATE retail_sales
        SET businessKey = :targetBusinessId
        WHERE workspace = 'SHOP'
          AND businessKey = :sourceBusinessId
          AND createdAt < :createdBefore
        """
    )
    suspend fun moveRetailSalesCreatedBefore(
        sourceBusinessId: String,
        createdBefore: Long,
        targetBusinessId: String
    ): Int

    @Query(
        """
        UPDATE inventory_products
        SET businessKey = :targetBusinessKey
        WHERE workspace = :workspace
          AND businessKey = :sourceBusinessKey
        """
    )
    suspend fun moveInventoryBusinessKey(
        workspace: String,
        sourceBusinessKey: String,
        targetBusinessKey: String
    ): Int

    @Query(
        """
        UPDATE retail_sales
        SET businessKey = :targetBusinessKey
        WHERE workspace = :workspace
          AND businessKey = :sourceBusinessKey
        """
    )
    suspend fun moveRetailSalesBusinessKey(
        workspace: String,
        sourceBusinessKey: String,
        targetBusinessKey: String
    ): Int

    @Query("SELECT * FROM inventory_products WHERE id = :productId LIMIT 1")
    suspend fun getProductOnce(productId: Long): ProductEntity?

    @Query(
        """
        SELECT COUNT(*)
        FROM inventory_products
        WHERE workspace = :workspace
          AND businessKey = :businessKey
          AND id != :excludeProductId
          AND TRIM(sku) != ''
          AND LOWER(TRIM(sku)) =
              LOWER(TRIM(:sku))
        """
    )
    suspend fun countProductSkuConflicts(
        workspace: String,
        businessKey: String,
        sku: String,
        excludeProductId: Long = 0L
    ): Int

    @Query(
        """
        SELECT *
        FROM inventory_product_units
        WHERE productId = :productId
        ORDER BY sortOrder ASC, id ASC
        """
    )
    fun observeProductUnitConversions(
        productId: Long
    ): Flow<List<ProductUnitConversionEntity>>

    @Query(
        """
        SELECT *
        FROM inventory_product_units
        WHERE productId = :productId
        ORDER BY sortOrder ASC, id ASC
        """
    )
    suspend fun getProductUnitConversionsOnce(
        productId: Long
    ): List<ProductUnitConversionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProductUnitConversion(
        item: ProductUnitConversionEntity
    ): Long

    @Query(
        """
        DELETE FROM inventory_product_units
        WHERE productId = :productId
        """
    )
    suspend fun deleteProductUnitConversions(
        productId: Long
    ): Int

    @Query(
        """
        SELECT *
        FROM inventory_product_units
        ORDER BY productId ASC, sortOrder ASC, id ASC
        """
    )
    suspend fun getAllProductUnitConversions():
        List<ProductUnitConversionEntity>

    @Query("DELETE FROM inventory_product_units")
    suspend fun clearProductUnitConversions()

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

    // -----------------------------------------------------------------
    // Retail sales
    // -----------------------------------------------------------------

    @Insert
    suspend fun insertRetailSale(
        item: RetailSaleEntity
    ): Long

    @Insert
    suspend fun insertRetailSaleLine(
        item: RetailSaleLineEntity
    ): Long

    @Insert
    suspend fun insertRetailSaleStockAllocation(
        item: RetailSaleStockAllocationEntity
    ): Long

    @Insert(
        onConflict = OnConflictStrategy.ABORT
    )
    suspend fun insertRetailSalePayment(
        item: RetailSalePaymentEntity
    ): Long

    @Query(
        """
        SELECT *
        FROM retail_sale_payments
        WHERE saleId = :saleId
        ORDER BY paidAt ASC, id ASC
        """
    )
    suspend fun getRetailSalePaymentsOnce(
        saleId: Long
    ): List<RetailSalePaymentEntity>

    @Query(
        """
        SELECT *
        FROM retail_sale_payments
        WHERE saleId = :saleId
        ORDER BY paidAt DESC, id DESC
        """
    )
    fun observeRetailSalePayments(
        saleId: Long
    ): Flow<List<RetailSalePaymentEntity>>

    @Query(
        """
        SELECT *
        FROM retail_sales
        WHERE workspace = :workspace
          AND businessKey = :businessKey
        ORDER BY soldAt DESC, id DESC
        """
    )
    fun observeRetailSales(
        workspace: String,
        businessKey: String
    ): Flow<List<RetailSaleEntity>>

    @Query(
        """
        SELECT *
        FROM retail_sales
        WHERE workspace = :workspace
          AND businessKey = :businessKey
        ORDER BY soldAt ASC, id ASC
        """
    )
    suspend fun getRetailSalesOnce(
        workspace: String,
        businessKey: String
    ): List<RetailSaleEntity>

    @Query(
        """
        SELECT *
        FROM retail_sale_lines
        WHERE saleId = :saleId
        ORDER BY id ASC
        """
    )
    fun observeRetailSaleLines(
        saleId: Long
    ): Flow<List<RetailSaleLineEntity>>

    @Query(
        """
        SELECT *
        FROM retail_sale_stock_allocations
        WHERE saleLineId = :saleLineId
        ORDER BY id ASC
        """
    )
    fun observeRetailSaleStockAllocations(
        saleLineId: Long
    ): Flow<List<RetailSaleStockAllocationEntity>>

    @Query(
        """
        SELECT *
        FROM retail_sales
        WHERE id = :saleId
        LIMIT 1
        """
    )
    suspend fun getRetailSaleOnce(
        saleId: Long
    ): RetailSaleEntity?

    @Query(
        """
        SELECT *
        FROM retail_sale_lines
        WHERE saleId = :saleId
        ORDER BY id ASC
        """
    )
    suspend fun getRetailSaleLinesOnce(
        saleId: Long
    ): List<RetailSaleLineEntity>

    @Query(
        """
        SELECT COUNT(*)
        FROM retail_sales
        WHERE workspace = :workspace
          AND businessKey = :businessKey
          AND invoiceNo = :invoiceNo
        """
    )
    suspend fun retailInvoiceNumberCount(
        workspace: String,
        businessKey: String,
        invoiceNo: String
    ): Int

    @Query(
        """
        SELECT *
        FROM retail_sale_stock_allocations
        WHERE saleLineId = :saleLineId
        ORDER BY id ASC
        """
    )
    suspend fun getRetailSaleStockAllocationsOnce(
        saleLineId: Long
    ): List<RetailSaleStockAllocationEntity>

    @Query(
        """
        UPDATE retail_sales
        SET status = :status
        WHERE id = :saleId
        """
    )
    suspend fun updateRetailSaleStatus(
        saleId: Long,
        status: String
    )

    @Query(
        """
        UPDATE retail_sales
        SET paid = :paid,
            status = :status,
            paymentMethod = :paymentMethod
        WHERE id = :saleId
        """
    )
    suspend fun updateRetailSalePayment(
        saleId: Long,
        paid: Double,
        status: String,
        paymentMethod: String
    )

    @Query(
        """
        SELECT COUNT(*)
        FROM retail_sale_lines AS line
        INNER JOIN retail_sales AS sale
            ON sale.id = line.saleId
        WHERE line.productId = :productId
          AND sale.status != 'CANCELLED'
        """
    )
    suspend fun activeRetailSaleCountForProduct(
        productId: Long
    ): Int


    // Purchase + supplier
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPurchaseSupplier(
        item: PurchaseSupplierEntity
    ): Long

    @Query(
        """
        UPDATE purchase_suppliers
        SET name = :name,
            phone = :phone,
            address = :address,
            note = :note,
            isActive = :isActive
        WHERE id = :supplierId
        """
    )
    suspend fun updatePurchaseSupplier(
        supplierId: Long,
        name: String,
        phone: String,
        address: String,
        note: String,
        isActive: Boolean
    ): Int

    @Query(
        """
        SELECT *
        FROM purchase_suppliers
        WHERE id = :supplierId
        LIMIT 1
        """
    )
    suspend fun getPurchaseSupplierOnce(
        supplierId: Long
    ): PurchaseSupplierEntity?

    @Query(
        """
        SELECT
            s.id AS id,
            s.name AS name,
            s.phone AS phone,
            s.address AS address,
            s.note AS note,
            s.isActive AS isActive,
            COUNT(DISTINCT b.id) AS purchaseCount,
            COALESCE(
                SUM(
                    CASE
                        WHEN b.status != 'CANCELLED'
                        THEN b.total
                        ELSE 0
                    END
                ),
                0
            ) AS totalPurchase,
            COALESCE(
                (
                    SELECT SUM(p.amount)
                    FROM purchase_payments p
                    INNER JOIN purchase_bills pb
                        ON pb.id = p.billId
                    WHERE pb.supplierId = s.id
                      AND pb.status != 'CANCELLED'
                ),
                0
            ) AS paid,
            MAX(
                0,
                COALESCE(
                    SUM(
                        CASE
                            WHEN b.status != 'CANCELLED'
                            THEN b.total
                            ELSE 0
                        END
                    ),
                    0
                ) -
                COALESCE(
                    (
                        SELECT SUM(p.amount)
                        FROM purchase_payments p
                        INNER JOIN purchase_bills pb
                            ON pb.id = p.billId
                        WHERE pb.supplierId = s.id
                          AND pb.status != 'CANCELLED'
                    ),
                    0
                )
            ) AS due
        FROM purchase_suppliers s
        LEFT JOIN purchase_bills b
            ON b.supplierId = s.id
        WHERE s.workspace = :workspace
          AND s.businessKey = :businessKey
        GROUP BY s.id
        ORDER BY s.isActive DESC,
                 s.name COLLATE NOCASE ASC
        """
    )
    fun observePurchaseSupplierSummaries(
        workspace: String,
        businessKey: String
    ): Flow<List<PurchaseSupplierSummary>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPurchaseBill(
        item: PurchaseBillEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPurchaseBillLine(
        item: PurchaseBillLineEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPurchasePayment(
        item: PurchasePaymentEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPurchaseReturn(
        item: PurchaseReturnEntity
    ): Long

    @Query(
        """
        SELECT *
        FROM purchase_bills
        WHERE id = :billId
        LIMIT 1
        """
    )
    suspend fun getPurchaseBillOnce(
        billId: Long
    ): PurchaseBillEntity?

    @Query(
        """
        SELECT *
        FROM purchase_bill_lines
        WHERE billId = :billId
        ORDER BY id ASC
        """
    )
    suspend fun getPurchaseBillLinesOnce(
        billId: Long
    ): List<PurchaseBillLineEntity>

    @Query(
        """
        SELECT *
        FROM purchase_payments
        WHERE billId = :billId
        ORDER BY paidAt ASC, id ASC
        """
    )
    suspend fun getPurchasePaymentsOnce(
        billId: Long
    ): List<PurchasePaymentEntity>

    @Query(
        """
        SELECT *
        FROM purchase_returns
        WHERE billId = :billId
        ORDER BY returnedAt ASC, id ASC
        """
    )
    suspend fun getPurchaseReturnsOnce(
        billId: Long
    ): List<PurchaseReturnEntity>

    @Query(
        """
        SELECT
            b.id AS id,
            b.purchaseNo AS purchaseNo,
            b.supplierId AS supplierId,
            s.name AS supplierName,
            b.subtotal AS subtotal,
            b.discount AS discount,
            b.total AS total,
            COALESCE(SUM(p.amount), 0) AS paid,
            MAX(
                0,
                b.total - COALESCE(SUM(p.amount), 0)
            ) AS due,
            b.status AS status,
            b.note AS note,
            b.purchasedAt AS purchasedAt
        FROM purchase_bills b
        INNER JOIN purchase_suppliers s
            ON s.id = b.supplierId
        LEFT JOIN purchase_payments p
            ON p.billId = b.id
        WHERE b.workspace = :workspace
          AND b.businessKey = :businessKey
        GROUP BY b.id
        ORDER BY b.purchasedAt DESC,
                 b.id DESC
        """
    )
    fun observePurchaseBillSummaries(
        workspace: String,
        businessKey: String
    ): Flow<List<PurchaseBillSummary>>

    @Query("SELECT * FROM purchase_suppliers ORDER BY id ASC")
    suspend fun getAllPurchaseSuppliers():
        List<PurchaseSupplierEntity>

    @Query("SELECT * FROM purchase_bills ORDER BY id ASC")
    suspend fun getAllPurchaseBills():
        List<PurchaseBillEntity>

    @Query("SELECT * FROM purchase_bill_lines ORDER BY id ASC")
    suspend fun getAllPurchaseBillLines():
        List<PurchaseBillLineEntity>

    @Query("SELECT * FROM purchase_payments ORDER BY id ASC")
    suspend fun getAllPurchasePayments():
        List<PurchasePaymentEntity>

    @Query("SELECT * FROM purchase_returns ORDER BY id ASC")
    suspend fun getAllPurchaseReturns():
        List<PurchaseReturnEntity>

    @Query(
        """
        DELETE FROM purchase_payments
        WHERE eventKey = :eventKey
        """
    )
    suspend fun deletePurchasePaymentByEventKey(
        eventKey: String
    ): Int

    @Query("DELETE FROM purchase_returns")
    suspend fun clearPurchaseReturns()

    @Query("DELETE FROM purchase_payments")
    suspend fun clearPurchasePayments()

    @Query("DELETE FROM purchase_bill_lines")
    suspend fun clearPurchaseBillLines()

    @Query("DELETE FROM purchase_bills")
    suspend fun clearPurchaseBills()

    @Query("DELETE FROM purchase_suppliers")
    suspend fun clearPurchaseSuppliers()

    // Retail backup / restore
    @Query("SELECT * FROM retail_sales ORDER BY id ASC")
    suspend fun getAllRetailSales(): List<RetailSaleEntity>

    @Query("SELECT * FROM retail_sale_lines ORDER BY id ASC")
    suspend fun getAllRetailSaleLines(): List<RetailSaleLineEntity>

    @Query("SELECT * FROM retail_sale_stock_allocations ORDER BY id ASC")
    suspend fun getAllRetailSaleStockAllocations():
        List<RetailSaleStockAllocationEntity>

    @Query("SELECT * FROM retail_sale_payments ORDER BY id ASC")
    suspend fun getAllRetailSalePayments(): List<RetailSalePaymentEntity>

    @Query("DELETE FROM retail_sale_payments")
    suspend fun clearRetailSalePayments()

    @Query("DELETE FROM retail_sale_stock_allocations")
    suspend fun clearRetailSaleStockAllocations()

    @Query("DELETE FROM retail_sale_lines")
    suspend fun clearRetailSaleLines()

    @Query("DELETE FROM retail_sales")
    suspend fun clearRetailSales()


}
