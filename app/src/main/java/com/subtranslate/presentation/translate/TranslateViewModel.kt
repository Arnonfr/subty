package com.subtranslate.presentation.translate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subtranslate.data.repository.TranslationRepositoryImpl
import com.subtranslate.domain.model.SubtitleFile
import com.subtranslate.domain.model.TranslationProgress
import com.subtranslate.domain.model.TranslationStatus
import com.subtranslate.domain.usecase.DownloadSubtitleUseCase
import com.subtranslate.domain.usecase.SaveSubtitleUseCase
import com.subtranslate.domain.usecase.TranslateSubtitleUseCase
import com.subtranslate.data.local.datastore.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TranslateUiState(
    // sourceLang is auto-detected from the subtitle file — never entered manually
    val sourceLang: String = "en",
    val targetLang: String = "he",
    val selectedModel: String = "google",
    val isLoadingFile: Boolean = false,
    val progress: TranslationProgress = TranslationProgress(),
    val translatedFile: SubtitleFile? = null,
    val savedPath: String? = null,
    val saveError: String? = null
)

@HiltViewModel
class TranslateViewModel @Inject constructor(
    private val downloadUseCase: DownloadSubtitleUseCase,
    private val translateUseCase: TranslateSubtitleUseCase,
    private val saveUseCase: SaveSubtitleUseCase,
    private val translationRepo: TranslationRepositoryImpl,
    private val settings: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(TranslateUiState())
    val uiState: StateFlow<TranslateUiState> = _uiState

    // The subtitle file to translate - set by the owning screen via shared ViewModel or nav args
    var pendingFile: SubtitleFile? = null

    private var translationJob: Job? = null

    init {
        _uiState.value = _uiState.value.copy(
            targetLang = settings.defaultTargetLanguage,
            selectedModel = settings.translationModel
        )
    }

    /**
     * Called when a subtitle is selected. Sets sourceLang from the file's detected language
     * so the user never needs to enter it manually.
     */
    /** Downloads the subtitle and auto-sets source language — no user input required. */
    fun downloadAndLoad(fileId: Int, languageCode: String) {
        _uiState.value = _uiState.value.copy(isLoadingFile = true)
        viewModelScope.launch {
            val result = downloadUseCase(fileId, languageCode)
            _uiState.value = _uiState.value.copy(isLoadingFile = false)
            result.getOrNull()?.let { loadSubtitle(it) }
                ?: run {
                    val err = result.exceptionOrNull()
                    val msg = when {
                        err?.message?.contains("406") == true ->
                            "Daily download limit reached (5/day free plan). Try again tomorrow."
                        err?.message?.contains("401") == true || err?.message?.contains("403") == true ->
                            "API key error. Contact app support."
                        err?.message?.contains("503") == true ->
                            "Server temporarily unavailable. Try again in a moment."
                        else -> err?.message ?: "Download failed"
                    }
                    _uiState.value = _uiState.value.copy(
                        progress = _uiState.value.progress.copy(
                            status = com.subtranslate.domain.model.TranslationStatus.ERROR,
                            errorMessage = msg
                        )
                    )
                }
        }
    }

    fun loadSubtitle(file: SubtitleFile) {
        pendingFile = file
        val detectedLang = file.sourceLanguage ?: "en"
        val autoTarget = if (detectedLang == settings.defaultTargetLanguage)
            settings.defaultSourceLanguage
        else
            settings.defaultTargetLanguage
        _uiState.value = _uiState.value.copy(
            sourceLang = detectedLang,
            targetLang = autoTarget,
            progress = TranslationProgress(),
            translatedFile = null,
            savedPath = null,
            saveError = null
        )
    }

    fun onSourceLangChange(lang: String) {
        _uiState.value = _uiState.value.copy(sourceLang = lang)
    }

    fun onTargetLangChange(lang: String) {
        _uiState.value = _uiState.value.copy(targetLang = lang)
    }

    fun onModelChange(model: String) {
        _uiState.value = _uiState.value.copy(selectedModel = model)
    }

    fun startTranslation(file: SubtitleFile) {
        pendingFile = file
        translationJob = viewModelScope.launch {
            translateUseCase(
                subtitleFile = file,
                sourceLang = _uiState.value.sourceLang,
                targetLang = _uiState.value.targetLang,
                modelId = _uiState.value.selectedModel
            ).onEach { progress ->
                val translatedFile = if (progress.status == TranslationStatus.COMPLETE) {
                    translationRepo.lastTranslatedFile
                } else null
                _uiState.value = _uiState.value.copy(
                    progress = progress,
                    translatedFile = translatedFile ?: _uiState.value.translatedFile
                )
            }.launchIn(this)
        }
    }

    fun save() {
        val file = _uiState.value.translatedFile ?: return
        val ext = file.format.name.lowercase()
        val name = "${file.title?.substringBeforeLast(".")}_${_uiState.value.targetLang}.$ext"

        viewModelScope.launch {
            val result = saveUseCase(file, name)
            _uiState.value = _uiState.value.copy(
                savedPath = result.getOrNull()?.absolutePath,
                saveError = result.exceptionOrNull()?.message
            )
        }
    }

    fun cancelTranslation() {
        translationJob?.cancel()
        _uiState.value = _uiState.value.copy(
            progress = _uiState.value.progress.copy(status = TranslationStatus.IDLE)
        )
    }
}
