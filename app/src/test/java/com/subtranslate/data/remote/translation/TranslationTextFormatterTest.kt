package com.subtranslate.data.remote.translation

import org.junit.Assert.assertEquals
import org.junit.Test

class TranslationTextFormatterTest {
    @Test
    fun reinsertLeadingOverrideTags_preservesOnlyTagsAtStart() {
        val formatted = TranslationTextFormatter.reinsertLeadingOverrideTags(
            originalRaw = "{\\i1}Hello {\\i0}",
            translated = "Bonjour",
        )

        assertEquals("{\\i1}Bonjour", formatted)
    }

    @Test
    fun reinsertLeadingOverrideTags_doesNotMoveInlineTagsToStart() {
        val formatted = TranslationTextFormatter.reinsertLeadingOverrideTags(
            originalRaw = "Hello {\\i1}world{\\i0}",
            translated = "Bonjour le monde",
        )

        assertEquals("Bonjour le monde", formatted)
    }

    @Test
    fun reinsertLeadingOverrideTags_doesNotDuplicateTagsAlreadyReturnedByModel() {
        val formatted = TranslationTextFormatter.reinsertLeadingOverrideTags(
            originalRaw = "{\\an8}{\\i1}Hello",
            translated = "{\\an8}{\\i1}Bonjour",
        )

        assertEquals("{\\an8}{\\i1}Bonjour", formatted)
    }

    @Test
    fun translatedRawText_addsRtlMarkerAfterTerminalPunctuation() {
        val formatted = TranslationTextFormatter.translatedRawText(
            originalRaw = "{\\an8}Hello",
            translated = "Shalom!",
            targetLang = "he",
        )

        assertEquals("{\\an8}Shalom!\u200F", formatted)
    }
}
