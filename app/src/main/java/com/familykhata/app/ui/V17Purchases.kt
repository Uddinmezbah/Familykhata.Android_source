package com.familykhata.app.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.familykhata.app.InventoryViewModel
import com.familykhata.app.PurchaseLineInput
import com.familykhata.app.data.FinancialAccountSummary
import com.familykhata.app.data.ProductStockSummary
import com.familykhata.app.data.PurchaseBillSummary
import com.familykhata.app.data.PurchaseSupplierSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

private val purchaseLineKey =
    AtomicLong(1L)

private data class PurchaseLineDraft(
    val key: Long =
        purchaseLineKey.getAndIncrement(),
    val productId: Long,
    val unitName: String,
    val unitFactor: Int = 1,
    val quantity: String = "1",
    val unitCost: String = "",
    val batchNo: String = ""
)

@Composable
internal fun V17PurchaseScreen(
    workspace: String,
    businessId: String,
    shopType: String,
    canWrite: Boolean,
    onExit: () -> Unit
) {
    val vm: InventoryViewModel = viewModel()

    val products by
        vm.products.collectAsState()

    val suppliers by
        vm.purchaseSuppliers.collectAsState()

    val bills by
        vm.purchaseBills.collectAsState()

    val financialAccounts by
        vm.financialAccounts.collectAsState()

    val activeAccounts =
        remember(financialAccounts) {
            financialAccounts.filter {
                it.isActive
            }
        }

    val activeSuppliers =
        remember(suppliers) {
            suppliers.filter {
                it.isActive
            }
        }

    var showNewPurchase by remember {
        mutableStateOf(false)
    }

    var showSupplierDialog by remember {
        mutableStateOf(false)
    }

    var payingBill by remember {
        mutableStateOf<PurchaseBillSummary?>(null)
    }

    val context = LocalContext.current

    LaunchedEffect(
        workspace,
        businessId,
        shopType
    ) {
        vm.setContext(
            workspaceValue = workspace,
            shopType = shopType,
            businessIdValue = businessId
        )
    }

    BackHandler(
        enabled = showNewPurchase
    ) {
        showNewPurchase = false
    }

    BackHandler(
        enabled = !showNewPurchase
    ) {
        onExit()
    }

    if (showNewPurchase) {
        V15DeepScreenContainer(
            title =
                v15Text(
                    "নতুন ক্রয়",
                    "New Purchase"
                ),
            onBack = {
                showNewPurchase = false
            }
        ) {
            NewPurchaseForm(
                viewModel = vm,
                products = products,
                suppliers =
                    activeSuppliers,
                financialAccounts =
                    activeAccounts,
                canWrite = canWrite,
                onSaved = {
                    showNewPurchase = false

                    Toast.makeText(
                        context,
                        v15Text(
                            "ক্রয় সংরক্ষণ হয়েছে এবং স্টক বেড়েছে",
                            "Purchase saved and stock updated"
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }

        return
    }

    V15DeepScreenContainer(
        title =
            v15Text(
                "ক্রয় ও সাপ্লায়ার",
                "Purchases & Suppliers"
            ),
        onBack = onExit
    ) {
        val activeBills =
            bills.filter {
                it.status != "CANCELLED"
            }

        val totalPurchase =
            activeBills.sumOf {
                it.total
            }

        val totalPaid =
            activeBills.sumOf {
                it.paid
            }

        val totalDue =
            activeBills.sumOf {
                it.due
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
                    "সাপ্লায়ার, ক্রয়, স্টক ইন ও বাকি এক জায়গায় পরিচালনা করুন।",
                    "Manage suppliers, purchases, stock-in and dues in one place."
                ),
                style =
                    MaterialTheme.typography
                        .bodySmall,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                PurchaseMetric(
                    title =
                        v15Text(
                            "মোট ক্রয়",
                            "Purchase"
                        ),
                    value =
                        "${V14DisplayState.currencySymbol}${purchaseMoney(totalPurchase)}",
                    modifier =
                        Modifier.weight(1f)
                )

                PurchaseMetric(
                    title =
                        v15Text(
                            "পরিশোধ",
                            "Paid"
                        ),
                    value =
                        "${V14DisplayState.currencySymbol}${purchaseMoney(totalPaid)}",
                    modifier =
                        Modifier.weight(1f)
                )

                PurchaseMetric(
                    title =
                        v15Text(
                            "বাকি",
                            "Due"
                        ),
                    value =
                        "${V14DisplayState.currencySymbol}${purchaseMoney(totalDue)}",
                    modifier =
                        Modifier.weight(1f)
                )
            }

            Button(
                onClick = {
                    showNewPurchase = true
                },
                enabled =
                    canWrite &&
                        products.isNotEmpty() &&
                        activeSuppliers.isNotEmpty(),
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Text(
                    v15Text(
                        "＋ নতুন ক্রয়",
                        "＋ New Purchase"
                    )
                )
            }

            OutlinedButton(
                onClick = {
                    showSupplierDialog = true
                },
                enabled = canWrite,
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Text(
                    v15Text(
                        "＋ নতুন সাপ্লায়ার",
                        "＋ Add Supplier"
                    )
                )
            }

            if (
                canWrite &&
                activeSuppliers.isEmpty()
            ) {
                Text(
                    v15Text(
                        "নতুন ক্রয় করার আগে একটি সাপ্লায়ার যোগ করুন।",
                        "Add a supplier before creating a purchase."
                    ),
                    style =
                        MaterialTheme.typography
                            .bodySmall,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }

            if (
                canWrite &&
                products.isEmpty()
            ) {
                Text(
                    v15Text(
                        "ক্রয় করার আগে অন্তত একটি পণ্য যোগ করুন।",
                        "Add at least one product before creating a purchase."
                    ),
                    style =
                        MaterialTheme.typography
                            .bodySmall,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }

            Text(
                v15Text(
                    "সাপ্লায়ার",
                    "Suppliers"
                ),
                style =
                    MaterialTheme.typography
                        .titleMedium,
                fontWeight =
                    FontWeight.Bold
            )

            if (suppliers.isEmpty()) {
                PurchaseEmptyCard(
                    text =
                        v15Text(
                            "এখনও কোনো সাপ্লায়ার নেই।",
                            "No suppliers yet."
                        )
                )
            } else {
                suppliers.forEach {
                    supplier ->

                    SupplierCard(
                        supplier =
                            supplier
                    )
                }
            }

            Text(
                v15Text(
                    "ক্রয়ের ইতিহাস",
                    "Purchase History"
                ),
                style =
                    MaterialTheme.typography
                        .titleMedium,
                fontWeight =
                    FontWeight.Bold
            )

            if (bills.isEmpty()) {
                PurchaseEmptyCard(
                    text =
                        v15Text(
                            "এখনও কোনো ক্রয় রেকর্ড নেই।",
                            "No purchases recorded yet."
                        )
                )
            } else {
                bills.forEach { bill ->
                    PurchaseBillCard(
                        bill = bill,
                        canWrite = canWrite,
                        onPay = {
                            payingBill = bill
                        }
                    )
                }
            }
        }
    }

    if (showSupplierDialog) {
        AddSupplierDialog(
            saving = false,
            onDismiss = {
                showSupplierDialog = false
            },
            onSave = {
                    name,
                    phone,
                    address,
                    note ->

                vm.addPurchaseSupplier(
                    name = name,
                    phone = phone,
                    address = address,
                    note = note
                ) { id ->
                    if (id != null) {
                        showSupplierDialog =
                            false

                        Toast.makeText(
                            context,
                            v15Text(
                                "সাপ্লায়ার যোগ হয়েছে",
                                "Supplier added"
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            context,
                            v15Text(
                                "সাপ্লায়ার যোগ করা যায়নি",
                                "Could not add supplier"
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )
    }

    payingBill?.let { bill ->
        PurchasePaymentDialog(
            bill = bill,
            accounts = activeAccounts,
            onDismiss = {
                payingBill = null
            },
            onPay = {
                    amount,
                    accountId,
                    note ->

                vm.addPurchasePayment(
                    billId = bill.id,
                    amount = amount,
                    financialAccountId =
                        accountId,
                    note = note
                ) { success ->
                    if (success) {
                        payingBill = null

                        Toast.makeText(
                            context,
                            v15Text(
                                "পেমেন্ট সংরক্ষণ হয়েছে",
                                "Payment saved"
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            context,
                            v15Text(
                                "পেমেন্ট করা যায়নি। Account balance ও তথ্য যাচাই করুন।",
                                "Could not save payment. Check account balance and entered values."
                            ),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        )
    }
}

@Composable
private fun PurchaseMetric(
    title: String,
    value: String,
    modifier: Modifier
) {
    Surface(
        modifier = modifier,
        shape =
            MaterialTheme.shapes.medium,
        color =
            MaterialTheme.colorScheme
                .surfaceVariant
    ) {
        Column(
            modifier =
                Modifier.padding(10.dp),
            verticalArrangement =
                Arrangement.spacedBy(3.dp)
        ) {
            Text(
                title,
                style =
                    MaterialTheme.typography
                        .labelSmall,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )

            Text(
                value,
                fontWeight =
                    FontWeight.Bold,
                style =
                    MaterialTheme.typography
                        .bodyMedium
            )
        }
    }
}

@Composable
private fun PurchaseEmptyCard(
    text: String
) {
    Surface(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            MaterialTheme.shapes.large,
        color =
            MaterialTheme.colorScheme
                .surfaceVariant
    ) {
        Text(
            text,
            modifier =
                Modifier.padding(16.dp),
            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant
        )
    }
}

@Composable
private fun SupplierCard(
    supplier: PurchaseSupplierSummary
) {
    Card(
        modifier =
            Modifier.fillMaxWidth()
    ) {
        Column(
            modifier =
                Modifier.padding(14.dp),
            verticalArrangement =
                Arrangement.spacedBy(5.dp)
        ) {
            Text(
                supplier.name,
                fontWeight =
                    FontWeight.Bold
            )

            if (supplier.phone.isNotBlank()) {
                Text(
                    supplier.phone,
                    style =
                        MaterialTheme.typography
                            .bodySmall
                )
            }

            Text(
                v15Text(
                    "ক্রয় ${supplier.purchaseCount}টি • মোট ${V14DisplayState.currencySymbol}${purchaseMoney(supplier.totalPurchase)} • বাকি ${V14DisplayState.currencySymbol}${purchaseMoney(supplier.due)}",
                    "${supplier.purchaseCount} purchases • Total ${V14DisplayState.currencySymbol}${purchaseMoney(supplier.totalPurchase)} • Due ${V14DisplayState.currencySymbol}${purchaseMoney(supplier.due)}"
                ),
                style =
                    MaterialTheme.typography
                        .bodySmall,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PurchaseBillCard(
    bill: PurchaseBillSummary,
    canWrite: Boolean,
    onPay: () -> Unit
) {
    Card(
        modifier =
            Modifier.fillMaxWidth()
    ) {
        Column(
            modifier =
                Modifier.padding(14.dp),
            verticalArrangement =
                Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {
                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text(
                        bill.purchaseNo,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        bill.supplierName,
                        style =
                            MaterialTheme.typography
                                .bodySmall
                    )
                }

                Text(
                    "${V14DisplayState.currencySymbol}${purchaseMoney(bill.total)}",
                    fontWeight =
                        FontWeight.Bold
                )
            }

            Text(
                purchaseDate(
                    bill.purchasedAt
                ),
                style =
                    MaterialTheme.typography
                        .labelSmall,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )

            Text(
                v15Text(
                    "পরিশোধ: ${V14DisplayState.currencySymbol}${purchaseMoney(bill.paid)} • বাকি: ${V14DisplayState.currencySymbol}${purchaseMoney(bill.due)}",
                    "Paid: ${V14DisplayState.currencySymbol}${purchaseMoney(bill.paid)} • Due: ${V14DisplayState.currencySymbol}${purchaseMoney(bill.due)}"
                ),
                style =
                    MaterialTheme.typography
                        .bodySmall
            )

            if (
                canWrite &&
                bill.status != "CANCELLED" &&
                bill.due > 0.0001
            ) {
                OutlinedButton(
                    onClick = onPay,
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text(
                        v15Text(
                            "বাকি পরিশোধ",
                            "Pay Due"
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun AddSupplierDialog(
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        String,
        String,
        String,
        String
    ) -> Unit
) {
    var name by remember {
        mutableStateOf("")
    }

    var phone by remember {
        mutableStateOf("")
    }

    var address by remember {
        mutableStateOf("")
    }

    var note by remember {
        mutableStateOf("")
    }

    AlertDialog(
        onDismissRequest = {
            if (!saving) onDismiss()
        },
        title = {
            Text(
                v15Text(
                    "নতুন সাপ্লায়ার",
                    "Add Supplier"
                )
            )
        },
        text = {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                    },
                    label = {
                        Text(
                            v15Text(
                                "সাপ্লায়ারের নাম",
                                "Supplier name"
                            )
                        )
                    },
                    singleLine = true,
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
                                "মোবাইল",
                                "Phone"
                            )
                        )
                    },
                    singleLine = true,
                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = {
                        address = it
                    },
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
            Button(
                enabled =
                    !saving &&
                        name.isNotBlank(),
                onClick = {
                    onSave(
                        name,
                        phone,
                        address,
                        note
                    )
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
                enabled = !saving,
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
private fun NewPurchaseForm(
    viewModel: InventoryViewModel,
    products: List<ProductStockSummary>,
    suppliers:
        List<PurchaseSupplierSummary>,
    financialAccounts:
        List<FinancialAccountSummary>,
    canWrite: Boolean,
    onSaved: () -> Unit
) {
    val context = LocalContext.current

    var supplierId by remember {
        mutableStateOf<Long?>(null)
    }

    var purchaseNo by remember {
        mutableStateOf("")
    }

    var discountText by remember {
        mutableStateOf("0")
    }

    var paidText by remember {
        mutableStateOf("0")
    }

    var financialAccountId by remember {
        mutableStateOf<Long?>(null)
    }

    var note by remember {
        mutableStateOf("")
    }

    var saving by remember {
        mutableStateOf(false)
    }

    val lines =
        remember {
            mutableStateListOf<
                PurchaseLineDraft
            >()
        }

    LaunchedEffect(suppliers) {
        if (
            supplierId == null ||
            suppliers.none {
                it.id == supplierId
            }
        ) {
            supplierId =
                suppliers
                    .singleOrNull()
                    ?.id
        }
    }

    LaunchedEffect(financialAccounts) {
        if (
            financialAccountId == null ||
            financialAccounts.none {
                it.id ==
                    financialAccountId
            }
        ) {
            financialAccountId =
                financialAccounts
                    .singleOrNull()
                    ?.id
        }
    }

    LaunchedEffect(products) {
        if (
            lines.isEmpty() &&
            products.isNotEmpty()
        ) {
            val product =
                products.first()

            lines +=
                PurchaseLineDraft(
                    productId =
                        product.id,
                    unitName =
                        product.unit,
                    unitFactor = 1,
                    unitCost =
                        if (
                            product.avgPurchasePrice >
                            0.0
                        ) {
                            purchaseMoney(
                                product.avgPurchasePrice
                            )
                        } else {
                            ""
                        }
                )
        }
    }

    val subtotal =
        lines.sumOf { line ->
            val quantity =
                line.quantity
                    .purchaseIntOrNull()
                    ?: 0

            val cost =
                line.unitCost
                    .purchaseDoubleOrNull()
                    ?: 0.0

            quantity.toDouble() *
                cost
        }

    val discount =
        discountText
            .purchaseDoubleOrNull()
            ?: 0.0

    val total =
        (
            subtotal -
                discount
        ).coerceAtLeast(
            0.0
        )

    val paid =
        paidText
            .purchaseDoubleOrNull()
            ?: 0.0

    val due =
        (
            total -
                paid
        ).coerceAtLeast(
            0.0
        )

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
        PurchaseIdPicker(
            title =
                v15Text(
                    "সাপ্লায়ার",
                    "Supplier"
                ),
            selectedId =
                supplierId,
            items =
                suppliers.map {
                    it.id to it.name
                },
            enabled =
                !saving,
            onSelect = {
                supplierId = it
            }
        )

        OutlinedTextField(
            value = purchaseNo,
            onValueChange = {
                purchaseNo = it
            },
            label = {
                Text(
                    v15Text(
                        "বিল / চালান নম্বর (ঐচ্ছিক)",
                        "Bill / invoice no. (optional)"
                    )
                )
            },
            singleLine = true,
            enabled = !saving,
            modifier =
                Modifier.fillMaxWidth()
        )

        Text(
            v15Text(
                "পণ্যসমূহ",
                "Purchase Items"
            ),
            style =
                MaterialTheme.typography
                    .titleMedium,
            fontWeight =
                FontWeight.Bold
        )

        lines.forEachIndexed {
                index,
                line ->

            PurchaseLineEditor(
                line = line,
                products = products,
                viewModel =
                    viewModel,
                enabled = !saving,
                onChange = {
                    lines[index] = it
                },
                onRemove = {
                    if (lines.size > 1) {
                        lines.removeAt(
                            index
                        )
                    }
                }
            )
        }

        OutlinedButton(
            enabled =
                !saving &&
                    products.isNotEmpty(),
            onClick = {
                val product =
                    products.first()

                lines +=
                    PurchaseLineDraft(
                        productId =
                            product.id,
                        unitName =
                            product.unit,
                        unitFactor = 1,
                        unitCost =
                            if (
                                product.avgPurchasePrice >
                                0.0
                            ) {
                                purchaseMoney(
                                    product.avgPurchasePrice
                                )
                            } else {
                                ""
                            }
                    )
            },
            modifier =
                Modifier.fillMaxWidth()
        ) {
            Text(
                v15Text(
                    "＋ আরেকটি পণ্য",
                    "＋ Add another item"
                )
            )
        }

        OutlinedTextField(
            value = discountText,
            onValueChange = {
                discountText = it
            },
            label = {
                Text(
                    v15Text(
                        "ডিসকাউন্ট",
                        "Discount"
                    )
                )
            },
            singleLine = true,
            enabled = !saving,
            modifier =
                Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = paidText,
            onValueChange = {
                paidText = it
            },
            label = {
                Text(
                    v15Text(
                        "এখন পরিশোধ",
                        "Paid now"
                    )
                )
            },
            singleLine = true,
            enabled = !saving,
            modifier =
                Modifier.fillMaxWidth()
        )

        if (paid > 0.0001) {
            PurchaseIdPicker(
                title =
                    v15Text(
                        "যে Account থেকে টাকা যাবে",
                        "Payment Account"
                    ),
                selectedId =
                    financialAccountId,
                items =
                    financialAccounts.map {
                        it.id to
                            "${it.name} (${V14DisplayState.currencySymbol}${purchaseMoney(it.balance)})"
                    },
                enabled =
                    !saving,
                onSelect = {
                    financialAccountId =
                        it
                }
            )
        }

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
            enabled = !saving,
            modifier =
                Modifier.fillMaxWidth()
        )

        Surface(
            modifier =
                Modifier.fillMaxWidth(),
            shape =
                MaterialTheme.shapes.large,
            color =
                MaterialTheme.colorScheme
                    .surfaceVariant
        ) {
            Column(
                modifier =
                    Modifier.padding(14.dp),
                verticalArrangement =
                    Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    v15Text(
                        "সাবটোটাল: ${V14DisplayState.currencySymbol}${purchaseMoney(subtotal)}",
                        "Subtotal: ${V14DisplayState.currencySymbol}${purchaseMoney(subtotal)}"
                    )
                )

                Text(
                    v15Text(
                        "মোট: ${V14DisplayState.currencySymbol}${purchaseMoney(total)}",
                        "Total: ${V14DisplayState.currencySymbol}${purchaseMoney(total)}"
                    ),
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    v15Text(
                        "বাকি: ${V14DisplayState.currencySymbol}${purchaseMoney(due)}",
                        "Due: ${V14DisplayState.currencySymbol}${purchaseMoney(due)}"
                    )
                )
            }
        }

        Button(
            enabled =
                canWrite &&
                    !saving,
            onClick = {
                val selectedSupplierId =
                    supplierId

                val parsedDiscount =
                    discountText
                        .purchaseDoubleOrNull()

                val parsedPaid =
                    paidText
                        .purchaseDoubleOrNull()

                val parsedLines =
                    lines.mapNotNull {
                        line ->

                        val quantity =
                            line.quantity
                                .purchaseIntOrNull()

                        val cost =
                            line.unitCost
                                .purchaseDoubleOrNull()

                        if (
                            quantity == null ||
                            quantity <= 0 ||
                            cost == null ||
                            cost < 0.0
                        ) {
                            null
                        } else {
                            PurchaseLineInput(
                                productId =
                                    line.productId,
                                quantity =
                                    quantity,
                                unitCost =
                                    cost,
                                unitName =
                                    line.unitName,
                                unitFactor =
                                    line.unitFactor,
                                batchNo =
                                    line.batchNo
                            )
                        }
                    }

                val invalid =
                    selectedSupplierId ==
                        null ||
                        lines.isEmpty() ||
                        parsedLines.size !=
                            lines.size ||
                        parsedDiscount ==
                            null ||
                        parsedDiscount < 0.0 ||
                        parsedDiscount >
                            subtotal +
                                0.0001 ||
                        parsedPaid ==
                            null ||
                        parsedPaid < 0.0 ||
                        parsedPaid >
                            total +
                                0.0001 ||
                        (
                            parsedPaid >
                                0.0001 &&
                                financialAccountId ==
                                    null
                            )

                if (invalid) {
                    Toast.makeText(
                        context,
                        v15Text(
                            "ক্রয়ের তথ্য সঠিকভাবে পূরণ করুন",
                            "Check the purchase information"
                        ),
                        Toast.LENGTH_SHORT
                    ).show()

                    return@Button
                }

                val safeSupplierId =
                    requireNotNull(
                        selectedSupplierId
                    )

                val safeDiscount =
                    requireNotNull(
                        parsedDiscount
                    )

                val safePaid =
                    requireNotNull(
                        parsedPaid
                    )

                saving = true

                viewModel.createPurchase(
                    purchaseNo =
                        purchaseNo,
                    supplierId =
                        safeSupplierId,
                    lines =
                        parsedLines,
                    discount =
                        safeDiscount,
                    initialPaid =
                        safePaid,
                    financialAccountId =
                        if (
                            safePaid >
                            0.0001
                        ) {
                            financialAccountId
                        } else {
                            null
                        },
                    note = note
                ) { purchaseId ->
                    saving = false

                    if (purchaseId != null) {
                        onSaved()
                    } else {
                        Toast.makeText(
                            context,
                            v15Text(
                                "ক্রয় সংরক্ষণ করা যায়নি। পণ্য, unit, account ও balance যাচাই করুন।",
                                "Could not save purchase. Check products, units, account and balance."
                            ),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            },
            modifier =
                Modifier.fillMaxWidth()
        ) {
            Text(
                if (saving) {
                    v15Text(
                        "সংরক্ষণ হচ্ছে...",
                        "Saving..."
                    )
                } else {
                    v15Text(
                        "ক্রয় সংরক্ষণ করুন",
                        "Save Purchase"
                    )
                }
            )
        }
    }
}

@Composable
private fun PurchaseLineEditor(
    line: PurchaseLineDraft,
    products: List<ProductStockSummary>,
    viewModel: InventoryViewModel,
    enabled: Boolean,
    onChange:
        (PurchaseLineDraft) -> Unit,
    onRemove: () -> Unit
) {
    val product =
        products.firstOrNull {
            it.id == line.productId
        }

    val unitsFlow =
        remember(line.productId) {
            viewModel
                .observeProductUnitConversions(
                    line.productId
                )
        }

    val conversions by
        unitsFlow.collectAsState(
            initial = emptyList()
        )

    val unitOptions =
        remember(
            product,
            conversions
        ) {
            buildList {
                if (product != null) {
                    add(
                        product.unit to 1
                    )

                    conversions.forEach {
                        conversion ->

                        add(
                            conversion.unitName to
                                conversion.baseQuantity
                        )
                    }
                }
            }
        }

    Card(
        modifier =
            Modifier.fillMaxWidth()
    ) {
        Column(
            modifier =
                Modifier.padding(12.dp),
            verticalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            PurchaseProductPicker(
                products = products,
                selectedId =
                    line.productId,
                enabled = enabled,
                onSelect = {
                    selected ->

                    onChange(
                        line.copy(
                            productId =
                                selected.id,
                            unitName =
                                selected.unit,
                            unitFactor = 1,
                            unitCost =
                                if (
                                    selected.avgPurchasePrice >
                                    0.0
                                ) {
                                    purchaseMoney(
                                        selected.avgPurchasePrice
                                    )
                                } else {
                                    ""
                                }
                        )
                    )
                }
            )

            PurchaseUnitPicker(
                units =
                    unitOptions,
                selectedName =
                    line.unitName,
                selectedFactor =
                    line.unitFactor,
                enabled = enabled,
                onSelect = {
                        name,
                        factor ->

                    onChange(
                        line.copy(
                            unitName = name,
                            unitFactor = factor
                        )
                    )
                }
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value =
                        line.quantity,
                    onValueChange = {
                        onChange(
                            line.copy(
                                quantity = it
                            )
                        )
                    },
                    label = {
                        Text(
                            v15Text(
                                "পরিমাণ",
                                "Quantity"
                            )
                        )
                    },
                    enabled = enabled,
                    singleLine = true,
                    modifier =
                        Modifier.weight(1f)
                )

                OutlinedTextField(
                    value =
                        line.unitCost,
                    onValueChange = {
                        onChange(
                            line.copy(
                                unitCost = it
                            )
                        )
                    },
                    label = {
                        Text(
                            v15Text(
                                "ক্রয় মূল্য / ${line.unitName.ifBlank { "unit" }}",
                                "Cost / ${line.unitName.ifBlank { "unit" }}"
                            )
                        )
                    },
                    enabled = enabled,
                    singleLine = true,
                    modifier =
                        Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value =
                    line.batchNo,
                onValueChange = {
                    onChange(
                        line.copy(
                            batchNo = it
                        )
                    )
                },
                label = {
                    Text(
                        v15Text(
                            "ব্যাচ নম্বর (ঐচ্ছিক)",
                            "Batch no. (optional)"
                        )
                    )
                },
                enabled = enabled,
                singleLine = true,
                modifier =
                    Modifier.fillMaxWidth()
            )

            val quantity =
                line.quantity
                    .purchaseIntOrNull()
                    ?: 0

            val cost =
                line.unitCost
                    .purchaseDoubleOrNull()
                    ?: 0.0

            Text(
                v15Text(
                    "লাইন মোট: ${V14DisplayState.currencySymbol}${purchaseMoney(quantity * cost)}",
                    "Line total: ${V14DisplayState.currencySymbol}${purchaseMoney(quantity * cost)}"
                ),
                fontWeight =
                    FontWeight.SemiBold,
                style =
                    MaterialTheme.typography
                        .bodySmall
            )

            TextButton(
                enabled = enabled,
                onClick = onRemove
            ) {
                Text(
                    v15Text(
                        "এই পণ্যটি বাদ দিন",
                        "Remove item"
                    )
                )
            }
        }
    }
}

@Composable
private fun PurchaseProductPicker(
    products: List<ProductStockSummary>,
    selectedId: Long,
    enabled: Boolean,
    onSelect:
        (ProductStockSummary) -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    val selected =
        products.firstOrNull {
            it.id == selectedId
        }

    Box(
        modifier =
            Modifier.fillMaxWidth()
    ) {
        OutlinedButton(
            enabled = enabled,
            onClick = {
                expanded = true
            },
            modifier =
                Modifier.fillMaxWidth()
        ) {
            Text(
                selected?.name
                    ?: v15Text(
                        "পণ্য নির্বাচন করুন",
                        "Select product"
                    )
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {
            products.forEach {
                product ->

                DropdownMenuItem(
                    text = {
                        Text(
                            if (
                                product.sku.isBlank()
                            ) {
                                product.name
                            } else {
                                "${product.name} • ${product.sku}"
                            }
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelect(product)
                    }
                )
            }
        }
    }
}

@Composable
private fun PurchaseUnitPicker(
    units: List<Pair<String, Int>>,
    selectedName: String,
    selectedFactor: Int,
    enabled: Boolean,
    onSelect:
        (String, Int) -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    Box(
        modifier =
            Modifier.fillMaxWidth()
    ) {
        OutlinedButton(
            enabled =
                enabled &&
                    units.isNotEmpty(),
            onClick = {
                expanded = true
            },
            modifier =
                Modifier.fillMaxWidth()
        ) {
            Text(
                v15Text(
                    "Unit: ${selectedName.ifBlank { "-" }}",
                    "Unit: ${selectedName.ifBlank { "-" }}"
                )
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {
            units.forEach {
                    (name, factor) ->

                DropdownMenuItem(
                    text = {
                        Text(
                            if (factor == 1) {
                                name
                            } else {
                                "$name = $factor base units"
                            }
                        )
                    },
                    onClick = {
                        expanded = false

                        onSelect(
                            name,
                            factor
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun PurchaseIdPicker(
    title: String,
    selectedId: Long?,
    items: List<Pair<Long, String>>,
    enabled: Boolean,
    onSelect: (Long) -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    val selectedText =
        items.firstOrNull {
            it.first ==
                selectedId
        }?.second

    Column(
        verticalArrangement =
            Arrangement.spacedBy(4.dp)
    ) {
        Text(
            title,
            style =
                MaterialTheme.typography
                    .labelMedium,
            fontWeight =
                FontWeight.SemiBold
        )

        Box(
            modifier =
                Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                enabled =
                    enabled &&
                        items.isNotEmpty(),
                onClick = {
                    expanded = true
                },
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Text(
                    selectedText
                        ?: v15Text(
                            "নির্বাচন করুন",
                            "Select"
                        )
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                }
            ) {
                items.forEach {
                        (id, label) ->

                    DropdownMenuItem(
                        text = {
                            Text(label)
                        },
                        onClick = {
                            expanded = false
                            onSelect(id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PurchasePaymentDialog(
    bill: PurchaseBillSummary,
    accounts: List<FinancialAccountSummary>,
    onDismiss: () -> Unit,
    onPay: (
        Double,
        Long,
        String
    ) -> Unit
) {
    var amountText by remember(
        bill.id
    ) {
        mutableStateOf(
            purchaseMoney(
                bill.due
            )
        )
    }

    var accountId by remember(
        bill.id,
        accounts
    ) {
        mutableStateOf(
            accounts
                .singleOrNull()
                ?.id
        )
    }

    var note by remember(
        bill.id
    ) {
        mutableStateOf("")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                v15Text(
                    "সাপ্লায়ার বাকি পরিশোধ",
                    "Pay Supplier Due"
                )
            )
        },
        text = {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "${bill.supplierName} • ${bill.purchaseNo}",
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    v15Text(
                        "বর্তমান বাকি: ${V14DisplayState.currencySymbol}${purchaseMoney(bill.due)}",
                        "Current due: ${V14DisplayState.currencySymbol}${purchaseMoney(bill.due)}"
                    )
                )

                PurchaseIdPicker(
                    title =
                        v15Text(
                            "Payment Account",
                            "Payment Account"
                        ),
                    selectedId =
                        accountId,
                    items =
                        accounts.map {
                            it.id to
                                "${it.name} (${V14DisplayState.currencySymbol}${purchaseMoney(it.balance)})"
                        },
                    enabled = true,
                    onSelect = {
                        accountId = it
                    }
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                    },
                    label = {
                        Text(
                            v15Text(
                                "পরিশোধের পরিমাণ",
                                "Payment amount"
                            )
                        )
                    },
                    singleLine = true,
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
            Button(
                onClick = {
                    val amount =
                        amountText
                            .purchaseDoubleOrNull()

                    val selectedAccount =
                        accountId

                    if (
                        amount != null &&
                        amount > 0.0001 &&
                        amount <=
                            bill.due +
                                0.0001 &&
                        selectedAccount !=
                            null
                    ) {
                        onPay(
                            amount,
                            selectedAccount,
                            note
                        )
                    }
                }
            ) {
                Text(
                    v15Text(
                        "পরিশোধ",
                        "Pay"
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

private fun String.purchaseNormalizeNumber():
    String =
    buildString(length) {
        this@purchaseNormalizeNumber
            .forEach { ch ->
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

private fun String.purchaseDoubleOrNull():
    Double? =
    purchaseNormalizeNumber()
        .toDoubleOrNull()

private fun String.purchaseIntOrNull():
    Int? =
    purchaseNormalizeNumber()
        .toIntOrNull()

private fun purchaseMoney(
    value: Double
): String =
    if (
        value.isFinite() &&
        value % 1.0 == 0.0
    ) {
        value.toLong().toString()
    } else if (value.isFinite()) {
        String.format(
            Locale.US,
            "%.2f",
            value
        )
    } else {
        "0"
    }

private fun purchaseDate(
    value: Long
): String =
    SimpleDateFormat(
        "dd MMM yyyy, hh:mm a",
        Locale.getDefault()
    ).format(
        Date(value)
    )