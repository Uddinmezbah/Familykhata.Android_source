package com.familykhata.app.ui

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.familykhata.app.FamilyKhataViewModel
import com.familykhata.app.TrialStatus
import com.familykhata.app.data.BakiPersonSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private val PersonPalette = listOf(
    Color(0xFF6C4CCF),
    Color(0xFF1479B8),
    Color(0xFFB85C00),
    Color(0xFF00897B),
    Color(0xFFD81B60),
    Color(0xFF3949AB),
    Color(0xFF2E7D32),
    Color(0xFFE64A19),
    Color(0xFF00838F),
    Color(0xFF7B4F2C)
)

internal fun personAccent(id: Long): Color {
    val index = ((id % PersonPalette.size + PersonPalette.size) % PersonPalette.size).toInt()
    return PersonPalette[index]
}

@Composable
internal fun TrialNotice(status: TrialStatus) {
    val accent = if (status.expired) MaterialTheme.colorScheme.error else Color(0xFFB85C00)
    val message = when {
        status.premiumUnlocked -> "Premium সক্রিয়"
        status.expired -> "৩০ দিনের ট্রায়াল শেষ • পুরনো হিসাব দেখা, ব্যাকআপ ও রিপোর্ট ডাউনলোড করা যাবে"
        else -> "৩০ দিনের ট্রায়াল • ${status.daysRemaining} দিন বাকি"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = 0.09f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f))
    ) {
        Text(
            message,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = accent,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun TrialLockedMessage() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Text(
            "ট্রায়াল শেষ হয়েছে। নতুন হিসাব যোগ বা সম্পাদনা বন্ধ আছে। আপনার পুরনো হিসাব, ব্যাকআপ ও রিপোর্ট নিরাপদে ব্যবহার করতে পারবেন।",
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Composable
internal fun AppLockScreen(viewModel: FamilyKhataViewModel) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    "৳",
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                "হিসাবী খাতা লক করা আছে",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                "আপনার ৪–৬ সংখ্যার PIN লিখুন",
                modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = pin,
                onValueChange = { value ->
                    pin = value.filter { ch -> ch.isDigit() }.take(6)
                    error = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("PIN") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
            )
            error?.let {
                Text(
                    it,
                    modifier = Modifier.padding(top = 6.dp),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Button(
                onClick = {
                    if (!viewModel.verifyPin(pin)) {
                        error = "PIN সঠিক নয়"
                        pin = ""
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                enabled = pin.length in 4..6
            ) {
                Text("আনলক করুন")
            }
            Text(
                "PIN আপনার ডিভাইসেই সুরক্ষিতভাবে hash আকারে রাখা হয়। PIN মনে রাখুন।",
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
internal fun CommercialToolsSection(viewModel: FamilyKhataViewModel) {
    val context = LocalContext.current
    val trialStatus by viewModel.trialStatus.collectAsState()
    val pinConfigured by viewModel.isPinConfigured.collectAsState()
    val workspace by viewModel.selectedWorkspace.collectAsState()

    var reportPeriod by remember { mutableStateOf("MONTH") }
    var reportText by remember { mutableStateOf<String?>(null) }
    var reportFileName by remember { mutableStateOf<String?>(null) }
    var showSetPin by remember { mutableStateOf(false) }
    var showChangePin by remember { mutableStateOf(false) }
    var showDisablePin by remember { mutableStateOf(false) }

    val createReportFile = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        val csv = reportText
        reportText = null
        reportFileName = null
        if (uri != null && csv != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { writer ->
                    writer.write(csv)
                } ?: error("রিপোর্ট ফাইল লেখা যায়নি")
            }.onSuccess {
                commercialToast(context, "রিপোর্ট সেভ হয়েছে")
            }.onFailure {
                commercialToast(context, it.message ?: "রিপোর্ট সেভ করা যায়নি")
            }
        }
    }

    Text(
        "ব্যবহারের মেয়াদ",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
    TrialNotice(trialStatus)

    Text(
        "সময় অনুযায়ী রিপোর্ট",
        modifier = Modifier.padding(top = 4.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
    Text(
        "বর্তমান ওয়ার্কস্পেসের হিসাব CSV ফাইলে নামান। Excel/Google Sheets-এ খোলা যাবে।",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ReportPeriodButton(
            label = "এই মাস",
            value = "MONTH",
            selected = reportPeriod,
            modifier = Modifier.weight(1f)
        ) { reportPeriod = it }
        ReportPeriodButton(
            label = "এই বছর",
            value = "YEAR",
            selected = reportPeriod,
            modifier = Modifier.weight(1f)
        ) { reportPeriod = it }
    }
    ReportPeriodButton(
        label = "শুরু থেকে আজ পর্যন্ত",
        value = "ALL",
        selected = reportPeriod,
        modifier = Modifier.fillMaxWidth()
    ) { reportPeriod = it }

    Button(
        onClick = {
            viewModel.createCsvReport(
                period = reportPeriod,
                onReady = { csv ->
                    reportText = csv
                    val stamp = SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.US).format(Date())
                    val workspacePart = when (workspace) {
                        "PERSONAL" -> "personal"
                        "SHOP" -> "business"
                        else -> "family"
                    }
                    val periodPart = when (reportPeriod) {
                        "MONTH" -> "this-month"
                        "YEAR" -> "this-year"
                        else -> "all-to-date"
                    }
                    val fileName = "HisabiKhata-$workspacePart-$periodPart-$stamp.csv"
                    reportFileName = fileName
                    createReportFile.launch(fileName)
                },
                onError = { commercialToast(context, it) }
            )
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("CSV রিপোর্ট ডাউনলোড করুন")
    }

    Text(
        "অ্যাপ নিরাপত্তা",
        modifier = Modifier.padding(top = 4.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF6C4CCF).copy(alpha = 0.07f)
        ),
        border = BorderStroke(1.dp, Color(0xFF6C4CCF).copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                if (pinConfigured) "PIN App Lock চালু আছে" else "PIN App Lock বন্ধ আছে",
                fontWeight = FontWeight.Bold
            )
            Text(
                if (pinConfigured) "অ্যাপ পুনরায় চালু হলে PIN দিয়ে খুলতে হবে। চাইলে এখনই লক করতে পারেন।"
                else "৪–৬ সংখ্যার PIN দিয়ে আপনার হিসাব অন্যের কাছ থেকে সুরক্ষিত রাখুন।",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!pinConfigured) {
                Button(
                    onClick = { showSetPin = true },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("PIN সেট করুন") }
            } else {
                Button(
                    onClick = { viewModel.lockApp() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("এখনই অ্যাপ লক করুন") }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showChangePin = true },
                        modifier = Modifier.weight(1f)
                    ) { Text("PIN বদলান") }
                    OutlinedButton(
                        onClick = { showDisablePin = true },
                        modifier = Modifier.weight(1f)
                    ) { Text("PIN বন্ধ") }
                }
            }
        }
    }

    if (showSetPin) {
        CreatePinDialog(
            title = "নতুন PIN সেট করুন",
            onDismiss = { showSetPin = false },
            onSave = { pin ->
                val ok = viewModel.setPin(pin)
                if (ok) {
                    showSetPin = false
                    commercialToast(context, "PIN App Lock চালু হয়েছে")
                }
                ok
            }
        )
    }

    if (showChangePin) {
        ChangePinDialog(
            onDismiss = { showChangePin = false },
            onSave = { currentPin, newPin ->
                val ok = viewModel.changePin(currentPin, newPin)
                if (ok) {
                    showChangePin = false
                    commercialToast(context, "PIN পরিবর্তন হয়েছে")
                }
                ok
            }
        )
    }

    if (showDisablePin) {
        CurrentPinDialog(
            title = "PIN App Lock বন্ধ করবেন?",
            actionLabel = "PIN বন্ধ করুন",
            onDismiss = { showDisablePin = false },
            onConfirm = { currentPin ->
                val ok = viewModel.disablePin(currentPin)
                if (ok) {
                    showDisablePin = false
                    commercialToast(context, "PIN App Lock বন্ধ হয়েছে")
                }
                ok
            }
        )
    }
}

@Composable
private fun ReportPeriodButton(
    label: String,
    value: String,
    selected: String,
    modifier: Modifier,
    onSelect: (String) -> Unit
) {
    if (selected == value) {
        Button(onClick = { onSelect(value) }, modifier = modifier) { Text(label) }
    } else {
        OutlinedButton(onClick = { onSelect(value) }, modifier = modifier) { Text(label) }
    }
}

@Composable
internal fun PersonManagementActions(
    person: BakiPersonSummary,
    personLabel: String,
    canWrite: Boolean,
    viewModel: FamilyKhataViewModel,
    onDeleted: () -> Unit
) {
    var showEdit by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = { showEdit = true },
            enabled = canWrite,
            modifier = Modifier.weight(1f)
        ) { Text("তথ্য সম্পাদনা") }
        OutlinedButton(
            onClick = { showDelete = true },
            modifier = Modifier.weight(1f)
        ) { Text("$personLabel ডিলিট") }
    }

    if (showEdit) {
        var name by remember(person.id) { mutableStateOf(person.name) }
        var phone by remember(person.id) { mutableStateOf(person.phone) }
        var error by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showEdit = false },
            title = { Text("$personLabel তথ্য সম্পাদনা") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("নাম") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("ফোন") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (name.isBlank()) {
                            error = "নাম খালি রাখা যাবে না"
                        } else {
                            viewModel.updateBakiPerson(person.id, name, phone)
                            showEdit = false
                        }
                    }
                ) { Text("সেভ করুন") }
            },
            dismissButton = {
                TextButton(onClick = { showEdit = false }) { Text("বাতিল") }
            }
        )
    }

    if (showDelete) {
        val balanceWarning = when {
            person.balance > 0 -> "এই $personLabel-এর কাছে আপনি ${V14DisplayState.currencySymbol} ${commercialMoney(person.balance)} পাবেন।"
            person.balance < 0 -> "এই $personLabel-কে আপনি ${V14DisplayState.currencySymbol} ${commercialMoney(abs(person.balance))} দেবেন।"
            else -> "এই $personLabel-এর বর্তমান হিসাব সমান।"
        }
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("$personLabel ডিলিট করবেন?") },
            text = {
                Text(
                    "$balanceWarning\n\n${person.name}-এর সব বাকি/পাওনা এন্ট্রিও স্থায়ীভাবে মুছে যাবে। আগে প্রয়োজন হলে ব্যাকআপ তৈরি করুন।"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteBakiPerson(person.id)
                        showDelete = false
                        onDeleted()
                    }
                ) { Text("স্থায়ীভাবে ডিলিট") }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text("বাতিল") }
            }
        )
    }
}

