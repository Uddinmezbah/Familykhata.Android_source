package com.familykhata.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.familykhata.app.agency.AgencyClientEntity
import com.familykhata.app.agency.AgencyViewModel
import com.familykhata.app.coaching.CoachingBatchEntity
import com.familykhata.app.coaching.CoachingStudentEntity
import com.familykhata.app.coaching.CoachingViewModel

@Composable
internal fun V15AgencyClientManagement(
    clients: List<AgencyClientEntity>,
    canWrite: Boolean,
    viewModel: AgencyViewModel
) {
    var editing by remember {
        mutableStateOf<AgencyClientEntity?>(null)
    }

    var deleting by remember {
        mutableStateOf<AgencyClientEntity?>(null)
    }

    Text(
        v15Text(
            "ক্লায়েন্ট তালিকা (${clients.size})",
            "Client list (${clients.size})"
        ),
        fontWeight = FontWeight.Bold
    )

    if (clients.isEmpty()) {
        Text(
            v15Text(
                "এখনো কোনো ক্লায়েন্ট যোগ করা হয়নি।",
                "No clients added yet."
            )
        )
    } else {
        clients.forEach { client ->
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        client.name,
                        fontWeight = FontWeight.Bold
                    )

                    if (client.company.isNotBlank()) {
                        Text(client.company)
                    }

                    if (client.phone.isNotBlank()) {
                        Text(client.phone)
                    }

                    if (client.email.isNotBlank()) {
                        Text(client.email)
                    }

                    if (canWrite) {
                        Row(
                            modifier =
                                Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    editing = client
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
                                onClick = {
                                    deleting = client
                                },
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
    }

    editing?.let { client ->
        EditAgencyClientDialog(
            item = client,
            onDismiss = {
                editing = null
            },
            onSave = {
                    name,
                    phone,
                    email,
                    company,
                    note ->

                viewModel.updateClient(
                    item = client,
                    name = name,
                    phone = phone,
                    email = email,
                    company = company,
                    note = note
                )

                editing = null
            }
        )
    }

    deleting?.let { client ->
        ConfirmEntityDeleteDialog(
            title =
                v15Text(
                    "ক্লায়েন্ট ডিলিট করবেন?",
                    "Delete client?"
                ),
            message =
                v15Text(
                    "${client.name} ডিলিট করলে তার প্রজেক্ট, চার্জ ও পেমেন্টের হিসাবও মুছে যাবে।",
                    "Deleting ${client.name} will also remove related projects, charges and payments."
                ),
            onDismiss = {
                deleting = null
            },
            onConfirm = {
                viewModel.deleteClient(client)
                deleting = null
            }
        )
    }
}

@Composable
private fun EditAgencyClientDialog(
    item: AgencyClientEntity,
    onDismiss: () -> Unit,
    onSave: (
        String,
        String,
        String,
        String,
        String
    ) -> Unit
) {
    var name by remember(item.id) {
        mutableStateOf(item.name)
    }

    var phone by remember(item.id) {
        mutableStateOf(item.phone)
    }

    var email by remember(item.id) {
        mutableStateOf(item.email)
    }

    var company by remember(item.id) {
        mutableStateOf(item.company)
    }

    var note by remember(item.id) {
        mutableStateOf(item.note)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                v15Text(
                    "ক্লায়েন্ট এডিট",
                    "Edit client"
                )
            )
        },
        text = {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(7.dp)
            ) {
                EntityField(
                    name,
                    { name = it },
                    v15Text(
                        "ক্লায়েন্টের নাম",
                        "Client name"
                    )
                )

                EntityField(
                    company,
                    { company = it },
                    v15Text(
                        "কোম্পানি / ব্র্যান্ড",
                        "Company / brand"
                    )
                )

                EntityField(
                    phone,
                    { phone = it },
                    v15Text("ফোন", "Phone")
                )

                EntityField(
                    email,
                    { email = it },
                    "Email"
                )

                EntityField(
                    note,
                    { note = it },
                    v15Text("নোট", "Note")
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(
                        name,
                        phone,
                        email,
                        company,
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
internal fun V15CoachingEntityManagement(
    students: List<CoachingStudentEntity>,
    batches: List<CoachingBatchEntity>,
    canWrite: Boolean,
    viewModel: CoachingViewModel
) {
    var editingStudent by remember {
        mutableStateOf<CoachingStudentEntity?>(null)
    }

    var deletingStudent by remember {
        mutableStateOf<CoachingStudentEntity?>(null)
    }

    var editingBatch by remember {
        mutableStateOf<CoachingBatchEntity?>(null)
    }

    var deletingBatch by remember {
        mutableStateOf<CoachingBatchEntity?>(null)
    }

    Text(
        v15Text(
            "শিক্ষার্থী তালিকা (${students.size})",
            "Student list (${students.size})"
        ),
        fontWeight = FontWeight.Bold
    )

    if (students.isEmpty()) {
        Text(
            v15Text(
                "এখনো কোনো শিক্ষার্থী যোগ করা হয়নি।",
                "No students added yet."
            )
        )
    } else {
        students.forEach { student ->
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        student.name,
                        fontWeight = FontWeight.Bold
                    )

                    if (student.phone.isNotBlank()) {
                        Text(student.phone)
                    }

                    if (
                        student.guardianName.isNotBlank()
                    ) {
                        Text(
                            v15Text(
                                "অভিভাবক: ${student.guardianName}",
                                "Guardian: ${student.guardianName}"
                            )
                        )
                    }

                    if (canWrite) {
                        EntityEditDeleteRow(
                            onEdit = {
                                editingStudent =
                                    student
                            },
                            onDelete = {
                                deletingStudent =
                                    student
                            }
                        )
                    }
                }
            }
        }
    }

    Text(
        v15Text(
            "ব্যাচ / কোর্স তালিকা (${batches.size})",
            "Batch / course list (${batches.size})"
        ),
        fontWeight = FontWeight.Bold
    )

    if (batches.isEmpty()) {
        Text(
            v15Text(
                "এখনো কোনো ব্যাচ / কোর্স যোগ করা হয়নি।",
                "No batch / course added yet."
            )
        )
    } else {
        batches.forEach { batch ->
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        batch.name,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        v15Text(
                            "ভর্তি ফি: ৳${entityMoney(batch.admissionFee)}",
                            "Admission fee: ৳${entityMoney(batch.admissionFee)}"
                        )
                    )

                    Text(
                        v15Text(
                            "মাসিক ফি: ৳${entityMoney(batch.monthlyFee)}",
                            "Monthly fee: ৳${entityMoney(batch.monthlyFee)}"
                        )
                    )

                    if (canWrite) {
                        EntityEditDeleteRow(
                            onEdit = {
                                editingBatch = batch
                            },
                            onDelete = {
                                deletingBatch = batch
                            }
                        )
                    }
                }
            }
        }
    }

    editingStudent?.let { student ->
        EditCoachingStudentDialog(
            item = student,
            onDismiss = {
                editingStudent = null
            },
            onSave = {
                    name,
                    phone,
                    guardian,
                    address ->

                viewModel.updateStudent(
                    item = student,
                    name = name,
                    phone = phone,
                    guardian = guardian,
                    address = address
                )

                editingStudent = null
            }
        )
    }

    deletingStudent?.let { student ->
        ConfirmEntityDeleteDialog(
            title =
                v15Text(
                    "শিক্ষার্থী ডিলিট করবেন?",
                    "Delete student?"
                ),
            message =
                v15Text(
                    "${student.name} ডিলিট করলে তার ভর্তি, ফি ও পেমেন্টের হিসাবও মুছে যাবে।",
                    "Deleting ${student.name} will also remove enrollment, fee and payment records."
                ),
            onDismiss = {
                deletingStudent = null
            },
            onConfirm = {
                viewModel.deleteStudent(student)
                deletingStudent = null
            }
        )
    }

    editingBatch?.let { batch ->
        EditCoachingBatchDialog(
            item = batch,
            onDismiss = {
                editingBatch = null
            },
            onSave = {
                    name,
                    admission,
                    monthly,
                    note ->

                viewModel.updateBatch(
                    item = batch,
                    name = name,
                    admissionFee = admission,
                    monthlyFee = monthly,
                    note = note
                )

                editingBatch = null
            }
        )
    }

    deletingBatch?.let { batch ->
        ConfirmEntityDeleteDialog(
            title =
                v15Text(
                    "কোর্স / ব্যাচ ডিলিট করবেন?",
                    "Delete course / batch?"
                ),
            message =
                v15Text(
                    "${batch.name} ডিলিট করলে এই কোর্সের ভর্তি, ফি ও পেমেন্টের হিসাবও মুছে যাবে।",
                    "Deleting ${batch.name} will also remove related enrollment, fee and payment records."
                ),
            onDismiss = {
                deletingBatch = null
            },
            onConfirm = {
                viewModel.deleteBatch(batch)
                deletingBatch = null
            }
        )
    }
}

