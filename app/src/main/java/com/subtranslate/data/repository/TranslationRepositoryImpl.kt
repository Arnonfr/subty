package com.subtranslate.data.repository

import com.subtranslate.data.remote.translation.ClaudeTranslationService
import com.subtranslate.domain.model.SubtitleFile
import com.subtranslate.domain.model.TranslationProgress
import com.subtranslate.domain.model.TranslationStatus
import com.subtranslate.domain.repository.TranslationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslationRepositoryImpl @Inject constructor(
    private val claudeService: ClaudeTranslationService
) : TranslationRepository {

    // Holds the most recently completed translated file so the ViewModel can read it
    var lastTranslatedFile: SubtitleFile? = null

    override fun translate(
        subtitleFile: SubtitleFile,
        sourceLang: String,
        targetLang: String,
        modelId: String
    ): Flow<TranslationProgress> = flow {
        emit(TranslationProgress(
            totalEntries = subtitleFile.entries.size,
            status = TranslationStatus.TRANSLATING
        ))

        try {
            val translatedEntries = claudeService.translateEntries(
                entries = subtitleFile.entries,
                sourceLang = sourceLang,
                targetLang = targetLang,
                title = subtitleFile.title,
                modelId = modelId
            ) { translated, total, batch, totalBatches ->
                // This is called from IO dispatcher; flow builder handles thread-safety
                kotlinx.coroutines.runBlocking {
                    emit(TranslationProgress(
                        totalEntries = total,
                        translatedEntries = translated,
                        currentBatch = batch,
                        totalBatches = totalBatches,
                        status = TranslationStatus.TRANSLATING
                    ))
                }
            }

            lastTranslatedFile = subtitleFile.copy(entries = translatedEntries)

            emit(TranslationProgress(
                totalEntries = subtitleFile.entries.size,
                translatedEntries = subtitleFile.entries.size,
                currentBatch = 0,
                totalBatches = 0,
                status = TranslationStatus.COMPLETE
            ))
        } catch (e: Exception) {
            emit(TranslationProgress(
                totalEntries = subtitleFile.entries.size,
                status = TranslationStatus.ERROR,
                errorMessage = e.message
            ))
        }
    }
}
