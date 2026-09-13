package com.familykhata.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.familykhata.app.FamilyKhataViewModel
import com.familykhata.app.data.BakiEntryEntity
import com.familykhata.app.data.BakiPersonSummary
import com.familykhata.app.data.TransactionEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val APP_PACKAGE = "com.familykhata.app"
private const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=$APP_PACKAGE"
private const val WEBSITE_URL = "https://uddinmezbah.github.io/Familykhata.Android_source/"
private const val PRIVACY_URL = "https://uddinmezbah.github.io/Familykhata.Android_source/privacy.html"
private const val SUPPORT_URL = "https://github.com/Uddinmezbah/Familykhata.Android_source/issues/new?title=Support%3A%20"
private const val FEATURE_REQUEST_URL = "https://github.com/Uddinmezbah/Familykhata.Android_source/issues/new?title=Feature%20request%3A%20"

private val PersonalAccent = Color(0xFF6C4CCF)
private val FamilyAccent = Color(0xFF1479B8)
private val ShopAccent = Color(0xFFB85C00)
private val IncomeAccent = Color(0xFF0B7A53)
private val ExpenseAccent = Color(0xFFC4473A)
private val ReceivableAccent = Color(0xFF1565C0)
private val PayableAccent = Color(0xFFD17A00)
private val NeutralAccent = Color(0xFF5D6672)

private enum class Tab(val label: String) {
    DASHBOARD("হোম"),
    ADD("নতুন"),
    BAKI("বাকি"),
    PRODUCTS("পণ্য"),
    HISTORY("হিসাব"),
    MORE("আরও")
}

