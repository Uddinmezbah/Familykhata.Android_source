package com.familykhata.app.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
internal fun TopCornerMenuButton(
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surfaceVariant
            ),
        border =
            BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(
                    alpha = 0.22f
                )
            )
    ) {
        Text(
            v15Text(
                "☰",
                "☰"
            ),
            modifier =
                Modifier.padding(
                    horizontal = 12.dp,
                    vertical = 10.dp
                ),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight =
                FontWeight.Black
        )
    }
}

@Composable
internal fun V14SettingsScreen(
    viewModel: FamilyKhataViewModel,
    onClose: () -> Unit,
    onOpenLedger: () -> Unit,
    onOpenProducts: () -> Unit,
    onOpenMore: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(V14_PREFS, Context.MODE_PRIVATE) }
    BackHandler { onClose() }

    var profileName by remember { mutableStateOf(prefs.getString("profile_name", "") ?: "") }
    var businessName by remember { mutableStateOf(prefs.getString("business_name", "") ?: "") }
    var businessType by remember { mutableStateOf(prefs.getString("business_type", "") ?: "") }
    var businessAddress by remember { mutableStateOf(prefs.getString("business_address", "") ?: "") }
    var businessLogoPath by remember { mutableStateOf(prefs.getString("business_logo_path", "") ?: "") }
    var profilePhone by remember { mutableStateOf(prefs.getString("profile_phone", "") ?: "") }
    var showProfile by remember { mutableStateOf(false) }
    var showLanguage by remember { mutableStateOf(false) }
    var showCurrency by remember { mutableStateOf(false) }
    var showReminder by remember { mutableStateOf(false) }
    var showInventoryAlerts by remember { mutableStateOf(false) }
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
            Text(v15Text("সেটিংস","Settings"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            Text("v1.5", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
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
                    Text(profileName.ifBlank { v15Text("আপনার প্রোফাইল","Your profile") }, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                    if (businessName.isNotBlank()) Text(businessName, style = MaterialTheme.typography.bodySmall)
                    if (businessType.isNotBlank()) Text(v15Text(v15Text("দোকানের ধরন: $businessType","Business type: $businessType"),"Business type: $businessType"), style = MaterialTheme.typography.bodySmall)
                    if (businessAddress.isNotBlank()) Text(v15Text("ঠিকানা: $businessAddress","Address: $businessAddress"), style = MaterialTheme.typography.bodySmall)
                    if (profilePhone.isNotBlank()) Text(profilePhone, style = MaterialTheme.typography.bodySmall)
                    if (profileName.isBlank() && profilePhone.isBlank()) Text(v15Text("নাম, দোকানের ধরন, ঠিকানা ও ফোন যোগ করুন","Add name, business type, address and phone"), style = MaterialTheme.typography.bodySmall)
                }
                Text("›", style = MaterialTheme.typography.headlineSmall)
            }
        }

        SettingsActionCard("🌐", v15Text("ভাষা পরিবর্তন","Change language"), v15Text("বাংলা / English","Bangla / English")) { showLanguage = true }
        SettingsActionCard("★", v15Text("প্রিমিয়াম হয়ে যান","Go Premium"), v15Text("মাসিক • বার্ষিক • Lifetime","Monthly • Yearly • Lifetime")) { showPlan = true }
        SettingsActionCard("💬", v15Text("তাগাদা মেসেজ","Reminder message"), v15Text("SMS/WhatsApp-এ প্রস্তুত বার্তা; আলাদা SMS প্যাক এখন লাগবে না","Ready message for SMS/WhatsApp; no separate SMS pack needed")) { showSmsInfo = true }
        SettingsActionCard("🔒", v15Text("নিরাপত্তা PIN পরিবর্তন","Change security PIN"), v15Text("ডাটা ডিলিট করার PIN পরিবর্তন করুন","Change the PIN required for deleting data")) { showPin = true }
        SettingsActionCard("🔔", v15Text("বাকি পরিশোধের নোটিফিকেশন","Due payment notifications"), v15Text("৩০/১৫/৭/৩ দিন আগে এবং নির্ধারিত দিনে মনে করাবে","Remind 30/15/7/3 days before and on the due date")) { showReminder = true }
        SettingsActionCard("📦", v15Text("পণ্য ও Expiry নোটিফিকেশন","Product & Expiry notifications"), v15Text("Low stock, Out of stock এবং Expiry reminder","Low stock, out-of-stock and expiry reminders")) { showInventoryAlerts = true }
        SettingsActionCard("💱", v15Text("মুদ্রা পরিবর্তন করুন","Change currency"), v15Text("প্রদর্শনের মুদ্রা বদলাবে; FX conversion হবে না","Changes display currency only; no FX conversion")) { showCurrency = true }

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
                    Text(v15Text("হোম সারাংশ","Home summary"), fontWeight = FontWeight.Bold)
                    Text(if (summaryVisible) v15Text("টাকার সারাংশ দেখা যাচ্ছে","Money summary is visible") else v15Text("টাকার সারাংশ লুকানো আছে","Money summary is hidden"), style = MaterialTheme.typography.bodySmall)
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

        SettingsSectionTitle(v15Text("দ্রুত কাজ","Quick actions"))
        SettingsActionCard("👥", v15Text("কাস্টমার / ব্যক্তি খাতা","Customer / person ledger"), v15Text("নাম বা ফোন দিয়ে খুঁজুন, বাকি ও লেনদেন দেখুন","Search by name or phone and view dues and transactions")) { onOpenLedger() }
        SettingsActionCard("📦", v15Text("পণ্য / স্টক / Expiry","Products / Stock / Expiry"), v15Text("ব্যাচ, কেনার তারিখ, মেয়াদ ও low-stock দেখুন","View batch, purchase date, expiry and low stock")) { onOpenProducts() }
        SettingsActionCard("↥", v15Text("ডাটা ব্যাকআপ ও রিপোর্ট","Data backup & reports"), v15Text("JSON Backup/Restore এবং CSV রিপোর্ট","JSON backup/restore and CSV reports")) { onOpenMore() }
        SettingsActionCard("▶", v15Text("কিভাবে ব্যবহার করব?","How to use?"), v15Text("ব্যবহারের নিয়ম ও ভিডিও টিউটোরিয়াল","Usage guide and video tutorial")) { openUrlV14(context, TUTORIAL_URL_V14) }
        SettingsActionCard("☏", v15Text("আমাদের সাথে যোগাযোগ করুন","Contact us"), "WhatsApp / SMS / Website") { openSupportChooser(context) }

        SettingsSectionTitle(v15Text("শেয়ার ও তথ্য","Share & information"))
        SettingsActionCard("↗", v15Text(v15Text("অ্যাপ শেয়ার করুন","Share app"),"Share app"), v15Text("পরিবার, বন্ধু বা ব্যবসায়িক পরিচিতদের পাঠান","Share with family, friends or business contacts")) { shareAppV14(context) }
        SettingsActionCard("★", v15Text("রিভিউ দিন","Write a review"), v15Text("Play Store প্রকাশের পর রেটিং দিন","Rate the app after it is published on Play Store")) { openPlayStoreV14(context) }
        SettingsActionCard("▦", v15Text("আরও অ্যাপ","More apps"), v15Text("ডেভেলপারের অন্যান্য প্রজেক্ট দেখুন","View other developer projects")) { openUrlV14(context, MORE_APPS_URL_V14) }
        SettingsActionCard("🌍", v15Text("ওয়েবসাইট","Website"), v15Text("হিসাবী খাতার অফিসিয়াল ওয়েব পেজ","Official Hisabi Khata web page")) { openUrlV14(context, WEBSITE_URL_V14) }
        SettingsActionCard("✦", v15Text("ফিচার রিকোয়েস্ট","Feature request"), v15Text("যে নতুন সুবিধা চান তা জানান","Tell us which new feature you want")) { openUrlV14(context, FEATURE_URL_V14) }

        Text(
            v15Text("হিসাবী খাতা v1.5 • Offline-first • লোকাল ডেটা","Hisabi Khata v1.5 • Offline-first • Local data"),
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (showProfile) {
        V15BusinessProfileDialog(
            viewModel = viewModel,
            initialName = profileName,
            initialBusiness = businessName,
            initialPhone = profilePhone,
            initialBusinessType = businessType,
            initialAddress = businessAddress,
            initialLogoPath = businessLogoPath,
            onDismiss = { showProfile = false }
        ) { name, business, phone, type, address, logo ->
            profileName = name
            businessName = business
            profilePhone = phone
            businessType = type
            businessAddress = address
            businessLogoPath = logo

            prefs.edit()
                .putString("profile_name", name)
                .putString("business_name", business)
                .putString("profile_phone", phone)
                .putString("business_type", type)
                .putString("business_address", address)
                .putString("business_logo_path", logo)
                .apply()

            showProfile = false
        }
    }
    if (showLanguage) LanguageDialog(context) { showLanguage = false }
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
    if (showInventoryAlerts) {
        V15InventoryNotificationSettingsDialog {
            showInventoryAlerts = false
        }
    }
    if (showPlan) V15PremiumDialog { showPlan = false }
    if (showPin) PinSettingsDialog(viewModel) { showPin = false }
    if (showSmsInfo) {
        AlertDialog(
            onDismissRequest = { showSmsInfo = false },
            title = { Text(v15Text("তাগাদা মেসেজ","Reminder message")) },
            text = { Text(v15Text("প্রতিটি বাকি এন্ট্রির নিচে SMS, WhatsApp ও Call থাকবে। SMS/WhatsApp আপনার ফোনের অ্যাপ খুলে প্রস্তুত বার্তা দেবে। তাই আলাদা SMS credit কেনার প্রয়োজন নেই।","Each due entry includes SMS, WhatsApp and Call options. SMS/WhatsApp opens your phone app with a ready message.")) },
            confirmButton = { TextButton(onClick = { showSmsInfo = false }) { Text(v15Text("ঠিক আছে","OK")) } }
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
    initialBusinessType: String,
    initialAddress: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var business by remember { mutableStateOf(initialBusiness) }
    var phone by remember { mutableStateOf(initialPhone) }
    var businessType by remember { mutableStateOf(initialBusinessType) }
    var address by remember { mutableStateOf(initialAddress) }

    var showTypePicker by remember { mutableStateOf(false) }
    var customType by remember { mutableStateOf("") }

    val businessTypes = listOf(
        v15Text("ফার্মেসি","Pharmacy"),
        v15Text("মুদি দোকান","Grocery"),
        v15Text("ইলেকট্রনিক্স","Electronics"),
        v15Text("মোবাইল ও এক্সেসরিজ","Mobile & Accessories"),
        v15Text("ফ্যাশন","Fashion / Clothing"),
        v15Text("কসমেটিকস","Cosmetics"),
        v15Text("রেস্টুরেন্ট","Restaurant / Food"),
        v15Text("হার্ডওয়্যার","Hardware"),
        v15Text("স্টেশনারি","Stationery"),
        "E-commerce",
        "Wholesale"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(v15Text("প্রোফাইল ও ব্যবসার তথ্য","Profile & business information")) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(v15Text("আপনার নাম","Your name")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = business,
                    onValueChange = { business = it },
                    label = { Text(v15Text("দোকান / প্রতিষ্ঠানের নাম","Business name")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedButton(
                    onClick = { showTypePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (businessType.isBlank())
                            v15Text("দোকানের ধরন নির্বাচন করুন","Select business type")
                        else
                            v15Text("দোকানের ধরন: $businessType","Business type: $businessType")
                    )
                }

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(v15Text("দোকানের ঠিকানা","Business address")) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(v15Text("ফোন","Phone")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        name.trim(),
                        business.trim(),
                        phone.trim(),
                        businessType.trim(),
                        address.trim()
                    )
                }
            ) {
                Text(v15Text("সেভ","Save"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(v15Text("বাতিল","Cancel"))
            }
        }
    )

    if (showTypePicker) {
        AlertDialog(
            onDismissRequest = { showTypePicker = false },
            title = { Text(v15Text("দোকানের ধরন নির্বাচন করুন","Select business type")) },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 450.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    businessTypes.forEach { type ->
                        OutlinedButton(
                            onClick = {
                                businessType = type
                                showTypePicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(type)
                        }
                    }

                    OutlinedTextField(
                        value = customType,
                        onValueChange = { customType = it },
                        label = { Text(v15Text("অন্যান্য / Custom type","Other / Custom type")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (customType.isNotBlank()) {
                                businessType = customType.trim()
                                showTypePicker = false
                            }
                        },
                        enabled = customType.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(v15Text("এই ধরন ব্যবহার করুন","Use this type"))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTypePicker = false }) {
                    Text(v15Text("বন্ধ","Close"))
                }
            }
        )
    }
}

@Composable
private fun LanguageDialog(context: Context, onDismiss: () -> Unit) {
    val current = V15LanguageState.languageCode ?: "bn"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select app language") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (current == "bn") {
                    Button(onClick = { V15LanguageState.setLanguage(context, "bn"); onDismiss() }, modifier = Modifier.fillMaxWidth()) { Text("বাংলা ✓") }
                    OutlinedButton(onClick = { V15LanguageState.setLanguage(context, "en"); onDismiss() }, modifier = Modifier.fillMaxWidth()) { Text("English") }
                } else {
                    OutlinedButton(onClick = { V15LanguageState.setLanguage(context, "bn"); onDismiss() }, modifier = Modifier.fillMaxWidth()) { Text("বাংলা") }
                    Button(onClick = { V15LanguageState.setLanguage(context, "en"); onDismiss() }, modifier = Modifier.fillMaxWidth()) { Text("English ✓") }
                }
                Text("Bangla and English are the supported languages in v1.5.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

private data class CurrencyChoice(val country: String, val code: String, val symbol: String)

@Composable
private fun CurrencyDialog(currentCode: String, onDismiss: () -> Unit, onSelect: (String, String) -> Unit) {
    val choices = listOf(
        CurrencyChoice("বাংলাদেশ", "BDT", "৳"),
        CurrencyChoice(v15Text("ভারত","India"), "INR", "₹"),
        CurrencyChoice(v15Text("পাকিস্তান","Pakistan"), "PKR", "Rs"),
        CurrencyChoice(v15Text("শ্রীলঙ্কা","Sri Lanka"), "LKR", "Rs"),
        CurrencyChoice(v15Text("নেপাল","Nepal"), "NPR", "रू"),
        CurrencyChoice(v15Text("সংযুক্ত আরব আমিরাত","United Arab Emirates"), "AED", "د.إ"),
        CurrencyChoice(v15Text("সৌদি আরব","Saudi Arabia"), "SAR", "ر.س"),
        CurrencyChoice(v15Text("কাতার","Qatar"), "QAR", "ر.ق"),
        CurrencyChoice(v15Text("কুয়েত","Kuwait"), "KWD", "د.ك"),
        CurrencyChoice(v15Text("ইরাক","Iraq"), "IQD", "ع.د")
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(v15Text("মুদ্রা নির্বাচন করুন","Select currency")) },
        text = {
            Column(modifier = Modifier.heightIn(max = 470.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                choices.forEach { item ->
                    if (item.code == currentCode) {
                        Button(onClick = { onSelect(item.code, item.symbol) }, modifier = Modifier.fillMaxWidth()) { Text("${item.country} — ${item.code}  ${item.symbol}") }
                    } else {
                        OutlinedButton(onClick = { onSelect(item.code, item.symbol) }, modifier = Modifier.fillMaxWidth()) { Text("${item.country} — ${item.code}  ${item.symbol}") }
                    }
                }
                Text(v15Text("এটি শুধু display symbol/unit বদলায়; টাকার মান convert করে না।","This changes only the display currency; values are not converted."), style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(v15Text("বাতিল","Cancel")) } }
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
        Toast.makeText(context, if (granted) v15Text("নোটিফিকেশন অনুমতি দেওয়া হয়েছে","Notification permission granted") else v15Text("নোটিফিকেশন অনুমতি দেওয়া হয়নি","Notification permission denied"), Toast.LENGTH_SHORT).show()
    }

    fun save() {
        prefs.edit().putBoolean("reminder_30", d30).putBoolean("reminder_15", d15).putBoolean("reminder_7", d7).putBoolean("reminder_3", d3).putBoolean("reminder_0", d0).apply()
        DueReminderScheduler.schedule(context)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(v15Text("বাকি পরিশোধের নোটিফিকেশন","Due payment notifications")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ReminderSwitch(v15Text("৩০ দিন আগে","30 days before"), d30) { d30 = it }
                ReminderSwitch(v15Text("১৫ দিন আগে","15 days before"), d15) { d15 = it }
                ReminderSwitch(v15Text("৭ দিন আগে","7 days before"), d7) { d7 = it }
                ReminderSwitch(v15Text("৩ দিন আগে","3 days before"), d3) { d3 = it }
                ReminderSwitch(v15Text("নির্ধারিত দিনে","On due date"), d0) { d0 = it }
                if (Build.VERSION.SDK_INT >= 33) {
                    OutlinedButton(onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }, modifier = Modifier.fillMaxWidth()) { Text(v15Text("নোটিফিকেশন অনুমতি দিন","Allow notifications")) }
                }
                Text(v15Text("রিমাইন্ডার লোকাল ডাটাবেস দেখে কাজ করবে; ইন্টারনেট প্রয়োজন নেই।","Reminders use the local database; internet is not required."), style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = { save(); onDismiss() }) { Text(v15Text("সেভ","Save")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(v15Text("বাতিল","Cancel")) } }
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
private fun PinSettingsDialog(viewModel: FamilyKhataViewModel, onDismiss: () -> Unit) {
    val configured = viewModel.isPinConfigured.value
    var current by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (configured) v15Text("PIN পরিবর্তন","Change PIN") else v15Text("PIN সেট করুন","Set PIN")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (configured) OutlinedTextField(current, { current = it.filter(Char::isDigit).take(6) }, label = { Text(v15Text("বর্তমান PIN","Current PIN")) }, visualTransformation = PasswordVisualTransformation())
                OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(6) }, label = { Text(v15Text("নতুন PIN","New PIN")) }, visualTransformation = PasswordVisualTransformation())
                OutlinedTextField(confirm, { confirm = it.filter(Char::isDigit).take(6) }, label = { Text(v15Text("নতুন PIN আবার","Confirm new PIN")) }, visualTransformation = PasswordVisualTransformation())
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                error = when {
                    pin.length !in 4..6 -> v15Text("PIN ৪–৬ সংখ্যার হতে হবে","PIN must be 4–6 digits")
                    pin != confirm -> v15Text("দুইটি PIN মিলছে না","PINs do not match")
                    configured && !viewModel.changePin(current, pin) -> v15Text("বর্তমান PIN সঠিক নয়","Current PIN is incorrect")
                    !configured && !viewModel.setPin(pin) -> v15Text("PIN সেট করা যায়নি","Unable to set PIN")
                    else -> { onDismiss(); null }
                }
            }) { Text(v15Text("সেভ","Save")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(v15Text("বাতিল","Cancel")) } }
    )
}

private fun openSupportChooser(context: Context) {
    runCatching {
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, v15Text("হিসাবী খাতা সাপোর্ট প্রয়োজন।","I need Hisabi Khata support."))
        }, v15Text("যোগাযোগের মাধ্যম বেছে নিন","Choose contact method")))
    }
}

private fun shareAppV14(context: Context) {
    runCatching {
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, v15Text("হিসাবী খাতা – আয়, খরচ ও বাকি হিসাব\n$WEBSITE_URL_V14","Hisabi Khata – income, expenses and dues\n$WEBSITE_URL_V14"))
        }, v15Text("অ্যাপ শেয়ার করুন","Share app")))
    }
}

private fun openPlayStoreV14(context: Context) {
    val pkg = "com.familykhata.app"
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))) }
        .recoverCatching { openUrlV14(context, "https://play.google.com/store/apps/details?id=$pkg") }
}

private fun openUrlV14(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        .onFailure { Toast.makeText(context, v15Text("লিংক খোলা যায়নি","Unable to open link"), Toast.LENGTH_SHORT).show() }
}
