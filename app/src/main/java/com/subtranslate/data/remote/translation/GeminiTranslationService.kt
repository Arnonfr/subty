package com.subtranslate.data.remote.translation

import com.subtranslate.BuildConfig
import com.subtranslate.domain.model.SubtitleEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiTranslationService @Inject constructor() {
    companion object {
        private const val BATCH_SIZE = 18
        private const val CONTEXT_OVERLAP = 3
        private const val MAX_RETRIES = 3
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
        modelId: String = "gemini-2.5-flash",
        onProgress: (translated: Int, total: Int, batch: Int, totalBatches: Int) -> Unit
    ): List<SubtitleEntry> = withContext(Dispatchers.IO) {

        val apiKey = BuildConfig.GEMINI_API_KEY
        val systemPrompt = TranslationPromptBuilder.buildSystemPrompt(sourceLang, targetLang, title)
        val batches = createBatches(entries, BATCH_SIZE, CONTEXT_OVERLAP)
        val translationMap = mutableMapOf<Int, String>()

        batches.forEachIndexed { batchIdx, batch ->
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

            onProgress(translationMap.size, entries.size, batchIdx + 1, batches.size)
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
                put("temperature", 0.3)
                put("maxOutputTokens", 4096)
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
