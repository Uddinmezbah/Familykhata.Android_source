package com.familykhata.app.ui

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.familykhata.app.InventoryViewModel
import com.familykhata.app.data.ProductStockSummary
import com.familykhata.app.data.StockBatchEntity
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val InventoryGreen = Color(0xFF0B7A53)
private val InventoryOrange = Color(0xFFB85C00)
private val InventoryRed = Color(0xFFC4473A)
private val InventoryBlue = Color(0xFF1565C0)

@Composable
internal fun V15InventoryScreen(
    workspace: String,
    canWrite: Boolean,
    onExit: () -> Unit
) {
    val vm: InventoryViewModel = viewModel()
    val products by vm.products.collectAsState()
    var selectedId by remember { mutableStateOf<Long?>(null) }
    val selected = selectedId?.let { id -> products.firstOrNull { it.id == id } }

    LaunchedEffect(workspace) { vm.setWorkspace(workspace) }
    BackHandler(enabled = selected != null) { selectedId = null }
    BackHandler(enabled = selected == null) { onExit() }

    if (selected == null) {
        ProductListScreen(
            products = products,
            workspace = workspace,
            canWrite = canWrite,
            viewModel = vm,
            onSelect = { selectedId = it.id }
        )
    } else {
        ProductDetailScreen(
            product = selected,
            canWrite = canWrite,
            viewModel = vm,
            onDeleted = { selectedId = null }
        )
    }
}

