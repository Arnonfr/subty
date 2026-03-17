package com.subtranslate.data.remote.opensubtitles.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SubtitleSearchResponse(
    @Json(name = "total_count") val totalCount: Int,
    @Json(name = "total_pages") val totalPages: Int,
    @Json(name = "page") val page: Int,
    @Json(name = "data") val data: List<SubtitleSearchDto>
)

@JsonClass(generateAdapter = true)
data class SubtitleSearchDto(
    @Json(name = "id") val id: String,
    @Json(name = "type") val type: String,
    @Json(name = "attributes") val attributes: SubtitleAttributesDto
)

@JsonClass(generateAdapter = true)
data class SubtitleAttributesDto(
    @Json(name = "subtitle_id") val subtitleId: String?,
    @Json(name = "language") val language: String?,
    @Json(name = "download_count") val downloadCount: Int?,
    @Json(name = "new_download_count") val newDownloadCount: Int?,
    @Json(name = "hearing_impaired") val hearingImpaired: Boolean?,
    @Json(name = "hd") val hd: Boolean?,
    @Json(name = "fps") val fps: Double?,
    @Json(name = "votes") val votes: Int?,
    @Json(name = "ratings") val ratings: Double?,
    @Json(name = "from_trusted") val fromTrusted: Boolean?,
    @Json(name = "foreign_parts_only") val foreignPartsOnly: Boolean?,
    @Json(name = "upload_date") val uploadDate: String?,
    @Json(name = "ai_translated") val aiTranslated: Boolean?,
    @Json(name = "machine_translated") val machineTranslated: Boolean?,
    @Json(name = "release") val release: String?,
    @Json(name = "comments") val comments: String?,
    @Json(name = "uploader") val uploader: UploaderDto?,
    @Json(name = "feature_details") val featureDetails: FeatureDetailsDto?,
    @Json(name = "files") val files: List<SubtitleFileDto>?
)

@JsonClass(generateAdapter = true)
data class UploaderDto(
    @Json(name = "uploader_id") val uploaderId: Int?,
    @Json(name = "name") val name: String?,
    @Json(name = "rank") val rank: String?
)

@JsonClass(generateAdapter = true)
data class FeatureDetailsDto(
    @Json(name = "feature_id") val featureId: Int?,
    @Json(name = "feature_type") val featureType: String?,
    @Json(name = "year") val year: Int?,
    @Json(name = "title") val title: String?,
    @Json(name = "movie_name") val movieName: String?,
    @Json(name = "imdb_id") val imdbId: Int?,
    @Json(name = "tmdb_id") val tmdbId: Int?,
    @Json(name = "season_number") val seasonNumber: Int?,
    @Json(name = "episode_number") val episodeNumber: Int?,
    @Json(name = "parent_title") val parentTitle: String?
)

@JsonClass(generateAdapter = true)
data class SubtitleFileDto(
    @Json(name = "file_id") val fileId: Int,
    @Json(name = "cd_number") val cdNumber: Int?,
    @Json(name = "file_name") val fileName: String?
)

@JsonClass(generateAdapter = true)
data class DownloadRequest(
    @Json(name = "file_id") val fileId: Int,
    @Json(name = "sub_format") val subFormat: String? = null,
    @Json(name = "in_fps") val inFps: Double? = null,
    @Json(name = "out_fps") val outFps: Double? = null
)

@JsonClass(generateAdapter = true)
data class DownloadResponse(
    @Json(name = "link") val link: String,
    @Json(name = "file_name") val fileName: String,
    @Json(name = "requests") val requests: Int?,
    @Json(name = "remaining") val remaining: Int?,
    @Json(name = "message") val message: String?,
    @Json(name = "reset_time") val resetTime: String?,
    @Json(name = "reset_time_utc") val resetTimeUtc: String?
)

@JsonClass(generateAdapter = true)
data class LoginRequest(
    @Json(name = "username") val username: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class LoginResponse(
    @Json(name = "user") val user: UserDto?,
    @Json(name = "token") val token: String?,
    @Json(name = "status") val status: Int?
)

@JsonClass(generateAdapter = true)
data class UserDto(
    @Json(name = "allowed_downloads") val allowedDownloads: Int?,
    @Json(name = "level") val level: String?,
    @Json(name = "user_id") val userId: Int?,
    @Json(name = "ext_installed") val extInstalled: Boolean?,
    @Json(name = "vip") val vip: Boolean?
)

@JsonClass(generateAdapter = true)
data class FeatureSearchResponse(
    @Json(name = "total_count") val totalCount: Int?,
    @Json(name = "data") val data: List<FeatureDto>
)

@JsonClass(generateAdapter = true)
data class FeatureDto(
    @Json(name = "id") val id: String,
    @Json(name = "type") val type: String,
    @Json(name = "attributes") val attributes: FeatureAttributesDto
)

@JsonClass(generateAdapter = true)
data class FeatureAttributesDto(
    @Json(name = "title") val title: String?,
    @Json(name = "original_title") val originalTitle: String?,
    @Json(name = "year") val year: Int?,
    @Json(name = "imdb_id") val imdbId: Int?,
    @Json(name = "tmdb_id") val tmdbId: Int?,
    @Json(name = "feature_type") val featureType: String?,
    @Json(name = "img_url") val imgUrl: String?,
    @Json(name = "seasons_count") val seasonsCount: Int?,
    @Json(name = "episodes_count") val episodesCount: Int?
)
