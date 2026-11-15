package com.subtranslate.data.remote.translation

import com.subtranslate.domain.model.SubtitleEntry
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslationGatewayRouter @Inject constructor(
    private val mlKitTranslationProvider: MlKitTranslationProvider,
) : TranslationGateway {
    override suspend fun translateEntries(
        entries: List<SubtitleEntry>,
        sourceLang: String,
        targetLang: String,
        modelId: String,
        title: String?,
        onProgress: (translated: Int, total: Int) -> Unit,
    ): List<SubtitleEntry> = mlKitTranslationProvider.translateEntries(
        entries = entries,
        sourceLang = sourceLang,
        targetLang = targetLang,
        onProgress = onProgress,
        onPreparing = {},
    )
}
