package com.subtranslate.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ── Always-static tokens (same in both themes) ────────────────────────────────
val SubtyBlack     = Color(0xFF0A0908)
val SubtyWhite     = Color(0xFFF5F0EB)
val SubtyMocha     = Color(0xFFB5926A)
val SubtyMochaDark = Color(0xFF7A5C3A)
val SubtyError     = Color(0xFFFF6B6B)

// ── Raw dark palette (private) ────────────────────────────────────────────────
private val _DarkBg        = Color(0xFF0A0908)
private val _DarkBg2       = Color(0xFF1A1612)
private val _DarkBg3       = Color(0xFF241E18)
private val _DarkBorder    = Color(0xFFF5F0EB)
private val _DarkBorderDim = Color(0xFF3A342E)
private val _DarkText1     = Color(0xFFF5F0EB)
private val _DarkText2     = Color(0xFFC8BFB5)
private val _DarkText3     = Color(0xFF7A6E65)

// ── Raw light palette (private) ───────────────────────────────────────────────
private val _LightBg        = Color(0xFFF5F0EB)
private val _LightBg2       = Color(0xFFE8E0D5)
private val _LightBg3       = Color(0xFFD8CEC0)
private val _LightBorder    = Color(0xFF1A1410)
private val _LightBorderDim = Color(0xFFBBAFA4)
private val _LightText1     = Color(0xFF1A1410)
private val _LightText2     = Color(0xFF4A3E30)
private val _LightText3     = Color(0xFF9A8878)

// ── Theme-aware color data class ──────────────────────────────────────────────
data class SubtyColors(
    val bg: Color,
    val bg2: Color,
    val bg3: Color,
    val border: Color,
    val borderDim: Color,
    val text1: Color,
    val text2: Color,
    val text3: Color,
)

private val DarkSubtyColors = SubtyColors(
    bg        = _DarkBg,
    bg2       = _DarkBg2,
    bg3       = _DarkBg3,
    border    = _DarkBorder,
    borderDim = _DarkBorderDim,
    text1     = _DarkText1,
    text2     = _DarkText2,
    text3     = _DarkText3,
)

private val LightSubtyColors = SubtyColors(
    bg        = _LightBg,
    bg2       = _LightBg2,
    bg3       = _LightBg3,
    border    = _LightBorder,
    borderDim = _LightBorderDim,
    text1     = _LightText1,
    text2     = _LightText2,
    text3     = _LightText3,
)

val LocalSubtyColors = staticCompositionLocalOf { DarkSubtyColors }

// ── Theme-aware color accessors ───────────────────────────────────────────────
// These are @Composable getters so they automatically reflect the active theme
// without any changes needed in screen files.
val SubtyBg: Color
    @Composable @ReadOnlyComposable get() = LocalSubtyColors.current.bg
val SubtyBg2: Color
    @Composable @ReadOnlyComposable get() = LocalSubtyColors.current.bg2
val SubtyBg3: Color
    @Composable @ReadOnlyComposable get() = LocalSubtyColors.current.bg3
val SubtyBorder: Color
    @Composable @ReadOnlyComposable get() = LocalSubtyColors.current.border
val SubtyBorderDim: Color
    @Composable @ReadOnlyComposable get() = LocalSubtyColors.current.borderDim
val SubtyText1: Color
    @Composable @ReadOnlyComposable get() = LocalSubtyColors.current.text1
val SubtyText2: Color
    @Composable @ReadOnlyComposable get() = LocalSubtyColors.current.text2
val SubtyText3: Color
    @Composable @ReadOnlyComposable get() = LocalSubtyColors.current.text3

// ── Material color schemes ────────────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary             = SubtyMocha,
    onPrimary           = SubtyBlack,
    primaryContainer    = Color(0xFF3D2800),
    onPrimaryContainer  = SubtyWhite,
    secondary           = _DarkText2,
    onSecondary         = SubtyBlack,
    background          = _DarkBg,
    surface             = _DarkBg2,
    surfaceVariant      = _DarkBg3,
    onBackground        = _DarkText1,
    onSurface           = _DarkText1,
    onSurfaceVariant    = _DarkText3,
    outline             = _DarkBorder,
    error               = SubtyError,
    onError             = SubtyBlack,
    errorContainer      = Color(0xFF3D0000),
    onErrorContainer    = SubtyError,
)

private val LightColorScheme = lightColorScheme(
    primary             = SubtyMocha,
    onPrimary           = SubtyWhite,
    primaryContainer    = Color(0xFFFFDEB3),
    onPrimaryContainer  = Color(0xFF2C1600),
    secondary           = _LightText2,
    onSecondary         = SubtyWhite,
    background          = _LightBg,
    surface             = _LightBg2,
    surfaceVariant      = _LightBg3,
    onBackground        = _LightText1,
    onSurface           = _LightText1,
    onSurfaceVariant    = _LightText3,
    outline             = _LightBorder,
    error               = Color(0xFFCC3333),
    onError             = SubtyWhite,
)

// ── Shapes ────────────────────────────────────────────────────────────────────
private val Shapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small      = RoundedCornerShape(0.dp),
    medium     = RoundedCornerShape(0.dp),
    large      = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp),
)

// ── Theme entry point ─────────────────────────────────────────────────────────
@Composable
fun SubTranslateTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkSubtyColors else LightSubtyColors
    CompositionLocalProvider(LocalSubtyColors provides colors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            shapes      = Shapes,
            typography  = Typography,
            content     = content,
        )
    }
}
