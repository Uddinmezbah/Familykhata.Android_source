package com.familykhata.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.familykhata.app.membership.MembershipMemberEntity
import com.familykhata.app.membership.MembershipPaymentEntity
import com.familykhata.app.membership.MembershipPlanEntity
import com.familykhata.app.membership.MembershipSummary
import com.familykhata.app.membership.MembershipViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun V15MembershipScreen(
    workspace: String,
    canWrite: Boolean,
    onExit: () -> Unit
) {
    val vm: MembershipViewModel =
        viewModel()

    val members by
        vm.members.collectAsState()

    val plans by
        vm.plans.collectAsState()

    val memberships by
        vm.memberships.collectAsState()

    var selectedId by remember {
        mutableStateOf<Long?>(null)
    }

    var showMemberDialog by remember {
        mutableStateOf(false)
    }

    var showPlanDialog by remember {
        mutableStateOf(false)
    }

    var showMembershipDialog by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(workspace) {
        vm.setWorkspace(workspace)
    }

    val selected =
        selectedId?.let { id ->
            memberships.firstOrNull {
                it.subscriptionId == id
            }
        }

    BackHandler(
        enabled = selectedId != null
    ) {
        selectedId = null
    }

    BackHandler(
        enabled = selectedId == null
    ) {
        onExit()
    }

    if (selected != null) {
        MembershipLedger(
            item = selected,
            plans = plans,
            viewModel = vm,
            canWrite = canWrite,
            onBack = {
                selectedId = null
            }
        )
        return
    }

    val now =
        System.currentTimeMillis()

    val sevenDays =
        now + 7L * 86_400_000L

    val activeCount =
        memberships.count {
            it.status == "ACTIVE" &&
            it.endDate >= now
        }

    val expiringCount =
        memberships.count {
            it.status == "ACTIVE" &&
            it.endDate in now..sevenDays
        }

    val uniqueMembers =
        memberships
            .map { it.memberId }
            .distinct()
            .size

    val totalDue =
        memberships.sumOf {
            it.dueAmount.coerceAtLeast(0.0)
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            ),
        verticalArrangement =
            Arrangement.spacedBy(10.dp)
    ) {
        Text(
            v15Text(
                "জিম ও মেম্বারশিপ",
                "Gym & Membership"
            ),
            style =
                MaterialTheme.typography
                    .headlineSmall,
            fontWeight =
                FontWeight.ExtraBold
        )

        Text(
            v15Text(
                "মেম্বার, প্ল্যান, মেয়াদ, পেমেন্ট ও বকেয়া",
                "Members, plans, expiry, payments and dues"
            )
        )

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(7.dp)
        ) {
            MembershipMetric(
                v15Text(
                    "মেম্বার",
                    "Members"
                ),
                uniqueMembers.toString(),
                Modifier.weight(1f)
            )

            MembershipMetric(
                v15Text(
                    "সক্রিয়",
                    "Active"
                ),
                activeCount.toString(),
                Modifier.weight(1f)
            )

            MembershipMetric(
                v15Text(
                    "৭ দিনে শেষ",
                    "Expiring"
                ),
                expiringCount.toString(),
                Modifier.weight(1f)
            )
        }

        MembershipMetric(
            title =
                v15Text(
                    "মোট বকেয়া",
                    "Total due"
                ),
            value =
                membershipMoney(totalDue),
            modifier =
                Modifier.fillMaxWidth()
        )

        if (canWrite) {
            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(7.dp)
            ) {
                Button(
                    onClick = {
                        showMemberDialog = true
                    },
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text(
                        v15Text(
                            "+ মেম্বার",
                            "+ Member"
                        )
                    )
                }

                OutlinedButton(
                    onClick = {
                        showPlanDialog = true
                    },
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text(
                        v15Text(
                            "+ প্ল্যান",
                            "+ Plan"
                        )
                    )
                }
            }

            Button(
                onClick = {
                    showMembershipDialog = true
                },
                enabled =
                    members.isNotEmpty() &&
                    plans.isNotEmpty(),
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Text(
                    v15Text(
                        "+ মেম্বারশিপ / রিনিউ",
                        "+ Membership / Renewal"
                    )
                )
            }
        }

        Text(
            v15Text(
                "মেম্বারশিপ তালিকা",
                "Memberships"
            ),
            fontWeight =
                FontWeight.Bold
        )

        if (memberships.isEmpty()) {
            Text(
                v15Text(
                    "এখনো কোনো মেম্বারশিপ যোগ করা হয়নি।",
                    "No memberships added yet."
                )
            )
        } else {
            memberships.forEach { item ->
                MembershipCard(
                    item = item,
                    now = now,
                    onClick = {
                        selectedId =
                            item.subscriptionId
                    }
                )
            }
        }
    }

    if (showMemberDialog) {
        AddMemberDialog(
            onDismiss = {
                showMemberDialog = false
            }
        ) {
                name,
                phone,
                address,
                note ->

            vm.addMember(
                name = name,
                phone = phone,
                address = address,
                note = note,
                workspace = workspace
            )

            showMemberDialog = false
        }
    }

    if (showPlanDialog) {
        AddPlanDialog(
            onDismiss = {
                showPlanDialog = false
            }
        ) {
                name,
                days,
                fee,
                note ->

            vm.addPlan(
                name = name,
                durationDays = days,
                fee = fee,
                note = note,
                workspace = workspace
            )

            showPlanDialog = false
        }
    }

    if (showMembershipDialog) {
        StartMembershipDialog(
            members = members,
            plans = plans,
            onDismiss = {
                showMembershipDialog = false
            }
        ) {
                memberId,
                plan,
                advance ->

            vm.startMembership(
                memberId = memberId,
                plan = plan,
                advance = advance
            )

            showMembershipDialog = false
        }
    }
}

