package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Extra color tokens for dark mode support defined first to prevent forward reference errors
val ColorTextDarkBg = Color(0xFF1E1510)
val ColorDarkSurface = Color(0xFF2E221B)

private val FestiveColorScheme = lightColorScheme(
    primary = PrimarySaffron,
    onPrimary = SurfaceWhite,
    primaryContainer = LightSaffron,
    onPrimaryContainer = DarkSaffron,
    secondary = SecondaryGreen,
    onSecondary = SurfaceWhite,
    secondaryContainer = LightGreen,
    onSecondaryContainer = SecondaryGreen,
    tertiary = AccentGold,
    background = BackgroundCream,
    onBackground = TextDark,
    surface = SurfaceWhite,
    onSurface = TextDark,
    surfaceVariant = BackgroundCream,
    onSurfaceVariant = TextMuted,
    outline = BorderLight,
    error = ErrorRed,
    errorContainer = ErrorContainer,
    onError = SurfaceWhite
)

// In case user prefers dark mode, we can output a slightly richer dark saffron
private val FestiveDarkColorScheme = darkColorScheme(
    primary = PrimarySaffron,
    onPrimary = SurfaceWhite,
    primaryContainer = DarkSaffron,
    secondary = SecondaryGreen,
    onSecondary = SurfaceWhite,
    background = ColorTextDarkBg,
    surface = ColorDarkSurface,
    onBackground = SurfaceWhite,
    onSurface = SurfaceWhite
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disable dynamic colors to enforce the branded saffron festival theme
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) FestiveDarkColorScheme else FestiveColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
