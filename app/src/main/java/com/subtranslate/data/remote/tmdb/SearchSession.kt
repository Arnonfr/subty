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
}
