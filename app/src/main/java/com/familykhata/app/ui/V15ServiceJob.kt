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
import com.familykhata.app.servicejob.ServiceChargeEntity
import com.familykhata.app.servicejob.ServiceCustomerEntity
import com.familykhata.app.servicejob.ServiceJobSummary
import com.familykhata.app.servicejob.ServiceJobViewModel
import com.familykhata.app.servicejob.ServicePaymentEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun V15ServiceJobScreen(
    workspace: String,
    shopType: String,
    canWrite: Boolean,
    onExit: () -> Unit
) {
    val vm: ServiceJobViewModel =
        viewModel()

    val customers by
        vm.customers.collectAsState()

    val jobs by
        vm.jobs.collectAsState()

    var selectedJobId by remember {
        mutableStateOf<Long?>(null)
    }

    var showCustomerDialog by remember {
        mutableStateOf(false)
    }

    var showJobDialog by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(workspace) {
        vm.setWorkspace(workspace)
    }

    val selectedJob =
        selectedJobId?.let { id ->
            jobs.firstOrNull {
                it.jobId == id
            }
        }

    BackHandler(
        enabled = selectedJobId != null
    ) {
        selectedJobId = null
    }

    BackHandler(
        enabled = selectedJobId == null
    ) {
        onExit()
    }

    if (selectedJob != null) {
        ServiceJobLedger(
            job = selectedJob,
            viewModel = vm,
            shopType = shopType,
            canWrite = canWrite,
            onBack = {
                selectedJobId = null
            }
        )
        return
    }

    val openCount =
        jobs.count {
            it.status != "COMPLETED" &&
            it.status != "CANCELLED"
        }

    val totalDue =
        jobs.sumOf {
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
            serviceBusinessTitle(shopType),
            style =
                MaterialTheme.typography
                    .headlineSmall,
            fontWeight =
                FontWeight.ExtraBold
        )

        Text(
            serviceBusinessSubtitle(shopType)
        )

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            ServiceMetric(
                title =
                    v15Text(
                        "মোট কাজ",
                        "Jobs"
                    ),
                value =
                    jobs.size.toString(),
                modifier =
                    Modifier.weight(1f)
            )

            ServiceMetric(
                title =
                    v15Text(
                        "চলমান",
                        "Open"
                    ),
                value =
                    openCount.toString(),
                modifier =
                    Modifier.weight(1f)
            )

            ServiceMetric(
                title =
                    v15Text(
                        "বকেয়া",
                        "Due"
                    ),
                value =
                    serviceMoney(totalDue),
                modifier =
                    Modifier.weight(1f)
            )
        }

        if (canWrite) {
            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        showJobDialog = true
                    },
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text(
                        v15Text(
                            "+ নতুন কাজ",
                            "+ New job"
                        )
                    )
                }

                OutlinedButton(
                    onClick = {
                        showCustomerDialog = true
                    },
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text(
                        v15Text(
                            "+ কাস্টমার",
                            "+ Customer"
                        )
                    )
                }
            }
        }

        Text(
            v15Text(
                "কাজের তালিকা",
                "Job list"
            ),
            fontWeight =
                FontWeight.Bold
        )

        if (jobs.isEmpty()) {
            Text(
                v15Text(
                    "এখনো কোনো কাজ যোগ করা হয়নি।",
                    "No jobs added yet."
                )
            )
        } else {
            jobs.forEach { job ->
                ServiceJobCard(
                    job = job,
                    onClick = {
                        selectedJobId =
                            job.jobId
                    }
                )
            }
        }
    }

    if (showCustomerDialog) {
        AddServiceCustomerDialog(
            onDismiss = {
                showCustomerDialog = false
            }
        ) {
                name,
                phone,
                address,
                note ->

            vm.addCustomer(
                name = name,
                phone = phone,
                address = address,
                note = note,
                workspace = workspace
            )

            showCustomerDialog = false
        }
    }

    if (showJobDialog) {
        AddServiceJobDialog(
            customers = customers,
            shopType = shopType,
            onDismiss = {
                showJobDialog = false
            }
        ) {
                customerId,
                title,
                serviceType,
                itemName,
                reference,
                initialCharge,
                advance,
                note ->

            vm.addJob(
                customerId = customerId,
                title = title,
                serviceType = serviceType,
                itemName = itemName,
                serialOrReference = reference,
                initialCharge = initialCharge,
                advance = advance,
                note = note,
                workspace = workspace
            )

            showJobDialog = false
        }
    }
}

