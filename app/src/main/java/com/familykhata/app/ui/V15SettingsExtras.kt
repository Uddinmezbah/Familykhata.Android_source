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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.familykhata.app.InventoryReminderScheduler
import java.io.File

private const val V15_SETTINGS_PREFS = "hisabi_khata_v14_settings"
private const val V15_SUPPORT_PHONE = "8801886665676"

private data class ShopTypeChoice(
    val bn: String,
    val en: String
) {
    val display: String
        get() = "$bn / $en"
}

private val V15_SHOP_TYPES = listOf(
    ShopTypeChoice("ফার্মেসি / মেডিসিন", "Pharmacy / Medicine"),
    ShopTypeChoice("মুদি দোকান", "Grocery"),
    ShopTypeChoice("ফল ও সবজি", "Fruit & Vegetable"),
    ShopTypeChoice("বেকারি", "Bakery"),
    ShopTypeChoice("রেস্টুরেন্ট", "Restaurant"),
    ShopTypeChoice("ক্যাটারিং", "Catering"),
    ShopTypeChoice("ইলেকট্রনিক্স", "Electronics"),
    ShopTypeChoice("মোবাইল", "Mobile"),
    ShopTypeChoice("গ্যাজেট", "Gadgets"),
    ShopTypeChoice("কম্পিউটার", "Computer"),
    ShopTypeChoice("ফ্যাশন / পোশাক", "Fashion / Clothing"),
    ShopTypeChoice("জুতা", "Shoes"),
    ShopTypeChoice("ব্যাগ / লাগেজ", "Bag / Luggage"),
    ShopTypeChoice("কসমেটিকস", "Cosmetics"),
    ShopTypeChoice("বিউটি / সেলুন / স্পা", "Beauty / Salon / Spa"),
    ShopTypeChoice("জুয়েলারি", "Jewellery"),
    ShopTypeChoice("লেদার পণ্য", "Leather Goods"),
    ShopTypeChoice("বই", "Books"),
    ShopTypeChoice("স্টেশনারি", "Stationery"),
    ShopTypeChoice("হার্ডওয়্যার", "Hardware"),
    ShopTypeChoice("নির্মাণ সামগ্রী", "Construction Materials"),
    ShopTypeChoice("ফার্নিচার", "Furniture"),
    ShopTypeChoice("গাড়ি / মোটরসাইকেল", "Vehicle / Motorcycle"),
    ShopTypeChoice("অটো পার্টস", "Auto Parts"),
    ShopTypeChoice("স্পোর্টস", "Sports"),
    ShopTypeChoice("জিম / ফিটনেস", "Gym / Fitness"),
    ShopTypeChoice("গিফট", "Gift"),
    ShopTypeChoice("খেলনা", "Toys"),
    ShopTypeChoice("মোবাইল রিচার্জ", "Mobile Top-up"),
    ShopTypeChoice("ই-কমার্স", "E-commerce"),
    ShopTypeChoice("F-commerce / Facebook", "F-commerce / Facebook"),
    ShopTypeChoice("পাইকারি", "Wholesale"),
    ShopTypeChoice("ট্রেডিং", "Trading"),
    ShopTypeChoice("কাঁচামাল", "Raw Materials"),
    ShopTypeChoice("পোল্ট্রি", "Poultry"),
    ShopTypeChoice("কৃষি / এগ্রো", "Agriculture / Agro"),
    ShopTypeChoice("হস্তশিল্প", "Handicraft"),
    ShopTypeChoice("ম্যানুফ্যাকচারিং", "Manufacturing"),
    ShopTypeChoice("সার্ভিস ও রিপেয়ার", "Service & Repair"),
    ShopTypeChoice("লন্ড্রি", "Laundry"),
    ShopTypeChoice("ট্রাভেল / টিকেট", "Travel / Ticket"),
    ShopTypeChoice("কার রেন্টাল", "Car Rental"),
    ShopTypeChoice("অন্যান্য", "Other")
)

