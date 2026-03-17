package com.subtranslate.data.parser

import com.subtranslate.domain.model.SubtitleEntry
import com.subtranslate.domain.model.SubtitleFile
import com.subtranslate.domain.model.SubtitleFormat

class VttParser : SubtitleParser {

    override fun supportedFormat() = SubtitleFormat.VTT

    // 00:02:16.612 --> 00:02:19.376 (optional cue settings after)
    private val TIMING_REGEX = Regex(
        """(\d{1,2}):(\d{2}):(\d{2})[.,](\d{3})\s*-->\s*(\d{1,2}):(\d{2}):(\d{2})[.,](\d{3})(.*)"""
    )

    override fun parse(content: String): SubtitleFile {
        val normalised = content
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .trimStart('\uFEFF')

        val lines = normalised.lines()
        val headerEnd = lines.indexOfFirst { it.isBlank() }.takeIf { it >= 0 } ?: 0
        val rawHeader = lines.take(headerEnd + 1).joinToString("\n")

        // Split into cue blocks (separated by blank lines, after header)
        val body = lines.drop(headerEnd + 1).joinToString("\n")
        val blocks = body.split(Regex("\n{2,}")).map { it.trim() }.filter { it.isNotEmpty() }

        val entries = mutableListOf<SubtitleEntry>()
        var index = 1

        for (block in blocks) {
            val blockLines = block.lines()
            // Skip NOTE and STYLE blocks
            if (blockLines.firstOrNull()?.startsWith("NOTE") == true) continue
            if (blockLines.firstOrNull()?.startsWith("STYLE") == true) continue

            val timingLineIndex = blockLines.indexOfFirst { TIMING_REGEX.containsMatchIn(it) }
            if (timingLineIndex < 0) continue

            val timingLine = blockLines[timingLineIndex]
            val match = TIMING_REGEX.find(timingLine) ?: continue

            val (h1, m1, s1, ms1, h2, m2, s2, ms2, cueSettings) = match.destructured
            val startMs = toMs(h1.toInt(), m1.toInt(), s1.toInt(), ms1.toInt())
            val endMs = toMs(h2.toInt(), m2.toInt(), s2.toInt(), ms2.toInt())
            val startRaw = "${h1}:${m1}:${s1}.${ms1}"
            val endRaw = "${h2}:${m2}:${s2}.${ms2}${cueSettings}"

            val textLines = blockLines.drop(timingLineIndex + 1)
            val rawText = textLines.joinToString("\n")
            val plainText = rawText.replace(Regex("<[^>]+>"), "")

            entries.add(
                SubtitleEntry(
                    index = index++,
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
            format = SubtitleFormat.VTT,
            entries = entries,
            rawHeader = rawHeader
        )
    }

    override fun write(subtitleFile: SubtitleFile): String {
        val sb = StringBuilder()
        sb.append(subtitleFile.rawHeader ?: "WEBVTT")
        sb.append("\n\n")
        subtitleFile.entries.forEachIndexed { i, entry ->
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
