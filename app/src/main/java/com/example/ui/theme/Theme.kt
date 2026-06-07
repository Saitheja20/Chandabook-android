package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ChandaBookColorScheme = lightColorScheme(
    primary              = Color(0xFF0F2F3A),   // TealDark
    onPrimary            = Color(0xFFF4C542),   // Gold text on teal
    primaryContainer     = Color(0xFF1E5368),   // TealLight
    onPrimaryContainer   = Color(0xFFF4C542),   // Gold on teal container
    secondary            = Color(0xFFF4C542),   // Gold
    onSecondary          = Color(0xFF0F2F3A),   // TealDark on gold
    secondaryContainer   = Color(0xFFFFF3CD),   // Gold tint bg
    onSecondaryContainer = Color(0xFF0F2F3A),
    tertiary             = Color(0xFF1F8F4E),   // Green
    onTertiary           = Color(0xFFFFFFFF),
    tertiaryContainer    = Color(0xFFD4EDDA),   // Green tint bg
    onTertiaryContainer  = Color(0xFF166B3A),
    error                = Color(0xFFFF8C1A),   // Orange for errors
    onError              = Color(0xFFFFFFFF),
    errorContainer       = Color(0xFFFFEDD5),
    onErrorContainer     = Color(0xFF0F2F3A),
    background           = Color(0xFFF8FAFB),   // --bg
    onBackground         = Color(0xFF1A2E38),   // --text
    surface              = Color(0xFFFFFFFF),   // --white
    onSurface            = Color(0xFF1A2E38),
    surfaceVariant       = Color(0xFFE8F0F3),   // light teal tint
    onSurfaceVariant     = Color(0xFF5A7685),   // --text-muted
    outline              = Color(0xFFE2EAED),   // --border
    outlineVariant       = Color(0xFFCDD9DF),
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disable dynamic colors to enforce the branded theme
    content: @Composable () -> Unit,
) {
    // Rely exclusively on ChandaBookColorScheme for full brand consistency
    MaterialTheme(
        colorScheme = ChandaBookColorScheme,
        shapes = ChandaBookShapes,
        typography = Typography,
        content = content
    )
}
