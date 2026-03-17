package com.subtranslate.presentation.translate

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.subtranslate.domain.model.TranslationStatus

private val LANGUAGES = listOf(
    "en" to "English", "he" to "עברית", "fr" to "Français", "de" to "Deutsch",
    "es" to "Español", "ar" to "العربية", "ru" to "Русский",
    "zh-cn" to "中文", "ja" to "日本語", "pt" to "Português"
)

private val MODELS = listOf(
    "claude-sonnet-4-5" to "Claude Sonnet (Quality)",
    "claude-haiku-4-5-20251001" to "Claude Haiku (Fast)"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslateScreen(
    onBack: () -> Unit,
    viewModel: TranslateViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val progress = state.progress

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Translate") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        },
        floatingActionButton = {
            if (progress.status == TranslationStatus.COMPLETE && state.translatedFile != null) {
                FloatingActionButton(onClick = viewModel::save) {
                    Icon(Icons.Default.Save, "Save")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Source / Target language pickers
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LanguageDropdown(
                        label = "From",
                        selected = state.sourceLang,
                        options = LANGUAGES,
                        onSelect = viewModel::onSourceLangChange,
                        modifier = Modifier.weight(1f)
                    )
                    LanguageDropdown(
                        label = "To",
                        selected = state.targetLang,
                        options = LANGUAGES,
                        onSelect = viewModel::onTargetLangChange,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                // Model selector
                var modelExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = modelExpanded, onExpandedChange = { modelExpanded = it }) {
                    OutlinedTextField(
                        value = MODELS.find { it.first == state.selectedModel }?.second ?: state.selectedModel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Model") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = modelExpanded, onDismissRequest = { modelExpanded = false }) {
                        MODELS.forEach { (id, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = {
                                viewModel.onModelChange(id)
                                modelExpanded = false
                            })
                        }
                    }
                }
            }

            item {
                // Start / Cancel button
                val canStart = progress.status == TranslationStatus.IDLE ||
                        progress.status == TranslationStatus.ERROR
                Button(
                    onClick = {
                        val file = viewModel.pendingFile
                        if (canStart && file != null) viewModel.startTranslation(file)
                        else if (!canStart) viewModel.cancelTranslation()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = if (!canStart) ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ) else ButtonDefaults.buttonColors()
                ) {
                    Text(if (canStart) "Start Translation" else "Cancel")
                }
            }

            // Progress
            if (progress.status == TranslationStatus.TRANSLATING) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LinearProgressIndicator(
                            progress = { progress.progressFraction },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "Batch ${progress.currentBatch}/${progress.totalBatches} · " +
                                    "${progress.translatedEntries}/${progress.totalEntries} lines",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            // Error
            progress.errorMessage?.let { err ->
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Text(err, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }

            // Saved confirmation
            state.savedPath?.let { path ->
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Text("Saved to: $path", Modifier.padding(12.dp))
                    }
                }
            }

            // Live translated preview
            AnimatedVisibility(visible = progress.status == TranslationStatus.COMPLETE &&
                    state.translatedFile != null) {
                Column {
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    Text("Translated preview", style = MaterialTheme.typography.titleMedium)
                }
            }

            state.translatedFile?.entries?.let { entries ->
                items(entries.take(50)) { entry ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            entry.startTimeRaw,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(80.dp)
                        )
                        Text(entry.text, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    }
                }
                if (entries.size > 50) {
                    item { Text("… and ${entries.size - 50} more lines", style = MaterialTheme.typography.labelSmall) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageDropdown(
    label: String,
    selected: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
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
