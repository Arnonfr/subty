package com.subtranslate.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.subtranslate.data.local.entity.SearchHistoryEntity
import com.subtranslate.domain.model.HistoryItem
import com.subtranslate.presentation.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    onSearchAgain: (SearchHistoryEntity) -> Unit = {},
    onBrowseEpisodes: (SearchHistoryEntity) -> Unit = {},
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val downloads by viewModel.history.collectAsState()
    val searches  by viewModel.searchHistory.collectAsState()
    val hasAny = downloads.isNotEmpty() || searches.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SubtyBg),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 8.dp, top = 24.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            SubtyPageTitle("History")
            if (hasAny) {
                SubtyButton(
                    text = "Clear All",
                    onClick = { viewModel.clearAllSearches(); viewModel.clearAllDownloads() },
                    style = SubtyButtonStyle.OUTLINE,
                    small = true,
                )
            }
        }
        SubtyLabel(
            "Searches and downloads",
            modifier = Modifier.padding(start = 24.dp, bottom = 20.dp),
        )

        if (!hasAny) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                SubtyText("No history yet", color = SubtyText3)
            }
        } else {
            SubtyDivider()
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                // ── Search history ────────────────────────────────────────────
                if (searches.isNotEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SubtyBg2)
                                .padding(horizontal = 24.dp, vertical = 10.dp),
                        ) {
                            SubtyLabel("Recent Searches")
                        }
                        SubtyDividerDim()
                    }
                    items(searches, key = { "s${it.id}" }) { item ->
                        SearchHistoryRow(
                            item = item,
                            onClick = {
                                viewModel.restoreSession(item)
                                onSearchAgain(item)
                            },
                            onDelete = { viewModel.deleteSearch(item.id) },
                            onBrowseEpisodes = if (item.contentType == "tv") {
                                {
                                    viewModel.prepareSeriesBrowse(item)
                                    onBrowseEpisodes(item)
                                }
                            } else null,
                        )
                        SubtyDividerDim()
                    }
                }

                // ── Download history ──────────────────────────────────────────
                if (downloads.isNotEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SubtyBg2)
                                .padding(horizontal = 24.dp, vertical = 10.dp),
                        ) {
                            SubtyLabel("Downloads")
                        }
                        SubtyDividerDim()
                    }
                    items(downloads, key = { "d${it.id}" }) { item ->
                        DownloadHistoryRow(item = item, onDelete = { viewModel.delete(item.id) })
                        SubtyDividerDim()
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchHistoryRow(
    item: SearchHistoryEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onBrowseEpisodes: (() -> Unit)? = null,
) {
    val sdf = remember { SimpleDateFormat("dd MMM yyyy  HH:mm", Locale.getDefault()) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SubtyBg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(start = 24.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                SubtyText(item.query, fontSize = 14, weight = FontWeight.SemiBold, color = SubtyText1)
                val detail = buildString {
                    item.season?.let { append("S$it") }
                    item.episode?.let { if (isNotEmpty()) append(" "); append("E$it") }
                    item.languages?.let {
                        if (isNotEmpty()) append("  ·  ")
                        append(it.uppercase())
                    }
                }
                if (detail.isNotEmpty()) SubtyText(detail, fontSize = 11, color = SubtyMocha)
                SubtyText(sdf.format(Date(item.searchedAt)), fontSize = 10, color = SubtyText3)
            }
            if (onBrowseEpisodes != null) {
                SubtyButton(
                    text = "Browse",
                    onClick = onBrowseEpisodes,
                    style = SubtyButtonStyle.OUTLINE,
                    small = true,
                )
                Spacer(Modifier.width(4.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = SubtyText3,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun DownloadHistoryRow(item: HistoryItem, onDelete: () -> Unit) {
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SubtyBg)
            .padding(start = 24.dp, top = 14.dp, bottom = 14.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            SubtyText(item.movieTitle, fontSize = 14, weight = FontWeight.SemiBold, color = SubtyText1)
            SubtyText(
                buildString {
                    append(item.originalLanguage.uppercase())
                    if (item.translatedLanguage != null) {
                        append(" → ${item.translatedLanguage.uppercase()}")
                    } else {
                        append("  ·  Downloaded")
                    }
                    append("  ·  ${item.format.uppercase()}")
                },
                fontSize = 11, color = SubtyMocha, letterSpacing = 0.04f,
            )
            SubtyText(sdf.format(Date(item.downloadedAt)), fontSize = 10, color = SubtyText3)
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                tint = SubtyText3,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
