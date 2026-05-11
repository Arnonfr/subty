package com.subtranslate.data.remote.tmdb

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.subtranslate.data.remote.opensubtitles.dto.FeatureAttributesDto
import com.subtranslate.data.remote.opensubtitles.dto.FeatureDto

@JsonClass(generateAdapter = true)
data class TmdbSearchResponse(
    @Json(name = "results") val results: List<TmdbItem> = emptyList(),
    @Json(name = "total_results") val totalResults: Int = 0,
    @Json(name = "total_pages") val totalPages: Int = 1,
    @Json(name = "page") val page: Int = 1,
)

@JsonClass(generateAdapter = true)
data class TmdbItem(
    @Json(name = "id") val id: Int,
    @Json(name = "media_type") val mediaType: String,
    // Movies use "title"; TV shows use "name"
    @Json(name = "title") val title: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "original_title") val originalTitle: String? = null,
    @Json(name = "original_name") val originalName: String? = null,
    @Json(name = "release_date") val releaseDate: String? = null,
    @Json(name = "first_air_date") val firstAirDate: String? = null,
    @Json(name = "poster_path") val posterPath: String? = null,
    @Json(name = "popularity") val popularity: Double? = null,
    @Json(name = "vote_average") val voteAverage: Double? = null,
)

private const val TMDB_IMAGE_BASE = "https://image.tmdb.org/t/p/w342"

fun TmdbItem.toFeatureDto(): FeatureDto {
    val isTv = mediaType == "tv"
    val displayTitle = if (isTv) name else title
    val displayOriginalTitle = if (isTv) originalName else originalTitle
    val year = (if (isTv) firstAirDate else releaseDate)?.take(4)?.toIntOrNull()
    val posterUrl = posterPath?.let { "$TMDB_IMAGE_BASE$it" }
    return FeatureDto(
        id = "tmdb_${mediaType}_$id",
        type = if (isTv) "tv" else "movie",
        attributes = FeatureAttributesDto(
            title = displayTitle,
            originalTitle = displayOriginalTitle,
            year = year,
            imdbId = null,
            tmdbId = id,
            featureType = if (isTv) "Tvshow" else "Movie",
            imgUrl = posterUrl,
            seasonsCount = null,
            episodesCount = null,
        ),
    )
}
