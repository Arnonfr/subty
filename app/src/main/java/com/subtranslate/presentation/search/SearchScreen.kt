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
import com.subtranslate.presentation.theme.*
import com.subtranslate.util.OPENSUBTITLES_SEARCH_LANGUAGES

@Composable
fun SearchScreen(
    onSearch: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SubtyBg)
            .verticalScroll(rememberScrollState()),
    ) {
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
                // Field row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, if (state.query.isNotEmpty()) SubtyMocha else SubtyBorderDim)
                        .background(SubtyBg2)
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    state.selectedPosterUrl?.let { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(width = 24.dp, height = 36.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                    }
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
                    if (state.suggestionsLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 1.5.dp,
                            color = SubtyMocha,
                        )
                    } else {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = SubtyText3,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                // Autocomplete dropdown
                if (state.showSuggestions && state.suggestions.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SubtyBorder)
                            .background(SubtyBg),
                    ) {
                        state.suggestions.forEachIndexed { idx, result ->
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
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 32.dp, height = 48.dp)
                                        .background(SubtyBg3),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    result.attributes.imgUrl?.let { url ->
                                        AsyncImage(
                                            model = url,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    } ?: SubtyText(
                                        title.take(2).uppercase(),
                                        fontSize = 10,
                                        weight = FontWeight.Bold,
                                        color = SubtyText3,
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
                            if (idx < state.suggestions.lastIndex) SubtyDividerDim()
                        }
                    }
                }

                state.suggestionsError?.let { err ->
                    Spacer(Modifier.height(4.dp))
                    SubtyText("Autocomplete: $err", fontSize = 11, color = SubtyError)
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

        // ── Season / Episode ──────────────────────────────────────────────────
        if (state.seasonsCount > 0) {
            Spacer(Modifier.height(16.dp))
            SubtyLabel("Season", modifier = Modifier.padding(horizontal = 24.dp))
            Spacer(Modifier.height(8.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                items(state.seasonsCount) { i ->
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
            val epCount = if (state.episodesCount in 1..50) state.episodesCount else 30
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

        // ── Language chips ────────────────────────────────────────────────────
        Spacer(Modifier.height(16.dp))
        SubtyLabel("Languages", modifier = Modifier.padding(horizontal = 24.dp))
        Spacer(Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            items(OPENSUBTITLES_SEARCH_LANGUAGES) { (code, _) ->
                val i = OPENSUBTITLES_SEARCH_LANGUAGES.indexOfFirst { it.first == code }
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
            enabled = !state.isLoading && (state.query.isNotBlank() || state.imdbId.isNotBlank()),
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
