package com.subtranslate.data.analytics

import javax.inject.Inject

class FirestoreTranslationEventLogger @Inject constructor(
    private val analyticsTracker: AnalyticsTracker,
) : TranslationEventLogger {
    override fun logTranslationComplete(
        engine: String,
        chars: Int,
        entries: Int,
        sourceLang: String,
        targetLang: String,
        stars: Int,
    ) = analyticsTracker.track(
        "translation_complete",
        mapOf(
            "engine" to engine,
            "chars" to chars.toString(),
            "entries" to entries.toString(),
            "source_lang" to sourceLang,
            "target_lang" to targetLang,
            "stars" to stars.toString(),
        ),
    )
}
