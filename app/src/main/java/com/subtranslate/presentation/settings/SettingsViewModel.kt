package com.subtranslate.presentation.settings

import androidx.lifecycle.ViewModel
import com.subtranslate.util.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data class SettingsUiState(
    // Translation
    val defaultTargetLang: String = "he",
    val translationModel: String = "gemini-2.5-flash",
    val autoTranslate: Boolean = false,
    // Display
    val showPosters: Boolean = true,
    val compactResults: Boolean = false,
    // Save
    val preferredSaveFormat: String = "srt",
    val autoSave: Boolean = false,
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
            preferredSaveFormat = settings.preferredSaveFormat,
            autoSave = settings.autoSaveTranslated
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState

    fun onTargetLangChange(v: String) = update { copy(defaultTargetLang = v, saved = false) }
    fun onModelChange(v: String) = update { copy(translationModel = v, saved = false) }
    fun onAutoTranslateChange(v: Boolean) = update { copy(autoTranslate = v, saved = false) }
    fun onShowPostersChange(v: Boolean) = update { copy(showPosters = v, saved = false) }
    fun onCompactResultsChange(v: Boolean) = update { copy(compactResults = v, saved = false) }
    fun onSaveFormatChange(v: String) = update { copy(preferredSaveFormat = v, saved = false) }
    fun onAutoSaveChange(v: Boolean) = update { copy(autoSave = v, saved = false) }

    fun save() {
        val s = _uiState.value
        settings.defaultTargetLanguage = s.defaultTargetLang
        settings.translationModel = s.translationModel
        settings.autoTranslateAfterDownload = s.autoTranslate
        settings.showPosters = s.showPosters
        settings.compactResults = s.compactResults
        settings.preferredSaveFormat = s.preferredSaveFormat
        settings.autoSaveTranslated = s.autoSave
        update { copy(saved = true) }
    }

    private fun update(block: SettingsUiState.() -> SettingsUiState) {
        _uiState.value = _uiState.value.block()
    }
}
