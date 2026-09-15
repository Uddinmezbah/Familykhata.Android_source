package com.familykhata.app.ui

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.familykhata.app.InventoryViewModel
import com.familykhata.app.RetailSaleLineInput
import com.familykhata.app.data.ProductStockSummary
import com.familykhata.app.data.RetailSaleEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class RetailCartLine(
    val productId: Long,
    val name: String,
    val unit: String,
    val available: Int,
    val quantity: String,
    val price: String
)

private data class RetailSaleDraft(
    val invoiceNo: String,
    val lines: List<RetailSaleLineInput>,
    val discount: Double,
    val paid: Double,
    val paymentMethod: String,
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
    val products by vm.products.collectAsState()
    val sales by vm.retailSales.collectAsState()
    val context = LocalContext.current

    var showNewSale by remember {
        mutableStateOf(false)
    }

    var saving by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(
        workspace,
        shopType
    ) {
        vm.setContext(
            workspaceValue = workspace,
            shopType = shopType
        )
    }

    BackHandler {
        onExit()
    }

    V15DeepScreenContainer(
        title = v15Text(
            "বিক্রি ও ইনভয়েস",
            "Sales & Invoices"
        ),
        onBack = onExit
    ) {
        val totalSales =
            sales.sumOf {
                it.total
            }

        val collected =
            sales.sumOf {
                it.paid
            }

        val due =
            sales.sumOf {
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
                        sale = sale
                    )
                }
            }
        }
    }

    if (showNewSale) {
        RetailSaleDialog(
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
                    return@RetailSaleDialog
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
    sale: RetailSaleEntity
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
        }
    }
}

@Composable
private fun RetailSaleDialog(
    products: List<ProductStockSummary>,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (RetailSaleDraft) -> Unit
) {
    var invoiceNo by remember {
        mutableStateOf("")
    }

    var customerName by remember {
        mutableStateOf("")
    }

    var customerPhone by remember {
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

    val due =
        (
            total -
                paidValue
                    .coerceAtLeast(0.0)
        ).coerceAtLeast(0.0)

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
                    Modifier
                        .heightIn(
                            max = 560.dp
                        )
                        .verticalScroll(
                            rememberScrollState()
                        ),
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
                                "পণ্য / বারকোড খুঁজুন",
                                "Search product / barcode"
                            )
                        )
                    },
                    singleLine = true,
                    modifier =
                        Modifier.fillMaxWidth()
                )

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
                                if (!alreadyAdded) {
                                    cart +=
                                        RetailCartLine(
                                            productId =
                                                product.id,
                                            name =
                                                product.name,
                                            unit =
                                                product.unit,
                                            available =
                                                product.totalStock,
                                            quantity =
                                                "1",
                                            price =
                                                retailMoney(
                                                    product
                                                        .sellingPrice
                                                )
                                        )
                                }
                            },
                            enabled =
                                !alreadyAdded &&
                                    !saving,
                            modifier =
                                Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (alreadyAdded) {
                                    v15Text(
                                        "✓ ${product.name}",
                                        "✓ ${product.name}"
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
                                    "স্টক: ${item.available} ${item.unit}",
                                    "Stock: ${item.available} ${item.unit}"
                                ),
                                style =
                                    MaterialTheme.typography
                                        .bodySmall
                            )

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
                                                "পরিমাণ",
                                                "Qty"
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
                                                "দর",
                                                "Price"
                                            )
                                        )
                                    },
                                    singleLine = true,
                                    modifier =
                                        Modifier.weight(1f)
                                )
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
                        "পেমেন্ট পদ্ধতি",
                        "Payment method"
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
                    RetailPaymentButton(
                        code = "CASH",
                        label =
                            v15Text(
                                "ক্যাশ",
                                "Cash"
                            ),
                        selected =
                            paymentMethod ==
                                "CASH",
                        onSelect = {
                            paymentMethod =
                                "CASH"
                        },
                        modifier =
                            Modifier.weight(1f)
                    )

                    RetailPaymentButton(
                        code = "BKASH",
                        label = "bKash",
                        selected =
                            paymentMethod ==
                                "BKASH",
                        onSelect = {
                            paymentMethod =
                                "BKASH"
                        },
                        modifier =
                            Modifier.weight(1f)
                    )

                    RetailPaymentButton(
                        code = "NAGAD",
                        label = "Nagad",
                        selected =
                            paymentMethod ==
                                "NAGAD",
                        onSelect = {
                            paymentMethod =
                                "NAGAD"
                        },
                        modifier =
                            Modifier.weight(1f)
                    )
                }

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(6.dp)
                ) {
                    RetailPaymentButton(
                        code = "ROCKET",
                        label = "Rocket",
                        selected =
                            paymentMethod ==
                                "ROCKET",
                        onSelect = {
                            paymentMethod =
                                "ROCKET"
                        },
                        modifier =
                            Modifier.weight(1f)
                    )

                    RetailPaymentButton(
                        code = "BANK",
                        label =
                            v15Text(
                                "ব্যাংক",
                                "Bank"
                            ),
                        selected =
                            paymentMethod ==
                                "BANK",
                        onSelect = {
                            paymentMethod =
                                "BANK"
                        },
                        modifier =
                            Modifier.weight(1f)
                    )

                    RetailPaymentButton(
                        code = "CARD",
                        label =
                            v15Text(
                                "কার্ড",
                                "Card"
                            ),
                        selected =
                            paymentMethod ==
                                "CARD",
                        onSelect = {
                            paymentMethod =
                                "CARD"
                        },
                        modifier =
                            Modifier.weight(1f)
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

                OutlinedButton(
                    onClick = {
                        paid =
                            retailMoney(total)
                    },
                    enabled =
                        total >= 0.0 &&
                            !saving,
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text(
                        v15Text(
                            "সম্পূর্ণ পরিশোধ",
                            "Mark fully paid"
                        )
                    )
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
            }
        },
        confirmButton = {
            Button(
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

                        if (
                            quantity == null ||
                            quantity <= 0 ||
                            quantity >
                                item.available ||
                            price == null ||
                            price < 0.0
                        ) {
                            error =
                                v15Text(
                                    "পণ্যের পরিমাণ, স্টক ও বিক্রয় মূল্য যাচাই করুন।",
                                    "Check item quantity, available stock and selling price."
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
                                    price
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
                                        paymentMethod,
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
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !saving
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

        else -> method
    }
