package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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

private val LightColorScheme = lightColorScheme(
    primary = NeonCyanDark,
    onPrimary = Color.White,
    primaryContainer = StudioLightCardElevated,
    onPrimaryContainer = NeonCyanDark,
    secondary = ElectricBlueDark,
    onSecondary = Color.White,
    secondaryContainer = StudioLightCard,
    onSecondaryContainer = ElectricBlueDark,
    tertiary = AccentAmber,
    onTertiary = Color.White,
    background = StudioLightBg,
    onBackground = TextPrimaryLight,
    surface = StudioLightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = StudioLightCard,
    onSurfaceVariant = TextSecondaryLight,
    outline = DividerColorLight,
    error = AccentMagenta,
    onError = Color.White
)

@Composable
fun AuraSoundTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
