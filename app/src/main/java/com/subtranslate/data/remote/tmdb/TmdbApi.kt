package com.subtranslate.data.remote.tmdb

import retrofit2.http.GET
import retrofit2.http.Query

interface TmdbApi {
    @GET("3/search/multi")
    suspend fun searchMulti(
        @Query("query") query: String,
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("page") page: Int = 1,
    ): TmdbSearchResponse
}
