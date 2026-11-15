package com.subtranslate.presentation.translate

import android.Manifest
import android.app.DownloadManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.subtranslate.domain.model.TranslationStatus
import com.subtranslate.presentation.theme.LanguagePickerDialogPublic
import com.subtranslate.presentation.theme.SubtyBg
import com.subtranslate.presentation.theme.SubtyBg2
import com.subtranslate.presentation.theme.SubtyBg3
import com.subtranslate.presentation.theme.SubtyBorderDim
import com.subtranslate.presentation.theme.SubtyButton
import com.subtranslate.presentation.theme.SubtyButtonStyle
import com.subtranslate.presentation.theme.SubtyDivider
import com.subtranslate.presentation.theme.SubtyDividerDim
import com.subtranslate.presentation.theme.SubtyErrorBanner
import com.subtranslate.presentation.theme.SubtyLabel
import com.subtranslate.presentation.theme.SubtyMocha
import com.subtranslate.presentation.theme.SubtyProgressBar
import com.subtranslate.presentation.theme.SubtySuccessBanner
import com.subtranslate.presentation.theme.SubtyText
import com.subtranslate.presentation.theme.SubtyText1
import com.subtranslate.presentation.theme.SubtyText3
import com.subtranslate.presentation.theme.SubtyTopBar
import com.subtranslate.presentation.theme.SubtyWarningBanner
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
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var startAfterPermission by remember { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
        // Android permits a foreground translation even when notifications are denied.
        if (startAfterPermission) viewModel.pendingFile?.let(viewModel::startTranslation)
        startAfterPermission = false
    }

    LaunchedEffect(fileId, languageCode) { viewModel.downloadAndLoad(fileId, languageCode) }
    LaunchedEffect(Unit) {
        viewModel.saveDoneEvents.collect { savedName ->
            if (snackbarHostState.showSnackbar("Saved: $savedName", "Open folder") == SnackbarResult.ActionPerformed) {
                val folder = Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload")
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(folder, "vnd.android.document/directory").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION))
                }.onFailure {
                    runCatching { context.startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                }
            }
        }
    }

    Scaffold(
        containerColor = SubtyBg,
        snackbarHost = { SnackbarHost(snackbarHostState) { data -> Snackbar(data, containerColor = SubtyBg2, contentColor = SubtyText1, actionColor = SubtyMocha) } },
        bottomBar = {
            if (!state.isLoadingFile && translateEnabled) {
                TranslateBottomBar(
                    state = state,
                    onStart = {
                        viewModel.pendingFile?.let { file ->
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) viewModel.startTranslation(file)
                            else { startAfterPermission = true; notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                        }
                    },
                    onCancel = viewModel::cancelTranslation,
                    onSave = viewModel::save,
                    enabled = viewModel.pendingFile != null,
                )
            }
        },
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().background(SubtyBg).padding(bottom = innerPadding.calculateBottomPadding())) {
            SubtyTopBar(title = "Translate", onBack = onBack)
            if (!translateEnabled) {
                SubtyWarningBanner(maintenanceMessage.ifBlank { "Translation is temporarily unavailable." }, Modifier.padding(16.dp))
            }
            if (state.isLoadingFile) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(color = SubtyMocha, strokeWidth = 1.5.dp)
                        SubtyText("Loading subtitle…", color = SubtyText3)
                    }
                }
            } else {
                TranslateContent(
                    state = state,
                    onSourceChange = viewModel::onSourceLangChange,
                    onTargetChange = viewModel::onTargetLangChange,
                )
            }
        }
    }
}

