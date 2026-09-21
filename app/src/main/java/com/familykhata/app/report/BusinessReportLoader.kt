package com.familykhata.app.report

import android.content.Context
import com.familykhata.app.inventoryBusinessKey
import com.familykhata.app.data.AppDatabase
import com.familykhata.app.data.InventoryDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun loadBusinessReport(
    context: Context,
    businessId: String,
    businessType: String,
    startInclusive: Long,
    endExclusive: Long
): BusinessReportSummary =
    withContext(Dispatchers.IO) {
        require(businessId.isNotBlank()) {
            "Business not selected"
        }

        require(endExclusive > startInclusive) {
            "Invalid report date range"
        }

        val workspace = "SHOP"
        val businessKey =
            inventoryBusinessKey(
                businessId = businessId,
                shopType = businessType
            )

        val inventoryDao =
            InventoryDatabase
                .get(context.applicationContext)
                .dao()

        val ledgerDao =
            AppDatabase
                .get(context.applicationContext)
                .dao()

        val sales =
            inventoryDao
                .getAllRetailSales()
                .filter {
                    it.workspace == workspace &&
                        it.businessKey == businessKey
                }

        val saleIds =
            sales.mapTo(mutableSetOf()) {
                it.id
            }

        val saleLines =
            inventoryDao
                .getAllRetailSaleLines()
                .filter {
                    it.saleId in saleIds
                }

        val salePayments =
            inventoryDao
                .getAllRetailSalePayments()
                .filter {
                    it.saleId in saleIds
                }

        val purchaseBills =
            inventoryDao
                .getAllPurchaseBills()
                .filter {
                    it.workspace == workspace &&
                        it.businessKey == businessKey
                }

        val purchaseBillIds =
            purchaseBills.mapTo(mutableSetOf()) {
                it.id
            }

        val purchasePayments =
            inventoryDao
                .getAllPurchasePayments()
                .filter {
                    it.billId in purchaseBillIds
                }

        val purchaseReturns =
            inventoryDao
                .getAllPurchaseReturns()
                .filter {
                    it.billId in purchaseBillIds
                }

        val transactions =
            ledgerDao
                .getAllTransactions()
                .filter {
                    it.workspace == workspace &&
                        it.businessId == businessId
                }

        val accounts =
            ledgerDao
                .getAllFinancialAccounts()
                .filter {
                    it.workspace == workspace &&
                        it.businessId == businessId
                }

        val accountIds =
            accounts.mapTo(mutableSetOf()) {
                it.id
            }

        val accountEntries =
            ledgerDao
                .getAllFinancialAccountEntries()
                .filter {
                    it.accountId in accountIds
                }

        val products =
            inventoryDao
                .getAllProducts()
                .filter {
                    it.workspace == workspace &&
                        it.businessKey == businessKey
                }

        val productIds =
            products.mapTo(mutableSetOf()) {
                it.id
            }

        val stockValue =
            inventoryDao
                .getAllBatches()
                .asSequence()
                .filter {
                    it.productId in productIds &&
                        it.quantity > 0
                }
                .sumOf {
                    it.quantity.toDouble() *
                        it.purchasePrice
                }

        val people =
            ledgerDao
                .getAllPeople()
                .filter {
                    it.workspace == workspace &&
                        it.businessId == businessId
                }

        val personIds =
            people.mapTo(mutableSetOf()) {
                it.id
            }

        val bakiEntries =
            ledgerDao
                .getAllBakiEntries()
                .filter {
                    it.personId in personIds
                }

        val outstandingCustomerDue =
            bakiEntries
                .groupBy {
                    it.personId
                }
                .values
                .sumOf { entries ->
                    entries
                        .sumOf {
                            it.balanceDelta
                        }
                        .coerceAtLeast(0.0)
                }

        val manualDueCollection =
            bakiEntries
                .asSequence()
                .filter {
                    it.action == "RECEIVED_BACK" &&
                        it.createdAt >= startInclusive &&
                        it.createdAt < endExclusive &&
                        it.sourceKey
                            ?.startsWith(
                                "RETAIL_SALE_PAYMENT:"
                            ) != true
                }
                .sumOf {
                    it.amount
                }

        val report =
            buildBusinessReport(
                startInclusive = startInclusive,
                endExclusive = endExclusive,
                sales = sales,
                saleLines = saleLines,
                salePayments = salePayments,
                purchaseBills = purchaseBills,
                purchasePayments = purchasePayments,
                purchaseReturns = purchaseReturns,
                transactions = transactions,
                products = emptyList(),
                accounts = accounts,
                accountEntries = accountEntries
            )

        report.copy(
            dueCollection =
                report.dueCollection +
                    manualDueCollection,
            outstandingCustomerDue =
                outstandingCustomerDue,
            stockValue =
                stockValue
        )
    }
