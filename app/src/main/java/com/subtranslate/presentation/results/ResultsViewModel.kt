package com.subtranslate.presentation.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subtranslate.data.local.datastore.SettingsDataStore
import com.subtranslate.data.remote.tmdb.SearchSession
import com.subtranslate.data.repository.SubtitleRepositoryImpl
import com.subtranslate.domain.model.HistoryItem
import com.subtranslate.domain.model.SubtitleSearchResult
import com.subtranslate.domain.repository.HistoryRepository
import com.subtranslate.domain.usecase.DownloadSubtitleUseCase
import com.subtranslate.domain.usecase.SaveSubtitleUseCase
import com.subtranslate.domain.usecase.SearchSubtitlesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DownloadState { IDLE, DOWNLOADING, DONE, ERROR }

data class ResultsUiState(
    val results: List<SubtitleSearchResult> = emptyList(),
    val filteredResults: List<SubtitleSearchResult> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val languageFilter: String? = null,
    val downloadStates: Map<Int, DownloadState> = emptyMap(),
    val downloadErrors: Map<Int, String> = emptyMap(),
    val posterUrl: String? = null
)

@HiltViewModel
class ResultsViewModel @Inject constructor(
    private val searchUseCase: SearchSubtitlesUseCase,
    private val downloadUseCase: DownloadSubtitleUseCase,
    private val saveUseCase: SaveSubtitleUseCase,
    private val repository: SubtitleRepositoryImpl,
    private val searchSession: SearchSession,
    private val settings: SettingsDataStore,
    private val historyRepository: HistoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResultsUiState())
    val uiState: StateFlow<ResultsUiState> = _uiState

    private val _downloadDoneEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val downloadDoneEvents: SharedFlow<String> = _downloadDoneEvents.asSharedFlow()

    fun search(query: String) {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            error = null,
            posterUrl = if (settings.showPosters) searchSession.posterUrl else null
        )
        viewModelScope.launch {
            val posterUrl = searchSession.posterUrl
            val imdbIdStr = searchSession.imdbId
            val season = searchSession.season
            val episode = searchSession.episode
            val languages = searchSession.languages

            val osDeferred = async {
                searchUseCase(
                    query     = query.ifBlank { null },
                    imdbId    = imdbIdStr?.removePrefix("tt")?.toIntOrNull(),
                    languages = languages,
                    season    = season,
                    episode   = episode,
                ).getOrNull()?.map { it.copy(posterUrl = posterUrl) } ?: emptyList()
            }
            val sdDeferred = async {
                try {
                    repository.searchSubDL(
                        title     = query.ifBlank { null },
                        imdbId    = imdbIdStr,
                        season    = season,
                        episode   = episode,
                        languages = languages?.uppercase(),
                        type      = searchSession.contentType,
                    )
                } catch (_: Exception) { emptyList() }
            }

            // Show OS results immediately (~289ms), don't wait for SubDL
            val osItems = osDeferred.await()
            _uiState.value = _uiState.value.copy(
                results = osItems,
                filteredResults = applyFilter(osItems, _uiState.value.languageFilter),
                isLoading = false,
                error = if (osItems.isEmpty()) "Searching more sources…" else null,
            )

            // Append SubDL results when ready (5s timeout)
            val sdItems = withTimeoutOrNull(5_000) { sdDeferred.await() } ?: emptyList()
            val combined = osItems + sdItems
            _uiState.value = _uiState.value.copy(
                results = combined,
                filteredResults = applyFilter(combined, _uiState.value.languageFilter),
                error = if (combined.isEmpty()) "No subtitles found" else null,
            )
        }
    }

    fun filterByLanguage(lang: String?) {
        val all = _uiState.value.results
        _uiState.value = _uiState.value.copy(
            languageFilter = lang,
            filteredResults = applyFilter(all, lang),
        )
    }

    private fun applyFilter(results: List<SubtitleSearchResult>, lang: String?) =
        if (lang == null) results else results.filter { it.languageCode == lang }

    /** Download-only: downloads the file and saves it directly — no translation */
    fun downloadAndSave(fileId: Int, languageCode: String, fileName: String) {
        setDownloadState(fileId, DownloadState.DOWNLOADING)
        viewModelScope.launch {
            val fileResult = downloadUseCase(fileId, languageCode)
            val file = fileResult.getOrElse {
                setDownloadState(fileId, DownloadState.ERROR, friendlyError(it))
                return@launch
            }
            val ext = file.format.name.lowercase()
            val saveName = fileName.substringBeforeLast(".").ifEmpty { fileName } + ".$ext"
            val saveResult = saveUseCase(file, saveName)
            if (saveResult.isSuccess) {
                setDownloadState(fileId, DownloadState.DONE)
                _downloadDoneEvents.tryEmit(saveName)
                // Save to history DB
                viewModelScope.launch {
                    runCatching {
                        historyRepository.save(
                            HistoryItem(
                                id = 0,
                                movieTitle = saveName.substringBeforeLast("."),
                                originalLanguage = languageCode,
                                translatedLanguage = null,
                                format = ext,
                                openSubtitlesFileId = fileId,
                                originalFilePath = saveName,
                                translatedFilePath = null,
                                downloadedAt = System.currentTimeMillis(),
                                translatedAt = null
                            )
                        )
                    }
                }
            } else {
                setDownloadState(fileId, DownloadState.ERROR, saveResult.exceptionOrNull()?.message)
            }
        }
    }

    private fun friendlyError(e: Throwable): String = when {
        e.message?.contains("406") == true ->
            "Daily download limit reached (5/day free). Try again tomorrow."
        e.message?.contains("401") == true || e.message?.contains("403") == true ->
            "Invalid API key — contact support."
        e.message?.contains("429") == true ->
            "Too many requests — wait a moment and retry."
        e.message?.contains("503") == true ->
            "Server temporarily unavailable — try again in a moment."
        else -> e.message ?: "Download failed"
    }

    private fun setDownloadState(fileId: Int, state: DownloadState, error: String? = null) {
        _uiState.value = _uiState.value.copy(
            downloadStates = _uiState.value.downloadStates + (fileId to state),
            downloadErrors = if (error != null)
                _uiState.value.downloadErrors + (fileId to error)
            else _uiState.value.downloadErrors
        )
    }
}
