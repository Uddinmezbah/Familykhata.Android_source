package com.familykhata.app.coaching

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "coaching_students",
    indices = [Index(value = ["workspace", "name"])]
)
data class CoachingStudentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val guardianName: String = "",
    val address: String = "",
    val workspace: String = "SHOP",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "coaching_batches",
    indices = [Index(value = ["workspace", "name"])]
)
data class CoachingBatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val admissionFee: Double = 0.0,
    val monthlyFee: Double = 0.0,
    val note: String = "",
    val workspace: String = "SHOP",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "coaching_enrollments",
    foreignKeys = [
        ForeignKey(
            entity = CoachingStudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CoachingBatchEntity::class,
            parentColumns = ["id"],
            childColumns = ["batchId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("studentId"),
        Index("batchId"),
        Index(value = ["studentId", "batchId"], unique = true)
    ]
)
data class CoachingEnrollmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val batchId: Long,
    val joinedAt: Long = System.currentTimeMillis(),
    val active: Boolean = true
)

@Entity(
    tableName = "coaching_charges",
    foreignKeys = [
        ForeignKey(
            entity = CoachingEnrollmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["enrollmentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("enrollmentId"), Index("dueDate")]
)
data class CoachingChargeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val enrollmentId: Long,
    val feeType: String,
    val periodKey: String = "",
    val amount: Double,
    val dueDate: Long? = null,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "coaching_payments",
    foreignKeys = [
        ForeignKey(
            entity = CoachingEnrollmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["enrollmentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("enrollmentId"), Index("paidAt")]
)
data class CoachingPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val enrollmentId: Long,
    val amount: Double,
    val paidAt: Long = System.currentTimeMillis(),
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class CoachingEnrollmentSummary(
    val enrollmentId: Long,
    val studentId: Long,
    val studentName: String,
    val phone: String,
    val batchId: Long,
    val batchName: String,
    val totalCharge: Double,
    val totalPaid: Double,
    val dueAmount: Double
)
