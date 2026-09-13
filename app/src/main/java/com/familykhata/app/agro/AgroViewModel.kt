package com.familykhata.app.agro

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.familykhata.app.data.InventoryDatabase
import com.familykhata.app.data.ProductEntity
import com.familykhata.app.data.StockBatchEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class AgroViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val database =
        InventoryDatabase.get(application)

    private val inventoryDao =
        database.dao()

    private val dao =
        database.agroDao()

    private val workspace =
        MutableStateFlow("SHOP")

    val products:
        StateFlow<List<ProductEntity>> =
        workspace.flatMapLatest {
            inventoryDao.observeProducts(it)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    val cycles:
        StateFlow<List<AgroCycleSummary>> =
        workspace.flatMapLatest {
            dao.observeCycleSummaries(it)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    fun setWorkspace(
        value: String
    ) {
        if (
            workspace.value !=
            value
        ) {
            workspace.value =
                value
        }
    }

    fun addCycle(
        cycleType: String,
        name: String,
        breedOrVariety: String,
        location: String,
        startingAmount: Double,
        startingUnit: String,
        expectedEndDate: Long?,
        note: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        val normalizedType =
            cycleType
                .trim()
                .uppercase()

        if (
            name.isBlank() ||
            normalizedType !in setOf(
                "POULTRY",
                "CROP",
                "GENERAL"
            ) ||
            startingAmount < 0
        ) {
            onDone(false)
            return
        }

        val currentWorkspace =
            workspace.value

        viewModelScope.launch {
            val success =
                runCatching {
                    dao.insertCycle(
                        AgroCycleEntity(
                            cycleType =
                                normalizedType,
                            name =
                                name.trim(),
                            breedOrVariety =
                                breedOrVariety.trim(),
                            location =
                                location.trim(),
                            startingAmount =
                                startingAmount,
                            startingUnit =
                                startingUnit.trim(),
                            expectedEndDate =
                                expectedEndDate,
                            status =
                                "ACTIVE",
                            note =
                                note.trim(),
                            workspace =
                                currentWorkspace
                        )
                    )
                }.isSuccess

            onDone(success)
        }
    }

    fun addCost(
        cycleId: Long,
        costType: String,
        quantity: Double,
        unit: String,
        amount: Double,
        note: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            cycleId <= 0 ||
            amount <= 0 ||
            quantity < 0
        ) {
            onDone(false)
            return
        }

        val normalizedType =
            costType
                .trim()
                .uppercase()
                .ifBlank {
                    "OTHER"
                }

        val currentWorkspace =
            workspace.value

        viewModelScope.launch {
            val success =
                runCatching {
                    database.withTransaction {
                        val cycle =
                            dao.getCycleOnce(
                                cycleId
                            ) ?: error(
                                "Cycle not found"
                            )

                        require(
                            cycle.workspace ==
                                currentWorkspace
                        )

                        require(
                            cycle.status ==
                                "ACTIVE"
                        )

                        dao.insertCost(
                            AgroCostEntity(
                                cycleId =
                                    cycleId,
                                costType =
                                    normalizedType,
                                quantity =
                                    quantity,
                                unit =
                                    unit.trim(),
                                amount =
                                    amount,
                                note =
                                    note.trim()
                            )
                        )
                    }
                }.isSuccess

            onDone(success)
        }
    }

    fun addLoss(
        cycleId: Long,
        lossType: String,
        quantity: Double,
        unit: String,
        reason: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            cycleId <= 0 ||
            quantity <= 0
        ) {
            onDone(false)
            return
        }

        val normalizedType =
            lossType
                .trim()
                .uppercase()
                .ifBlank {
                    "OTHER"
                }

        val currentWorkspace =
            workspace.value

        viewModelScope.launch {
            val success =
                runCatching {
                    database.withTransaction {
                        val cycle =
                            dao.getCycleOnce(
                                cycleId
                            ) ?: error(
                                "Cycle not found"
                            )

                        require(
                            cycle.workspace ==
                                currentWorkspace
                        )

                        require(
                            cycle.status ==
                                "ACTIVE"
                        )

                        dao.insertLoss(
                            AgroLossEntity(
                                cycleId =
                                    cycleId,
                                lossType =
                                    normalizedType,
                                quantity =
                                    quantity,
                                unit =
                                    unit.trim(),
                                reason =
                                    reason.trim()
                            )
                        )
                    }
                }.isSuccess

            onDone(success)
        }
    }

    fun addHarvest(
        cycleId: Long,
        productId: Long,
        quantity: Int,
        allocatedCost: Double?,
        batchNo: String,
        note: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            cycleId <= 0 ||
            productId <= 0 ||
            quantity <= 0 ||
            (
                allocatedCost != null &&
                allocatedCost < 0
            )
        ) {
            onDone(false)
            return
        }

        val currentWorkspace =
            workspace.value

        viewModelScope.launch {
            val success =
                runCatching {
                    database.withTransaction {
                        val cycle =
                            dao.getCycleOnce(
                                cycleId
                            ) ?: error(
                                "Cycle not found"
                            )

                        require(
                            cycle.workspace ==
                                currentWorkspace
                        )

                        require(
                            cycle.status ==
                                "ACTIVE"
                        )

                        val product =
                            inventoryDao
                                .getProductOnce(
                                    productId
                                )
                                ?: error(
                                    "Product not found"
                                )

                        require(
                            product.workspace ==
                                currentWorkspace
                        )

                        val totalCost =
                            dao.getTotalCost(
                                cycleId
                            )

                        val alreadyAllocated =
                            dao.getAllocatedCost(
                                cycleId
                            )

                        val remainingCost =
                            (
                                totalCost -
                                    alreadyAllocated
                                ).coerceAtLeast(
                                    0.0
                                )

                        val finalAllocatedCost =
                            allocatedCost
                                ?: remainingCost

                        require(
                            finalAllocatedCost >= 0
                        )

                        require(
                            finalAllocatedCost <=
                                remainingCost +
                                0.009
                        )

                        val now =
                            System.currentTimeMillis()

                        val finalBatchNo =
                            batchNo
                                .trim()
                                .ifBlank {
                                    "AG-$cycleId-$now"
                                }

                        val unitCost =
                            finalAllocatedCost /
                                quantity

                        val inventoryBatchId =
                            inventoryDao.insertBatch(
                                StockBatchEntity(
                                    productId =
                                        product.id,
                                    batchNo =
                                        finalBatchNo,
                                    quantity =
                                        quantity,
                                    purchasePrice =
                                        unitCost,
                                    purchaseDate =
                                        now
                                )
                            )

                        require(
                            inventoryBatchId > 0
                        )

                        val harvestId =
                            dao.insertHarvest(
                                AgroHarvestEntity(
                                    cycleId =
                                        cycle.id,
                                    productId =
                                        product.id,
                                    inventoryBatchId =
                                        inventoryBatchId,
                                    productNameSnapshot =
                                        product.name,
                                    quantity =
                                        quantity,
                                    unitSnapshot =
                                        product.unit,
                                    allocatedCost =
                                        finalAllocatedCost,
                                    unitCost =
                                        unitCost,
                                    batchNo =
                                        finalBatchNo,
                                    harvestedAt =
                                        now,
                                    note =
                                        note.trim()
                                )
                            )

                        require(
                            harvestId > 0
                        )
                    }
                }.isSuccess

            onDone(success)
        }
    }

    fun completeCycle(
        cycleId: Long,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (cycleId <= 0) {
            onDone(false)
            return
        }

        val currentWorkspace =
            workspace.value

        viewModelScope.launch {
            val success =
                runCatching {
                    database.withTransaction {
                        val cycle =
                            dao.getCycleOnce(
                                cycleId
                            ) ?: error(
                                "Cycle not found"
                            )

                        require(
                            cycle.workspace ==
                                currentWorkspace
                        )

                        require(
                            cycle.status ==
                                "ACTIVE"
                        )

                        dao.updateCycleStatus(
                            cycleId =
                                cycleId,
                            status =
                                "COMPLETED"
                        )
                    }
                }.isSuccess

            onDone(success)
        }
    }

    fun observeCosts(
        cycleId: Long
    ): Flow<List<AgroCostEntity>> =
        dao.observeCosts(
            cycleId
        )

    fun observeLosses(
        cycleId: Long
    ): Flow<List<AgroLossEntity>> =
        dao.observeLosses(
            cycleId
        )

    fun observeHarvests(
        cycleId: Long
    ): Flow<List<AgroHarvestEntity>> =
        dao.observeHarvests(
            cycleId
        )
}
