package com.subtranslate.presentation.translate

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.subtranslate.domain.model.TranslationStatus
import androidx.compose.foundation.layout.Box

private val LANGUAGES = listOf(
    "en" to "English", "he" to "עברית", "fr" to "Français", "de" to "Deutsch",
    "es" to "Español", "ar" to "العربية", "ru" to "Русский",
    "zh-cn" to "中文", "ja" to "日本語", "pt" to "Português"
)

// "google" = Google Translate (default, free). Gemini models start with "gemini-".
private const val MODEL_GOOGLE = "google"
private val GEMINI_MODELS = listOf(
    "gemini-2.5-flash" to "Gemini Flash",
    "gemini-2.5-pro" to "Gemini Pro"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslateScreen(
    fileId: Int,
    fileName: String,
    languageCode: String,
    onBack: () -> Unit,
    viewModel: TranslateViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val progress = state.progress

    // Load the subtitle and auto-set source language once
    LaunchedEffect(fileId, languageCode) {
        viewModel.downloadAndLoad(fileId, languageCode)
    }

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
        // Show loading spinner while subtitle file is being downloaded
        if (state.isLoadingFile) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator()
                    Text("Loading subtitle…", style = MaterialTheme.typography.bodyMedium)
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Source language: auto-detected (read-only) → arrow → target language picker
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Detected source — display only, no dropdown
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Detected language",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Text(
                                text = LANGUAGES.find { it.first == state.sourceLang }?.second
                                    ?: state.sourceLang.uppercase(),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 18.dp)
                    )
                    LanguageDropdown(
                        label = "Translate to",
                        selected = state.targetLang,
                        options = LANGUAGES,
                        onSelect = viewModel::onTargetLangChange,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                // Engine: Google Translate (default) with optional Gemini upgrade
                val useGemini = state.selectedModel.startsWith("gemini")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            if (useGemini) "Gemini AI" else "Google Translate",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            if (useGemini) "Context-aware · slower" else "Fast · free",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = useGemini,
                        onCheckedChange = { on ->
                            viewModel.onModelChange(if (on) "gemini-2.5-flash" else MODEL_GOOGLE)
                        }
                    )
                }
                // Gemini model sub-picker — only shown when Gemini is on
                AnimatedVisibility(visible = useGemini) {
                    var geminiExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = geminiExpanded,
                        onExpandedChange = { geminiExpanded = it },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = GEMINI_MODELS.find { it.first == state.selectedModel }?.second
                                ?: "Gemini Flash",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Gemini model") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = geminiExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = geminiExpanded,
                            onDismissRequest = { geminiExpanded = false }
                        ) {
                            GEMINI_MODELS.forEach { (id, label) ->
                                DropdownMenuItem(text = { Text(label) }, onClick = {
                                    viewModel.onModelChange(id)
                                    geminiExpanded = false
                                })
                            }
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
            item {
                AnimatedVisibility(visible = progress.status == TranslationStatus.COMPLETE &&
                        state.translatedFile != null) {
                    Column {
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))
                        Text("Translated preview", style = MaterialTheme.typography.titleMedium)
                    }
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
