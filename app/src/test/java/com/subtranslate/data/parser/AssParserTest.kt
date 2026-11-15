package com.subtranslate.data.parser

import org.junit.Assert.*
import org.junit.Test

class AssParserTest {
    private val parser = AssParser()

    @Test
    fun translationPreservesStyleActorMarginsLayerAndEffect() {
        val source = """
            [Script Info]
            Title: Sample
            [V4+ Styles]
            Format: Name, Fontname, Fontsize
            Style: Sign,Arial,28
            [Events]
            Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
            Dialogue: 2,0:00:01.20,0:00:04.50,Sign,Alex,0010,0020,0030,fade,{\i1}Hello, world{\i0}
        """.trimIndent()
        val file = parser.parse(source)
        assertEquals(1200L, file.entries.single().startTime)
        val translated = file.copy(entries = file.entries.map { it.copy(rawText = "{\\i1}שלום, עולם{\\i0}") })
        val output = parser.write(translated)
        assertTrue(output.contains("Style: Sign,Arial,28"))
        assertTrue(output.contains("Dialogue: 2,0:00:01.20,0:00:04.50,Sign,Alex,0010,0020,0030,fade,{\\i1}שלום, עולם{\\i0}"))
    }

    @Test
    fun lowercaseFieldNamesAndSsaMarkedFieldArePreserved() {
        val source = "[Events]\nformat: Marked, start, end, style, name, marginl, marginr, marginv, effect, text\ndialogue: Marked=0,0:00:01.00,0:00:02.00,Default,,0,0,0,,Hello"
        val file = parser.parse(source)
        assertEquals("Hello", file.entries.single().text)
        assertTrue(parser.write(file).contains("Marked=0,0:00:01.00,0:00:02.00,Default,,0,0,0,,Hello"))
    }

    @Test
    fun missingFormatUsesStandardAssFields() {
        val file = parser.parse("[Events]\nDialogue: 0,0:00:01.00,0:00:02.00,Default,,0,0,0,,Hello")
        assertEquals("Hello", file.entries.single().text)
    }
}
