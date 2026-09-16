package com.familykhata.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.familykhata.app.data.FinancialAccountSummary

private data class V16AccountKind(
    val key: String,
    val bangla: String,
    val english: String,
    val type: String,
    val provider: String
)

private val v16AccountKinds =
    listOf(
        V16AccountKind(
            key = "CASH",
            bangla = "ক্যাশ",
            english = "Cash",
            type = "CASH",
            provider = ""
        ),
        V16AccountKind(
            key = "BKASH",
            bangla = "বিকাশ",
            english = "bKash",
            type = "MOBILE_WALLET",
            provider = "bKash"
        ),
        V16AccountKind(
            key = "NAGAD",
            bangla = "নগদ",
            english = "Nagad",
            type = "MOBILE_WALLET",
            provider = "Nagad"
        ),
        V16AccountKind(
            key = "ROCKET",
            bangla = "রকেট",
            english = "Rocket",
            type = "MOBILE_WALLET",
            provider = "Rocket"
        ),
        V16AccountKind(
            key = "BANK",
            bangla = "ব্যাংক",
            english = "Bank",
            type = "BANK",
            provider = ""
        ),
        V16AccountKind(
            key = "CARD",
            bangla = "কার্ড",
            english = "Card",
            type = "CARD",
            provider = ""
        ),
        V16AccountKind(
            key = "OTHER",
            bangla = "অন্যান্য",
            english = "Other",
            type = "OTHER",
            provider = ""
        )
    )

