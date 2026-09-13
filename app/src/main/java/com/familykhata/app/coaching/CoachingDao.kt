package com.familykhata.app.coaching

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CoachingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(item: CoachingStudentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(item: CoachingBatchEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEnrollment(item: CoachingEnrollmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharge(item: CoachingChargeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(item: CoachingPaymentEntity): Long

    @Query("SELECT * FROM coaching_students WHERE workspace = :workspace ORDER BY name COLLATE NOCASE")
    fun observeStudents(workspace: String): Flow<List<CoachingStudentEntity>>

    @Query("SELECT * FROM coaching_batches WHERE workspace = :workspace ORDER BY name COLLATE NOCASE")
    fun observeBatches(workspace: String): Flow<List<CoachingBatchEntity>>

    @Query("""
        SELECT
            e.id AS enrollmentId,
            s.id AS studentId,
            s.name AS studentName,
            s.phone AS phone,
            b.id AS batchId,
            b.name AS batchName,
            COALESCE(
                (SELECT SUM(c.amount)
                 FROM coaching_charges c
                 WHERE c.enrollmentId = e.id), 0
            ) AS totalCharge,
            COALESCE(
                (SELECT SUM(p.amount)
                 FROM coaching_payments p
                 WHERE p.enrollmentId = e.id), 0
            ) AS totalPaid,
            COALESCE(
                (SELECT SUM(c.amount)
                 FROM coaching_charges c
                 WHERE c.enrollmentId = e.id), 0
            ) -
            COALESCE(
                (SELECT SUM(p.amount)
                 FROM coaching_payments p
                 WHERE p.enrollmentId = e.id), 0
            ) AS dueAmount
        FROM coaching_enrollments e
        INNER JOIN coaching_students s ON s.id = e.studentId
        INNER JOIN coaching_batches b ON b.id = e.batchId
        WHERE s.workspace = :workspace
          AND e.active = 1
        ORDER BY s.name COLLATE NOCASE
    """)
    fun observeEnrollmentSummaries(
        workspace: String
    ): Flow<List<CoachingEnrollmentSummary>>

    @Query("SELECT * FROM coaching_charges WHERE enrollmentId = :enrollmentId ORDER BY createdAt DESC")
    fun observeCharges(enrollmentId: Long): Flow<List<CoachingChargeEntity>>

    @Query("SELECT * FROM coaching_payments WHERE enrollmentId = :enrollmentId ORDER BY paidAt DESC")
    fun observePayments(enrollmentId: Long): Flow<List<CoachingPaymentEntity>>

    @Delete
    suspend fun deleteCharge(item: CoachingChargeEntity)

    @Delete
    suspend fun deletePayment(item: CoachingPaymentEntity)

    @Query("SELECT * FROM coaching_students ORDER BY id")
    suspend fun getAllStudents(): List<CoachingStudentEntity>

    @Query("SELECT * FROM coaching_batches ORDER BY id")
    suspend fun getAllBatches(): List<CoachingBatchEntity>

    @Query("SELECT * FROM coaching_enrollments ORDER BY id")
    suspend fun getAllEnrollments(): List<CoachingEnrollmentEntity>

    @Query("SELECT * FROM coaching_charges ORDER BY id")
    suspend fun getAllCharges(): List<CoachingChargeEntity>

    @Query("SELECT * FROM coaching_payments ORDER BY id")
    suspend fun getAllPayments(): List<CoachingPaymentEntity>
}
