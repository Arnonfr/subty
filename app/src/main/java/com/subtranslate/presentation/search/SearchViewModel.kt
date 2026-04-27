package com.subtranslate.presentation.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subtranslate.data.local.dao.SearchHistoryDao
import com.subtranslate.data.local.datastore.SettingsDataStore
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

sealed class Suggestion {
    data class Remote(val result: FeatureDto) : Suggestion()
    data class History(val item: SearchHistoryEntity) : Suggestion()
}

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
    val combinedSuggestions: List<Suggestion> = emptyList(),
    val showSuggestions: Boolean = false,
    val suggestionsLoading: Boolean = false,
    val suggestionsError: String? = null,
    // Selected title metadata
    val selectedPosterUrl: String? = null,
    val selectedMovieTitle: String? = null,
    val seasonsCount: Int = 0,
    val episodesCount: Int = 0,
    val useSeasonEpisodeTextFields: Boolean = false,
    /** true when the selected suggestion is a movie (no season/episode needed) */
    val isMovie: Boolean = false,
)

enum class SearchMode { TITLE, IMDB_ID }

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val openSubtitlesApi: OpenSubtitlesApi,
    private val searchSession: SearchSession,
    private val searchHistoryDao: SearchHistoryDao,
    private val settings: SettingsDataStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SearchUiState(useSeasonEpisodeTextFields = settings.useSeasonEpisodeTextFields)
    )
    val uiState: StateFlow<SearchUiState> = _uiState

    private var suggestJob: Job? = null

    init {
        val pending = searchSession.pendingBrowseTitle
        if (!pending.isNullOrBlank()) {
            searchSession.pendingBrowseTitle = null
            val langs = searchSession.pendingBrowseLangs
            searchSession.pendingBrowseLangs = null
            val langSet = langs?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }?.toSet()
                ?: setOf("en", "he")
            _uiState.value = _uiState.value.copy(
                query = pending,
                selectedLanguages = langSet,
                showSuggestions = true,
                suggestionsLoading = true,
            )
            fetchSuggestions(pending)
        }
    }

    fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(
            query = query,
            suggestions = if (query.length < 2) emptyList() else _uiState.value.suggestions,
            combinedSuggestions = if (query.length < 2) emptyList() else _uiState.value.combinedSuggestions,
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
            delay(100)
            // Fetch history suggestions in parallel
            val historyItems = runCatching { searchHistoryDao.searchByPrefix(query) }
                .getOrDefault(emptyList())
            runCatching { openSubtitlesApi.searchFeatures(query) }
                .onSuccess { response ->
                    val remoteResults = rankSuggestions(response.data, query).take(8)
                    val historySuggestions = historyItems.map { Suggestion.History(it) }
                    val remoteSuggestions = remoteResults.map { Suggestion.Remote(it) }
                    val combined = (historySuggestions + remoteSuggestions).take(8)
                    _uiState.value = _uiState.value.copy(
                        suggestions = remoteResults,
                        combinedSuggestions = combined,
                        showSuggestions = combined.isNotEmpty(),
                        suggestionsLoading = false,
                        suggestionsError = null
                    )
                }
                .onFailure { throwable ->
                    Log.e("SearchViewModel", "Suggestions fetch failed: ${throwable.message}")
                    // Fall back to history-only suggestions
                    val historySuggestions = historyItems.map { Suggestion.History(it) }
                    _uiState.value = _uiState.value.copy(
                        suggestions = emptyList(),
                        combinedSuggestions = historySuggestions,
                        showSuggestions = historySuggestions.isNotEmpty(),
                        suggestionsLoading = false,
                        suggestionsError = null,
                    )
                }
        }
    }

    private fun rankSuggestions(results: List<FeatureDto>, query: String): List<FeatureDto> {
        val q = query.lowercase().trim()
        return results.sortedByDescending { feature ->
            val title = feature.attributes.title?.lowercase() ?: ""
            val attrs = feature.attributes
            var score = 0
            // 1. Exact prefix match is highest signal
            if (title.startsWith(q)) score += 100
            // 2. Query matches at word boundary
            if (title.split(" ").any { it.startsWith(q) }) score += 50
            // 3. Popular TV shows (many seasons/episodes)
            score += (attrs.seasonsCount ?: 0) * 10
            score += minOf((attrs.episodesCount ?: 0) / 10, 50)
            // 4. Recent content (2010+ gets bonus)
            val year = attrs.year ?: 2000
            if (year >= 2010) score += 20
            if (year >= 2015) score += 10
            // 5. Movies get slight boost for recency (higher IMDB ID = more recent)
            if (attrs.featureType == "Movie") score += 5
            score
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
            season = if (isTv) "1" else "",
            episode = if (isTv) "1" else "",
        )
    }

    fun onHistorySuggestionSelected(item: SearchHistoryEntity) {
        searchSession.movieTitle  = item.query
        searchSession.season      = item.season
        searchSession.episode     = item.episode
        searchSession.languages   = item.languages
        searchSession.contentType = item.contentType
        searchSession.imdbId      = null
        searchSession.posterUrl   = null
        _uiState.value = _uiState.value.copy(
            query = item.query,
            showSuggestions = false,
            combinedSuggestions = emptyList(),
            suggestions = emptyList(),
            suggestionsLoading = false,
            suggestionsError = null,
            selectedPosterUrl = null,
            selectedMovieTitle = item.query,
            isMovie = item.contentType == "movie",
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
        if (state.searchMode == SearchMode.IMDB_ID || state.imdbId.isNotBlank()) {
            searchSession.imdbId = state.imdbId.ifBlank { null }
        }
        // Collapse autocomplete
        _uiState.value = state.copy(
            showSuggestions = false,
            suggestionsLoading = false,
        )
        // Save to search history (fire-and-forget)
        val queryToSave = if (state.searchMode == SearchMode.TITLE) state.query.ifBlank { state.imdbId } else state.imdbId
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
