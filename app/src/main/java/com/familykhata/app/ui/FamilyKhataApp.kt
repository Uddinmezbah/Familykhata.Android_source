package com.familykhata.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.familykhata.app.FamilyKhataViewModel
import com.familykhata.app.data.BakiEntryEntity
import com.familykhata.app.data.BakiPersonSummary
import com.familykhata.app.data.TransactionEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class Tab(val label: String) {
    DASHBOARD("হোম"),
    ADD("নতুন"),
    BAKI("বাকি"),
    HISTORY("হিসাব")
}

@Composable
fun FamilyKhataApp(viewModel: FamilyKhataViewModel) {
    var tab by remember { mutableStateOf(Tab.DASHBOARD) }
    var addTypePreset by remember { mutableStateOf("EXPENSE") }
    val workspace by viewModel.selectedWorkspace.collectAsState()

    HisabiKhataTheme {
        Scaffold(
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    Tab.entries.forEach { item ->
                        NavigationBarItem(
                            selected = tab == item,
                            onClick = { tab = item },
                            icon = {
                                Text(
                                    tabSymbol(item),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            label = { Text(tabLabel(item, workspace)) }
                        )
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                BrandHeader(workspace)
                Spacer(Modifier.height(10.dp))
                WorkspaceSwitcher(
                    selected = workspace,
                    onSelect = {
                        viewModel.selectWorkspace(it)
                        addTypePreset = "EXPENSE"
                        tab = Tab.DASHBOARD
                    }
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (tab) {
                        Tab.DASHBOARD -> DashboardScreen(
                            viewModel = viewModel,
                            workspace = workspace,
                            onCashIn = {
                                addTypePreset = "INCOME"
                                tab = Tab.ADD
                            },
                            onCashOut = {
                                addTypePreset = "EXPENSE"
                                tab = Tab.ADD
                            },
                            onLedger = { tab = Tab.BAKI },
                            onHistory = { tab = Tab.HISTORY }
                        )
                        Tab.ADD -> AddTransactionScreen(
                            viewModel = viewModel,
                            workspace = workspace,
                            initialType = addTypePreset
                        )
                        Tab.BAKI -> BakiScreen(viewModel, workspace)
                        Tab.HISTORY -> HistoryScreen(viewModel, workspace)
                    }
                }
            }
        }
    }
}

@Composable
private fun BrandHeader(workspace: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 15.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "হিসাবী খাতা",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "নিজের, পরিবারের ও দোকান/প্রতিষ্ঠানের হিসাব এক জায়গায়",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        workspaceLabel(workspace),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            Text(
                "v0.9 • Business Foundation",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun WorkspaceSwitcher(
    selected: String,
    onSelect: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "কোন হিসাব দেখবেন?",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                WorkspaceButton(
                    label = "নিজের",
                    value = "PERSONAL",
                    selected = selected,
                    modifier = Modifier.weight(1f),
                    onSelect = onSelect
                )
                WorkspaceButton(
                    label = "পরিবার",
                    value = "FAMILY",
                    selected = selected,
                    modifier = Modifier.weight(1f),
                    onSelect = onSelect
                )
                WorkspaceButton(
                    label = "দোকান/\nপ্রতিষ্ঠান",
                    value = "SHOP",
                    selected = selected,
                    modifier = Modifier.weight(1f),
                    onSelect = onSelect
                )
            }
        }
    }
}

@Composable
private fun WorkspaceButton(
    label: String,
    value: String,
    selected: String,
    modifier: Modifier,
    onSelect: (String) -> Unit
) {
    val contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
    if (selected == value) {
        Button(
            onClick = { onSelect(value) },
            modifier = modifier,
            contentPadding = contentPadding
        ) { Text(label, style = MaterialTheme.typography.labelSmall) }
    } else {
        OutlinedButton(
            onClick = { onSelect(value) },
            modifier = modifier,
            contentPadding = contentPadding
        ) { Text(label, style = MaterialTheme.typography.labelSmall) }
    }
}

@Composable
private fun DashboardScreen(
    viewModel: FamilyKhataViewModel,
    workspace: String,
    onCashIn: () -> Unit,
    onCashOut: () -> Unit,
    onLedger: () -> Unit,
    onHistory: () -> Unit
) {
    val totals by viewModel.totals.collectAsState()
    val bakiPeople by viewModel.bakiPeople.collectAsState()
    val receivable = bakiPeople.filter { it.balance > 0 }.sumOf { it.balance }
    val payable = -bakiPeople.filter { it.balance < 0 }.sumOf { it.balance }
    val balance = totals.income - totals.expense

    if (workspace == "SHOP") {
        BusinessDashboard(
            balance = balance,
            income = totals.income,
            expense = totals.expense,
            receivable = receivable,
            payable = payable,
            ledgerCount = bakiPeople.size,
            onCashIn = onCashIn,
            onCashOut = onCashOut,
            onLedger = onLedger,
            onHistory = onHistory
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primary
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "বর্তমান ব্যালেন্স",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    "৳ ${money(balance)}",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    workspaceSummary(workspace),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                title = "মোট আয়",
                amount = totals.income,
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
            MetricCard(
                title = "মোট খরচ",
                amount = totals.expense,
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                title = "পাবো",
                amount = receivable,
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
            MetricCard(
                title = "দেবো",
                amount = payable,
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text("হিসাব গুছিয়ে রাখুন", fontWeight = FontWeight.Bold)
                Text(
                    "নিচের “নতুন” থেকে আয়-খরচ এবং “বাকি” থেকে দেনা-পাওনা যোগ করুন।",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun BusinessDashboard(
    balance: Double,
    income: Double,
    expense: Double,
    receivable: Double,
    payable: Double,
    ledgerCount: Int,
    onCashIn: () -> Unit,
    onCashOut: () -> Unit,
    onLedger: () -> Unit,
    onHistory: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primary
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    "ব্যবসার ক্যাশ ব্যালেন্স",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    "৳ ${money(balance)}",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    "দোকান/প্রতিষ্ঠানের আয়-খরচের বর্তমান হিসাব",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                title = "মোট ক্যাশ ইন",
                amount = income,
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
            MetricCard(
                title = "মোট ক্যাশ আউট",
                amount = expense,
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                title = "পাবো",
                amount = receivable,
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
            MetricCard(
                title = "দেবো",
                amount = payable,
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        Text(
            "দ্রুত কাজ",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BusinessActionCard(
                symbol = "＋",
                title = "ক্যাশ ইন",
                subtitle = "আয় বা টাকা জমা",
                modifier = Modifier.weight(1f),
                onClick = onCashIn
            )
            BusinessActionCard(
                symbol = "−",
                title = "ক্যাশ আউট",
                subtitle = "খরচ বা টাকা বের",
                modifier = Modifier.weight(1f),
                onClick = onCashOut
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BusinessActionCard(
                symbol = "৳",
                title = "খাতা",
                subtitle = "কাস্টমার/সাপ্লায়ার",
                modifier = Modifier.weight(1f),
                onClick = onLedger
            )
            BusinessActionCard(
                symbol = "≡",
                title = "লেনদেন",
                subtitle = "সব ক্যাশ ইতিহাস",
                modifier = Modifier.weight(1f),
                onClick = onHistory
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("ব্যবসার খাতা", fontWeight = FontWeight.Bold)
                Text(
                    "কাস্টমার/সাপ্লায়ার: $ledgerCount জন • পাবো ৳ ${money(receivable)} • দেবো ৳ ${money(payable)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "পরবর্তী ধাপে পণ্য-স্টক, ক্রয়-বিক্রি, ইনভয়েস/চালান ও রিপোর্ট যোগ হবে।",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun BusinessActionCard(
    symbol: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    symbol,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(title, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    amount: Double,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = containerColor
    ) {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(
                "৳ ${money(amount)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AddTransactionScreen(
    viewModel: FamilyKhataViewModel,
    workspace: String,
    initialType: String
) {
    val isBusiness = workspace == "SHOP"
    var type by remember(initialType) { mutableStateOf(initialType) }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            if (isBusiness) "ক্যাশ লেনদেন" else "নতুন আয়/খরচ",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        if (isBusiness) {
            Text(
                "দোকান/প্রতিষ্ঠানের টাকা আসা বা বের হওয়ার হিসাব যোগ করুন।",
                style = MaterialTheme.typography.bodySmall
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val expenseLabel = if (isBusiness) "ক্যাশ আউট" else "খরচ"
            val incomeLabel = if (isBusiness) "ক্যাশ ইন" else "আয়"
            if (type == "EXPENSE") Button(onClick = { type = "EXPENSE" }) { Text(expenseLabel) }
            else OutlinedButton(onClick = { type = "EXPENSE" }) { Text(expenseLabel) }
            if (type == "INCOME") Button(onClick = { type = "INCOME" }) { Text(incomeLabel) }
            else OutlinedButton(onClick = { type = "INCOME" }) { Text(incomeLabel) }
        }

        OutlinedTextField(
            amount,
            { amount = it },
            label = { Text("টাকার পরিমাণ") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            category,
            { category = it },
            label = { Text(if (isBusiness) "ক্যাটাগরি / খাত" else "ক্যাটাগরি") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            note,
            { note = it },
            label = { Text("নোট") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                val value = parseAmount(amount)
                if (value == null) {
                    error = "সঠিক টাকার পরিমাণ লিখুন"
                } else {
                    viewModel.addTransaction(type, value, category, note)
                    amount = ""
                    category = ""
                    note = ""
                    error = null
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (isBusiness) "ক্যাশ লেনদেন সেভ করুন" else "সেভ করুন") }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun HistoryScreen(viewModel: FamilyKhataViewModel, workspace: String) {
    val transactions by viewModel.transactions.collectAsState()
    val isBusiness = workspace == "SHOP"

    if (transactions.isEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                if (isBusiness) "ব্যবসার লেনদেন" else "আয়-খরচের ইতিহাস",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(if (isBusiness) "এখনও কোনো ক্যাশ লেনদেন যোগ করা হয়নি।" else "এখনও কোনো আয়/খরচ যোগ করা হয়নি।")
        }
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text(
                if (isBusiness) "ব্যবসার লেনদেন" else "আয়-খরচের ইতিহাস",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        items(transactions, key = { it.id }) { item ->
            TransactionRow(
                item = item,
                isBusiness = isBusiness,
                onDelete = { viewModel.deleteTransaction(item) }
            )
        }
    }
}

@Composable
private fun TransactionRow(
    item: TransactionEntity,
    isBusiness: Boolean,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    if (isBusiness) {
                        if (item.type == "INCOME") "ক্যাশ ইন" else "ক্যাশ আউট"
                    } else {
                        if (item.type == "INCOME") "আয়" else "খরচ"
                    },
                    fontWeight = FontWeight.Bold
                )
                Text("৳ ${money(item.amount)}", fontWeight = FontWeight.Bold)
            }
            Text(item.category)
            if (item.note.isNotBlank()) Text(item.note, style = MaterialTheme.typography.bodySmall)
            Text(formatDate(item.createdAt), style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onDelete) { Text("মুছুন") }
        }
    }
}

@Composable
private fun BakiScreen(viewModel: FamilyKhataViewModel, workspace: String) {
    val people by viewModel.bakiPeople.collectAsState()
    var selectedId by remember { mutableStateOf<Long?>(null) }

    val selected = selectedId?.let { id -> people.firstOrNull { it.id == id } }

    if (selected == null) {
        BakiPeopleScreen(
            people = people,
            viewModel = viewModel,
            workspace = workspace,
            onSelect = { selectedId = it.id }
        )
    } else {
        BakiEntryScreen(
            person = selected,
            viewModel = viewModel,
            workspace = workspace,
            onBack = { selectedId = null }
        )
    }
}

@Composable
private fun BakiPeopleScreen(
    people: List<BakiPersonSummary>,
    viewModel: FamilyKhataViewModel,
    workspace: String,
    onSelect: (BakiPersonSummary) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    val personLabel = if (workspace == "SHOP") "কাস্টমার/সাপ্লায়ার" else "ব্যক্তি"
    val sectionTitle = if (workspace == "SHOP") "কাস্টমার/সাপ্লায়ার খাতা" else "বাকি/পাওনা"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("নতুন $personLabel", fontWeight = FontWeight.Bold)
        OutlinedTextField(name, { name = it }, label = { Text("নাম") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(phone, { phone = it }, label = { Text("ফোন (ঐচ্ছিক)") }, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = {
                viewModel.addBakiPerson(name, phone)
                name = ""
                phone = ""
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("$personLabel যোগ করুন") }

        Spacer(Modifier.height(4.dp))
        Text(sectionTitle, fontWeight = FontWeight.Bold)

        if (people.isEmpty()) {
            Text("প্রথমে একজন $personLabel যোগ করুন।")
        } else {
            people.forEach { person ->
                Card(
                    onClick = { onSelect(person) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(person.name, fontWeight = FontWeight.Bold)
                        Text(
                            when {
                                person.balance > 0 -> "পাবো: ৳ ${money(person.balance)}"
                                person.balance < 0 -> "দেবো: ৳ ${money(-person.balance)}"
                                else -> "হিসাব সমান"
                            }
                        )
                        Text(
                            if (workspace == "SHOP") "খাতা ও লেনদেন দেখতে চাপুন" else "হিসাব ও ইতিহাস দেখতে চাপুন",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BakiEntryScreen(
    person: BakiPersonSummary,
    viewModel: FamilyKhataViewModel,
    workspace: String,
    onBack: () -> Unit
) {
    val entriesFlow = remember(person.id) { viewModel.observeBakiEntries(person.id) }
    val entries by entriesFlow.collectAsState(initial = emptyList())
    var action by remember { mutableStateOf("GAVE") }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<BakiEntryEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TextButton(onClick = onBack) {
            Text(if (workspace == "SHOP") "← কাস্টমার/সাপ্লায়ার তালিকায় ফিরুন" else "← ব্যক্তি তালিকায় ফিরুন")
        }

        Text(person.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            when {
                person.balance > 0 -> "বর্তমানে পাবো: ৳ ${money(person.balance)}"
                person.balance < 0 -> "বর্তমানে দেবো: ৳ ${money(-person.balance)}"
                else -> "বর্তমান হিসাব সমান"
            },
            style = MaterialTheme.typography.titleMedium
        )

        Text("নতুন এন্ট্রি", fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ActionButton("দিলাম", "GAVE", action) { action = "GAVE" }
            ActionButton("ফেরত পেলাম", "RECEIVED_BACK", action) { action = "RECEIVED_BACK" }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ActionButton("নিলাম", "TOOK", action) { action = "TOOK" }
            ActionButton("ফেরত দিলাম", "PAID_BACK", action) { action = "PAID_BACK" }
        }

        OutlinedTextField(
            amount,
            { amount = it },
            label = { Text("টাকার পরিমাণ") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            note,
            { note = it },
            label = { Text("নোট") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                val value = parseAmount(amount)
                if (value == null) {
                    error = "সঠিক টাকার পরিমাণ লিখুন"
                } else {
                    viewModel.addBakiEntry(person.id, action, value, note)
                    amount = ""
                    note = ""
                    error = null
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (workspace == "SHOP") "খাতার এন্ট্রি সেভ করুন" else "বাকি এন্ট্রি সেভ করুন") }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Spacer(Modifier.height(8.dp))
        Text("লেনদেনের ইতিহাস (${entries.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        if (entries.isEmpty()) {
            Text(
                if (workspace == "SHOP") "এই খাতায় এখনো কোনো লেনদেন নেই।"
                else "এই ব্যক্তির কোনো বাকি লেনদেন এখনো নেই।"
            )
        } else {
            entries.forEach { entry ->
                BakiHistoryCard(
                    item = entry,
                    onDelete = { pendingDelete = entry }
                )
            }
        }
    }

    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("এন্ট্রি মুছবেন?") },
            text = {
                Text(
                    "${actionLabel(entry.action)} — ৳ ${money(entry.amount)}\n" +
                        "মুছে দিলে ব্যক্তির বর্তমান হিসাবও স্বয়ংক্রিয়ভাবে বদলে যাবে।"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteBakiEntry(entry)
                        pendingDelete = null
                    }
                ) { Text("মুছুন") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("বাতিল") }
            }
        )
    }
}

@Composable
private fun BakiHistoryCard(item: BakiEntryEntity, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(actionLabel(item.action), fontWeight = FontWeight.Bold)
                Text("৳ ${money(item.amount)}", fontWeight = FontWeight.Bold)
            }
            if (item.note.isNotBlank()) {
                Text(item.note)
            }
            Text(formatDate(item.createdAt), style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onDelete) { Text("এন্ট্রি মুছুন") }
        }
    }
}

@Composable
private fun ActionButton(label: String, value: String, selected: String, onClick: () -> Unit) {
    if (selected == value) Button(onClick = onClick) { Text(label) }
    else OutlinedButton(onClick = onClick) { Text(label) }
}

private fun workspaceLabel(workspace: String): String = when (workspace) {
    "PERSONAL" -> "নিজের"
    "SHOP" -> "দোকান/প্রতিষ্ঠান"
    else -> "পরিবার"
}

private fun workspaceSummary(workspace: String): String = when (workspace) {
    "PERSONAL" -> "আপনার ব্যক্তিগত আয়-খরচ ও দেনা-পাওনা"
    "SHOP" -> "দোকান/প্রতিষ্ঠানের আয়-খরচ ও দেনা-পাওনার হিসাব"
    else -> "পরিবারের সার্বিক আয়-খরচ ও দেনা-পাওনা"
}

private fun tabLabel(tab: Tab, workspace: String): String {
    if (workspace != "SHOP") return tab.label
    return when (tab) {
        Tab.DASHBOARD -> "হোম"
        Tab.ADD -> "ক্যাশ"
        Tab.BAKI -> "খাতা"
        Tab.HISTORY -> "লেনদেন"
    }
}

private fun tabSymbol(tab: Tab): String = when (tab) {
    Tab.DASHBOARD -> "⌂"
    Tab.ADD -> "＋"
    Tab.BAKI -> "৳"
    Tab.HISTORY -> "≡"
}

private fun actionLabel(action: String): String = when (action) {
    "GAVE" -> "দিলাম"
    "RECEIVED_BACK" -> "ফেরত পেলাম"
    "TOOK" -> "নিলাম"
    "PAID_BACK" -> "ফেরত দিলাম"
    else -> action
}

private fun parseAmount(input: String): Double? {
    val normalized = buildString {
        input.trim().forEach { char ->
            when (char) {
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
                ',', '৳', ' ' -> Unit
                else -> append(char)
            }
        }
    }
    return normalized.toDoubleOrNull()?.takeIf { it > 0.0 }
}

private fun money(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString()
    else String.format(Locale.US, "%.2f", value)

private fun formatDate(timestamp: Long): String =
    SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(timestamp))
