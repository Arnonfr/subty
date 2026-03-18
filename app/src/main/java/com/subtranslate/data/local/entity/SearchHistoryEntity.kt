package com.subtranslate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val season: Int?,
    val episode: Int?,
    val languages: String?,
    val contentType: String?,   // "movie" | "tv" | null
    val searchedAt: Long = System.currentTimeMillis(),
)
