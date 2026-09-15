package com.familykhata.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "inventory_products",
    indices = [
        Index(value = ["workspace", "name"]),
        Index(
            value = [
                "workspace",
                "businessKey",
                "name"
            ]
        )
    ]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String = "",
    val sku: String = "",
    val unit: String = "pcs",
    val brand: String = "",
    val genericName: String = "",
    val modelName: String = "",
    val serialOrImei: String = "",
    val size: String = "",
    val color: String = "",
    val warrantyMonths: Int = 0,
    val sellingPrice: Double = 0.0,
    @ColumnInfo(defaultValue = "0")
    val mrp: Double = 0.0,
    @ColumnInfo(defaultValue = "''")
    val rackLocation: String = "",
    val lowStockLevel: Int = 5,
    val note: String = "",
    @ColumnInfo(defaultValue = "'SHOP'") val workspace: String = "SHOP",
    @ColumnInfo(defaultValue = "'legacy'")
    val businessKey: String = "legacy",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "inventory_batches",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("productId")]
)
data class StockBatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val batchNo: String = "",
    val quantity: Int,
    val purchasePrice: Double,
    val purchaseDate: Long,
    val expiryDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class ProductStockSummary(
    val id: Long,
    val name: String,
    val category: String,
    val sku: String,
    val unit: String,
    val brand: String,
    val genericName: String,
    val modelName: String,
    val serialOrImei: String,
    val size: String,
    val color: String,
    val warrantyMonths: Int,
    val sellingPrice: Double,
    val mrp: Double,
    val rackLocation: String,
    val lowStockLevel: Int,
    val note: String,
    val workspace: String,
    val businessKey: String,
    val totalStock: Int,
    val stockValue: Double,
    val avgPurchasePrice: Double,
    val saleValue: Double,
    val potentialProfit: Double,
    val nextExpiry: Long?
)

@Entity(
    tableName = "retail_sales",
    indices = [
        Index(
            value = [
                "workspace",
                "businessKey",
                "soldAt"
            ]
        ),
        Index(
            value = [
                "workspace",
                "businessKey",
                "invoiceNo"
            ],
            unique = true
        )
    ]
)
data class RetailSaleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceNo: String,
    val bakiPersonId: Long? = null,
    val customerName: String = "",
    val customerPhone: String = "",
    val subtotal: Double,
    val discount: Double = 0.0,
    val total: Double,
    val paid: Double = 0.0,
    val paymentMethod: String = "CASH",
    val status: String = "DUE",
    val note: String = "",
    val workspace: String = "SHOP",
    val businessKey: String = "legacy",
    val soldAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "retail_sale_lines",
    foreignKeys = [
        ForeignKey(
            entity = RetailSaleEntity::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("saleId"),
        Index("productId")
    ]
)
data class RetailSaleLineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val saleId: Long,
    val productId: Long,
    val productNameSnapshot: String,
    val skuSnapshot: String = "",
    val unitSnapshot: String = "pcs",
    val quantity: Int,
    val unitPrice: Double,
    val unitCost: Double,
    val lineTotal: Double,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "retail_sale_stock_allocations",
    foreignKeys = [
        ForeignKey(
            entity = RetailSaleLineEntity::class,
            parentColumns = ["id"],
            childColumns = ["saleLineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("saleLineId"),
        Index("batchId")
    ]
)
data class RetailSaleStockAllocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val saleLineId: Long,
    val batchId: Long,
    val quantity: Int,
    val unitCost: Double,
    val createdAt: Long = System.currentTimeMillis()
)
