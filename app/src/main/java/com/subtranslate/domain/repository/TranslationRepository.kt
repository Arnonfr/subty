package com.subtranslate.domain.repository

import com.subtranslate.domain.model.SubtitleFile
import com.subtranslate.domain.model.TranslationProgress
import kotlinx.coroutines.flow.Flow

interface TranslationRepository {
    fun translate(
        subtitleFile: SubtitleFile,
        sourceLang: String,
        targetLang: String,
        modelId: String
    ): Flow<TranslationProgress>
}
