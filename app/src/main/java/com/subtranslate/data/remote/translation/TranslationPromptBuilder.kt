package com.subtranslate.data.remote.translation

import com.subtranslate.domain.model.SubtitleEntry

private val RTL_LANGUAGES = setOf("he", "ar", "fa", "ur", "yi", "iw")

/** Fix Unicode BiDi: trailing punctuation in RTL text visually drifts to the wrong side.
 *  Appending RLM (‏) after each line's terminal punctuation anchors it correctly. */
fun fixRtlPunctuation(text: String, targetLang: String): String {
    if (targetLang.lowercase() !in RTL_LANGUAGES) return text
    val rlm = "‏"
    // Add RLM after punctuation that ends a line (period, comma, ?, !, :, ;)
    return text.lines().joinToString("\n") { line ->
        val trimmed = line.trimEnd()
        if (trimmed.isNotEmpty() && trimmed.last() in ".,:;!?") "$trimmed$rlm"
        else line
    }
}

object TranslationPromptBuilder {

    private const val NEWLINE_PLACEHOLDER = "{LF}"

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
8. Multi-line subtitle cues use {LF} as a line separator — preserve {LF} markers in output.
9. Do not add any explanation, commentary, or extra text — only the numbered translated lines.
        """.trimIndent()
    }

    /**
     * Builds the user message for a batch.
     * Multi-line subtitle text is encoded with {LF} so each entry stays on one line,
     * keeping the numbered format unambiguous for the model.
     */
    fun buildUserMessage(
        contextEntries: List<SubtitleEntry>,
        translateEntries: List<SubtitleEntry>
    ): String {
        val sb = StringBuilder()

        if (contextEntries.isNotEmpty()) {
            sb.appendLine("=== CONTEXT (already translated, for reference only) ===")
            contextEntries.forEach { entry ->
                sb.appendLine("${entry.index}: ${entry.text.replace("\n", NEWLINE_PLACEHOLDER)}")
            }
            sb.appendLine()
            sb.appendLine("=== TRANSLATE THESE LINES ===")
        }

        translateEntries.forEach { entry ->
            sb.appendLine("${entry.index}: ${entry.text.replace("\n", NEWLINE_PLACEHOLDER)}")
        }

        return sb.toString().trimEnd()
    }

    /**
     * Parses the model response into a map of entry index -> translated text.
     * Expected format: "42: translated text here"
     * {LF} placeholders are restored to actual newlines.
     */
    fun parseResponse(
        response: String,
        expectedIndices: List<Int>
    ): Map<Int, String> {
        val result = mutableMapOf<Int, String>()
        val lineRegex = Regex("""^(\d+):\s*(.*)$""")

        // Strip markdown code fences the model may wrap output in
        val cleaned = response
            .replace(Regex("^```[a-zA-Z]*\\s*", RegexOption.MULTILINE), "")
            .replace(Regex("^```\\s*$", RegexOption.MULTILINE), "")

        for (line in cleaned.lines()) {
            val match = lineRegex.find(line.trim()) ?: continue
            val index = match.groupValues[1].toIntOrNull() ?: continue
            val text = match.groupValues[2].replace(NEWLINE_PLACEHOLDER, "\n")
            if (index in expectedIndices) {
                result[index] = text
            }
        }

        return result
    }
}
