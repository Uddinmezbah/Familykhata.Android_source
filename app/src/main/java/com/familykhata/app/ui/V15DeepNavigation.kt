package com.familykhata.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal object V15DeepNavigationState {
    private val activeOwners =
        mutableStateMapOf<String, Boolean>()

    val active: Boolean
        get() = activeOwners.isNotEmpty()

    fun set(
        owner: String,
        active: Boolean
    ) {
        if (active) {
            activeOwners[owner] = true
        } else {
            activeOwners.remove(owner)
        }
    }

    fun clear() {
        activeOwners.clear()
    }
}

@Composable
internal fun TrackV15DeepScreen(
    owner: String,
    active: Boolean
) {
    LaunchedEffect(
        owner,
        active
    ) {
        V15DeepNavigationState.set(
            owner,
            active
        )
    }

    DisposableEffect(owner) {
        onDispose {
            V15DeepNavigationState.set(
                owner,
                false
            )
        }
    }
}

@Composable
internal fun V15DeepScreenContainer(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 2.dp
        ) {
            Row(
                modifier =
                    Modifier.padding(
                        horizontal = 4.dp,
                        vertical = 6.dp
                    ),
                verticalAlignment =
                    Alignment.CenterVertically,
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onBack
                ) {
                    Text("←")
                }

                Text(
                    title,
                    modifier = Modifier.weight(1f),
                    style =
                        MaterialTheme.typography.titleMedium,
                    fontWeight =
                        FontWeight.ExtraBold
                )
            }
        }

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 8.dp)
        ) {
            content()
        }
    }
}
