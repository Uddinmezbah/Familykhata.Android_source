package com.familykhata.app

import androidx.room.withTransaction
import com.familykhata.app.data.FinancialAccountEntryEntity
import com.familykhata.app.data.PurchaseReturnEntity
import java.util.UUID

import com.familykhata.app.data.AppDatabase
import com.familykhata.app.data.InventoryDatabase

internal class PurchaseReturnService(
    private val inventoryDb: InventoryDatabase,
    private val appDb: AppDatabase
) {
    private val dao = inventoryDb.dao()
    private val appDao = appDb.dao()

    suspend fun record(
        billId: Long,
        lineId: Long,
        quantity: Int,
        refundAccountId: Long?,
        note: String,
        returnedAt: Long,
        workspace: String,
        businessKey: String,
        ledgerBusinessId: String
    ): Boolean = runCatching {
        require(
            billId > 0L &&
                lineId > 0L &&
                quantity > 0 &&
                returnedAt > 0L
        )

        val bill =
            requireNotNull(
                dao.getPurchaseBillOnce(
                    billId
                )
            )

        require(
            bill.workspace == workspace &&
                bill.businessKey == businessKey &&
                bill.status != "CANCELLED"
        )

        val line =
            requireNotNull(
                dao.getPurchaseBillLinesOnce(
                    billId
                ).firstOrNull {
                    it.id == lineId
                }
            )

        val factor =
            line.unitFactor.coerceAtLeast(1)

        val baseQuantityLong =
            quantity.toLong() *
                factor.toLong()

        require(
            baseQuantityLong in
                1L..Int.MAX_VALUE.toLong()
        )

        val baseQuantity =
            baseQuantityLong.toInt()

        val previousReturns =
            dao.getPurchaseReturnsOnce(
                billId
            )

        val alreadyReturnedBase =
            previousReturns
                .filter {
                    it.purchaseLineId == lineId
                }
                .sumOf {
                    it.baseQuantity
                }

        require(
            alreadyReturnedBase +
                baseQuantity <=
                line.baseQuantity
        )

        val stockBatchId =
            requireNotNull(
                line.stockBatchId
            )

        val stockBatch =
            requireNotNull(
                dao.getBatchOnce(
                    stockBatchId
                )
            )

        require(
            stockBatch.productId ==
                line.productId &&
                stockBatch.quantity >=
                    baseQuantity
        )

        val discountFactor =
            if (
                bill.subtotal > 0.0001
            ) {
                (
                    bill.total /
                        bill.subtotal
                ).coerceIn(
                    0.0,
                    1.0
                )
            } else {
                0.0
            }

        val amount =
            (
                quantity.toDouble() *
                    line.unitCost *
                    discountFactor
            ).coerceAtLeast(0.0)

        require(amount.isFinite())

        val returnedAmount =
            previousReturns.sumOf {
                it.amount
            }

        val refundedAmount =
            previousReturns.sumOf {
                it.refundAmount
            }

        val payments =
            dao.getPurchasePaymentsOnce(
                billId
            )

        val paidAmount =
            payments.sumOf {
                it.amount
            }

        val effectiveTotal =
            (
                bill.total -
                    returnedAmount
            ).coerceAtLeast(0.0)

        val netPaid =
            (
                paidAmount -
                    refundedAmount
            ).coerceAtLeast(0.0)

        require(
            amount <=
                effectiveTotal + 0.01
        )

        val dueBefore =
            (
                effectiveTotal -
                    netPaid
            ).coerceAtLeast(0.0)

        val dueReduction =
            minOf(
                amount,
                dueBefore
            )

        val refundAmount =
            (
                amount -
                    dueReduction
            ).coerceAtLeast(0.0)

        val refundAccount =
            if (refundAmount > 0.0001) {
                val accountId =
                    requireNotNull(
                        refundAccountId
                            ?.takeIf {
                                it > 0L
                            }
                    )

                requireNotNull(
                    appDao.getFinancialAccountOnce(
                        accountId
                    )
                ).also { account ->
                    require(
                        account.workspace ==
                            workspace &&
                            (
                                workspace != "SHOP" ||
                                    account.businessId ==
                                        ledgerBusinessId
                            ) &&
                            account.isActive
                    )
                }
            } else {
                null
            }

        val eventKey =
            "PURCHASE_RETURN:${UUID.randomUUID()}"

        val refundSourceKey =
            "PURCHASE_RETURN_ACCOUNT:$eventKey"

        if (
            refundAccount != null &&
                refundAmount > 0.0001
        ) {
            appDb.withTransaction {
                require(
                    appDao.insertFinancialAccountEntry(
                        FinancialAccountEntryEntity(
                            accountId =
                                refundAccount.id,
                            entryType =
                                "PURCHASE_RETURN_IN",
                            amount =
                                refundAmount,
                            balanceDelta =
                                refundAmount,
                            sourceKey =
                                refundSourceKey,
                            note =
                                "Purchase return ${bill.purchaseNo}",
                            workspace =
                                workspace,
                            businessId =
                                ledgerBusinessId,
                            createdAt =
                                returnedAt
                        )
                    ) > 0L
                )
            }
        }

        try {
            inventoryDb.withTransaction {
                val freshBill =
                    requireNotNull(
                        dao.getPurchaseBillOnce(
                            billId
                        )
                    )

                require(
                    freshBill.workspace ==
                        workspace &&
                        freshBill.businessKey ==
                            businessKey &&
                        freshBill.status !=
                            "CANCELLED"
                )

                val freshLine =
                    requireNotNull(
                        dao.getPurchaseBillLinesOnce(
                            billId
                        ).firstOrNull {
                            it.id == lineId
                        }
                    )

                require(
                    freshLine.productId ==
                        line.productId
                )

                val currentReturnedBase =
                    dao.getPurchaseReturnsOnce(
                        billId
                    )
                        .filter {
                            it.purchaseLineId ==
                                lineId
                        }
                        .sumOf {
                            it.baseQuantity
                        }

                require(
                    currentReturnedBase ==
                        alreadyReturnedBase
                )

                val freshBatch =
                    requireNotNull(
                        dao.getBatchOnce(
                            stockBatchId
                        )
                    )

                require(
                    freshBatch.productId ==
                        line.productId &&
                        freshBatch.quantity >=
                            baseQuantity
                )

                dao.updateBatchQuantity(
                    freshBatch.id,
                    freshBatch.quantity -
                        baseQuantity
                )

                require(
                    dao.insertPurchaseReturn(
                        PurchaseReturnEntity(
                            eventKey =
                                eventKey,
                            billId =
                                billId,
                            purchaseLineId =
                                lineId,
                            productId =
                                line.productId,
                            stockBatchId =
                                stockBatchId,
                            baseQuantity =
                                baseQuantity,
                            amount =
                                amount,
                            refundAmount =
                                refundAmount,
                            refundFinancialAccountId =
                                refundAccount?.id,
                            note =
                                note.trim(),
                            returnedAt =
                                returnedAt
                        )
                    ) > 0L
                )
            }
        } catch (error: Throwable) {
            if (refundAccount != null) {
                runCatching {
                    appDb.withTransaction {
                        appDao.deleteFinancialAccountEntryBySourceKey(
                            refundSourceKey
                        )
                    }
                }
            }

            throw error
        }

        true
    }.getOrDefault(false)
}
