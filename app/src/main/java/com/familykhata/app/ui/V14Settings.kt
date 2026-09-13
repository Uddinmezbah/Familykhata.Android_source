package com.familykhata.app.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.familykhata.app.DueReminderScheduler
import com.familykhata.app.FamilyKhataViewModel

private const val V14_PREFS = "hisabi_khata_v14_settings"
private const val SUPPORT_PHONE = "8801886665676"
private const val WEBSITE_URL_V14 = "https://uddinmezbah.github.io/Familykhata.Android_source/"
private const val TUTORIAL_URL_V14 = "https://uddinmezbah.github.io/Familykhata.Android_source/#tutorial"
private const val FEATURE_URL_V14 = "https://github.com/Uddinmezbah/Familykhata.Android_source/issues/new?title=Feature%20request%3A%20"
private const val MORE_APPS_URL_V14 = "https://github.com/Uddinmezbah"

internal object V14DisplayState {
    var currencySymbol by mutableStateOf("৳")
    var summaryVisible by mutableStateOf(true)
    private var initialized = false

    fun initialize(context: Context) {
        if (initialized) return
        val prefs = context.getSharedPreferences(V14_PREFS, Context.MODE_PRIVATE)
        currencySymbol = prefs.getString("currency_symbol", "৳") ?: "৳"
        summaryVisible = prefs.getBoolean("summary_visible", true)
        initialized = true
    }
}

