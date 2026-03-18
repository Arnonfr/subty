package com.subtranslate.presentation.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subtranslate.data.local.dao.SearchHistoryDao
import com.subtranslate.data.local.entity.SearchHistoryEntity
import com.subtranslate.data.remote.opensubtitles.OpenSubtitlesApi
import com.subtranslate.data.remote.opensubtitles.dto.FeatureDto
import com.subtranslate.data.remote.tmdb.SearchSession
import com.subtranslate.domain.model.SubtitleSearchResult
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
    // Autocomplete
    val suggestions: List<FeatureDto> = emptyList(),
    val showSuggestions: Boolean = false,
    val suggestionsLoading: Boolean = false,
    val suggestionsError: String? = null,
    // Selected title metadata
    val selectedPosterUrl: String? = null,
    val selectedMovieTitle: String? = null,
    val seasonsCount: Int = 0,
    val episodesCount: Int = 0,
    /** true when the selected suggestion is a movie (no season/episode needed) */
    val isMovie: Boolean = false,
)

enum class SearchMode { TITLE, IMDB_ID }

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val openSubtitlesApi: OpenSubtitlesApi,
    private val searchSession: SearchSession,
    private val searchHistoryDao: SearchHistoryDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState

    private var suggestJob: Job? = null

    fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(
            query = query,
            suggestions = if (query.length < 2) emptyList() else _uiState.value.suggestions,
            showSuggestions = query.length >= 2,
            suggestionsError = null,
            selectedPosterUrl = null,
            selectedMovieTitle = null,
            isMovie = false,
        )
        fetchSuggestions(query)
    }

    private fun fetchSuggestions(query: String) {
        suggestJob?.cancel()
        if (query.length < 2) {
            _uiState.value = _uiState.value.copy(
                suggestions = emptyList(),
                showSuggestions = false,
                suggestionsLoading = false
            )
            return
        }
        _uiState.value = _uiState.value.copy(suggestionsLoading = true)
        suggestJob = viewModelScope.launch {
            delay(200)
            runCatching { openSubtitlesApi.searchFeatures(query) }
                .onSuccess { response ->
                    val results = response.data.take(6)
                    _uiState.value = _uiState.value.copy(
                        suggestions = results,
                        showSuggestions = results.isNotEmpty(),
                        suggestionsLoading = false,
                        suggestionsError = null
                    )
                }
                .onFailure { throwable ->
                    Log.e("SearchViewModel", "Suggestions fetch failed: ${throwable.message}")
                    // Silently collapse — never show parse/network errors to the user
                    _uiState.value = _uiState.value.copy(
                        suggestions = emptyList(),
                        showSuggestions = false,
                        suggestionsLoading = false,
                        suggestionsError = null,
                    )
                }
        }
    }

    fun onSuggestionSelected(feature: FeatureDto) {
        val title = feature.attributes.title ?: feature.attributes.originalTitle ?: ""
        val posterUrl = feature.attributes.imgUrl
        val imdbId = feature.attributes.imdbId?.toString()
        val isTv = feature.type == "tv" ||
                feature.attributes.featureType?.lowercase() == "tvshow"
        val contentType = if (isTv) "tv" else "movie"

        searchSession.posterUrl = posterUrl
        searchSession.movieTitle = title
        searchSession.imdbId = imdbId
        searchSession.contentType = contentType

        _uiState.value = _uiState.value.copy(
            query = title,
            showSuggestions = false,
            suggestions = emptyList(),
            suggestionsLoading = false,
            suggestionsError = null,
            selectedPosterUrl = posterUrl,
            selectedMovieTitle = title,
            seasonsCount = feature.attributes.seasonsCount ?: 0,
            episodesCount = feature.attributes.episodesCount ?: 0,
            isMovie = !isTv,
        )
    }

    fun dismissSuggestions() {
        _uiState.value = _uiState.value.copy(showSuggestions = false)
    }

    fun onImdbIdChange(id: String) { _uiState.value = _uiState.value.copy(imdbId = id) }
    fun onSeasonChange(s: String) {
        _uiState.value = _uiState.value.copy(
            season = if (_uiState.value.season == s) "" else s
        )
    }

    fun onEpisodeChange(e: String) {
        _uiState.value = _uiState.value.copy(
            episode = if (_uiState.value.episode == e) "" else e
        )
    }
    fun onSearchModeChange(mode: SearchMode) { _uiState.value = _uiState.value.copy(searchMode = mode) }

    fun toggleLanguage(lang: String) {
        val current = _uiState.value.selectedLanguages.toMutableSet()
        if (lang in current && current.size > 1) current.remove(lang) else current.add(lang)
        _uiState.value = _uiState.value.copy(selectedLanguages = current)
    }

    fun search() {
        val state = _uiState.value
        // Persist search params to session — ResultsViewModel runs the actual search
        searchSession.season = state.season.toIntOrNull()
        searchSession.episode = state.episode.toIntOrNull()
        searchSession.languages = state.selectedLanguages.joinToString(",")
        if (state.searchMode == SearchMode.IMDB_ID) {
            searchSession.imdbId = state.imdbId.ifBlank { null }
        }
        // Collapse autocomplete
        _uiState.value = state.copy(
            showSuggestions = false,
            suggestionsLoading = false,
        )
        // Save to search history (fire-and-forget)
        val queryToSave = if (state.searchMode == SearchMode.TITLE) state.query else state.imdbId
        if (queryToSave.isNotBlank()) {
            viewModelScope.launch {
                runCatching {
                    searchHistoryDao.insert(
                        SearchHistoryEntity(
                            query = queryToSave,
                            season = state.season.toIntOrNull(),
                            episode = state.episode.toIntOrNull(),
                            languages = state.selectedLanguages.joinToString(","),
                            contentType = searchSession.contentType,
                        )
                    )
                }
            }
        }
    }
}
