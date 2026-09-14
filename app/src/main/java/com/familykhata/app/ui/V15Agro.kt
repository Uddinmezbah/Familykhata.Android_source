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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.familykhata.app.agro.AgroCycleSummary
import com.familykhata.app.agro.AgroViewModel
import com.familykhata.app.data.ProductEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun V15AgroScreen(
    workspace: String,
    shopType: String,
    canWrite: Boolean,
    onExit: () -> Unit
) {
    val vm: AgroViewModel = viewModel()

    val products by vm.products.collectAsState()
    val cycles by vm.cycles.collectAsState()

    val isPoultry =
        shopType.lowercase(
            Locale.getDefault()
        ).let {
            it.contains("poultry") ||
                it.contains("পোল্ট্রি")
        }

    val cycleType =
        if (isPoultry) {
            "POULTRY"
        } else {
            "CROP"
        }

    var showInventory by remember {
        mutableStateOf(false)
    }

    var showCycleDialog by remember {
        mutableStateOf(false)
    }

    var selectedCycle by remember {
        mutableStateOf<AgroCycleSummary?>(null)
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

    if (selectedCycle != null) {
        BackHandler {
            selectedCycle = null
        }

        AgroCycleDetails(
            cycle = selectedCycle!!,
            products = products,
            viewModel = vm,
            canWrite = canWrite,
            onBack = {
                selectedCycle = null
            }
        )
        return
    }

    BackHandler {
        onExit()
    }

    val activeCount =
        cycles.count {
            it.status == "ACTIVE"
        }

    val completedCount =
        cycles.count {
            it.status == "COMPLETED"
        }

    val totalCost =
        cycles.sumOf {
            it.totalCost
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
            if (isPoultry) {
                v15Text(
                    "পোল্ট্রি ব্যবস্থাপনা",
                    "Poultry Management"
                )
            } else {
                v15Text(
                    "কৃষি / এগ্রো ব্যবস্থাপনা",
                    "Agriculture / Agro Management"
                )
            },
            style =
                MaterialTheme.typography.headlineSmall,
            fontWeight =
                FontWeight.ExtraBold
        )

        Text(
            if (isPoultry) {
                v15Text(
                    "ফ্লক → খাদ্য/ওষুধ খরচ → মৃত্যুহার → উৎপাদন → স্টক",
                    "Flock → feed/medicine cost → mortality → output → stock"
                )
            } else {
                v15Text(
                    "ফসল চক্র → বীজ/সার খরচ → ক্ষতি → ফসল সংগ্রহ → স্টক",
                    "Crop cycle → seed/fertilizer cost → loss → harvest → stock"
                )
            }
        )

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            AgroMetric(
                title =
                    v15Text(
                        "চলমান",
                        "Active"
                    ),
                value =
                    activeCount.toString(),
                modifier =
                    Modifier.weight(1f)
            )

            AgroMetric(
                title =
                    v15Text(
                        "সম্পন্ন",
                        "Completed"
                    ),
                value =
                    completedCount.toString(),
                modifier =
                    Modifier.weight(1f)
            )
        }

        AgroMetric(
            title =
                v15Text(
                    "মোট অপারেটিং খরচ",
                    "Total operating cost"
                ),
            value =
                agroMoney(totalCost),
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
                    "পণ্য ও স্টক ম্যানেজ করুন",
                    "Manage products & stock"
                )
            )
        }

        if (canWrite) {
            Button(
                onClick = {
                    showCycleDialog = true
                },
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Text(
                    if (isPoultry) {
                        v15Text(
                            "+ নতুন ফ্লক",
                            "+ New flock"
                        )
                    } else {
                        v15Text(
                            "+ নতুন ফসল চক্র",
                            "+ New crop cycle"
                        )
                    }
                )
            }
        }

        Text(
            if (isPoultry) {
                v15Text(
                    "ফ্লক তালিকা",
                    "Flocks"
                )
            } else {
                v15Text(
                    "ফসল চক্র",
                    "Crop cycles"
                )
            },
            fontWeight =
                FontWeight.Bold
        )

        if (cycles.isEmpty()) {
            Text(
                if (isPoultry) {
                    v15Text(
                        "এখনো কোনো ফ্লক যোগ করা হয়নি।",
                        "No flock added yet."
                    )
                } else {
                    v15Text(
                        "এখনো কোনো ফসল চক্র যোগ করা হয়নি।",
                        "No crop cycle added yet."
                    )
                }
            )
        }

        cycles.forEach { cycle ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selectedCycle =
                            cycle
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
                            cycle.name,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            agroStatusLabel(
                                cycle.status
                            )
                        )
                    }

                    if (
                        cycle.breedOrVariety
                            .isNotBlank()
                    ) {
                        Text(
                            if (
                                cycle.cycleType ==
                                "POULTRY"
                            ) {
                                v15Text(
                                    "জাত: ${cycle.breedOrVariety}",
                                    "Breed: ${cycle.breedOrVariety}"
                                )
                            } else {
                                v15Text(
                                    "জাত/ভ্যারাইটি: ${cycle.breedOrVariety}",
                                    "Variety: ${cycle.breedOrVariety}"
                                )
                            }
                        )
                    }

                    if (
                        cycle.startingAmount > 0
                    ) {
                        Text(
                            v15Text(
                                "শুরু: ${agroNumber(cycle.startingAmount)} ${cycle.startingUnit}",
                                "Start: ${agroNumber(cycle.startingAmount)} ${cycle.startingUnit}"
                            )
                        )
                    }

                    Text(
                        v15Text(
                            "তারিখ: ${agroDate(cycle.startDate)}",
                            "Date: ${agroDate(cycle.startDate)}"
                        )
                    )

                    Text(
                        v15Text(
                            "খরচ: ${agroMoney(cycle.totalCost)}",
                            "Cost: ${agroMoney(cycle.totalCost)}"
                        )
                    )

                    Text(
                        v15Text(
                            "স্টকে যোগ: ${cycle.totalHarvestQuantity}",
                            "Added to stock: ${cycle.totalHarvestQuantity}"
                        )
                    )
                }
            }
        }
    }

    if (showCycleDialog) {
        AddAgroCycleDialog(
            cycleType = cycleType,
            isPoultry = isPoultry,
            onDismiss = {
                showCycleDialog = false
            },
            onSave = {
                    name,
                    breedOrVariety,
                    location,
                    startingAmount,
                    startingUnit,
                    note ->

                vm.addCycle(
                    cycleType =
                        cycleType,
                    name =
                        name,
                    breedOrVariety =
                        breedOrVariety,
                    location =
                        location,
                    startingAmount =
                        startingAmount,
                    startingUnit =
                        startingUnit,
                    expectedEndDate =
                        null,
                    note =
                        note
                ) { success ->
                    if (success) {
                        showCycleDialog =
                            false
                    }
                }
            }
        )
    }
}

