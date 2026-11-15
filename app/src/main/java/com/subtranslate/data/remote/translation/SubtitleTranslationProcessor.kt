package com.subtranslate.data.remote.translation

import com.subtranslate.domain.model.SubtitleEntry
import kotlin.math.abs

/**
 * Translates subtitle cues one sentence at a time while never changing cue ids, timing,
 * order, or empty cues.
 *
 * - Dialogue cues (`- Hi.\n- Hello.`) are translated one speaker line at a time, so the
 *   engine cannot fold two speakers into one line.
 * - Wrapped lines of a single speaker are joined before translation and rebalanced into
 *   the original number of lines afterwards.
 * - A sentence that runs across up to [MAX_GROUP_CUES] untagged cues is translated once and
 *   split back over those cues in proportion to their source length.
 * - Right-to-left targets get RLM marks so leading dashes and trailing punctuation render on
 *   the correct side, even in players that lay subtitles out left-to-right.
 */
internal object SubtitleTranslationProcessor {
    // Android ICU rejects escaped braces used by the JVM form; literal braces in
    // character classes are portable across both engines.
    private val tag = Regex("<[^>]+>|[{][^}]*[}]")
    private val leadingTags = Regex("^(?:<[^>]+>|[{][^}]*[}])*")
    private val placeholder = Regex("""zz\s*subtag\s*\d+\s*zz""", RegexOption.IGNORE_CASE)
    private val lineBreak = Regex("""\\[Nn]|\n""")
    private val speakerDash = Regex("""^[-‐–—]\s*""")
    private val letter = Regex("""\p{L}""")
    private val rtlLetter = Regex("[\u0590-\u05FF\u0600-\u06FF\u0750-\u077F\uFB1D-\uFDFF\uFE70-\uFEFF]")
    private val whitespace = Regex("""\s+""")

    private const val RLM = "\u200F"
    private const val ELLIPSIS = "..."
    private const val MAX_GROUP_CUES = 3
    private const val MAX_GROUP_GAP_MS = 1_500L
    private const val MAX_GROUP_CHARS = 220
    private const val MIN_GROUP_WORDS = 3
    // A trailing dash marks an interrupted line, which the next speaker usually cuts off.
    private const val SENTENCE_END = ".!?♪:-‐–—"
    private const val CLOSING_MARKS = "\"'”’)]"
    private const val PREFERRED_BREAK = ",;:.!?"
    private const val TRAILING_NEUTRAL = ".,:;!?…\"'”’)]-"
    private val RTL_LANGUAGES = setOf("he", "iw", "ar", "fa", "ur", "yi")

    // On-device Hebrew models sometimes open a sentence with "Untitled:" ("Damn it!" -> "ללא שם:").
    private val hebrewUntitled = Regex("""(^|[.!?…]\s+)ללא (?:שם|כותרת)\s*:\s*""")
    private val engineArtifacts = mapOf("he" to hebrewUntitled, "iw" to hebrewUntitled)
    private val artifactSources = Regex("""untitled|no name|nameless|no title""", RegexOption.IGNORE_CASE)

    suspend fun translate(
        entries: List<SubtitleEntry>,
        targetLang: String,
        translateText: suspend (String) -> String,
        onProgress: (Int, Int) -> Unit,
    ): List<SubtitleEntry> {
        val language = targetLang.trim().lowercase().substringBefore('-').substringBefore('_')
        val rtl = language in RTL_LANGUAGES
        val artifact = engineArtifacts[language]
        val engine: suspend (String) -> String = { source ->
            val translated = translateText(source)
            if (artifact == null || artifactSources.containsMatchIn(source)) translated
            else artifact.replace(translated, "$1").trim()
        }
        val cues = entries.map(::Cue)
        val result = entries.toMutableList()
        var cursor = 0
        while (cursor < cues.size) {
            val size = groupSizeAt(cues, cursor)
            val group = cues.subList(cursor, cursor + size)
            val grouped = if (size > 1) translateGroup(group, engine) else null
            group.forEachIndexed { offset, cue ->
                val raw = grouped?.get(offset) ?: translateCue(cue, engine)
                result[cursor + offset] = if (raw == null) cue.entry else cue.entry.withRaw(raw, rtl)
            }
            cursor += size
            onProgress(cursor, entries.size)
        }
        return result
    }

