package com.subtranslate.domain.model

data class SubtitleSearchResult(
    val fileId: Int,
    val fileName: String,
    val language: String,
    val languageCode: String,
    val downloadCount: Int,
    val rating: Double?,
    val format: String,
    val uploadedAt: String?,
    val movieTitle: String?,
    val movieYear: Int?,
    val imdbId: String?,
    val isHearingImpaired: Boolean,
    val isTrusted: Boolean,
    val uploaderName: String?,
    val posterUrl: String? = null   // injected from TMDB via SearchSession
)