@Composable
private fun ProductListScreen(
    products: List<ProductStockSummary>,
    workspace: String,
    canWrite: Boolean,
    viewModel: InventoryViewModel,
    onSelect: (ProductStockSummary) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var showAdd by remember { mutableStateOf(false) }
    val now = System.currentTimeMillis()
    val nearLimit = now + 30L * 86_400_000L
    val lowCount = products.count { it.totalStock <= it.lowStockLevel }
    val expiringCount = products.count { it.nextExpiry != null && it.nextExpiry in now..nearLimit }
    val stockValue = products.sumOf { it.stockValue }
    val filtered = products.filter {
        query.isBlank() || it.name.contains(query, true) || it.category.contains(query, true) || it.sku.contains(query, true)
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(v15Text("পণ্য ও স্টক", "Products & Stock"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Text(v15Text("পণ্যের স্টক, কেনার তারিখ, ব্যাচ ও মেয়াদ এক জায়গায় রাখুন।", "Track stock, purchase batches and expiry dates in one place."), style = MaterialTheme.typography.bodySmall)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InventoryMetric(v15Text("পণ্য", "Products"), products.size.toString(), InventoryBlue, Modifier.weight(1f))
            InventoryMetric(v15Text("লো স্টক", "Low stock"), lowCount.toString(), InventoryOrange, Modifier.weight(1f))
            InventoryMetric(v15Text("মেয়াদ নিকট", "Expiring"), expiringCount.toString(), InventoryRed, Modifier.weight(1f))
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = InventoryGreen.copy(alpha = 0.09f),
            border = BorderStroke(1.dp, InventoryGreen.copy(alpha = 0.20f))
        ) {
            Text(
                v15Text("বর্তমান স্টক ভ্যালু: ${V14DisplayState.currencySymbol} ${v15Money(stockValue)}", "Current stock value: ${V14DisplayState.currencySymbol} ${v15Money(stockValue)}"),
                modifier = Modifier.padding(14.dp),
                fontWeight = FontWeight.Bold,
                color = InventoryGreen
            )
        }

        Button(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth(), enabled = canWrite) {
            Text(v15Text("＋ নতুন পণ্য যোগ করুন", "＋ Add product"))
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(v15Text("নাম / ক্যাটাগরি / বারকোড দিয়ে খুঁজুন", "Search name / category / barcode")) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (filtered.isEmpty()) {
            Text(v15Text("এখনও কোনো পণ্য নেই।", "No products yet."), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            filtered.forEach { item -> ProductCard(item, onSelect) }
        }
    }

    if (showAdd) {
        AddProductDialog(
            workspace = workspace,
            onDismiss = { showAdd = false },
            onSave = { name, category, sku, sell, low, note, qty, buy, purchaseDate, expiryDate ->
                viewModel.addProduct(name, category, sku, sell, low, note, workspace, qty, buy, purchaseDate, expiryDate)
                showAdd = false
            }
        )
    }
}

@Composable
private fun InventoryMetric(title: String, value: String, accent: Color, modifier: Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = accent.copy(alpha = 0.09f)) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.ExtraBold, color = accent, style = MaterialTheme.typography.titleLarge)
            Text(title, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ProductCard(item: ProductStockSummary, onSelect: (ProductStockSummary) -> Unit) {
    val now = System.currentTimeMillis()
    val accent = when {
        item.totalStock <= 0 -> InventoryRed
        item.totalStock <= item.lowStockLevel -> InventoryOrange
        item.nextExpiry != null && item.nextExpiry < now -> InventoryRed
        else -> InventoryGreen
    }
    Card(
        onClick = { onSelect(item) },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.07f)),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.18f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(item.name, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                Text(v15Text("স্টক ${item.totalStock}", "Stock ${item.totalStock}"), fontWeight = FontWeight.Bold, color = accent)
            }
            if (item.category.isNotBlank()) Text(item.category, style = MaterialTheme.typography.bodySmall)
            if (item.sku.isNotBlank()) Text(v15Text("বারকোড/SKU: ${item.sku}", "Barcode/SKU: ${item.sku}"), style = MaterialTheme.typography.bodySmall)
            item.nextExpiry?.let {
                Text(v15Text("নিকটতম মেয়াদ: ${v15Date(it)}", "Next expiry: ${v15Date(it)}"), style = MaterialTheme.typography.bodySmall, color = if (it < now) InventoryRed else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(v15Text("বিস্তারিত দেখতে চাপুন", "Tap for details"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProductDetailScreen(
    product: ProductStockSummary,
    canWrite: Boolean,
    viewModel: InventoryViewModel,
    onDeleted: () -> Unit
) {
    val context = LocalContext.current
    val batchesFlow = remember(product.id) { viewModel.observeBatches(product.id) }
    val batches by batchesFlow.collectAsState(initial = emptyList())
    var showAddBatch by remember { mutableStateOf(false) }
    var showReduce by remember { mutableStateOf(false) }
    var showEdit by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(product.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Text(v15Text("ফোনের Back ব্যবহার করে পণ্য তালিকায় ফিরুন", "Use your phone Back button to return to the product list"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = InventoryGreen.copy(alpha = 0.09f)) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(v15Text("বর্তমান স্টক: ${product.totalStock}", "Current stock: ${product.totalStock}"), fontWeight = FontWeight.ExtraBold)
                Text(v15Text("স্টক ভ্যালু: ${V14DisplayState.currencySymbol} ${v15Money(product.stockValue)}", "Stock value: ${V14DisplayState.currencySymbol} ${v15Money(product.stockValue)}"))
                Text(v15Text("বিক্রয় মূল্য: ${V14DisplayState.currencySymbol} ${v15Money(product.sellingPrice)}", "Selling price: ${V14DisplayState.currencySymbol} ${v15Money(product.sellingPrice)}"))
                if (product.lowStockLevel > 0) Text(v15Text("লো-স্টক সীমা: ${product.lowStockLevel}", "Low-stock threshold: ${product.lowStockLevel}"))
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { showAddBatch = true }, enabled = canWrite, modifier = Modifier.weight(1f)) { Text(v15Text("＋ স্টক", "+ Stock")) }
            OutlinedButton(onClick = { showReduce = true }, enabled = canWrite && product.totalStock > 0, modifier = Modifier.weight(1f)) { Text(v15Text("− স্টক", "- Stock")) }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showEdit = true }, enabled = canWrite, modifier = Modifier.weight(1f)) { Text(v15Text("Edit", "Edit")) }
            OutlinedButton(onClick = { showDelete = true }, enabled = canWrite, modifier = Modifier.weight(1f)) { Text(v15Text("Delete", "Delete")) }
        }

        Text(v15Text("স্টক ব্যাচ (${batches.size})", "Stock batches (${batches.size})"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (batches.isEmpty()) Text(v15Text("কোনো স্টক ব্যাচ নেই।", "No stock batches."))
        batches.filter { it.quantity > 0 }.forEach { BatchCard(it) }
    }

    if (showAddBatch) {
        AddBatchDialog(onDismiss = { showAddBatch = false }) { qty, buy, purchaseDate, expiryDate ->
            viewModel.addBatch(product.id, qty, buy, purchaseDate, expiryDate)
            showAddBatch = false
        }
    }
    if (showReduce) {
        ReduceStockDialog(max = product.totalStock, onDismiss = { showReduce = false }) { qty ->
            viewModel.reduceStock(product.id, qty) { ok ->
                Toast.makeText(context, if (ok) v15Text("স্টক আপডেট হয়েছে", "Stock updated") else v15Text("স্টক কমানো যায়নি", "Could not reduce stock"), Toast.LENGTH_SHORT).show()
            }
            showReduce = false
        }
    }
    if (showEdit) {
        EditProductDialog(product, onDismiss = { showEdit = false }) { name, category, sku, sell, low, note ->
            viewModel.updateProduct(product, name, category, sku, sell, low, note)
            showEdit = false
        }
    }
    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text(v15Text("পণ্য ডিলিট করবেন?", "Delete product?")) },
            text = { Text(v15Text("পণ্যের সব stock batch-ও মুছে যাবে।", "All stock batches for this product will also be deleted.")) },
            confirmButton = { TextButton(onClick = { viewModel.deleteProduct(product.id); showDelete = false; onDeleted() }) { Text(v15Text("ডিলিট", "Delete")) } },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text(v15Text("বাতিল", "Cancel")) } }
        )
    }
}

@Composable
private fun BatchCard(item: StockBatchEntity) {
    val now = System.currentTimeMillis()
    val expiryColor = if (item.expiryDate != null && item.expiryDate < now) InventoryRed else MaterialTheme.colorScheme.onSurfaceVariant
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(v15Text("পরিমাণ: ${item.quantity}", "Quantity: ${item.quantity}"), fontWeight = FontWeight.Bold)
            Text(v15Text("কেনা: ${v15Date(item.purchaseDate)} • ${V14DisplayState.currencySymbol} ${v15Money(item.purchasePrice)}", "Purchased: ${v15Date(item.purchaseDate)} • ${V14DisplayState.currencySymbol} ${v15Money(item.purchasePrice)}"), style = MaterialTheme.typography.bodySmall)
            item.expiryDate?.let { Text(v15Text("মেয়াদ: ${v15Date(it)}", "Expiry: ${v15Date(it)}"), style = MaterialTheme.typography.bodySmall, color = expiryColor) }
        }
    }
}

