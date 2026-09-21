package com.familykhata.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import com.familykhata.app.data.RetailSaleLineEntity

internal data class RetailSaleReturnDraft(
    val quantity: Int,
    val returnType: String,
    val refundFinancialAccountId: Long?,
    val note: String
)

@Composable
internal fun RetailSaleReturnDialog(
    line: RetailSaleLineEntity,
    maxQuantity: Int,
    financialAccounts: List<FinancialAccountSummary>,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (RetailSaleReturnDraft) -> Unit
) {
    var quantityText by remember(
        line.id,
        maxQuantity
    ) {
        mutableStateOf(
            maxQuantity
                .coerceAtLeast(1)
                .toString()
        )
    }

    var returnType by remember(
        line.id
    ) {
        mutableStateOf("RESTOCK")
    }

    var refundAccountId by remember(
        line.id
    ) {
        mutableStateOf<Long?>(null)
    }

    var note by remember(
        line.id
    ) {
        mutableStateOf("")
    }

    LaunchedEffect(
        line.id,
        financialAccounts
    ) {
        refundAccountId =
            financialAccounts
                .singleOrNull()
                ?.id
    }

    val quantity =
        quantityText
            .trim()
            .toIntOrNull()

    val validQuantity =
        quantity != null &&
            quantity > 0 &&
            quantity <= maxQuantity

    AlertDialog(
        onDismissRequest = {
            if (!busy) {
                onDismiss()
            }
        },
        title = {
            Text(
                v15Text(
                    "পণ্য ফেরত",
                    "Sales return"
                )
            )
        },
        text = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(
                            max = 520.dp
                        )
                        .verticalScroll(
                            rememberScrollState()
                        ),
                verticalArrangement =
                    Arrangement.spacedBy(
                        10.dp
                    )
            ) {
                Text(
                    line.productNameSnapshot,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    v15Text(
                        "ফেরতযোগ্য: $maxQuantity ${line.unitSnapshot}",
                        "Returnable: $maxQuantity ${line.unitSnapshot}"
                    ),
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )

                OutlinedTextField(
                    value = quantityText,
                    onValueChange = {
                        quantityText = it
                    },
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
                        quantityText
                            .isNotBlank() &&
                            !validQuantity,
                    modifier =
                        Modifier.fillMaxWidth()
                )

                Text(
                    v15Text(
                        "পণ্যের অবস্থা",
                        "Return type"
                    ),
                    fontWeight =
                        FontWeight.SemiBold
                )

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            8.dp
                        )
                ) {
                    OutlinedButton(
                        enabled = !busy,
                        onClick = {
                            returnType =
                                "RESTOCK"
                        },
                        modifier =
                            Modifier.weight(1f)
                    ) {
                        Text(
                            if (
                                returnType ==
                                "RESTOCK"
                            ) {
                                "✓ " +
                                    v15Text(
                                        "স্টকে ফেরত",
                                        "Restock"
                                    )
                            } else {
                                v15Text(
                                    "স্টকে ফেরত",
                                    "Restock"
                                )
                            }
                        )
                    }

                    OutlinedButton(
                        enabled = !busy,
                        onClick = {
                            returnType =
                                "DAMAGED"
                        },
                        modifier =
                            Modifier.weight(1f)
                    ) {
                        Text(
                            if (
                                returnType ==
                                "DAMAGED"
                            ) {
                                "✓ " +
                                    v15Text(
                                        "নষ্ট/ক্ষতিগ্রস্ত",
                                        "Damaged"
                                    )
                            } else {
                                v15Text(
                                    "নষ্ট/ক্ষতিগ্রস্ত",
                                    "Damaged"
                                )
                            }
                        )
                    }
                }

                Text(
                    v15Text(
                        "যদি ক্রেতাকে টাকা ফেরত দিতে হয়, সেই হিসাব নির্বাচন করুন। শুধু বাকি কমলে এই হিসাব ব্যবহার হবে না।",
                        "Select the account to use if a cash/bank/wallet refund is required. It is not used when the return only reduces customer due."
                    ),
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )

                if (
                    financialAccounts.isEmpty()
                ) {
                    Text(
                        v15Text(
                            "কোনো সক্রিয় Financial Account নেই। টাকা ফেরত দরকার হলে আগে একটি হিসাব তৈরি করুন।",
                            "No active Financial Account is available. Create one first if a refund is required."
                        ),
                        color =
                            MaterialTheme
                                .colorScheme
                                .error,
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall
                    )
                } else {
                    financialAccounts
                        .forEach { account ->
                            OutlinedButton(
                                enabled = !busy,
                                onClick = {
                                    refundAccountId =
                                        if (
                                            refundAccountId ==
                                            account.id
                                        ) {
                                            null
                                        } else {
                                            account.id
                                        }
                                },
                                modifier =
                                    Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    (
                                        if (
                                            refundAccountId ==
                                            account.id
                                        ) {
                                            "✓ "
                                        } else {
                                            ""
                                        }
                                    ) +
                                        account.name
                                )
                            }
                        }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = {
                        note = it
                    },
                    enabled = !busy,
                    label = {
                        Text(
                            v15Text(
                                "নোট (ঐচ্ছিক)",
                                "Note (optional)"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled =
                    !busy &&
                        validQuantity,
                onClick = {
                    onSubmit(
                        RetailSaleReturnDraft(
                            quantity =
                                requireNotNull(
                                    quantity
                                ),
                            returnType =
                                returnType,
                            refundFinancialAccountId =
                                refundAccountId,
                            note =
                                note.trim()
                        )
                    )
                }
            ) {
                Text(
                    if (busy) {
                        v15Text(
                            "সংরক্ষণ হচ্ছে…",
                            "Saving…"
                        )
                    } else {
                        v15Text(
                            "ফেরত সংরক্ষণ",
                            "Save return"
                        )
                    }
                )
            }
        },
        dismissButton = {
            TextButton(
                enabled = !busy,
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
