package com.subtranslate.data.parser

import com.subtranslate.domain.model.SubtitleFile
import com.subtranslate.domain.model.SubtitleFormat

interface SubtitleParser {
    fun parse(content: String): SubtitleFile
    fun write(subtitleFile: SubtitleFile): String
    fun supportedFormat(): SubtitleFormat
}
