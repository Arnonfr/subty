package com.subtranslate.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subtranslate.data.local.datastore.SettingsDataStore
import com.subtranslate.data.remote.translation.MlKitTranslationProvider
import com.subtranslate.data.remote.translation.TranslationModelCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val defaultTargetLang: String = "he",
    val autoTranslate: Boolean = false,
    val showPosters: Boolean = true,
    val compactResults: Boolean = false,
    val isDarkTheme: Boolean = false,
    val useSeasonEpisodeTextFields: Boolean = false,
    val preferredSaveFormat: String = "srt",
    val saveDestinationMode: String = "movies",
    val customSaveFolderUri: String? = null,
    val autoSave: Boolean = false,
    val liveAnalyticsEnabled: Boolean = true,
    val shareSearchTerms: Boolean = false,
    val saved: Boolean = false,
    val downloadedOfflineLanguages: Set<String> = emptySet(),
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsDataStore,
    private val mlKitTranslationProvider: MlKitTranslationProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        SettingsUiState(
            defaultTargetLang = settings.defaultTargetLanguage,
            autoTranslate = settings.autoTranslateAfterDownload,
            showPosters = settings.showPosters,
            compactResults = settings.compactResults,
            isDarkTheme = settings.isDarkTheme,
            useSeasonEpisodeTextFields = settings.useSeasonEpisodeTextFields,
            preferredSaveFormat = settings.preferredSaveFormat,
            saveDestinationMode = settings.saveDestinationMode,
            customSaveFolderUri = settings.customSaveFolderUri,
            autoSave = settings.autoSaveTranslated,
            liveAnalyticsEnabled = settings.liveAnalyticsEnabled,
            shareSearchTerms = settings.shareSearchTerms,
        ),
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init { refreshOfflineLanguages() }

    fun onTargetLangChange(v: String) = update { copy(defaultTargetLang = v, saved = false) }
    fun onAutoTranslateChange(v: Boolean) = update { copy(autoTranslate = v, saved = false) }
    fun onShowPostersChange(v: Boolean) = update { copy(showPosters = v, saved = false) }
    fun onCompactResultsChange(v: Boolean) = update { copy(compactResults = v, saved = false) }
    fun onDarkThemeChange(v: Boolean) = update { copy(isDarkTheme = v, saved = false) }
    fun onUseSeasonEpisodeTextFieldsChange(v: Boolean) = update { copy(useSeasonEpisodeTextFields = v, saved = false) }
    fun onSaveFormatChange(v: String) = update { copy(preferredSaveFormat = v, saved = false) }
    fun onSaveDestinationModeChange(v: String) = update { copy(saveDestinationMode = v, saved = false) }
    fun onCustomSaveFolderUriChange(v: String?) = update { copy(customSaveFolderUri = v, saved = false) }
    fun onAutoSaveChange(v: Boolean) = update { copy(autoSave = v, saved = false) }
    fun onLiveAnalyticsEnabledChange(v: Boolean) = update { copy(liveAnalyticsEnabled = v, saved = false) }
    fun onShareSearchTermsChange(v: Boolean) = update { copy(shareSearchTerms = v, saved = false) }

    fun save() {
        val state = _uiState.value
        settings.defaultTargetLanguage = state.defaultTargetLang
        settings.translationModel = TranslationModelCatalog.MLKIT_ON_DEVICE
        settings.autoTranslateAfterDownload = state.autoTranslate
        settings.showPosters = state.showPosters
        settings.compactResults = state.compactResults
        settings.isDarkTheme = state.isDarkTheme
        settings.useSeasonEpisodeTextFields = state.useSeasonEpisodeTextFields
        settings.preferredSaveFormat = state.preferredSaveFormat
        settings.saveDestinationMode = state.saveDestinationMode
        settings.customSaveFolderUri = state.customSaveFolderUri
        settings.autoSaveTranslated = state.autoSave
        settings.liveAnalyticsEnabled = state.liveAnalyticsEnabled
        settings.shareSearchTerms = state.shareSearchTerms
        _uiState.value = state.copy(saved = true)
    }

    fun refreshOfflineLanguages() {
        viewModelScope.launch {
            runCatching { mlKitTranslationProvider.downloadedLanguageCodes() }
                .onSuccess { downloaded -> _uiState.value = _uiState.value.copy(downloadedOfflineLanguages = downloaded) }
        }
    }

    fun downloadOfflineLanguage(code: String) {
        viewModelScope.launch {
            runCatching { mlKitTranslationProvider.downloadLanguage(code) }
                .onSuccess { refreshOfflineLanguages() }
        }
    }

    fun removeOfflineLanguage(code: String) {
        viewModelScope.launch {
            runCatching { mlKitTranslationProvider.deleteLanguage(code) }
                .onSuccess { refreshOfflineLanguages() }
        }
    }

    private fun update(block: SettingsUiState.() -> SettingsUiState) { _uiState.value = _uiState.value.block() }
}
