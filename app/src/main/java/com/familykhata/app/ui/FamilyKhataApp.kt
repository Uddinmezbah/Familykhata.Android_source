package com.familykhata.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.familykhata.app.FamilyKhataViewModel
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

    MaterialTheme {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    Tab.entries.forEach { item ->
                        NavigationBarItem(
                            selected = tab == item,
                            onClick = { tab = item },
                            icon = { Text(if (tab == item) "●" else "○") },
                            label = { Text(item.label) }
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
                Text("Family Khata", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (tab) {
                        Tab.DASHBOARD -> DashboardScreen(viewModel)
                        Tab.ADD -> AddTransactionScreen(viewModel)
                        Tab.BAKI -> BakiScreen(viewModel)
                        Tab.HISTORY -> HistoryScreen(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardScreen(viewModel: FamilyKhataViewModel) {
    val totals by viewModel.totals.collectAsState()
    val bakiPeople by viewModel.bakiPeople.collectAsState()
    val receivable = bakiPeople.filter { it.balance > 0 }.sumOf { it.balance }
    val payable = -bakiPeople.filter { it.balance < 0 }.sumOf { it.balance }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SummaryCard("মোট আয়", totals.income)
        SummaryCard("মোট খরচ", totals.expense)
        SummaryCard("বর্তমান ব্যালেন্স", totals.income - totals.expense)
        SummaryCard("পাবো", receivable)
        SummaryCard("দেবো", payable)
    }
}

@Composable
private fun SummaryCard(title: String, amount: Double) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text("৳ ${money(amount)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AddTransactionScreen(viewModel: FamilyKhataViewModel) {
    var type by remember { mutableStateOf("EXPENSE") }
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (type == "EXPENSE") Button(onClick = { type = "EXPENSE" }) { Text("খরচ") }
            else OutlinedButton(onClick = { type = "EXPENSE" }) { Text("খরচ") }
            if (type == "INCOME") Button(onClick = { type = "INCOME" }) { Text("আয়") }
            else OutlinedButton(onClick = { type = "INCOME" }) { Text("আয়") }
        }
        OutlinedTextField(amount, { amount = it }, label = { Text("টাকার পরিমাণ") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(category, { category = it }, label = { Text("ক্যাটাগরি") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(note, { note = it }, label = { Text("নোট") }, modifier = Modifier.fillMaxWidth())
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
        ) { Text("সেভ করুন") }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun HistoryScreen(viewModel: FamilyKhataViewModel) {
    val transactions by viewModel.transactions.collectAsState()
    if (transactions.isEmpty()) {
        Text("এখনও কোনো আয়/খরচ যোগ করা হয়নি।")
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(transactions, key = { it.id }) { item ->
            TransactionRow(item, onDelete = { viewModel.deleteTransaction(item) })
        }
    }
}

@Composable
private fun TransactionRow(item: TransactionEntity, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (item.type == "INCOME") "আয়" else "খরচ", fontWeight = FontWeight.Bold)
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
private fun BakiScreen(viewModel: FamilyKhataViewModel) {
    val people by viewModel.bakiPeople.collectAsState()
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<BakiPersonSummary?>(null) }
    var action by remember { mutableStateOf("GAVE") }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("নতুন ব্যক্তি", fontWeight = FontWeight.Bold)
        OutlinedTextField(name, { name = it }, label = { Text("নাম") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(phone, { phone = it }, label = { Text("ফোন (ঐচ্ছিক)") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            viewModel.addBakiPerson(name, phone)
            name = ""
            phone = ""
        }, modifier = Modifier.fillMaxWidth()) { Text("ব্যক্তি যোগ করুন") }

        Spacer(Modifier.height(4.dp))
        Text("বাকি/পাওনা", fontWeight = FontWeight.Bold)

        if (people.isEmpty()) {
            Text("প্রথমে একজন ব্যক্তি যোগ করুন।")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                people.forEach { person ->
                    val selectedNow = selected?.id == person.id
                    Card(
                        onClick = { selected = person },
                        modifier = Modifier.fillMaxWidth(),
                        colors = if (selectedNow) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        else CardDefaults.cardColors()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(person.name, fontWeight = FontWeight.Bold)
                            Text(
                                when {
                                    person.balance > 0 -> "পাবো: ৳ ${money(person.balance)}"
                                    person.balance < 0 -> "দেবো: ৳ ${money(-person.balance)}"
                                    else -> "হিসাব সমান"
                                }
                            )
                        }
                    }
                }
            }
        }

        selected?.let { person ->
            Text("${person.name}-এর নতুন এন্ট্রি", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ActionButton("দিলাম", "GAVE", action) { action = "GAVE" }
                ActionButton("ফেরত পেলাম", "RECEIVED_BACK", action) { action = "RECEIVED_BACK" }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ActionButton("নিলাম", "TOOK", action) { action = "TOOK" }
                ActionButton("ফেরত দিলাম", "PAID_BACK", action) { action = "PAID_BACK" }
            }
            OutlinedTextField(amount, { amount = it }, label = { Text("টাকার পরিমাণ") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(note, { note = it }, label = { Text("নোট") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = {
                val value = parseAmount(amount)
                if (value == null) {
                    error = "সঠিক টাকার পরিমাণ লিখুন"
                } else {
                    viewModel.addBakiEntry(person.id, action, value, note)
                    amount = ""
                    note = ""
                    error = null
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("বাকি এন্ট্রি সেভ") }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun ActionButton(label: String, value: String, selected: String, onClick: () -> Unit) {
    if (selected == value) Button(onClick = onClick) { Text(label) }
    else OutlinedButton(onClick = onClick) { Text(label) }
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

private fun money(value: Double): String = if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.US, "%.2f", value)

private fun formatDate(timestamp: Long): String = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(timestamp))
