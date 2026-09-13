package com.familykhata.app.servicejob

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "service_customers",
    indices = [
        Index(value = ["workspace", "name"])
    ]
)
data class ServiceCustomerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val note: String = "",
    val workspace: String = "SHOP",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "service_jobs",
    foreignKeys = [
        ForeignKey(
            entity = ServiceCustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("customerId"),
        Index("workspace"),
        Index("status"),
        Index("dueAt")
    ]
)
data class ServiceJobEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerId: Long? = null,
    val title: String,
    val serviceType: String = "",
    val itemName: String = "",
    val serialOrReference: String = "",
    val receivedAt: Long = System.currentTimeMillis(),
    val dueAt: Long? = null,
    val status: String = "OPEN",
    val note: String = "",
    val workspace: String = "SHOP",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "service_charges",
    foreignKeys = [
        ForeignKey(
            entity = ServiceJobEntity::class,
            parentColumns = ["id"],
            childColumns = ["jobId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("jobId"),
        Index("createdAt")
    ]
)
data class ServiceChargeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val jobId: Long,
    val chargeType: String = "SERVICE",
    val amount: Double,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "service_payments",
    foreignKeys = [
        ForeignKey(
            entity = ServiceJobEntity::class,
            parentColumns = ["id"],
            childColumns = ["jobId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("jobId"),
        Index("paidAt")
    ]
)
data class ServicePaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val jobId: Long,
    val amount: Double,
    val paidAt: Long = System.currentTimeMillis(),
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class ServiceJobSummary(
    val jobId: Long,
    val customerId: Long?,
    val customerName: String?,
    val phone: String?,
    val title: String,
    val serviceType: String,
    val itemName: String,
    val serialOrReference: String,
    val receivedAt: Long,
    val dueAt: Long?,
    val status: String,
    val totalCharge: Double,
    val totalPaid: Double,
    val dueAmount: Double
)
