package com.familykhata.app.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.familykhata.app.data.ProductEntity
import com.familykhata.app.dealerbusiness.DealerAreaEntity
import com.familykhata.app.dealerbusiness.DealerBusinessViewModel
import com.familykhata.app.dealerbusiness.DealerCollectionEntity
import com.familykhata.app.dealerbusiness.DealerCompanyEntity
import com.familykhata.app.dealerbusiness.DealerCustomerEntity
import com.familykhata.app.dealerbusiness.DealerExpenseEntity
import com.familykhata.app.dealerbusiness.DealerPurchaseEntity
import com.familykhata.app.dealerbusiness.DealerPurchaseLineEntity
import com.familykhata.app.dealerbusiness.DealerPurchaseLineInput
import com.familykhata.app.dealerbusiness.DealerPurchaseReturnEntity
import com.familykhata.app.dealerbusiness.DealerSaleEntity
import com.familykhata.app.dealerbusiness.DealerSaleLineEntity
import com.familykhata.app.dealerbusiness.DealerSaleLineInput
import com.familykhata.app.dealerbusiness.DealerSalesReturnEntity
import com.familykhata.app.dealerbusiness.DealerSupplierPaymentEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun String.dealerBusinessNormalizeNumber(): String =
    buildString(length) {
        this@dealerBusinessNormalizeNumber.forEach { ch ->
            when (ch) {
                '০' -> append('0')
                '১' -> append('1')
                '২' -> append('2')
                '৩' -> append('3')
                '৪' -> append('4')
                '৫' -> append('5')
                '৬' -> append('6')
                '৭' -> append('7')
                '৮' -> append('8')
                '৯' -> append('9')
                else -> append(ch)
            }
        }
    }
        .trim()
        .replace(",", "")

private fun String.dealerBusinessDouble(): Double? =
    dealerBusinessNormalizeNumber()
        .toDoubleOrNull()

private fun String.dealerBusinessInt(): Int? =
    dealerBusinessNormalizeNumber()
        .toIntOrNull()

private fun dealerBusinessMoney(
    value: Double
): String =
    "${V14DisplayState.currencySymbol}${
        String.format(
            Locale.US,
            "%.2f",
            value
        )
    }"

private fun dealerBusinessDate(
    value: Long
): String =
    SimpleDateFormat(
        "yyyy-MM-dd",
        Locale.US
    ).format(Date(value))

private fun String.dealerBusinessDateMillis(): Long? {
    val formatter =
        SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        ).apply {
            isLenient = false
        }

    return runCatching {
        formatter
            .parse(trim())
            ?.time
    }.getOrNull()
}

