package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = StudioDarkBg,
    primaryContainer = StudioCardElevated,
    onPrimaryContainer = NeonCyan,
    secondary = ElectricBlue,
    onSecondary = StudioDarkBg,
    secondaryContainer = StudioCard,
    onSecondaryContainer = ElectricBlue,
    tertiary = AccentAmber,
    onTertiary = StudioDarkBg,
    background = StudioDarkBg,
    onBackground = TextPrimary,
    surface = StudioSurface,
    onSurface = TextPrimary,
    surfaceVariant = StudioCard,
    onSurfaceVariant = TextSecondary,
    outline = DividerColor,
    error = AccentMagenta,
    onError = Color.White
)

@Composable
fun AuraSoundTheme(
    darkTheme: Boolean = true, // Default to sleek studio dark aesthetic
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
