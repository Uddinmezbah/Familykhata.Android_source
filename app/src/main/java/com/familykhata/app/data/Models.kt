package com.familykhata.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // INCOME or EXPENSE
    val amount: Double,
    val category: String,
    val note: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "baki_people")
data class BakiPersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "baki_entries",
    foreignKeys = [
        ForeignKey(
            entity = BakiPersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("personId")]
)
data class BakiEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long,
    val action: String, // GAVE, RECEIVED_BACK, TOOK, PAID_BACK
    val amount: Double,
    val balanceDelta: Double,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class DashboardTotals(
    val income: Double,
    val expense: Double
)

data class BakiPersonSummary(
    val id: Long,
    val name: String,
    val phone: String,
    val balance: Double
)
