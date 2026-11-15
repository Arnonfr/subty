package com.subtranslate.data.remote.tmdb

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApi {
    @GET("3/search/multi")
    suspend fun searchMulti(
        @Query("query") query: String,
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("page") page: Int = 1,
    ): TmdbSearchResponse

    @GET("3/tv/{tv_id}")
    suspend fun tvDetails(
        @Path("tv_id") tvId: Int,
    ): TmdbTvDetailsResponse

    @GET("3/tv/{tv_id}/season/{season_number}")
    suspend fun tvSeasonDetails(
        @Path("tv_id") tvId: Int,
        @Path("season_number") seasonNumber: Int,
    ): TmdbSeasonDetailsResponse
}
