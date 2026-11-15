package com.subtranslate.data.remote.translation

import com.subtranslate.data.local.datastore.SettingsDataStore
import com.subtranslate.domain.model.SubtitleEntry
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenRouterTranslationService @Inject constructor(
    private val settings: SettingsDataStore,
) : TranslationGateway {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    override suspend fun translateEntries(
        entries: List<SubtitleEntry>,
        sourceLang: String,
        targetLang: String,
        modelId: String,
        title: String?,
        onProgress: (translated: Int, total: Int) -> Unit,
    ): List<SubtitleEntry> = withContext(Dispatchers.IO) {
        if (entries.isEmpty()) return@withContext emptyList()

        val apiKey = settings.effectiveOpenRouterApiKey
            ?: throw IllegalStateException("OpenRouter API key not configured.")

        val batches = entries.chunked(BATCH_SIZE)
        val systemPrompt = TranslationPromptBuilder.buildSystemPrompt(sourceLang, targetLang, title)
        val completed = java.util.concurrent.atomic.AtomicInteger(0)
        val batchDispatcher = Dispatchers.IO.limitedParallelism(MAX_PARALLEL_BATCHES)

        val translatedBatches = coroutineScope {
            batches.map { batch ->
                async(batchDispatcher) {
                    val userMessage = TranslationPromptBuilder.buildUserMessage(emptyList(), batch)
                    val response = translateBatch(
                        apiKey = apiKey,
                        modelId = modelId,
                        systemPrompt = systemPrompt,
                        userMessage = userMessage,
                    )

                    val parsed = TranslationPromptBuilder.parseResponse(response, batch.map { it.index })
                    val translatedBatch = batch.map { entry ->
                        val translatedText = parsed[entry.index]?.trim().orEmpty().ifBlank { entry.text }
                        val translatedRawText = TranslationTextFormatter.translatedRawText(
                            originalRaw = entry.rawText,
                            translated = translatedText,
                            targetLang = targetLang,
                        )
                        entry.copy(
                            text = translatedRawText,
                            rawText = translatedRawText,
                        )
                    }

                    onProgress(completed.addAndGet(batch.size), entries.size)
                    translatedBatch
                }
            }.map { it.await() }
        }

        translatedBatches.flatten()
    }

    private fun translateBatch(
        apiKey: String,
        modelId: String,
        systemPrompt: String,
        userMessage: String,
    ): String {
        val body = JSONObject().apply {
            put("model", modelId)
            put(
                "messages",
                org.json.JSONArray()
                    .put(
                        JSONObject().apply {
                            put("role", "system")
                            put("content", systemPrompt)
                        },
                    )
                    .put(
                        JSONObject().apply {
                            put("role", "user")
                            put("content", userMessage)
                        },
                    ),
            )
            put("temperature", 0.1)
            put("max_tokens", 12000)
        }

        val request = Request.Builder()
            .url("https://openrouter.ai/api/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .header("HTTP-Referer", "https://subty.app")
            .header("X-Title", "Subty")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException(
                    "OpenRouter API error ${response.code}: ${response.body?.string()?.take(300)}",
                )
            }

            val json = JSONObject(response.body!!.string())
            return json
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        }
    }

    companion object {
        const val BATCH_SIZE = 36
        private const val MAX_PARALLEL_BATCHES = 2
    }
}
