package com.subtranslate.presentation.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@Composable
fun SearchScreen(
    onSearch: (String) -> Unit,
    onShowAll: (String) -> Unit = {},
    searchEnabled: Boolean = true,
    maintenanceMessage: String = "",
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var langFilter by remember { mutableStateOf("") }
    var langSearchOpen by remember { mutableStateOf(false) }
    var showAdvancedOptions by remember { mutableStateOf(false) }

    // Filtered language list — if user types in lang filter field, narrow the chips
    val visibleLanguages = remember(langFilter) {
        if (langFilter.isBlank()) OPENSUBTITLES_SEARCH_LANGUAGES
        else OPENSUBTITLES_SEARCH_LANGUAGES.filter { (code, name) ->
            code.contains(langFilter, ignoreCase = true) ||
            name.contains(langFilter, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SubtyBg)
            .verticalScroll(rememberScrollState()),
    ) {
        // ── Feature-disabled banner ────────────────────────────────────────────
        if (!searchEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SubtyMocha)
                    .padding(12.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center,
            ) {
                SubtyText(
                    text = maintenanceMessage.ifBlank { "Search is temporarily unavailable." },
                    color = androidx.compose.ui.graphics.Color.White,
                    fontSize = 13,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // ── Page title ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            SubtyPageTitle("Search")
        }
        SubtyLabel(
            "Find subtitles by title",
            modifier = Modifier.padding(start = 24.dp, bottom = 20.dp),
        )

        // ── Search field + autocomplete ───────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (state.query.isNotEmpty()) SubtyMocha else SubtyBorderDim)
                    .background(SubtyBg2)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        val q = state.query.ifBlank { state.imdbId }
                        if (state.selectedMovieTitle == null && q.length >= 2) {
                            onShowAll(q)
                        } else {
                            viewModel.search(); onSearch(q)
                        }
                    }),
                    textStyle = TextStyle(
                        color = SubtyText1,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                    ),
                    cursorBrush = SolidColor(SubtyMocha),
                    decorationBox = { inner ->
                        if (state.query.isEmpty()) {
                            SubtyText("Movie / Series name…", color = SubtyText3, fontSize = 16)
                        }
                        inner()
                    },
                )
                Spacer(Modifier.width(8.dp))
                when {
                    state.suggestionsLoading -> CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 1.5.dp,
                        color = SubtyMocha,
                    )
                    state.query.isNotEmpty() -> Icon(
                        Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = SubtyText3,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { viewModel.onQueryChange("") },
                    )
                    else -> Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = SubtyText3,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            // Advanced search toggle
            Spacer(Modifier.height(6.dp))
            SubtyText(
                text = if (showAdvancedOptions) "▾ Hide advanced" else "▸ Advanced search",
                fontSize = 12,
                color = SubtyMocha,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { showAdvancedOptions = !showAdvancedOptions },
            )

            // IMDB ID field (shown in advanced mode or when explicitly selected)
            if (showAdvancedOptions || state.searchMode == SearchMode.IMDB_ID) {
                Spacer(Modifier.height(12.dp))
                SubtyTextField(
                    value = state.imdbId,
                    onValueChange = viewModel::onImdbIdChange,
                    placeholder = "IMDB ID (e.g. 0133093)",
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Autocomplete dropdown — no posters, just text (faster)
            if (state.showSuggestions && state.combinedSuggestions.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SubtyBorder)
                        .background(SubtyBg),
                ) {
                    state.combinedSuggestions.forEachIndexed { idx, suggestion ->
                        when (suggestion) {
                            is Suggestion.History -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                        ) { viewModel.onHistorySuggestionSelected(suggestion.item) }
                                        .padding(horizontal = 14.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 28.dp, height = 28.dp)
                                            .background(SubtyBg3)
                                            .border(1.dp, SubtyBorderDim),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            Icons.Default.History,
                                            contentDescription = "History",
                                            tint = SubtyText3,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        SubtyText(
                                            suggestion.item.query,
                                            fontSize = 13,
                                            weight = FontWeight.SemiBold,
                                            color = SubtyText1,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        SubtyText(
                                            "Recent search",
                                            fontSize = 11,
                                            color = SubtyText3,
                                        )
                                    }
                                }
                            }
                            is Suggestion.Remote -> {
                                val result = suggestion.result
                                val title = result.attributes.title
                                    ?: result.attributes.originalTitle ?: ""
                                val year  = result.attributes.year?.toString() ?: ""
                                val isTv  = result.type == "tv" ||
                                        result.attributes.featureType?.lowercase() == "tvshow"

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                        ) { viewModel.onSuggestionSelected(result) }
                                        .padding(horizontal = 14.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    // Initials badge instead of poster (much faster — no extra network request)
                                    Box(
                                        modifier = Modifier
                                            .size(width = 28.dp, height = 28.dp)
                                            .background(SubtyBg3)
                                            .border(1.dp, SubtyBorderDim),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        SubtyText(
                                            title.take(1).uppercase(),
                                            fontSize = 11,
                                            weight = FontWeight.Bold,
                                            color = SubtyMocha,
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        SubtyText(
                                            title,
                                            fontSize = 13,
                                            weight = FontWeight.SemiBold,
                                            color = SubtyText1,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        SubtyText(
                                            buildString {
                                                if (year.isNotEmpty()) { append(year); append(" · ") }
                                                append(if (isTv) "Series" else "Movie")
                                            },
                                            fontSize = 11,
                                            color = SubtyText3,
                                        )
                                    }
                                }
                            }
                        }
                        if (idx < state.combinedSuggestions.lastIndex) SubtyDividerDim()
                    }

                    // "Show All" footer — always shown when there are any remote results
                    if (state.suggestions.isNotEmpty()) {
                        SubtyDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { onShowAll(state.query) }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SubtyText(
                                "Show all results for \"${state.query}\"",
                                fontSize = 12,
                                color = SubtyMocha,
                                weight = FontWeight.SemiBold,
                            )
                            SubtyText("→", fontSize = 14, color = SubtyMocha, weight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ── Recent content carousel ───────────────────────────────────────────
        if (state.recentShows.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            SubtyLabel(
                "Recently searched",
                modifier = Modifier.padding(start = 24.dp, bottom = 10.dp),
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.recentShows, key = { it.id }) { item ->
                    RecentShowCard(
                        item = item,
                        isLoadingNext = state.nextEpisodeLoadingId == item.id,
                        onFindNext = {
                            viewModel.findNextEpisode(item) { q ->
                                viewModel.search()
                                onSearch(q)
                            }
                        },
                        onTap = {
                            viewModel.onHistorySuggestionSelected(item)
                        },
                    )
                }
            }
        }

        // ── Season / Episode — only for TV series ─────────────────────────────
        if (!state.isMovie) {
            Spacer(Modifier.height(8.dp))
            SubtyText(
                text = "Optional — leave defaults to search all episodes",
                fontSize = 11,
                color = SubtyText3,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            if (!state.useSeasonEpisodeTextFields) {
                Spacer(Modifier.height(12.dp))
                SubtyLabel("Season", modifier = Modifier.padding(horizontal = 24.dp))
                Spacer(Modifier.height(8.dp))
                val seasonCount = if (state.seasonsCount > 0) state.seasonsCount else 30
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    items(seasonCount) { i ->
                        val s = (i + 1).toString()
                        SubtyChip(
                            text = s, selected = state.season == s,
                            onClick = { viewModel.onSeasonChange(s) },
                            modifier = if (i > 0) Modifier.offset(x = (-1).dp) else Modifier,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                SubtyLabel("Episode", modifier = Modifier.padding(horizontal = 24.dp))
                Spacer(Modifier.height(8.dp))
                val epCount = if (state.episodesCount in 1..80) state.episodesCount else 30
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    items(epCount) { i ->
                        val e = (i + 1).toString()
                        SubtyChip(
                            text = e, selected = state.episode == e,
                            onClick = { viewModel.onEpisodeChange(e) },
                            modifier = if (i > 0) Modifier.offset(x = (-1).dp) else Modifier,
                        )
                    }
                }
            } else {
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.padding(horizontal = 24.dp)) {
                    SubtyTextField(
                        value = state.season,
                        onValueChange = viewModel::onSeasonChange,
                        placeholder = "Season",
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width((-1).dp))
                    SubtyTextField(
                        value = state.episode,
                        onValueChange = viewModel::onEpisodeChange,
                        placeholder = "Episode",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // ── Language selector ─────────────────────────────────────────────────
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            SubtyLabel("Languages")
            // Magnifying glass toggle — tap to open/close search field
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        langSearchOpen = !langSearchOpen
                        if (!langSearchOpen) langFilter = ""
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search languages",
                    tint = if (langSearchOpen) SubtyMocha else SubtyText3,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        // Expandable language search row
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
                    .padding(horizontal = 24.dp)
                    .border(1.dp, SubtyMocha)
                    .background(SubtyBg2)
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                decorationBox = { inner ->
                    if (langFilter.isEmpty()) SubtyText("Search languages…", color = SubtyText3, fontSize = 13)
                    inner()
                },
            )
        }
        Spacer(Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            items(visibleLanguages) { (code, _) ->
                val i = visibleLanguages.indexOfFirst { it.first == code }
                SubtyChip(
                    text = code.uppercase(),
                    selected = code in state.selectedLanguages,
                    onClick = { viewModel.toggleLanguage(code) },
                    modifier = if (i > 0) Modifier.offset(x = (-1).dp) else Modifier,
                )
            }
        }

        // ── Search button ─────────────────────────────────────────────────────
        Spacer(Modifier.height(24.dp))
        SubtyButton(
            text = "Search Subtitles",
            onClick = { viewModel.search(); onSearch(state.query.ifBlank { state.imdbId }) },
            style = SubtyButtonStyle.FILLED,
            enabled = searchEnabled && !state.isLoading && (state.query.isNotBlank() || state.imdbId.isNotBlank()),
            loading = state.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        )

        state.error?.let { err ->
            Spacer(Modifier.height(12.dp))
            SubtyErrorBanner(err, modifier = Modifier.padding(horizontal = 24.dp))
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun RecentShowCard(
    item: SearchHistoryEntity,
    isLoadingNext: Boolean,
    onFindNext: () -> Unit,
    onTap: () -> Unit,
) {
    val isTv = item.contentType == "tv"
    val episodeLabel = if (item.season != null && item.episode != null)
        "S${item.season} E${item.episode}" else ""

    Column(
        modifier = Modifier.width(120.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        // Poster — tappable to pre-fill the search bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp)
                .background(SubtyBg3)
                .border(1.dp, SubtyBorderDim)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onTap,
                ),
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
                        fontSize = 28,
                        weight = FontWeight.Bold,
                        color = SubtyMocha,
                    )
                }
            }

            // Season/episode badge (TV) or "Movie" badge
            val badge = when {
                episodeLabel.isNotEmpty() -> episodeLabel
                !isTv -> "Film"
                else -> null
            }
            if (badge != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .background(SubtyBg.copy(alpha = 0.85f))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                ) {
                    SubtyText(badge, fontSize = 11, weight = FontWeight.Bold, color = SubtyMocha)
                }
            }
        }

        Spacer(Modifier.height(5.dp))
        SubtyText(
            item.query,
            fontSize = 11,
            weight = FontWeight.SemiBold,
            color = SubtyText1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        if (isTv) {
            Spacer(Modifier.height(5.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (isLoadingNext) SubtyBorderDim else SubtyMocha)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = !isLoadingNext,
                        onClick = onFindNext,
                    )
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (isLoadingNext) {
                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = SubtyMocha)
                } else {
                    SubtyText("Find next episode", fontSize = 10, weight = FontWeight.SemiBold, color = SubtyMocha)
                }
            }
        }
    }
}
