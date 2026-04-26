package com.subtranslate.data.repository

import com.subtranslate.data.local.datastore.SettingsDataStore
import com.subtranslate.data.remote.translation.DeepLTranslationService
import com.subtranslate.data.remote.translation.GeminiTranslationService
import com.subtranslate.data.remote.translation.MicrosoftTranslationService
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
    private val geminiService: GeminiTranslationService,
    private val deeplService: DeepLTranslationService,
    private val microsoftService: MicrosoftTranslationService,
    private val settings: SettingsDataStore
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
            val translatedEntries = when {
                modelId.startsWith("gemini") -> {
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
                }
                modelId == "deepl" -> {
                    val apiKey = settings.deeplApiKey
                        ?: throw IllegalStateException("DeepL API key not configured. Add it in Settings.")
                    val batchesTotal = (subtitleFile.entries.size + 49) / 50
                    deeplService.translateEntries(
                        entries = subtitleFile.entries,
                        sourceLang = sourceLang,
                        targetLang = targetLang,
                        apiKey = apiKey
                    ) { translated, total ->
                        trySend(TranslationProgress(
                            totalEntries = total,
                            translatedEntries = translated,
                            currentBatch = (translated + 49) / 50,
                            totalBatches = batchesTotal,
                            status = TranslationStatus.TRANSLATING
                        ))
                    }
                }
                modelId == "microsoft" -> {
                    val apiKey = settings.microsoftApiKey
                        ?: throw IllegalStateException("Microsoft API key not configured. Add it in Settings.")
                    val region = settings.microsoftRegion
                    val batchesTotal = (subtitleFile.entries.size + 99) / 100
                    microsoftService.translateEntries(
                        entries = subtitleFile.entries,
                        sourceLang = sourceLang,
                        targetLang = targetLang,
                        apiKey = apiKey,
                        region = region
                    ) { translated, total ->
                        trySend(TranslationProgress(
                            totalEntries = total,
                            translatedEntries = translated,
                            currentBatch = (translated + 99) / 100,
                            totalBatches = batchesTotal,
                            status = TranslationStatus.TRANSLATING
                        ))
                    }
                }
                else -> {
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
            }

            // Track usage
            val totalChars = subtitleFile.entries.sumOf { it.text.length }
            val engineKey = when {
                modelId.startsWith("gemini") -> "gemini"
                modelId == "deepl" -> "deepl"
                modelId == "microsoft" -> "microsoft"
                else -> "mymemory"
            }
            settings.addCharsUsed(engineKey, totalChars)

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
