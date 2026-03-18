package com.subtranslate.presentation.translate

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.subtranslate.domain.model.TranslationStatus
import com.subtranslate.presentation.theme.*
import com.subtranslate.util.GOOGLE_TRANSLATE_LANGUAGES

private const val MODEL_GOOGLE = "google"
private val GEMINI_MODELS = listOf(
    "gemini-1.5-flash" to "Gemini 1.5 Flash",
    "gemini-2.0-flash" to "Gemini 2.0 Flash",
)

@Composable
fun TranslateScreen(
    fileId: Int,
    fileName: String,
    languageCode: String,
    onBack: () -> Unit,
    viewModel: TranslateViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val progress = state.progress

    LaunchedEffect(fileId, languageCode) {
        viewModel.downloadAndLoad(fileId, languageCode)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SubtyBg),
    ) {
        SubtyTopBar(title = "Translate", onBack = onBack)

        if (state.isLoadingFile) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(color = SubtyMocha, strokeWidth = 1.5.dp)
                    SubtyText("Loading subtitle…", color = SubtyText3)
                }
            }
        } else LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            // ── Language + Engine selector ────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(width = 0.dp, color = SubtyBg) // just for structure
                        .background(SubtyBg2)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    // Source / Target row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Source
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End,
                        ) {
                            SubtyLabel("ממקור", color = SubtyMocha)
                            Spacer(Modifier.height(6.dp))
                            LanguageSelector(
                                selected = state.sourceLang,
                                options = GOOGLE_TRANSLATE_LANGUAGES,
                                onSelect = viewModel::onSourceLangChange,
                            )
                        }

                        // Arrow
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .size(32.dp)
                                .border(1.dp, SubtyBorderDim)
                                .background(SubtyBg3),
                            contentAlignment = Alignment.Center,
                        ) {
                            SubtyText("→", fontSize = 14, color = SubtyMocha, weight = FontWeight.Bold)
                        }

                        // Target
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.Start,
                        ) {
                            SubtyLabel("ליעד", color = SubtyMocha)
                            Spacer(Modifier.height(6.dp))
                            LanguageSelector(
                                selected = state.targetLang,
                                options = GOOGLE_TRANSLATE_LANGUAGES,
                                onSelect = viewModel::onTargetLangChange,
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    SubtyDividerDim()
                    Spacer(Modifier.height(20.dp))

                    // Engine row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        val useGemini = state.selectedModel.startsWith("gemini")
                        Column(modifier = Modifier.weight(1f)) {
                            SubtyText(
                                if (useGemini) "Gemini AI" else "Google Translate",
                                fontSize = 13,
                                weight = FontWeight.Bold,
                                color = SubtyText1,
                            )
                            Spacer(Modifier.height(2.dp))
                            SubtyText(
                                if (useGemini) "תרגום חכם מבוסס הקשר" else "תרגום מהיר וחינמי",
                                fontSize = 11,
                                color = SubtyText3,
                            )
                        }
                        SubtySwitch(
                            checked = useGemini,
                            onCheckedChange = { on ->
                                viewModel.onModelChange(if (on) "gemini-1.5-flash" else MODEL_GOOGLE)
                            },
                        )
                    }

                    // Gemini model selector
                    AnimatedVisibility(visible = state.selectedModel.startsWith("gemini")) {
                        var expanded by remember { mutableStateOf(false) }
                        Column {
                            Spacer(Modifier.height(12.dp))
                            Box {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, SubtyBorderDim)
                                        .background(SubtyBg3)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                        ) { expanded = true }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    SubtyText(
                                        GEMINI_MODELS.find { it.first == state.selectedModel }?.second
                                            ?: "Gemini 1.5 Flash",
                                        fontSize = 13,
                                        color = SubtyText1,
                                    )
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = SubtyText3,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                ) {
                                    GEMINI_MODELS.forEach { (id, label) ->
                                        DropdownMenuItem(
                                            text = { SubtyText(label, fontSize = 13) },
                                            onClick = { viewModel.onModelChange(id); expanded = false },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                SubtyDivider()
            }

            // ── Start button ──────────────────────────────────────────────────
            item {
                val isTranslating = progress.status == TranslationStatus.TRANSLATING
                SubtyButton(
                    text = when {
                        isTranslating -> "מתרגם כרגע…"
                        state.translatedFile != null -> "תרגם מחדש"
                        else -> "התחל תרגום"
                    },
                    onClick = {
                        val file = viewModel.pendingFile
                        if (file != null) viewModel.startTranslation(file)
                    },
                    style = SubtyButtonStyle.FILLED,
                    enabled = !isTranslating && viewModel.pendingFile != null,
                    loading = isTranslating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                )
            }

            // ── Progress bar ──────────────────────────────────────────────────
            if (progress.status == TranslationStatus.TRANSLATING) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                        SubtyProgressBar(progress = progress.progressFraction)
                        Spacer(Modifier.height(6.dp))
                        SubtyText(
                            "${(progress.progressFraction * 100).toInt()}% translated",
                            fontSize = 10,
                            color = SubtyText3,
                            uppercase = true,
                            letterSpacing = 0.06f,
                        )
                    }
                }
            }

            // ── Error ─────────────────────────────────────────────────────────
            progress.errorMessage?.let { err ->
                item {
                    SubtyErrorBanner(
                        err,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                }
            }

            // ── Saved path ────────────────────────────────────────────────────
            state.savedPath?.let { path ->
                item {
                    SubtySuccessBanner(
                        "Saved: $path",
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                }
            }

            // ── Save FAB-equivalent ───────────────────────────────────────────
            if (progress.status == TranslationStatus.COMPLETE && state.translatedFile != null) {
                item {
                    SubtyButton(
                        text = "Save",
                        onClick = viewModel::save,
                        style = SubtyButtonStyle.MOCHA,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                    )
                }
            }

            // ── Translated preview ────────────────────────────────────────────
            if (state.translatedFile != null) {
                item {
                    Spacer(Modifier.height(8.dp))
                    SubtyDivider()
                    SubtyLabel(
                        "Translated preview",
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    )
                }

                items(state.translatedFile!!.entries.take(50)) { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SubtyText(
                            entry.startTimeRaw,
                            fontSize = 10,
                            weight = FontWeight.Bold,
                            color = SubtyMocha,
                            modifier = Modifier.width(52.dp),
                        )
                        SubtyText(
                            entry.text,
                            fontSize = 13,
                            color = SubtyText1,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    SubtyDividerDim()
                }

                if (state.translatedFile!!.entries.size > 50) {
                    item {
                        SubtyText(
                            "… and ${state.translatedFile!!.entries.size - 50} more lines",
                            fontSize = 10,
                            color = SubtyText3,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelector(
    selected: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .border(1.dp, SubtyBorderDim)
                .background(SubtyBg3)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { expanded = true }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SubtyText(
                options.find { it.first == selected }?.second ?: selected,
                fontSize = 13,
                weight = FontWeight.Bold,
                color = SubtyText1,
            )
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = SubtyText3,
                modifier = Modifier.size(16.dp),
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
