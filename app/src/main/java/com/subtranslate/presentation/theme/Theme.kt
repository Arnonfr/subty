package com.subtranslate.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF82B1FF),
    onPrimary = Color(0xFF003399),
    primaryContainer = Color(0xFF004CBD),
    onPrimaryContainer = Color(0xFFDAE2FF),
    secondary = Color(0xFFBBC7FF),
    onSecondary = Color(0xFF222D6E),
    background = Color(0xFF1A1C1E),
    surface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFF44474E),
    onBackground = Color(0xFFE3E2E6),
    onSurface = Color(0xFFE3E2E6),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1750D1),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDAE2FF),
    onPrimaryContainer = Color(0xFF001553),
    secondary = Color(0xFF575E8B),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFFAFAFF),
    surface = Color(0xFFFAFAFF),
    surfaceVariant = Color(0xFFE3E2EC),
    onBackground = Color(0xFF1A1B1F),
    onSurface = Color(0xFF1A1B1F),
)

@Composable
fun SubTranslateTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
