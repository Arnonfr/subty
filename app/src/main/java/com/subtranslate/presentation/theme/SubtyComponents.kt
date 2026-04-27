package com.subtranslate.presentation.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.material3.Text as M3Text

// ── Typography helpers ─────────────────────────────────────────────────────────

@Composable
fun SubtyText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = SubtyText1,
    fontSize: Int = 14,
    weight: FontWeight = FontWeight.Normal,
    letterSpacing: Float = 0f,
    uppercase: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    M3Text(
        text = if (uppercase) text.uppercase() else text,
        modifier = modifier,
        color = color,
        fontSize = fontSize.sp,
        fontWeight = weight,
        letterSpacing = letterSpacing.em,
        maxLines = maxLines,
        overflow = overflow,
        lineHeight = (fontSize * 1.4).sp,
    )
}

/** Large page title — e.g. "SEARCH" */
@Composable
fun SubtyPageTitle(text: String, modifier: Modifier = Modifier) {
    SubtyText(
        text = text,
        modifier = modifier,
        fontSize = 32,
        weight = FontWeight.Black,
        letterSpacing = 0f,
        uppercase = true,
        color = SubtyText1,
    )
}

/** Small uppercase section label */
@Composable
fun SubtyLabel(text: String, modifier: Modifier = Modifier, color: Color = SubtyText3) {
    SubtyText(
        text = text,
        modifier = modifier,
        fontSize = 9,
        weight = FontWeight.Bold,
        letterSpacing = 0.12f,
        uppercase = true,
        color = color,
    )
}

// ── Horizontal rule ─────────────────────────────────────────────────────────────

@Composable
fun SubtyDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(SubtyBorder)
    )
}

@Composable
fun SubtyDividerDim(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(SubtyBorderDim)
    )
}

// ── Top bar ───────────────────────────────────────────────────────────────────

@Composable
fun SubtyTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(SubtyBg)
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (onBack != null) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    val arrowColor = SubtyText1
                    Canvas(modifier = Modifier.size(18.dp)) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val color = arrowColor
                        drawLine(color, Offset(cx, 2f), Offset(4f, cy), strokeWidth = 1.5f)
                        drawLine(color, Offset(4f, cy), Offset(cx, size.height - 2f), strokeWidth = 1.5f)
                        drawLine(color, Offset(4f, cy), Offset(size.width - 2f, cy), strokeWidth = 1.5f)
                    }
                }
            }
            SubtyText(
                text = title,
                fontSize = 13,
                weight = FontWeight.Bold,
                letterSpacing = 0.06f,
                uppercase = true,
                color = SubtyText1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        SubtyDivider()
    }
}

// ── Buttons ───────────────────────────────────────────────────────────────────

enum class SubtyButtonStyle { OUTLINE, FILLED, MOCHA }

@Composable
fun SubtyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: SubtyButtonStyle = SubtyButtonStyle.OUTLINE,
    enabled: Boolean = true,
    small: Boolean = false,
    loading: Boolean = false,
    leadingContent: (@Composable () -> Unit)? = null,
) {
    val bg = when {
        !enabled -> Color.Transparent
        style == SubtyButtonStyle.FILLED -> SubtyText1
        style == SubtyButtonStyle.MOCHA -> SubtyMocha
        else -> Color.Transparent
    }
    val fg = when {
        !enabled -> SubtyText3
        style == SubtyButtonStyle.FILLED -> SubtyBg
        style == SubtyButtonStyle.MOCHA -> SubtyBg
        else -> SubtyText1
    }
    val borderColor = when {
        !enabled -> SubtyText3
        style == SubtyButtonStyle.MOCHA -> SubtyMocha
        else -> SubtyBorder
    }
    val hPad = if (small) 16.dp else 20.dp
    val vPad = if (small) 11.dp else 13.dp
    val fSize = if (small) 11 else 12

    Row(
        modifier = modifier
            .border(1.dp, borderColor)
            .background(bg)
            .then(
                if (enabled) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ) else Modifier
            )
            .padding(horizontal = hPad, vertical = vPad),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            SubtyText(
                "…", fontSize = fSize, weight = FontWeight.Bold,
                letterSpacing = 0.1f, uppercase = true, color = fg,
            )
        } else {
            if (leadingContent != null) {
                leadingContent()
                Spacer(Modifier.width(8.dp))
            }
            SubtyText(text, fontSize = fSize, weight = FontWeight.Bold,
                letterSpacing = 0.1f, uppercase = true, color = fg)
        }
    }
}

