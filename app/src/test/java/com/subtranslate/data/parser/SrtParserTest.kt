package com.subtranslate.data.parser

import org.junit.Assert.*
import org.junit.Test

class SrtParserTest {

    private val parser = SrtParser()

    // ── parse ─────────────────────────────────────────────────────────────────

    @Test
    fun `parse basic SRT with two entries`() {
        val srt = """
            1
            00:00:01,000 --> 00:00:03,500
            Hello world

            2
            00:00:04,000 --> 00:00:06,000
            How are you?
        """.trimIndent()

        val file = parser.parse(srt)
        assertEquals(2, file.entries.size)

        val e1 = file.entries[0]
        assertEquals(1, e1.index)
        assertEquals(1000L, e1.startTime)
        assertEquals(3500L, e1.endTime)
        assertEquals("00:00:01,000", e1.startTimeRaw)
        assertEquals("00:00:03,500", e1.endTimeRaw)
        assertEquals("Hello world", e1.text)

        val e2 = file.entries[1]
        assertEquals(2, e2.index)
        assertEquals("How are you?", e2.text)
    }

    @Test
    fun `parse strips HTML formatting tags from text`() {
        val srt = """
            1
            00:00:01,000 --> 00:00:03,000
            <i>Italic line</i>
            <b>Bold line</b>
        """.trimIndent()

        val file = parser.parse(srt)
        val e = file.entries[0]
        assertEquals("Italic line\nBold line", e.text)
        assertEquals("<i>Italic line</i>\n<b>Bold line</b>", e.rawText)
    }

    @Test
    fun `parse keeps rawText unchanged for round-trip`() {
        val srt = """
            1
            00:00:01,000 --> 00:00:03,000
            <i>Keep me</i>
        """.trimIndent()

        val file = parser.parse(srt)
        assertEquals("<i>Keep me</i>", file.entries[0].rawText)
    }

    @Test
    fun `parse handles Windows CRLF line endings`() {
        val srt = "1\r\n00:00:01,000 --> 00:00:02,000\r\nHello\r\n\r\n2\r\n00:00:03,000 --> 00:00:04,000\r\nWorld"
        val file = parser.parse(srt)
        assertEquals(2, file.entries.size)
        assertEquals("Hello", file.entries[0].text)
    }

    @Test
    fun `parse handles BOM at start of file`() {
        val srt = "\uFEFF1\n00:00:01,000 --> 00:00:02,000\nHi"
        val file = parser.parse(srt)
        assertEquals(1, file.entries.size)
        assertEquals("Hi", file.entries[0].text)
    }

    @Test
    fun `parse multi-line subtitle entry`() {
        val srt = """
            1
            00:00:01,000 --> 00:00:03,000
            Line one
            Line two
            Line three
        """.trimIndent()

        val file = parser.parse(srt)
        assertEquals("Line one\nLine two\nLine three", file.entries[0].text)
    }

    @Test
    fun `parse timing with period separator (VLC variant)`() {
        val srt = """
            1
            00:00:01.000 --> 00:00:03.500
            Hello
        """.trimIndent()

        val file = parser.parse(srt)
        assertEquals(1000L, file.entries[0].startTime)
        assertEquals(3500L, file.entries[0].endTime)
    }

    @Test
    fun `parse empty content returns empty entry list`() {
        val file = parser.parse("")
        assertTrue(file.entries.isEmpty())
    }

    @Test
    fun `parse skips malformed blocks without timing`() {
        val srt = """
            This is not a subtitle block

            1
            00:00:01,000 --> 00:00:02,000
            Valid entry
        """.trimIndent()

        val file = parser.parse(srt)
        assertEquals(1, file.entries.size)
        assertEquals("Valid entry", file.entries[0].text)
    }

    // ── write ─────────────────────────────────────────────────────────────────

    @Test
    fun `write produces valid SRT output`() {
        val srt = """
            1
            00:00:01,000 --> 00:00:03,500
            Hello world

            2
            00:00:04,000 --> 00:00:06,000
            How are you?
        """.trimIndent()

        val file = parser.parse(srt)
        val output = parser.write(file)

        assertTrue(output.contains("00:00:01,000 --> 00:00:03,500"))
        assertTrue(output.contains("Hello world"))
        assertTrue(output.contains("00:00:04,000 --> 00:00:06,000"))
        assertTrue(output.contains("How are you?"))
    }

    @Test
    fun `write preserves original timing strings exactly`() {
        val srt = """
            1
            00:02:16,612 --> 00:02:19,376
            Some dialogue
        """.trimIndent()

        val file = parser.parse(srt)
        val output = parser.write(file)
        assertTrue("Timing must be preserved verbatim", output.contains("00:02:16,612 --> 00:02:19,376"))
    }

    // ── round-trip ────────────────────────────────────────────────────────────

    @Test
    fun `parse-write round-trip preserves timing and text`() {
        val original = """
            1
            00:00:01,000 --> 00:00:03,500
            <i>First line</i>

            2
            00:00:04,000 --> 00:00:06,000
            Second line
        """.trimIndent()

        val file   = parser.parse(original)
        val output = parser.write(file)
        val reparsed = parser.parse(output)

        assertEquals(file.entries.size, reparsed.entries.size)
        file.entries.zip(reparsed.entries).forEach { (a, b) ->
            assertEquals("startTime mismatch", a.startTime, b.startTime)
            assertEquals("endTime mismatch",   a.endTime,   b.endTime)
            assertEquals("rawText mismatch",   a.rawText,   b.rawText)
        }
    }

    @Test
    fun `timing is calculated correctly for hours`() {
        val srt = """
            1
            01:30:00,000 --> 01:30:05,500
            End credits
        """.trimIndent()

        val file = parser.parse(srt)
        val e = file.entries[0]
        assertEquals(1 * 3600_000L + 30 * 60_000L, e.startTime)
        assertEquals(1 * 3600_000L + 30 * 60_000L + 5_500L, e.endTime)
    }
}
