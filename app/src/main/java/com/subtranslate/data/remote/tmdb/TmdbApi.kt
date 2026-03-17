package com.subtranslate.data.remote.tmdb

import retrofit2.http.GET
import retrofit2.http.Query

interface TmdbApi {
    @GET("search/multi")
    suspend fun searchMulti(
        @Query("query") query: String,
        @Query("page") page: Int = 1
    ): TmdbSearchResponse
}

data class TmdbSearchResponse(
    val results: List<TmdbResult> = emptyList()
)

data class TmdbResult(
    val id: Int = 0,
    val media_type: String = "",          // "movie" | "tv"
    val title: String? = null,            // movie
    val name: String? = null,             // tv
    val release_date: String? = null,     // movie
    val first_air_date: String? = null,   // tv
    val poster_path: String? = null,
    val imdb_id: String? = null,
    val overview: String? = null
) {
    val displayTitle: String get() = title ?: name ?: ""
    val year: String get() = (release_date ?: first_air_date)?.take(4) ?: ""
    val posterUrl: String? get() = poster_path?.let { "https://image.tmdb.org/t/p/w92$it" }
}
