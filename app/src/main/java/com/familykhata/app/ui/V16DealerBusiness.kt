package com.familykhata.app.ui

import android.content.ClipData
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.familykhata.app.FamilyKhataViewModel
import com.familykhata.app.data.ProductEntity
import com.familykhata.app.dealerbusiness.DealerAreaEntity
import com.familykhata.app.dealerbusiness.DealerBusinessViewModel
import com.familykhata.app.dealerbusiness.DealerCollectionEntity
import com.familykhata.app.dealerbusiness.DealerCompanyEntity
import com.familykhata.app.dealerbusiness.DealerCustomerEntity
import com.familykhata.app.dealerbusiness.DealerExpenseEntity
import com.familykhata.app.dealerbusiness.DealerDamageEntity
import com.familykhata.app.dealerbusiness.DealerDeliveryChallanEntity
import com.familykhata.app.dealerbusiness.DealerDeliveryChallanLineEntity
import com.familykhata.app.dealerbusiness.DealerDeliveryPersonEntity
import com.familykhata.app.dealerbusiness.DealerProductPackEntity
import com.familykhata.app.dealerbusiness.DealerPurchaseEntity
import com.familykhata.app.dealerbusiness.DealerPurchaseLineEntity
import com.familykhata.app.dealerbusiness.DealerPurchaseLineInput
import com.familykhata.app.dealerbusiness.DealerPurchaseReturnEntity
import com.familykhata.app.dealerbusiness.DealerSaleEntity
import com.familykhata.app.dealerbusiness.DealerSaleLineEntity
import com.familykhata.app.dealerbusiness.DealerSaleLineInput
import com.familykhata.app.dealerbusiness.DealerSalesReturnEntity
import com.familykhata.app.dealerbusiness.DealerSupplierPaymentEntity
import com.familykhata.app.report.writeDeliveryChallanPdf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

private fun dealerDeliveryQuantityText(
    baseQuantity: Int,
    line: DealerDeliveryChallanLineEntity
): String {
    val quantity =
        baseQuantity.coerceAtLeast(0)

    val factor =
        line.unitFactor
            .coerceAtLeast(1)

    val unit =
        line.unitSnapshot
            .trim()
            .ifBlank {
                "unit"
            }

    if (factor == 1) {
        return "$quantity $unit"
    }

    val unitQuantity =
        quantity.toDouble() /
            factor.toDouble()

    val formattedUnitQuantity =
        if (
            quantity %
            factor ==
            0
        ) {
            (
                quantity /
                    factor
                ).toString()
        } else {
            String.format(
                Locale.US,
                "%.3f",
                unitQuantity
            )
                .trimEnd('0')
                .trimEnd('.')
        }

    return if (
        quantity %
        factor ==
        0
    ) {
        "$formattedUnitQuantity $unit " +
            "($quantity base)"
    } else {
        "$quantity base " +
            "($formattedUnitQuantity $unit)"
    }
}

private fun dealerDeliveryBaseQuantityLabel(
    line: DealerDeliveryChallanLineEntity,
    banglaAction: String,
    englishAction: String
): String {
    val unit =
        line.unitSnapshot
            .trim()
            .ifBlank {
                "unit"
            }

    val factor =
        line.unitFactor
            .coerceAtLeast(1)

    return if (factor == 1) {
        v15Text(
            "$banglaAction ($unit)",
            "$englishAction ($unit)"
        )
    } else {
        v15Text(
            "$banglaAction — বেস পরিমাণ • 1 $unit = $factor base",
            "$englishAction — base quantity • 1 $unit = $factor base"
        )
    }
}

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

private sealed interface DealerDeleteTarget {
    data class Company(
        val item: DealerCompanyEntity
    ) : DealerDeleteTarget

    data class Area(
        val item: DealerAreaEntity
    ) : DealerDeleteTarget

    data class Customer(
        val item: DealerCustomerEntity
    ) : DealerDeleteTarget

    data class DeliveryPerson(
        val item: DealerDeliveryPersonEntity
    ) : DealerDeleteTarget

    data class Collection(
        val item: DealerCollectionEntity
    ) : DealerDeleteTarget

    data class SupplierPayment(
        val item: DealerSupplierPaymentEntity
    ) : DealerDeleteTarget

    data class Expense(
        val item: DealerExpenseEntity
    ) : DealerDeleteTarget

    data class ProductPack(
        val item: DealerProductPackEntity
    ) : DealerDeleteTarget

    data class Purchase(
        val item: DealerPurchaseEntity
    ) : DealerDeleteTarget

    data class Sale(
        val item: DealerSaleEntity
    ) : DealerDeleteTarget

    data class DeliveryChallan(
        val item: DealerDeliveryChallanEntity
    ) : DealerDeleteTarget

    data class ReopenSettlement(
        val item: DealerDeliveryChallanEntity
    ) : DealerDeleteTarget

    data class WarehouseDamage(
        val item: DealerDamageEntity
    ) : DealerDeleteTarget
}

