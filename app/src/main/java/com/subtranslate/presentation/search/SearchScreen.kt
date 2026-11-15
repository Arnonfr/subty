package com.subtranslate.presentation.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.subtranslate.data.local.entity.SearchHistoryEntity
import com.subtranslate.presentation.theme.*
import com.subtranslate.util.OPENSUBTITLES_SEARCH_LANGUAGES
import kotlin.math.min

@Composable
fun SearchScreen(
    onSearch: (String) -> Unit,
    onShowAll: (String) -> Unit,
    searchEnabled: Boolean = true,
    maintenanceMessage: String = "",
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var langFilter by remember { mutableStateOf("") }
    var langSearchOpen by remember { mutableStateOf(false) }
    var showAdvancedOptions by remember { mutableStateOf(false) }
    val submitSearch = {
        viewModel.search()
        val q = state.query.ifBlank { state.imdbId }
        if (state.selectedMovieTitle != null || state.imdbId.isNotBlank()) {
            onSearch(q)
        } else {
            viewModel.prepareTitleBrowse()
            onShowAll(state.query)
        }
    }

    val visibleLanguages = remember(langFilter) {
        if (langFilter.isBlank()) {
            OPENSUBTITLES_SEARCH_LANGUAGES
        } else {
            OPENSUBTITLES_SEARCH_LANGUAGES.filter { (code, name) ->
                code.contains(langFilter, ignoreCase = true) || name.contains(langFilter, ignoreCase = true)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshUsage()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SubtyBg)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp),
    ) {
        if (!searchEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SubtyAccent)
                    .padding(12.dp),
            ) {
                SubtyText(
                    text = maintenanceMessage.ifBlank { "Search is temporarily unavailable." },
                    color = SubtyWhite,
                    fontSize = 13,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF0B0D13),
                            Color(0xFF0B0D13),
                            Color(0xFF120F18),
                        ),
                    ),
                ),
    ) {
        if (state.selectedMovieTitle == null) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 26.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
                SubtyText(
                    text = "SEARCH",
                    color = SubtyWhite,
                    fontSize = 34,
                    weight = FontWeight.Black,
                    uppercase = true,
                )

                SubtyText(
                    text = "Find subtitles by title",
                    color = SubtyWhite.copy(alpha = 0.72f),
                    fontSize = 11,
                    weight = FontWeight.Bold,
                    uppercase = true,
                    letterSpacing = 0.12f,
                )

                DarkSearchBar(
                    query = state.query,
                    showAdvancedOptions = showAdvancedOptions,
                    suggestionsLoading = state.suggestionsLoading,
                    onQueryChange = viewModel::onQueryChange,
                    onSubmit = {
                        if (state.imdbId.isNotBlank()) {
                            submitSearch()
                        } else {
                            viewModel.prepareTitleBrowse()
                            onShowAll(state.query)
                        }
                    },
                    onClear = { viewModel.onQueryChange("") },
                    onToggleAdvanced = { showAdvancedOptions = !showAdvancedOptions },
                )

                if (showAdvancedOptions || state.searchMode == SearchMode.IMDB_ID) {
                    SubtyTextField(
                        value = state.imdbId,
                        onValueChange = viewModel::onImdbIdChange,
                        placeholder = "IMDB ID (e.g. 0133093)",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (state.showSuggestions && state.combinedSuggestions.isNotEmpty()) {
                    SuggestionPanel(
                        suggestions = state.combinedSuggestions,
                        onHistorySelected = { item ->
                            val isMovie = !isHistoryItemTv(item)
                            viewModel.onHistorySuggestionSelected(item)
                            if (isMovie) onSearch(item.query)
                        },
                        onRemoteSelected = { feature ->
                            val isMovie = feature.type != "tv" && feature.attributes.featureType?.lowercase() != "tvshow"
                            viewModel.onSuggestionSelected(feature)
                            if (isMovie) {
                                onSearch(feature.attributes.title ?: feature.attributes.originalTitle ?: "")
                            }
                        },
                        onShowAll = {
                            viewModel.prepareTitleBrowse()
                            onShowAll(state.query)
                        },
                    )
                } else if (state.query.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SubtyWhite.copy(alpha = 0.16f))
                            .background(Color.White.copy(alpha = 0.03f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                viewModel.prepareTitleBrowse()
                                onShowAll(state.query)
                            }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SubtyText(
                                text = "Show all results for \"${state.query}\"",
                                fontSize = 12,
                                color = SubtyWhite,
                                weight = FontWeight.SemiBold,
                            )
                            SubtyText("→", fontSize = 14, color = SubtyWhite, weight = FontWeight.Bold)
                        }
                    }
                }

                SubtyLabel(
                    "Recently searched",
                    color = SubtyWhite.copy(alpha = 0.70f),
                )

                LazyRow(
                    contentPadding = PaddingValues(end = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.recentShows, key = { it.id }) { item ->
                        RecentPosterCard(
                            item = item,
                            isLoadingNext = state.nextEpisodeLoadingId == item.id,
                            onClick = {
                                val isMovie = !isHistoryItemTv(item)
                                viewModel.onHistorySuggestionSelected(item)
                                if (isMovie) onSearch(item.query)
                            },
                            onFindNext = { viewModel.findNextEpisode(item) { nextQuery -> onSearch(nextQuery) } },
                        )
                    }
                }
            }
        }
        }

        if (state.selectedMovieTitle != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(Color(0xFF0B0D13)),
            ) {
                if (!state.selectedPosterUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = state.selectedPosterUrl,
                        contentDescription = state.selectedMovieTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.10f),
                                    Color.Black.copy(alpha = 0.20f),
                                    Color.Black.copy(alpha = 0.88f),
                                ),
                            ),
                        ),
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 16.dp, top = 16.dp)
                        .size(32.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { viewModel.clearSelectedTitle() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = SubtyWhite,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SubtyLabel(
                        if (state.isMovie) "Movie" else "Series",
                        color = SubtyWhite.copy(alpha = 0.82f),
                    )
                        SubtyText(
                            text = state.selectedMovieTitle.orEmpty(),
                            fontSize = 26,
                            weight = FontWeight.Black,
                            color = SubtyWhite,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (!state.isMovie && state.season.isNotBlank() && state.episode.isNotBlank()) {
                            SubtyText(
                                text = "Season ${state.season}  •  Episode ${state.episode}",
                                fontSize = 11,
                                weight = FontWeight.SemiBold,
                                color = SubtyWhite.copy(alpha = 0.88f),
                            )
                        }
                        if (!state.isMovie) {
                            SubtyText(
                                text = "${state.seasonsCount} seasons",
                                fontSize = 12,
                            weight = FontWeight.SemiBold,
                            color = SubtyWhite.copy(alpha = 0.82f),
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                if (false && !state.isMovie) {
                    SubtyButton(
                        text = "Back to search",
                        onClick = { viewModel.clearSelectedTitle() },
                        style = SubtyButtonStyle.OUTLINE,
                        small = true,
                        modifier = Modifier.align(Alignment.Start),
                    )
                }

                    if (!state.isMovie) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SubtyDivider()
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                SubtyText(
                                    text = "Season and episode",
                                    fontSize = 12,
                                weight = FontWeight.Bold,
                                color = SubtyText1,
                                uppercase = true,
                                letterSpacing = 0.08f,
                            )
                            if (!state.useSeasonEpisodeTextFields) {
                                val seasonCount = if (state.seasonsCount > 0) state.seasonsCount else 12
                                val episodeCount = if (state.episodesCount in 1..80) state.episodesCount else 24

                                SubtyText(
                                    text = "Season",
                                    fontSize = 11,
                                    color = SubtyText3,
                                    weight = FontWeight.Bold,
                                    uppercase = true,
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                                    items((1..min(seasonCount, 24)).map { it.toString() }, key = { it }) { season ->
                                        SubtyChip(
                                            text = season,
                                            selected = state.season == season,
                                            onClick = { viewModel.onSeasonChange(season) },
                                            modifier = Modifier.offset(x = if (season == "1") 0.dp else (-1).dp),
                                        )
                                    }
                                }

                                Spacer(Modifier.height(4.dp))

                                SubtyText(
                                    text = "Episode",
                                    fontSize = 11,
                                    color = SubtyText3,
                                    weight = FontWeight.Bold,
                                    uppercase = true,
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                                    items((1..min(episodeCount, 24)).map { it.toString() }, key = { it }) { episode ->
                                        SubtyChip(
                                            text = episode,
                                            selected = state.episode == episode,
                                            onClick = { viewModel.onEpisodeChange(episode) },
                                            modifier = Modifier.offset(x = if (episode == "1") 0.dp else (-1).dp),
                                        )
                                    }
                                }
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    SubtyTextField(
                                        value = state.season,
                                        onValueChange = viewModel::onSeasonChange,
                                        placeholder = "Season",
                                        modifier = Modifier.weight(1f),
                                    )
                                    SubtyTextField(
                                        value = state.episode,
                                        onValueChange = viewModel::onEpisodeChange,
                                        placeholder = "Episode",
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                            SubtyDivider()

                            SubtyButton(
                                text = "Search subtitles",
                                onClick = submitSearch,
                                style = SubtyButtonStyle.FILLED,
                                enabled = searchEnabled && !state.isLoading && state.query.isNotBlank(),
                                loading = state.isLoading,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }

        if (state.selectedMovieTitle != null || state.query.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                SubtyLabel("Languages")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search languages",
                        tint = if (langSearchOpen) SubtyMocha else SubtyText3,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { langSearchOpen = !langSearchOpen },
                    )
                }
            }

            if (langSearchOpen) {
                Spacer(Modifier.height(8.dp))
                BasicTextField(
                    value = langFilter,
                    onValueChange = { langFilter = it },
                    singleLine = true,
                    textStyle = TextStyle(color = SubtyText1, fontSize = 13.sp),
                    cursorBrush = SolidColor(SubtyMocha),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                        .border(1.dp, SubtyMocha)
                        .background(SubtyBg2)
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    decorationBox = { inner ->
                        if (langFilter.isEmpty()) {
                            SubtyText("Search languages…", color = SubtyText3, fontSize = 13)
                        }
                        inner()
                    },
                )
            }

            Spacer(Modifier.height(8.dp))
            LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                items(visibleLanguages) { (code, _) ->
                    val index = visibleLanguages.indexOfFirst { it.first == code }
                    SubtyChip(
                        text = code.uppercase(),
                        selected = code in state.selectedLanguages,
                        onClick = { viewModel.toggleLanguage(code) },
                        modifier = if (index > 0) Modifier.offset(x = (-1).dp) else Modifier,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            val showBottomSearchButton = state.selectedMovieTitle == null
            if (showBottomSearchButton) {
                SubtyButton(
                    text = "Search subtitles",
                    onClick = submitSearch,
                    style = SubtyButtonStyle.FILLED,
                    enabled = searchEnabled && !state.isLoading && (state.query.isNotBlank() || state.imdbId.isNotBlank()),
                    loading = state.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }

        state.error?.let { err ->
            Spacer(Modifier.height(12.dp))
                        SubtyErrorBanner(err, modifier = Modifier.padding(horizontal = 16.dp))
        }

        Spacer(Modifier.height(if (state.selectedMovieTitle != null) 72.dp else 24.dp))
    }
}

private fun isHistoryItemTv(item: SearchHistoryEntity): Boolean {
    return item.contentType == "tv" ||
        item.season != null ||
        item.episode != null ||
        (item.seasonsCount ?: 0) > 0
}

@Composable
private fun DarkSearchBar(
    query: String,
    showAdvancedOptions: Boolean,
    suggestionsLoading: Boolean,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onClear: () -> Unit,
    onToggleAdvanced: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SubtyWhite.copy(alpha = 0.20f))
            .background(Color.White.copy(alpha = 0.04f))
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
            textStyle = TextStyle(
                color = SubtyWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            ),
            cursorBrush = SolidColor(SubtyMocha),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    SubtyText(
                        text = "Movie / Series name…",
                        color = SubtyWhite.copy(alpha = 0.52f),
                        fontSize = 16,
                    )
                }
                inner()
            },
        )

        Spacer(Modifier.width(10.dp))
        if (suggestionsLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 1.8.dp,
                color = SubtyWhite,
            )
        } else {
            Icon(
                Icons.Default.Tune,
                contentDescription = "Advanced search",
                tint = if (showAdvancedOptions) SubtyMocha else SubtyWhite.copy(alpha = 0.72f),
                modifier = Modifier
                    .size(20.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onToggleAdvanced() },
            )
            Spacer(Modifier.width(10.dp))
            if (query.isNotEmpty()) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Clear",
                    tint = SubtyWhite.copy(alpha = 0.72f),
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onClear() },
                )
                Spacer(Modifier.width(10.dp))
            }
            Icon(
                Icons.Default.Search,
                contentDescription = "Search",
                tint = SubtyWhite.copy(alpha = 0.72f),
                modifier = Modifier
                    .size(20.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onSubmit() },
            )
        }
    }
}

