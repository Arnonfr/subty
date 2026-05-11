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
    var languages: String? = null
    /** "movie" | "tv" | null — set from autocomplete suggestion */
    var contentType: String? = null
    /** Set when navigating from history "Browse Episodes" — pre-populates SearchScreen with the show */
    var pendingBrowseTitle: String? = null
    var pendingBrowseLangs: String? = null
    /** Set by TitleBrowserScreen when user picks a title — consumed by SearchViewModel.init */
    /** TMDB content ID — used for subtitle search when imdbId is unavailable */
    var tmdbId: Int? = null
    /** Set by TitleBrowserScreen when user picks a title — consumed by SearchViewModel.init */
    var pendingSelectedFeatureId: String? = null
    var pendingSelectedFeatureTitle: String? = null
    var pendingSelectedFeaturePoster: String? = null
    var pendingSelectedFeatureImdbId: Int? = null
    var pendingSelectedFeatureTmdbId: Int? = null
    var pendingSelectedFeatureType: String? = null  // "tv" | "movie"
    var pendingSelectedFeatureSeasons: Int? = null
    var pendingSelectedFeatureEpisodes: Int? = null
}
