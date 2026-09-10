package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val QuantDarkColorScheme = darkColorScheme(
    primary = NeonGreen,
    onPrimary = DarkBackground,
    primaryContainer = NeonGreenDim,
    onPrimaryContainer = NeonGreen,
    secondary = AccentCyan,
    onSecondary = DarkBackground,
    secondaryContainer = AccentCyanDim,
    onSecondaryContainer = AccentCyan,
    tertiary = AccentAmber,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = NeonRed,
    onError = TextPrimary,
    errorContainer = NeonRedDim,
    onErrorContainer = NeonRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // For quantitative trading vision, we enforce high-contrast Dark theme
    MaterialTheme(
        colorScheme = QuantDarkColorScheme,
        typography = Typography,
        content = content
    )
}