@Composable
internal fun TopCornerMenuButton(onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Card(
            onClick = onClick,
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f))
        ) {
            Text("☰  মেনু", modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun V14SettingsScreen(
    viewModel: FamilyKhataViewModel,
    onClose: () -> Unit,
    onOpenLedger: () -> Unit,
    onOpenMore: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(V14_PREFS, Context.MODE_PRIVATE) }

    var profileName by remember { mutableStateOf(prefs.getString("profile_name", "") ?: "") }
    var businessName by remember { mutableStateOf(prefs.getString("business_name", "") ?: "") }
    var profilePhone by remember { mutableStateOf(prefs.getString("profile_phone", "") ?: "") }
    var showProfile by remember { mutableStateOf(false) }
    var showLanguage by remember { mutableStateOf(false) }
    var showCurrency by remember { mutableStateOf(false) }
    var showReminder by remember { mutableStateOf(false) }
    var showPlan by remember { mutableStateOf(false) }
    var showPin by remember { mutableStateOf(false) }
    var showSmsInfo by remember { mutableStateOf(false) }
    var summaryVisible by remember { mutableStateOf(prefs.getBoolean("summary_visible", true)) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onClose) { Text("✕") }
            Text("সেটিংস", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            Text("v1.4", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }

        Card(
            onClick = { showProfile = true },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
            shape = RoundedCornerShape(22.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primary) {
                    Text("👤", modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.titleLarge)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(profileName.ifBlank { "আপনার প্রোফাইল" }, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                    if (businessName.isNotBlank()) Text(businessName, style = MaterialTheme.typography.bodySmall)
                    if (profilePhone.isNotBlank()) Text(profilePhone, style = MaterialTheme.typography.bodySmall)
                    if (profileName.isBlank() && profilePhone.isBlank()) Text("নাম, প্রতিষ্ঠান ও ফোন যোগ করুন", style = MaterialTheme.typography.bodySmall)
                }
                Text("›", style = MaterialTheme.typography.headlineSmall)
            }
        }

        SettingsActionCard("🌐", "ভাষা পরিবর্তন", "বাংলা সক্রিয় • English/Hindi/Arabic প্রস্তুত করা হবে") { showLanguage = true }
        SettingsActionCard("★", "প্রিমিয়াম হয়ে যান", "মাসিক • বার্ষিক • Lifetime") { showPlan = true }
        SettingsActionCard("💬", "তাগাদা মেসেজ", "SMS/WhatsApp-এ প্রস্তুত বার্তা; আলাদা SMS প্যাক এখন লাগবে না") { showSmsInfo = true }
        SettingsActionCard("🔒", "PIN / পাসওয়ার্ড পরিবর্তন", "অ্যাপ লক সেট, পরিবর্তন বা বন্ধ করুন") { showPin = true }
        SettingsActionCard("🔔", "বাকি পরিশোধের নোটিফিকেশন", "৩০/১৫/৭/৩ দিন আগে এবং নির্ধারিত দিনে মনে করাবে") { showReminder = true }
        SettingsActionCard("💱", "মুদ্রা পরিবর্তন করুন", "প্রদর্শনের মুদ্রা বদলাবে; FX conversion হবে না") { showCurrency = true }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("📄", style = MaterialTheme.typography.titleLarge)
                Column(modifier = Modifier.weight(1f)) {
                    Text("হোম সারাংশ", fontWeight = FontWeight.Bold)
                    Text(if (summaryVisible) "টাকার সারাংশ দেখা যাচ্ছে" else "টাকার সারাংশ লুকানো আছে", style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = summaryVisible,
                    onCheckedChange = { checked ->
                        summaryVisible = checked
                        prefs.edit().putBoolean("summary_visible", checked).apply()
                        V14DisplayState.summaryVisible = checked
                    }
                )
            }
        }

        SettingsSectionTitle("দ্রুত কাজ")
        SettingsActionCard("👥", "কাস্টমার / ব্যক্তি খাতা", "নাম বা ফোন দিয়ে খুঁজুন, বাকি ও লেনদেন দেখুন") { onOpenLedger() }
        SettingsActionCard("↥", "ডাটা ব্যাকআপ ও রিপোর্ট", "JSON Backup/Restore এবং CSV রিপোর্ট") { onOpenMore() }
        SettingsActionCard("▶", "কিভাবে ব্যবহার করব?", "ব্যবহারের নিয়ম ও ভিডিও টিউটোরিয়াল") { openUrlV14(context, TUTORIAL_URL_V14) }
        SettingsActionCard("☏", "আমাদের সাথে যোগাযোগ করুন", "WhatsApp / SMS / Website") { openSupportChooser(context) }

        SettingsSectionTitle("শেয়ার ও তথ্য")
        SettingsActionCard("↗", "অ্যাপ শেয়ার করুন", "পরিবার, বন্ধু বা ব্যবসায়িক পরিচিতদের পাঠান") { shareAppV14(context) }
        SettingsActionCard("★", "রিভিউ দিন", "Play Store প্রকাশের পর রেটিং দিন") { openPlayStoreV14(context) }
        SettingsActionCard("▦", "আরও অ্যাপ", "ডেভেলপারের অন্যান্য প্রজেক্ট দেখুন") { openUrlV14(context, MORE_APPS_URL_V14) }
        SettingsActionCard("🌍", "ওয়েবসাইট", "হিসাবী খাতার অফিসিয়াল ওয়েব পেজ") { openUrlV14(context, WEBSITE_URL_V14) }
        SettingsActionCard("✦", "ফিচার রিকোয়েস্ট", "যে নতুন সুবিধা চান তা জানান") { openUrlV14(context, FEATURE_URL_V14) }
        SettingsActionCard("🔐", "এখনই অ্যাপ লক করুন", "PIN চালু থাকলে সঙ্গে সঙ্গে লক হবে") {
            viewModel.lockApp()
            onClose()
        }

        Text(
            "হিসাবী খাতা v1.4 • Offline-first • লোকাল ডেটা",
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (showProfile) {
        ProfileDialog(profileName, businessName, profilePhone, { showProfile = false }) { name, business, phone ->
            profileName = name
            businessName = business
            profilePhone = phone
            prefs.edit().putString("profile_name", name).putString("business_name", business).putString("profile_phone", phone).apply()
            showProfile = false
        }
    }
    if (showLanguage) LanguageDialog { showLanguage = false }
    if (showCurrency) CurrencyDialog(
        currentCode = prefs.getString("currency_code", "BDT") ?: "BDT",
        onDismiss = { showCurrency = false },
        onSelect = { code, symbol ->
            prefs.edit().putString("currency_code", code).putString("currency_symbol", symbol).apply()
            V14DisplayState.currencySymbol = symbol
            showCurrency = false
        }
    )
    if (showReminder) ReminderSettingsDialog(context, prefs) { showReminder = false }
    if (showPlan) PlanPurchaseDialog(context) { showPlan = false }
    if (showPin) PinSettingsDialog(viewModel) { showPin = false }
    if (showSmsInfo) {
        AlertDialog(
            onDismissRequest = { showSmsInfo = false },
            title = { Text("তাগাদা মেসেজ") },
            text = { Text("প্রতিটি বাকি এন্ট্রির নিচে SMS, WhatsApp ও Call থাকবে। SMS/WhatsApp আপনার ফোনের অ্যাপ খুলে প্রস্তুত বার্তা দেবে। তাই v1.4-এ আলাদা SMS credit কেনার প্রয়োজন নেই।") },
            confirmButton = { TextButton(onClick = { showSmsInfo = false }) { Text("ঠিক আছে") } }
        )
    }
}

@Composable
private fun SettingsActionCard(symbol: String, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(symbol, style = MaterialTheme.typography.titleLarge)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("›", style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun SettingsSectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun ProfileDialog(
    initialName: String,
    initialBusiness: String,
    initialPhone: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var business by remember { mutableStateOf(initialBusiness) }
    var phone by remember { mutableStateOf(initialPhone) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("প্রোফাইল") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("নাম") }, singleLine = true)
                OutlinedTextField(business, { business = it }, label = { Text("প্রতিষ্ঠান (ঐচ্ছিক)") }, singleLine = true)
                OutlinedTextField(phone, { phone = it }, label = { Text("ফোন") }, singleLine = true)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name.trim(), business.trim(), phone.trim()) }) { Text("সেভ") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } }
    )
}

