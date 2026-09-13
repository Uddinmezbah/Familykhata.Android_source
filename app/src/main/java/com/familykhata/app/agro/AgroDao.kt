package com.familykhata.app.agro

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AgroDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCycle(
        item: AgroCycleEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCost(
        item: AgroCostEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertLoss(
        item: AgroLossEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertHarvest(
        item: AgroHarvestEntity
    ): Long

    @Query(
        """
        SELECT *
        FROM agro_cycles
        WHERE id = :cycleId
        LIMIT 1
        """
    )
    suspend fun getCycleOnce(
        cycleId: Long
    ): AgroCycleEntity?

    @Query(
        """
        SELECT
            c.id AS cycleId,
            c.cycleType AS cycleType,
            c.name AS name,
            c.breedOrVariety AS breedOrVariety,
            c.startingAmount AS startingAmount,
            c.startingUnit AS startingUnit,
            c.status AS status,
            c.startDate AS startDate,
            c.expectedEndDate AS expectedEndDate,

            COALESCE(
                (
                    SELECT SUM(cost.amount)
                    FROM agro_costs cost
                    WHERE cost.cycleId = c.id
                ),
                0
            ) AS totalCost,

            COALESCE(
                (
                    SELECT SUM(loss.quantity)
                    FROM agro_losses loss
                    WHERE loss.cycleId = c.id
                ),
                0
            ) AS totalLoss,

            COALESCE(
                (
                    SELECT SUM(h.quantity)
                    FROM agro_harvests h
                    WHERE h.cycleId = c.id
                ),
                0
            ) AS totalHarvestQuantity,

            COALESCE(
                (
                    SELECT SUM(h.allocatedCost)
                    FROM agro_harvests h
                    WHERE h.cycleId = c.id
                ),
                0
            ) AS totalAllocatedCost

        FROM agro_cycles c
        WHERE c.workspace = :workspace

        ORDER BY
            CASE
                WHEN c.status = 'ACTIVE' THEN 0
                WHEN c.status = 'COMPLETED' THEN 1
                WHEN c.status = 'CANCELLED' THEN 2
                ELSE 3
            END,
            c.startDate DESC,
            c.id DESC
        """
    )
    fun observeCycleSummaries(
        workspace: String
    ): Flow<List<AgroCycleSummary>>

    @Query(
        """
        SELECT *
        FROM agro_costs
        WHERE cycleId = :cycleId
        ORDER BY occurredAt DESC, id DESC
        """
    )
    fun observeCosts(
        cycleId: Long
    ): Flow<List<AgroCostEntity>>

    @Query(
        """
        SELECT *
        FROM agro_losses
        WHERE cycleId = :cycleId
        ORDER BY occurredAt DESC, id DESC
        """
    )
    fun observeLosses(
        cycleId: Long
    ): Flow<List<AgroLossEntity>>

    @Query(
        """
        SELECT *
        FROM agro_harvests
        WHERE cycleId = :cycleId
        ORDER BY harvestedAt DESC, id DESC
        """
    )
    fun observeHarvests(
        cycleId: Long
    ): Flow<List<AgroHarvestEntity>>

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0)
        FROM agro_costs
        WHERE cycleId = :cycleId
        """
    )
    suspend fun getTotalCost(
        cycleId: Long
    ): Double

    @Query(
        """
        SELECT COALESCE(SUM(allocatedCost), 0)
        FROM agro_harvests
        WHERE cycleId = :cycleId
        """
    )
    suspend fun getAllocatedCost(
        cycleId: Long
    ): Double

    @Query(
        """
        UPDATE agro_cycles
        SET status = :status
        WHERE id = :cycleId
    """
    )
    suspend fun updateCycleStatus(
        cycleId: Long,
        status: String
    )

    @Query(
        """
        SELECT *
        FROM agro_cycles
        ORDER BY id
        """
    )
    suspend fun getAllCycles():
        List<AgroCycleEntity>

    @Query(
        """
        SELECT *
        FROM agro_costs
        ORDER BY id
        """
    )
    suspend fun getAllCosts():
        List<AgroCostEntity>

    @Query(
        """
        SELECT *
        FROM agro_losses
        ORDER BY id
        """
    )
    suspend fun getAllLosses():
        List<AgroLossEntity>

    @Query(
        """
        SELECT *
        FROM agro_harvests
        ORDER BY id
        """
    )
    suspend fun getAllHarvests():
        List<AgroHarvestEntity>
}
