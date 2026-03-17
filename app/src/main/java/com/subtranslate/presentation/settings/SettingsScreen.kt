package com.subtranslate.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

private val LANGUAGES = listOf(
    "he" to "עברית", "en" to "English", "fr" to "Français", "de" to "Deutsch",
    "es" to "Español", "ar" to "العربية", "ru" to "Русский",
    "zh-cn" to "中文", "ja" to "日本語", "pt" to "Português"
)

private val MODELS = listOf(
    "gemini-2.5-flash" to "Gemini Flash — Fast & Cheap",
    "gemini-2.5-pro" to "Gemini Pro — Best Quality"
)

private val FORMATS = listOf("srt" to "SRT", "vtt" to "VTT", "ass" to "ASS")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.titleLarge)

        // ── Display ──────────────────────────────────────────────────────────
        Card {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Display", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                ToggleRow(
                    label = "Show movie posters",
                    description = "Display cover art in search results",
                    checked = state.showPosters,
                    onCheckedChange = viewModel::onShowPostersChange
                )
                HorizontalDivider()
                ToggleRow(
                    label = "Compact results",
                    description = "Smaller cards, more results on screen",
                    checked = state.compactResults,
                    onCheckedChange = viewModel::onCompactResultsChange
                )
            }
        }

        // ── Translation ───────────────────────────────────────────────────────
        Card {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Translation", style = MaterialTheme.typography.titleMedium)

                SimpleDropdown(
                    label = "Default target language",
                    selected = state.defaultTargetLang,
                    options = LANGUAGES,
                    onSelect = viewModel::onTargetLangChange
                )

                SimpleDropdown(
                    label = "Model",
                    selected = state.translationModel,
                    options = MODELS,
                    onSelect = viewModel::onModelChange
                )

                ToggleRow(
                    label = "Auto-translate on download",
                    description = "Start translation immediately after downloading",
                    checked = state.autoTranslate,
                    onCheckedChange = viewModel::onAutoTranslateChange
                )
            }
        }

        // ── Save ─────────────────────────────────────────────────────────────
        Card {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Save", style = MaterialTheme.typography.titleMedium)

                SimpleDropdown(
                    label = "Preferred format",
                    selected = state.preferredSaveFormat,
                    options = FORMATS,
                    onSelect = viewModel::onSaveFormatChange
                )

                ToggleRow(
                    label = "Auto-save translated files",
                    description = "Save to Downloads automatically when done",
                    checked = state.autoSave,
                    onCheckedChange = viewModel::onAutoSaveChange
                )
            }
        }

        Button(
            onClick = viewModel::save,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save")
        }

        if (state.saved) {
            Text("Saved!", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleDropdown(
    label: String,
    selected: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = options.find { it.first == selected }?.second ?: selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (code, name) ->
                DropdownMenuItem(text = { Text(name) }, onClick = { onSelect(code); expanded = false })
            }
        }
    }
}
