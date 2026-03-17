package com.subtranslate.presentation.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subtranslate.data.remote.tmdb.SearchSession
import com.subtranslate.domain.model.SubtitleSearchResult
import com.subtranslate.domain.usecase.DownloadSubtitleUseCase
import com.subtranslate.domain.usecase.SaveSubtitleUseCase
import com.subtranslate.domain.usecase.SearchSubtitlesUseCase
import com.subtranslate.util.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    private val searchSession: SearchSession,
    private val settings: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResultsUiState())
    val uiState: StateFlow<ResultsUiState> = _uiState

    fun search(query: String) {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            error = null,
            posterUrl = if (settings.showPosters) searchSession.posterUrl else null
        )
        viewModelScope.launch {
            val result = searchUseCase(query = query.ifBlank { null })
            val posterUrl = searchSession.posterUrl
            val items = result.getOrNull()
                ?.map { it.copy(posterUrl = posterUrl) }
                ?: emptyList()
            _uiState.value = _uiState.value.copy(
                results = items,
                filteredResults = items,
                isLoading = false,
                error = result.exceptionOrNull()?.message
            )
        }
    }

    fun filterByLanguage(lang: String?) {
        val all = _uiState.value.results
        _uiState.value = _uiState.value.copy(
            languageFilter = lang,
            filteredResults = if (lang == null) all else all.filter { it.languageCode == lang }
        )
    }

    /** Download-only: downloads the file and saves it directly — no translation */
    fun downloadAndSave(fileId: Int, languageCode: String, fileName: String) {
        setDownloadState(fileId, DownloadState.DOWNLOADING)
        viewModelScope.launch {
            val fileResult = downloadUseCase(fileId, languageCode)
            val file = fileResult.getOrElse {
                setDownloadState(fileId, DownloadState.ERROR, it.message)
                return@launch
            }
            val ext = file.format.name.lowercase()
            val saveName = fileName.substringBeforeLast(".").ifEmpty { fileName } + ".$ext"
            val saveResult = saveUseCase(file, saveName)
            if (saveResult.isSuccess) {
                setDownloadState(fileId, DownloadState.DONE)
            } else {
                setDownloadState(fileId, DownloadState.ERROR, saveResult.exceptionOrNull()?.message)
            }
        }
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