    private class Cue(val entry: SubtitleEntry) {
        val raw = entry.rawText.ifBlank { entry.text }
        val separator = lineBreak.find(raw)?.value ?: "\n"
        val lines = raw.split(lineBreak).map { it.trim() }.filter { it.isNotEmpty() }
        val plain = tag.replace(lines.joinToString(" "), "").trim()
        val translatable = entry.text.isNotBlank() && letter.containsMatchIn(plain)
        val isDialogue = lines.size > 1 &&
            (speakerDash.containsMatchIn(lines.first()) || lines.drop(1).any { speakerDash.containsMatchIn(it) })
        // Very short fragments ("...Switch...") are usually name lists or interjections, which
        // the engine reorders when joined, so splitting the result back by length misplaces them.
        val canGroup = translatable && !isDialogue && !tag.containsMatchIn(raw) &&
            !speakerDash.containsMatchIn(plain) && plain.split(whitespace).size >= MIN_GROUP_WORDS
    }

    private fun SubtitleEntry.withRaw(raw: String, rtl: Boolean): SubtitleEntry {
        val marked = if (rtl) raw.split(lineBreak).let { parts ->
            val separators = lineBreak.findAll(raw).map { it.value }.toList()
            parts.mapIndexed { i, line -> markRtl(line) + separators.getOrElse(i) { "" } }.joinToString("")
        } else raw
        return copy(text = tag.replace(marked, "").replace(lineBreak, "\n").trim(), rawText = marked)
    }

    // ── Cross-cue sentences ──────────────────────────────────────────────────

    private fun groupSizeAt(cues: List<Cue>, start: Int): Int {
        var end = start
        var chars = cues[start].plain.length
        while (end + 1 < cues.size && end - start + 1 < MAX_GROUP_CUES) {
            val current = cues[end]
            val next = cues[end + 1]
            if (!current.canGroup || !next.canGroup) break
            if (!continuesInto(current.plain, next.plain)) break
            if (next.entry.startTime - current.entry.endTime > MAX_GROUP_GAP_MS) break
            if (chars + next.plain.length > MAX_GROUP_CHARS) break
            chars += next.plain.length
            end += 1
        }
        return end - start + 1
    }

    private fun continuesInto(current: String, next: String): Boolean {
        val ending = current.trimEnd().trimEnd { it in CLOSING_MARKS }
        if (endsWithEllipsis(ending)) return startsWithEllipsis(next)
        return ending.isNotEmpty() && ending.last() !in SENTENCE_END
    }

    private suspend fun translateGroup(group: List<Cue>, translateText: suspend (String) -> String): List<String>? {
        val ellipsisAfter = group.zipWithNext { a, b -> endsWithEllipsis(a.plain) && startsWithEllipsis(b.plain) }
        val parts = group.mapIndexed { i, cue ->
            var text = cue.plain
            if (i > 0 && ellipsisAfter[i - 1]) text = stripLeadingEllipsis(text)
            if (i < ellipsisAfter.size && ellipsisAfter[i]) text = stripTrailingEllipsis(text)
            text
        }
        val translated = normalize(translateText(parts.joinToString(" ")))
        val pieces = distribute(translated, parts.map { it.length.coerceAtLeast(1) }) ?: return null
        return pieces.mapIndexed { i, piece ->
            var text = piece
            if (i < ellipsisAfter.size && ellipsisAfter[i]) text = text.trimEnd { it in ".,;:" } + ELLIPSIS
            if (i > 0 && ellipsisAfter[i - 1]) text = ELLIPSIS + text
            rebalance(text, group[i].lines.size).joinToString(group[i].separator)
        }
    }

    // ── Single cue ───────────────────────────────────────────────────────────

    private suspend fun translateCue(cue: Cue, translateText: suspend (String) -> String): String? {
        if (!cue.translatable) return null
        if (cue.isDialogue) {
            val speakers = mutableListOf<String>()
            for (line in cue.lines) speakers += translateUnit(line, 1, translateText)
            return speakers.joinToString(cue.separator)
        }
        return translateUnit(cue.lines.joinToString(" "), cue.lines.size, translateText)
            .replace("\n", cue.separator)
    }

    /** Translates one speaker's text, keeping its dash and inline tags, laid out over [lineCount] lines. */
    private suspend fun translateUnit(text: String, lineCount: Int, translateText: suspend (String) -> String): String {
        val tags = leadingTags.find(text)?.value.orEmpty()
        val afterTags = text.substring(tags.length)
        val dash = speakerDash.find(afterTags)?.value.orEmpty()
        val body = tags + afterTags.substring(dash.length)
        if (!letter.containsMatchIn(tag.replace(body, ""))) return text
        val protected = protect(body)
        val translated = normalize(translateText(protected.text)).ifBlank { protected.text }
        val laidOut = dash + rebalance(translated, lineCount).joinToString("\n")
        return restore(laidOut, protected.tokens)
    }

