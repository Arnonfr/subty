package com.subtranslate.data.analytics

interface TranslationEventLogger {
    fun logTranslationComplete(
        engine: String,
        chars: Int,
        entries: Int,
        sourceLang: String,
        targetLang: String,
        stars: Int,
    )
}
