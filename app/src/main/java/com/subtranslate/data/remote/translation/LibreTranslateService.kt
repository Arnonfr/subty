package com.subtranslate.data.remote.translation

import com.subtranslate.domain.model.SubtitleEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Free/open-source translation fallback via LibreTranslate-compatible endpoints.
 * Uses a small list of public instances and falls back between them.
 */
@Singleton
class LibreTranslateService @Inject constructor() {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val maxChars = 450
    private val endpoints = listOf(
        "https://translate.argosopentech.com/translate",
        "https://libretranslate.de/translate"
    )

    suspend fun translateEntries(
        entries: List<SubtitleEntry>,
        sourceLang: String,
        targetLang: String,
        onProgress: (translated: Int, total: Int) -> Unit
    ): List<SubtitleEntry> = withContext(Dispatchers.IO) {
        val resultMap = mutableMapOf<Int, String>()
        var done = 0

        entries.forEach { entry ->
            val translated = translateSingleWithFallback(
                text = entry.text.trim(),
                sourceLang = sourceLang,
                targetLang = targetLang
            )
            resultMap[entry.index] = fixRtlPunctuation(translated, targetLang)
            done += 1
            onProgress(done, entries.size)
        }

        entries.map { entry ->
            val t = resultMap[entry.index] ?: entry.text
            entry.copy(
                text = t,
                rawText = reinsertOverrideTags(entry.rawText, t)
            )
        }
    }

    private fun translateSingleWithFallback(
        text: String,
        sourceLang: String,
        targetLang: String
    ): String {
        if (text.isBlank()) return text
        val chunks = text.chunked(maxChars)
        val translatedChunks = chunks.map { chunk ->
            translateChunkWithFallback(chunk, sourceLang, targetLang)
        }
        return translatedChunks.joinToString(" ")
    }

    private fun translateChunkWithFallback(
        text: String,
        sourceLang: String,
        targetLang: String
    ): String {
        var lastError: Exception? = null
        for (endpoint in endpoints) {
            try {
                return callLibreTranslate(endpoint, text, sourceLang, targetLang)
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: Exception("LibreTranslate fallback failed")
    }

    private fun callLibreTranslate(
        endpoint: String,
        text: String,
        sourceLang: String,
        targetLang: String
    ): String {
        val body = JSONObject().apply {
            put("q", text)
            put("source", sourceLang)
            put("target", targetLang)
            put("format", "text")
        }

        val request = Request.Builder()
            .url(endpoint)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("LibreTranslate HTTP ${response.code}")
            }
            val json = JSONObject(response.body?.string().orEmpty())
            return json.optString("translatedText").ifBlank { text }
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
