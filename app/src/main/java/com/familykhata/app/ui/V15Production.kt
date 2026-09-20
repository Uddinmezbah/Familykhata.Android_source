package com.familykhata.app.ui
import com.familykhata.app.baseWorkspaceKey

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
import com.familykhata.app.production.ProductionBatchSummary
import com.familykhata.app.production.ProductionExtraCostInput
import com.familykhata.app.production.ProductionItemRoleEntity
import com.familykhata.app.production.ProductionMaterialInput
import com.familykhata.app.production.ProductionViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun V15ProductionScreen(
    workspace: String,
    shopType: String,
    canWrite: Boolean,
    onExit: () -> Unit
) {
    val vm: ProductionViewModel = viewModel()

    val products by vm.products.collectAsState()
    val roles by vm.itemRoles.collectAsState()
    val batches by vm.batches.collectAsState()

    var showInventory by remember {
        mutableStateOf(false)
    }

    var showRoleDialog by remember {
        mutableStateOf(false)
    }

    var showProductionDialog by remember {
        mutableStateOf(false)
    }

    var selectedBatch by remember {
        mutableStateOf<ProductionBatchSummary?>(null)
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
        owner = "production-detail",
        active =
            showInventory ||
                selectedBatch != null
    )

    if (showInventory) {
        V15InventoryScreen(
            workspace = baseWorkspaceKey(workspace),
            shopType = shopType,
            canWrite = canWrite,
            nestedEntry = true,
            onExit = {
                showInventory = false
            }
        )
        return
    }

    if (selectedBatch != null) {
        BackHandler {
            selectedBatch = null
        }

        V15DeepScreenContainer(
            title = v15Text(
                "উৎপাদন ব্যাচ",
                "Production batch"
            ),
            onBack = {
                selectedBatch = null
            }
        ) {
            ProductionBatchDetails(
                batch = selectedBatch!!,
                viewModel = vm,
                onBack = {
                    selectedBatch = null
                }
            )
        }
        return
    }

    BackHandler {
        onExit()
    }

    val roleByProduct =
        roles.associateBy {
            it.productId
        }

    val rawCount =
        roles.count {
            it.role == "RAW_MATERIAL" ||
                it.role == "BOTH"
        }

    val finishedCount =
        roles.count {
            it.role == "FINISHED_GOOD" ||
                it.role == "BOTH"
        }

    val completed =
        batches.filter {
            it.status == "COMPLETED"
        }

    val totalProductionCost =
        completed.sumOf {
            it.totalProductionCost
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
                "উৎপাদন / ম্যানুফ্যাকচারিং",
                "Production / Manufacturing"
            ),
            style =
                MaterialTheme.typography.headlineSmall,
            fontWeight =
                FontWeight.ExtraBold
        )

        Text(
            v15Text(
                "কাঁচামাল → উৎপাদন → খরচ → তৈরি পণ্য → স্টক",
                "Raw material → production → cost → finished goods → stock"
            )
        )

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            ProductionMetric(
                title =
                    v15Text(
                        "কাঁচামাল",
                        "Raw materials"
                    ),
                value =
                    rawCount.toString(),
                modifier =
                    Modifier.weight(1f)
            )

            ProductionMetric(
                title =
                    v15Text(
                        "তৈরি পণ্য",
                        "Finished goods"
                    ),
                value =
                    finishedCount.toString(),
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
            ProductionMetric(
                title =
                    v15Text(
                        "সম্পন্ন ব্যাচ",
                        "Completed batches"
                    ),
                value =
                    completed.size.toString(),
                modifier =
                    Modifier.weight(1f)
            )

            ProductionMetric(
                title =
                    v15Text(
                        "মোট উৎপাদন খরচ",
                        "Production cost"
                    ),
                value =
                    productionMoney(
                        totalProductionCost
                    ),
                modifier =
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
                    "পণ্য ও স্টক ম্যানেজ করুন",
                    "Manage products & stock"
                )
            )
        }

        if (canWrite) {
            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        showRoleDialog = true
                    },
                    enabled =
                        products.isNotEmpty(),
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text(
                        v15Text(
                            "পণ্যের ধরন সেট করুন",
                            "Set item role"
                        )
                    )
                }

                Button(
                    onClick = {
                        showProductionDialog = true
                    },
                    enabled =
                        rawCount > 0 &&
                        finishedCount > 0,
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text(
                        v15Text(
                            "+ উৎপাদন",
                            "+ Production"
                        )
                    )
                }
            }
        }

        Text(
            v15Text(
                "পণ্যের ভূমিকা",
                "Item roles"
            ),
            fontWeight =
                FontWeight.Bold
        )

        if (roles.isEmpty()) {
            Text(
                v15Text(
                    "প্রথমে ইনভেন্টরিতে পণ্য যোগ করে কাঁচামাল বা তৈরি পণ্য হিসেবে সেট করুন।",
                    "Add products in inventory, then mark them as raw material or finished goods."
                )
            )
        }

        roles.forEach { role ->
            val product =
                products.firstOrNull {
                    it.id ==
                        role.productId
                }

            if (product != null) {
                Card(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(11.dp),
                        horizontalArrangement =
                            Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                product.name,
                                fontWeight =
                                    FontWeight.Bold
                            )

                            Text(
                                productionRoleLabel(
                                    role.role
                                )
                            )
                        }

                        if (canWrite) {
                            TextButton(
                                onClick = {
                                    vm.removeItemRole(
                                        role.productId
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
                }
            }
        }

        Text(
            v15Text(
                "উৎপাদন ব্যাচ",
                "Production batches"
            ),
            fontWeight =
                FontWeight.Bold
        )

        if (batches.isEmpty()) {
            Text(
                v15Text(
                    "এখনো কোনো উৎপাদন ব্যাচ নেই।",
                    "No production batches yet."
                )
            )
        }

        batches.forEach { batch ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selectedBatch =
                            batch
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
                            batch.batchNo
                                .ifBlank {
                                    "#${batch.batchId}"
                                },
                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            productionStatusLabel(
                                batch.status
                            )
                        )
                    }

                    Text(
                        batch.finishedProductName
                    )

                    Text(
                        v15Text(
                            "আউটপুট: ${batch.outputQuantity}",
                            "Output: ${batch.outputQuantity}"
                        )
                    )

                    Text(
                        v15Text(
                            "উপকরণ খরচ: ${productionMoney(batch.materialCost)}",
                            "Material cost: ${productionMoney(batch.materialCost)}"
                        )
                    )

                    Text(
                        v15Text(
                            "অন্যান্য খরচ: ${productionMoney(batch.additionalCost)}",
                            "Additional cost: ${productionMoney(batch.additionalCost)}"
                        )
                    )

                    Text(
                        v15Text(
                            "মোট খরচ: ${productionMoney(batch.totalProductionCost)}",
                            "Total cost: ${productionMoney(batch.totalProductionCost)}"
                        ),
                        fontWeight =
                            FontWeight.SemiBold
                    )

                    if (
                        batch.outputQuantity > 0
                    ) {
                        Text(
                            v15Text(
                                "প্রতি ইউনিট খরচ: ${productionMoney(batch.unitProductionCost)}",
                                "Unit cost: ${productionMoney(batch.unitProductionCost)}"
                            )
                        )
                    }
                }
            }
        }
    }

    if (showRoleDialog) {
        ProductionRoleDialog(
            products = products,
            roles = roleByProduct,
            onDismiss = {
                showRoleDialog = false
            },
            onSave = {
                    productId,
                    role ->

                vm.setItemRole(
                    productId =
                        productId,
                    role =
                        role
                ) { success ->
                    if (success) {
                        showRoleDialog = false
                    }
                }
            }
        )
    }

    if (showProductionDialog) {
        ProductionRunDialog(
            products = products,
            roles = roleByProduct,
            onDismiss = {
                showProductionDialog = false
            },
            onSave = {
                    finishedProductId,
                    outputQuantity,
                    batchNo,
                    materials,
                    costs,
                    note ->

                vm.completeProduction(
                    finishedProductId =
                        finishedProductId,
                    outputQuantity =
                        outputQuantity,
                    batchNo =
                        batchNo,
                    materials =
                        materials,
                    extraCosts =
                        costs,
                    note =
                        note
                ) { success ->
                    if (success) {
                        showProductionDialog =
                            false
                    }
                }
            }
        )
    }
}

