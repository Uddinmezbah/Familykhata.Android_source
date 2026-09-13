package com.familykhata.app.production

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.familykhata.app.data.ProductEntity
import com.familykhata.app.data.StockBatchEntity

/**
 * Adds a production-specific role to an existing inventory product.
 *
 * The actual product and stock continue to live in InventoryDatabase.
 *
 * Supported roles:
 * RAW_MATERIAL
 * FINISHED_GOOD
 * BOTH
 */
@Entity(
    tableName = "production_item_roles",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["workspace", "role"])
    ]
)
data class ProductionItemRoleEntity(
    @PrimaryKey
    val productId: Long,
    val role: String = "RAW_MATERIAL",
    val workspace: String = "SHOP",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * One manufacturing / production run.
 *
 * finishedProductId points to the existing inventory product.
 * The name snapshot keeps production history readable even if the
 * inventory product is deleted later.
 */
@Entity(
    tableName = "production_batches",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["finishedProductId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = StockBatchEntity::class,
            parentColumns = ["id"],
            childColumns = ["outputStockBatchId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("finishedProductId"),
        Index("outputStockBatchId"),
        Index(value = ["workspace", "status"]),
        Index("startedAt")
    ]
)
data class ProductionBatchEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val batchNo: String = "",

    val finishedProductId: Long? = null,
    val finishedProductNameSnapshot: String,

    val outputQuantity: Int = 0,

    val status: String = "DRAFT",

    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,

    /**
     * Set only after production is completed and finished stock
     * has been written into inventory_batches.
     */
    val outputStockBatchId: Long? = null,

    val note: String = "",
    val workspace: String = "SHOP",

    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Raw material consumed by a production batch.
 *
 * Quantity follows the existing inventory model and therefore remains Int.
 * Fractional materials should use the smallest practical unit:
 * e.g. 1500 g instead of 1.5 kg, 750 ml instead of 0.75 L.
 *
 * unitCost and totalCost are snapshots taken at consumption time.
 */
@Entity(
    tableName = "production_consumptions",
    foreignKeys = [
        ForeignKey(
            entity = ProductionBatchEntity::class,
            parentColumns = ["id"],
            childColumns = ["productionBatchId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["materialProductId"],
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
        Index("productionBatchId"),
        Index("materialProductId"),
        Index("sourceStockBatchId")
    ]
)
data class ProductionConsumptionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val productionBatchId: Long,

    val materialProductId: Long? = null,
    val materialNameSnapshot: String,
    val unitSnapshot: String = "pcs",

    /**
     * Exact inventory batch used by FIFO consumption.
     * May become null if that stock batch is later deleted,
     * while the snapshots below preserve historical information.
     */
    val sourceStockBatchId: Long? = null,
    val sourceBatchNoSnapshot: String = "",

    val quantity: Int,

    val unitCost: Double,
    val totalCost: Double,

    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Additional production costs that do not directly reduce inventory.
 *
 * Typical costType values:
 * LABOR
 * OVERHEAD
 * TRANSPORT
 * PACKAGING
 * OTHER
 */
@Entity(
    tableName = "production_costs",
    foreignKeys = [
        ForeignKey(
            entity = ProductionBatchEntity::class,
            parentColumns = ["id"],
            childColumns = ["productionBatchId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("productionBatchId"),
        Index("costType")
    ]
)
data class ProductionCostEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val productionBatchId: Long,

    val costType: String = "OTHER",
    val amount: Double,

    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Read model for the Production dashboard/list.
 *
 * Calculated costs will come from ProductionDao queries.
 */
data class ProductionBatchSummary(
    val batchId: Long,
    val batchNo: String,

    val finishedProductId: Long?,
    val finishedProductName: String,

    val outputQuantity: Int,
    val status: String,

    val startedAt: Long,
    val completedAt: Long?,

    val materialCost: Double,
    val additionalCost: Double,
    val totalProductionCost: Double,
    val unitProductionCost: Double
)
