package com.subtranslate.data.remote.opensubtitles

import com.subtranslate.data.remote.opensubtitles.dto.*
import retrofit2.http.*

interface OpenSubtitlesApi {

    @GET("subtitles")
    suspend fun searchSubtitles(
        @Query("query") query: String? = null,
        @Query("imdb_id") imdbId: Int? = null,
        @Query("tmdb_id") tmdbId: Int? = null,
        @Query("languages") languages: String? = null,
        @Query("moviehash") movieHash: String? = null,
        @Query("season_number") season: Int? = null,
        @Query("episode_number") episode: Int? = null,
        @Query("type") type: String? = null,
        @Query("order_by") orderBy: String = "download_count",
        @Query("page") page: Int = 1
    ): SubtitleSearchResponse

    @GET("features")
    suspend fun searchFeatures(
        @Query("query") query: String,
        @Query("type") type: String? = null
    ): FeatureSearchResponse

    @POST("download")
    suspend fun requestDownload(
        @Body request: DownloadRequest
    ): DownloadResponse

    @POST("login")
    suspend fun login(
        @Body credentials: LoginRequest
    ): LoginResponse

    @DELETE("logout")
    suspend fun logout(): Unit
}