@Composable
private fun ServiceMetric(
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
private fun ServiceJobCard(
    job: ServiceJobSummary,
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
                    job.title,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    serviceStatusLabel(
                        job.status
                    )
                )
            }

            Text(
                if (
                    job.customerName
                        .isNullOrBlank()
                ) {
                    v15Text(
                        "কাস্টমার: ওয়াক-ইন",
                        "Customer: Walk-in"
                    )
                } else {
                    v15Text(
                        "কাস্টমার: ${job.customerName}",
                        "Customer: ${job.customerName}"
                    )
                }
            )

            if (
                job.itemName.isNotBlank()
            ) {
                Text(job.itemName)
            }

            Text(
                v15Text(
                    "চার্জ: ${serviceMoney(job.totalCharge)}",
                    "Charge: ${serviceMoney(job.totalCharge)}"
                )
            )

            Text(
                v15Text(
                    "পরিশোধ: ${serviceMoney(job.totalPaid)}",
                    "Paid: ${serviceMoney(job.totalPaid)}"
                )
            )

            Text(
                v15Text(
                    "বকেয়া: ${serviceMoney(job.dueAmount)}",
                    "Due: ${serviceMoney(job.dueAmount)}"
                ),
                fontWeight =
                    FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun AddServiceCustomerDialog(
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
                    "নতুন কাস্টমার",
                    "New customer"
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
                ServiceField(
                    value = name,
                    onChange = {
                        name = it
                    },
                    label =
                        v15Text(
                            "নাম",
                            "Name"
                        )
                )

                ServiceField(
                    value = phone,
                    onChange = {
                        phone = it
                    },
                    label =
                        v15Text(
                            "ফোন",
                            "Phone"
                        )
                )

                ServiceField(
                    value = address,
                    onChange = {
                        address = it
                    },
                    label =
                        v15Text(
                            "ঠিকানা",
                            "Address"
                        )
                )

                ServiceField(
                    value = note,
                    onChange = {
                        note = it
                    },
                    label =
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
private fun AddServiceJobDialog(
    customers:
        List<ServiceCustomerEntity>,
    shopType: String,
    onDismiss: () -> Unit,
    onSave: (
        Long?,
        String,
        String,
        String,
        String,
        Double,
        Double,
        String
    ) -> Unit
) {
    var customerId by remember {
        mutableStateOf<Long?>(null)
    }

    var title by remember {
        mutableStateOf("")
    }

    var serviceType by remember {
        mutableStateOf("")
    }

    var itemName by remember {
        mutableStateOf("")
    }

    var reference by remember {
        mutableStateOf("")
    }

    var charge by remember {
        mutableStateOf("")
    }

    var advance by remember {
        mutableStateOf("")
    }

    var note by remember {
        mutableStateOf("")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                serviceNewJobTitle(
                    shopType
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
                        "কাস্টমার",
                        "Customer"
                    ),
                    fontWeight =
                        FontWeight.Bold
                )

                OutlinedButton(
                    onClick = {
                        customerId = null
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (customerId == null)
                            v15Text(
                                "✓ ওয়াক-ইন",
                                "✓ Walk-in"
                            )
                        else
                            v15Text(
                                "ওয়াক-ইন",
                                "Walk-in"
                            )
                    )
                }

                customers.forEach {
                    customer ->

                    OutlinedButton(
                        onClick = {
                            customerId =
                                customer.id
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (
                                customerId ==
                                customer.id
                            ) {
                                "✓ ${customer.name}"
                            } else {
                                customer.name
                            }
                        )
                    }
                }

                ServiceField(
                    value = title,
                    onChange = {
                        title = it
                    },
                    label =
                        serviceJobNameLabel(
                            shopType
                        )
                )

                ServiceField(
                    value = serviceType,
                    onChange = {
                        serviceType = it
                    },
                    label =
                        v15Text(
                            "সার্ভিসের ধরন",
                            "Service type"
                        )
                )

                ServiceField(
                    value = itemName,
                    onChange = {
                        itemName = it
                    },
                    label =
                        serviceItemLabel(
                            shopType
                        )
                )

                ServiceField(
                    value = reference,
                    onChange = {
                        reference = it
                    },
                    label =
                        serviceReferenceLabel(
                            shopType
                        )
                )

                ServiceField(
                    value = charge,
                    onChange = {
                        charge = it
                    },
                    label =
                        v15Text(
                            "প্রাথমিক চার্জ",
                            "Initial charge"
                        )
                )

                ServiceField(
                    value = advance,
                    onChange = {
                        advance = it
                    },
                    label =
                        v15Text(
                            "অগ্রিম",
                            "Advance"
                        )
                )

                ServiceField(
                    value = note,
                    onChange = {
                        note = it
                    },
                    label =
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
                    title.isNotBlank(),
                onClick = {
                    onSave(
                        customerId,
                        title,
                        serviceType,
                        itemName,
                        reference,
                        charge.toDoubleOrNull()
                            ?: 0.0,
                        advance.toDoubleOrNull()
                            ?: 0.0,
                        note
                    )
                }
            ) {
                Text(
                    v15Text(
                        "যোগ করুন",
                        "Add"
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
private fun ServiceJobLedger(
    job: ServiceJobSummary,
    viewModel: ServiceJobViewModel,
    shopType: String,
    canWrite: Boolean,
    onBack: () -> Unit
) {
    val charges by
        viewModel.observeCharges(
            job.jobId
        ).collectAsState(
            initial = emptyList()
        )

    val payments by
        viewModel.observePayments(
            job.jobId
        ).collectAsState(
            initial = emptyList()
        )

    var chargeAmount by remember {
        mutableStateOf("")
    }

    var chargeNote by remember {
        mutableStateOf("")
    }

    var chargeType by remember {
        mutableStateOf("SERVICE")
    }

    var paymentAmount by remember {
        mutableStateOf("")
    }

    var paymentNote by remember {
        mutableStateOf("")
    }

    val liveCharge =
        charges.sumOf {
            it.amount
        }

    val livePaid =
        payments.sumOf {
            it.amount
        }

    val liveDue =
        (
            liveCharge -
            livePaid
        ).coerceAtLeast(0.0)

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
                    "← কাজের তালিকা",
                    "← Job list"
                )
            )
        }

        Text(
            job.title,
            style =
                MaterialTheme.typography
                    .headlineSmall,
            fontWeight =
                FontWeight.ExtraBold
        )

        Text(
            if (
                job.customerName
                    .isNullOrBlank()
            ) {
                v15Text(
                    "কাস্টমার: ওয়াক-ইন",
                    "Customer: Walk-in"
                )
            } else {
                v15Text(
                    "কাস্টমার: ${job.customerName}",
                    "Customer: ${job.customerName}"
                )
            }
        )

        if (job.phone?.isNotBlank() == true) {
            Text(job.phone)
        }

        if (
            job.serviceType.isNotBlank()
        ) {
            Text(
                v15Text(
                    "সার্ভিস: ${job.serviceType}",
                    "Service: ${job.serviceType}"
                )
            )
        }

        if (
            job.itemName.isNotBlank()
        ) {
            Text(
                "${serviceItemLabel(shopType)}: ${job.itemName}"
            )
        }

        if (
            job.serialOrReference
                .isNotBlank()
        ) {
            Text(
                "${serviceReferenceLabel(shopType)}: ${job.serialOrReference}"
            )
        }

        Text(
            v15Text(
                "গ্রহণ: ${serviceDate(job.receivedAt)}",
                "Received: ${serviceDate(job.receivedAt)}"
            )
        )

        Text(
            v15Text(
                "মোট চার্জ: ${serviceMoney(liveCharge)}",
                "Total charge: ${serviceMoney(liveCharge)}"
            )
        )

        Text(
            v15Text(
                "পরিশোধ: ${serviceMoney(livePaid)}",
                "Paid: ${serviceMoney(livePaid)}"
            )
        )

        Text(
            v15Text(
                "বকেয়া: ${serviceMoney(liveDue)}",
                "Due: ${serviceMoney(liveDue)}"
            ),
            fontWeight =
                FontWeight.Bold
        )

        Text(
            v15Text(
                "অবস্থা: ${serviceStatusLabel(job.status)}",
                "Status: ${serviceStatusLabel(job.status)}"
            )
        )

        if (canWrite) {
            Text(
                v15Text(
                    "কাজের অবস্থা",
                    "Job status"
                ),
                fontWeight =
                    FontWeight.Bold
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.setStatus(
                            job.jobId,
                            "OPEN"
                        )
                    },
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text(
                        v15Text(
                            "নতুন",
                            "Open"
                        )
                    )
                }

                OutlinedButton(
                    onClick = {
                        viewModel.setStatus(
                            job.jobId,
                            "IN_PROGRESS"
                        )
                    },
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text(
                        v15Text(
                            "চলমান",
                            "Working"
                        )
                    )
                }
            }

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.setStatus(
                            job.jobId,
                            "READY"
                        )
                    },
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text(
                        v15Text(
                            "প্রস্তুত",
                            "Ready"
                        )
                    )
                }

                OutlinedButton(
                    onClick = {
                        viewModel.setStatus(
                            job.jobId,
                            "COMPLETED"
                        )
                    },
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text(
                        v15Text(
                            "সম্পন্ন",
                            "Completed"
                        )
                    )
                }
            }

            Text(
                v15Text(
                    "নতুন চার্জ",
                    "Add charge"
                ),
                fontWeight =
                    FontWeight.Bold
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        chargeType =
                            "SERVICE"
                    },
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text(
                        if (
                            chargeType ==
                            "SERVICE"
                        ) {
                            "✓ " +
                            v15Text(
                                "সার্ভিস",
                                "Service"
                            )
                        } else {
                            v15Text(
                                "সার্ভিস",
                                "Service"
                            )
                        }
                    )
                }

                OutlinedButton(
                    onClick = {
                        chargeType =
                            "PARTS"
                    },
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text(
                        if (
                            chargeType ==
                            "PARTS"
                        ) {
                            "✓ " +
                            v15Text(
                                "পার্টস/পণ্য",
                                "Parts"
                            )
                        } else {
                            v15Text(
                                "পার্টস/পণ্য",
                                "Parts"
                            )
                        }
                    )
                }
            }

            ServiceField(
                value = chargeAmount,
                onChange = {
                    chargeAmount = it
                },
                label =
                    v15Text(
                        "চার্জের পরিমাণ",
                        "Charge amount"
                    )
            )

            ServiceField(
                value = chargeNote,
                onChange = {
                    chargeNote = it
                },
                label =
                    v15Text(
                        "চার্জ নোট",
                        "Charge note"
                    )
            )

            Button(
                onClick = {
                    val amount =
                        chargeAmount
                            .toDoubleOrNull()
                            ?: 0.0

                    if (amount > 0) {
                        viewModel.addCharge(
                            jobId = job.jobId,
                            amount = amount,
                            chargeType =
                                chargeType,
                            note = chargeNote
                        )

                        chargeAmount = ""
                        chargeNote = ""
                    }
                },
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Text(
                    v15Text(
                        "চার্জ যোগ করুন",
                        "Add charge"
                    )
                )
            }

            Text(
                v15Text(
                    "পেমেন্ট",
                    "Payment"
                ),
                fontWeight =
                    FontWeight.Bold
            )

            ServiceField(
                value = paymentAmount,
                onChange = {
                    paymentAmount = it
                },
                label =
                    v15Text(
                        "পেমেন্টের পরিমাণ",
                        "Payment amount"
                    )
            )

            ServiceField(
                value = paymentNote,
                onChange = {
                    paymentNote = it
                },
                label =
                    v15Text(
                        "পেমেন্ট নোট",
                        "Payment note"
                    )
            )

            Button(
                enabled = liveDue > 0,
                onClick = {
                    val requested =
                        paymentAmount
                            .toDoubleOrNull()
                            ?: 0.0

                    if (requested > 0) {
                        val amount =
                            requested
                                .coerceAtMost(
                                    liveDue
                                )

                        viewModel.addPayment(
                            jobId = job.jobId,
                            amount = amount,
                            note = paymentNote
                        )

                        paymentAmount = ""
                        paymentNote = ""
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
                "চার্জের ইতিহাস",
                "Charge history"
            ),
            fontWeight =
                FontWeight.Bold
        )

        if (charges.isEmpty()) {
            Text(
                v15Text(
                    "এখনো কোনো চার্জ নেই।",
                    "No charges yet."
                )
            )
        }

        charges.forEach {
            ServiceChargeCard(it)
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
            ServicePaymentCard(it)
        }
    }
}

