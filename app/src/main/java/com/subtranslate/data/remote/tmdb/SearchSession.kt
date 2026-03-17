package com.subtranslate.data.remote.tmdb

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton that carries the currently-selected movie's metadata between
 * SearchScreen → ResultsScreen so we can show the poster without encoding
 * a URL in nav args.
 */
@Singleton
class SearchSession @Inject constructor() {
    var posterUrl: String? = null
    var movieTitle: String? = null
    var imdbId: String? = null
}
