package com.familykhata.app.foodservice

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.familykhata.app.data.InventoryDatabase
import com.familykhata.app.data.ProductEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.min

data class FoodOrderLineInput(
    val menuItemId: Long,
    val quantity: Int
)

@OptIn(ExperimentalCoroutinesApi::class)
class FoodServiceViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val database =
        InventoryDatabase.get(application)

    private val inventoryDao =
        database.dao()

    private val dao =
        database.foodServiceDao()

    private val workspace =
        MutableStateFlow("SHOP")

    val inventoryProducts:
        StateFlow<List<ProductEntity>> =
        workspace.flatMapLatest {
            inventoryDao.observeProducts(it)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    val menuItems:
        StateFlow<List<FoodMenuItemEntity>> =
        workspace.flatMapLatest {
            dao.observeMenuItems(it)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    val orders:
        StateFlow<List<FoodOrderSummary>> =
        workspace.flatMapLatest {
            dao.observeOrderSummaries(it)
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

    fun addMenuItem(
        name: String,
        category: String,
        sellingPrice: Double,
        note: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            name.isBlank() ||
            sellingPrice < 0
        ) {
            onDone(false)
            return
        }

        val currentWorkspace =
            workspace.value

        viewModelScope.launch {
            val success =
                runCatching {
                    dao.insertMenuItem(
                        FoodMenuItemEntity(
                            name =
                                name.trim(),
                            category =
                                category.trim(),
                            sellingPrice =
                                sellingPrice,
                            active =
                                true,
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

    fun addRecipeIngredient(
        menuItemId: Long,
        productId: Long,
        quantityPerItem: Int,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            menuItemId <= 0 ||
            productId <= 0 ||
            quantityPerItem <= 0
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
                        val menu =
                            dao.getMenuItemOnce(
                                menuItemId
                            ) ?: error(
                                "Menu item not found"
                            )

                        require(
                            menu.workspace ==
                                currentWorkspace
                        )

                        val product =
                            inventoryDao
                                .getProductOnce(
                                    productId
                                )
                                ?: error(
                                    "Ingredient product not found"
                                )

                        require(
                            product.workspace ==
                                currentWorkspace
                        )

                        require(
                            dao.countRecipeIngredient(
                                menuItemId =
                                    menuItemId,
                                productId =
                                    product.id
                            ) == 0
                        ) {
                            "Ingredient already exists in recipe"
                        }

                        dao.insertRecipeIngredient(
                            FoodRecipeIngredientEntity(
                                menuItemId =
                                    menuItemId,
                                ingredientProductId =
                                    product.id,
                                ingredientNameSnapshot =
                                    product.name,
                                quantityPerItem =
                                    quantityPerItem,
                                unitSnapshot =
                                    product.unit
                            )
                        )
                    }
                }.isSuccess

            onDone(success)
        }
    }

    fun setMenuItemActive(
        menuItemId: Long,
        active: Boolean
    ) {
        if (menuItemId <= 0) return

        viewModelScope.launch {
            dao.updateMenuItemActive(
                menuItemId =
                    menuItemId,
                active =
                    active
            )
        }
    }

    fun createOrder(
        orderNo: String,
        orderType: String,
        customerName: String,
        phone: String,
        tableOrReference: String,
        eventDate: Long?,
        guestCount: Int,
        lines: List<FoodOrderLineInput>,
        initialPayment: Double,
        note: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        val normalizedOrderType =
            orderType
                .trim()
                .uppercase()

        if (
            normalizedOrderType !in setOf(
                "DINE_IN",
                "TAKEAWAY",
                "DELIVERY",
                "CATERING"
            ) ||
            guestCount < 0 ||
            initialPayment < 0
        ) {
            onDone(false)
            return
        }

        val normalizedLines =
            lines
                .filter {
                    it.menuItemId > 0 &&
                    it.quantity > 0
                }
                .groupBy {
                    it.menuItemId
                }
                .map {
                        (menuItemId, rows) ->
                    FoodOrderLineInput(
                        menuItemId =
                            menuItemId,
                        quantity =
                            rows.sumOf {
                                it.quantity
                            }
                    )
                }

        if (normalizedLines.isEmpty()) {
            onDone(false)
            return
        }

        val currentWorkspace =
            workspace.value

        viewModelScope.launch {
            val success =
                runCatching {
                    database.withTransaction {

                        val now =
                            System.currentTimeMillis()

                        val finalOrderNo =
                            orderNo
                                .trim()
                                .ifBlank {
                                    "FO-$now"
                                }

                        require(
                            dao.countOrderNo(
                                workspace =
                                    currentWorkspace,
                                orderNo =
                                    finalOrderNo
                            ) == 0
                        )

                        data class PreparedLine(
                            val menu:
                                FoodMenuItemEntity,
                            val quantity:
                                Int
                        )

                        val preparedLines =
                            normalizedLines.map {
                                    input ->

                                val menu =
                                    dao.getMenuItemOnce(
                                        input.menuItemId
                                    ) ?: error(
                                        "Menu item not found"
                                    )

                                require(
                                    menu.workspace ==
                                        currentWorkspace
                                )

                                require(
                                    menu.active
                                )

                                PreparedLine(
                                    menu =
                                        menu,
                                    quantity =
                                        input.quantity
                                )
                            }

                        val orderTotal =
                            preparedLines.sumOf {
                                it.menu.sellingPrice *
                                    it.quantity
                            }

                        require(
                            initialPayment <=
                                orderTotal +
                                0.009
                        )

                        val orderId =
                            dao.insertOrder(
                                FoodOrderEntity(
                                    orderNo =
                                        finalOrderNo,
                                    orderType =
                                        normalizedOrderType,
                                    customerName =
                                        customerName.trim(),
                                    phone =
                                        phone.trim(),
                                    tableOrReference =
                                        tableOrReference.trim(),
                                    eventDate =
                                        eventDate,
                                    guestCount =
                                        guestCount,
                                    status =
                                        "CONFIRMED",
                                    orderedAt =
                                        now,
                                    note =
                                        note.trim(),
                                    workspace =
                                        currentWorkspace
                                )
                            )

                        require(orderId > 0)

                        preparedLines.forEach {
                                line ->

                            dao.insertOrderLine(
                                FoodOrderLineEntity(
                                    orderId =
                                        orderId,
                                    menuItemId =
                                        line.menu.id,
                                    menuItemNameSnapshot =
                                        line.menu.name,
                                    quantity =
                                        line.quantity,
                                    unitPrice =
                                        line.menu.sellingPrice,
                                    lineTotal =
                                        line.menu.sellingPrice *
                                            line.quantity
                                )
                            )
                        }

                        if (initialPayment > 0) {
                            dao.insertPayment(
                                FoodPaymentEntity(
                                    orderId =
                                        orderId,
                                    amount =
                                        initialPayment,
                                    paidAt =
                                        now,
                                    note =
                                        "INITIAL"
                                )
                            )
                        }
                    }
                }.isSuccess

            onDone(success)
        }
    }

    fun completeOrder(
        orderId: Long,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (orderId <= 0) {
            onDone(false)
            return
        }

        val currentWorkspace =
            workspace.value

        viewModelScope.launch {
            val success =
                runCatching {
                    database.withTransaction {

                        val order =
                            dao.getOrderOnce(
                                orderId
                            ) ?: error(
                                "Order not found"
                            )

                        require(
                            order.workspace ==
                                currentWorkspace
                        )

                        require(
                            order.status ==
                                "CONFIRMED"
                        )

                        val orderLines =
                            dao.getOrderLinesOnce(
                                orderId
                            )

                        require(
                            orderLines.isNotEmpty()
                        )

                        data class IngredientNeed(
                            val orderLineId: Long,
                            val productId: Long,
                            val productName: String,
                            val requiredQuantity: Int
                        )

                        val needs =
                            mutableListOf<
                                IngredientNeed
                                >()

                        orderLines.forEach {
                                line ->

                            val menuItemId =
                                line.menuItemId
                                    ?: error(
                                        "Menu item missing"
                                    )

                            val menu =
                                dao.getMenuItemOnce(
                                    menuItemId
                                ) ?: error(
                                    "Menu item not found"
                                )

                            require(
                                menu.workspace ==
                                    currentWorkspace
                            )

                            val recipe =
                                dao.getRecipeIngredientsOnce(
                                    menu.id
                                )

                            require(
                                recipe.isNotEmpty()
                            )

                            recipe.forEach {
                                    ingredient ->

                                val productId =
                                    ingredient
                                        .ingredientProductId
                                        ?: error(
                                            "Ingredient product missing"
                                        )

                                val product =
                                    inventoryDao
                                        .getProductOnce(
                                            productId
                                        )
                                        ?: error(
                                            "Ingredient product not found"
                                        )

                                require(
                                    product.workspace ==
                                        currentWorkspace
                                )

                                val requiredLong =
                                    ingredient
                                        .quantityPerItem
                                        .toLong() *
                                        line.quantity
                                            .toLong()

                                require(
                                    requiredLong > 0 &&
                                    requiredLong <=
                                        Int.MAX_VALUE
                                )

                                needs.add(
                                    IngredientNeed(
                                        orderLineId =
                                            line.id,
                                        productId =
                                            product.id,
                                        productName =
                                            product.name,
                                        requiredQuantity =
                                            requiredLong
                                                .toInt()
                                    )
                                )
                            }
                        }

                        /*
                         * Pre-check combined demand per ingredient.
                         * This prevents a misleading late failure when
                         * multiple menu lines use the same stock item.
                         */
                        val totalNeedByProduct =
                            needs
                                .groupBy {
                                    it.productId
                                }
                                .mapValues {
                                        (_, rows) ->
                                    rows.sumOf {
                                        it.requiredQuantity
                                    }
                                }

                        totalNeedByProduct.forEach {
                                (productId, required) ->

                            val available =
                                inventoryDao
                                    .getBatchesOnce(
                                        productId
                                    )
                                    .filter {
                                        it.quantity > 0
                                    }
                                    .sumOf {
                                        it.quantity
                                    }

                            require(
                                available >=
                                    required
                            )
                        }

                        /*
                         * FIFO consumption.
                         *
                         * Because every update/allocation happens inside
                         * this single Room transaction, any later failure
                         * rolls all stock changes back.
                         */
                        needs.forEach {
                                need ->

                            var remaining =
                                need.requiredQuantity

                            val batches =
                                inventoryDao
                                    .getBatchesOnce(
                                        need.productId
                                    )
                                    .filter {
                                        it.quantity > 0
                                    }

                            for (
                                stockBatch
                                in batches
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

                                val totalCost =
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

                                dao.insertStockAllocation(
                                    FoodStockAllocationEntity(
                                        orderLineId =
                                            need.orderLineId,
                                        ingredientProductId =
                                            need.productId,
                                        ingredientNameSnapshot =
                                            need.productName,
                                        sourceStockBatchId =
                                            stockBatch.id,
                                        sourceBatchNoSnapshot =
                                            stockBatch.batchNo,
                                        quantity =
                                            used,
                                        unitCost =
                                            stockBatch.purchasePrice,
                                        totalCost =
                                            totalCost
                                    )
                                )

                                remaining -=
                                    used
                            }

                            require(
                                remaining == 0
                            )
                        }

                        val affected =
                            dao.markOrderCompleted(
                                orderId
                            )

                        require(
                            affected == 1
                        )
                    }
                }.isSuccess

            onDone(success)
        }
    }

    fun addPayment(
        orderId: Long,
        amount: Double,
        note: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            orderId <= 0 ||
            amount <= 0
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

                        val order =
                            dao.getOrderOnce(
                                orderId
                            ) ?: error(
                                "Order not found"
                            )

                        require(
                            order.workspace ==
                                currentWorkspace
                        )

                        require(
                            order.status !=
                                "CANCELLED"
                        )

                        val total =
                            dao.getOrderTotal(
                                orderId
                            )

                        val paid =
                            dao.getOrderPaid(
                                orderId
                            )

                        val remaining =
                            (
                                total -
                                    paid
                                ).coerceAtLeast(
                                    0.0
                                )

                        require(
                            amount <=
                                remaining +
                                0.009
                        )

                        dao.insertPayment(
                            FoodPaymentEntity(
                                orderId =
                                    orderId,
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

    fun observeRecipeIngredients(
        menuItemId: Long
    ): Flow<List<FoodRecipeIngredientEntity>> =
        dao.observeRecipeIngredients(
            menuItemId
        )

    fun observeOrderLines(
        orderId: Long
    ): Flow<List<FoodOrderLineEntity>> =
        dao.observeOrderLines(
            orderId
        )

    fun observePayments(
        orderId: Long
    ): Flow<List<FoodPaymentEntity>> =
        dao.observePayments(
            orderId
        )
}
