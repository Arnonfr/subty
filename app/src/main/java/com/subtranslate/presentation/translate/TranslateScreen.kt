package com.subtranslate.presentation.translate

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import android.app.DownloadManager
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.subtranslate.domain.model.TranslationStatus
import com.subtranslate.presentation.theme.*
import com.subtranslate.util.GOOGLE_TRANSLATE_LANGUAGES

@Composable
fun TranslateScreen(
    fileId: Int,
    fileName: String,
    languageCode: String,
    onBack: () -> Unit,
    translateEnabled: Boolean = true,
    maintenanceMessage: String = "",
    viewModel: TranslateViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val progress = state.progress
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showApiKeyDialog by remember { mutableStateOf(false) }

    LaunchedEffect(fileId, languageCode) {
        viewModel.downloadAndLoad(fileId, languageCode)
    }

    LaunchedEffect(Unit) {
        viewModel.saveDoneEvents.collect { savedName ->
            val result = snackbarHostState.showSnackbar(
                message = "Saved: $savedName",
                actionLabel = "Open Folder",
            )
            if (result == SnackbarResult.ActionPerformed) {
                // Open Downloads folder — try document tree URI first, fallback to generic
                val docUri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload")
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(docUri, "vnd.android.document/directory")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                runCatching { context.startActivity(intent) }.onFailure {
                    // fallback
                    runCatching {
                        context.startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                    }
                }
            }
        }
    }

    Scaffold(
        containerColor = SubtyBg,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = SubtyBg2,
                    contentColor = SubtyText1,
                    actionColor = SubtyMocha,
                )
            }
        }
    ) { innerPadding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SubtyBg)
            .padding(bottom = innerPadding.calculateBottomPadding()),
    ) {
        SubtyTopBar(title = "Translate", onBack = onBack)

        // ── Feature-disabled banner ────────────────────────────────────────────
        if (!translateEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SubtyMocha)
                    .padding(12.dp),
                contentAlignment = Alignment.Center,
            ) {
                SubtyText(
                    text = maintenanceMessage.ifBlank { "Translation is temporarily unavailable." },
                    color = androidx.compose.ui.graphics.Color.White,
                    fontSize = 13,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

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
                    // Source / Target row — forced LTR so layout never flips on RTL devices
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Source (always left)
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.Start,
                            ) {
                                SubtyLabel("SOURCE", color = SubtyMocha)
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

                            // Target (always right)
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.End,
                            ) {
                                SubtyLabel("TARGET", color = SubtyMocha)
                                Spacer(Modifier.height(6.dp))
                                LanguageSelector(
                                    selected = state.targetLang,
                                    options = GOOGLE_TRANSLATE_LANGUAGES,
                                    onSelect = viewModel::onTargetLangChange,
                                )
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
                        isTranslating -> "Translating…"
                        state.translatedFile != null -> "Re-translate"
                        else -> "Start translation"
                    },
                    onClick = {
                        if (!viewModel.hasTranslateApiKey()) {
                            showApiKeyDialog = true
                        } else {
                            val file = viewModel.pendingFile
                            if (file != null) viewModel.startTranslation(file)
                        }
                    },
                    style = SubtyButtonStyle.FILLED,
                    enabled = translateEnabled && !isTranslating && viewModel.pendingFile != null,
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
    } // end Column

    if (showApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = { androidx.compose.material3.Text("Google Translate API Key Required") },
            text = { androidx.compose.material3.Text("To translate subtitles, you need a free Google Translate API key. Go to Settings to enter yours.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showApiKeyDialog = false
                    onBack()
                }) {
                    androidx.compose.material3.Text("Go to Settings")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showApiKeyDialog = false }) {
                    androidx.compose.material3.Text("Cancel")
                }
            },
        )
    }
    } // end Scaffold
}

@Composable
fun LanguageSelector(
    selected: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    val displayName = options.find { it.first == selected }?.second ?: selected

    Row(
        modifier = Modifier
            .border(1.dp, SubtyBorderDim)
            .background(SubtyBg3)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { showDialog = true }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SubtyText(
            displayName,
            fontSize = 13,
            weight = FontWeight.Bold,
            color = SubtyText1,
            maxLines = 1,
        )
        Icon(
            Icons.Default.ArrowDropDown,
            contentDescription = null,
            tint = SubtyText3,
            modifier = Modifier.size(16.dp),
        )
    }

    if (showDialog) {
        com.subtranslate.presentation.theme.LanguagePickerDialogPublic(
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