    // ── Layout ───────────────────────────────────────────────────────────────

    private fun rebalance(text: String, lineCount: Int): List<String> =
        distribute(text, List(lineCount.coerceAtLeast(1)) { 1 }) ?: listOf(text.trim())

    /**
     * Splits [text] at word boundaries into one piece per weight, sized in proportion to the
     * weights and nudged towards punctuation. Returns null when there are fewer words than pieces.
     */
    internal fun distribute(text: String, weights: List<Int>): List<String>? {
        val words = text.split(whitespace).filter { it.isNotEmpty() }
        if (weights.isEmpty() || words.size < weights.size) return null
        if (weights.size == 1) return listOf(words.joinToString(" "))
        val prefix = IntArray(words.size + 1)
        words.forEachIndexed { i, word -> prefix[i + 1] = prefix[i] + placeholder.replace(word, "").length + 1 }
        val total = prefix[words.size].toDouble()
        val totalWeight = weights.sum().coerceAtLeast(1)
        val punctuationPull = total / (weights.size * 4.0)
        val pieces = mutableListOf<String>()
        var start = 0
        var weightSoFar = 0
        for (i in 0 until weights.lastIndex) {
            weightSoFar += weights[i]
            val target = total * weightSoFar / totalWeight
            val lastCut = words.size - (weights.lastIndex - i)
            val cut = (start + 1..lastCut).minBy { k ->
                val distance = abs(prefix[k] - target)
                val lastChar = placeholder.replace(words[k - 1], "").lastOrNull()
                if (lastChar != null && lastChar in PREFERRED_BREAK) distance - punctuationPull else distance
            }
            pieces += words.subList(start, cut).joinToString(" ")
            start = cut
        }
        pieces += words.subList(start, words.size).joinToString(" ")
        return pieces
    }

    /** Prefixes RLM (and suffixes it after trailing punctuation) on lines containing RTL script. */
    internal fun markRtl(line: String): String {
        if (!rtlLetter.containsMatchIn(line)) return line
        val tags = leadingTags.find(line)?.value.orEmpty()
        var body = line.substring(tags.length)
        if (!body.startsWith(RLM)) body = RLM + body
        val lastVisible = tag.replace(body, "").trimEnd().lastOrNull()
        if (lastVisible != null && lastVisible in TRAILING_NEUTRAL && !body.endsWith(RLM)) body += RLM
        return tags + body
    }

    // ── Text helpers ─────────────────────────────────────────────────────────

    private fun endsWithEllipsis(text: String) = text.trimEnd().let { it.endsWith(ELLIPSIS) || it.endsWith("…") }
    private fun startsWithEllipsis(text: String) = text.trimStart().let { it.startsWith(ELLIPSIS) || it.startsWith("…") }
    private fun stripLeadingEllipsis(text: String) = text.trimStart().trimStart('.', '…').trimStart()
    private fun stripTrailingEllipsis(text: String) = text.trimEnd().trimEnd('.', '…').trimEnd()

    /** Collapses engine whitespace (we lay lines out ourselves) and repairs mangled tag markers. */
    private fun normalize(text: String): String = placeholder
        .replace(text) { it.value.replace(whitespace, "").uppercase() }
        .replace(whitespace, " ")
        .trim()

    private data class Protected(val text: String, val tokens: List<Pair<String, String>>)
    private fun protect(text: String): Protected {
        var number = 0
        val tokens = mutableListOf<Pair<String, String>>()
        val protected = tag.replace(text) { match ->
            val placeholder = "ZZSUBTAG${number++}ZZ"
            tokens += placeholder to match.value
            placeholder
        }
        return Protected(protected, tokens)
    }
    private fun restore(text: String, tokens: List<Pair<String, String>>): String {
        // ML Kit commonly normalizes our all-caps marker to `Zzsubtag0zz`.
        // Restore case-insensitively, then remove any altered marker rather
        // than exposing internal implementation text in the subtitle. The lambda form keeps
        // ASS backslashes (`{\i1}`) literal instead of reading them as replacement escapes.
        return tokens.fold(text) { value, (key, token) ->
            value.replace(Regex(Regex.escape(key), RegexOption.IGNORE_CASE)) { token }
        }.replace(placeholder, "")
    }
}
