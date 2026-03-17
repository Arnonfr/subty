package com.subtranslate.presentation.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.subtranslate.util.OPENSUBTITLES_SEARCH_LANGUAGES

@Composable
fun SearchScreen(
    onSearch: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Subty", style = MaterialTheme.typography.titleLarge)

        // Search mode toggle
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SearchMode.values().forEach { mode ->
                FilterChip(
                    selected = state.searchMode == mode,
                    onClick = { viewModel.onSearchModeChange(mode) },
                    label = { Text(if (mode == SearchMode.TITLE) "Title" else "IMDB ID") }
                )
            }
        }

        // Main search field with autocomplete (OpenSubtitles /features)
        if (state.searchMode == SearchMode.TITLE) {
            Column {
                // Show selected poster as thumbnail inside the field row
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.selectedPosterUrl?.let { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(MaterialTheme.shapes.small)
                        )
                    }
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = viewModel::onQueryChange,
                        modifier = Modifier.weight(1f),
                        label = { Text("Movie / Series name") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            if (state.suggestionsLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            viewModel.search()
                            onSearch(state.query)
                        })
                    )
                }

                // Autocomplete suggestions
                if (state.showSuggestions && state.suggestions.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Column {
                            state.suggestions.forEach { result ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.onSuggestionSelected(result) }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    val title = result.attributes.title
                                        ?: result.attributes.originalTitle ?: ""
                                    val year = result.attributes.year?.toString() ?: ""
                                    val isTv = result.type == "tv" ||
                                        result.attributes.featureType?.lowercase() == "tvshow"

                                    result.attributes.imgUrl?.let { url ->
                                        AsyncImage(
                                            model = url,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(width = 28.dp, height = 42.dp)
                                                .clip(MaterialTheme.shapes.small)
                                        )
                                    } ?: Box(
                                        modifier = Modifier.size(width = 28.dp, height = 42.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            title.take(1),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            if (year.isNotEmpty()) {
                                                Text(year, style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Text(
                                                if (isTv) "Series" else "Movie",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                                if (result != state.suggestions.last()) HorizontalDivider()
                            }
                        }
                    }
                }

                // Show error if suggestions fetch failed
                state.suggestionsError?.let { err ->
                    Text(
                        text = "Autocomplete error: $err",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }
            }
        } else {
            OutlinedTextField(
                value = state.imdbId,
                onValueChange = viewModel::onImdbIdChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("IMDB ID (e.g. 0133093)") },
                singleLine = true
            )
        }

        // Season / Episode
        // Season / Episode
        if (state.seasonsCount > 0) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Season", style = MaterialTheme.typography.labelSmall)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(state.seasonsCount) { index ->
                        val s = (index + 1).toString()
                        FilterChip(
                            selected = state.season == s,
                            onClick = { viewModel.onSeasonChange(s) },
                            label = { Text(s) }
                        )
                    }
                }

                Text("Episode", style = MaterialTheme.typography.labelSmall)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Use episodes_count as a guide, or default to a reasonable number
                    val count = if (state.episodesCount > 0) {
                        // If it's a huge number, it's probably total episodes across all seasons
                        if (state.episodesCount > 50) 30 else state.episodesCount
                    } else 30
                    
                    items(count) { index ->
                        val e = (index + 1).toString()
                        FilterChip(
                            selected = state.episode == e,
                            onClick = { viewModel.onEpisodeChange(e) },
                            label = { Text(e) }
                        )
                    }
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.season,
                    onValueChange = viewModel::onSeasonChange,
                    modifier = Modifier.weight(1f),
                    label = { Text("Season") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.episode,
                    onValueChange = viewModel::onEpisodeChange,
                    modifier = Modifier.weight(1f),
                    label = { Text("Episode") },
                    singleLine = true
                )
            }
        }

        // Language selection
        Text("Languages", style = MaterialTheme.typography.labelMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(OPENSUBTITLES_SEARCH_LANGUAGES) { (code, label) ->
                FilterChip(
                    selected = code in state.selectedLanguages,
                    onClick = { viewModel.toggleLanguage(code) },
                    label = { Text(label) }
                )
            }
        }

        Button(
            onClick = { viewModel.search(); onSearch(state.query) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading && (state.query.isNotBlank() || state.imdbId.isNotBlank())
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text("Search Subtitles")
        }

        state.error?.let { err ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(text = err, modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}
