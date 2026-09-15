package com.familykhata.app.dealerbusiness

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.familykhata.app.data.ProductEntity
import com.familykhata.app.data.StockBatchEntity

@Entity(
    tableName = "dealer_business_companies",
    indices = [
        Index(value = ["workspace", "name"])
    ]
)
data class DealerCompanyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val code: String = "",
    val phone: String = "",
    val contactPerson: String = "",
    val address: String = "",
    val note: String = "",
    val workspace: String = "SHOP",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "dealer_business_areas",
    indices = [
        Index(value = ["workspace", "name"])
    ]
)
data class DealerAreaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val code: String = "",
    val note: String = "",
    val workspace: String = "SHOP",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "dealer_business_customers",
    foreignKeys = [
        ForeignKey(
            entity = DealerAreaEntity::class,
            parentColumns = ["id"],
            childColumns = ["areaId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("areaId"),
        Index(value = ["workspace", "name"]),
        Index(value = ["workspace", "phone"]),
        Index(value = ["workspace", "customerCode"])
    ]
)
data class DealerCustomerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val areaId: Long? = null,
    val name: String,
    val customerCode: String = "",
    val phone: String = "",
    val address: String = "",
    val creditLimit: Double = 0.0,
    val note: String = "",
    val workspace: String = "SHOP",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "dealer_business_purchases",
    foreignKeys = [
        ForeignKey(
            entity = DealerCompanyEntity::class,
            parentColumns = ["id"],
            childColumns = ["companyId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("companyId"),
        Index(value = ["workspace", "status"]),
        Index("purchasedAt")
    ]
)
data class DealerPurchaseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val companyId: Long? = null,
    val companyNameSnapshot: String = "",
    val invoiceNo: String = "",
    val status: String = "OPEN",
    val purchasedAt: Long = System.currentTimeMillis(),
    val note: String = "",
    val workspace: String = "SHOP",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "dealer_business_purchase_lines",
    foreignKeys = [
        ForeignKey(
            entity = DealerPurchaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["purchaseId"],
            onDelete = ForeignKey.CASCADE
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
        Index("purchaseId"),
        Index("productId"),
        Index("inventoryBatchId")
    ]
)
data class DealerPurchaseLineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val purchaseId: Long,
    val productId: Long? = null,
    val inventoryBatchId: Long? = null,
    val productNameSnapshot: String,
    val batchNoSnapshot: String = "",
    val quantity: Int,
    val unitCost: Double,
    val lineTotal: Double
)

@Entity(
    tableName = "dealer_business_supplier_payments",
    foreignKeys = [
        ForeignKey(
            entity = DealerCompanyEntity::class,
            parentColumns = ["id"],
            childColumns = ["companyId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("companyId"),
        Index(value = ["workspace", "paidAt"])
    ]
)
data class DealerSupplierPaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val companyId: Long? = null,
    val companyNameSnapshot: String = "",
    val amount: Double,
    val paidAt: Long = System.currentTimeMillis(),
    val note: String = "",
    val workspace: String = "SHOP",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "dealer_business_supplier_payment_allocations",
    foreignKeys = [
        ForeignKey(
            entity = DealerSupplierPaymentEntity::class,
            parentColumns = ["id"],
            childColumns = ["paymentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = DealerPurchaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["purchaseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("paymentId"),
        Index("purchaseId")
    ]
)
data class DealerSupplierPaymentAllocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val paymentId: Long,
    val purchaseId: Long,
    val amount: Double
)

@Entity(
    tableName = "dealer_business_sales",
    foreignKeys = [
        ForeignKey(
            entity = DealerCustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("customerId"),
        Index(value = ["workspace", "status"]),
        Index("soldAt")
    ]
)
data class DealerSaleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerId: Long? = null,
    val customerNameSnapshot: String = "",
    val invoiceNo: String = "",
    val status: String = "OPEN",
    val soldAt: Long = System.currentTimeMillis(),
    val note: String = "",
    val workspace: String = "SHOP",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "dealer_business_sale_lines",
    foreignKeys = [
        ForeignKey(
            entity = DealerSaleEntity::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
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
        Index("saleId"),
        Index("productId")
    ]
)
data class DealerSaleLineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val saleId: Long,
    val productId: Long? = null,
    val productNameSnapshot: String,
    val quantity: Int,
    val unitPrice: Double,
    val lineTotal: Double
)

@Entity(
    tableName = "dealer_business_stock_allocations",
    foreignKeys = [
        ForeignKey(
            entity = DealerSaleLineEntity::class,
            parentColumns = ["id"],
            childColumns = ["saleLineId"],
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
        Index("saleLineId"),
        Index("sourceStockBatchId")
    ]
)
data class DealerStockAllocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val saleLineId: Long,
    val sourceStockBatchId: Long? = null,
    val batchNoSnapshot: String = "",
    val quantity: Int,
    val unitCost: Double,
    val totalCost: Double
)

@Entity(
    tableName = "dealer_business_collections",
    foreignKeys = [
        ForeignKey(
            entity = DealerCustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("customerId"),
        Index(value = ["workspace", "collectedAt"])
    ]
)
data class DealerCollectionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerId: Long? = null,
    val customerNameSnapshot: String = "",
    val amount: Double,
    val collectedAt: Long = System.currentTimeMillis(),
    val note: String = "",
    val workspace: String = "SHOP",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "dealer_business_collection_allocations",
    foreignKeys = [
        ForeignKey(
            entity = DealerCollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = DealerSaleEntity::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("collectionId"),
        Index("saleId")
    ]
)
data class DealerCollectionAllocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val collectionId: Long,
    val saleId: Long,
    val amount: Double
)

@Entity(
    tableName = "dealer_business_sales_returns",
    foreignKeys = [
        ForeignKey(
            entity = DealerSaleEntity::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = DealerSaleLineEntity::class,
            parentColumns = ["id"],
            childColumns = ["saleLineId"],
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
        Index("saleId"),
        Index("saleLineId"),
        Index("productId"),
        Index("returnedAt")
    ]
)
data class DealerSalesReturnEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val saleId: Long,
    val saleLineId: Long,
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

@Entity(
    tableName = "dealer_business_sales_return_allocations",
    foreignKeys = [
        ForeignKey(
            entity = DealerSalesReturnEntity::class,
            parentColumns = ["id"],
            childColumns = ["returnId"],
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
        Index("returnId"),
        Index("sourceStockBatchId")
    ]
)
data class DealerSalesReturnAllocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val returnId: Long,
    val sourceStockBatchId: Long? = null,
    val batchNoSnapshot: String = "",
    val quantity: Int,
    val unitCost: Double,
    val totalCost: Double
)

@Entity(
    tableName = "dealer_business_purchase_returns",
    foreignKeys = [
        ForeignKey(
            entity = DealerPurchaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["purchaseId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = DealerPurchaseLineEntity::class,
            parentColumns = ["id"],
            childColumns = ["purchaseLineId"],
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
        Index("purchaseId"),
        Index("purchaseLineId"),
        Index("productId"),
        Index("returnedAt")
    ]
)
data class DealerPurchaseReturnEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val purchaseId: Long,
    val purchaseLineId: Long,
    val productId: Long? = null,
    val productNameSnapshot: String,
    val quantity: Int,
    val unitCost: Double,
    val totalValue: Double,
    val returnedAt: Long = System.currentTimeMillis(),
    val note: String = "",
    val workspace: String = "SHOP",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "dealer_business_expenses",
    indices = [
        Index(value = ["workspace", "expenseAt"]),
        Index(value = ["workspace", "category"])
    ]
)
data class DealerExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: String,
    val amount: Double,
    val expenseAt: Long = System.currentTimeMillis(),
    val note: String = "",
    val workspace: String = "SHOP",
    val createdAt: Long = System.currentTimeMillis()
)
