package com.subtranslate.domain.usecase

import android.content.Context
import com.subtranslate.data.parser.AssParser
import com.subtranslate.data.parser.SrtParser
import com.subtranslate.data.parser.VttParser
import com.subtranslate.domain.model.SubtitleFile
import com.subtranslate.domain.model.SubtitleFormat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

class SaveSubtitleUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    operator fun invoke(subtitleFile: SubtitleFile, fileName: String): Result<File> = runCatching {
        val parser = when (subtitleFile.format) {
            SubtitleFormat.SRT, SubtitleFormat.SUB -> SrtParser()
            SubtitleFormat.VTT -> VttParser()
            SubtitleFormat.ASS, SubtitleFormat.SSA -> AssParser()
        }

        val content = parser.write(subtitleFile)
        val dir = File(context.filesDir, "subtitles/translated").apply { mkdirs() }
        val file = File(dir, fileName)
        file.writeText(content, Charsets.UTF_8)
        file
    }
}
