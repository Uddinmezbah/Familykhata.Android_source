package com.familykhata.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import com.familykhata.app.dealership.DealershipDealerEntity
import com.familykhata.app.dealership.DealershipInvoiceSummary
import com.familykhata.app.dealership.DealershipSaleLineInput
import com.familykhata.app.dealership.DealershipSupplierEntity
import com.familykhata.app.dealership.DealershipTerritoryEntity
import com.familykhata.app.dealership.DealershipViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun V15DealershipScreen(
    workspace: String,
    shopType: String,
    canWrite: Boolean,
    onExit: () -> Unit
) {
    val vm: DealershipViewModel = viewModel()

    val suppliers by vm.suppliers.collectAsState()
    val territories by vm.territories.collectAsState()
    val dealers by vm.dealers.collectAsState()
    val products by vm.products.collectAsState()
    val policies by vm.productPolicies.collectAsState()
    val receipts by vm.stockReceipts.collectAsState()
    val invoices by vm.invoices.collectAsState()

    var showSupplier by remember { mutableStateOf(false) }
    var showTerritory by remember { mutableStateOf(false) }
    var showDealer by remember { mutableStateOf(false) }
    var showPolicy by remember { mutableStateOf(false) }
    var showReceipt by remember { mutableStateOf(false) }
    var showInvoice by remember { mutableStateOf(false) }
    var showInventory by remember { mutableStateOf(false) }

    var selectedInvoice by remember {
        mutableStateOf<DealershipInvoiceSummary?>(null)
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

    if (showInventory) {
        BackHandler {
            showInventory = false
        }

        V15InventoryScreen(
            workspace = workspace,
            shopType = shopType,
            canWrite = canWrite,
            onExit = {
                showInventory = false
            }
        )
        return
    }

    if (selectedInvoice != null) {
        BackHandler {
            selectedInvoice = null
        }

        DealershipInvoiceLedger(
            invoice = selectedInvoice!!,
            viewModel = vm,
            canWrite = canWrite,
            onBack = {
                selectedInvoice = null
            }
        )
        return
    }

    BackHandler {
        onExit()
    }

    val totalSales =
        invoices.sumOf {
            it.totalAmount
        }

    val totalDue =
        invoices.sumOf {
            it.dueAmount.coerceAtLeast(0.0)
        }

    val grossProfit =
        invoices.sumOf {
            it.grossProfit
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            ),
        verticalArrangement =
            Arrangement.spacedBy(10.dp)
    ) {
        Text(
            v15Text(
                "ডিলারশিপ / ডিস্ট্রিবিউশন",
                "Dealership / Distribution"
            ),
            style =
                MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold
        )

        Text(
            v15Text(
                "সাপ্লায়ার, টেরিটরি, ডিলার, স্টক, ইনভয়েস, পেমেন্ট ও বকেয়া",
                "Suppliers, territories, dealers, stock, invoices, payments and dues"
            )
        )

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            DealershipMetric(
                title =
                    v15Text(
                        "ডিলার",
                        "Dealers"
                    ),
                value =
                    dealers.size.toString(),
                modifier =
                    Modifier.weight(1f)
            )

            DealershipMetric(
                title =
                    v15Text(
                        "ইনভয়েস",
                        "Invoices"
                    ),
                value =
                    invoices.size.toString(),
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
            DealershipMetric(
                title =
                    v15Text(
                        "বিক্রি",
                        "Sales"
                    ),
                value =
                    dealershipMoney(totalSales),
                modifier =
                    Modifier.weight(1f)
            )

            DealershipMetric(
                title =
                    v15Text(
                        "বকেয়া",
                        "Due"
                    ),
                value =
                    dealershipMoney(totalDue),
                modifier =
                    Modifier.weight(1f)
            )
        }

        DealershipMetric(
            title =
                v15Text(
                    "গ্রস প্রফিট",
                    "Gross profit"
                ),
            value =
                dealershipMoney(grossProfit),
            modifier =
                Modifier.fillMaxWidth()
        )

        OutlinedButton(
            onClick = {
                showInventory = true
            },
            modifier =
                Modifier.fillMaxWidth()
        ) {
            Text(
                v15Text(
                    "পণ্য ও ইনভেন্টরি ম্যানেজ করুন",
                    "Manage products & inventory"
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
                        showSupplier = true
                    },
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text(
                        v15Text(
                            "+ সাপ্লায়ার",
                            "+ Supplier"
                        )
                    )
                }

                OutlinedButton(
                    onClick = {
                        showTerritory = true
                    },
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text(
                        v15Text(
                            "+ টেরিটরি",
                            "+ Territory"
                        )
                    )
                }

                OutlinedButton(
                    onClick = {
                        showDealer = true
                    },
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text(
                        v15Text(
                            "+ ডিলার",
                            "+ Dealer"
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
                        showPolicy = true
                    },
                    enabled =
                        products.isNotEmpty(),
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text(
                        v15Text(
                            "ডিলার মূল্য",
                            "Dealer price"
                        )
                    )
                }

                OutlinedButton(
                    onClick = {
                        showReceipt = true
                    },
                    enabled =
                        products.isNotEmpty(),
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text(
                        v15Text(
                            "+ স্টক",
                            "+ Stock"
                        )
                    )
                }

                Button(
                    onClick = {
                        showInvoice = true
                    },
                    enabled =
                        dealers.isNotEmpty() &&
                        products.isNotEmpty(),
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text(
                        v15Text(
                            "+ ইনভয়েস",
                            "+ Invoice"
                        )
                    )
                }
            }
        }

        Text(
            v15Text(
                "ডিলার তালিকা",
                "Dealers"
            ),
            fontWeight = FontWeight.Bold
        )

        if (dealers.isEmpty()) {
            Text(
                v15Text(
                    "এখনো কোনো ডিলার যোগ করা হয়নি।",
                    "No dealers added yet."
                )
            )
        }

        dealers.forEach { dealer ->
            val territory =
                territories.firstOrNull {
                    it.id ==
                        dealer.territoryId
                }

            Card(
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier =
                        Modifier.padding(11.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        dealer.name,
                        fontWeight =
                            FontWeight.Bold
                    )

                    if (
                        dealer.dealerCode
                            .isNotBlank()
                    ) {
                        Text(
                            v15Text(
                                "কোড: ${dealer.dealerCode}",
                                "Code: ${dealer.dealerCode}"
                            )
                        )
                    }

                    if (territory != null) {
                        Text(
                            v15Text(
                                "টেরিটরি: ${territory.name}",
                                "Territory: ${territory.name}"
                            )
                        )
                    }

                    if (
                        dealer.creditLimit > 0
                    ) {
                        Text(
                            v15Text(
                                "ক্রেডিট লিমিট: ${dealershipMoney(dealer.creditLimit)}",
                                "Credit limit: ${dealershipMoney(dealer.creditLimit)}"
                            )
                        )
                    }
                }
            }
        }

        Text(
            v15Text(
                "ইনভয়েস ও বকেয়া",
                "Invoices & dues"
            ),
            fontWeight = FontWeight.Bold
        )

        if (invoices.isEmpty()) {
            Text(
                v15Text(
                    "এখনো কোনো ইনভয়েস নেই।",
                    "No invoices yet."
                )
            )
        }

        invoices.forEach { invoice ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selectedInvoice =
                            invoice
                    }
            ) {
                Column(
                    modifier =
                        Modifier.padding(12.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.SpaceBetween
                    ) {
                        Text(
                            invoice.invoiceNo
                                .ifBlank {
                                    "#${invoice.invoiceId}"
                                },
                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            dealershipStatusLabel(
                                invoice.status
                            )
                        )
                    }

                    Text(invoice.dealerName)

                    Text(
                        dealershipDate(
                            invoice.soldAt
                        )
                    )

                    Text(
                        v15Text(
                            "মোট: ${dealershipMoney(invoice.totalAmount)}",
                            "Total: ${dealershipMoney(invoice.totalAmount)}"
                        )
                    )

                    Text(
                        v15Text(
                            "পরিশোধ: ${dealershipMoney(invoice.totalPaid)}",
                            "Paid: ${dealershipMoney(invoice.totalPaid)}"
                        )
                    )

                    Text(
                        v15Text(
                            "বকেয়া: ${dealershipMoney(invoice.dueAmount.coerceAtLeast(0.0))}",
                            "Due: ${dealershipMoney(invoice.dueAmount.coerceAtLeast(0.0))}"
                        ),
                        fontWeight =
                            FontWeight.SemiBold
                    )
                }
            }
        }

        Text(
            v15Text(
                "সাম্প্রতিক স্টক রিসিভ",
                "Recent stock receipts"
            ),
            fontWeight = FontWeight.Bold
        )

        if (receipts.isEmpty()) {
            Text(
                v15Text(
                    "এখনো ডিলারশিপ স্টক রিসিভ নেই।",
                    "No dealership stock receipts yet."
                )
            )
        }

        receipts.take(10).forEach { item ->
            Card(
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier =
                        Modifier.padding(10.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(2.dp)
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
                        v15Text(
                            "ক্রয় মূল্য: ${dealershipMoney(item.unitCost)}",
                            "Unit cost: ${dealershipMoney(item.unitCost)}"
                        )
                    )

                    if (
                        item.supplierNameSnapshot
                            .isNotBlank()
                    ) {
                        Text(
                            v15Text(
                                "সাপ্লায়ার: ${item.supplierNameSnapshot}",
                                "Supplier: ${item.supplierNameSnapshot}"
                            )
                        )
                    }
                }
            }
        }
    }

    if (showSupplier) {
        AddDealershipSupplierDialog(
            onDismiss = {
                showSupplier = false
            },
            onSave = {
                    name,
                    phone,
                    contact,
                    address,
                    note ->

                vm.addSupplier(
                    name = name,
                    phone = phone,
                    contactPerson = contact,
                    address = address,
                    note = note
                )

                showSupplier = false
            }
        )
    }

    if (showTerritory) {
        AddDealershipTerritoryDialog(
            onDismiss = {
                showTerritory = false
            },
            onSave = {
                    name,
                    code,
                    note ->

                vm.addTerritory(
                    name = name,
                    code = code,
                    note = note
                )

                showTerritory = false
            }
        )
    }

    if (showDealer) {
        AddDealershipDealerDialog(
            territories = territories,
            onDismiss = {
                showDealer = false
            },
            onSave = {
                    territoryId,
                    name,
                    code,
                    phone,
                    address,
                    creditLimit,
                    note ->

                vm.addDealer(
                    territoryId =
                        territoryId,
                    name = name,
                    dealerCode = code,
                    phone = phone,
                    address = address,
                    creditLimit =
                        creditLimit,
                    note = note
                )

                showDealer = false
            }
        )
    }

    if (showPolicy) {
        DealershipPolicyDialog(
            products = products,
            onDismiss = {
                showPolicy = false
            },
            onSave = {
                    productId,
                    price,
                    margin,
                    note ->

                vm.setProductPolicy(
                    productId =
                        productId,
                    dealerPrice =
                        price,
                    marginPercent =
                        margin,
                    note = note
                )

                showPolicy = false
            }
        )
    }

    if (showReceipt) {
        DealershipStockReceiptDialog(
            suppliers = suppliers,
            products = products,
            onDismiss = {
                showReceipt = false
            },
            onSave = {
                    supplierId,
                    productId,
                    quantity,
                    cost,
                    batchNo,
                    reference,
                    note ->

                vm.receiveStock(
                    supplierId =
                        supplierId,
                    productId =
                        productId,
                    quantity =
                        quantity,
                    unitCost =
                        cost,
                    batchNo =
                        batchNo,
                    invoiceReference =
                        reference,
                    note = note
                ) { success ->
                    if (success) {
                        showReceipt = false
                    }
                }
            }
        )
    }

    if (showInvoice) {
        DealershipInvoiceDialog(
            dealers = dealers,
            products = products,
            priceByProduct =
                policies.associate {
                    it.productId to
                        it.dealerPrice
                },
            onDismiss = {
                showInvoice = false
            },
            onSave = {
                    dealerId,
                    invoiceNo,
                    lines,
                    initialPayment,
                    note ->

                vm.createInvoice(
                    dealerId =
                        dealerId,
                    invoiceNo =
                        invoiceNo,
                    lines =
                        lines,
                    initialPayment =
                        initialPayment,
                    note =
                        note
                ) { success ->
                    if (success) {
                        showInvoice = false
                    }
                }
            }
        )
    }
}

