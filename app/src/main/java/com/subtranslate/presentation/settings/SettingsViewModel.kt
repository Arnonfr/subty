package com.subtranslate.presentation.settings

import androidx.lifecycle.ViewModel
import com.subtranslate.util.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data class SettingsUiState(
    val osApiKey: String = "",
    val osUsername: String = "",
    val osPassword: String = "",
    val anthropicKey: String = "",
    val defaultSourceLang: String = "en",
    val defaultTargetLang: String = "he",
    val saved: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            osApiKey = settings.openSubtitlesApiKey,
            osUsername = settings.openSubtitlesUsername,
            osPassword = settings.openSubtitlesPassword,
            anthropicKey = settings.anthropicApiKey,
            defaultSourceLang = settings.defaultSourceLanguage,
            defaultTargetLang = settings.defaultTargetLanguage
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState

    fun onOsApiKeyChange(v: String) = update { copy(osApiKey = v, saved = false) }
    fun onOsUsernameChange(v: String) = update { copy(osUsername = v, saved = false) }
    fun onOsPasswordChange(v: String) = update { copy(osPassword = v, saved = false) }
    fun onAnthropicKeyChange(v: String) = update { copy(anthropicKey = v, saved = false) }
    fun onSourceLangChange(v: String) = update { copy(defaultSourceLang = v, saved = false) }
    fun onTargetLangChange(v: String) = update { copy(defaultTargetLang = v, saved = false) }

    fun save() {
        val s = _uiState.value
        settings.openSubtitlesApiKey = s.osApiKey
        settings.openSubtitlesUsername = s.osUsername
        settings.openSubtitlesPassword = s.osPassword
        settings.anthropicApiKey = s.anthropicKey
        settings.defaultSourceLanguage = s.defaultSourceLang
        settings.defaultTargetLanguage = s.defaultTargetLang
        update { copy(saved = true) }
    }

    private fun update(block: SettingsUiState.() -> SettingsUiState) {
        _uiState.value = _uiState.value.block()
    }
}
