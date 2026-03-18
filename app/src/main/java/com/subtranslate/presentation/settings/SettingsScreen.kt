package com.subtranslate.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.subtranslate.presentation.theme.*
import com.subtranslate.util.GOOGLE_TRANSLATE_LANGUAGES

private val MODELS = listOf(
    "google"          to "Google Translate",
    "gemini-1.5-flash" to "Gemini 1.5 Flash",
    "gemini-2.0-flash" to "Gemini 2.0 Flash",
)

private val FORMATS = listOf("srt" to "SRT", "vtt" to "VTT", "ass" to "ASS")

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SubtyBg)
            .verticalScroll(rememberScrollState()),
    ) {
        SubtyPageTitle(
            "Settings",
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 4.dp),
        )

        Spacer(Modifier.height(16.dp))

        // ── Display ───────────────────────────────────────────────────────────
        SubtyLabel(
            "Display",
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        SubtyDivider()
        SubtyToggleRow(
            label = "Show movie posters",
            description = "Display cover art in search results",
            checked = state.showPosters,
            onCheckedChange = viewModel::onShowPostersChange,
        )
        SubtyDividerDim()
        SubtyToggleRow(
            label = "Compact results",
            description = "Smaller cards, more results on screen",
            checked = state.compactResults,
            onCheckedChange = viewModel::onCompactResultsChange,
        )
        SubtyDivider()

        Spacer(Modifier.height(16.dp))

        // ── Translation ───────────────────────────────────────────────────────
        SubtyLabel(
            "Translation",
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        SubtyDivider()
        SettingsDropdown(
            label = "Default target language",
            selected = state.defaultTargetLang,
            options = GOOGLE_TRANSLATE_LANGUAGES,
            onSelect = viewModel::onTargetLangChange,
        )
        SubtyDividerDim()
        SettingsDropdown(
            label = "Model",
            selected = state.translationModel,
            options = MODELS,
            onSelect = viewModel::onModelChange,
        )
        SubtyDividerDim()
        SubtyToggleRow(
            label = "Auto-translate on download",
            description = "Start translation immediately after downloading",
            checked = state.autoTranslate,
            onCheckedChange = viewModel::onAutoTranslateChange,
        )
        SubtyDivider()

        Spacer(Modifier.height(16.dp))

        // ── Save ──────────────────────────────────────────────────────────────
        SubtyLabel(
            "Save",
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        SubtyDivider()
        SettingsDropdown(
            label = "Preferred format",
            selected = state.preferredSaveFormat,
            options = FORMATS,
            onSelect = viewModel::onSaveFormatChange,
        )
        SubtyDividerDim()
        SubtyToggleRow(
            label = "Auto-save translated files",
            description = "Save to Downloads automatically when done",
            checked = state.autoSave,
            onCheckedChange = viewModel::onAutoSaveChange,
        )
        SubtyDivider()

        Spacer(Modifier.height(24.dp))

        SubtyButton(
            text = "Save",
            onClick = viewModel::save,
            style = SubtyButtonStyle.FILLED,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        )

        if (state.saved) {
            Spacer(Modifier.height(12.dp))
            SubtySuccessBanner(
                "Settings saved",
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsDropdown(
    label: String,
    selected: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { expanded = true }
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                SubtyLabel(label)
                Spacer(Modifier.height(4.dp))
                SubtyText(
                    options.find { it.first == selected }?.second ?: selected,
                    fontSize = 14,
                    weight = FontWeight.Medium,
                    color = SubtyText1,
                )
            }
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = SubtyText3,
                modifier = Modifier.size(20.dp),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { (code, name) ->
                DropdownMenuItem(
                    text = { SubtyText(name, fontSize = 13) },
                    onClick = { onSelect(code); expanded = false },
                )
            }
        }
    }
}
