package com.subtranslate.presentation.translate

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subtranslate.data.analytics.AnalyticsTracker
import com.subtranslate.data.local.datastore.SettingsDataStore
import com.subtranslate.data.remote.translation.TranslationModelCatalog
import com.subtranslate.data.repository.TranslationRepositoryImpl
import com.subtranslate.domain.model.HistoryItem
import com.subtranslate.domain.model.SubtitleFile
import com.subtranslate.domain.model.TranslationProgress
import com.subtranslate.domain.model.TranslationStatus
import com.subtranslate.domain.repository.HistoryRepository
import com.subtranslate.domain.usecase.DownloadSubtitleUseCase
import com.subtranslate.domain.usecase.SaveSubtitleUseCase
import com.subtranslate.service.TranslationForegroundService
import com.subtranslate.service.TranslationStateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class TranslateUiState(
    val sourceLang: String = "en",
    val targetLang: String = "he",
    val isLoadingFile: Boolean = false,
    val progress: TranslationProgress = TranslationProgress(),
    val translatedFile: SubtitleFile? = null,
    val savedPath: String? = null,
    val saveError: String? = null,
)

@HiltViewModel
class TranslateViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadUseCase: DownloadSubtitleUseCase,
    private val saveUseCase: SaveSubtitleUseCase,
    private val historyRepository: HistoryRepository,
    private val analyticsTracker: AnalyticsTracker,
    private val translationRepository: TranslationRepositoryImpl,
    private val settings: SettingsDataStore,
    private val stateHolder: TranslationStateHolder,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        TranslateUiState(
            sourceLang = settings.defaultSourceLanguage,
            targetLang = settings.defaultTargetLanguage,
        ),
    )
    val uiState: StateFlow<TranslateUiState> = _uiState.asStateFlow()
    private val _saveDoneEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val saveDoneEvents: SharedFlow<String> = _saveDoneEvents.asSharedFlow()

    var pendingFile: SubtitleFile? = null
        private set
    private var progressJob: Job? = null

    init { observeProgress() }

    fun downloadAndLoad(fileId: Int, languageCode: String) {
        _uiState.value = _uiState.value.copy(isLoadingFile = true)
        viewModelScope.launch {
            downloadUseCase(fileId, languageCode).fold(
                onSuccess = { file -> loadSubtitle(file); _uiState.value = _uiState.value.copy(isLoadingFile = false) },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingFile = false,
                        progress = _uiState.value.progress.copy(status = TranslationStatus.ERROR, errorMessage = err.message ?: "Download failed"),
                    )
                },
            )
        }
    }

    fun loadSubtitle(file: SubtitleFile) {
        pendingFile = file
        val source = file.sourceLanguage ?: settings.defaultSourceLanguage
        _uiState.value = _uiState.value.copy(
            sourceLang = source,
            targetLang = if (source == settings.defaultTargetLanguage) settings.defaultSourceLanguage else settings.defaultTargetLanguage,
            progress = TranslationProgress(), translatedFile = null, savedPath = null, saveError = null,
        )
    }

    fun onSourceLangChange(lang: String) {
        if (_uiState.value.progress.status == TranslationStatus.TRANSLATING || lang == _uiState.value.sourceLang) return
        _uiState.value = _uiState.value.copy(sourceLang = lang, translatedFile = null, savedPath = null, saveError = null, progress = TranslationProgress())
    }
    fun onTargetLangChange(lang: String) {
        if (_uiState.value.progress.status == TranslationStatus.TRANSLATING || lang == _uiState.value.targetLang) return
        _uiState.value = _uiState.value.copy(targetLang = lang, translatedFile = null, savedPath = null, saveError = null, progress = TranslationProgress())
    }

    fun startTranslation(file: SubtitleFile) {
        val state = _uiState.value
        analyticsTracker.track("translation_start_clicked", mapOf("model" to TranslationModelCatalog.MLKIT_ON_DEVICE, "source_lang" to state.sourceLang, "target_lang" to state.targetLang))
        pendingFile = file
        val jobId = stateHolder.beginTranslation()
        TranslationForegroundService.pendingFile = file
        ContextCompat.startForegroundService(context, Intent(context, TranslationForegroundService::class.java).apply {
            putExtra(TranslationForegroundService.EXTRA_SOURCE_LANG, state.sourceLang)
            putExtra(TranslationForegroundService.EXTRA_TARGET_LANG, state.targetLang)
            putExtra(TranslationForegroundService.EXTRA_MODEL_ID, TranslationModelCatalog.MLKIT_ON_DEVICE)
            putExtra(TranslationForegroundService.EXTRA_JOB_ID, jobId)
        })
    }

    fun cancelTranslation() {
        context.stopService(Intent(context, TranslationForegroundService::class.java))
        stateHolder.reset()
        _uiState.value = _uiState.value.copy(progress = _uiState.value.progress.copy(status = TranslationStatus.IDLE))
    }

    fun save() {
        val file = _uiState.value.translatedFile ?: return
        val ext = file.format.name.lowercase()
        val baseName = file.title?.substringBeforeLast(".").orEmpty().ifBlank { "translated_subtitle" }
        val saveName = "$baseName.${_uiState.value.targetLang}.$ext"
        viewModelScope.launch {
            val result = saveUseCase(file, saveName)
            _uiState.value = _uiState.value.copy(savedPath = result.getOrNull()?.absolutePath, saveError = result.exceptionOrNull()?.message)
            if (result.isSuccess) {
                _saveDoneEvents.tryEmit(saveName)
                runCatching {
                    historyRepository.save(
                        HistoryItem(
                            id = 0,
                            movieTitle = file.title?.substringBeforeLast(".") ?: saveName.substringBeforeLast("."),
                            originalLanguage = _uiState.value.sourceLang,
                            translatedLanguage = _uiState.value.targetLang,
                            format = ext,
                            openSubtitlesFileId = null,
                            originalFilePath = null,
                            translatedFilePath = result.getOrNull()?.path ?: saveName,
                            downloadedAt = System.currentTimeMillis(),
                            translatedAt = System.currentTimeMillis(),
                        ),
                    )
                }
            }
        }
    }

    private fun observeProgress() {
        progressJob?.cancel()
        progressJob = stateHolder.progress.onEach { progress ->
            _uiState.value = _uiState.value.copy(
                progress = progress,
                translatedFile = if (progress.status == TranslationStatus.COMPLETE) translationRepository.lastTranslatedFile ?: _uiState.value.translatedFile else _uiState.value.translatedFile,
            )
        }.launchIn(viewModelScope)
    }
}
