package com.familykhata.app.data

import androidx.room.ColumnInfo
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
    @ColumnInfo(defaultValue = "'FAMILY'") val workspace: String = "FAMILY",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "baki_people")
data class BakiPersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val note: String = "",
    @ColumnInfo(defaultValue = "'FAMILY'") val workspace: String = "FAMILY",
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
    indices = [
        Index("personId"),
        Index(
            value = ["sourceKey"],
            unique = true
        )
    ]
)
data class BakiEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long,
    val action: String, // GAVE, RECEIVED_BACK, TOOK, PAID_BACK
    val amount: Double,
    val balanceDelta: Double,
    val note: String = "",
    val dueAt: Long? = null,
    val sourceKey: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "financial_accounts",
    indices = [
        Index(value = ["workspace", "name"]),
        Index(value = ["workspace", "type"])
    ]
)
data class FinancialAccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String,
    val provider: String = "",
    val openingBalance: Double = 0.0,
    val workspace: String = "SHOP",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "financial_account_entries",
    foreignKeys = [
        ForeignKey(
            entity = FinancialAccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("accountId"),
        Index(value = ["workspace", "createdAt"]),
        Index("transferGroupId"),
        Index(value = ["sourceKey"], unique = true)
    ]
)
data class FinancialAccountEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val accountId: Long,
    val entryType: String,
    val amount: Double,
    val balanceDelta: Double,
    val relatedAccountId: Long? = null,
    val transferGroupId: String? = null,
    val sourceKey: String? = null,
    val note: String = "",
    val workspace: String = "SHOP",
    val createdAt: Long = System.currentTimeMillis()
)

data class FinancialAccountSummary(
    val id: Long,
    val name: String,
    val type: String,
    val provider: String,
    val openingBalance: Double,
    val balance: Double,
    val workspace: String,
    val isActive: Boolean
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

data class DueReceivableItem(
    val entryId: Long,
    val personId: Long,
    val personName: String,
    val phone: String,
    val originalAmount: Double,
    val remainingAmount: Double,
    val dueAt: Long,
    val note: String
)
