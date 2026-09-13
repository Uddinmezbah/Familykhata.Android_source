package com.familykhata.app.dealership

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DealershipDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(
        item: DealershipSupplierEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTerritory(
        item: DealershipTerritoryEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDealer(
        item: DealershipDealerEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProductPolicy(
        item: DealershipProductPolicyEntity
    )

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStockReceipt(
        item: DealershipStockReceiptEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertInvoice(
        item: DealershipInvoiceEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertInvoiceLine(
        item: DealershipInvoiceLineEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStockAllocation(
        item: DealershipStockAllocationEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(
        item: DealershipPaymentEntity
    ): Long

    @Query("""
        SELECT *
        FROM dealership_suppliers
        WHERE workspace = :workspace
        ORDER BY name COLLATE NOCASE
    """)
    fun observeSuppliers(
        workspace: String
    ): Flow<List<DealershipSupplierEntity>>

    @Query("""
        SELECT *
        FROM dealership_territories
        WHERE workspace = :workspace
        ORDER BY name COLLATE NOCASE
    """)
    fun observeTerritories(
        workspace: String
    ): Flow<List<DealershipTerritoryEntity>>

    @Query("""
        SELECT *
        FROM dealership_dealers
        WHERE workspace = :workspace
        ORDER BY name COLLATE NOCASE
    """)
    fun observeDealers(
        workspace: String
    ): Flow<List<DealershipDealerEntity>>

    @Query("""
        SELECT *
        FROM dealership_product_policies
        WHERE workspace = :workspace
        ORDER BY productId
    """)
    fun observeProductPolicies(
        workspace: String
    ): Flow<List<DealershipProductPolicyEntity>>

    @Query("""
        SELECT *
        FROM dealership_payments
        WHERE invoiceId = :invoiceId
        ORDER BY paidAt DESC
    """)
    fun observePayments(
        invoiceId: Long
    ): Flow<List<DealershipPaymentEntity>>

    @Query("""
        SELECT *
        FROM dealership_stock_receipts
        WHERE workspace = :workspace
        ORDER BY receivedAt DESC, id DESC
    """)
    fun observeStockReceipts(
        workspace: String
    ): Flow<List<DealershipStockReceiptEntity>>

    @Query("""
        SELECT *
        FROM dealership_invoice_lines
        WHERE invoiceId = :invoiceId
        ORDER BY id
    """)
    fun observeInvoiceLines(
        invoiceId: Long
    ): Flow<List<DealershipInvoiceLineEntity>>

    @Query("""
        SELECT *
        FROM dealership_suppliers
        WHERE id = :supplierId
        LIMIT 1
    """)
    suspend fun getSupplierOnce(
        supplierId: Long
    ): DealershipSupplierEntity?

    @Query("""
        SELECT *
        FROM dealership_dealers
        WHERE id = :dealerId
        LIMIT 1
    """)
    suspend fun getDealerOnce(
        dealerId: Long
    ): DealershipDealerEntity?

    @Query("""
        SELECT *
        FROM dealership_invoices
        WHERE id = :invoiceId
        LIMIT 1
    """)
    suspend fun getInvoiceOnce(
        invoiceId: Long
    ): DealershipInvoiceEntity?

    @Query("""
        SELECT COALESCE(SUM(lineTotal), 0)
        FROM dealership_invoice_lines
        WHERE invoiceId = :invoiceId
    """)
    suspend fun getInvoiceTotal(
        invoiceId: Long
    ): Double

    @Query("""
        SELECT COALESCE(SUM(amount), 0)
        FROM dealership_payments
        WHERE invoiceId = :invoiceId
    """)
    suspend fun getInvoicePaid(
        invoiceId: Long
    ): Double

    @Query("""
        SELECT COALESCE(
            SUM(
                COALESCE(
                    (
                        SELECT SUM(l.lineTotal)
                        FROM dealership_invoice_lines l
                        WHERE l.invoiceId = i.id
                    ),
                    0
                )
                -
                COALESCE(
                    (
                        SELECT SUM(p.amount)
                        FROM dealership_payments p
                        WHERE p.invoiceId = i.id
                    ),
                    0
                )
            ),
            0
        )
        FROM dealership_invoices i
        WHERE i.dealerId = :dealerId
          AND i.workspace = :workspace
          AND i.status != 'CANCELLED'
    """)
    suspend fun getDealerOutstanding(
        dealerId: Long,
        workspace: String
    ): Double

    @Query("""
        SELECT
            i.id AS invoiceId,
            i.dealerId AS dealerId,
            i.dealerNameSnapshot AS dealerName,
            i.invoiceNo AS invoiceNo,
            i.soldAt AS soldAt,
            i.status AS status,

            COALESCE(
                (
                    SELECT SUM(l.lineTotal)
                    FROM dealership_invoice_lines l
                    WHERE l.invoiceId = i.id
                ),
                0
            ) AS totalAmount,

            COALESCE(
                (
                    SELECT SUM(p.amount)
                    FROM dealership_payments p
                    WHERE p.invoiceId = i.id
                ),
                0
            ) AS totalPaid,

            COALESCE(
                (
                    SELECT SUM(l.lineTotal)
                    FROM dealership_invoice_lines l
                    WHERE l.invoiceId = i.id
                ),
                0
            )
            -
            COALESCE(
                (
                    SELECT SUM(p.amount)
                    FROM dealership_payments p
                    WHERE p.invoiceId = i.id
                ),
                0
            ) AS dueAmount,

            COALESCE(
                (
                    SELECT SUM(a.totalCost)
                    FROM dealership_stock_allocations a
                    INNER JOIN dealership_invoice_lines l
                        ON l.id = a.invoiceLineId
                    WHERE l.invoiceId = i.id
                ),
                0
            ) AS totalCost,

            COALESCE(
                (
                    SELECT SUM(l.lineTotal)
                    FROM dealership_invoice_lines l
                    WHERE l.invoiceId = i.id
                ),
                0
            )
            -
            COALESCE(
                (
                    SELECT SUM(a.totalCost)
                    FROM dealership_stock_allocations a
                    INNER JOIN dealership_invoice_lines l
                        ON l.id = a.invoiceLineId
                    WHERE l.invoiceId = i.id
                ),
                0
            ) AS grossProfit

        FROM dealership_invoices i
        WHERE i.workspace = :workspace
        ORDER BY i.soldAt DESC, i.id DESC
    """)
    fun observeInvoiceSummaries(
        workspace: String
    ): Flow<List<DealershipInvoiceSummary>>

    @Query("""
        UPDATE dealership_invoices
        SET status = :status
        WHERE id = :invoiceId
    """)
    suspend fun updateInvoiceStatus(
        invoiceId: Long,
        status: String
    )

    @Query("SELECT * FROM dealership_suppliers ORDER BY id")
    suspend fun getAllSuppliers():
        List<DealershipSupplierEntity>

    @Query("SELECT * FROM dealership_territories ORDER BY id")
    suspend fun getAllTerritories():
        List<DealershipTerritoryEntity>

    @Query("SELECT * FROM dealership_dealers ORDER BY id")
    suspend fun getAllDealers():
        List<DealershipDealerEntity>

    @Query("SELECT * FROM dealership_product_policies ORDER BY productId")
    suspend fun getAllProductPolicies():
        List<DealershipProductPolicyEntity>

    @Query("SELECT * FROM dealership_stock_receipts ORDER BY id")
    suspend fun getAllStockReceipts():
        List<DealershipStockReceiptEntity>

    @Query("SELECT * FROM dealership_invoices ORDER BY id")
    suspend fun getAllInvoices():
        List<DealershipInvoiceEntity>

    @Query("SELECT * FROM dealership_invoice_lines ORDER BY id")
    suspend fun getAllInvoiceLines():
        List<DealershipInvoiceLineEntity>

    @Query("SELECT * FROM dealership_stock_allocations ORDER BY id")
    suspend fun getAllStockAllocations():
        List<DealershipStockAllocationEntity>

    @Query("SELECT * FROM dealership_payments ORDER BY id")
    suspend fun getAllPayments():
        List<DealershipPaymentEntity>
}
