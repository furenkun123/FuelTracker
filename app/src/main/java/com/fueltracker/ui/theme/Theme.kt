package com.fueltracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
private val LightColors = lightColorScheme(
    primary = Blue600,
    background = BackgroundLight,
    surface = SurfaceWhite,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary
)
private val DarkColors = darkColorScheme(
    primary = Blue400
)
@Composable
fun FuelTrackerTheme(
    content: @Composable () -> Unit
) {
    val colors =
        if (isSystemInDarkTheme()) {
            DarkColors
        } else {
            LightColors
        }
    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content
    )
}