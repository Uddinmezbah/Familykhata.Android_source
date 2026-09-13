package com.familykhata.app.foodservice

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.familykhata.app.data.ProductEntity
import com.familykhata.app.data.StockBatchEntity

@Entity(
    tableName = "food_menu_items",
    indices = [
        Index(value = ["workspace", "name"])
    ]
)
data class FoodMenuItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,
    val category: String = "",

    val sellingPrice: Double = 0.0,

    val active: Boolean = true,

    val note: String = "",

    val workspace: String = "SHOP",

    val createdAt: Long =
        System.currentTimeMillis()
)

/**
 * Recipe ingredient quantity follows the current Inventory model.
 *
 * Use the smallest practical stock unit:
 * 1000 g instead of 1 kg,
 * 500 ml instead of 0.5 litre.
 */
@Entity(
    tableName = "food_recipe_ingredients",
    foreignKeys = [
        ForeignKey(
            entity = FoodMenuItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["menuItemId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["ingredientProductId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("menuItemId"),
        Index("ingredientProductId")
    ]
)
data class FoodRecipeIngredientEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val menuItemId: Long,

    val ingredientProductId: Long? = null,

    val ingredientNameSnapshot: String,

    val quantityPerItem: Int,

    val unitSnapshot: String = "pcs",

    val createdAt: Long =
        System.currentTimeMillis()
)

/**
 * orderType:
 * DINE_IN
 * TAKEAWAY
 * DELIVERY
 * CATERING
 *
 * status:
 * DRAFT
 * CONFIRMED
 * COMPLETED
 * CANCELLED
 */
@Entity(
    tableName = "food_orders",
    indices = [
        Index(value = ["workspace", "status"]),
        Index("orderedAt"),
        Index("eventDate")
    ]
)
data class FoodOrderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val orderNo: String,

    val orderType: String = "DINE_IN",

    val customerName: String = "",
    val phone: String = "",

    /**
     * Restaurant: table number / delivery reference.
     * Catering: event / venue reference.
     */
    val tableOrReference: String = "",

    val eventDate: Long? = null,

    val guestCount: Int = 0,

    val status: String = "DRAFT",

    val orderedAt: Long =
        System.currentTimeMillis(),

    val note: String = "",

    val workspace: String = "SHOP",

    val createdAt: Long =
        System.currentTimeMillis()
)

@Entity(
    tableName = "food_order_lines",
    foreignKeys = [
        ForeignKey(
            entity = FoodOrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FoodMenuItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["menuItemId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("orderId"),
        Index("menuItemId")
    ]
)
data class FoodOrderLineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val orderId: Long,

    val menuItemId: Long? = null,

    val menuItemNameSnapshot: String,

    val quantity: Int,

    val unitPrice: Double,

    val lineTotal: Double,

    val createdAt: Long =
        System.currentTimeMillis()
)

/**
 * Exact FIFO ingredient batches consumed when an order is completed.
 */
@Entity(
    tableName = "food_stock_allocations",
    foreignKeys = [
        ForeignKey(
            entity = FoodOrderLineEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderLineId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["ingredientProductId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = StockBatchEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceStockBatchId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("orderLineId"),
        Index("ingredientProductId"),
        Index("sourceStockBatchId")
    ]
)
data class FoodStockAllocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val orderLineId: Long,

    val ingredientProductId: Long? = null,

    val ingredientNameSnapshot: String,

    val sourceStockBatchId: Long? = null,
    val sourceBatchNoSnapshot: String = "",

    val quantity: Int,

    val unitCost: Double,

    val totalCost: Double,

    val createdAt: Long =
        System.currentTimeMillis()
)

@Entity(
    tableName = "food_payments",
    foreignKeys = [
        ForeignKey(
            entity = FoodOrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("orderId"),
        Index("paidAt")
    ]
)
data class FoodPaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val orderId: Long,

    val amount: Double,

    val paidAt: Long =
        System.currentTimeMillis(),

    val note: String = "",

    val createdAt: Long =
        System.currentTimeMillis()
)

data class FoodOrderSummary(
    val orderId: Long,

    val orderNo: String,
    val orderType: String,

    val customerName: String,

    val tableOrReference: String,

    val eventDate: Long?,

    val guestCount: Int,

    val status: String,

    val orderedAt: Long,

    val totalAmount: Double,

    val totalPaid: Double,

    val dueAmount: Double,

    val totalCost: Double,

    val grossProfit: Double
)
