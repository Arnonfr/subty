package com.subtranslate.data.parser

import com.subtranslate.domain.model.SubtitleEntry
import com.subtranslate.domain.model.SubtitleFile
import com.subtranslate.domain.model.SubtitleFormat

/**
 * Parses Advanced SubStation Alpha (.ass / .ssa) files.
 * Only the Text field in [Events] Dialogue lines is replaced during translation.
 * All headers, styles, and override tags ({\...}) are preserved verbatim.
 */
class AssParser : SubtitleParser {

    override fun supportedFormat() = SubtitleFormat.ASS

    // Override tags like {\an8}, {\b1}, {\pos(320,240)} etc.
    private val OVERRIDE_TAG_REGEX = Regex("""[{][^}]*[}]""")

    // ASS timestamp: H:MM:SS.cc  (centiseconds, not milliseconds)
    private val TIMESTAMP_REGEX = Regex("""(\d):(\d{2}):(\d{2})\.(\d{2})""")

    override fun parse(content: String): SubtitleFile {
        val normalised = content
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .trimStart('\uFEFF')

        val lines = normalised.lines()

        // Split into sections
        val headerLines = mutableListOf<String>()
        val eventLines = mutableListOf<String>()
        var inEvents = false
        var formatFields: List<String> = emptyList()

        for (line in lines) {
            when {
                line.trim().equals("[Events]", ignoreCase = true) -> {
                    inEvents = true
                    headerLines.add(line)
                }
                inEvents && line.startsWith("Format:", ignoreCase = true) -> {
                    formatFields = line.substringAfter(":").split(",").map { it.trim() }
                    headerLines.add(line)
                }
                inEvents && line.startsWith("Dialogue:", ignoreCase = true) -> {
                    eventLines.add(line)
                }
                else -> {
                    if (!inEvents) headerLines.add(line)
                    else headerLines.add(line)
                }
            }
        }

        val rawHeader = headerLines.joinToString("\n")
        if (formatFields.isEmpty()) formatFields = listOf("Layer", "Start", "End", "Style", "Name", "MarginL", "MarginR", "MarginV", "Effect", "Text")
        val textIndex = formatFields.indexOfFirst { it.equals("Text", true) }
        val startIndex = formatFields.indexOfFirst { it.equals("Start", true) }.takeIf { it >= 0 } ?: 1
        val endIndex = formatFields.indexOfFirst { it.equals("End", true) }.takeIf { it >= 0 } ?: 2
        val metadata = mutableMapOf("formatFields" to formatFields.joinToString(","))

        val entries = mutableListOf<SubtitleEntry>()

        eventLines.forEachIndexed { i, line ->
            val payload = line.substringAfter(":").trimStart()
            val values = payload.split(",", limit = formatFields.size)
            if (values.size < formatFields.size) return@forEachIndexed

            val startRaw = values.getOrNull(startIndex)?.trim() ?: return@forEachIndexed
            val endRaw = values.getOrNull(endIndex)?.trim() ?: return@forEachIndexed
            val rawText = if (textIndex >= 0) values.getOrNull(textIndex)?.trim() ?: "" else ""

            val startMs = assTimestampToMs(startRaw)
            val endMs = assTimestampToMs(endRaw)
            val plainText = rawText.replace(OVERRIDE_TAG_REGEX, "").replace("\\N", "\n").replace("\\n", "\n")
            metadata["assDialogue:${i + 1}"] = payload

            entries.add(
                SubtitleEntry(
                    index = i + 1,
                    startTime = startMs,
                    endTime = endMs,
                    startTimeRaw = startRaw,
                    endTimeRaw = endRaw,
                    text = plainText.trim(),
                    rawText = rawText.trim()
                )
            )
        }

        return SubtitleFile(
            format = SubtitleFormat.ASS,
            entries = entries,
            rawHeader = rawHeader,
            metadata = metadata
        )
    }

    override fun write(subtitleFile: SubtitleFile): String {
        val sb = StringBuilder()
        sb.append(subtitleFile.rawHeader ?: "")
        sb.append("\n")

        val formatFields = subtitleFile.metadata["formatFields"]
            ?.split(",")?.map { it.trim() }
            ?: listOf("Layer", "Start", "End", "Style", "Name", "MarginL", "MarginR", "MarginV", "Effect", "Text")

        subtitleFile.entries.forEach { entry ->
            val fieldCount = formatFields.size
            // Build dialogue values: re-insert rawText into Text field position
            val textIdx = formatFields.indexOfFirst { it.equals("Text", true) }.takeIf { it >= 0 } ?: (fieldCount - 1)
            val startIdx = formatFields.indexOfFirst { it.equals("Start", true) }.takeIf { it >= 0 } ?: 1
            val endIdx = formatFields.indexOfFirst { it.equals("End", true) }.takeIf { it >= 0 } ?: 2

            // Use a template of empty fields and fill known ones
            val original = subtitleFile.metadata["assDialogue:${entry.index}"]?.split(",", limit = fieldCount)
            val fields = Array(fieldCount) { original?.getOrNull(it) ?: "" }
            fields[startIdx] = entry.startTimeRaw
            fields[endIdx] = entry.endTimeRaw
            fields[textIdx] = entry.rawText

            sb.append("Dialogue: ${fields.joinToString(",")}\n")
        }

        return sb.toString().trimEnd()
    }

    private fun assTimestampToMs(ts: String): Long {
        val match = TIMESTAMP_REGEX.find(ts) ?: return 0L
        val (h, m, s, cs) = match.destructured
        return (h.toLong() * 3600 + m.toLong() * 60 + s.toLong()) * 1000 + cs.toLong() * 10
    }
}
