package com.subtranslate.data.remote.translation

import com.subtranslate.domain.model.SubtitleEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

/**
 * Microsoft Azure Translator API service.
 * Free tier (F0): 2 million characters/month.
 * Standard tier (S1): $10 per million characters.
 * Supports batching up to 100 texts per request.
 */
@Singleton
class MicrosoftTranslationService @Inject constructor() {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val BATCH_SIZE = 100
    private val MAX_RETRIES = 3
    private val ENDPOINT = "https://api.cognitive.microsofttranslator.com/translate"

    suspend fun translateEntries(
        entries: List<SubtitleEntry>,
        sourceLang: String,
        targetLang: String,
        apiKey: String,
        region: String,
        onProgress: (translated: Int, total: Int) -> Unit
    ): List<SubtitleEntry> = withContext(Dispatchers.IO) {
        val resultMap = mutableMapOf<Int, String>()
        val batches = entries.chunked(BATCH_SIZE)
        var translatedCount = 0

        for (batch in batches) {
            val texts = batch.map { it.text.trim() }
            val translations = translateBatchWithRetry(
                texts = texts,
                sourceLang = sourceLang,
                targetLang = targetLang,
                apiKey = apiKey,
                region = region
            )

            batch.forEachIndexed { index, entry ->
                val translatedText = translations.getOrNull(index)?.trim() ?: entry.text
                val fixed = fixRtlPunctuation(translatedText, targetLang)
                resultMap[entry.index] = fixed
            }

            translatedCount += batch.size
            onProgress(translatedCount, entries.size)
        }

        entries.map { entry ->
            val t = resultMap[entry.index] ?: entry.text
            entry.copy(
                text = t,
                rawText = reinsertOverrideTags(entry.rawText, t)
            )
        }
    }

    private suspend fun translateBatchWithRetry(
        texts: List<String>,
        sourceLang: String,
        targetLang: String,
        apiKey: String,
        region: String
    ): List<String> {
        var lastException: Exception? = null
        for (attempt in 1..MAX_RETRIES) {
            try {
                return translateBatch(texts, sourceLang, targetLang, apiKey, region)
            } catch (e: Exception) {
                lastException = e
                if (attempt < MAX_RETRIES) {
                    delay(500L * attempt)
                }
            }
        }
        // All retries exhausted — return originals
        return texts
    }

    private fun translateBatch(
        texts: List<String>,
        sourceLang: String,
        targetLang: String,
        apiKey: String,
        region: String
    ): List<String> {
        val jsonBody = JSONArray().apply {
            texts.forEach { put(JSONObject().put("Text", it)) }
        }.toString()

        val url = "$ENDPOINT?api-version=3.0&from=$sourceLang&to=$targetLang"
        val requestBuilder = Request.Builder()
            .url(url)
            .header("Ocp-Apim-Subscription-Key", apiKey)
            .header("Content-Type", "application/json")

        // Region header is required for most keys (except global multi-service)
        if (region.isNotBlank() && region.lowercase() != "global") {
            requestBuilder.header("Ocp-Apim-Subscription-Region", region)
        }

        val request = requestBuilder
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val body = response.body?.string() ?: ""
                throw Exception("Microsoft Translator HTTP ${response.code}: $body")
            }
            val jsonArray = JSONArray(response.body!!.string())
            return (0 until jsonArray.length()).map { i ->
                jsonArray.getJSONObject(i)
                    .getJSONArray("translations")
                    .getJSONObject(0)
                    .getString("text")
            }
        }
    }

    private fun reinsertOverrideTags(originalRaw: String, translated: String): String {
        val overrideRegex = Regex("""\{[^}]*\}""")
        val leadingTags = overrideRegex.findAll(originalRaw)
            .takeWhile { it.range.first < originalRaw.indexOf(it.value) + it.value.length }
            .joinToString("") { it.value }
        return if (leadingTags.isNotEmpty()) "$leadingTags$translated" else translated
    }
}
