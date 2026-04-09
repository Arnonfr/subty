package com.subtranslate.presentation.settings

import androidx.lifecycle.ViewModel
import com.subtranslate.data.local.datastore.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class SettingsUiState(
    // Translation
    val defaultTargetLang: String = "he",
    val translationModel: String = "google",
    val autoTranslate: Boolean = false,
    // Display
    val showPosters: Boolean = true,
    val compactResults: Boolean = false,
    val isDarkTheme: Boolean = true,
    // Save
    val preferredSaveFormat: String = "srt",
    val autoSave: Boolean = false,
    // API Keys
    val googleTranslateApiKey: String = "",
    val geminiApiKey: String = "",
    val saved: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            defaultTargetLang = settings.defaultTargetLanguage,
            translationModel = settings.translationModel,
            autoTranslate = settings.autoTranslateAfterDownload,
            showPosters = settings.showPosters,
            compactResults = settings.compactResults,
            isDarkTheme = settings.isDarkTheme,
            preferredSaveFormat = settings.preferredSaveFormat,
            autoSave = settings.autoSaveTranslated,
            googleTranslateApiKey = settings.googleTranslateApiKey ?: "",
            geminiApiKey = settings.geminiApiKey ?: "",
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState

    fun onTargetLangChange(v: String) = update { copy(defaultTargetLang = v, saved = false) }
    fun onModelChange(v: String) = update { copy(translationModel = v, saved = false) }
    fun onAutoTranslateChange(v: Boolean) = update { copy(autoTranslate = v, saved = false) }
    fun onShowPostersChange(v: Boolean) = update { copy(showPosters = v, saved = false) }
    fun onCompactResultsChange(v: Boolean) = update { copy(compactResults = v, saved = false) }
    fun onDarkThemeChange(v: Boolean) {
        settings.isDarkTheme = v
        update { copy(isDarkTheme = v, saved = false) }
    }
    fun onSaveFormatChange(v: String) = update { copy(preferredSaveFormat = v, saved = false) }
    fun onAutoSaveChange(v: Boolean) = update { copy(autoSave = v, saved = false) }
    fun onGoogleTranslateApiKeyChange(v: String) = update { copy(googleTranslateApiKey = v, saved = false) }
    fun onGeminiApiKeyChange(v: String) = update { copy(geminiApiKey = v, saved = false) }

    fun save() {
        val s = _uiState.value
        settings.defaultTargetLanguage = s.defaultTargetLang
        settings.translationModel = s.translationModel
        settings.autoTranslateAfterDownload = s.autoTranslate
        settings.showPosters = s.showPosters
        settings.compactResults = s.compactResults
        settings.preferredSaveFormat = s.preferredSaveFormat
        settings.autoSaveTranslated = s.autoSave
        settings.googleTranslateApiKey = s.googleTranslateApiKey.ifBlank { null }
        settings.geminiApiKey = s.geminiApiKey.ifBlank { null }
        update { copy(saved = true) }
    }

    private fun update(block: SettingsUiState.() -> SettingsUiState) {
        _uiState.value = _uiState.value.block()
    }
}