@Composable
private fun DealershipMetric(
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
                title,
                style =
                    MaterialTheme.typography.labelSmall
            )

            Text(
                value,
                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AddDealershipSupplierDialog(
    onDismiss: () -> Unit,
    onSave: (
        String,
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
    var contact by remember {
        mutableStateOf("")
    }
    var address by remember {
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
                    "নতুন সাপ্লায়ার",
                    "New supplier"
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
                DealershipField(
                    name,
                    { name = it },
                    v15Text(
                        "সাপ্লায়ারের নাম",
                        "Supplier name"
                    )
                )

                DealershipField(
                    contact,
                    { contact = it },
                    v15Text(
                        "যোগাযোগ ব্যক্তি",
                        "Contact person"
                    )
                )

                DealershipField(
                    phone,
                    { phone = it },
                    v15Text(
                        "ফোন",
                        "Phone"
                    )
                )

                DealershipField(
                    address,
                    { address = it },
                    v15Text(
                        "ঠিকানা",
                        "Address"
                    )
                )

                DealershipField(
                    note,
                    { note = it },
                    v15Text(
                        "নোট",
                        "Note"
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled =
                    name.isNotBlank(),
                onClick = {
                    onSave(
                        name,
                        phone,
                        contact,
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
private fun AddDealershipTerritoryDialog(
    onDismiss: () -> Unit,
    onSave: (
        String,
        String,
        String
    ) -> Unit
) {
    var name by remember {
        mutableStateOf("")
    }
    var code by remember {
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
                    "নতুন টেরিটরি",
                    "New territory"
                )
            )
        },
        text = {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(7.dp)
            ) {
                DealershipField(
                    name,
                    { name = it },
                    v15Text(
                        "টেরিটরির নাম",
                        "Territory name"
                    )
                )

                DealershipField(
                    code,
                    { code = it },
                    v15Text(
                        "কোড",
                        "Code"
                    )
                )

                DealershipField(
                    note,
                    { note = it },
                    v15Text(
                        "নোট",
                        "Note"
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled =
                    name.isNotBlank(),
                onClick = {
                    onSave(
                        name,
                        code,
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
private fun AddDealershipDealerDialog(
    territories:
        List<DealershipTerritoryEntity>,
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
    var territoryId by remember {
        mutableStateOf<Long?>(null)
    }
    var name by remember {
        mutableStateOf("")
    }
    var code by remember {
        mutableStateOf("")
    }
    var phone by remember {
        mutableStateOf("")
    }
    var address by remember {
        mutableStateOf("")
    }
    var credit by remember {
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
                    "নতুন ডিলার",
                    "New dealer"
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
                if (territories.isNotEmpty()) {
                    Text(
                        v15Text(
                            "টেরিটরি নির্বাচন",
                            "Select territory"
                        ),
                        fontWeight =
                            FontWeight.Bold
                    )

                    territories.forEach {
                            territory ->
                        OutlinedButton(
                            onClick = {
                                territoryId =
                                    territory.id
                            },
                            modifier =
                                Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (
                                    territoryId ==
                                    territory.id
                                ) {
                                    "✓ ${territory.name}"
                                } else {
                                    territory.name
                                }
                            )
                        }
                    }
                }

                DealershipField(
                    name,
                    { name = it },
                    v15Text(
                        "ডিলারের নাম",
                        "Dealer name"
                    )
                )

                DealershipField(
                    code,
                    { code = it },
                    v15Text(
                        "ডিলার কোড",
                        "Dealer code"
                    )
                )

                DealershipField(
                    phone,
                    { phone = it },
                    v15Text(
                        "ফোন",
                        "Phone"
                    )
                )

                DealershipField(
                    address,
                    { address = it },
                    v15Text(
                        "ঠিকানা",
                        "Address"
                    )
                )

                DealershipField(
                    credit,
                    { credit = it },
                    v15Text(
                        "ক্রেডিট লিমিট",
                        "Credit limit"
                    )
                )

                DealershipField(
                    note,
                    { note = it },
                    v15Text(
                        "নোট",
                        "Note"
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled =
                    name.isNotBlank(),
                onClick = {
                    onSave(
                        territoryId,
                        name,
                        code,
                        phone,
                        address,
                        credit.toDoubleOrNull()
                            ?: 0.0,
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
private fun DealershipPolicyDialog(
    products: List<ProductEntity>,
    onDismiss: () -> Unit,
    onSave: (
        Long,
        Double,
        Double,
        String
    ) -> Unit
) {
    var productId by remember {
        mutableStateOf<Long?>(null)
    }
    var price by remember {
        mutableStateOf("")
    }
    var margin by remember {
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
                    "ডিলার মূল্য / মার্জিন",
                    "Dealer price / margin"
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
                products.forEach { product ->
                    OutlinedButton(
                        onClick = {
                            productId =
                                product.id
                            price =
                                product.sellingPrice
                                    .toString()
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

                DealershipField(
                    price,
                    { price = it },
                    v15Text(
                        "ডিলার মূল্য",
                        "Dealer price"
                    )
                )

                DealershipField(
                    margin,
                    { margin = it },
                    v15Text(
                        "মার্জিন %",
                        "Margin %"
                    )
                )

                DealershipField(
                    note,
                    { note = it },
                    v15Text(
                        "নোট",
                        "Note"
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled =
                    productId != null &&
                    (
                        price.toDoubleOrNull()
                            ?: -1.0
                    ) >= 0,
                onClick = {
                    onSave(
                        productId!!,
                        price.toDoubleOrNull()
                            ?: 0.0,
                        margin.toDoubleOrNull()
                            ?: 0.0,
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
private fun DealershipStockReceiptDialog(
    suppliers:
        List<DealershipSupplierEntity>,
    products:
        List<ProductEntity>,
    onDismiss: () -> Unit,
    onSave: (
        Long?,
        Long,
        Int,
        Double,
        String,
        String,
        String
    ) -> Unit
) {
    var supplierId by remember {
        mutableStateOf<Long?>(null)
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
    var batchNo by remember {
        mutableStateOf("")
    }
    var reference by remember {
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
                    "স্টক রিসিভ",
                    "Receive stock"
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
                if (suppliers.isNotEmpty()) {
                    Text(
                        v15Text(
                            "সাপ্লায়ার",
                            "Supplier"
                        ),
                        fontWeight =
                            FontWeight.Bold
                    )

                    suppliers.forEach {
                            supplier ->
                        OutlinedButton(
                            onClick = {
                                supplierId =
                                    supplier.id
                            },
                            modifier =
                                Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (
                                    supplierId ==
                                    supplier.id
                                ) {
                                    "✓ ${supplier.name}"
                                } else {
                                    supplier.name
                                }
                            )
                        }
                    }
                }

                Text(
                    v15Text(
                        "পণ্য",
                        "Product"
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

                DealershipField(
                    quantity,
                    { quantity = it },
                    v15Text(
                        "পরিমাণ",
                        "Quantity"
                    )
                )

                DealershipField(
                    cost,
                    { cost = it },
                    v15Text(
                        "একক ক্রয় মূল্য",
                        "Unit cost"
                    )
                )

                DealershipField(
                    batchNo,
                    { batchNo = it },
                    v15Text(
                        "ব্যাচ / লট",
                        "Batch / lot"
                    )
                )

                DealershipField(
                    reference,
                    { reference = it },
                    v15Text(
                        "সাপ্লায়ার ইনভয়েস / রেফারেন্স",
                        "Supplier invoice / reference"
                    )
                )

                DealershipField(
                    note,
                    { note = it },
                    v15Text(
                        "নোট",
                        "Note"
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled =
                    productId != null &&
                    (
                        quantity.toIntOrNull()
                            ?: 0
                    ) > 0 &&
                    (
                        cost.toDoubleOrNull()
                            ?: -1.0
                    ) >= 0,
                onClick = {
                    onSave(
                        supplierId,
                        productId!!,
                        quantity.toIntOrNull()
                            ?: 0,
                        cost.toDoubleOrNull()
                            ?: 0.0,
                        batchNo,
                        reference,
                        note
                    )
                }
            ) {
                Text(
                    v15Text(
                        "স্টক যোগ করুন",
                        "Add stock"
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
private fun DealershipInvoiceDialog(
    dealers:
        List<DealershipDealerEntity>,
    products:
        List<ProductEntity>,
    priceByProduct:
        Map<Long, Double>,
    onDismiss: () -> Unit,
    onSave: (
        Long,
        String,
        List<DealershipSaleLineInput>,
        Double,
        String
    ) -> Unit
) {
    var dealerId by remember {
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

    var unitPrice by remember {
        mutableStateOf("")
    }

    var payment by remember {
        mutableStateOf("")
    }

    var note by remember {
        mutableStateOf("")
    }

    val lines =
        remember {
            mutableStateListOf<
                DealershipSaleLineInput
                >()
        }

    val productById =
        remember(products) {
            products.associateBy {
                it.id
            }
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                v15Text(
                    "নতুন ডিলার ইনভয়েস",
                    "New dealer invoice"
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
                        "ডিলার নির্বাচন",
                        "Select dealer"
                    ),
                    fontWeight =
                        FontWeight.Bold
                )

                dealers.forEach { dealer ->
                    OutlinedButton(
                        onClick = {
                            dealerId =
                                dealer.id
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (
                                dealerId ==
                                dealer.id
                            ) {
                                "✓ ${dealer.name}"
                            } else {
                                dealer.name
                            }
                        )
                    }
                }

                DealershipField(
                    invoiceNo,
                    { invoiceNo = it },
                    v15Text(
                        "ইনভয়েস নম্বর (ফাঁকা রাখা যাবে)",
                        "Invoice number (optional)"
                    )
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

                            val policyPrice =
                                priceByProduct[
                                    product.id
                                ]

                            unitPrice =
                                (
                                    policyPrice
                                        ?.takeIf {
                                            it > 0
                                        }
                                        ?: product
                                            .sellingPrice
                                    ).toString()
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

                DealershipField(
                    quantity,
                    { quantity = it },
                    v15Text(
                        "পরিমাণ",
                        "Quantity"
                    )
                )

                DealershipField(
                    unitPrice,
                    { unitPrice = it },
                    v15Text(
                        "ডিলার বিক্রয় মূল্য",
                        "Dealer unit price"
                    )
                )

                OutlinedButton(
                    enabled =
                        productId != null &&
                        (
                            quantity.toIntOrNull()
                                ?: 0
                        ) > 0 &&
                        (
                            unitPrice
                                .toDoubleOrNull()
                                ?: 0.0
                        ) > 0,
                    onClick = {
                        lines.add(
                            DealershipSaleLineInput(
                                productId =
                                    productId!!,
                                quantity =
                                    quantity
                                        .toIntOrNull()
                                        ?: 0,
                                unitPrice =
                                    unitPrice
                                        .toDoubleOrNull()
                                        ?: 0.0
                            )
                        )

                        productId = null
                        quantity = ""
                        unitPrice = ""
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text(
                        v15Text(
                            "+ পণ্য লাইন যোগ করুন",
                            "+ Add product line"
                        )
                    )
                }

                if (lines.isNotEmpty()) {
                    Text(
                        v15Text(
                            "ইনভয়েসের পণ্য",
                            "Invoice items"
                        ),
                        fontWeight =
                            FontWeight.Bold
                    )

                    lines.forEachIndexed {
                            index,
                            line ->

                        val product =
                            productById[
                                line.productId
                            ]

                        Row(
                            modifier =
                                Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.SpaceBetween
                        ) {
                            Text(
                                "${product?.name ?: line.productId.toString()} × ${line.quantity}"
                            )

                            TextButton(
                                onClick = {
                                    lines.removeAt(
                                        index
                                    )
                                }
                            ) {
                                Text(
                                    v15Text(
                                        "বাদ",
                                        "Remove"
                                    )
                                )
                            }
                        }
                    }

                    val total =
                        lines.sumOf {
                            it.quantity *
                                it.unitPrice
                        }

                    Text(
                        v15Text(
                            "মোট: ${dealershipMoney(total)}",
                            "Total: ${dealershipMoney(total)}"
                        ),
                        fontWeight =
                            FontWeight.Bold
                    )
                }

                DealershipField(
                    payment,
                    { payment = it },
                    v15Text(
                        "প্রাথমিক পেমেন্ট",
                        "Initial payment"
                    )
                )

                DealershipField(
                    note,
                    { note = it },
                    v15Text(
                        "নোট",
                        "Note"
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled =
                    dealerId != null &&
                    lines.isNotEmpty(),
                onClick = {
                    onSave(
                        dealerId!!,
                        invoiceNo,
                        lines.toList(),
                        payment.toDoubleOrNull()
                            ?: 0.0,
                        note
                    )
                }
            ) {
                Text(
                    v15Text(
                        "ইনভয়েস তৈরি করুন",
                        "Create invoice"
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
private fun DealershipInvoiceLedger(
    invoice: DealershipInvoiceSummary,
    viewModel: DealershipViewModel,
    canWrite: Boolean,
    onBack: () -> Unit
) {
    val lines by
        viewModel.observeInvoiceLines(
            invoice.invoiceId
        ).collectAsState(
            initial = emptyList()
        )

    val payments by
        viewModel.observePayments(
            invoice.invoiceId
        ).collectAsState(
            initial = emptyList()
        )

    var paymentAmount by remember {
        mutableStateOf("")
    }

    var paymentNote by remember {
        mutableStateOf("")
    }

    var message by remember {
        mutableStateOf("")
    }

    val liveTotal =
        lines.sumOf {
            it.lineTotal
        }

    val livePaid =
        payments.sumOf {
            it.amount
        }

    val liveDue =
        (
            liveTotal -
                livePaid
            ).coerceAtLeast(0.0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            ),
        verticalArrangement =
            Arrangement.spacedBy(9.dp)
    ) {
        TextButton(
            onClick = onBack
        ) {
            Text(
                v15Text(
                    "← ইনভয়েস তালিকা",
                    "← Invoice list"
                )
            )
        }

        Text(
            invoice.invoiceNo
                .ifBlank {
                    "#${invoice.invoiceId}"
                },
            style =
                MaterialTheme.typography.headlineSmall,
            fontWeight =
                FontWeight.ExtraBold
        )

        Text(
            v15Text(
                "ডিলার: ${invoice.dealerName}",
                "Dealer: ${invoice.dealerName}"
            )
        )

        Text(
            v15Text(
                "মোট: ${dealershipMoney(liveTotal)}",
                "Total: ${dealershipMoney(liveTotal)}"
            )
        )

        Text(
            v15Text(
                "পরিশোধ: ${dealershipMoney(livePaid)}",
                "Paid: ${dealershipMoney(livePaid)}"
            )
        )

        Text(
            v15Text(
                "বকেয়া: ${dealershipMoney(liveDue)}",
                "Due: ${dealershipMoney(liveDue)}"
            ),
            fontWeight =
                FontWeight.Bold
        )

        Text(
            v15Text(
                "পণ্য",
                "Items"
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
                        Modifier.padding(10.dp)
                ) {
                    Text(
                        line.productNameSnapshot,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        "${line.quantity} × ${dealershipMoney(line.unitPrice)} = ${dealershipMoney(line.lineTotal)}"
                    )
                }
            }
        }

        if (
            canWrite &&
            liveDue > 0.009
        ) {
            Text(
                v15Text(
                    "পেমেন্ট যোগ করুন",
                    "Add payment"
                ),
                fontWeight =
                    FontWeight.Bold
            )

            DealershipField(
                paymentAmount,
                {
                    paymentAmount = it
                    message = ""
                },
                v15Text(
                    "পেমেন্টের পরিমাণ",
                    "Payment amount"
                )
            )

            DealershipField(
                paymentNote,
                {
                    paymentNote = it
                },
                v15Text(
                    "পেমেন্ট নোট",
                    "Payment note"
                )
            )

            Button(
                enabled =
                    (
                        paymentAmount
                            .toDoubleOrNull()
                            ?: 0.0
                    ) > 0,
                onClick = {
                    val amount =
                        paymentAmount
                            .toDoubleOrNull()
                            ?: 0.0

                    viewModel.addPayment(
                        invoiceId =
                            invoice.invoiceId,
                        amount =
                            amount,
                        note =
                            paymentNote
                    ) { success ->
                        if (success) {
                            paymentAmount = ""
                            paymentNote = ""

                            message =
                                v15Text(
                                    "পেমেন্ট যোগ হয়েছে।",
                                    "Payment added."
                                )
                        } else {
                            message =
                                v15Text(
                                    "পেমেন্ট যোগ করা যায়নি। বকেয়ার চেয়ে বেশি হয়েছে কি না দেখুন।",
                                    "Payment could not be added. Check that it does not exceed the due."
                                )
                        }
                    }
                },
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Text(
                    v15Text(
                        "পেমেন্ট সেভ করুন",
                        "Save payment"
                    )
                )
            }
        }

        if (message.isNotBlank()) {
            Text(message)
        }

        Text(
            v15Text(
                "পেমেন্ট ইতিহাস",
                "Payment history"
            ),
            fontWeight =
                FontWeight.Bold
        )

        if (payments.isEmpty()) {
            Text(
                v15Text(
                    "কোনো পেমেন্ট নেই।",
                    "No payments yet."
                )
            )
        }

        payments.forEach { payment ->
            Card(
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier =
                        Modifier.padding(10.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.SpaceBetween
                    ) {
                        Text(
                            dealershipDate(
                                payment.paidAt
                            )
                        )

                        Text(
                            dealershipMoney(
                                payment.amount
                            ),
                            fontWeight =
                                FontWeight.Bold
                        )
                    }

                    if (
                        payment.note.isNotBlank()
                    ) {
                        Text(payment.note)
                    }
                }
            }
        }
    }
}

@Composable
private fun DealershipField(
    value: String,
    onChange: (String) -> Unit,
    label: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = {
            Text(label)
        },
        singleLine = true,
        modifier =
            Modifier.fillMaxWidth()
    )
}

private fun dealershipMoney(
    value: Double
): String =
    String.format(
        Locale.US,
        "%.2f",
        value
    )

private fun dealershipDate(
    value: Long
): String =
    SimpleDateFormat(
        "dd MMM yyyy",
        Locale.getDefault()
    ).format(
        Date(value)
    )

private fun dealershipStatusLabel(
    value: String
): String =
    when (value) {
        "PAID" ->
            v15Text(
                "পরিশোধিত",
                "Paid"
            )

        "CANCELLED" ->
            v15Text(
                "বাতিল",
                "Cancelled"
            )

        else ->
            v15Text(
                "চলমান",
                "Open"
            )
    }
