package com.familykhata.app.ui

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private const val LANGUAGE_PREFS = "hisabi_khata_v15_language"

internal object V15LanguageState {
    var languageCode by mutableStateOf<String?>(null)
        private set
    private var initialized = false

    fun ensureInitialized(context: Context) {
        if (initialized) return
        languageCode = context.getSharedPreferences(LANGUAGE_PREFS, Context.MODE_PRIVATE)
            .getString("language", null)
            ?.takeIf { it == "bn" || it == "en" }
        initialized = true
    }

    fun setLanguage(context: Context, code: String) {
        if (code !in setOf("bn", "en")) return
        context.getSharedPreferences(LANGUAGE_PREFS, Context.MODE_PRIVATE)
            .edit().putString("language", code).apply()
        languageCode = code
    }

    fun isBangla(): Boolean = languageCode != "en"
}

internal fun v15Text(bn: String, en: String): String =
    if (V15LanguageState.isBangla()) bn else en

@Composable
internal fun LanguageOnboardingScreen() {
    val context = LocalContext.current
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(shape = RoundedCornerShape(50), color = Color(0xFF0B6B58)) {
                        Text("৳", modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp), color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                    }
                    Text("হিসাবী খাতা / Hisabi Khata", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                    Text("ভাষা নির্বাচন করুন\nChoose your language", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                    Button(
                        onClick = { V15LanguageState.setLanguage(context, "bn") },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("বাংলা") }
                    OutlinedButton(
                        onClick = { V15LanguageState.setLanguage(context, "en") },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("English") }
                    Text("You can change this later from Menu → Language.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
