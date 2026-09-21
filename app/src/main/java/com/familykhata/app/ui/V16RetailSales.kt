package com.familykhata.app.ui

import android.content.ClipData
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.familykhata.app.InventoryViewModel
import com.familykhata.app.FamilyKhataViewModel
import com.familykhata.app.RetailSaleLineInput
import com.familykhata.app.data.BakiPersonSummary
import com.familykhata.app.data.ProductStockSummary
import com.familykhata.app.data.RetailSaleEntity
import com.familykhata.app.report.writeRetailInvoicePdf
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class RetailCartLine(
    val productId: Long,
    val name: String,
    val baseUnit: String,
    val selectedUnit: String,
    val unitFactor: Int,
    val availableBase: Int,
    val basePrice: Double,
    val quantity: String,
    val price: String
)

private data class RetailUnitOption(
    val name: String,
    val factor: Int
)

private data class RetailSaleDraft(
    val invoiceNo: String,
    val lines: List<RetailSaleLineInput>,
    val discount: Double,
    val paid: Double,
    val paymentMethod: String,
    val financialAccountId: Long?,
    val bakiPersonId: Long?,
    val customerName: String,
    val customerPhone: String,
    val note: String
)

@Composable
internal fun V16RetailSalesScreen(
    workspace: String,
    shopType: String,
    canWrite: Boolean,
    onExit: () -> Unit
) {
    val vm: InventoryViewModel = viewModel()
    val securityViewModel: FamilyKhataViewModel = viewModel()
    val selectedBusinessId by securityViewModel.selectedBusinessId.collectAsState()
    val products by vm.products.collectAsState()
    val sales by vm.retailSales.collectAsState()
    val bakiPeople by vm.bakiPeople.collectAsState()
    val financialAccounts by
        vm.financialAccounts.collectAsState()

    val activeFinancialAccounts =
        remember(financialAccounts) {
            financialAccounts.filter {
                it.isActive
            }
        }

    val context = LocalContext.current

    var showNewSale by remember {
        mutableStateOf(false)
    }

    var saving by remember {
        mutableStateOf(false)
    }

    var cancellingSale by remember {
        mutableStateOf<RetailSaleEntity?>(null)
    }

    var cancellationBusy by remember {
        mutableStateOf(false)
    }

    var collectingSale by remember {
        mutableStateOf<RetailSaleEntity?>(null)
    }

    var collectionAmount by remember {
        mutableStateOf("")
    }

    var collectionFinancialAccountId by remember {
        mutableStateOf<Long?>(null)
    }

    androidx.compose.runtime.LaunchedEffect(
        collectingSale?.id,
        activeFinancialAccounts
    ) {
        collectionFinancialAccountId =
            activeFinancialAccounts
                .singleOrNull()
                ?.id
    }

    var collectionBusy by remember {
        mutableStateOf(false)
    }

    var selectedSaleId by remember {
        mutableStateOf<Long?>(null)
    }

    val selectedSale =
        selectedSaleId?.let { id ->
            sales.firstOrNull {
                it.id == id
            }
        }

    LaunchedEffect(
        workspace,
        shopType,
        selectedBusinessId
    ) {
        vm.setContext(
            workspaceValue = workspace,
            shopType = shopType,
            businessIdValue = selectedBusinessId
        )
    }

    BackHandler(
        enabled = showNewSale
    ) {
        if (!saving) {
            showNewSale = false
        }
    }

    BackHandler(
        enabled =
            !showNewSale &&
                selectedSale != null
    ) {
        selectedSaleId = null
    }

    BackHandler(
        enabled =
            !showNewSale &&
                selectedSale == null
    ) {
        onExit()
    }

    if (showNewSale) {
        V15DeepScreenContainer(
            title =
                v15Text(
                    "নতুন বিক্রি",
                    "New sale"
                ),
            onBack = {
                if (!saving) {
                    showNewSale = false
                }
            }
        ) {
            RetailSaleForm(
                viewModel = vm,
                bakiPeople = bakiPeople,
                financialAccounts =
                    activeFinancialAccounts,
                products =
                    products.filter {
                        it.totalStock > 0
                    },
                saving = saving,
                onDismiss = {
                    if (!saving) {
                        showNewSale = false
                    }
                },
                onSave = { draft ->
                    if (saving) {
                        return@RetailSaleForm
                    }

                    saving = true

                    vm.createRetailSale(
                        invoiceNo =
                            draft.invoiceNo,
                        lines =
                            draft.lines,
                        discount =
                            draft.discount,
                        paid =
                            draft.paid,
                        paymentMethod =
                            draft.paymentMethod,
                        financialAccountId =
                            draft.financialAccountId,
                        bakiPersonId =
                            draft.bakiPersonId,
                        customerName =
                            draft.customerName,
                        customerPhone =
                            draft.customerPhone,
                        note =
                            draft.note
                    ) { saleId ->
                        saving = false

                        if (saleId != null) {
                            showNewSale = false

                            Toast.makeText(
                                context,
                                v15Text(
                                    "বিক্রি সংরক্ষণ হয়েছে",
                                    "Sale saved"
                                ),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                v15Text(
                                    "বিক্রি সংরক্ষণ করা যায়নি। স্টক ও তথ্য যাচাই করুন।",
                                    "Could not save sale. Check stock and entered values."
                                ),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            )
        }
    } else if (selectedSale != null) {
        RetailSaleDetailScreen(
            sale = selectedSale,
            viewModel = vm,
            onBack = {
                selectedSaleId = null
            }
        )
    } else {
        V15DeepScreenContainer(
            title = v15Text(
                "বিক্রি ও ইনভয়েস",
                "Sales & Invoices"
            ),
            onBack = onExit
        ) {
        val activeSales =
            sales.filter {
                it.status != "CANCELLED"
            }

        val totalSales =
            activeSales.sumOf {
                it.total
            }

        val collected =
            activeSales.sumOf {
                it.paid
            }

        val due =
            activeSales.sumOf {
                (
                    it.total -
                        it.paid
                ).coerceAtLeast(0.0)
            }

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .padding(bottom = 24.dp),
            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {
            Text(
                v15Text(
                    "দোকানের বিক্রি, আদায় ও বাকি এক জায়গায় দেখুন।",
                    "Track sales, collections and dues in one place."
                ),
                style =
                    MaterialTheme.typography.bodySmall,
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
                RetailSaleMetric(
                    title =
                        v15Text(
                            "বিক্রি",
                            "Sales"
                        ),
                    value =
                        "${V14DisplayState.currencySymbol}${retailMoney(totalSales)}",
                    modifier =
                        Modifier.weight(1f)
                )

                RetailSaleMetric(
                    title =
                        v15Text(
                            "আদায়",
                            "Collected"
                        ),
                    value =
                        "${V14DisplayState.currencySymbol}${retailMoney(collected)}",
                    modifier =
                        Modifier.weight(1f)
                )

                RetailSaleMetric(
                    title =
                        v15Text(
                            "বাকি",
                            "Due"
                        ),
                    value =
                        "${V14DisplayState.currencySymbol}${retailMoney(due)}",
                    modifier =
                        Modifier.weight(1f)
                )
            }

            Button(
                onClick = {
                    showNewSale = true
                },
                enabled =
                    canWrite &&
                        products.any {
                            it.totalStock > 0
                        },
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Text(
                    v15Text(
                        "＋ নতুন বিক্রি",
                        "＋ New sale"
                    )
                )
            }

            if (
                canWrite &&
                products.none {
                    it.totalStock > 0
                }
            ) {
                Text(
                    v15Text(
                        "বিক্রি করার আগে অন্তত একটি পণ্যে স্টক যোগ করুন।",
                        "Add stock to at least one product before creating a sale."
                    ),
                    style =
                        MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }

            if (sales.isEmpty()) {
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
                        v15Text(
                            "এখনও কোনো বিক্রি রেকর্ড নেই।",
                            "No sales recorded yet."
                        ),
                        modifier =
                            Modifier.padding(16.dp),
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )
                }
            } else {
                sales.forEach { sale ->
                    RetailSaleCard(
                        sale = sale,
                        canWrite = canWrite,
                        onOpen = {
                            selectedSaleId =
                                sale.id
                        },
                        onCollect = {
                            collectingSale =
                                sale
                            collectionAmount =
                                retailMoney(
                                    (
                                        sale.total -
                                            sale.paid
                                    ).coerceAtLeast(
                                        0.0
                                    )
                                )
                        },
                        onCancel = {
                            cancellingSale =
                                sale
                        }
                    )
                }
            }
        }
    }

    }

    collectingSale?.let { sale ->
        val remainingDue =
            (
                sale.total -
                    sale.paid
            ).coerceAtLeast(
                0.0
            )

        AlertDialog(
            onDismissRequest = {
                if (!collectionBusy) {
                    collectingSale = null
                }
            },
            title = {
                Text(
                    v15Text(
                        "বাকি আদায়",
                        "Collect due"
                    )
                )
            },
            text = {
                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(
                            8.dp
                        )
                ) {
                    Text(
                        v15Text(
                            "বর্তমান বাকি: ${V14DisplayState.currencySymbol}${retailMoney(remainingDue)}",
                            "Current due: ${V14DisplayState.currencySymbol}${retailMoney(remainingDue)}"
                        )
                    )

                    Text(
                        v15Text(
                            "যে হিসাবে টাকা জমা হবে",
                            "Financial Account"
                        ),
                        fontWeight =
                            FontWeight.SemiBold
                    )

                    RetailFinancialAccountPicker(
                        accounts =
                            activeFinancialAccounts,
                        selectedId =
                            collectionFinancialAccountId,
                        enabled =
                            !collectionBusy,
                        onSelect = {
                            collectionFinancialAccountId =
                                it
                        }
                    )

                    OutlinedTextField(
                        value =
                            collectionAmount,
                        onValueChange = {
                            collectionAmount =
                                it
                        },
                        label = {
                            Text(
                                v15Text(
                                    "আদায়ের পরিমাণ",
                                    "Amount collected"
                                )
                            )
                        },
                        singleLine = true,
                        modifier =
                            Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled =
                        !collectionBusy,
                    onClick = {
                        val amount =
                            collectionAmount
                                .retailDoubleOrNull()

                        val selectedAccountId =
                            collectionFinancialAccountId

                        if (
                            selectedAccountId == null ||
                            activeFinancialAccounts.none {
                                it.id ==
                                    selectedAccountId
                            }
                        ) {
                            Toast.makeText(
                                context,
                                v15Text(
                                    "টাকা জমার আর্থিক হিসাব নির্বাচন করুন",
                                    "Select a Financial Account"
                                ),
                                Toast.LENGTH_SHORT
                            ).show()

                            return@Button
                        }

                        if (
                            amount == null ||
                            amount <= 0.0 ||
                            amount >
                                remainingDue +
                                    0.0001
                        ) {
                            Toast.makeText(
                                context,
                                v15Text(
                                    "আদায়ের পরিমাণ সঠিক নয়",
                                    "Invalid collection amount"
                                ),
                                Toast.LENGTH_SHORT
                            ).show()

                            return@Button
                        }

                        collectionBusy =
                            true

                        vm.recordRetailSalePayment(
                            saleId =
                                sale.id,
                            amount =
                                amount,
                            financialAccountId =
                                selectedAccountId
                        ) { success ->
                            collectionBusy =
                                false

                            if (success) {
                                collectingSale =
                                    null

                                Toast.makeText(
                                    context,
                                    v15Text(
                                        "বাকি আদায় সংরক্ষণ হয়েছে",
                                        "Collection saved"
                                    ),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    v15Text(
                                        "আদায় সংরক্ষণ করা যায়নি",
                                        "Could not save collection"
                                    ),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                ) {
                    Text(
                        if (collectionBusy) {
                            v15Text(
                                "সংরক্ষণ হচ্ছে…",
                                "Saving…"
                            )
                        } else {
                            v15Text(
                                "আদায় সংরক্ষণ",
                                "Save collection"
                            )
                        }
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled =
                        !collectionBusy,
                    onClick = {
                        collectingSale =
                            null
                    }
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

    cancellingSale?.let { sale ->
        AlertDialog(
            onDismissRequest = {
                if (!cancellationBusy) {
                    cancellingSale = null
                }
            },
            title = {
                Text(
                    v15Text(
                        "বিক্রি বাতিল করবেন?",
                        "Cancel this sale?"
                    )
                )
            },
            text = {
                Text(
                    v15Text(
                        "ইনভয়েস ${sale.invoiceNo} বাতিল করলে বিক্রিটি ইতিহাসে থাকবে, কিন্তু মোট বিক্রি/আদায়/বাকির হিসাবে ধরা হবে না। পণ্য ও স্টক এখনও থাকলে বিক্রি হওয়া স্টক আবার ফেরত যাবে।",
                        "Invoice ${sale.invoiceNo} will remain in history but will no longer count toward sales, collections or dues. Sold stock will be restored when the product and stock batches still exist."
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (cancellationBusy) {
                            return@Button
                        }

                        cancellationBusy = true

                        vm.cancelRetailSale(
                            sale.id
                        ) { success ->
                            cancellationBusy = false

                            if (success) {
                                cancellingSale = null

                                Toast.makeText(
                                    context,
                                    v15Text(
                                        "বিক্রি বাতিল হয়েছে",
                                        "Sale cancelled"
                                    ),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    v15Text(
                                        "বিক্রি বাতিল করা যায়নি। স্টক তথ্য যাচাই করুন।",
                                        "Could not cancel sale. Check the stock data."
                                    ),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                    enabled =
                        !cancellationBusy
                ) {
                    Text(
                        if (cancellationBusy) {
                            v15Text(
                                "বাতিল হচ্ছে…",
                                "Cancelling…"
                            )
                        } else {
                            v15Text(
                                "বিক্রি বাতিল করুন",
                                "Cancel sale"
                            )
                        }
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        cancellingSale = null
                    },
                    enabled =
                        !cancellationBusy
                ) {
                    Text(
                        v15Text(
                            "ফিরে যান",
                            "Go back"
                        )
                    )
                }
            }
        )
    }

}

@Composable
private fun RetailSaleDetailScreen(
    sale: RetailSaleEntity,
    viewModel: InventoryViewModel,
    onBack: () -> Unit
) {
    val context =
        LocalContext.current

    val scope =
        rememberCoroutineScope()

    val linesFlow =
        remember(
            sale.id
        ) {
            viewModel.observeRetailSaleLines(
                sale.id
            )
        }

    val lines by
        linesFlow.collectAsState(
            initial = emptyList()
        )

    var sharing by remember(
        sale.id
    ) {
        mutableStateOf(false)
    }

    var error by remember(
        sale.id
    ) {
        mutableStateOf<String?>(null)
    }

    val due =
        (
            sale.total -
                sale.paid
        ).coerceAtLeast(
            0.0
        )

    V15DeepScreenContainer(
        title =
            v15Text(
                "বিক্রির বিস্তারিত",
                "Sale details"
            ),
        onBack = onBack
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .padding(
                        bottom = 24.dp
                    ),
            verticalArrangement =
                Arrangement.spacedBy(
                    10.dp
                )
        ) {
            Text(
                sale.invoiceNo,
                style =
                    MaterialTheme
                        .typography
                        .headlineSmall,
                fontWeight =
                    FontWeight.ExtraBold
            )

            Text(
                "${retailDate(sale.soldAt)} • ${retailStatusLabel(sale.status)}",
                style =
                    MaterialTheme
                        .typography
                        .bodySmall,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )

            if (
                sale.customerName
                    .isNotBlank()
            ) {
                Text(
                    v15Text(
                        "ক্রেতা: ${sale.customerName}",
                        "Customer: ${sale.customerName}"
                    )
                )
            }

            if (
                sale.customerPhone
                    .isNotBlank()
            ) {
                Text(
                    v15Text(
                        "মোবাইল: ${sale.customerPhone}",
                        "Phone: ${sale.customerPhone}"
                    )
                )
            }

            Text(
                v15Text(
                    "পেমেন্ট: ${retailPaymentLabel(sale.paymentMethod)}",
                    "Payment: ${retailPaymentLabel(sale.paymentMethod)}"
                )
            )

            if (
                sale.status ==
                "CANCELLED"
            ) {
                Surface(
                    modifier =
                        Modifier.fillMaxWidth(),
                    shape =
                        MaterialTheme
                            .shapes
                            .large,
                    color =
                        MaterialTheme
                            .colorScheme
                            .errorContainer
                ) {
                    Text(
                        v15Text(
                            "এই বিক্রিটি বাতিল করা হয়েছে এবং সক্রিয় বিক্রির হিসাবে গণনা হয় না।",
                            "This sale is cancelled and is not counted as an active sale."
                        ),
                        modifier =
                            Modifier.padding(
                                12.dp
                            ),
                        color =
                            MaterialTheme
                                .colorScheme
                                .onErrorContainer
                    )
                }
            }

            Text(
                v15Text(
                    "পণ্যসমূহ",
                    "Items"
                ),
                fontWeight =
                    FontWeight.Bold
            )

            if (lines.isEmpty()) {
                Text(
                    v15Text(
                        "পণ্যের তথ্য পাওয়া যাচ্ছে না।",
                        "No sale-line information available."
                    ),
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            } else {
                lines.forEach { line ->
                    Card(
                        modifier =
                            Modifier.fillMaxWidth(),
                        colors =
                            CardDefaults
                                .cardColors(
                                    containerColor =
                                        MaterialTheme
                                            .colorScheme
                                            .surfaceVariant
                                            .copy(
                                                alpha =
                                                    0.45f
                                            )
                                )
                    ) {
                        Column(
                            modifier =
                                Modifier.padding(
                                    12.dp
                                ),
                            verticalArrangement =
                                Arrangement.spacedBy(
                                    4.dp
                                )
                        ) {
                            Text(
                                line.productNameSnapshot,
                                fontWeight =
                                    FontWeight.SemiBold
                            )

                            if (
                                line.skuSnapshot
                                    .isNotBlank()
                            ) {
                                Text(
                                    "SKU: ${line.skuSnapshot}",
                                    style =
                                        MaterialTheme
                                            .typography
                                            .bodySmall
                                )
                            }

                            Text(
                                "${line.quantity} ${line.unitSnapshot} × " +
                                    "${V14DisplayState.currencySymbol}${retailMoney(line.unitPrice)}"
                            )

                            Text(
                                "${V14DisplayState.currencySymbol}${retailMoney(line.lineTotal)}",
                                fontWeight =
                                    FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Surface(
                modifier =
                    Modifier.fillMaxWidth(),
                shape =
                    MaterialTheme
                        .shapes
                        .large,
                color =
                    MaterialTheme
                        .colorScheme
                        .surfaceVariant
            ) {
                Column(
                    modifier =
                        Modifier.padding(
                            12.dp
                        ),
                    verticalArrangement =
                        Arrangement.spacedBy(
                            5.dp
                        )
                ) {
                    Text(
                        v15Text(
                            "সাবটোটাল: ${V14DisplayState.currencySymbol}${retailMoney(sale.subtotal)}",
                            "Subtotal: ${V14DisplayState.currencySymbol}${retailMoney(sale.subtotal)}"
                        )
                    )

                    if (
                        sale.discount >
                        0.0001
                    ) {
                        Text(
                            v15Text(
                                "ছাড়: ${V14DisplayState.currencySymbol}${retailMoney(sale.discount)}",
                                "Discount: ${V14DisplayState.currencySymbol}${retailMoney(sale.discount)}"
                            )
                        )
                    }

                    Text(
                        v15Text(
                            "মোট: ${V14DisplayState.currencySymbol}${retailMoney(sale.total)}",
                            "Total: ${V14DisplayState.currencySymbol}${retailMoney(sale.total)}"
                        ),
                        fontWeight =
                            FontWeight.ExtraBold
                    )

                    Text(
                        v15Text(
                            "আদায়: ${V14DisplayState.currencySymbol}${retailMoney(sale.paid)}",
                            "Paid: ${V14DisplayState.currencySymbol}${retailMoney(sale.paid)}"
                        )
                    )

                    Text(
                        v15Text(
                            "বাকি: ${V14DisplayState.currencySymbol}${retailMoney(due)}",
                            "Due: ${V14DisplayState.currencySymbol}${retailMoney(due)}"
                        ),
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }

            if (
                sale.note
                    .isNotBlank()
            ) {
                Text(
                    v15Text(
                        "নোট: ${sale.note}",
                        "Note: ${sale.note}"
                    )
                )
            }

            Button(
                enabled =
                    !sharing &&
                        lines.isNotEmpty(),
                modifier =
                    Modifier.fillMaxWidth(),
                onClick = {
                    sharing = true
                    error = null

                    val bangla =
                        V15LanguageState
                            .isBangla()

                    val currency =
                        V14DisplayState
                            .currencySymbol

                    scope.launch {
                        try {
                            val pdf =
                                withContext(
                                    Dispatchers.IO
                                ) {
                                    writeRetailInvoicePdf(
                                        context =
                                            context.applicationContext,
                                        sale = sale,
                                        lines = lines,
                                        bangla = bangla,
                                        currency = currency
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
                                        "Hisabi Khata - ${sale.invoiceNo}"
                                    )

                                    clipData =
                                        ClipData.newRawUri(
                                            "Sales invoice",
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
                                        "ইনভয়েস PDF শেয়ার করুন",
                                        "Share invoice PDF"
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
                            error =
                                v15Text(
                                    "ইনভয়েস PDF তৈরি বা শেয়ার করা যায়নি। আবার চেষ্টা করুন।",
                                    "Unable to create or share the invoice PDF. Please try again."
                                )
                        } finally {
                            sharing = false
                        }
                    }
                }
            ) {
                Text(
                    if (sharing) {
                        v15Text(
                            "PDF তৈরি হচ্ছে…",
                            "Creating PDF…"
                        )
                    } else {
                        v15Text(
                            "PDF ইনভয়েস শেয়ার",
                            "Share PDF invoice"
                        )
                    }
                )
            }

            if (sharing) {
                CircularProgressIndicator()
            }

            error?.let {
                Text(
                    it,
                    color =
                        MaterialTheme
                            .colorScheme
                            .error
                )
            }

            Text(
                v15Text(
                    "WhatsApp, Messenger, ইমেইল বা অন্য অ্যাপ Share menu থেকে বেছে নিতে পারবেন।",
                    "Choose WhatsApp, Messenger, email or another app from the Share menu."
                ),
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
}

@Composable
private fun RetailSaleMetric(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape =
            MaterialTheme.shapes.large,
        color =
            MaterialTheme.colorScheme
                .surfaceVariant
    ) {
        Column(
            modifier =
                Modifier.padding(10.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            Text(
                value,
                fontWeight =
                    FontWeight.ExtraBold,
                style =
                    MaterialTheme.typography
                        .titleMedium
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
private fun RetailSaleCard(
    sale: RetailSaleEntity,
    canWrite: Boolean,
    onOpen: () -> Unit,
    onCollect: () -> Unit,
    onCancel: () -> Unit
) {
    val due =
        (
            sale.total -
                sale.paid
        ).coerceAtLeast(0.0)

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme
                        .surfaceVariant
                        .copy(alpha = 0.45f)
            )
    ) {
        Column(
            modifier =
                Modifier.padding(12.dp),
            verticalArrangement =
                Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text(
                        sale.invoiceNo,
                        fontWeight =
                            FontWeight.Bold
                    )

                    if (
                        sale.customerName
                            .isNotBlank()
                    ) {
                        Text(
                            sale.customerName,
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall
                        )
                    }
                }

                Surface(
                    shape =
                        MaterialTheme.shapes
                            .small,
                    color =
                        MaterialTheme
                            .colorScheme
                            .primaryContainer
                ) {
                    Text(
                        retailStatusLabel(
                            sale.status
                        ),
                        modifier =
                            Modifier.padding(
                                horizontal = 8.dp,
                                vertical = 4.dp
                            ),
                        style =
                            MaterialTheme
                                .typography
                                .labelSmall
                    )
                }
            }

            Text(
                "${V14DisplayState.currencySymbol}${retailMoney(sale.total)}  •  " +
                    v15Text(
                        "আদায় ${V14DisplayState.currencySymbol}${retailMoney(sale.paid)}",
                        "Collected ${V14DisplayState.currencySymbol}${retailMoney(sale.paid)}"
                    ),
                fontWeight =
                    FontWeight.SemiBold
            )

            if (due > 0.0001) {
                Text(
                    v15Text(
                        "বাকি: ${V14DisplayState.currencySymbol}${retailMoney(due)}",
                        "Due: ${V14DisplayState.currencySymbol}${retailMoney(due)}"
                    ),
                    color =
                        MaterialTheme.colorScheme
                            .error,
                    style =
                        MaterialTheme.typography
                            .bodySmall
                )
            }

            Text(
                "${retailPaymentLabel(sale.paymentMethod)} • ${retailDate(sale.soldAt)}",
                style =
                    MaterialTheme.typography
                        .bodySmall,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )

            OutlinedButton(
                onClick = onOpen,
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Text(
                    v15Text(
                        "বিস্তারিত ও ইনভয়েস",
                        "Details & Invoice"
                    )
                )
            }

            if (
                canWrite &&
                sale.status != "CANCELLED" &&
                due > 0.0001
            ) {
                OutlinedButton(
                    onClick = onCollect,
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text(
                        v15Text(
                            "বাকি আদায়",
                            "Collect due"
                        )
                    )
                }
            }

            if (
                canWrite &&
                sale.status != "CANCELLED"
            ) {
                TextButton(
                    onClick = onCancel
                ) {
                    Text(
                        v15Text(
                            "বিক্রি বাতিল",
                            "Cancel sale"
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun RetailSaleForm(
    viewModel: InventoryViewModel,
    bakiPeople: List<BakiPersonSummary>,
    financialAccounts:
        List<com.familykhata.app.data.FinancialAccountSummary>,
    products: List<ProductStockSummary>,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (RetailSaleDraft) -> Unit
) {
    val context = LocalContext.current
    val barcodeScanner =
        remember {
            GmsBarcodeScanning.getClient(
                context
            )
        }

    var invoiceNo by remember {
        mutableStateOf("")
    }

    var customerName by remember {
        mutableStateOf("")
    }

    var customerPhone by remember {
        mutableStateOf("")
    }

    var selectedBakiPersonId by remember {
        mutableStateOf<Long?>(null)
    }

    var bakiSearch by remember {
        mutableStateOf("")
    }

    var discount by remember {
        mutableStateOf("0")
    }

    var paid by remember {
        mutableStateOf("0")
    }

    var paymentMethod by remember {
        mutableStateOf("CASH")
    }

    var selectedFinancialAccountId by remember {
        mutableStateOf<Long?>(null)
    }

    androidx.compose.runtime.LaunchedEffect(
        financialAccounts
    ) {
        if (
            selectedFinancialAccountId == null ||
            financialAccounts.none {
                it.id ==
                    selectedFinancialAccountId
            }
        ) {
            selectedFinancialAccountId =
                financialAccounts
                    .singleOrNull()
                    ?.id
        }
    }

    var note by remember {
        mutableStateOf("")
    }

    var search by remember {
        mutableStateOf("")
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    val cart =
        remember {
            mutableStateListOf<RetailCartLine>()
        }

    fun addScannedProductToCart(
        product: ProductStockSummary
    ) {
        val existingIndex =
            cart.indexOfFirst {
                it.productId ==
                    product.id
            }

        if (existingIndex >= 0) {
            val current =
                cart[existingIndex]

            val currentQuantity =
                current.quantity
                    .retailIntOrNull()
                    ?.coerceAtLeast(0)
                    ?: 0

            val nextQuantity =
                currentQuantity.toLong() +
                    1L

            val requiredBase =
                nextQuantity *
                    current.unitFactor
                        .toLong()

            if (
                nextQuantity <=
                    Int.MAX_VALUE.toLong() &&
                requiredBase <=
                    current.availableBase
                        .toLong()
            ) {
                cart[existingIndex] =
                    current.copy(
                        quantity =
                            nextQuantity
                                .toString()
                    )

                Toast.makeText(
                    context,
                    v15Text(
                        "${product.name} • পরিমাণ ${nextQuantity}",
                        "${product.name} • Qty ${nextQuantity}"
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    context,
                    v15Text(
                        "${product.name}-এর পর্যাপ্ত স্টক নেই",
                        "Not enough stock for ${product.name}"
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            }

            return
        }

        if (product.totalStock <= 0) {
            Toast.makeText(
                context,
                v15Text(
                    "${product.name}-এর স্টক নেই",
                    "${product.name} is out of stock"
                ),
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        cart +=
            RetailCartLine(
                productId =
                    product.id,
                name =
                    product.name,
                baseUnit =
                    product.unit,
                selectedUnit =
                    product.unit,
                unitFactor = 1,
                availableBase =
                    product.totalStock,
                basePrice =
                    product.sellingPrice,
                quantity = "1",
                price =
                    retailMoney(
                        product.sellingPrice
                    )
            )

        Toast.makeText(
            context,
            v15Text(
                "${product.name} কার্টে যোগ হয়েছে",
                "${product.name} added to cart"
            ),
            Toast.LENGTH_SHORT
        ).show()
    }

    fun addExactSkuToCart(
        rawCode: String
    ) {
        val code =
            rawCode.trim()

        if (code.isBlank()) {
            Toast.makeText(
                context,
                v15Text(
                    "Barcode / SKU লিখুন",
                    "Enter a Barcode / SKU"
                ),
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val matches =
            products.filter { product ->
                product.sku
                    .trim()
                    .equals(
                        code,
                        ignoreCase = true
                    )
            }

        when {
            matches.size == 1 -> {
                error = null
                search = ""

                addScannedProductToCart(
                    matches.first()
                )
            }

            matches.isEmpty() -> {
                search = code

                Toast.makeText(
                    context,
                    v15Text(
                        "এই Barcode / SKU-এর পণ্য পাওয়া যায়নি: $code",
                        "No product found for Barcode / SKU: $code"
                    ),
                    Toast.LENGTH_LONG
                ).show()
            }

            else -> {
                search = code

                Toast.makeText(
                    context,
                    v15Text(
                        "একই Barcode / SKU একাধিক পণ্যে আছে। আগে duplicate SKU ঠিক করুন।",
                        "This Barcode / SKU is assigned to multiple products. Fix the duplicate SKU first."
                    ),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    val filteredProducts =
        products.filter { product ->
            search.isBlank() ||
                product.name.contains(
                    search,
                    ignoreCase = true
                ) ||
                product.sku.contains(
                    search,
                    ignoreCase = true
                )
        }

    val subtotal =
        cart.sumOf { item ->
            val quantity =
                item.quantity
                    .retailIntOrNull()

            val price =
                item.price
                    .retailDoubleOrNull()

            if (
                quantity != null &&
                quantity > 0 &&
                price != null &&
                price >= 0.0
            ) {
                quantity * price
            } else {
                0.0
            }
        }

    val totalCartQuantity =
        cart.sumOf { item ->
            item.quantity
                .retailIntOrNull()
                ?.coerceAtLeast(0)
                ?: 0
        }

    val discountValue =
        discount.retailDoubleOrNull()
            ?: 0.0

    val total =
        (
            subtotal -
                discountValue
                    .coerceAtLeast(0.0)
        ).coerceAtLeast(0.0)

    val paidValue =
        paid.retailDoubleOrNull()
            ?: 0.0

    val filteredBakiPeople =
        bakiPeople.filter { person ->
            bakiSearch.isBlank() ||
                person.name.contains(
                    bakiSearch,
                    ignoreCase = true
                ) ||
                person.phone.contains(
                    bakiSearch,
                    ignoreCase = true
                )
        }

    val due =
        (
            total -
                paidValue
                    .coerceAtLeast(0.0)
        ).coerceAtLeast(0.0)

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(horizontal = 12.dp)
                .padding(bottom = 28.dp),
        verticalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {
                OutlinedTextField(
                    value = invoiceNo,
                    onValueChange = {
                        invoiceNo = it
                    },
                    label = {
                        Text(
                            v15Text(
                                "ইনভয়েস নম্বর (ঐচ্ছিক)",
                                "Invoice number (optional)"
                            )
                        )
                    },
                    supportingText = {
                        Text(
                            v15Text(
                                "খালি রাখলে স্বয়ংক্রিয়ভাবে তৈরি হবে।",
                                "Leave blank to generate automatically."
                            )
                        )
                    },
                    singleLine = true,
                    modifier =
                        Modifier.fillMaxWidth()
                )

                Text(
                    v15Text(
                        "পণ্য যোগ করুন",
                        "Add products"
                    ),
                    fontWeight =
                        FontWeight.Bold
                )

                OutlinedTextField(
                    value = search,
                    onValueChange = {
                        search = it
                    },
                    label = {
                        Text(
                            v15Text(
                                "পণ্য / Barcode / SKU খুঁজুন",
                                "Search product / Barcode / SKU"
                            )
                        )
                    },
                    singleLine = true,
                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedButton(
                    onClick = {
                        addExactSkuToCart(
                            search
                        )
                    },
                    enabled =
                        !saving &&
                            search.trim().isNotBlank(),
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text(
                        v15Text(
                            "＋ Barcode / SKU দিয়ে দ্রুত যোগ",
                            "＋ Quick add by Barcode / SKU"
                        )
                    )
                }

                OutlinedButton(
                    onClick = {
                        barcodeScanner
                            .startScan()
                            .addOnSuccessListener {
                                    barcode ->

                                addExactSkuToCart(
                                    barcode.rawValue
                                        .orEmpty()
                                )
                            }
                            .addOnFailureListener {
                                Toast.makeText(
                                    context,
                                    v15Text(
                                        "বারকোড স্ক্যান করা যায়নি",
                                        "Barcode scan failed"
                                    ),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    },
                    enabled = !saving,
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text(
                        v15Text(
                            "▣ স্ক্যান করে কার্টে যোগ করুন",
                            "▣ Scan & Add to Cart"
                        )
                    )
                }

                filteredProducts
                    .take(12)
                    .forEach { product ->
                        val alreadyAdded =
                            cart.any {
                                it.productId ==
                                    product.id
                            }

                        OutlinedButton(
                            onClick = {
                                addScannedProductToCart(
                                    product
                                )
                            },
                            enabled =
                                !saving &&
                                    product.totalStock > 0,
                            modifier =
                                Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (alreadyAdded) {
                                    v15Text(
                                        "＋1 ${product.name} • কার্টে আছে",
                                        "＋1 ${product.name} • In cart"
                                    )
                                } else {
                                    v15Text(
                                        "＋ ${product.name} • স্টক ${product.totalStock}",
                                        "＋ ${product.name} • Stock ${product.totalStock}"
                                    )
                                }
                            )
                        }
                    }

                if (
                    filteredProducts.size > 12
                ) {
                    Text(
                        v15Text(
                            "আরও নির্দিষ্ট নাম বা বারকোড লিখে খুঁজুন।",
                            "Type a more specific name or barcode to narrow the list."
                        ),
                        style =
                            MaterialTheme.typography
                                .bodySmall,
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )
                }

                if (cart.isNotEmpty()) {
                    Text(
                        v15Text(
                            "বিক্রির পণ্য",
                            "Sale items"
                        ),
                        fontWeight =
                            FontWeight.Bold
                    )
                }

                cart.forEachIndexed {
                        index,
                        item ->

                    key(item.productId) {
                        val unitFlow =
                            remember(
                                item.productId
                            ) {
                                viewModel
                                    .observeProductUnitConversions(
                                        item.productId
                                    )
                            }

                        val unitConversions by
                            unitFlow.collectAsState(
                                initial =
                                    emptyList()
                            )

                        val unitOptions =
                            remember(
                                item.baseUnit,
                                unitConversions
                            ) {
                                buildList {
                                    add(
                                        RetailUnitOption(
                                            name =
                                                item.baseUnit,
                                            factor =
                                                1
                                        )
                                    )

                                    unitConversions
                                        .filter {
                                            it.unitName
                                                .isNotBlank() &&
                                                it.baseQuantity >
                                                    1 &&
                                                !it.unitName
                                                    .equals(
                                                        item.baseUnit,
                                                        ignoreCase =
                                                            true
                                                    )
                                        }
                                        .forEach {
                                            add(
                                                RetailUnitOption(
                                                    name =
                                                        it.unitName,
                                                    factor =
                                                        it.baseQuantity
                                                )
                                            )
                                        }
                                }
                            }

                        val selectedFactor =
                            item.unitFactor
                                .coerceAtLeast(1)

                        val availableSelected =
                            item.availableBase /
                                selectedFactor

                        Card(
                            modifier =
                                Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier =
                                    Modifier.padding(10.dp),
                                verticalArrangement =
                                    Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    item.name,
                                    fontWeight =
                                        FontWeight.SemiBold
                                )

                                Text(
                                    v15Text(
                                        "স্টক: ${item.availableBase} ${item.baseUnit} • ${availableSelected} ${item.selectedUnit} পর্যন্ত",
                                        "Stock: ${item.availableBase} ${item.baseUnit} • up to ${availableSelected} ${item.selectedUnit}"
                                    ),
                                    style =
                                        MaterialTheme.typography
                                            .bodySmall
                                )

                                if (
                                    unitOptions.size > 1
                                ) {
                                    Text(
                                        v15Text(
                                            "বিক্রির ইউনিট",
                                            "Selling unit"
                                        ),
                                        style =
                                            MaterialTheme
                                                .typography
                                                .bodySmall,
                                        fontWeight =
                                            FontWeight.SemiBold
                                    )

                                    Column {
                                        unitOptions
                                            .forEach { option ->
                                                TextButton(
                                                    onClick = {
                                                        cart[index] =
                                                            item.copy(
                                                                selectedUnit =
                                                                    option.name,
                                                                unitFactor =
                                                                    option.factor,
                                                                price =
                                                                    retailMoney(
                                                                        item.basePrice *
                                                                            option.factor
                                                                    )
                                                            )
                                                    },
                                                    enabled =
                                                        !saving
                                                ) {
                                                    val relation =
                                                        if (
                                                            option.factor ==
                                                            1
                                                        ) {
                                                            option.name
                                                        } else {
                                                            "${option.name} • 1 = ${option.factor} ${item.baseUnit}"
                                                        }

                                                    Text(
                                                        if (
                                                            item.selectedUnit
                                                                .equals(
                                                                    option.name,
                                                                    ignoreCase =
                                                                        true
                                                                ) &&
                                                            item.unitFactor ==
                                                                option.factor
                                                        ) {
                                                            "✓ $relation"
                                                        } else {
                                                            relation
                                                        }
                                                    )
                                                }
                                            }
                                    }
                                }

                                Row(
                                    modifier =
                                        Modifier.fillMaxWidth(),
                                    horizontalArrangement =
                                        Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value =
                                            item.quantity,
                                        onValueChange = {
                                            cart[index] =
                                                item.copy(
                                                    quantity = it
                                                )
                                        },
                                        label = {
                                            Text(
                                                v15Text(
                                                    "পরিমাণ (${item.selectedUnit})",
                                                    "Qty (${item.selectedUnit})"
                                                )
                                            )
                                        },
                                        singleLine = true,
                                        modifier =
                                            Modifier.weight(1f)
                                    )

                                    OutlinedTextField(
                                        value =
                                            item.price,
                                        onValueChange = {
                                            cart[index] =
                                                item.copy(
                                                    price = it
                                                )
                                        },
                                        label = {
                                            Text(
                                                v15Text(
                                                    "দর / ${item.selectedUnit}",
                                                    "Price / ${item.selectedUnit}"
                                                )
                                            )
                                        },
                                        singleLine = true,
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
                                    OutlinedButton(
                                        onClick = {
                                            val currentQuantity =
                                                item.quantity
                                                    .retailIntOrNull()
                                                    ?: 1

                                            if (
                                                currentQuantity >
                                                1
                                            ) {
                                                cart[index] =
                                                    item.copy(
                                                        quantity =
                                                            (
                                                                currentQuantity -
                                                                    1
                                                            ).toString()
                                                    )
                                            }
                                        },
                                        enabled =
                                            !saving &&
                                                (
                                                    item.quantity
                                                        .retailIntOrNull()
                                                        ?: 1
                                                ) > 1,
                                        modifier =
                                            Modifier.weight(1f)
                                    ) {
                                        Text("− 1")
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val currentQuantity =
                                                item.quantity
                                                    .retailIntOrNull()
                                                    ?.coerceAtLeast(
                                                        0
                                                    )
                                                    ?: 0

                                            val nextQuantity =
                                                currentQuantity
                                                    .toLong() +
                                                    1L

                                            val requiredBase =
                                                nextQuantity *
                                                    item.unitFactor
                                                        .toLong()

                                            if (
                                                nextQuantity <=
                                                    Int.MAX_VALUE
                                                        .toLong() &&
                                                requiredBase <=
                                                    item.availableBase
                                                        .toLong()
                                            ) {
                                                cart[index] =
                                                    item.copy(
                                                        quantity =
                                                            nextQuantity
                                                                .toString()
                                                    )
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    v15Text(
                                                        "${item.name}-এর পর্যাপ্ত স্টক নেই",
                                                        "Not enough stock for ${item.name}"
                                                    ),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        },
                                        enabled = !saving,
                                        modifier =
                                            Modifier.weight(1f)
                                    ) {
                                        Text("＋ 1")
                                    }
                                }

                                if (
                                    item.unitFactor > 1
                                ) {
                                    val enteredQuantity =
                                        item.quantity
                                            .retailIntOrNull()

                                    if (
                                        enteredQuantity != null &&
                                        enteredQuantity > 0
                                    ) {
                                        val baseNeeded =
                                            enteredQuantity
                                                .toLong() *
                                                item.unitFactor
                                                    .toLong()

                                        Text(
                                            v15Text(
                                                "${enteredQuantity} ${item.selectedUnit} = ${baseNeeded} ${item.baseUnit}",
                                                "${enteredQuantity} ${item.selectedUnit} = ${baseNeeded} ${item.baseUnit}"
                                            ),
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

                                TextButton(
                                    onClick = {
                                        cart.removeAt(
                                            index
                                        )
                                    },
                                    enabled = !saving
                                ) {
                                    Text(
                                        v15Text(
                                            "সরান",
                                            "Remove"
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                if (cart.isNotEmpty()) {
                    Text(
                        v15Text(
                            "সাবটোটাল: ${V14DisplayState.currencySymbol}${retailMoney(subtotal)}",
                            "Subtotal: ${V14DisplayState.currencySymbol}${retailMoney(subtotal)}"
                        ),
                        fontWeight =
                            FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = discount,
                    onValueChange = {
                        discount = it
                    },
                    label = {
                        Text(
                            v15Text(
                                "ছাড়",
                                "Discount"
                            )
                        )
                    },
                    singleLine = true,
                    modifier =
                        Modifier.fillMaxWidth()
                )

                Text(
                    v15Text(
                        "মোট: ${V14DisplayState.currencySymbol}${retailMoney(total)}",
                        "Total: ${V14DisplayState.currencySymbol}${retailMoney(total)}"
                    ),
                    style =
                        MaterialTheme.typography
                            .titleMedium,
                    fontWeight =
                        FontWeight.ExtraBold
                )

                Text(
                    v15Text(
                        "যে হিসাবে টাকা জমা হবে",
                        "Financial Account"
                    ),
                    fontWeight =
                        FontWeight.Bold
                )

                RetailFinancialAccountPicker(
                    accounts =
                        financialAccounts,
                    selectedId =
                        selectedFinancialAccountId,
                    enabled =
                        !saving,
                    onSelect = {
                        selectedFinancialAccountId =
                            it
                    }
                )

                if (financialAccounts.isEmpty()) {
                    Text(
                        v15Text(
                            "আদায় থাকলে আগে একটি সক্রিয় আর্থিক হিসাব তৈরি করুন।",
                            "Create an active Financial Account before recording a payment."
                        ),
                        style =
                            MaterialTheme.typography.bodySmall,
                        color =
                            MaterialTheme.colorScheme.error
                    )
                }

                OutlinedTextField(
                    value = paid,
                    onValueChange = {
                        paid = it
                    },
                    label = {
                        Text(
                            v15Text(
                                "এখন আদায়",
                                "Paid now"
                            )
                        )
                    },
                    singleLine = true,
                    modifier =
                        Modifier.fillMaxWidth()
                )

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            paid =
                                retailMoney(total)

                            paymentMethod =
                                "CASH"

                            selectedFinancialAccountId =
                                financialAccounts
                                    .firstOrNull {
                                        it.type ==
                                            "CASH"
                                    }
                                    ?.id
                                    ?: selectedFinancialAccountId
                                    ?: financialAccounts
                                        .singleOrNull()
                                        ?.id
                        },
                        enabled =
                            total >= 0.0 &&
                                !saving,
                        modifier =
                            Modifier.weight(1f)
                    ) {
                        Text(
                            v15Text(
                                "নগদ সম্পূর্ণ",
                                "Full cash"
                            )
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            paid = "0"
                        },
                        enabled = !saving,
                        modifier =
                            Modifier.weight(1f)
                    ) {
                        Text(
                            v15Text(
                                "বাকি রাখুন",
                                "Keep due"
                            )
                        )
                    }
                }

                if (cart.isNotEmpty()) {
                    Card(
                        modifier =
                            Modifier.fillMaxWidth(),
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    MaterialTheme
                                        .colorScheme
                                        .surfaceVariant
                                        .copy(
                                            alpha = 0.5f
                                        )
                            )
                    ) {
                        Column(
                            modifier =
                                Modifier.padding(
                                    12.dp
                                ),
                            verticalArrangement =
                                Arrangement.spacedBy(
                                    4.dp
                                )
                        ) {
                            Text(
                                v15Text(
                                    "চেকআউট সারাংশ",
                                    "Checkout summary"
                                ),
                                fontWeight =
                                    FontWeight.Bold
                            )

                            Text(
                                v15Text(
                                    "পণ্য: ${cart.size} • মোট পরিমাণ: $totalCartQuantity",
                                    "Items: ${cart.size} • Total qty: $totalCartQuantity"
                                )
                            )

                            Text(
                                v15Text(
                                    "সাবটোটাল: ${V14DisplayState.currencySymbol}${retailMoney(subtotal)} • ছাড়: ${V14DisplayState.currencySymbol}${retailMoney(discountValue)}",
                                    "Subtotal: ${V14DisplayState.currencySymbol}${retailMoney(subtotal)} • Discount: ${V14DisplayState.currencySymbol}${retailMoney(discountValue)}"
                                ),
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodySmall
                            )

                            Text(
                                v15Text(
                                    "মোট: ${V14DisplayState.currencySymbol}${retailMoney(total)} • আদায়: ${V14DisplayState.currencySymbol}${retailMoney(paidValue)} • বাকি: ${V14DisplayState.currencySymbol}${retailMoney(due)}",
                                    "Total: ${V14DisplayState.currencySymbol}${retailMoney(total)} • Paid: ${V14DisplayState.currencySymbol}${retailMoney(paidValue)} • Due: ${V14DisplayState.currencySymbol}${retailMoney(due)}"
                                ),
                                fontWeight =
                                    FontWeight.SemiBold
                            )
                        }
                    }
                }

                if (due > 0.0001) {
                    Text(
                        v15Text(
                            "বাকি থাকবে: ${V14DisplayState.currencySymbol}${retailMoney(due)}",
                            "Remaining due: ${V14DisplayState.currencySymbol}${retailMoney(due)}"
                        ),
                        color =
                            MaterialTheme.colorScheme
                                .error,
                        fontWeight =
                            FontWeight.Bold
                    )
                }

                if (cart.isNotEmpty()) {
                    Card(
                        modifier =
                            Modifier.fillMaxWidth(),
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    MaterialTheme
                                        .colorScheme
                                        .surfaceVariant
                                        .copy(
                                            alpha = 0.5f
                                        )
                            )
                    ) {
                        Column(
                            modifier =
                                Modifier.padding(
                                    12.dp
                                ),
                            verticalArrangement =
                                Arrangement.spacedBy(
                                    4.dp
                                )
                        ) {
                            Text(
                                v15Text(
                                    "চেকআউট সারাংশ",
                                    "Checkout summary"
                                ),
                                fontWeight =
                                    FontWeight.Bold
                            )

                            Text(
                                v15Text(
                                    "পণ্য: ${cart.size} • মোট পরিমাণ: $totalCartQuantity",
                                    "Items: ${cart.size} • Total qty: $totalCartQuantity"
                                )
                            )

                            Text(
                                v15Text(
                                    "সাবটোটাল: ${V14DisplayState.currencySymbol}${retailMoney(subtotal)} • ছাড়: ${V14DisplayState.currencySymbol}${retailMoney(discountValue)}",
                                    "Subtotal: ${V14DisplayState.currencySymbol}${retailMoney(subtotal)} • Discount: ${V14DisplayState.currencySymbol}${retailMoney(discountValue)}"
                                ),
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodySmall
                            )

                            Text(
                                v15Text(
                                    "মোট: ${V14DisplayState.currencySymbol}${retailMoney(total)} • আদায়: ${V14DisplayState.currencySymbol}${retailMoney(paidValue)} • বাকি: ${V14DisplayState.currencySymbol}${retailMoney(due)}",
                                    "Total: ${V14DisplayState.currencySymbol}${retailMoney(total)} • Paid: ${V14DisplayState.currencySymbol}${retailMoney(paidValue)} • Due: ${V14DisplayState.currencySymbol}${retailMoney(due)}"
                                ),
                                fontWeight =
                                    FontWeight.SemiBold
                            )
                        }
                    }
                }

                if (due > 0.0001) {
                    Text(
                        v15Text(
                            "বাকির খাতা নির্বাচন",
                            "Select Baki ledger"
                        ),
                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        v15Text(
                            "বাকি বিক্রি হলে একটি বাকির খাতার সাথে যুক্ত করা আবশ্যক।",
                            "A due sale must be linked to an existing Baki ledger."
                        ),
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )

                    if (bakiPeople.isEmpty()) {
                        Text(
                            v15Text(
                                "আগে বাকির খাতায় ক্রেতা যোগ করুন, তারপর বাকি বিক্রি সংরক্ষণ করুন।",
                                "Add the customer to Baki first, then save the due sale."
                            ),
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .error,
                            fontWeight =
                                FontWeight.SemiBold
                        )
                    } else {
                        OutlinedTextField(
                            value =
                                bakiSearch,
                            onValueChange = {
                                bakiSearch = it
                            },
                            label = {
                                Text(
                                    v15Text(
                                        "নাম / মোবাইল দিয়ে খুঁজুন",
                                        "Search name / phone"
                                    )
                                )
                            },
                            singleLine = true,
                            modifier =
                                Modifier.fillMaxWidth()
                        )

                        filteredBakiPeople
                            .take(10)
                            .forEach { person ->
                                OutlinedButton(
                                    onClick = {
                                        selectedBakiPersonId =
                                            person.id

                                        customerName =
                                            person.name

                                        if (
                                            person.phone
                                                .isNotBlank()
                                        ) {
                                            customerPhone =
                                                person.phone
                                        }
                                    },
                                    enabled = !saving,
                                    modifier =
                                        Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        if (
                                            selectedBakiPersonId ==
                                            person.id
                                        ) {
                                            "✓ ${person.name}"
                                        } else {
                                            person.name
                                        }
                                    )
                                }
                            }

                        if (
                            filteredBakiPeople.size >
                            10
                        ) {
                            Text(
                                v15Text(
                                    "আরও নির্দিষ্ট নাম বা মোবাইল লিখুন।",
                                    "Type a more specific name or phone number."
                                ),
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodySmall
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = customerName,
                    onValueChange = {
                        customerName = it
                    },
                    label = {
                        Text(
                            v15Text(
                                "ক্রেতার নাম (ঐচ্ছিক)",
                                "Customer name (optional)"
                            )
                        )
                    },
                    singleLine = true,
                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = customerPhone,
                    onValueChange = {
                        customerPhone = it
                    },
                    label = {
                        Text(
                            v15Text(
                                "মোবাইল (ঐচ্ছিক)",
                                "Phone (optional)"
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

                error?.let {
                    Text(
                        it,
                        color =
                            MaterialTheme.colorScheme
                                .error
                    )
                }
        Button(
            modifier =
                Modifier.fillMaxWidth(),
            onClick = {
                    val lineInputs =
                        mutableListOf<RetailSaleLineInput>()

                    for (item in cart) {
                        val quantity =
                            item.quantity
                                .retailIntOrNull()

                        val price =
                            item.price
                                .retailDoubleOrNull()

                        val baseQuantity =
                            if (
                                quantity != null &&
                                quantity > 0 &&
                                item.unitFactor > 0
                            ) {
                                quantity.toLong() *
                                    item.unitFactor
                                        .toLong()
                            } else {
                                -1L
                            }

                        if (
                            quantity == null ||
                            quantity <= 0 ||
                            item.unitFactor <= 0 ||
                            baseQuantity <= 0L ||
                            baseQuantity >
                                item.availableBase
                                    .toLong() ||
                            baseQuantity >
                                Int.MAX_VALUE
                                    .toLong() ||
                            price == null ||
                            !price.isFinite() ||
                            price < 0.0
                        ) {
                            error =
                                v15Text(
                                    "পণ্যের পরিমাণ, ইউনিট, স্টক ও বিক্রয় মূল্য যাচাই করুন।",
                                    "Check item quantity, unit, available stock and selling price."
                                )
                            return@Button
                        }

                        lineInputs +=
                            RetailSaleLineInput(
                                productId =
                                    item.productId,
                                quantity =
                                    quantity,
                                unitPrice =
                                    price,
                                unitName =
                                    item.selectedUnit,
                                unitFactor =
                                    item.unitFactor
                            )
                    }

                    val cleanDiscount =
                        discount
                            .retailDoubleOrNull()

                    val cleanPaid =
                        paid
                            .retailDoubleOrNull()

                    when {
                        lineInputs.isEmpty() -> {
                            error =
                                v15Text(
                                    "কমপক্ষে একটি পণ্য যোগ করুন।",
                                    "Add at least one product."
                                )
                        }

                        cleanDiscount == null ||
                            cleanDiscount < 0.0 ||
                            cleanDiscount >
                                subtotal + 0.0001 -> {
                            error =
                                v15Text(
                                    "ছাড়ের পরিমাণ সঠিক নয়।",
                                    "Invalid discount amount."
                                )
                        }

                        cleanPaid == null ||
                            cleanPaid < 0.0 ||
                            cleanPaid >
                                total + 0.0001 -> {
                            error =
                                v15Text(
                                    "আদায়ের পরিমাণ মোট টাকার বেশি হতে পারবে না।",
                                    "Paid amount cannot exceed the total."
                                )
                        }

                        cleanPaid > 0.0001 &&
                            (
                                selectedFinancialAccountId ==
                                    null ||
                                financialAccounts.none {
                                    it.id ==
                                        selectedFinancialAccountId
                                }
                            ) -> {
                            error =
                                v15Text(
                                    "আদায় থাকলে আর্থিক হিসাব নির্বাচন করুন।",
                                    "Select a Financial Account for the payment."
                                )
                        }

                        (
                            total -
                                cleanPaid
                        ) > 0.0001 &&
                            (
                                selectedBakiPersonId ==
                                    null ||
                                    bakiPeople.none {
                                        it.id ==
                                            selectedBakiPersonId
                                    }
                            ) -> {
                            error =
                                v15Text(
                                    "বাকি বিক্রির জন্য বাকির খাতা নির্বাচন করুন।",
                                    "Select a Baki ledger for the due sale."
                                )
                        }

                        else -> {
                            error = null

                            onSave(
                                RetailSaleDraft(
                                    invoiceNo =
                                        invoiceNo.trim(),
                                    lines =
                                        lineInputs,
                                    discount =
                                        cleanDiscount,
                                    paid =
                                        cleanPaid,
                                    paymentMethod =
                                        if (
                                            cleanPaid >
                                            0.0001
                                        ) {
                                            paymentMethod
                                        } else {
                                            "UNPAID"
                                        },
                                    financialAccountId =
                                        selectedFinancialAccountId
                                            ?.takeIf {
                                                cleanPaid >
                                                    0.0001
                                            },
                                    bakiPersonId =
                                        selectedBakiPersonId
                                            ?.takeIf {
                                                (
                                                    total -
                                                        cleanPaid
                                                ) >
                                                    0.0001
                                            },
                                    customerName =
                                        customerName
                                            .trim(),
                                    customerPhone =
                                        customerPhone
                                            .trim(),
                                    note =
                                        note.trim()
                                )
                            )
                        }
                    }
            },
            enabled = !saving
        ) {
            Text(
                if (saving) {
                    v15Text(
                        "সংরক্ষণ হচ্ছে…",
                        "Saving…"
                    )
                } else {
                    v15Text(
                        "বিক্রি সংরক্ষণ",
                        "Save sale"
                    )
                }
            )
        }

        OutlinedButton(
            onClick = onDismiss,
            enabled = !saving,
            modifier =
                Modifier.fillMaxWidth()
        ) {
            Text(
                v15Text(
                    "বাতিল",
                    "Cancel"
                )
            )
        }
    }
}

@Composable
private fun RetailPaymentButton(
    code: String,
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier
) {
    if (selected) {
        Button(
            onClick = onSelect,
            modifier = modifier
        ) {
            Text(label)
        }
    } else {
        OutlinedButton(
            onClick = onSelect,
            modifier = modifier
        ) {
            Text(label)
        }
    }
}

private fun String.retailNormalizeNumber(): String =
    buildString(length) {
        this@retailNormalizeNumber
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

private fun String.retailDoubleOrNull(): Double? =
    retailNormalizeNumber()
        .toDoubleOrNull()
        ?.takeIf {
            it.isFinite()
        }

private fun String.retailIntOrNull(): Int? =
    retailNormalizeNumber()
        .toIntOrNull()

private fun retailMoney(
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

private fun retailDate(
    value: Long
): String =
    SimpleDateFormat(
        "dd MMM yyyy, hh:mm a",
        Locale.getDefault()
    ).format(
        Date(value)
    )

private fun retailStatusLabel(
    status: String
): String =
    when (status) {
        "CANCELLED" ->
            v15Text(
                "বাতিল",
                "Cancelled"
            )

        "PAID" ->
            v15Text(
                "পরিশোধ",
                "Paid"
            )

        "PARTIAL" ->
            v15Text(
                "আংশিক",
                "Partial"
            )

        else ->
            v15Text(
                "বাকি",
                "Due"
            )
    }

@Composable
private fun RetailFinancialAccountPicker(
    accounts:
        List<com.familykhata.app.data.FinancialAccountSummary>,
    selectedId: Long?,
    enabled: Boolean,
    onSelect: (Long) -> Unit
) {
    if (accounts.isEmpty()) {
        return
    }

    Column(
        verticalArrangement =
            Arrangement.spacedBy(6.dp)
    ) {
        accounts.forEach { account ->
            val detail =
                account.provider
                    .ifBlank {
                        account.type
                    }
                    .trim()

            val label =
                if (
                    detail.isBlank() ||
                    detail.equals(
                        account.name,
                        ignoreCase = true
                    )
                ) {
                    account.name
                } else {
                    "${account.name} • $detail"
                }

            OutlinedButton(
                onClick = {
                    onSelect(account.id)
                },
                enabled = enabled,
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Text(
                    if (selectedId == account.id) {
                        "✓ $label"
                    } else {
                        label
                    }
                )
            }
        }
    }
}

private fun retailPaymentLabel(
    method: String
): String =
    when (method.uppercase()) {
        "CASH" ->
            v15Text(
                "ক্যাশ",
                "Cash"
            )

        "BKASH" -> "bKash"
        "NAGAD" -> "Nagad"
        "ROCKET" -> "Rocket"

        "BANK" ->
            v15Text(
                "ব্যাংক",
                "Bank"
            )

        "CARD" ->
            v15Text(
                "কার্ড",
                "Card"
            )

        "WALLET" -> "Wallet"
        "MIXED" -> "Mixed"
        "UNPAID" ->
            v15Text(
                "বাকি",
                "Unpaid"
            )
        "OTHER" ->
            v15Text(
                "অন্যান্য",
                "Other"
            )

        else -> method
    }
