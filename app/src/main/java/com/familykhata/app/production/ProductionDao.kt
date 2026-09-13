package com.familykhata.app.production

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItemRole(
        item: ProductionItemRoleEntity
    )

    @Query(
        """
        DELETE FROM production_item_roles
        WHERE productId = :productId
        """
    )
    suspend fun deleteItemRole(
        productId: Long
    )

    @Query(
        """
        SELECT *
        FROM production_item_roles
        WHERE workspace = :workspace
        ORDER BY productId ASC
        """
    )
    fun observeItemRoles(
        workspace: String
    ): Flow<List<ProductionItemRoleEntity>>

    @Query(
        """
        SELECT *
        FROM production_item_roles
        WHERE productId = :productId
        LIMIT 1
        """
    )
    suspend fun getItemRoleOnce(
        productId: Long
    ): ProductionItemRoleEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProductionBatch(
        item: ProductionBatchEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertConsumption(
        item: ProductionConsumptionEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCost(
        item: ProductionCostEntity
    ): Long

    @Query(
        """
        SELECT *
        FROM production_batches
        WHERE id = :batchId
        LIMIT 1
        """
    )
    suspend fun getProductionBatch(
        batchId: Long
    ): ProductionBatchEntity?

    @Query(
        """
        SELECT *
        FROM production_batches
        WHERE id = :batchId
        LIMIT 1
        """
    )
    fun observeProductionBatch(
        batchId: Long
    ): Flow<ProductionBatchEntity?>

    @Query(
        """
        SELECT *
        FROM production_consumptions
        WHERE productionBatchId = :batchId
        ORDER BY id ASC
        """
    )
    fun observeConsumptions(
        batchId: Long
    ): Flow<List<ProductionConsumptionEntity>>

    @Query(
        """
        SELECT *
        FROM production_consumptions
        WHERE productionBatchId = :batchId
        ORDER BY id ASC
        """
    )
    suspend fun getConsumptionsOnce(
        batchId: Long
    ): List<ProductionConsumptionEntity>

    @Query(
        """
        SELECT *
        FROM production_costs
        WHERE productionBatchId = :batchId
        ORDER BY createdAt ASC, id ASC
        """
    )
    fun observeCosts(
        batchId: Long
    ): Flow<List<ProductionCostEntity>>

    @Query(
        """
        SELECT *
        FROM production_costs
        WHERE productionBatchId = :batchId
        ORDER BY createdAt ASC, id ASC
        """
    )
    suspend fun getCostsOnce(
        batchId: Long
    ): List<ProductionCostEntity>

    @Query(
        """
        SELECT
            pb.id AS batchId,
            pb.batchNo AS batchNo,

            pb.finishedProductId AS finishedProductId,
            pb.finishedProductNameSnapshot AS finishedProductName,

            pb.outputQuantity AS outputQuantity,
            pb.status AS status,

            pb.startedAt AS startedAt,
            pb.completedAt AS completedAt,

            COALESCE(
                (
                    SELECT SUM(pc.totalCost)
                    FROM production_consumptions pc
                    WHERE pc.productionBatchId = pb.id
                ),
                0
            ) AS materialCost,

            COALESCE(
                (
                    SELECT SUM(pcost.amount)
                    FROM production_costs pcost
                    WHERE pcost.productionBatchId = pb.id
                ),
                0
            ) AS additionalCost,

            (
                COALESCE(
                    (
                        SELECT SUM(pc.totalCost)
                        FROM production_consumptions pc
                        WHERE pc.productionBatchId = pb.id
                    ),
                    0
                )
                +
                COALESCE(
                    (
                        SELECT SUM(pcost.amount)
                        FROM production_costs pcost
                        WHERE pcost.productionBatchId = pb.id
                    ),
                    0
                )
            ) AS totalProductionCost,

            CASE
                WHEN pb.outputQuantity > 0
                THEN
                    (
                        COALESCE(
                            (
                                SELECT SUM(pc.totalCost)
                                FROM production_consumptions pc
                                WHERE pc.productionBatchId = pb.id
                            ),
                            0
                        )
                        +
                        COALESCE(
                            (
                                SELECT SUM(pcost.amount)
                                FROM production_costs pcost
                                WHERE pcost.productionBatchId = pb.id
                            ),
                            0
                        )
                    ) * 1.0 / pb.outputQuantity
                ELSE 0
            END AS unitProductionCost

        FROM production_batches pb

        WHERE pb.workspace = :workspace

        ORDER BY
            CASE
                WHEN pb.status = 'DRAFT' THEN 0
                WHEN pb.status = 'IN_PROGRESS' THEN 1
                WHEN pb.status = 'COMPLETED' THEN 2
                WHEN pb.status = 'CANCELLED' THEN 3
                ELSE 4
            END,
            pb.startedAt DESC,
            pb.id DESC
        """
    )
    fun observeBatchSummaries(
        workspace: String
    ): Flow<List<ProductionBatchSummary>>

    @Query(
        """
        UPDATE production_batches
        SET status = :status
        WHERE id = :batchId
        """
    )
    suspend fun updateStatus(
        batchId: Long,
        status: String
    )

    @Query(
        """
        UPDATE production_batches
        SET outputQuantity = :outputQuantity
        WHERE id = :batchId
        """
    )
    suspend fun updateOutputQuantity(
        batchId: Long,
        outputQuantity: Int
    )

    @Query(
        """
        UPDATE production_batches
        SET status = 'COMPLETED',
            outputQuantity = :outputQuantity,
            completedAt = :completedAt,
            outputStockBatchId = :outputStockBatchId
        WHERE id = :batchId
          AND status != 'COMPLETED'
        """
    )
    suspend fun markCompleted(
        batchId: Long,
        outputQuantity: Int,
        completedAt: Long,
        outputStockBatchId: Long
    ): Int

    @Delete
    suspend fun deleteConsumption(
        item: ProductionConsumptionEntity
    )

    @Delete
    suspend fun deleteCost(
        item: ProductionCostEntity
    )

    @Query(
        """
        DELETE FROM production_batches
        WHERE id = :batchId
          AND status = 'DRAFT'
        """
    )
    suspend fun deleteDraftBatch(
        batchId: Long
    ): Int

    @Query(
        """
        SELECT *
        FROM production_item_roles
        ORDER BY productId ASC
        """
    )
    suspend fun getAllItemRoles():
        List<ProductionItemRoleEntity>

    @Query(
        """
        SELECT *
        FROM production_batches
        ORDER BY id ASC
        """
    )
    suspend fun getAllProductionBatches():
        List<ProductionBatchEntity>

    @Query(
        """
        SELECT *
        FROM production_consumptions
        ORDER BY id ASC
        """
    )
    suspend fun getAllConsumptions():
        List<ProductionConsumptionEntity>

    @Query(
        """
        SELECT *
        FROM production_costs
        ORDER BY id ASC
        """
    )
    suspend fun getAllCosts():
        List<ProductionCostEntity>
}
