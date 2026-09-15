package com.familykhata.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.familykhata.app.FamilyKhataViewModel

@Composable
internal fun ProtectedDeleteDialog(
    viewModel: FamilyKhataViewModel,
    title: String,
    message: String,
    confirmLabel: String = v15Text("ডিলিট করুন", "Delete"),
    onDismiss: () -> Unit,
    onConfirmed: () -> Unit
) {
    val pinConfigured by viewModel.isPinConfigured.collectAsState()
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(message)

                if (pinConfigured) {
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { value ->
                            pin = value
                                .filter { it.isDigit() }
                                .take(6)
                            error = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        label = { Text("PIN") },
                        singleLine = true,
                        visualTransformation =
                            PasswordVisualTransformation(),
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType =
                                    KeyboardType.NumberPassword
                            )
                    )

                    error?.let {
                        Text(
                            it,
                            modifier = Modifier.padding(top = 6.dp),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    Text(
                        v15Text(
                            "নিরাপত্তার জন্য ডিলিট করতে আগে Settings থেকে PIN সেট করুন।",
                            "Set a PIN in Settings before deleting for security."
                        ),
                        modifier = Modifier.padding(top = 12.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            if (pinConfigured) {
                TextButton(
                    onClick = {
                        if (viewModel.verifyPin(pin)) {
                            onConfirmed()
                        } else {
                            error =
                                v15Text(
                                    "PIN সঠিক নয়",
                                    "Incorrect PIN"
                                )
                            pin = ""
                        }
                    },
                    enabled = pin.length in 4..6
                ) {
                    Text(confirmLabel)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(v15Text("বাতিল", "Cancel"))
            }
        }
    )
}
