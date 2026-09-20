package com.familykhata.app.production

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.familykhata.app.businessDataKey
import com.familykhata.app.baseWorkspaceKey
import com.familykhata.app.businessIdFromWorkspaceKey
import com.familykhata.app.data.InventoryDatabase
import com.familykhata.app.data.ProductEntity
import com.familykhata.app.data.StockBatchEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.min

data class ProductionMaterialInput(
    val productId: Long,
    val quantity: Int
)

data class ProductionExtraCostInput(
    val costType: String,
    val amount: Double,
    val note: String = ""
)

@OptIn(ExperimentalCoroutinesApi::class)
class ProductionViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val database =
        InventoryDatabase.get(application)

    private val inventoryDao =
        database.dao()

    private val dao =
        database.productionDao()

    private val workspace =
        MutableStateFlow("SHOP")

    private val businessKey =
        MutableStateFlow("legacy")

    private val productContext =
        combine(
            workspace,
            businessKey
        ) { workspaceValue, businessValue ->
            workspaceValue to businessValue
        }


    val products:
        StateFlow<List<ProductEntity>> =
        productContext.flatMapLatest { context ->
            inventoryDao.observeProductsForBusiness(
                workspace = baseWorkspaceKey(context.first),
                businessKey = context.second
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    val itemRoles:
        StateFlow<List<ProductionItemRoleEntity>> =
        workspace.flatMapLatest {
            dao.observeItemRoles(it)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    val batches:
        StateFlow<List<ProductionBatchSummary>> =
        workspace.flatMapLatest {
            dao.observeBatchSummaries(it)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    fun setBusinessContext(
        workspaceValue: String,
        shopType: String
    ) {
        val legacyKey =
            businessDataKey(shopType)
        val key =
            businessIdFromWorkspaceKey(
                workspaceValue
            ) ?: legacyKey

        if (workspace.value != workspaceValue) {
            workspace.value = workspaceValue
        }

        if (businessKey.value != key) {
            businessKey.value = key
        }

        viewModelScope.launch {
            inventoryDao.claimExistingBusinessProducts(
                workspace =
                    baseWorkspaceKey(workspaceValue),
                legacyBusinessKey = legacyKey,
                targetBusinessId = key
            )
        }
    }

    fun setWorkspace(value: String) {
        if (workspace.value != value) {
            workspace.value = value
        }
    }

    fun setItemRole(
        productId: Long,
        role: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (productId <= 0) {
            onDone(false)
            return
        }

        val normalizedRole =
            role.trim().uppercase()

        if (
            normalizedRole !in setOf(
                "RAW_MATERIAL",
                "FINISHED_GOOD",
                "BOTH"
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
                    val product =
                        inventoryDao.getProductOnce(
                            productId
                        ) ?: error(
                            "Product not found"
                        )

                    require(
                        product.workspace == baseWorkspaceKey(currentWorkspace)
                    )

                    dao.upsertItemRole(
                        ProductionItemRoleEntity(
                            productId =
                                productId,
                            role =
                                normalizedRole,
                            workspace =
                                currentWorkspace
                        )
                    )
                }.isSuccess

            onDone(success)
        }
    }

    fun removeItemRole(
        productId: Long
    ) {
        if (productId <= 0) return

        viewModelScope.launch {
            dao.deleteItemRole(
                productId
            )
        }
    }

    fun completeProduction(
        finishedProductId: Long,
        outputQuantity: Int,
        batchNo: String,
        materials:
            List<ProductionMaterialInput>,
        extraCosts:
            List<ProductionExtraCostInput>,
        note: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            finishedProductId <= 0 ||
            outputQuantity <= 0 ||
            materials.isEmpty()
        ) {
            onDone(false)
            return
        }

        val normalizedMaterials =
            materials
                .filter {
                    it.productId > 0 &&
                    it.quantity > 0
                }
                .groupBy {
                    it.productId
                }
                .map {
                        (productId, lines) ->
                    ProductionMaterialInput(
                        productId =
                            productId,
                        quantity =
                            lines.sumOf {
                                it.quantity
                            }
                    )
                }

        if (
            normalizedMaterials.isEmpty() ||
            normalizedMaterials.any {
                it.productId ==
                    finishedProductId
            }
        ) {
            onDone(false)
            return
        }

        val normalizedCosts =
            extraCosts
                .filter {
                    it.amount > 0
                }
                .map {
                    ProductionExtraCostInput(
                        costType =
                            it.costType
                                .trim()
                                .uppercase()
                                .ifBlank {
                                    "OTHER"
                                },
                        amount =
                            it.amount,
                        note =
                            it.note.trim()
                    )
                }

        val currentWorkspace =
            workspace.value

        viewModelScope.launch {
            val success =
                runCatching {
                    database.withTransaction {

                        val finishedProduct =
                            inventoryDao
                                .getProductOnce(
                                    finishedProductId
                                )
                                ?: error(
                                    "Finished product not found"
                                )

                        require(
                            finishedProduct.workspace ==
                                currentWorkspace
                        )

                        val finishedRole =
                            dao.getItemRoleOnce(
                                finishedProductId
                            ) ?: error(
                                "Finished product role missing"
                            )

                        require(
                            finishedRole.workspace ==
                                currentWorkspace
                        )

                        require(
                            finishedRole.role ==
                                "FINISHED_GOOD" ||
                            finishedRole.role ==
                                "BOTH"
                        )

                        data class PreparedMaterial(
                            val product:
                                ProductEntity,
                            val quantity:
                                Int,
                            val batches:
                                List<StockBatchEntity>
                        )

                        val preparedMaterials =
                            normalizedMaterials.map {
                                    input ->

                                val product =
                                    inventoryDao
                                        .getProductOnce(
                                            input.productId
                                        )
                                        ?: error(
                                            "Raw material not found"
                                        )

                                require(
                                    product.workspace == baseWorkspaceKey(currentWorkspace)
                                )

                                val role =
                                    dao.getItemRoleOnce(
                                        product.id
                                    ) ?: error(
                                        "Raw material role missing"
                                    )

                                require(
                                    role.workspace ==
                                        currentWorkspace
                                )

                                require(
                                    role.role ==
                                        "RAW_MATERIAL" ||
                                    role.role ==
                                        "BOTH"
                                )

                                val batches =
                                    inventoryDao
                                        .getBatchesOnce(
                                            product.id
                                        )
                                        .filter {
                                            it.quantity > 0
                                        }

                                val available =
                                    batches.sumOf {
                                        it.quantity
                                    }

                                require(
                                    available >=
                                        input.quantity
                                )

                                PreparedMaterial(
                                    product =
                                        product,
                                    quantity =
                                        input.quantity,
                                    batches =
                                        batches
                                )
                            }

                        val now =
                            System.currentTimeMillis()

                        val finalBatchNo =
                            batchNo.trim()
                                .ifBlank {
                                    "P-$now"
                                }

                        val productionBatchId =
                            dao.insertProductionBatch(
                                ProductionBatchEntity(
                                    batchNo =
                                        finalBatchNo,
                                    finishedProductId =
                                        finishedProduct.id,
                                    finishedProductNameSnapshot =
                                        finishedProduct.name,
                                    outputQuantity =
                                        0,
                                    status =
                                        "IN_PROGRESS",
                                    startedAt =
                                        now,
                                    note =
                                        note.trim(),
                                    workspace =
                                        currentWorkspace
                                )
                            )

                        require(
                            productionBatchId > 0
                        )

                        var materialCost =
                            0.0

                        preparedMaterials.forEach {
                                prepared ->

                            var remaining =
                                prepared.quantity

                            for (
                                stockBatch
                                in prepared.batches
                            ) {
                                if (
                                    remaining <= 0
                                ) {
                                    break
                                }

                                val used =
                                    min(
                                        remaining,
                                        stockBatch.quantity
                                    )

                                val lineCost =
                                    used *
                                        stockBatch.purchasePrice

                                inventoryDao
                                    .updateBatchQuantity(
                                        batchId =
                                            stockBatch.id,
                                        quantity =
                                            stockBatch.quantity -
                                                used
                                    )

                                dao.insertConsumption(
                                    ProductionConsumptionEntity(
                                        productionBatchId =
                                            productionBatchId,
                                        materialProductId =
                                            prepared.product.id,
                                        materialNameSnapshot =
                                            prepared.product.name,
                                        unitSnapshot =
                                            prepared.product.unit,
                                        sourceStockBatchId =
                                            stockBatch.id,
                                        sourceBatchNoSnapshot =
                                            stockBatch.batchNo,
                                        quantity =
                                            used,
                                        unitCost =
                                            stockBatch.purchasePrice,
                                        totalCost =
                                            lineCost
                                    )
                                )

                                materialCost +=
                                    lineCost

                                remaining -=
                                    used
                            }

                            require(
                                remaining == 0
                            )
                        }

                        var additionalCost =
                            0.0

                        normalizedCosts.forEach {
                                cost ->

                            dao.insertCost(
                                ProductionCostEntity(
                                    productionBatchId =
                                        productionBatchId,
                                    costType =
                                        cost.costType,
                                    amount =
                                        cost.amount,
                                    note =
                                        cost.note
                                )
                            )

                            additionalCost +=
                                cost.amount
                        }

                        val totalCost =
                            materialCost +
                                additionalCost

                        val unitProductionCost =
                            totalCost /
                                outputQuantity

                        val outputStockBatchId =
                            inventoryDao.insertBatch(
                                StockBatchEntity(
                                    productId =
                                        finishedProduct.id,
                                    batchNo =
                                        finalBatchNo,
                                    quantity =
                                        outputQuantity,
                                    purchasePrice =
                                        unitProductionCost,
                                    purchaseDate =
                                        now
                                )
                            )

                        require(
                            outputStockBatchId > 0
                        )

                        val completed =
                            dao.markCompleted(
                                batchId =
                                    productionBatchId,
                                outputQuantity =
                                    outputQuantity,
                                completedAt =
                                    now,
                                outputStockBatchId =
                                    outputStockBatchId
                            )

                        require(
                            completed == 1
                        )
                    }
                }.isSuccess

            onDone(success)
        }
    }

    fun observeConsumptions(
        batchId: Long
    ): Flow<List<ProductionConsumptionEntity>> =
        dao.observeConsumptions(
            batchId
        )

    fun observeCosts(
        batchId: Long
    ): Flow<List<ProductionCostEntity>> =
        dao.observeCosts(
            batchId
        )
}
