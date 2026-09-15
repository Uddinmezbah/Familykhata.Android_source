package com.familykhata.app.dealerbusiness

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DealerBusinessDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompany(
        item: DealerCompanyEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArea(
        item: DealerAreaEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(
        item: DealerCustomerEntity
    ): Long

    @Update
    suspend fun updateCompany(
        item: DealerCompanyEntity
    ): Int

    @Update
    suspend fun updateArea(
        item: DealerAreaEntity
    ): Int

    @Update
    suspend fun updateCustomer(
        item: DealerCustomerEntity
    ): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPurchase(
        item: DealerPurchaseEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPurchaseLine(
        item: DealerPurchaseLineEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSupplierPayment(
        item: DealerSupplierPaymentEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSupplierPaymentAllocation(
        item: DealerSupplierPaymentAllocationEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSale(
        item: DealerSaleEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSaleLine(
        item: DealerSaleLineEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStockAllocation(
        item: DealerStockAllocationEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCollection(
        item: DealerCollectionEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCollectionAllocation(
        item: DealerCollectionAllocationEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSalesReturn(
        item: DealerSalesReturnEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSalesReturnAllocation(
        item: DealerSalesReturnAllocationEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPurchaseReturn(
        item: DealerPurchaseReturnEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(
        item: DealerExpenseEntity
    ): Long

    @Update
    suspend fun updateExpense(
        item: DealerExpenseEntity
    ): Int

    @Update
    suspend fun updatePurchase(
        item: DealerPurchaseEntity
    ): Int

    @Update
    suspend fun updateSale(
        item: DealerSaleEntity
    ): Int

    @Update
    suspend fun updateCollection(
        item: DealerCollectionEntity
    ): Int

    @Update
    suspend fun updateSupplierPayment(
        item: DealerSupplierPaymentEntity
    ): Int

    @Update
    suspend fun updateSalesReturn(
        item: DealerSalesReturnEntity
    ): Int

    @Update
    suspend fun updatePurchaseReturn(
        item: DealerPurchaseReturnEntity
    ): Int

    @Query("""
        SELECT *
        FROM dealer_business_companies
        WHERE workspace = :workspace
        ORDER BY name COLLATE NOCASE
    """)
    fun observeCompanies(
        workspace: String
    ): Flow<List<DealerCompanyEntity>>

    @Query("""
        SELECT *
        FROM dealer_business_areas
        WHERE workspace = :workspace
        ORDER BY name COLLATE NOCASE
    """)
    fun observeAreas(
        workspace: String
    ): Flow<List<DealerAreaEntity>>

    @Query("""
        SELECT *
        FROM dealer_business_customers
        WHERE workspace = :workspace
        ORDER BY name COLLATE NOCASE
    """)
    fun observeCustomers(
        workspace: String
    ): Flow<List<DealerCustomerEntity>>

    @Query("""
        SELECT *
        FROM dealer_business_purchases
        WHERE workspace = :workspace
        ORDER BY purchasedAt DESC, id DESC
    """)
    fun observePurchases(
        workspace: String
    ): Flow<List<DealerPurchaseEntity>>

    @Query("""
        SELECT *
        FROM dealer_business_sales
        WHERE workspace = :workspace
        ORDER BY soldAt DESC, id DESC
    """)
    fun observeSales(
        workspace: String
    ): Flow<List<DealerSaleEntity>>

    @Query("""
        SELECT *
        FROM dealer_business_expenses
        WHERE workspace = :workspace
        ORDER BY expenseAt DESC, id DESC
    """)
    fun observeExpenses(
        workspace: String
    ): Flow<List<DealerExpenseEntity>>

    @Query("""
        SELECT *
        FROM dealer_business_collections
        WHERE workspace = :workspace
        ORDER BY collectedAt DESC, id DESC
    """)
    fun observeAllCollections(
        workspace: String
    ): Flow<List<DealerCollectionEntity>>

    @Query("""
        SELECT *
        FROM dealer_business_supplier_payments
        WHERE workspace = :workspace
        ORDER BY paidAt DESC, id DESC
    """)
    fun observeAllSupplierPayments(
        workspace: String
    ): Flow<List<DealerSupplierPaymentEntity>>

    @Query("""
        SELECT *
        FROM dealer_business_purchase_lines
        WHERE purchaseId = :purchaseId
        ORDER BY id
    """)
    fun observePurchaseLines(
        purchaseId: Long
    ): Flow<List<DealerPurchaseLineEntity>>

    @Query("""
        SELECT *
        FROM dealer_business_sale_lines
        WHERE saleId = :saleId
        ORDER BY id
    """)
    fun observeSaleLines(
        saleId: Long
    ): Flow<List<DealerSaleLineEntity>>

    @Query("""
        SELECT *
        FROM dealer_business_supplier_payments
        WHERE companyId = :companyId
        ORDER BY paidAt DESC, id DESC
    """)
    fun observeSupplierPayments(
        companyId: Long
    ): Flow<List<DealerSupplierPaymentEntity>>

    @Query("""
        SELECT *
        FROM dealer_business_supplier_payment_allocations
        WHERE purchaseId = :purchaseId
        ORDER BY id
    """)
    fun observeSupplierPaymentAllocations(
        purchaseId: Long
    ): Flow<List<DealerSupplierPaymentAllocationEntity>>

    @Query("""
        SELECT *
        FROM dealer_business_collections
        WHERE customerId = :customerId
        ORDER BY collectedAt DESC, id DESC
    """)
    fun observeCollections(
        customerId: Long
    ): Flow<List<DealerCollectionEntity>>

    @Query("""
        SELECT *
        FROM dealer_business_collection_allocations
        WHERE saleId = :saleId
        ORDER BY id
    """)
    fun observeCollectionAllocations(
        saleId: Long
    ): Flow<List<DealerCollectionAllocationEntity>>

    @Query("""
        SELECT *
        FROM dealer_business_sales_returns
        WHERE saleId = :saleId
        ORDER BY returnedAt DESC, id DESC
    """)
    fun observeSalesReturns(
        saleId: Long
    ): Flow<List<DealerSalesReturnEntity>>

    @Query("""
        SELECT *
        FROM dealer_business_sales_return_allocations
        WHERE returnId = :returnId
        ORDER BY id
    """)
    fun observeSalesReturnAllocations(
        returnId: Long
    ): Flow<List<DealerSalesReturnAllocationEntity>>

    @Query("""
        SELECT *
        FROM dealer_business_purchase_returns
        WHERE purchaseId = :purchaseId
        ORDER BY returnedAt DESC, id DESC
    """)
    fun observePurchaseReturns(
        purchaseId: Long
    ): Flow<List<DealerPurchaseReturnEntity>>

    @Query("""
        SELECT *
        FROM dealer_business_companies
        WHERE id = :id
        LIMIT 1
    """)
    suspend fun getCompanyOnce(
        id: Long
    ): DealerCompanyEntity?

    @Query("""
        SELECT *
        FROM dealer_business_areas
        WHERE id = :id
        LIMIT 1
    """)
    suspend fun getAreaOnce(
        id: Long
    ): DealerAreaEntity?

    @Query("""
        SELECT *
        FROM dealer_business_customers
        WHERE id = :id
        LIMIT 1
    """)
    suspend fun getCustomerOnce(
        id: Long
    ): DealerCustomerEntity?

    @Query("""
        SELECT *
        FROM dealer_business_purchases
        WHERE id = :id
        LIMIT 1
    """)
    suspend fun getPurchaseOnce(
        id: Long
    ): DealerPurchaseEntity?

    @Query("""
        SELECT *
        FROM dealer_business_purchase_lines
        WHERE id = :id
        LIMIT 1
    """)
    suspend fun getPurchaseLineOnce(
        id: Long
    ): DealerPurchaseLineEntity?

    @Query("""
        SELECT *
        FROM dealer_business_sales
        WHERE id = :id
        LIMIT 1
    """)
    suspend fun getSaleOnce(
        id: Long
    ): DealerSaleEntity?

    @Query("""
        SELECT *
        FROM dealer_business_sale_lines
        WHERE id = :id
        LIMIT 1
    """)
    suspend fun getSaleLineOnce(
        id: Long
    ): DealerSaleLineEntity?

    @Query("""
        SELECT *
        FROM dealer_business_stock_allocations
        WHERE saleLineId = :saleLineId
        ORDER BY id
    """)
    suspend fun getStockAllocationsOnce(
        saleLineId: Long
    ): List<DealerStockAllocationEntity>

    @Query("""
        SELECT COALESCE(SUM(lineTotal), 0)
        FROM dealer_business_purchase_lines
        WHERE purchaseId = :purchaseId
    """)
    suspend fun getPurchaseTotal(
        purchaseId: Long
    ): Double

    @Query("""
        SELECT COALESCE(SUM(amount), 0)
        FROM dealer_business_supplier_payment_allocations
        WHERE purchaseId = :purchaseId
    """)
    suspend fun getPurchasePaid(
        purchaseId: Long
    ): Double

    @Query("""
        SELECT COALESCE(SUM(totalValue), 0)
        FROM dealer_business_purchase_returns
        WHERE purchaseId = :purchaseId
    """)
    suspend fun getPurchaseReturnTotal(
        purchaseId: Long
    ): Double

    @Query("""
        SELECT COALESCE(SUM(lineTotal), 0)
        FROM dealer_business_sale_lines
        WHERE saleId = :saleId
    """)
    suspend fun getSaleTotal(
        saleId: Long
    ): Double

    @Query("""
        SELECT COALESCE(SUM(amount), 0)
        FROM dealer_business_collection_allocations
        WHERE saleId = :saleId
    """)
    suspend fun getSaleCollected(
        saleId: Long
    ): Double

    @Query("""
        SELECT COALESCE(SUM(totalRefund), 0)
        FROM dealer_business_sales_returns
        WHERE saleId = :saleId
    """)
    suspend fun getSaleReturnTotal(
        saleId: Long
    ): Double

    @Query("""
        SELECT COALESCE(SUM(quantity), 0)
        FROM dealer_business_sales_returns
        WHERE saleLineId = :saleLineId
    """)
    suspend fun getReturnedSaleQuantity(
        saleLineId: Long
    ): Int

    @Query("""
        SELECT COALESCE(SUM(quantity), 0)
        FROM dealer_business_purchase_returns
        WHERE purchaseLineId = :purchaseLineId
    """)
    suspend fun getReturnedPurchaseQuantity(
        purchaseLineId: Long
    ): Int

    @Query("""
        SELECT *
        FROM dealer_business_sales
        WHERE customerId = :customerId
          AND workspace = :workspace
        ORDER BY soldAt ASC, id ASC
    """)
    suspend fun getSalesForCustomerOnce(
        customerId: Long,
        workspace: String
    ): List<DealerSaleEntity>

    @Query("""
        SELECT *
        FROM dealer_business_purchases
        WHERE companyId = :companyId
          AND workspace = :workspace
        ORDER BY purchasedAt ASC, id ASC
    """)
    suspend fun getPurchasesForCompanyOnce(
        companyId: Long,
        workspace: String
    ): List<DealerPurchaseEntity>

    @Query("""
        SELECT *
        FROM dealer_business_collections
        WHERE customerId = :customerId
          AND workspace = :workspace
        ORDER BY collectedAt ASC, id ASC
    """)
    suspend fun getCollectionsForCustomerOnce(
        customerId: Long,
        workspace: String
    ): List<DealerCollectionEntity>

    @Query("""
        SELECT *
        FROM dealer_business_supplier_payments
        WHERE companyId = :companyId
          AND workspace = :workspace
        ORDER BY paidAt ASC, id ASC
    """)
    suspend fun getSupplierPaymentsForCompanyOnce(
        companyId: Long,
        workspace: String
    ): List<DealerSupplierPaymentEntity>

    @Query("""
        SELECT COALESCE(SUM(amount), 0)
        FROM dealer_business_collection_allocations
        WHERE collectionId = :collectionId
    """)
    suspend fun getCollectionAllocated(
        collectionId: Long
    ): Double

    @Query("""
        SELECT COALESCE(SUM(amount), 0)
        FROM dealer_business_supplier_payment_allocations
        WHERE paymentId = :paymentId
    """)
    suspend fun getSupplierPaymentAllocated(
        paymentId: Long
    ): Double

    @Query("""
        SELECT *
        FROM dealer_business_collection_allocations
        WHERE collectionId = :collectionId
        ORDER BY id
    """)
    suspend fun getCollectionAllocationsForCollectionOnce(
        collectionId: Long
    ): List<DealerCollectionAllocationEntity>

    @Query("""
        DELETE FROM dealer_business_collection_allocations
        WHERE collectionId = :collectionId
    """)
    suspend fun deleteCollectionAllocationsByCollection(
        collectionId: Long
    )

    @Query("""
        SELECT *
        FROM dealer_business_supplier_payment_allocations
        WHERE paymentId = :paymentId
        ORDER BY id
    """)
    suspend fun getSupplierPaymentAllocationsForPaymentOnce(
        paymentId: Long
    ): List<DealerSupplierPaymentAllocationEntity>

    @Query("""
        DELETE FROM dealer_business_supplier_payment_allocations
        WHERE paymentId = :paymentId
    """)
    suspend fun deleteSupplierPaymentAllocationsByPayment(
        paymentId: Long
    )

    @Query("""
        SELECT COALESCE(SUM(ra.quantity), 0)
        FROM dealer_business_sales_return_allocations ra
        INNER JOIN dealer_business_sales_returns r
            ON r.id = ra.returnId
        WHERE r.saleLineId = :saleLineId
          AND ra.saleAllocationId = :saleAllocationId
    """)
    suspend fun getReturnedAllocationQuantity(
        saleLineId: Long,
        saleAllocationId: Long
    ): Int

    @Query("""
        SELECT *
        FROM dealer_business_collection_allocations
        WHERE saleId = :saleId
        ORDER BY id DESC
    """)
    suspend fun getCollectionAllocationsForSaleOnce(
        saleId: Long
    ): List<DealerCollectionAllocationEntity>

    @Query("""
        UPDATE dealer_business_collection_allocations
        SET amount = :amount
        WHERE id = :allocationId
    """)
    suspend fun updateCollectionAllocationAmount(
        allocationId: Long,
        amount: Double
    )

    @Query("""
        DELETE FROM dealer_business_collection_allocations
        WHERE id = :allocationId
    """)
    suspend fun deleteCollectionAllocationById(
        allocationId: Long
    )

    @Query("""
        SELECT *
        FROM dealer_business_supplier_payment_allocations
        WHERE purchaseId = :purchaseId
        ORDER BY id DESC
    """)
    suspend fun getSupplierPaymentAllocationsForPurchaseOnce(
        purchaseId: Long
    ): List<DealerSupplierPaymentAllocationEntity>

    @Query("""
        UPDATE dealer_business_supplier_payment_allocations
        SET amount = :amount
        WHERE id = :allocationId
    """)
    suspend fun updateSupplierPaymentAllocationAmount(
        allocationId: Long,
        amount: Double
    )

    @Query("""
        DELETE FROM dealer_business_supplier_payment_allocations
        WHERE id = :allocationId
    """)
    suspend fun deleteSupplierPaymentAllocationById(
        allocationId: Long
    )

    @Query("""
        UPDATE dealer_business_sales
        SET status = :status
        WHERE id = :saleId
    """)
    suspend fun updateSaleStatus(
        saleId: Long,
        status: String
    )

    @Query("""
        UPDATE dealer_business_purchases
        SET status = :status
        WHERE id = :purchaseId
    """)
    suspend fun updatePurchaseStatus(
        purchaseId: Long,
        status: String
    )
}
