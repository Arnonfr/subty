package com.subtranslate.data.remote.translation

import com.subtranslate.domain.model.SubtitleEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.test.runTest

class SubtitleTranslationProcessorTest {
    private val rlm = "‏"

    @Test
    fun `translates a sentence split across cues once and keeps cue boundaries and timestamps`() = runTest {
        val entries = listOf(cue(1, "I don't know why", 0, 1200), cue(2, "you would do that.", 1250, 2600))
        val requests = mutableListOf<String>()
        val translated = SubtitleTranslationProcessor.translate(
            entries, "he", { text -> requests += text; "אני לא יודע למה היית עושה את זה." }, { _, _ -> },
        )
        assertEquals(listOf("I don't know why you would do that."), requests)
        assertEquals(entries.map { it.index }, translated.map { it.index })
        assertEquals(entries.map { it.startTimeRaw to it.endTimeRaw }, translated.map { it.startTimeRaw to it.endTimeRaw })
        assertEquals("אני לא יודע למה", translated[0].text.withoutRlm())
        assertEquals("היית עושה את זה.", translated[1].text.withoutRlm())
    }

    @Test
    fun `does not join cues across a finished sentence or a long pause`() = runTest {
        val entries = listOf(
            cue(1, "Hello.", 0, 1000),
            cue(2, "Wait for me", 1100, 2000),
            cue(3, "outside.", 6000, 7000),
        )
        val requests = mutableListOf<String>()
        SubtitleTranslationProcessor.translate(entries, "fr", { requests += it; it }, { _, _ -> })
        assertEquals(listOf("Hello.", "Wait for me", "outside."), requests)
    }

    @Test
    fun `joins trailing and leading ellipsis continuation and restores the ellipses`() = runTest {
        val entries = listOf(cue(1, "I think I'd better...", 0, 1000), cue(2, "...go home right now.", 1100, 2500))
        val requests = mutableListOf<String>()
        val translated = SubtitleTranslationProcessor.translate(
            entries, "fr", { requests += it; "Je pense que je ferais mieux de rentrer maintenant." }, { _, _ -> },
        )
        assertEquals(listOf("I think I'd better go home right now."), requests)
        assertTrue(translated[0].rawText.endsWith("..."))
        assertTrue(translated[1].rawText.startsWith("..."))
    }

    @Test
    fun `treats a trailing dash as an interruption, not a continuation`() = runTest {
        val entries = listOf(cue(1, "Flat or pumps? No, just -", 0, 1000), cue(2, "What the hell is that?", 1100, 2000))
        val requests = mutableListOf<String>()
        SubtitleTranslationProcessor.translate(entries, "fr", { requests += it; it }, { _, _ -> })
        assertEquals(entries.map { it.text }, requests)
    }

    @Test
    fun `does not merge short fragments such as name lists`() = runTest {
        val entries = listOf(
            cue(1, "This is Apoc...", 0, 1000),
            cue(2, "...Switch...", 1100, 2000),
            cue(3, "...and Cypher.", 2100, 3000),
        )
        val requests = mutableListOf<String>()
        SubtitleTranslationProcessor.translate(entries, "fr", { requests += it; it }, { _, _ -> })
        assertEquals(3, requests.size)
    }

    @Test
    fun `strips the Hebrew Untitled artifact and keeps the source when nothing else is left`() = runTest {
        val entries = listOf(
            cue(1, "Yeah, of course I'm sure.", 0, 1000),
            cue(2, "- Damn it!\n- The trace was completed.", 5000, 6000),
            cue(3, "They cut the hard line.\nIt's a trap! Get out!", 9000, 10000),
        )
        val replies = mapOf(
            "Yeah, of course I'm sure." to "ללא שם: כן, כמובן שאני בטוח.",
            "Damn it!" to "ללא שם:",
            "The trace was completed." to "העקבות הושלמה.",
            "They cut the hard line. It's a trap! Get out!" to "הם חתכו את הקו. ללא שם: זו מלכודת! צא!",
        )
        val translated = SubtitleTranslationProcessor.translate(entries, "he", { replies.getValue(it) }, { _, _ -> })
        assertEquals("כן, כמובן שאני בטוח.", translated[0].text.withoutRlm())
        assertEquals("- Damn it!\n- העקבות הושלמה.", translated[1].text.withoutRlm())
        assertEquals("הם חתכו את הקו. זו מלכודת! צא!", translated[2].text.withoutRlm().replace("\n", " "))
    }

    @Test
    fun `translates each speaker separately and keeps one line per speaker`() = runTest {
        val entries = listOf(cue(1, "- Is everything in place?\n- You don't have to coddle me.", 0, 2000))
        val requests = mutableListOf<String>()
        val dictionary = mapOf(
            "Is everything in place?" to "האם הכל במקום?",
            "You don't have to coddle me." to "אתה לא צריך לפנק אותי.",
        )
        val translated = SubtitleTranslationProcessor.translate(
            entries, "he", { requests += it; dictionary.getValue(it) }, { _, _ -> },
        )
        assertEquals(dictionary.keys.toList(), requests)
        assertEquals("- האם הכל במקום?\n- אתה לא צריך לפנק אותי.", translated.single().rawText.withoutRlm())
    }

