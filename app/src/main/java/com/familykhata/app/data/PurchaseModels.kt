package com.familykhata.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "purchase_suppliers",
    indices = [
        Index(
            value = [
                "workspace",
                "businessKey",
                "name"
            ]
        )
    ]
)
data class PurchaseSupplierEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val note: String = "",
    val workspace: String = "SHOP",
    val businessKey: String = "legacy",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "purchase_bills",
    foreignKeys = [
        ForeignKey(
            entity = PurchaseSupplierEntity::class,
            parentColumns = ["id"],
            childColumns = ["supplierId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index("supplierId"),
        Index(
            value = [
                "workspace",
                "businessKey",
                "purchasedAt"
            ]
        ),
        Index(
            value = [
                "workspace",
                "businessKey",
                "purchaseNo"
            ],
            unique = true
        )
    ]
)
data class PurchaseBillEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val purchaseNo: String,
    val supplierId: Long,
    val subtotal: Double,
    val discount: Double = 0.0,
    val total: Double,
    val status: String = "ACTIVE",
    val note: String = "",
    val workspace: String = "SHOP",
    val businessKey: String = "legacy",
    val purchasedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "purchase_bill_lines",
    foreignKeys = [
        ForeignKey(
            entity = PurchaseBillEntity::class,
            parentColumns = ["id"],
            childColumns = ["billId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("billId"),
        Index("productId"),
        Index("stockBatchId")
    ]
)
data class PurchaseBillLineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val billId: Long,
    val productId: Long,
    val stockBatchId: Long? = null,
    val productNameSnapshot: String,
    val skuSnapshot: String = "",
    val unitSnapshot: String = "pcs",
    val unitFactor: Int = 1,
    val quantity: Int,
    val baseQuantity: Int,
    val unitCost: Double,
    val lineTotal: Double,
    val batchNo: String = "",
    val expiryDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "purchase_payments",
    foreignKeys = [
        ForeignKey(
            entity = PurchaseBillEntity::class,
            parentColumns = ["id"],
            childColumns = ["billId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("billId"),
        Index("financialAccountId"),
        Index(
            value = ["eventKey"],
            unique = true
        )
    ]
)
data class PurchasePaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val eventKey: String,
    val billId: Long,
    val financialAccountId: Long,
    val amount: Double,
    val paymentMethod: String,
    val note: String = "",
    val paidAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "purchase_returns",
    foreignKeys = [
        ForeignKey(
            entity = PurchaseBillEntity::class,
            parentColumns = ["id"],
            childColumns = ["billId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PurchaseBillLineEntity::class,
            parentColumns = ["id"],
            childColumns = ["purchaseLineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("billId"),
        Index("purchaseLineId"),
        Index(
            value = ["eventKey"],
            unique = true
        )
    ]
)
data class PurchaseReturnEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val eventKey: String,
    val billId: Long,
    val purchaseLineId: Long,
    val productId: Long,
    val stockBatchId: Long? = null,
    val baseQuantity: Int,
    val amount: Double,
    val note: String = "",
    val returnedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

data class PurchaseSupplierSummary(
    val id: Long,
    val name: String,
    val phone: String,
    val address: String,
    val note: String,
    val isActive: Boolean,
    val purchaseCount: Int,
    val totalPurchase: Double,
    val paid: Double,
    val due: Double
)

data class PurchaseBillSummary(
    val id: Long,
    val purchaseNo: String,
    val supplierId: Long,
    val supplierName: String,
    val subtotal: Double,
    val discount: Double,
    val total: Double,
    val paid: Double,
    val due: Double,
    val status: String,
    val note: String,
    val purchasedAt: Long
)
