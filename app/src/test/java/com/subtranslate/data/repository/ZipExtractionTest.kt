package com.subtranslate.data.repository

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZipExtractionTest {

    private fun makeZip(vararg entries: Pair<String, String>): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            for ((name, content) in entries) {
                zos.putNextEntry(ZipEntry(name))
                zos.write(content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
        }
        return baos.toByteArray()
    }

    private fun extract(zip: ByteArray) = SubtitleRepositoryImpl.extractSubtitleFromZip(zip)

    @Test
    fun `extracts srt file from zip`() {
        val content = "1\n00:00:01,000 --> 00:00:02,000\nHello"
        val result = extract(makeZip("subtitle.srt" to content))
        assertNotNull(result)
        assertEquals("subtitle.srt", result!!.first)
        assertEquals(content, result.second)
    }

    @Test
    fun `extracts vtt file from zip`() {
        val result = extract(makeZip("sub.vtt" to "WEBVTT\n\n00:00:01.000 --> 00:00:02.000\nHello"))
        assertNotNull(result)
        assertEquals("sub.vtt", result!!.first)
    }

    @Test
    fun `extracts ass file from zip`() {
        val result = extract(makeZip("movie.ass" to "[Script Info]"))
        assertNotNull(result)
        assertEquals("movie.ass", result!!.first)
    }

    @Test
    fun `skips non-subtitle files and finds srt`() {
        val zip = makeZip(
            "readme.txt"   to "ignore me",
            "subtitle.srt" to "1\n00:00:01,000 --> 00:00:02,000\nHello",
        )
        val result = extract(zip)
        assertNotNull(result)
        assertEquals("subtitle.srt", result!!.first)
    }

    @Test
    fun `returns null when zip has no subtitle file`() {
        assertNull(extract(makeZip("readme.txt" to "no subtitle here")))
    }

    @Test
    fun `extension matching is case-insensitive`() {
        assertNotNull(extract(makeZip("MOVIE.SRT" to "content")))
        assertNotNull(extract(makeZip("movie.VTT" to "content")))
        assertNotNull(extract(makeZip("movie.ASS" to "content")))
    }

    @Test
    fun `handles zip entry with nested folder path`() {
        val zip = makeZip("folder/sub/movie.en.srt" to "content")
        val result = extract(zip)
        assertNotNull(result)
        assertEquals("folder/sub/movie.en.srt", result!!.first)
    }

    @Test
    fun `preserves UTF-8 content including Hebrew and special chars`() {
        val content = "1\n00:00:01,000 --> 00:00:02,000\nשלום עולם — Hello"
        val result = extract(makeZip("he.srt" to content))
        assertNotNull(result)
        assertEquals(content, result!!.second)
    }

    @Test
    fun `ignores directory entries`() {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            // directory entry named like a subtitle file — should be skipped
            val dir = ZipEntry("subtitles.srt/")
            zos.putNextEntry(dir)
            zos.closeEntry()
            // actual file
            zos.putNextEntry(ZipEntry("real.srt"))
            zos.write("content".toByteArray())
            zos.closeEntry()
        }
        val result = extract(baos.toByteArray())
        assertNotNull(result)
        assertEquals("real.srt", result!!.first)
    }
}
