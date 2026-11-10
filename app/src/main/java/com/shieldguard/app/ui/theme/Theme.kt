package com.shieldguard.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val GlassyColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = BackgroundObsidian,
    secondary = NeonEmerald,
    onSecondary = BackgroundObsidian,
    tertiary = ElectricIndigo,
    background = BackgroundObsidian,
    onBackground = TextPrimary,
    surface = BackgroundSurface,
    onSurface = TextPrimary,
    surfaceVariant = BackgroundElevated,
    onSurfaceVariant = TextSecondary,
    error = GmailCoral,
    onError = TextPrimary
)

@Composable
fun ShieldGuardTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GlassyColorScheme,
        typography = Typography,
        content = content
    )
}