@Composable
private fun AddProductDialog(
    workspace: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Double, Int, String, Int, Double, Long, Long?) -> Unit
) {
    val context = LocalContext.current
    val scanner = remember { GmsBarcodeScanning.getClient(context) }
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("") }
    var sell by remember { mutableStateOf("") }
    var low by remember { mutableStateOf("5") }
    var note by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("") }
    var buy by remember { mutableStateOf("") }
    var purchaseDate by remember { mutableStateOf(startOfDay(System.currentTimeMillis())) }
    var expiryDate by remember { mutableStateOf<Long?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(v15Text("নতুন পণ্য", "New product")) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text(v15Text("পণ্যের নাম", "Product name")) }, singleLine = true)
                OutlinedTextField(category, { category = it }, label = { Text(v15Text("ক্যাটাগরি", "Category")) }, singleLine = true)
                OutlinedTextField(sku, { sku = it }, label = { Text("Barcode / SKU") }, singleLine = true)
                OutlinedButton(onClick = {
                    scanner.startScan().addOnSuccessListener { code -> sku = code.rawValue.orEmpty() }
                        .addOnFailureListener { Toast.makeText(context, v15Text("স্ক্যান করা যায়নি", "Scan failed"), Toast.LENGTH_SHORT).show() }
                }, modifier = Modifier.fillMaxWidth()) { Text(v15Text("▣ বারকোড স্ক্যান", "▣ Scan barcode")) }
                OutlinedTextField(sell, { sell = it }, label = { Text(v15Text("বিক্রয় মূল্য", "Selling price")) }, singleLine = true)
                OutlinedTextField(low, { low = it }, label = { Text(v15Text("লো-স্টক সীমা", "Low-stock threshold")) }, singleLine = true)
                OutlinedTextField(qty, { qty = it }, label = { Text(v15Text("প্রাথমিক পরিমাণ", "Initial quantity")) }, singleLine = true)
                OutlinedTextField(buy, { buy = it }, label = { Text(v15Text("ক্রয় মূল্য / ইউনিট", "Purchase price / unit")) }, singleLine = true)
                DateButton(v15Text("কেনার তারিখ", "Purchase date"), purchaseDate) { purchaseDate = it }
                NullableDateButton(v15Text("মেয়াদ শেষের তারিখ", "Expiry date"), expiryDate) { expiryDate = it }
                OutlinedTextField(note, { note = it }, label = { Text(v15Text("নোট", "Note")) })
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val selling = sell.toDoubleOrNull() ?: 0.0
                val lowValue = low.toIntOrNull() ?: 0
                val quantity = qty.toIntOrNull() ?: 0
                val purchase = buy.toDoubleOrNull() ?: 0.0
                if (name.isBlank()) error = v15Text("পণ্যের নাম লিখুন", "Enter a product name")
                else if (quantity < 0 || selling < 0 || purchase < 0 || lowValue < 0) error = v15Text("সংখ্যাগুলো সঠিক নয়", "Check the numeric values")
                else onSave(name, category, sku, selling, lowValue, note, quantity, purchase, purchaseDate, expiryDate)
            }) { Text(v15Text("সেভ", "Save")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(v15Text("বাতিল", "Cancel")) } }
    )
}

