package com.familykhata.app.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.familykhata.app.FamilyKhataViewModel
import com.familykhata.app.data.TransactionEntity
import com.familykhata.app.report.KhataPdfPrintAdapter
import com.familykhata.app.report.KhataReportPdf
import com.familykhata.app.report.writeKhataReportPdf
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun KhataReportDialog(
    viewModel: FamilyKhataViewModel,
    workspace: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val transactions by
        viewModel.transactions
            .collectAsState()

    val folders by
        viewModel.khataFolders
            .collectAsState()

    val selectedId by
        viewModel.selectedKhataFolderId
            .collectAsState()

    val folder =
        folders.firstOrNull {
            it.id == selectedId
        }

    val folderName =
        folder?.name
            ?: v15Text(
                "বর্তমান খাতা",
                "Current khata"
            )

    val income =
        transactions
            .filter {
                it.type == "INCOME"
            }
            .sumOf {
                it.amount
            }

    val expense =
        transactions
            .filter {
                it.type == "EXPENSE"
            }
            .sumOf {
                it.amount
            }

    val balance =
        income - expense

    var busy by
        remember {
            mutableStateOf(false)
        }

    var error by
        remember {
            mutableStateOf<String?>(
                null
            )
        }

    var pendingSavePath by
        remember {
            mutableStateOf<String?>(
                null
            )
        }

    val safeFileName =
        remember(
            folderName
        ) {
            folderName
                .replace(
                    Regex(
                        """[\\/:*?"<>|]"""
                    ),
                    "-"
                )
                .take(
                    60
                )
                .ifBlank {
                    "khata-report"
                }
        }

    val save =
        rememberLauncherForActivityResult(
            ActivityResultContracts
                .CreateDocument(
                    "application/pdf"
                )
        ) { uri ->
            val path =
                pendingSavePath

            pendingSavePath =
                null

            if (
                uri != null &&
                path != null
            ) {
                scope.launch {
                    busy = true
                    error = null

                    try {
                        withContext(
                            Dispatchers.IO
                        ) {
                            File(
                                path
                            ).inputStream()
                                .use {
                                        input ->
                                    requireNotNull(
                                        context
                                            .contentResolver
                                            .openOutputStream(
                                                uri,
                                                "wt"
                                            )
                                    ).use {
                                            output ->
                                        input.copyTo(
                                            output
                                        )
                                    }
                                }
                        }

                        Toast.makeText(
                            context,
                            v15Text(
                                "PDF সেভ হয়েছে",
                                "PDF saved"
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (
                        cancelled:
                            CancellationException
                    ) {
                        throw cancelled
                    } catch (
                        _: Exception
                    ) {
                        error =
                            v15Text(
                                "PDF সেভ করা যায়নি",
                                "Unable to save PDF"
                            )
                    } finally {
                        busy = false
                    }
                }
            }
        }

    suspend fun createPdf():
        KhataReportPdf =
        withContext(
            Dispatchers.IO
        ) {
            writeKhataReportPdf(
                context =
                    context.applicationContext,
                khataName =
                    folderName,
                workspace =
                    workspace,
                transactions =
                    transactions,
                bangla =
                    V15LanguageState
                        .isBangla(),
                currency =
                    V14DisplayState
                        .currencySymbol
            )
        }

    fun runPdfAction(
        action:
            (
                KhataReportPdf
            ) -> Unit
    ) {
        if (busy) return

        busy = true
        error = null

        scope.launch {
            try {
                val pdf =
                    createPdf()

                action(
                    pdf
                )
            } catch (
                cancelled:
                    CancellationException
            ) {
                throw cancelled
            } catch (
                _: Exception
            ) {
                error =
                    v15Text(
                        "রিপোর্ট PDF তৈরি করা যায়নি",
                        "Unable to create report PDF"
                    )
            } finally {
                busy = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                usePlatformDefaultWidth =
                    false
            )
    ) {
        Surface(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        8.dp
                    ),
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
                        .padding(
                            16.dp
                        ),
                verticalArrangement =
                    Arrangement.spacedBy(
                        10.dp
                    )
            ) {
                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement
                            .SpaceBetween,
                    verticalAlignment =
                        Alignment
                            .CenterVertically
                ) {
                    Column(
                        modifier =
                            Modifier.weight(
                                1f
                            )
                    ) {
                        Text(
                            v15Text(
                                "খাতা রিপোর্ট",
                                "Khata Report"
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
                            folderName,
                            style =
                                MaterialTheme
                                    .typography
                                    .bodyMedium
                        )
                    }

                    OutlinedButton(
                        onClick =
                            onDismiss
                    ) {
                        Text(
                            v15Text(
                                "বন্ধ",
                                "Close"
                            )
                        )
                    }
                }

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            8.dp
                        )
                ) {
                    KhataMetric(
                        title =
                            v15Text(
                                "আয়",
                                "Income"
                            ),
                        amount =
                            income,
                        modifier =
                            Modifier.weight(
                                1f
                            )
                    )

                    KhataMetric(
                        title =
                            v15Text(
                                "খরচ",
                                "Expense"
                            ),
                        amount =
                            expense,
                        modifier =
                            Modifier.weight(
                                1f
                            )
                    )
                }

                KhataMetric(
                    title =
                        v15Text(
                            "ব্যালেন্স",
                            "Balance"
                        ),
                    amount =
                        balance,
                    modifier =
                        Modifier.fillMaxWidth(),
                    emphasized =
                        true
                )

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            8.dp
                        )
                ) {
                    Button(
                        enabled = !busy,
                        modifier =
                            Modifier.weight(
                                1f
                            ),
                        onClick = {
                            runPdfAction {
                                pdf ->
                                pendingSavePath =
                                    pdf.file
                                        .absolutePath
                                save.launch(
                                    "HisabiKhata-$safeFileName.pdf"
                                )
                            }
                        }
                    ) {
                        Text(
                            v15Text(
                                "PDF সেভ",
                                "Save PDF"
                            )
                        )
                    }

                    OutlinedButton(
                        enabled = !busy,
                        modifier =
                            Modifier.weight(
                                1f
                            ),
                        onClick = {
                            runPdfAction {
                                pdf ->
                                val manager =
                                    context
                                        .getSystemService(
                                            Context.PRINT_SERVICE
                                        ) as
                                        PrintManager

                                manager.print(
                                    "HisabiKhata-$safeFileName",
                                    KhataPdfPrintAdapter(
                                        file =
                                            pdf.file,
                                        documentName =
                                            "$safeFileName.pdf"
                                    ),
                                    PrintAttributes
                                        .Builder()
                                        .setMediaSize(
                                            PrintAttributes
                                                .MediaSize
                                                .ISO_A4
                                        )
                                        .build()
                                )
                            }
                        }
                    ) {
                        Text(
                            v15Text(
                                "প্রিন্ট",
                                "Print"
                            )
                        )
                    }
                }

                OutlinedButton(
                    enabled = !busy,
                    modifier =
                        Modifier.fillMaxWidth(),
                    onClick = {
                        runPdfAction {
                            pdf ->
                            try {
                                val uri =
                                    FileProvider
                                        .getUriForFile(
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

                                        clipData =
                                            ClipData
                                                .newRawUri(
                                                    "Khata report",
                                                    uri
                                                )

                                        addFlags(
                                            Intent
                                                .FLAG_GRANT_READ_URI_PERMISSION
                                        )
                                    }

                                context.startActivity(
                                    Intent
                                        .createChooser(
                                            intent,
                                            v15Text(
                                                "PDF শেয়ার করুন",
                                                "Share PDF"
                                            )
                                        )
                                )
                            } catch (
                                _: Exception
                            ) {
                                error =
                                    v15Text(
                                        "শেয়ার অপশন খোলা যায়নি",
                                        "Unable to open sharing"
                                    )
                            }
                        }
                    }
                ) {
                    Text(
                        v15Text(
                            "PDF শেয়ার",
                            "Share PDF"
                        )
                    )
                }

                if (busy) {
                    CircularProgressIndicator()
                }

                error?.let {
                    Text(
                        it,
                        color =
                            MaterialTheme
                                .colorScheme
                                .error
                    )
                }

                Text(
                    v15Text(
                        "লেনদেন: ${transactions.size} টি",
                        "Transactions: ${transactions.size}"
                    ),
                    fontWeight =
                        FontWeight.Bold
                )

                LazyColumn(
                    modifier =
                        Modifier.weight(
                            1f
                        ),
                    verticalArrangement =
                        Arrangement.spacedBy(
                            6.dp
                        )
                ) {
                    if (
                        transactions
                            .isEmpty()
                    ) {
                        item {
                            Text(
                                v15Text(
                                    "এই খাতায় এখনো কোনো আয়-খরচ নেই।",
                                    "There are no income/expense transactions in this khata yet."
                                )
                            )
                        }
                    }

                    items(
                        transactions,
                        key = {
                            it.id
                        }
                    ) {
                        item ->

                        KhataTransactionRow(
                            item
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KhataMetric(
    title: String,
    amount: Double,
    modifier: Modifier,
    emphasized: Boolean = false
) {
    Card(
        modifier =
            modifier,
        colors =
            CardDefaults
                .cardColors(
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
                                .surfaceVariant
                        }
                )
    ) {
        Column(
            modifier =
                Modifier.padding(
                    12.dp
                )
        ) {
            Text(
                title,
                style =
                    MaterialTheme
                        .typography
                        .labelMedium
            )

            Text(
                "${V14DisplayState.currencySymbol} ${money(amount)}",
                fontWeight =
                    FontWeight
                        .ExtraBold
            )
        }
    }
}

@Composable
private fun KhataTransactionRow(
    item: TransactionEntity
) {
    val date =
        remember(
            item.createdAt
        ) {
            SimpleDateFormat(
                "dd MMM yyyy",
                Locale.getDefault()
            ).format(
                Date(
                    item.createdAt
                )
            )
        }

    Card(
        modifier =
            Modifier.fillMaxWidth()
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        12.dp
                    ),
            horizontalArrangement =
                Arrangement.spacedBy(
                    10.dp
                ),
            verticalAlignment =
                Alignment
                    .CenterVertically
        ) {
            Column(
                modifier =
                    Modifier.weight(
                        1f
                    )
            ) {
                Text(
                    item.category,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    date +
                        if (
                            item.note
                                .isNotBlank()
                        ) {
                            " • ${item.note}"
                        } else {
                            ""
                        },
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )
            }

            Text(
                (
                    if (
                        item.type ==
                            "INCOME"
                    ) {
                        "+"
                    } else {
                        "−"
                    }
                    ) +
                    V14DisplayState
                        .currencySymbol +
                    " " +
                    money(
                        item.amount
                    ),
                fontWeight =
                    FontWeight
                        .ExtraBold
            )
        }
    }
}