@Composable
fun V16FinancialAccountsDialog(
    viewModel: FamilyKhataViewModel,
    canWrite: Boolean,
    onDismiss: () -> Unit
) {
    val accounts by
        viewModel.financialAccounts
            .collectAsState()

    val activeAccounts =
        accounts.filter {
            it.isActive
        }

    var mode by
        remember {
            mutableStateOf("LIST")
        }

    var selectedKindKey by
        remember {
            mutableStateOf("CASH")
        }

    var accountName by
        remember {
            mutableStateOf("")
        }

    var openingBalanceText by
        remember {
            mutableStateOf("")
        }

    var fromAccountId by
        remember {
            mutableStateOf<Long?>(
                null
            )
        }

    var toAccountId by
        remember {
            mutableStateOf<Long?>(
                null
            )
        }

    var transferAmountText by
        remember {
            mutableStateOf("")
        }

    var transferNote by
        remember {
            mutableStateOf("")
        }

    var message by
        remember {
            mutableStateOf("")
        }

    val selectedKind =
        v16AccountKinds
            .first {
                it.key ==
                    selectedKindKey
            }

    AlertDialog(
        onDismissRequest =
            onDismiss,
        title = {
            Text(
                v15Text(
                    "অ্যাকাউন্ট ও ট্রান্সফার",
                    "Accounts & Transfer"
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
                when (mode) {
                    "ADD" -> {
                        Text(
                            v15Text(
                                "নতুন অ্যাকাউন্ট",
                                "New account"
                            ),
                            style =
                                MaterialTheme
                                    .typography
                                    .titleMedium,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            v15Text(
                                "অ্যাকাউন্টের ধরন",
                                "Account type"
                            ),
                            fontWeight =
                                FontWeight.Bold
                        )

                        v16AccountKinds
                            .chunked(2)
                            .forEach {
                                    rowKinds ->
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth(),
                                    horizontalArrangement =
                                        Arrangement
                                            .spacedBy(
                                                8.dp
                                            )
                                ) {
                                    rowKinds
                                        .forEach {
                                                kind ->
                                            val selected =
                                                selectedKindKey ==
                                                    kind.key

                                            if (selected) {
                                                Button(
                                                    onClick = {},
                                                    modifier =
                                                        Modifier
                                                            .weight(
                                                                1f
                                                            )
                                                ) {
                                                    Text(
                                                        v15Text(
                                                            kind.bangla,
                                                            kind.english
                                                        )
                                                    )
                                                }
                                            } else {
                                                OutlinedButton(
                                                    onClick = {
                                                        selectedKindKey =
                                                            kind.key

                                                        if (
                                                            accountName
                                                                .isBlank()
                                                        ) {
                                                            accountName =
                                                                kind.english
                                                        }
                                                    },
                                                    modifier =
                                                        Modifier
                                                            .weight(
                                                                1f
                                                            )
                                                ) {
                                                    Text(
                                                        v15Text(
                                                            kind.bangla,
                                                            kind.english
                                                        )
                                                    )
                                                }
                                            }
                                        }

                                    if (
                                        rowKinds.size ==
                                        1
                                    ) {
                                        Spacer(
                                            modifier =
                                                Modifier
                                                    .weight(
                                                        1f
                                                    )
                                        )
                                    }
                                }
                            }

                        OutlinedTextField(
                            value =
                                accountName,
                            onValueChange = {
                                accountName = it
                            },
                            modifier =
                                Modifier
                                    .fillMaxWidth(),
                            label = {
                                Text(
                                    v15Text(
                                        "অ্যাকাউন্টের নাম",
                                        "Account name"
                                    )
                                )
                            },
                            placeholder = {
                                Text(
                                    v15Text(
                                        "যেমন: দোকানের ক্যাশ / Personal bKash",
                                        "e.g. Shop Cash / Personal bKash"
                                    )
                                )
                            },
                            singleLine = true
                        )

                        OutlinedTextField(
                            value =
                                openingBalanceText,
                            onValueChange = {
                                openingBalanceText =
                                    it.filter {
                                            char ->
                                            char.isDigit() ||
                                                char ==
                                                    '.'
                                        }
                            },
                            modifier =
                                Modifier
                                    .fillMaxWidth(),
                            label = {
                                Text(
                                    v15Text(
                                        "শুরুর ব্যালেন্স",
                                        "Opening balance"
                                    )
                                )
                            },
                            supportingText = {
                                Text(
                                    v15Text(
                                        "এই মুহূর্তে অ্যাকাউন্টে যত টাকা আছে",
                                        "Current amount already held in this account"
                                    )
                                )
                            },
                            singleLine = true
                        )

                        if (
                            message.isNotBlank()
                        ) {
                            Text(
                                message,
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodySmall
                            )
                        }

                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement
                                    .spacedBy(
                                        8.dp
                                    )
                        ) {
                            OutlinedButton(
                                onClick = {
                                    mode =
                                        "LIST"
                                    message =
                                        ""
                                },
                                modifier =
                                    Modifier
                                        .weight(
                                            1f
                                        )
                            ) {
                                Text(
                                    v15Text(
                                        "ফিরে যান",
                                        "Back"
                                    )
                                )
                            }

                            Button(
                                onClick = {
                                    val amount =
                                        openingBalanceText
                                            .toDoubleOrNull()
                                            ?: 0.0

                                    val name =
                                        accountName
                                            .trim()
                                            .ifBlank {
                                                selectedKind
                                                    .english
                                            }

                                    viewModel
                                        .addFinancialAccount(
                                            name =
                                                name,
                                            type =
                                                selectedKind
                                                    .type,
                                            provider =
                                                selectedKind
                                                    .provider,
                                            openingBalance =
                                                amount
                                        ) {
                                                success ->
                                            if (
                                                success
                                            ) {
                                                accountName =
                                                    ""
                                                openingBalanceText =
                                                    ""
                                                selectedKindKey =
                                                    "CASH"
                                                message =
                                                    ""
                                                mode =
                                                    "LIST"
                                            } else {
                                                message =
                                                    v15Text(
                                                        "অ্যাকাউন্ট সংরক্ষণ করা যায়নি। তথ্য যাচাই করুন।",
                                                        "Could not save the account. Check the information."
                                                    )
                                            }
                                        }
                                },
                                enabled =
                                    canWrite,
                                modifier =
                                    Modifier
                                        .weight(
                                            1f
                                        )
                            ) {
                                Text(
                                    v15Text(
                                        "সংরক্ষণ",
                                        "Save"
                                    )
                                )
                            }
                        }
                    }

                    "TRANSFER" -> {
                        Text(
                            v15Text(
                                "নিজের অ্যাকাউন্টে টাকা ট্রান্সফার",
                                "Transfer between your accounts"
                            ),
                            style =
                                MaterialTheme
                                    .typography
                                    .titleMedium,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            v15Text(
                                "এটি আয় বা খরচ নয়। একটি অ্যাকাউন্ট কমবে, অন্যটি সমপরিমাণ বাড়বে।",
                                "This is not income or expense. One account decreases and the other increases by the same amount."
                            ),
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall
                        )

                        if (
                            activeAccounts.size <
                            2
                        ) {
                            Text(
                                v15Text(
                                    "Transfer করতে কমপক্ষে ২টি সক্রিয় অ্যাকাউন্ট লাগবে।",
                                    "At least two active accounts are required for a transfer."
                                )
                            )
                        } else {
                            Text(
                                v15Text(
                                    "যে অ্যাকাউন্ট থেকে টাকা যাবে",
                                    "From account"
                                ),
                                fontWeight =
                                    FontWeight.Bold
                            )

                            activeAccounts
                                .forEach {
                                        account ->
                                    V16AccountSelectButton(
                                        account =
                                            account,
                                        selected =
                                            fromAccountId ==
                                                account.id,
                                        enabled =
                                            toAccountId !=
                                                account.id,
                                        onClick = {
                                            fromAccountId =
                                                account.id
                                        }
                                    )
                                }

                            Text(
                                v15Text(
                                    "যে অ্যাকাউন্টে টাকা যাবে",
                                    "To account"
                                ),
                                fontWeight =
                                    FontWeight.Bold
                            )

                            activeAccounts
                                .forEach {
                                        account ->
                                    V16AccountSelectButton(
                                        account =
                                            account,
                                        selected =
                                            toAccountId ==
                                                account.id,
                                        enabled =
                                            fromAccountId !=
                                                account.id,
                                        onClick = {
                                            toAccountId =
                                                account.id
                                        }
                                    )
                                }

                            OutlinedTextField(
                                value =
                                    transferAmountText,
                                onValueChange = {
                                    transferAmountText =
                                        it.filter {
                                            char ->
                                            char.isDigit() ||
                                                char ==
                                                    '.'
                                        }
                                },
                                modifier =
                                    Modifier
                                        .fillMaxWidth(),
                                label = {
                                    Text(
                                        v15Text(
                                            "টাকার পরিমাণ",
                                            "Amount"
                                        )
                                    )
                                },
                                singleLine = true
                            )

                            OutlinedTextField(
                                value =
                                    transferNote,
                                onValueChange = {
                                    transferNote = it
                                },
                                modifier =
                                    Modifier
                                        .fillMaxWidth(),
                                label = {
                                    Text(
                                        v15Text(
                                            "নোট (ঐচ্ছিক)",
                                            "Note (optional)"
                                        )
                                    )
                                }
                            )
                        }

                        if (
                            message.isNotBlank()
                        ) {
                            Text(
                                message,
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodySmall
                            )
                        }

                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement
                                    .spacedBy(
                                        8.dp
                                    )
                        ) {
                            OutlinedButton(
                                onClick = {
                                    mode =
                                        "LIST"
                                    message =
                                        ""
                                },
                                modifier =
                                    Modifier
                                        .weight(
                                            1f
                                        )
                            ) {
                                Text(
                                    v15Text(
                                        "ফিরে যান",
                                        "Back"
                                    )
                                )
                            }

                            Button(
                                onClick = {
                                    val from =
                                        fromAccountId

                                    val to =
                                        toAccountId

                                    val amount =
                                        transferAmountText
                                            .toDoubleOrNull()
                                            ?: 0.0

                                    if (
                                        from == null ||
                                        to == null ||
                                        amount <= 0.0
                                    ) {
                                        message =
                                            v15Text(
                                                "From, To এবং সঠিক টাকার পরিমাণ দিন।",
                                                "Choose From, To and enter a valid amount."
                                            )
                                    } else {
                                        viewModel
                                            .transferBetweenAccounts(
                                                fromAccountId =
                                                    from,
                                                toAccountId =
                                                    to,
                                                amount =
                                                    amount,
                                                note =
                                                    transferNote
                                            ) {
                                                    success ->
                                                if (
                                                    success
                                                ) {
                                                    fromAccountId =
                                                        null
                                                    toAccountId =
                                                        null
                                                    transferAmountText =
                                                        ""
                                                    transferNote =
                                                        ""
                                                    message =
                                                        v15Text(
                                                            "Transfer সম্পন্ন হয়েছে।",
                                                            "Transfer completed."
                                                        )
                                                } else {
                                                    message =
                                                        v15Text(
                                                            "Transfer করা যায়নি। Source account-এর balance এবং তথ্য যাচাই করুন।",
                                                            "Transfer failed. Check the source balance and account details."
                                                        )
                                                }
                                            }
                                    }
                                },
                                enabled =
                                    canWrite &&
                                        activeAccounts
                                            .size >=
                                        2,
                                modifier =
                                    Modifier
                                        .weight(
                                            1f
                                        )
                            ) {
                                Text(
                                    v15Text(
                                        "Transfer",
                                        "Transfer"
                                    )
                                )
                            }
                        }
                    }

                    else -> {
                        val total =
                            activeAccounts
                                .sumOf {
                                    it.balance
                                }

                        Card(
                            modifier =
                                Modifier
                                    .fillMaxWidth(),
                            colors =
                                CardDefaults
                                    .cardColors(
                                        containerColor =
                                            MaterialTheme
                                                .colorScheme
                                                .primaryContainer
                                    ),
                            shape =
                                RoundedCornerShape(
                                    18.dp
                                )
                        ) {
                            Column(
                                modifier =
                                    Modifier
                                        .padding(
                                            16.dp
                                        ),
                                verticalArrangement =
                                    Arrangement
                                        .spacedBy(
                                            4.dp
                                        )
                            ) {
                                Text(
                                    v15Text(
                                        "সব অ্যাকাউন্টে মোট",
                                        "Total across accounts"
                                    ),
                                    style =
                                        MaterialTheme
                                            .typography
                                            .labelLarge
                                )

                                Text(
                                    "${V14DisplayState.currencySymbol} ${money(total)}",
                                    style =
                                        MaterialTheme
                                            .typography
                                            .headlineSmall,
                                    fontWeight =
                                        FontWeight
                                            .ExtraBold
                                )

                                Text(
                                    v15Text(
                                        "Internal Transfer করলে এই মোট টাকা বদলাবে না।",
                                        "Internal transfers do not change this total."
                                    ),
                                    style =
                                        MaterialTheme
                                            .typography
                                            .bodySmall
                                )
                            }
                        }

                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement
                                    .spacedBy(
                                        8.dp
                                    )
                        ) {
                            Button(
                                onClick = {
                                    mode =
                                        "ADD"
                                    message =
                                        ""
                                },
                                enabled =
                                    canWrite,
                                modifier =
                                    Modifier
                                        .weight(
                                            1f
                                        )
                            ) {
                                Text(
                                    v15Text(
                                        "+ অ্যাকাউন্ট",
                                        "+ Account"
                                    )
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    mode =
                                        "TRANSFER"
                                    message =
                                        ""
                                },
                                enabled =
                                    canWrite &&
                                        activeAccounts
                                            .size >=
                                        2,
                                modifier =
                                    Modifier
                                        .weight(
                                            1f
                                        )
                            ) {
                                Text(
                                    v15Text(
                                        "↔ Transfer",
                                        "↔ Transfer"
                                    )
                                )
                            }
                        }

                        if (
                            activeAccounts
                                .isEmpty()
                        ) {
                            Text(
                                v15Text(
                                    "এখনও কোনো Cash/Bank/Wallet account নেই। প্রথমে একটি অ্যাকাউন্ট যোগ করুন।",
                                    "No Cash, Bank or Wallet account yet. Add your first account."
                                ),
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodyMedium
                            )
                        } else {
                            activeAccounts
                                .forEach {
                                        account ->
                                    V16FinancialAccountCard(
                                        account =
                                            account
                                    )
                                }
                        }

                        if (!canWrite) {
                            Text(
                                v15Text(
                                    "Trial শেষ হওয়ায় নতুন account বা transfer বন্ধ আছে; আগের হিসাব দেখা যাবে।",
                                    "New accounts and transfers are disabled after trial expiry; existing records remain visible."
                                ),
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodySmall
                            )
                        }
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(
                            2.dp
                        )
                )
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
private fun V16FinancialAccountCard(
    account:
        FinancialAccountSummary
) {
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
                Modifier.padding(
                    14.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(
                    4.dp
                )
        ) {
            Row(
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier =
                        Modifier.weight(
                            1f
                        )
                ) {
                    Text(
                        account.name,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        V16FinancialAccountTypeLabel(
                            account
                        ),
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall
                    )
                }

                Text(
                    "${V14DisplayState.currencySymbol} ${money(account.balance)}",
                    fontWeight =
                        FontWeight.ExtraBold
                )
            }

            if (
                account.openingBalance >
                0.0
            ) {
                Text(
                    v15Text(
                        "শুরুর ব্যালেন্স: ${V14DisplayState.currencySymbol} ${money(account.openingBalance)}",
                        "Opening balance: ${V14DisplayState.currencySymbol} ${money(account.openingBalance)}"
                    ),
                    style =
                        MaterialTheme
                            .typography
                            .labelSmall
                )
            }
        }
    }
}

@Composable
private fun V16AccountSelectButton(
    account:
        FinancialAccountSummary,
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

private fun V16FinancialAccountTypeLabel(
    account:
        FinancialAccountSummary
): String =
    when (
        account.type
    ) {
        "CASH" ->
            v15Text(
                "ক্যাশ",
                "Cash"
            )

        "MOBILE_WALLET" ->
            account.provider
                .ifBlank {
                    v15Text(
                        "মোবাইল ওয়ালেট",
                        "Mobile wallet"
                    )
                }

        "BANK" ->
            v15Text(
                "ব্যাংক",
                "Bank"
            )

        "CARD" ->
            v15Text(
                "কার্ড",
                "Card"
            )

        else ->
            v15Text(
                "অন্যান্য",
                "Other"
            )
    }
