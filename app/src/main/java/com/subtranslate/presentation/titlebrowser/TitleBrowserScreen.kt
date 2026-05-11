package com.subtranslate.presentation.titlebrowser

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.subtranslate.data.remote.opensubtitles.dto.FeatureDto
import com.subtranslate.presentation.theme.*

@Composable
fun TitleBrowserScreen(
    query: String,
    onBack: () -> Unit,
    onSelected: () -> Unit,
    viewModel: TitleBrowserViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var sortMenuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(query) { viewModel.load(query) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SubtyBg),
    ) {
        SubtyTopBar(title = "\"$query\"", onBack = onBack)
        SubtyDivider()

        // ── Toolbar: count + filter chips + sort + view toggle ────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Result count
            if (!state.isLoading) {
                SubtyLabel(
                    "${state.displayResults.size} titles",
                    modifier = Modifier.weight(1f),
                )
            } else {
                Spacer(Modifier.weight(1f))
            }

            // Sort picker
            Box {
                val sortLabel = when (state.sortOrder) {
                    TitleSortOrder.POPULARITY -> "Popular"
                    TitleSortOrder.YEAR_DESC -> "Newest"
                    TitleSortOrder.YEAR_ASC -> "Oldest"
                }
                Box(
                    modifier = Modifier
                        .border(1.dp, SubtyBorder)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { sortMenuOpen = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    SubtyText("↕ $sortLabel", fontSize = 12, color = SubtyText2)
                }
                DropdownMenu(
                    expanded = sortMenuOpen,
                    onDismissRequest = { sortMenuOpen = false },
                ) {
                    TitleSortOrder.entries.forEach { order ->
                        val label = when (order) {
                            TitleSortOrder.POPULARITY -> "Popularity"
                            TitleSortOrder.YEAR_DESC -> "Newest first"
                            TitleSortOrder.YEAR_ASC -> "Oldest first"
                        }
                        DropdownMenuItem(
                            text = { SubtyText(label, fontSize = 13) },
                            onClick = { viewModel.setSortOrder(order); sortMenuOpen = false },
                        )
                    }
                }
            }

            // Grid / List toggle
            Icon(
                imageVector = if (state.viewMode == TitleViewMode.GRID) Icons.Default.ViewList else Icons.Default.GridView,
                contentDescription = "Toggle view",
                tint = SubtyMocha,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { viewModel.toggleViewMode() },
            )
        }

        // ── Filter chips: All / Movie / TV ────────────────────────────────────
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy((-1).dp),
            modifier = Modifier.padding(bottom = 10.dp),
        ) {
            items(TitleTypeFilter.entries) { filter ->
                val label = when (filter) {
                    TitleTypeFilter.ALL -> "All"
                    TitleTypeFilter.MOVIE -> "Movies"
                    TitleTypeFilter.TV -> "Series"
                }
                SubtyChip(
                    text = label,
                    selected = state.typeFilter == filter,
                    onClick = { viewModel.setTypeFilter(filter) },
                )
            }
        }

        SubtyDividerDim()

        // ── Results ───────────────────────────────────────────────────────────
        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SubtyMocha, strokeWidth = 1.5.dp)
            }
            state.error != null -> SubtyErrorBanner(
                state.error!!,
                modifier = Modifier.padding(16.dp),
            )
            state.viewMode == TitleViewMode.LIST -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy((-1).dp),
                ) {
                    items(state.displayResults, key = { it.id }) { feature ->
                        TitleListRow(feature = feature, onClick = {
                            viewModel.selectFeature(feature); onSelected()
                        })
                    }
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.displayResults, key = { it.id }) { feature ->
                        TitleGridCell(feature = feature, onClick = {
                            viewModel.selectFeature(feature); onSelected()
                        })
                    }
                }
            }
        }
    }
}

// ── List row ──────────────────────────────────────────────────────────────────

@Composable
private fun TitleListRow(feature: FeatureDto, onClick: () -> Unit) {
    val title = feature.attributes.title ?: feature.attributes.originalTitle ?: ""
    val year = feature.attributes.year?.toString() ?: ""
    val isTv = feature.type == "tv" || feature.attributes.featureType?.lowercase() == "tvshow"
    val seasons = feature.attributes.seasonsCount
    val posterUrl = feature.attributes.imgUrl

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SubtyBg)
            .border(1.dp, SubtyBorderDim)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 62.dp)
                .background(SubtyBg3)
                .border(1.dp, SubtyBorderDim),
            contentAlignment = Alignment.Center,
        ) {
            if (posterUrl != null) {
                AsyncImage(model = posterUrl, contentDescription = title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                SubtyText(title.take(1).uppercase(), fontSize = 16, weight = FontWeight.Bold, color = SubtyMocha)
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            SubtyText(title, fontSize = 14, weight = FontWeight.SemiBold, color = SubtyText1, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            SubtyText(
                buildString {
                    if (year.isNotEmpty()) { append(year); append(" · ") }
                    if (isTv) {
                        append("Series")
                        if (seasons != null && seasons > 0) append(" · $seasons seasons")
                    } else append("Movie")
                },
                fontSize = 12, color = SubtyText3,
            )
        }

        Box(
            modifier = Modifier.border(1.dp, if (isTv) SubtyMocha else SubtyBorderDim).padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            SubtyText(if (isTv) "TV" else "Film", fontSize = 11, weight = FontWeight.SemiBold, color = if (isTv) SubtyMocha else SubtyText3)
        }
    }
}

// ── Grid cell ─────────────────────────────────────────────────────────────────

@Composable
private fun TitleGridCell(feature: FeatureDto, onClick: () -> Unit) {
    val title = feature.attributes.title ?: feature.attributes.originalTitle ?: ""
    val year = feature.attributes.year?.toString() ?: ""
    val isTv = feature.type == "tv" || feature.attributes.featureType?.lowercase() == "tvshow"
    val posterUrl = feature.attributes.imgUrl

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.Start,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .background(SubtyBg3)
                .border(1.dp, SubtyBorderDim),
            contentAlignment = Alignment.Center,
        ) {
            if (posterUrl != null) {
                AsyncImage(model = posterUrl, contentDescription = title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                SubtyText(title.take(1).uppercase(), fontSize = 22, weight = FontWeight.Bold, color = SubtyMocha)
            }

            // Year badge top-right
            if (year.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .background(SubtyBg.copy(alpha = 0.82f))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                ) {
                    SubtyText(year, fontSize = 10, color = SubtyText3)
                }
            }

            // TV/Film badge bottom-left
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .background((if (isTv) SubtyMocha else SubtyBg3).copy(alpha = 0.9f))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            ) {
                SubtyText(if (isTv) "TV" else "Film", fontSize = 10, weight = FontWeight.Bold, color = if (isTv) SubtyBg else SubtyText3)
            }
        }

        Spacer(Modifier.height(4.dp))
        SubtyText(title, fontSize = 11, weight = FontWeight.SemiBold, color = SubtyText1, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
