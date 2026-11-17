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

val Accent = Color(0xFFFF7A1A)
val LiveRed = Color(0xFFE5383B)
val WinGreen = Color(0xFF2EB872)
val LossRed = Color(0xFFE5484D)
val PlayoffGreen = Color(0xFF2EB872)
val PlayInOrange = Color(0xFFFFA62B)

private val DarkColors = darkColorScheme(
    primary = Accent,
    onPrimary = Color(0xFF1A0D00),
    secondary = Color(0xFF8AB4F8),
    background = Color(0xFF0D1117),
    onBackground = Color(0xFFE6EDF3),
    surface = Color(0xFF0D1117),
    onSurface = Color(0xFFE6EDF3),
    surfaceVariant = Color(0xFF1B222C),
    onSurfaceVariant = Color(0xFF9AA7B4),
    surfaceContainer = Color(0xFF151B23),
    surfaceContainerHigh = Color(0xFF1B222C),
    surfaceContainerHighest = Color(0xFF232B36),
    surfaceContainerLow = Color(0xFF12171F),
    outline = Color(0xFF39424E),
    outlineVariant = Color(0xFF262E39),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFFE0620A),
    onPrimary = Color.White,
    secondary = Color(0xFF1F5FBF),
    background = Color(0xFFF5F7FA),
    onBackground = Color(0xFF0F1720),
    surface = Color(0xFFF5F7FA),
    onSurface = Color(0xFF0F1720),
    surfaceVariant = Color(0xFFE9EDF2),
    onSurfaceVariant = Color(0xFF55606D),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF0F3F7),
    surfaceContainerHighest = Color(0xFFE6EAF0),
    surfaceContainerLow = Color(0xFFFFFFFF),
    outline = Color(0xFFC3CBD5),
    outlineVariant = Color(0xFFDDE3EA),
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
