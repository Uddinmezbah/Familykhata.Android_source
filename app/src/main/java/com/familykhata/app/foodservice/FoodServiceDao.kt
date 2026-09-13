package com.familykhata.app.foodservice

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodServiceDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMenuItem(
        item: FoodMenuItemEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRecipeIngredient(
        item: FoodRecipeIngredientEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertOrder(
        item: FoodOrderEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertOrderLine(
        item: FoodOrderLineEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStockAllocation(
        item: FoodStockAllocationEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPayment(
        item: FoodPaymentEntity
    ): Long

    @Query(
        """
        SELECT *
        FROM food_menu_items
        WHERE workspace = :workspace
        ORDER BY active DESC, name COLLATE NOCASE ASC
        """
    )
    fun observeMenuItems(
        workspace: String
    ): Flow<List<FoodMenuItemEntity>>

    @Query(
        """
        SELECT *
        FROM food_menu_items
        WHERE id = :menuItemId
        LIMIT 1
        """
    )
    suspend fun getMenuItemOnce(
        menuItemId: Long
    ): FoodMenuItemEntity?

    @Query(
        """
        SELECT *
        FROM food_recipe_ingredients
        WHERE menuItemId = :menuItemId
        ORDER BY id ASC
        """
    )
    fun observeRecipeIngredients(
        menuItemId: Long
    ): Flow<List<FoodRecipeIngredientEntity>>

    @Query(
        """
        SELECT *
        FROM food_recipe_ingredients
        WHERE menuItemId = :menuItemId
        ORDER BY id ASC
        """
    )
    suspend fun getRecipeIngredientsOnce(
        menuItemId: Long
    ): List<FoodRecipeIngredientEntity>

    @Query(
        """
        SELECT COUNT(*)
        FROM food_recipe_ingredients
        WHERE menuItemId = :menuItemId
          AND ingredientProductId = :productId
        """
    )
    suspend fun countRecipeIngredient(
        menuItemId: Long,
        productId: Long
    ): Int

    @Delete
    suspend fun deleteRecipeIngredient(
        item: FoodRecipeIngredientEntity
    )

    @Query(
        """
        SELECT *
        FROM food_orders
        WHERE id = :orderId
        LIMIT 1
        """
    )
    suspend fun getOrderOnce(
        orderId: Long
    ): FoodOrderEntity?

    @Query(
        """
        SELECT COUNT(*)
        FROM food_orders
        WHERE workspace = :workspace
          AND orderNo = :orderNo
        """
    )
    suspend fun countOrderNo(
        workspace: String,
        orderNo: String
    ): Int

    @Query(
        """
        SELECT *
        FROM food_order_lines
        WHERE orderId = :orderId
        ORDER BY id ASC
        """
    )
    fun observeOrderLines(
        orderId: Long
    ): Flow<List<FoodOrderLineEntity>>

    @Query(
        """
        SELECT *
        FROM food_order_lines
        WHERE orderId = :orderId
        ORDER BY id ASC
        """
    )
    suspend fun getOrderLinesOnce(
        orderId: Long
    ): List<FoodOrderLineEntity>

    @Query(
        """
        SELECT *
        FROM food_payments
        WHERE orderId = :orderId
        ORDER BY paidAt DESC, id DESC
        """
    )
    fun observePayments(
        orderId: Long
    ): Flow<List<FoodPaymentEntity>>

    @Query(
        """
        SELECT COALESCE(SUM(lineTotal), 0)
        FROM food_order_lines
        WHERE orderId = :orderId
        """
    )
    suspend fun getOrderTotal(
        orderId: Long
    ): Double

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0)
        FROM food_payments
        WHERE orderId = :orderId
        """
    )
    suspend fun getOrderPaid(
        orderId: Long
    ): Double

    @Query(
        """
        SELECT
            o.id AS orderId,
            o.orderNo AS orderNo,
            o.orderType AS orderType,
            o.customerName AS customerName,
            o.tableOrReference AS tableOrReference,
            o.eventDate AS eventDate,
            o.guestCount AS guestCount,
            o.status AS status,
            o.orderedAt AS orderedAt,

            COALESCE(
                (
                    SELECT SUM(l.lineTotal)
                    FROM food_order_lines l
                    WHERE l.orderId = o.id
                ),
                0
            ) AS totalAmount,

            COALESCE(
                (
                    SELECT SUM(p.amount)
                    FROM food_payments p
                    WHERE p.orderId = o.id
                ),
                0
            ) AS totalPaid,

            (
                COALESCE(
                    (
                        SELECT SUM(l.lineTotal)
                        FROM food_order_lines l
                        WHERE l.orderId = o.id
                    ),
                    0
                )
                -
                COALESCE(
                    (
                        SELECT SUM(p.amount)
                        FROM food_payments p
                        WHERE p.orderId = o.id
                    ),
                    0
                )
            ) AS dueAmount,

            COALESCE(
                (
                    SELECT SUM(a.totalCost)
                    FROM food_stock_allocations a
                    INNER JOIN food_order_lines l
                        ON l.id = a.orderLineId
                    WHERE l.orderId = o.id
                ),
                0
            ) AS totalCost,

            (
                COALESCE(
                    (
                        SELECT SUM(l.lineTotal)
                        FROM food_order_lines l
                        WHERE l.orderId = o.id
                    ),
                    0
                )
                -
                COALESCE(
                    (
                        SELECT SUM(a.totalCost)
                        FROM food_stock_allocations a
                        INNER JOIN food_order_lines l2
                            ON l2.id = a.orderLineId
                        WHERE l2.orderId = o.id
                    ),
                    0
                )
            ) AS grossProfit

        FROM food_orders o
        WHERE o.workspace = :workspace

        ORDER BY
            CASE
                WHEN o.status = 'DRAFT' THEN 0
                WHEN o.status = 'CONFIRMED' THEN 1
                WHEN o.status = 'COMPLETED' THEN 2
                WHEN o.status = 'CANCELLED' THEN 3
                ELSE 4
            END,
            o.orderedAt DESC,
            o.id DESC
        """
    )
    fun observeOrderSummaries(
        workspace: String
    ): Flow<List<FoodOrderSummary>>

    @Query(
        """
        UPDATE food_orders
        SET status = :status
        WHERE id = :orderId
        """
    )
    suspend fun updateOrderStatus(
        orderId: Long,
        status: String
    )

    @Query(
        """
        UPDATE food_orders
        SET status = 'COMPLETED'
        WHERE id = :orderId
          AND status = 'CONFIRMED'
        """
    )
    suspend fun markOrderCompleted(
        orderId: Long
    ): Int

    @Query(
        """
        UPDATE food_menu_items
        SET active = :active
        WHERE id = :menuItemId
        """
    )
    suspend fun updateMenuItemActive(
        menuItemId: Long,
        active: Boolean
    )

    @Query(
        """
        SELECT *
        FROM food_menu_items
        ORDER BY id ASC
        """
    )
    suspend fun getAllMenuItems():
        List<FoodMenuItemEntity>

    @Query(
        """
        SELECT *
        FROM food_recipe_ingredients
        ORDER BY id ASC
        """
    )
    suspend fun getAllRecipeIngredients():
        List<FoodRecipeIngredientEntity>

    @Query(
        """
        SELECT *
        FROM food_orders
        ORDER BY id ASC
        """
    )
    suspend fun getAllOrders():
        List<FoodOrderEntity>

    @Query(
        """
        SELECT *
        FROM food_order_lines
        ORDER BY id ASC
        """
    )
    suspend fun getAllOrderLines():
        List<FoodOrderLineEntity>

    @Query(
        """
        SELECT *
        FROM food_stock_allocations
        ORDER BY id ASC
        """
    )
    suspend fun getAllStockAllocations():
        List<FoodStockAllocationEntity>

    @Query(
        """
        SELECT *
        FROM food_payments
        ORDER BY id ASC
        """
    )
    suspend fun getAllPayments():
        List<FoodPaymentEntity>
}
