package com.subtranslate.data.repository

import com.subtranslate.data.analytics.TranslationEventLogger
import com.subtranslate.data.remote.translation.TranslationGateway
import com.subtranslate.data.remote.translation.TranslationModelCatalog
import com.subtranslate.domain.model.SubtitleFile
import com.subtranslate.domain.model.TranslationProgress
import com.subtranslate.domain.model.TranslationStatus
import com.subtranslate.domain.repository.TranslationRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.CancellationException

@Singleton
class TranslationRepositoryImpl @Inject constructor(
    private val translationGateway: TranslationGateway,
    private val eventLogger: TranslationEventLogger,
) : TranslationRepository {

    var lastTranslatedFile: SubtitleFile? = null
        private set

    override fun translate(
        subtitleFile: SubtitleFile,
        sourceLang: String,
        targetLang: String,
        modelId: String,
    ): Flow<TranslationProgress> = channelFlow {
        send(
            TranslationProgress(
                totalEntries = subtitleFile.entries.size,
                currentBatch = 1,
                totalBatches = 1,
                status = TranslationStatus.TRANSLATING,
                warningMessage = "Preparing offline translation. Language models download once and are reused.",
            ),
        )
        try {
            val translatedEntries = translationGateway.translateEntries(
                entries = subtitleFile.entries,
                sourceLang = sourceLang,
                targetLang = targetLang,
                modelId = TranslationModelCatalog.MLKIT_ON_DEVICE,
                title = subtitleFile.title,
                onProgress = { translated, total ->
                    trySend(
                        TranslationProgress(
                            totalEntries = total,
                            translatedEntries = translated,
                            currentBatch = 1,
                            totalBatches = 1,
                            status = TranslationStatus.TRANSLATING,
                        ),
                    )
                },
            )
            lastTranslatedFile = subtitleFile.copy(entries = translatedEntries)
            eventLogger.logTranslationComplete(
                engine = TranslationModelCatalog.MLKIT_ON_DEVICE,
                chars = translatedEntries.sumOf { it.text.length },
                entries = subtitleFile.entries.size,
                sourceLang = sourceLang,
                targetLang = targetLang,
                stars = 0,
            )
            send(
                TranslationProgress(
                    totalEntries = subtitleFile.entries.size,
                    translatedEntries = translatedEntries.size,
                    currentBatch = 1,
                    totalBatches = 1,
                    status = TranslationStatus.COMPLETE,
                ),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            send(
                TranslationProgress(
                    totalEntries = subtitleFile.entries.size,
                    status = TranslationStatus.ERROR,
                    errorMessage = e.message ?: "Offline translation failed",
                ),
            )
        }
    }
}
