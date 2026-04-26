package com.subtranslate.data.remote.translation

import com.subtranslate.BuildConfig
import com.subtranslate.domain.model.SubtitleEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Collections
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import com.subtranslate.data.local.datastore.SettingsDataStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiTranslationService @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) {
    companion object {
        private const val BATCH_SIZE = 40        // smaller batches preserve cue mapping better
        private const val CONTEXT_OVERLAP = 3
        private const val MAX_RETRIES = 3
        private const val MAX_CONCURRENT = 2
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
    }

    // Plain OkHttpClient — no OpenSubtitles interceptors
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun translateEntries(
        entries: List<SubtitleEntry>,
        sourceLang: String,
        targetLang: String,
        title: String?,
        modelId: String = "gemini-3.1-flash-lite-preview",
        onProgress: (translated: Int, total: Int, batch: Int, totalBatches: Int) -> Unit
    ): List<SubtitleEntry> = withContext(Dispatchers.IO) {

        val apiKey = settingsDataStore.geminiApiKey?.ifBlank { null } ?: BuildConfig.GEMINI_API_KEY
        val systemPrompt = TranslationPromptBuilder.buildSystemPrompt(sourceLang, targetLang, title)
        val batches = createBatches(entries, BATCH_SIZE, CONTEXT_OVERLAP)
        val translationMap: MutableMap<Int, String> = Collections.synchronizedMap(mutableMapOf())
        val completedCount = AtomicInteger(0)

        coroutineScope {
            val semaphore = Semaphore(MAX_CONCURRENT)
            batches.mapIndexed { batchIdx, batch ->
                async {
                    semaphore.withPermit {
                        val (contextEntries, translateEntries) = batch
                        val expectedIndices = translateEntries.map { it.index }
                        var attempt = 0
                        var success = false
                        while (attempt < MAX_RETRIES && !success) {
                            try {
                                val userMessage = TranslationPromptBuilder.buildUserMessage(contextEntries, translateEntries)
                                val responseText = callGemini(apiKey, modelId, systemPrompt, userMessage)
                                val parsed = TranslationPromptBuilder.parseResponse(responseText, expectedIndices)
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
                        if (!success) {
                            throw IllegalStateException("Translation batch ${batchIdx + 1} returned too few mapped subtitle lines")
                        }
                        val done = completedCount.incrementAndGet()
                        onProgress(translationMap.size, entries.size, done, batches.size)
                    }
                }
            }.awaitAll()
        }

        entries.map { entry ->
            val translated = translationMap[entry.index]
            if (translated != null) {
                entry.copy(
                    text = translated,
                    rawText = reinsertOverrideTags(entry.rawText, translated)
                )
            } else entry
        }
    }

    private fun callGemini(apiKey: String, modelId: String, systemPrompt: String, userMessage: String): String {
        val body = JSONObject().apply {
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
            })
            put("contents", JSONArray().put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().put("text", userMessage)))
            }))
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.1)
                put("maxOutputTokens", 8192)
                // Disable thinking mode on 2.5-flash — not needed for translation,
                // significantly faster and cheaper without it.
                put("thinkingConfig", JSONObject().apply {
                    put("thinkingBudget", 0)
                })
            })
        }

        val request = Request.Builder()
            .url("$BASE_URL/$modelId:generateContent?key=$apiKey")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Gemini API error ${response.code}: ${response.body?.string()?.take(300)}")
            }
            val json = JSONObject(response.body!!.string())
            return json
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
        }
    }

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

    private fun createBatches(entries: List<SubtitleEntry>, batchSize: Int, overlap: Int): List<Batch> {
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
