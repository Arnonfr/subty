package com.subtranslate.data.parser

import com.subtranslate.domain.model.SubtitleEntry
import com.subtranslate.domain.model.SubtitleFile
import com.subtranslate.domain.model.SubtitleFormat

class SrtParser : SubtitleParser {

    override fun supportedFormat() = SubtitleFormat.SRT

    // Matches: 00:02:16,612 --> 00:02:19,376
    private val TIMING_REGEX = Regex(
        """(\d{1,2}):(\d{2}):(\d{2})[,.](\d{3})\s*-->\s*(\d{1,2}):(\d{2}):(\d{2})[,.](\d{3})"""
    )

    override fun parse(content: String): SubtitleFile {
        val normalised = content
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .trimStart('\uFEFF') // strip BOM

        val blocks = normalised.split(Regex("\n{2,}")).map { it.trim() }.filter { it.isNotEmpty() }

        val entries = mutableListOf<SubtitleEntry>()

        for (block in blocks) {
            val lines = block.lines()
            if (lines.size < 2) continue

            // Find timing line (may be line 0 or line 1)
            val timingLineIndex = lines.indexOfFirst { TIMING_REGEX.containsMatchIn(it) }
            if (timingLineIndex < 0) continue

            val timingLine = lines[timingLineIndex]
            val match = TIMING_REGEX.find(timingLine) ?: continue

            val (h1, m1, s1, ms1, h2, m2, s2, ms2) = match.destructured
            val startMs = toMs(h1.toInt(), m1.toInt(), s1.toInt(), ms1.toInt())
            val endMs = toMs(h2.toInt(), m2.toInt(), s2.toInt(), ms2.toInt())

            val startRaw = match.value.substringBefore("-->").trim()
            val endRaw = match.value.substringAfter("-->").trim()

            val index = lines.take(timingLineIndex).lastOrNull()?.trim()?.toIntOrNull()
                ?: (entries.size + 1)

            val textLines = lines.drop(timingLineIndex + 1)
            val rawText = textLines.joinToString("\n")
            val plainText = rawText.replace(Regex("<[^>]+>"), "") // strip HTML tags

            entries.add(
                SubtitleEntry(
                    index = index,
                    startTime = startMs,
                    endTime = endMs,
                    startTimeRaw = startRaw,
                    endTimeRaw = endRaw,
                    text = plainText.trim(),
                    rawText = rawText.trim()
                )
            )
        }

        return SubtitleFile(format = SubtitleFormat.SRT, entries = entries)
    }

    override fun write(subtitleFile: SubtitleFile): String {
        val sb = StringBuilder()
        subtitleFile.entries.forEachIndexed { i, entry ->
            sb.append(entry.index)
            sb.append("\n")
            sb.append("${entry.startTimeRaw} --> ${entry.endTimeRaw}")
            sb.append("\n")
            sb.append(entry.rawText)
            if (i < subtitleFile.entries.size - 1) sb.append("\n\n")
        }
        return sb.toString()
    }

    private fun toMs(h: Int, m: Int, s: Int, ms: Int): Long =
        (h * 3600L + m * 60L + s) * 1000L + ms
}
