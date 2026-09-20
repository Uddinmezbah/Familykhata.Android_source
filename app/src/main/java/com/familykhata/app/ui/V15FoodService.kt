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
import com.familykhata.app.foodservice.FoodMenuItemEntity
import com.familykhata.app.foodservice.FoodOrderLineInput
import com.familykhata.app.foodservice.FoodOrderSummary
import com.familykhata.app.foodservice.FoodServiceViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun V15FoodServiceScreen(
    workspace: String,
    shopType: String,
    canWrite: Boolean,
    onExit: () -> Unit
) {
    val vm: FoodServiceViewModel = viewModel()

    val products by
        vm.inventoryProducts.collectAsState()

    val menuItems by
        vm.menuItems.collectAsState()

    val orders by
        vm.orders.collectAsState()

    val isCatering =
        shopType.lowercase(
            Locale.getDefault()
        ).let {
            it.contains("catering") ||
                it.contains("ক্যাটারিং")
        }

    var showInventory by remember {
        mutableStateOf(false)
    }

    var showAddMenu by remember {
        mutableStateOf(false)
    }

    var showAddOrder by remember {
        mutableStateOf(false)
    }

    var selectedMenu by remember {
        mutableStateOf<FoodMenuItemEntity?>(null)
    }

    var selectedOrder by remember {
        mutableStateOf<FoodOrderSummary?>(null)
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
        owner = "food-service-detail",
        active =
            showInventory ||
                selectedMenu != null ||
                selectedOrder != null
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

    if (selectedMenu != null) {
        BackHandler {
            selectedMenu = null
        }

        V15DeepScreenContainer(
            title = v15Text(
                "রেসিপি / মেনু",
                "Recipe / Menu"
            ),
            onBack = {
                selectedMenu = null
            }
        ) {
            FoodMenuRecipeScreen(
                menu = selectedMenu!!,
                products = products,
                viewModel = vm,
                canWrite = canWrite,
                onBack = {
                    selectedMenu = null
                }
            )
        }
        return
    }

    if (selectedOrder != null) {
        BackHandler {
            selectedOrder = null
        }

        V15DeepScreenContainer(
            title = v15Text(
                "অর্ডার বিস্তারিত",
                "Order details"
            ),
            onBack = {
                selectedOrder = null
            }
        ) {
            FoodOrderDetailsScreen(
                order = selectedOrder!!,
                viewModel = vm,
                canWrite = canWrite,
                onBack = {
                    selectedOrder = null
                }
            )
        }
        return
    }

    BackHandler {
        onExit()
    }

    val confirmedOrders =
        orders.count {
            it.status == "CONFIRMED"
        }

    val validOrders =
        orders.filter {
            it.status != "CANCELLED"
        }

    val totalSales =
        validOrders.sumOf {
            it.totalAmount
        }

    val totalDue =
        validOrders.sumOf {
            it.dueAmount
                .coerceAtLeast(0.0)
        }

    val grossProfit =
        orders
            .filter {
                it.status == "COMPLETED"
            }
            .sumOf {
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
            if (isCatering) {
                v15Text(
                    "ক্যাটারিং ব্যবস্থাপনা",
                    "Catering Management"
                )
            } else {
                v15Text(
                    "রেস্টুরেন্ট ব্যবস্থাপনা",
                    "Restaurant Management"
                )
            },
            style =
                MaterialTheme.typography.headlineSmall,
            fontWeight =
                FontWeight.ExtraBold
        )

        Text(
            if (isCatering) {
                v15Text(
                    "মেনু → রেসিপি → ইভেন্ট অর্ডার → পেমেন্ট → উপকরণ স্টক → লাভ",
                    "Menu → recipe → event order → payment → ingredient stock → profit"
                )
            } else {
                v15Text(
                    "মেনু → রেসিপি → অর্ডার → পেমেন্ট → রান্নার উপকরণ স্টক → লাভ",
                    "Menu → recipe → order → payment → ingredient stock → profit"
                )
            }
        )

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            FoodMetric(
                title =
                    v15Text(
                        "মেনু",
                        "Menu"
                    ),
                value =
                    menuItems.count {
                        it.active
                    }.toString(),
                modifier =
                    Modifier.weight(1f)
            )

            FoodMetric(
                title =
                    v15Text(
                        "চলমান অর্ডার",
                        "Open orders"
                    ),
                value =
                    confirmedOrders.toString(),
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
            FoodMetric(
                title =
                    v15Text(
                        "মোট বিক্রি",
                        "Sales"
                    ),
                value =
                    foodMoney(
                        totalSales
                    ),
                modifier =
                    Modifier.weight(1f)
            )

            FoodMetric(
                title =
                    v15Text(
                        "বাকি",
                        "Due"
                    ),
                value =
                    foodMoney(
                        totalDue
                    ),
                modifier =
                    Modifier.weight(1f)
            )
        }

        FoodMetric(
            title =
                v15Text(
                    "সম্পন্ন অর্ডারের গ্রস প্রফিট",
                    "Completed-order gross profit"
                ),
            value =
                foodMoney(
                    grossProfit
                ),
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
                    "উপকরণ ও ইনভেন্টরি ম্যানেজ করুন",
                    "Manage ingredients & inventory"
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
                        showAddMenu = true
                    },
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text(
                        v15Text(
                            "+ মেনু",
                            "+ Menu"
                        )
                    )
                }

                Button(
                    onClick = {
                        showAddOrder = true
                    },
                    enabled =
                        menuItems.any {
                            it.active
                        },
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text(
                        if (isCatering) {
                            v15Text(
                                "+ ইভেন্ট",
                                "+ Event"
                            )
                        } else {
                            v15Text(
                                "+ অর্ডার",
                                "+ Order"
                            )
                        }
                    )
                }
            }
        }

        Text(
            v15Text(
                "মেনু ও রেসিপি",
                "Menu & Recipes"
            ),
            fontWeight =
                FontWeight.Bold
        )

        if (menuItems.isEmpty()) {
            Text(
                v15Text(
                    "এখনো কোনো মেনু আইটেম নেই।",
                    "No menu item yet."
                )
            )
        }

        menuItems.forEach { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selectedMenu =
                            item
                    }
            ) {
                Column(
                    modifier =
                        Modifier.padding(11.dp),
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
                            item.name,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            foodMoney(
                                item.sellingPrice
                            )
                        )
                    }

                    if (
                        item.category.isNotBlank()
                    ) {
                        Text(item.category)
                    }

                    Text(
                        if (item.active) {
                            v15Text(
                                "সক্রিয়",
                                "Active"
                            )
                        } else {
                            v15Text(
                                "বন্ধ",
                                "Inactive"
                            )
                        }
                    )
                }
            }
        }

        Text(
            if (isCatering) {
                v15Text(
                    "ইভেন্ট / অর্ডার",
                    "Events / Orders"
                )
            } else {
                v15Text(
                    "অর্ডার",
                    "Orders"
                )
            },
            fontWeight =
                FontWeight.Bold
        )

        if (orders.isEmpty()) {
            Text(
                v15Text(
                    "এখনো কোনো অর্ডার নেই।",
                    "No orders yet."
                )
            )
        }

        orders.forEach { order ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selectedOrder =
                            order
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
                            order.orderNo,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            foodStatusLabel(
                                order.status
                            )
                        )
                    }

                    Text(
                        foodOrderTypeLabel(
                            order.orderType
                        )
                    )

                    if (
                        order.customerName
                            .isNotBlank()
                    ) {
                        Text(order.customerName)
                    }

                    if (
                        order.tableOrReference
                            .isNotBlank()
                    ) {
                        Text(
                            order.tableOrReference
                        )
                    }

                    Text(
                        v15Text(
                            "বিল: ${foodMoney(order.totalAmount)} • পরিশোধ: ${foodMoney(order.totalPaid)} • বাকি: ${foodMoney(order.dueAmount.coerceAtLeast(0.0))}",
                            "Bill: ${foodMoney(order.totalAmount)} • Paid: ${foodMoney(order.totalPaid)} • Due: ${foodMoney(order.dueAmount.coerceAtLeast(0.0))}"
                        )
                    )

                    if (
                        order.status ==
                        "COMPLETED"
                    ) {
                        Text(
                            v15Text(
                                "খরচ: ${foodMoney(order.totalCost)} • গ্রস প্রফিট: ${foodMoney(order.grossProfit)}",
                                "Cost: ${foodMoney(order.totalCost)} • Gross profit: ${foodMoney(order.grossProfit)}"
                            )
                        )
                    }
                }
            }
        }
    }

    if (showAddMenu) {
        AddFoodMenuDialog(
            onDismiss = {
                showAddMenu = false
            },
            onSave = {
                    name,
                    category,
                    price,
                    note ->

                vm.addMenuItem(
                    name =
                        name,
                    category =
                        category,
                    sellingPrice =
                        price,
                    note =
                        note
                ) { success ->
                    if (success) {
                        showAddMenu =
                            false
                    }
                }
            }
        )
    }

    if (showAddOrder) {
        AddFoodOrderDialog(
            menuItems =
                menuItems.filter {
                    it.active
                },
            isCatering =
                isCatering,
            onDismiss = {
                showAddOrder =
                    false
            },
            onSave = {
                    orderNo,
                    orderType,
                    customer,
                    phone,
                    reference,
                    eventDate,
                    guestCount,
                    lines,
                    payment,
                    note ->

                vm.createOrder(
                    orderNo =
                        orderNo,
                    orderType =
                        orderType,
                    customerName =
                        customer,
                    phone =
                        phone,
                    tableOrReference =
                        reference,
                    eventDate =
                        eventDate,
                    guestCount =
                        guestCount,
                    lines =
                        lines,
                    initialPayment =
                        payment,
                    note =
                        note
                ) { success ->
                    if (success) {
                        showAddOrder =
                            false
                    }
                }
            }
        )
    }
}

