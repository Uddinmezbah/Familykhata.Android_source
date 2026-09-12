package com.familykhata.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val HisabiLightColors = lightColorScheme(
    primary = Color(0xFF0B6B58),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD7F4E9),
    onPrimaryContainer = Color(0xFF082E27),
    secondary = Color(0xFF4D635B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD9E9E2),
    onSecondaryContainer = Color(0xFF10211C),
    tertiary = Color(0xFF59633B),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFDDE9B5),
    onTertiaryContainer = Color(0xFF1A2108),
    background = Color(0xFFF7FAF7),
    onBackground = Color(0xFF181D1B),
    surface = Color(0xFFFBFDFB),
    onSurface = Color(0xFF181D1B),
    surfaceVariant = Color(0xFFE1EAE5),
    onSurfaceVariant = Color(0xFF414945),
    outline = Color(0xFF717974),
    error = Color(0xFFBA1A1A)
)

private val HisabiDarkColors = darkColorScheme(
    primary = Color(0xFF9DD6C5),
    onPrimary = Color(0xFF00382D),
    primaryContainer = Color(0xFF005141),
    onPrimaryContainer = Color(0xFFB8F2E0),
    secondary = Color(0xFFBDCCC4),
    onSecondary = Color(0xFF28332F),
    secondaryContainer = Color(0xFF3E4A45),
    onSecondaryContainer = Color(0xFFD9E9E2),
    tertiary = Color(0xFFC1CC96),
    onTertiary = Color(0xFF2B3412),
    tertiaryContainer = Color(0xFF414B26),
    onTertiaryContainer = Color(0xFFDDE9B5),
    background = Color(0xFF101412),
    onBackground = Color(0xFFE0E4E1),
    surface = Color(0xFF101412),
    onSurface = Color(0xFFE0E4E1),
    surfaceVariant = Color(0xFF414945),
    onSurfaceVariant = Color(0xFFC1C9C4),
    outline = Color(0xFF8B938E),
    error = Color(0xFFFFB4AB)
)

private val HisabiShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun HisabiKhataTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) HisabiDarkColors else HisabiLightColors,
        shapes = HisabiShapes,
        content = content
    )
}
