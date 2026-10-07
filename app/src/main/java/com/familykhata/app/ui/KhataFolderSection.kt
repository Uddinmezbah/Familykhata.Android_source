package com.familykhata.app.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.familykhata.app.FamilyKhataViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun KhataFolderSection(
    viewModel: FamilyKhataViewModel,
    workspace: String
) {
    if (workspace == "SHOP") return

    val folders by viewModel.khataFolders.collectAsState()
    val selectedId by viewModel.selectedKhataFolderId.collectAsState()
    val selected = folders.firstOrNull { it.id == selectedId }
    val context = LocalContext.current

    var showList by remember { mutableStateOf(false) }
    var showCreate by remember { mutableStateOf(false) }
    var showReport by remember { mutableStateOf(false) }
    var draftName by remember(workspace) { mutableStateOf("") }

    val monthLabel =
        remember {
            SimpleDateFormat(
                "MMMM yyyy",
                Locale.getDefault()
            ).format(Date())
        }

    val workspaceLabel =
        if (workspace == "FAMILY") {
            v15Text("পরিবার", "Family")
        } else {
            v15Text("নিজের", "Personal")
        }

    Card(
        onClick = { showList = true },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme
                        .secondaryContainer
                        .copy(alpha = 0.55f)
            )
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
            horizontalArrangement =
                Arrangement.spacedBy(12.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Text("📁", style = MaterialTheme.typography.titleLarge)

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    v15Text("বর্তমান খাতা", "Current khata"),
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    selected?.name
                        ?: v15Text(
                            "খাতা প্রস্তুত হচ্ছে…",
                            "Preparing khata…"
                        ),
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Text("›", style = MaterialTheme.typography.titleLarge)
        }
    }

    OutlinedButton(
        enabled = selected != null,
        onClick = { showReport = true },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            v15Text(
                "খাতা রিপোর্ট • PDF • Print • Share",
                "Khata Report • PDF • Print • Share"
            )
        )
    }

    if (showList) {
        AlertDialog(
            onDismissRequest = { showList = false },
            title = {
                Text(v15Text("আমার খাতা", "My khata"))
            },
            text = {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 460.dp)
                            .verticalScroll(
                                rememberScrollState()
                            ),
                    verticalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    folders.forEach { folder ->
                        if (folder.id == selectedId) {
                            Button(
                                onClick = {
                                    viewModel.selectKhataFolder(
                                        folder.id
                                    )
                                    showList = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("✓ ${folder.name}")
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    viewModel.selectKhataFolder(
                                        folder.id
                                    )
                                    showList = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(folder.name)
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            draftName =
                                "$workspaceLabel — $monthLabel"
                            showList = false
                            showCreate = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(v15Text("+ নতুন খাতা", "+ New khata"))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showList = false }) {
                    Text(v15Text("বন্ধ", "Close"))
                }
            }
        )
    }

    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = {
                Text(
                    v15Text(
                        "নতুন খাতা তৈরি করুন",
                        "Create new khata"
                    )
                )
            },
            text = {
                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = draftName,
                        onValueChange = { draftName = it },
                        label = {
                            Text(v15Text("খাতার নাম", "Khata name"))
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        v15Text(
                            "উদাহরণ: পরিবার — জানুয়ারি 2027",
                            "Example: Family — January 2027"
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = draftName.trim().isNotBlank(),
                    onClick = {
                        viewModel.createKhataFolder(
                            draftName
                        ) { success ->
                            if (success) {
                                showCreate = false
                                Toast.makeText(
                                    context,
                                    v15Text(
                                        "নতুন খাতা তৈরি হয়েছে",
                                        "New khata created"
                                    ),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    v15Text(
                                        "এই নামে খাতা আছে বা নামটি সঠিক নয়",
                                        "A khata with this name already exists or the name is invalid"
                                    ),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                ) {
                    Text(v15Text("তৈরি করুন", "Create"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreate = false }) {
                    Text(v15Text("বাতিল", "Cancel"))
                }
            }
        )
    }

    if (showReport) {
        KhataReportDialog(
            viewModel = viewModel,
            workspace = workspace,
            onDismiss = {
                showReport = false
            }
        )
    }
}
