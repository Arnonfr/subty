package com.subtranslate.data.remote.translation

import com.subtranslate.domain.model.SubtitleEntry

interface TranslationGateway {
    suspend fun translateEntries(
        entries: List<SubtitleEntry>,
        sourceLang: String,
        targetLang: String,
        modelId: String,
        title: String? = null,
        onProgress: (translated: Int, total: Int) -> Unit,
    ): List<SubtitleEntry>
}
