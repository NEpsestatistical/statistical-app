package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TerminalDarkColorScheme = darkColorScheme(
    primary = GainGreen,
    onPrimary = Color.White,
    secondary = AccentBlue,
    background = TerminalDarkBg,
    onBackground = TextPrimaryDark,
    surface = TerminalDarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = TerminalDarkCard,
    onSurfaceVariant = TextSecondaryDark,
    error = LossRed,
    onError = Color.White
)

private val TerminalLightColorScheme = lightColorScheme(
    primary = GainGreen,
    onPrimary = Color.White,
    secondary = AccentBlue,
    background = TerminalLightBg,
    onBackground = TextPrimaryLight,
    surface = TerminalLightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = TerminalLightCard,
    onSurfaceVariant = TextSecondaryLight,
    error = LossRed,
    onError = Color.White
)

@Composable
fun StatisticalPortfolioTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) TerminalDarkColorScheme else TerminalLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
