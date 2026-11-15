package com.subtranslate.presentation.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.subtranslate.presentation.theme.SubtyBg
import com.subtranslate.presentation.theme.SubtyButton
import com.subtranslate.presentation.theme.SubtyButtonStyle
import com.subtranslate.presentation.theme.SubtyLanguagePickerRow
import com.subtranslate.presentation.theme.SubtyLabel
import com.subtranslate.presentation.theme.SubtySegmentChip
import com.subtranslate.presentation.theme.SubtySwitch
import com.subtranslate.presentation.theme.SubtyText
import com.subtranslate.presentation.theme.SubtyText3
import com.subtranslate.presentation.theme.SubtyTopBar
import com.subtranslate.util.GOOGLE_TRANSLATE_LANGUAGES

private val FORMATS = listOf("srt" to "SRT", "vtt" to "VTT", "ass" to "ASS")

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            runCatching { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION) }
            viewModel.onSaveDestinationModeChange("custom")
            viewModel.onCustomSaveFolderUriChange(it.toString())
        }
    }
    LaunchedEffect(Unit) { viewModel.refreshOfflineLanguages() }

    Column(Modifier.fillMaxSize().background(SubtyBg)) {
        SubtyTopBar(title = "Settings")
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            SettingsSection("Translation") {
                SubtyText("Offline translation", fontSize = 16)
                SubtyText("Translations are processed on this device. Download a language model once, then use it without an internet connection.", fontSize = 11, color = SubtyText3)
                SubtyLanguagePickerRow("Default target language", state.defaultTargetLang, GOOGLE_TRANSLATE_LANGUAGES, viewModel::onTargetLangChange)
                SubtyLabel("Offline languages")
                listOf("en" to "English", "he" to "Hebrew", "es" to "Spanish", "fr" to "French").forEach { (code, name) ->
                    val downloaded = code in state.downloadedOfflineLanguages
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        SubtyText("$name  ${if (downloaded) "Downloaded" else "Not downloaded"}", fontSize = 12)
                        SubtyButton(if (downloaded) "Remove" else "Download", { if (downloaded) viewModel.removeOfflineLanguage(code) else viewModel.downloadOfflineLanguage(code) }, style = SubtyButtonStyle.OUTLINE, small = true)
                    }
                }
            }
            SettingsSection("Output") {
                SubtyLabel("Save destination")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SubtySegmentChip("Movies", state.saveDestinationMode == "movies", { viewModel.onSaveDestinationModeChange("movies") }, Modifier.weight(1f))
                    SubtySegmentChip("Custom", state.saveDestinationMode == "custom", { viewModel.onSaveDestinationModeChange("custom") }, Modifier.weight(1f))
                }
                SubtyText(if (state.saveDestinationMode == "custom" && !state.customSaveFolderUri.isNullOrBlank()) "Custom folder selected." else "Default path: Movies/Subty on this device.", fontSize = 11, color = SubtyText3)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SubtyButton("Choose folder", { viewModel.onSaveDestinationModeChange("custom"); folderPicker.launch(null) }, style = SubtyButtonStyle.OUTLINE, small = true, modifier = Modifier.weight(1f))
                    SubtyButton("Use Movies", { viewModel.onSaveDestinationModeChange("movies") }, style = SubtyButtonStyle.FILLED, small = true, modifier = Modifier.weight(1f))
                }
                SubtyLabel("Preferred save format")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FORMATS.forEach { (id, name) -> SubtySegmentChip(name, state.preferredSaveFormat == id, { viewModel.onSaveFormatChange(id) }, Modifier.weight(1f)) }
                }
            }
            SettingsSection("Behavior") {
                ToggleRow("Auto translate after download", state.autoTranslate, viewModel::onAutoTranslateChange)
                ToggleRow("Auto save translated files", state.autoSave, viewModel::onAutoSaveChange)
                ToggleRow("Dark mode", state.isDarkTheme, viewModel::onDarkThemeChange)
                ToggleRow("Show posters", state.showPosters, viewModel::onShowPostersChange)
                ToggleRow("Compact results", state.compactResults, viewModel::onCompactResultsChange)
                ToggleRow("Use season / episode fields", state.useSeasonEpisodeTextFields, viewModel::onUseSeasonEpisodeTextFieldsChange)
            }
            SettingsSection("Privacy") {
                SubtyText("Help improve Subty with anonymous usage events. Subtitle text, files, and keys are never sent.", fontSize = 11, color = SubtyText3)
                ToggleRow("Share anonymous usage data", state.liveAnalyticsEnabled, viewModel::onLiveAnalyticsEnabledChange)
                if (state.liveAnalyticsEnabled) {
                    SubtyText("Search words are optional and let us see what people look for.", fontSize = 11, color = SubtyText3)
                    ToggleRow("Include search words", state.shareSearchTerms, viewModel::onShareSearchTermsChange)
                }
            }
            SubtyButton(if (state.saved) "Saved" else "Save changes", viewModel::save, style = SubtyButtonStyle.FILLED, small = true, modifier = Modifier.fillMaxWidth())
            SubtyText(if (state.saved) "Settings saved locally." else "Changes are stored locally on this device.", fontSize = 11, color = SubtyText3)
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SubtyText(title, fontSize = 16, uppercase = true, letterSpacing = 0.04f)
        content()
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        SubtyText(label, fontSize = 13)
        SubtySwitch(checked, onCheckedChange)
    }
}
