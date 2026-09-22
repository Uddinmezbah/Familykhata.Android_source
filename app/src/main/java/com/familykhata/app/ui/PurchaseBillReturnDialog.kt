package com.familykhata.app.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.familykhata.app.InventoryViewModel
import com.familykhata.app.data.FinancialAccountSummary
import com.familykhata.app.data.PurchaseBillLineEntity
import com.familykhata.app.data.PurchaseBillSummary

@Composable
internal fun PurchaseBillReturnDialog(
    viewModel: InventoryViewModel,
    bill: PurchaseBillSummary,
    financialAccounts: List<FinancialAccountSummary>,
    canWrite: Boolean,
    onDismiss: () -> Unit
) {
    val lines by
        viewModel.observePurchaseBillLines(bill.id)
            .collectAsState(initial = emptyList())

    val returns by
        viewModel.observePurchaseReturns(bill.id)
            .collectAsState(initial = emptyList())

    var selectedLine by remember(bill.id) {
        mutableStateOf<PurchaseBillLineEntity?>(null)
    }

    var selectedMaxQuantity by remember(bill.id) {
        mutableStateOf(0)
    }

    var saving by remember(bill.id) {
        mutableStateOf(false)
    }

    val context = LocalContext.current

    selectedLine?.let { line ->
        PurchaseReturnDialog(
            line = line,
            maxQuantity = selectedMaxQuantity,
            financialAccounts = financialAccounts,
            busy = saving,
            onDismiss = {
                if (!saving) {
                    selectedLine = null
                }
            },
            onSubmit = { draft ->
                saving = true

                viewModel.recordPurchaseReturn(
                    billId = bill.id,
                    purchaseLineId = line.id,
                    quantity = draft.quantity,
                    refundFinancialAccountId =
                        draft.refundFinancialAccountId,
                    note = draft.note
                ) { success ->
                    saving = false

                    if (success) {
                        selectedLine = null

                        Toast.makeText(
                            context,
                            v15Text(
                                "ক্রয় ফেরত সংরক্ষণ হয়েছে",
                                "Purchase return saved"
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            context,
                            v15Text(
                                "ফেরত সংরক্ষণ করা যায়নি। ফেরতযোগ্য স্টক, পরিমাণ ও Account যাচাই করুন।",
                                "Could not save return. Check returnable stock, quantity and account."
                            ),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        )

        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                v15Text(
                    "ক্রয় ফেরত • ${bill.purchaseNo}",
                    "Purchase return • ${bill.purchaseNo}"
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
                if (lines.isEmpty()) {
                    Text(
                        v15Text(
                            "এই ক্রয়ের কোনো পণ্য পাওয়া যায়নি।",
                            "No purchase items found."
                        )
                    )
                } else {
                    lines.forEach { line ->
                        val factor = line.unitFactor.coerceAtLeast(1)

                        val returnedBase =
                            returns
                                .filter {
                                    it.purchaseLineId == line.id
                                }
                                .sumOf {
                                    it.baseQuantity
                                }

                        val returnedQuantity =
                            returnedBase / factor

                        val maxQuantity =
                            (
                                line.quantity -
                                    returnedQuantity
                            ).coerceAtLeast(0)

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement =
                                Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                line.productNameSnapshot,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                v15Text(
                                    "ক্রয়: ${line.quantity} ${line.unitSnapshot} • ফেরত: $returnedQuantity • বাকি: $maxQuantity",
                                    "Purchased: ${line.quantity} ${line.unitSnapshot} • Returned: $returnedQuantity • Remaining: $maxQuantity"
                                ),
                                style = MaterialTheme.typography.bodySmall
                            )

                            if (canWrite && maxQuantity > 0) {
                                OutlinedButton(
                                    onClick = {
                                        selectedLine = line
                                        selectedMaxQuantity = maxQuantity
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        v15Text(
                                            "পণ্য ফেরত দিন",
                                            "Return Item"
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(v15Text("বন্ধ করুন", "Close"))
            }
        }
    )
}
