package com.familykhata.app.ui

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.familykhata.app.FamilyKhataViewModel
import com.familykhata.app.data.BakiEntryEntity
import com.familykhata.app.data.BakiPersonSummary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val V13_WEBSITE = "https://uddinmezbah.github.io/Familykhata.Android_source/"
private const val V13_TUTORIAL = "https://uddinmezbah.github.io/Familykhata.Android_source/#tutorial"
private val DueTodayAccent = Color(0xFF0B7A53)
private val OverdueAccent = Color(0xFFC4473A)
private val PlanAccent = Color(0xFF6C4CCF)

@Composable
internal fun DueDatePickerField(
    value: Long?,
    onChange: (Long?) -> Unit,
    label: String = v15Text("পরিশোধের তারিখ","Due date")
) {
    val context = LocalContext.current
    val calendar = remember(value) {
        Calendar.getInstance().apply {
            if (value != null) timeInMillis = value
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = {
                DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        val selected = Calendar.getInstance().apply {
                            clear()
                            set(year, month, day, 0, 0, 0)
                        }.timeInMillis
                        onChange(selected)
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            },
            modifier = Modifier.weight(1f)
        ) {
            Text(if (value == null) v15Text("$label নির্বাচন করুন","Select $label") else "$label: ${v13Date(value)}")
        }
        if (value != null) {
            TextButton(onClick = { onChange(null) }) { Text(v15Text("মুছুন","Remove")) }
        }
    }
}

@Composable
internal fun DueDashboardSection(
    viewModel: FamilyKhataViewModel,
    onLedger: () -> Unit
) {
    val items by viewModel.dueReceivables.collectAsState()
    val todayStart = startOfToday()
    val tomorrowStart = todayStart + 86_400_000L
    val today = items.filter { it.dueAt in todayStart until tomorrowStart }
    val overdue = items.filter { it.dueAt < todayStart }
    val todayAmount = today.sumOf { it.remainingAmount }
    val overdueAmount = overdue.sumOf { it.remainingAmount }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        DueMetricCard(
            title = v15Text("আজকে পাবো","Due today"),
            amount = todayAmount,
            count = today.size,
            accent = DueTodayAccent,
            modifier = Modifier.weight(1f),
            onClick = onLedger
        )
        DueMetricCard(
            title = v15Text("বকেয়া","Overdue"),
            amount = overdueAmount,
            count = overdue.size,
            accent = OverdueAccent,
            modifier = Modifier.weight(1f),
            onClick = onLedger
        )
    }
}

@Composable
private fun DueMetricCard(
    title: String,
    amount: Double,
    count: Int,
    accent: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.09f)),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.24f)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, fontWeight = FontWeight.Bold, color = accent)
            Text(if (V14DisplayState.summaryVisible) "${V14DisplayState.currencySymbol} ${v13Money(amount)}" else "••••", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Text(v15Text("$count টি এন্ট্রি","$count entries"), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
internal fun BakiEntryActionRow(
    person: BakiPersonSummary,
    item: BakiEntryEntity,
    canWrite: Boolean,
    viewModel: FamilyKhataViewModel,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var showEdit by remember(item.id, item.amount, item.note, item.dueAt) { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedButton(
                onClick = { showEdit = true },
                enabled = canWrite,
                modifier = Modifier.weight(1f)
            ) { Text("Edit") }
            OutlinedButton(
                onClick = onDelete,
                enabled = canWrite,
                modifier = Modifier.weight(1f)
            ) { Text("Delete") }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedButton(
                onClick = { sendEntrySms(context, person, item) },
                enabled = person.phone.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) { Text("SMS") }
            OutlinedButton(
                onClick = { sendEntryWhatsApp(context, person, item) },
                enabled = person.phone.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) { Text("WhatsApp") }
            OutlinedButton(
                onClick = { callPerson(context, person.phone) },
                enabled = person.phone.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) { Text("Call") }
        }
    }

    if (showEdit) {
        var amount by remember(item.id) { mutableStateOf(v13Money(item.amount)) }
        var note by remember(item.id) { mutableStateOf(item.note) }
        var dueAt by remember(item.id) { mutableStateOf(item.dueAt) }
        var error by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showEdit = false },
            title = { Text(v15Text("এন্ট্রি সম্পাদনা","Edit entry")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it; error = null },
                        label = { Text(v15Text("টাকার পরিমাণ","Amount")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text(v15Text("নোট","Note")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DueDatePickerField(value = dueAt, onChange = { dueAt = it })
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val parsed = v13ParseAmount(amount)
                    if (parsed == null) {
                        error = v15Text("সঠিক টাকার পরিমাণ লিখুন","Enter a valid amount")
                    } else {
                        viewModel.updateBakiEntry(item, parsed, note, dueAt)
                        showEdit = false
                    }
                }) { Text(v15Text("সেভ করুন","Save")) }
            },
            dismissButton = {
                TextButton(onClick = { showEdit = false }) { Text(v15Text("বাতিল","Cancel")) }
            }
        )
    }
}

