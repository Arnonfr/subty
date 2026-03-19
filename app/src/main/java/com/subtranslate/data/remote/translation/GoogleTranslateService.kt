package com.subtranslate.data.remote.translation

import com.subtranslate.BuildConfig
import com.subtranslate.data.local.datastore.SettingsDataStore
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
class GoogleTranslateService @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Translates a batch of subtitle entries using Google Translate.
     * Sends all text in a single batched request for efficiency.
     */
    suspend fun translateEntries(
        entries: List<SubtitleEntry>,
        sourceLang: String,
        targetLang: String,
        onProgress: (translated: Int, total: Int) -> Unit
    ): List<SubtitleEntry> = withContext(Dispatchers.IO) {

        // Split into chunks of 100 (Google Translate limit per request)
        val chunkSize = 100
        val chunks = entries.chunked(chunkSize)
        val translationMap = mutableMapOf<Int, String>()

        chunks.forEachIndexed { chunkIdx, chunk ->
            val texts = chunk.map { it.text }
            val translated = callGoogleTranslate(texts, sourceLang, targetLang)

            chunk.forEachIndexed { i, entry ->
                translationMap[entry.index] = translated.getOrElse(i) { entry.text }
            }

            onProgress(translationMap.size, entries.size)
        }

        entries.map { entry ->
            val translated = translationMap[entry.index] ?: return@map entry
            entry.copy(
                text = translated,
                rawText = reinsertOverrideTags(entry.rawText, translated)
            )
        }
    }

    private fun callGoogleTranslate(texts: List<String>, source: String, target: String): List<String> {
        val body = JSONObject().apply {
            put("q", JSONArray(texts))
            put("source", source)
            put("target", target)
            put("format", "text")
        }

        val apiKey = settingsDataStore.googleTranslateApiKey?.ifBlank { null }
            ?: BuildConfig.GOOGLE_TRANSLATE_API_KEY
        val request = Request.Builder()
            .url("https://translation.googleapis.com/language/translate/v2?key=$apiKey")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Google Translate error ${response.code}: ${response.body?.string()?.take(300)}")
            }
            val json = JSONObject(response.body!!.string())
            val translations = json
                .getJSONObject("data")
                .getJSONArray("translations")
            return (0 until translations.length()).map { i ->
                translations.getJSONObject(i).getString("translatedText")
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
