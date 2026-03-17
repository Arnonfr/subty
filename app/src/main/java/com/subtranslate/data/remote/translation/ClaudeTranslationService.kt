package com.subtranslate.data.remote.translation

import com.anthropic.client.AnthropicClient
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.Model
import com.subtranslate.domain.model.SubtitleEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClaudeTranslationService @Inject constructor(
    private val clientProvider: AnthropicClientProvider
) {
    companion object {
        private const val BATCH_SIZE = 18
        private const val CONTEXT_OVERLAP = 3
        private const val MAX_RETRIES = 3
    }

    /**
     * Translates a list of entries, emitting progress via [onProgress].
     * Returns the full list with translated [SubtitleEntry.text] and [SubtitleEntry.rawText].
     */
    suspend fun translateEntries(
        entries: List<SubtitleEntry>,
        sourceLang: String,
        targetLang: String,
        title: String?,
        modelId: String = Model.CLAUDE_SONNET_4_5.toString(),
        onProgress: (translated: Int, total: Int, batch: Int, totalBatches: Int) -> Unit
    ): List<SubtitleEntry> = withContext(Dispatchers.IO) {

        val client = clientProvider.getClient()
        val systemPrompt = TranslationPromptBuilder.buildSystemPrompt(sourceLang, targetLang, title)

        // Create batches with overlap
        val batches = createBatches(entries, BATCH_SIZE, CONTEXT_OVERLAP)
        val translationMap = mutableMapOf<Int, String>()

        batches.forEachIndexed { batchIdx, batch ->
            val (contextEntries, translateEntries) = batch
            val expectedIndices = translateEntries.map { it.index }

            var attempt = 0
            var success = false

            while (attempt < MAX_RETRIES && !success) {
                try {
                    val userMessage = TranslationPromptBuilder.buildUserMessage(
                        contextEntries, translateEntries
                    )

                    val response = client.messages().create(
                        MessageCreateParams.builder()
                            .model(modelId)
                            .maxTokens(4096)
                            .system(systemPrompt)
                            .addUserMessage(userMessage)
                            .build()
                    )

                    val responseText = response.content()
                        .firstOrNull()
                        ?.text()
                        ?.text() ?: ""

                    val parsed = TranslationPromptBuilder.parseResponse(responseText, expectedIndices)

                    // Only accept if we got all expected lines
                    if (parsed.size >= expectedIndices.size * 0.9) {
                        translationMap.putAll(parsed)
                        success = true
                    } else {
                        attempt++
                    }
                } catch (e: Exception) {
                    attempt++
                    if (attempt >= MAX_RETRIES) throw e
                }
            }

            val translatedSoFar = translationMap.size
            onProgress(translatedSoFar, entries.size, batchIdx + 1, batches.size)
        }

        // Reconstruct entries with translated text
        entries.map { entry ->
            val translated = translationMap[entry.index]
            if (translated != null) {
                entry.copy(
                    text = translated,
                    rawText = reinsertOverrideTags(entry.rawText, translated)
                )
            } else {
                entry
            }
        }
    }

    /**
     * If rawText had ASS override tags, put them back around the translated text.
     * Simple strategy: keep leading/trailing tag blocks unchanged.
     */
    private fun reinsertOverrideTags(originalRaw: String, translated: String): String {
        val overrideRegex = Regex("""\{[^}]*\}""")
        val leadingTags = overrideRegex.findAll(originalRaw)
            .takeWhile { it.range.first < originalRaw.indexOf(it.value) + it.value.length }
            .joinToString("") { it.value }

        return if (leadingTags.isNotEmpty()) "$leadingTags$translated" else translated
    }

    private data class Batch(
        val contextEntries: List<SubtitleEntry>,
        val translateEntries: List<SubtitleEntry>
    )

    private fun createBatches(
        entries: List<SubtitleEntry>,
        batchSize: Int,
        overlap: Int
    ): List<Batch> {
        val batches = mutableListOf<Batch>()
        var start = 0

        while (start < entries.size) {
            val end = minOf(start + batchSize, entries.size)
            val translateEntries = entries.subList(start, end)

            val contextStart = maxOf(0, start - overlap)
            val contextEntries = if (start > 0) entries.subList(contextStart, start) else emptyList()

            batches.add(Batch(contextEntries, translateEntries))
            start = end
        }

        return batches
    }
}

interface AnthropicClientProvider {
    fun getClient(): AnthropicClient
}