@Composable
internal fun PurchaseAndTutorialSection() {
    val context = LocalContext.current
    var selectedPlan by remember { mutableStateOf(v15Text("মাসিক","Monthly")) }

    Text(
        v15Text("প্ল্যান ও সহায়তা","Plans & support"),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
    Text(
        v15Text("৩০ দিনের ট্রায়াল শেষে নতুন হিসাব যোগ/সম্পাদনার জন্য একটি প্ল্যান নিন। পুরনো ডেটা মুছে যাবে না।","After the 30-day trial, choose a plan to add or edit records. Existing data will not be deleted."),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(v15Text("মাসিক","Monthly"), v15Text("বার্ষিক","Yearly"), "Lifetime").forEach { plan ->
            val selected = selectedPlan == plan
            if (selected) {
                Button(
                    onClick = { selectedPlan = plan },
                    modifier = Modifier.weight(1f)
                ) { Text(plan) }
            } else {
                OutlinedButton(
                    onClick = { selectedPlan = plan },
                    modifier = Modifier.weight(1f)
                ) { Text(plan) }
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PlanAccent.copy(alpha = 0.07f)),
        border = BorderStroke(1.dp, PlanAccent.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(v15Text("$selectedPlan প্ল্যান কিনতে যোগাযোগ করুন","Contact us to get the $selectedPlan plan"), fontWeight = FontWeight.Bold)
            Text(
                v15Text("দাম ও পেমেন্ট নির্দেশনা বিক্রয় চ্যানেলে জানানো হবে।","Pricing and payment instructions will be provided through the sales channel."),
                style = MaterialTheme.typography.bodySmall
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = { purchaseWhatsApp(context, selectedPlan) },
                    modifier = Modifier.weight(1f)
                ) { Text("WhatsApp") }
                OutlinedButton(
                    onClick = { purchaseMessenger(context, selectedPlan) },
                    modifier = Modifier.weight(1f)
                ) { Text("Messenger") }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = { purchaseSms(context, selectedPlan) },
                    modifier = Modifier.weight(1f)
                ) { Text("SMS") }
                OutlinedButton(
                    onClick = { v13OpenUrl(context, V13_WEBSITE) },
                    modifier = Modifier.weight(1f)
                ) { Text("Website") }
            }
        }
    }

    Card(
        onClick = { v13OpenUrl(context, V13_TUTORIAL) },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DueTodayAccent.copy(alpha = 0.07f)),
        border = BorderStroke(1.dp, DueTodayAccent.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(v15Text("▶ ব্যবহারের নিয়ম / ভিডিও","▶ Usage guide / video"), fontWeight = FontWeight.Bold)
            Text(
                v15Text("নতুন খাতা, বাকি, পরিশোধ, ব্যাকআপ ও রিপোর্ট ব্যবহারের গাইড দেখুন।","See the guide for ledgers, dues, payments, backups and reports."),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun sendEntrySms(context: Context, person: BakiPersonSummary, item: BakiEntryEntity) {
    val dueText = item.dueAt?.let { v15Text(" পরিশোধের তারিখ ${v13Date(it)}।"," Due date: ${v13Date(it)}.") }.orEmpty()
    val message = v15Text("আসসালামু আলাইকুম ${person.name}, হিসাবী খাতা অনুযায়ী ${V14DisplayState.currencySymbol} ${v13Money(item.amount)} টাকার একটি হিসাব আছে.$dueText সুবিধামতো পরিশোধ/যোগাযোগ করার অনুরোধ রইল।","Hello ${person.name}, according to Hisabi Khata there is an account of ${V14DisplayState.currencySymbol} ${v13Money(item.amount)}.$dueText Please make payment or contact us when convenient.")
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("smsto:${Uri.encode(person.phone)}")
        putExtra("sms_body", message)
    }
    runCatching { context.startActivity(intent) }
}

private fun sendEntryWhatsApp(context: Context, person: BakiPersonSummary, item: BakiEntryEntity) {
    val dueText = item.dueAt?.let { v15Text(" পরিশোধের তারিখ ${v13Date(it)}।"," Due date: ${v13Date(it)}.") }.orEmpty()
    val message = v15Text("আসসালামু আলাইকুম ${person.name}, হিসাবী খাতা অনুযায়ী ${V14DisplayState.currencySymbol} ${v13Money(item.amount)} টাকার একটি হিসাব আছে.$dueText সুবিধামতো পরিশোধ/যোগাযোগ করার অনুরোধ রইল।","Hello ${person.name}, according to Hisabi Khata there is an account of ${V14DisplayState.currencySymbol} ${v13Money(item.amount)}.$dueText Please make payment or contact us when convenient.")
    val phone = person.phone.filter(Char::isDigit).let { digits -> if (digits.startsWith("0")) "88$digits" else digits }
    v13OpenUrl(context, "https://wa.me/$phone?text=${Uri.encode(message)}")
}

private fun callPerson(context: Context, phone: String) {
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phone)}"))
    runCatching { context.startActivity(intent) }
}

private fun purchaseMessage(plan: String): String =
    v15Text("আমি হিসাবী খাতা অ্যাপের $plan প্ল্যান নিতে চাই। পেমেন্ট ও অ্যাক্টিভেশন নির্দেশনা দিন।","I want the $plan plan for Hisabi Khata. Please send payment and activation instructions.")

private fun purchaseWhatsApp(context: Context, plan: String) {
    val url = "https://wa.me/?text=${Uri.encode(purchaseMessage(plan))}"
    v13OpenUrl(context, url)
}

private fun purchaseMessenger(context: Context, plan: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, purchaseMessage(plan))
        setPackage("com.facebook.orca")
    }
    runCatching { context.startActivity(intent) }
        .recoverCatching {
            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, purchaseMessage(plan))
            }, v15Text("যোগাযোগ করুন","Contact")))
        }
}

private fun purchaseSms(context: Context, plan: String) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("smsto:")
        putExtra("sms_body", purchaseMessage(plan))
    }
    runCatching { context.startActivity(intent) }
}

private fun v13OpenUrl(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}

private fun startOfToday(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

internal fun v13Date(timestamp: Long): String =
    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))

private fun v13ParseAmount(input: String): Double? {
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

private fun v13Money(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString()
    else String.format(Locale.US, "%.2f", value)
