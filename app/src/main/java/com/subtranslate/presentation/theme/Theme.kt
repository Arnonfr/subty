package com.subtranslate.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Warm mocha / amber palette matching preview design
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFB5926A),          // Mocha
    onPrimary = Color(0xFF1A0E00),
    primaryContainer = Color(0xFF3D2800),
    onPrimaryContainer = Color(0xFFFFDDB8),
    secondary = Color(0xFF9A8F86),         // Warm gray
    onSecondary = Color(0xFF1A1210),
    secondaryContainer = Color(0xFF2D2418),
    onSecondaryContainer = Color(0xFFD6C8BC),
    background = Color(0xFF0F0D0B),        // Near-black warm
    surface = Color(0xFF1A1610),
    surfaceVariant = Color(0xFF2D2920),
    onBackground = Color(0xFFF5F0EB),      // Warm white
    onSurface = Color(0xFFF5F0EB),
    onSurfaceVariant = Color(0xFFB0A89F),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF8B6340),           // Darker mocha
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDDB8),
    onPrimaryContainer = Color(0xFF2E1600),
    secondary = Color(0xFF6E5E54),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF4DDD1),
    onSecondaryContainer = Color(0xFF271914),
    background = Color(0xFFFFF8F3),        // Warm white
    surface = Color(0xFFFFF8F3),
    surfaceVariant = Color(0xFFF0E6DA),
    onBackground = Color(0xFF1A0E00),
    onSurface = Color(0xFF1A0E00),
    onSurfaceVariant = Color(0xFF52443B),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onError = Color(0xFFFFFFFF),
    onErrorContainer = Color(0xFF410002),
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
