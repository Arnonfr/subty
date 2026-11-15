package com.subtranslate.data.remote.tmdb

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TmdbSeasonDetailsResponse(
    @Json(name = "episodes") val episodes: List<TmdbSeasonEpisodeDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class TmdbSeasonEpisodeDto(
    @Json(name = "episode_number") val episodeNumber: Int? = null,
)
