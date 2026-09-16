package com.familykhata.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.familykhata.app.FamilyKhataViewModel
import com.familykhata.app.data.DigitalServiceTransactionEntity
import com.familykhata.app.data.FinancialAccountSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun V16DigitalServicesDialog(
    viewModel: FamilyKhataViewModel,
    initialMode: String,
    canWrite: Boolean,
    onDismiss: () -> Unit
) {
    val accounts by
        viewModel.financialAccounts.collectAsState()

    val history by
        viewModel.digitalServiceTransactions
            .collectAsState()

    val activeAccounts =
        accounts.filter {
            it.isActive
        }

    val cashAccounts =
        activeAccounts.filter {
            it.type == "CASH"
        }

    val walletAccounts =
        activeAccounts.filter {
            it.type == "MOBILE_WALLET"
        }

    var mode by
        remember {
            mutableStateOf(initialMode)
        }

    var sourceAccountId by
        remember {
            mutableStateOf<Long?>(null)
        }

    var destinationAccountId by
        remember {
            mutableStateOf<Long?>(null)
        }

    var serviceAmountText by
        remember {
            mutableStateOf("")
        }

    var customerFeeText by
        remember {
            mutableStateOf("")
        }

    var providerChargeText by
        remember {
            mutableStateOf("")
        }

    var customerPaidText by
        remember {
            mutableStateOf("")
        }

    var providerCostText by
        remember {
            mutableStateOf("")
        }

    var note by
        remember {
            mutableStateOf("")
        }

    var message by
        remember {
            mutableStateOf("")
        }

    fun clearForm() {
        sourceAccountId = null
        destinationAccountId = null
        serviceAmountText = ""
        customerFeeText = ""
        providerChargeText = ""
        customerPaidText = ""
        providerCostText = ""
        note = ""
    }

    AlertDialog(
        onDismissRequest =
            onDismiss,
        title = {
            Text(
                v15Text(
                    "ডিজিটাল সার্ভিস",
                    "Digital Services"
                ),
                fontWeight =
                    FontWeight.ExtraBold
            )
        },
        text = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(
                            rememberScrollState()
                        ),
                verticalArrangement =
                    Arrangement.spacedBy(
                        12.dp
                    )
            ) {
                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            6.dp
                        )
                ) {
                    V16ServiceModeButton(
                        label =
                            v15Text(
                                "ক্যাশ আউট",
                                "Cash Out"
                            ),
                        selected =
                            mode ==
                                "AGENT_CASH_OUT",
                        modifier =
                            Modifier.weight(1f),
                        onClick = {
                            mode =
                                "AGENT_CASH_OUT"
                            clearForm()
                            message = ""
                        }
                    )

                    V16ServiceModeButton(
                        label =
                            v15Text(
                                "রিচার্জ",
                                "Recharge"
                            ),
                        selected =
                            mode ==
                                "MOBILE_RECHARGE",
                        modifier =
                            Modifier.weight(1f),
                        onClick = {
                            mode =
                                "MOBILE_RECHARGE"
                            clearForm()
                            message = ""
                        }
                    )

                    V16ServiceModeButton(
                        label =
                            v15Text(
                                "ইতিহাস",
                                "History"
                            ),
                        selected =
                            mode == "HISTORY",
                        modifier =
                            Modifier.weight(1f),
                        onClick = {
                            mode = "HISTORY"
                            message = ""
                        }
                    )
                }

                when (mode) {
                    "AGENT_CASH_OUT" ->
                        V16AgentCashOutForm(
                            cashAccounts =
                                cashAccounts,
                            walletAccounts =
                                walletAccounts,
                            sourceAccountId =
                                sourceAccountId,
                            destinationAccountId =
                                destinationAccountId,
                            serviceAmountText =
                                serviceAmountText,
                            customerFeeText =
                                customerFeeText,
                            providerChargeText =
                                providerChargeText,
                            note =
                                note,
                            canWrite =
                                canWrite,
                            message =
                                message,
                            onSourceSelected = {
                                sourceAccountId = it
                            },
                            onDestinationSelected = {
                                destinationAccountId =
                                    it
                            },
                            onServiceAmountChanged = {
                                serviceAmountText =
                                    v16MoneyInput(it)
                            },
                            onCustomerFeeChanged = {
                                customerFeeText =
                                    v16MoneyInput(it)
                            },
                            onProviderChargeChanged = {
                                providerChargeText =
                                    v16MoneyInput(it)
                            },
                            onNoteChanged = {
                                note = it
                            },
                            onSave = {
                                val source =
                                    sourceAccountId

                                val destination =
                                    destinationAccountId

                                val principal =
                                    serviceAmountText
                                        .toDoubleOrNull()

                                val fee =
                                    customerFeeText
                                        .toDoubleOrNull()
                                        ?: 0.0

                                val charge =
                                    providerChargeText
                                        .toDoubleOrNull()
                                        ?: 0.0

                                if (
                                    source == null ||
                                    destination == null ||
                                    principal == null ||
                                    principal <= 0.0
                                ) {
                                    message =
                                        v15Text(
                                            "Cash account, Wallet এবং টাকার পরিমাণ ঠিকভাবে দিন।",
                                            "Choose Cash, Wallet and enter a valid amount."
                                        )
                                } else {
                                    viewModel
                                        .recordAgentCashOut(
                                            cashAccountId =
                                                source,
                                            walletAccountId =
                                                destination,
                                            principalAmount =
                                                principal,
                                            customerFee =
                                                fee,
                                            providerCharge =
                                                charge,
                                            note =
                                                note
                                        ) {
                                                success ->
                                            if (success) {
                                                clearForm()
                                                message =
                                                    v15Text(
                                                        "Agent Cash Out সংরক্ষণ হয়েছে।",
                                                        "Agent Cash Out saved."
                                                    )
                                            } else {
                                                message =
                                                    v15Text(
                                                        "সংরক্ষণ করা যায়নি। Cash balance ও account নির্বাচন যাচাই করুন।",
                                                        "Could not save. Check the cash balance and selected accounts."
                                                    )
                                            }
                                        }
                                }
                            }
                        )

                    "MOBILE_RECHARGE" ->
                        V16MobileRechargeForm(
                            accounts =
                                activeAccounts,
                            sourceAccountId =
                                sourceAccountId,
                            destinationAccountId =
                                destinationAccountId,
                            faceValueText =
                                serviceAmountText,
                            customerPaidText =
                                customerPaidText,
                            providerCostText =
                                providerCostText,
                            note =
                                note,
                            canWrite =
                                canWrite,
                            message =
                                message,
                            onSourceSelected = {
                                sourceAccountId = it
                            },
                            onDestinationSelected = {
                                destinationAccountId =
                                    it
                            },
                            onFaceValueChanged = {
                                serviceAmountText =
                                    v16MoneyInput(it)
                            },
                            onCustomerPaidChanged = {
                                customerPaidText =
                                    v16MoneyInput(it)
                            },
                            onProviderCostChanged = {
                                providerCostText =
                                    v16MoneyInput(it)
                            },
                            onNoteChanged = {
                                note = it
                            },
                            onSave = {
                                val source =
                                    sourceAccountId

                                val destination =
                                    destinationAccountId

                                val faceValue =
                                    serviceAmountText
                                        .toDoubleOrNull()

                                val customerPaid =
                                    customerPaidText
                                        .toDoubleOrNull()

                                val providerCost =
                                    providerCostText
                                        .toDoubleOrNull()

                                if (
                                    source == null ||
                                    destination == null ||
                                    source ==
                                        destination ||
                                    faceValue == null ||
                                    customerPaid == null ||
                                    providerCost == null ||
                                    faceValue <= 0.0 ||
                                    customerPaid <= 0.0 ||
                                    providerCost <= 0.0
                                ) {
                                    message =
                                        v15Text(
                                            "Source, Payment account এবং সব টাকার পরিমাণ ঠিকভাবে দিন।",
                                            "Choose source/payment accounts and enter valid amounts."
                                        )
                                } else {
                                    viewModel
                                        .recordMobileRecharge(
                                            rechargeAccountId =
                                                source,
                                            receiveAccountId =
                                                destination,
                                            faceValue =
                                                faceValue,
                                            customerPaid =
                                                customerPaid,
                                            providerCost =
                                                providerCost,
                                            note =
                                                note
                                        ) {
                                                success ->
                                            if (success) {
                                                clearForm()
                                                message =
                                                    v15Text(
                                                        "Mobile Recharge সংরক্ষণ হয়েছে।",
                                                        "Mobile Recharge saved."
                                                    )
                                            } else {
                                                message =
                                                    v15Text(
                                                        "সংরক্ষণ করা যায়নি। Source balance ও account যাচাই করুন।",
                                                        "Could not save. Check the source balance and accounts."
                                                    )
                                            }
                                        }
                                }
                            }
                        )

                    else ->
                        V16DigitalServiceHistory(
                            rows =
                                history,
                            accounts =
                                activeAccounts
                        )
                }
            }
        },
        confirmButton = {
            TextButton(
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
    )
}

