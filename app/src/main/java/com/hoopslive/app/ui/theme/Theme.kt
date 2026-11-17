package com.hoopslive.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Palette: plant green + light apricot-beige, with cream, apricot, espresso and terracotta accents.
val PlantGreen = Color(0xFF3E6B3A)
val PlantGreenLight = Color(0xFF8DBF84)
val Beige = Color(0xFFF5EADD)
val Cream = Color(0xFFFCF6EF)
val Apricot = Color(0xFFE9D3BC)
val Espresso = Color(0xFF3B2A1F)
val Terracotta = Color(0xFFC2502E)

val Accent = PlantGreen
val LiveRed = Terracotta
val WinGreen = PlantGreen
val LossRed = Terracotta
val PlayoffGreen = PlantGreen
val PlayInOrange = Color(0xFFD4A373)

private val LightColors = lightColorScheme(
    primary = PlantGreen,
    onPrimary = Color.White,
    secondary = PlantGreen,
    onSecondary = Color.White,
    secondaryContainer = PlantGreen,
    onSecondaryContainer = Color.White,
    background = Beige,
    onBackground = Espresso,
    surface = Beige,
    onSurface = Espresso,
    surfaceVariant = Apricot,
    onSurfaceVariant = Color(0xFF8A7461),
    surfaceContainer = Cream,
    surfaceContainerLow = Cream,
    surfaceContainerHigh = Color(0xFFF0E2D2),
    surfaceContainerHighest = Apricot,
    outline = Color(0xFFCDB49B),
    outlineVariant = Apricot,
)

private val DarkColors = darkColorScheme(
    primary = PlantGreenLight,
    onPrimary = Color(0xFF10200F),
    secondary = PlantGreenLight,
    onSecondary = Color(0xFF10200F),
    secondaryContainer = PlantGreen,
    onSecondaryContainer = Color.White,
    background = Color(0xFF121B15),
    onBackground = Beige,
    surface = Color(0xFF121B15),
    onSurface = Beige,
    surfaceVariant = Color(0xFF2A3A2F),
    onSurfaceVariant = Color(0xFFB8A592),
    surfaceContainer = Color(0xFF1B271F),
    surfaceContainerLow = Color(0xFF1B271F),
    surfaceContainerHigh = Color(0xFF213027),
    surfaceContainerHighest = Color(0xFF2A3A2F),
    outline = Color(0xFF4A5B4E),
    outlineVariant = Color(0xFF26342A),
)

private val AppTypography = Typography().let { t ->
    t.copy(
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp),
    )
}

@Composable
fun HoopsTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