@Composable
private fun ServiceChargeCard(
    item: ServiceChargeEntity
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
                    if (
                        item.chargeType ==
                        "PARTS"
                    ) {
                        v15Text(
                            "পার্টস/পণ্য",
                            "Parts"
                        )
                    } else {
                        v15Text(
                            "সার্ভিস",
                            "Service"
                        )
                    }
                )

                Text(
                    serviceMoney(
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
private fun ServicePaymentCard(
    item: ServicePaymentEntity
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
                    serviceDate(
                        item.paidAt
                    )
                )

                Text(
                    serviceMoney(
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
private fun ServiceField(
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

private fun serviceBusinessTitle(
    shopType: String
): String {
    val value =
        shopType.lowercase(
            Locale.getDefault()
        )

    return when {
        value.contains("laundry") ||
        value.contains("লন্ড্রি") ->
            v15Text(
                "লন্ড্রি অর্ডার",
                "Laundry Orders"
            )

        value.contains("salon") ||
        value.contains("beauty") ||
        value.contains("spa") ||
        value.contains("সেলুন") ||
        value.contains("বিউটি") ||
        value.contains("স্পা") ->
            v15Text(
                "সার্ভিস ও বুকিং",
                "Services & Bookings"
            )

        else ->
            v15Text(
                "সার্ভিস ও রিপেয়ার",
                "Service & Repair"
            )
    }
}

private fun serviceBusinessSubtitle(
    shopType: String
): String =
    when {
        shopType.contains(
            "Laundry",
            ignoreCase = true
        ) ||
        shopType.contains("লন্ড্রি") ->
            v15Text(
                "কাস্টমার, অর্ডার, চার্জ, পেমেন্ট ও বকেয়া",
                "Customers, orders, charges, payments and dues"
            )

        else ->
            v15Text(
                "কাস্টমার, কাজ, সার্ভিস চার্জ, পেমেন্ট ও বকেয়া",
                "Customers, jobs, service charges, payments and dues"
            )
    }

private fun serviceNewJobTitle(
    shopType: String
): String =
    if (
        shopType.contains(
            "Laundry",
            ignoreCase = true
        ) ||
        shopType.contains("লন্ড্রি")
    ) {
        v15Text(
            "নতুন লন্ড্রি অর্ডার",
            "New laundry order"
        )
    } else {
        v15Text(
            "নতুন কাজ / সার্ভিস",
            "New job / service"
        )
    }

private fun serviceJobNameLabel(
    shopType: String
): String =
    if (
        shopType.contains(
            "Laundry",
            ignoreCase = true
        ) ||
        shopType.contains("লন্ড্রি")
    ) {
        v15Text(
            "অর্ডারের নাম",
            "Order name"
        )
    } else {
        v15Text(
            "কাজের নাম",
            "Job title"
        )
    }

private fun serviceItemLabel(
    shopType: String
): String {
    val value =
        shopType.lowercase(
            Locale.getDefault()
        )

    return when {
        value.contains("laundry") ||
        value.contains("লন্ড্রি") ->
            v15Text(
                "কাপড় / আইটেম",
                "Clothes / items"
            )

        value.contains("salon") ||
        value.contains("beauty") ||
        value.contains("spa") ->
            v15Text(
                "সার্ভিস / প্যাকেজ",
                "Service / package"
            )

        else ->
            v15Text(
                "ডিভাইস / পণ্য",
                "Device / item"
            )
    }
}

private fun serviceReferenceLabel(
    shopType: String
): String =
    if (
        shopType.contains(
            "Repair",
            ignoreCase = true
        ) ||
        shopType.contains("রিপেয়ার")
    ) {
        v15Text(
            "সিরিয়াল / রেফারেন্স",
            "Serial / reference"
        )
    } else {
        v15Text(
            "রেফারেন্স",
            "Reference"
        )
    }

private fun serviceStatusLabel(
    status: String
): String =
    when (status) {
        "IN_PROGRESS" ->
            v15Text(
                "চলমান",
                "Working"
            )

        "READY" ->
            v15Text(
                "প্রস্তুত",
                "Ready"
            )

        "COMPLETED" ->
            v15Text(
                "সম্পন্ন",
                "Completed"
            )

        "CANCELLED" ->
            v15Text(
                "বাতিল",
                "Cancelled"
            )

        else ->
            v15Text(
                "নতুন",
                "Open"
            )
    }

private fun serviceMoney(
    value: Double
): String =
    String.format(
        Locale.US,
        "%.2f",
        value
    )

private fun serviceDate(
    value: Long
): String =
    SimpleDateFormat(
        "dd MMM yyyy",
        Locale.getDefault()
    ).format(
        Date(value)
    )