@Composable
private fun CreatePinDialog(
    title: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Boolean
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PinField("নতুন PIN", pin) { pin = it; error = null }
                PinField("PIN আবার লিখুন", confirmPin) { confirmPin = it; error = null }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                error = when {
                    pin.length !in 4..6 -> "PIN ৪–৬ সংখ্যার হতে হবে"
                    pin != confirmPin -> "দুইটি PIN মিলছে না"
                    !onSave(pin) -> "PIN সেট করা যায়নি"
                    else -> null
                }
            }) { Text("সেভ করুন") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } }
    )
}

@Composable
private fun ChangePinDialog(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Boolean
) {
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("PIN পরিবর্তন করুন") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PinField("বর্তমান PIN", currentPin) { currentPin = it; error = null }
                PinField("নতুন PIN", newPin) { newPin = it; error = null }
                PinField("নতুন PIN আবার লিখুন", confirmPin) { confirmPin = it; error = null }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                error = when {
                    newPin.length !in 4..6 -> "নতুন PIN ৪–৬ সংখ্যার হতে হবে"
                    newPin != confirmPin -> "দুইটি নতুন PIN মিলছে না"
                    !onSave(currentPin, newPin) -> "বর্তমান PIN সঠিক নয়"
                    else -> null
                }
            }) { Text("পরিবর্তন করুন") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } }
    )
}

@Composable
private fun CurrentPinDialog(
    title: String,
    actionLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Boolean
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                PinField("বর্তমান PIN", pin) { pin = it; error = null }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (!onConfirm(pin)) error = "বর্তমান PIN সঠিক নয়"
            }) { Text(actionLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } }
    )
}

@Composable
private fun PinField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter { ch -> ch.isDigit() }.take(6)) },
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth()
    )
}

private fun commercialToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

private fun commercialMoney(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString()
    else String.format(Locale.US, "%.2f", value)
