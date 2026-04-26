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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.subtranslate.presentation.theme.*
import com.subtranslate.util.OPENSUBTITLES_SEARCH_LANGUAGES

@Composable
fun SearchScreen(
    onSearch: (String) -> Unit,
    searchEnabled: Boolean = true,
    maintenanceMessage: String = "",
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var langFilter by remember { mutableStateOf("") }
    var langSearchOpen by remember { mutableStateOf(false) }

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
        SubtyPageTitle(
            "Search",
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 4.dp),
        )
        SubtyLabel(
            "Find subtitles by title or IMDB ID",
            modifier = Modifier.padding(start = 24.dp, bottom = 20.dp),
        )

        // ── Search mode tabs ──────────────────────────────────────────────────
        Row(modifier = Modifier.padding(horizontal = 24.dp)) {
            SearchMode.values().forEachIndexed { i, mode ->
                val selected = state.searchMode == mode
                SubtyChip(
                    text = if (mode == SearchMode.TITLE) "Title" else "IMDB ID",
                    selected = selected,
                    onClick = { viewModel.onSearchModeChange(mode) },
                    modifier = if (i > 0) Modifier.offset(x = (-1).dp) else Modifier,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Search field + autocomplete ───────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            if (state.searchMode == SearchMode.TITLE) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, if (state.query.isNotEmpty()) SubtyMocha else SubtyBorderDim)
                        .background(SubtyBg2)
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicTextField(
                        value = state.query,
                        onValueChange = viewModel::onQueryChange,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            viewModel.search(); onSearch(state.query)
                        }),
                        textStyle = TextStyle(
                            color = SubtyText1,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                        ),
                        cursorBrush = SolidColor(SubtyMocha),
                        decorationBox = { inner ->
                            if (state.query.isEmpty()) {
                                SubtyText("Movie / Series name…", color = SubtyText3, fontSize = 14)
                            }
                            inner()
                        },
                    )
                    Spacer(Modifier.width(8.dp))
                    when {
                        state.suggestionsLoading -> CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 1.5.dp,
                            color = SubtyMocha,
                        )
                        state.query.isNotEmpty() -> Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = SubtyText3,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { viewModel.onQueryChange("") },
                        )
                        else -> Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = SubtyText3,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                // Autocomplete dropdown — no posters, just text (faster)
                if (state.showSuggestions && state.combinedSuggestions.isNotEmpty()) {
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
                    }
                }
            } else {
                SubtyTextField(
                    value = state.imdbId,
                    onValueChange = viewModel::onImdbIdChange,
                    placeholder = "IMDB ID (e.g. 0133093)",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // ── Season / Episode — only for TV series ─────────────────────────────
        if (!state.isMovie) {
            if (!state.useSeasonEpisodeTextFields) {
                Spacer(Modifier.height(16.dp))
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
            onClick = { viewModel.search(); onSearch(state.query) },
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
