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
import com.familykhata.app.agency.AgencyChargeEntity
import com.familykhata.app.agency.AgencyClientEntity
import com.familykhata.app.agency.AgencyPaymentEntity
import com.familykhata.app.agency.AgencyProjectSummary
import com.familykhata.app.agency.AgencyViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun V15AgencyScreen(
    workspace: String,
    canWrite: Boolean,
    onExit: () -> Unit
) {
    val vm: AgencyViewModel = viewModel()

    val clients by vm.clients.collectAsState()
    val projects by vm.projects.collectAsState()

    var showClientDialog by remember {
        mutableStateOf(false)
    }

    var showProjectDialog by remember {
        mutableStateOf(false)
    }

    var selectedProject by remember {
        mutableStateOf<AgencyProjectSummary?>(null)
    }

    var editingProject by remember {
        mutableStateOf<AgencyProjectSummary?>(null)
    }

    var deletingProject by remember {
        mutableStateOf<AgencyProjectSummary?>(null)
    }

    LaunchedEffect(workspace) {
        vm.setWorkspace(workspace)
    }

    BackHandler(enabled = selectedProject != null) {
        selectedProject = null
    }

    BackHandler(enabled = selectedProject == null) {
        onExit()
    }

    if (selectedProject != null) {
        AgencyProjectLedger(
            project = selectedProject!!,
            viewModel = vm,
            canWrite = canWrite,
            onBack = {
                selectedProject = null
            }
        )

        return
    }

    val totalValue =
        projects.sumOf {
            it.totalPrice
        }

    val totalPaid =
        projects.sumOf {
            it.totalPaid
        }

    val totalDue =
        projects.sumOf {
            it.dueAmount.coerceAtLeast(0.0)
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            ),
        verticalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {
        Text(
            v15Text(
                "ডিজিটাল এজেন্সি",
                "Digital Agency"
            ),
            style =
                MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold
        )

        Text(
            v15Text(
                "ক্লায়েন্ট, প্রজেক্ট, পেমেন্ট ও বকেয়া",
                "Clients, projects, payments and dues"
            )
        )

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            AgencyMetric(
                title =
                    v15Text(
                        "প্রজেক্ট",
                        "Projects"
                    ),
                value =
                    projects.size.toString(),
                modifier =
                    Modifier.weight(1f)
            )

            AgencyMetric(
                title =
                    v15Text(
                        "মোট মূল্য",
                        "Value"
                    ),
                value =
                    agencyMoney(totalValue),
                modifier =
                    Modifier.weight(1f)
            )
        }

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            AgencyMetric(
                title =
                    v15Text(
                        "আদায়",
                        "Paid"
                    ),
                value =
                    agencyMoney(totalPaid),
                modifier =
                    Modifier.weight(1f)
            )

            AgencyMetric(
                title =
                    v15Text(
                        "বকেয়া",
                        "Due"
                    ),
                value =
                    agencyMoney(totalDue),
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
                        showClientDialog = true
                    },
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text(
                        v15Text(
                            "+ ক্লায়েন্ট",
                            "+ Client"
                        )
                    )
                }

                OutlinedButton(
                    onClick = {
                        showProjectDialog = true
                    },
                    enabled = clients.isNotEmpty(),
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text(
                        v15Text(
                            "+ প্রজেক্ট",
                            "+ Project"
                        )
                    )
                }
            }
        }

        V15AgencyClientManagement(
            clients = clients,
            canWrite = canWrite,
            viewModel = vm
        )

        Text(
            v15Text(
                "চলমান প্রজেক্ট ও হিসাব",
                "Projects & accounts"
            ),
            fontWeight = FontWeight.Bold
        )

        if (projects.isEmpty()) {
            Text(
                v15Text(
                    "এখনো কোনো প্রজেক্ট যোগ করা হয়নি।",
                    "No projects added yet."
                )
            )
        } else {
            projects.forEach { item ->
                AgencyProjectCard(
                    item = item,
                    canWrite = canWrite,
                    onOpen = {
                        selectedProject = item
                    },
                    onEdit = {
                        editingProject = item
                    },
                    onDelete = {
                        deletingProject = item
                    }
                )
            }
        }
    }

    if (showClientDialog) {
        AddAgencyClientDialog(
            onDismiss = {
                showClientDialog = false
            }
        ) {
                name,
                phone,
                email,
                company,
                note ->

            vm.addClient(
                name = name,
                phone = phone,
                email = email,
                company = company,
                note = note,
                workspace = workspace
            )

            showClientDialog = false
        }
    }

    if (showProjectDialog) {
        AddAgencyProjectDialog(
            clients = clients,
            onDismiss = {
                showProjectDialog = false
            }
        ) {
                clientId,
                title,
                service,
                total,
                advance,
                note ->

            vm.addProject(
                clientId = clientId,
                title = title,
                serviceType = service,
                totalPrice = total,
                advance = advance,
                note = note
            )

            showProjectDialog = false
        }
    }

    editingProject?.let { project ->
        EditAgencyProjectDialog(
            project = project,
            clients = clients,
            onDismiss = {
                editingProject = null
            },
            onSave = {
                    clientId,
                    title,
                    service,
                    basePrice,
                    note ->

                vm.updateProject(
                    projectId =
                        project.projectId,
                    clientId = clientId,
                    title = title,
                    serviceType = service,
                    basePrice = basePrice,
                    note = note
                ) { success ->
                    if (success) {
                        editingProject = null
                    }
                }
            }
        )
    }

    deletingProject?.let { project ->
        DeleteAgencyProjectDialog(
            project = project,
            onDismiss = {
                deletingProject = null
            },
            onConfirm = {
                vm.deleteProject(
                    project.projectId
                ) { success ->
                    if (success) {
                        if (
                            selectedProject
                                ?.projectId ==
                            project.projectId
                        ) {
                            selectedProject = null
                        }

                        deletingProject = null
                    }
                }
            }
        )
    }
}

