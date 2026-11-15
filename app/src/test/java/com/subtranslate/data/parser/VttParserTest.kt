package com.subtranslate.data.parser

import org.junit.Assert.*
import org.junit.Test

class VttParserTest {
    private val parser = VttParser()

    @Test
    fun minuteOnlyTimestampsAndCueSettingsRoundTrip() {
        val source = "WEBVTT\n\nintro\n00:01.250 --> 00:04.500 align:start position:10%\nHello"
        val file = parser.parse(source)
        assertEquals(1250L, file.entries.single().startTime)
        assertEquals(4500L, file.entries.single().endTime)
        assertEquals(source, parser.write(file))
    }

    @Test
    fun stylesRegionsAndNotesSurviveTranslation() {
        val source = "WEBVTT\n\nSTYLE\n::cue { color: lime; }\n\nREGION\nid:main\n\nNOTE Intro\nPreserve this\n\n00:00:01.000 --> 00:00:02.000\nHello"
        val file = parser.parse(source)
        val output = parser.write(file.copy(entries = file.entries.map { it.copy(rawText = "שלום") }))
        assertTrue(output.contains("STYLE\n::cue { color: lime; }"))
        assertTrue(output.contains("REGION\nid:main"))
        assertTrue(output.contains("NOTE Intro\nPreserve this"))
        assertTrue(output.endsWith("שלום"))
    }
}
