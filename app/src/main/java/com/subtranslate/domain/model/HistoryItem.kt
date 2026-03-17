package com.subtranslate.domain.model

data class HistoryItem(
    val id: Long,
    val movieTitle: String,
    val originalLanguage: String,
    val translatedLanguage: String?,
    val format: String,
    val openSubtitlesFileId: Int?,
    val originalFilePath: String?,
    val translatedFilePath: String?,
    val downloadedAt: Long,
    val translatedAt: Long?
)
