package com.familykhata.app.dealerbusiness

import androidx.room.ColumnInfo
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

data class DealerCustomerLedgerSummary(
    val customerId: Long,
    val customerName: String,
    val customerCode: String,
    val grossSales: Double,
    val totalReturns: Double,
    val totalCollections: Double
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
        Index("sourceStockBatchId"),
        Index("saleAllocationId")
    ]
)
data class DealerSalesReturnAllocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val returnId: Long,
    val saleAllocationId: Long? = null,
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

@Entity(
    tableName = "dealer_business_product_packs",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("workspace")
    ]
)
data class DealerProductPackEntity(
    @PrimaryKey
    val productId: Long,
    val piecesPerBox: Int = 0,
    val piecesPerSheet: Int = 0,
    val workspace: String = "SHOP",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "dealer_business_delivery_people",
    indices = [
        Index(value = ["workspace", "name"])
    ]
)
data class DealerDeliveryPersonEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String = "",
    val note: String = "",
    val workspace: String = "SHOP",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "dealer_business_delivery_challans",
    foreignKeys = [
        ForeignKey(
            entity = DealerDeliveryPersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["deliveryPersonId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("deliveryPersonId"),
        Index(value = ["workspace", "status"]),
        Index(value = ["workspace", "challanNo"])
    ]
)
data class DealerDeliveryChallanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val deliveryPersonId: Long? = null,
    val deliveryPersonNameSnapshot: String = "",
    val challanNo: String = "",
    val issuedAt: Long = System.currentTimeMillis(),
    val settledAt: Long? = null,
    val status: String = "OPEN",
    val note: String = "",
    val workspace: String = "SHOP",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "dealer_business_delivery_challan_lines",
    foreignKeys = [
        ForeignKey(
            entity = DealerDeliveryChallanEntity::class,
            parentColumns = ["id"],
            childColumns = ["challanId"],
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
        Index("challanId"),
        Index("productId")
    ]
)
data class DealerDeliveryChallanLineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val challanId: Long,
    val productId: Long? = null,
    val productNameSnapshot: String,
    val boxCount: Int = 0,
    val sheetCount: Int = 0,
    val loosePieces: Int = 0,
    val piecesPerBoxSnapshot: Int = 0,
    val piecesPerSheetSnapshot: Int = 0,
    val quantityPieces: Int,
    @ColumnInfo(defaultValue = "'pcs'")
    val unitSnapshot: String = "pcs",
    @ColumnInfo(defaultValue = "1")
    val unitFactor: Int = 1,
    @ColumnInfo(defaultValue = "0")
    val enteredQuantity: Int = 0
)

@Entity(
    tableName = "dealer_business_delivery_challan_allocations",
    foreignKeys = [
        ForeignKey(
            entity = DealerDeliveryChallanLineEntity::class,
            parentColumns = ["id"],
            childColumns = ["challanLineId"],
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
        Index("challanLineId"),
        Index("sourceStockBatchId")
    ]
)
data class DealerDeliveryChallanAllocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val challanLineId: Long,
    val sourceStockBatchId: Long? = null,
    val batchNoSnapshot: String = "",
    val quantityPieces: Int,
    val unitCost: Double,
    val totalCost: Double
)

@Entity(
    tableName = "dealer_business_delivery_challan_sales",
    foreignKeys = [
        ForeignKey(
            entity = DealerDeliveryChallanEntity::class,
            parentColumns = ["id"],
            childColumns = ["challanId"],
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
        Index("challanId"),
        Index("saleId"),
        Index(
            value = ["challanId", "saleId"],
            unique = true
        )
    ]
)
data class DealerDeliveryChallanSaleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val challanId: Long,
    val saleId: Long
)

@Entity(
    tableName = "dealer_business_delivery_sale_allocations",
    foreignKeys = [
        ForeignKey(
            entity = DealerDeliveryChallanLineEntity::class,
            parentColumns = ["id"],
            childColumns = ["challanLineId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = DealerDeliveryChallanAllocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["challanAllocationId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = DealerSaleLineEntity::class,
            parentColumns = ["id"],
            childColumns = ["saleLineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("challanLineId"),
        Index("challanAllocationId"),
        Index("saleLineId")
    ]
)
data class DealerDeliverySaleAllocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val challanLineId: Long,
    val challanAllocationId: Long,
    val saleLineId: Long,
    val quantityPieces: Int,
    val unitCost: Double,
    val totalCost: Double
)

@Entity(
    tableName = "dealer_business_delivery_settlements",
    foreignKeys = [
        ForeignKey(
            entity = DealerDeliveryChallanEntity::class,
            parentColumns = ["id"],
            childColumns = ["challanId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(
            value = ["challanId"],
            unique = true
        ),
        Index("receivedAt")
    ]
)
data class DealerDeliverySettlementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val challanId: Long,
    val cashHandedOver: Double = 0.0,
    val receivedAt: Long = System.currentTimeMillis(),
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "dealer_business_delivery_settlement_lines",
    foreignKeys = [
        ForeignKey(
            entity = DealerDeliverySettlementEntity::class,
            parentColumns = ["id"],
            childColumns = ["settlementId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = DealerDeliveryChallanLineEntity::class,
            parentColumns = ["id"],
            childColumns = ["challanLineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("settlementId"),
        Index("challanLineId"),
        Index(
            value = [
                "settlementId",
                "challanLineId"
            ],
            unique = true
        )
    ]
)
data class DealerDeliverySettlementLineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val settlementId: Long,
    val challanLineId: Long,
    val soldPieces: Int = 0,
    val returnedPieces: Int = 0,
    val damagedPieces: Int = 0,
    val note: String = ""
)

@Entity(
    tableName = "dealer_business_damages",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = StockBatchEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceStockBatchId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = DealerDeliveryChallanEntity::class,
            parentColumns = ["id"],
            childColumns = ["deliveryChallanId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("productId"),
        Index("sourceStockBatchId"),
        Index("deliveryChallanId"),
        Index(value = ["workspace", "damagedAt"])
    ]
)
data class DealerDamageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long? = null,
    val sourceStockBatchId: Long? = null,
    val deliveryChallanId: Long? = null,
    val productNameSnapshot: String,
    val batchNoSnapshot: String = "",
    val quantityPieces: Int,
    val unitCost: Double,
    val totalCost: Double,
    val sourceType: String = "WAREHOUSE",
    val reason: String = "",
    val damagedAt: Long = System.currentTimeMillis(),
    val note: String = "",
    val workspace: String = "SHOP",
    val createdAt: Long = System.currentTimeMillis()
)
