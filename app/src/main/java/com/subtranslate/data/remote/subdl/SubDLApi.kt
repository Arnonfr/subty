package com.subtranslate.data.remote.subdl

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

// ── DTOs ─────────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class SubDLSearchResponse(
    @Json(name = "status") val status: Boolean,
    @Json(name = "results") val results: List<SubDLTitleDto>?,
    @Json(name = "subtitles") val subtitles: List<SubDLSubtitleDto>?,
)

@JsonClass(generateAdapter = true)
data class SubDLTitleDto(
    @Json(name = "sd_id")   val sdId: Int,
    @Json(name = "type")    val type: String?,
    @Json(name = "name")    val name: String?,
    @Json(name = "imdb_id") val imdbId: String?,
    @Json(name = "tmdb_id") val tmdbId: Int?,
    @Json(name = "first_air_date") val firstAirDate: String?,
    @Json(name = "release_date")   val releaseDate: String?,
    @Json(name = "poster")  val poster: String?,
)

@JsonClass(generateAdapter = true)
data class SubDLSubtitleDto(
    @Json(name = "release_name") val releaseName: String?,
    @Json(name = "name")         val name: String?,
    @Json(name = "lang")         val lang: String?,
    @Json(name = "author")       val author: String?,
    @Json(name = "url")          val url: String,       // e.g. "/subtitle/12345-name.zip"
    @Json(name = "hi")           val hi: Boolean?,
    @Json(name = "season")       val season: Int?,
    @Json(name = "episode")      val episode: Int?,
    @Json(name = "ratings")      val ratings: Double?,
    @Json(name = "sd_id")        val sdId: Int?,
)

// ── Retrofit interface ────────────────────────────────────────────────────────

interface SubDLApi {
    /**
     * Search for subtitles.
     * languages: comma-separated 2-letter codes, e.g. "EN,HE"
     * type: "movie" | "tv"
     */
    @GET("subtitles/")
    suspend fun searchSubtitles(
        @Query("api_key")        apiKey: String,
        @Query("title")          title: String?    = null,
        @Query("imdb_id")        imdbId: String?   = null,
        @Query("tmdb_id")        tmdbId: Int?      = null,
        @Query("season_number")  season: Int?      = null,
        @Query("episode_number") episode: Int?     = null,
        @Query("languages")      languages: String? = null,
        @Query("type")           type: String?     = null,
    ): SubDLSearchResponse
}