@Composable
private fun ProductionMetric(
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
private fun ProductionRoleDialog(
    products: List<ProductEntity>,
    roles:
        Map<Long, ProductionItemRoleEntity>,
    onDismiss: () -> Unit,
    onSave: (
        Long,
        String
    ) -> Unit
) {
    var productId by remember {
        mutableStateOf<Long?>(null)
    }

    var role by remember {
        mutableStateOf("RAW_MATERIAL")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                v15Text(
                    "পণ্যের ভূমিকা",
                    "Production item role"
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
                    val existing =
                        roles[product.id]

                    OutlinedButton(
                        onClick = {
                            productId =
                                product.id

                            if (existing != null) {
                                role =
                                    existing.role
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

                Text(
                    v15Text(
                        "ভূমিকা নির্বাচন",
                        "Select role"
                    ),
                    fontWeight =
                        FontWeight.Bold
                )

                listOf(
                    "RAW_MATERIAL",
                    "FINISHED_GOOD",
                    "BOTH"
                ).forEach { option ->
                    OutlinedButton(
                        onClick = {
                            role = option
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (role == option) {
                                "✓ ${productionRoleLabel(option)}"
                            } else {
                                productionRoleLabel(
                                    option
                                )
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled =
                    productId != null,
                onClick = {
                    onSave(
                        productId!!,
                        role
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
private fun ProductionRunDialog(
    products: List<ProductEntity>,
    roles:
        Map<Long, ProductionItemRoleEntity>,
    onDismiss: () -> Unit,
    onSave: (
        Long,
        Int,
        String,
        List<ProductionMaterialInput>,
        List<ProductionExtraCostInput>,
        String
    ) -> Unit
) {
    val rawProducts =
        products.filter {
            val role =
                roles[it.id]?.role

            role == "RAW_MATERIAL" ||
                role == "BOTH"
        }

    val finishedProducts =
        products.filter {
            val role =
                roles[it.id]?.role

            role == "FINISHED_GOOD" ||
                role == "BOTH"
        }

    var finishedProductId by remember {
        mutableStateOf<Long?>(null)
    }

    var outputQuantity by remember {
        mutableStateOf("")
    }

    var batchNo by remember {
        mutableStateOf("")
    }

    var materialProductId by remember {
        mutableStateOf<Long?>(null)
    }

    var materialQuantity by remember {
        mutableStateOf("")
    }

    val materials =
        remember {
            mutableStateListOf<
                ProductionMaterialInput
                >()
        }

    var costType by remember {
        mutableStateOf("LABOR")
    }

    var costAmount by remember {
        mutableStateOf("")
    }

    var costNote by remember {
        mutableStateOf("")
    }

    val costs =
        remember {
            mutableStateListOf<
                ProductionExtraCostInput
                >()
        }

    var note by remember {
        mutableStateOf("")
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
                    "নতুন উৎপাদন",
                    "New production run"
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
                    Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    v15Text(
                        "তৈরি পণ্য নির্বাচন",
                        "Select finished product"
                    ),
                    fontWeight =
                        FontWeight.Bold
                )

                finishedProducts.forEach {
                        product ->
                    OutlinedButton(
                        onClick = {
                            finishedProductId =
                                product.id
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (
                                finishedProductId ==
                                product.id
                            ) {
                                "✓ ${product.name}"
                            } else {
                                product.name
                            }
                        )
                    }
                }

                ProductionField(
                    value = outputQuantity,
                    onChange = {
                        outputQuantity = it
                    },
                    label =
                        v15Text(
                            "উৎপাদিত পরিমাণ",
                            "Output quantity"
                        )
                )

                ProductionField(
                    value = batchNo,
                    onChange = {
                        batchNo = it
                    },
                    label =
                        v15Text(
                            "ব্যাচ নম্বর (ঐচ্ছিক)",
                            "Batch number (optional)"
                        )
                )

                Text(
                    v15Text(
                        "কাঁচামাল",
                        "Raw materials"
                    ),
                    fontWeight =
                        FontWeight.Bold
                )

                rawProducts
                    .filter {
                        it.id !=
                            finishedProductId
                    }
                    .forEach {
                            product ->

                        OutlinedButton(
                            onClick = {
                                materialProductId =
                                    product.id
                            },
                            modifier =
                                Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (
                                    materialProductId ==
                                    product.id
                                ) {
                                    "✓ ${product.name}"
                                } else {
                                    product.name
                                }
                            )
                        }
                    }

                ProductionField(
                    value = materialQuantity,
                    onChange = {
                        materialQuantity = it
                    },
                    label =
                        v15Text(
                            "ব্যবহারের পরিমাণ",
                            "Consumption quantity"
                        )
                )

                OutlinedButton(
                    enabled =
                        materialProductId != null &&
                        (
                            materialQuantity
                                .toIntOrNull()
                                ?: 0
                        ) > 0,
                    onClick = {
                        materials.add(
                            ProductionMaterialInput(
                                productId =
                                    materialProductId!!,
                                quantity =
                                    materialQuantity
                                        .toIntOrNull()
                                        ?: 0
                            )
                        )

                        materialProductId = null
                        materialQuantity = ""
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text(
                        v15Text(
                            "+ কাঁচামাল যোগ করুন",
                            "+ Add material"
                        )
                    )
                }

                materials.forEachIndexed {
                        index,
                        material ->

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${productById[material.productId]?.name ?: material.productId} × ${material.quantity}"
                        )

                        TextButton(
                            onClick = {
                                materials.removeAt(
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

                Text(
                    v15Text(
                        "অতিরিক্ত উৎপাদন খরচ",
                        "Additional production cost"
                    ),
                    fontWeight =
                        FontWeight.Bold
                )

                listOf(
                    "LABOR",
                    "OVERHEAD",
                    "TRANSPORT",
                    "PACKAGING",
                    "OTHER"
                ).forEach { option ->
                    OutlinedButton(
                        onClick = {
                            costType =
                                option
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (
                                costType ==
                                option
                            ) {
                                "✓ ${productionCostTypeLabel(option)}"
                            } else {
                                productionCostTypeLabel(
                                    option
                                )
                            }
                        )
                    }
                }

                ProductionField(
                    value = costAmount,
                    onChange = {
                        costAmount = it
                    },
                    label =
                        v15Text(
                            "খরচের পরিমাণ",
                            "Cost amount"
                        )
                )

                ProductionField(
                    value = costNote,
                    onChange = {
                        costNote = it
                    },
                    label =
                        v15Text(
                            "খরচের নোট",
                            "Cost note"
                        )
                )

                OutlinedButton(
                    enabled =
                        (
                            costAmount
                                .toDoubleOrNull()
                                ?: 0.0
                        ) > 0,
                    onClick = {
                        costs.add(
                            ProductionExtraCostInput(
                                costType =
                                    costType,
                                amount =
                                    costAmount
                                        .toDoubleOrNull()
                                        ?: 0.0,
                                note =
                                    costNote
                            )
                        )

                        costAmount = ""
                        costNote = ""
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text(
                        v15Text(
                            "+ খরচ যোগ করুন",
                            "+ Add cost"
                        )
                    )
                }

                costs.forEachIndexed {
                        index,
                        cost ->

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${productionCostTypeLabel(cost.costType)}: ${productionMoney(cost.amount)}"
                        )

                        TextButton(
                            onClick = {
                                costs.removeAt(
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

                ProductionField(
                    value = note,
                    onChange = {
                        note = it
                    },
                    label =
                        v15Text(
                            "উৎপাদন নোট",
                            "Production note"
                        )
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled =
                    finishedProductId != null &&
                    (
                        outputQuantity
                            .toIntOrNull()
                            ?: 0
                    ) > 0 &&
                    materials.isNotEmpty(),
                onClick = {
                    onSave(
                        finishedProductId!!,
                        outputQuantity
                            .toIntOrNull()
                            ?: 0,
                        batchNo,
                        materials.toList(),
                        costs.toList(),
                        note
                    )
                }
            ) {
                Text(
                    v15Text(
                        "উৎপাদন সম্পন্ন করুন",
                        "Complete production"
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
private fun ProductionBatchDetails(
    batch: ProductionBatchSummary,
    viewModel: ProductionViewModel,
    onBack: () -> Unit
) {
    val consumptions by
        viewModel.observeConsumptions(
            batch.batchId
        ).collectAsState(
            initial = emptyList()
        )

    val costs by
        viewModel.observeCosts(
            batch.batchId
        ).collectAsState(
            initial = emptyList()
        )

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
                    "← উৎপাদন তালিকা",
                    "← Production list"
                )
            )
        }

        Text(
            batch.batchNo
                .ifBlank {
                    "#${batch.batchId}"
                },
            style =
                MaterialTheme.typography.headlineSmall,
            fontWeight =
                FontWeight.ExtraBold
        )

        Text(
            batch.finishedProductName
        )

        Text(
            v15Text(
                "স্ট্যাটাস: ${productionStatusLabel(batch.status)}",
                "Status: ${productionStatusLabel(batch.status)}"
            )
        )

        Text(
            v15Text(
                "আউটপুট: ${batch.outputQuantity}",
                "Output: ${batch.outputQuantity}"
            )
        )

        Text(
            v15Text(
                "উপকরণ খরচ: ${productionMoney(batch.materialCost)}",
                "Material cost: ${productionMoney(batch.materialCost)}"
            )
        )

        Text(
            v15Text(
                "অন্যান্য খরচ: ${productionMoney(batch.additionalCost)}",
                "Additional cost: ${productionMoney(batch.additionalCost)}"
            )
        )

        Text(
            v15Text(
                "মোট উৎপাদন খরচ: ${productionMoney(batch.totalProductionCost)}",
                "Total production cost: ${productionMoney(batch.totalProductionCost)}"
            ),
            fontWeight =
                FontWeight.Bold
        )

        Text(
            v15Text(
                "প্রতি ইউনিট খরচ: ${productionMoney(batch.unitProductionCost)}",
                "Unit production cost: ${productionMoney(batch.unitProductionCost)}"
            )
        )

        if (batch.completedAt != null) {
            Text(
                v15Text(
                    "সম্পন্ন: ${productionDate(batch.completedAt)}",
                    "Completed: ${productionDate(batch.completedAt)}"
                )
            )
        }

        Text(
            v15Text(
                "কাঁচামাল ব্যবহার",
                "Material consumption"
            ),
            fontWeight =
                FontWeight.Bold
        )

        consumptions.forEach { item ->
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
                        item.materialNameSnapshot,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        "${item.quantity} ${item.unitSnapshot}"
                    )

                    if (
                        item.sourceBatchNoSnapshot
                            .isNotBlank()
                    ) {
                        Text(
                            v15Text(
                                "স্টক ব্যাচ: ${item.sourceBatchNoSnapshot}",
                                "Stock batch: ${item.sourceBatchNoSnapshot}"
                            )
                        )
                    }

                    Text(
                        v15Text(
                            "খরচ: ${productionMoney(item.totalCost)}",
                            "Cost: ${productionMoney(item.totalCost)}"
                        )
                    )
                }
            }
        }

        Text(
            v15Text(
                "অতিরিক্ত খরচ",
                "Additional costs"
            ),
            fontWeight =
                FontWeight.Bold
        )

        if (costs.isEmpty()) {
            Text(
                v15Text(
                    "কোনো অতিরিক্ত খরচ নেই।",
                    "No additional costs."
                )
            )
        }

        costs.forEach { item ->
            Card(
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier =
                        Modifier.padding(10.dp)
                ) {
                    Text(
                        productionCostTypeLabel(
                            item.costType
                        ),
                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        productionMoney(
                            item.amount
                        )
                    )

                    if (
                        item.note.isNotBlank()
                    ) {
                        Text(item.note)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductionField(
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
        modifier =
            Modifier.fillMaxWidth(),
        singleLine = true
    )
}

private fun productionRoleLabel(
    role: String
): String =
    when (role) {
        "RAW_MATERIAL" ->
            v15Text(
                "কাঁচামাল",
                "Raw material"
            )

        "FINISHED_GOOD" ->
            v15Text(
                "তৈরি পণ্য",
                "Finished good"
            )

        "BOTH" ->
            v15Text(
                "কাঁচামাল + তৈরি পণ্য",
                "Raw + finished"
            )

        else ->
            role
    }

private fun productionCostTypeLabel(
    value: String
): String =
    when (value) {
        "LABOR" ->
            v15Text(
                "শ্রম",
                "Labor"
            )

        "OVERHEAD" ->
            v15Text(
                "ওভারহেড",
                "Overhead"
            )

        "TRANSPORT" ->
            v15Text(
                "পরিবহন",
                "Transport"
            )

        "PACKAGING" ->
            v15Text(
                "প্যাকেজিং",
                "Packaging"
            )

        else ->
            v15Text(
                "অন্যান্য",
                "Other"
            )
    }

private fun productionStatusLabel(
    value: String
): String =
    when (value) {
        "COMPLETED" ->
            v15Text(
                "সম্পন্ন",
                "Completed"
            )

        "IN_PROGRESS" ->
            v15Text(
                "চলমান",
                "In progress"
            )

        "CANCELLED" ->
            v15Text(
                "বাতিল",
                "Cancelled"
            )

        else ->
            v15Text(
                "ড্রাফট",
                "Draft"
            )
    }

private fun productionMoney(
    value: Double
): String =
    String.format(
        Locale.US,
        "%.2f",
        value
    )

private fun productionDate(
    value: Long
): String =
    SimpleDateFormat(
        "dd MMM yyyy",
        Locale.getDefault()
    ).format(
        Date(value)
    )
