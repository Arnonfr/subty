package com.subtranslate.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subtranslate.domain.model.SubtitleSearchResult
import com.subtranslate.domain.usecase.SearchSubtitlesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val imdbId: String = "",
    val season: String = "",
    val episode: String = "",
    val selectedLanguages: Set<String> = setOf("en"),
    val searchMode: SearchMode = SearchMode.TITLE,
    val isLoading: Boolean = false,
    val results: List<SubtitleSearchResult> = emptyList(),
    val error: String? = null
)

enum class SearchMode { TITLE, IMDB_ID }

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchUseCase: SearchSubtitlesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState

    fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
    }

    fun onImdbIdChange(id: String) {
        _uiState.value = _uiState.value.copy(imdbId = id)
    }

    fun onSeasonChange(s: String) {
        _uiState.value = _uiState.value.copy(season = s)
    }

    fun onEpisodeChange(e: String) {
        _uiState.value = _uiState.value.copy(episode = e)
    }

    fun onSearchModeChange(mode: SearchMode) {
        _uiState.value = _uiState.value.copy(searchMode = mode)
    }

    fun toggleLanguage(lang: String) {
        val current = _uiState.value.selectedLanguages.toMutableSet()
        if (lang in current && current.size > 1) current.remove(lang) else current.add(lang)
        _uiState.value = _uiState.value.copy(selectedLanguages = current)
    }

    fun search() {
        val state = _uiState.value
        _uiState.value = state.copy(isLoading = true, error = null, results = emptyList())

        viewModelScope.launch {
            val result = searchUseCase(
                query = if (state.searchMode == SearchMode.TITLE) state.query.ifBlank { null } else null,
                imdbId = if (state.searchMode == SearchMode.IMDB_ID) state.imdbId.toIntOrNull() else null,
                languages = state.selectedLanguages.joinToString(","),
                season = state.season.toIntOrNull(),
                episode = state.episode.toIntOrNull()
            )
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                results = result.getOrNull() ?: emptyList(),
                error = result.exceptionOrNull()?.message
            )
        }
    }
}