@Composable
private fun AgencyMetric(
    title: String,
    value: String,
    modifier: Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier =
                Modifier.padding(11.dp)
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
private fun AgencyProjectCard(
    item: AgencyProjectSummary,
    canWrite: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier =
            Modifier.fillMaxWidth()
    ) {
        Column(
            modifier =
                Modifier.padding(14.dp),
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
                    item.title,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    agencyStatusLabel(
                        item.status
                    ),
                    style =
                        MaterialTheme.typography.labelMedium
                )
            }

            Text(
                v15Text(
                    "ক্লায়েন্ট: ${item.clientName}",
                    "Client: ${item.clientName}"
                )
            )

            if (item.company.isNotBlank()) {
                Text(item.company)
            }

            if (item.serviceType.isNotBlank()) {
                Text(
                    v15Text(
                        "সার্ভিস: ${item.serviceType}",
                        "Service: ${item.serviceType}"
                    )
                )
            }

            Text(
                v15Text(
                    "প্রজেক্ট মূল্য: ${agencyMoney(item.projectBasePrice)}",
                    "Project price: ${agencyMoney(item.projectBasePrice)}"
                )
            )

            Text(
                v15Text(
                    "মোট চার্জ: ${agencyMoney(item.totalPrice)}",
                    "Total charges: ${agencyMoney(item.totalPrice)}"
                )
            )

            Text(
                v15Text(
                    "পরিশোধ: ${agencyMoney(item.totalPaid)}",
                    "Paid: ${agencyMoney(item.totalPaid)}"
                )
            )

            Text(
                v15Text(
                    "বকেয়া: ${agencyMoney(item.dueAmount.coerceAtLeast(0.0))}",
                    "Due: ${agencyMoney(item.dueAmount.coerceAtLeast(0.0))}"
                ),
                fontWeight =
                    FontWeight.SemiBold
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = onOpen,
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text(
                        v15Text(
                            "হিসাব",
                            "Details"
                        )
                    )
                }

                if (canWrite) {
                    OutlinedButton(
                        onClick = onEdit,
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
                                "ডিলিট",
                                "Delete"
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddAgencyClientDialog(
    onDismiss: () -> Unit,
    onSave: (
        String,
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

    var email by remember {
        mutableStateOf("")
    }

    var company by remember {
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
                    "নতুন ক্লায়েন্ট",
                    "New client"
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
                AgencyField(
                    value = name,
                    onChange = {
                        name = it
                    },
                    label =
                        v15Text(
                            "ক্লায়েন্টের নাম",
                            "Client name"
                        )
                )

                AgencyField(
                    value = company,
                    onChange = {
                        company = it
                    },
                    label =
                        v15Text(
                            "কোম্পানি / ব্র্যান্ড",
                            "Company / brand"
                        )
                )

                AgencyField(
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

                AgencyField(
                    value = email,
                    onChange = {
                        email = it
                    },
                    label = "Email"
                )

                AgencyField(
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
                onClick = {
                    if (
                        name.isNotBlank()
                    ) {
                        onSave(
                            name,
                            phone,
                            email,
                            company,
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
private fun AddAgencyProjectDialog(
    clients: List<AgencyClientEntity>,
    onDismiss: () -> Unit,
    onSave: (
        Long,
        String,
        String,
        Double,
        Double,
        String
    ) -> Unit
) {
    var clientId by remember {
        mutableStateOf<Long?>(null)
    }

    var title by remember {
        mutableStateOf("")
    }

    var service by remember {
        mutableStateOf("")
    }

    var total by remember {
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
                v15Text(
                    "নতুন প্রজেক্ট / সার্ভিস",
                    "New project / service"
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
                        "ক্লায়েন্ট নির্বাচন",
                        "Select client"
                    ),
                    fontWeight = FontWeight.Bold
                )

                clients.forEach { client ->
                    OutlinedButton(
                        onClick = {
                            clientId =
                                client.id
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (
                                clientId ==
                                client.id
                            ) {
                                "✓ ${client.name}"
                            } else {
                                client.name
                            }
                        )
                    }
                }

                AgencyField(
                    value = title,
                    onChange = {
                        title = it
                    },
                    label =
                        v15Text(
                            "প্রজেক্টের নাম",
                            "Project name"
                        )
                )

                AgencyField(
                    value = service,
                    onChange = {
                        service = it
                    },
                    label =
                        v15Text(
                            "সার্ভিস",
                            "Service"
                        )
                )

                AgencyField(
                    value = total,
                    onChange = {
                        total = it
                    },
                    label =
                        v15Text(
                            "মোট মূল্য",
                            "Total price"
                        )
                )

                AgencyField(
                    value = advance,
                    onChange = {
                        advance = it
                    },
                    label =
                        v15Text(
                            "অগ্রিম / Advance",
                            "Advance payment"
                        )
                )

                AgencyField(
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
                    clientId != null &&
                    title.isNotBlank() &&
                    (
                        agencyInputNumber(total)
                    ) > 0,
                onClick = {
                    onSave(
                        clientId!!,
                        title,
                        service,
                        agencyInputNumber(total),
                        agencyInputNumber(advance),
                        note
                    )
                }
            ) {
                Text(
                    v15Text(
                        "প্রজেক্ট যোগ করুন",
                        "Add project"
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
private fun EditAgencyProjectDialog(
    project: AgencyProjectSummary,
    clients: List<AgencyClientEntity>,
    onDismiss: () -> Unit,
    onSave: (
        Long,
        String,
        String,
        Double,
        String
    ) -> Unit
) {
    var clientId by remember(project.projectId) {
        mutableStateOf(project.clientId)
    }

    var title by remember(project.projectId) {
        mutableStateOf(project.title)
    }

    var service by remember(project.projectId) {
        mutableStateOf(project.serviceType)
    }

    var basePrice by remember(project.projectId) {
        mutableStateOf(
            project.projectBasePrice.toString()
        )
    }

    var note by remember(project.projectId) {
        mutableStateOf(project.projectNote)
    }

    val price =
        agencyInputNumber(basePrice)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                v15Text(
                    "প্রজেক্ট এডিট",
                    "Edit project"
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
                        "ক্লায়েন্ট নির্বাচন",
                        "Select client"
                    ),
                    fontWeight =
                        FontWeight.Bold
                )

                clients.forEach { client ->
                    OutlinedButton(
                        onClick = {
                            clientId = client.id
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (
                                clientId ==
                                client.id
                            ) {
                                "✓ ${client.name}"
                            } else {
                                client.name
                            }
                        )
                    }
                }

                AgencyField(
                    value = title,
                    onChange = {
                        title = it
                    },
                    label =
                        v15Text(
                            "প্রজেক্টের নাম",
                            "Project name"
                        )
                )

                AgencyField(
                    value = service,
                    onChange = {
                        service = it
                    },
                    label =
                        v15Text(
                            "সার্ভিস",
                            "Service"
                        )
                )

                AgencyField(
                    value = basePrice,
                    onChange = {
                        basePrice = it
                    },
                    label =
                        v15Text(
                            "প্রজেক্টের মূল মূল্য",
                            "Base project price"
                        )
                )

                Text(
                    v15Text(
                        "এখানে মূল প্রজেক্ট মূল্য পরিবর্তন হবে। অতিরিক্ত মাসিক/অন্যান্য চার্জ অপরিবর্তিত থাকবে।",
                        "This changes the base project price only. Recurring and other charges remain unchanged."
                    ),
                    style =
                        MaterialTheme.typography.bodySmall
                )

                AgencyField(
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
                    clientId > 0 &&
                    title.isNotBlank() &&
                    price > 0,
                onClick = {
                    onSave(
                        clientId,
                        title,
                        service,
                        price,
                        note
                    )
                }
            ) {
                Text(
                    v15Text(
                        "আপডেট",
                        "Update"
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
private fun DeleteAgencyProjectDialog(
    project: AgencyProjectSummary,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                v15Text(
                    "প্রজেক্ট ডিলিট করবেন?",
                    "Delete project?"
                )
            )
        },
        text = {
            Text(
                v15Text(
                    "${project.title} ডিলিট করলে এই প্রজেক্টের চার্জ ও পেমেন্ট হিসাবও মুছে যাবে। এই কাজ ফিরিয়ে আনা যাবে না।",
                    "Deleting ${project.title} will also remove its charges and payments. This cannot be undone."
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text(
                    v15Text(
                        "ডিলিট করুন",
                        "Delete"
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
private fun AgencyProjectLedger(
    project: AgencyProjectSummary,
    viewModel: AgencyViewModel,
    canWrite: Boolean,
    onBack: () -> Unit
) {
    val charges by
        viewModel.observeCharges(
            project.projectId
        ).collectAsState(
            initial = emptyList()
        )

    val payments by
        viewModel.observePayments(
            project.projectId
        ).collectAsState(
            initial = emptyList()
        )

    var chargeType by remember {
        mutableStateOf("RECURRING")
    }

    var chargePeriod by remember {
        mutableStateOf("")
    }

    var chargeAmount by remember {
        mutableStateOf("")
    }

    var chargeNote by remember {
        mutableStateOf("")
    }

    var chargeMessage by remember {
        mutableStateOf("")
    }

    var paymentAmount by remember {
        mutableStateOf("")
    }

    var paymentNote by remember {
        mutableStateOf("")
    }

    val liveTotal =
        charges.sumOf {
            it.amount
        }

    val livePaid =
        payments.sumOf {
            it.amount
        }

    val liveDue =
        (
            liveTotal -
                livePaid
            ).coerceAtLeast(0.0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            ),
        verticalArrangement =
            Arrangement.spacedBy(10.dp)
    ) {
        TextButton(
            onClick = onBack
        ) {
            Text(
                v15Text(
                    "← প্রজেক্ট তালিকা",
                    "← Project list"
                )
            )
        }

        Text(
            project.title,
            style =
                MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold
        )

        Text(
            v15Text(
                "ক্লায়েন্ট: ${project.clientName}",
                "Client: ${project.clientName}"
            )
        )

        if (
            project.serviceType
                .isNotBlank()
        ) {
            Text(
                v15Text(
                    "সার্ভিস: ${project.serviceType}",
                    "Service: ${project.serviceType}"
                )
            )
        }

        Text(
            v15Text(
                "মোট চার্জ: ${agencyMoney(liveTotal)}",
                "Total charges: ${agencyMoney(liveTotal)}"
            )
        )

        Text(
            v15Text(
                "মোট পরিশোধ: ${agencyMoney(livePaid)}",
                "Total paid: ${agencyMoney(livePaid)}"
            )
        )

        Text(
            v15Text(
                "বকেয়া: ${agencyMoney(liveDue)}",
                "Due: ${agencyMoney(liveDue)}"
            ),
            fontWeight = FontWeight.Bold
        )

        Text(
            v15Text(
                "স্ট্যাটাস: ${agencyStatusLabel(project.status)}",
                "Status: ${agencyStatusLabel(project.status)}"
            )
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
                        viewModel
                            .setProjectStatus(
                                project.projectId,
                                "ACTIVE"
                            )
                    },
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text(
                        v15Text(
                            "চলমান",
                            "Active"
                        )
                    )
                }

                OutlinedButton(
                    onClick = {
                        viewModel
                            .setProjectStatus(
                                project.projectId,
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
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "PACKAGE",
                    "RECURRING",
                    "OTHER"
                ).forEach { type ->
                    OutlinedButton(
                        onClick = {
                            chargeType = type
                            chargeMessage = ""
                        },
                        modifier =
                            Modifier.weight(1f)
                    ) {
                        Text(
                            if (chargeType == type) {
                                "✓ ${agencyChargeTypeLabel(type)}"
                            } else {
                                agencyChargeTypeLabel(type)
                            }
                        )
                    }
                }
            }

            if (
                chargeType == "PACKAGE" ||
                chargeType == "RECURRING"
            ) {
                AgencyField(
                    value = chargePeriod,
                    onChange = {
                        chargePeriod = it
                        chargeMessage = ""
                    },
                    label =
                        v15Text(
                            "পিরিয়ড (যেমন 2026-09)",
                            "Period (e.g. 2026-09)"
                        )
                )
            }

            AgencyField(
                value = chargeAmount,
                onChange = {
                    chargeAmount = it
                    chargeMessage = ""
                },
                label =
                    v15Text(
                        "চার্জের পরিমাণ",
                        "Charge amount"
                    )
            )

            AgencyField(
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
                enabled =
                    (chargeAmount.toDoubleOrNull() ?: 0.0) > 0 &&
                    (
                        chargeType == "OTHER" ||
                        chargePeriod.isNotBlank()
                    ),
                onClick = {
                    val amount =
                        chargeAmount
                            .toDoubleOrNull()
                            ?: 0.0

                    viewModel.addCharge(
                        projectId = project.projectId,
                        chargeType = chargeType,
                        periodKey =
                            if (chargeType == "OTHER") {
                                ""
                            } else {
                                chargePeriod
                            },
                        amount = amount,
                        dueDate = null,
                        note = chargeNote
                    ) { success ->
                        chargeMessage =
                            if (success) {
                                chargeAmount = ""
                                chargeNote = ""

                                if (
                                    chargeType != "OTHER"
                                ) {
                                    chargePeriod = ""
                                }

                                v15Text(
                                    "চার্জ যোগ হয়েছে।",
                                    "Charge added."
                                )
                            } else {
                                v15Text(
                                    "একই পিরিয়ডের চার্জ আগে আছে অথবা তথ্য সঠিক নয়।",
                                    "This period already has a charge or the data is invalid."
                                )
                            }
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

            if (chargeMessage.isNotBlank()) {
                Text(
                    chargeMessage,
                    style =
                        MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                v15Text(
                    "পেমেন্ট",
                    "Payment"
                ),
                fontWeight = FontWeight.Bold
            )

            AgencyField(
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

            AgencyField(
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
                onClick = {
                    val amount =
                        paymentAmount
                            .toDoubleOrNull()
                            ?: 0.0

                    if (amount > 0) {
                        viewModel
                            .addPayment(
                                project.projectId,
                                amount,
                                paymentNote
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
                "চার্জ ইতিহাস",
                "Charge history"
            ),
            fontWeight = FontWeight.Bold
        )

        if (charges.isEmpty()) {
            Text(
                v15Text(
                    "এখনো কোনো চার্জ নেই।",
                    "No charges yet."
                )
            )
        }

        charges.forEach { charge ->
            AgencyChargeCard(charge)
        }

        Text(
            v15Text(
                "পেমেন্ট ইতিহাস",
                "Payment history"
            ),
            fontWeight = FontWeight.Bold
        )

        if (payments.isEmpty()) {
            Text(
                v15Text(
                    "এখনো কোনো পেমেন্ট নেই।",
                    "No payments yet."
                )
            )
        }

        payments.forEach { payment ->
            AgencyPaymentCard(payment)
        }
    }
}

@Composable
private fun AgencyChargeCard(
    charge: AgencyChargeEntity
) {
    Card(
        modifier =
            Modifier.fillMaxWidth()
    ) {
        Column(
            modifier =
                Modifier.padding(11.dp),
            verticalArrangement =
                Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {
                Text(
                    agencyChargeTypeLabel(
                        charge.chargeType
                    )
                )

                Text(
                    agencyMoney(
                        charge.amount
                    ),
                    fontWeight =
                        FontWeight.Bold
                )
            }

            if (
                charge.periodKey.isNotBlank()
            ) {
                Text(
                    v15Text(
                        "পিরিয়ড: ${charge.periodKey}",
                        "Period: ${charge.periodKey}"
                    ),
                    style =
                        MaterialTheme.typography.bodySmall
                )
            }

            Text(
                agencyDate(
                    charge.createdAt
                ),
                style =
                    MaterialTheme.typography.bodySmall
            )

            if (
                charge.note.isNotBlank()
            ) {
                Text(
                    charge.note,
                    style =
                        MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}


@Composable
private fun AgencyPaymentCard(
    payment: AgencyPaymentEntity
) {
    Card(
        modifier =
            Modifier.fillMaxWidth()
    ) {
        Column(
            modifier =
                Modifier.padding(11.dp),
            verticalArrangement =
                Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {
                Text(
                    agencyDate(
                        payment.paidAt
                    )
                )

                Text(
                    agencyMoney(
                        payment.amount
                    ),
                    fontWeight =
                        FontWeight.Bold
                )
            }

            if (
                payment.note.isNotBlank()
            ) {
                Text(
                    payment.note,
                    style =
                        MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun AgencyField(
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

private fun agencyInputNumber(
    value: String
): Double {
    val normalized =
        value.trim()
            .replace('০', '0')
            .replace('১', '1')
            .replace('২', '2')
            .replace('৩', '3')
            .replace('৪', '4')
            .replace('৫', '5')
            .replace('৬', '6')
            .replace('৭', '7')
            .replace('৮', '8')
            .replace('৯', '9')
            .replace(",", "")

    return normalized
        .toDoubleOrNull()
        ?.coerceAtLeast(0.0)
        ?: 0.0
}

private fun agencyChargeTypeLabel(
    value: String
): String =
    when (value) {
        "PACKAGE" ->
            v15Text(
                "প্যাকেজ",
                "Package"
            )

        "RECURRING" ->
            v15Text(
                "মাসিক / নিয়মিত",
                "Recurring"
            )

        else ->
            v15Text(
                "অন্যান্য",
                "Other"
            )
    }

private fun agencyStatusLabel(
    value: String
): String =
    when (value) {
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
                "চলমান",
                "Active"
            )
    }

private fun agencyMoney(
    value: Double
): String =
    String.format(
        Locale.US,
        "%.2f",
        value
    )

private fun agencyDate(
    value: Long
): String =
    SimpleDateFormat(
        "dd MMM yyyy",
        Locale.getDefault()
    ).format(
        Date(value)
    )
