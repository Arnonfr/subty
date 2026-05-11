package com.subtranslate.presentation.titlebrowser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subtranslate.data.remote.opensubtitles.dto.FeatureDto
import com.subtranslate.data.remote.tmdb.SearchSession
import com.subtranslate.data.remote.tmdb.TmdbApi
import com.subtranslate.data.remote.tmdb.toFeatureDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TitleSortOrder { POPULARITY, YEAR_DESC, YEAR_ASC }
enum class TitleTypeFilter { ALL, MOVIE, TV }
enum class TitleViewMode { LIST, GRID }

data class TitleBrowserUiState(
    val query: String = "",
    val allResults: List<FeatureDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val sortOrder: TitleSortOrder = TitleSortOrder.POPULARITY,
    val typeFilter: TitleTypeFilter = TitleTypeFilter.ALL,
    val viewMode: TitleViewMode = TitleViewMode.LIST,
) {
    val displayResults: List<FeatureDto>
        get() {
            val filtered = when (typeFilter) {
                TitleTypeFilter.ALL -> allResults
                TitleTypeFilter.MOVIE -> allResults.filter {
                    it.type != "tv" && it.attributes.featureType?.lowercase() != "tvshow"
                }
                TitleTypeFilter.TV -> allResults.filter {
                    it.type == "tv" || it.attributes.featureType?.lowercase() == "tvshow"
                }
            }
            return when (sortOrder) {
                TitleSortOrder.POPULARITY -> filtered
                TitleSortOrder.YEAR_DESC -> filtered.sortedByDescending { it.attributes.year ?: 0 }
                TitleSortOrder.YEAR_ASC -> filtered.sortedBy { it.attributes.year ?: 9999 }
            }
        }
}

@HiltViewModel
class TitleBrowserViewModel @Inject constructor(
    private val tmdbApi: TmdbApi,
    private val searchSession: SearchSession,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TitleBrowserUiState())
    val uiState: StateFlow<TitleBrowserUiState> = _uiState

    fun load(query: String) {
        if (_uiState.value.query == query && _uiState.value.allResults.isNotEmpty()) return
        _uiState.value = TitleBrowserUiState(query = query, isLoading = true)
        viewModelScope.launch {
            runCatching { tmdbApi.searchMulti(query) }
                .onSuccess { response ->
                    val features = response.results
                        .filter { it.mediaType != "person" }
                        .map { it.toFeatureDto() }
                    _uiState.update { it.copy(allResults = features, isLoading = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load") }
                }
        }
    }

    fun setSortOrder(order: TitleSortOrder) = _uiState.update { it.copy(sortOrder = order) }
    fun setTypeFilter(filter: TitleTypeFilter) = _uiState.update { it.copy(typeFilter = filter) }
    fun toggleViewMode() = _uiState.update {
        it.copy(viewMode = if (it.viewMode == TitleViewMode.LIST) TitleViewMode.GRID else TitleViewMode.LIST)
    }

    fun selectFeature(feature: FeatureDto) {
        val title = feature.attributes.title ?: feature.attributes.originalTitle ?: ""
        val isTv = feature.type == "tv" || feature.attributes.featureType?.lowercase() == "tvshow"
        searchSession.pendingSelectedFeatureId = feature.id
        searchSession.pendingSelectedFeatureTitle = title
        searchSession.pendingSelectedFeaturePoster = feature.attributes.imgUrl
        searchSession.pendingSelectedFeatureImdbId = feature.attributes.imdbId
        searchSession.pendingSelectedFeatureTmdbId = feature.attributes.tmdbId
        searchSession.pendingSelectedFeatureType = if (isTv) "tv" else "movie"
        searchSession.pendingSelectedFeatureSeasons = feature.attributes.seasonsCount
        searchSession.pendingSelectedFeatureEpisodes = feature.attributes.episodesCount
    }
}