@Composable
private fun V16AgentCashOutForm(
    cashAccounts: List<FinancialAccountSummary>,
    walletAccounts: List<FinancialAccountSummary>,
    sourceAccountId: Long?,
    destinationAccountId: Long?,
    serviceAmountText: String,
    customerFeeText: String,
    providerChargeText: String,
    note: String,
    canWrite: Boolean,
    message: String,
    onSourceSelected: (Long) -> Unit,
    onDestinationSelected: (Long) -> Unit,
    onServiceAmountChanged: (String) -> Unit,
    onCustomerFeeChanged: (String) -> Unit,
    onProviderChargeChanged: (String) -> Unit,
    onNoteChanged: (String) -> Unit,
    onSave: () -> Unit
) {
    val principal =
        serviceAmountText.toDoubleOrNull()
            ?: 0.0

    val customerFee =
        customerFeeText.toDoubleOrNull()
            ?: 0.0

    val providerCharge =
        providerChargeText.toDoubleOrNull()
            ?: 0.0

    val profit =
        customerFee -
            providerCharge

    val walletIncrease =
        principal +
            profit

    Text(
        v15Text(
            "এজেন্ট ক্যাশ আউট",
            "Agent Cash Out"
        ),
        style =
            MaterialTheme.typography.titleMedium,
        fontWeight =
            FontWeight.Bold
    )

    Text(
        v15Text(
            "Customer-কে Cash দেওয়া হবে; আপনার agent wallet বাড়বে। শুধু Customer fee − Provider charge = লাভ/ক্ষতি।",
            "Cash is paid to the customer while your agent wallet increases. Only customer fee minus provider charge is profit/loss."
        ),
        style =
            MaterialTheme.typography.bodySmall
    )

    if (
        cashAccounts.isEmpty() ||
        walletAccounts.isEmpty()
    ) {
        Text(
            v15Text(
                "এই service-এর জন্য অন্তত ১টি Cash account এবং ১টি Mobile Wallet account লাগবে।",
                "This service needs at least one Cash account and one Mobile Wallet account."
            )
        )
    } else {
        Text(
            v15Text(
                "Cash account",
                "Cash account"
            ),
            fontWeight =
                FontWeight.Bold
        )

        cashAccounts.forEach {
                account ->
            V16ServiceAccountButton(
                account =
                    account,
                selected =
                    sourceAccountId ==
                        account.id,
                enabled =
                    true,
                onClick = {
                    onSourceSelected(
                        account.id
                    )
                }
            )
        }

        Text(
            v15Text(
                "Agent wallet",
                "Agent wallet"
            ),
            fontWeight =
                FontWeight.Bold
        )

        walletAccounts.forEach {
                account ->
            V16ServiceAccountButton(
                account =
                    account,
                selected =
                    destinationAccountId ==
                        account.id,
                enabled =
                    true,
                onClick = {
                    onDestinationSelected(
                        account.id
                    )
                }
            )
        }
    }

    OutlinedTextField(
        value =
            serviceAmountText,
        onValueChange =
            onServiceAmountChanged,
        modifier =
            Modifier.fillMaxWidth(),
        label = {
            Text(
                v15Text(
                    "Customer-কে Cash",
                    "Cash given to customer"
                )
            )
        },
        singleLine = true
    )

    OutlinedTextField(
        value =
            customerFeeText,
        onValueChange =
            onCustomerFeeChanged,
        modifier =
            Modifier.fillMaxWidth(),
        label = {
            Text(
                v15Text(
                    "Customer fee",
                    "Customer fee"
                )
            )
        },
        singleLine = true
    )

    OutlinedTextField(
        value =
            providerChargeText,
        onValueChange =
            onProviderChargeChanged,
        modifier =
            Modifier.fillMaxWidth(),
        label = {
            Text(
                v15Text(
                    "Provider charge",
                    "Provider charge"
                )
            )
        },
        singleLine = true
    )

    V16ServicePreview(
        firstLabel =
            v15Text(
                "Cash কমবে",
                "Cash decreases"
            ),
        firstAmount =
            principal,
        secondLabel =
            v15Text(
                "Wallet বাড়বে",
                "Wallet increases"
            ),
        secondAmount =
            walletIncrease,
        profit =
            profit
    )

    OutlinedTextField(
        value =
            note,
        onValueChange =
            onNoteChanged,
        modifier =
            Modifier.fillMaxWidth(),
        label = {
            Text(
                v15Text(
                    "নোট (ঐচ্ছিক)",
                    "Note (optional)"
                )
            )
        }
    )

    if (message.isNotBlank()) {
        Text(
            message,
            style =
                MaterialTheme.typography.bodySmall
        )
    }

    Button(
        onClick =
            onSave,
        modifier =
            Modifier.fillMaxWidth(),
        enabled =
            canWrite &&
                cashAccounts.isNotEmpty() &&
                walletAccounts.isNotEmpty()
    ) {
        Text(
            v15Text(
                "ক্যাশ আউট সংরক্ষণ",
                "Save Cash Out"
            )
        )
    }
}

