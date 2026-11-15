package com.subtranslate.data.remote.tmdb

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchSession @Inject constructor() {
    var posterUrl: String? = null
    var movieTitle: String? = null
    var imdbId: String? = null
    var season: Int? = null
    var episode: Int? = null
    var languages: String = "en,he"
    var contentType: String = "movie"
    var tmdbId: Int? = null
    var seasonsCount: Int? = null
    var episodesCount: Int? = null

    var pendingBrowseTitle: String? = null
    var pendingBrowseLangs: String? = null

    var pendingSelectedFeatureId: String? = null
    var pendingSelectedFeatureTitle: String? = null
    var pendingSelectedFeaturePoster: String? = null
    var pendingSelectedFeatureImdbId: Int? = null
    var pendingSelectedFeatureTmdbId: Int? = null
    var pendingSelectedFeatureType: String? = null
    var pendingSelectedFeatureSeasons: Int? = null
    var pendingSelectedFeatureEpisodes: Int? = null

    fun clearSelectionState() {
        posterUrl = null
        movieTitle = null
        imdbId = null
        season = null
        episode = null
        contentType = "movie"
        tmdbId = null
        seasonsCount = null
        episodesCount = null
    }

    fun rememberSelectedFeature() {
        pendingSelectedFeatureTitle = movieTitle
        pendingSelectedFeaturePoster = posterUrl
        pendingSelectedFeatureImdbId = imdbId?.removePrefix("tt")?.toIntOrNull()
        pendingSelectedFeatureTmdbId = tmdbId
        pendingSelectedFeatureType = contentType
        pendingSelectedFeatureSeasons = seasonsCount
        pendingSelectedFeatureEpisodes = episodesCount
    }

    fun clearPendingSelectedFeature() {
        pendingSelectedFeatureId = null
        pendingSelectedFeatureTitle = null
        pendingSelectedFeaturePoster = null
        pendingSelectedFeatureImdbId = null
        pendingSelectedFeatureTmdbId = null
        pendingSelectedFeatureType = null
        pendingSelectedFeatureSeasons = null
        pendingSelectedFeatureEpisodes = null
    }
}
