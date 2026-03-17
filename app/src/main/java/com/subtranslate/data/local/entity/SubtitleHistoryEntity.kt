package com.subtranslate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subtitle_history")
data class SubtitleHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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