@Composable
internal fun V16DealerBusinessScreen(
    workspace: String,
    shopType: String,
    canWrite: Boolean,
    onExit: () -> Unit
) {
    val vm: DealerBusinessViewModel = viewModel()

    val companies by vm.companies.collectAsState()
    val areas by vm.areas.collectAsState()
    val customers by vm.customers.collectAsState()
    val products by vm.products.collectAsState()
    val purchases by vm.purchases.collectAsState()
    val sales by vm.sales.collectAsState()
    val expenses by vm.expenses.collectAsState()
    val collections by vm.collections.collectAsState()
    val supplierPayments by
        vm.supplierPayments.collectAsState()

    var showInventory by remember {
        mutableStateOf(false)
    }

    var companyDialog by remember {
        mutableStateOf<DealerCompanyEntity?>(null)
    }

    var showNewCompany by remember {
        mutableStateOf(false)
    }

    var areaDialog by remember {
        mutableStateOf<DealerAreaEntity?>(null)
    }

    var showNewArea by remember {
        mutableStateOf(false)
    }

    var customerDialog by remember {
        mutableStateOf<DealerCustomerEntity?>(null)
    }

    var showNewCustomer by remember {
        mutableStateOf(false)
    }

    var showPurchase by remember {
        mutableStateOf(false)
    }

    var showSale by remember {
        mutableStateOf(false)
    }

    var showCollection by remember {
        mutableStateOf(false)
    }

    var showSupplierPayment by remember {
        mutableStateOf(false)
    }

    var showExpense by remember {
        mutableStateOf(false)
    }

    var editingExpense by remember {
        mutableStateOf<DealerExpenseEntity?>(null)
    }

    var selectedPurchase by remember {
        mutableStateOf<DealerPurchaseEntity?>(null)
    }

    var selectedSale by remember {
        mutableStateOf<DealerSaleEntity?>(null)
    }

    var editingPurchaseMeta by remember {
        mutableStateOf<DealerPurchaseEntity?>(null)
    }

    var editingSaleMeta by remember {
        mutableStateOf<DealerSaleEntity?>(null)
    }

    var editingCollection by remember {
        mutableStateOf<DealerCollectionEntity?>(null)
    }

    var editingSupplierPayment by remember {
        mutableStateOf<DealerSupplierPaymentEntity?>(null)
    }

    LaunchedEffect(
        workspace,
        shopType
    ) {
        vm.setBusinessContext(
            workspaceValue = workspace,
            shopType = shopType
        )
    }

    TrackV15DeepScreen(
        owner = "dealer-business",
        active =
            showInventory ||
                selectedPurchase != null ||
                selectedSale != null
    )

    if (showInventory) {
        V15InventoryScreen(
            workspace = workspace,
            shopType = "dealer_business",
            canWrite = canWrite,
            nestedEntry = true,
            onExit = {
                showInventory = false
            }
        )
        return
    }

    selectedSale?.let { sale ->
        V15DeepScreenContainer(
            title =
                v15Text(
                    "বিক্রয় ইনভয়েস",
                    "Sales invoice"
                ),
            onBack = {
                selectedSale = null
            }
        ) {
            DealerSaleDetail(
                sale = sale,
                viewModel = vm,
                canWrite = canWrite
            )
        }
        return
    }

    selectedPurchase?.let { purchase ->
        V15DeepScreenContainer(
            title =
                v15Text(
                    "ক্রয় ইনভয়েস",
                    "Purchase invoice"
                ),
            onBack = {
                selectedPurchase = null
            }
        ) {
            DealerPurchaseDetail(
                purchase = purchase,
                viewModel = vm,
                canWrite = canWrite
            )
        }
        return
    }

    BackHandler {
        onExit()
    }

    val totalExpense =
        expenses.sumOf {
            it.amount
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(bottom = 24.dp),
        verticalArrangement =
            Arrangement.spacedBy(10.dp)
    ) {
        Text(
            v15Text(
                "ডিলার ব্যবসা",
                "Dealer Business"
            ),
            style =
                MaterialTheme.typography.headlineSmall,
            fontWeight =
                FontWeight.ExtraBold
        )

        Text(
            v15Text(
                "কোম্পানি → ক্রয়/স্টক → রিটেইলার → বিক্রি → বকেয়া → কালেকশন → রিটার্ন → খরচ",
                "Company → purchase/stock → retailer → sale → due → collection → return → expense"
            ),
            style =
                MaterialTheme.typography.bodySmall
        )

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            DealerBusinessMetric(
                v15Text(
                    "কোম্পানি",
                    "Companies"
                ),
                companies.size.toString(),
                Modifier.weight(1f)
            )

            DealerBusinessMetric(
                v15Text(
                    "রিটেইলার",
                    "Retailers"
                ),
                customers.size.toString(),
                Modifier.weight(1f)
            )
        }

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            DealerBusinessMetric(
                v15Text(
                    "ক্রয়",
                    "Purchases"
                ),
                purchases.size.toString(),
                Modifier.weight(1f)
            )

            DealerBusinessMetric(
                v15Text(
                    "বিক্রি",
                    "Sales"
                ),
                sales.size.toString(),
                Modifier.weight(1f)
            )
        }

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            DealerBusinessMetric(
                v15Text(
                    "পণ্য",
                    "Products"
                ),
                products.size.toString(),
                Modifier.weight(1f)
            )

            DealerBusinessMetric(
                v15Text(
                    "খরচ",
                    "Expense"
                ),
                "${V14DisplayState.currencySymbol}${
                    String.format(
                        Locale.US,
                        "%.2f",
                        totalExpense
                    )
                }",
                Modifier.weight(1f)
            )
        }

        OutlinedButton(
            onClick = {
                showInventory = true
            },
            modifier =
                Modifier.fillMaxWidth()
        ) {
            Text(
                v15Text(
                    "পণ্য ও স্টক",
                    "Products & Stock"
                )
            )
        }

        if (canWrite) {
            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        companyDialog = null
                        showNewCompany = true
                    },
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text(
                        v15Text(
                            "+ কোম্পানি",
                            "+ Company"
                        )
                    )
                }

                OutlinedButton(
                    onClick = {
                        areaDialog = null
                        showNewArea = true
                    },
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text(
                        v15Text(
                            "+ এরিয়া",
                            "+ Area"
                        )
                    )
                }

                Button(
                    onClick = {
                        customerDialog = null
                        showNewCustomer = true
                    },
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text(
                        v15Text(
                            "+ রিটেইলার",
                            "+ Retailer"
                        )
                    )
                }
            }
        }

        Text(
            v15Text(
                "কোম্পানি / সাপ্লায়ার",
                "Companies / Suppliers"
            ),
            fontWeight =
                FontWeight.Bold
        )

        if (companies.isEmpty()) {
            Text(
                v15Text(
                    "এখনও কোনো কোম্পানি যোগ করা হয়নি।",
                    "No company added yet."
                )
            )
        }

        companies.forEach { item ->
            Card(
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier =
                        Modifier.padding(12.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        item.name,
                        fontWeight =
                            FontWeight.Bold
                    )

                    if (item.code.isNotBlank()) {
                        Text(
                            v15Text(
                                "কোড: ${item.code}",
                                "Code: ${item.code}"
                            )
                        )
                    }

                    if (item.phone.isNotBlank()) {
                        Text(
                            v15Text(
                                "ফোন: ${item.phone}",
                                "Phone: ${item.phone}"
                            )
                        )
                    }

                    if (canWrite) {
                        TextButton(
                            onClick = {
                                companyDialog = item
                            }
                        ) {
                            Text(
                                v15Text(
                                    "সম্পাদনা",
                                    "Edit"
                                )
                            )
                        }
                    }
                }
            }
        }

        Text(
            v15Text(
                "এরিয়া / রুট",
                "Areas / Routes"
            ),
            fontWeight =
                FontWeight.Bold
        )

        if (areas.isEmpty()) {
            Text(
                v15Text(
                    "এখনও কোনো এরিয়া যোগ করা হয়নি।",
                    "No area added yet."
                )
            )
        }

        areas.forEach { item ->
            Card(
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            item.name,
                            fontWeight =
                                FontWeight.Bold
                        )

                        if (item.code.isNotBlank()) {
                            Text(
                                v15Text(
                                    "কোড: ${item.code}",
                                    "Code: ${item.code}"
                                )
                            )
                        }
                    }

                    if (canWrite) {
                        TextButton(
                            onClick = {
                                areaDialog = item
                            }
                        ) {
                            Text(
                                v15Text(
                                    "সম্পাদনা",
                                    "Edit"
                                )
                            )
                        }
                    }
                }
            }
        }

        Text(
            v15Text(
                "রিটেইলার / কাস্টমার",
                "Retailers / Customers"
            ),
            fontWeight =
                FontWeight.Bold
        )

        if (customers.isEmpty()) {
            Text(
                v15Text(
                    "এখনও কোনো রিটেইলার যোগ করা হয়নি।",
                    "No retailer added yet."
                )
            )
        }

        customers.forEach { item ->
            val areaName =
                item.areaId?.let { areaId ->
                    areas.firstOrNull {
                        it.id == areaId
                    }?.name
                }

            Card(
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier =
                        Modifier.padding(12.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        item.name,
                        fontWeight =
                            FontWeight.Bold
                    )

                    if (
                        item.customerCode.isNotBlank()
                    ) {
                        Text(
                            v15Text(
                                "কোড: ${item.customerCode}",
                                "Code: ${item.customerCode}"
                            )
                        )
                    }

                    if (areaName != null) {
                        Text(
                            v15Text(
                                "এরিয়া: $areaName",
                                "Area: $areaName"
                            )
                        )
                    }

                    if (item.phone.isNotBlank()) {
                        Text(
                            v15Text(
                                "ফোন: ${item.phone}",
                                "Phone: ${item.phone}"
                            )
                        )
                    }

                    if (item.creditLimit > 0) {
                        Text(
                            v15Text(
                                "ক্রেডিট সীমা: ${V14DisplayState.currencySymbol}${
                                    String.format(
                                        Locale.US,
                                        "%.2f",
                                        item.creditLimit
                                    )
                                }",
                                "Credit limit: ${V14DisplayState.currencySymbol}${
                                    String.format(
                                        Locale.US,
                                        "%.2f",
                                        item.creditLimit
                                    )
                                }"
                            )
                        )
                    }

                    if (canWrite) {
                        TextButton(
                            onClick = {
                                customerDialog = item
                            }
                        ) {
                            Text(
                                v15Text(
                                    "সম্পাদনা",
                                    "Edit"
                                )
                            )
                        }
                    }
                }
            }
        }

        Text(
            v15Text(
                "লেনদেন",
                "Transactions"
            ),
            fontWeight =
                FontWeight.Bold
        )

        if (canWrite) {
            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = {
                        showPurchase = true
                    },
                    enabled =
                        companies.isNotEmpty() &&
                            products.isNotEmpty(),
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text(
                        v15Text(
                            "+ ক্রয়",
                            "+ Purchase"
                        )
                    )
                }

                Button(
                    onClick = {
                        showSale = true
                    },
                    enabled =
                        customers.isNotEmpty() &&
                            products.isNotEmpty(),
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text(
                        v15Text(
                            "+ বিক্রি",
                            "+ Sale"
                        )
                    )
                }
            }

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        showCollection = true
                    },
                    enabled =
                        customers.isNotEmpty(),
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text(
                        v15Text(
                            "কালেকশন",
                            "Collection"
                        )
                    )
                }

                OutlinedButton(
                    onClick = {
                        showSupplierPayment = true
                    },
                    enabled =
                        companies.isNotEmpty(),
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text(
                        v15Text(
                            "সাপ্লায়ার পেমেন্ট",
                            "Supplier payment"
                        )
                    )
                }
            }

            OutlinedButton(
                onClick = {
                    editingExpense = null
                    showExpense = true
                },
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Text(
                    v15Text(
                        "+ ব্যবসার খরচ",
                        "+ Business expense"
                    )
                )
            }
        }

        Text(
            v15Text(
                "ক্রয়ের ইতিহাস",
                "Purchase history"
            ),
            fontWeight =
                FontWeight.Bold
        )

        if (purchases.isEmpty()) {
            Text(
                v15Text(
                    "এখনও কোনো ক্রয় নেই।",
                    "No purchases yet."
                )
            )
        }

        purchases.forEach { purchase ->
            Card(
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier =
                        Modifier.padding(12.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        purchase.companyNameSnapshot
                            .ifBlank {
                                v15Text(
                                    "কোম্পানি",
                                    "Company"
                                )
                            },
                        fontWeight =
                            FontWeight.Bold
                    )

                    if (
                        purchase.invoiceNo.isNotBlank()
                    ) {
                        Text(
                            "Invoice: ${
                                purchase.invoiceNo
                            }"
                        )
                    }

                    Text(
                        v15Text(
                            "স্ট্যাটাস: ${purchase.status}",
                            "Status: ${purchase.status}"
                        )
                    )

                    OutlinedButton(
                        onClick = {
                            selectedPurchase =
                                purchase
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {
                        Text(
                            v15Text(
                                "বিস্তারিত / রিটার্ন",
                                "Details / Return"
                            )
                        )
                    }

                    if (canWrite) {
                        TextButton(
                            onClick = {
                                editingPurchaseMeta =
                                    purchase
                            }
                        ) {
                            Text(
                                v15Text(
                                    "ইনভয়েস তথ্য সম্পাদনা",
                                    "Edit invoice info"
                                )
                            )
                        }
                    }
                }
            }
        }

        Text(
            v15Text(
                "বিক্রয়ের ইতিহাস",
                "Sales history"
            ),
            fontWeight =
                FontWeight.Bold
        )

        if (sales.isEmpty()) {
            Text(
                v15Text(
                    "এখনও কোনো বিক্রি নেই।",
                    "No sales yet."
                )
            )
        }

        sales.forEach { sale ->
            Card(
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier =
                        Modifier.padding(12.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        sale.customerNameSnapshot
                            .ifBlank {
                                v15Text(
                                    "রিটেইলার",
                                    "Retailer"
                                )
                            },
                        fontWeight =
                            FontWeight.Bold
                    )

                    if (
                        sale.invoiceNo.isNotBlank()
                    ) {
                        Text(
                            "Invoice: ${
                                sale.invoiceNo
                            }"
                        )
                    }

                    Text(
                        v15Text(
                            "স্ট্যাটাস: ${sale.status}",
                            "Status: ${sale.status}"
                        )
                    )

                    OutlinedButton(
                        onClick = {
                            selectedSale = sale
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {
                        Text(
                            v15Text(
                                "বিস্তারিত / রিটার্ন",
                                "Details / Return"
                            )
                        )
                    }

                    if (canWrite) {
                        TextButton(
                            onClick = {
                                editingSaleMeta =
                                    sale
                            }
                        ) {
                            Text(
                                v15Text(
                                    "ইনভয়েস তথ্য সম্পাদনা",
                                    "Edit invoice info"
                                )
                            )
                        }
                    }
                }
            }
        }

        Text(
            v15Text(
                "কালেকশন ইতিহাস",
                "Collection history"
            ),
            fontWeight =
                FontWeight.Bold
        )

        if (collections.isEmpty()) {
            Text(
                v15Text(
                    "এখনও কোনো কালেকশন নেই।",
                    "No collections yet."
                )
            )
        }

        collections.forEach { item ->
            Card(
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier =
                        Modifier.padding(12.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        item.customerNameSnapshot,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        dealerBusinessMoney(
                            item.amount
                        )
                    )

                    Text(
                        dealerBusinessDate(
                            item.collectedAt
                        )
                    )

                    if (item.note.isNotBlank()) {
                        Text(
                            item.note,
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall
                        )
                    }

                    if (canWrite) {
                        TextButton(
                            onClick = {
                                editingCollection =
                                    item
                                showCollection = true
                            }
                        ) {
                            Text(
                                v15Text(
                                    "সংশোধন",
                                    "Correct"
                                )
                            )
                        }
                    }
                }
            }
        }

        Text(
            v15Text(
                "সাপ্লায়ার পেমেন্ট ইতিহাস",
                "Supplier payment history"
            ),
            fontWeight =
                FontWeight.Bold
        )

        if (supplierPayments.isEmpty()) {
            Text(
                v15Text(
                    "এখনও কোনো সাপ্লায়ার পেমেন্ট নেই।",
                    "No supplier payments yet."
                )
            )
        }

        supplierPayments.forEach { item ->
            Card(
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier =
                        Modifier.padding(12.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        item.companyNameSnapshot,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        dealerBusinessMoney(
                            item.amount
                        )
                    )

                    Text(
                        dealerBusinessDate(
                            item.paidAt
                        )
                    )

                    if (item.note.isNotBlank()) {
                        Text(
                            item.note,
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall
                        )
                    }

                    if (canWrite) {
                        TextButton(
                            onClick = {
                                editingSupplierPayment =
                                    item
                                showSupplierPayment =
                                    true
                            }
                        ) {
                            Text(
                                v15Text(
                                    "সংশোধন",
                                    "Correct"
                                )
                            )
                        }
                    }
                }
            }
        }

        Text(
            v15Text(
                "ব্যবসার খরচ",
                "Business expenses"
            ),
            fontWeight =
                FontWeight.Bold
        )

        if (expenses.isEmpty()) {
            Text(
                v15Text(
                    "এখনও কোনো খরচ নেই।",
                    "No expenses yet."
                )
            )
        }

        expenses.forEach { expense ->
            Card(
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            expense.category,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            dealerBusinessMoney(
                                expense.amount
                            )
                        )

                        if (
                            expense.note.isNotBlank()
                        ) {
                            Text(
                                expense.note,
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodySmall
                            )
                        }
                    }

                    if (canWrite) {
                        TextButton(
                            onClick = {
                                editingExpense =
                                    expense
                                showExpense =
                                    true
                            }
                        ) {
                            Text(
                                v15Text(
                                    "সম্পাদনা",
                                    "Edit"
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    if (
        showNewCompany ||
        companyDialog != null
    ) {
        DealerCompanyDialog(
            initial = companyDialog,
            onDismiss = {
                showNewCompany = false
                companyDialog = null
            },
            onSave = {
                    name,
                    code,
                    phone,
                    contact,
                    address,
                    note ->

                val editing =
                    companyDialog

                if (editing == null) {
                    vm.addCompany(
                        name,
                        code,
                        phone,
                        contact,
                        address,
                        note
                    ) {
                        if (it) {
                            showNewCompany = false
                        }
                    }
                } else {
                    vm.updateCompany(
                        editing,
                        name,
                        code,
                        phone,
                        contact,
                        address,
                        note
                    ) {
                        if (it) {
                            companyDialog = null
                        }
                    }
                }
            }
        )
    }

    if (
        showNewArea ||
        areaDialog != null
    ) {
        DealerAreaDialog(
            initial = areaDialog,
            onDismiss = {
                showNewArea = false
                areaDialog = null
            },
            onSave = {
                    name,
                    code,
                    note ->

                val editing =
                    areaDialog

                if (editing == null) {
                    vm.addArea(
                        name,
                        code,
                        note
                    ) {
                        if (it) {
                            showNewArea = false
                        }
                    }
                } else {
                    vm.updateArea(
                        editing,
                        name,
                        code,
                        note
                    ) {
                        if (it) {
                            areaDialog = null
                        }
                    }
                }
            }
        )
    }

    if (
        showNewCustomer ||
        customerDialog != null
    ) {
        DealerCustomerDialog(
            initial = customerDialog,
            areas = areas,
            onDismiss = {
                showNewCustomer = false
                customerDialog = null
            },
            onSave = {
                    areaId,
                    name,
                    code,
                    phone,
                    address,
                    creditLimit,
                    note ->

                val editing =
                    customerDialog

                if (editing == null) {
                    vm.addCustomer(
                        areaId,
                        name,
                        code,
                        phone,
                        address,
                        creditLimit,
                        note
                    ) {
                        if (it) {
                            showNewCustomer = false
                        }
                    }
                } else {
                    vm.updateCustomer(
                        editing,
                        areaId,
                        name,
                        code,
                        phone,
                        address,
                        creditLimit,
                        note
                    ) {
                        if (it) {
                            customerDialog = null
                        }
                    }
                }
            }
        )
    }

    if (showPurchase) {
        DealerPurchaseDialog(
            companies = companies,
            products = products,
            onDismiss = {
                showPurchase = false
            },
            onSave = {
                    companyId,
                    invoiceNo,
                    lines,
                    paidNow,
                    note ->

                vm.createPurchase(
                    companyId = companyId,
                    invoiceNo = invoiceNo,
                    lines = lines,
                    paidNow = paidNow,
                    note = note
                ) {
                    if (it) {
                        showPurchase = false
                    }
                }
            }
        )
    }

    if (showSale) {
        DealerSaleDialog(
            customers = customers,
            products = products,
            onDismiss = {
                showSale = false
            },
            onSave = {
                    customerId,
                    invoiceNo,
                    lines,
                    collectedNow,
                    note ->

                vm.createSale(
                    customerId = customerId,
                    invoiceNo = invoiceNo,
                    lines = lines,
                    collectedNow =
                        collectedNow,
                    note = note
                ) {
                    if (it) {
                        showSale = false
                    }
                }
            }
        )
    }

    if (showCollection) {
        DealerCollectionDialog(
            initial = editingCollection,
            customers = customers,
            onDismiss = {
                showCollection = false
                editingCollection = null
            },
            onSave = {
                    customerId,
                    amount,
                    collectedAt,
                    note ->

                val editing =
                    editingCollection

                if (editing == null) {
                    vm.addCollection(
                        customerId = customerId,
                        amount = amount,
                        collectedAt =
                            collectedAt,
                        note = note
                    ) {
                        if (it) {
                            showCollection =
                                false
                        }
                    }
                } else {
                    vm.correctCollection(
                        item = editing,
                        customerId =
                            customerId,
                        amount = amount,
                        collectedAt =
                            collectedAt,
                        note = note
                    ) {
                        if (it) {
                            showCollection =
                                false
                            editingCollection =
                                null
                        }
                    }
                }
            }
        )
    }

    if (showSupplierPayment) {
        DealerSupplierPaymentDialog(
            initial =
                editingSupplierPayment,
            companies = companies,
            onDismiss = {
                showSupplierPayment = false
                editingSupplierPayment = null
            },
            onSave = {
                    companyId,
                    amount,
                    paidAt,
                    note ->

                val editing =
                    editingSupplierPayment

                if (editing == null) {
                    vm.addSupplierPayment(
                        companyId = companyId,
                        amount = amount,
                        paidAt = paidAt,
                        note = note
                    ) {
                        if (it) {
                            showSupplierPayment =
                                false
                        }
                    }
                } else {
                    vm.correctSupplierPayment(
                        item = editing,
                        companyId = companyId,
                        amount = amount,
                        paidAt = paidAt,
                        note = note
                    ) {
                        if (it) {
                            showSupplierPayment =
                                false
                            editingSupplierPayment =
                                null
                        }
                    }
                }
            }
        )
    }

    editingPurchaseMeta?.let { item ->
        DealerTransactionMetaDialog(
            title =
                v15Text(
                    "ক্রয় ইনভয়েস তথ্য",
                    "Purchase invoice info"
                ),
            invoiceNo = item.invoiceNo,
            date = item.purchasedAt,
            note = item.note,
            onDismiss = {
                editingPurchaseMeta = null
            },
            onSave = {
                    invoiceNo,
                    date,
                    note ->

                vm.updatePurchaseMeta(
                    item = item,
                    invoiceNo = invoiceNo,
                    purchasedAt = date,
                    note = note
                ) {
                    if (it) {
                        editingPurchaseMeta =
                            null
                    }
                }
            }
        )
    }

    editingSaleMeta?.let { item ->
        DealerTransactionMetaDialog(
            title =
                v15Text(
                    "বিক্রয় ইনভয়েস তথ্য",
                    "Sales invoice info"
                ),
            invoiceNo = item.invoiceNo,
            date = item.soldAt,
            note = item.note,
            onDismiss = {
                editingSaleMeta = null
            },
            onSave = {
                    invoiceNo,
                    date,
                    note ->

                vm.updateSaleMeta(
                    item = item,
                    invoiceNo = invoiceNo,
                    soldAt = date,
                    note = note
                ) {
                    if (it) {
                        editingSaleMeta = null
                    }
                }
            }
        )
    }

    if (showExpense) {
        DealerExpenseDialog(
            initial = editingExpense,
            onDismiss = {
                showExpense = false
                editingExpense = null
            },
            onSave = {
                    category,
                    amount,
                    note ->

                val editing =
                    editingExpense

                if (editing == null) {
                    vm.addExpense(
                        category = category,
                        amount = amount,
                        note = note
                    ) {
                        if (it) {
                            showExpense = false
                        }
                    }
                } else {
                    vm.updateExpense(
                        item = editing,
                        category = category,
                        amount = amount,
                        note = note
                    ) {
                        if (it) {
                            showExpense = false
                            editingExpense = null
                        }
                    }
                }
            }
        )
    }

}

@Composable
private fun DealerSaleDetail(
    sale: DealerSaleEntity,
    viewModel: DealerBusinessViewModel,
    canWrite: Boolean
) {
    val context =
        androidx.compose.ui.platform
            .LocalContext.current

    val flow =
        remember(sale.id) {
            viewModel.observeSaleLines(
                sale.id
            )
        }

    val lines by
        flow.collectAsState(
            initial = emptyList()
        )

    val returnFlow =
        remember(sale.id) {
            viewModel.observeSalesReturns(
                sale.id
            )
        }

    val returns by
        returnFlow.collectAsState(
            initial = emptyList()
        )

    var returnLine by remember {
        mutableStateOf<DealerSaleLineEntity?>(
            null
        )
    }

    var editingReturn by remember {
        mutableStateOf<DealerSalesReturnEntity?>(
            null
        )
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(bottom = 24.dp),
        verticalArrangement =
            Arrangement.spacedBy(10.dp)
    ) {
        Text(
            sale.customerNameSnapshot,
            style =
                MaterialTheme.typography
                    .titleLarge,
            fontWeight =
                FontWeight.ExtraBold
        )

        if (sale.invoiceNo.isNotBlank()) {
            Text(
                "Invoice: ${sale.invoiceNo}"
            )
        }

        Text(
            v15Text(
                "স্ট্যাটাস: ${sale.status}",
                "Status: ${sale.status}"
            )
        )

        val total =
            lines.sumOf {
                it.lineTotal
            }

        Text(
            v15Text(
                "মূল বিক্রয়: ${
                    dealerBusinessMoney(total)
                }",
                "Original sale: ${
                    dealerBusinessMoney(total)
                }"
            ),
            fontWeight =
                FontWeight.Bold
        )

        lines.forEach { line ->
            Card(
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier =
                        Modifier.padding(12.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        line.productNameSnapshot,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        v15Text(
                            "পরিমাণ: ${line.quantity}",
                            "Quantity: ${line.quantity}"
                        )
                    )

                    Text(
                        v15Text(
                            "দর: ${
                                dealerBusinessMoney(
                                    line.unitPrice
                                )
                            }",
                            "Rate: ${
                                dealerBusinessMoney(
                                    line.unitPrice
                                )
                            }"
                        )
                    )

                    Text(
                        v15Text(
                            "মোট: ${
                                dealerBusinessMoney(
                                    line.lineTotal
                                )
                            }",
                            "Total: ${
                                dealerBusinessMoney(
                                    line.lineTotal
                                )
                            }"
                        )
                    )

                    if (canWrite) {
                        OutlinedButton(
                            onClick = {
                                returnLine = line
                            },
                            modifier =
                                Modifier.fillMaxWidth()
                        ) {
                            Text(
                                v15Text(
                                    "রিটার্ন / ড্যামেজ",
                                    "Return / Damage"
                                )
                            )
                        }
                    }
                }
            }
        }

        Text(
            v15Text(
                "রিটার্ন / ড্যামেজ ইতিহাস",
                "Return / damage history"
            ),
            fontWeight =
                FontWeight.Bold
        )

        if (returns.isEmpty()) {
            Text(
                v15Text(
                    "কোনো রিটার্ন নেই।",
                    "No returns."
                )
            )
        }

        returns.forEach { item ->
            Card(
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier =
                        Modifier.padding(10.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        item.productNameSnapshot,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        v15Text(
                            "পরিমাণ: ${item.quantity} • ${item.returnType}",
                            "Quantity: ${item.quantity} • ${item.returnType}"
                        )
                    )

                    Text(
                        dealerBusinessDate(
                            item.returnedAt
                        )
                    )

                    if (item.note.isNotBlank()) {
                        Text(item.note)
                    }

                    if (canWrite) {
                        TextButton(
                            onClick = {
                                editingReturn = item
                            }
                        ) {
                            Text(
                                v15Text(
                                    "নোট/তারিখ সম্পাদনা",
                                    "Edit note/date"
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    returnLine?.let { line ->
        DealerSalesReturnDialog(
            line = line,
            onDismiss = {
                returnLine = null
            },
            onSave = {
                    quantity,
                    type,
                    note ->

                viewModel.recordSalesReturn(
                    saleLineId = line.id,
                    quantity = quantity,
                    returnType = type,
                    note = note
                ) { ok ->
                    Toast.makeText(
                        context,
                        if (ok) {
                            v15Text(
                                "রিটার্ন সংরক্ষণ হয়েছে",
                                "Return saved"
                            )
                        } else {
                            v15Text(
                                "রিটার্ন করা যায়নি",
                                "Return failed"
                            )
                        },
                        Toast.LENGTH_SHORT
                    ).show()

                    if (ok) {
                        returnLine = null
                    }
                }
            }
        )
    }

    editingReturn?.let { item ->
        DealerReturnMetaDialog(
            initialDate =
                item.returnedAt,
            initialNote =
                item.note,
            onDismiss = {
                editingReturn = null
            },
            onSave = {
                    date,
                    note ->

                viewModel.updateSalesReturnMeta(
                    item = item,
                    returnedAt = date,
                    note = note
                ) {
                    if (it) {
                        editingReturn = null
                    }
                }
            }
        )
    }
}

@Composable
private fun DealerPurchaseDetail(
    purchase: DealerPurchaseEntity,
    viewModel: DealerBusinessViewModel,
    canWrite: Boolean
) {
    val context =
        androidx.compose.ui.platform
            .LocalContext.current

    val flow =
        remember(purchase.id) {
            viewModel.observePurchaseLines(
                purchase.id
            )
        }

    val lines by
        flow.collectAsState(
            initial = emptyList()
        )

    val returnFlow =
        remember(purchase.id) {
            viewModel.observePurchaseReturns(
                purchase.id
            )
        }

    val returns by
        returnFlow.collectAsState(
            initial = emptyList()
        )

    var returnLine by remember {
        mutableStateOf<DealerPurchaseLineEntity?>(
            null
        )
    }

    var editingReturn by remember {
        mutableStateOf<DealerPurchaseReturnEntity?>(
            null
        )
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(bottom = 24.dp),
        verticalArrangement =
            Arrangement.spacedBy(10.dp)
    ) {
        Text(
            purchase.companyNameSnapshot,
            style =
                MaterialTheme.typography
                    .titleLarge,
            fontWeight =
                FontWeight.ExtraBold
        )

        if (
            purchase.invoiceNo.isNotBlank()
        ) {
            Text(
                "Invoice: ${
                    purchase.invoiceNo
                }"
            )
        }

        Text(
            v15Text(
                "স্ট্যাটাস: ${purchase.status}",
                "Status: ${purchase.status}"
            )
        )

        val total =
            lines.sumOf {
                it.lineTotal
            }

        Text(
            v15Text(
                "মূল ক্রয়: ${
                    dealerBusinessMoney(total)
                }",
                "Original purchase: ${
                    dealerBusinessMoney(total)
                }"
            ),
            fontWeight =
                FontWeight.Bold
        )

        lines.forEach { line ->
            Card(
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier =
                        Modifier.padding(12.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        line.productNameSnapshot,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        v15Text(
                            "পরিমাণ: ${line.quantity}",
                            "Quantity: ${line.quantity}"
                        )
                    )

                    Text(
                        v15Text(
                            "ক্রয় দর: ${
                                dealerBusinessMoney(
                                    line.unitCost
                                )
                            }",
                            "Purchase rate: ${
                                dealerBusinessMoney(
                                    line.unitCost
                                )
                            }"
                        )
                    )

                    if (
                        line.batchNoSnapshot
                            .isNotBlank()
                    ) {
                        Text(
                            "Batch: ${
                                line.batchNoSnapshot
                            }"
                        )
                    }

                    if (canWrite) {
                        OutlinedButton(
                            onClick = {
                                returnLine = line
                            },
                            modifier =
                                Modifier.fillMaxWidth()
                        ) {
                            Text(
                                v15Text(
                                    "সাপ্লায়ারকে রিটার্ন",
                                    "Return to supplier"
                                )
                            )
                        }
                    }
                }
            }
        }

        Text(
            v15Text(
                "পারচেজ রিটার্ন ইতিহাস",
                "Purchase return history"
            ),
            fontWeight =
                FontWeight.Bold
        )

        if (returns.isEmpty()) {
            Text(
                v15Text(
                    "কোনো পারচেজ রিটার্ন নেই।",
                    "No purchase returns."
                )
            )
        }

        returns.forEach { item ->
            Card(
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier =
                        Modifier.padding(10.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        item.productNameSnapshot,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        v15Text(
                            "পরিমাণ: ${item.quantity}",
                            "Quantity: ${item.quantity}"
                        )
                    )

                    Text(
                        dealerBusinessDate(
                            item.returnedAt
                        )
                    )

                    if (item.note.isNotBlank()) {
                        Text(item.note)
                    }

                    if (canWrite) {
                        TextButton(
                            onClick = {
                                editingReturn = item
                            }
                        ) {
                            Text(
                                v15Text(
                                    "নোট/তারিখ সম্পাদনা",
                                    "Edit note/date"
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    returnLine?.let { line ->
        DealerPurchaseReturnDialog(
            line = line,
            onDismiss = {
                returnLine = null
            },
            onSave = {
                    quantity,
                    note ->

                viewModel.recordPurchaseReturn(
                    purchaseLineId =
                        line.id,
                    quantity = quantity,
                    note = note
                ) { ok ->
                    Toast.makeText(
                        context,
                        if (ok) {
                            v15Text(
                                "পারচেজ রিটার্ন সংরক্ষণ হয়েছে",
                                "Purchase return saved"
                            )
                        } else {
                            v15Text(
                                "রিটার্ন করা যায়নি। স্টকে পর্যাপ্ত পণ্য আছে কিনা দেখুন।",
                                "Return failed. Check available stock."
                            )
                        },
                        Toast.LENGTH_SHORT
                    ).show()

                    if (ok) {
                        returnLine = null
                    }
                }
            }
        )
    }

    editingReturn?.let { item ->
        DealerReturnMetaDialog(
            initialDate =
                item.returnedAt,
            initialNote =
                item.note,
            onDismiss = {
                editingReturn = null
            },
            onSave = {
                    date,
                    note ->

                viewModel.updatePurchaseReturnMeta(
                    item = item,
                    returnedAt = date,
                    note = note
                ) {
                    if (it) {
                        editingReturn = null
                    }
                }
            }
        )
    }
}

@Composable
private fun DealerPurchaseDialog(
    companies: List<DealerCompanyEntity>,
    products: List<ProductEntity>,
    onDismiss: () -> Unit,
    onSave: (
        Long,
        String,
        List<DealerPurchaseLineInput>,
        Double,
        String
    ) -> Unit
) {
    var companyId by remember {
        mutableStateOf<Long?>(null)
    }

    var invoiceNo by remember {
        mutableStateOf("")
    }

    var productId by remember {
        mutableStateOf<Long?>(null)
    }

    var quantity by remember {
        mutableStateOf("")
    }

    var cost by remember {
        mutableStateOf("")
    }

    var batch by remember {
        mutableStateOf("")
    }

    var paidNow by remember {
        mutableStateOf("")
    }

    var note by remember {
        mutableStateOf("")
    }

    val lines =
        remember {
            mutableStateListOf<
                DealerPurchaseLineInput
            >()
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                v15Text(
                    "নতুন ক্রয়",
                    "New purchase"
                )
            )
        },
        text = {
            Column(
                modifier =
                    Modifier.verticalScroll(
                        rememberScrollState()
                    ),
                verticalArrangement =
                    Arrangement.spacedBy(7.dp)
            ) {
                Text(
                    v15Text(
                        "কোম্পানি নির্বাচন",
                        "Select company"
                    ),
                    fontWeight =
                        FontWeight.Bold
                )

                companies.forEach { company ->
                    OutlinedButton(
                        onClick = {
                            companyId =
                                company.id
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (
                                companyId ==
                                    company.id
                            ) {
                                "✓ ${company.name}"
                            } else {
                                company.name
                            }
                        )
                    }
                }

                OutlinedTextField(
                    invoiceNo,
                    { invoiceNo = it },
                    label = {
                        Text(
                            v15Text(
                                "ইনভয়েস নম্বর",
                                "Invoice number"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                Text(
                    v15Text(
                        "পণ্য নির্বাচন",
                        "Select product"
                    ),
                    fontWeight =
                        FontWeight.Bold
                )

                products.forEach { product ->
                    OutlinedButton(
                        onClick = {
                            productId =
                                product.id

                            if (
                                cost.isBlank()
                            ) {
                                cost =
                                    product.sellingPrice
                                        .takeIf {
                                            it > 0
                                        }
                                        ?.toString()
                                        .orEmpty()
                            }
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (
                                productId ==
                                    product.id
                            ) {
                                "✓ ${product.name}"
                            } else {
                                product.name
                            }
                        )
                    }
                }

                OutlinedTextField(
                    quantity,
                    { quantity = it },
                    label = {
                        Text(
                            v15Text(
                                "পরিমাণ",
                                "Quantity"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    cost,
                    { cost = it },
                    label = {
                        Text(
                            v15Text(
                                "ক্রয় দর / ইউনিট",
                                "Purchase rate / unit"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    batch,
                    { batch = it },
                    label = {
                        Text(
                            v15Text(
                                "ব্যাচ / লট",
                                "Batch / lot"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedButton(
                    onClick = {
                        val p = productId
                        val q =
                            quantity
                                .dealerBusinessInt()
                        val c =
                            cost
                                .dealerBusinessDouble()

                        if (
                            p != null &&
                            q != null &&
                            q > 0 &&
                            c != null &&
                            c >= 0
                        ) {
                            lines +=
                                DealerPurchaseLineInput(
                                    productId = p,
                                    quantity = q,
                                    unitCost = c,
                                    batchNo =
                                        batch.trim()
                                )

                            productId = null
                            quantity = ""
                            cost = ""
                            batch = ""
                        }
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text(
                        v15Text(
                            "+ পণ্য লাইনে যোগ করুন",
                            "+ Add product line"
                        )
                    )
                }

                lines.forEachIndexed {
                        index,
                        line ->

                    val name =
                        products.firstOrNull {
                            it.id ==
                                line.productId
                        }?.name
                            ?: "#${line.productId}"

                    Card(
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                            horizontalArrangement =
                                Arrangement.SpaceBetween
                        ) {
                            Text(
                                "$name × ${
                                    line.quantity
                                } = ${
                                    dealerBusinessMoney(
                                        line.quantity *
                                            line.unitCost
                                    )
                                }"
                            )

                            TextButton(
                                onClick = {
                                    lines.removeAt(
                                        index
                                    )
                                }
                            ) {
                                Text("×")
                            }
                        }
                    }
                }

                OutlinedTextField(
                    paidNow,
                    { paidNow = it },
                    label = {
                        Text(
                            v15Text(
                                "এখন পেমেন্ট",
                                "Paid now"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    note,
                    { note = it },
                    label = {
                        Text(
                            v15Text(
                                "নোট",
                                "Note"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val company =
                        companyId

                    val paid =
                        paidNow
                            .dealerBusinessDouble()
                            ?: 0.0

                    if (
                        company != null &&
                        lines.isNotEmpty() &&
                        paid >= 0
                    ) {
                        onSave(
                            company,
                            invoiceNo,
                            lines.toList(),
                            paid,
                            note
                        )
                    }
                }
            ) {
                Text(
                    v15Text(
                        "ক্রয় সেভ",
                        "Save purchase"
                    )
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    v15Text(
                        "বাতিল",
                        "Cancel"
                    )
                )
            }
        }
    )
}

@Composable
private fun DealerSaleDialog(
    customers: List<DealerCustomerEntity>,
    products: List<ProductEntity>,
    onDismiss: () -> Unit,
    onSave: (
        Long,
        String,
        List<DealerSaleLineInput>,
        Double,
        String
    ) -> Unit
) {
    var customerId by remember {
        mutableStateOf<Long?>(null)
    }

    var invoiceNo by remember {
        mutableStateOf("")
    }

    var productId by remember {
        mutableStateOf<Long?>(null)
    }

    var quantity by remember {
        mutableStateOf("")
    }

    var price by remember {
        mutableStateOf("")
    }

    var collectedNow by remember {
        mutableStateOf("")
    }

    var note by remember {
        mutableStateOf("")
    }

    val lines =
        remember {
            mutableStateListOf<
                DealerSaleLineInput
            >()
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                v15Text(
                    "নতুন বিক্রি",
                    "New sale"
                )
            )
        },
        text = {
            Column(
                modifier =
                    Modifier.verticalScroll(
                        rememberScrollState()
                    ),
                verticalArrangement =
                    Arrangement.spacedBy(7.dp)
            ) {
                Text(
                    v15Text(
                        "রিটেইলার নির্বাচন",
                        "Select retailer"
                    ),
                    fontWeight =
                        FontWeight.Bold
                )

                customers.forEach { customer ->
                    OutlinedButton(
                        onClick = {
                            customerId =
                                customer.id
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (
                                customerId ==
                                    customer.id
                            ) {
                                "✓ ${customer.name}"
                            } else {
                                customer.name
                            }
                        )
                    }
                }

                OutlinedTextField(
                    invoiceNo,
                    { invoiceNo = it },
                    label = {
                        Text(
                            v15Text(
                                "ইনভয়েস নম্বর",
                                "Invoice number"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                Text(
                    v15Text(
                        "পণ্য নির্বাচন",
                        "Select product"
                    ),
                    fontWeight =
                        FontWeight.Bold
                )

                products.forEach { product ->
                    OutlinedButton(
                        onClick = {
                            productId =
                                product.id

                            if (
                                product.sellingPrice >
                                    0
                            ) {
                                price =
                                    product.sellingPrice
                                        .toString()
                            }
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (
                                productId ==
                                    product.id
                            ) {
                                "✓ ${product.name}"
                            } else {
                                product.name
                            }
                        )
                    }
                }

                OutlinedTextField(
                    quantity,
                    { quantity = it },
                    label = {
                        Text(
                            v15Text(
                                "পরিমাণ",
                                "Quantity"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    price,
                    { price = it },
                    label = {
                        Text(
                            v15Text(
                                "বিক্রয় দর / ইউনিট",
                                "Selling price / unit"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedButton(
                    onClick = {
                        val p = productId
                        val q =
                            quantity
                                .dealerBusinessInt()
                        val value =
                            price
                                .dealerBusinessDouble()

                        if (
                            p != null &&
                            q != null &&
                            q > 0 &&
                            value != null &&
                            value >= 0
                        ) {
                            lines +=
                                DealerSaleLineInput(
                                    productId = p,
                                    quantity = q,
                                    unitPrice = value
                                )

                            productId = null
                            quantity = ""
                            price = ""
                        }
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text(
                        v15Text(
                            "+ বিক্রয় লাইনে যোগ করুন",
                            "+ Add sale line"
                        )
                    )
                }

                lines.forEachIndexed {
                        index,
                        line ->

                    val name =
                        products.firstOrNull {
                            it.id ==
                                line.productId
                        }?.name
                            ?: "#${line.productId}"

                    Card(
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                            horizontalArrangement =
                                Arrangement.SpaceBetween
                        ) {
                            Text(
                                "$name × ${
                                    line.quantity
                                } = ${
                                    dealerBusinessMoney(
                                        line.quantity *
                                            line.unitPrice
                                    )
                                }"
                            )

                            TextButton(
                                onClick = {
                                    lines.removeAt(
                                        index
                                    )
                                }
                            ) {
                                Text("×")
                            }
                        }
                    }
                }

                OutlinedTextField(
                    collectedNow,
                    { collectedNow = it },
                    label = {
                        Text(
                            v15Text(
                                "এখন আদায়",
                                "Collected now"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    note,
                    { note = it },
                    label = {
                        Text(
                            v15Text(
                                "নোট",
                                "Note"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val customer =
                        customerId

                    val collected =
                        collectedNow
                            .dealerBusinessDouble()
                            ?: 0.0

                    if (
                        customer != null &&
                        lines.isNotEmpty() &&
                        collected >= 0
                    ) {
                        onSave(
                            customer,
                            invoiceNo,
                            lines.toList(),
                            collected,
                            note
                        )
                    }
                }
            ) {
                Text(
                    v15Text(
                        "বিক্রি সেভ",
                        "Save sale"
                    )
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    v15Text(
                        "বাতিল",
                        "Cancel"
                    )
                )
            }
        }
    )
}

@Composable
private fun DealerCollectionDialog(
    initial: DealerCollectionEntity?,
    customers: List<DealerCustomerEntity>,
    onDismiss: () -> Unit,
    onSave: (
        Long,
        Double,
        Long,
        String
    ) -> Unit
) {
    var customerId by remember(initial?.id) {
        mutableStateOf(initial?.customerId)
    }

    var amount by remember(initial?.id) {
        mutableStateOf(
            initial
                ?.amount
                ?.toString()
                .orEmpty()
        )
    }

    var date by remember(initial?.id) {
        mutableStateOf(
            dealerBusinessDate(
                initial?.collectedAt
                    ?: System.currentTimeMillis()
            )
        )
    }

    var note by remember(initial?.id) {
        mutableStateOf(
            initial?.note.orEmpty()
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initial == null) {
                    v15Text(
                        "কাস্টমার কালেকশন",
                        "Customer collection"
                    )
                } else {
                    v15Text(
                        "কালেকশন সংশোধন",
                        "Correct collection"
                    )
                }
            )
        },
        text = {
            Column(
                modifier =
                    Modifier.verticalScroll(
                        rememberScrollState()
                    ),
                verticalArrangement =
                    Arrangement.spacedBy(7.dp)
            ) {
                customers.forEach { customer ->
                    OutlinedButton(
                        onClick = {
                            customerId =
                                customer.id
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (
                                customerId ==
                                    customer.id
                            ) {
                                "✓ ${customer.name}"
                            } else {
                                customer.name
                            }
                        )
                    }
                }

                OutlinedTextField(
                    amount,
                    { amount = it },
                    label = {
                        Text(
                            v15Text(
                                "আদায়ের পরিমাণ",
                                "Collection amount"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    date,
                    { date = it },
                    label = {
                        Text(
                            v15Text(
                                "তারিখ (YYYY-MM-DD)",
                                "Date (YYYY-MM-DD)"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    note,
                    { note = it },
                    label = {
                        Text(
                            v15Text(
                                "নোট",
                                "Note"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val id = customerId
                    val value =
                        amount
                            .dealerBusinessDouble()

                    val time =
                        date
                            .dealerBusinessDateMillis()

                    if (
                        id != null &&
                        value != null &&
                        value > 0 &&
                        time != null
                    ) {
                        onSave(
                            id,
                            value,
                            time,
                            note
                        )
                    }
                }
            ) {
                Text(
                    v15Text(
                        "সংরক্ষণ",
                        "Save"
                    )
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    v15Text(
                        "বাতিল",
                        "Cancel"
                    )
                )
            }
        }
    )
}

@Composable
private fun DealerSupplierPaymentDialog(
    initial: DealerSupplierPaymentEntity?,
    companies: List<DealerCompanyEntity>,
    onDismiss: () -> Unit,
    onSave: (
        Long,
        Double,
        Long,
        String
    ) -> Unit
) {
    var companyId by remember(initial?.id) {
        mutableStateOf(initial?.companyId)
    }

    var amount by remember(initial?.id) {
        mutableStateOf(
            initial
                ?.amount
                ?.toString()
                .orEmpty()
        )
    }

    var date by remember(initial?.id) {
        mutableStateOf(
            dealerBusinessDate(
                initial?.paidAt
                    ?: System.currentTimeMillis()
            )
        )
    }

    var note by remember(initial?.id) {
        mutableStateOf(
            initial?.note.orEmpty()
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initial == null) {
                    v15Text(
                        "সাপ্লায়ার পেমেন্ট",
                        "Supplier payment"
                    )
                } else {
                    v15Text(
                        "পেমেন্ট সংশোধন",
                        "Correct payment"
                    )
                }
            )
        },
        text = {
            Column(
                modifier =
                    Modifier.verticalScroll(
                        rememberScrollState()
                    ),
                verticalArrangement =
                    Arrangement.spacedBy(7.dp)
            ) {
                companies.forEach { company ->
                    OutlinedButton(
                        onClick = {
                            companyId =
                                company.id
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (
                                companyId ==
                                    company.id
                            ) {
                                "✓ ${company.name}"
                            } else {
                                company.name
                            }
                        )
                    }
                }

                OutlinedTextField(
                    amount,
                    { amount = it },
                    label = {
                        Text(
                            v15Text(
                                "পেমেন্টের পরিমাণ",
                                "Payment amount"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    date,
                    { date = it },
                    label = {
                        Text(
                            v15Text(
                                "তারিখ (YYYY-MM-DD)",
                                "Date (YYYY-MM-DD)"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    note,
                    { note = it },
                    label = {
                        Text(
                            v15Text(
                                "নোট",
                                "Note"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val id = companyId
                    val value =
                        amount
                            .dealerBusinessDouble()

                    val time =
                        date
                            .dealerBusinessDateMillis()

                    if (
                        id != null &&
                        value != null &&
                        value > 0 &&
                        time != null
                    ) {
                        onSave(
                            id,
                            value,
                            time,
                            note
                        )
                    }
                }
            ) {
                Text(
                    v15Text(
                        "সংরক্ষণ",
                        "Save"
                    )
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    v15Text(
                        "বাতিল",
                        "Cancel"
                    )
                )
            }
        }
    )
}

@Composable
private fun DealerExpenseDialog(
    initial: DealerExpenseEntity?,
    onDismiss: () -> Unit,
    onSave: (
        String,
        Double,
        String
    ) -> Unit
) {
    var category by remember(initial?.id) {
        mutableStateOf(
            initial?.category.orEmpty()
        )
    }

    var amount by remember(initial?.id) {
        mutableStateOf(
            initial
                ?.amount
                ?.toString()
                .orEmpty()
        )
    }

    var note by remember(initial?.id) {
        mutableStateOf(
            initial?.note.orEmpty()
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initial == null) {
                    v15Text(
                        "ব্যবসার খরচ",
                        "Business expense"
                    )
                } else {
                    v15Text(
                        "খরচ সম্পাদনা",
                        "Edit expense"
                    )
                }
            )
        },
        text = {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(7.dp)
            ) {
                OutlinedTextField(
                    category,
                    { category = it },
                    label = {
                        Text(
                            v15Text(
                                "খরচের ধরন",
                                "Expense category"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    amount,
                    { amount = it },
                    label = {
                        Text(
                            v15Text(
                                "পরিমাণ",
                                "Amount"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    note,
                    { note = it },
                    label = {
                        Text(
                            v15Text(
                                "নোট",
                                "Note"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val value =
                        amount
                            .dealerBusinessDouble()

                    if (
                        category.isNotBlank() &&
                        value != null &&
                        value > 0
                    ) {
                        onSave(
                            category,
                            value,
                            note
                        )
                    }
                }
            ) {
                Text(
                    v15Text(
                        "সংরক্ষণ",
                        "Save"
                    )
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    v15Text(
                        "বাতিল",
                        "Cancel"
                    )
                )
            }
        }
    )
}

@Composable
private fun DealerSalesReturnDialog(
    line: DealerSaleLineEntity,
    onDismiss: () -> Unit,
    onSave: (
        Int,
        String,
        String
    ) -> Unit
) {
    var quantity by remember {
        mutableStateOf("")
    }

    var type by remember {
        mutableStateOf("RESTOCK")
    }

    var note by remember {
        mutableStateOf("")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                v15Text(
                    "বিক্রয় রিটার্ন / ড্যামেজ",
                    "Sales return / damage"
                )
            )
        },
        text = {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(7.dp)
            ) {
                Text(
                    line.productNameSnapshot,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    v15Text(
                        "বিক্রি হয়েছিল: ${line.quantity}",
                        "Sold quantity: ${line.quantity}"
                    )
                )

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            type = "RESTOCK"
                        },
                        modifier =
                            Modifier.weight(1f)
                    ) {
                        Text(
                            if (
                                type == "RESTOCK"
                            ) {
                                "✓ ${
                                    v15Text(
                                        "স্টকে ফেরত",
                                        "Restock"
                                    )
                                }"
                            } else {
                                v15Text(
                                    "স্টকে ফেরত",
                                    "Restock"
                                )
                            }
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            type = "DAMAGED"
                        },
                        modifier =
                            Modifier.weight(1f)
                    ) {
                        Text(
                            if (
                                type == "DAMAGED"
                            ) {
                                "✓ ${
                                    v15Text(
                                        "ড্যামেজ",
                                        "Damaged"
                                    )
                                }"
                            } else {
                                v15Text(
                                    "ড্যামেজ",
                                    "Damaged"
                                )
                            }
                        )
                    }
                }

                OutlinedTextField(
                    quantity,
                    { quantity = it },
                    label = {
                        Text(
                            v15Text(
                                "রিটার্ন পরিমাণ",
                                "Return quantity"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    note,
                    { note = it },
                    label = {
                        Text(
                            v15Text(
                                "কারণ / নোট",
                                "Reason / note"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val value =
                        quantity
                            .dealerBusinessInt()

                    if (
                        value != null &&
                        value > 0 &&
                        value <= line.quantity
                    ) {
                        onSave(
                            value,
                            type,
                            note
                        )
                    }
                }
            ) {
                Text(
                    v15Text(
                        "রিটার্ন করুন",
                        "Return"
                    )
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    v15Text(
                        "বাতিল",
                        "Cancel"
                    )
                )
            }
        }
    )
}

@Composable
private fun DealerPurchaseReturnDialog(
    line: DealerPurchaseLineEntity,
    onDismiss: () -> Unit,
    onSave: (
        Int,
        String
    ) -> Unit
) {
    var quantity by remember {
        mutableStateOf("")
    }

    var note by remember {
        mutableStateOf("")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                v15Text(
                    "সাপ্লায়ারকে রিটার্ন",
                    "Return to supplier"
                )
            )
        },
        text = {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(7.dp)
            ) {
                Text(
                    line.productNameSnapshot,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    v15Text(
                        "ক্রয় হয়েছিল: ${line.quantity}",
                        "Purchased quantity: ${line.quantity}"
                    )
                )

                OutlinedTextField(
                    quantity,
                    { quantity = it },
                    label = {
                        Text(
                            v15Text(
                                "রিটার্ন পরিমাণ",
                                "Return quantity"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    note,
                    { note = it },
                    label = {
                        Text(
                            v15Text(
                                "কারণ / নোট",
                                "Reason / note"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val value =
                        quantity
                            .dealerBusinessInt()

                    if (
                        value != null &&
                        value > 0 &&
                        value <= line.quantity
                    ) {
                        onSave(
                            value,
                            note
                        )
                    }
                }
            ) {
                Text(
                    v15Text(
                        "রিটার্ন করুন",
                        "Return"
                    )
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    v15Text(
                        "বাতিল",
                        "Cancel"
                    )
                )
            }
        }
    )
}


@Composable
private fun DealerTransactionMetaDialog(
    title: String,
    invoiceNo: String,
    date: Long,
    note: String,
    onDismiss: () -> Unit,
    onSave: (
        String,
        Long,
        String
    ) -> Unit
) {
    var invoice by remember {
        mutableStateOf(invoiceNo)
    }

    var dateText by remember {
        mutableStateOf(
            dealerBusinessDate(date)
        )
    }

    var noteText by remember {
        mutableStateOf(note)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title)
        },
        text = {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(7.dp)
            ) {
                OutlinedTextField(
                    invoice,
                    { invoice = it },
                    label = {
                        Text(
                            v15Text(
                                "ইনভয়েস নম্বর",
                                "Invoice number"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    dateText,
                    { dateText = it },
                    label = {
                        Text(
                            v15Text(
                                "তারিখ (YYYY-MM-DD)",
                                "Date (YYYY-MM-DD)"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    noteText,
                    { noteText = it },
                    label = {
                        Text(
                            v15Text(
                                "নোট",
                                "Note"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val millis =
                        dateText
                            .dealerBusinessDateMillis()

                    if (millis != null) {
                        onSave(
                            invoice,
                            millis,
                            noteText
                        )
                    }
                }
            ) {
                Text(
                    v15Text(
                        "সংরক্ষণ",
                        "Save"
                    )
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    v15Text(
                        "বাতিল",
                        "Cancel"
                    )
                )
            }
        }
    )
}

@Composable
private fun DealerReturnMetaDialog(
    initialDate: Long,
    initialNote: String,
    onDismiss: () -> Unit,
    onSave: (
        Long,
        String
    ) -> Unit
) {
    var dateText by remember(initialDate) {
        mutableStateOf(
            dealerBusinessDate(
                initialDate
            )
        )
    }

    var note by remember(initialDate) {
        mutableStateOf(initialNote)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                v15Text(
                    "রিটার্ন তথ্য সম্পাদনা",
                    "Edit return information"
                )
            )
        },
        text = {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(7.dp)
            ) {
                OutlinedTextField(
                    dateText,
                    { dateText = it },
                    label = {
                        Text(
                            v15Text(
                                "তারিখ (YYYY-MM-DD)",
                                "Date (YYYY-MM-DD)"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    note,
                    { note = it },
                    label = {
                        Text(
                            v15Text(
                                "কারণ / নোট",
                                "Reason / note"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val millis =
                        dateText
                            .dealerBusinessDateMillis()

                    if (millis != null) {
                        onSave(
                            millis,
                            note
                        )
                    }
                }
            ) {
                Text(
                    v15Text(
                        "সংরক্ষণ",
                        "Save"
                    )
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    v15Text(
                        "বাতিল",
                        "Cancel"
                    )
                )
            }
        }
    )
}

@Composable
private fun DealerBusinessMetric(
    title: String,
    value: String,
    modifier: Modifier
) {
    Card(
        modifier = modifier
    ) {
        Column(
            modifier =
                Modifier.padding(10.dp)
        ) {
            Text(
                value,
                fontWeight =
                    FontWeight.ExtraBold,
                style =
                    MaterialTheme.typography
                        .titleLarge
            )

            Text(
                title,
                style =
                    MaterialTheme.typography
                        .labelSmall
            )
        }
    }
}

@Composable
private fun DealerCompanyDialog(
    initial: DealerCompanyEntity?,
    onDismiss: () -> Unit,
    onSave: (
        String,
        String,
        String,
        String,
        String,
        String
    ) -> Unit
) {
    var name by remember(initial?.id) {
        mutableStateOf(initial?.name.orEmpty())
    }

    var code by remember(initial?.id) {
        mutableStateOf(initial?.code.orEmpty())
    }

    var phone by remember(initial?.id) {
        mutableStateOf(initial?.phone.orEmpty())
    }

    var contact by remember(initial?.id) {
        mutableStateOf(
            initial?.contactPerson.orEmpty()
        )
    }

    var address by remember(initial?.id) {
        mutableStateOf(initial?.address.orEmpty())
    }

    var note by remember(initial?.id) {
        mutableStateOf(initial?.note.orEmpty())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initial == null) {
                    v15Text(
                        "কোম্পানি যোগ করুন",
                        "Add company"
                    )
                } else {
                    v15Text(
                        "কোম্পানি সম্পাদনা",
                        "Edit company"
                    )
                }
            )
        },
        text = {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(7.dp)
            ) {
                OutlinedTextField(
                    name,
                    { name = it },
                    label = {
                        Text(
                            v15Text(
                                "কোম্পানির নাম",
                                "Company name"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    code,
                    { code = it },
                    label = {
                        Text(
                            v15Text(
                                "কোড",
                                "Code"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    phone,
                    { phone = it },
                    label = {
                        Text(
                            v15Text(
                                "ফোন",
                                "Phone"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    contact,
                    { contact = it },
                    label = {
                        Text(
                            v15Text(
                                "যোগাযোগ ব্যক্তি",
                                "Contact person"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    address,
                    { address = it },
                    label = {
                        Text(
                            v15Text(
                                "ঠিকানা",
                                "Address"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    note,
                    { note = it },
                    label = {
                        Text(
                            v15Text(
                                "নোট",
                                "Note"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            name,
                            code,
                            phone,
                            contact,
                            address,
                            note
                        )
                    }
                }
            ) {
                Text(
                    v15Text(
                        "সেভ",
                        "Save"
                    )
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    v15Text(
                        "বাতিল",
                        "Cancel"
                    )
                )
            }
        }
    )
}

@Composable
private fun DealerAreaDialog(
    initial: DealerAreaEntity?,
    onDismiss: () -> Unit,
    onSave: (
        String,
        String,
        String
    ) -> Unit
) {
    var name by remember(initial?.id) {
        mutableStateOf(initial?.name.orEmpty())
    }

    var code by remember(initial?.id) {
        mutableStateOf(initial?.code.orEmpty())
    }

    var note by remember(initial?.id) {
        mutableStateOf(initial?.note.orEmpty())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initial == null) {
                    v15Text(
                        "এরিয়া যোগ করুন",
                        "Add area"
                    )
                } else {
                    v15Text(
                        "এরিয়া সম্পাদনা",
                        "Edit area"
                    )
                }
            )
        },
        text = {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(7.dp)
            ) {
                OutlinedTextField(
                    name,
                    { name = it },
                    label = {
                        Text(
                            v15Text(
                                "এরিয়া / রুট",
                                "Area / route"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    code,
                    { code = it },
                    label = {
                        Text(
                            v15Text(
                                "কোড",
                                "Code"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    note,
                    { note = it },
                    label = {
                        Text(
                            v15Text(
                                "নোট",
                                "Note"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            name,
                            code,
                            note
                        )
                    }
                }
            ) {
                Text(
                    v15Text(
                        "সেভ",
                        "Save"
                    )
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    v15Text(
                        "বাতিল",
                        "Cancel"
                    )
                )
            }
        }
    )
}

@Composable
private fun DealerCustomerDialog(
    initial: DealerCustomerEntity?,
    areas: List<DealerAreaEntity>,
    onDismiss: () -> Unit,
    onSave: (
        Long?,
        String,
        String,
        String,
        String,
        Double,
        String
    ) -> Unit
) {
    var areaId by remember(initial?.id) {
        mutableStateOf(initial?.areaId)
    }

    var name by remember(initial?.id) {
        mutableStateOf(initial?.name.orEmpty())
    }

    var code by remember(initial?.id) {
        mutableStateOf(
            initial?.customerCode.orEmpty()
        )
    }

    var phone by remember(initial?.id) {
        mutableStateOf(initial?.phone.orEmpty())
    }

    var address by remember(initial?.id) {
        mutableStateOf(initial?.address.orEmpty())
    }

    var credit by remember(initial?.id) {
        mutableStateOf(
            initial
                ?.creditLimit
                ?.takeIf { it > 0 }
                ?.toString()
                .orEmpty()
        )
    }

    var note by remember(initial?.id) {
        mutableStateOf(initial?.note.orEmpty())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initial == null) {
                    v15Text(
                        "রিটেইলার যোগ করুন",
                        "Add retailer"
                    )
                } else {
                    v15Text(
                        "রিটেইলার সম্পাদনা",
                        "Edit retailer"
                    )
                }
            )
        },
        text = {
            Column(
                modifier =
                    Modifier.verticalScroll(
                        rememberScrollState()
                    ),
                verticalArrangement =
                    Arrangement.spacedBy(7.dp)
            ) {
                Text(
                    v15Text(
                        "এরিয়া নির্বাচন",
                        "Select area"
                    ),
                    fontWeight =
                        FontWeight.Bold
                )

                OutlinedButton(
                    onClick = {
                        areaId = null
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (areaId == null) {
                            v15Text(
                                "✓ কোনো এরিয়া নয়",
                                "✓ No area"
                            )
                        } else {
                            v15Text(
                                "কোনো এরিয়া নয়",
                                "No area"
                            )
                        }
                    )
                }

                areas.forEach { area ->
                    OutlinedButton(
                        onClick = {
                            areaId = area.id
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (areaId == area.id) {
                                "✓ ${area.name}"
                            } else {
                                area.name
                            }
                        )
                    }
                }

                OutlinedTextField(
                    name,
                    { name = it },
                    label = {
                        Text(
                            v15Text(
                                "রিটেইলার / কাস্টমার নাম",
                                "Retailer / customer name"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    code,
                    { code = it },
                    label = {
                        Text(
                            v15Text(
                                "কাস্টমার কোড",
                                "Customer code"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    phone,
                    { phone = it },
                    label = {
                        Text(
                            v15Text(
                                "ফোন",
                                "Phone"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    address,
                    { address = it },
                    label = {
                        Text(
                            v15Text(
                                "ঠিকানা",
                                "Address"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    credit,
                    { credit = it },
                    label = {
                        Text(
                            v15Text(
                                "ক্রেডিট সীমা",
                                "Credit limit"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    note,
                    { note = it },
                    label = {
                        Text(
                            v15Text(
                                "নোট",
                                "Note"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val creditValue =
                        credit
                            .trim()
                            .replace(",", "")
                            .toDoubleOrNull()
                            ?: 0.0

                    if (
                        name.isNotBlank() &&
                        creditValue >= 0
                    ) {
                        onSave(
                            areaId,
                            name,
                            code,
                            phone,
                            address,
                            creditValue,
                            note
                        )
                    }
                }
            ) {
                Text(
                    v15Text(
                        "সেভ",
                        "Save"
                    )
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    v15Text(
                        "বাতিল",
                        "Cancel"
                    )
                )
            }
        }
    )
}
