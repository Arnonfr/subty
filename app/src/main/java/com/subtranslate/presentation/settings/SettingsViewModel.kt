package com.subtranslate.presentation.settings

import androidx.lifecycle.ViewModel
import com.subtranslate.BuildConfig
import com.subtranslate.data.local.datastore.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class UsageStats(
    val myMemory: Int = 0,
    val deepL: Int = 0,
    val microsoft: Int = 0,
    val gemini: Int = 0,
)

data class SettingsUiState(
    // Translation
    val defaultTargetLang: String = "he",
    val translationModel: String = "gemini-2.5-flash",
    val autoTranslate: Boolean = false,
    // Display
    val showPosters: Boolean = true,
    val compactResults: Boolean = false,
    val isDarkTheme: Boolean = false,
    val useSeasonEpisodeTextFields: Boolean = false,
    // Save
    val preferredSaveFormat: String = "srt",
    val autoSave: Boolean = false,
    // API Keys
    val geminiApiKey: String = "",
    val deeplApiKey: String = "",
    val microsoftApiKey: String = "",
    val microsoftRegion: String = "global",
    val availableModels: List<String> = emptyList(),
    // Usage
    val usage: UsageStats = UsageStats(),
    val saved: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            defaultTargetLang = settings.defaultTargetLanguage,
            translationModel = settings.translationModel
                .takeIf { it in loadAvailableModels() }
                ?: loadAvailableModels().firstOrNull()
                ?: settings.translationModel,
            autoTranslate = settings.autoTranslateAfterDownload,
            showPosters = settings.showPosters,
            compactResults = settings.compactResults,
            isDarkTheme = settings.isDarkTheme,
            useSeasonEpisodeTextFields = settings.useSeasonEpisodeTextFields,
            preferredSaveFormat = settings.preferredSaveFormat,
            autoSave = settings.autoSaveTranslated,
            geminiApiKey = settings.geminiApiKey ?: "",
            deeplApiKey = settings.deeplApiKey ?: "",
            microsoftApiKey = settings.customMicrosoftApiKey ?: "",
            microsoftRegion = settings.microsoftRegion,
            availableModels = loadAvailableModels(),
            usage = loadUsage(),
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState

    fun onTargetLangChange(v: String) = update { copy(defaultTargetLang = v, saved = false) }
    fun onModelChange(v: String) = update { copy(translationModel = v, saved = false) }
    fun onAutoTranslateChange(v: Boolean) = update { copy(autoTranslate = v, saved = false) }
    fun onShowPostersChange(v: Boolean) = update { copy(showPosters = v, saved = false) }
    fun onCompactResultsChange(v: Boolean) = update { copy(compactResults = v, saved = false) }
    fun onSeasonEpisodeInputModeChange(v: Boolean) = update { copy(useSeasonEpisodeTextFields = v, saved = false) }
    fun onDarkThemeChange(v: Boolean) {
        settings.isDarkTheme = v
        update { copy(isDarkTheme = v, saved = false) }
    }
    fun onSaveFormatChange(v: String) = update { copy(preferredSaveFormat = v, saved = false) }
    fun onAutoSaveChange(v: Boolean) = update { copy(autoSave = v, saved = false) }
    fun onGeminiApiKeyChange(v: String) = update { copy(geminiApiKey = v, saved = false) }
    fun onDeepLApiKeyChange(v: String) = update { copy(deeplApiKey = v, saved = false) }
    fun onMicrosoftApiKeyChange(v: String) = update { copy(microsoftApiKey = v, saved = false) }
    fun onMicrosoftRegionChange(v: String) = update { copy(microsoftRegion = v, saved = false) }

    fun save() {
        val s = _uiState.value
        settings.defaultTargetLanguage = s.defaultTargetLang
        settings.translationModel = s.translationModel
        settings.autoTranslateAfterDownload = s.autoTranslate
        settings.showPosters = s.showPosters
        settings.compactResults = s.compactResults
        settings.useSeasonEpisodeTextFields = s.useSeasonEpisodeTextFields
        settings.preferredSaveFormat = s.preferredSaveFormat
        settings.autoSaveTranslated = s.autoSave
        settings.geminiApiKey = s.geminiApiKey.ifBlank { null }
        settings.deeplApiKey = s.deeplApiKey.ifBlank { null }
        settings.microsoftApiKey = s.microsoftApiKey.ifBlank { null }
        settings.microsoftRegion = s.microsoftRegion.ifBlank { "global" }
        update { copy(saved = true, availableModels = loadAvailableModels()) }
    }

    fun refreshUsage() {
        update { copy(usage = loadUsage()) }
    }

    private fun loadUsage(): UsageStats {
        return UsageStats(
            myMemory = settings.getCharsUsed("mymemory"),
            deepL = settings.getCharsUsed("deepl"),
            microsoft = settings.getCharsUsed("microsoft"),
            gemini = settings.getCharsUsed("gemini"),
        )
    }

    private fun loadAvailableModels(): List<String> = buildList {
        if (!settings.effectiveMicrosoftApiKey.isNullOrBlank()) add("microsoft")
        if (!settings.geminiApiKey.isNullOrBlank() || BuildConfig.GEMINI_API_KEY.isNotBlank()) {
            add("gemini-2.5-flash")
            add("gemini-3.1-flash-lite-preview")
        }
        if (!settings.deeplApiKey.isNullOrBlank()) add("deepl")
        add("mymemory") // always available — no key required
    }

    private fun update(block: SettingsUiState.() -> SettingsUiState) {
        _uiState.value = _uiState.value.block()
    }
}
