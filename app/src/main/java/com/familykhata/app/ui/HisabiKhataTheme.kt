package com.familykhata.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val HisabiLightColors = lightColorScheme(
    primary = Color(0xFF075F50),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD7F4E9),
    onPrimaryContainer = Color(0xFF062F28),
    secondary = Color(0xFF355E78),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD9ECF8),
    onSecondaryContainer = Color(0xFF102D3D),
    tertiary = Color(0xFF7A5A20),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFE3AE),
    onTertiaryContainer = Color(0xFF2B1C00),
    background = Color(0xFFF6F8F7),
    onBackground = Color(0xFF171C1A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF171C1A),
    surfaceVariant = Color(0xFFE7ECE9),
    onSurfaceVariant = Color(0xFF434A47),
    outline = Color(0xFF737B77),
    error = Color(0xFFBA1A1A)
)

private val HisabiDarkColors = darkColorScheme(
    primary = Color(0xFF96D9C4),
    onPrimary = Color(0xFF00382D),
    primaryContainer = Color(0xFF075446),
    onPrimaryContainer = Color(0xFFB8F2E0),
    secondary = Color(0xFFA8CCE1),
    onSecondary = Color(0xFF103447),
    secondaryContainer = Color(0xFF2A4B5E),
    onSecondaryContainer = Color(0xFFD0E9F7),
    tertiary = Color(0xFFF1C36F),
    onTertiary = Color(0xFF402D00),
    tertiaryContainer = Color(0xFF5D430B),
    onTertiaryContainer = Color(0xFFFFE3AE),
    background = Color(0xFF101412),
    onBackground = Color(0xFFE0E4E1),
    surface = Color(0xFF151A17),
    onSurface = Color(0xFFE0E4E1),
    surfaceVariant = Color(0xFF414945),
    onSurfaceVariant = Color(0xFFC1C9C4),
    outline = Color(0xFF8B938E),
    error = Color(0xFFFFB4AB)
)

private val HisabiShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp)
)

private val HisabiTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 31.sp,
        lineHeight = 39.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 27.sp,
        lineHeight = 35.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 23.sp,
        lineHeight = 31.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 25.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 27.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 24.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 18.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp
    )
)

@Composable
fun HisabiKhataTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) HisabiDarkColors else HisabiLightColors,
        typography = HisabiTypography,
        shapes = HisabiShapes,
        content = content
    )
}
