package com.subtranslate.data.repository

import com.subtranslate.data.remote.translation.GeminiTranslationService
import com.subtranslate.data.remote.translation.MyMemoryTranslationService
import com.subtranslate.domain.model.SubtitleFile
import com.subtranslate.domain.model.TranslationProgress
import com.subtranslate.domain.model.TranslationStatus
import com.subtranslate.domain.repository.TranslationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslationRepositoryImpl @Inject constructor(
    private val myMemoryService: MyMemoryTranslationService,
    private val geminiService: GeminiTranslationService
) : TranslationRepository {

    var lastTranslatedFile: SubtitleFile? = null

    override fun translate(
        subtitleFile: SubtitleFile,
        sourceLang: String,
        targetLang: String,
        modelId: String
    ): Flow<TranslationProgress> = channelFlow {
        // channelFlow allows send() from any thread/coroutine context,
        // fixing the "Flow invariant is violated" crash caused by runBlocking inside flow{}
        send(TranslationProgress(
            totalEntries = subtitleFile.entries.size,
            status = TranslationStatus.TRANSLATING
        ))

        try {
            val translatedEntries = if (modelId.startsWith("gemini")) {
                // Premium: Gemini (context-aware, batched with overlap)
                geminiService.translateEntries(
                    entries = subtitleFile.entries,
                    sourceLang = sourceLang,
                    targetLang = targetLang,
                    title = subtitleFile.title,
                    modelId = modelId
                ) { translated, total, batch, totalBatches ->
                    trySend(TranslationProgress(
                        totalEntries = total,
                        translatedEntries = translated,
                        currentBatch = batch,
                        totalBatches = totalBatches,
                        status = TranslationStatus.TRANSLATING
                    ))
                }
            } else {
                // Basic: MyMemory (free, no API key, non-AI)
                myMemoryService.translateEntries(
                    entries = subtitleFile.entries,
                    sourceLang = sourceLang,
                    targetLang = targetLang
                ) { translated, total ->
                    trySend(TranslationProgress(
                        totalEntries = total,
                        translatedEntries = translated,
                        currentBatch = 1,
                        totalBatches = 1,
                        status = TranslationStatus.TRANSLATING
                    ))
                }
            }

            lastTranslatedFile = subtitleFile.copy(entries = translatedEntries)

            send(TranslationProgress(
                totalEntries = subtitleFile.entries.size,
                translatedEntries = subtitleFile.entries.size,
                currentBatch = 0,
                totalBatches = 0,
                status = TranslationStatus.COMPLETE
            ))
        } catch (e: Exception) {
            send(TranslationProgress(
                totalEntries = subtitleFile.entries.size,
                status = TranslationStatus.ERROR,
                errorMessage = e.message
            ))
        }
    }
}
