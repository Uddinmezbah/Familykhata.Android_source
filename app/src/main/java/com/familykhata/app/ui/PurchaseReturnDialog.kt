package com.familykhata.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.familykhata.app.data.FinancialAccountSummary
import com.familykhata.app.data.PurchaseBillLineEntity

internal data class PurchaseReturnDraft(
    val quantity: Int,
    val refundFinancialAccountId: Long?,
    val note: String
)

@Composable
internal fun PurchaseReturnDialog(
    line: PurchaseBillLineEntity,
    maxQuantity: Int,
    financialAccounts: List<FinancialAccountSummary>,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (PurchaseReturnDraft) -> Unit
) {
    var quantityText by remember(line.id, maxQuantity) {
        mutableStateOf(maxQuantity.coerceAtLeast(1).toString())
    }

    var refundAccountId by remember(line.id) {
        mutableStateOf<Long?>(null)
    }

    var note by remember(line.id) {
        mutableStateOf("")
    }

    LaunchedEffect(line.id, financialAccounts) {
        refundAccountId = financialAccounts.singleOrNull()?.id
    }

    val quantity = quantityText.trim().toIntOrNull()
    val validQuantity =
        quantity != null &&
            quantity > 0 &&
            quantity <= maxQuantity

    AlertDialog(
        onDismissRequest = {
            if (!busy) onDismiss()
        },
        title = {
            Text(
                v15Text(
                    "ক্রয় ফেরত",
                    "Purchase return"
                )
            )
        },
        text = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    line.productNameSnapshot,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    v15Text(
                        "ফেরতযোগ্য: $maxQuantity ${line.unitSnapshot}",
                        "Returnable: $maxQuantity ${line.unitSnapshot}"
                    ),
                    style = MaterialTheme.typography.bodySmall
                )

                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    enabled = !busy,
                    label = {
                        Text(
                            v15Text(
                                "ফেরতের পরিমাণ",
                                "Return quantity"
                            )
                        )
                    },
                    singleLine = true,
                    isError =
                        quantityText.isNotBlank() &&
                            !validQuantity,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    v15Text(
                        "ফেরতটি আগে সাপ্লায়ারের বাকি কমাবে। অতিরিক্ত টাকা সাপ্লায়ার ফেরত দিলে যে Financial Account-এ টাকা আসবে সেটি নির্বাচন করুন।",
                        "The return first reduces supplier due. If the supplier refunds excess money, select the Financial Account that receives it."
                    ),
                    style = MaterialTheme.typography.bodySmall
                )

                financialAccounts.forEach { account ->
                    OutlinedButton(
                        enabled = !busy,
                        onClick = {
                            refundAccountId =
                                if (refundAccountId == account.id) {
                                    null
                                } else {
                                    account.id
                                }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            (
                                if (refundAccountId == account.id) "✓ " else ""
                            ) + account.name
                        )
                    }
                }

                if (financialAccounts.isEmpty()) {
                    Text(
                        v15Text(
                            "কোনো সক্রিয় Financial Account নেই। Supplier refund প্রয়োজন হলে আগে একটি Account তৈরি করুন।",
                            "No active Financial Account is available. Create one first if a supplier refund is required."
                        ),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    enabled = !busy,
                    label = {
                        Text(
                            v15Text(
                                "নোট (ঐচ্ছিক)",
                                "Note (optional)"
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !busy && validQuantity,
                onClick = {
                    onSubmit(
                        PurchaseReturnDraft(
                            quantity = requireNotNull(quantity),
                            refundFinancialAccountId = refundAccountId,
                            note = note.trim()
                        )
                    )
                }
            ) {
                Text(
                    if (busy) {
                        v15Text("সংরক্ষণ হচ্ছে…", "Saving…")
                    } else {
                        v15Text("ফেরত সংরক্ষণ", "Save return")
                    }
                )
            }
        },
        dismissButton = {
            TextButton(
                enabled = !busy,
                onClick = onDismiss
            ) {
                Text(v15Text("বাতিল", "Cancel"))
            }
        }
    )
}
