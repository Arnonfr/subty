package com.subtranslate.presentation.search

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchUiHelpersTest {
    @Test
    fun `season count never falls below one`() {
        assertEquals(1, displaySeasonCount(0))
        assertEquals(5, displaySeasonCount(5))
    }

    @Test
    fun `episode count never falls below one`() {
        assertEquals(1, displayEpisodeCount(0))
        assertEquals(24, displayEpisodeCount(24))
    }
}