@Composable
private fun TranslateContent(
    state: TranslateUiState,
    onSourceChange: (String) -> Unit,
    onTargetChange: (String) -> Unit,
) {
    val translating = state.progress.status == TranslationStatus.TRANSLATING
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Column(Modifier.fillMaxWidth().background(SubtyBg2).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SubtyText("Offline translation", fontSize = 15, weight = FontWeight.Bold)
                SubtyText("Processed on this device. Language models download once and are then reused.", fontSize = 11, color = SubtyText3)
                androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { SubtyLabel("SOURCE", color = SubtyMocha); Spacer(Modifier.height(6.dp)); LanguageSelector(state.sourceLang, GOOGLE_TRANSLATE_LANGUAGES, onSourceChange) }
                        SubtyText("→", Modifier.padding(horizontal = 8.dp), fontSize = 16, color = SubtyMocha, weight = FontWeight.Bold)
                        Column(Modifier.weight(1f)) { SubtyLabel("TARGET", color = SubtyMocha); Spacer(Modifier.height(6.dp)); LanguageSelector(state.targetLang, GOOGLE_TRANSLATE_LANGUAGES, onTargetChange) }
                    }
                }
            }
            SubtyDivider()
        }
        item { SubtyText("Free and on-device", fontSize = 11, color = SubtyText3, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) }
        if (translating) item {
            Column(Modifier.padding(20.dp)) {
                SubtyProgressBar(state.progress.progressFraction)
                Spacer(Modifier.height(6.dp))
                SubtyText("${(state.progress.progressFraction * 100).toInt()}% translated", fontSize = 10, color = SubtyText3, uppercase = true, letterSpacing = 0.06f)
            }
        }
        state.progress.errorMessage?.let { error -> item { SubtyErrorBanner(error, Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) } }
        state.saveError?.let { error -> item { SubtyErrorBanner("Could not save: $error", Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) } }
        state.progress.warningMessage?.let { warning -> item { SubtyWarningBanner(warning, Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) } }
        state.savedPath?.let { path -> item { SubtySuccessBanner("Saved: $path", Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) } }
        state.translatedFile?.let { file ->
            item { SubtyDivider(); SubtyLabel("Translated preview", Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) }
            items(file.entries.take(50)) { entry ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SubtyText(entry.startTimeRaw, fontSize = 10, weight = FontWeight.Bold, color = SubtyMocha, modifier = Modifier.width(90.dp), maxLines = 1)
                    SubtyText(
                        entry.text, fontSize = 13, color = SubtyText1, modifier = Modifier.weight(1f),
                        textDirection = TextDirection.Content,
                        textAlign = if (state.targetLang.substringBefore('-').substringBefore('_').lowercase() in setOf("he", "iw", "ar", "fa", "ur", "yi")) TextAlign.Right else TextAlign.Left,
                    )
                }
                SubtyDividerDim()
            }
        }
    }
}

@Composable
private fun TranslateBottomBar(
    state: TranslateUiState,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    enabled: Boolean,
) {
    val translating = state.progress.status == TranslationStatus.TRANSLATING
    Column(Modifier.fillMaxWidth().background(SubtyBg2)) {
        SubtyDivider()
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (translating) {
                SubtyButton("Cancel", onCancel, style = SubtyButtonStyle.OUTLINE, small = true, modifier = Modifier.fillMaxWidth())
            } else {
                SubtyButton(
                    text = if (state.translatedFile == null) "Translate" else "Translate again",
                    onClick = onStart,
                    style = SubtyButtonStyle.OUTLINE,
                    enabled = enabled,
                    small = true,
                    modifier = Modifier.weight(1f),
                )
                if (state.translatedFile != null) {
                    SubtyButton("Save", onSave, style = SubtyButtonStyle.MOCHA, small = true, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun LanguageSelector(selected: String, options: List<Pair<String, String>>, onSelect: (String) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    Row(
        Modifier.border(1.dp, SubtyBorderDim).background(SubtyBg3).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showDialog = true }.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SubtyText(options.find { it.first == selected }?.second ?: selected, fontSize = 13, weight = FontWeight.Bold, maxLines = 1)
        Icon(Icons.Default.ArrowDropDown, contentDescription = "Choose language", tint = SubtyText3, modifier = Modifier.size(16.dp))
    }
    if (showDialog) LanguagePickerDialogPublic(options, selected, { code -> onSelect(code); showDialog = false }, { showDialog = false })
}