@Composable
fun FamilyKhataApp(viewModel: FamilyKhataViewModel) {
    var tab by remember { mutableStateOf(Tab.DASHBOARD) }
    var addTypePreset by remember { mutableStateOf("EXPENSE") }
    val workspace by viewModel.selectedWorkspace.collectAsState()
    val trialStatus by viewModel.trialStatus.collectAsState()
    val isAppUnlocked by viewModel.isAppUnlocked.collectAsState()
    val appContext = LocalContext.current
    V15LanguageState.ensureInitialized(appContext)
    var showSettingsMenu by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { V14DisplayState.initialize(appContext) }

    HisabiKhataTheme {
        if (V15LanguageState.languageCode == null) {
            LanguageOnboardingScreen()
        } else if (!isAppUnlocked) {
            AppLockScreen(viewModel)
        } else if (showSettingsMenu) {
            V14SettingsScreen(
                viewModel = viewModel,
                onClose = { showSettingsMenu = false },
                onOpenLedger = { showSettingsMenu = false; tab = Tab.BAKI },
                onOpenProducts = { showSettingsMenu = false; tab = Tab.PRODUCTS },
                onOpenMore = { showSettingsMenu = false; tab = Tab.MORE }
            )
        } else {
            Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    val navTabs = if (workspace == "SHOP") {
                        listOf(Tab.DASHBOARD, Tab.ADD, Tab.BAKI, Tab.PRODUCTS, Tab.HISTORY)
                    } else {
                        listOf(Tab.DASHBOARD, Tab.ADD, Tab.BAKI, Tab.HISTORY, Tab.MORE)
                    }
                    navTabs.forEach { item ->
                        val accent = tabAccent(item)
                        NavigationBarItem(
                            selected = tab == item,
                            onClick = { tab = item },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = accent,
                                selectedTextColor = accent,
                                indicatorColor = accent.copy(alpha = 0.16f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            icon = {
                                Text(
                                    tabSymbol(item),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (tab == item) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            label = {
                                Text(
                                    tabLabel(item, workspace),
                                    fontWeight = if (tab == item) FontWeight.Bold else FontWeight.Medium
                                )
                            }
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
                TopCornerMenuButton { showSettingsMenu = true }
                Spacer(Modifier.height(6.dp))
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
                Spacer(Modifier.height(8.dp))
                TrialNotice(trialStatus)
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
                            initialType = addTypePreset,
                            canWrite = !trialStatus.expired
                        )
                        Tab.BAKI -> BakiScreen(
                            viewModel = viewModel,
                            workspace = workspace,
                            canWrite = !trialStatus.expired,
                            onExit = { tab = Tab.DASHBOARD }
                        )
                        Tab.PRODUCTS -> V15InventoryScreen(
                            workspace = workspace,
                            canWrite = !trialStatus.expired,
                            onExit = { tab = Tab.DASHBOARD }
                        )
                        Tab.HISTORY -> HistoryScreen(viewModel, workspace)
                        Tab.MORE -> MoreScreen(viewModel)
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun BrandHeader(workspace: String) {
    val accent = workspaceAccent(workspace)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = accent.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        "হিসাবী খাতা",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "নিজের, পরিবারের ও দোকান/প্রতিষ্ঠানের হিসাব এক জায়গায়",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = accent
                ) {
                    Text(
                        workspaceLabel(workspace),
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            Text(
                "v1.5 • Customers, Stock & Expiry",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = accent
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
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "কোন হিসাব দেখবেন?",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WorkspaceCard(
                    symbol = "●",
                    label = "নিজের",
                    value = "PERSONAL",
                    selected = selected,
                    modifier = Modifier.weight(1f),
                    onSelect = onSelect
                )
                WorkspaceCard(
                    symbol = "⌂",
                    label = "পরিবার",
                    value = "FAMILY",
                    selected = selected,
                    modifier = Modifier.weight(1f),
                    onSelect = onSelect
                )
                WorkspaceCard(
                    symbol = "▦",
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
private fun WorkspaceCard(
    symbol: String,
    label: String,
    value: String,
    selected: String,
    modifier: Modifier,
    onSelect: (String) -> Unit
) {
    val isSelected = selected == value
    val accent = workspaceAccent(value)
    Card(
        onClick = { onSelect(value) },
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) accent else accent.copy(alpha = 0.10f),
            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 0.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) accent else accent.copy(alpha = 0.28f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 11.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                symbol,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = if (isSelected) Color.White else accent
            )
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
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
    val accent = workspaceAccent(workspace)

    if (workspace == "SHOP") {
        BusinessDashboard(
            viewModel = viewModel,
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
            color = accent
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "বর্তমান ব্যালেন্স",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
                Text(
                    if (V14DisplayState.summaryVisible) "${V14DisplayState.currencySymbol} ${money(balance)}" else "••••",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    workspaceSummary(workspace),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
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
                accentColor = IncomeAccent
            )
            MetricCard(
                title = "মোট খরচ",
                amount = totals.expense,
                modifier = Modifier.weight(1f),
                accentColor = ExpenseAccent
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
                accentColor = ReceivableAccent
            )
            MetricCard(
                title = "দেবো",
                amount = payable,
                modifier = Modifier.weight(1f),
                accentColor = PayableAccent
            )
        }

        DueDashboardSection(
            viewModel = viewModel,
            onLedger = onLedger
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = accent.copy(alpha = 0.09f)
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
    viewModel: FamilyKhataViewModel,
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
            color = ShopAccent
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    "ব্যবসার ক্যাশ ব্যালেন্স",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
                Text(
                    if (V14DisplayState.summaryVisible) "${V14DisplayState.currencySymbol} ${money(balance)}" else "••••",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "দোকান/প্রতিষ্ঠানের আয়-খরচের বর্তমান হিসাব",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
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
                accentColor = IncomeAccent
            )
            MetricCard(
                title = "মোট ক্যাশ আউট",
                amount = expense,
                modifier = Modifier.weight(1f),
                accentColor = ExpenseAccent
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
                accentColor = ReceivableAccent
            )
            MetricCard(
                title = "দেবো",
                amount = payable,
                modifier = Modifier.weight(1f),
                accentColor = PayableAccent
            )
        }

        DueDashboardSection(
            viewModel = viewModel,
            onLedger = onLedger
        )

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
                accentColor = IncomeAccent,
                modifier = Modifier.weight(1f),
                onClick = onCashIn
            )
            BusinessActionCard(
                symbol = "−",
                title = "ক্যাশ আউট",
                subtitle = "খরচ বা টাকা বের",
                accentColor = ExpenseAccent,
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
                accentColor = ReceivableAccent,
                modifier = Modifier.weight(1f),
                onClick = onLedger
            )
            BusinessActionCard(
                symbol = "≡",
                title = "লেনদেন",
                subtitle = "সব ক্যাশ ইতিহাস",
                accentColor = ShopAccent,
                modifier = Modifier.weight(1f),
                onClick = onHistory
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = ShopAccent.copy(alpha = 0.09f)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("ব্যবসার খাতা", fontWeight = FontWeight.Bold)
                Text(
                    "কাস্টমার/সাপ্লায়ার: $ledgerCount জন • পাবো ${V14DisplayState.currencySymbol} ${money(receivable)} • দেবো ${V14DisplayState.currencySymbol} ${money(payable)}",
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
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = accentColor.copy(alpha = 0.10f)
        ),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.22f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accentColor
            ) {
                Text(
                    symbol,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Text(
                title,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    amount: Double,
    modifier: Modifier = Modifier,
    accentColor: Color = NeutralAccent
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = accentColor.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.22f))
    ) {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = accentColor
                ) {
                    Text(
                        "•",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                if (V14DisplayState.summaryVisible) "${V14DisplayState.currencySymbol} ${money(amount)}" else "••••",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun AddTransactionScreen(
    viewModel: FamilyKhataViewModel,
    workspace: String,
    initialType: String,
    canWrite: Boolean
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
        if (!canWrite) {
            TrialLockedMessage()
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val expenseLabel = if (isBusiness) "ক্যাশ আউট" else "খরচ"
            val incomeLabel = if (isBusiness) "ক্যাশ ইন" else "আয়"
            TransactionTypeCard(
                label = expenseLabel,
                selected = type == "EXPENSE",
                accent = ExpenseAccent,
                modifier = Modifier.weight(1f),
                onClick = { type = "EXPENSE" }
            )
            TransactionTypeCard(
                label = incomeLabel,
                selected = type == "INCOME",
                accent = IncomeAccent,
                modifier = Modifier.weight(1f),
                onClick = { type = "INCOME" }
            )
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
            modifier = Modifier.fillMaxWidth(),
            enabled = canWrite
        ) { Text(if (isBusiness) "ক্যাশ লেনদেন সেভ করুন" else "সেভ করুন") }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun TransactionTypeCard(
    label: String,
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) accent else accent.copy(alpha = 0.10f)
        ),
        border = BorderStroke(
            1.dp,
            if (selected) accent else accent.copy(alpha = 0.25f)
        )
    ) {
        Text(
            label,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 11.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color.White else accent,
            textAlign = TextAlign.Center
        )
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
    val accent = if (item.type == "INCOME") IncomeAccent else ExpenseAccent
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = accent.copy(alpha = 0.09f)
        ),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.20f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    if (isBusiness) {
                        if (item.type == "INCOME") "ক্যাশ ইন" else "ক্যাশ আউট"
                    } else {
                        if (item.type == "INCOME") "আয়" else "খরচ"
                    },
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
                Text(
                    "${V14DisplayState.currencySymbol} ${money(item.amount)}",
                    fontWeight = FontWeight.ExtraBold,
                    color = accent
                )
            }
            Text(item.category, fontWeight = FontWeight.SemiBold)
            if (item.note.isNotBlank()) {
                Text(
                    item.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                formatDate(item.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onDelete) { Text("মুছুন") }
        }
    }
}

@Composable
private fun BakiScreen(
    viewModel: FamilyKhataViewModel,
    workspace: String,
    canWrite: Boolean,
    onExit: () -> Unit
) {
    val people by viewModel.bakiPeople.collectAsState()
    var selectedId by remember { mutableStateOf<Long?>(null) }
    val selected = selectedId?.let { id -> people.firstOrNull { it.id == id } }

    BackHandler(enabled = selected != null) { selectedId = null }
    BackHandler(enabled = selected == null) { onExit() }

    if (selected == null) {
        BakiPeopleScreen(
            people = people,
            viewModel = viewModel,
            workspace = workspace,
            canWrite = canWrite,
            onSelect = { selectedId = it.id }
        )
    } else {
        BakiEntryScreen(
            person = selected,
            viewModel = viewModel,
            workspace = workspace,
            canWrite = canWrite,
            onBack = { selectedId = null }
        )
    }
}

@Composable
private fun BakiPeopleScreen(
    people: List<BakiPersonSummary>,
    viewModel: FamilyKhataViewModel,
    workspace: String,
    canWrite: Boolean,
    onSelect: (BakiPersonSummary) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var showAdd by remember { mutableStateOf(false) }
    val personLabel = if (workspace == "SHOP") v15Text("কাস্টমার/সাপ্লায়ার", "Customer/Supplier") else v15Text("ব্যক্তি", "Person")
    val sectionTitle = if (workspace == "SHOP") v15Text("কাস্টমার/সাপ্লায়ার", "Customers & Suppliers") else v15Text("বাকি/পাওনা", "Due Accounts")
    val dueItems by viewModel.dueReceivables.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(sectionTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Text(
            v15Text("নামগুলো সামনে থাকবে। যেকোনো নাম চাপলে আলাদা খাতা খুলবে; ফোনের Back দিলে আবার এই তালিকায় ফিরবেন।", "Names stay on this list. Tap a name to open its ledger; use the phone Back button to return here."),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = { showAdd = true }, enabled = canWrite, modifier = Modifier.fillMaxWidth()) {
            Text(v15Text("＋ নতুন $personLabel যোগ করুন", "＋ Add $personLabel"))
        }
        if (!canWrite) TrialLockedMessage()

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(v15Text("নাম বা ফোন দিয়ে খুঁজুন", "Search by name or phone")) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        val filteredPeople = people.filter { person ->
            query.isBlank() || person.name.contains(query, ignoreCase = true) || person.phone.contains(query)
        }
        if (filteredPeople.isEmpty()) {
            Text(v15Text("কোনো $personLabel পাওয়া যায়নি।", "No $personLabel found."))
        } else {
            filteredPeople.forEach { person ->
                val personTone = personAccent(person.id)
                val balanceTone = balanceAccent(person.balance)
                val nextDue = dueItems.filter { it.personId == person.id }.minByOrNull { it.dueAt }
                Card(
                    onClick = { onSelect(person) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = personTone.copy(alpha = 0.09f)),
                    border = BorderStroke(1.dp, personTone.copy(alpha = 0.22f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(person.name, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium, color = personTone)
                            Text(
                                when {
                                    person.balance > 0 -> v15Text("পাবো", "Receivable")
                                    person.balance < 0 -> v15Text("দেবো", "Payable")
                                    else -> v15Text("সমান", "Settled")
                                },
                                fontWeight = FontWeight.Bold,
                                color = balanceTone
                            )
                        }
                        if (person.phone.isNotBlank()) Text(person.phone, style = MaterialTheme.typography.bodySmall)
                        Text(
                            when {
                                person.balance > 0 -> "${V14DisplayState.currencySymbol} ${money(person.balance)}"
                                person.balance < 0 -> "${V14DisplayState.currencySymbol} ${money(-person.balance)}"
                                else -> "${V14DisplayState.currencySymbol} 0"
                            },
                            fontWeight = FontWeight.Bold,
                            color = balanceTone
                        )
                        nextDue?.let {
                            Text(v15Text("পরবর্তী তারিখ: ${v13Date(it.dueAt)}", "Next due: ${v13Date(it.dueAt)}"), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text(v15Text("নতুন $personLabel", "New $personLabel")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it; error = null }, label = { Text(v15Text("নাম", "Name")) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(phone, { phone = it }, label = { Text(v15Text("ফোন (ঐচ্ছিক)", "Phone (optional)")) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isBlank()) error = v15Text("নাম লিখুন", "Enter a name")
                    else {
                        viewModel.addBakiPerson(name, phone)
                        showAdd = false
                    }
                }) { Text(v15Text("যোগ করুন", "Add")) }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text(v15Text("বাতিল", "Cancel")) } }
        )
    }
}

@Composable
private fun BakiEntryScreen(
    person: BakiPersonSummary,
    viewModel: FamilyKhataViewModel,
    workspace: String,
    canWrite: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val entriesFlow = remember(person.id) { viewModel.observeBakiEntries(person.id) }
    val entries by entriesFlow.collectAsState(initial = emptyList())
    var action by remember { mutableStateOf("GAVE") }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var dueAt by remember { mutableStateOf<Long?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<BakiEntryEntity?>(null) }
    val balanceTone = balanceAccent(person.balance)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            person.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = personAccent(person.id)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = balanceTone.copy(alpha = 0.11f),
            border = BorderStroke(1.dp, balanceTone.copy(alpha = 0.22f))
        ) {
            Text(
                when {
                    person.balance > 0 -> "বর্তমানে পাবো: ${V14DisplayState.currencySymbol} ${money(person.balance)}"
                    person.balance < 0 -> "বর্তমানে দেবো: ${V14DisplayState.currencySymbol} ${money(-person.balance)}"
                    else -> "বর্তমান হিসাব সমান"
                },
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = balanceTone
            )
        }

        PersonManagementActions(
            person = person,
            personLabel = if (workspace == "SHOP") "কাস্টমার/সাপ্লায়ার" else "ব্যক্তি",
            canWrite = canWrite,
            viewModel = viewModel,
            onDeleted = onBack
        )

        if (person.phone.isNotBlank()) {
            OutlinedButton(
                onClick = { sendLedgerSms(context, person) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("SMS-এ হিসাব পাঠান")
            }
        }

        if (!canWrite) {
            TrialLockedMessage()
        }
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
        DueDatePickerField(
            value = dueAt,
            onChange = { dueAt = it }
        )
        Button(
            onClick = {
                val value = parseAmount(amount)
                if (value == null) {
                    error = "সঠিক টাকার পরিমাণ লিখুন"
                } else {
                    viewModel.addBakiEntry(person.id, action, value, note, dueAt)
                    amount = ""
                    note = ""
                    dueAt = null
                    error = null
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = canWrite
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
                    person = person,
                    canWrite = canWrite,
                    viewModel = viewModel,
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
                    "${actionLabel(entry.action)} — ${V14DisplayState.currencySymbol} ${money(entry.amount)}\n" +
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
private fun BakiHistoryCard(
    item: BakiEntryEntity,
    person: BakiPersonSummary,
    canWrite: Boolean,
    viewModel: FamilyKhataViewModel,
    onDelete: () -> Unit
) {
    val accent = bakiActionAccent(item.action)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = accent.copy(alpha = 0.08f)
        ),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier.padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    actionLabel(item.action),
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
                Text(
                    "${V14DisplayState.currencySymbol} ${money(item.amount)}",
                    fontWeight = FontWeight.ExtraBold,
                    color = accent
                )
            }
            if (item.note.isNotBlank()) {
                Text(item.note)
            }
            item.dueAt?.let {
                Text(
                    "পরিশোধের তারিখ: ${v13Date(it)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (it < System.currentTimeMillis()) ExpenseAccent else ReceivableAccent
                )
            }
            Text(
                formatDate(item.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            BakiEntryActionRow(
                person = person,
                item = item,
                canWrite = canWrite,
                viewModel = viewModel,
                onDelete = onDelete
            )
        }
    }
}

@Composable
private fun ActionButton(
    label: String,
    value: String,
    selected: String,
    onClick: () -> Unit
) {
    val isSelected = selected == value
    val accent = bakiActionAccent(value)
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) accent else accent.copy(alpha = 0.09f)
        ),
        border = BorderStroke(1.dp, accent.copy(alpha = if (isSelected) 0.70f else 0.22f))
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else accent
        )
    }
}


@Composable
private fun MoreScreen(viewModel: FamilyKhataViewModel) {
    val context = LocalContext.current
    var backupJson by remember { mutableStateOf<String?>(null) }
    var pendingRestoreJson by remember { mutableStateOf<String?>(null) }

    val createBackupFile = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val json = backupJson
        backupJson = null
        if (uri != null && json != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                    writer.write(json)
                } ?: error("ফাইল লেখা যায়নি")
            }.onSuccess {
                toast(context, "ব্যাকআপ সেভ হয়েছে")
            }.onFailure {
                toast(context, it.message ?: "ব্যাকআপ সেভ করা যায়নি")
            }
        }
    }

    val openBackupFile = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                    reader.readText()
                } ?: error("ফাইল পড়া যায়নি")
            }.onSuccess { json ->
                pendingRestoreJson = json
            }.onFailure {
                toast(context, it.message ?: "ব্যাকআপ ফাইল পড়া যায়নি")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "আরও",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            "ডেটা নিরাপত্তা, শেয়ার, সাপোর্ট ও অ্যাপ সম্পর্কিত প্রয়োজনীয় অপশন।",
            style = MaterialTheme.typography.bodyMedium
        )

        CommercialToolsSection(viewModel)
        PurchaseAndTutorialSection()

        MoreSectionTitle("ডেটা নিরাপত্তা")
        MoreActionCard(
            symbol = "⇩",
            title = "ব্যাকআপ তৈরি করুন",
            subtitle = "সব ওয়ার্কস্পেসের আয়-খরচ ও বাকি হিসাব একটি JSON ফাইলে রাখুন"
        ) {
            viewModel.createBackup(
                onReady = { json ->
                    backupJson = json
                    val stamp = SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.US).format(Date())
                    createBackupFile.launch("HisabiKhata-backup-$stamp.json")
                },
                onError = { toast(context, it) }
            )
        }
        MoreActionCard(
            symbol = "⇧",
            title = "ব্যাকআপ রিস্টোর করুন",
            subtitle = "আগের হিসাবী খাতা ব্যাকআপ থেকে সব ডেটা ফিরিয়ে আনুন"
        ) {
            openBackupFile.launch(arrayOf("application/json", "text/plain"))
        }

        MoreSectionTitle("শেয়ার ও মতামত")
        MoreActionCard(
            symbol = "↗",
            title = "অ্যাপ শেয়ার করুন",
            subtitle = "পরিবার, বন্ধু বা ব্যবসায়িক পরিচিতদের হিসাবী খাতা জানান"
        ) {
            shareApp(context)
        }
        MoreActionCard(
            symbol = "★",
            title = "রিভিউ দিন",
            subtitle = "Play Store-এ প্রকাশের পর এখান থেকে রেটিং ও রিভিউ দেওয়া যাবে"
        ) {
            openPlayStore(context)
        }
        MoreActionCard(
            symbol = "✦",
            title = "ফিচার রিকোয়েস্ট",
            subtitle = "কোন নতুন সুবিধা চান তা আমাদের জানান"
        ) {
            openUrl(context, FEATURE_REQUEST_URL)
        }

        MoreSectionTitle("সাপোর্ট ও তথ্য")
        MoreActionCard(
            symbol = "?",
            title = "আমাদের সাথে যোগাযোগ",
            subtitle = "সাপোর্ট প্রশ্ন বা সমস্যার জন্য যোগাযোগ করুন"
        ) {
            openUrl(context, SUPPORT_URL)
        }
        MoreActionCard(
            symbol = "⌂",
            title = "ওয়েবসাইট",
            subtitle = "হিসাবী খাতার অফিসিয়াল ওয়েবসাইট"
        ) {
            openUrl(context, WEBSITE_URL)
        }
        MoreActionCard(
            symbol = "ⓘ",
            title = "Privacy Policy",
            subtitle = "আপনার ডেটা কীভাবে সংরক্ষণ ও ব্যবহার করা হয়"
        ) {
            openUrl(context, PRIVACY_URL)
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
                Text("হিসাবী খাতা v1.4", fontWeight = FontWeight.Bold)
                Text(
                    "আপনার টাকা-পয়সার সহজ হিসাব • ডেটা আপনার ডিভাইসে থাকে",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    pendingRestoreJson?.let { json ->
        AlertDialog(
            onDismissRequest = { pendingRestoreJson = null },
            title = { Text("ব্যাকআপ রিস্টোর করবেন?") },
            text = {
                Text(
                    "বর্তমান অ্যাপের সব হিসাব মুছে ব্যাকআপ ফাইলের ডেটা বসবে। " +
                        "নিশ্চিত হওয়ার আগে চাইলে বর্তমান ডেটার একটি ব্যাকআপ তৈরি করুন।"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingRestoreJson = null
                        viewModel.restoreBackup(
                            json = json,
                            onDone = { count ->
                                toast(context, "রিস্টোর সম্পন্ন: $count টি রেকর্ড")
                            },
                            onError = { toast(context, it) }
                        )
                    }
                ) { Text("রিস্টোর করুন") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreJson = null }) { Text("বাতিল") }
            }
        )
    }
}

@Composable
private fun MoreSectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun MoreActionCard(
    symbol: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val accent = when (symbol) {
        "⇩" -> IncomeAccent
        "⇧" -> ReceivableAccent
        "↗" -> FamilyAccent
        "★" -> PayableAccent
        "✦" -> PersonalAccent
        "?" -> ExpenseAccent
        "⌂" -> ShopAccent
        else -> NeutralAccent
    }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = accent.copy(alpha = 0.07f)
        ),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.16f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accent
            ) {
                Text(
                    symbol,
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun sendLedgerSms(context: Context, person: BakiPersonSummary) {
    val balanceText = when {
        person.balance > 0 -> "আপনার কাছে ${V14DisplayState.currencySymbol} ${money(person.balance)} পাওনা আছে।"
        person.balance < 0 -> "আপনাকে ${V14DisplayState.currencySymbol} ${money(-person.balance)} পরিশোধযোগ্য আছে।"
        else -> "আপনার হিসাব বর্তমানে সমান আছে।"
    }
    val message = "হিসাবী খাতা: ${person.name}, $balanceText"
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("smsto:${Uri.encode(person.phone)}")
        putExtra("sms_body", message)
    }
    runCatching { context.startActivity(intent) }
        .onFailure { toast(context, "SMS অ্যাপ খোলা যায়নি") }
}

private fun shareApp(context: Context) {
    val text = "হিসাবী খাতা – আয় ব্যয় ও বাকি\n$PLAY_STORE_URL"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "হিসাবী খাতা")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    runCatching {
        context.startActivity(Intent.createChooser(intent, "অ্যাপ শেয়ার করুন"))
    }.onFailure {
        toast(context, "শেয়ার অপশন খোলা যায়নি")
    }
}

private fun openPlayStore(context: Context) {
    val marketIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("market://details?id=$APP_PACKAGE")
    )
    runCatching { context.startActivity(marketIntent) }
        .recoverCatching { openUrl(context, PLAY_STORE_URL) }
        .onFailure { toast(context, "Play Store খোলা যায়নি") }
}

private fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }.onFailure {
        toast(context, "লিংক খোলা যায়নি")
    }
}

private fun toast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

private fun workspaceAccent(workspace: String): Color = when (workspace) {
    "PERSONAL" -> PersonalAccent
    "SHOP" -> ShopAccent
    else -> FamilyAccent
}

private fun balanceAccent(balance: Double): Color = when {
    balance > 0 -> ReceivableAccent
    balance < 0 -> PayableAccent
    else -> NeutralAccent
}

private fun bakiActionAccent(action: String): Color = when (action) {
    "GAVE", "RECEIVED_BACK" -> ReceivableAccent
    "TOOK", "PAID_BACK" -> PayableAccent
    else -> NeutralAccent
}

private fun tabAccent(tab: Tab): Color = when (tab) {
    Tab.DASHBOARD -> Color(0xFF0B6B58)
    Tab.ADD -> IncomeAccent
    Tab.BAKI -> ReceivableAccent
    Tab.PRODUCTS -> ShopAccent
    Tab.HISTORY -> Color(0xFF6C4CCF)
    Tab.MORE -> ShopAccent
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

private fun tabLabel(tab: Tab, workspace: String): String = when (tab) {
    Tab.DASHBOARD -> v15Text("হোম", "Home")
    Tab.ADD -> if (workspace == "SHOP") v15Text("ক্যাশ", "Cash") else v15Text("নতুন", "Add")
    Tab.BAKI -> if (workspace == "SHOP") v15Text("খাতা", "Ledger") else v15Text("বাকি", "Due")
    Tab.PRODUCTS -> v15Text("পণ্য", "Products")
    Tab.HISTORY -> if (workspace == "SHOP") v15Text("লেনদেন", "History") else v15Text("হিসাব", "History")
    Tab.MORE -> v15Text("আরও", "More")
}

private fun tabSymbol(tab: Tab): String = when (tab) {
    Tab.DASHBOARD -> "⌂"
    Tab.ADD -> "＋"
    Tab.BAKI -> "৳"
    Tab.PRODUCTS -> "▦"
    Tab.HISTORY -> "≡"
    Tab.MORE -> "⋯"
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