// ── Chips ─────────────────────────────────────────────────────────────────────

@Composable
fun SubtyChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg    = if (selected) SubtyText1 else Color.Transparent
    val fg    = if (selected) SubtyBg else SubtyText2
    Box(
        modifier = modifier
            .border(1.dp, if (selected) SubtyBorder else SubtyBorderDim)
            .background(bg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 26.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        SubtyText(text, fontSize = 14, weight = FontWeight.Bold,
            letterSpacing = 0.06f, uppercase = true, color = fg)
    }
}

/** Language chip in the filter bar (underline style) */
@Composable
fun SubtyLangChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 26.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SubtyText(
            text,
            fontSize = 14,
            weight = FontWeight.Bold,
            letterSpacing = 0.08f,
            uppercase = true,
            color = if (selected) SubtyText1 else SubtyText3,
        )
        if (selected) {
            Spacer(Modifier.height(5.dp))
            Box(Modifier.height(2.dp).width(32.dp).background(SubtyMocha))
        }
    }
}

// ── Card ─────────────────────────────────────────────────────────────────────

@Composable
fun SubtyCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, SubtyBorder)
            .background(SubtyBg2),
        content = content,
    )
}

// ── Text field ───────────────────────────────────────────────────────────────

@Composable
fun SubtyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    label: String = "",
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailingIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    val borderColor = if (value.isNotEmpty()) SubtyMocha else SubtyBorderDim
    Column(modifier = modifier) {
        if (label.isNotEmpty()) {
            SubtyLabel(label, modifier = Modifier.padding(bottom = 6.dp))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, borderColor)
                .background(SubtyBg2)
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                enabled = enabled,
                singleLine = singleLine,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                visualTransformation = visualTransformation,
                textStyle = TextStyle(
                    color = SubtyText1,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                ),
                cursorBrush = SolidColor(SubtyMocha),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        SubtyText(placeholder, color = SubtyText3, fontSize = 14)
                    }
                    inner()
                },
            )
            if (trailingIcon != null) {
                Spacer(Modifier.width(8.dp))
                trailingIcon()
            }
        }
    }
}

// ── Switch (keep Material but restyle) ───────────────────────────────────────

@Composable
fun SubtySwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = SubtyBlack,
            checkedTrackColor = SubtyMocha,
            uncheckedThumbColor = SubtyText3,
            uncheckedTrackColor = SubtyBg3,
            uncheckedBorderColor = SubtyBorderDim,
        ),
    )
}

// ── Toggle row ────────────────────────────────────────────────────────────────

@Composable
fun SubtyToggleRow(
    label: String,
    description: String = "",
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            SubtyText(label, fontSize = 14, weight = FontWeight.SemiBold, color = SubtyText1)
            if (description.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                SubtyText(description, fontSize = 12, color = SubtyText3)
            }
        }
        SubtySwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

// ── Inline progress bar ───────────────────────────────────────────────────────

@Composable
fun SubtyProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(SubtyBg3),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .background(SubtyMocha),
        )
    }
}

// ── Error / Info banner ───────────────────────────────────────────────────────

@Composable
fun SubtyErrorBanner(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, SubtyError)
            .background(Color(0xFF3D0000))
            .padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        SubtyText(text, fontSize = 12, color = SubtyError)
    }
}

@Composable
fun SubtySuccessBanner(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, SubtyMocha)
            .background(Color(0xFF2A1A00))
            .padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        SubtyText(text, fontSize = 12, color = SubtyMocha)
    }
}

@Composable
fun SubtyWarningBanner(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFFFA500))
            .background(Color(0xFF2A1A00))
            .padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        SubtyText(text, fontSize = 12, color = Color(0xFFFFA500))
    }
}