@Composable
private fun MembershipMetric(
    title: String,
    value: String,
    modifier: Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier =
                Modifier.padding(10.dp)
        ) {
            Text(
                title,
                style =
                    MaterialTheme.typography
                        .labelSmall
            )

            Text(
                value,
                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MembershipCard(
    item: MembershipSummary,
    now: Long,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick
            )
    ) {
        Column(
            modifier =
                Modifier.padding(13.dp),
            verticalArrangement =
                Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {
                Text(
                    item.memberName,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    membershipStatus(
                        item,
                        now
                    )
                )
            }

            Text(
                v15Text(
                    "প্ল্যান: ${item.planName}",
                    "Plan: ${item.planName}"
                )
            )

            Text(
                v15Text(
                    "শুরু: ${membershipDate(item.startDate)}",
                    "Start: ${membershipDate(item.startDate)}"
                )
            )

            Text(
                v15Text(
                    "শেষ: ${membershipDate(item.endDate)}",
                    "Expiry: ${membershipDate(item.endDate)}"
                )
            )

            Text(
                v15Text(
                    "ফি: ${membershipMoney(item.totalFee)}",
                    "Fee: ${membershipMoney(item.totalFee)}"
                )
            )

            Text(
                v15Text(
                    "পরিশোধ: ${membershipMoney(item.totalPaid)}",
                    "Paid: ${membershipMoney(item.totalPaid)}"
                )
            )

            Text(
                v15Text(
                    "বকেয়া: ${membershipMoney(item.dueAmount)}",
                    "Due: ${membershipMoney(item.dueAmount)}"
                ),
                fontWeight =
                    FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun AddMemberDialog(
    onDismiss: () -> Unit,
    onSave: (
        String,
        String,
        String,
        String
    ) -> Unit
) {
    var name by remember {
        mutableStateOf("")
    }

    var phone by remember {
        mutableStateOf("")
    }

    var address by remember {
        mutableStateOf("")
    }

    var note by remember {
        mutableStateOf("")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                v15Text(
                    "নতুন মেম্বার",
                    "New member"
                )
            )
        },
        text = {
            Column(
                modifier =
                    Modifier.verticalScroll(
                        rememberScrollState()
                    ),
                verticalArrangement =
                    Arrangement.spacedBy(7.dp)
            ) {
                MembershipField(
                    name,
                    { name = it },
                    v15Text(
                        "নাম",
                        "Name"
                    )
                )

                MembershipField(
                    phone,
                    { phone = it },
                    v15Text(
                        "ফোন",
                        "Phone"
                    )
                )

                MembershipField(
                    address,
                    { address = it },
                    v15Text(
                        "ঠিকানা",
                        "Address"
                    )
                )

                MembershipField(
                    note,
                    { note = it },
                    v15Text(
                        "নোট",
                        "Note"
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled =
                    name.isNotBlank(),
                onClick = {
                    onSave(
                        name,
                        phone,
                        address,
                        note
                    )
                }
            ) {
                Text(
                    v15Text(
                        "সেভ",
                        "Save"
                    )
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
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

@Composable
private fun AddPlanDialog(
    onDismiss: () -> Unit,
    onSave: (
        String,
        Int,
        Double,
        String
    ) -> Unit
) {
    var name by remember {
        mutableStateOf("")
    }

    var days by remember {
        mutableStateOf("30")
    }

    var fee by remember {
        mutableStateOf("")
    }

    var note by remember {
        mutableStateOf("")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                v15Text(
                    "নতুন মেম্বারশিপ প্ল্যান",
                    "New membership plan"
                )
            )
        },
        text = {
            Column(
                modifier =
                    Modifier.verticalScroll(
                        rememberScrollState()
                    ),
                verticalArrangement =
                    Arrangement.spacedBy(7.dp)
            ) {
                MembershipField(
                    name,
                    { name = it },
                    v15Text(
                        "প্ল্যানের নাম",
                        "Plan name"
                    )
                )

                MembershipField(
                    days,
                    { days = it },
                    v15Text(
                        "মেয়াদ (দিন)",
                        "Duration days"
                    )
                )

                MembershipField(
                    fee,
                    { fee = it },
                    v15Text(
                        "ফি",
                        "Fee"
                    )
                )

                MembershipField(
                    note,
                    { note = it },
                    v15Text(
                        "নোট",
                        "Note"
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled =
                    name.isNotBlank() &&
                    (
                        days.toIntOrNull()
                            ?: 0
                    ) > 0 &&
                    (
                        fee.toDoubleOrNull()
                            ?: -1.0
                    ) >= 0,
                onClick = {
                    onSave(
                        name,
                        days.toIntOrNull()
                            ?: 30,
                        fee.toDoubleOrNull()
                            ?: 0.0,
                        note
                    )
                }
            ) {
                Text(
                    v15Text(
                        "প্ল্যান সেভ",
                        "Save plan"
                    )
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
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

@Composable
private fun StartMembershipDialog(
    members:
        List<MembershipMemberEntity>,
    plans:
        List<MembershipPlanEntity>,
    onDismiss: () -> Unit,
    onSave: (
        Long,
        MembershipPlanEntity,
        Double
    ) -> Unit
) {
    var memberId by remember {
        mutableStateOf<Long?>(null)
    }

    var planId by remember {
        mutableStateOf<Long?>(null)
    }

    var advance by remember {
        mutableStateOf("")
    }

    val selectedPlan =
        plans.firstOrNull {
            it.id == planId
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                v15Text(
                    "মেম্বারশিপ / রিনিউ",
                    "Membership / Renewal"
                )
            )
        },
        text = {
            Column(
                modifier =
                    Modifier.verticalScroll(
                        rememberScrollState()
                    ),
                verticalArrangement =
                    Arrangement.spacedBy(7.dp)
            ) {
                Text(
                    v15Text(
                        "মেম্বার নির্বাচন",
                        "Select member"
                    ),
                    fontWeight =
                        FontWeight.Bold
                )

                members.forEach { member ->
                    OutlinedButton(
                        onClick = {
                            memberId =
                                member.id
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (
                                memberId ==
                                member.id
                            ) {
                                "✓ ${member.name}"
                            } else {
                                member.name
                            }
                        )
                    }
                }

                Text(
                    v15Text(
                        "প্ল্যান নির্বাচন",
                        "Select plan"
                    ),
                    fontWeight =
                        FontWeight.Bold
                )

                plans.forEach { plan ->
                    OutlinedButton(
                        onClick = {
                            planId =
                                plan.id
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (
                                planId ==
                                plan.id
                            ) {
                                "✓ ${plan.name} - ${membershipMoney(plan.fee)}"
                            } else {
                                "${plan.name} - ${membershipMoney(plan.fee)}"
                            }
                        )
                    }
                }

                if (selectedPlan != null) {
                    Text(
                        v15Text(
                            "মেয়াদ: ${selectedPlan.durationDays} দিন",
                            "Duration: ${selectedPlan.durationDays} days"
                        )
                    )
                }

                MembershipField(
                    value = advance,
                    onChange = {
                        advance = it
                    },
                    label =
                        v15Text(
                            "অগ্রিম পেমেন্ট",
                            "Advance payment"
                        )
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled =
                    memberId != null &&
                    selectedPlan != null,
                onClick = {
                    onSave(
                        memberId!!,
                        selectedPlan!!,
                        advance.toDoubleOrNull()
                            ?: 0.0
                    )
                }
            ) {
                Text(
                    v15Text(
                        "শুরু করুন",
                        "Start"
                    )
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
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

@Composable
private fun MembershipLedger(
    item: MembershipSummary,
    plans: List<MembershipPlanEntity>,
    viewModel: MembershipViewModel,
    canWrite: Boolean,
    onBack: () -> Unit
) {
    val payments by
        viewModel.observePayments(
            item.subscriptionId
        ).collectAsState(
            initial = emptyList()
        )

    var amount by remember {
        mutableStateOf("")
    }

    var note by remember {
        mutableStateOf("")
    }

    val livePaid =
        payments.sumOf {
            it.amount
        }

    val liveDue =
        (
            item.totalFee -
            livePaid
        ).coerceAtLeast(0.0)

    val plan =
        plans.firstOrNull {
            it.id == item.planId
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            ),
        verticalArrangement =
            Arrangement.spacedBy(9.dp)
    ) {
        TextButton(
            onClick = onBack
        ) {
            Text(
                v15Text(
                    "← মেম্বারশিপ তালিকা",
                    "← Membership list"
                )
            )
        }

        Text(
            item.memberName,
            style =
                MaterialTheme.typography
                    .headlineSmall,
            fontWeight =
                FontWeight.ExtraBold
        )

        if (item.phone.isNotBlank()) {
            Text(item.phone)
        }

        Text(
            v15Text(
                "প্ল্যান: ${item.planName}",
                "Plan: ${item.planName}"
            )
        )

        Text(
            v15Text(
                "শুরু: ${membershipDate(item.startDate)}",
                "Start: ${membershipDate(item.startDate)}"
            )
        )

        Text(
            v15Text(
                "মেয়াদ শেষ: ${membershipDate(item.endDate)}",
                "Expiry: ${membershipDate(item.endDate)}"
            )
        )

        Text(
            v15Text(
                "মোট ফি: ${membershipMoney(item.totalFee)}",
                "Total fee: ${membershipMoney(item.totalFee)}"
            )
        )

        Text(
            v15Text(
                "পরিশোধ: ${membershipMoney(livePaid)}",
                "Paid: ${membershipMoney(livePaid)}"
            )
        )

        Text(
            v15Text(
                "বকেয়া: ${membershipMoney(liveDue)}",
                "Due: ${membershipMoney(liveDue)}"
            ),
            fontWeight =
                FontWeight.Bold
        )

        Text(
            v15Text(
                "অবস্থা: ${membershipStatus(item, System.currentTimeMillis())}",
                "Status: ${membershipStatus(item, System.currentTimeMillis())}"
            )
        )

        if (canWrite) {
            if (plan != null) {
                OutlinedButton(
                    onClick = {
                        viewModel
                            .renewMembership(
                                memberId =
                                    item.memberId,
                                plan = plan,
                                previousEndDate =
                                    item.endDate
                            )

                        onBack()
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text(
                        v15Text(
                            "একই প্ল্যানে রিনিউ করুন",
                            "Renew same plan"
                        )
                    )
                }
            }

            if (liveDue > 0) {
                MembershipField(
                    value = amount,
                    onChange = {
                        amount = it
                    },
                    label =
                        v15Text(
                            "পেমেন্টের পরিমাণ",
                            "Payment amount"
                        )
                )

                MembershipField(
                    value = note,
                    onChange = {
                        note = it
                    },
                    label =
                        v15Text(
                            "পেমেন্ট নোট",
                            "Payment note"
                        )
                )

                Button(
                    onClick = {
                        val requested =
                            amount.toDoubleOrNull()
                                ?: 0.0

                        if (requested > 0) {
                            viewModel.addPayment(
                                subscriptionId =
                                    item.subscriptionId,
                                amount =
                                    requested.coerceAtMost(
                                        liveDue
                                    ),
                                note = note
                            )

                            amount = ""
                            note = ""
                        }
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text(
                        v15Text(
                            "পেমেন্ট যোগ করুন",
                            "Add payment"
                        )
                    )
                }
            }

            OutlinedButton(
                onClick = {
                    viewModel.setStatus(
                        subscriptionId =
                            item.subscriptionId,
                        status = "CANCELLED"
                    )
                },
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Text(
                    v15Text(
                        "মেম্বারশিপ বাতিল করুন",
                        "Cancel membership"
                    )
                )
            }
        }

        Text(
            v15Text(
                "পেমেন্ট ইতিহাস",
                "Payment history"
            ),
            fontWeight =
                FontWeight.Bold
        )

        if (payments.isEmpty()) {
            Text(
                v15Text(
                    "এখনো কোনো পেমেন্ট নেই।",
                    "No payments yet."
                )
            )
        }

        payments.forEach {
            MembershipPaymentCard(it)
        }
    }
}

@Composable
private fun MembershipPaymentCard(
    item: MembershipPaymentEntity
) {
    Card(
        modifier =
            Modifier.fillMaxWidth()
    ) {
        Column(
            modifier =
                Modifier.padding(10.dp)
        ) {
            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {
                Text(
                    membershipDate(
                        item.paidAt
                    )
                )

                Text(
                    membershipMoney(
                        item.amount
                    ),
                    fontWeight =
                        FontWeight.Bold
                )
            }

            if (item.note.isNotBlank()) {
                Text(
                    item.note,
                    style =
                        MaterialTheme.typography
                            .bodySmall
                )
            }
        }
    }
}

@Composable
private fun MembershipField(
    value: String,
    onChange: (String) -> Unit,
    label: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = {
            Text(label)
        },
        modifier =
            Modifier.fillMaxWidth(),
        singleLine = true
    )
}

private fun membershipStatus(
    item: MembershipSummary,
    now: Long
): String =
    when {
        item.status == "CANCELLED" ->
            v15Text(
                "বাতিল",
                "Cancelled"
            )

        item.endDate < now ->
            v15Text(
                "মেয়াদ শেষ",
                "Expired"
            )

        item.status == "EXPIRED" ->
            v15Text(
                "মেয়াদ শেষ",
                "Expired"
            )

        else ->
            v15Text(
                "সক্রিয়",
                "Active"
            )
    }

private fun membershipMoney(
    value: Double
): String =
    String.format(
        Locale.US,
        "%.2f",
        value
    )

private fun membershipDate(
    value: Long
): String =
    SimpleDateFormat(
        "dd MMM yyyy",
        Locale.getDefault()
    ).format(
        Date(value)
    )
