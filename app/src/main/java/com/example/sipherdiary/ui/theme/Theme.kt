package com.example.sipherdiary.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val SipherLightColorScheme = lightColorScheme(
    primary = SipherColor.Blue500,
    onPrimary = SipherColor.Bg,
    primaryContainer = SipherColor.Tint,
    onPrimaryContainer = SipherColor.Blue700,
    background = SipherColor.Bg,
    onBackground = SipherColor.Ink900,
    surface = SipherColor.Bg,
    onSurface = SipherColor.Ink900,
    surfaceVariant = SipherColor.Subtle,
    onSurfaceVariant = SipherColor.Ink600,
    outline = SipherColor.Line,
    error = SipherColor.Danger
)

@Composable
fun SipherDiaryTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SipherLightColorScheme,
        typography = Typography,
        content = content
    )
}