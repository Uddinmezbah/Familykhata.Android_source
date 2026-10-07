package com.familykhata.app.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.familykhata.app.data.BusinessProfileEntity
import com.familykhata.app.data.TransactionEntity
import com.familykhata.app.report.writeBusinessVoucherPdf
import kotlinx.coroutines.launch
import java.text.DecimalFormat

@Composable
internal fun BusinessVoucherDialog(
    transaction: TransactionEntity,
    businessProfile: BusinessProfileEntity?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var clientName by remember(transaction.id) { mutableStateOf("") }
    var clientPhone by remember(transaction.id) { mutableStateOf("") }
    var details by remember(transaction.id, transaction.note) { mutableStateOf(transaction.note) }
    var isCreating by remember(transaction.id) { mutableStateOf(false) }
    var error by remember(transaction.id) { mutableStateOf<String?>(null) }
    val amountText = remember(transaction.amount) { DecimalFormat("#,##0.##").format(transaction.amount) }

    AlertDialog(
        onDismissRequest = {
            if (!isCreating) onDismiss()
        },
        title = {
            Text(v15Text("ক্লায়েন্ট ভাউচার", "Client voucher"))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    v15Text(
                        "এই আয় এন্ট্রি থেকে PDF ভাউচার তৈরি করে WhatsApp, Messenger, Email বা অন্য অ্যাপে পাঠাতে পারবেন।",
                        "Create a PDF voucher from this income entry and share it by WhatsApp, Messenger, email or another app."
                    )
                )

                OutlinedTextField(
                    value = clientName,
                    onValueChange = {
                        clientName = it
                        error = null
                    },
                    label = { Text(v15Text("ক্লায়েন্টের নাম", "Client name")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = clientPhone,
                    onValueChange = { clientPhone = it },
                    label = { Text(v15Text("ফোন (ঐচ্ছিক)", "Phone (optional)")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text(v15Text("কাজের বিস্তারিত", "Work details")) },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    v15Text(
                        "সার্ভিস/খাত: ${transaction.category}",
                        "Service/category: ${transaction.category}"
                    )
                )

                Text("${V14DisplayState.currencySymbol} $amountText")

                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = clientName.trim()
                    if (cleanName.isBlank()) {
                        error = v15Text("ক্লায়েন্টের নাম লিখুন", "Enter the client name")
                        return@Button
                    }

                    isCreating = true
                    error = null

                    scope.launch {
                        runCatching {
                            val pdf = writeBusinessVoucherPdf(
                                context = context,
                                transaction = transaction,
                                business = businessProfile,
                                clientName = cleanName,
                                clientPhone = clientPhone.trim(),
                                workDetails = details.trim(),
                                currencySymbol = V14DisplayState.currencySymbol
                            )

                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.statements",
                                pdf.file
                            )

                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                putExtra(Intent.EXTRA_SUBJECT, "Hisabi Khata Voucher")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }

                            context.startActivity(
                                Intent.createChooser(
                                    shareIntent,
                                    v15Text("ভাউচার PDF শেয়ার করুন", "Share voucher PDF")
                                )
                            )
                        }.onFailure {
                            error = it.message ?: v15Text(
                                "ভাউচার তৈরি করা যায়নি",
                                "Unable to create voucher"
                            )
                            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                        }

                        isCreating = false
                    }
                },
                enabled = !isCreating
            ) {
                if (isCreating) {
                    CircularProgressIndicator()
                } else {
                    Text(v15Text("PDF শেয়ার", "Share PDF"))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isCreating) {
                Text(v15Text("বাতিল", "Cancel"))
            }
        }
    )
}
