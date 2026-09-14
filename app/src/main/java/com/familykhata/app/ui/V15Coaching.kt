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
import com.familykhata.app.coaching.CoachingBatchEntity
import com.familykhata.app.coaching.CoachingEnrollmentSummary
import com.familykhata.app.coaching.CoachingStudentEntity
import com.familykhata.app.coaching.CoachingViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun V15CoachingScreen(
    workspace: String,
    canWrite: Boolean,
    onExit: () -> Unit
) {
    val vm: CoachingViewModel = viewModel()

    val students by vm.students.collectAsState()
    val batches by vm.batches.collectAsState()
    val summaries by vm.summaries.collectAsState()

    var showStudent by remember {
        mutableStateOf(false)
    }

    var showBatch by remember {
        mutableStateOf(false)
    }

    var showEnroll by remember {
        mutableStateOf(false)
    }

    var selected by remember {
        mutableStateOf<CoachingEnrollmentSummary?>(null)
    }

    LaunchedEffect(workspace) {
        vm.setWorkspace(workspace)
    }

    BackHandler(enabled = selected != null) {
        selected = null
    }

    BackHandler(enabled = selected == null) {
        onExit()
    }

    if (selected != null) {
        CoachingStudentLedger(
            summary = selected!!,
            batch = batches.firstOrNull {
                it.id == selected!!.batchId
            },
            viewModel = vm,
            canWrite = canWrite,
            onBack = {
                selected = null
            }
        )

        return
    }

    val totalDue =
        summaries.sumOf {
            it.dueAmount.coerceAtLeast(0.0)
        }

    val totalPaid =
        summaries.sumOf {
            it.totalPaid
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {
        Text(
            v15Text(
                "কোচিং / শিক্ষা প্রতিষ্ঠান",
                "Coaching / Education"
            ),
            style =
                MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold
        )

        Text(
            v15Text(
                "শিক্ষার্থী, ব্যাচ, ফি ও বকেয়া হিসাব",
                "Students, batches, fees and dues"
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            CoachingMetric(
                title = v15Text(
                    "শিক্ষার্থী",
                    "Students"
                ),
                value =
                    students.size.toString(),
                modifier = Modifier.weight(1f)
            )

            CoachingMetric(
                title = v15Text(
                    "মোট আদায়",
                    "Paid"
                ),
                value = coachingMoney(totalPaid),
                modifier = Modifier.weight(1f)
            )

            CoachingMetric(
                title = v15Text(
                    "মোট বকেয়া",
                    "Due"
                ),
                value = coachingMoney(totalDue),
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedButton(
            onClick = {
                vm.refresh()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                v15Text(
                    "↻ রিফ্রেশ",
                    "↻ Refresh"
                )
            )
        }

        if (canWrite) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        showStudent = true
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        v15Text(
                            "+ শিক্ষার্থী",
                            "+ Student"
                        )
                    )
                }

                OutlinedButton(
                    onClick = {
                        showBatch = true
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        v15Text(
                            "+ ব্যাচ",
                            "+ Batch"
                        )
                    )
                }
            }

            OutlinedButton(
                onClick = {
                    showEnroll = true
                },
                modifier = Modifier.fillMaxWidth(),
                enabled =
                    students.isNotEmpty() &&
                    batches.isNotEmpty()
            ) {
                Text(
                    v15Text(
                        "শিক্ষার্থীকে ব্যাচে ভর্তি করুন",
                        "Enroll student in batch"
                    )
                )
            }
        }

        V15CoachingEntityManagement(
            students = students,
            batches = batches,
            canWrite = canWrite,
            viewModel = vm
        )

        if (summaries.isEmpty()) {
            Text(
                v15Text(
                    "এখনো কোনো ভর্তি শিক্ষার্থী নেই।",
                    "No enrolled students yet."
                )
            )
        } else {
            summaries.forEach { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selected = item
                        }
                ) {
                    Column(
                        modifier =
                            Modifier.padding(14.dp),
                        verticalArrangement =
                            Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            item.studentName,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            v15Text(
                                "ব্যাচ: ${item.batchName}",
                                "Batch: ${item.batchName}"
                            )
                        )

                        if (item.phone.isNotBlank()) {
                            Text(item.phone)
                        }

                        Text(
                            v15Text(
                                "মোট ফি: ${coachingMoney(item.totalCharge)}",
                                "Total fee: ${coachingMoney(item.totalCharge)}"
                            )
                        )

                        Text(
                            v15Text(
                                "পরিশোধ: ${coachingMoney(item.totalPaid)}",
                                "Paid: ${coachingMoney(item.totalPaid)}"
                            )
                        )

                        Text(
                            v15Text(
                                "বকেয়া: ${coachingMoney(item.dueAmount)}",
                                "Due: ${coachingMoney(item.dueAmount)}"
                            ),
                            fontWeight =
                                FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    if (showStudent) {
        AddCoachingStudentDialog(
            onDismiss = {
                showStudent = false
            }
        ) { name, phone, guardian, address ->
            vm.addStudent(
                name = name,
                phone = phone,
                guardian = guardian,
                address = address,
                workspace = workspace
            )

            showStudent = false
        }
    }

    if (showBatch) {
        AddCoachingBatchDialog(
            onDismiss = {
                showBatch = false
            }
        ) { name, admission, monthly, note ->
            vm.addBatch(
                name = name,
                admissionFee = admission,
                monthlyFee = monthly,
                note = note,
                workspace = workspace
            )

            showBatch = false
        }
    }

    if (showEnroll) {
        CoachingEnrollDialog(
            students = students,
            batches = batches,
            onDismiss = {
                showEnroll = false
            }
        ) { studentId, batchId ->
            vm.enroll(
                studentId = studentId,
                batchId = batchId
            )

            showEnroll = false
        }
    }
}

@Composable
private fun CoachingMetric(
    title: String,
    value: String,
    modifier: Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            Text(
                title,
                style =
                    MaterialTheme.typography.labelSmall
            )

            Text(
                value,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AddCoachingStudentDialog(
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

    var guardian by remember {
        mutableStateOf("")
    }

    var address by remember {
        mutableStateOf("")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                v15Text(
                    "নতুন শিক্ষার্থী",
                    "New student"
                )
            )
        },
        text = {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(7.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                    },
                    label = {
                        Text(
                            v15Text(
                                "শিক্ষার্থীর নাম",
                                "Student name"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        phone = it
                    },
                    label = {
                        Text(
                            v15Text(
                                "ফোন",
                                "Phone"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = guardian,
                    onValueChange = {
                        guardian = it
                    },
                    label = {
                        Text(
                            v15Text(
                                "অভিভাবকের নাম",
                                "Guardian name"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = {
                        address = it
                    },
                    label = {
                        Text(
                            v15Text(
                                "ঠিকানা",
                                "Address"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            name,
                            phone,
                            guardian,
                            address
                        )
                    }
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
            TextButton(onClick = onDismiss) {
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
private fun AddCoachingBatchDialog(
    onDismiss: () -> Unit,
    onSave: (
        String,
        Double,
        Double,
        String
    ) -> Unit
) {
    var name by remember {
        mutableStateOf("")
    }

    var admission by remember {
        mutableStateOf("")
    }

    var monthly by remember {
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
                    "নতুন ব্যাচ / কোর্স",
                    "New batch / course"
                )
            )
        },
        text = {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(7.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                    },
                    label = {
                        Text(
                            v15Text(
                                "ব্যাচ / কোর্সের নাম",
                                "Batch / course name"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = admission,
                    onValueChange = {
                        admission = it
                    },
                    label = {
                        Text(
                            v15Text(
                                "ভর্তি ফি",
                                "Admission fee"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = monthly,
                    onValueChange = {
                        monthly = it
                    },
                    label = {
                        Text(
                            v15Text(
                                "মাসিক ফি",
                                "Monthly fee"
                            )
                        )
                    },
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
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            name,
                            admission.toDoubleOrNull()
                                ?: 0.0,
                            monthly.toDoubleOrNull()
                                ?: 0.0,
                            note
                        )
                    }
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
            TextButton(onClick = onDismiss) {
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
private fun CoachingEnrollDialog(
    students: List<CoachingStudentEntity>,
    batches: List<CoachingBatchEntity>,
    onDismiss: () -> Unit,
    onSave: (Long, Long) -> Unit
) {
    var studentId by remember {
        mutableStateOf<Long?>(null)
    }

    var batchId by remember {
        mutableStateOf<Long?>(null)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                v15Text(
                    "ব্যাচে ভর্তি",
                    "Enroll in batch"
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
                        "শিক্ষার্থী নির্বাচন",
                        "Select student"
                    ),
                    fontWeight = FontWeight.Bold
                )

                students.forEach { student ->
                    OutlinedButton(
                        onClick = {
                            studentId = student.id
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (studentId == student.id)
                                "✓ ${student.name}"
                            else
                                student.name
                        )
                    }
                }

                Text(
                    v15Text(
                        "ব্যাচ নির্বাচন",
                        "Select batch"
                    ),
                    fontWeight = FontWeight.Bold
                )

                batches.forEach { batch ->
                    OutlinedButton(
                        onClick = {
                            batchId = batch.id
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (batchId == batch.id)
                                "✓ ${batch.name}"
                            else
                                batch.name
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled =
                    studentId != null &&
                    batchId != null,
                onClick = {
                    onSave(
                        studentId!!,
                        batchId!!
                    )
                }
            ) {
                Text(
                    v15Text(
                        "ভর্তি করুন",
                        "Enroll"
                    )
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
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
private fun CoachingStudentLedger(
    summary: CoachingEnrollmentSummary,
    batch: CoachingBatchEntity?,
    viewModel: CoachingViewModel,
    canWrite: Boolean,
    onBack: () -> Unit
) {
    val charges by
        viewModel.observeCharges(
            summary.enrollmentId
        ).collectAsState(initial = emptyList())

    val payments by
        viewModel.observePayments(
            summary.enrollmentId
        ).collectAsState(initial = emptyList())

    var paymentAmount by remember {
        mutableStateOf("")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement =
            Arrangement.spacedBy(10.dp)
    ) {
        TextButton(onClick = onBack) {
            Text(
                v15Text(
                    "← শিক্ষার্থী তালিকা",
                    "← Student list"
                )
            )
        }

        Text(
            summary.studentName,
            style =
                MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold
        )

        Text(
            v15Text(
                "ব্যাচ: ${summary.batchName}",
                "Batch: ${summary.batchName}"
            )
        )

        Text(
            v15Text(
                "মোট ফি: ${coachingMoney(summary.totalCharge)}",
                "Total fee: ${coachingMoney(summary.totalCharge)}"
            )
        )

        Text(
            v15Text(
                "মোট পরিশোধ: ${coachingMoney(summary.totalPaid)}",
                "Total paid: ${coachingMoney(summary.totalPaid)}"
            )
        )

        Text(
            v15Text(
                "বকেয়া: ${coachingMoney(summary.dueAmount)}",
                "Due: ${coachingMoney(summary.dueAmount)}"
            ),
            fontWeight = FontWeight.Bold
        )

        if (canWrite) {
            batch?.let { currentBatch ->
                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (
                                currentBatch
                                    .admissionFee > 0
                            ) {
                                viewModel
                                    .addAdmissionCharge(
                                        summary
                                            .enrollmentId,
                                        currentBatch
                                            .admissionFee
                                    )
                            }
                        },
                        modifier =
                            Modifier.weight(1f)
                    ) {
                        Text(
                            v15Text(
                                "ভর্তি ফি যোগ",
                                "Add admission fee"
                            )
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            if (
                                currentBatch
                                    .monthlyFee > 0
                            ) {
                                viewModel
                                    .addMonthlyCharge(
                                        summary
                                            .enrollmentId,
                                        currentBatch
                                            .monthlyFee
                                    )
                            }
                        },
                        modifier =
                            Modifier.weight(1f)
                    ) {
                        Text(
                            v15Text(
                                "মাসিক ফি যোগ",
                                "Add monthly fee"
                            )
                        )
                    }
                }
            }

            OutlinedTextField(
                value = paymentAmount,
                onValueChange = {
                    paymentAmount = it
                },
                label = {
                    Text(
                        v15Text(
                            "পরিশোধের টাকা",
                            "Payment amount"
                        )
                    )
                },
                modifier =
                    Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    val amount =
                        paymentAmount
                            .toDoubleOrNull()
                            ?: 0.0

                    if (amount > 0) {
                        viewModel.addPayment(
                            summary.enrollmentId,
                            amount
                        )

                        paymentAmount = ""
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

        Text(
            v15Text(
                "ফি / চার্জ",
                "Fees / charges"
            ),
            fontWeight = FontWeight.Bold
        )

        charges.forEach { item ->
            Card(
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier =
                        Modifier.padding(10.dp)
                ) {
                    Text(
                        when (item.feeType) {
                            "ADMISSION" ->
                                v15Text(
                                    "ভর্তি ফি",
                                    "Admission fee"
                                )

                            "MONTHLY" ->
                                v15Text(
                                    "মাসিক ফি ${item.periodKey}",
                                    "Monthly fee ${item.periodKey}"
                                )

                            else ->
                                item.feeType
                        }
                    )

                    Text(
                        coachingMoney(
                            item.amount
                        )
                    )
                }
            }
        }

        Text(
            v15Text(
                "পেমেন্ট ইতিহাস",
                "Payment history"
            ),
            fontWeight = FontWeight.Bold
        )

        payments.forEach { item ->
            Card(
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {
                    Text(
                        coachingDate(
                            item.paidAt
                        )
                    )

                    Text(
                        coachingMoney(
                            item.amount
                        ),
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun coachingMoney(
    value: Double
): String =
    String.format(
        Locale.US,
        "%.2f",
        value
    )

private fun coachingDate(
    value: Long
): String =
    SimpleDateFormat(
        "dd MMM yyyy",
        Locale.getDefault()
    ).format(Date(value))
