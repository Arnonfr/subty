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
    private val googleFreeEndpoint = "https://translate.googleapis.com/translate_a/single"

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
        try {
            return callGoogleFreeTranslate(text, sourceLang, targetLang)
        } catch (e: Exception) {
            lastError = e
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

    private fun callGoogleFreeTranslate(
        text: String,
        sourceLang: String,
        targetLang: String
    ): String {
        val encoded = java.net.URLEncoder.encode(text, Charsets.UTF_8.name())
        val url = "$googleFreeEndpoint?client=gtx&sl=$sourceLang&tl=$targetLang&dt=t&q=$encoded"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Google Free Translate HTTP ${response.code}")
            }
            val body = response.body?.string().orEmpty()
            val root = org.json.JSONArray(body)
            val sentenceArray = root.optJSONArray(0) ?: return text
            val builder = StringBuilder()
            for (i in 0 until sentenceArray.length()) {
                val item = sentenceArray.optJSONArray(i) ?: continue
                val translated = item.optString(0)
                if (translated.isNotBlank()) {
                    if (builder.isNotEmpty()) builder.append(" ")
                    builder.append(translated)
                }
            }
            return builder.toString().ifBlank { text }
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
