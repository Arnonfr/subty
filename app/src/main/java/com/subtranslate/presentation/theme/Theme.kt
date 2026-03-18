package com.subtranslate.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ── Design tokens ─────────────────────────────────────────────────────────────
val SubtyBlack     = Color(0xFF0A0908)
val SubtyWhite     = Color(0xFFF5F0EB)
val SubtyMocha     = Color(0xFFB5926A)
val SubtyMochaDark = Color(0xFF7A5C3A)
val SubtyBg        = Color(0xFF0A0908)
val SubtyBg2       = Color(0xFF1A1612)
val SubtyBg3       = Color(0xFF241E18)
val SubtyBorder    = Color(0xFFF5F0EB)   // 1px border in dark mode
val SubtyBorderDim = Color(0xFF3A342E)   // subtle separator
val SubtyText1     = Color(0xFFF5F0EB)
val SubtyText2     = Color(0xFFC8BFB5)
val SubtyText3     = Color(0xFF7A6E65)
val SubtyError     = Color(0xFFFF6B6B)

private val ColorScheme = darkColorScheme(
    primary             = SubtyMocha,
    onPrimary           = SubtyBlack,
    primaryContainer    = Color(0xFF3D2800),
    onPrimaryContainer  = SubtyWhite,
    secondary           = SubtyText2,
    onSecondary         = SubtyBlack,
    background          = SubtyBg,
    surface             = SubtyBg2,
    surfaceVariant      = SubtyBg3,
    onBackground        = SubtyText1,
    onSurface           = SubtyText1,
    onSurfaceVariant    = SubtyText3,
    outline             = SubtyBorder,
    error               = SubtyError,
    onError             = SubtyBlack,
    errorContainer      = Color(0xFF3D0000),
    onErrorContainer    = SubtyError,
)

// Zero border radius everywhere — Swiss/Bauhaus aesthetic
private val Shapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small      = RoundedCornerShape(0.dp),
    medium     = RoundedCornerShape(0.dp),
    large      = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp),
)

@Composable
fun SubTranslateTheme(
    darkTheme: Boolean = true,   // always dark — matches design
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ColorScheme,
        shapes      = Shapes,
        typography  = Typography,
        content     = content,
    )
}
