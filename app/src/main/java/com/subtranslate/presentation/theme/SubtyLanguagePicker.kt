package com.subtranslate.presentation.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

/**
 * A full-screen searchable language picker dialog.
 * Shows a search field + scrollable list of flag + language name items.
 * Dismisses and calls [onSelect] when a language is chosen.
 *
 * @param label      Row label shown in the trigger row (e.g. "Default target language")
 * @param selected   Currently selected language code (e.g. "he")
 * @param options    List of (code, "🏳 Display Name") pairs
 * @param onSelect   Called with the chosen code
 */
@Composable
fun SubtyLanguagePickerRow(
    label: String,
    selected: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }
    val displayName = options.find { it.first == selected }?.second ?: selected

    // Trigger row — same visual style as old SettingsDropdown
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { showDialog = true }
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            SubtyLabel(label)
            Spacer(Modifier.height(4.dp))
            SubtyText(displayName, fontSize = 14, weight = FontWeight.Medium, color = SubtyText1)
        }
        Icon(
            Icons.Default.ArrowDropDown,
            contentDescription = null,
            tint = SubtyText3,
            modifier = Modifier.size(20.dp),
        )
    }

    if (showDialog) {
        LanguagePickerDialogPublic(
            options = options,
            selected = selected,
            onSelect = { code ->
                onSelect(code)
                showDialog = false
            },
            onDismiss = { showDialog = false },
        )
    }
}

@Composable
fun LanguagePickerDialogPublic(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, options) {
        if (query.isBlank()) options
        else options.filter { (code, name) ->
            name.contains(query, ignoreCase = true) || code.contains(query, ignoreCase = true)
        }
    }
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }

    // Scroll to selected item when dialog opens
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        val idx = options.indexOfFirst { it.first == selected }.coerceAtLeast(0)
        if (idx > 0) listState.scrollToItem(idx)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SubtyBg),
        ) {
            // ── Header ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(SubtyBg)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onDismiss() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = SubtyText1,
                        modifier = Modifier.size(20.dp),
                    )
                }
                SubtyText(
                    "Select Language",
                    fontSize = 13,
                    weight = FontWeight.Bold,
                    letterSpacing = 0.06f,
                    uppercase = true,
                    color = SubtyText1,
                )
            }
            SubtyDivider()

            // ── Search field ────────────────────────────────────────────────
            SubtyTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Search languages…",
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .focusRequester(focusRequester),
            )

            // ── Language list ────────────────────────────────────────────────
            LazyColumn(state = listState) {
                items(filtered, key = { it.first }) { (code, name) ->
                    val isSelected = code == selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isSelected) SubtyBg2 else SubtyBg)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { onSelect(code) }
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        SubtyText(
                            text = name,
                            fontSize = 14,
                            weight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) SubtyMocha else SubtyText1,
                            modifier = Modifier.weight(1f),
                        )
                        if (isSelected) {
                            SubtyText("✓", fontSize = 14, weight = FontWeight.Bold, color = SubtyMocha)
                        }
                    }
                    SubtyDividerDim()
                }
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}
