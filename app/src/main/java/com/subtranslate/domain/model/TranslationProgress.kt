package com.subtranslate.domain.model

data class TranslationProgress(
    val totalEntries: Int = 0,
    val translatedEntries: Int = 0,
    val currentBatch: Int = 0,
    val totalBatches: Int = 0,
    val status: TranslationStatus = TranslationStatus.IDLE,
    val errorMessage: String? = null,
    val warningMessage: String? = null,
) {
    val progressFraction: Float
        get() = if (totalEntries == 0) 0f else translatedEntries.toFloat() / totalEntries
}

enum class TranslationStatus {
    IDLE, TRANSLATING, PAUSED, COMPLETE, ERROR
}