@Composable
private fun AgroCycleDetails(
    cycle: AgroCycleSummary,
    products: List<ProductEntity>,
    viewModel: AgroViewModel,
    canWrite: Boolean,
    onBack: () -> Unit
) {
    val costs by
        viewModel.observeCosts(
            cycle.cycleId
        ).collectAsState(
            initial = emptyList()
        )

    val losses by
        viewModel.observeLosses(
            cycle.cycleId
        ).collectAsState(
            initial = emptyList()
        )

    val harvests by
        viewModel.observeHarvests(
            cycle.cycleId
        ).collectAsState(
            initial = emptyList()
        )

    val isPoultry =
        cycle.cycleType ==
            "POULTRY"

    var costType by remember {
        mutableStateOf(
            if (isPoultry) {
                "FEED"
            } else {
                "SEED"
            }
        )
    }

    var costQuantity by remember {
        mutableStateOf("")
    }

    var costUnit by remember {
        mutableStateOf("")
    }

    var costAmount by remember {
        mutableStateOf("")
    }

    var costNote by remember {
        mutableStateOf("")
    }

    var lossType by remember {
        mutableStateOf(
            if (isPoultry) {
                "MORTALITY"
            } else {
                "CROP_LOSS"
            }
        )
    }

    var lossQuantity by remember {
        mutableStateOf("")
    }

    var lossUnit by remember {
        mutableStateOf("")
    }

    var lossReason by remember {
        mutableStateOf("")
    }

    var harvestProductId by remember {
        mutableStateOf<Long?>(null)
    }

    var harvestQuantity by remember {
        mutableStateOf("")
    }

    var harvestCost by remember {
        mutableStateOf("")
    }

    var harvestBatchNo by remember {
        mutableStateOf("")
    }

    var harvestNote by remember {
        mutableStateOf("")
    }

    var message by remember {
        mutableStateOf("")
    }

    val liveTotalCost =
        costs.sumOf {
            it.amount
        }

    val allocatedCost =
        harvests.sumOf {
            it.allocatedCost
        }

    val unallocatedCost =
        (
            liveTotalCost -
                allocatedCost
            ).coerceAtLeast(0.0)

    val active =
        cycle.status ==
            "ACTIVE"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            ),
        verticalArrangement =
            Arrangement.spacedBy(10.dp)
    ) {
        TextButton(
            onClick = onBack
        ) {
            Text(
                v15Text(
                    "← খামার তালিকা",
                    "← Agro list"
                )
            )
        }

        Text(
            cycle.name,
            style =
                MaterialTheme.typography.headlineSmall,
            fontWeight =
                FontWeight.ExtraBold
        )

        Text(
            agroStatusLabel(
                cycle.status
            )
        )

        if (
            cycle.breedOrVariety
                .isNotBlank()
        ) {
            Text(cycle.breedOrVariety)
        }

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            AgroMetric(
                title =
                    v15Text(
                        "মোট খরচ",
                        "Total cost"
                    ),
                value =
                    agroMoney(
                        liveTotalCost
                    ),
                modifier =
                    Modifier.weight(1f)
            )

            AgroMetric(
                title =
                    v15Text(
                        "স্টকে বরাদ্দ",
                        "Allocated"
                    ),
                value =
                    agroMoney(
                        allocatedCost
                    ),
                modifier =
                    Modifier.weight(1f)
            )
        }

        Text(
            v15Text(
                "এখনো বরাদ্দ হয়নি: ${agroMoney(unallocatedCost)}",
                "Unallocated cost: ${agroMoney(unallocatedCost)}"
            )
        )

        if (
            canWrite &&
            active
        ) {
            Text(
                if (isPoultry) {
                    v15Text(
                        "খাদ্য / ওষুধ / অন্যান্য খরচ",
                        "Feed / medicine / operating cost"
                    )
                } else {
                    v15Text(
                        "বীজ / সার / অন্যান্য খরচ",
                        "Seed / fertilizer / operating cost"
                    )
                },
                fontWeight =
                    FontWeight.Bold
            )

            agroCostTypes(
                isPoultry
            ).forEach { type ->
                OutlinedButton(
                    onClick = {
                        costType =
                            type
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (
                            costType ==
                            type
                        ) {
                            "✓ ${agroCostLabel(type)}"
                        } else {
                            agroCostLabel(
                                type
                            )
                        }
                    )
                }
            }

            AgroField(
                value =
                    costQuantity,
                onChange = {
                    costQuantity = it
                },
                label =
                    v15Text(
                        "পরিমাণ (ঐচ্ছিক)",
                        "Quantity (optional)"
                    )
            )

            AgroField(
                value =
                    costUnit,
                onChange = {
                    costUnit = it
                },
                label =
                    v15Text(
                        "ইউনিট (kg, bag, dose ইত্যাদি)",
                        "Unit (kg, bag, dose etc.)"
                    )
            )

            AgroField(
                value =
                    costAmount,
                onChange = {
                    costAmount = it
                },
                label =
                    v15Text(
                        "মোট খরচ",
                        "Total cost"
                    )
            )

            AgroField(
                value =
                    costNote,
                onChange = {
                    costNote = it
                },
                label =
                    v15Text(
                        "নোট",
                        "Note"
                    )
            )

            Button(
                enabled =
                    (
                        costAmount
                            .toDoubleOrNull()
                            ?: 0.0
                    ) > 0,
                onClick = {
                    viewModel.addCost(
                        cycleId =
                            cycle.cycleId,
                        costType =
                            costType,
                        quantity =
                            costQuantity
                                .toDoubleOrNull()
                                ?: 0.0,
                        unit =
                            costUnit,
                        amount =
                            costAmount
                                .toDoubleOrNull()
                                ?: 0.0,
                        note =
                            costNote
                    ) { success ->
                        if (success) {
                            costQuantity = ""
                            costUnit = ""
                            costAmount = ""
                            costNote = ""

                            message =
                                v15Text(
                                    "খরচ যোগ হয়েছে।",
                                    "Cost added."
                                )
                        } else {
                            message =
                                v15Text(
                                    "খরচ যোগ করা যায়নি।",
                                    "Could not add cost."
                                )
                        }
                    }
                },
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Text(
                    v15Text(
                        "খরচ যোগ করুন",
                        "Add cost"
                    )
                )
            }

            Text(
                if (isPoultry) {
                    v15Text(
                        "মৃত্যু / ক্ষতি",
                        "Mortality / loss"
                    )
                } else {
                    v15Text(
                        "ফসলের ক্ষতি",
                        "Crop loss"
                    )
                },
                fontWeight =
                    FontWeight.Bold
            )

            agroLossTypes(
                isPoultry
            ).forEach { type ->
                OutlinedButton(
                    onClick = {
                        lossType =
                            type
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (
                            lossType ==
                            type
                        ) {
                            "✓ ${agroLossLabel(type)}"
                        } else {
                            agroLossLabel(
                                type
                            )
                        }
                    )
                }
            }

            AgroField(
                value =
                    lossQuantity,
                onChange = {
                    lossQuantity = it
                },
                label =
                    v15Text(
                        "ক্ষতির পরিমাণ",
                        "Loss quantity"
                    )
            )

            AgroField(
                value =
                    lossUnit,
                onChange = {
                    lossUnit = it
                },
                label =
                    v15Text(
                        "ইউনিট",
                        "Unit"
                    )
            )

            AgroField(
                value =
                    lossReason,
                onChange = {
                    lossReason = it
                },
                label =
                    v15Text(
                        "কারণ",
                        "Reason"
                    )
            )

            OutlinedButton(
                enabled =
                    (
                        lossQuantity
                            .toDoubleOrNull()
                            ?: 0.0
                    ) > 0,
                onClick = {
                    viewModel.addLoss(
                        cycleId =
                            cycle.cycleId,
                        lossType =
                            lossType,
                        quantity =
                            lossQuantity
                                .toDoubleOrNull()
                                ?: 0.0,
                        unit =
                            lossUnit,
                        reason =
                            lossReason
                    ) { success ->
                        if (success) {
                            lossQuantity = ""
                            lossUnit = ""
                            lossReason = ""

                            message =
                                v15Text(
                                    "ক্ষতি রেকর্ড হয়েছে।",
                                    "Loss recorded."
                                )
                        } else {
                            message =
                                v15Text(
                                    "ক্ষতি রেকর্ড করা যায়নি।",
                                    "Could not record loss."
                                )
                        }
                    }
                },
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Text(
                    v15Text(
                        "ক্ষতি রেকর্ড করুন",
                        "Record loss"
                    )
                )
            }

            Text(
                if (isPoultry) {
                    v15Text(
                        "উৎপাদন / বিক্রয়যোগ্য পণ্য স্টকে যোগ",
                        "Add poultry output to inventory"
                    )
                } else {
                    v15Text(
                        "ফসল সংগ্রহ করে স্টকে যোগ",
                        "Add harvest to inventory"
                    )
                },
                fontWeight =
                    FontWeight.Bold
            )

            if (products.isEmpty()) {
                Text(
                    v15Text(
                        "আগে Inventory থেকে একটি output product তৈরি করুন।",
                        "Create an output product in Inventory first."
                    )
                )
            }

            products.forEach { product ->
                OutlinedButton(
                    onClick = {
                        harvestProductId =
                            product.id
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (
                            harvestProductId ==
                            product.id
                        ) {
                            "✓ ${product.name}"
                        } else {
                            product.name
                        }
                    )
                }
            }

            AgroField(
                value =
                    harvestQuantity,
                onChange = {
                    harvestQuantity = it
                },
                label =
                    v15Text(
                        "স্টকে যোগ করার পরিমাণ",
                        "Quantity to add to stock"
                    )
            )

            AgroField(
                value =
                    harvestCost,
                onChange = {
                    harvestCost = it
                },
                label =
                    v15Text(
                        "খরচ বরাদ্দ (ফাঁকা = অবশিষ্ট সব খরচ)",
                        "Allocated cost (blank = all remaining cost)"
                    )
            )

            AgroField(
                value =
                    harvestBatchNo,
                onChange = {
                    harvestBatchNo = it
                },
                label =
                    v15Text(
                        "ব্যাচ / লট নম্বর",
                        "Batch / lot number"
                    )
            )

            AgroField(
                value =
                    harvestNote,
                onChange = {
                    harvestNote = it
                },
                label =
                    v15Text(
                        "নোট",
                        "Note"
                    )
            )

            Button(
                enabled =
                    harvestProductId != null &&
                    (
                        harvestQuantity
                            .toIntOrNull()
                            ?: 0
                    ) > 0,
                onClick = {
                    viewModel.addHarvest(
                        cycleId =
                            cycle.cycleId,
                        productId =
                            harvestProductId!!,
                        quantity =
                            harvestQuantity
                                .toIntOrNull()
                                ?: 0,
                        allocatedCost =
                            harvestCost
                                .takeIf {
                                    it.isNotBlank()
                                }
                                ?.toDoubleOrNull(),
                        batchNo =
                            harvestBatchNo,
                        note =
                            harvestNote
                    ) { success ->
                        if (success) {
                            harvestProductId =
                                null
                            harvestQuantity = ""
                            harvestCost = ""
                            harvestBatchNo = ""
                            harvestNote = ""

                            message =
                                v15Text(
                                    "স্টকে যোগ হয়েছে।",
                                    "Added to inventory."
                                )
                        } else {
                            message =
                                v15Text(
                                    "স্টকে যোগ করা যায়নি। খরচ বরাদ্দ বা input পরীক্ষা করুন।",
                                    "Could not add to inventory. Check allocated cost and inputs."
                                )
                        }
                    }
                },
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Text(
                    if (isPoultry) {
                        v15Text(
                            "উৎপাদন স্টকে যোগ করুন",
                            "Add output to stock"
                        )
                    } else {
                        v15Text(
                            "ফসল স্টকে যোগ করুন",
                            "Add harvest to stock"
                        )
                    }
                )
            }

            OutlinedButton(
                onClick = {
                    viewModel.completeCycle(
                        cycleId =
                            cycle.cycleId
                    ) { success ->
                        if (success) {
                            onBack()
                        } else {
                            message =
                                v15Text(
                                    "চক্র সম্পন্ন করা যায়নি।",
                                    "Could not complete cycle."
                                )
                        }
                    }
                },
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Text(
                    if (isPoultry) {
                        v15Text(
                            "ফ্লক সম্পন্ন করুন",
                            "Complete flock"
                        )
                    } else {
                        v15Text(
                            "ফসল চক্র সম্পন্ন করুন",
                            "Complete crop cycle"
                        )
                    }
                )
            }
        }

        if (message.isNotBlank()) {
            Text(message)
        }

        Text(
            v15Text(
                "খরচের ইতিহাস",
                "Cost history"
            ),
            fontWeight =
                FontWeight.Bold
        )

        if (costs.isEmpty()) {
            Text(
                v15Text(
                    "কোনো খরচ নেই।",
                    "No costs yet."
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
                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.SpaceBetween
                    ) {
                        Text(
                            agroCostLabel(
                                item.costType
                            ),
                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            agroMoney(
                                item.amount
                            )
                        )
                    }

                    if (item.quantity > 0) {
                        Text(
                            "${agroNumber(item.quantity)} ${item.unit}"
                        )
                    }

                    if (
                        item.note.isNotBlank()
                    ) {
                        Text(item.note)
                    }
                }
            }
        }

        Text(
            v15Text(
                "ক্ষতির ইতিহাস",
                "Loss history"
            ),
            fontWeight =
                FontWeight.Bold
        )

        if (losses.isEmpty()) {
            Text(
                v15Text(
                    "কোনো ক্ষতি রেকর্ড নেই।",
                    "No loss records."
                )
            )
        }

        losses.forEach { item ->
            Card(
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier =
                        Modifier.padding(10.dp)
                ) {
                    Text(
                        agroLossLabel(
                            item.lossType
                        ),
                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        "${agroNumber(item.quantity)} ${item.unit}"
                    )

                    if (
                        item.reason.isNotBlank()
                    ) {
                        Text(item.reason)
                    }
                }
            }
        }

        Text(
            if (isPoultry) {
                v15Text(
                    "উৎপাদন ইতিহাস",
                    "Output history"
                )
            } else {
                v15Text(
                    "ফসল সংগ্রহের ইতিহাস",
                    "Harvest history"
                )
            },
            fontWeight =
                FontWeight.Bold
        )

        if (harvests.isEmpty()) {
            Text(
                v15Text(
                    "এখনো কিছু স্টকে যোগ হয়নি।",
                    "Nothing added to inventory yet."
                )
            )
        }

        harvests.forEach { item ->
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
                        "${item.quantity} ${item.unitSnapshot}"
                    )

                    Text(
                        v15Text(
                            "বরাদ্দ খরচ: ${agroMoney(item.allocatedCost)}",
                            "Allocated cost: ${agroMoney(item.allocatedCost)}"
                        )
                    )

                    Text(
                        v15Text(
                            "প্রতি ইউনিট খরচ: ${agroMoney(item.unitCost)}",
                            "Unit cost: ${agroMoney(item.unitCost)}"
                        )
                    )

                    if (
                        item.batchNo.isNotBlank()
                    ) {
                        Text(
                            v15Text(
                                "ব্যাচ: ${item.batchNo}",
                                "Batch: ${item.batchNo}"
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddAgroCycleDialog(
    cycleType: String,
    isPoultry: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        String,
        String,
        String,
        Double,
        String,
        String
    ) -> Unit
) {
    var name by remember {
        mutableStateOf("")
    }

    var variety by remember {
        mutableStateOf("")
    }

    var location by remember {
        mutableStateOf("")
    }

    var startingAmount by remember {
        mutableStateOf("")
    }

    var startingUnit by remember {
        mutableStateOf(
            if (isPoultry) {
                "birds"
            } else {
                "acre"
            }
        )
    }

    var note by remember {
        mutableStateOf("")
    }

    AlertDialog(
        onDismissRequest =
            onDismiss,
        title = {
            Text(
                if (isPoultry) {
                    v15Text(
                        "নতুন ফ্লক",
                        "New flock"
                    )
                } else {
                    v15Text(
                        "নতুন ফসল চক্র",
                        "New crop cycle"
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
                AgroField(
                    value =
                        name,
                    onChange = {
                        name = it
                    },
                    label =
                        if (isPoultry) {
                            v15Text(
                                "ফ্লকের নাম",
                                "Flock name"
                            )
                        } else {
                            v15Text(
                                "ফসল / চক্রের নাম",
                                "Crop / cycle name"
                            )
                        }
                )

                AgroField(
                    value =
                        variety,
                    onChange = {
                        variety = it
                    },
                    label =
                        if (isPoultry) {
                            v15Text(
                                "জাত / ব্রিড",
                                "Breed"
                            )
                        } else {
                            v15Text(
                                "জাত / ভ্যারাইটি",
                                "Variety"
                            )
                        }
                )

                AgroField(
                    value =
                        location,
                    onChange = {
                        location = it
                    },
                    label =
                        v15Text(
                            "স্থান / শেড / জমি",
                            "Location / shed / field"
                        )
                )

                AgroField(
                    value =
                        startingAmount,
                    onChange = {
                        startingAmount = it
                    },
                    label =
                        if (isPoultry) {
                            v15Text(
                                "শুরুর পাখির সংখ্যা",
                                "Starting bird count"
                            )
                        } else {
                            v15Text(
                                "জমি / গাছ / শুরুর পরিমাণ",
                                "Starting land / plant amount"
                            )
                        }
                )

                AgroField(
                    value =
                        startingUnit,
                    onChange = {
                        startingUnit = it
                    },
                    label =
                        v15Text(
                            "ইউনিট",
                            "Unit"
                        )
                )

                AgroField(
                    value =
                        note,
                    onChange = {
                        note = it
                    },
                    label =
                        v15Text(
                            "নোট",
                            "Note"
                        )
                )

                Text(
                    v15Text(
                        "ধরন: $cycleType",
                        "Type: $cycleType"
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
                        variety,
                        location,
                        startingAmount
                            .toDoubleOrNull()
                            ?: 0.0,
                        startingUnit,
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
private fun AgroMetric(
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
private fun AgroField(
    value: String,
    onChange: (String) -> Unit,
    label: String
) {
    OutlinedTextField(
        value = value,
        onValueChange =
            onChange,
        label = {
            Text(label)
        },
        modifier =
            Modifier.fillMaxWidth(),
        singleLine = true
    )
}

private fun agroCostTypes(
    poultry: Boolean
): List<String> =
    if (poultry) {
        listOf(
            "CHICK",
            "FEED",
            "MEDICINE",
            "VACCINE",
            "LABOR",
            "TRANSPORT",
            "OTHER"
        )
    } else {
        listOf(
            "SEED",
            "FERTILIZER",
            "PESTICIDE",
            "IRRIGATION",
            "LABOR",
            "TRANSPORT",
            "EQUIPMENT",
            "OTHER"
        )
    }

private fun agroLossTypes(
    poultry: Boolean
): List<String> =
    if (poultry) {
        listOf(
            "MORTALITY",
            "WASTAGE",
            "OTHER"
        )
    } else {
        listOf(
            "CROP_LOSS",
            "WASTAGE",
            "OTHER"
        )
    }

private fun agroCostLabel(
    value: String
): String =
    when (value) {
        "CHICK" ->
            v15Text(
                "বাচ্চা / চিক",
                "Chicks"
            )

        "FEED" ->
            v15Text(
                "খাদ্য",
                "Feed"
            )

        "MEDICINE" ->
            v15Text(
                "ওষুধ",
                "Medicine"
            )

        "VACCINE" ->
            v15Text(
                "ভ্যাকসিন",
                "Vaccine"
            )

        "SEED" ->
            v15Text(
                "বীজ",
                "Seed"
            )

        "FERTILIZER" ->
            v15Text(
                "সার",
                "Fertilizer"
            )

        "PESTICIDE" ->
            v15Text(
                "কীটনাশক",
                "Pesticide"
            )

        "IRRIGATION" ->
            v15Text(
                "সেচ",
                "Irrigation"
            )

        "LABOR" ->
            v15Text(
                "শ্রম",
                "Labor"
            )

        "TRANSPORT" ->
            v15Text(
                "পরিবহন",
                "Transport"
            )

        "EQUIPMENT" ->
            v15Text(
                "যন্ত্রপাতি",
                "Equipment"
            )

        else ->
            v15Text(
                "অন্যান্য",
                "Other"
            )
    }

private fun agroLossLabel(
    value: String
): String =
    when (value) {
        "MORTALITY" ->
            v15Text(
                "মৃত্যু",
                "Mortality"
            )

        "CROP_LOSS" ->
            v15Text(
                "ফসল ক্ষতি",
                "Crop loss"
            )

        "WASTAGE" ->
            v15Text(
                "অপচয়",
                "Wastage"
            )

        else ->
            v15Text(
                "অন্যান্য",
                "Other"
            )
    }

private fun agroStatusLabel(
    value: String
): String =
    when (value) {
        "COMPLETED" ->
            v15Text(
                "সম্পন্ন",
                "Completed"
            )

        "CANCELLED" ->
            v15Text(
                "বাতিল",
                "Cancelled"
            )

        else ->
            v15Text(
                "চলমান",
                "Active"
            )
    }

private fun agroMoney(
    value: Double
): String =
    String.format(
        Locale.US,
        "%.2f",
        value
    )

private fun agroNumber(
    value: Double
): String =
    if (
        value ==
        value.toLong().toDouble()
    ) {
        value.toLong()
            .toString()
    } else {
        String.format(
            Locale.US,
            "%.2f",
            value
        )
    }

private fun agroDate(
    value: Long
): String =
    SimpleDateFormat(
        "dd MMM yyyy",
        Locale.getDefault()
    ).format(
        Date(value)
    )
