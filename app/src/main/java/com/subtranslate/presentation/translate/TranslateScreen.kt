package com.subtranslate.presentation.translate

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.subtranslate.domain.model.TranslationStatus
import com.subtranslate.util.GOOGLE_TRANSLATE_LANGUAGES

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
                // Unified Premium Engine & Language Selector Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = if (state.selectedModel.startsWith("gemini"))
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                    else
                        MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Language Selection Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Source Selector
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "ממקור",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(4.dp))
                                LanguageDropdownMinimal(
                                    selected = state.sourceLang,
                                    options = GOOGLE_TRANSLATE_LANGUAGES,
                                    onSelect = viewModel::onSourceLangChange
                                )
                            }

                            // Elegant Glass Divider / Arrow
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Target Selector
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "ליעד",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(4.dp))
                                LanguageDropdownMinimal(
                                    selected = state.targetLang,
                                    options = GOOGLE_TRANSLATE_LANGUAGES,
                                    onSelect = viewModel::onTargetLangChange
                                )
                            }
                        }

                        Spacer(Modifier.height(24.dp)) // Separator for engine selection

                        // Engine Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val useGemini = state.selectedModel.startsWith("gemini")
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (useGemini) "Gemini AI" else "Google Translate",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    if (useGemini) "תרגום חכם מבוסס הקשר" else "תרגום מהיר וחינמי",
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

                        // Gemini Model Options
                        val isGemini = state.selectedModel.startsWith("gemini")
                        AnimatedVisibility(visible = isGemini) {
                            Column {
                                Spacer(Modifier.height(12.dp))
                                var geminiExpanded by remember { mutableStateOf(false) }
                                ExposedDropdownMenuBox(
                                    expanded = geminiExpanded,
                                    onExpandedChange = { geminiExpanded = it },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = GEMINI_MODELS.find { it.first == state.selectedModel }?.second ?: "Gemini Flash",
                                        onValueChange = {},
                                        readOnly = true,
                                        shape = RoundedCornerShape(16.dp),
                                        textStyle = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = geminiExpanded) },
                                        modifier = Modifier.menuAnchor().fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = geminiExpanded,
                                        onDismissRequest = { geminiExpanded = false }
                                    ) {
                                        GEMINI_MODELS.forEach { (id, label) ->
                                            DropdownMenuItem(
                                                text = { Text(label, style = MaterialTheme.typography.bodySmall) },
                                                onClick = {
                                                    viewModel.onModelChange(id)
                                                    geminiExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                // Start / Action Button (Premium Pulse/Gradient)
                val isTranslating = progress.status == TranslationStatus.TRANSLATING
                
                Button(
                    onClick = {
                        val file = viewModel.pendingFile
                        if (file != null) {
                            viewModel.startTranslation(file)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 4.dp),
                    enabled = !isTranslating && (viewModel.pendingFile != null),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isTranslating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("מתרגם כרגע...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                if (state.translatedFile != null) Icons.Default.Refresh else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                if (state.translatedFile != null) "תרגם מחדש" else "התחל תרגום",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            if (progress.status == TranslationStatus.TRANSLATING) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LinearProgressIndicator(
                            progress = { progress.progressFraction },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Translating... ${(progress.progressFraction * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            progress.errorMessage?.let { err ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = err,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Saved confirmation
            state.savedPath?.let { path ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Saved to: $path", Modifier.padding(12.dp))
                    }
                }
            }

            // Live translated preview
            if (state.translatedFile != null) {
                item {
                    Column {
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))
                        Text("Translated preview", style = MaterialTheme.typography.titleMedium)
                    }
                }

                items(state.translatedFile!!.entries.take(50)) { entry ->
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
                        Text(
                            entry.text,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (state.translatedFile!!.entries.size > 50) {
                    item {
                        Text(
                            "… and ${state.translatedFile!!.entries.size - 50} more lines",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageDropdownMinimal(
    selected: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
            modifier = Modifier.height(44.dp).padding(horizontal = 4.dp)
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = options.find { it.first == selected }?.second ?: selected,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (code, name) ->
                DropdownMenuItem(
                    text = { Text(name, style = MaterialTheme.typography.bodyMedium) },
                    onClick = { onSelect(code); expanded = false }
                )
            }
        }
    }
}
