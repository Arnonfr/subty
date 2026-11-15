package com.subtranslate.data.remote.tmdb

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TmdbTvDetailsResponse(
    @Json(name = "number_of_seasons") val numberOfSeasons: Int? = null,
    @Json(name = "number_of_episodes") val numberOfEpisodes: Int? = null,
    @Json(name = "poster_path") val posterPath: String? = null,
    @Json(name = "backdrop_path") val backdropPath: String? = null,
)

private const val TMDB_IMAGE_BASE = "https://image.tmdb.org/t/p"

fun TmdbTvDetailsResponse.heroImageUrl(): String? {
    return backdropPath?.tmdbImageUrl("w780") ?: posterPath?.tmdbImageUrl("w780")
}

private fun String.tmdbImageUrl(size: String): String = "$TMDB_IMAGE_BASE/$size$this"