@Composable
private fun LanguageDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select app language") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("বাংলা ✓") }
                OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text("English — শিগগিরই") }
                OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text("हिंदी — শিগগিরই") }
                OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text("العربية — শিগগিরই") }
                Text("v1.4-এ বাংলা সম্পূর্ণ সক্রিয়। অন্য ভাষাগুলো পুরো UI translation resource-এ স্থানান্তরের পর চালু করা হবে।", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("সম্পন্ন") } }
    )
}

private data class CurrencyChoice(val country: String, val code: String, val symbol: String)

@Composable
private fun CurrencyDialog(currentCode: String, onDismiss: () -> Unit, onSelect: (String, String) -> Unit) {
    val choices = listOf(
        CurrencyChoice("বাংলাদেশ", "BDT", "৳"),
        CurrencyChoice("ভারত", "INR", "₹"),
        CurrencyChoice("পাকিস্তান", "PKR", "Rs"),
        CurrencyChoice("শ্রীলঙ্কা", "LKR", "Rs"),
        CurrencyChoice("নেপাল", "NPR", "रू"),
        CurrencyChoice("সংযুক্ত আরব আমিরাত", "AED", "د.إ"),
        CurrencyChoice("সৌদি আরব", "SAR", "ر.س"),
        CurrencyChoice("কাতার", "QAR", "ر.ق"),
        CurrencyChoice("কুয়েত", "KWD", "د.ك"),
        CurrencyChoice("ইরাক", "IQD", "ع.د")
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("মুদ্রা নির্বাচন করুন") },
        text = {
            Column(modifier = Modifier.heightIn(max = 470.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                choices.forEach { item ->
                    if (item.code == currentCode) {
                        Button(onClick = { onSelect(item.code, item.symbol) }, modifier = Modifier.fillMaxWidth()) { Text("${item.country} — ${item.code}  ${item.symbol}") }
                    } else {
                        OutlinedButton(onClick = { onSelect(item.code, item.symbol) }, modifier = Modifier.fillMaxWidth()) { Text("${item.country} — ${item.code}  ${item.symbol}") }
                    }
                }
                Text("এটি শুধু display symbol/unit বদলায়; টাকার মান convert করে না।", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } }
    )
}

@Composable
private fun ReminderSettingsDialog(context: Context, prefs: android.content.SharedPreferences, onDismiss: () -> Unit) {
    var d30 by remember { mutableStateOf(prefs.getBoolean("reminder_30", false)) }
    var d15 by remember { mutableStateOf(prefs.getBoolean("reminder_15", false)) }
    var d7 by remember { mutableStateOf(prefs.getBoolean("reminder_7", true)) }
    var d3 by remember { mutableStateOf(prefs.getBoolean("reminder_3", true)) }
    var d0 by remember { mutableStateOf(prefs.getBoolean("reminder_0", true)) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        Toast.makeText(context, if (granted) "নোটিফিকেশন অনুমতি দেওয়া হয়েছে" else "নোটিফিকেশন অনুমতি দেওয়া হয়নি", Toast.LENGTH_SHORT).show()
    }

    fun save() {
        prefs.edit().putBoolean("reminder_30", d30).putBoolean("reminder_15", d15).putBoolean("reminder_7", d7).putBoolean("reminder_3", d3).putBoolean("reminder_0", d0).apply()
        DueReminderScheduler.schedule(context)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("বাকি পরিশোধের নোটিফিকেশন") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ReminderSwitch("৩০ দিন আগে", d30) { d30 = it }
                ReminderSwitch("১৫ দিন আগে", d15) { d15 = it }
                ReminderSwitch("৭ দিন আগে", d7) { d7 = it }
                ReminderSwitch("৩ দিন আগে", d3) { d3 = it }
                ReminderSwitch("নির্ধারিত দিনে", d0) { d0 = it }
                if (Build.VERSION.SDK_INT >= 33) {
                    OutlinedButton(onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }, modifier = Modifier.fillMaxWidth()) { Text("নোটিফিকেশন অনুমতি দিন") }
                }
                Text("রিমাইন্ডার লোকাল ডাটাবেস দেখে কাজ করবে; ইন্টারনেট প্রয়োজন নেই।", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = { save(); onDismiss() }) { Text("সেভ") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } }
    )
}

