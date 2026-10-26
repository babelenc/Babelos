package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BabelDarkColorScheme = darkColorScheme(
    primary = BabelCyan,
    onPrimary = Color(0xFF001F29),
    primaryContainer = Color(0xFF004D61),
    onPrimaryContainer = Color(0xFFBCE9FF),
    secondary = BabelBlue,
    onSecondary = Color(0xFF002868),
    secondaryContainer = Color(0xFF003D9E),
    onSecondaryContainer = Color(0xFFDCE2FF),
    tertiary = BabelTeal,
    onTertiary = Color(0xFF003730),
    background = BabelDarkBackground,
    onBackground = BabelTextPrimary,
    surface = BabelDarkSurface,
    onSurface = BabelTextPrimary,
    surfaceVariant = BabelDarkSurfaceVariant,
    onSurfaceVariant = BabelTextSecondary,
    outline = BabelDarkCardBorder,
    outlineVariant = BabelDivider,
    error = BabelError,
    onError = Color.White
)

@Composable
fun BabelOSTheme(
    darkTheme: Boolean = true, // BabelOS is by default a futuristic dark OS
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BabelDarkColorScheme,
        typography = Typography,
        content = content
    )
}
