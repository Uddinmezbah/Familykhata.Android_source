package com.familykhata.app.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.familykhata.app.agency.AgencyProjectSummary
import com.familykhata.app.report.writeAgencyReportPdf
import kotlinx.coroutines.launch

@Composable
internal fun AgencyReportSection(
    projects: List<AgencyProjectSummary>
) {
    val context =
        LocalContext.current

    val scope =
        rememberCoroutineScope()

    var busy by
        remember {
            mutableStateOf(false)
        }

    var error by
        remember {
            mutableStateOf<String?>(null)
        }

    Card(
        modifier =
            Modifier.fillMaxWidth()
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
            verticalArrangement =
                Arrangement.spacedBy(
                    8.dp
                )
        ) {
            Text(
                v15Text(
                    "এজেন্সি রিপোর্ট",
                    "Agency report"
                ),
                fontWeight =
                    FontWeight.Bold
            )

            Text(
                v15Text(
                    "ক্লায়েন্ট, প্রজেক্ট, মোট চার্জ, আদায়, বকেয়া ও স্ট্যাটাসসহ PDF রিপোর্ট তৈরি করুন।",
                    "Create a PDF report with clients, projects, charges, payments, dues and status."
                ),
                style =
                    MaterialTheme.typography
                        .bodySmall
            )

            Button(
                onClick = {
                    if (
                        projects.isEmpty()
                    ) {
                        error =
                            v15Text(
                                "রিপোর্ট তৈরির জন্য কোনো প্রজেক্ট নেই",
                                "There are no projects to report"
                            )

                        return@Button
                    }

                    busy = true
                    error = null

                    scope.launch {
                        runCatching {
                            val pdf =
                                writeAgencyReportPdf(
                                    context =
                                        context,
                                    projects =
                                        projects
                                )

                            val uri =
                                FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.statements",
                                    pdf.file
                                )

                            val intent =
                                Intent(
                                    Intent.ACTION_SEND
                                ).apply {
                                    type =
                                        "application/pdf"

                                    putExtra(
                                        Intent.EXTRA_STREAM,
                                        uri
                                    )

                                    putExtra(
                                        Intent.EXTRA_SUBJECT,
                                        "Hisabi Khata Agency Report"
                                    )

                                    addFlags(
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    )
                                }

                            context.startActivity(
                                Intent.createChooser(
                                    intent,
                                    v15Text(
                                        "এজেন্সি রিপোর্ট শেয়ার করুন",
                                        "Share agency report"
                                    )
                                )
                            )
                        }.onFailure {
                            error =
                                it.message
                                    ?: v15Text(
                                        "রিপোর্ট তৈরি করা যায়নি",
                                        "Unable to create report"
                                    )
                        }

                        busy = false
                    }
                },
                modifier =
                    Modifier.fillMaxWidth(),
                enabled =
                    !busy
            ) {
                if (busy) {
                    CircularProgressIndicator()
                } else {
                    Text(
                        v15Text(
                            "PDF রিপোর্ট তৈরি / শেয়ার",
                            "Create / share PDF report"
                        )
                    )
                }
            }

            error?.let {
                Text(
                    it,
                    color =
                        MaterialTheme.colorScheme
                            .error,
                    style =
                        MaterialTheme.typography
                            .bodySmall
                )
            }
        }
    }
}