@Composable
private fun ReminderSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun PlanPurchaseDialog(context: Context, onDismiss: () -> Unit) {
    var plan by remember { mutableStateOf("মাসিক") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("প্রিমিয়াম প্ল্যান") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("মাসিক", "বার্ষিক", "Lifetime").forEach { item ->
                    if (plan == item) Button(onClick = { plan = item }, modifier = Modifier.fillMaxWidth()) { Text(item) }
                    else OutlinedButton(onClick = { plan = item }, modifier = Modifier.fillMaxWidth()) { Text(item) }
                }
                Text("৩০ দিনের trial শেষে পুরনো ডেটা থাকবে; নতুন Add/Edit সীমিত হবে। $plan প্ল্যানের দাম ও activation পেতে যোগাযোগ করুন।", style = MaterialTheme.typography.bodySmall)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { purchaseWhatsAppV14(context, plan) }, modifier = Modifier.weight(1f)) { Text("WhatsApp") }
                    OutlinedButton(onClick = { purchaseSmsV14(context, plan) }, modifier = Modifier.weight(1f)) { Text("SMS") }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { purchaseMessengerV14(context, plan) }, modifier = Modifier.weight(1f)) { Text("Messenger") }
                    OutlinedButton(onClick = { openUrlV14(context, WEBSITE_URL_V14) }, modifier = Modifier.weight(1f)) { Text("Website") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("বন্ধ") } }
    )
}

@Composable
private fun PinSettingsDialog(viewModel: FamilyKhataViewModel, onDismiss: () -> Unit) {
    val configured = viewModel.isPinConfigured.value
    var current by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (configured) "PIN পরিবর্তন" else "PIN সেট করুন") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (configured) OutlinedTextField(current, { current = it.filter(Char::isDigit).take(6) }, label = { Text("বর্তমান PIN") }, visualTransformation = PasswordVisualTransformation())
                OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(6) }, label = { Text("নতুন PIN") }, visualTransformation = PasswordVisualTransformation())
                OutlinedTextField(confirm, { confirm = it.filter(Char::isDigit).take(6) }, label = { Text("নতুন PIN আবার") }, visualTransformation = PasswordVisualTransformation())
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (configured) {
                    OutlinedButton(
                        onClick = { if (viewModel.disablePin(current)) onDismiss() else error = "বর্তমান PIN সঠিক নয়" },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("PIN বন্ধ করুন") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                error = when {
                    pin.length !in 4..6 -> "PIN ৪–৬ সংখ্যার হতে হবে"
                    pin != confirm -> "দুইটি PIN মিলছে না"
                    configured && !viewModel.changePin(current, pin) -> "বর্তমান PIN সঠিক নয়"
                    !configured && !viewModel.setPin(pin) -> "PIN সেট করা যায়নি"
                    else -> { onDismiss(); null }
                }
            }) { Text("সেভ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } }
    )
}

private fun purchaseMessageV14(plan: String) = "আমি হিসাবী খাতা অ্যাপের $plan প্ল্যান নিতে চাই। পেমেন্ট ও activation নির্দেশনা দিন।"

private fun purchaseWhatsAppV14(context: Context, plan: String) {
    openUrlV14(context, "https://wa.me/$SUPPORT_PHONE?text=${Uri.encode(purchaseMessageV14(plan))}")
}

private fun purchaseSmsV14(context: Context, plan: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$SUPPORT_PHONE")
            putExtra("sms_body", purchaseMessageV14(plan))
        })
    }
}

private fun purchaseMessengerV14(context: Context, plan: String) {
    val message = purchaseMessageV14(plan)
    runCatching {
        context.startActivity(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            setPackage("com.facebook.orca")
        })
    }.recoverCatching {
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }, "যোগাযোগ করুন"))
    }
}

private fun openSupportChooser(context: Context) {
    runCatching {
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "হিসাবী খাতা সাপোর্ট প্রয়োজন।")
        }, "যোগাযোগের মাধ্যম বেছে নিন"))
    }
}

private fun shareAppV14(context: Context) {
    runCatching {
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "হিসাবী খাতা – আয়, খরচ ও বাকি হিসাব\n$WEBSITE_URL_V14")
        }, "অ্যাপ শেয়ার করুন"))
    }
}

private fun openPlayStoreV14(context: Context) {
    val pkg = "com.familykhata.app"
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))) }
        .recoverCatching { openUrlV14(context, "https://play.google.com/store/apps/details?id=$pkg") }
}

private fun openUrlV14(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        .onFailure { Toast.makeText(context, "লিংক খোলা যায়নি", Toast.LENGTH_SHORT).show() }
}
