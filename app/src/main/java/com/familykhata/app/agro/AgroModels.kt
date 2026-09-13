package com.familykhata.app.agro

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.familykhata.app.data.ProductEntity
import com.familykhata.app.data.StockBatchEntity

/**
 * One poultry flock / crop / farm production cycle.
 *
 * cycleType:
 * POULTRY
 * CROP
 * GENERAL
 *
 * startingAmount + startingUnit allow the same model to represent:
 * 500 birds
 * 2.5 acres
 * 100 plants
 */
@Entity(
    tableName = "agro_cycles",
    indices = [
        Index(value = ["workspace", "status"]),
        Index("startDate")
    ]
)
data class AgroCycleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val cycleType: String,
    val name: String,

    val breedOrVariety: String = "",
    val location: String = "",

    val startingAmount: Double = 0.0,
    val startingUnit: String = "",

    val startDate: Long =
        System.currentTimeMillis(),

    val expectedEndDate: Long? = null,

    val status: String = "ACTIVE",

    val note: String = "",

    val workspace: String = "SHOP",

    val createdAt: Long =
        System.currentTimeMillis()
)

/**
 * Operating cost for one Agro cycle.
 *
 * Poultry examples:
 * CHICK, FEED, MEDICINE, VACCINE, LABOR
 *
 * Crop examples:
 * SEED, FERTILIZER, PESTICIDE, IRRIGATION, LABOR
 *
 * Shared:
 * TRANSPORT, EQUIPMENT, OTHER
 */
@Entity(
    tableName = "agro_costs",
    foreignKeys = [
        ForeignKey(
            entity = AgroCycleEntity::class,
            parentColumns = ["id"],
            childColumns = ["cycleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("cycleId"),
        Index("costType"),
        Index("occurredAt")
    ]
)
data class AgroCostEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val cycleId: Long,

    val costType: String,

    val quantity: Double = 0.0,
    val unit: String = "",

    val amount: Double,

    val note: String = "",

    val occurredAt: Long =
        System.currentTimeMillis(),

    val createdAt: Long =
        System.currentTimeMillis()
)

/**
 * Mortality / crop loss / wastage.
 *
 * lossType:
 * MORTALITY
 * CROP_LOSS
 * WASTAGE
 * OTHER
 */
@Entity(
    tableName = "agro_losses",
    foreignKeys = [
        ForeignKey(
            entity = AgroCycleEntity::class,
            parentColumns = ["id"],
            childColumns = ["cycleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("cycleId"),
        Index("lossType"),
        Index("occurredAt")
    ]
)
data class AgroLossEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val cycleId: Long,

    val lossType: String,

    val quantity: Double,
    val unit: String = "",

    val reason: String = "",

    val occurredAt: Long =
        System.currentTimeMillis(),

    val createdAt: Long =
        System.currentTimeMillis()
)

/**
 * Harvest / poultry output connected to existing inventory.
 *
 * Examples:
 * Paddy
 * Vegetables
 * Eggs
 * Broiler chickens
 *
 * allocatedCost is the portion of the cycle cost assigned to this
 * output. It becomes inventory purchase cost for profit calculation.
 */
@Entity(
    tableName = "agro_harvests",
    foreignKeys = [
        ForeignKey(
            entity = AgroCycleEntity::class,
            parentColumns = ["id"],
            childColumns = ["cycleId"],
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
        Index("cycleId"),
        Index("productId"),
        Index("inventoryBatchId"),
        Index("harvestedAt")
    ]
)
data class AgroHarvestEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val cycleId: Long,

    val productId: Long? = null,
    val inventoryBatchId: Long? = null,

    val productNameSnapshot: String,

    val quantity: Int,
    val unitSnapshot: String = "pcs",

    val allocatedCost: Double = 0.0,
    val unitCost: Double = 0.0,

    val batchNo: String = "",

    val harvestedAt: Long =
        System.currentTimeMillis(),

    val note: String = "",

    val createdAt: Long =
        System.currentTimeMillis()
)

data class AgroCycleSummary(
    val cycleId: Long,

    val cycleType: String,
    val name: String,

    val breedOrVariety: String,

    val startingAmount: Double,
    val startingUnit: String,

    val status: String,

    val startDate: Long,
    val expectedEndDate: Long?,

    val totalCost: Double,
    val totalLoss: Double,

    val totalHarvestQuantity: Int,
    val totalAllocatedCost: Double
)
