package com.familykhata.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "inventory_products",
    indices = [Index(value = ["workspace", "name"])]
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
    val lowStockLevel: Int = 5,
    val note: String = "",
    @ColumnInfo(defaultValue = "'SHOP'") val workspace: String = "SHOP",
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
    val lowStockLevel: Int,
    val note: String,
    val workspace: String,
    val totalStock: Int,
    val stockValue: Double,
    val avgPurchasePrice: Double,
    val saleValue: Double,
    val potentialProfit: Double,
    val nextExpiry: Long?
)