@Composable
private fun EntityEditDeleteRow(
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onEdit,
            modifier = Modifier.weight(1f)
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
            modifier = Modifier.weight(1f)
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

@Composable
private fun EditCoachingStudentDialog(
    item: CoachingStudentEntity,
    onDismiss: () -> Unit,
    onSave: (
        String,
        String,
        String,
        String
    ) -> Unit
) {
    var name by remember(item.id) {
        mutableStateOf(item.name)
    }

    var phone by remember(item.id) {
        mutableStateOf(item.phone)
    }

    var guardian by remember(item.id) {
        mutableStateOf(item.guardianName)
    }

    var address by remember(item.id) {
        mutableStateOf(item.address)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                v15Text(
                    "শিক্ষার্থী এডিট",
                    "Edit student"
                )
            )
        },
        text = {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(7.dp)
            ) {
                EntityField(
                    name,
                    { name = it },
                    v15Text(
                        "শিক্ষার্থীর নাম",
                        "Student name"
                    )
                )

                EntityField(
                    phone,
                    { phone = it },
                    v15Text("ফোন", "Phone")
                )

                EntityField(
                    guardian,
                    { guardian = it },
                    v15Text(
                        "অভিভাবকের নাম",
                        "Guardian name"
                    )
                )

                EntityField(
                    address,
                    { address = it },
                    v15Text(
                        "ঠিকানা",
                        "Address"
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(
                        name,
                        phone,
                        guardian,
                        address
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
private fun EditCoachingBatchDialog(
    item: CoachingBatchEntity,
    onDismiss: () -> Unit,
    onSave: (
        String,
        Double,
        Double,
        String
    ) -> Unit
) {
    var name by remember(item.id) {
        mutableStateOf(item.name)
    }

    var admission by remember(item.id) {
        mutableStateOf(
            item.admissionFee.toString()
        )
    }

    var monthly by remember(item.id) {
        mutableStateOf(
            item.monthlyFee.toString()
        )
    }

    var note by remember(item.id) {
        mutableStateOf(item.note)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                v15Text(
                    "ব্যাচ / কোর্স এডিট",
                    "Edit batch / course"
                )
            )
        },
        text = {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(7.dp)
            ) {
                EntityField(
                    name,
                    { name = it },
                    v15Text(
                        "ব্যাচ / কোর্সের নাম",
                        "Batch / course name"
                    )
                )

                EntityField(
                    admission,
                    { admission = it },
                    v15Text(
                        "ভর্তি ফি",
                        "Admission fee"
                    )
                )

                EntityField(
                    monthly,
                    { monthly = it },
                    v15Text(
                        "মাসিক ফি",
                        "Monthly fee"
                    )
                )

                EntityField(
                    note,
                    { note = it },
                    v15Text("নোট", "Note")
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(
                        name,
                        entityInputNumber(
                            admission
                        ),
                        entityInputNumber(
                            monthly
                        ),
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
private fun ConfirmEntityDeleteDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title)
        },
        text = {
            Text(message)
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
private fun EntityField(
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


private fun entityInputNumber(
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

private fun entityMoney(
    value: Double
): String =
    String.format(
        java.util.Locale.US,
        "%.2f",
        value
    )