@Composable
private fun FoodMenuRecipeScreen(
    menu: FoodMenuItemEntity,
    products: List<ProductEntity>,
    viewModel: FoodServiceViewModel,
    canWrite: Boolean,
    onBack: () -> Unit
) {
    val ingredients by
        viewModel.observeRecipeIngredients(
            menu.id
        ).collectAsState(
            initial = emptyList()
        )

    var productId by remember {
        mutableStateOf<Long?>(null)
    }

    var quantity by remember {
        mutableStateOf("")
    }

    var message by remember {
        mutableStateOf("")
    }

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
            onClick =
                onBack
        ) {
            Text(
                v15Text(
                    "← মেনু তালিকা",
                    "← Menu list"
                )
            )
        }

        Text(
            menu.name,
            style =
                MaterialTheme.typography.headlineSmall,
            fontWeight =
                FontWeight.ExtraBold
        )

        Text(
            v15Text(
                "বিক্রয় মূল্য: ${foodMoney(menu.sellingPrice)}",
                "Selling price: ${foodMoney(menu.sellingPrice)}"
            )
        )

        Text(
            v15Text(
                "প্রতি ১টি মেনু আইটেম তৈরিতে যত stock লাগে সেটি recipe-তে দিন।",
                "Set the inventory quantity needed to prepare one menu item."
            )
        )

        if (canWrite) {
            OutlinedButton(
                onClick = {
                    viewModel.setMenuItemActive(
                        menuItemId =
                            menu.id,
                        active =
                            !menu.active
                    )
                    onBack()
                },
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Text(
                    if (menu.active) {
                        v15Text(
                            "মেনু বন্ধ করুন",
                            "Deactivate menu item"
                        )
                    } else {
                        v15Text(
                            "মেনু চালু করুন",
                            "Activate menu item"
                        )
                    }
                )
            }
        }

        Text(
            v15Text(
                "রেসিপির উপকরণ",
                "Recipe ingredients"
            ),
            fontWeight =
                FontWeight.Bold
        )

        if (ingredients.isEmpty()) {
            Text(
                v15Text(
                    "এখনো recipe ingredient যোগ করা হয়নি। এই মেনু দিয়ে order complete করা যাবে না।",
                    "No recipe ingredient yet. Orders containing this item cannot be completed."
                )
            )
        }

        ingredients.forEach { item ->
            Card(
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier =
                        Modifier.padding(10.dp)
                ) {
                    Text(
                        item.ingredientNameSnapshot,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        v15Text(
                            "প্রতি আইটেম: ${item.quantityPerItem} ${item.unitSnapshot}",
                            "Per item: ${item.quantityPerItem} ${item.unitSnapshot}"
                        )
                    )
                }
            }
        }

        if (canWrite) {
            Text(
                v15Text(
                    "উপকরণ যোগ করুন",
                    "Add ingredient"
                ),
                fontWeight =
                    FontWeight.Bold
            )

            if (products.isEmpty()) {
                Text(
                    v15Text(
                        "আগে Inventory থেকে ingredient product তৈরি করুন।",
                        "Create ingredient products in Inventory first."
                    )
                )
            }

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
                            "✓ ${product.name} (${product.unit})"
                        } else {
                            "${product.name} (${product.unit})"
                        }
                    )
                }
            }

            FoodField(
                value =
                    quantity,
                onChange = {
                    quantity = it
                },
                label =
                    v15Text(
                        "প্রতি মেনুতে প্রয়োজনীয় পরিমাণ",
                        "Quantity needed per menu item"
                    )
            )

            Button(
                enabled =
                    productId != null &&
                    (
                        quantity
                            .toIntOrNull()
                            ?: 0
                    ) > 0,
                onClick = {
                    viewModel.addRecipeIngredient(
                        menuItemId =
                            menu.id,
                        productId =
                            productId!!,
                        quantityPerItem =
                            quantity
                                .toIntOrNull()
                                ?: 0
                    ) { success ->
                        if (success) {
                            productId = null
                            quantity = ""

                            message =
                                v15Text(
                                    "উপকরণ যোগ হয়েছে।",
                                    "Ingredient added."
                                )
                        } else {
                            message =
                                v15Text(
                                    "উপকরণ যোগ করা যায়নি।",
                                    "Could not add ingredient."
                                )
                        }
                    }
                },
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Text(
                    v15Text(
                        "Recipe-তে যোগ করুন",
                        "Add to recipe"
                    )
                )
            }
        }

        if (message.isNotBlank()) {
            Text(message)
        }
    }
}

