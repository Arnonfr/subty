package com.subtranslate.presentation.search

internal fun displaySeasonCount(rawCount: Int): Int = rawCount.coerceAtLeast(1)

internal fun displayEpisodeCount(rawCount: Int): Int = rawCount.coerceAtLeast(1)
