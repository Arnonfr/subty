package com.subtranslate.data.local.mapper

import com.subtranslate.data.local.entity.SubtitleHistoryEntity
import com.subtranslate.domain.model.HistoryItem

fun SubtitleHistoryEntity.toDomain() = HistoryItem(
    id = id,
    movieTitle = movieTitle,
    originalLanguage = originalLanguage,
    translatedLanguage = translatedLanguage,
    format = format,
    openSubtitlesFileId = openSubtitlesFileId,
    originalFilePath = originalFilePath,
    translatedFilePath = translatedFilePath,
    downloadedAt = downloadedAt,
    translatedAt = translatedAt
)

fun HistoryItem.toEntity() = SubtitleHistoryEntity(
    id = id,
    movieTitle = movieTitle,
    originalLanguage = originalLanguage,
    translatedLanguage = translatedLanguage,
    format = format,
    openSubtitlesFileId = openSubtitlesFileId,
    originalFilePath = originalFilePath,
    translatedFilePath = translatedFilePath,
    downloadedAt = downloadedAt,
    translatedAt = translatedAt
)