@Composable
private fun FoodOrderDetailsScreen(
    order: FoodOrderSummary,
    viewModel: FoodServiceViewModel,
    canWrite: Boolean,
    onBack: () -> Unit
) {
    val lines by
        viewModel.observeOrderLines(
            order.orderId
        ).collectAsState(
            initial = emptyList()
        )

    val payments by
        viewModel.observePayments(
            order.orderId
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
            ).coerceAtLeast(
                0.0
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
            onClick =
                onBack
        ) {
            Text(
                v15Text(
                    "← অর্ডার তালিকা",
                    "← Order list"
                )
            )
        }

        Text(
            order.orderNo,
            style =
                MaterialTheme.typography.headlineSmall,
            fontWeight =
                FontWeight.ExtraBold
        )

        Text(
            foodOrderTypeLabel(
                order.orderType
            )
        )

        Text(
            v15Text(
                "স্ট্যাটাস: ${foodStatusLabel(order.status)}",
                "Status: ${foodStatusLabel(order.status)}"
            )
        )

        if (
            order.customerName
                .isNotBlank()
        ) {
            Text(order.customerName)
        }

        if (
            order.tableOrReference
                .isNotBlank()
        ) {
            Text(
                order.tableOrReference
            )
        }

        if (
            order.eventDate != null
        ) {
            Text(
                v15Text(
                    "ইভেন্ট: ${foodDate(order.eventDate)}",
                    "Event: ${foodDate(order.eventDate)}"
                )
            )
        }

        if (
            order.guestCount > 0
        ) {
            Text(
                v15Text(
                    "অতিথি: ${order.guestCount}",
                    "Guests: ${order.guestCount}"
                )
            )
        }

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            FoodMetric(
                title =
                    v15Text(
                        "বিল",
                        "Bill"
                    ),
                value =
                    foodMoney(
                        liveTotal
                    ),
                modifier =
                    Modifier.weight(1f)
            )

            FoodMetric(
                title =
                    v15Text(
                        "বাকি",
                        "Due"
                    ),
                value =
                    foodMoney(
                        liveDue
                    ),
                modifier =
                    Modifier.weight(1f)
            )
        }

        if (
            order.status ==
            "COMPLETED"
        ) {
            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                FoodMetric(
                    title =
                        v15Text(
                            "উপকরণ খরচ",
                            "Ingredient cost"
                        ),
                    value =
                        foodMoney(
                            order.totalCost
                        ),
                    modifier =
                        Modifier.weight(1f)
                )

                FoodMetric(
                    title =
                        v15Text(
                            "গ্রস প্রফিট",
                            "Gross profit"
                        ),
                    value =
                        foodMoney(
                            order.grossProfit
                        ),
                    modifier =
                        Modifier.weight(1f)
                )
            }
        }

        Text(
            v15Text(
                "অর্ডার আইটেম",
                "Order items"
            ),
            fontWeight =
                FontWeight.Bold
        )

        lines.forEach { line ->
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
                    Text(
                        "${line.menuItemNameSnapshot} × ${line.quantity}"
                    )

                    Text(
                        foodMoney(
                            line.lineTotal
                        )
                    )
                }
            }
        }

        if (
            canWrite &&
            order.status ==
            "CONFIRMED"
        ) {
            Button(
                onClick = {
                    viewModel.completeOrder(
                        orderId =
                            order.orderId
                    ) { success ->
                        if (success) {
                            onBack()
                        } else {
                            message =
                                v15Text(
                                    "অর্ডার complete করা যায়নি। Recipe বা ingredient stock পরীক্ষা করুন।",
                                    "Could not complete order. Check recipes and ingredient stock."
                                )
                        }
                    }
                },
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Text(
                    v15Text(
                        "অর্ডার সম্পন্ন করুন ও স্টক কাটুন",
                        "Complete order & deduct stock"
                    )
                )
            }
        }

        if (
            canWrite &&
            liveDue > 0.009 &&
            order.status !=
            "CANCELLED"
        ) {
            Text(
                v15Text(
                    "পেমেন্ট যোগ করুন",
                    "Add payment"
                ),
                fontWeight =
                    FontWeight.Bold
            )

            FoodField(
                value =
                    paymentAmount,
                onChange = {
                    paymentAmount = it
                },
                label =
                    v15Text(
                        "টাকার পরিমাণ",
                        "Amount"
                    )
            )

            FoodField(
                value =
                    paymentNote,
                onChange = {
                    paymentNote = it
                },
                label =
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
                    viewModel.addPayment(
                        orderId =
                            order.orderId,
                        amount =
                            paymentAmount
                                .toDoubleOrNull()
                                ?: 0.0,
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
                                    "পেমেন্ট যোগ করা যায়নি। বাকি টাকার বেশি payment দেওয়া যাবে না।",
                                    "Could not add payment. Payment cannot exceed the due amount."
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
                    "No payment yet."
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
                        Modifier.padding(10.dp)
                ) {
                    Text(
                        foodMoney(
                            payment.amount
                        ),
                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        foodDateTime(
                            payment.paidAt
                        )
                    )

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
private fun AddFoodMenuDialog(
    onDismiss: () -> Unit,
    onSave: (
        String,
        String,
        Double,
        String
    ) -> Unit
) {
    var name by remember {
        mutableStateOf("")
    }

    var category by remember {
        mutableStateOf("")
    }

    var price by remember {
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
                    "নতুন মেনু আইটেম",
                    "New menu item"
                )
            )
        },
        text = {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(7.dp)
            ) {
                FoodField(
                    value =
                        name,
                    onChange = {
                        name = it
                    },
                    label =
                        v15Text(
                            "মেনুর নাম",
                            "Menu name"
                        )
                )

                FoodField(
                    value =
                        category,
                    onChange = {
                        category = it
                    },
                    label =
                        v15Text(
                            "ক্যাটাগরি",
                            "Category"
                        )
                )

                FoodField(
                    value =
                        price,
                    onChange = {
                        price = it
                    },
                    label =
                        v15Text(
                            "বিক্রয় মূল্য",
                            "Selling price"
                        )
                )

                FoodField(
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
            }
        },
        confirmButton = {
            TextButton(
                enabled =
                    name.isNotBlank() &&
                    (
                        price
                            .toDoubleOrNull()
                            ?: -1.0
                    ) >= 0,
                onClick = {
                    onSave(
                        name,
                        category,
                        price
                            .toDoubleOrNull()
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
private fun AddFoodOrderDialog(
    menuItems: List<FoodMenuItemEntity>,
    isCatering: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        String,
        String,
        String,
        String,
        String,
        Long?,
        Int,
        List<FoodOrderLineInput>,
        Double,
        String
    ) -> Unit
) {
    var orderNo by remember {
        mutableStateOf("")
    }

    var orderType by remember {
        mutableStateOf(
            if (isCatering) {
                "CATERING"
            } else {
                "DINE_IN"
            }
        )
    }

    var customer by remember {
        mutableStateOf("")
    }

    var phone by remember {
        mutableStateOf("")
    }

    var reference by remember {
        mutableStateOf("")
    }

    var eventDateText by remember {
        mutableStateOf("")
    }

    var guestCount by remember {
        mutableStateOf("")
    }

    var selectedMenuId by remember {
        mutableStateOf<Long?>(null)
    }

    var quantity by remember {
        mutableStateOf("")
    }

    val lines =
        remember {
            mutableStateListOf<
                FoodOrderLineInput
                >()
        }

    var payment by remember {
        mutableStateOf("")
    }

    var note by remember {
        mutableStateOf("")
    }

    val menuById =
        remember(menuItems) {
            menuItems.associateBy {
                it.id
            }
        }

    AlertDialog(
        onDismissRequest =
            onDismiss,
        title = {
            Text(
                if (isCatering) {
                    v15Text(
                        "নতুন ক্যাটারিং অর্ডার",
                        "New catering order"
                    )
                } else {
                    v15Text(
                        "নতুন অর্ডার",
                        "New order"
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
                FoodField(
                    value =
                        orderNo,
                    onChange = {
                        orderNo = it
                    },
                    label =
                        v15Text(
                            "অর্ডার নম্বর (ফাঁকা রাখা যাবে)",
                            "Order number (optional)"
                        )
                )

                Text(
                    v15Text(
                        "অর্ডারের ধরন",
                        "Order type"
                    ),
                    fontWeight =
                        FontWeight.Bold
                )

                val orderTypes =
                    if (isCatering) {
                        listOf(
                            "CATERING"
                        )
                    } else {
                        listOf(
                            "DINE_IN",
                            "TAKEAWAY",
                            "DELIVERY"
                        )
                    }

                orderTypes.forEach { type ->
                    OutlinedButton(
                        onClick = {
                            orderType = type
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (
                                orderType ==
                                type
                            ) {
                                "✓ ${foodOrderTypeLabel(type)}"
                            } else {
                                foodOrderTypeLabel(
                                    type
                                )
                            }
                        )
                    }
                }

                FoodField(
                    value =
                        customer,
                    onChange = {
                        customer = it
                    },
                    label =
                        v15Text(
                            "কাস্টমারের নাম",
                            "Customer name"
                        )
                )

                FoodField(
                    value =
                        phone,
                    onChange = {
                        phone = it
                    },
                    label =
                        v15Text(
                            "ফোন",
                            "Phone"
                        )
                )

                FoodField(
                    value =
                        reference,
                    onChange = {
                        reference = it
                    },
                    label =
                        if (isCatering) {
                            v15Text(
                                "ভেন্যু / ইভেন্ট রেফারেন্স",
                                "Venue / event reference"
                            )
                        } else {
                            v15Text(
                                "টেবিল / ডেলিভারি রেফারেন্স",
                                "Table / delivery reference"
                            )
                        }
                )

                if (isCatering) {
                    FoodField(
                        value =
                            eventDateText,
                        onChange = {
                            eventDateText = it
                        },
                        label =
                            v15Text(
                                "ইভেন্ট তারিখ YYYY-MM-DD",
                                "Event date YYYY-MM-DD"
                            )
                    )

                    FoodField(
                        value =
                            guestCount,
                        onChange = {
                            guestCount = it
                        },
                        label =
                            v15Text(
                                "অতিথির সংখ্যা",
                                "Guest count"
                            )
                    )
                }

                Text(
                    v15Text(
                        "মেনু আইটেম",
                        "Menu items"
                    ),
                    fontWeight =
                        FontWeight.Bold
                )

                menuItems.forEach { item ->
                    OutlinedButton(
                        onClick = {
                            selectedMenuId =
                                item.id
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (
                                selectedMenuId ==
                                item.id
                            ) {
                                "✓ ${item.name} • ${foodMoney(item.sellingPrice)}"
                            } else {
                                "${item.name} • ${foodMoney(item.sellingPrice)}"
                            }
                        )
                    }
                }

                FoodField(
                    value =
                        quantity,
                    onChange = {
                        quantity = it
                    },
                    label =
                        v15Text(
                            "পরিমাণ",
                            "Quantity"
                        )
                )

                OutlinedButton(
                    enabled =
                        selectedMenuId != null &&
                        (
                            quantity
                                .toIntOrNull()
                                ?: 0
                        ) > 0,
                    onClick = {
                        lines.add(
                            FoodOrderLineInput(
                                menuItemId =
                                    selectedMenuId!!,
                                quantity =
                                    quantity
                                        .toIntOrNull()
                                        ?: 0
                            )
                        )

                        selectedMenuId =
                            null

                        quantity = ""
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text(
                        v15Text(
                            "+ অর্ডারে যোগ করুন",
                            "+ Add to order"
                        )
                    )
                }

                lines.forEachIndexed {
                        index,
                        line ->

                    val menu =
                        menuById[
                            line.menuItemId
                        ]

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${menu?.name ?: line.menuItemId} × ${line.quantity}"
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

                FoodField(
                    value =
                        payment,
                    onChange = {
                        payment = it
                    },
                    label =
                        v15Text(
                            "প্রাথমিক পেমেন্ট",
                            "Initial payment"
                        )
                )

                FoodField(
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
            }
        },
        confirmButton = {
            TextButton(
                enabled =
                    lines.isNotEmpty(),
                onClick = {
                    onSave(
                        orderNo,
                        orderType,
                        customer,
                        phone,
                        reference,
                        parseFoodDate(
                            eventDateText
                        ),
                        guestCount
                            .toIntOrNull()
                            ?: 0,
                        lines.toList(),
                        payment
                            .toDoubleOrNull()
                            ?: 0.0,
                        note
                    )
                }
            ) {
                Text(
                    v15Text(
                        "অর্ডার তৈরি করুন",
                        "Create order"
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
private fun FoodMetric(
    title: String,
    value: String,
    modifier: Modifier
) {
    Card(
        modifier =
            modifier
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
private fun FoodField(
    value: String,
    onChange: (String) -> Unit,
    label: String
) {
    OutlinedTextField(
        value =
            value,
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

private fun foodOrderTypeLabel(
    value: String
): String =
    when (value) {
        "DINE_IN" ->
            v15Text(
                "ডাইন-ইন",
                "Dine-in"
            )

        "TAKEAWAY" ->
            v15Text(
                "টেকঅ্যাওয়ে",
                "Takeaway"
            )

        "DELIVERY" ->
            v15Text(
                "ডেলিভারি",
                "Delivery"
            )

        "CATERING" ->
            v15Text(
                "ক্যাটারিং",
                "Catering"
            )

        else ->
            value
    }

private fun foodStatusLabel(
    value: String
): String =
    when (value) {
        "COMPLETED" ->
            v15Text(
                "সম্পন্ন",
                "Completed"
            )

        "CONFIRMED" ->
            v15Text(
                "কনফার্মড",
                "Confirmed"
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

private fun foodMoney(
    value: Double
): String =
    String.format(
        Locale.US,
        "%.2f",
        value
    )

private fun foodDate(
    value: Long
): String =
    SimpleDateFormat(
        "dd MMM yyyy",
        Locale.getDefault()
    ).format(
        Date(value)
    )

private fun foodDateTime(
    value: Long
): String =
    SimpleDateFormat(
        "dd MMM yyyy, hh:mm a",
        Locale.getDefault()
    ).format(
        Date(value)
    )

private fun parseFoodDate(
    value: String
): Long? {
    if (value.isBlank()) {
        return null
    }

    return runCatching {
        SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        ).apply {
            isLenient = false
        }.parse(
            value.trim()
        )?.time
    }.getOrNull()
}