@Composable
private fun SuggestionPanel(
    suggestions: List<Suggestion>,
    onHistorySelected: (SearchHistoryEntity) -> Unit,
    onRemoteSelected: (com.subtranslate.data.remote.opensubtitles.dto.FeatureDto) -> Unit,
    onShowAll: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SubtyWhite.copy(alpha = 0.14f))
            .background(Color(0xFF15131C)),
    ) {
        suggestions.forEachIndexed { index, suggestion ->
            when (suggestion) {
                is Suggestion.History -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { onHistorySelected(suggestion.item) }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color.White.copy(alpha = 0.07f))
                                .border(1.dp, Color.White.copy(alpha = 0.10f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = "History",
                                tint = SubtyWhite.copy(alpha = 0.75f),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            SubtyText(
                                suggestion.item.query,
                                fontSize = 13,
                                weight = FontWeight.SemiBold,
                                color = SubtyWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            SubtyText(
                                "Recent search",
                                fontSize = 11,
                                color = SubtyWhite.copy(alpha = 0.58f),
                            )
                        }
                    }
                }

                is Suggestion.Remote -> {
                    val result = suggestion.result
                    val title = result.attributes.title ?: result.attributes.originalTitle ?: ""
                    val year = result.attributes.year?.toString().orEmpty()
                    val isTv = result.type == "tv" || result.attributes.featureType?.lowercase() == "tvshow"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { onRemoteSelected(result) }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 36.dp, height = 50.dp)
                                .background(SubtyBg3)
                                .border(1.dp, Color.White.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            val poster = result.attributes.imgUrl
                            if (poster != null) {
                                AsyncImage(
                                    model = poster,
                                    contentDescription = title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                SubtyText(
                                    title.take(1).uppercase(),
                                    fontSize = 11,
                                    weight = FontWeight.Bold,
                                    color = SubtyWhite,
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            SubtyText(
                                title,
                                fontSize = 13,
                                weight = FontWeight.SemiBold,
                                color = SubtyWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            SubtyText(
                                buildString {
                                    if (year.isNotEmpty()) append(year).append(" · ")
                                    append(if (isTv) "Series" else "Movie")
                                },
                                fontSize = 11,
                                color = SubtyWhite.copy(alpha = 0.58f),
                            )
                        }
                    }
                }
            }

            if (index < suggestions.lastIndex) {
                SubtyDividerDim()
            }
        }

        SubtyDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onShowAll() }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SubtyText(
                "Show all results",
                fontSize = 12,
                color = SubtyMocha,
                weight = FontWeight.SemiBold,
            )
            SubtyText("→", fontSize = 14, color = SubtyMocha, weight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RecentPosterCard(
    item: SearchHistoryEntity,
    isLoadingNext: Boolean,
    onClick: () -> Unit,
    onFindNext: () -> Unit,
) {
    val isTv = isHistoryItemTv(item)
    val episodeLabel = if (item.season != null && item.episode != null) {
        "S${item.season} E${item.episode}"
    } else {
        ""
    }
    val badgeLabel = when {
        episodeLabel.isNotEmpty() -> episodeLabel
        isTv -> "Series"
        else -> "Film"
    }

    Column(
        modifier = Modifier.width(148.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .background(SubtyBg3)
                .border(1.dp, Color.White.copy(alpha = 0.12f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onClick() },
        ) {
            if (item.posterUrl != null) {
                AsyncImage(
                    model = item.posterUrl,
                    contentDescription = item.query,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    SubtyText(
                        item.query.take(1).uppercase(),
                        fontSize = 32,
                        weight = FontWeight.Black,
                        color = SubtyMocha,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.86f),
                            ),
                        ),
                    ),
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .background(Color.Black.copy(alpha = 0.62f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                SubtyText(
                    badgeLabel,
                    fontSize = 10,
                    weight = FontWeight.Bold,
                    color = SubtyWhite,
                    uppercase = true,
                )
            }

            SubtyText(
                item.query,
                color = SubtyWhite,
                fontSize = 13,
                weight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 10.dp, end = 10.dp, bottom = 34.dp),
            )
        }

        if (isTv) {
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (isLoadingNext) SubtyBorderDim else SubtyMocha)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = !isLoadingNext,
                    ) { onFindNext() }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (isLoadingNext) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 1.5.dp,
                        color = SubtyMocha,
                    )
                } else {
                    SubtyText(
                        "Find next episode",
                        fontSize = 11,
                        weight = FontWeight.SemiBold,
                        color = SubtyMocha,
                    )
                }
            }
        }
    }
}
