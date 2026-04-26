package com.subtranslate.data.remote.translation

import com.subtranslate.domain.model.SubtitleEntry
import kotlinx.coroutines.Dispatchers
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
 * Each subtitle cue is translated separately. This is slower, but keeps subtitle
 * cue boundaries intact; translation APIs do not reliably preserve delimiters.
 */
@Singleton
class MyMemoryTranslationService @Inject constructor() {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val MAX_CHARS = 450 // stay safely under the 500-char API limit

    suspend fun translateEntries(
        entries: List<SubtitleEntry>,
        sourceLang: String,
        targetLang: String,
        onProgress: (translated: Int, total: Int) -> Unit
    ): List<SubtitleEntry> = withContext(Dispatchers.IO) {
        val langPair = "$sourceLang|$targetLang"
        val resultMap = mutableMapOf<Int, String>()

        var translated = 0
        for (entry in entries) {
            val sourceText = entry.text.trim()
            val chunks = sourceText.chunked(MAX_CHARS)
            val translatedText = chunks.joinToString(" ") { chunk ->
                runCatching { callMyMemory(chunk, langPair) }
                    .getOrNull()
                    ?.trim()
                    ?.ifBlank { null }
                    ?: chunk
            }
            resultMap[entry.index] = translatedText
            translated += 1
            onProgress(translated, entries.size)
        }

        entries.map { entry ->
            val t = resultMap[entry.index] ?: return@map entry
            val fixed = fixRtlPunctuation(t, targetLang)
            entry.copy(text = fixed, rawText = reinsertOverrideTags(entry.rawText, fixed))
        }
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
