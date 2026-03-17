package com.subtranslate.presentation.results

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.subtranslate.domain.model.SubtitleSearchResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    query: String,
    onTranslate: (fileId: Int, fileName: String, languageCode: String) -> Unit,
    onBack: () -> Unit,
    viewModel: ResultsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(query) { viewModel.search(query) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(query, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {

            // ── Movie poster header ─────────────────────────────────────────
            state.posterUrl?.let { url ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AsyncImage(
                        model = url,
                        contentDescription = query,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(width = 56.dp, height = 84.dp)
                            .clip(MaterialTheme.shapes.small)
                    )
                    Column {
                        Text(query, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${state.filteredResults.size} subtitles found",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalDivider()
            }

            // ── Language filter chips ────────────────────────────────────────
            val languages = state.results.map { it.languageCode }.distinct()
            if (languages.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = state.languageFilter == null,
                            onClick = { viewModel.filterByLanguage(null) },
                            label = { Text("All") }
                        )
                    }
                    items(languages) { lang ->
                        FilterChip(
                            selected = state.languageFilter == lang,
                            onClick = { viewModel.filterByLanguage(lang) },
                            label = { Text(lang.uppercase()) }
                        )
                    }
                }
            }

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.error != null -> Box(Modifier.fillMaxSize().padding(16.dp), Alignment.Center) {
                    Text(state.error!!, color = MaterialTheme.colorScheme.error)
                }
                state.filteredResults.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("No subtitles found")
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.filteredResults, key = { it.fileId }) { result ->
                        SubtitleResultCard(
                            result = result,
                            downloadState = state.downloadStates[result.fileId] ?: DownloadState.IDLE,
                            downloadError = state.downloadErrors[result.fileId],
                            onDownload = {
                                viewModel.downloadAndSave(result.fileId, result.languageCode, result.fileName)
                            },
                            onTranslate = {
                                onTranslate(result.fileId, result.fileName, result.languageCode)
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitleResultCard(
    result: SubtitleSearchResult,
    downloadState: DownloadState,
    downloadError: String?,
    onDownload: () -> Unit,
    onTranslate: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Language + badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = result.languageCode.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (result.isHearingImpaired) {
                        Icon(Icons.Default.Accessibility, "HI", modifier = Modifier.size(16.dp))
                    }
                    if (result.isTrusted) {
                        Icon(Icons.Default.VerifiedUser, "Trusted",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Text(result.fileName, style = MaterialTheme.typography.bodySmall,
                maxLines = 1, overflow = TextOverflow.Ellipsis)

            // Stats row
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Icon(Icons.Default.Download, null, modifier = Modifier.size(12.dp))
                    Text("${result.downloadCount}", style = MaterialTheme.typography.labelSmall)
                }
                result.rating?.let { r ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Icon(Icons.Default.Star, null, modifier = Modifier.size(12.dp))
                        Text("${"%.1f".format(r)}", style = MaterialTheme.typography.labelSmall)
                    }
                }
                result.uploadedAt?.let {
                    Text(it.take(10), style = MaterialTheme.typography.labelSmall)
                }
                result.uploaderName?.let {
                    Text("by $it", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            // ── Action buttons ───────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Download button
                OutlinedButton(
                    onClick = onDownload,
                    enabled = downloadState == DownloadState.IDLE || downloadState == DownloadState.ERROR,
                    modifier = Modifier.weight(1f)
                ) {
                    when (downloadState) {
                        DownloadState.DOWNLOADING -> {
                            CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(6.dp))
                            Text("Saving…")
                        }
                        DownloadState.DONE -> {
                            Icon(Icons.Default.CheckCircle, null, Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Saved")
                        }
                        DownloadState.ERROR -> {
                            Icon(Icons.Default.Error, null, Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Retry")
                        }
                        else -> {
                            Icon(Icons.Default.Download, null, Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Download")
                        }
                    }
                }

                // Translate button
                Button(
                    onClick = onTranslate,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Translate, null, Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Translate")
                }
            }

            // Download error message
            downloadError?.let { err ->
                Text(err, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
