package com.subtranslate.data.remote.translation

import com.subtranslate.data.local.dao.TranslationCacheDao
import com.subtranslate.data.local.datastore.SettingsDataStore
import com.subtranslate.data.local.entity.TranslationCacheEntity
import com.subtranslate.domain.model.SubtitleEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Free/open-source translation via LibreTranslate-compatible endpoints.
 * Prefers a configured self-hosted endpoint and falls back to public instances.
 */
@Singleton
class LibreTranslateService @Inject constructor(
    private val settings: SettingsDataStore,
    private val translationCacheDao: TranslationCacheDao
) : TranslationGateway {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val maxChars = 450
    private val fallbackEndpoints = listOf(
        "https://translate.argosopentech.com",
        "https://libretranslate.de"
    )

    override suspend fun translateEntries(
        entries: List<SubtitleEntry>,
        sourceLang: String,
        targetLang: String,
        modelId: String,
        title: String?,
        onProgress: (translated: Int, total: Int) -> Unit
    ): List<SubtitleEntry> = withContext(Dispatchers.IO) {
        val resultMap = mutableMapOf<Int, String>()
        var done = 0

        entries.forEach { entry ->
            val normalizedText = entry.text.trim()
            val cacheKey = cacheKey(normalizedText, sourceLang, targetLang)
            val cached = translationCacheDao.get(cacheKey)

            val translated = cached ?: translateSingleWithFallback(
                text = entry.text.trim(),
                sourceLang = sourceLang,
                targetLang = targetLang
            ).also { fresh ->
                translationCacheDao.upsert(
                    TranslationCacheEntity(
                        cacheKey = cacheKey,
                        translatedText = fresh,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }
            resultMap[entry.index] = fixRtlPunctuation(translated, targetLang)
            done += 1
            onProgress(done, entries.size)
        }

        entries.map { entry ->
            val t = resultMap[entry.index] ?: entry.text
            val raw = TranslationTextFormatter.translatedRawText(
                originalRaw = entry.rawText,
                translated = t,
                targetLang = targetLang,
            )
            entry.copy(
                text = raw,
                rawText = raw,
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
        val configured = settings.libreTranslateBaseUrl.trim().removeSuffix("/")
        val apiKey = settings.libreTranslateApiKey?.trim()?.ifBlank { null }
        var lastError: Exception? = null
        val endpoints = buildList {
            if (configured.isNotBlank()) add(configured)
            addAll(fallbackEndpoints.filter { it != configured })
        }
        for (endpoint in endpoints) {
            try {
                return callLibreTranslate("$endpoint/translate", text, sourceLang, targetLang, apiKey)
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
        targetLang: String,
        apiKey: String?
    ): String {
        val body = JSONObject().apply {
            put("q", text)
            put("source", sourceLang)
            put("target", targetLang)
            put("format", "text")
            if (!apiKey.isNullOrBlank()) put("api_key", apiKey)
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

    private fun cacheKey(text: String, sourceLang: String, targetLang: String): String {
        val raw = "libretranslate|$sourceLang|$targetLang|$text"
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
