package com.familykhata.app.report

import com.familykhata.app.data.FinancialAccountEntity
import com.familykhata.app.data.FinancialAccountEntryEntity
import com.familykhata.app.data.ProductStockSummary
import com.familykhata.app.data.PurchaseBillEntity
import com.familykhata.app.data.PurchasePaymentEntity
import com.familykhata.app.data.PurchaseReturnEntity
import com.familykhata.app.data.RetailSaleEntity
import com.familykhata.app.data.RetailSaleLineEntity
import com.familykhata.app.data.RetailSalePaymentEntity
import com.familykhata.app.data.TransactionEntity

fun buildBusinessReport(
    startInclusive: Long,
    endExclusive: Long,
    sales: List<RetailSaleEntity>,
    saleLines: List<RetailSaleLineEntity>,
    salePayments: List<RetailSalePaymentEntity>,
    purchaseBills: List<PurchaseBillEntity>,
    purchasePayments: List<PurchasePaymentEntity>,
    purchaseReturns: List<PurchaseReturnEntity>,
    transactions: List<TransactionEntity>,
    products: List<ProductStockSummary>,
    accounts: List<FinancialAccountEntity>,
    accountEntries: List<FinancialAccountEntryEntity>
): BusinessReportSummary {
    require(endExclusive > startInclusive)

    val activeSales =
        sales.filter {
            it.status != "CANCELLED"
        }

    val reportSales =
        activeSales.filter {
            it.soldAt >= startInclusive &&
                it.soldAt < endExclusive
        }

    val reportSaleMap =
        reportSales.associateBy {
            it.id
        }

    val reportSaleLines =
        saleLines.filter {
            it.saleId in reportSaleMap
        }

    val salesRevenue =
        reportSales.sumOf {
            it.total
        }

    val costOfGoodsSold =
        reportSaleLines.sumOf {
            it.unitCost *
                it.quantity.toDouble()
        }

    val grossProfit =
        salesRevenue -
            costOfGoodsSold

    /*
     * transactions is the P&L source for manual income/expense
     * and digital-service profit.
     *
     * Retail sale receipts and purchase account movements must
     * never be counted here as additional P&L.
     */
    val reportTransactions =
        transactions.filter {
            it.createdAt >= startInclusive &&
                it.createdAt < endExclusive &&
                it.sourceKey
                    ?.startsWith(
                        "RETAIL_SALE"
                    ) != true &&
                it.sourceKey
                    ?.startsWith(
                        "PURCHASE"
                    ) != true
        }

    val otherIncome =
        reportTransactions
            .filter {
                it.type == "INCOME"
            }
            .sumOf {
                it.amount
            }

    val expense =
        reportTransactions
            .filter {
                it.type == "EXPENSE"
            }
            .sumOf {
                it.amount
            }

    val netProfit =
        grossProfit +
            otherIncome -
            expense

    /*
     * Initial POS payment is saved with paidAt == soldAt.
     * A later payment event is therefore a due collection.
     */
    val activeSaleIds =
        activeSales.mapTo(
            mutableSetOf()
        ) {
            it.id
        }

    val dueCollection =
        salePayments
            .asSequence()
            .filter {
                it.saleId in activeSaleIds &&
                    it.paidAt >= startInclusive &&
                    it.paidAt < endExclusive
            }
            .filter { payment ->
                val sale =
                    activeSales.firstOrNull {
                        it.id ==
                            payment.saleId
                    }

                sale != null &&
                    payment.paidAt >
                    sale.soldAt
            }
            .sumOf {
                it.amount
            }

    /*
     * Outstanding figures are current snapshots.
     * They intentionally do not represent a reconstructed
     * historical balance for the selected date range.
     */
    val outstandingCustomerDue =
        activeSales.sumOf {
            (
                it.total -
                    it.paid
                ).coerceAtLeast(
                0.0
            )
        }

    val activeBills =
        purchaseBills.filter {
            it.status != "CANCELLED"
        }

    val activeBillIds =
        activeBills.mapTo(
            mutableSetOf()
        ) {
            it.id
        }

    val purchaseInPeriod =
        activeBills
            .filter {
                it.purchasedAt >=
                    startInclusive &&
                    it.purchasedAt <
                    endExclusive
            }
            .sumOf {
                it.total
            }

    val purchaseReturnsInPeriod =
        purchaseReturns
            .filter {
                it.billId in
                    activeBillIds &&
                    it.returnedAt >=
                    startInclusive &&
                    it.returnedAt <
                    endExclusive
            }
            .sumOf {
                it.amount
            }

    val netPurchase =
        purchaseInPeriod -
            purchaseReturnsInPeriod

    val totalPurchaseCurrent =
        activeBills.sumOf {
            it.total
        }

    val totalPurchasePayments =
        purchasePayments
            .filter {
                it.billId in
                    activeBillIds
            }
            .sumOf {
                it.amount
            }

    val totalPurchaseReturns =
        purchaseReturns
            .filter {
                it.billId in
                    activeBillIds
            }
            .sumOf {
                it.amount
            }

    val supplierDue =
        (
            totalPurchaseCurrent -
                totalPurchaseReturns -
                totalPurchasePayments
            ).coerceAtLeast(
            0.0
        )

    val stockValue =
        products.sumOf {
            it.stockValue
        }

    val paymentsByMethod =
        salePayments
            .asSequence()
            .filter {
                it.saleId in
                    activeSaleIds &&
                    it.paidAt >=
                    startInclusive &&
                    it.paidAt <
                    endExclusive
            }
            .groupBy {
                it.paymentMethod
                    .trim()
                    .ifBlank {
                        "OTHER"
                    }
                    .uppercase()
            }
            .map {
                    (method, rows) ->

                BusinessReportPaymentMethod(
                    method = method,
                    amount =
                        rows.sumOf {
                            it.amount
                        }
                )
            }
            .sortedByDescending {
                it.amount
            }

    val accountMap =
        accounts.associateBy {
            it.id
        }

    val accountMovements =
        accountEntries
            .asSequence()
            .filter {
                it.createdAt >=
                    startInclusive &&
                    it.createdAt <
                    endExclusive
            }
            .filter {
                accountMap[
                    it.accountId
                ]?.type in setOf(
                    "CASH",
                    "BANK",
                    "MOBILE_WALLET"
                )
            }
            .groupBy {
                accountMap[
                    it.accountId
                ]?.type ?: "OTHER"
            }
            .map {
                    (type, rows) ->

                val moneyIn =
                    rows.sumOf {
                        row ->
                        row.balanceDelta
                            .coerceAtLeast(
                                0.0
                            )
                    }

                val moneyOut =
                    rows.sumOf {
                        row ->
                        (-row.balanceDelta)
                            .coerceAtLeast(
                                0.0
                            )
                    }

                BusinessReportAccountMovement(
                    accountType = type,
                    moneyIn = moneyIn,
                    moneyOut = moneyOut,
                    netMovement =
                        moneyIn -
                            moneyOut
                )
            }
            .sortedBy {
                it.accountType
            }

    data class ProductAccumulator(
        val productId: Long,
        val name: String,
        var quantity: Int = 0,
        var sales: Double = 0.0,
        var cost: Double = 0.0
    )

    val productTotals =
        linkedMapOf<Long, ProductAccumulator>()

    reportSaleLines.forEach {
            line ->

        val sale =
            reportSaleMap[
                line.saleId
            ] ?: return@forEach

        /*
         * Invoice discount is allocated proportionally so that
         * product revenue reconciles with sale.total.
         */
        val revenueFactor =
            if (
                sale.subtotal >
                0.0001
            ) {
                sale.total /
                    sale.subtotal
            } else {
                0.0
            }

        val lineRevenue =
            line.lineTotal *
                revenueFactor

        val lineCost =
            line.unitCost *
                line.quantity
                    .toDouble()

        val baseQuantity =
            if (
                line.baseQuantity >
                0
            ) {
                line.baseQuantity
            } else {
                line.quantity *
                    line.unitFactor
                        .coerceAtLeast(
                            1
                        )
            }

        val accumulator =
            productTotals.getOrPut(
                line.productId
            ) {
                ProductAccumulator(
                    productId =
                        line.productId,
                    name =
                        line.productNameSnapshot
                )
            }

        accumulator.quantity +=
            baseQuantity

        accumulator.sales +=
            lineRevenue

        accumulator.cost +=
            lineCost
    }

    val topProducts =
        productTotals
            .values
            .map {
                BusinessReportProduct(
                    productId =
                        it.productId,
                    name =
                        it.name,
                    quantity =
                        it.quantity,
                    sales =
                        it.sales,
                    cost =
                        it.cost,
                    grossProfit =
                        it.sales -
                            it.cost
                )
            }
            .sortedByDescending {
                it.sales
            }
            .take(10)

    return BusinessReportSummary(
        startInclusive =
            startInclusive,
        endExclusive =
            endExclusive,
        sales =
            salesRevenue,
        costOfGoodsSold =
            costOfGoodsSold,
        grossProfit =
            grossProfit,
        otherIncome =
            otherIncome,
        expense =
            expense,
        netProfit =
            netProfit,
        dueCollection =
            dueCollection,
        outstandingCustomerDue =
            outstandingCustomerDue,
        purchase =
            netPurchase,
        supplierDue =
            supplierDue,
        stockValue =
            stockValue,
        paymentMethods =
            paymentsByMethod,
        accountMovements =
            accountMovements,
        topProducts =
            topProducts
    )
}