    @Test
    fun `joins a wrapped single speaker line and rebalances it over the original line count`() = runTest {
        val entries = listOf(cue(1, "Morpheus believes he is\nthe One.", 0, 2000))
        val requests = mutableListOf<String>()
        val translated = SubtitleTranslationProcessor.translate(
            entries, "he", { requests += it; "מורפיאוס מאמין שהוא האחד." }, { _, _ -> },
        )
        assertEquals(listOf("Morpheus believes he is the One."), requests)
        val lines = translated.single().rawText.withoutRlm().lines()
        assertEquals(2, lines.size)
        assertEquals("מורפיאוס מאמין שהוא האחד.", lines.joinToString(" "))
    }

    @Test
    fun `marks Hebrew lines so dashes and trailing punctuation render on the right side`() = runTest {
        val entries = listOf(cue(1, "- Are you?\n- Yes.", 0, 2000))
        val translated = SubtitleTranslationProcessor.translate(
            entries, "he", { if (it == "Are you?") "האם אתה?" else "כן." }, { _, _ -> },
        )
        assertEquals("$rlm- האם אתה?$rlm\n$rlm- כן.$rlm", translated.single().rawText)
        assertEquals(translated.single().rawText, translated.single().text)
    }

    @Test
    fun `places RLM after leading tags and keeps ASS line breaks`() = runTest {
        val entries = listOf(cue(1, "{\\an8}Where are you going?\\NHome.", 0, 2000))
        val translated = SubtitleTranslationProcessor.translate(
            entries, "he", { "ZZSUBTAG0ZZלאן אתה הולך? הביתה." }, { _, _ -> },
        )
        val raw = translated.single().rawText
        assertTrue("ASS break must survive: $raw", "\\N" in raw)
        assertTrue("Override tag stays first: $raw", raw.startsWith("{\\an8}$rlm"))
        assertTrue("No real newline in an ASS field: $raw", "\n" !in raw)
    }

    @Test
    fun `leaves non-Latin-target text unmarked for left-to-right languages`() = runTest {
        val entries = listOf(cue(1, "Hello.", 0, 1000))
        val translated = SubtitleTranslationProcessor.translate(entries, "fr", { "Bonjour." }, { _, _ -> })
        assertEquals("Bonjour.", translated.single().rawText)
    }

    @Test
    fun `skips cues without words`() = runTest {
        val entries = listOf(cue(1, "♪", 0, 1000), cue(2, "", 1100, 1500))
        var calls = 0
        val translated = SubtitleTranslationProcessor.translate(entries, "he", { calls++; "x" }, { _, _ -> })
        assertEquals(0, calls)
        assertEquals(entries, translated)
    }

    @Test
    fun `does not join separate speakers and restores html formatting`() = runTest {
        val entries = listOf(cue(1, "- <i>Hello</i>", 0, 1000), cue(2, "- goodbye", 1100, 2000))
        val translated = SubtitleTranslationProcessor.translate(entries, "fr", { it.replace("Hello", "Bonjour") }, { _, _ -> })
        assertEquals("- <i>Bonjour</i>", translated.first().rawText)
        assertEquals(2, translated.size)
    }

    @Test
    fun `restores formatting when ML Kit changes placeholder casing or spacing`() = runTest {
        val entries = listOf(cue(1, "<i>Dough</i>", 0, 1000), cue(2, "<b>Bread</b>", 1100, 2000))
        val replies = ArrayDeque(listOf("Zzsubtag0zzבצקzzsubtag1zz", "Zz subtag 0 zz לחם zz subtag 1 zz"))

        val translated = SubtitleTranslationProcessor.translate(entries, "he", { replies.removeFirst() }, { _, _ -> })

        assertEquals("<i>$rlm" + "בצק</i>", translated[0].rawText)
        assertEquals("בצק", translated[0].text.withoutRlm())
        assertTrue(translated[1].rawText.startsWith("<b>") && translated[1].rawText.endsWith("</b>"))
        assertEquals("לחם", translated[1].text.withoutRlm().trim())
        assertTrue("No translation marker should reach the user", translated.none { "subtag" in it.rawText.lowercase() })
    }

    @Test
    fun `reports progress for every cue`() = runTest {
        val entries = listOf(cue(1, "One", 0, 500), cue(2, "two.", 600, 1000), cue(3, "Three.", 5000, 6000))
        val progress = mutableListOf<Pair<Int, Int>>()
        SubtitleTranslationProcessor.translate(entries, "fr", { it }, { done, total -> progress += done to total })
        assertEquals(3 to 3, progress.last())
    }

    @Test
    fun `distributes text by weight and prefers punctuation boundaries`() {
        assertEquals(
            listOf("one two,", "three four five"),
            SubtitleTranslationProcessor.distribute("one two, three four five", listOf(1, 1)),
        )
        assertEquals(null, SubtitleTranslationProcessor.distribute("one", listOf(1, 1)))
    }

    private fun String.withoutRlm() = replace(rlm, "")

    private fun cue(index: Int, text: String, start: Long, end: Long) = SubtitleEntry(
        index, start, end, "00:00:0$index,000", "00:00:0$index,900", text.replace(Regex("<[^>]+>|[{][^}]*[}]"), ""), text,
    )
}
