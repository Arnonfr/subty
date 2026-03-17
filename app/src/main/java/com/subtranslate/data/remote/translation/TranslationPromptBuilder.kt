package com.subtranslate.data.remote.translation

import com.subtranslate.domain.model.SubtitleEntry

object TranslationPromptBuilder {

    fun buildSystemPrompt(sourceLang: String, targetLang: String, title: String?): String {
        val titleContext = if (!title.isNullOrBlank()) {
            "\nContext: This subtitle is from \"$title\"."
        } else ""
        return """
You are a professional subtitle translator.$titleContext
Translate from $sourceLang to $targetLang.

Rules:
1. Translate as natural spoken dialogue, not literal word-for-word.
2. Maintain character voice consistency across lines.
3. Keep translations concise — subtitles must be read quickly.
4. Preserve proper nouns, character names, and technical terms.
5. Output EXACTLY the same number of numbered lines as the input.
6. Do NOT merge, split, or reorder lines. One input line = one output line.
7. If a line contains only sound effects like [music], [laughs], or "...", keep it as-is.
8. Do not add any explanation, commentary, or extra text — only the numbered translated lines.
        """.trimIndent()
    }

    /**
     * Builds the user message for a batch.
     * @param contextEntries entries provided as context only (their translations will be discarded)
     * @param translateEntries entries that must be translated
     */
    fun buildUserMessage(
        contextEntries: List<SubtitleEntry>,
        translateEntries: List<SubtitleEntry>
    ): String {
        val sb = StringBuilder()

        if (contextEntries.isNotEmpty()) {
            sb.appendLine("=== CONTEXT (already translated, for reference only) ===")
            contextEntries.forEach { entry ->
                sb.appendLine("${entry.index}: ${entry.text}")
            }
            sb.appendLine()
            sb.appendLine("=== TRANSLATE THESE LINES ===")
        }

        translateEntries.forEach { entry ->
            sb.appendLine("${entry.index}: ${entry.text}")
        }

        return sb.toString().trimEnd()
    }

    /**
     * Parses Claude's response into a map of entry index -> translated text.
     * Expected format: "42: translated text here"
     */
    fun parseResponse(
        response: String,
        expectedIndices: List<Int>
    ): Map<Int, String> {
        val result = mutableMapOf<Int, String>()
        val lineRegex = Regex("""^(\d+):\s*(.*)$""")

        for (line in response.lines()) {
            val match = lineRegex.find(line.trim()) ?: continue
            val index = match.groupValues[1].toIntOrNull() ?: continue
            val text = match.groupValues[2]
            if (index in expectedIndices) {
                result[index] = text
            }
        }

        return result
    }
}