@Composable
internal fun V16DealerBusinessScreen(
    workspace: String,
    shopType: String,
    canWrite: Boolean,
    onExit: () -> Unit
) {
    val vm: DealerBusinessViewModel = viewModel()
    val securityViewModel:
        FamilyKhataViewModel = viewModel()

    val context =
        androidx.compose.ui.platform
            .LocalContext.current

    val scope =
        rememberCoroutineScope()

    val companies by vm.companies.collectAsState()
    val areas by vm.areas.collectAsState()
    val customers by vm.customers.collectAsState()
    val customerLedgers by
        vm.customerLedgers.collectAsState()
    val products by vm.products.collectAsState()
    val purchases by vm.purchases.collectAsState()
    val sales by vm.sales.collectAsState()
    val expenses by vm.expenses.collectAsState()
    val collections by vm.collections.collectAsState()
    val supplierPayments by
        vm.supplierPayments.collectAsState()

    val productPacks by
        vm.productPacks.collectAsState()

    val deliveryPeople by
        vm.deliveryPeople.collectAsState()

    val deliveryChallans by
        vm.deliveryChallans.collectAsState()

    val damages by
        vm.damages.collectAsState()

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

    var showPackSetup by remember {
        mutableStateOf(false)
    }

    var showDeliveryPerson by remember {
        mutableStateOf(false)
    }

    var editingDeliveryPerson by remember {
        mutableStateOf<DealerDeliveryPersonEntity?>(
            null
        )
    }

    var showDeliveryChallan by remember {
        mutableStateOf(false)
    }

    var sharingChallanId by remember {
        mutableStateOf<Long?>(null)
    }

    var showWarehouseDamage by remember {
        mutableStateOf(false)
    }

    var deliverySaleChallan by remember {
        mutableStateOf<DealerDeliveryChallanEntity?>(
            null
        )
    }

    var settlementChallan by remember {
        mutableStateOf<DealerDeliveryChallanEntity?>(
            null
        )
    }

    var editingDeliveryChallan by remember {
        mutableStateOf<DealerDeliveryChallanEntity?>(
            null
        )
    }

    var editingSettlementChallan by remember {
        mutableStateOf<DealerDeliveryChallanEntity?>(
            null
        )
    }

    var editingExpense by remember {
        mutableStateOf<DealerExpenseEntity?>(null)
    }

    var selectedLedgerCustomer by remember {
        mutableStateOf<DealerCustomerEntity?>(null)
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

    var editingDamage by remember {
        mutableStateOf<DealerDamageEntity?>(null)
    }

    var deleteTarget by remember {
        mutableStateOf<DealerDeleteTarget?>(null)
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
                selectedSale != null ||
                selectedLedgerCustomer != null
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

    selectedLedgerCustomer?.let { customer ->
        val summary =
            customerLedgers.firstOrNull {
                it.customerId ==
                    customer.id
            }

        V15DeepScreenContainer(
            title =
                v15Text(
                    "বাকি খাতা",
                    "Due ledger"
                ),
            onBack = {
                selectedLedgerCustomer =
                    null
            }
        ) {
            DealerCustomerLedgerScreen(
                customer = customer,
                summary = summary,
                sales =
                    sales.filter {
                        it.customerId ==
                            customer.id
                    },
                collections =
                    collections.filter {
                        it.customerId ==
                            customer.id
                    },
                onOpenSale = {
                    selectedSale = it
                }
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
                        Row(
                            horizontalArrangement =
                                Arrangement.spacedBy(6.dp)
                        ) {
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

                            TextButton(
                                onClick = {
                                    deleteTarget =
                                        DealerDeleteTarget
                                            .Company(item)
                                }
                            ) {
                                Text(
                                    v15Text(
                                        "ডিলিট",
                                        "Delete"
                                    )
                                )
                            }
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
                        Column {
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

                            TextButton(
                                onClick = {
                                    deleteTarget =
                                        DealerDeleteTarget
                                            .Area(item)
                                }
                            ) {
                                Text(
                                    v15Text(
                                        "ডিলিট",
                                        "Delete"
                                    )
                                )
                            }
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
                        Row(
                            horizontalArrangement =
                                Arrangement.spacedBy(6.dp)
                        ) {
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

                            TextButton(
                                onClick = {
                                    deleteTarget =
                                        DealerDeleteTarget
                                            .Customer(item)
                                }
                            ) {
                                Text(
                                    v15Text(
                                        "ডিলিট",
                                        "Delete"
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        Text(
            v15Text(
                "ডিলার সেটআপ",
                "Dealer setup"
            ),
            fontWeight =
                FontWeight.Bold
        )

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(6.dp)
        ) {
            OutlinedButton(
                onClick = {
                    showPackSetup = true
                },
                enabled =
                    canWrite &&
                        products.isNotEmpty(),
                modifier =
                    Modifier.weight(1f)
            ) {
                Text(
                    v15Text(
                        "বক্স / পাতা সেটআপ",
                        "Box / sheet setup"
                    )
                )
            }

            OutlinedButton(
                onClick = {
                    editingDeliveryPerson =
                        null

                    showDeliveryPerson =
                        true
                },
                enabled =
                    canWrite,
                modifier =
                    Modifier.weight(1f)
            ) {
                Text(
                    v15Text(
                        "+ ডেলিভারি ম্যান",
                        "+ Delivery man"
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
            Button(
                onClick = {
                    showDeliveryChallan =
                        true
                },
                enabled =
                    canWrite &&
                        deliveryPeople.isNotEmpty() &&
                        products.isNotEmpty(),
                modifier =
                    Modifier.weight(1f)
            ) {
                Text(
                    v15Text(
                        "মাল দেওয়ার চালান",
                        "Issue challan"
                    )
                )
            }

            OutlinedButton(
                onClick = {
                    showWarehouseDamage =
                        true
                },
                enabled =
                    canWrite &&
                        products.isNotEmpty(),
                modifier =
                    Modifier.weight(1f)
            ) {
                Text(
                    v15Text(
                        "ড্যামেজ",
                        "Damage"
                    )
                )
            }
        }

        if (deliveryChallans.isNotEmpty()) {
            Text(
                v15Text(
                    "ডেলিভারি চালান",
                    "Delivery challans"
                ),
                fontWeight =
                    FontWeight.Bold
            )

            deliveryChallans
                .take(10)
                .forEach { challan ->
                    Card(
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier =
                                Modifier.padding(
                                    10.dp
                                ),
                            verticalArrangement =
                                Arrangement.spacedBy(
                                    3.dp
                                )
                        ) {
                            Text(
                                challan.challanNo,
                                fontWeight =
                                    FontWeight.Bold
                            )

                            Text(
                                challan
                                    .deliveryPersonNameSnapshot
                            )

                            Text(
                                dealerBusinessDate(
                                    challan.issuedAt
                                )
                            )

                            Text(
                                v15Text(
                                    "স্ট্যাটাস: ${challan.status}",
                                    "Status: ${challan.status}"
                                )
                            )

                            OutlinedButton(
                                enabled =
                                    sharingChallanId ==
                                        null,
                                modifier =
                                    Modifier
                                        .fillMaxWidth(),
                                onClick = {
                                    sharingChallanId =
                                        challan.id

                                    val bangla =
                                        V15LanguageState
                                            .isBangla()

                                    scope.launch {
                                        try {
                                            val pdf =
                                                withContext(
                                                    Dispatchers.IO
                                                ) {
                                                    val lines =
                                                        vm
                                                            .loadDeliveryLineStatuses(
                                                                challan.id
                                                            )
                                                            .map {
                                                                it.line
                                                            }

                                                    writeDeliveryChallanPdf(
                                                        context =
                                                            context
                                                                .applicationContext,
                                                        challan =
                                                            challan,
                                                        lines =
                                                            lines,
                                                        bangla =
                                                            bangla
                                                    )
                                                }

                                            val uri =
                                                FileProvider
                                                    .getUriForFile(
                                                        context,
                                                        "${context.packageName}.statements",
                                                        pdf.file
                                                    )

                                            val intent =
                                                Intent(
                                                    Intent.ACTION_SEND
                                                ).apply {
                                                    type =
                                                        "application/pdf"

                                                    putExtra(
                                                        Intent.EXTRA_STREAM,
                                                        uri
                                                    )

                                                    putExtra(
                                                        Intent.EXTRA_SUBJECT,
                                                        "Hisabi Khata - ${challan.challanNo}"
                                                    )

                                                    clipData =
                                                        ClipData
                                                            .newRawUri(
                                                                "Delivery challan",
                                                                uri
                                                            )

                                                    addFlags(
                                                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                    )
                                                }

                                            context.startActivity(
                                                Intent.createChooser(
                                                    intent,
                                                    v15Text(
                                                        "চালান PDF শেয়ার করুন",
                                                        "Share challan PDF"
                                                    )
                                                )
                                            )
                                        } catch (
                                            cancelled:
                                                CancellationException
                                        ) {
                                            throw cancelled
                                        } catch (
                                            _: Exception
                                        ) {
                                            Toast.makeText(
                                                context,
                                                v15Text(
                                                    "চালান PDF তৈরি বা শেয়ার করা যায়নি।",
                                                    "Unable to create or share challan PDF."
                                                ),
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } finally {
                                            if (
                                                sharingChallanId ==
                                                challan.id
                                            ) {
                                                sharingChallanId =
                                                    null
                                            }
                                        }
                                    }
                                }
                            ) {
                                Text(
                                    if (
                                        sharingChallanId ==
                                        challan.id
                                    ) {
                                        v15Text(
                                            "PDF তৈরি হচ্ছে…",
                                            "Creating PDF…"
                                        )
                                    } else {
                                        v15Text(
                                            "PDF চালান শেয়ার",
                                            "Share PDF challan"
                                        )
                                    }
                                )
                            }

                            if (
                                canWrite &&
                                challan.status ==
                                    "OPEN"
                            ) {
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth(),
                                    horizontalArrangement =
                                        Arrangement
                                            .spacedBy(
                                                6.dp
                                            )
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            deliverySaleChallan =
                                                challan
                                        },
                                        modifier =
                                            Modifier
                                                .weight(
                                                    1f
                                                )
                                    ) {
                                        Text(
                                            v15Text(
                                                "বিক্রি / বাকি",
                                                "Sale / due"
                                            )
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            settlementChallan =
                                                challan
                                        },
                                        modifier =
                                            Modifier
                                                .weight(
                                                    1f
                                                )
                                    ) {
                                        Text(
                                            v15Text(
                                                "রাতে বুঝে নিন",
                                                "Night settle"
                                            )
                                        )
                                    }
                                }
                            }

                            if (
                                challan.status ==
                                    "SETTLED" &&
                                challan.settledAt !=
                                    null
                            ) {
                                Text(
                                    v15Text(
                                        "বুঝে নেওয়া: ${
                                            dealerBusinessDate(
                                                challan.settledAt
                                            )
                                        }",
                                        "Settled: ${
                                            dealerBusinessDate(
                                                challan.settledAt
                                            )
                                        }"
                                    )
                                )
                            }

                            if (canWrite) {
                                TextButton(
                                    onClick = {
                                        editingDeliveryChallan =
                                            challan
                                    }
                                ) {
                                    Text(
                                        v15Text(
                                            "চালান তথ্য সম্পাদনা",
                                            "Edit challan info"
                                        )
                                    )
                                }

                                if (
                                    challan.status ==
                                        "OPEN"
                                ) {
                                    TextButton(
                                        onClick = {
                                            deleteTarget =
                                                DealerDeleteTarget
                                                    .DeliveryChallan(
                                                        challan
                                                    )
                                        }
                                    ) {
                                        Text(
                                            v15Text(
                                                "চালান ডিলিট",
                                                "Delete challan"
                                            )
                                        )
                                    }
                                }

                                if (
                                    challan.status ==
                                        "SETTLED"
                                ) {
                                    Row(
                                        modifier =
                                            Modifier.fillMaxWidth(),
                                        horizontalArrangement =
                                            Arrangement.spacedBy(
                                                6.dp
                                            )
                                    ) {
                                        TextButton(
                                            onClick = {
                                                editingSettlementChallan =
                                                    challan
                                            },
                                            modifier =
                                                Modifier.weight(
                                                    1f
                                                )
                                        ) {
                                            Text(
                                                v15Text(
                                                    "সেটেলমেন্ট সম্পাদনা",
                                                    "Edit settlement"
                                                )
                                            )
                                        }

                                        TextButton(
                                            onClick = {
                                                deleteTarget =
                                                    DealerDeleteTarget
                                                        .ReopenSettlement(
                                                            challan
                                                        )
                                            },
                                            modifier =
                                                Modifier.weight(
                                                    1f
                                                )
                                        ) {
                                            Text(
                                                v15Text(
                                                    "পুনরায় খুলুন",
                                                    "Reopen"
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
        }

        if (damages.isNotEmpty()) {
            Text(
                v15Text(
                    "সাম্প্রতিক ড্যামেজ",
                    "Recent damage"
                ),
                fontWeight =
                    FontWeight.Bold
            )

            damages
                .take(10)
                .forEach { damage ->
                    Card(
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier =
                                Modifier.padding(
                                    10.dp
                                ),
                            verticalArrangement =
                                Arrangement.spacedBy(
                                    3.dp
                                )
                        ) {
                            Text(
                                damage.productNameSnapshot,
                                fontWeight =
                                    FontWeight.Bold
                            )

                            Text(
                                v15Text(
                                    "পরিমাণ: ${damage.quantityPieces} পিস",
                                    "Quantity: ${damage.quantityPieces} pcs"
                                )
                            )

                            Text(
                                v15Text(
                                    "ক্ষতি: ${
                                        dealerBusinessMoney(
                                            damage.totalCost
                                        )
                                    }",
                                    "Loss: ${
                                        dealerBusinessMoney(
                                            damage.totalCost
                                        )
                                    }"
                                )
                            )

                            if (
                                damage.reason
                                    .isNotBlank()
                            ) {
                                Text(
                                    damage.reason
                                )
                            }

                            Text(
                                dealerBusinessDate(
                                    damage.damagedAt
                                )
                            )

                            if (
                                canWrite &&
                                damage.sourceType ==
                                    "WAREHOUSE"
                            ) {
                                Row(
                                    horizontalArrangement =
                                        Arrangement.spacedBy(
                                            6.dp
                                        )
                                ) {
                                    TextButton(
                                        onClick = {
                                            editingDamage =
                                                damage
                                        }
                                    ) {
                                        Text(
                                            v15Text(
                                                "সম্পাদনা",
                                                "Edit"
                                            )
                                        )
                                    }

                                    TextButton(
                                        onClick = {
                                            deleteTarget =
                                                DealerDeleteTarget
                                                    .WarehouseDamage(
                                                        damage
                                                    )
                                        }
                                    ) {
                                        Text(
                                            v15Text(
                                                "ডিলিট",
                                                "Delete"
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
        }

        if (deliveryPeople.isNotEmpty()) {
            Text(
                v15Text(
                    "ডেলিভারি ম্যান",
                    "Delivery people"
                ),
                fontWeight =
                    FontWeight.Bold
            )

            deliveryPeople.forEach { person ->
                Card(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                        horizontalArrangement =
                            Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier =
                                Modifier.weight(1f)
                        ) {
                            Text(
                                person.name,
                                fontWeight =
                                    FontWeight.Bold
                            )

                            if (
                                person.phone
                                    .isNotBlank()
                            ) {
                                Text(
                                    person.phone
                                )
                            }

                            if (
                                person.note
                                    .isNotBlank()
                            ) {
                                Text(
                                    person.note,
                                    style =
                                        MaterialTheme
                                            .typography
                                            .bodySmall
                                )
                            }
                        }

                        if (canWrite) {
                            Column {
                                TextButton(
                                    onClick = {
                                        editingDeliveryPerson =
                                            person

                                        showDeliveryPerson =
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

                                TextButton(
                                    onClick = {
                                        deleteTarget =
                                            DealerDeleteTarget
                                                .DeliveryPerson(
                                                    person
                                                )
                                    }
                                ) {
                                    Text(
                                        v15Text(
                                            "ডিলিট",
                                            "Delete"
                                        )
                                    )
                                }
                            }
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
                        Row(
                            modifier =
                                Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    6.dp
                                )
                        ) {
                            TextButton(
                                onClick = {
                                    editingPurchaseMeta =
                                        purchase
                                },
                                modifier =
                                    Modifier.weight(1f)
                            ) {
                                Text(
                                    v15Text(
                                        "সম্পাদনা",
                                        "Edit"
                                    )
                                )
                            }

                            TextButton(
                                onClick = {
                                    deleteTarget =
                                        DealerDeleteTarget
                                            .Purchase(
                                                purchase
                                            )
                                },
                                modifier =
                                    Modifier.weight(1f)
                            ) {
                                Text(
                                    v15Text(
                                        "ডিলিট",
                                        "Delete"
                                    )
                                )
                            }
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
                        Row(
                            modifier =
                                Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    6.dp
                                )
                        ) {
                            TextButton(
                                onClick = {
                                    editingSaleMeta =
                                        sale
                                },
                                modifier =
                                    Modifier.weight(1f)
                            ) {
                                Text(
                                    v15Text(
                                        "সম্পাদনা",
                                        "Edit"
                                    )
                                )
                            }

                            TextButton(
                                onClick = {
                                    deleteTarget =
                                        DealerDeleteTarget
                                            .Sale(sale)
                                },
                                modifier =
                                    Modifier.weight(1f)
                            ) {
                                Text(
                                    v15Text(
                                        "ডিলিট",
                                        "Delete"
                                    )
                                )
                            }
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
                        Row(
                            horizontalArrangement =
                                Arrangement.spacedBy(6.dp)
                        ) {
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

                            TextButton(
                                onClick = {
                                    deleteTarget =
                                        DealerDeleteTarget
                                            .Collection(item)
                                }
                            ) {
                                Text(
                                    v15Text(
                                        "ডিলিট",
                                        "Delete"
                                    )
                                )
                            }
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
                        Row(
                            horizontalArrangement =
                                Arrangement.spacedBy(6.dp)
                        ) {
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

                            TextButton(
                                onClick = {
                                    deleteTarget =
                                        DealerDeleteTarget
                                            .SupplierPayment(
                                                item
                                            )
                                }
                            ) {
                                Text(
                                    v15Text(
                                        "ডিলিট",
                                        "Delete"
                                    )
                                )
                            }
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
                        Column {
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

                            TextButton(
                                onClick = {
                                    deleteTarget =
                                        DealerDeleteTarget
                                            .Expense(expense)
                                }
                            ) {
                                Text(
                                    v15Text(
                                        "ডিলিট",
                                        "Delete"
                                    )
                                )
                            }
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

    deliverySaleChallan?.let {
            challan ->

        DealerDeliverySaleDialog(
            challan = challan,
            customers = customers,
            viewModel = vm,
            onDismiss = {
                deliverySaleChallan =
                    null
            },
            onSave = {
                    customerId,
                    invoiceNo,
                    lines,
                    collectedNow,
                    note ->

                vm.createDeliverySale(
                    challanId =
                        challan.id,
                    customerId =
                        customerId,
                    invoiceNo =
                        invoiceNo,
                    lines =
                        lines,
                    collectedNow =
                        collectedNow,
                    note =
                        note
                ) {
                    if (it) {
                        deliverySaleChallan =
                            null
                    }
                }
            }
        )
    }

    settlementChallan?.let {
            challan ->

        DealerDeliverySettlementDialog(
            challan = challan,
            viewModel = vm,
            onDismiss = {
                settlementChallan =
                    null
            },
            onSave = {
                    lines,
                    cashHandedOver,
                    note ->

                vm.settleDeliveryChallan(
                    challanId =
                        challan.id,
                    lines =
                        lines,
                    cashHandedOver =
                        cashHandedOver,
                    note =
                        note
                ) {
                    if (it) {
                        settlementChallan =
                            null
                    }
                }
            }
        )
    }

    if (showDeliveryChallan) {
        DealerDeliveryChallanDialog(
            viewModel = vm,
            deliveryPeople =
                deliveryPeople,
            products =
                products,
            onDismiss = {
                showDeliveryChallan =
                    false
            },
            onSave = {
                    deliveryPersonId,
                    challanNo,
                    lines,
                    note ->

                vm.createDeliveryChallan(
                    deliveryPersonId =
                        deliveryPersonId,
                    challanNo =
                        challanNo,
                    lines =
                        lines,
                    note =
                        note
                ) {
                    if (it) {
                        showDeliveryChallan =
                            false
                    }
                }
            }
        )
    }

    if (showWarehouseDamage) {
        DealerWarehouseDamageDialog(
            products =
                products,
            viewModel =
                vm,
            onDismiss = {
                showWarehouseDamage =
                    false
            },
            onSave = {
                    batchId,
                    quantity,
                    reason,
                    note ->

                vm.recordWarehouseDamage(
                    batchId =
                        batchId,
                    quantityPieces =
                        quantity,
                    reason =
                        reason,
                    note =
                        note
                ) {
                    if (it) {
                        showWarehouseDamage =
                            false
                    }
                }
            }
        )
    }

    editingDeliveryChallan?.let { item ->
        DealerDeliveryChallanMetaDialog(
            initial = item,
            deliveryPeople =
                deliveryPeople,
            onDismiss = {
                editingDeliveryChallan =
                    null
            },
            onSave = {
                    personId,
                    challanNo,
                    issuedAt,
                    note ->

                vm.updateDeliveryChallanMeta(
                    item = item,
                    deliveryPersonId =
                        personId,
                    challanNo =
                        challanNo,
                    issuedAt =
                        issuedAt,
                    note = note
                ) {
                    if (it) {
                        editingDeliveryChallan =
                            null
                    }
                }
            }
        )
    }

    editingSettlementChallan?.let {
            challan ->

        DealerDeliverySettlementMetaDialog(
            challan = challan,
            viewModel = vm,
            onDismiss = {
                editingSettlementChallan =
                    null
            },
            onSave = {
                    settlement,
                    cash,
                    receivedAt,
                    note ->

                vm.updateDeliverySettlementMeta(
                    challan = challan,
                    item = settlement,
                    cashHandedOver =
                        cash,
                    receivedAt =
                        receivedAt,
                    note = note
                ) {
                    if (it) {
                        editingSettlementChallan =
                            null
                    }
                }
            }
        )
    }

    editingDamage?.let { item ->
        DealerWarehouseDamageEditDialog(
            initial = item,
            onDismiss = {
                editingDamage = null
            },
            onSave = {
                    quantity,
                    reason,
                    note ->

                vm.updateWarehouseDamage(
                    item = item,
                    quantityPieces =
                        quantity,
                    reason = reason,
                    note = note
                ) {
                    if (it) {
                        editingDamage = null
                    }
                }
            }
        )
    }

    if (showPackSetup) {
        DealerPackSetupDialog(
            products = products,
            packs = productPacks,
            onDismiss = {
                showPackSetup = false
            },
            onDelete = { pack ->
                deleteTarget =
                    DealerDeleteTarget
                        .ProductPack(pack)
            },
            onSave = {
                    productId,
                    piecesPerBox,
                    piecesPerSheet ->

                vm.saveProductPack(
                    productId = productId,
                    piecesPerBox =
                        piecesPerBox,
                    piecesPerSheet =
                        piecesPerSheet
                ) {
                    if (it) {
                        showPackSetup =
                            false
                    }
                }
            }
        )
    }

    if (showDeliveryPerson) {
        DealerDeliveryPersonDialog(
            initial =
                editingDeliveryPerson,
            onDismiss = {
                showDeliveryPerson =
                    false

                editingDeliveryPerson =
                    null
            },
            onSave = {
                    name,
                    phone,
                    note ->

                val editing =
                    editingDeliveryPerson

                if (editing == null) {
                    vm.addDeliveryPerson(
                        name = name,
                        phone = phone,
                        note = note
                    ) {
                        if (it) {
                            showDeliveryPerson =
                                false
                        }
                    }
                } else {
                    vm.updateDeliveryPerson(
                        item = editing,
                        name = name,
                        phone = phone,
                        note = note
                    ) {
                        if (it) {
                            showDeliveryPerson =
                                false

                            editingDeliveryPerson =
                                null
                        }
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

    deleteTarget?.let { target ->
        val title =
            when (target) {
                is DealerDeleteTarget.Company ->
                    v15Text(
                        "কোম্পানি ডিলিট করবেন?",
                        "Delete company?"
                    )

                is DealerDeleteTarget.Area ->
                    v15Text(
                        "এরিয়া ডিলিট করবেন?",
                        "Delete area?"
                    )

                is DealerDeleteTarget.Customer ->
                    v15Text(
                        "রিটেইলার ডিলিট করবেন?",
                        "Delete retailer?"
                    )

                is DealerDeleteTarget.DeliveryPerson ->
                    v15Text(
                        "ডেলিভারি ম্যান ডিলিট করবেন?",
                        "Delete delivery person?"
                    )

                is DealerDeleteTarget.Collection ->
                    v15Text(
                        "কালেকশন ডিলিট করবেন?",
                        "Delete collection?"
                    )

                is DealerDeleteTarget.SupplierPayment ->
                    v15Text(
                        "সাপ্লায়ার পেমেন্ট ডিলিট করবেন?",
                        "Delete supplier payment?"
                    )

                is DealerDeleteTarget.Expense ->
                    v15Text(
                        "খরচ ডিলিট করবেন?",
                        "Delete expense?"
                    )

                is DealerDeleteTarget.ProductPack ->
                    v15Text(
                        "প্যাক সেটআপ ডিলিট করবেন?",
                        "Delete pack setup?"
                    )

                is DealerDeleteTarget.Purchase ->
                    v15Text(
                        "ক্রয় ডিলিট করবেন?",
                        "Delete purchase?"
                    )

                is DealerDeleteTarget.Sale ->
                    v15Text(
                        "বিক্রি ডিলিট করবেন?",
                        "Delete sale?"
                    )

                is DealerDeleteTarget.DeliveryChallan ->
                    v15Text(
                        "ডেলিভারি চালান ডিলিট করবেন?",
                        "Delete delivery challan?"
                    )

                is DealerDeleteTarget.ReopenSettlement ->
                    v15Text(
                        "সেটেলমেন্ট পুনরায় খুলবেন?",
                        "Reopen settlement?"
                    )

                is DealerDeleteTarget.WarehouseDamage ->
                    v15Text(
                        "ড্যামেজ এন্ট্রি ডিলিট করবেন?",
                        "Delete damage entry?"
                    )
            }

        val message =
            when (target) {
                is DealerDeleteTarget.Company ->
                    v15Text(
                        "${target.item.name} ডিলিট হবে। আগের ক্রয়/পেমেন্ট ইতিহাস থাকবে, তবে কোম্পানির লিংক সরবে।",
                        "${target.item.name} will be deleted. Existing purchase/payment history remains, but the company link will be removed."
                    )

                is DealerDeleteTarget.Area ->
                    v15Text(
                        "এরিয়া ডিলিট হলে সংশ্লিষ্ট রিটেইলার থাকবে, শুধু এরিয়া লিংক সরবে।",
                        "Retailers remain; only their area link is removed."
                    )

                is DealerDeleteTarget.Customer ->
                    v15Text(
                        "${target.item.name} ডিলিট হবে। আগের বিক্রি/কালেকশন snapshot ইতিহাস থাকবে।",
                        "${target.item.name} will be deleted. Historical sale/collection snapshots remain."
                    )

                is DealerDeleteTarget.DeliveryPerson ->
                    v15Text(
                        "ডেলিভারি ম্যান ডিলিট হবে। পুরনো চালানে নামের snapshot থাকবে।",
                        "The delivery person will be deleted. Existing challans keep the name snapshot."
                    )

                is DealerDeleteTarget.Collection ->
                    v15Text(
                        "এই কালেকশন ডিলিট হলে রিটেইলারের বাকি হিসাব স্বয়ংক্রিয়ভাবে পুনরায় হিসাব হবে।",
                        "Deleting this collection will automatically rebuild the retailer due allocation."
                    )

                is DealerDeleteTarget.SupplierPayment ->
                    v15Text(
                        "এই পেমেন্ট ডিলিট হলে সাপ্লায়ারের পাওনা হিসাব স্বয়ংক্রিয়ভাবে পুনরায় হিসাব হবে।",
                        "Deleting this payment will automatically rebuild supplier payment allocation."
                    )

                is DealerDeleteTarget.Expense ->
                    v15Text(
                        "এই ব্যবসার খরচ স্থায়ীভাবে ডিলিট হবে।",
                        "This business expense will be permanently deleted."
                    )

                is DealerDeleteTarget.ProductPack ->
                    v15Text(
                        "বক্স/পাতা conversion সেটআপ মুছে যাবে। পণ্যের stock পিসে অপরিবর্তিত থাকবে।",
                        "Box/sheet conversion will be removed. Piece stock remains unchanged."
                    )

                is DealerDeleteTarget.Purchase ->
                    v15Text(
                        "ক্রয়টি শুধু তখনই ডিলিট হবে যখন এর stock অন্য বিক্রি/ডেলিভারি/ড্যামেজে ব্যবহার হয়নি। পেমেন্ট allocation পুনরায় হিসাব হবে।",
                        "The purchase is deleted only when its stock has not been used by sale, delivery or damage. Payment allocations are rebuilt."
                    )

                is DealerDeleteTarget.Sale ->
                    v15Text(
                        "বিক্রি ডিলিট হলে stock effect reverse হবে এবং রিটেইলারের কালেকশন/বাকি allocation পুনরায় হিসাব হবে।",
                        "Deleting the sale reverses its stock effect and rebuilds retailer collection/due allocations."
                    )

                is DealerDeleteTarget.DeliveryChallan ->
                    v15Text(
                        "OPEN চালানে কোনো বিক্রি বা সেটেলমেন্ট না থাকলেই ডিলিট হবে। চালানের মাল warehouse stock-এ ফেরত যাবে।",
                        "Only an OPEN challan with no sale or settlement can be deleted. Issued goods return to warehouse stock."
                    )

                is DealerDeleteTarget.ReopenSettlement ->
                    v15Text(
                        "রাতের সেটেলমেন্ট reverse করে চালান আবার OPEN হবে। ফেরত মাল delivery custody-তে যাবে এবং settlement damage entry সরবে।",
                        "The night settlement will be reversed and the challan reopened. Returned stock goes back to delivery custody and settlement damage records are removed."
                    )

                is DealerDeleteTarget.WarehouseDamage ->
                    v15Text(
                        "ড্যামেজ এন্ট্রি মুছলে ঐ পরিমাণ পণ্য আবার সংশ্লিষ্ট stock batch-এ ফেরত যাবে।",
                        "Deleting this damage entry restores the quantity to its stock batch."
                    )
            }

        val finishDelete:
            (Boolean) -> Unit = { ok ->
                Toast.makeText(
                    context,
                    if (ok) {
                        v15Text(
                            "ডিলিট হয়েছে",
                            "Deleted"
                        )
                    } else {
                        v15Text(
                            "ডিলিট করা যায়নি। সংযুক্ত হিসাব/স্টক যাচাই করুন।",
                            "Delete failed. Check linked accounting/stock."
                        )
                    },
                    Toast.LENGTH_SHORT
                ).show()

                if (ok) {
                    deleteTarget = null
                }
            }

        ProtectedDeleteDialog(
            viewModel =
                securityViewModel,
            title = title,
            message = message,
            onDismiss = {
                deleteTarget = null
            },
            onConfirmed = {
                when (target) {
                    is DealerDeleteTarget.Company ->
                        vm.deleteCompany(
                            target.item,
                            finishDelete
                        )

                    is DealerDeleteTarget.Area ->
                        vm.deleteArea(
                            target.item,
                            finishDelete
                        )

                    is DealerDeleteTarget.Customer ->
                        vm.deleteCustomer(
                            target.item,
                            finishDelete
                        )

                    is DealerDeleteTarget.DeliveryPerson ->
                        vm.deleteDeliveryPerson(
                            target.item,
                            finishDelete
                        )

                    is DealerDeleteTarget.Collection ->
                        vm.deleteCollection(
                            target.item,
                            finishDelete
                        )

                    is DealerDeleteTarget.SupplierPayment ->
                        vm.deleteSupplierPayment(
                            target.item,
                            finishDelete
                        )

                    is DealerDeleteTarget.Expense ->
                        vm.deleteExpense(
                            target.item,
                            finishDelete
                        )

                    is DealerDeleteTarget.ProductPack ->
                        vm.deleteProductPack(
                            target.item,
                            finishDelete
                        )

                    is DealerDeleteTarget.Purchase ->
                        vm.deletePurchase(
                            target.item,
                            finishDelete
                        )

                    is DealerDeleteTarget.Sale ->
                        vm.deleteSale(
                            target.item,
                            finishDelete
                        )

                    is DealerDeleteTarget.DeliveryChallan ->
                        vm.deleteDeliveryChallan(
                            target.item,
                            finishDelete
                        )

                    is DealerDeleteTarget.ReopenSettlement ->
                        vm.reopenDeliverySettlement(
                            target.item,
                            finishDelete
                        )

                    is DealerDeleteTarget.WarehouseDamage ->
                        vm.deleteWarehouseDamage(
                            target.item,
                            finishDelete
                        )
                }
            }
        )
    }

}

@Composable
private fun DealerCustomerLedgerScreen(
    customer: DealerCustomerEntity,
    summary:
        com.familykhata.app.dealerbusiness
            .DealerCustomerLedgerSummary?,
    sales: List<DealerSaleEntity>,
    collections: List<DealerCollectionEntity>,
    onOpenSale: (DealerSaleEntity) -> Unit
) {
    val grossSales =
        summary?.grossSales ?: 0.0

    val totalReturns =
        summary?.totalReturns ?: 0.0

    val netSales =
        (
            grossSales -
                totalReturns
        ).coerceAtLeast(0.0)

    val totalCollections =
        summary?.totalCollections ?: 0.0

    val due =
        (
            netSales -
                totalCollections
        ).coerceAtLeast(0.0)

    val advance =
        (
            totalCollections -
                netSales
        ).coerceAtLeast(0.0)

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
            customer.name,
            style =
                MaterialTheme
                    .typography
                    .titleLarge,
            fontWeight =
                FontWeight.ExtraBold
        )

        if (
            customer.customerCode
                .isNotBlank()
        ) {
            Text(
                v15Text(
                    "কোড: ${customer.customerCode}",
                    "Code: ${customer.customerCode}"
                )
            )
        }

        if (customer.phone.isNotBlank()) {
            Text(
                v15Text(
                    "ফোন: ${customer.phone}",
                    "Phone: ${customer.phone}"
                )
            )
        }

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            DealerBusinessMetric(
                title =
                    v15Text(
                        "মোট বিক্রি",
                        "Gross sales"
                    ),
                value =
                    dealerBusinessMoney(
                        grossSales
                    ),
                modifier =
                    Modifier.weight(1f)
            )

            DealerBusinessMetric(
                title =
                    v15Text(
                        "রিটার্ন",
                        "Returns"
                    ),
                value =
                    dealerBusinessMoney(
                        totalReturns
                    ),
                modifier =
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
                title =
                    v15Text(
                        "নেট বিক্রি",
                        "Net sales"
                    ),
                value =
                    dealerBusinessMoney(
                        netSales
                    ),
                modifier =
                    Modifier.weight(1f)
            )

            DealerBusinessMetric(
                title =
                    v15Text(
                        "মোট আদায়",
                        "Collected"
                    ),
                value =
                    dealerBusinessMoney(
                        totalCollections
                    ),
                modifier =
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
                title =
                    v15Text(
                        "বর্তমান বাকি",
                        "Current due"
                    ),
                value =
                    dealerBusinessMoney(
                        due
                    ),
                modifier =
                    Modifier.weight(1f)
            )

            DealerBusinessMetric(
                title =
                    v15Text(
                        "অগ্রিম",
                        "Advance"
                    ),
                value =
                    dealerBusinessMoney(
                        advance
                    ),
                modifier =
                    Modifier.weight(1f)
            )
        }

        if (customer.creditLimit > 0.0) {
            Text(
                v15Text(
                    "ক্রেডিট সীমা: ${
                        dealerBusinessMoney(
                            customer.creditLimit
                        )
                    }",
                    "Credit limit: ${
                        dealerBusinessMoney(
                            customer.creditLimit
                        )
                    }"
                ),
                fontWeight =
                    FontWeight.Bold
            )

            val remainingCredit =
                (
                    customer.creditLimit -
                        due
                ).coerceAtLeast(0.0)

            Text(
                v15Text(
                    "অবশিষ্ট ক্রেডিট: ${
                        dealerBusinessMoney(
                            remainingCredit
                        )
                    }",
                    "Remaining credit: ${
                        dealerBusinessMoney(
                            remainingCredit
                        )
                    }"
                )
            )
        }

        Text(
            v15Text(
                "ইনভয়েস ইতিহাস",
                "Invoice history"
            ),
            fontWeight =
                FontWeight.Bold
        )

        if (sales.isEmpty()) {
            Text(
                v15Text(
                    "কোনো বিক্রয় ইনভয়েস নেই।",
                    "No sales invoice."
                )
            )
        }

        sales
            .sortedWith(
                compareByDescending<DealerSaleEntity> {
                    it.soldAt
                }.thenByDescending {
                    it.id
                }
            )
            .forEach { sale ->
                Card(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier =
                            Modifier.padding(
                                12.dp
                            ),
                        verticalArrangement =
                            Arrangement.spacedBy(
                                3.dp
                            )
                    ) {
                        Text(
                            sale.invoiceNo
                                .ifBlank {
                                    v15Text(
                                        "ইনভয়েস #${sale.id}",
                                        "Invoice #${sale.id}"
                                    )
                                },
                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            dealerBusinessDate(
                                sale.soldAt
                            )
                        )

                        Text(
                            v15Text(
                                "স্ট্যাটাস: ${sale.status}",
                                "Status: ${sale.status}"
                            )
                        )

                        OutlinedButton(
                            onClick = {
                                onOpenSale(sale)
                            },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                        ) {
                            Text(
                                v15Text(
                                    "ইনভয়েস বিস্তারিত",
                                    "Invoice details"
                                )
                            )
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
                    "কোনো কালেকশন নেই।",
                    "No collection."
                )
            )
        }

        collections
            .sortedWith(
                compareByDescending<DealerCollectionEntity> {
                    it.collectedAt
                }.thenByDescending {
                    it.id
                }
            )
            .forEach { collection ->
                Card(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier =
                            Modifier.padding(
                                12.dp
                            ),
                        verticalArrangement =
                            Arrangement.spacedBy(
                                3.dp
                            )
                    ) {
                        Text(
                            dealerBusinessMoney(
                                collection.amount
                            ),
                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            dealerBusinessDate(
                                collection
                                    .collectedAt
                            )
                        )

                        if (
                            collection.note
                                .isNotBlank()
                        ) {
                            Text(
                                collection.note
                            )
                        }
                    }
                }
            }
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

    var deletingReturn by remember {
        mutableStateOf<DealerSalesReturnEntity?>(
            null
        )
    }

    val deleteSecurityViewModel:
        FamilyKhataViewModel = viewModel()

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
                        Row(
                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    6.dp
                                )
                        ) {
                            TextButton(
                                onClick = {
                                    editingReturn =
                                        item
                                }
                            ) {
                                Text(
                                    v15Text(
                                        "সম্পাদনা",
                                        "Edit"
                                    )
                                )
                            }

                            TextButton(
                                onClick = {
                                    deletingReturn =
                                        item
                                }
                            ) {
                                Text(
                                    v15Text(
                                        "ডিলিট",
                                        "Delete"
                                    )
                                )
                            }
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

    deletingReturn?.let { item ->
        ProtectedDeleteDialog(
            viewModel =
                deleteSecurityViewModel,
            title =
                v15Text(
                    "রিটার্ন ডিলিট করবেন?",
                    "Delete return?"
                ),
            message =
                v15Text(
                    "রিটার্নের stock ও বাকি হিসাব reverse হবে। ফেরত stock পরে ব্যবহার হয়ে থাকলে নিরাপত্তার জন্য ডিলিট বন্ধ হবে।",
                    "Return stock and due effects will be reversed. Delete is blocked if restored stock has already been consumed."
                ),
            onDismiss = {
                deletingReturn = null
            },
            onConfirmed = {
                viewModel.deleteSalesReturn(
                    item
                ) { ok ->
                    Toast.makeText(
                        context,
                        if (ok) {
                            v15Text(
                                "রিটার্ন ডিলিট হয়েছে",
                                "Return deleted"
                            )
                        } else {
                            v15Text(
                                "রিটার্ন ডিলিট করা যায়নি",
                                "Return could not be deleted"
                            )
                        },
                        Toast.LENGTH_SHORT
                    ).show()

                    if (ok) {
                        deletingReturn = null
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

    var deletingReturn by remember {
        mutableStateOf<DealerPurchaseReturnEntity?>(
            null
        )
    }

    val deleteSecurityViewModel:
        FamilyKhataViewModel = viewModel()

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
                        Row(
                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    6.dp
                                )
                        ) {
                            TextButton(
                                onClick = {
                                    editingReturn =
                                        item
                                }
                            ) {
                                Text(
                                    v15Text(
                                        "সম্পাদনা",
                                        "Edit"
                                    )
                                )
                            }

                            TextButton(
                                onClick = {
                                    deletingReturn =
                                        item
                                }
                            ) {
                                Text(
                                    v15Text(
                                        "ডিলিট",
                                        "Delete"
                                    )
                                )
                            }
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

    deletingReturn?.let { item ->
        ProtectedDeleteDialog(
            viewModel =
                deleteSecurityViewModel,
            title =
                v15Text(
                    "পারচেজ রিটার্ন ডিলিট করবেন?",
                    "Delete purchase return?"
                ),
            message =
                v15Text(
                    "ডিলিট করলে রিটার্ন করা পণ্য আবার stock-এ যোগ হবে এবং supplier payable পুনরায় হিসাব হবে।",
                    "Deleting this return adds the quantity back to stock and rebuilds supplier payable allocations."
                ),
            onDismiss = {
                deletingReturn = null
            },
            onConfirmed = {
                viewModel.deletePurchaseReturn(
                    item
                ) { ok ->
                    Toast.makeText(
                        context,
                        if (ok) {
                            v15Text(
                                "পারচেজ রিটার্ন ডিলিট হয়েছে",
                                "Purchase return deleted"
                            )
                        } else {
                            v15Text(
                                "পারচেজ রিটার্ন ডিলিট করা যায়নি",
                                "Purchase return could not be deleted"
                            )
                        },
                        Toast.LENGTH_SHORT
                    ).show()

                    if (ok) {
                        deletingReturn = null
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

                            cost = ""
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
private fun DealerDeliverySaleDialog(
    challan: DealerDeliveryChallanEntity,
    customers:
        List<DealerCustomerEntity>,
    viewModel:
        DealerBusinessViewModel,
    onDismiss: () -> Unit,
    onSave: (
        Long,
        String,
        List<
            com.familykhata.app.dealerbusiness
                .DealerDeliverySaleLineInput
        >,
        Double,
        String
    ) -> Unit
) {
    var customerId by
        remember(challan.id) {
            mutableStateOf<Long?>(null)
        }

    var invoiceNo by
        remember(challan.id) {
            mutableStateOf("")
        }

    var collectedNow by
        remember(challan.id) {
            mutableStateOf("")
        }

    var note by
        remember(challan.id) {
            mutableStateOf("")
        }

    var statuses by
        remember(challan.id) {
            mutableStateOf(
                emptyList<
                    com.familykhata.app
                        .dealerbusiness
                        .DealerDeliveryLineStatus
                >()
            )
        }

    var loading by
        remember(challan.id) {
            mutableStateOf(true)
        }

    var quantityValues by
        remember(challan.id) {
            mutableStateOf<
                Map<Long, String>
            >(
                emptyMap()
            )
        }

    var rateValues by
        remember(challan.id) {
            mutableStateOf<
                Map<Long, String>
            >(
                emptyMap()
            )
        }

    LaunchedEffect(
        challan.id
    ) {
        loading = true

        statuses =
            viewModel
                .loadDeliveryLineStatuses(
                    challan.id
                )

        loading = false
    }

    AlertDialog(
        onDismissRequest =
            onDismiss,
        title = {
            Text(
                v15Text(
                    "ডেলিভারি বিক্রি / বাকি",
                    "Delivery sale / due"
                )
            )
        },
        text = {
            Column(
                modifier =
                    Modifier
                        .heightIn(
                            max = 600.dp
                        )
                        .verticalScroll(
                            rememberScrollState()
                        ),
                verticalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    )
            ) {
                Text(
                    v15Text(
                        "চালান: ${challan.challanNo}",
                        "Challan: ${challan.challanNo}"
                    ),
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    challan
                        .deliveryPersonNameSnapshot
                )

                Text(
                    v15Text(
                        "রিটেইলার নির্বাচন",
                        "Select retailer"
                    ),
                    fontWeight =
                        FontWeight.Bold
                )

                customers.forEach {
                        customer ->

                    OutlinedButton(
                        onClick = {
                            customerId =
                                customer.id
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
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
                    value =
                        invoiceNo,
                    onValueChange = {
                        invoiceNo = it
                    },
                    label = {
                        Text(
                            v15Text(
                                "ইনভয়েস নং",
                                "Invoice no."
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                Text(
                    v15Text(
                        "বিক্রিত মাল",
                        "Sold items"
                    ),
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    v15Text(
                        "চালানে যে Unit-এ মাল দেওয়া হয়েছিল সেটি পাশে দেখানো হবে। বিক্রি ও দর-এর ঘর Base quantity অনুযায়ী থাকবে, যাতে stock হিসাব নির্ভুল থাকে।",
                        "The challan unit is shown beside each quantity. Sale quantity and rate are entered in base quantity so stock accounting remains exact."
                    ),
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )

                if (loading) {
                    Text(
                        v15Text(
                            "লোড হচ্ছে...",
                            "Loading..."
                        )
                    )
                }

                statuses.forEach {
                        status ->

                    val line =
                        status.line

                    Card(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                    ) {
                        Column(
                            modifier =
                                Modifier.padding(
                                    10.dp
                                ),
                            verticalArrangement =
                                Arrangement
                                    .spacedBy(
                                        6.dp
                                    )
                        ) {
                            Text(
                                line.productNameSnapshot,
                                fontWeight =
                                    FontWeight.Bold
                            )

                            Text(
                                v15Text(
                                    "দেওয়া ${dealerDeliveryQuantityText(line.quantityPieces, line)} • আগে বিক্রি ${dealerDeliveryQuantityText(status.soldPieces, line)} • বাকি ${dealerDeliveryQuantityText(status.remainingPieces, line)}",
                                    "Issued ${dealerDeliveryQuantityText(line.quantityPieces, line)} • already sold ${dealerDeliveryQuantityText(status.soldPieces, line)} • remaining ${dealerDeliveryQuantityText(status.remainingPieces, line)}"
                                )
                            )

                            if (
                                status.remainingPieces >
                                    0
                            ) {
                                OutlinedTextField(
                                    value =
                                        quantityValues[
                                            line.id
                                        ].orEmpty(),
                                    onValueChange = {
                                        value ->

                                        quantityValues =
                                            quantityValues +
                                                (
                                                    line.id to
                                                        value
                                                )
                                    },
                                    label = {
                                        Text(
                                            dealerDeliveryBaseQuantityLabel(
                                                line = line,
                                                banglaAction =
                                                    "এই রিটেইলারে বিক্রি",
                                                englishAction =
                                                    "Sold to this retailer"
                                            )
                                        )
                                    },
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value =
                                        rateValues[
                                            line.id
                                        ].orEmpty(),
                                    onValueChange = {
                                        value ->

                                        rateValues =
                                            rateValues +
                                                (
                                                    line.id to
                                                        value
                                                )
                                    },
                                    label = {
                                        Text(
                                            v15Text(
                                                "বিক্রয় দর / বেস পরিমাণ",
                                                "Selling rate / base quantity"
                                            )
                                        )
                                    },
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                )
                            } else {
                                Text(
                                    v15Text(
                                        "এই পণ্যের দেওয়া সব মাল ইতিমধ্যে বিক্রি হয়েছে।",
                                        "All issued quantity has already been sold."
                                    )
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value =
                        collectedNow,
                    onValueChange = {
                        collectedNow = it
                    },
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

                Text(
                    v15Text(
                        "এখন আদায় না হলে ০ রাখুন। বাকি স্বয়ংক্রিয়ভাবে রিটেইলারের বাকি খাতায় যাবে।",
                        "Use 0 if unpaid. The balance automatically goes to the retailer due ledger."
                    ),
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )

                OutlinedTextField(
                    value =
                        note,
                    onValueChange = {
                        note = it
                    },
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
                    val selectedCustomer =
                        customerId
                            ?: return@TextButton

                    val saleLines =
                        statuses
                            .mapNotNull {
                                    status ->

                                val quantity =
                                    quantityValues[
                                        status.line.id
                                    ]
                                        ?.dealerBusinessInt()
                                        ?: 0

                                val rate =
                                    rateValues[
                                        status.line.id
                                    ]
                                        ?.trim()
                                        ?.replace(
                                            ",",
                                            ""
                                        )
                                        ?.toDoubleOrNull()
                                        ?: -1.0

                                if (
                                    quantity > 0 &&
                                    quantity <=
                                        status
                                            .remainingPieces &&
                                    rate > 0
                                ) {
                                    com.familykhata.app
                                        .dealerbusiness
                                        .DealerDeliverySaleLineInput(
                                            challanLineId =
                                                status
                                                    .line
                                                    .id,
                                            quantityPieces =
                                                quantity,
                                            unitPrice =
                                                rate
                                        )
                                } else {
                                    null
                                }
                            }

                    val invalidQuantity =
                        statuses.any {
                                status ->

                            val raw =
                                quantityValues[
                                    status.line.id
                                ]
                                    ?.dealerBusinessInt()
                                    ?: 0

                            raw <
                                0 ||
                                raw >
                                    status
                                        .remainingPieces
                        }

                    val invalidRate =
                        statuses.any {
                                status ->

                            val quantity =
                                quantityValues[
                                    status.line.id
                                ]
                                    ?.dealerBusinessInt()
                                    ?: 0

                            val rate =
                                rateValues[
                                    status.line.id
                                ]
                                    ?.trim()
                                    ?.replace(
                                        ",",
                                        ""
                                    )
                                    ?.toDoubleOrNull()

                            quantity > 0 &&
                                (
                                    rate == null ||
                                        rate <= 0
                                )
                        }

                    val collectionText =
                        collectedNow
                            .trim()
                            .replace(
                                ",",
                                ""
                            )

                    val collection =
                        if (
                            collectionText.isBlank()
                        ) {
                            0.0
                        } else {
                            collectionText
                                .toDoubleOrNull()
                                ?: return@TextButton
                        }

                    if (
                        saleLines.isNotEmpty() &&
                        !invalidQuantity &&
                        !invalidRate &&
                        collection >= 0
                    ) {
                        onSave(
                            selectedCustomer,
                            invoiceNo,
                            saleLines,
                            collection,
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
                onClick =
                    onDismiss
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
private fun DealerDeliverySettlementDialog(
    challan:
        DealerDeliveryChallanEntity,
    viewModel:
        DealerBusinessViewModel,
    onDismiss: () -> Unit,
    onSave: (
        List<
            com.familykhata.app.dealerbusiness
                .DealerDeliverySettlementLineInput
        >,
        Double,
        String
    ) -> Unit
) {
    var statuses by
        remember(challan.id) {
            mutableStateOf(
                emptyList<
                    com.familykhata.app
                        .dealerbusiness
                        .DealerDeliveryLineStatus
                >()
            )
        }

    var loading by
        remember(challan.id) {
            mutableStateOf(true)
        }

    var returnedValues by
        remember(challan.id) {
            mutableStateOf<
                Map<Long, String>
            >(
                emptyMap()
            )
        }

    var damagedValues by
        remember(challan.id) {
            mutableStateOf<
                Map<Long, String>
            >(
                emptyMap()
            )
        }

    var lineNotes by
        remember(challan.id) {
            mutableStateOf<
                Map<Long, String>
            >(
                emptyMap()
            )
        }

    var cash by
        remember(challan.id) {
            mutableStateOf("")
        }

    var note by
        remember(challan.id) {
            mutableStateOf("")
        }

    LaunchedEffect(
        challan.id
    ) {
        loading = true

        statuses =
            viewModel
                .loadDeliveryLineStatuses(
                    challan.id
                )

        loading = false
    }

    AlertDialog(
        onDismissRequest =
            onDismiss,
        title = {
            Text(
                v15Text(
                    "রাতে মাল বুঝে নেওয়া",
                    "Night settlement"
                )
            )
        },
        text = {
            Column(
                modifier =
                    Modifier
                        .heightIn(
                            max = 620.dp
                        )
                        .verticalScroll(
                            rememberScrollState()
                        ),
                verticalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    )
            ) {
                Text(
                    v15Text(
                        "চালান: ${challan.challanNo}",
                        "Challan: ${challan.challanNo}"
                    ),
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    challan
                        .deliveryPersonNameSnapshot
                )

                Text(
                    v15Text(
                        "প্রতি পণ্যে: দেওয়া = বিক্রি + ফেরত + ড্যামেজ",
                        "For every item: issued = sold + returned + damaged"
                    ),
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    v15Text(
                        "Unit conversion পাশে দেখা যাবে। ফেরত ও ড্যামেজ Base quantity-তে লিখুন; ভিতরের stock reconciliation Base quantity-তেই হবে।",
                        "Unit conversions are shown beside the quantities. Enter returned and damaged amounts in base quantity; stock reconciliation remains in base quantity."
                    ),
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )

                if (loading) {
                    Text(
                        v15Text(
                            "লোড হচ্ছে...",
                            "Loading..."
                        )
                    )
                }

                statuses.forEach {
                        status ->

                    val line =
                        status.line

                    val returned =
                        returnedValues[
                            line.id
                        ]
                            ?.dealerBusinessInt()
                            ?: 0

                    val damaged =
                        damagedValues[
                            line.id
                        ]
                            ?.dealerBusinessInt()
                            ?: 0

                    val difference =
                        status.remainingPieces -
                            returned -
                            damaged

                    Card(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                    ) {
                        Column(
                            modifier =
                                Modifier.padding(
                                    10.dp
                                ),
                            verticalArrangement =
                                Arrangement
                                    .spacedBy(
                                        6.dp
                                    )
                        ) {
                            Text(
                                line.productNameSnapshot,
                                fontWeight =
                                    FontWeight.Bold
                            )

                            Text(
                                v15Text(
                                    "দেওয়া: ${dealerDeliveryQuantityText(line.quantityPieces, line)} • বিক্রি: ${dealerDeliveryQuantityText(status.soldPieces, line)} • বুঝে নিতে বাকি: ${dealerDeliveryQuantityText(status.remainingPieces, line)}",
                                    "Issued: ${dealerDeliveryQuantityText(line.quantityPieces, line)} • sold: ${dealerDeliveryQuantityText(status.soldPieces, line)} • remaining to settle: ${dealerDeliveryQuantityText(status.remainingPieces, line)}"
                                )
                            )

                            OutlinedTextField(
                                value =
                                    returnedValues[
                                        line.id
                                    ].orEmpty(),
                                onValueChange = {
                                    value ->

                                    returnedValues =
                                        returnedValues +
                                            (
                                                line.id to
                                                    value
                                            )
                                },
                                label = {
                                    Text(
                                        dealerDeliveryBaseQuantityLabel(
                                            line = line,
                                            banglaAction =
                                                "ফেরত",
                                            englishAction =
                                                "Returned"
                                        )
                                    )
                                },
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                            )

                            OutlinedTextField(
                                value =
                                    damagedValues[
                                        line.id
                                    ].orEmpty(),
                                onValueChange = {
                                    value ->

                                    damagedValues =
                                        damagedValues +
                                            (
                                                line.id to
                                                    value
                                            )
                                },
                                label = {
                                    Text(
                                        dealerDeliveryBaseQuantityLabel(
                                            line = line,
                                            banglaAction =
                                                "ড্যামেজ",
                                            englishAction =
                                                "Damaged"
                                        )
                                    )
                                },
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                            )

                            OutlinedTextField(
                                value =
                                    lineNotes[
                                        line.id
                                    ].orEmpty(),
                                onValueChange = {
                                    value ->

                                    lineNotes =
                                        lineNotes +
                                            (
                                                line.id to
                                                    value
                                            )
                                },
                                label = {
                                    Text(
                                        v15Text(
                                            "লাইন নোট",
                                            "Line note"
                                        )
                                    )
                                },
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                            )

                            Text(
                                if (difference == 0) {
                                    v15Text(
                                        "✓ হিসাব মিলেছে",
                                        "✓ Balanced"
                                    )
                                } else {
                                    v15Text(
                                        "আরও মিলাতে হবে: ${dealerDeliveryQuantityText(difference.coerceAtLeast(0), line)}",
                                        "Difference: ${dealerDeliveryQuantityText(difference.coerceAtLeast(0), line)}"
                                    )
                                },
                                fontWeight =
                                    FontWeight.Bold
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value =
                        cash,
                    onValueChange = {
                        cash = it
                    },
                    label = {
                        Text(
                            v15Text(
                                "ডেলিভারি ম্যানের জমা নগদ",
                                "Cash handed over"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                Text(
                    v15Text(
                        "এই ঘরটি রাতের নগদ হস্তান্তরের রেকর্ড। রিটেইলারের বাকি/আদায়ের হিসাব বিক্রির সময়ের entry থেকেই হবে।",
                        "This records the delivery person's night cash handover. Retailer due/collection accounting comes from the sale entries."
                    ),
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )

                OutlinedTextField(
                    value =
                        note,
                    onValueChange = {
                        note = it
                    },
                    label = {
                        Text(
                            v15Text(
                                "সেটেলমেন্ট নোট",
                                "Settlement note"
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
                    if (
                        statuses.isEmpty() ||
                        loading
                    ) {
                        return@TextButton
                    }

                    val settlementLines =
                        statuses.map {
                                status ->

                            val returned =
                                returnedValues[
                                    status.line.id
                                ]
                                    ?.dealerBusinessInt()
                                    ?: 0

                            val damaged =
                                damagedValues[
                                    status.line.id
                                ]
                                    ?.dealerBusinessInt()
                                    ?: 0

                            com.familykhata.app
                                .dealerbusiness
                                .DealerDeliverySettlementLineInput(
                                    challanLineId =
                                        status.line.id,
                                    returnedPieces =
                                        returned,
                                    damagedPieces =
                                        damaged,
                                    note =
                                        lineNotes[
                                            status.line.id
                                        ].orEmpty()
                                )
                        }

                    val balanced =
                        statuses.all {
                                status ->

                            val returned =
                                returnedValues[
                                    status.line.id
                                ]
                                    ?.dealerBusinessInt()
                                    ?: 0

                            val damaged =
                                damagedValues[
                                    status.line.id
                                ]
                                    ?.dealerBusinessInt()
                                    ?: 0

                            returned >= 0 &&
                                damaged >= 0 &&
                                status.soldPieces +
                                    returned +
                                    damaged ==
                                    status.line
                                        .quantityPieces
                        }

                    val cashText =
                        cash
                            .trim()
                            .replace(
                                ",",
                                ""
                            )

                    val cashValue =
                        if (
                            cashText.isBlank()
                        ) {
                            0.0
                        } else {
                            cashText
                                .toDoubleOrNull()
                                ?: return@TextButton
                        }

                    if (
                        balanced &&
                        cashValue >= 0
                    ) {
                        onSave(
                            settlementLines,
                            cashValue,
                            note
                        )
                    }
                }
            ) {
                Text(
                    v15Text(
                        "বুঝে নিন / চালান বন্ধ",
                        "Settle / close challan"
                    )
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick =
                    onDismiss
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
private fun DealerDeliveryChallanDialog(
    viewModel:
        DealerBusinessViewModel,
    deliveryPeople:
        List<DealerDeliveryPersonEntity>,
    products:
        List<com.familykhata.app.data.ProductEntity>,
    onDismiss: () -> Unit,
    onSave: (
        Long,
        String,
        List<com.familykhata.app.dealerbusiness.DealerDeliveryChallanLineInput>,
        String
    ) -> Unit
) {
    var deliveryPersonId by remember {
        mutableStateOf<Long?>(null)
    }

    var challanNo by remember {
        mutableStateOf("")
    }

    var note by remember {
        mutableStateOf("")
    }

    var selectedProductId by remember {
        mutableStateOf<Long?>(null)
    }

    var selectedUnitName by remember {
        mutableStateOf("")
    }

    var selectedUnitFactor by remember {
        mutableStateOf(1)
    }

    var quantity by remember {
        mutableStateOf("")
    }

    var lines by remember {
        mutableStateOf(
            emptyList<
                com.familykhata.app.dealerbusiness
                    .DealerDeliveryChallanLineInput
            >()
        )
    }

    AlertDialog(
        onDismissRequest =
            onDismiss,
        title = {
            Text(
                v15Text(
                    "মাল দেওয়ার চালান",
                    "Issue delivery challan"
                )
            )
        },
        text = {
            Column(
                modifier =
                    Modifier
                        .heightIn(
                            max = 560.dp
                        )
                        .verticalScroll(
                            rememberScrollState()
                        ),
                verticalArrangement =
                    Arrangement.spacedBy(
                        7.dp
                    )
            ) {
                Text(
                    v15Text(
                        "ডেলিভারি ম্যান নির্বাচন",
                        "Select delivery man"
                    ),
                    fontWeight =
                        FontWeight.Bold
                )

                deliveryPeople.forEach {
                        person ->

                    OutlinedButton(
                        onClick = {
                            deliveryPersonId =
                                person.id
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (
                                deliveryPersonId ==
                                    person.id
                            ) {
                                "✓ ${person.name}"
                            } else {
                                person.name
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value =
                        challanNo,
                    onValueChange = {
                        challanNo = it
                    },
                    label = {
                        Text(
                            v15Text(
                                "চালান নং (ঐচ্ছিক)",
                                "Challan no. (optional)"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                Text(
                    v15Text(
                        "মাল যোগ করুন",
                        "Add products"
                    ),
                    fontWeight =
                        FontWeight.Bold
                )

                products.forEach { product ->
                    OutlinedButton(
                        onClick = {
                            selectedProductId =
                                product.id

                            selectedUnitName =
                                product.unit
                                    .trim()
                                    .ifBlank {
                                        "pcs"
                                    }

                            selectedUnitFactor =
                                1

                            quantity =
                                ""
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (
                                selectedProductId ==
                                    product.id
                            ) {
                                "✓ ${product.name}"
                            } else {
                                product.name
                            }
                        )
                    }
                }

                selectedProductId?.let {
                        productId ->

                    val product =
                        products.firstOrNull {
                            it.id ==
                                productId
                        }

                    if (product != null) {
                        val baseUnit =
                            product.unit
                                .trim()
                                .ifBlank {
                                    "pcs"
                                }

                        val unitFlow =
                            remember(
                                productId
                            ) {
                                viewModel
                                    .observeProductUnitConversions(
                                        productId
                                    )
                            }

                        val unitConversions by
                            unitFlow.collectAsState(
                                initial =
                                    emptyList()
                            )

                        val unitOptions =
                            remember(
                                baseUnit,
                                unitConversions
                            ) {
                                buildList<
                                    Pair<String, Int>
                                > {
                                    add(
                                        baseUnit to 1
                                    )

                                    unitConversions
                                        .filter {
                                            it.unitName
                                                .isNotBlank() &&
                                                it.baseQuantity >
                                                    1 &&
                                                !it.unitName
                                                    .equals(
                                                        baseUnit,
                                                        ignoreCase =
                                                            true
                                                    )
                                        }
                                        .forEach {
                                            conversion ->

                                            add(
                                                conversion.unitName to
                                                    conversion.baseQuantity
                                            )
                                        }
                                }
                            }

                        /*
                         * Product may have been selected before its
                         * unit-conversion Flow finished loading.
                         * Base unit remains the safe default.
                         */
                        LaunchedEffect(
                            productId,
                            baseUnit
                        ) {
                            if (
                                selectedUnitName
                                    .isBlank()
                            ) {
                                selectedUnitName =
                                    baseUnit

                                selectedUnitFactor =
                                    1
                            }
                        }

                        Text(
                            product.name,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            v15Text(
                                "বেস ইউনিট: $baseUnit",
                                "Base unit: $baseUnit"
                            ),
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall
                        )

                        Text(
                            v15Text(
                                "চালানের ইউনিট",
                                "Challan unit"
                            ),
                            fontWeight =
                                FontWeight.SemiBold
                        )

                        unitOptions.forEach {
                                option ->

                            val unitName =
                                option.first

                            val unitFactor =
                                option.second

                            OutlinedButton(
                                onClick = {
                                    selectedUnitName =
                                        unitName

                                    selectedUnitFactor =
                                        unitFactor

                                    quantity =
                                        ""
                                },
                                modifier =
                                    Modifier.fillMaxWidth()
                            ) {
                                val relation =
                                    if (
                                        unitFactor ==
                                        1
                                    ) {
                                        unitName
                                    } else {
                                        "$unitName • 1 = " +
                                            "$unitFactor $baseUnit"
                                    }

                                Text(
                                    if (
                                        selectedUnitName
                                            .equals(
                                                unitName,
                                                ignoreCase =
                                                    true
                                            ) &&
                                        selectedUnitFactor ==
                                            unitFactor
                                    ) {
                                        "✓ $relation"
                                    } else {
                                        relation
                                    }
                                )
                            }
                        }

                        OutlinedTextField(
                            value =
                                quantity,
                            onValueChange = {
                                quantity = it
                            },
                            label = {
                                Text(
                                    v15Text(
                                        "পরিমাণ ($selectedUnitName)",
                                        "Quantity ($selectedUnitName)"
                                    )
                                )
                            },
                            singleLine = true,
                            modifier =
                                Modifier.fillMaxWidth()
                        )

                        val enteredQuantity =
                            quantity
                                .dealerBusinessInt()

                        if (
                            enteredQuantity !=
                                null &&
                            enteredQuantity >
                                0
                        ) {
                            val baseQuantity =
                                enteredQuantity
                                    .toLong() *
                                    selectedUnitFactor
                                        .toLong()

                            if (
                                selectedUnitFactor >
                                1
                            ) {
                                Text(
                                    "$enteredQuantity " +
                                        "$selectedUnitName = " +
                                        "$baseQuantity " +
                                        baseUnit,
                                    style =
                                        MaterialTheme
                                            .typography
                                            .bodySmall,
                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .onSurfaceVariant
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                val entered =
                                    quantity
                                        .dealerBusinessInt()
                                        ?: 0

                                val baseQuantity =
                                    entered.toLong() *
                                        selectedUnitFactor
                                            .toLong()

                                if (
                                    entered > 0 &&
                                    selectedUnitFactor >
                                        0 &&
                                    selectedUnitName
                                        .isNotBlank() &&
                                    baseQuantity >
                                        0L &&
                                    baseQuantity <=
                                        Int.MAX_VALUE
                                            .toLong()
                                ) {
                                    lines =
                                        lines +
                                            com.familykhata.app
                                                .dealerbusiness
                                                .DealerDeliveryChallanLineInput(
                                                    productId =
                                                        productId,
                                                    unitName =
                                                        selectedUnitName,
                                                    unitFactor =
                                                        selectedUnitFactor,
                                                    quantity =
                                                        entered
                                                )

                                    selectedProductId =
                                        null

                                    selectedUnitName =
                                        ""

                                    selectedUnitFactor =
                                        1

                                    quantity =
                                        ""
                                }
                            },
                            modifier =
                                Modifier.fillMaxWidth()
                        ) {
                            Text(
                                v15Text(
                                    "চালানে যোগ করুন",
                                    "Add to challan"
                                )
                            )
                        }
                    }
                }

                if (lines.isNotEmpty()) {
                    Text(
                        v15Text(
                            "চালানের মাল",
                            "Challan items"
                        ),
                        fontWeight =
                            FontWeight.Bold
                    )
                }

                lines.forEachIndexed {
                        index,
                        line ->

                    val product =
                        products.firstOrNull {
                            it.id ==
                                line.productId
                        }

                    val productName =
                        product?.name
                            ?: "#${line.productId}"

                    val baseUnit =
                        product
                            ?.unit
                            ?.trim()
                            ?.ifBlank {
                                "pcs"
                            }
                            ?: "pcs"

                    Card(
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier =
                                Modifier.padding(
                                    8.dp
                                ),
                            verticalArrangement =
                                Arrangement.spacedBy(
                                    3.dp
                                )
                        ) {
                            Text(
                                productName,
                                fontWeight =
                                    FontWeight.Bold
                            )

                            if (
                                line.quantity >
                                0
                            ) {
                                Text(
                                    "${line.quantity} " +
                                        line.unitName
                                )

                                if (
                                    line.unitFactor >
                                    1
                                ) {
                                    val baseQuantity =
                                        line.quantity
                                            .toLong() *
                                            line.unitFactor
                                                .toLong()

                                    Text(
                                        "$baseQuantity $baseUnit",
                                        style =
                                            MaterialTheme
                                                .typography
                                                .bodySmall,
                                        color =
                                            MaterialTheme
                                                .colorScheme
                                                .onSurfaceVariant
                                    )
                                }
                            } else {
                                /*
                                 * Defensive fallback for any legacy
                                 * in-memory caller.
                                 */
                                Text(
                                    v15Text(
                                        "বক্স ${line.boxCount} • পাতা ${line.sheetCount} • খোলা ${line.loosePieces}",
                                        "Box ${line.boxCount} • Sheet ${line.sheetCount} • Loose ${line.loosePieces}"
                                    )
                                )
                            }

                            TextButton(
                                onClick = {
                                    lines =
                                        lines.filterIndexed {
                                                currentIndex,
                                                _ ->

                                            currentIndex !=
                                                index
                                        }
                                }
                            ) {
                                Text(
                                    v15Text(
                                        "বাদ দিন",
                                        "Remove"
                                    )
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value =
                        note,
                    onValueChange = {
                        note = it
                    },
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
                    val personId =
                        deliveryPersonId
                            ?: return@TextButton

                    if (
                        lines.isNotEmpty()
                    ) {
                        onSave(
                            personId,
                            challanNo,
                            lines,
                            note
                        )
                    }
                }
            ) {
                Text(
                    v15Text(
                        "চালান তৈরি",
                        "Create challan"
                    )
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick =
                    onDismiss
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
private fun DealerWarehouseDamageDialog(
    products:
        List<com.familykhata.app.data.ProductEntity>,
    viewModel:
        DealerBusinessViewModel,
    onDismiss: () -> Unit,
    onSave: (
        Long,
        Int,
        String,
        String
    ) -> Unit
) {
    var productId by remember {
        mutableStateOf<Long?>(null)
    }

    var batchId by remember {
        mutableStateOf<Long?>(null)
    }

    var quantity by remember {
        mutableStateOf("")
    }

    var reason by remember {
        mutableStateOf("")
    }

    var note by remember {
        mutableStateOf("")
    }

    AlertDialog(
        onDismissRequest =
            onDismiss,
        title = {
            Text(
                v15Text(
                    "ড্যামেজ হিসাব",
                    "Record damage"
                )
            )
        },
        text = {
            Column(
                modifier =
                    Modifier
                        .heightIn(
                            max = 560.dp
                        )
                        .verticalScroll(
                            rememberScrollState()
                        ),
                verticalArrangement =
                    Arrangement.spacedBy(7.dp)
            ) {
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

                            batchId =
                                null
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

                productId?.let {
                        selectedProductId ->

                    val flow =
                        remember(
                            selectedProductId
                        ) {
                            viewModel
                                .observeProductBatches(
                                    selectedProductId
                                )
                        }

                    val batches by
                        flow.collectAsState(
                            initial =
                                emptyList()
                        )

                    val available =
                        batches.filter {
                            it.quantity > 0
                        }

                    Text(
                        v15Text(
                            "ক্রয় রেট / ব্যাচ নির্বাচন",
                            "Select purchase rate / batch"
                        ),
                        fontWeight =
                            FontWeight.Bold
                    )

                    if (available.isEmpty()) {
                        Text(
                            v15Text(
                                "এই পণ্যের স্টক নেই।",
                                "No stock available."
                            )
                        )
                    }

                    available.forEach {
                            batch ->

                        OutlinedButton(
                            onClick = {
                                batchId =
                                    batch.id
                            },
                            modifier =
                                Modifier.fillMaxWidth()
                        ) {
                            Text(
                                buildString {
                                    if (
                                        batchId ==
                                            batch.id
                                    ) {
                                        append("✓ ")
                                    }

                                    append(
                                        dealerBusinessMoney(
                                            batch.purchasePrice
                                        )
                                    )

                                    append(
                                        " • "
                                    )

                                    append(
                                        v15Text(
                                            "স্টক ${batch.quantity}",
                                            "Stock ${batch.quantity}"
                                        )
                                    )

                                    if (
                                        batch.batchNo
                                            .isNotBlank()
                                    ) {
                                        append(
                                            " • ${batch.batchNo}"
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value =
                        quantity,
                    onValueChange = {
                        quantity = it
                    },
                    label = {
                        Text(
                            v15Text(
                                "ড্যামেজ পিস",
                                "Damaged pieces"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value =
                        reason,
                    onValueChange = {
                        reason = it
                    },
                    label = {
                        Text(
                            v15Text(
                                "ড্যামেজের কারণ",
                                "Damage reason"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value =
                        note,
                    onValueChange = {
                        note = it
                    },
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
                    val selectedBatch =
                        batchId
                            ?: return@TextButton

                    val qty =
                        quantity
                            .dealerBusinessInt()
                            ?: return@TextButton

                    if (qty > 0) {
                        onSave(
                            selectedBatch,
                            qty,
                            reason,
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
                onClick =
                    onDismiss
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
private fun DealerDeliveryChallanMetaDialog(
    initial: DealerDeliveryChallanEntity,
    deliveryPeople:
        List<DealerDeliveryPersonEntity>,
    onDismiss: () -> Unit,
    onSave: (
        Long,
        String,
        Long,
        String
    ) -> Unit
) {
    var personId by remember(initial.id) {
        mutableStateOf(
            initial.deliveryPersonId
        )
    }

    var challanNo by remember(initial.id) {
        mutableStateOf(
            initial.challanNo
        )
    }

    var dateText by remember(initial.id) {
        mutableStateOf(
            dealerBusinessDate(
                initial.issuedAt
            )
        )
    }

    var note by remember(initial.id) {
        mutableStateOf(
            initial.note
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                v15Text(
                    "চালান তথ্য সম্পাদনা",
                    "Edit challan information"
                )
            )
        },
        text = {
            Column(
                modifier =
                    Modifier
                        .heightIn(
                            max = 560.dp
                        )
                        .verticalScroll(
                            rememberScrollState()
                        ),
                verticalArrangement =
                    Arrangement.spacedBy(7.dp)
            ) {
                Text(
                    v15Text(
                        "ডেলিভারি ম্যান",
                        "Delivery person"
                    ),
                    fontWeight =
                        FontWeight.Bold
                )

                deliveryPeople.forEach {
                        person ->

                    OutlinedButton(
                        onClick = {
                            personId =
                                person.id
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (
                                personId ==
                                    person.id
                            ) {
                                "✓ ${person.name}"
                            } else {
                                person.name
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = challanNo,
                    onValueChange = {
                        challanNo = it
                    },
                    label = {
                        Text(
                            v15Text(
                                "চালান নং",
                                "Challan no."
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dateText,
                    onValueChange = {
                        dateText = it
                    },
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
                    value = note,
                    onValueChange = {
                        note = it
                    },
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
                    val selectedPerson =
                        personId
                            ?: return@TextButton

                    val issuedAt =
                        dateText
                            .dealerBusinessDateMillis()
                            ?: return@TextButton

                    onSave(
                        selectedPerson,
                        challanNo,
                        issuedAt,
                        note
                    )
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
private fun DealerDeliverySettlementMetaDialog(
    challan: DealerDeliveryChallanEntity,
    viewModel: DealerBusinessViewModel,
    onDismiss: () -> Unit,
    onSave: (
        com.familykhata.app.dealerbusiness
            .DealerDeliverySettlementEntity,
        Double,
        Long,
        String
    ) -> Unit
) {
    var settlement by
        remember(challan.id) {
            mutableStateOf<
                com.familykhata.app
                    .dealerbusiness
                    .DealerDeliverySettlementEntity?
            >(null)
        }

    var loading by remember(challan.id) {
        mutableStateOf(true)
    }

    var cash by remember(challan.id) {
        mutableStateOf("")
    }

    var dateText by remember(challan.id) {
        mutableStateOf("")
    }

    var note by remember(challan.id) {
        mutableStateOf("")
    }

    LaunchedEffect(challan.id) {
        loading = true

        val loaded =
            viewModel.loadDeliverySettlement(
                challan.id
            )

        settlement = loaded

        if (loaded != null) {
            cash =
                loaded.cashHandedOver
                    .toString()

            dateText =
                dealerBusinessDate(
                    loaded.receivedAt
                )

            note =
                loaded.note
        }

        loading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                v15Text(
                    "সেটেলমেন্ট সম্পাদনা",
                    "Edit settlement"
                )
            )
        },
        text = {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(7.dp)
            ) {
                if (loading) {
                    Text(
                        v15Text(
                            "লোড হচ্ছে...",
                            "Loading..."
                        )
                    )
                } else if (
                    settlement == null
                ) {
                    Text(
                        v15Text(
                            "সেটেলমেন্ট পাওয়া যায়নি।",
                            "Settlement not found."
                        )
                    )
                } else {
                    OutlinedTextField(
                        value = cash,
                        onValueChange = {
                            cash = it
                        },
                        label = {
                            Text(
                                v15Text(
                                    "জমা নগদ",
                                    "Cash handed over"
                                )
                            )
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = dateText,
                        onValueChange = {
                            dateText = it
                        },
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
                        value = note,
                        onValueChange = {
                            note = it
                        },
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

                    Text(
                        v15Text(
                            "ফেরত/ড্যামেজ পরিমাণ বদলাতে 'পুনরায় খুলুন' ব্যবহার করুন, তারপর আবার রাতে বুঝে নিন।",
                            "To change returned/damaged quantities, reopen the settlement and settle it again."
                        ),
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val current =
                        settlement
                            ?: return@TextButton

                    val cashValue =
                        cash
                            .trim()
                            .replace(
                                ",",
                                ""
                            )
                            .toDoubleOrNull()
                            ?: return@TextButton

                    val receivedAt =
                        dateText
                            .dealerBusinessDateMillis()
                            ?: return@TextButton

                    if (cashValue >= 0) {
                        onSave(
                            current,
                            cashValue,
                            receivedAt,
                            note
                        )
                    }
                },
                enabled =
                    !loading &&
                        settlement != null
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
private fun DealerWarehouseDamageEditDialog(
    initial: DealerDamageEntity,
    onDismiss: () -> Unit,
    onSave: (
        Int,
        String,
        String
    ) -> Unit
) {
    var quantity by remember(initial.id) {
        mutableStateOf(
            initial.quantityPieces.toString()
        )
    }

    var reason by remember(initial.id) {
        mutableStateOf(initial.reason)
    }

    var note by remember(initial.id) {
        mutableStateOf(initial.note)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                v15Text(
                    "ড্যামেজ এন্ট্রি সম্পাদনা",
                    "Edit damage entry"
                )
            )
        },
        text = {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(7.dp)
            ) {
                Text(
                    initial.productNameSnapshot,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    v15Text(
                        "ক্রয় দর: ${dealerBusinessMoney(initial.unitCost)}",
                        "Purchase rate: ${dealerBusinessMoney(initial.unitCost)}"
                    )
                )

                OutlinedTextField(
                    value = quantity,
                    onValueChange = {
                        quantity = it
                    },
                    label = {
                        Text(
                            v15Text(
                                "ড্যামেজ পিস",
                                "Damaged pieces"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = reason,
                    onValueChange = {
                        reason = it
                    },
                    label = {
                        Text(
                            v15Text(
                                "কারণ",
                                "Reason"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = {
                        note = it
                    },
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
                    val qty =
                        quantity
                            .dealerBusinessInt()

                    if (
                        qty != null &&
                        qty > 0
                    ) {
                        onSave(
                            qty,
                            reason,
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
private fun DealerPackSetupDialog(
    products:
        List<com.familykhata.app.data.ProductEntity>,
    packs:
        List<DealerProductPackEntity>,
    onDismiss: () -> Unit,
    onDelete: (
        DealerProductPackEntity
    ) -> Unit,
    onSave: (
        Long,
        Int,
        Int
    ) -> Unit
) {
    var productId by remember {
        mutableStateOf<Long?>(null)
    }

    var boxPieces by remember {
        mutableStateOf("")
    }

    var sheetPieces by remember {
        mutableStateOf("")
    }

    val selectedPack =
        productId?.let { id ->
            packs.firstOrNull {
                it.productId == id
            }
        }

    LaunchedEffect(
        productId,
        selectedPack?.updatedAt
    ) {
        boxPieces =
            selectedPack
                ?.piecesPerBox
                ?.takeIf { it > 0 }
                ?.toString()
                .orEmpty()

        sheetPieces =
            selectedPack
                ?.piecesPerSheet
                ?.takeIf { it > 0 }
                ?.toString()
                .orEmpty()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                v15Text(
                    "বক্স / পাতা সেটআপ",
                    "Box / sheet setup"
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
                    value =
                        boxPieces,
                    onValueChange = {
                        boxPieces = it
                    },
                    label = {
                        Text(
                            v15Text(
                                "১ বক্সে কয় পিস",
                                "Pieces per box"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value =
                        sheetPieces,
                    onValueChange = {
                        sheetPieces = it
                    },
                    label = {
                        Text(
                            v15Text(
                                "১ পাতায় কয় পিস",
                                "Pieces per sheet"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                Text(
                    v15Text(
                        "বক্স বা পাতার মান না থাকলে ০ রাখা যাবে। স্টকের মূল হিসাব পিসে থাকবে।",
                        "Use 0 when box or sheet is not applicable. Stock is stored in pieces."
                    ),
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )

                selectedPack?.let { pack ->
                    TextButton(
                        onClick = {
                            onDelete(pack)
                        }
                    ) {
                        Text(
                            v15Text(
                                "এই প্যাক সেটআপ ডিলিট",
                                "Delete this pack setup"
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val id =
                        productId
                            ?: return@TextButton

                    val box =
                        boxPieces
                            .dealerBusinessInt()
                            ?: 0

                    val sheet =
                        sheetPieces
                            .dealerBusinessInt()
                            ?: 0

                    if (
                        box >= 0 &&
                        sheet >= 0
                    ) {
                        onSave(
                            id,
                            box,
                            sheet
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
private fun DealerDeliveryPersonDialog(
    initial:
        DealerDeliveryPersonEntity?,
    onDismiss: () -> Unit,
    onSave: (
        String,
        String,
        String
    ) -> Unit
) {
    var name by remember(initial?.id) {
        mutableStateOf(
            initial?.name.orEmpty()
        )
    }

    var phone by remember(initial?.id) {
        mutableStateOf(
            initial?.phone.orEmpty()
        )
    }

    var note by remember(initial?.id) {
        mutableStateOf(
            initial?.note.orEmpty()
        )
    }

    AlertDialog(
        onDismissRequest =
            onDismiss,
        title = {
            Text(
                if (initial == null) {
                    v15Text(
                        "ডেলিভারি ম্যান যোগ করুন",
                        "Add delivery man"
                    )
                } else {
                    v15Text(
                        "ডেলিভারি ম্যান সম্পাদনা",
                        "Edit delivery man"
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
                    value = name,
                    onValueChange = {
                        name = it
                    },
                    label = {
                        Text(
                            v15Text(
                                "নাম",
                                "Name"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        phone = it
                    },
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
                    value = note,
                    onValueChange = {
                        note = it
                    },
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
                    if (
                        name.isNotBlank()
                    ) {
                        onSave(
                            name,
                            phone,
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
                onClick =
                    onDismiss
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
