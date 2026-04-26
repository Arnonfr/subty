package com.subtranslate.data.remote.translation

import com.subtranslate.domain.model.SubtitleEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Free non-AI translation via MyMemory API (mymemory.translated.net).
 * No API key required. Limit: ~1000 words/day anonymous, 500 chars/request.
 * Uses concurrent requests with limited parallelism for speed, and retries
 * failed entries with exponential backoff.
 */
@Singleton
class MyMemoryTranslationService @Inject constructor() {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val MAX_CHARS = 450 // stay safely under the 500-char API limit
    private val MAX_RETRIES = 3
    private val CONCURRENCY = 5 // MyMemory tolerates modest parallelism

    suspend fun translateEntries(
        entries: List<SubtitleEntry>,
        sourceLang: String,
        targetLang: String,
        onProgress: (translated: Int, total: Int) -> Unit
    ): List<SubtitleEntry> = withContext(Dispatchers.IO) {
        val langPair = "$sourceLang|$targetLang"
        val semaphore = Semaphore(CONCURRENCY)
        val progressCounter = java.util.concurrent.atomic.AtomicInteger(0)

        val deferreds = entries.map { entry ->
            async {
                semaphore.withPermit {
                    val translatedText = translateEntryWithRetry(entry, langPair)
                    val current = progressCounter.incrementAndGet()
                    onProgress(current, entries.size)
                    entry.index to translatedText
                }
            }
        }

        val results = deferreds.awaitAll().toMap()

        entries.map { entry ->
            val t = results[entry.index] ?: entry.text
            val fixed = fixRtlPunctuation(t, targetLang)
            entry.copy(
                text = fixed,
                rawText = reinsertOverrideTags(entry.rawText, fixed)
            )
        }
    }

    private suspend fun translateEntryWithRetry(
        entry: SubtitleEntry,
        langPair: String
    ): String {
        val sourceText = entry.text.trim()
        val chunks = sourceText.chunked(MAX_CHARS)
        val results = mutableListOf<String>()

        for (chunk in chunks) {
            var translated: String? = null
            for (attempt in 1..MAX_RETRIES) {
                try {
                    translated = callMyMemory(chunk, langPair)
                        ?.trim()
                        ?.ifBlank { null }
                    if (translated != null) break
                } catch (e: Exception) {
                    if (attempt < MAX_RETRIES) {
                        delay(500L * attempt)
                    }
                }
            }
            results.add(translated ?: chunk)
        }
        return results.joinToString(" ")
    }

    private fun callMyMemory(text: String, langPair: String): String {
        val encoded = URLEncoder.encode(text, "UTF-8")
        val url = "https://api.mymemory.translated.net/get?q=$encoded&langpair=$langPair"
        val request = Request.Builder().url(url).get().build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("MyMemory error ${response.code}")
            }
            val json = JSONObject(response.body!!.string())
            val status = json.optInt("responseStatus", 0)
            if (status != 200) {
                throw Exception("MyMemory status $status: ${json.optString("responseDetails")}")
            }
            return json.getJSONObject("responseData").getString("translatedText")
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
