package com.familykhata.app.agency

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AgencyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(
        item: AgencyClientEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(
        item: AgencyProjectEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCharge(
        item: AgencyChargeEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(
        item: AgencyPaymentEntity
    ): Long

    @Query("""
        SELECT *
        FROM agency_clients
        WHERE workspace = :workspace
        ORDER BY name COLLATE NOCASE
    """)
    fun observeClients(
        workspace: String
    ): Flow<List<AgencyClientEntity>>

    @Query("""
        SELECT
            p.id AS projectId,
            c.id AS clientId,
            c.name AS clientName,
            c.phone AS phone,
            c.company AS company,
            p.title AS title,
            p.serviceType AS serviceType,

            COALESCE(
                (
                    SELECT SUM(ac.amount)
                    FROM agency_charges ac
                    WHERE ac.projectId = p.id
                ),
                0
            ) AS totalPrice,

            COALESCE(
                (
                    SELECT SUM(ap.amount)
                    FROM agency_payments ap
                    WHERE ap.projectId = p.id
                ),
                0
            ) AS totalPaid,

            COALESCE(
                (
                    SELECT SUM(ac.amount)
                    FROM agency_charges ac
                    WHERE ac.projectId = p.id
                ),
                0
            )
            -
            COALESCE(
                (
                    SELECT SUM(ap.amount)
                    FROM agency_payments ap
                    WHERE ap.projectId = p.id
                ),
                0
            ) AS dueAmount,

            p.dueDate AS dueDate,
            p.status AS status

        FROM agency_projects p

        INNER JOIN agency_clients c
            ON c.id = p.clientId

        WHERE c.workspace = :workspace

        ORDER BY
            CASE
                WHEN p.status = 'ACTIVE' THEN 0
                WHEN p.status = 'COMPLETED' THEN 1
                ELSE 2
            END,
            p.createdAt DESC
    """)
    fun observeProjectSummaries(
        workspace: String
    ): Flow<List<AgencyProjectSummary>>

    @Query("""
        SELECT *
        FROM agency_projects
        WHERE clientId = :clientId
        ORDER BY createdAt DESC
    """)
    fun observeProjectsForClient(
        clientId: Long
    ): Flow<List<AgencyProjectEntity>>

    @Query("""
        SELECT *
        FROM agency_charges
        WHERE projectId = :projectId
        ORDER BY createdAt DESC, id DESC
    """)
    fun observeCharges(
        projectId: Long
    ): Flow<List<AgencyChargeEntity>>

    @Query("""
        SELECT *
        FROM agency_payments
        WHERE projectId = :projectId
        ORDER BY paidAt DESC
    """)
    fun observePayments(
        projectId: Long
    ): Flow<List<AgencyPaymentEntity>>

    @Query("""
        SELECT COUNT(*)
        FROM agency_charges
        WHERE projectId = :projectId
          AND chargeType = :chargeType
          AND periodKey = :periodKey
    """)
    suspend fun countCharge(
        projectId: Long,
        chargeType: String,
        periodKey: String
    ): Int

    @Query("""
        UPDATE agency_projects
        SET status = :status
        WHERE id = :projectId
    """)
    suspend fun updateProjectStatus(
        projectId: Long,
        status: String
    )

    @Update
    suspend fun updateClient(
        item: AgencyClientEntity
    )

    @Delete
    suspend fun deleteClient(
        item: AgencyClientEntity
    )

    @Delete
    suspend fun deleteProject(
        item: AgencyProjectEntity
    )

    @Delete
    suspend fun deleteCharge(
        item: AgencyChargeEntity
    )

    @Delete
    suspend fun deletePayment(
        item: AgencyPaymentEntity
    )

    @Query("SELECT * FROM agency_clients ORDER BY id")
    suspend fun getAllClients():
        List<AgencyClientEntity>

    @Query("SELECT * FROM agency_projects ORDER BY id")
    suspend fun getAllProjects():
        List<AgencyProjectEntity>

    @Query("SELECT * FROM agency_charges ORDER BY id")
    suspend fun getAllCharges():
        List<AgencyChargeEntity>

    @Query("SELECT * FROM agency_payments ORDER BY id")
    suspend fun getAllPayments():
        List<AgencyPaymentEntity>
}
