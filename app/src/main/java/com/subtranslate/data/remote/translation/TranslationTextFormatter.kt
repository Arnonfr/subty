package com.subtranslate.data.remote.translation

internal object TranslationTextFormatter {
    private val leadingOverrideTagsRegex = Regex("""^(?:\{[^}]*\})+""")

    fun translatedRawText(
        originalRaw: String,
        translated: String,
        targetLang: String,
    ): String {
        return reinsertLeadingOverrideTags(
            originalRaw = originalRaw,
            translated = fixRtlPunctuation(translated, targetLang),
        )
    }

    fun reinsertLeadingOverrideTags(originalRaw: String, translated: String): String {
        val leadingTags = leadingOverrideTagsRegex.find(originalRaw)?.value.orEmpty()
        if (leadingTags.isBlank() || translated.startsWith(leadingTags)) return translated
        return "$leadingTags$translated"
    }
}
