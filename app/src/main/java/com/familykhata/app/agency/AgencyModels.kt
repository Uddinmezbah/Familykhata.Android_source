package com.familykhata.app.agency

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "agency_clients",
    indices = [
        Index(value = ["workspace", "name"])
    ]
)
data class AgencyClientEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val company: String = "",
    val note: String = "",
    val workspace: String = "SHOP",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "agency_projects",
    foreignKeys = [
        ForeignKey(
            entity = AgencyClientEntity::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("clientId"),
        Index("status"),
        Index("dueDate")
    ]
)
data class AgencyProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val clientId: Long,
    val title: String,
    val serviceType: String = "",
    val totalPrice: Double = 0.0,
    val startDate: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,
    val status: String = "ACTIVE",
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "agency_payments",
    foreignKeys = [
        ForeignKey(
            entity = AgencyProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("projectId"),
        Index("paidAt")
    ]
)
data class AgencyPaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val amount: Double,
    val paidAt: Long = System.currentTimeMillis(),
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class AgencyProjectSummary(
    val projectId: Long,
    val clientId: Long,
    val clientName: String,
    val phone: String,
    val company: String,
    val title: String,
    val serviceType: String,
    val totalPrice: Double,
    val totalPaid: Double,
    val dueAmount: Double,
    val dueDate: Long?,
    val status: String
)
