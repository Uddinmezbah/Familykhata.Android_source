package com.familykhata.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.familykhata.app.report.BusinessReportSummary
import com.familykhata.app.report.loadBusinessReport
import java.util.Calendar

private enum class BusinessReportPeriod {
    TODAY,
    LAST_7_DAYS,
    THIS_MONTH,
    ALL
}

@Composable
internal fun BusinessReportsDialog(
    businessId: String,
    businessType: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var period by remember {
        mutableStateOf(
            BusinessReportPeriod.THIS_MONTH
        )
    }

    var report by remember {
        mutableStateOf<BusinessReportSummary?>(null)
    }

    var loading by remember {
        mutableStateOf(true)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    var reloadToken by remember {
        mutableStateOf(0)
    }

    LaunchedEffect(
        businessId,
        businessType,
        period,
        reloadToken
    ) {
        loading = true
        error = null

        val now = System.currentTimeMillis()
        val start =
            reportStart(
                period = period,
                now = now
            )

        runCatching {
            loadBusinessReport(
                context = context,
                businessId = businessId,
                businessType = businessType,
                startInclusive = start,
                endExclusive =
                    if (
                        now == Long.MAX_VALUE
                    ) {
                        now
                    } else {
                        now + 1L
                    }
            )
        }.onSuccess {
            report = it
        }.onFailure {
            report = null
            error =
                it.message
                    ?: v15Text(
                        "রিপোর্ট তৈরি করা যায়নি",
                        "Unable to create report"
                    )
        }

        loading = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false
            )
    ) {
        Surface(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(8.dp),
            shape =
                RoundedCornerShape(
                    24.dp
                ),
            color =
                MaterialTheme
                    .colorScheme
                    .background
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(16.dp)
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement
                            .SpaceBetween,
                    verticalAlignment =
                        Alignment
                            .CenterVertically
                ) {
                    Column {
                        Text(
                            v15Text(
                                "ব্যবসার রিপোর্ট",
                                "Business Reports"
                            ),
                            style =
                                MaterialTheme
                                    .typography
                                    .titleLarge,
                            fontWeight =
                                FontWeight
                                    .ExtraBold
                        )

                        Text(
                            v15Text(
                                "বিক্রি • লাভ • বকেয়া • স্টক",
                                "Sales • Profit • Due • Stock"
                            ),
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = onDismiss
                    ) {
                        Text(
                            v15Text(
                                "বন্ধ",
                                "Close"
                            )
                        )
                    }
                }

                Spacer(
                    Modifier.height(
                        12.dp
                    )
                )

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(
                                rememberScrollState()
                            ),
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            8.dp
                        )
                ) {
                    BusinessReportPeriod.entries
                        .forEach {
                            item ->

                            FilterChip(
                                selected =
                                    period ==
                                        item,
                                onClick = {
                                    period =
                                        item
                                },
                                label = {
                                    Text(
                                        periodLabel(
                                            item
                                        )
                                    )
                                }
                            )
                        }
                }

                Spacer(
                    Modifier.height(
                        10.dp
                    )
                )

                when {
                    loading -> {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        32.dp
                                    ),
                            horizontalAlignment =
                                Alignment
                                    .CenterHorizontally
                        ) {
                            CircularProgressIndicator()

                            Spacer(
                                Modifier.height(
                                    12.dp
                                )
                            )

                            Text(
                                v15Text(
                                    "রিপোর্ট তৈরি হচ্ছে…",
                                    "Preparing report…"
                                )
                            )
                        }
                    }

                    error != null -> {
                        Card(
                            modifier =
                                Modifier
                                    .fillMaxWidth(),
                            colors =
                                CardDefaults
                                    .cardColors(
                                        containerColor =
                                            MaterialTheme
                                                .colorScheme
                                                .errorContainer
                                    )
                        ) {
                            Column(
                                modifier =
                                    Modifier.padding(
                                        16.dp
                                    )
                            ) {
                                Text(
                                    error.orEmpty()
                                )

                                Spacer(
                                    Modifier.height(
                                        8.dp
                                    )
                                )

                                Button(
                                    onClick = {
                                        reloadToken++
                                    }
                                ) {
                                    Text(
                                        v15Text(
                                            "আবার চেষ্টা",
                                            "Try again"
                                        )
                                    )
                                }
                            }
                        }
                    }

                    report != null -> {
                        BusinessReportContent(
                            report =
                                report!!
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BusinessReportContent(
    report: BusinessReportSummary
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                ),
        verticalArrangement =
            Arrangement.spacedBy(
                10.dp
            )
    ) {
        ReportMetricCard(
            v15Text(
                "নিট বিক্রি",
                "Net Sales"
            ),
            report.sales
        )

        ReportMetricCard(
            v15Text(
                "বিক্রি হওয়া পণ্যের খরচ",
                "Cost of Goods Sold"
            ),
            report.costOfGoodsSold
        )

        ReportMetricCard(
            v15Text(
                "গ্রস লাভ",
                "Gross Profit"
            ),
            report.grossProfit
        )

        ReportMetricCard(
            v15Text(
                "অন্যান্য আয়",
                "Other Income"
            ),
            report.otherIncome
        )

        ReportMetricCard(
            v15Text(
                "খরচ",
                "Expense"
            ),
            report.expense
        )

        ReportMetricCard(
            v15Text(
                "নিট লাভ",
                "Net Profit"
            ),
            report.netProfit,
            emphasized = true
        )

        ReportSectionTitle(
            v15Text(
                "বকেয়া ও ক্রয়",
                "Due & Purchase"
            )
        )

        ReportMetricCard(
            v15Text(
                "বকেয়া আদায়",
                "Due Collection"
            ),
            report.dueCollection
        )

        ReportMetricCard(
            v15Text(
                "বর্তমান কাস্টমার পাওনা",
                "Current Customer Due"
            ),
            report.outstandingCustomerDue
        )

        ReportMetricCard(
            v15Text(
                "নিট ক্রয়",
                "Net Purchase"
            ),
            report.purchase
        )

        ReportMetricCard(
            v15Text(
                "বর্তমান সাপ্লায়ার বকেয়া",
                "Current Supplier Due"
            ),
            report.supplierDue
        )

        ReportMetricCard(
            v15Text(
                "বর্তমান স্টক মূল্য",
                "Current Stock Value"
            ),
            report.stockValue
        )

        if (
            report.paymentMethods
                .isNotEmpty()
        ) {
            ReportSectionTitle(
                v15Text(
                    "পেমেন্ট মাধ্যম",
                    "Payment Methods"
                )
            )

            report.paymentMethods
                .forEach {
                    item ->

                    ReportBreakdownRow(
                        label =
                            item.method,
                        value =
                            item.amount
                    )
                }
        }

        if (
            report.accountMovements
                .isNotEmpty()
        ) {
            ReportSectionTitle(
                v15Text(
                    "অ্যাকাউন্টে টাকা চলাচল",
                    "Account Movement"
                )
            )

            report.accountMovements
                .forEach {
                    item ->

                    Card(
                        modifier =
                            Modifier
                                .fillMaxWidth(),
                        border =
                            BorderStroke(
                                1.dp,
                                MaterialTheme
                                    .colorScheme
                                    .outline
                                    .copy(
                                        alpha =
                                            0.16f
                                    )
                            )
                    ) {
                        Column(
                            modifier =
                                Modifier.padding(
                                    12.dp
                                ),
                            verticalArrangement =
                                Arrangement
                                    .spacedBy(
                                        4.dp
                                    )
                        ) {
                            Text(
                                item.accountType,
                                fontWeight =
                                    FontWeight
                                        .Bold
                            )

                            Text(
                                v15Text(
                                    "ঢুকেছে: ${currency(item.moneyIn)}",
                                    "In: ${currency(item.moneyIn)}"
                                )
                            )

                            Text(
                                v15Text(
                                    "বের হয়েছে: ${currency(item.moneyOut)}",
                                    "Out: ${currency(item.moneyOut)}"
                                )
                            )

                            Text(
                                v15Text(
                                    "নিট চলাচল: ${currency(item.netMovement)}",
                                    "Net movement: ${currency(item.netMovement)}"
                                ),
                                fontWeight =
                                    FontWeight
                                        .SemiBold
                            )
                        }
                    }
                }

            Text(
                v15Text(
                    "নিজের অ্যাকাউন্টের মধ্যে Transfer এখানে movement হিসেবে দেখা যাবে, কিন্তু লাভ/খরচ হিসেবে গণনা হয় না।",
                    "Transfers between your own accounts appear as movement, but are not counted as profit or expense."
                ),
                style =
                    MaterialTheme
                        .typography
                        .bodySmall,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }

        if (
            report.topProducts
                .isNotEmpty()
        ) {
            ReportSectionTitle(
                v15Text(
                    "শীর্ষ পণ্য",
                    "Top Products"
                )
            )

            report.topProducts
                .forEachIndexed {
                    index,
                    item ->

                    Card(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                    ) {
                        Column(
                            modifier =
                                Modifier.padding(
                                    12.dp
                                ),
                            verticalArrangement =
                                Arrangement
                                    .spacedBy(
                                        3.dp
                                    )
                        ) {
                            Text(
                                "${index + 1}. ${item.name}",
                                fontWeight =
                                    FontWeight
                                        .Bold
                            )

                            Text(
                                v15Text(
                                    "পরিমাণ: ${item.quantity} • বিক্রি: ${currency(item.sales)}",
                                    "Qty: ${item.quantity} • Sales: ${currency(item.sales)}"
                                ),
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodySmall
                            )

                            Text(
                                v15Text(
                                    "গ্রস লাভ: ${currency(item.grossProfit)}",
                                    "Gross profit: ${currency(item.grossProfit)}"
                                ),
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodySmall
                            )
                        }
                    }
                }
        }

        Spacer(
            Modifier.height(
                12.dp
            )
        )
    }
}

@Composable
private fun ReportMetricCard(
    title: String,
    amount: Double,
    emphasized: Boolean = false
) {
    Card(
        modifier =
            Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (
                        emphasized
                    ) {
                        MaterialTheme
                            .colorScheme
                            .primaryContainer
                    } else {
                        MaterialTheme
                            .colorScheme
                            .surface
                    }
            ),
        border =
            BorderStroke(
                1.dp,
                MaterialTheme
                    .colorScheme
                    .outline
                    .copy(
                        alpha =
                            0.14f
                    )
            )
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        14.dp
                    ),
            horizontalArrangement =
                Arrangement
                    .SpaceBetween,
            verticalAlignment =
                Alignment
                    .CenterVertically
        ) {
            Text(
                title,
                style =
                    MaterialTheme
                        .typography
                        .bodyMedium
            )

            Text(
                currency(amount),
                fontWeight =
                    if (
                        emphasized
                    ) {
                        FontWeight
                            .ExtraBold
                    } else {
                        FontWeight
                            .Bold
                    }
            )
        }
    }
}

