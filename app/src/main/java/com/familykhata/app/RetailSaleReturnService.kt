package com.familykhata.app

import androidx.room.withTransaction
import com.familykhata.app.data.AppDatabase
import com.familykhata.app.data.BakiEntryEntity
import com.familykhata.app.data.FinancialAccountEntryEntity
import com.familykhata.app.data.InventoryDatabase
import com.familykhata.app.data.RetailSaleReturnEntity
import com.familykhata.app.data.RetailSaleStockAllocationEntity
import java.util.Locale
import java.util.UUID

internal class RetailSaleReturnService(
    private val inventoryDb: InventoryDatabase,
    private val appDb: AppDatabase
) {
    private val dao = inventoryDb.dao()
    private val bakiDao = appDb.dao()

    suspend fun record(
        saleId: Long,
        lineId: Long,
        quantity: Int,
        returnType: String,
        refundAccountId: Long?,
        note: String,
        returnedAt: Long,
        workspace: String,
        businessKey: String,
        ledgerBusinessId: String
    ): Boolean = runCatching {
        val type = returnType.trim().uppercase(Locale.ROOT)
        require(saleId > 0 && lineId > 0 && quantity > 0 && returnedAt > 0)
        require(type == "RESTOCK" || type == "DAMAGED")

        val sale = requireNotNull(dao.getRetailSaleOnce(saleId))
        require(sale.workspace == workspace && sale.businessKey == businessKey)
        require(sale.status != "CANCELLED")

        val line = requireNotNull(
            dao.getRetailSaleLinesOnce(saleId).firstOrNull { it.id == lineId }
        )
        val factor = line.unitFactor.coerceAtLeast(1)
        val baseLong = quantity.toLong() * factor
        require(baseLong in 1L..Int.MAX_VALUE.toLong())
        val baseQty = baseLong.toInt()
        val expectedBase = if (line.baseQuantity > 0) line.baseQuantity
        else (line.quantity.toLong() * factor).also {
            require(it in 1L..Int.MAX_VALUE.toLong())
        }.toInt()

        val oldLineReturns = dao.getRetailSaleReturnsForLineOnce(lineId)
        val alreadyReturned = oldLineReturns.sumOf { it.baseQuantity }
        require(alreadyReturned + baseQty <= expectedBase)

        val allocations = dao.getRetailSaleStockAllocationsOnce(lineId)
        require(allocations.sumOf { it.quantity.toLong() } == expectedBase.toLong())

        data class Slice(val a: RetailSaleStockAllocationEntity, val q: Int)
        var skip = alreadyReturned
        var remaining = baseQty
        val slices = mutableListOf<Slice>()
        allocations.forEach { a ->
            if (remaining <= 0) return@forEach
            if (skip >= a.quantity) {
                skip -= a.quantity
                return@forEach
            }
            val available = a.quantity - skip
            skip = 0
            val used = minOf(available, remaining)
            if (used > 0) {
                slices += Slice(a, used)
                remaining -= used
            }
        }
        require(remaining == 0)

        val cost = slices.sumOf { it.q * it.a.unitCost }
        val discountFactor = if (sale.subtotal > 0.0001)
            (sale.total / sale.subtotal).coerceIn(0.0, 1.0) else 0.0
        val amount = (quantity * line.unitPrice * discountFactor).coerceAtLeast(0.0)
        require(amount.isFinite())

        val oldReturns = dao.getRetailSaleReturnsOnce(saleId)
        val oldReturnAmount = oldReturns.sumOf { it.amount }
        val oldRefundAmount = oldReturns.sumOf { it.refundAmount }
        val effectiveTotal = (sale.total - oldReturnAmount).coerceAtLeast(0.0)
        val netPaid = (sale.paid - oldRefundAmount).coerceAtLeast(0.0)
        require(amount <= effectiveTotal + 0.01)
        val dueBefore = (effectiveTotal - netPaid).coerceAtLeast(0.0)
        val dueReduction = minOf(amount, dueBefore)
        val refund = (amount - dueReduction).coerceAtLeast(0.0)

        val refundAccount = if (refund > 0.0001) {
            val id = requireNotNull(refundAccountId?.takeIf { it > 0 })
            requireNotNull(bakiDao.getFinancialAccountOnce(id)).also { account ->
                require(account.workspace == workspace)
                require(workspace != "SHOP" || account.businessId == ledgerBusinessId)
                require(account.isActive)
                val balance = requireNotNull(
                    bakiDao.getFinancialAccountBalanceOnce(account.id)
                )
                require(balance - refund >= -0.0001)
            }
        } else null

        val personId = if (dueReduction > 0.0001) {
            val id = requireNotNull(sale.bakiPersonId)
            requireNotNull(
                bakiDao.getStatementPerson(id, workspace, ledgerBusinessId)
            )
            id
        } else null

        val eventKey = UUID.randomUUID().toString()
        val refundKey = "RETAIL_SALE_RETURN_ACCOUNT:$eventKey"
        val bakiKey = "RETAIL_SALE_RETURN_BAKI:$eventKey"

        appDb.withTransaction {
            if (refundAccount != null && refund > 0.0001) {
                require(
                    bakiDao.insertFinancialAccountEntry(
                        FinancialAccountEntryEntity(
                            accountId = refundAccount.id,
                            entryType = "RETAIL_SALE_RETURN_OUT",
                            amount = refund,
                            balanceDelta = -refund,
                            sourceKey = refundKey,
                            note = "Retail return ${sale.invoiceNo}",
                            workspace = workspace,
                            businessId = ledgerBusinessId,
                            createdAt = returnedAt
                        )
                    ) > 0
                )
            }
            if (personId != null && dueReduction > 0.0001) {
                require(
                    bakiDao.insertBakiEntryIgnore(
                        BakiEntryEntity(
                            personId = personId,
                            action = "RECEIVED_BACK",
                            amount = dueReduction,
                            balanceDelta = -dueReduction,
                            note = "Retail return ${sale.invoiceNo}",
                            sourceKey = bakiKey,
                            createdAt = returnedAt
                        )
                    ) > 0
                )
            }
        }

        try {
            inventoryDb.withTransaction {
                val freshSale = requireNotNull(dao.getRetailSaleOnce(saleId))
                require(freshSale.status != "CANCELLED")
                require(freshSale.workspace == workspace && freshSale.businessKey == businessKey)
                require(
                    dao.getRetailSaleReturnsForLineOnce(lineId)
                        .sumOf { it.baseQuantity } == alreadyReturned
                )

                if (type == "RESTOCK") {
                    slices.forEach { s ->
                        val batch = requireNotNull(dao.getBatchOnce(s.a.batchId))
                        val restored = batch.quantity.toLong() + s.q
                        require(restored <= Int.MAX_VALUE)
                        dao.updateBatchQuantity(batch.id, restored.toInt())
                    }
                }

                require(
                    dao.insertRetailSaleReturn(
                        RetailSaleReturnEntity(
                            eventKey = eventKey,
                            saleId = saleId,
                            saleLineId = lineId,
                            productId = line.productId,
                            productNameSnapshot = line.productNameSnapshot,
                            unitSnapshot = line.unitSnapshot,
                            unitFactor = factor,
                            quantity = quantity,
                            baseQuantity = baseQty,
                            amount = amount,
                            cost = cost,
                            returnType = type,
                            refundAmount = refund,
                            refundFinancialAccountId = refundAccount?.id,
                            note = note.trim(),
                            returnedAt = returnedAt
                        )
                    ) > 0
                )

                val dueAfter = (dueBefore - dueReduction).coerceAtLeast(0.0)
                val netPaidAfter = (netPaid - refund).coerceAtLeast(0.0)
                val status = when {
                    dueAfter <= 0.0001 -> "PAID"
                    netPaidAfter > 0.0001 -> "PARTIAL"
                    else -> "DUE"
                }
                dao.updateRetailSaleStatus(saleId, status)
            }
        } catch (t: Throwable) {
            runCatching {
                appDb.withTransaction {
                    bakiDao.deleteFinancialAccountEntryBySourceKey(refundKey)
                    bakiDao.deleteBakiEntryBySourceKey(bakiKey)
                }
            }
            throw t
        }
        true
    }.getOrDefault(false)
}
