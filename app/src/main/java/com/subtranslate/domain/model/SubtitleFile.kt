package com.subtranslate.domain.model

import java.nio.charset.Charset

data class SubtitleFile(
    val format: SubtitleFormat,
    val entries: List<SubtitleEntry>,
    val metadata: Map<String, String> = emptyMap(),
    val rawHeader: String? = null,  // preserved verbatim for ASS/VTT header sections
    val encoding: Charset = Charsets.UTF_8,
    val sourceLanguage: String? = null,
    val title: String? = null
)

enum class SubtitleFormat {
    SRT, VTT, ASS, SSA, SUB;

    companion object {
        fun fromExtension(ext: String): SubtitleFormat? = when (ext.lowercase()) {
            "srt" -> SRT
            "vtt" -> VTT
            "ass" -> ASS
            "ssa" -> SSA
            "sub" -> SUB
            else -> null
        }
    }
}