@Composable
private fun ReportBreakdownRow(
    label: String,
    value: Double
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal =
                        4.dp,
                    vertical =
                        3.dp
                ),
        horizontalArrangement =
            Arrangement
                .SpaceBetween
    ) {
        Text(label)

        Text(
            currency(value),
            fontWeight =
                FontWeight
                    .SemiBold
        )
    }
}

@Composable
private fun ReportSectionTitle(
    title: String
) {
    Text(
        title,
        style =
            MaterialTheme
                .typography
                .titleMedium,
        fontWeight =
            FontWeight
                .Bold,
        modifier =
            Modifier.padding(
                top = 8.dp
            )
    )
}

private fun currency(
    value: Double
): String =
    "${V14DisplayState.currencySymbol} ${money(value)}"

private fun periodLabel(
    period: BusinessReportPeriod
): String =
    when (period) {
        BusinessReportPeriod.TODAY ->
            v15Text(
                "আজ",
                "Today"
            )

        BusinessReportPeriod.LAST_7_DAYS ->
            v15Text(
                "শেষ ৭ দিন",
                "Last 7 Days"
            )

        BusinessReportPeriod.THIS_MONTH ->
            v15Text(
                "এই মাস",
                "This Month"
            )

        BusinessReportPeriod.ALL ->
            v15Text(
                "সব",
                "All"
            )
    }

private fun reportStart(
    period: BusinessReportPeriod,
    now: Long
): Long {
    if (
        period ==
        BusinessReportPeriod.ALL
    ) {
        return 0L
    }

    val calendar =
        Calendar.getInstance().apply {
            timeInMillis = now

            set(
                Calendar.HOUR_OF_DAY,
                0
            )

            set(
                Calendar.MINUTE,
                0
            )

            set(
                Calendar.SECOND,
                0
            )

            set(
                Calendar.MILLISECOND,
                0
            )
        }

    when (period) {
        BusinessReportPeriod.TODAY ->
            Unit

        BusinessReportPeriod.LAST_7_DAYS ->
            calendar.add(
                Calendar.DAY_OF_YEAR,
                -6
            )

        BusinessReportPeriod.THIS_MONTH ->
            calendar.set(
                Calendar.DAY_OF_MONTH,
                1
            )

        BusinessReportPeriod.ALL ->
            Unit
    }

    return calendar.timeInMillis
}
