package com.familykhata.app.servicejob

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceJobDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(
        item: ServiceCustomerEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(
        item: ServiceJobEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharge(
        item: ServiceChargeEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(
        item: ServicePaymentEntity
    ): Long

    @Query("""
        SELECT *
        FROM service_customers
        WHERE workspace = :workspace
        ORDER BY name COLLATE NOCASE
    """)
    fun observeCustomers(
        workspace: String
    ): Flow<List<ServiceCustomerEntity>>

    @Query("""
        SELECT
            j.id AS jobId,
            j.customerId AS customerId,
            c.name AS customerName,
            c.phone AS phone,
            j.title AS title,
            j.serviceType AS serviceType,
            j.itemName AS itemName,
            j.serialOrReference AS serialOrReference,
            j.receivedAt AS receivedAt,
            j.dueAt AS dueAt,
            j.status AS status,
            COALESCE(
                (
                    SELECT SUM(sc.amount)
                    FROM service_charges sc
                    WHERE sc.jobId = j.id
                ),
                0
            ) AS totalCharge,
            COALESCE(
                (
                    SELECT SUM(sp.amount)
                    FROM service_payments sp
                    WHERE sp.jobId = j.id
                ),
                0
            ) AS totalPaid,
            COALESCE(
                (
                    SELECT SUM(sc.amount)
                    FROM service_charges sc
                    WHERE sc.jobId = j.id
                ),
                0
            )
            -
            COALESCE(
                (
                    SELECT SUM(sp.amount)
                    FROM service_payments sp
                    WHERE sp.jobId = j.id
                ),
                0
            ) AS dueAmount
        FROM service_jobs j
        LEFT JOIN service_customers c
            ON c.id = j.customerId
        WHERE j.workspace = :workspace
        ORDER BY
            CASE
                WHEN j.status = 'OPEN' THEN 0
                WHEN j.status = 'IN_PROGRESS' THEN 1
                WHEN j.status = 'READY' THEN 2
                WHEN j.status = 'COMPLETED' THEN 3
                ELSE 4
            END,
            j.createdAt DESC
    """)
    fun observeJobSummaries(
        workspace: String
    ): Flow<List<ServiceJobSummary>>

    @Query("""
        SELECT *
        FROM service_charges
        WHERE jobId = :jobId
        ORDER BY createdAt DESC
    """)
    fun observeCharges(
        jobId: Long
    ): Flow<List<ServiceChargeEntity>>

    @Query("""
        SELECT *
        FROM service_payments
        WHERE jobId = :jobId
        ORDER BY paidAt DESC
    """)
    fun observePayments(
        jobId: Long
    ): Flow<List<ServicePaymentEntity>>

    @Query("""
        UPDATE service_jobs
        SET status = :status
        WHERE id = :jobId
    """)
    suspend fun updateJobStatus(
        jobId: Long,
        status: String
    )

    @Delete
    suspend fun deleteCharge(
        item: ServiceChargeEntity
    )

    @Delete
    suspend fun deletePayment(
        item: ServicePaymentEntity
    )

    @Query("SELECT * FROM service_customers ORDER BY id")
    suspend fun getAllCustomers():
        List<ServiceCustomerEntity>

    @Query("SELECT * FROM service_jobs ORDER BY id")
    suspend fun getAllJobs():
        List<ServiceJobEntity>

    @Query("SELECT * FROM service_charges ORDER BY id")
    suspend fun getAllCharges():
        List<ServiceChargeEntity>

    @Query("SELECT * FROM service_payments ORDER BY id")
    suspend fun getAllPayments():
        List<ServicePaymentEntity>
}
