package com.subtranslate.data.remote.screwzira

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ScrewziraParserTest {

    @Test
    fun `parse search candidates extracts films from nested payload`() {
        val body = """
            {"d":"{\"Films\":[{\"ID\":\"42\",\"EngName\":\"The Boys\",\"HebName\":\"הבנים\",\"ReleaseDate\":2019,\"IsSeries\":true,\"ImdbID\":\"tt1190634\"},{\"ID\":\"99\",\"EngName\":\"The Boys in the Boat\",\"ReleaseDate\":2023,\"IsSeries\":false}]}"}
        """.trimIndent()

        val candidates = ScrewziraParser.parseSearchCandidates(body)

        assertEquals(2, candidates.size)
        assertEquals("42", candidates.first().id)
        assertEquals("The Boys", candidates.first().englishName)
        assertEquals("tt1190634", candidates.first().imdbId)
    }

    @Test
    fun `pick best candidate prefers exact type-safe match`() {
        val candidates = listOf(
            ScrewziraSearchCandidate("1", "The Boys in the Boat", null, 2023, false, null),
            ScrewziraSearchCandidate("2", "The Boys", null, 2019, true, "tt1190634"),
        )

        val picked = ScrewziraParser.pickBestCandidate(
            query = "The Boys",
            isSeries = true,
            releaseYear = null,
            candidates = candidates,
        )

        assertNotNull(picked)
        assertEquals("2", picked?.id)
    }

    @Test
    fun `parse subtitle entries extracts subtitle ids from html`() {
        val body = """
            <table>
              <tr>
                <td>
                  <div style="float:right">The Boys S01E01 720p<br /></div>
                  <button data-subtitle-id="777"></button>
                </td>
              </tr>
            </table>
        """.trimIndent()

        val entries = ScrewziraParser.parseSubtitleEntries(body)

        assertEquals(1, entries.size)
        assertEquals("777", entries.first().subtitleId)
        assertEquals("The Boys S01E01 720p", entries.first().name)
    }

    @Test
    fun `parse download identifier reads nested response`() {
        val body = """{"d":"{\"DownloadIdentifier\":\"abc123\"}"}"""

        assertEquals("abc123", ScrewziraParser.parseDownloadIdentifier(body))
    }

    @Test
    fun `pick best candidate returns null when no title match`() {
        val picked = ScrewziraParser.pickBestCandidate(
            query = "Severance",
            isSeries = true,
            releaseYear = null,
            candidates = listOf(
                ScrewziraSearchCandidate("1", "The Boys", null, 2019, true, null),
            ),
        )

        assertNull(picked)
    }
}
