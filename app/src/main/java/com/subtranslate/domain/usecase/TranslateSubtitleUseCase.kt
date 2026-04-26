package com.subtranslate.domain.usecase

import com.subtranslate.domain.model.SubtitleFile
import com.subtranslate.domain.model.TranslationProgress
import com.subtranslate.domain.repository.TranslationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TranslateSubtitleUseCase @Inject constructor(
    private val repository: TranslationRepository
) {
    operator fun invoke(
        subtitleFile: SubtitleFile,
        sourceLang: String,
        targetLang: String,
        modelId: String = "mymemory"
    ): Flow<TranslationProgress> = repository.translate(subtitleFile, sourceLang, targetLang, modelId)
}
