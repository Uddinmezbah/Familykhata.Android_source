package com.familykhata.app.dealership

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.familykhata.app.data.ProductEntity
import com.familykhata.app.data.StockBatchEntity

@Entity(
    tableName = "dealership_suppliers",
    indices = [
        Index(value = ["workspace", "name"])
    ]
)
data class DealershipSupplierEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String = "",
    val contactPerson: String = "",
    val address: String = "",
    val note: String = "",
    val workspace: String = "SHOP",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "dealership_territories",
    indices = [
        Index(value = ["workspace", "name"])
    ]
)
data class DealershipTerritoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val code: String = "",
    val note: String = "",
    val workspace: String = "SHOP",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "dealership_dealers",
    foreignKeys = [
        ForeignKey(
            entity = DealershipTerritoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["territoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("territoryId"),
        Index(value = ["workspace", "name"])
    ]
)
data class DealershipDealerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val territoryId: Long? = null,
    val name: String,
    val dealerCode: String = "",
    val phone: String = "",
    val address: String = "",
    val creditLimit: Double = 0.0,
    val note: String = "",
    val workspace: String = "SHOP",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "dealership_product_policies",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["workspace", "productId"])
    ]
)
data class DealershipProductPolicyEntity(
    @PrimaryKey
    val productId: Long,
    val dealerPrice: Double = 0.0,
    val marginPercent: Double = 0.0,
    val note: String = "",
    val workspace: String = "SHOP",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "dealership_stock_receipts",
    foreignKeys = [
        ForeignKey(
            entity = DealershipSupplierEntity::class,
            parentColumns = ["id"],
            childColumns = ["supplierId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = StockBatchEntity::class,
            parentColumns = ["id"],
            childColumns = ["inventoryBatchId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("supplierId"),
        Index("productId"),
        Index("inventoryBatchId"),
        Index("receivedAt")
    ]
)
data class DealershipStockReceiptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val supplierId: Long? = null,
    val productId: Long? = null,
    val inventoryBatchId: Long? = null,
    val supplierNameSnapshot: String = "",
    val productNameSnapshot: String,
    val invoiceReference: String = "",
    val quantity: Int,
    val unitCost: Double,
    val receivedAt: Long = System.currentTimeMillis(),
    val note: String = "",
    val workspace: String = "SHOP",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "dealership_invoices",
    foreignKeys = [
        ForeignKey(
            entity = DealershipDealerEntity::class,
            parentColumns = ["id"],
            childColumns = ["dealerId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("dealerId"),
        Index(value = ["workspace", "status"]),
        Index("soldAt")
    ]
)
data class DealershipInvoiceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dealerId: Long? = null,
    val dealerNameSnapshot: String,
    val invoiceNo: String = "",
    val status: String = "OPEN",
    val soldAt: Long = System.currentTimeMillis(),
    val note: String = "",
    val workspace: String = "SHOP",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "dealership_invoice_lines",
    foreignKeys = [
        ForeignKey(
            entity = DealershipInvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("invoiceId"),
        Index("productId")
    ]
)
data class DealershipInvoiceLineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceId: Long,
    val productId: Long? = null,
    val productNameSnapshot: String,
    val quantity: Int,
    val unitPrice: Double,
    val lineTotal: Double,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "dealership_stock_allocations",
    foreignKeys = [
        ForeignKey(
            entity = DealershipInvoiceLineEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoiceLineId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = StockBatchEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceStockBatchId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("invoiceLineId"),
        Index("sourceStockBatchId")
    ]
)
data class DealershipStockAllocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceLineId: Long,
    val sourceStockBatchId: Long? = null,
    val sourceBatchNoSnapshot: String = "",
    val quantity: Int,
    val unitCost: Double,
    val totalCost: Double,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "dealership_payments",
    foreignKeys = [
        ForeignKey(
            entity = DealershipInvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("invoiceId"),
        Index("paidAt")
    ]
)
data class DealershipPaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceId: Long,
    val amount: Double,
    val paidAt: Long = System.currentTimeMillis(),
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "dealership_returns",
    foreignKeys = [
        ForeignKey(
            entity = DealershipInvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = DealershipInvoiceLineEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoiceLineId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("invoiceId"),
        Index("invoiceLineId"),
        Index("productId"),
        Index("returnedAt")
    ]
)
data class DealershipReturnEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceId: Long,
    val invoiceLineId: Long,
    val productId: Long? = null,
    val productNameSnapshot: String,
    val quantity: Int,
    val unitPrice: Double,
    val totalRefund: Double,
    val totalCost: Double,
    val returnType: String = "RESTOCK",
    val returnedAt: Long = System.currentTimeMillis(),
    val note: String = "",
    val workspace: String = "SHOP",
    val createdAt: Long = System.currentTimeMillis()
)


data class DealershipInvoiceSummary(
    val invoiceId: Long,
    val dealerId: Long?,
    val dealerName: String,
    val invoiceNo: String,
    val soldAt: Long,
    val status: String,
    val totalAmount: Double,
    val totalPaid: Double,
    val dueAmount: Double,
    val totalCost: Double,
    val grossProfit: Double
)
