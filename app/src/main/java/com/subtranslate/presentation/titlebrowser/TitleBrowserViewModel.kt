package com.subtranslate.presentation.titlebrowser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subtranslate.data.remote.opensubtitles.OpenSubtitlesApi
import com.subtranslate.data.remote.opensubtitles.dto.FeatureDto
import com.subtranslate.data.remote.tmdb.SearchSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TitleBrowserUiState(
    val query: String = "",
    val results: List<FeatureDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class TitleBrowserViewModel @Inject constructor(
    private val openSubtitlesApi: OpenSubtitlesApi,
    private val searchSession: SearchSession,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TitleBrowserUiState())
    val uiState: StateFlow<TitleBrowserUiState> = _uiState

    fun load(query: String) {
        if (_uiState.value.query == query && _uiState.value.results.isNotEmpty()) return
        _uiState.value = TitleBrowserUiState(query = query, isLoading = true)
        viewModelScope.launch {
            runCatching { openSubtitlesApi.searchFeatures(query) }
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        results = response.data,
                        isLoading = false,
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load results",
                    )
                }
        }
    }

    fun selectFeature(feature: FeatureDto) {
        val title = feature.attributes.title ?: feature.attributes.originalTitle ?: ""
        val isTv = feature.type == "tv" || feature.attributes.featureType?.lowercase() == "tvshow"
        searchSession.pendingSelectedFeatureId = feature.id
        searchSession.pendingSelectedFeatureTitle = title
        searchSession.pendingSelectedFeaturePoster = feature.attributes.imgUrl
        searchSession.pendingSelectedFeatureImdbId = feature.attributes.imdbId
        searchSession.pendingSelectedFeatureType = if (isTv) "tv" else "movie"
        searchSession.pendingSelectedFeatureSeasons = feature.attributes.seasonsCount
        searchSession.pendingSelectedFeatureEpisodes = feature.attributes.episodesCount
    }
}
