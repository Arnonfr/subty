package com.subtranslate.data.remote.translation

import com.subtranslate.domain.model.SubtitleEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DeepL API translation service.
 * Free tier: 500,000 characters/month via api-free.deepl.com
 * Pro tier: api.deepl.com (requires paid plan)
 * Supports batching — multiple texts in one request for speed.
 */
@Singleton
class DeepLTranslationService @Inject constructor() {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /** Max texts per request to stay within DeepL limits comfortably. */
    private val BATCH_SIZE = 50
    private val MAX_RETRIES = 3

    suspend fun translateEntries(
        entries: List<SubtitleEntry>,
        sourceLang: String,
        targetLang: String,
        apiKey: String,
        onProgress: (translated: Int, total: Int) -> Unit
    ): List<SubtitleEntry> = withContext(Dispatchers.IO) {
        val endpoint = if (apiKey.endsWith(":fx")) {
            "https://api-free.deepl.com/v2/translate"
        } else {
            "https://api.deepl.com/v2/translate"
        }

        val resultMap = mutableMapOf<Int, String>()
        val batches = entries.chunked(BATCH_SIZE)
        var translatedCount = 0

        for (batch in batches) {
            val texts = batch.map { it.text.trim() }
            val translations = translateBatchWithRetry(
                endpoint = endpoint,
                apiKey = apiKey,
                texts = texts,
                sourceLang = sourceLang.uppercase(),
                targetLang = targetLang.uppercase()
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
        endpoint: String,
        apiKey: String,
        texts: List<String>,
        sourceLang: String,
        targetLang: String
    ): List<String> {
        var lastException: Exception? = null
        for (attempt in 1..MAX_RETRIES) {
            try {
                return translateBatch(endpoint, apiKey, texts, sourceLang, targetLang)
            } catch (e: Exception) {
                lastException = e
                if (attempt < MAX_RETRIES) {
                    val delayMs = 500L * attempt
                    kotlinx.coroutines.delay(delayMs)
                }
            }
        }
        // All retries exhausted — return originals
        return texts
    }

    private fun translateBatch(
        endpoint: String,
        apiKey: String,
        texts: List<String>,
        sourceLang: String,
        targetLang: String
    ): List<String> {
        val bodyBuilder = FormBody.Builder()
            .add("target_lang", targetLang)
            .add("source_lang", sourceLang)
        texts.forEach { bodyBuilder.add("text[]", it) }

        val request = Request.Builder()
            .url(endpoint)
            .header("Authorization", "DeepL-Auth-Key $apiKey")
            .post(bodyBuilder.build())
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val body = response.body?.string() ?: ""
                throw Exception("DeepL HTTP ${response.code}: $body")
            }
            val json = JSONObject(response.body!!.string())
            val translations = json.getJSONArray("translations")
            return (0 until translations.length()).map { i ->
                translations.getJSONObject(i).getString("text")
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