@Composable
private fun AddBatchDialog(onDismiss: () -> Unit, onSave: (Int, Double, Long, Long?) -> Unit) {
    var qty by remember { mutableStateOf("") }
    var buy by remember { mutableStateOf("") }
    var purchaseDate by remember { mutableStateOf(startOfDay(System.currentTimeMillis())) }
    var expiryDate by remember { mutableStateOf<Long?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(v15Text("স্টক ব্যাচ যোগ করুন", "Add stock batch")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(qty, { qty = it }, label = { Text(v15Text("পরিমাণ", "Quantity")) }, singleLine = true)
                OutlinedTextField(buy, { buy = it }, label = { Text(v15Text("ক্রয় মূল্য / ইউনিট", "Purchase price / unit")) }, singleLine = true)
                DateButton(v15Text("কেনার তারিখ", "Purchase date"), purchaseDate) { purchaseDate = it }
                NullableDateButton(v15Text("মেয়াদ শেষ", "Expiry date"), expiryDate) { expiryDate = it }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(onClick = {
            val q = qty.toIntOrNull()
            val p = buy.toDoubleOrNull() ?: 0.0
            if (q == null || q <= 0 || p < 0) error = v15Text("সঠিক তথ্য দিন", "Enter valid values") else onSave(q, p, purchaseDate, expiryDate)
        }) { Text(v15Text("যোগ করুন", "Add")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(v15Text("বাতিল", "Cancel")) } }
    )
}

@Composable
private fun ReduceStockDialog(max: Int, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    var qty by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(v15Text("স্টক কমান", "Reduce stock")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(v15Text("বর্তমান স্টক: $max", "Current stock: $max"))
                OutlinedTextField(qty, { qty = it }, label = { Text(v15Text("কতটি কমাবেন", "Quantity to remove")) }, singleLine = true)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(onClick = {
            val q = qty.toIntOrNull()
            if (q == null || q <= 0 || q > max) error = v15Text("সঠিক পরিমাণ দিন", "Enter a valid quantity") else onSave(q)
        }) { Text(v15Text("আপডেট", "Update")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(v15Text("বাতিল", "Cancel")) } }
    )
}

@Composable
private fun EditProductDialog(product: ProductStockSummary, onDismiss: () -> Unit, onSave: (String, String, String, Double, Int, String) -> Unit) {
    var name by remember { mutableStateOf(product.name) }
    var category by remember { mutableStateOf(product.category) }
    var sku by remember { mutableStateOf(product.sku) }
    var sell by remember { mutableStateOf(v15Money(product.sellingPrice)) }
    var low by remember { mutableStateOf(product.lowStockLevel.toString()) }
    var note by remember { mutableStateOf(product.note) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(v15Text("পণ্য সম্পাদনা", "Edit product")) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text(v15Text("পণ্যের নাম", "Product name")) })
                OutlinedTextField(category, { category = it }, label = { Text(v15Text("ক্যাটাগরি", "Category")) })
                OutlinedTextField(sku, { sku = it }, label = { Text("Barcode / SKU") })
                OutlinedTextField(sell, { sell = it }, label = { Text(v15Text("বিক্রয় মূল্য", "Selling price")) })
                OutlinedTextField(low, { low = it }, label = { Text(v15Text("লো-স্টক সীমা", "Low-stock threshold")) })
                OutlinedTextField(note, { note = it }, label = { Text(v15Text("নোট", "Note")) })
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name, category, sku, sell.toDoubleOrNull() ?: 0.0, low.toIntOrNull() ?: 0, note) }) { Text(v15Text("সেভ", "Save")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(v15Text("বাতিল", "Cancel")) } }
    )
}

@Composable
private fun DateButton(label: String, value: Long, onChange: (Long) -> Unit) {
    val context = LocalContext.current
    OutlinedButton(onClick = {
        val c = Calendar.getInstance().apply { timeInMillis = value }
        DatePickerDialog(context, { _, y, m, d -> onChange(Calendar.getInstance().apply { clear(); set(y, m, d, 0, 0, 0) }.timeInMillis) }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }, modifier = Modifier.fillMaxWidth()) { Text("$label: ${v15Date(value)}") }
}

@Composable
private fun NullableDateButton(label: String, value: Long?, onChange: (Long?) -> Unit) {
    val context = LocalContext.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedButton(onClick = {
            val c = Calendar.getInstance().apply { if (value != null) timeInMillis = value }
            DatePickerDialog(context, { _, y, m, d -> onChange(Calendar.getInstance().apply { clear(); set(y, m, d, 0, 0, 0) }.timeInMillis) }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }, modifier = Modifier.weight(1f)) { Text(if (value == null) label else "$label: ${v15Date(value)}") }
        if (value != null) TextButton(onClick = { onChange(null) }) { Text("×") }
    }
}

private fun startOfDay(value: Long): Long = Calendar.getInstance().apply {
    timeInMillis = value
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun v15Date(value: Long): String = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(value))
private fun v15Money(value: Double): String = if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.US, "%.2f", value)
