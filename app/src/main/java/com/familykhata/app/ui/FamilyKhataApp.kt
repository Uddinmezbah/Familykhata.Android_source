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
import com.familykhata.app.BusinessMode
import com.familykhata.app.detectBusinessMode
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

    var appRefreshToken by remember {
        mutableStateOf(0L)
    }

    @Suppress("UNUSED_VARIABLE")
    val refreshDependency = appRefreshToken

    val businessType =
        appContext.getSharedPreferences(
            "hisabi_khata_v14_settings",
            Context.MODE_PRIVATE
        ).getString(
            "business_type",
            ""
        ).orEmpty()

    val businessMode =
        if (workspace == "SHOP") {
            detectBusinessMode(businessType)
        } else {
            BusinessMode.RETAIL
        }

    V15LanguageState.ensureInitialized(appContext)
    var showSettingsMenu by remember { mutableStateOf(false) }

    val deepScreenActive =
        V15DeepNavigationState.active

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
                if (!deepScreenActive) {
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
                            onClick = {
                                V15DeepNavigationState.clear()
                                tab = item
                            },
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
                                    style =
                                        MaterialTheme.typography.titleLarge,
                                    fontWeight =
                                        FontWeight.ExtraBold
                                )
                            },
                            label = {
                                Text(
                                    tabLabel(
                                        item,
                                        workspace,
                                        businessMode
                                    ),
                                    style =
                                        MaterialTheme.typography.labelMedium,
                                    fontWeight =
                                        FontWeight.Bold
                                )
                            }
                        )
                    }
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
                if (!deepScreenActive) {
                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        TopCornerMenuButton {
                            showSettingsMenu = true
                        }

                        Spacer(
                            modifier =
                                Modifier.weight(1f)
                        )

                        Button(
                            onClick = {
                                appRefreshToken =
                                    appRefreshToken + 1L

                                viewModel.refreshTrialStatus()
                            },
                            modifier =
                                Modifier.height(42.dp),
                            colors =
                                androidx.compose.material3.ButtonDefaults
                                    .buttonColors(
                                        containerColor =
                                            workspaceAccent(
                                                workspace
                                            ),
                                        contentColor =
                                            Color.White
                                    )
                        ) {
                            Text(
                                v15Text(
                                    "↻ রিফ্রেশ",
                                    "↻ Refresh"
                                ),
                                fontWeight =
                                    FontWeight.ExtraBold
                            )
                        }
                    }

                    Spacer(
                        Modifier.height(4.dp)
                    )

                    if (tab == Tab.DASHBOARD) {
                        Row(
                            modifier =
                                Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.SpaceBetween,
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {
                            Text(
                                v15Text(
                                    "হিসাবী খাতা",
                                    "Hisabi Khata"
                                ),
                                style =
                                    MaterialTheme.typography.headlineMedium,
                                fontWeight =
                                    FontWeight.ExtraBold
                            )

                            if (trialStatus.premiumUnlocked) {
                                Text(
                                    v15Text(
                                        "Premium ✓",
                                        "Premium ✓"
                                    ),
                                    style =
                                        MaterialTheme.typography.labelLarge,
                                    fontWeight =
                                        FontWeight.ExtraBold,
                                    color = IncomeAccent
                                )
                            } else {
                                TextButton(
                                    onClick = {
                                        V15DeepNavigationState.clear()
                                        tab = Tab.MORE
                                    }
                                ) {
                                    Text(
                                        if (trialStatus.expired) {
                                            v15Text(
                                                "Get Premium",
                                                "Get Premium"
                                            )
                                        } else {
                                            v15Text(
                                                "Get Premium • ${trialStatus.daysRemaining} দিন বাকি",
                                                "Get Premium • ${trialStatus.daysRemaining} days left"
                                            )
                                        },
                                        fontWeight =
                                            FontWeight.ExtraBold,
                                        color =
                                            if (trialStatus.expired)
                                                ExpenseAccent
                                            else
                                                workspaceAccent(
                                                    workspace
                                                )
                                    )
                                }
                            }
                        }

                        Spacer(
                            Modifier.height(8.dp)
                        )

                        WorkspaceSwitcher(
                            selected = workspace,
                            onSelect = {
                                viewModel.selectWorkspace(it)
                                V15DeepNavigationState.clear()
                                addTypePreset = "EXPENSE"
                                tab = Tab.DASHBOARD
                            }
                        )

                        Spacer(
                            Modifier.height(10.dp)
                        )
                    } else {
                        Spacer(
                            Modifier.height(2.dp)
                        )
                    }
                }

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
                        Tab.PRODUCTS -> {
                            when (businessMode) {
                                BusinessMode.COACHING ->
                                    V15CoachingScreen(
                                        workspace = workspace,
                                        canWrite = !trialStatus.expired,
                                        onExit = {
                                            tab = Tab.DASHBOARD
                                        }
                                    )

                                BusinessMode.DIGITAL_AGENCY ->
                                    V15AgencyScreen(
                                        workspace = workspace,
                                        canWrite = !trialStatus.expired,
                                        onExit = {
                                            tab = Tab.DASHBOARD
                                        }
                                    )

                                BusinessMode.FOOD_SERVICE ->
                                    V15FoodServiceScreen(
                                        workspace = workspace,
                                        shopType = businessType,
                                        canWrite = !trialStatus.expired,
                                        onExit = {
                                            tab = Tab.DASHBOARD
                                        }
                                    )

                                BusinessMode.AGRO ->
                                    V15AgroScreen(
                                        workspace = workspace,
                                        shopType = businessType,
                                        canWrite = !trialStatus.expired,
                                        onExit = {
                                            tab = Tab.DASHBOARD
                                        }
                                    )

                                BusinessMode.PRODUCTION ->
                                    V15ProductionScreen(
                                        workspace = workspace,
                                        shopType = businessType,
                                        canWrite = !trialStatus.expired,
                                        onExit = {
                                            tab = Tab.DASHBOARD
                                        }
                                    )

                                BusinessMode.DEALERSHIP ->
                                    V15DealershipScreen(
                                        workspace = workspace,
                                        shopType = businessType,
                                        canWrite = !trialStatus.expired,
                                        onExit = {
                                            tab = Tab.DASHBOARD
                                        }
                                    )

                                BusinessMode.SERVICE_JOB ->
                                    V15ServiceJobScreen(
                                        workspace = workspace,
                                        shopType = businessType,
                                        canWrite = !trialStatus.expired,
                                        onExit = {
                                            tab = Tab.DASHBOARD
                                        }
                                    )

                                BusinessMode.MEMBERSHIP_SERVICE ->
                                    V15MembershipScreen(
                                        workspace = workspace,
                                        canWrite = !trialStatus.expired,
                                        onExit = {
                                            tab = Tab.DASHBOARD
                                        }
                                    )

                                BusinessMode.BOOKING_RENTAL ->
                                    V15BookingScreen(
                                        workspace = workspace,
                                        shopType = businessType,
                                        canWrite = !trialStatus.expired,
                                        onExit = {
                                            tab = Tab.DASHBOARD
                                        }
                                    )

                                else ->
                                    V15InventoryScreen(
                                        workspace = workspace,
                                        shopType = businessType,
                                        canWrite = !trialStatus.expired,
                                        onExit = {
                                            tab = Tab.DASHBOARD
                                        }
                                    )
                            }
                        }
                        Tab.HISTORY ->
                            HistoryScreen(
                                viewModel = viewModel,
                                workspace = workspace,
                                canWrite = !trialStatus.expired
                            )
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
                        v15Text("হিসাবী খাতা", "Hisabi Khata"),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        v15Text("নিজের, পরিবারের ও দোকান/প্রতিষ্ঠানের হিসাব এক জায়গায়", "Personal, family and business accounts in one place"),
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
                v15Text("কোন হিসাব দেখবেন?", "Which account do you want to view?"),
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
                    label = v15Text("নিজের", "Personal"),
                    value = "PERSONAL",
                    selected = selected,
                    modifier = Modifier.weight(1f),
                    onSelect = onSelect
                )
                WorkspaceCard(
                    symbol = "⌂",
                    label = v15Text("পরিবার", "Family"),
                    value = "FAMILY",
                    selected = selected,
                    modifier = Modifier.weight(1f),
                    onSelect = onSelect
                )
                WorkspaceCard(
                    symbol = "▦",
                    label = v15Text("দোকান/\nপ্রতিষ্ঠান","Business"),
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
                    v15Text("বর্তমান ব্যালেন্স", "Current balance"),
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
                title = v15Text("মোট আয়", "Total income"),
                amount = totals.income,
                modifier = Modifier.weight(1f),
                accentColor = IncomeAccent
            )
            MetricCard(
                title = v15Text("মোট খরচ", "Total expense"),
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
                title = v15Text("পাবো", "Receivable"),
                amount = receivable,
                modifier = Modifier.weight(1f),
                accentColor = ReceivableAccent
            )
            MetricCard(
                title = v15Text("দেবো", "Payable"),
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
                Text(v15Text("হিসাব গুছিয়ে রাখুন", "Keep your accounts organized"), fontWeight = FontWeight.Bold)
                Text(
                    v15Text("নিচের “নতুন” থেকে আয়-খরচ এবং “বাকি” থেকে দেনা-পাওনা যোগ করুন।", "Use Add for income/expense and Due for receivables/payables."),
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
                    v15Text("ব্যবসার ক্যাশ ব্যালেন্স", "Business cash balance"),
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
                    v15Text("দোকান/প্রতিষ্ঠানের আয়-খরচের বর্তমান হিসাব", "Current business cash-flow summary"),
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
                title = v15Text("মোট ক্যাশ ইন", "Total cash in"),
                amount = income,
                modifier = Modifier.weight(1f),
                accentColor = IncomeAccent
            )
            MetricCard(
                title = v15Text("মোট ক্যাশ আউট", "Total cash out"),
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
                title = v15Text("পাবো", "Receivable"),
                amount = receivable,
                modifier = Modifier.weight(1f),
                accentColor = ReceivableAccent
            )
            MetricCard(
                title = v15Text("দেবো", "Payable"),
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
            v15Text("দ্রুত কাজ", "Quick actions"),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BusinessActionCard(
                symbol = "＋",
                title = v15Text("ক্যাশ ইন", "Cash in"),
                subtitle = v15Text("আয় বা টাকা জমা", "Income or money received"),
                accentColor = IncomeAccent,
                modifier = Modifier.weight(1f),
                onClick = onCashIn
            )
            BusinessActionCard(
                symbol = "−",
                title = v15Text("ক্যাশ আউট", "Cash out"),
                subtitle = v15Text("খরচ বা টাকা বের", "Expense or money paid"),
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
                title = v15Text("খাতা", "Ledger"),
                subtitle = v15Text("কাস্টমার/সাপ্লায়ার", "Customer/Supplier"),
                accentColor = ReceivableAccent,
                modifier = Modifier.weight(1f),
                onClick = onLedger
            )
            BusinessActionCard(
                symbol = "≡",
                title = v15Text("লেনদেন", "Transactions"),
                subtitle = v15Text("সব ক্যাশ ইতিহাস", "All cash history"),
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
                Text(v15Text("ব্যবসার খাতা", "Business ledger"), fontWeight = FontWeight.Bold)
                Text(
                    v15Text("কাস্টমার/সাপ্লায়ার: $ledgerCount জন • পাবো ${V14DisplayState.currencySymbol} ${money(receivable)} • দেবো ${V14DisplayState.currencySymbol} ${money(payable)}", "Customers/Suppliers: $ledgerCount • Receivable ${V14DisplayState.currencySymbol} ${money(receivable)} • Payable ${V14DisplayState.currencySymbol} ${money(payable)}"),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    v15Text("পরবর্তী ধাপে পণ্য-স্টক, ক্রয়-বিক্রি, ইনভয়েস/চালান ও রিপোর্ট যোগ হবে।", "Manage products, stock, purchases, sales and reports from the business tools."),
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
            if (isBusiness) v15Text("ক্যাশ লেনদেন", "Cash transaction") else v15Text("নতুন আয়/খরচ", "New income/expense"),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        if (isBusiness) {
            Text(
                v15Text("দোকান/প্রতিষ্ঠানের টাকা আসা বা বের হওয়ার হিসাব যোগ করুন।", "Record money received or paid by the business."),
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
            val expenseLabel = if (isBusiness) v15Text("ক্যাশ আউট", "Cash out") else v15Text("খরচ", "Expense")
            val incomeLabel = if (isBusiness) v15Text("ক্যাশ ইন", "Cash in") else v15Text("আয়", "Income")
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
            label = { Text(v15Text("টাকার পরিমাণ", "Amount")) },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            category,
            { category = it },
            label = { Text(if (isBusiness) v15Text("ক্যাটাগরি / খাত", "Category / Account") else v15Text("ক্যাটাগরি", "Category")) },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            note,
            { note = it },
            label = { Text(v15Text("নোট", "Note")) },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                val value = parseAmount(amount)
                if (value == null) {
                    error = v15Text("সঠিক টাকার পরিমাণ লিখুন", "Enter a valid amount")
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
        ) { Text(if (isBusiness) v15Text("ক্যাশ লেনদেন সেভ করুন", "Save cash transaction") else v15Text("সেভ করুন", "Save")) }
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
private fun HistoryScreen(
    viewModel: FamilyKhataViewModel,
    workspace: String,
    canWrite: Boolean
) {
    val transactions by
        viewModel.transactions.collectAsState()

    val isBusiness =
        workspace == "SHOP"

    if (transactions.isEmpty()) {
        Column(
            verticalArrangement =
                Arrangement.spacedBy(6.dp)
        ) {
            Text(
                if (isBusiness)
                    v15Text(
                        "ব্যবসার লেনদেন",
                        "Business transactions"
                    )
                else
                    v15Text(
                        "আয়-খরচের ইতিহাস",
                        "Income & expense history"
                    ),
                style =
                    MaterialTheme.typography.titleMedium,
                fontWeight =
                    FontWeight.Bold
            )

            Text(
                if (isBusiness)
                    v15Text(
                        "এখনও কোনো ক্যাশ লেনদেন যোগ করা হয়নি।",
                        "No cash transaction has been added yet."
                    )
                else
                    v15Text(
                        "এখনও কোনো আয়/খরচ যোগ করা হয়নি।",
                        "No income or expense has been added yet."
                    )
            )
        }

        return
    }

    LazyColumn(
        verticalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                if (isBusiness)
                    v15Text(
                        "ব্যবসার লেনদেন",
                        "Business transactions"
                    )
                else
                    v15Text(
                        "আয়-খরচের ইতিহাস",
                        "Income & expense history"
                    ),
                style =
                    MaterialTheme.typography.titleMedium,
                fontWeight =
                    FontWeight.Bold
            )
        }

        items(
            transactions,
            key = { it.id }
        ) { item ->
            TransactionRow(
                item = item,
                isBusiness = isBusiness,
                canWrite = canWrite,
                viewModel = viewModel,
                onDelete = {
                    viewModel.deleteTransaction(item)
                }
            )
        }
    }
}

@Composable
private fun TransactionRow(
    item: TransactionEntity,
    isBusiness: Boolean,
    canWrite: Boolean,
    viewModel: FamilyKhataViewModel,
    onDelete: () -> Unit
) {
    val accent =
        if (item.type == "INCOME")
            IncomeAccent
        else
            ExpenseAccent

    var showEdit by remember(
        item.id,
        item.type,
        item.amount,
        item.category,
        item.note
    ) {
        mutableStateOf(false)
    }

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    accent.copy(alpha = 0.09f)
            ),
        border =
            BorderStroke(
                1.dp,
                accent.copy(alpha = 0.20f)
            )
    ) {
        Column(
            modifier =
                Modifier.padding(14.dp),
            verticalArrangement =
                Arrangement.spacedBy(5.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {
                Text(
                    if (isBusiness) {
                        if (item.type == "INCOME")
                            v15Text(
                                "ক্যাশ ইন",
                                "Cash in"
                            )
                        else
                            v15Text(
                                "ক্যাশ আউট",
                                "Cash out"
                            )
                    } else {
                        if (item.type == "INCOME")
                            v15Text(
                                "আয়",
                                "Income"
                            )
                        else
                            v15Text(
                                "খরচ",
                                "Expense"
                            )
                    },
                    fontWeight =
                        FontWeight.Bold,
                    color = accent
                )

                Text(
                    "${V14DisplayState.currencySymbol} ${money(item.amount)}",
                    fontWeight =
                        FontWeight.ExtraBold,
                    color = accent
                )
            }

            Text(
                item.category,
                fontWeight =
                    FontWeight.SemiBold
            )

            if (item.note.isNotBlank()) {
                Text(
                    item.note,
                    style =
                        MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }

            Text(
                formatDate(item.createdAt),
                style =
                    MaterialTheme.typography.bodySmall,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )

            if (canWrite) {
                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            showEdit = true
                        },
                        modifier =
                            Modifier.weight(1f)
                    ) {
                        Text(
                            v15Text(
                                "এডিট",
                                "Edit"
                            )
                        )
                    }

                    OutlinedButton(
                        onClick = onDelete,
                        modifier =
                            Modifier.weight(1f)
                    ) {
                        Text(
                            v15Text(
                                "মুছুন",
                                "Delete"
                            )
                        )
                    }
                }
            }
        }
    }

    if (showEdit) {
        var type by remember(
            item.id,
            item.type
        ) {
            mutableStateOf(item.type)
        }

        var amount by remember(
            item.id,
            item.amount
        ) {
            mutableStateOf(
                money(item.amount)
            )
        }

        var category by remember(
            item.id,
            item.category
        ) {
            mutableStateOf(item.category)
        }

        var note by remember(
            item.id,
            item.note
        ) {
            mutableStateOf(item.note)
        }

        var error by remember {
            mutableStateOf<String?>(null)
        }

        AlertDialog(
            onDismissRequest = {
                showEdit = false
            },
            title = {
                Text(
                    if (isBusiness)
                        v15Text(
                            "ক্যাশ লেনদেন এডিট",
                            "Edit cash transaction"
                        )
                    else
                        v15Text(
                            "আয়/খরচ এডিট",
                            "Edit income/expense"
                        )
                )
            },
            text = {
                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        v15Text(
                            "লেনদেনের ধরন",
                            "Transaction type"
                        ),
                        fontWeight =
                            FontWeight.Bold
                    )

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                type = "INCOME"
                                error = null
                            },
                            modifier =
                                Modifier.weight(1f)
                        ) {
                            Text(
                                (
                                    if (type == "INCOME")
                                        "✓ "
                                    else
                                        ""
                                ) +
                                    if (isBusiness)
                                        v15Text(
                                            "ক্যাশ ইন",
                                            "Cash in"
                                        )
                                    else
                                        v15Text(
                                            "আয়",
                                            "Income"
                                        )
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                type = "EXPENSE"
                                error = null
                            },
                            modifier =
                                Modifier.weight(1f)
                        ) {
                            Text(
                                (
                                    if (type == "EXPENSE")
                                        "✓ "
                                    else
                                        ""
                                ) +
                                    if (isBusiness)
                                        v15Text(
                                            "ক্যাশ আউট",
                                            "Cash out"
                                        )
                                    else
                                        v15Text(
                                            "খরচ",
                                            "Expense"
                                        )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = amount,
                        onValueChange = {
                            amount = it
                            error = null
                        },
                        label = {
                            Text(
                                v15Text(
                                    "টাকার পরিমাণ",
                                    "Amount"
                                )
                            )
                        },
                        singleLine = true,
                        modifier =
                            Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = category,
                        onValueChange = {
                            category = it
                        },
                        label = {
                            Text(
                                v15Text(
                                    "ক্যাটাগরি",
                                    "Category"
                                )
                            )
                        },
                        singleLine = true,
                        modifier =
                            Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = note,
                        onValueChange = {
                            note = it
                        },
                        label = {
                            Text(
                                v15Text(
                                    "নোট",
                                    "Note"
                                )
                            )
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    )

                    error?.let {
                        Text(
                            it,
                            color =
                                MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val parsed =
                            parseAmount(amount)

                        if (parsed == null) {
                            error =
                                v15Text(
                                    "সঠিক টাকার পরিমাণ লিখুন",
                                    "Enter a valid amount"
                                )
                        } else {
                            viewModel.updateTransaction(
                                item = item,
                                type = type,
                                amount = parsed,
                                category = category,
                                note = note
                            )

                            showEdit = false
                        }
                    }
                ) {
                    Text(
                        v15Text(
                            "সেভ করুন",
                            "Save"
                        )
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showEdit = false
                    }
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

    TrackV15DeepScreen(
        owner = "baki-ledger",
        active = selected != null
    )

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
        V15DeepScreenContainer(
            title = selected.name,
            onBack = {
                selectedId = null
            }
        ) {
            BakiEntryScreen(
                person = selected,
                viewModel = viewModel,
                workspace = workspace,
                canWrite = canWrite,
                onBack = {
                    selectedId = null
                }
            )
        }
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
                    person.balance > 0 -> v15Text("বর্তমানে পাবো: ${V14DisplayState.currencySymbol} ${money(person.balance)}", "Receivable now: ${V14DisplayState.currencySymbol} ${money(person.balance)}")
                    person.balance < 0 -> v15Text("বর্তমানে দেবো: ${V14DisplayState.currencySymbol} ${money(-person.balance)}", "Payable now: ${V14DisplayState.currencySymbol} ${money(-person.balance)}")
                    else -> v15Text("বর্তমান হিসাব সমান", "Account is settled")
                },
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = balanceTone
            )
        }

        PersonManagementActions(
            person = person,
            personLabel = if (workspace == "SHOP") v15Text("কাস্টমার/সাপ্লায়ার", "Customer/Supplier") else v15Text("ব্যক্তি", "Person"),
            canWrite = canWrite,
            viewModel = viewModel,
            onDeleted = onBack
        )

        if (person.phone.isNotBlank()) {
            OutlinedButton(
                onClick = { sendLedgerSms(context, person) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(v15Text("SMS-এ হিসাব পাঠান", "Send account by SMS"))
            }
        }

        if (!canWrite) {
            TrialLockedMessage()
        }
        Text(v15Text("নতুন এন্ট্রি", "New entry"), fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ActionButton(v15Text("দিলাম", "Gave"), "GAVE", action) { action = "GAVE" }
            ActionButton(v15Text("ফেরত পেলাম", "Received back"), "RECEIVED_BACK", action) { action = "RECEIVED_BACK" }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ActionButton(v15Text("নিলাম", "Took"), "TOOK", action) { action = "TOOK" }
            ActionButton(v15Text("ফেরত দিলাম", "Paid back"), "PAID_BACK", action) { action = "PAID_BACK" }
        }

        OutlinedTextField(
            amount,
            { amount = it },
            label = { Text(v15Text("টাকার পরিমাণ", "Amount")) },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            note,
            { note = it },
            label = { Text(v15Text("নোট", "Note")) },
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
                    error = v15Text("সঠিক টাকার পরিমাণ লিখুন", "Enter a valid amount")
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
        ) { Text(if (workspace == "SHOP") v15Text("খাতার এন্ট্রি সেভ করুন", "Save ledger entry") else v15Text("বাকি এন্ট্রি সেভ করুন", "Save due entry")) }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Spacer(Modifier.height(8.dp))
        Text(v15Text("লেনদেনের ইতিহাস (${entries.size})", "Transaction history (${entries.size})"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        if (entries.isEmpty()) {
            Text(
                if (workspace == "SHOP") v15Text("এই খাতায় এখনো কোনো লেনদেন নেই।", "There are no transactions in this ledger yet.")
                else v15Text("এই ব্যক্তির কোনো বাকি লেনদেন এখনো নেই।", "This person has no due transactions yet.")
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
            title = { Text(v15Text("এন্ট্রি মুছবেন?", "Delete entry?")) },
            text = {
                Text(
                    "${actionLabel(entry.action)} — ${V14DisplayState.currencySymbol} ${money(entry.amount)}\n" +
                        v15Text("মুছে দিলে ব্যক্তির বর্তমান হিসাবও স্বয়ংক্রিয়ভাবে বদলে যাবে।", "Deleting this entry will automatically update the current balance.")
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteBakiEntry(entry)
                        pendingDelete = null
                    }
                ) { Text(v15Text("মুছুন", "Delete")) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(v15Text("বাতিল", "Cancel")) }
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
                    v15Text("পরিশোধের তারিখ: ${v13Date(it)}", "Due date: ${v13Date(it)}"),
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
                } ?: error(v15Text("ফাইল লেখা যায়নি", "Unable to write file"))
            }.onSuccess {
                toast(context, v15Text("ব্যাকআপ সেভ হয়েছে", "Backup saved"))
            }.onFailure {
                toast(context, it.message ?: v15Text("ব্যাকআপ সেভ করা যায়নি", "Unable to save backup"))
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
                } ?: error(v15Text("ফাইল পড়া যায়নি", "Unable to read file"))
            }.onSuccess { json ->
                pendingRestoreJson = json
            }.onFailure {
                toast(context, it.message ?: v15Text("ব্যাকআপ ফাইল পড়া যায়নি", "Unable to read backup file"))
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
            v15Text("আরও","More"),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            v15Text("ডেটা নিরাপত্তা, শেয়ার, সাপোর্ট ও অ্যাপ সম্পর্কিত প্রয়োজনীয় অপশন।","Data safety, sharing, support and app options."),
            style = MaterialTheme.typography.bodyMedium
        )

        CommercialToolsSection(viewModel)
        PurchaseAndTutorialSection()

        MoreSectionTitle(v15Text("ডেটা নিরাপত্তা","Data safety"))
        MoreActionCard(
            symbol = "⇩",
            title = v15Text("ব্যাকআপ তৈরি করুন","Create backup"),
            subtitle = v15Text("সব ওয়ার্কস্পেসের আয়-খরচ ও বাকি হিসাব একটি JSON ফাইলে রাখুন","Save all workspace accounts in one JSON backup file")
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
            title = v15Text("ব্যাকআপ রিস্টোর করুন","Restore backup"),
            subtitle = v15Text("আগের হিসাবী খাতা ব্যাকআপ থেকে সব ডেটা ফিরিয়ে আনুন","Restore all data from a previous Hisabi Khata backup")
        ) {
            openBackupFile.launch(arrayOf("application/json", "text/plain"))
        }

        MoreSectionTitle(v15Text("শেয়ার ও মতামত","Share & feedback"))
        MoreActionCard(
            symbol = "↗",
            title = v15Text("অ্যাপ শেয়ার করুন","Share app"),
            subtitle = v15Text("পরিবার, বন্ধু বা ব্যবসায়িক পরিচিতদের হিসাবী খাতা জানান","Share Hisabi Khata with family, friends or business contacts")
        ) {
            shareApp(context)
        }
        MoreActionCard(
            symbol = "★",
            title = v15Text("রিভিউ দিন","Write a review"),
            subtitle = v15Text("Play Store-এ প্রকাশের পর এখান থেকে রেটিং ও রিভিউ দেওয়া যাবে","Rate and review the app after it is published on Play Store")
        ) {
            openPlayStore(context)
        }
        MoreActionCard(
            symbol = "✦",
            title = v15Text("ফিচার রিকোয়েস্ট","Feature request"),
            subtitle = v15Text("কোন নতুন সুবিধা চান তা আমাদের জানান","Tell us which new feature you want")
        ) {
            openUrl(context, FEATURE_REQUEST_URL)
        }

        MoreSectionTitle(v15Text("সাপোর্ট ও তথ্য","Support & information"))
        MoreActionCard(
            symbol = "?",
            title = v15Text("আমাদের সাথে যোগাযোগ","Contact us"),
            subtitle = v15Text("সাপোর্ট প্রশ্ন বা সমস্যার জন্য যোগাযোগ করুন","Contact us for support or issues")
        ) {
            openUrl(context, SUPPORT_URL)
        }
        MoreActionCard(
            symbol = "⌂",
            title = v15Text("ওয়েবসাইট","Website"),
            subtitle = v15Text("হিসাবী খাতার অফিসিয়াল ওয়েবসাইট","Official Hisabi Khata website")
        ) {
            openUrl(context, WEBSITE_URL)
        }
        MoreActionCard(
            symbol = "ⓘ",
            title = "Privacy Policy",
            subtitle = v15Text("আপনার ডেটা কীভাবে সংরক্ষণ ও ব্যবহার করা হয়","How your data is stored and used")
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
                Text(v15Text("হিসাবী খাতা v1.5","Hisabi Khata v1.5"), fontWeight = FontWeight.Bold)
                Text(
                    v15Text("আপনার টাকা-পয়সার সহজ হিসাব • ডেটা আপনার ডিভাইসে থাকে","Simple money tracking • Your data stays on your device"),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    pendingRestoreJson?.let { json ->
        AlertDialog(
            onDismissRequest = { pendingRestoreJson = null },
            title = { Text(v15Text("ব্যাকআপ রিস্টোর করবেন?","Restore backup?")) },
            text = {
                Text(
                    v15Text("বর্তমান অ্যাপের সব হিসাব মুছে ব্যাকআপ ফাইলের ডেটা বসবে। ","All current app data will be replaced by the backup data. ") +
                        v15Text("নিশ্চিত হওয়ার আগে চাইলে বর্তমান ডেটার একটি ব্যাকআপ তৈরি করুন।","Create a backup of your current data first if needed.")
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingRestoreJson = null
                        viewModel.restoreBackup(
                            json = json,
                            onDone = { count ->
                                val settingsPrefs =
                                    context.getSharedPreferences(
                                        "hisabi_khata_v14_settings",
                                        Context.MODE_PRIVATE
                                    )

                                V14DisplayState.currencySymbol =
                                    settingsPrefs.getString(
                                        "currency_symbol",
                                        "৳"
                                    ) ?: "৳"

                                V14DisplayState.summaryVisible =
                                    settingsPrefs.getBoolean(
                                        "summary_visible",
                                        true
                                    )

                                val restoredLanguage =
                                    context.getSharedPreferences(
                                        "hisabi_khata_v15_language",
                                        Context.MODE_PRIVATE
                                    ).getString(
                                        "language",
                                        "bn"
                                    )

                                if (
                                    restoredLanguage == "bn" ||
                                    restoredLanguage == "en"
                                ) {
                                    V15LanguageState.setLanguage(
                                        context,
                                        restoredLanguage
                                    )
                                }

                                toast(
                                    context,
                                    v15Text("রিস্টোর সম্পন্ন: $count টি রেকর্ড","Restore complete: $count records")
                                )
                            },
                            onError = { toast(context, it) }
                        )
                    }
                ) { Text(v15Text("রিস্টোর করুন","Restore")) }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreJson = null }) { Text(v15Text("বাতিল","Cancel")) }
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
        person.balance > 0 -> v15Text("আপনার কাছে ${V14DisplayState.currencySymbol} ${money(person.balance)} পাওনা আছে।","You are owed ${V14DisplayState.currencySymbol} ${money(person.balance)}.")
        person.balance < 0 -> v15Text("আপনাকে ${V14DisplayState.currencySymbol} ${money(-person.balance)} পরিশোধযোগ্য আছে।","You owe ${V14DisplayState.currencySymbol} ${money(-person.balance)}.")
        else -> v15Text("আপনার হিসাব বর্তমানে সমান আছে।","Your account is currently settled.")
    }
    val message = v15Text("হিসাবী খাতা: ${person.name}, $balanceText","Hisabi Khata: ${person.name}, $balanceText")
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("smsto:${Uri.encode(person.phone)}")
        putExtra("sms_body", message)
    }
    runCatching { context.startActivity(intent) }
        .onFailure { toast(context, v15Text("SMS অ্যাপ খোলা যায়নি","Unable to open SMS app")) }
}

private fun shareApp(context: Context) {
    val text = v15Text("হিসাবী খাতা – আয় ব্যয় ও বাকি\n$PLAY_STORE_URL","Hisabi Khata – income, expense and dues\n$PLAY_STORE_URL")
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, v15Text("হিসাবী খাতা","Hisabi Khata"))
        putExtra(Intent.EXTRA_TEXT, text)
    }
    runCatching {
        context.startActivity(Intent.createChooser(intent, v15Text("অ্যাপ শেয়ার করুন","Share app")))
    }.onFailure {
        toast(context, v15Text("শেয়ার অপশন খোলা যায়নি","Unable to open share options"))
    }
}

private fun openPlayStore(context: Context) {
    val marketIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("market://details?id=$APP_PACKAGE")
    )
    runCatching { context.startActivity(marketIntent) }
        .recoverCatching { openUrl(context, PLAY_STORE_URL) }
        .onFailure { toast(context, v15Text("Play Store খোলা যায়নি","Unable to open Play Store")) }
}

private fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }.onFailure {
        toast(context, v15Text("লিংক খোলা যায়নি","Unable to open link"))
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
    "PERSONAL" -> v15Text("নিজের", "Personal")
    "SHOP" -> v15Text("দোকান/প্রতিষ্ঠান", "Business")
    else -> v15Text("পরিবার", "Family")
}

private fun workspaceSummary(workspace: String): String = when (workspace) {
    "PERSONAL" -> v15Text("আপনার ব্যক্তিগত আয়-খরচ ও দেনা-পাওনা", "Your personal income, expenses and dues")
    "SHOP" -> v15Text("দোকান/প্রতিষ্ঠানের আয়-খরচ ও দেনা-পাওনার হিসাব", "Business income, expenses and dues")
    else -> v15Text("পরিবারের সার্বিক আয়-খরচ ও দেনা-পাওনা", "Family income, expenses and dues")
}

private fun tabLabel(
    tab: Tab,
    workspace: String,
    businessMode: BusinessMode
): String = when (tab) {
    Tab.DASHBOARD ->
        v15Text("হোম", "Home")

    Tab.ADD ->
        if (workspace == "SHOP")
            v15Text("ক্যাশ", "Cash")
        else
            v15Text("নতুন", "Add")

    Tab.BAKI ->
        if (workspace == "SHOP")
            v15Text("খাতা", "Ledger")
        else
            v15Text("বাকি", "Due")

    Tab.PRODUCTS ->
        when (businessMode) {
            BusinessMode.COACHING ->
                v15Text("শিক্ষার্থী", "Students")

            BusinessMode.DIGITAL_AGENCY ->
                v15Text("ক্লায়েন্ট", "Clients")

            BusinessMode.FOOD_SERVICE ->
                v15Text("অর্ডার", "Orders")

            BusinessMode.AGRO ->
                v15Text("খামার", "Agro")

            BusinessMode.PRODUCTION ->
                v15Text("উৎপাদন", "Production")

            BusinessMode.DEALERSHIP ->
                v15Text("ডিলার", "Dealers")

            BusinessMode.SERVICE_JOB ->
                v15Text("সার্ভিস", "Services")

            BusinessMode.MEMBERSHIP_SERVICE ->
                v15Text("মেম্বার", "Members")

            BusinessMode.BOOKING_RENTAL ->
                v15Text("বুকিং", "Bookings")

            else ->
                v15Text("পণ্য", "Products")
        }

    Tab.HISTORY ->
        if (workspace == "SHOP")
            v15Text("লেনদেন", "History")
        else
            v15Text("হিসাব", "History")

    Tab.MORE ->
        v15Text("আরও", "More")
}

private fun tabSymbol(tab: Tab): String = when (tab) {
    Tab.DASHBOARD -> "⌂"
    Tab.ADD -> "✚"
    Tab.BAKI -> "▤"
    Tab.PRODUCTS -> "▦"
    Tab.HISTORY -> "◷"
    Tab.MORE -> "⋯"
}

private fun actionLabel(action: String): String = when (action) {
    "GAVE" -> v15Text("দিলাম", "Gave")
    "RECEIVED_BACK" -> v15Text("ফেরত পেলাম", "Received back")
    "TOOK" -> v15Text("নিলাম", "Took")
    "PAID_BACK" -> v15Text("ফেরত দিলাম", "Paid back")
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
