package com.subtranslate.domain.usecase

import org.junit.Assert.assertTrue
import org.junit.Test

class SubtitleSaveDestinationTest {
    @Test
    fun `default save path targets movies subty folder`() {
        assertTrue(defaultSubtitleRelativePath().endsWith("Subty/"))
        assertTrue(defaultSubtitleFolderLabel().endsWith("Subty"))
    }
}