@Composable
private fun V16MobileRechargeForm(
    accounts: List<FinancialAccountSummary>,
    sourceAccountId: Long?,
    destinationAccountId: Long?,
    faceValueText: String,
    customerPaidText: String,
    providerCostText: String,
    note: String,
    canWrite: Boolean,
    message: String,
    onSourceSelected: (Long) -> Unit,
    onDestinationSelected: (Long) -> Unit,
    onFaceValueChanged: (String) -> Unit,
    onCustomerPaidChanged: (String) -> Unit,
    onProviderCostChanged: (String) -> Unit,
    onNoteChanged: (String) -> Unit,
    onSave: () -> Unit
) {
    val faceValue =
        faceValueText.toDoubleOrNull()
            ?: 0.0

    val customerPaid =
        customerPaidText.toDoubleOrNull()
            ?: 0.0

    val providerCost =
        providerCostText.toDoubleOrNull()
            ?: 0.0

    val profit =
        customerPaid -
            providerCost

    Text(
        v15Text(
            "মোবাইল রিচার্জ",
            "Mobile Recharge"
        ),
        style =
            MaterialTheme.typography.titleMedium,
        fontWeight =
            FontWeight.Bold
    )

    Text(
        v15Text(
            "Recharge/float account থেকে actual cost কমবে; Customer যে account-এ টাকা দিয়েছে সেটি বাড়বে। পার্থক্যটাই লাভ/ক্ষতি।",
            "Actual cost leaves the recharge/float account and customer payment enters the receiving account. The difference is profit/loss."
        ),
        style =
            MaterialTheme.typography.bodySmall
    )

    if (accounts.size < 2) {
        Text(
            v15Text(
                "Recharge হিসাবের জন্য কমপক্ষে ২টি account লাগবে।",
                "At least two accounts are required for recharge accounting."
            )
        )
    } else {
        Text(
            v15Text(
                "Recharge / Float source",
                "Recharge / Float source"
            ),
            fontWeight =
                FontWeight.Bold
        )

        accounts.forEach {
                account ->
            V16ServiceAccountButton(
                account =
                    account,
                selected =
                    sourceAccountId ==
                        account.id,
                enabled =
                    destinationAccountId !=
                        account.id,
                onClick = {
                    onSourceSelected(
                        account.id
                    )
                }
            )
        }

        Text(
            v15Text(
                "Customer payment account",
                "Customer payment account"
            ),
            fontWeight =
                FontWeight.Bold
        )

        accounts.forEach {
                account ->
            V16ServiceAccountButton(
                account =
                    account,
                selected =
                    destinationAccountId ==
                        account.id,
                enabled =
                    sourceAccountId !=
                        account.id,
                onClick = {
                    onDestinationSelected(
                        account.id
                    )
                }
            )
        }
    }

    OutlinedTextField(
        value =
            faceValueText,
        onValueChange =
            onFaceValueChanged,
        modifier =
            Modifier.fillMaxWidth(),
        label = {
            Text(
                v15Text(
                    "Recharge face value",
                    "Recharge face value"
                )
            )
        },
        singleLine = true
    )

    OutlinedTextField(
        value =
            customerPaidText,
        onValueChange =
            onCustomerPaidChanged,
        modifier =
            Modifier.fillMaxWidth(),
        label = {
            Text(
                v15Text(
                    "Customer দিয়েছে",
                    "Customer paid"
                )
            )
        },
        singleLine = true
    )

    OutlinedTextField(
        value =
            providerCostText,
        onValueChange =
            onProviderCostChanged,
        modifier =
            Modifier.fillMaxWidth(),
        label = {
            Text(
                v15Text(
                    "আপনার actual cost",
                    "Your actual cost"
                )
            )
        },
        singleLine = true
    )

    V16ServicePreview(
        firstLabel =
            v15Text(
                "Source কমবে",
                "Source decreases"
            ),
        firstAmount =
            providerCost,
        secondLabel =
            v15Text(
                "Payment account বাড়বে",
                "Payment account increases"
            ),
        secondAmount =
            customerPaid,
        profit =
            profit
    )

    if (faceValue > 0.0) {
        Text(
            v15Text(
                "Recharge value: ${V14DisplayState.currencySymbol} ${money(faceValue)}",
                "Recharge value: ${V14DisplayState.currencySymbol} ${money(faceValue)}"
            ),
            style =
                MaterialTheme.typography.labelSmall
        )
    }

    OutlinedTextField(
        value =
            note,
        onValueChange =
            onNoteChanged,
        modifier =
            Modifier.fillMaxWidth(),
        label = {
            Text(
                v15Text(
                    "নোট (ঐচ্ছিক)",
                    "Note (optional)"
                )
            )
        }
    )

    if (message.isNotBlank()) {
        Text(
            message,
            style =
                MaterialTheme.typography.bodySmall
        )
    }

    Button(
        onClick =
            onSave,
        modifier =
            Modifier.fillMaxWidth(),
        enabled =
            canWrite &&
                accounts.size >= 2
    ) {
        Text(
            v15Text(
                "রিচার্জ সংরক্ষণ",
                "Save Recharge"
            )
        )
    }
}

