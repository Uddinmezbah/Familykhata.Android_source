package com.familykhata.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index(
            value = ["sourceKey"],
            unique = true
        ),
        Index(
            value = ["financialAccountId"]
        )
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // INCOME or EXPENSE
    val amount: Double,
    val category: String,
    val note: String,
    @ColumnInfo(defaultValue = "'FAMILY'")
    val workspace: String = "FAMILY",
    @ColumnInfo(defaultValue = "''")
    val businessId: String = "",
    val sourceKey: String? = null,
    val financialAccountId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "baki_people")
data class BakiPersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val note: String = "",
    @ColumnInfo(defaultValue = "'FAMILY'") val workspace: String = "FAMILY",
    @ColumnInfo(defaultValue = "''") val businessId: String = "",
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
    tableName = "business_profiles",
    indices = [
        Index(value = ["name"])
    ]
)
data class BusinessProfileEntity(
    @PrimaryKey
    val businessId: String,
    val name: String,
    val businessType: String = "",
    val phone: String = "",
    val address: String = "",
    val logoPath: String = "",
    val isActive: Boolean = true,
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
    @ColumnInfo(defaultValue = "''") val businessId: String = "",
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
    @ColumnInfo(defaultValue = "''") val businessId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "digital_service_transactions",
    foreignKeys = [
        ForeignKey(
            entity = FinancialAccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceAccountId"],
            onDelete = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = FinancialAccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["destinationAccountId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(
            value = ["eventKey"],
            unique = true
        ),
        Index("sourceAccountId"),
        Index("destinationAccountId"),
        Index("serviceType"),
        Index(
            value = ["workspace", "createdAt"]
        )
    ]
)
data class DigitalServiceTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val eventKey: String,
    val serviceType: String,
    val sourceAccountId: Long,
    val destinationAccountId: Long,
    val serviceAmount: Double,
    val customerFee: Double = 0.0,
    val providerCharge: Double = 0.0,
    val customerPaid: Double = 0.0,
    val providerCost: Double = 0.0,
    val sourceAmount: Double,
    val destinationAmount: Double,
    val profit: Double,
    val note: String = "",
    val workspace: String = "SHOP",
    @ColumnInfo(defaultValue = "''") val businessId: String = "",
    val createdAt: Long =
        System.currentTimeMillis()
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
