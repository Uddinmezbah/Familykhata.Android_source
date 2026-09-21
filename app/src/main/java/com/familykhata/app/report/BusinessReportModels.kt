package com.familykhata.app.report

data class BusinessReportSummary(
    val startInclusive: Long,
    val endExclusive: Long,
    val sales: Double,
    val costOfGoodsSold: Double,
    val grossProfit: Double,
    val otherIncome: Double,
    val expense: Double,
    val netProfit: Double,
    val dueCollection: Double,
    val outstandingCustomerDue: Double,
    val purchase: Double,
    val supplierDue: Double,
    val stockValue: Double,
    val paymentMethods: List<BusinessReportPaymentMethod>,
    val accountMovements: List<BusinessReportAccountMovement>,
    val topProducts: List<BusinessReportProduct>
)

data class BusinessReportPaymentMethod(
    val method: String,
    val amount: Double
)

data class BusinessReportAccountMovement(
    val accountType: String,
    val moneyIn: Double,
    val moneyOut: Double,
    val netMovement: Double
)

data class BusinessReportProduct(
    val productId: Long,
    val name: String,
    val quantity: Int,
    val sales: Double,
    val cost: Double,
    val grossProfit: Double
)