@Composable
private fun V16ServicePreview(
    firstLabel: String,
    firstAmount: Double,
    secondLabel: String,
    secondAmount: Double,
    profit: Double
) {
    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surfaceVariant
            )
    ) {
        Column(
            modifier =
                Modifier.padding(12.dp),
            verticalArrangement =
                Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "$firstLabel: ${V14DisplayState.currencySymbol} ${money(firstAmount)}"
            )

            Text(
                "$secondLabel: ${V14DisplayState.currencySymbol} ${money(secondAmount)}"
            )

            Text(
                v15Text(
                    "লাভ/ক্ষতি: ${V14DisplayState.currencySymbol} ${money(profit)}",
                    "Profit/Loss: ${V14DisplayState.currencySymbol} ${money(profit)}"
                ),
                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}

@Composable
private fun V16ServiceAccountButton(
    account: FinancialAccountSummary,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    if (selected) {
        Button(
            onClick =
                onClick,
            enabled =
                enabled,
            modifier =
                Modifier.fillMaxWidth()
        ) {
            Text(
                "${account.name} • ${V14DisplayState.currencySymbol} ${money(account.balance)}"
            )
        }
    } else {
        OutlinedButton(
            onClick =
                onClick,
            enabled =
                enabled,
            modifier =
                Modifier.fillMaxWidth()
        ) {
            Text(
                "${account.name} • ${V14DisplayState.currencySymbol} ${money(account.balance)}"
            )
        }
    }
}

@Composable
private fun V16ServiceModeButton(
    label: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    if (selected) {
        Button(
            onClick =
                onClick,
            modifier =
                modifier
        ) {
            Text(label)
        }
    } else {
        OutlinedButton(
            onClick =
                onClick,
            modifier =
                modifier
        ) {
            Text(label)
        }
    }
}

@Composable
private fun V16DigitalServiceHistory(
    rows:
        List<DigitalServiceTransactionEntity>,
    accounts:
        List<FinancialAccountSummary>
) {
    val accountNames =
        accounts.associate {
            it.id to it.name
        }

    Text(
        v15Text(
            "সার্ভিস ইতিহাস",
            "Service history"
        ),
        style =
            MaterialTheme.typography.titleMedium,
        fontWeight =
            FontWeight.Bold
    )

    if (rows.isEmpty()) {
        Text(
            v15Text(
                "এখনও কোনো Agent Cash Out বা Mobile Recharge যোগ করা হয়নি।",
                "No Agent Cash Out or Mobile Recharge has been recorded yet."
            )
        )
        return
    }

    rows.take(30).forEach {
            item ->

        Card(
            modifier =
                Modifier.fillMaxWidth(),
            shape =
                RoundedCornerShape(16.dp),
            border =
                BorderStroke(
                    1.dp,
                    MaterialTheme
                        .colorScheme
                        .outlineVariant
                )
        ) {
            Column(
                modifier =
                    Modifier.padding(12.dp),
                verticalArrangement =
                    Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    if (
                        item.serviceType ==
                            "AGENT_CASH_OUT"
                    ) {
                        v15Text(
                            "এজেন্ট ক্যাশ আউট",
                            "Agent Cash Out"
                        )
                    } else {
                        v15Text(
                            "মোবাইল রিচার্জ",
                            "Mobile Recharge"
                        )
                    },
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    "${V14DisplayState.currencySymbol} ${money(item.serviceAmount)} • " +
                        v15Text(
                            "লাভ/ক্ষতি",
                            "Profit/Loss"
                        ) +
                        " ${V14DisplayState.currencySymbol} ${money(item.profit)}"
                )

                Text(
                    "${accountNames[item.sourceAccountId] ?: "#${item.sourceAccountId}"} → " +
                        "${accountNames[item.destinationAccountId] ?: "#${item.destinationAccountId}"}",
                    style =
                        MaterialTheme.typography.bodySmall
                )

                Text(
                    v16ServiceDate(
                        item.createdAt
                    ),
                    style =
                        MaterialTheme.typography.labelSmall
                )

                if (item.note.isNotBlank()) {
                    Text(
                        item.note,
                        style =
                            MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

private fun v16MoneyInput(
    value: String
): String {
    var dotSeen = false

    return value.filter {
        char ->
        when {
            char.isDigit() ->
                true

            char == '.' &&
                !dotSeen -> {
                dotSeen = true
                true
            }

            else ->
                false
        }
    }
}

private fun v16ServiceDate(
    timestamp: Long
): String =
    SimpleDateFormat(
        "dd MMM yyyy, hh:mm a",
        Locale.getDefault()
    ).format(
        Date(timestamp)
    )