@Composable
internal fun V15BusinessProfileDialog(
    initialName: String,
    initialBusiness: String,
    initialPhone: String,
    initialBusinessType: String,
    initialAddress: String,
    initialLogoPath: String,
    onDismiss: () -> Unit,
    onSave: (
        String,
        String,
        String,
        String,
        String,
        String
    ) -> Unit
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf(initialName) }
    var business by remember { mutableStateOf(initialBusiness) }
    var phone by remember { mutableStateOf(initialPhone) }
    var businessType by remember { mutableStateOf(initialBusinessType) }
    var address by remember { mutableStateOf(initialAddress) }
    var logoPath by remember { mutableStateOf(initialLogoPath) }

    var showTypePicker by remember { mutableStateOf(false) }

    val logoLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            if (uri != null) {
                runCatching {
                    val file = File(context.filesDir, "hisabi_shop_logo.img")

                    context.contentResolver
                        .openInputStream(uri)
                        ?.use { input ->
                            file.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }

                    require(file.exists() && file.length() > 0L)

                    logoPath = file.absolutePath
                }.onFailure {
                    Toast.makeText(
                        context,
                        v15Text(
                            v15Text("লোগো সংরক্ষণ করা যায়নি","Unable to save logo"),
                            "Could not save logo"
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                v15Text(
                    v15Text("প্রোফাইল ও ব্যবসার তথ্য","Profile & business information"),
                    "Profile & business"
                )
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {

                if (logoPath.isNotBlank()) {
                    V15ProfileLogo(logoPath)
                }

                OutlinedButton(
                    onClick = { logoLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (logoPath.isBlank()) {
                            v15Text(
                                v15Text("＋ দোকানের লোগো যোগ করুন","＋ Add business logo"),
                                "＋ Add shop logo"
                            )
                        } else {
                            v15Text(
                                v15Text("দোকানের লোগো পরিবর্তন করুন","Change business logo"),
                                "Change shop logo"
                            )
                        }
                    )
                }

                if (logoPath.isNotBlank()) {
                    TextButton(
                        onClick = {
                            runCatching {
                                File(logoPath).delete()
                            }
                            logoPath = ""
                        }
                    ) {
                        Text(
                            v15Text(
                                v15Text("লোগো সরান","Remove logo"),
                                "Remove logo"
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = {
                        Text(v15Text("আপনার নাম", "Your name"))
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = business,
                    onValueChange = { business = it },
                    label = {
                        Text(
                            v15Text(
                                v15Text("দোকান / প্রতিষ্ঠানের নাম","Business name"),
                                "Shop / business name"
                            )
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedButton(
                    onClick = { showTypePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (businessType.isBlank()) {
                            v15Text(
                                v15Text("দোকানের ধরন নির্বাচন করুন","Select business type"),
                                "Select business type"
                            )
                        } else {
                            businessType
                        }
                    )
                }

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = {
                        Text(
                            v15Text(
                                v15Text("দোকানের ঠিকানা","Business address"),
                                "Shop address"
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = {
                        Text(v15Text("ফোন", "Phone"))
                    },
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
                        address.trim(),
                        logoPath
                    )
                }
            ) {
                Text(v15Text("আপডেট করুন", "Update"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(v15Text("বাতিল", "Cancel"))
            }
        }
    )

    if (showTypePicker) {
        V15ShopTypePicker(
            current = businessType,
            onDismiss = { showTypePicker = false },
            onSelect = {
                businessType = it
                showTypePicker = false
            }
        )
    }
}

@Composable
internal fun V15ProfileLogo(path: String) {
    val file = remember(path) {
        path.takeIf { it.isNotBlank() }?.let(::File)
    }

    if (file != null && file.exists()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            AsyncImage(
                model = file,
                contentDescription = "Shop logo",
                modifier = Modifier
                    .size(92.dp)
                    .clip(RoundedCornerShape(22.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun V15ShopTypePicker(
    current: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var custom by remember { mutableStateOf("") }

    val filtered = V15_SHOP_TYPES.filter {
        query.isBlank() ||
            it.bn.contains(query, ignoreCase = true) ||
            it.en.contains(query, ignoreCase = true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                v15Text(
                    v15Text("দোকানের ধরন নির্বাচন করুন","Select business type"),
                    "Select business type"
                )
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = {
                        Text(
                            v15Text(
                                v15Text("দোকানের ধরন খুঁজুন","Search business type"),
                                "Search business type"
                            )
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                filtered.forEach { type ->
                    val label = type.display

                    if (current == label) {
                        Button(
                            onClick = { onSelect(label) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("$label ✓")
                        }
                    } else {
                        OutlinedButton(
                            onClick = { onSelect(label) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(label)
                        }
                    }
                }

                OutlinedTextField(
                    value = custom,
                    onValueChange = { custom = it },
                    label = {
                        Text(
                            v15Text(
                                v15Text("নিজের দোকানের ধরন লিখুন","Enter custom business type"),
                                "Custom business type"
                            )
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (custom.isNotBlank()) {
                            onSelect(custom.trim())
                        }
                    },
                    enabled = custom.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        v15Text(
                            v15Text("এই ধরন ব্যবহার করুন","Use this type"),
                            "Use this type"
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(v15Text("বন্ধ", "Close"))
            }
        }
    )
}

@Composable
internal fun V15InventoryNotificationSettingsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val prefs = remember {
        context.getSharedPreferences(
            V15_SETTINGS_PREFS,
            Context.MODE_PRIVATE
        )
    }

    var lowStock by remember {
        mutableStateOf(
            prefs.getBoolean("inventory_low_stock", true)
        )
    }

    var d30 by remember {
        mutableStateOf(prefs.getBoolean("expiry_30", true))
    }

    var d15 by remember {
        mutableStateOf(prefs.getBoolean("expiry_15", false))
    }

    var d7 by remember {
        mutableStateOf(prefs.getBoolean("expiry_7", true))
    }

    var d3 by remember {
        mutableStateOf(prefs.getBoolean("expiry_3", true))
    }

    var d0 by remember {
        mutableStateOf(prefs.getBoolean("expiry_0", true))
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            Toast.makeText(
                context,
                if (granted) {
                    v15Text(
                        v15Text("নোটিফিকেশন অনুমতি দেওয়া হয়েছে","Notification permission granted"),
                        "Notification permission granted"
                    )
                } else {
                    v15Text(
                        v15Text("নোটিফিকেশন অনুমতি দেওয়া হয়নি","Notification permission denied"),
                        "Notification permission denied"
                    )
                },
                Toast.LENGTH_SHORT
            ).show()
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                v15Text(
                    v15Text("পণ্য ও Expiry নোটিফিকেশন","Product & Expiry notifications"),
                    "Product & expiry alerts"
                )
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                V15SettingSwitch(
                    v15Text(
                        "Low stock / Out of stock",
                        "Low stock / Out of stock"
                    ),
                    lowStock
                ) {
                    lowStock = it
                }

                Text(
                    v15Text(
                        "Expiry reminder",
                        "Expiry reminder"
                    ),
                    fontWeight = FontWeight.Bold
                )

                V15SettingSwitch(
                    v15Text("৩০ দিন আগে", "30 days before"),
                    d30
                ) { d30 = it }

                V15SettingSwitch(
                    v15Text("১৫ দিন আগে", "15 days before"),
                    d15
                ) { d15 = it }

                V15SettingSwitch(
                    v15Text("৭ দিন আগে", "7 days before"),
                    d7
                ) { d7 = it }

                V15SettingSwitch(
                    v15Text("৩ দিন আগে", "3 days before"),
                    d3
                ) { d3 = it }

                V15SettingSwitch(
                    v15Text("মেয়াদ শেষের দিন", "On expiry day"),
                    d0
                ) { d0 = it }

                if (Build.VERSION.SDK_INT >= 33) {
                    OutlinedButton(
                        onClick = {
                            permissionLauncher.launch(
                                Manifest.permission.POST_NOTIFICATIONS
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            v15Text(
                                v15Text("নোটিফিকেশন অনুমতি দিন","Allow notifications"),
                                "Allow notifications"
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    prefs.edit()
                        .putBoolean(
                            "inventory_low_stock",
                            lowStock
                        )
                        .putBoolean("expiry_30", d30)
                        .putBoolean("expiry_15", d15)
                        .putBoolean("expiry_7", d7)
                        .putBoolean("expiry_3", d3)
                        .putBoolean("expiry_0", d0)
                        .apply()

                    InventoryReminderScheduler.schedule(context)

                    onDismiss()
                }
            ) {
                Text(v15Text("সেভ", "Save"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(v15Text("বাতিল", "Cancel"))
            }
        }
    )
}

@Composable
private fun V15SettingSwitch(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f)
        )

        Switch(
            checked = checked,
            onCheckedChange = onChange
        )
    }
}

@Composable
internal fun V15PremiumDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedPlan by remember {
        mutableStateOf("YEARLY")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                v15Text(
                    v15Text("হিসাবী খাতা Premium","Hisabi Khata Premium"),
                    "Hisabi Khata Premium"
                )
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 540.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Text(
                            v15Text(
                                v15Text("৩০ দিন Full Premium Trial","30-day Full Premium Trial"),
                                "30-day Full Premium Trial"
                            ),
                            fontWeight = FontWeight.ExtraBold
                        )

                        Text(
                            v15Text(
                                v15Text("Trial শেষে আপনার ডেটা থাকবে।","Your data remains after the trial."),
                                "Your data remains after the trial."
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                V15PlanCard(
                    selected = selectedPlan == "MONTHLY",
                    title = v15Text("মাসিক", "Monthly"),
                    price = "৳99",
                    subtitle = v15Text(
                        v15Text("১ মাস Premium","1 month Premium"),
                        "1 month Premium"
                    )
                ) {
                    selectedPlan = "MONTHLY"
                }

                V15PlanCard(
                    selected = selectedPlan == "YEARLY",
                    title = v15Text(
                        v15Text("বার্ষিক • Best Value","Yearly • Best Value"),
                        "Yearly • Best Value"
                    ),
                    price = "৳699",
                    subtitle = v15Text(
                        v15Text("প্রায় ৪১% সাশ্রয়","Save about 41%"),
                        "Save about 41%"
                    )
                ) {
                    selectedPlan = "YEARLY"
                }

                V15PlanCard(
                    selected = selectedPlan == "LIFETIME",
                    title = v15Text(
                        "Lifetime • Launch Offer",
                        "Lifetime • Launch Offer"
                    ),
                    price = "৳1,499",
                    subtitle = v15Text(
                        "Regular ৳1,999",
                        "Regular ৳1,999"
                    )
                ) {
                    selectedPlan = "LIFETIME"
                }

                Text(
                    v15Text(
                        v15Text("Premium সুবিধা","Premium features"),
                        "Premium features"
                    ),
                    fontWeight = FontWeight.Bold
                )

                listOf(
                    v15Text(
                        v15Text("✓ Unlimited খাতা ও লেনদেন","✓ Unlimited ledgers & transactions"),
                        "✓ Unlimited ledgers & entries"
                    ),
                    v15Text(
                        v15Text("✓ Product, stock ও expiry","✓ Products, stock & expiry"),
                        "✓ Product, stock & expiry"
                    ),
                    v15Text(
                        v15Text("✓ Low-stock ও expiry alerts","✓ Low-stock & expiry alerts"),
                        "✓ Low-stock & expiry alerts"
                    ),
                    v15Text(
                        "✓ Backup / Restore / Report",
                        "✓ Backup / Restore / Report"
                    ),
                    v15Text(
                        "✓ PIN lock",
                        "✓ PIN lock"
                    ),
                    v15Text(
                        v15Text("✓ বাংলা + English","✓ Bangla + English"),
                        "✓ Bangla + English"
                    )
                ).forEach {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Text(
                    v15Text(
                        v15Text("কোনো auto-renewal নয়। Activation-এর জন্য যোগাযোগ করুন।","No auto-renewal. Contact us for activation."),
                        "No auto-renewal. Contact us for activation."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            V15PremiumContact.whatsApp(
                                context,
                                selectedPlan
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("WhatsApp")
                    }

                    OutlinedButton(
                        onClick = {
                            V15PremiumContact.sms(
                                context,
                                selectedPlan
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("SMS")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(v15Text("বন্ধ", "Close"))
            }
        }
    )
}

@Composable
private fun V15PlanCard(
    selected: Boolean,
    title: String,
    price: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
            }
        ),
        colors = CardDefaults.cardColors(
            containerColor =
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                }
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text(
                price,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

private object V15PremiumContact {

    fun whatsApp(
        context: Context,
        plan: String
    ) {
        val message =
            "Hisabi Khata Premium activation. Plan: $plan"

        val url =
            "https://wa.me/$V15_SUPPORT_PHONE?text=" +
                Uri.encode(message)

        runCatching {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(url)
                )
            )
        }.onFailure {
            Toast.makeText(
                context,
                v15Text("WhatsApp খোলা যায়নি","Unable to open WhatsApp"),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun sms(
        context: Context,
        plan: String
    ) {
        val intent =
            Intent(Intent.ACTION_SENDTO).apply {
                data =
                    Uri.parse(
                        "smsto:$V15_SUPPORT_PHONE"
                    )

                putExtra(
                    "sms_body",
                    "Hisabi Khata Premium activation. Plan: $plan"
                )
            }

        runCatching {
            context.startActivity(intent)
        }.onFailure {
            Toast.makeText(
                context,
                v15Text("SMS app খোলা যায়নি","Unable to open SMS app"),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
