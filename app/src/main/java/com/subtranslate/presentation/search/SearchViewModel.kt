package com.subtranslate.presentation.search

import android.util.Log
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subtranslate.data.analytics.AnalyticsTracker
import com.subtranslate.data.local.dao.SearchHistoryDao
import com.subtranslate.data.local.datastore.SettingsDataStore
import com.subtranslate.data.local.entity.SearchHistoryEntity
import com.subtranslate.data.remote.opensubtitles.OpenSubtitlesApi
import com.subtranslate.data.remote.opensubtitles.dto.FeatureDto
import com.subtranslate.data.remote.tmdb.SearchSession
import com.subtranslate.data.remote.tmdb.TmdbApi
import com.subtranslate.data.remote.tmdb.heroImageUrl
import com.subtranslate.data.remote.tmdb.toFeatureDto
import com.subtranslate.domain.model.SubtitleSearchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
    val recentShows: List<SearchHistoryEntity> = emptyList(),
    /** ID of the recent-show card currently resolving its next episode via API */
    val nextEpisodeLoadingId: Long? = null,
    val starBalance: Int = 0,
)

enum class SearchMode { TITLE, IMDB_ID }

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val tmdbApi: TmdbApi,
    private val openSubtitlesApi: OpenSubtitlesApi,
    private val searchSession: SearchSession,
    private val searchHistoryDao: SearchHistoryDao,
    private val settings: SettingsDataStore,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SearchUiState(useSeasonEpisodeTextFields = settings.useSeasonEpisodeTextFields)
    )
    val uiState: StateFlow<SearchUiState> = _uiState

    private var suggestJob: Job? = null

    init {
        refreshUsage()
        // Load recent titles (movies + TV) for the home carousel
        searchHistoryDao.getRecentTitles()
            .onEach { items -> _uiState.value = _uiState.value.copy(recentShows = items) }
            .launchIn(viewModelScope)

        // Pending browse title (from History screen "Browse Episodes")
        val pending = searchSession.pendingBrowseTitle
        val pendingLangs = searchSession.pendingBrowseLangs
        val pendingTitle = searchSession.pendingSelectedFeatureTitle
        val pendingSelectedType = searchSession.pendingSelectedFeatureType
        val pendingSelectedPoster = searchSession.pendingSelectedFeaturePoster
        val pendingSelectedImdbId = searchSession.pendingSelectedFeatureImdbId
        val pendingSelectedTmdbId = searchSession.pendingSelectedFeatureTmdbId
        val pendingSelectedSeasons = searchSession.pendingSelectedFeatureSeasons
        val pendingSelectedEpisodes = searchSession.pendingSelectedFeatureEpisodes
        searchSession.clearSelectionState()

        if (!pending.isNullOrBlank()) {
            val langSet = pendingLangs?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }?.toSet()
                ?: setOf("en", "he")
            _uiState.value = _uiState.value.copy(
                query = pending,
                selectedLanguages = langSet,
                showSuggestions = true,
                suggestionsLoading = true,
            )
            fetchSuggestions(pending)
        }

        // Pending selected feature (from TitleBrowserScreen)
        if (!pendingTitle.isNullOrBlank()) {
            val isTv = pendingSelectedType == "tv"
            searchSession.posterUrl = pendingSelectedPoster
            searchSession.movieTitle = pendingTitle
            searchSession.imdbId = pendingSelectedImdbId?.toString()
            searchSession.tmdbId = pendingSelectedTmdbId
            searchSession.contentType = pendingSelectedType ?: "movie"
            searchSession.seasonsCount = pendingSelectedSeasons
            searchSession.episodesCount = pendingSelectedEpisodes
            _uiState.value = _uiState.value.copy(
                query = pendingTitle,
                showSuggestions = false,
                selectedPosterUrl = searchSession.pendingSelectedFeaturePoster,
                selectedMovieTitle = pendingTitle,
                seasonsCount = searchSession.pendingSelectedFeatureSeasons ?: 0,
                episodesCount = searchSession.pendingSelectedFeatureEpisodes ?: 0,
                isMovie = !isTv,
                season = if (isTv) "1" else "",
                episode = if (isTv) "1" else "",
            )
            saveSelectedTitleToHistory(
                title = pendingTitle,
                isTv = isTv,
                posterUrl = pendingSelectedPoster,
                imdbId = pendingSelectedImdbId?.toString(),
                tmdbId = pendingSelectedTmdbId,
                seasonsCount = pendingSelectedSeasons,
            )
            searchSession.pendingSelectedFeatureTitle = null
                val pendingTmdbId = searchSession.pendingSelectedFeatureTmdbId
                if (isTv && pendingTmdbId != null) {
                    viewModelScope.launch {
                        hydrateSelectedSeries(
                            tmdbId = pendingTmdbId!!,
                            fallbackPosterUrl = searchSession.pendingSelectedFeaturePoster,
                            title = pendingTitle,
                        )
                    }
                } else if (pendingSelectedPoster.isNullOrBlank()) {
                    viewModelScope.launch {
                        hydrateVisualsFromTitle(
                            title = pendingTitle,
                            isMovie = !isTv,
                            fallbackPosterUrl = pendingSelectedPoster,
                        )
                    }
                }
                searchSession.pendingSelectedFeaturePoster = null
            searchSession.pendingSelectedFeatureImdbId = null
            searchSession.pendingSelectedFeatureTmdbId = null
            searchSession.pendingSelectedFeatureType = null
            searchSession.pendingSelectedFeatureSeasons = null
            searchSession.pendingSelectedFeatureEpisodes = null
            searchSession.pendingSelectedFeatureId = null
        }
    }

    fun refreshUsage() {
        _uiState.value = _uiState.value.copy(
            starBalance = settings.starBalance,
        )
    }

    fun onQueryChange(query: String) {
        if (query != _uiState.value.query) {
            clearSearchSessionSelection()
        }
        _uiState.value = _uiState.value.copy(
            query = query,
            suggestions = if (query.length < 2) emptyList() else _uiState.value.suggestions,
            combinedSuggestions = if (query.length < 2) emptyList() else _uiState.value.combinedSuggestions,
            showSuggestions = query.length >= 2,
            suggestionsError = null,
            selectedPosterUrl = null,
            selectedMovieTitle = null,
            season = "",
            episode = "",
            seasonsCount = 0,
            episodesCount = 0,
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
            val historyItems = runCatching { searchHistoryDao.searchByPrefix(query) }
                .getOrDefault(emptyList())
            runCatching { tmdbApi.searchMulti(query) }
                .onSuccess { response ->
                    val mapped = response.results
                        .filter { it.mediaType != "person" }
                        .map { it.toFeatureDto() }
                    val remoteResults = rankSuggestions(mapped, query).take(20)
                    val historySuggestions = historyItems.map { Suggestion.History(it) }
                    val remoteSuggestions = remoteResults.map { Suggestion.Remote(it) }
                    val combined = (historySuggestions + remoteSuggestions).take(12)
                    _uiState.value = _uiState.value.copy(
                        suggestions = remoteResults,
                        combinedSuggestions = combined,
                        showSuggestions = combined.isNotEmpty(),
                        suggestionsLoading = false,
                        suggestionsError = null,
                    )
                }
                .onFailure { throwable ->
                    Log.e("SearchViewModel", "Suggestions fetch failed: ${throwable.message}")
                    // Fallback to OpenSubtitles features if TMDB fails (e.g. missing API key)
                    runCatching { openSubtitlesApi.searchFeatures(query) }
                        .onSuccess { os ->
                            val remoteResults = rankSuggestions(os.data, query).take(20)
                            val historySuggestions = historyItems.map { Suggestion.History(it) }
                            val remoteSuggestions = remoteResults.map { Suggestion.Remote(it) }
                            val combined = (historySuggestions + remoteSuggestions).take(12)
                            _uiState.value = _uiState.value.copy(
                                suggestions = remoteResults,
                                combinedSuggestions = combined,
                                showSuggestions = combined.isNotEmpty(),
                                suggestionsLoading = false,
                                suggestionsError = null,
                            )
                        }
                        .onFailure {
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
    }

    private fun rankSuggestions(results: List<FeatureDto>, query: String): List<FeatureDto> {
        val q = query.lowercase().trim()
        val qWords = q.split(Regex("\\s+")).filter { it.isNotBlank() }
        return results.mapIndexed { index, feature ->
            val title = feature.attributes.title?.lowercase() ?: ""
            val attrs = feature.attributes
            var score = 0
            // Keep strong bias to API order (usually already popularity-tuned server-side)
            score += (results.size - index) * 5
            // 1. Exact prefix match is highest signal
            if (title.startsWith(q)) score += 100
            // 2. Query matches at word boundary
            if (title.split(" ").any { it.startsWith(q) }) score += 50
            // 2.5 Fuzzy per-word match to handle typos like "bpys" -> "boys"
            if (qWords.isNotEmpty()) {
                val titleWords = title.split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }
                val fuzzyMatches = qWords.count { qw ->
                    titleWords.any { tw ->
                        tw == qw || tw.startsWith(qw) || editDistance(tw, qw) <= 1
                    }
                }
                score += fuzzyMatches * 30
            }
            // 3. Popular TV shows (many seasons/episodes)
            score += (attrs.seasonsCount ?: 0) * 10
            score += minOf((attrs.episodesCount ?: 0) / 10, 50)
            // 4. Recent content (2010+ gets bonus)
            val year = attrs.year ?: 2000
            if (year >= 2010) score += 20
            if (year >= 2015) score += 10
            // 5. Movies get slight boost for recency (higher IMDB ID = more recent)
            if (attrs.featureType == "Movie") score += 5
            feature to score
        }.sortedByDescending { it.second }.map { it.first }
    }

    private fun editDistance(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        val dp = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            var prev = dp[0]
            dp[0] = i
            for (j in 1..b.length) {
                val tmp = dp[j]
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[j] = minOf(
                    dp[j] + 1,
                    dp[j - 1] + 1,
                    prev + cost,
                )
                prev = tmp
            }
        }
        return dp[b.length]
    }
    fun onSuggestionSelected(feature: FeatureDto) {
        analyticsTracker.track(
            "search_suggestion_selected",
            mapOf("type" to (feature.type ?: "unknown"))
        )
        val title = feature.attributes.title ?: feature.attributes.originalTitle ?: ""
        val posterUrl = feature.attributes.imgUrl
        val imdbId = feature.attributes.imdbId?.toString()
        val isTv = feature.type == "tv" ||
                feature.attributes.featureType?.lowercase() == "tvshow"
        val contentType = if (isTv) "tv" else "movie"

        searchSession.posterUrl = posterUrl
        searchSession.movieTitle = title
        searchSession.imdbId = imdbId
        searchSession.tmdbId = feature.attributes.tmdbId
        searchSession.contentType = contentType
        searchSession.pendingSelectedFeatureId = feature.id
        searchSession.pendingSelectedFeatureTitle = title
        searchSession.pendingSelectedFeaturePoster = posterUrl
        searchSession.pendingSelectedFeatureImdbId = imdbId?.removePrefix("tt")?.toIntOrNull()
        searchSession.pendingSelectedFeatureTmdbId = feature.attributes.tmdbId
        searchSession.pendingSelectedFeatureType = contentType
        searchSession.pendingSelectedFeatureSeasons = feature.attributes.seasonsCount
        searchSession.pendingSelectedFeatureEpisodes = feature.attributes.episodesCount

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
        saveSelectedTitleToHistory(
            title = title,
            isTv = isTv,
            posterUrl = posterUrl,
            imdbId = imdbId,
            tmdbId = feature.attributes.tmdbId,
            seasonsCount = feature.attributes.seasonsCount,
        )
        val featureTmdbId = feature.attributes.tmdbId
        if (isTv && featureTmdbId != null) {
            viewModelScope.launch {
                hydrateSelectedSeries(
                    tmdbId = feature.attributes.tmdbId,
                    fallbackPosterUrl = posterUrl,
                    title = title,
                )
                refreshEpisodeCountForSeason(1)
            }
        }
    }

    fun clearSelectedTitle() {
        clearSearchSessionSelection()
        _uiState.value = _uiState.value.copy(
            selectedMovieTitle = null,
            selectedPosterUrl = null,
            season = "",
            episode = "",
            seasonsCount = 0,
            episodesCount = 0,
            isMovie = false,
        )
    }

    fun onHistorySuggestionSelected(item: SearchHistoryEntity) {
        analyticsTracker.track("search_history_suggestion_selected")
        val normalizedQuery = Uri.decode(item.query)
        searchSession.movieTitle  = normalizedQuery
        searchSession.season      = item.season
        searchSession.episode     = item.episode
        searchSession.languages = item.languages ?: "en,he"
        val isTvHistoryItem =
            item.contentType == "tv" ||
                item.season != null ||
                item.episode != null ||
                (item.seasonsCount ?: 0) > 0
        searchSession.contentType = if (isTvHistoryItem) "tv" else "movie"
        searchSession.imdbId      = item.imdbId
        searchSession.tmdbId      = item.tmdbId
        searchSession.posterUrl   = item.posterUrl
        searchSession.seasonsCount = item.seasonsCount
        searchSession.pendingSelectedFeatureTitle = normalizedQuery
        searchSession.pendingSelectedFeaturePoster = item.posterUrl
        searchSession.pendingSelectedFeatureImdbId = item.imdbId?.removePrefix("tt")?.toIntOrNull()
        searchSession.pendingSelectedFeatureTmdbId = item.tmdbId
        searchSession.pendingSelectedFeatureType = if (isTvHistoryItem) "tv" else "movie"
        searchSession.pendingSelectedFeatureSeasons = item.seasonsCount
        _uiState.value = _uiState.value.copy(
            query = normalizedQuery,
            showSuggestions = false,
            combinedSuggestions = emptyList(),
            suggestions = emptyList(),
            suggestionsLoading = false,
            suggestionsError = null,
            selectedPosterUrl = item.posterUrl,
            selectedMovieTitle = normalizedQuery,
            imdbId = item.imdbId.orEmpty(),
            season = item.season?.toString().orEmpty(),
            episode = item.episode?.toString().orEmpty(),
            seasonsCount = item.seasonsCount ?: 0,
            episodesCount = 0,
            isMovie = !isTvHistoryItem,
        )
        viewModelScope.launch {
            hydrateVisualsFromTitle(
                title = normalizedQuery,
                isMovie = !isTvHistoryItem,
                fallbackPosterUrl = item.posterUrl,
            )
        }
    }

    fun dismissSuggestions() {
        _uiState.value = _uiState.value.copy(showSuggestions = false)
    }

    /**
     * Resolves the true next episode via API (checks same season, then season+1 E1),
     * updates the session, and calls [onReady] with the query string.
     * Shows a loading indicator on the card while the API call is in flight.
     */
    fun findNextEpisode(item: SearchHistoryEntity, onReady: (String) -> Unit) {
        _uiState.value = _uiState.value.copy(nextEpisodeLoadingId = item.id)
        viewModelScope.launch {
            val normalizedQuery = Uri.decode(item.query)
            val season = item.season ?: 1
            val candidateEp = (item.episode ?: 0) + 1
            val imdbIdInt = parseImdbId(item.imdbId)
            val isTvHistoryItem =
                item.contentType == "tv" ||
                    item.season != null ||
                    item.episode != null ||
                    (item.seasonsCount ?: 0) > 0
            val tvMatch = if (isTvHistoryItem && item.tmdbId == null) {
                findTvMatch(normalizedQuery)
            } else {
                null
            }
            val resolvedTmdbId = item.tmdbId ?: tvMatch?.id
            val resolvedPosterUrl = item.posterUrl ?: tvMatch?.toFeatureDto()?.attributes?.imgUrl
            val resolvedSeasonCount = item.seasonsCount ?: resolvedTmdbId?.let { tmdbId ->
                runCatching { tmdbApi.tvDetails(tmdbId).numberOfSeasons }.getOrNull()
            }

            val (resolvedSeason, resolvedEp) = resolveNextEpisode(
                title = normalizedQuery,
                tmdbId = resolvedTmdbId,
                imdbId = imdbIdInt,
                season = season,
                candidateEp = candidateEp,
                seasonsCount = resolvedSeasonCount,
                languages = item.languages,
            )

            searchSession.movieTitle = normalizedQuery
            searchSession.season = resolvedSeason
            searchSession.episode = resolvedEp
            searchSession.languages = item.languages ?: "en,he"
            searchSession.contentType = if (isTvHistoryItem) "tv" else "movie"
            searchSession.posterUrl = resolvedPosterUrl
            searchSession.imdbId = item.imdbId
            searchSession.tmdbId = resolvedTmdbId
            searchSession.seasonsCount = resolvedSeasonCount
            searchSession.rememberSelectedFeature()

            _uiState.value = _uiState.value.copy(
                nextEpisodeLoadingId = null,
                query = normalizedQuery,
                selectedMovieTitle = normalizedQuery,
                selectedPosterUrl = resolvedPosterUrl,
                isMovie = !isTvHistoryItem,
                season = resolvedSeason.toString(),
                episode = resolvedEp.toString(),
                seasonsCount = resolvedSeasonCount ?: 0,
            )
            saveSearchHistory(
                query = normalizedQuery,
                season = resolvedSeason,
                episode = resolvedEp,
                languages = searchSession.languages,
                contentType = searchSession.contentType,
                posterUrl = resolvedPosterUrl,
                imdbId = item.imdbId,
                tmdbId = resolvedTmdbId,
                seasonsCount = resolvedSeasonCount,
            )
            onReady(normalizedQuery)
        }
    }

    private suspend fun resolveNextEpisode(
        title: String,
        tmdbId: Int?,
        imdbId: Int?,
        season: Int,
        candidateEp: Int,
        seasonsCount: Int?,
        languages: String?,
    ): Pair<Int, Int> {
        if (tmdbId != null) {
            val sameSeasonEpisode = runCatching {
                tmdbApi.tvSeasonDetails(tmdbId, season).episodes
                    .mapNotNull { it.episodeNumber }
                    .filter { it >= candidateEp }
                    .minOrNull()
            }.getOrNull()
            if (sameSeasonEpisode != null) return Pair(season, sameSeasonEpisode)

            val hasNextSeason = seasonsCount == null || season < seasonsCount
            if (hasNextSeason) {
                val nextSeasonEpisode = runCatching {
                    tmdbApi.tvSeasonDetails(tmdbId, season + 1).episodes
                        .mapNotNull { it.episodeNumber }
                        .minOrNull()
                }.getOrNull()
                if (nextSeasonEpisode != null) return Pair(season + 1, nextSeasonEpisode)
            }
        }

        // 1. Check if next episode exists in same season
        val sameSeasonHits = runCatching {
            openSubtitlesApi.searchSubtitles(
                query = if (tmdbId == null && imdbId == null) title else null,
                tmdbId = tmdbId,
                imdbId = imdbId,
                season = season,
                episode = candidateEp,
                languages = languages,
                page = 1,
            ).totalCount
        }.getOrElse { 0 }

        if (sameSeasonHits > 0) return Pair(season, candidateEp)

        // 2. No subtitles found — try next season if one is known to exist
        val hasNextSeason = seasonsCount == null || season < seasonsCount
        if (!hasNextSeason) return Pair(season, candidateEp) // keep as-is, search will show no results

        val nextSeasonHits = runCatching {
            openSubtitlesApi.searchSubtitles(
                query = if (tmdbId == null && imdbId == null) title else null,
                tmdbId = tmdbId,
                imdbId = imdbId,
                season = season + 1,
                episode = 1,
                languages = languages,
                page = 1,
            ).totalCount
        }.getOrElse { 0 }

        return if (nextSeasonHits > 0) Pair(season + 1, 1) else Pair(season, candidateEp)
    }

    private suspend fun findTvMatch(title: String) =
        runCatching {
            val normalizedTitle = title.trim().lowercase()
            tmdbApi.searchMulti(title).results
                .filter { it.mediaType == "tv" }
                .sortedByDescending { result ->
                    val candidate = (result.name ?: result.title).orEmpty().trim().lowercase()
                    when {
                        candidate == normalizedTitle -> 3
                        candidate.startsWith(normalizedTitle) -> 2
                        else -> 1
                    }
                }
                .firstOrNull()
        }.getOrNull()

    private fun parseImdbId(value: String?): Int? {
        return value
            ?.trim()
            ?.removePrefix("tt")
            ?.trimStart('0')
            ?.ifBlank { "0" }
            ?.toIntOrNull()
    }

    private suspend fun hydrateSelectedSeries(
        tmdbId: Int,
        fallbackPosterUrl: String?,
        title: String,
    ) {
        runCatching { tmdbApi.tvDetails(tmdbId) }
            .onSuccess { details ->
                if (_uiState.value.selectedMovieTitle != title) return@onSuccess
                val seasonsCount = details.numberOfSeasons ?: 0
                val episodesCount = details.numberOfEpisodes ?: 0
                val imageUrl = details.heroImageUrl() ?: fallbackPosterUrl
                searchSession.posterUrl = imageUrl
                searchSession.seasonsCount = seasonsCount
                searchSession.episodesCount = episodesCount
                searchSession.rememberSelectedFeature()
                saveSelectedTitleToHistory(
                    title = title,
                    isTv = true,
                    posterUrl = imageUrl,
                    imdbId = searchSession.imdbId,
                    tmdbId = tmdbId,
                    seasonsCount = seasonsCount,
                )
                _uiState.value = _uiState.value.copy(
                    selectedMovieTitle = title,
                    selectedPosterUrl = imageUrl,
                    seasonsCount = seasonsCount,
                    episodesCount = episodesCount,
                    isMovie = false,
                )
            }
            .onFailure { throwable ->
                Log.w(
                    "SearchViewModel",
                    "TMDB details lookup failed for $tmdbId: ${throwable.message}",
                )
            }
    }

    private suspend fun hydrateVisualsFromTitle(
        title: String,
        isMovie: Boolean,
        fallbackPosterUrl: String?,
    ) {
        runCatching { tmdbApi.searchMulti(title) }
            .onSuccess { response ->
                val match = response.results.firstOrNull { result ->
                    if (isMovie) result.mediaType == "movie" else result.mediaType == "tv"
                } ?: return@onSuccess

                val posterUrl = match.toFeatureDto().attributes.imgUrl ?: fallbackPosterUrl
                searchSession.posterUrl = posterUrl

                if (!isMovie) {
                    hydrateSelectedSeries(
                        tmdbId = match.id,
                        fallbackPosterUrl = posterUrl,
                        title = title,
                    )
                } else {
                    _uiState.value = _uiState.value.copy(selectedPosterUrl = posterUrl)
                }
            }
    }

    fun onImdbIdChange(id: String) { _uiState.value = _uiState.value.copy(imdbId = id) }
    fun onSeasonChange(s: String) {
        _uiState.value = _uiState.value.copy(
            season = if (_uiState.value.season == s) "" else s
        )
        val seasonNumber = _uiState.value.season.toIntOrNull()
        if (!_uiState.value.isMovie && seasonNumber != null) {
            viewModelScope.launch {
                refreshEpisodeCountForSeason(seasonNumber)
            }
        }
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
        analyticsTracker.track(
            "search_submitted",
            mapOf(
                "mode" to state.searchMode.name.lowercase(),
                "has_season_episode" to (state.season.isNotBlank() || state.episode.isNotBlank()).toString(),
                "languages_count" to state.selectedLanguages.size.toString(),
                "search_query" to state.query.trim(),
            )
        )
        val hasSelectedTitle = !state.selectedMovieTitle.isNullOrBlank()
        if (!hasSelectedTitle) {
            searchSession.clearSelectionState()
            searchSession.clearPendingSelectedFeature()
            searchSession.movieTitle = state.query.ifBlank { null }
            searchSession.contentType = "movie"
        }
        // Persist search params to session — ResultsViewModel runs the actual search
        searchSession.season = state.season.toIntOrNull()
        searchSession.episode = state.episode.toIntOrNull()
        searchSession.seasonsCount = state.seasonsCount.takeIf { it > 0 }
        searchSession.episodesCount = state.episodesCount.takeIf { it > 0 }
        searchSession.languages = state.selectedLanguages.joinToString(",")
        if (!state.isMovie && hasSelectedTitle) {
            searchSession.rememberSelectedFeature()
        }
        if (state.searchMode == SearchMode.IMDB_ID || state.imdbId.isNotBlank()) {
            searchSession.imdbId = state.imdbId.ifBlank { null }
        } else if (!hasSelectedTitle) {
            searchSession.imdbId = null
        }
        // Collapse autocomplete
        _uiState.value = state.copy(
            showSuggestions = false,
            suggestionsLoading = false,
        )
        // Save to search history (fire-and-forget)
        val queryToSave = if (state.searchMode == SearchMode.TITLE) state.query.ifBlank { state.imdbId } else state.imdbId
        val normalizedQueryToSave = Uri.decode(queryToSave)
        if (normalizedQueryToSave.isNotBlank()) {
            viewModelScope.launch {
                runCatching {
                    saveSearchHistory(
                        query = normalizedQueryToSave,
                        season = state.season.toIntOrNull(),
                        episode = state.episode.toIntOrNull(),
                        languages = state.selectedLanguages.joinToString(","),
                        contentType = searchSession.contentType,
                        posterUrl = state.selectedPosterUrl,
                        imdbId = searchSession.imdbId,
                        tmdbId = searchSession.tmdbId,
                        seasonsCount = state.seasonsCount.takeIf { it > 0 },
                    )
                }
            }
        }
    }

    fun prepareTitleBrowse() {
        val state = _uiState.value
        searchSession.languages = state.selectedLanguages.joinToString(",")
        searchSession.pendingBrowseLangs = searchSession.languages
    }

    private suspend fun refreshEpisodeCountForSeason(seasonNumber: Int) {
        val tmdbId = searchSession.tmdbId ?: return
        val episodeCount = runCatching {
            tmdbApi.tvSeasonDetails(tmdbId, seasonNumber).episodes
                .mapNotNull { it.episodeNumber }
                .maxOrNull()
                ?: 0
        }.getOrElse { 0 }

        if (episodeCount <= 0) return

        searchSession.episodesCount = episodeCount
        searchSession.rememberSelectedFeature()

        val clampedEpisode = (_uiState.value.episode.toIntOrNull() ?: 1).coerceIn(1, episodeCount)
        _uiState.value = _uiState.value.copy(
            episodesCount = episodeCount,
            episode = clampedEpisode.toString(),
        )
    }

    private fun clearSearchSessionSelection() {
        searchSession.clearSelectionState()
        searchSession.clearPendingSelectedFeature()
    }

    private fun saveSelectedTitleToHistory(
        title: String,
        isTv: Boolean,
        posterUrl: String?,
        imdbId: String?,
        tmdbId: Int?,
        seasonsCount: Int?,
    ) {
        if (title.isBlank()) return
        val state = _uiState.value
        viewModelScope.launch {
            runCatching {
                saveSearchHistory(
                    query = Uri.decode(title),
                    season = if (isTv) 1 else null,
                    episode = if (isTv) 1 else null,
                    languages = state.selectedLanguages.joinToString(","),
                    contentType = if (isTv) "tv" else "movie",
                    posterUrl = posterUrl,
                    imdbId = imdbId,
                    tmdbId = tmdbId,
                    seasonsCount = seasonsCount,
                )
            }
        }
    }

    private suspend fun saveSearchHistory(
        query: String,
        season: Int?,
        episode: Int?,
        languages: String?,
        contentType: String?,
        posterUrl: String?,
        imdbId: String?,
        tmdbId: Int?,
        seasonsCount: Int?,
    ) {
        searchHistoryDao.insert(
            SearchHistoryEntity(
                query = query,
                season = season,
                episode = episode,
                languages = languages,
                contentType = contentType,
                posterUrl = posterUrl,
                imdbId = imdbId,
                tmdbId = tmdbId,
                seasonsCount = seasonsCount,
            ),
        )
    }
}
