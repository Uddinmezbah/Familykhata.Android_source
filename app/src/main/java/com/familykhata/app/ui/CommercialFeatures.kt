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
        status.premiumUnlocked -> v15Text("Premium সক্রিয়","Premium active")
        status.expired -> v15Text("৩০ দিনের ট্রায়াল শেষ • পুরনো হিসাব দেখা, ব্যাকআপ ও রিপোর্ট ডাউনলোড করা যাবে","30-day trial ended • You can still view old records, backups and reports")
        else -> v15Text("৩০ দিনের ট্রায়াল • ${status.daysRemaining} দিন বাকি","30-day trial • ${status.daysRemaining} days left")
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
            v15Text("ট্রায়াল শেষ হয়েছে। নতুন হিসাব যোগ বা সম্পাদনা বন্ধ আছে। আপনার পুরনো হিসাব, ব্যাকআপ ও রিপোর্ট নিরাপদে ব্যবহার করতে পারবেন।","Trial ended. Adding or editing new records is disabled, but your existing records, backups and reports remain available."),
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
                v15Text("হিসাবী খাতা লক করা আছে","Hisabi Khata is locked"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                v15Text("আপনার ৪–৬ সংখ্যার PIN লিখুন","Enter your 4–6 digit PIN"),
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
                        error = v15Text("PIN সঠিক নয়","Incorrect PIN")
                        pin = ""
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                enabled = pin.length in 4..6
            ) {
                Text(v15Text("আনলক করুন","Unlock"))
            }
            Text(
                v15Text("PIN আপনার ডিভাইসেই সুরক্ষিতভাবে hash আকারে রাখা হয়। PIN মনে রাখুন।","Your PIN is stored securely on this device as a hash. Please remember it."),
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
                } ?: error(v15Text("রিপোর্ট ফাইল লেখা যায়নি","Unable to write report file"))
            }.onSuccess {
                commercialToast(context, v15Text("রিপোর্ট সেভ হয়েছে","Report saved"))
            }.onFailure {
                commercialToast(context, it.message ?: v15Text("রিপোর্ট সেভ করা যায়নি","Unable to save report"))
            }
        }
    }

    Text(
        v15Text("ব্যবহারের মেয়াদ","Usage period"),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
    TrialNotice(trialStatus)

    Text(
        v15Text("সময় অনুযায়ী রিপোর্ট","Reports by period"),
        modifier = Modifier.padding(top = 4.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
    Text(
        v15Text("বর্তমান ওয়ার্কস্পেসের হিসাব CSV ফাইলে নামান। Excel/Google Sheets-এ খোলা যাবে।","Export the current workspace as CSV for Excel or Google Sheets."),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ReportPeriodButton(
            label = v15Text("এই মাস","This month"),
            value = "MONTH",
            selected = reportPeriod,
            modifier = Modifier.weight(1f)
        ) { reportPeriod = it }
        ReportPeriodButton(
            label = v15Text("এই বছর","This year"),
            value = "YEAR",
            selected = reportPeriod,
            modifier = Modifier.weight(1f)
        ) { reportPeriod = it }
    }
    ReportPeriodButton(
        label = v15Text("শুরু থেকে আজ পর্যন্ত","All time"),
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
        Text(v15Text("CSV রিপোর্ট ডাউনলোড করুন","Download CSV report"))
    }

    Text(
        v15Text("অ্যাপ নিরাপত্তা","App security"),
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
                if (pinConfigured) v15Text("PIN App Lock চালু আছে","PIN App Lock is enabled") else v15Text("PIN App Lock বন্ধ আছে","PIN App Lock is disabled"),
                fontWeight = FontWeight.Bold
            )
            Text(
                if (pinConfigured) v15Text("অ্যাপ পুনরায় চালু হলে PIN দিয়ে খুলতে হবে। চাইলে এখনই লক করতে পারেন।","You will need the PIN when reopening the app. You can lock it now.")
                else v15Text("৪–৬ সংখ্যার PIN দিয়ে আপনার হিসাব অন্যের কাছ থেকে সুরক্ষিত রাখুন।","Protect your accounts with a 4–6 digit PIN."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!pinConfigured) {
                Button(
                    onClick = { showSetPin = true },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(v15Text("PIN সেট করুন","Set PIN")) }
            } else {
                Button(
                    onClick = { viewModel.lockApp() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(v15Text("এখনই অ্যাপ লক করুন","Lock app now")) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showChangePin = true },
                        modifier = Modifier.weight(1f)
                    ) { Text(v15Text("PIN বদলান","Change PIN")) }
                    OutlinedButton(
                        onClick = { showDisablePin = true },
                        modifier = Modifier.weight(1f)
                    ) { Text(v15Text("PIN বন্ধ","Disable PIN")) }
                }
            }
        }
    }

    if (showSetPin) {
        CreatePinDialog(
            title = v15Text("নতুন PIN সেট করুন","Set new PIN"),
            onDismiss = { showSetPin = false },
            onSave = { pin ->
                val ok = viewModel.setPin(pin)
                if (ok) {
                    showSetPin = false
                    commercialToast(context, v15Text("PIN App Lock চালু হয়েছে","PIN App Lock enabled"))
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
                    commercialToast(context, v15Text("PIN পরিবর্তন হয়েছে","PIN changed"))
                }
                ok
            }
        )
    }

    if (showDisablePin) {
        CurrentPinDialog(
            title = v15Text("PIN App Lock বন্ধ করবেন?","Disable PIN App Lock?"),
            actionLabel = v15Text("PIN বন্ধ করুন","Disable PIN"),
            onDismiss = { showDisablePin = false },
            onConfirm = { currentPin ->
                val ok = viewModel.disablePin(currentPin)
                if (ok) {
                    showDisablePin = false
                    commercialToast(context, v15Text("PIN App Lock বন্ধ হয়েছে","PIN App Lock disabled"))
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
        ) { Text(v15Text("তথ্য সম্পাদনা","Edit information")) }
        OutlinedButton(
            onClick = { showDelete = true },
            modifier = Modifier.weight(1f)
        ) { Text(v15Text("$personLabel ডিলিট","Delete $personLabel")) }
    }

    if (showEdit) {
        var name by remember(person.id) { mutableStateOf(person.name) }
        var phone by remember(person.id) { mutableStateOf(person.phone) }
        var error by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showEdit = false },
            title = { Text(v15Text("$personLabel তথ্য সম্পাদনা","Edit $personLabel information")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(v15Text("নাম","Name")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(v15Text("ফোন","Phone")) },
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
                            error = v15Text("নাম খালি রাখা যাবে না","Name cannot be empty")
                        } else {
                            viewModel.updateBakiPerson(person.id, name, phone)
                            showEdit = false
                        }
                    }
                ) { Text(v15Text("সেভ করুন","Save")) }
            },
            dismissButton = {
                TextButton(onClick = { showEdit = false }) { Text(v15Text("বাতিল","Cancel")) }
            }
        )
    }

    if (showDelete) {
        val balanceWarning = when {
            person.balance > 0 -> v15Text("এই $personLabel-এর কাছে আপনি ${V14DisplayState.currencySymbol} ${commercialMoney(person.balance)} পাবেন।","You will receive ${V14DisplayState.currencySymbol} ${commercialMoney(person.balance)} from this $personLabel.")
            person.balance < 0 -> v15Text("এই $personLabel-কে আপনি ${V14DisplayState.currencySymbol} ${commercialMoney(abs(person.balance))} দেবেন।","You owe this $personLabel ${V14DisplayState.currencySymbol} ${commercialMoney(abs(person.balance))}.")
            else -> v15Text("এই $personLabel-এর বর্তমান হিসাব সমান।","This $personLabel account is settled.")
        }
        ProtectedDeleteDialog(
            viewModel = viewModel,
            title =
                v15Text(
                    "$personLabel ডিলিট করবেন?",
                    "Delete $personLabel?"
                ),
            message =
                v15Text(
                    "$balanceWarning\n\n${person.name}-এর সব বাকি/পাওনা এন্ট্রিও স্থায়ীভাবে মুছে যাবে। আগে প্রয়োজন হলে ব্যাকআপ তৈরি করুন।",
                    "$balanceWarning\n\nAll due entries for ${person.name} will also be permanently deleted. Create a backup first if needed."
                ),
            confirmLabel =
                v15Text(
                    "স্থায়ীভাবে ডিলিট",
                    "Delete permanently"
                ),
            onDismiss = {
                showDelete = false
            },
            onConfirmed = {
                viewModel.deleteBakiPerson(person.id)
                showDelete = false
                onDeleted()
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
                PinField(v15Text("নতুন PIN","New PIN"), pin) { pin = it; error = null }
                PinField(v15Text("PIN আবার লিখুন","Confirm PIN"), confirmPin) { confirmPin = it; error = null }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                error = when {
                    pin.length !in 4..6 -> v15Text("PIN ৪–৬ সংখ্যার হতে হবে","PIN must be 4–6 digits")
                    pin != confirmPin -> v15Text("দুইটি PIN মিলছে না","PINs do not match")
                    !onSave(pin) -> v15Text("PIN সেট করা যায়নি","Unable to set PIN")
                    else -> null
                }
            }) { Text(v15Text("সেভ করুন","Save")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(v15Text("বাতিল","Cancel")) } }
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
        title = { Text(v15Text("PIN পরিবর্তন করুন","Change PIN")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PinField(v15Text("বর্তমান PIN","Current PIN"), currentPin) { currentPin = it; error = null }
                PinField(v15Text("নতুন PIN","New PIN"), newPin) { newPin = it; error = null }
                PinField(v15Text("নতুন PIN আবার লিখুন","Confirm new PIN"), confirmPin) { confirmPin = it; error = null }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                error = when {
                    newPin.length !in 4..6 -> v15Text("নতুন PIN ৪–৬ সংখ্যার হতে হবে","New PIN must be 4–6 digits")
                    newPin != confirmPin -> v15Text("দুইটি নতুন PIN মিলছে না","New PINs do not match")
                    !onSave(currentPin, newPin) -> v15Text("বর্তমান PIN সঠিক নয়","Current PIN is incorrect")
                    else -> null
                }
            }) { Text(v15Text("পরিবর্তন করুন","Change")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(v15Text("বাতিল","Cancel")) } }
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
                PinField(v15Text("বর্তমান PIN","Current PIN"), pin) { pin = it; error = null }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (!onConfirm(pin)) error = v15Text("বর্তমান PIN সঠিক নয়","Current PIN is incorrect")
            }) { Text(actionLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(v15Text("বাতিল","Cancel")) } }
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
