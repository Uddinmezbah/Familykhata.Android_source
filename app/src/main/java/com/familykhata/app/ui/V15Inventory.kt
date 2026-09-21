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
import androidx.compose.foundation.layout.heightIn
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
import com.familykhata.app.FamilyKhataViewModel
import com.familykhata.app.InventoryViewModel
import com.familykhata.app.ProductUnitInput
import com.familykhata.app.data.ProductUnitConversionEntity
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

private fun String.v15InventoryNormalizeNumber(): String =
    buildString(length) {
        this@v15InventoryNormalizeNumber.forEach { ch ->
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

private fun String.v15InventoryDoubleOrNull(): Double? =
    v15InventoryNormalizeNumber().toDoubleOrNull()

private fun String.v15InventoryIntOrNull(): Int? =
    v15InventoryNormalizeNumber().toIntOrNull()


private enum class ProductFormMode {
    PHARMACY,
    ELECTRONICS,
    FASHION,
    EXPIRY_RETAIL,
    GENERAL
}

private data class ProductDetailsInput(
    val name: String,
    val category: String,
    val sku: String,
    val unit: String,
    val unitConversions:
        List<ProductUnitInput>,
    val brand: String,
    val genericName: String,
    val modelName: String,
    val serialOrImei: String,
    val size: String,
    val color: String,
    val warrantyMonths: Int,
    val sellingPrice: Double,
    val mrp: Double,
    val rackLocation: String,
    val lowStockLevel: Int,
    val note: String
)

private data class NewProductInput(
    val product: ProductDetailsInput,
    val initialQuantity: Int,
    val purchasePrice: Double,
    val purchaseDate: Long,
    val expiryDate: Long?,
    val batchNo: String,
    val stockUnitName: String,
    val stockUnitFactor: Int
)

private data class BatchInput(
    val quantity: Int,
    val purchasePrice: Double,
    val purchaseDate: Long,
    val expiryDate: Long?,
    val batchNo: String,
    val unitName: String,
    val unitFactor: Int
)

private data class StockQuantityInput(
    val quantity: Int,
    val unitName: String,
    val unitFactor: Int
)

private data class InventoryStockUnitOption(
    val name: String,
    val factor: Int
)

private data class EditableBatchInput(
    val id: Long,
    val quantity: String,
    val purchasePrice: String,
    val purchaseDate: Long,
    val expiryDate: Long?,
    val batchNo: String
)

private data class BatchUpdateInput(
    val id: Long,
    val quantity: Int,
    val purchasePrice: Double,
    val purchaseDate: Long,
    val expiryDate: Long?,
    val batchNo: String
)


private data class ProductUnitDraft(
    val unitName: String = "",
    val multiplier: String = "",
    val referenceUnit: String = ""
)

private fun buildProductUnitInputs(
    baseUnit: String,
    drafts: List<ProductUnitDraft>
): List<ProductUnitInput>? {
    val cleanBase =
        baseUnit.trim()
            .ifBlank { "pcs" }

    val baseKey =
        cleanBase.lowercase(
            Locale.ROOT
        )

    val factorByUnit =
        linkedMapOf<String, Long>(
            baseKey to 1L
        )

    val seen =
        mutableSetOf(baseKey)

    val result =
        mutableListOf<ProductUnitInput>()

    drafts.forEachIndexed { index, draft ->
        val unitName =
            draft.unitName.trim()

        val unitKey =
            unitName.lowercase(
                Locale.ROOT
            )

        val multiplier =
            draft.multiplier
                .v15InventoryIntOrNull()
                ?: return null

        val referenceKey =
            draft.referenceUnit
                .trim()
                .ifBlank { cleanBase }
                .lowercase(
                    Locale.ROOT
                )

        if (
            unitName.isBlank() ||
            unitKey in seen ||
            multiplier <= 0
        ) {
            return null
        }

        val referenceFactor =
            factorByUnit[referenceKey]
                ?: return null

        val baseQuantity =
            referenceFactor *
                multiplier.toLong()

        if (
            baseQuantity <= 1L ||
            baseQuantity >
                Int.MAX_VALUE
        ) {
            return null
        }

        result +=
            ProductUnitInput(
                unitName =
                    unitName,
                baseQuantity =
                    baseQuantity.toInt(),
                sortOrder =
                    index + 1
            )

        seen += unitKey
        factorByUnit[unitKey] =
            baseQuantity
    }

    return result
}

private fun storedUnitDrafts(
    baseUnit: String,
    units:
        List<ProductUnitConversionEntity>
): List<ProductUnitDraft> {
    val cleanBase =
        baseUnit.trim()
            .ifBlank { "pcs" }

    return units
        .sortedBy {
            it.sortOrder
        }
        .map { item ->
            ProductUnitDraft(
                unitName =
                    item.unitName,
                multiplier =
                    item.baseQuantity
                        .toString(),
                referenceUnit =
                    cleanBase
            )
        }
}

@Composable
private fun ProductUnitConversionEditor(
    baseUnit: String,
    drafts: List<ProductUnitDraft>,
    onChange:
        (List<ProductUnitDraft>) -> Unit
) {
    val cleanBase =
        baseUnit.trim()
            .ifBlank { "pcs" }

    Column(
        verticalArrangement =
            Arrangement.spacedBy(7.dp)
    ) {
        Text(
            v15Text(
                "বেস ইউনিট: $cleanBase — সব স্টক ভেতরে এই ইউনিটে হিসাব হবে",
                "Base unit: $cleanBase — stock is stored internally in this unit"
            ),
            style =
                MaterialTheme
                    .typography
                    .bodySmall,
            fontWeight =
                FontWeight.SemiBold
        )

        drafts.forEachIndexed { index, draft ->
            val availableReferences =
                buildList<String> {
                    add(cleanBase)

                    drafts
                        .take(index)
                        .map {
                            it.unitName.trim()
                        }
                        .filter {
                            it.isNotBlank()
                        }
                        .forEach { name ->
                            if (
                                none {
                                    it.equals(
                                        name,
                                        ignoreCase = true
                                    )
                                }
                            ) {
                                add(name)
                            }
                        }
                }

            val currentReference =
                availableReferences
                    .firstOrNull {
                        it.equals(
                            draft.referenceUnit,
                            ignoreCase = true
                        )
                    }
                    ?: cleanBase

            Card(
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier =
                        Modifier.padding(10.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(
                            6.dp
                        )
                ) {
                    Text(
                        v15Text(
                            "প্যাক ইউনিট ${index + 1}",
                            "Pack unit ${index + 1}"
                        ),
                        fontWeight =
                            FontWeight.Bold
                    )

                    OutlinedTextField(
                        value =
                            draft.unitName,
                        onValueChange = { value ->
                            val updated =
                                drafts
                                    .toMutableList()

                            val oldName =
                                draft.unitName
                                    .trim()

                            updated[index] =
                                draft.copy(
                                    unitName =
                                        value
                                )

                            if (
                                oldName.isNotBlank()
                            ) {
                                for (
                                    row in
                                    index + 1 until
                                        updated.size
                                ) {
                                    val later =
                                        updated[row]

                                    if (
                                        later.referenceUnit
                                            .equals(
                                                oldName,
                                                ignoreCase = true
                                            )
                                    ) {
                                        updated[row] =
                                            later.copy(
                                                referenceUnit =
                                                    value.trim()
                                            )
                                    }
                                }
                            }

                            onChange(updated)
                        },
                        label = {
                            Text(
                                v15Text(
                                    "ইউনিটের নাম — যেমন পাতা, Box, Carton",
                                    "Unit name — e.g. Strip, Box, Carton"
                                )
                            )
                        },
                        singleLine = true,
                        modifier =
                            Modifier.fillMaxWidth()
                    )

                    Text(
                        v15Text(
                            "রেফারেন্স ইউনিট নির্বাচন করুন",
                            "Select reference unit"
                        ),
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,
                        fontWeight =
                            FontWeight.SemiBold
                    )

                    Column {
                        availableReferences
                            .forEach { reference ->
                                TextButton(
                                    onClick = {
                                        val updated =
                                            drafts
                                                .toMutableList()

                                        updated[index] =
                                            draft.copy(
                                                referenceUnit =
                                                    reference
                                            )

                                        onChange(
                                            updated
                                        )
                                    }
                                ) {
                                    Text(
                                        if (
                                            reference.equals(
                                                currentReference,
                                                ignoreCase = true
                                            )
                                        ) {
                                            "✓ $reference"
                                        } else {
                                            reference
                                        }
                                    )
                                }
                            }
                    }

                    OutlinedTextField(
                        value =
                            draft.multiplier,
                        onValueChange = { value ->
                            val updated =
                                drafts
                                    .toMutableList()

                            updated[index] =
                                draft.copy(
                                    multiplier =
                                        value,
                                    referenceUnit =
                                        currentReference
                                )

                            onChange(updated)
                        },
                        label = {
                            Text(
                                v15Text(
                                    "১ ${draft.unitName.ifBlank { "ইউনিট" }} = কত $currentReference",
                                    "1 ${draft.unitName.ifBlank { "unit" }} = how many $currentReference"
                                )
                            )
                        },
                        singleLine = true,
                        modifier =
                            Modifier.fillMaxWidth()
                    )

                    if (
                        draft.unitName
                            .isNotBlank() &&
                        draft.multiplier
                            .isNotBlank()
                    ) {
                        Text(
                            v15Text(
                                "১ ${draft.unitName} = ${draft.multiplier} $currentReference",
                                "1 ${draft.unitName} = ${draft.multiplier} $currentReference"
                            ),
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall
                        )
                    }

                    TextButton(
                        onClick = {
                            val removedName =
                                draft.unitName
                                    .trim()

                            val remaining =
                                drafts
                                    .filterIndexed {
                                            row,
                                            _ ->
                                        row != index
                                    }
                                    .map { item ->
                                        if (
                                            removedName
                                                .isNotBlank() &&
                                            item.referenceUnit
                                                .equals(
                                                    removedName,
                                                    ignoreCase = true
                                                )
                                        ) {
                                            item.copy(
                                                referenceUnit =
                                                    cleanBase
                                            )
                                        } else {
                                            item
                                        }
                                    }

                            onChange(
                                remaining
                            )
                        }
                    ) {
                        Text(
                            v15Text(
                                "এই ইউনিট সরান",
                                "Remove this unit"
                            )
                        )
                    }
                }
            }
        }

        OutlinedButton(
            onClick = {
                if (
                    drafts.size < 6
                ) {
                    onChange(
                        drafts +
                            ProductUnitDraft(
                                referenceUnit =
                                    cleanBase
                            )
                    )
                }
            },
            enabled =
                drafts.size < 6,
            modifier =
                Modifier.fillMaxWidth()
        ) {
            Text(
                v15Text(
                    "＋ আরেকটি প্যাক ইউনিট যোগ করুন",
                    "+ Add another pack unit"
                )
            )
        }

        if (
            drafts.isNotEmpty()
        ) {
            Text(
                v15Text(
                    "উদাহরণ: Base=Piece, ১ Pata=১২ Piece। Box চাইলে ২০ Piece সরাসরি দিতে পারবে, অথবা ২০ Pata নির্বাচন করতে পারবে।",
                    "Example: Base=Piece, 1 Strip=12 Pieces. A Box may be 20 Pieces directly, or 20 Strips by selecting Strip as reference."
                ),
                style =
                    MaterialTheme
                        .typography
                        .bodySmall
            )
        }
    }
}

@Composable
private fun InventoryStockUnitSelector(
    options: List<InventoryStockUnitOption>,
    selectedName: String,
    label: String,
    enabled: Boolean = true,
    onSelect: (InventoryStockUnitOption) -> Unit
) {
    if (options.isEmpty()) return

    Text(
        label,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold
    )

    Column {
        options.forEach { option ->
            val selected =
                option.name.equals(
                    selectedName,
                    ignoreCase = true
                )

            TextButton(
                onClick = {
                    onSelect(option)
                },
                enabled = enabled
            ) {
                val relation =
                    if (option.factor == 1) {
                        option.name
                    } else {
                        "${option.name} • 1 = ${option.factor} ${options.first().name}"
                    }

                Text(
                    if (selected) {
                        "✓ $relation"
                    } else {
                        relation
                    }
                )
            }
        }
    }
}

private fun productFormMode(shopType: String): ProductFormMode {
    val value = shopType.lowercase(Locale.getDefault())

    return when {
        listOf("pharmacy", "medicine", "ফার্মেসি", "ঔষধ", "ওষুধ")
            .any { value.contains(it) } -> ProductFormMode.PHARMACY

        listOf("electronics", "mobile", "gadget", "ইলেকট্রনিক", "মোবাইল")
            .any { value.contains(it) } -> ProductFormMode.ELECTRONICS

        listOf("fashion", "clothing", "shoe", "জুতা", "পোশাক", "ফ্যাশন", "bag", "jewellery", "jewelry")
            .any { value.contains(it) } -> ProductFormMode.FASHION

        listOf("grocery", "food", "bakery", "cosmetic", "মুদি", "খাদ্য", "কসমেটিক", "restaurant", "agro")
            .any { value.contains(it) } -> ProductFormMode.EXPIRY_RETAIL

        else -> ProductFormMode.GENERAL
    }
}

private fun productNameLabel(mode: ProductFormMode): String = when (mode) {
    ProductFormMode.PHARMACY -> v15Text("ওষুধের নাম", "Medicine name")
    ProductFormMode.ELECTRONICS -> v15Text("পণ্যের নাম", "Product name")
    ProductFormMode.FASHION -> v15Text("পণ্যের নাম", "Product name")
    ProductFormMode.EXPIRY_RETAIL -> v15Text("পণ্যের নাম", "Product name")
    ProductFormMode.GENERAL -> v15Text("পণ্যের নাম", "Product name")
}


@Composable
internal fun V15InventoryScreen(
    workspace: String,
    businessId: String,
    shopType: String,
    canWrite: Boolean,
    nestedEntry: Boolean = false,
    onExit: () -> Unit
) {
    val vm: InventoryViewModel = viewModel()
    val securityViewModel: FamilyKhataViewModel = viewModel()
    val context = LocalContext.current
    val products by vm.products.collectAsState()
    var selectedId by remember {
        mutableStateOf<Long?>(null)
    }

    var showRetailSales by remember {
        mutableStateOf(false)
    }

    var showPurchases by remember {
        mutableStateOf(false)
    }

    var showAddProduct by remember {
        mutableStateOf(false)
    }

    var showAddStock by remember {
        mutableStateOf(false)
    }

    val selected =
        selectedId?.let { id ->
            products.firstOrNull {
                it.id == id
            }
        }

    LaunchedEffect(
        workspace,
        shopType,
        businessId
    ) {
        vm.setContext(
            workspaceValue = workspace,
            shopType = shopType,
            businessIdValue = businessId
        )
    }
    TrackV15DeepScreen(
        owner = "inventory-product",
        active =
            selected != null ||
                showRetailSales ||
                showPurchases ||
                showAddProduct ||
                showAddStock ||
                nestedEntry
    )

    BackHandler(
        enabled = showAddProduct
    ) {
        showAddProduct = false
    }

    BackHandler(
        enabled =
            !showAddProduct &&
                showAddStock
    ) {
        showAddStock = false
    }

    BackHandler(
        enabled =
            !showAddProduct &&
                !showAddStock &&
                showRetailSales
    ) {
        showRetailSales = false
    }
    BackHandler(
        enabled =
            !showAddProduct &&
                !showAddStock &&
                !showRetailSales &&
                showPurchases
    ) {
        showPurchases = false
    }

    BackHandler(
        enabled =
            !showAddProduct &&
                !showAddStock &&
                !showRetailSales &&
                selected != null
    ) {
        selectedId = null
    }

    BackHandler(
        enabled =
            !showAddProduct &&
                !showAddStock &&
                !showRetailSales &&
                selected == null
    ) {
        onExit()
    }

    if (showAddProduct) {
        V15DeepScreenContainer(
            title =
                v15Text(
                    "নতুন পণ্য",
                    "New product"
                ),
            onBack = {
                showAddProduct = false
            }
        ) {
            AddProductForm(
                workspace = workspace,
                onDismiss = {
                    showAddProduct = false
                },
                onSave = { input ->
                    val p =
                        input.product

                    val cleanSku =
                        p.sku.trim()

                    val duplicateSku =
                        cleanSku.isNotBlank() &&
                            products.any {
                                it.sku.trim()
                                    .equals(
                                        cleanSku,
                                        ignoreCase = true
                                    )
                            }

                    if (duplicateSku) {
                        Toast.makeText(
                            context,
                            v15Text(
                                "এই Barcode / SKU ইতিমধ্যে এই দোকান/ক্যাটাগরিতে আছে।",
                                "This Barcode / SKU already exists in this shop/category."
                            ),
                            Toast.LENGTH_LONG
                        ).show()

                        return@AddProductForm
                    }

                    vm.addProduct(
                        name = p.name,
                        category = p.category,
                        sku = p.sku,
                        unit = p.unit,
                        unitConversions =
                            p.unitConversions,
                        brand = p.brand,
                        genericName =
                            p.genericName,
                        modelName =
                            p.modelName,
                        serialOrImei =
                            p.serialOrImei,
                        size = p.size,
                        color = p.color,
                        warrantyMonths =
                            p.warrantyMonths,
                        sellingPrice =
                            p.sellingPrice,
                        mrp = p.mrp,
                        rackLocation =
                            p.rackLocation,
                        lowStockLevel =
                            p.lowStockLevel,
                        note = p.note,
                        workspace =
                            workspace,
                        initialQuantity =
                            input.initialQuantity,
                        purchasePrice =
                            input.purchasePrice,
                        purchaseDate =
                            input.purchaseDate,
                        expiryDate =
                            input.expiryDate,
                        batchNo =
                            input.batchNo,
                        initialUnitName =
                            input.stockUnitName,
                        initialUnitFactor =
                            input.stockUnitFactor
                    )

                    showAddProduct =
                        false
                }
            )
        }
    } else if (
        showAddStock &&
        selected != null
    ) {
        V15DeepScreenContainer(
            title =
                v15Text(
                    "স্টক যোগ করুন",
                    "Add stock"
                ),
            onBack = {
                showAddStock = false
            }
        ) {
            AddStockPage(
                product = selected,
                viewModel = vm,
                onDismiss = {
                    showAddStock = false
                }
            )
        }
    } else if (showRetailSales) {
        V16RetailSalesScreen(
            workspace = workspace,
            shopType = shopType,
            canWrite = canWrite,
            onExit = {
                showRetailSales = false
            }
        )
    } else if (showPurchases) {
        V17PurchaseScreen(
            workspace = workspace,
            businessId = businessId,
            shopType = shopType,
            canWrite = canWrite,
            onExit = {
                showPurchases = false
            }
        )
    } else if (selected == null) {
        if (nestedEntry) {
            V15DeepScreenContainer(
                title = v15Text(
                    "পণ্য ও স্টক",
                    "Products & Stock"
                ),
                onBack = onExit
            ) {
                ProductListScreen(
                    products = products,
                    workspace = workspace,
                    canWrite = canWrite,
                    viewModel = vm,
                    securityViewModel = securityViewModel,
                    onAddProduct = {
                        showAddProduct = true
                    },
                    onOpenSales = {
                        showRetailSales = true
                    },
                    onOpenPurchases = {
                        showPurchases = true
                    },
                    onSelect = {
                        selectedId = it.id
                    }
                )
            }
        } else {
            ProductListScreen(
                products = products,
                workspace = workspace,
                canWrite = canWrite,
                viewModel = vm,
                securityViewModel = securityViewModel,
                onAddProduct = {
                    showAddProduct = true
                },
                onOpenSales = {
                    showRetailSales = true
                },
                onOpenPurchases = {
                    showPurchases = true
                },
                onSelect = {
                    selectedId = it.id
                }
            )
        }
    } else {
        V15DeepScreenContainer(
            title = selected.name,
            onBack = {
                selectedId = null
            }
        ) {
            ProductDetailScreen(
                product = selected,
                canWrite = canWrite,
                viewModel = vm,
                securityViewModel = securityViewModel,
                onAddStock = {
                    showAddStock = true
                },
                onDeleted = {
                    selectedId = null
                }
            )
        }
    }
}

@Composable
private fun ProductListScreen(
    products: List<ProductStockSummary>,
    workspace: String,
    canWrite: Boolean,
    viewModel: InventoryViewModel,
    securityViewModel: FamilyKhataViewModel,
    onAddProduct: () -> Unit,
    onOpenSales: () -> Unit,
    onOpenPurchases: () -> Unit,
    onSelect: (ProductStockSummary) -> Unit
) {
    val context = LocalContext.current

    val prefs = remember {
        context.getSharedPreferences(
            "hisabi_khata_v14_settings",
            android.content.Context.MODE_PRIVATE
        )
    }

    val shopType = remember {
        prefs.getString(
            "business_type",
            ""
        ) ?: ""
    }

    val isPharmacy =
        remember(shopType) {
            productFormMode(shopType) ==
                ProductFormMode.PHARMACY
        }

    var query by remember {
        mutableStateOf("")
    }

    var stockFilter by remember {
        mutableStateOf("ALL")
    }

    var editingProduct by remember {
        mutableStateOf<ProductStockSummary?>(null)
    }

    var deletingProduct by remember {
        mutableStateOf<ProductStockSummary?>(null)
    }
    val now = System.currentTimeMillis()
    val nearLimit = now + 30L * 86_400_000L
    val lowCount = products.count { it.totalStock <= it.lowStockLevel }
    val expiringCount = products.count { it.nextExpiry != null && it.nextExpiry in now..nearLimit }
    val expiredCount = products.count { it.nextExpiry != null && it.nextExpiry < now }
    val stockValue = products.sumOf { it.stockValue }
    val saleValue = products.sumOf { it.saleValue }
    val potentialProfit = products.sumOf { it.potentialProfit }
    val filtered =
        products.filter { item ->
            val matchesQuery =
                query.isBlank() ||
                    item.name.contains(
                        query,
                        true
                    ) ||
                    item.category.contains(
                        query,
                        true
                    ) ||
                    item.sku.contains(
                        query,
                        true
                    ) ||
                    item.genericName.contains(
                        query,
                        true
                    ) ||
                    item.brand.contains(
                        query,
                        true
                    ) ||
                    item.rackLocation.contains(
                        query,
                        true
                    )

            val matchesFilter =
                when (stockFilter) {
                    "LOW" ->
                        item.totalStock <=
                            item.lowStockLevel

                    "EXPIRING" ->
                        item.nextExpiry != null &&
                            item.nextExpiry in
                                now..nearLimit

                    "EXPIRED" ->
                        item.nextExpiry != null &&
                            item.nextExpiry < now

                    else ->
                        true
                }

            matchesQuery &&
                matchesFilter
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
            InventoryMetric(v15Text("মেয়াদ শেষ", "Expired"), expiredCount.toString(), InventoryRed, Modifier.weight(1f))
        }

        Text(
            v15Text("আগামী ৩০ দিনে মেয়াদ শেষ: $expiringCount", "Expiring within 30 days: $expiringCount"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InventoryMetric(
                v15Text("ক্রয় মূল্য", "Purchase"),
                "${V14DisplayState.currencySymbol}${v15Money(stockValue)}",
                InventoryBlue,
                Modifier.weight(1f)
            )
            InventoryMetric(
                v15Text("বিক্রয় মূল্য", "Sale value"),
                "${V14DisplayState.currencySymbol}${v15Money(saleValue)}",
                InventoryGreen,
                Modifier.weight(1f)
            )
            InventoryMetric(
                v15Text("সম্ভাব্য লাভ", "Potential profit"),
                "${V14DisplayState.currencySymbol}${v15Money(potentialProfit)}",
                InventoryOrange,
                Modifier.weight(1f)
            )
        }

        Button(
            onClick = onOpenSales,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                v15Text(
                    "▣ বিক্রি ও ইনভয়েস",
                    "▣ Sales & Invoices"
                )
            )
        }

        OutlinedButton(
            onClick = onOpenPurchases,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                v15Text(
                    "▣ ক্রয় ও সাপ্লায়ার",
                    "▣ Purchases & Suppliers"
                )
            )
        }

        OutlinedButton(
            onClick = onAddProduct,
            modifier =
                Modifier.fillMaxWidth(),
            enabled = canWrite
        ) {
            Text(
                v15Text(
                    "＋ নতুন পণ্য যোগ করুন",
                    "＋ Add product"
                )
            )
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = {
                Text(
                    v15Text(
                        "নাম / জেনেরিক / কোম্পানি / বারকোড / র‍্যাক",
                        "Name / generic / company / barcode / rack"
                    )
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            InventoryFilterButton(
                selected =
                    stockFilter == "ALL",
                label =
                    v15Text(
                        "সব",
                        "All"
                    ),
                onClick = {
                    stockFilter = "ALL"
                },
                modifier =
                    Modifier.weight(1f)
            )

            InventoryFilterButton(
                selected =
                    stockFilter == "LOW",
                label =
                    v15Text(
                        "লো স্টক ($lowCount)",
                        "Low stock ($lowCount)"
                    ),
                onClick = {
                    stockFilter = "LOW"
                },
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
            InventoryFilterButton(
                selected =
                    stockFilter == "EXPIRING",
                label =
                    v15Text(
                        "৩০ দিনে মেয়াদ ($expiringCount)",
                        "30-day expiry ($expiringCount)"
                    ),
                onClick = {
                    stockFilter =
                        "EXPIRING"
                },
                modifier =
                    Modifier.weight(1f)
            )

            InventoryFilterButton(
                selected =
                    stockFilter == "EXPIRED",
                label =
                    v15Text(
                        "মেয়াদ শেষ ($expiredCount)",
                        "Expired ($expiredCount)"
                    ),
                onClick = {
                    stockFilter =
                        "EXPIRED"
                },
                modifier =
                    Modifier.weight(1f)
            )
        }

        if (filtered.isEmpty()) {
            Text(v15Text("এখনও কোনো পণ্য নেই।", "No products yet."), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            filtered.forEach { item ->
                ProductCard(
                    item = item,
                    isPharmacy =
                        isPharmacy,
                    canWrite = canWrite,
                    onSelect = {
                        onSelect(item)
                    },
                    onEdit = {
                        editingProduct = item
                    },
                    onDelete = {
                        deletingProduct = item
                    }
                )
            }
        }
    }

    editingProduct?.let { product ->
        val editBatchesFlow =
            remember(product.id) {
                viewModel.observeBatches(product.id)
            }

        val editBatches by
            editBatchesFlow.collectAsState(
                initial = emptyList()
            )

        EditProductDialog(
            viewModel = viewModel,
            product = product,
            batches = editBatches,
            onDismiss = {
                editingProduct = null
            }
        ) { input, batchUpdates ->
            val cleanSku =
                input.sku.trim()

            val duplicateSku =
                cleanSku.isNotBlank() &&
                    products.any {
                        it.id != product.id &&
                            it.sku.trim()
                                .equals(
                                    cleanSku,
                                    ignoreCase = true
                                )
                    }

            if (duplicateSku) {
                Toast.makeText(
                    context,
                    v15Text(
                        "এই Barcode / SKU অন্য একটি পণ্যে ব্যবহার করা আছে।",
                        "This Barcode / SKU is already used by another product."
                    ),
                    Toast.LENGTH_LONG
                ).show()

                return@EditProductDialog
            }

            viewModel.updateProduct(
                item = product,
                name = input.name,
                category = input.category,
                sku = input.sku,
                unit = input.unit,
                unitConversions =
                    input.unitConversions,
                brand = input.brand,
                genericName = input.genericName,
                modelName = input.modelName,
                serialOrImei = input.serialOrImei,
                size = input.size,
                color = input.color,
                warrantyMonths =
                    input.warrantyMonths,
                sellingPrice =
                    input.sellingPrice,
                mrp = input.mrp,
                rackLocation =
                    input.rackLocation,
                lowStockLevel =
                    input.lowStockLevel,
                note = input.note
            )

            batchUpdates.forEach { batch ->
                viewModel.updateBatch(
                    batchId = batch.id,
                    quantity = batch.quantity,
                    purchasePrice =
                        batch.purchasePrice,
                    purchaseDate =
                        batch.purchaseDate,
                    expiryDate =
                        batch.expiryDate,
                    batchNo = batch.batchNo
                )
            }

            editingProduct = null
        }
    }

    deletingProduct?.let { product ->
        ProtectedDeleteDialog(
            viewModel = securityViewModel,
            title =
                v15Text(
                    "পণ্য ডিলিট করবেন?",
                    "Delete product?"
                ),
            message =
                v15Text(
                    "${product.name} ডিলিট করলে এর সব stock batch মুছে যাবে এবং এই পণ্যের সাথে যুক্ত অন্য ব্যবসায়িক রেকর্ড প্রভাবিত হতে পারে। এই কাজ ফিরিয়ে আনা যাবে না।",
                    "Deleting ${product.name} will remove all of its stock batches and may affect business records linked to this product. This cannot be undone."
                ),
            confirmLabel =
                v15Text(
                    "ডিলিট করুন",
                    "Delete"
                ),
            onDismiss = {
                deletingProduct = null
            },
            onConfirmed = {
                deletingProduct = null

                viewModel.deleteProduct(
                    product.id
                ) { deleted ->
                    if (!deleted) {
                        Toast.makeText(
                            context,
                            v15Text(
                                "এই পণ্য দিয়ে সক্রিয় বিক্রি আছে। আগে সংশ্লিষ্ট বিক্রি বাতিল করুন।",
                                "This product has active sales. Cancel those sales first."
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
private fun InventoryMetric(title: String, value: String, accent: Color, modifier: Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = accent.copy(alpha = 0.09f)) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.ExtraBold, color = accent, style = MaterialTheme.typography.titleLarge)
            Text(title, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun InventoryFilterButton(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier
        ) {
            Text(
                label,
                maxLines = 1
            )
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier
        ) {
            Text(
                label,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ProductCard(
    item: ProductStockSummary,
    isPharmacy: Boolean,
    canWrite: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val now = System.currentTimeMillis()

    val accent = when {
        item.totalStock <= 0 ->
            InventoryRed

        item.totalStock <=
            item.lowStockLevel ->
            InventoryOrange

        item.nextExpiry != null &&
            item.nextExpiry < now ->
            InventoryRed

        else ->
            InventoryGreen
    }

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    accent.copy(
                        alpha = 0.07f
                    )
            ),
        border =
            BorderStroke(
                1.dp,
                accent.copy(
                    alpha = 0.18f
                )
            )
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
                Text(
                    item.name,
                    fontWeight =
                        FontWeight.ExtraBold,
                    style =
                        MaterialTheme.typography.titleMedium
                )

                Text(
                    v15Text(
                        "স্টক ${item.totalStock}",
                        "Stock ${item.totalStock}"
                    ),
                    fontWeight =
                        FontWeight.Bold,
                    color = accent
                )
            }

            if (item.category.isNotBlank()) {
                Text(
                    item.category,
                    style =
                        MaterialTheme.typography.bodySmall
                )
            }

            if (item.brand.isNotBlank()) {
                Text(
                    if (isPharmacy) {
                        v15Text(
                            "কোম্পানি: ${item.brand}",
                            "Company: ${item.brand}"
                        )
                    } else {
                        v15Text(
                            "ব্র্যান্ড: ${item.brand}",
                            "Brand: ${item.brand}"
                        )
                    },
                    style =
                        MaterialTheme.typography.bodySmall
                )
            }

            if (
                isPharmacy &&
                item.genericName.isNotBlank()
            ) {
                Text(
                    v15Text(
                        "জেনেরিক: ${item.genericName}",
                        "Generic: ${item.genericName}"
                    ),
                    style =
                        MaterialTheme.typography.bodySmall
                )
            }

            if (
                isPharmacy &&
                item.rackLocation.isNotBlank()
            ) {
                Text(
                    v15Text(
                        "র‍্যাক: ${item.rackLocation}",
                        "Rack: ${item.rackLocation}"
                    ),
                    style =
                        MaterialTheme.typography.bodySmall
                )
            }

            Text(
                v15Text(
                    "ইউনিট: ${item.unit}",
                    "Unit: ${item.unit}"
                ),
                style =
                    MaterialTheme.typography.bodySmall
            )

            if (item.sku.isNotBlank()) {
                Text(
                    v15Text(
                        "বারকোড/SKU: ${item.sku}",
                        "Barcode/SKU: ${item.sku}"
                    ),
                    style =
                        MaterialTheme.typography.bodySmall
                )
            }

            val unitProfit =
                item.sellingPrice -
                    item.avgPurchasePrice

            Text(
                v15Text(
                    "কেনা ${V14DisplayState.currencySymbol} ${v15Money(item.avgPurchasePrice)} • বিক্রি ${V14DisplayState.currencySymbol} ${v15Money(item.sellingPrice)}",
                    "Buy ${V14DisplayState.currencySymbol} ${v15Money(item.avgPurchasePrice)} • Sell ${V14DisplayState.currencySymbol} ${v15Money(item.sellingPrice)}"
                ),
                style =
                    MaterialTheme.typography.bodySmall,
                fontWeight =
                    FontWeight.SemiBold
            )

            if (
                isPharmacy &&
                item.mrp > 0.0
            ) {
                Text(
                    v15Text(
                        "MRP: ${V14DisplayState.currencySymbol} ${v15Money(item.mrp)}",
                        "MRP: ${V14DisplayState.currencySymbol} ${v15Money(item.mrp)}"
                    ),
                    style =
                        MaterialTheme.typography.bodySmall,
                    fontWeight =
                        FontWeight.SemiBold
                )
            }

            Text(
                v15Text(
                    "লাভ/ইউনিট: ${V14DisplayState.currencySymbol} ${v15Money(unitProfit)}",
                    "Profit/unit: ${V14DisplayState.currencySymbol} ${v15Money(unitProfit)}"
                ),
                style =
                    MaterialTheme.typography.bodySmall,
                color =
                    if (unitProfit >= 0)
                        InventoryGreen
                    else
                        InventoryRed
            )

            item.nextExpiry?.let {
                Text(
                    v15Text(
                        "নিকটতম মেয়াদ: ${v15Date(it)}",
                        "Next expiry: ${v15Date(it)}"
                    ),
                    style =
                        MaterialTheme.typography.bodySmall,
                    color =
                        if (it < now)
                            InventoryRed
                        else
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                )
            }

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = onSelect,
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text(
                        v15Text(
                            "বিস্তারিত",
                            "Details"
                        )
                    )
                }

                if (canWrite) {
                    OutlinedButton(
                        onClick = onEdit,
                        modifier =
                            Modifier.weight(1f)
                    ) {
                        Text(
                            v15Text(
                                "এডিট",
                                "Edit"
                            )
                        )
                    }

                    OutlinedButton(
                        onClick = onDelete,
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

@Composable
private fun ProductDetailScreen(
    product: ProductStockSummary,
    canWrite: Boolean,
    viewModel: InventoryViewModel,
    securityViewModel: FamilyKhataViewModel,
    onAddStock: () -> Unit,
    onDeleted: () -> Unit
) {
    val context = LocalContext.current

    val prefs = remember {
        context.getSharedPreferences(
            "hisabi_khata_v14_settings",
            android.content.Context.MODE_PRIVATE
        )
    }

    val shopType = remember {
        prefs.getString(
            "business_type",
            ""
        ) ?: ""
    }

    val isPharmacy =
        remember(shopType) {
            productFormMode(shopType) ==
                ProductFormMode.PHARMACY
        }

    val batchesFlow =
        remember(product.id) {
            viewModel.observeBatches(
                product.id
            )
        }
    val batches by batchesFlow.collectAsState(initial = emptyList())

    val stockUnitsFlow =
        remember(product.id) {
            viewModel.observeProductUnitConversions(
                product.id
            )
        }

    val stockUnitConversions by
        stockUnitsFlow.collectAsState(
            initial = emptyList()
        )

    val stockUnitOptions =
        buildList<InventoryStockUnitOption> {
            add(
                InventoryStockUnitOption(
                    name =
                        product.unit
                            .trim()
                            .ifBlank { "pcs" },
                    factor = 1
                )
            )

            stockUnitConversions.forEach { conversion ->
                if (
                    conversion.unitName.isNotBlank() &&
                    conversion.baseQuantity > 1
                ) {
                    add(
                        InventoryStockUnitOption(
                            name = conversion.unitName,
                            factor = conversion.baseQuantity
                        )
                    )
                }
            }
        }

    var showReduce by remember { mutableStateOf(false) }
    var showEdit by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(product.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Text(v15Text("ফোনের Back ব্যবহার করে পণ্য তালিকায় ফিরুন", "Use your phone Back button to return to the product list"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = InventoryGreen.copy(alpha = 0.09f)) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    v15Text(
                        v15Text("বর্তমান স্টক: ${product.totalStock} ${product.unit}","Current stock: ${product.totalStock} ${product.unit}"),
                        "Current stock: ${product.totalStock} ${product.unit}"
                    ),
                    fontWeight = FontWeight.ExtraBold
                )

                if (product.brand.isNotBlank()) {
                    Text(
                        if (isPharmacy) {
                            v15Text(
                                "কোম্পানি: ${product.brand}",
                                "Company: ${product.brand}"
                            )
                        } else {
                            v15Text(
                                "ব্র্যান্ড: ${product.brand}",
                                "Brand: ${product.brand}"
                            )
                        }
                    )
                }

                if (product.genericName.isNotBlank()) {
                    Text(
                        v15Text(
                            "জেনেরিক: ${product.genericName}",
                            "Generic: ${product.genericName}"
                        )
                    )
                }

                if (
                    isPharmacy &&
                    product.rackLocation.isNotBlank()
                ) {
                    Text(
                        v15Text(
                            "র‍্যাক / লোকেশন: ${product.rackLocation}",
                            "Rack / location: ${product.rackLocation}"
                        )
                    )
                }
                if (product.modelName.isNotBlank()) {
                    Text(v15Text("মডেল: ${product.modelName}", "Model: ${product.modelName}"))
                }
                if (product.serialOrImei.isNotBlank()) {
                    Text(v15Text("Serial / IMEI: ${product.serialOrImei}", "Serial / IMEI: ${product.serialOrImei}"))
                }
                if (product.size.isNotBlank()) {
                    Text(v15Text("সাইজ: ${product.size}", "Size: ${product.size}"))
                }
                if (product.color.isNotBlank()) {
                    Text(v15Text("রং: ${product.color}", "Color: ${product.color}"))
                }
                if (product.warrantyMonths > 0) {
                    Text(
                        v15Text(
                            v15Text("ওয়ারেন্টি: ${product.warrantyMonths} মাস","Warranty: ${product.warrantyMonths} months"),
                            "Warranty: ${product.warrantyMonths} months"
                        )
                    )
                }

                Text(
                    v15Text(
                        v15Text("গড় কেনা দাম: ${V14DisplayState.currencySymbol} ${v15Money(product.avgPurchasePrice)}","Average purchase price: ${V14DisplayState.currencySymbol} ${v15Money(product.avgPurchasePrice)}"),
                        "Average purchase price: ${V14DisplayState.currencySymbol} ${v15Money(product.avgPurchasePrice)}"
                    )
                )

                Text(
                    v15Text(
                        v15Text("বিক্রয় দাম: ${V14DisplayState.currencySymbol} ${v15Money(product.sellingPrice)}","Selling price: ${V14DisplayState.currencySymbol} ${v15Money(product.sellingPrice)}"),
                        "Selling price: ${V14DisplayState.currencySymbol} ${v15Money(product.sellingPrice)}"
                    )
                )

                if (
                    isPharmacy &&
                    product.mrp > 0.0
                ) {
                    Text(
                        v15Text(
                            "MRP: ${V14DisplayState.currencySymbol} ${v15Money(product.mrp)}",
                            "MRP: ${V14DisplayState.currencySymbol} ${v15Money(product.mrp)}"
                        ),
                        fontWeight =
                            FontWeight.Bold
                    )
                }

                Text(
                    v15Text(
                        v15Text("প্রতি ইউনিট লাভ: ${V14DisplayState.currencySymbol} ${v15Money(product.sellingPrice - product.avgPurchasePrice)}","Profit per unit: ${V14DisplayState.currencySymbol} ${v15Money(product.sellingPrice - product.avgPurchasePrice)}"),
                        "Profit per unit: ${V14DisplayState.currencySymbol} ${v15Money(product.sellingPrice - product.avgPurchasePrice)}"
                    ),
                    color = if (product.sellingPrice >= product.avgPurchasePrice) InventoryGreen else InventoryRed,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    v15Text(
                        v15Text("বর্তমান ক্রয় মূল্য: ${V14DisplayState.currencySymbol} ${v15Money(product.stockValue)}","Current purchase value: ${V14DisplayState.currencySymbol} ${v15Money(product.stockValue)}"),
                        "Current purchase value: ${V14DisplayState.currencySymbol} ${v15Money(product.stockValue)}"
                    )
                )

                Text(
                    v15Text(
                        v15Text("সম্ভাব্য বিক্রয় মূল্য: ${V14DisplayState.currencySymbol} ${v15Money(product.saleValue)}","Potential sales value: ${V14DisplayState.currencySymbol} ${v15Money(product.saleValue)}"),
                        "Potential sale value: ${V14DisplayState.currencySymbol} ${v15Money(product.saleValue)}"
                    )
                )

                Text(
                    v15Text(
                        v15Text("সম্ভাব্য লাভ: ${V14DisplayState.currencySymbol} ${v15Money(product.potentialProfit)}","Potential profit: ${V14DisplayState.currencySymbol} ${v15Money(product.potentialProfit)}"),
                        "Potential profit: ${V14DisplayState.currencySymbol} ${v15Money(product.potentialProfit)}"
                    ),
                    fontWeight = FontWeight.Bold,
                    color = if (product.potentialProfit >= 0) InventoryGreen else InventoryRed
                )

                if (product.lowStockLevel > 0) Text(v15Text("লো-স্টক সীমা: ${product.lowStockLevel}", "Low-stock threshold: ${product.lowStockLevel}"))
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onAddStock,
                enabled = canWrite,
                modifier =
                    Modifier.weight(1f)
            ) {
                Text(
                    v15Text(
                        "＋ স্টক",
                        "+ Stock"
                    )
                )
            }
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


    if (showReduce) {
        ReduceStockDialog(
            maxBaseQuantity = product.totalStock,
            unitOptions = stockUnitOptions,
            onDismiss = {
                showReduce = false
            }
        ) { input ->
            viewModel.reduceStock(
                productId = product.id,
                quantity = input.quantity,
                unitName = input.unitName,
                unitFactor = input.unitFactor
            ) { ok ->
                Toast.makeText(
                    context,
                    if (ok) {
                        v15Text(
                            "স্টক আপডেট হয়েছে",
                            "Stock updated"
                        )
                    } else {
                        v15Text(
                            "স্টক কমানো যায়নি",
                            "Could not reduce stock"
                        )
                    },
                    Toast.LENGTH_SHORT
                ).show()
            }
            showReduce = false
        }
    }
    if (showEdit) {
        EditProductDialog(
            viewModel = viewModel,
            product = product,
            batches = batches,
            onDismiss = { showEdit = false }
        ) { input, batchUpdates ->
            val cleanSku =
                input.sku.trim()

            val duplicateSku =
                cleanSku.isNotBlank() &&
                    products.any {
                        it.id != product.id &&
                            it.sku.trim()
                                .equals(
                                    cleanSku,
                                    ignoreCase = true
                                )
                    }

            if (duplicateSku) {
                Toast.makeText(
                    context,
                    v15Text(
                        "এই Barcode / SKU অন্য একটি পণ্যে ব্যবহার করা আছে।",
                        "This Barcode / SKU is already used by another product."
                    ),
                    Toast.LENGTH_LONG
                ).show()

                return@EditProductDialog
            }

            viewModel.updateProduct(
                item = product,
                name = input.name,
                category = input.category,
                sku = input.sku,
                unit = input.unit,
                unitConversions =
                    input.unitConversions,
                brand = input.brand,
                genericName = input.genericName,
                modelName = input.modelName,
                serialOrImei = input.serialOrImei,
                size = input.size,
                color = input.color,
                warrantyMonths = input.warrantyMonths,
                sellingPrice = input.sellingPrice,
                mrp = input.mrp,
                rackLocation = input.rackLocation,
                lowStockLevel = input.lowStockLevel,
                note = input.note
            )

            batchUpdates.forEach { batch ->
                viewModel.updateBatch(
                    batchId = batch.id,
                    quantity = batch.quantity,
                    purchasePrice = batch.purchasePrice,
                    purchaseDate = batch.purchaseDate,
                    expiryDate = batch.expiryDate,
                    batchNo = batch.batchNo
                )
            }

            showEdit = false
        }
    }
    if (showDelete) {
        ProtectedDeleteDialog(
            viewModel = securityViewModel,
            title =
                v15Text(
                    "পণ্য ডিলিট করবেন?",
                    "Delete product?"
                ),
            message =
                v15Text(
                    "পণ্যের সব stock batch-ও মুছে যাবে।",
                    "All stock batches for this product will also be deleted."
                ),
            confirmLabel =
                v15Text(
                    "ডিলিট",
                    "Delete"
                ),
            onDismiss = {
                showDelete = false
            },
            onConfirmed = {
                viewModel.deleteProduct(product.id)
                showDelete = false
                onDeleted()
            }
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
            if (item.batchNo.isNotBlank()) {
                Text(v15Text("Batch/Lot: ${item.batchNo}", "Batch/Lot: ${item.batchNo}"))
            }
            Text(v15Text("কেনা: ${v15Date(item.purchaseDate)} • ${V14DisplayState.currencySymbol} ${v15Money(item.purchasePrice)}", "Purchased: ${v15Date(item.purchaseDate)} • ${V14DisplayState.currencySymbol} ${v15Money(item.purchasePrice)}"), style = MaterialTheme.typography.bodySmall)
            item.expiryDate?.let { Text(v15Text("মেয়াদ: ${v15Date(it)}", "Expiry: ${v15Date(it)}"), style = MaterialTheme.typography.bodySmall, color = expiryColor) }
        }
    }
}

@Composable
private fun AddProductForm(
    workspace: String,
    onDismiss: () -> Unit,
    onSave: (NewProductInput) -> Unit
) {
    val context = LocalContext.current
    val scanner = remember { GmsBarcodeScanning.getClient(context) }

    val prefs = remember {
        context.getSharedPreferences(
            "hisabi_khata_v14_settings",
            android.content.Context.MODE_PRIVATE
        )
    }

    val shopType = remember {
        prefs.getString("business_type", "") ?: ""
    }

    val mode = remember(shopType) {
        productFormMode(shopType)
    }

    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("pcs") }
    var unitDrafts by remember {
        mutableStateOf(
            emptyList<ProductUnitDraft>()
        )
    }
    var brand by remember { mutableStateOf("") }
    var genericName by remember { mutableStateOf("") }
    var modelName by remember { mutableStateOf("") }
    var serialOrImei by remember { mutableStateOf("") }
    var size by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }
    var warranty by remember { mutableStateOf("") }

    var sell by remember { mutableStateOf("") }
    var mrp by remember { mutableStateOf("") }
    var rackLocation by remember { mutableStateOf("") }
    var low by remember { mutableStateOf("5") }
    var note by remember { mutableStateOf("") }

    var qty by remember { mutableStateOf("") }
    var buy by remember { mutableStateOf("") }
    var batchNo by remember { mutableStateOf("") }

    var stockUnitName by
        remember {
            mutableStateOf("pcs")
        }

    var purchaseDate by remember {
        mutableStateOf(startOfDay(System.currentTimeMillis()))
    }

    var expiryDate by remember {
        mutableStateOf<Long?>(null)
    }

    var showUnitPicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val showExpiry =
        mode == ProductFormMode.PHARMACY ||
        mode == ProductFormMode.EXPIRY_RETAIL ||
        mode == ProductFormMode.GENERAL

    val baseStockUnit =
        unit.trim()
            .ifBlank { "pcs" }

    val currentUnitInputs =
        buildProductUnitInputs(
            baseUnit = baseStockUnit,
            drafts = unitDrafts
        )

    val initialStockUnitOptions =
        buildList<InventoryStockUnitOption> {
            add(
                InventoryStockUnitOption(
                    name = baseStockUnit,
                    factor = 1
                )
            )

            currentUnitInputs
                .orEmpty()
                .forEach { conversion ->
                    add(
                        InventoryStockUnitOption(
                            name = conversion.unitName,
                            factor = conversion.baseQuantity
                        )
                    )
                }
        }

    val selectedInitialStockOption =
        initialStockUnitOptions
            .firstOrNull {
                it.name.equals(
                    stockUnitName,
                    ignoreCase = true
                )
            }
            ?: initialStockUnitOptions.first()

    BackHandler {
        onDismiss()
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(horizontal = 12.dp)
                .padding(bottom = 28.dp),
        verticalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {
                if (shopType.isNotBlank()) {
                    Text(
                        v15Text(
                            v15Text("দোকানের ধরন: $shopType","Business type: $shopType"),
                            "Business type: $shopType"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedTextField(
                    name,
                    { name = it },
                    label = { Text(productNameLabel(mode)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    category,
                    { category = it },
                    label = {
                        Text(v15Text("পণ্যের ক্যাটাগরি", "Product category"))
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    sku,
                    { sku = it },
                    label = { Text("Barcode / SKU") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedButton(
                    onClick = {
                        scanner.startScan()
                            .addOnSuccessListener {
                                sku = it.rawValue.orEmpty()
                            }
                            .addOnFailureListener {
                                Toast.makeText(
                                    context,
                                    v15Text("স্ক্যান করা যায়নি", "Scan failed"),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(v15Text("▣ বারকোড স্ক্যান", "▣ Scan barcode"))
                }

                OutlinedButton(
                    onClick = { showUnitPicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        v15Text(
                            v15Text("ইউনিট: $unit","Unit: $unit"),
                            "Unit: $unit"
                        )
                    )
                }

                ProductUnitConversionEditor(
                    baseUnit =
                        unit.trim()
                            .ifBlank { "pcs" },
                    drafts = unitDrafts,
                    onChange = {
                        unitDrafts = it
                    }
                )


                OutlinedTextField(
                    brand,
                    { brand = it },
                    label = {
                        Text(
                            if (
                                mode ==
                                    ProductFormMode.PHARMACY
                            ) {
                                v15Text(
                                    "কোম্পানি / প্রস্তুতকারক",
                                    "Company / Manufacturer"
                                )
                            } else {
                                v15Text(
                                    "ব্র্যান্ড",
                                    "Brand"
                                )
                            }
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (mode == ProductFormMode.PHARMACY) {
                    OutlinedTextField(
                        genericName,
                        { genericName = it },
                        label = {
                            Text(
                                v15Text(
                                    "জেনেরিক নাম",
                                    "Generic name"
                                )
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        rackLocation,
                        { rackLocation = it },
                        label = {
                            Text(
                                v15Text(
                                    "র‍্যাক / লোকেশন",
                                    "Rack / location"
                                )
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        mrp,
                        { mrp = it },
                        label = {
                            Text(
                                v15Text(
                                    "MRP / সর্বোচ্চ খুচরা মূল্য",
                                    "MRP / maximum retail price"
                                )
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (mode == ProductFormMode.ELECTRONICS) {
                    OutlinedTextField(
                        modelName,
                        { modelName = it },
                        label = { Text(v15Text("মডেল", "Model")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        serialOrImei,
                        { serialOrImei = it },
                        label = { Text("Serial / IMEI") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        warranty,
                        { warranty = it },
                        label = {
                            Text(
                                v15Text(
                                    v15Text("ওয়ারেন্টি (মাস)","Warranty (months)"),
                                    "Warranty (months)"
                                )
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (mode == ProductFormMode.FASHION) {
                    OutlinedTextField(
                        size,
                        { size = it },
                        label = { Text(v15Text("সাইজ", "Size")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        color,
                        { color = it },
                        label = { Text(v15Text("রং", "Color")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    sell,
                    { sell = it },
                    label = {
                        Text(
                            v15Text(
                                "বিক্রয় মূল্য / $baseStockUnit",
                                "Selling price / $baseStockUnit"
                            )
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                InventoryStockUnitSelector(
                    options = initialStockUnitOptions,
                    selectedName =
                        selectedInitialStockOption.name,
                    label =
                        v15Text(
                            "প্রাথমিক স্টকের ইউনিট",
                            "Initial stock unit"
                        ),
                    onSelect = { option ->
                        stockUnitName = option.name
                    }
                )

                OutlinedTextField(
                    buy,
                    { buy = it },
                    label = {
                        Text(
                            v15Text(
                                "ক্রয় মূল্য / ${selectedInitialStockOption.name}",
                                "Purchase price / ${selectedInitialStockOption.name}"
                            )
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    qty,
                    { qty = it },
                    label = {
                        Text(
                            v15Text(
                                "প্রাথমিক স্টক (${selectedInitialStockOption.name})",
                                "Initial stock (${selectedInitialStockOption.name})"
                            )
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                val initialEnteredQuantity =
                    qty.v15InventoryIntOrNull()

                if (
                    initialEnteredQuantity != null &&
                    initialEnteredQuantity > 0 &&
                    selectedInitialStockOption.factor > 1
                ) {
                    val convertedBaseQuantity =
                        initialEnteredQuantity.toLong() *
                            selectedInitialStockOption.factor.toLong()

                    Text(
                        "$initialEnteredQuantity ${selectedInitialStockOption.name} = " +
                            "$convertedBaseQuantity $baseStockUnit",
                        style =
                            MaterialTheme.typography.bodySmall,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    low,
                    { low = it },
                    label = {
                        Text(v15Text("লো-স্টক সীমা", "Low-stock threshold"))
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    batchNo,
                    { batchNo = it },
                    label = {
                        Text(v15Text("Batch / Lot No. (ঐচ্ছিক)", "Batch / Lot No. (optional)"))
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                DateButton(
                    v15Text("কেনার তারিখ", "Purchase date"),
                    purchaseDate
                ) {
                    purchaseDate = it
                }

                if (showExpiry) {
                    NullableDateButton(
                        v15Text("মেয়াদ শেষের তারিখ", "Expiry date"),
                        expiryDate
                    ) {
                        expiryDate = it
                    }
                }

                OutlinedTextField(
                    note,
                    { note = it },
                    label = { Text(v15Text("নোট", "Note")) },
                    modifier = Modifier.fillMaxWidth()
                )

                error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error
                    )
                }
        Button(
            modifier =
                Modifier.fillMaxWidth(),
            onClick = {
                    val unitInputs =
                        buildProductUnitInputs(
                            baseUnit = unit,
                            drafts = unitDrafts
                        )

                    if (unitInputs == null) {
                        error =
                            v15Text(
                                "ইউনিট কনভার্সন ঠিক করুন। সঠিক রেফারেন্স ইউনিট এবং ধনাত্মক অনুপাত দিন।",
                                "Check unit conversion. Select a valid reference unit and use a positive multiplier."
                            )
                        return@Button
                    }

                    val selling =
                        sell.v15InventoryDoubleOrNull()
                            ?: 0.0

                    val mrpValue =
                        mrp.v15InventoryDoubleOrNull()
                            ?: 0.0

                    val purchase =
                        buy.v15InventoryDoubleOrNull()
                            ?: 0.0

                    val quantity =
                        qty.v15InventoryIntOrNull()
                            ?: 0
                    val lowValue = low.v15InventoryIntOrNull() ?: 0
                    val warrantyMonths = warranty.v15InventoryIntOrNull() ?: 0

                    when {
                        name.isBlank() ->
                            error = v15Text(
                                v15Text("পণ্যের নাম লিখুন","Enter product name"),
                                "Enter product name"
                            )

                        selling < 0 ||
                            mrpValue < 0 ||
                            purchase < 0 ||
                            quantity < 0 ||
                            lowValue < 0 ||
                            warrantyMonths < 0 ->
                            error = v15Text(
                                v15Text("সংখ্যাগুলো সঠিক নয়","Enter valid numbers"),
                                "Check numeric values"
                            )

                        else -> {
                            onSave(
                                NewProductInput(
                                    product = ProductDetailsInput(
                                        name = name.trim(),
                                        category = category.trim(),
                                        sku = sku.trim(),
                                        unit = unit.trim().ifBlank { "pcs" },
                                          unitConversions =
                                              unitInputs,
                                        brand = brand.trim(),
                                        genericName = genericName.trim(),
                                        modelName = modelName.trim(),
                                        serialOrImei = serialOrImei.trim(),
                                        size = size.trim(),
                                        color = color.trim(),
                                        warrantyMonths = warrantyMonths,
                                        sellingPrice = selling,
                                        mrp = mrpValue,
                                        rackLocation =
                                            rackLocation.trim(),
                                        lowStockLevel = lowValue,
                                        note = note.trim()
                                    ),
                                    initialQuantity = quantity,
                                    purchasePrice = purchase,
                                    purchaseDate = purchaseDate,
                                    expiryDate = if (showExpiry) expiryDate else null,
                                    batchNo = batchNo.trim(),
                                    stockUnitName =
                                        selectedInitialStockOption.name,
                                    stockUnitFactor =
                                        selectedInitialStockOption.factor
                                )
                            )
                        }
                    }
                }
            ) {
                Text(
                    v15Text(
                        "পণ্য সেভ করুন",
                        "Save product"
                    )
                )
            }

        OutlinedButton(
            onClick = onDismiss,
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

    if (showUnitPicker) {
        UnitPickerDialog(
            selected = unit,
            onDismiss = { showUnitPicker = false }
        ) {
            unit = it
            stockUnitName = it
            showUnitPicker = false
        }
    }
}

@Composable
private fun AddStockPage(
    product: ProductStockSummary,
    viewModel: InventoryViewModel,
    onDismiss: () -> Unit
) {
    val conversionsFlow =
        remember(product.id) {
            viewModel
                .observeProductUnitConversions(
                    product.id
                )
        }

    val conversions by
        conversionsFlow.collectAsState(
            initial = emptyList()
        )

    val unitOptions =
        buildList<InventoryStockUnitOption> {
            add(
                InventoryStockUnitOption(
                    name =
                        product.unit
                            .trim()
                            .ifBlank {
                                "pcs"
                            },
                    factor = 1
                )
            )

            conversions.forEach {
                    conversion ->

                if (
                    conversion.unitName
                        .isNotBlank() &&
                    conversion.baseQuantity > 1
                ) {
                    add(
                        InventoryStockUnitOption(
                            name =
                                conversion.unitName,
                            factor =
                                conversion
                                    .baseQuantity
                        )
                    )
                }
            }
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
                .padding(bottom = 24.dp),
        verticalArrangement =
            Arrangement.spacedBy(10.dp)
    ) {
        Text(
            product.name,
            style =
                MaterialTheme
                    .typography
                    .titleMedium,
            fontWeight =
                FontWeight.Bold
        )

        Text(
            v15Text(
                "বর্তমান স্টক: ${product.totalStock} ${product.unit}",
                "Current stock: ${product.totalStock} ${product.unit}"
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

        AddBatchForm(
            unitOptions =
                unitOptions,
            onDismiss =
                onDismiss
        ) { input ->
            viewModel.addBatch(
                productId =
                    product.id,
                quantity =
                    input.quantity,
                purchasePrice =
                    input.purchasePrice,
                purchaseDate =
                    input.purchaseDate,
                expiryDate =
                    input.expiryDate,
                batchNo =
                    input.batchNo,
                unitName =
                    input.unitName,
                unitFactor =
                    input.unitFactor
            )

            onDismiss()
        }
    }
}

@Composable
private fun AddBatchForm(
    unitOptions: List<InventoryStockUnitOption>,
    onDismiss: () -> Unit,
    onSave: (BatchInput) -> Unit
) {
    var qty by remember { mutableStateOf("") }
    var buy by remember { mutableStateOf("") }
    var batchNo by remember { mutableStateOf("") }

    var selectedUnitName by
        remember {
            mutableStateOf(
                unitOptions.firstOrNull()
                    ?.name
                    .orEmpty()
            )
        }

    var purchaseDate by remember {
        mutableStateOf(
            startOfDay(
                System.currentTimeMillis()
            )
        )
    }

    var expiryDate by remember {
        mutableStateOf<Long?>(null)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    val selectedUnit =
        unitOptions.firstOrNull {
            it.name.equals(
                selectedUnitName,
                ignoreCase = true
            )
        } ?: unitOptions.firstOrNull()

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(
                    rememberScrollState()
                ),
        verticalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {
                InventoryStockUnitSelector(
                    options = unitOptions,
                    selectedName =
                        selectedUnit?.name.orEmpty(),
                    label =
                        v15Text(
                            "স্টক যোগ করার ইউনিট",
                            "Stock entry unit"
                        ),
                    onSelect = { option ->
                        selectedUnitName =
                            option.name
                    }
                )

                OutlinedTextField(
                    value = qty,
                    onValueChange = {
                        qty = it
                    },
                    label = {
                        Text(
                            v15Text(
                                "পরিমাণ (${selectedUnit?.name.orEmpty()})",
                                "Quantity (${selectedUnit?.name.orEmpty()})"
                            )
                        )
                    },
                    singleLine = true,
                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = buy,
                    onValueChange = {
                        buy = it
                    },
                    label = {
                        Text(
                            v15Text(
                                "ক্রয় মূল্য / ${selectedUnit?.name.orEmpty()}",
                                "Purchase price / ${selectedUnit?.name.orEmpty()}"
                            )
                        )
                    },
                    singleLine = true,
                    modifier =
                        Modifier.fillMaxWidth()
                )

                val enteredQuantity =
                    qty.v15InventoryIntOrNull()

                if (
                    selectedUnit != null &&
                    enteredQuantity != null &&
                    enteredQuantity > 0 &&
                    selectedUnit.factor > 1
                ) {
                    val converted =
                        enteredQuantity.toLong() *
                            selectedUnit.factor.toLong()

                    Text(
                        "$enteredQuantity ${selectedUnit.name} = " +
                            "$converted ${unitOptions.first().name}",
                        style =
                            MaterialTheme.typography.bodySmall,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    value = batchNo,
                    onValueChange = {
                        batchNo = it
                    },
                    label = {
                        Text("Batch / Lot No.")
                    },
                    singleLine = true,
                    modifier =
                        Modifier.fillMaxWidth()
                )

                DateButton(
                    v15Text(
                        "কেনার তারিখ",
                        "Purchase date"
                    ),
                    purchaseDate
                ) {
                    purchaseDate = it
                }

                NullableDateButton(
                    v15Text(
                        "মেয়াদ শেষ",
                        "Expiry date"
                    ),
                    expiryDate
                ) {
                    expiryDate = it
                }

                error?.let {
                    Text(
                        it,
                        color =
                            MaterialTheme.colorScheme.error
                    )
                }
        Button(
            modifier =
                Modifier.fillMaxWidth(),
            onClick = {
                    val q =
                        qty.v15InventoryIntOrNull()

                    val p =
                        buy.v15InventoryDoubleOrNull()
                            ?: 0.0

                    val option =
                        selectedUnit

                    val baseQuantity =
                        if (
                            q != null &&
                            q > 0 &&
                            option != null
                        ) {
                            q.toLong() *
                                option.factor.toLong()
                        } else {
                            -1L
                        }

                    if (
                        q == null ||
                        q <= 0 ||
                        option == null ||
                        option.factor <= 0 ||
                        baseQuantity <= 0L ||
                        baseQuantity >
                            Int.MAX_VALUE.toLong() ||
                        !p.isFinite() ||
                        p < 0.0
                    ) {
                        error =
                            v15Text(
                                "সঠিক পরিমাণ, ইউনিট ও ক্রয় মূল্য দিন",
                                "Enter a valid quantity, unit and purchase price"
                            )
                    } else {
                        onSave(
                            BatchInput(
                                quantity = q,
                                purchasePrice = p,
                                purchaseDate = purchaseDate,
                                expiryDate = expiryDate,
                                batchNo = batchNo.trim(),
                                unitName = option.name,
                                unitFactor = option.factor
                            )
                        )
                    }
                }
            ) {
                Text(
                    v15Text(
                        "স্টক যোগ করুন",
                        "Add stock"
                    )
                )
            }

        OutlinedButton(
            onClick = onDismiss,
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
private fun ReduceStockDialog(
    maxBaseQuantity: Int,
    unitOptions: List<InventoryStockUnitOption>,
    onDismiss: () -> Unit,
    onSave: (StockQuantityInput) -> Unit
) {
    var qty by remember { mutableStateOf("") }

    var selectedUnitName by
        remember {
            mutableStateOf(
                unitOptions.firstOrNull()
                    ?.name
                    .orEmpty()
            )
        }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    val selectedUnit =
        unitOptions.firstOrNull {
            it.name.equals(
                selectedUnitName,
                ignoreCase = true
            )
        } ?: unitOptions.firstOrNull()

    val selectedAvailable =
        if (
            selectedUnit != null &&
            selectedUnit.factor > 0
        ) {
            maxBaseQuantity /
                selectedUnit.factor
        } else {
            0
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                v15Text(
                    "স্টক কমান",
                    "Reduce stock"
                )
            )
        },
        text = {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    v15Text(
                        "বর্তমান স্টক: $maxBaseQuantity ${unitOptions.firstOrNull()?.name.orEmpty()}",
                        "Current stock: $maxBaseQuantity ${unitOptions.firstOrNull()?.name.orEmpty()}"
                    )
                )

                InventoryStockUnitSelector(
                    options = unitOptions,
                    selectedName =
                        selectedUnit?.name.orEmpty(),
                    label =
                        v15Text(
                            "স্টক কমানোর ইউনিট",
                            "Stock removal unit"
                        ),
                    onSelect = { option ->
                        selectedUnitName =
                            option.name
                    }
                )

                if (selectedUnit != null) {
                    Text(
                        v15Text(
                            "এই ইউনিটে সর্বোচ্চ: $selectedAvailable ${selectedUnit.name}",
                            "Maximum in this unit: $selectedAvailable ${selectedUnit.name}"
                        ),
                        style =
                            MaterialTheme.typography.bodySmall
                    )
                }

                OutlinedTextField(
                    value = qty,
                    onValueChange = {
                        qty = it
                    },
                    label = {
                        Text(
                            v15Text(
                                "কত ${selectedUnit?.name.orEmpty()} কমাবেন",
                                "Quantity to remove (${selectedUnit?.name.orEmpty()})"
                            )
                        )
                    },
                    singleLine = true
                )

                error?.let {
                    Text(
                        it,
                        color =
                            MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val q =
                        qty.v15InventoryIntOrNull()

                    val option =
                        selectedUnit

                    val baseQuantity =
                        if (
                            q != null &&
                            q > 0 &&
                            option != null
                        ) {
                            q.toLong() *
                                option.factor.toLong()
                        } else {
                            -1L
                        }

                    if (
                        q == null ||
                        q <= 0 ||
                        option == null ||
                        option.factor <= 0 ||
                        baseQuantity <= 0L ||
                        baseQuantity >
                            maxBaseQuantity.toLong() ||
                        baseQuantity >
                            Int.MAX_VALUE.toLong()
                    ) {
                        error =
                            v15Text(
                                "সঠিক পরিমাণ দিন",
                                "Enter a valid quantity"
                            )
                    } else {
                        onSave(
                            StockQuantityInput(
                                quantity = q,
                                unitName = option.name,
                                unitFactor = option.factor
                            )
                        )
                    }
                }
            ) {
                Text(
                    v15Text(
                        "আপডেট",
                        "Update"
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
private fun EditProductDialog(
    viewModel: InventoryViewModel,
    product: ProductStockSummary,
    batches: List<StockBatchEntity>,
    onDismiss: () -> Unit,
    onSave: (ProductDetailsInput, List<BatchUpdateInput>) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(
            "hisabi_khata_v14_settings",
            android.content.Context.MODE_PRIVATE
        )
    }

    val shopType = remember {
        prefs.getString("business_type", "") ?: ""
    }

    val mode = remember(shopType) {
        productFormMode(shopType)
    }

    val storedUnitConversions by
        viewModel
            .observeProductUnitConversions(
                product.id
            )
            .collectAsState(
                initial = emptyList()
            )

    var unitDrafts by remember(
        product.id,
        storedUnitConversions
    ) {
        mutableStateOf(
            storedUnitDrafts(
                baseUnit = product.unit,
                units =
                    storedUnitConversions
            )
        )
    }

    var name by remember { mutableStateOf(product.name) }
    var category by remember { mutableStateOf(product.category) }
    var sku by remember { mutableStateOf(product.sku) }
    var unit by remember { mutableStateOf(product.unit) }
    var brand by remember { mutableStateOf(product.brand) }
    var genericName by remember { mutableStateOf(product.genericName) }
    var modelName by remember { mutableStateOf(product.modelName) }
    var serialOrImei by remember { mutableStateOf(product.serialOrImei) }
    var size by remember { mutableStateOf(product.size) }
    var color by remember { mutableStateOf(product.color) }
    var warranty by remember {
        mutableStateOf(
            product.warrantyMonths.toString()
        )
    }

    var sell by remember {
        mutableStateOf(
            v15Money(product.sellingPrice)
        )
    }

    var mrp by remember {
        mutableStateOf(
            v15Money(product.mrp)
        )
    }

    var rackLocation by remember {
        mutableStateOf(
            product.rackLocation
        )
    }

    var low by remember {
        mutableStateOf(
            product.lowStockLevel.toString()
        )
    }
    var note by remember { mutableStateOf(product.note) }
    var showUnitPicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var editableBatches by remember(product.id, batches) {
        mutableStateOf(
            batches.map { batch ->
                EditableBatchInput(
                    id = batch.id,
                    quantity = batch.quantity.toString(),
                    purchasePrice = v15Money(batch.purchasePrice),
                    purchaseDate = batch.purchaseDate,
                    expiryDate = batch.expiryDate,
                    batchNo = batch.batchNo
                )
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(v15Text("পণ্য সম্পাদনা", "Edit product"))
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                OutlinedTextField(
                    name,
                    { name = it },
                    label = { Text(productNameLabel(mode)) }
                )

                OutlinedTextField(
                    category,
                    { category = it },
                    label = { Text(v15Text("ক্যাটাগরি", "Category")) }
                )

                OutlinedTextField(
                    sku,
                    { sku = it },
                    label = { Text("Barcode / SKU") }
                )

                OutlinedTextField(
                    value =
                        unit.trim()
                            .ifBlank { "pcs" },
                    onValueChange = {},
                    readOnly = true,
                    label = {
                        Text(
                            v15Text(
                                "Base Unit (লক)",
                                "Base Unit (locked)"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                Text(
                    v15Text(
                        "স্টক ও পুরনো রেকর্ড সঠিক রাখতে Base Unit পরিবর্তন করা যাবে না। Additional Unit ও conversion পরিবর্তন করতে পারবেন।",
                        "Base Unit is locked to protect stock and historical records. Additional units and conversions can still be edited."
                    ),
                    style =
                        MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )

                ProductUnitConversionEditor(
                    baseUnit =
                        unit.trim()
                            .ifBlank { "pcs" },
                    drafts = unitDrafts,
                    onChange = {
                        unitDrafts = it
                    }
                )


                OutlinedTextField(
                    brand,
                    { brand = it },
                    label = {
                        Text(
                            if (
                                mode ==
                                    ProductFormMode.PHARMACY
                            ) {
                                v15Text(
                                    "কোম্পানি / প্রস্তুতকারক",
                                    "Company / Manufacturer"
                                )
                            } else {
                                v15Text(
                                    "ব্র্যান্ড",
                                    "Brand"
                                )
                            }
                        )
                    }
                )

                if (mode == ProductFormMode.PHARMACY) {
                    OutlinedTextField(
                        genericName,
                        { genericName = it },
                        label = {
                            Text(
                                v15Text(
                                    "জেনেরিক নাম",
                                    "Generic name"
                                )
                            )
                        }
                    )

                    OutlinedTextField(
                        rackLocation,
                        { rackLocation = it },
                        label = {
                            Text(
                                v15Text(
                                    "র‍্যাক / লোকেশন",
                                    "Rack / location"
                                )
                            )
                        }
                    )

                    OutlinedTextField(
                        mrp,
                        { mrp = it },
                        label = {
                            Text(
                                v15Text(
                                    "MRP / সর্বোচ্চ খুচরা মূল্য",
                                    "MRP / maximum retail price"
                                )
                            )
                        }
                    )
                }

                if (mode == ProductFormMode.ELECTRONICS) {
                    OutlinedTextField(
                        modelName,
                        { modelName = it },
                        label = { Text(v15Text("মডেল", "Model")) }
                    )

                    OutlinedTextField(
                        serialOrImei,
                        { serialOrImei = it },
                        label = { Text("Serial / IMEI") }
                    )

                    OutlinedTextField(
                        warranty,
                        { warranty = it },
                        label = {
                            Text(v15Text(v15Text("ওয়ারেন্টি (মাস)","Warranty (months)"), "Warranty (months)"))
                        }
                    )
                }

                if (mode == ProductFormMode.FASHION) {
                    OutlinedTextField(
                        size,
                        { size = it },
                        label = { Text(v15Text("সাইজ", "Size")) }
                    )

                    OutlinedTextField(
                        color,
                        { color = it },
                        label = { Text(v15Text("রং", "Color")) }
                    )
                }

                OutlinedTextField(
                    sell,
                    { sell = it },
                    label = { Text(v15Text("বিক্রয় মূল্য", "Selling price")) }
                )

                OutlinedTextField(
                    low,
                    { low = it },
                    label = {
                        Text(v15Text("লো-স্টক সীমা", "Low-stock threshold"))
                    }
                )

                Text(
                    v15Text(
                        "স্টক ব্যাচ / ক্রয় তথ্য",
                        "Stock batches / purchase details"
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                if (editableBatches.isEmpty()) {
                    Text(
                        v15Text(
                            "এই পণ্যের কোনো stock batch নেই।",
                            "This product has no stock batch."
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                editableBatches.forEachIndexed { index, batch ->
                    Text(
                        v15Text(
                            "ব্যাচ ${index + 1}",
                            "Batch ${index + 1}"
                        ),
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = batch.quantity,
                        onValueChange = { value ->
                            editableBatches =
                                editableBatches.toMutableList().also {
                                    it[index] =
                                        batch.copy(quantity = value)
                                }
                        },
                        label = {
                            Text(
                                v15Text(
                                    "পরিমাণ (${product.unit.trim().ifBlank { "pcs" }} Base Unit)",
                                    "Quantity (${product.unit.trim().ifBlank { "pcs" }} Base Unit)"
                                )
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = batch.purchasePrice,
                        onValueChange = { value ->
                            editableBatches =
                                editableBatches.toMutableList().also {
                                    it[index] =
                                        batch.copy(
                                            purchasePrice = value
                                        )
                                }
                        },
                        label = {
                            Text(
                                v15Text(
                                    "ক্রয় মূল্য / ${product.unit.trim().ifBlank { "pcs" }} Base Unit",
                                    "Purchase price / ${product.unit.trim().ifBlank { "pcs" }} Base Unit"
                                )
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = batch.batchNo,
                        onValueChange = { value ->
                            editableBatches =
                                editableBatches.toMutableList().also {
                                    it[index] =
                                        batch.copy(batchNo = value)
                                }
                        },
                        label = {
                            Text(
                                v15Text(
                                    "Batch / Lot No.",
                                    "Batch / Lot No."
                                )
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    DateButton(
                        v15Text(
                            "কেনার তারিখ",
                            "Purchase date"
                        ),
                        batch.purchaseDate
                    ) { value ->
                        editableBatches =
                            editableBatches.toMutableList().also {
                                it[index] =
                                    batch.copy(
                                        purchaseDate = value
                                    )
                            }
                    }

                    NullableDateButton(
                        v15Text(
                            "মেয়াদ শেষের তারিখ",
                            "Expiry date"
                        ),
                        batch.expiryDate
                    ) { value ->
                        editableBatches =
                            editableBatches.toMutableList().also {
                                it[index] =
                                    batch.copy(
                                        expiryDate = value
                                    )
                            }
                    }
                }

                OutlinedTextField(
                    note,
                    { note = it },
                    label = { Text(v15Text("নোট", "Note")) }
                )

                error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val unitInputs =
                        buildProductUnitInputs(
                            baseUnit = unit,
                            drafts = unitDrafts
                        )

                    if (unitInputs == null) {
                        error =
                            v15Text(
                                "ইউনিট কনভার্সন ঠিক করুন। সঠিক রেফারেন্স ইউনিট এবং ধনাত্মক অনুপাত দিন।",
                                "Check unit conversion. Select a valid reference unit and use a positive multiplier."
                            )
                        return@TextButton
                    }

                    val originalBaseUnit =
                        product.unit.trim()
                            .ifBlank { "pcs" }

                    val finalBaseUnit =
                        unit.trim()
                            .ifBlank { "pcs" }

                    if (
                        batches.isNotEmpty() &&
                        !originalBaseUnit.equals(
                            finalBaseUnit,
                            ignoreCase = true
                        )
                    ) {
                        error =
                            v15Text(
                                "স্টক ব্যাচ থাকা অবস্থায় বেস ইউনিট পরিবর্তন করা যাবে না।",
                                "Base unit cannot be changed while stock batches exist."
                            )
                        return@TextButton
                    }

                    val warrantyMonths =
                        warranty.v15InventoryIntOrNull()
                    val sellingPrice =
                        sell.v15InventoryDoubleOrNull()

                    val mrpValue =
                        mrp.v15InventoryDoubleOrNull()

                    val lowStockLevel =
                        low.v15InventoryIntOrNull()

                    if (
                        name.isBlank() ||
                        warrantyMonths == null ||
                        warrantyMonths < 0 ||
                        sellingPrice == null ||
                        sellingPrice < 0 ||
                        mrpValue == null ||
                        mrpValue < 0 ||
                        lowStockLevel == null ||
                        lowStockLevel < 0
                    ) {
                        error =
                            v15Text(
                                "পণ্যের তথ্য ও সংখ্যাগুলো সঠিকভাবে দিন।",
                                "Enter valid product information and numbers."
                            )
                        return@TextButton
                    }

                    val batchUpdates =
                        mutableListOf<BatchUpdateInput>()

                    for (batch in editableBatches) {
                        val quantity =
                            batch.quantity.v15InventoryIntOrNull()
                        val purchasePrice =
                            batch.purchasePrice
                                .v15InventoryDoubleOrNull()

                        if (
                            quantity == null ||
                            quantity < 0 ||
                            purchasePrice == null ||
                            purchasePrice < 0
                        ) {
                            error =
                                v15Text(
                                    "Stock batch-এর পরিমাণ ও ক্রয় মূল্য সঠিকভাবে দিন।",
                                    "Enter valid stock batch quantity and purchase price."
                                )
                            return@TextButton
                        }

                        batchUpdates +=
                            BatchUpdateInput(
                                id = batch.id,
                                quantity = quantity,
                                purchasePrice = purchasePrice,
                                purchaseDate =
                                    batch.purchaseDate,
                                expiryDate =
                                    batch.expiryDate,
                                batchNo =
                                    batch.batchNo.trim()
                            )
                    }

                    error = null

                    onSave(
                        ProductDetailsInput(
                            name = name.trim(),
                            category = category.trim(),
                            sku = sku.trim(),
                            unit =
                                unit.trim()
                                    .ifBlank { "pcs" },                              unitConversions =
                                  unitInputs,

                            brand = brand.trim(),
                            genericName =
                                genericName.trim(),
                            modelName =
                                modelName.trim(),
                            serialOrImei =
                                serialOrImei.trim(),
                            size = size.trim(),
                            color = color.trim(),
                            warrantyMonths =
                                warrantyMonths,
                            sellingPrice =
                                sellingPrice,
                            mrp = mrpValue,
                            rackLocation =
                                rackLocation.trim(),
                            lowStockLevel =
                                lowStockLevel,
                            note = note.trim()
                        ),
                        batchUpdates
                    )
                }
            ) {
                Text(v15Text("সেভ", "Save"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(v15Text("বাতিল", "Cancel"))
            }
        }
    )

    if (showUnitPicker) {
        UnitPickerDialog(
            selected = unit,
            onDismiss = { showUnitPicker = false }
        ) {
            unit = it
            showUnitPicker = false
        }
    }
}

@Composable
private fun UnitPickerDialog(
    selected: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val units = listOf(
        "pcs",
        "box",
        "pack",
        "strip",
        "bottle",
        "kg",
        "gram",
        "liter",
        "ml",
        "meter",
        "feet",
        "pair",
        "dozen",
        "set"
    )

    var custom by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(v15Text("ইউনিট নির্বাচন করুন", "Select unit"))
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 430.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                units.forEach { item ->
                    if (item == selected) {
                        Button(
                            onClick = { onSelect(item) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("$item ✓")
                        }
                    } else {
                        OutlinedButton(
                            onClick = { onSelect(item) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(item)
                        }
                    }
                }

                OutlinedTextField(
                    custom,
                    { custom = it },
                    label = {
                        Text(v15Text("Custom unit", "Custom unit"))
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (custom.isNotBlank()) {
                            onSelect(custom.trim())
                        }
                    },
                    enabled = custom.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(v15Text("এই ইউনিট ব্যবহার করুন", "Use this unit"))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(v15Text("বন্ধ", "Close"))
            }
        }
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
