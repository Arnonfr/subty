package com.subtranslate.domain.model

data class SubtitleEntry(
    val index: Int,
    val startTime: Long,        // milliseconds
    val endTime: Long,          // milliseconds
    val startTimeRaw: String,   // original string, preserved for sync
    val endTimeRaw: String,     // original string, preserved for sync
    val text: String,           // plain text for display/translation
    val rawText: String         // original with formatting tags (ASS overrides, HTML)
)
