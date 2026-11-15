package com.subtranslate.data.repository

import com.subtranslate.data.analytics.TranslationEventLogger
import com.subtranslate.data.remote.translation.TranslationGateway
import com.subtranslate.data.remote.translation.TranslationModelCatalog
import com.subtranslate.domain.model.SubtitleEntry
import com.subtranslate.domain.model.SubtitleFile
import com.subtranslate.domain.model.SubtitleFormat
import com.subtranslate.domain.model.TranslationStatus
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TranslationRepositoryImplTest {
    @Test
    fun translateAlwaysUsesOfflineEngineAndDoesNotChargeCredits() = runTest {
        val file = SubtitleFile(SubtitleFormat.SRT, entries = listOf(entry("Hello")), title = "sample")
        var engine: String? = null
        val repository = TranslationRepositoryImpl(
            translationGateway = object : TranslationGateway {
                override suspend fun translateEntries(entries: List<SubtitleEntry>, sourceLang: String, targetLang: String, modelId: String, title: String?, onProgress: (Int, Int) -> Unit): List<SubtitleEntry> {
                    engine = modelId
                    onProgress(entries.size, entries.size)
                    return entries.map { it.copy(text = "שלום", rawText = "שלום") }
                }
            },
            eventLogger = NoOpEventLogger,
        )
        val progress = repository.translate(file, "en", "he", "a-previous-cloud-choice").toList()
        assertEquals(TranslationModelCatalog.MLKIT_ON_DEVICE, engine)
        assertEquals(TranslationStatus.COMPLETE, progress.last().status)
        assertEquals("שלום", repository.lastTranslatedFile?.entries?.single()?.text)
    }

    @Test
    fun gatewayFailureIsShownToTheUser() = runTest {
        val repository = TranslationRepositoryImpl(
            translationGateway = object : TranslationGateway {
                override suspend fun translateEntries(entries: List<SubtitleEntry>, sourceLang: String, targetLang: String, modelId: String, title: String?, onProgress: (Int, Int) -> Unit): List<SubtitleEntry> = error("model download failed")
            },
            eventLogger = NoOpEventLogger,
        )
        val progress = repository.translate(SubtitleFile(SubtitleFormat.SRT, entries = listOf(entry("Hello")), title = "sample"), "en", "he", TranslationModelCatalog.MLKIT_ON_DEVICE).toList()
        assertEquals(TranslationStatus.ERROR, progress.last().status)
        assertTrue(progress.last().errorMessage!!.contains("model download failed"))
    }

    private fun entry(text: String) = SubtitleEntry(1, 0, 1_000, "00:00:00,000", "00:00:01,000", text, text)

    @Test
    fun cancellingTranslationDoesNotBecomeAnErrorResult() = runTest {
        val repository = TranslationRepositoryImpl(
            translationGateway = object : TranslationGateway {
                override suspend fun translateEntries(entries: List<SubtitleEntry>, sourceLang: String, targetLang: String, modelId: String, title: String?, onProgress: (Int, Int) -> Unit): List<SubtitleEntry> {
                    throw CancellationException("user cancelled")
                }
            },
            eventLogger = NoOpEventLogger,
        )
        val progress = mutableListOf<com.subtranslate.domain.model.TranslationProgress>()
        var cancelled = false
        try {
            repository.translate(SubtitleFile(SubtitleFormat.SRT, entries = listOf(entry("Hello")), title = "sample"), "en", "he", TranslationModelCatalog.MLKIT_ON_DEVICE).toList(progress)
        } catch (_: CancellationException) {
            cancelled = true
        }
        assertTrue(cancelled)
        assertTrue(progress.none { it.status == TranslationStatus.ERROR })
    }
}

private object NoOpEventLogger : TranslationEventLogger {
    override fun logTranslationComplete(engine: String, chars: Int, entries: Int, sourceLang: String, targetLang: String, stars: Int) = Unit
}
