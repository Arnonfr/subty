package com.subtranslate.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subtranslate.data.remote.tmdb.SearchSession
import com.subtranslate.data.remote.tmdb.TmdbApi
import com.subtranslate.data.remote.tmdb.TmdbResult
import com.subtranslate.domain.model.SubtitleSearchResult
import com.subtranslate.domain.usecase.SearchSubtitlesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val imdbId: String = "",
    val season: String = "",
    val episode: String = "",
    val selectedLanguages: Set<String> = setOf("en", "he"),
    val searchMode: SearchMode = SearchMode.TITLE,
    val isLoading: Boolean = false,
    val results: List<SubtitleSearchResult> = emptyList(),
    val error: String? = null,
    // TMDB autocomplete
    val suggestions: List<TmdbResult> = emptyList(),
    val showSuggestions: Boolean = false,
    val selectedPosterUrl: String? = null,
    val selectedMovieTitle: String? = null
)

enum class SearchMode { TITLE, IMDB_ID }

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchUseCase: SearchSubtitlesUseCase,
    private val tmdbApi: TmdbApi,
    private val searchSession: SearchSession
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState

    private var suggestJob: Job? = null

    fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(
            query = query,
            showSuggestions = query.length >= 2,
            selectedPosterUrl = null,
            selectedMovieTitle = null
        )
        fetchSuggestions(query)
    }

    private fun fetchSuggestions(query: String) {
        suggestJob?.cancel()
        if (query.length < 2) {
            _uiState.value = _uiState.value.copy(suggestions = emptyList(), showSuggestions = false)
            return
        }
        suggestJob = viewModelScope.launch {
            delay(350) // debounce
            runCatching { tmdbApi.searchMulti(query) }
                .getOrNull()
                ?.results
                ?.filter { it.media_type in listOf("movie", "tv") }
                ?.take(6)
                ?.also { results ->
                    _uiState.value = _uiState.value.copy(
                        suggestions = results,
                        showSuggestions = results.isNotEmpty()
                    )
                }
        }
    }

    fun onSuggestionSelected(result: TmdbResult) {
        searchSession.posterUrl = result.posterUrl
        searchSession.movieTitle = result.displayTitle
        searchSession.imdbId = result.imdb_id

        _uiState.value = _uiState.value.copy(
            query = result.displayTitle,
            showSuggestions = false,
            suggestions = emptyList(),
            selectedPosterUrl = result.posterUrl,
            selectedMovieTitle = result.displayTitle
        )
    }

    fun dismissSuggestions() {
        _uiState.value = _uiState.value.copy(showSuggestions = false)
    }

    fun onImdbIdChange(id: String) { _uiState.value = _uiState.value.copy(imdbId = id) }
    fun onSeasonChange(s: String) { _uiState.value = _uiState.value.copy(season = s) }
    fun onEpisodeChange(e: String) { _uiState.value = _uiState.value.copy(episode = e) }
    fun onSearchModeChange(mode: SearchMode) { _uiState.value = _uiState.value.copy(searchMode = mode) }

    fun toggleLanguage(lang: String) {
        val current = _uiState.value.selectedLanguages.toMutableSet()
        if (lang in current && current.size > 1) current.remove(lang) else current.add(lang)
        _uiState.value = _uiState.value.copy(selectedLanguages = current)
    }

    fun search() {
        val state = _uiState.value
        _uiState.value = state.copy(isLoading = true, error = null, results = emptyList(), showSuggestions = false)
        viewModelScope.launch {
            val result = searchUseCase(
                query = if (state.searchMode == SearchMode.TITLE) state.query.ifBlank { null } else null,
                imdbId = if (state.searchMode == SearchMode.IMDB_ID) state.imdbId.toIntOrNull() else null,
                languages = state.selectedLanguages.joinToString(","),
                season = state.season.toIntOrNull(),
                episode = state.episode.toIntOrNull()
            )
            val posterUrl = searchSession.posterUrl
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                // Inject posterUrl into every result (same movie)
                results = result.getOrNull()?.map { it.copy(posterUrl = posterUrl) } ?: emptyList(),
                error = result.exceptionOrNull()?.message
            )
        }
    }
}
