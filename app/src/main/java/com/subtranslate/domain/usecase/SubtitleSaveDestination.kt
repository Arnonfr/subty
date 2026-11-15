package com.subtranslate.domain.usecase

import android.os.Environment

internal const val SAVE_DESTINATION_MOVIES = "movies"
internal const val SAVE_DESTINATION_CUSTOM = "custom"
internal const val DEFAULT_SUBTITLE_FOLDER_NAME = "Subty"

internal fun defaultSubtitleRelativePath(): String =
    "${Environment.DIRECTORY_MOVIES}/$DEFAULT_SUBTITLE_FOLDER_NAME/"

internal fun defaultSubtitleFolderLabel(): String =
    "${Environment.DIRECTORY_MOVIES}/$DEFAULT_SUBTITLE_FOLDER_NAME"
