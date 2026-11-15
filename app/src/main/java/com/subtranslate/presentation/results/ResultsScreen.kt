package com.subtranslate.presentation.results

import android.app.DownloadManager
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.GTranslate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.subtranslate.domain.model.SubtitleSearchResult
import com.subtranslate.domain.model.SubtitleSource
import kotlin.math.min
import com.subtranslate.presentation.theme.*
import java.net.URLEncoder
import kotlinx.coroutines.launch

@Composable
fun ResultsScreen(
    query: String,
    onTranslate: (fileId: Int, fileName: String, languageCode: String) -> Unit,
    onBack: () -> Unit,
    translateEnabled: Boolean = true,
    maintenanceMessage: String = "",
    viewModel: ResultsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(query) { viewModel.search(query) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // observe download events
    LaunchedEffect(Unit) {
        viewModel.downloadDoneEvents.collect { savedName ->
            val result = snackbarHostState.showSnackbar(
                message = "Saved: $savedName",
                actionLabel = "Open Folder",
            )
            if (result == SnackbarResult.ActionPerformed) {
                // Open Downloads folder — try document tree URI first, fallback to generic
                val docUri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload")
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(docUri, "vnd.android.document/directory")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                runCatching { context.startActivity(intent) }.onFailure {
                    // fallback
                    runCatching {
                        context.startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                    }
                }
            }
        }
    }

    Scaffold(
        containerColor = SubtyBg,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = SubtyBg2,
                    contentColor = SubtyText1,
                    actionColor = SubtyMocha,
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SubtyBg)
                .padding(bottom = innerPadding.calculateBottomPadding()),
        ) {
            SubtyTopBar(
                title = if (state.isSeries) query else "Subtitles",
                onBack = onBack,
                showDivider = false,
            )

            ResultsContextHeader(
                title = query,
                state = state,
                onLanguageSelected = viewModel::filterByLanguage,
            )

            if (state.isSeries) {
                SeriesEpisodeSearchControls(
                    state = state,
                    onToggleExpanded = viewModel::toggleSeasonEpisodeExpanded,
                    onSeasonSelected = viewModel::setSeason,
                    onEpisodeSelected = viewModel::setEpisode,
                    onRefresh = { viewModel.applySeasonEpisode(query) },
                )
            }

            // ── Content ───────────────────────────────────────────────────────────
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = SubtyMocha, strokeWidth = 1.5.dp)
                }

                state.error != null -> Box(
                    Modifier.fillMaxSize().padding(24.dp), Alignment.Center
                ) {
                    SubtyErrorBanner(state.error!!)
                }

                state.filteredResults.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    androidx.compose.foundation.layout.Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SubtyText("No subtitles found", color = SubtyText3)
                        SubtyText("Try a different language filter or search online below",
                                  color = SubtyText3, fontSize = 12)
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                    ) {
                        items(state.filteredResults, key = { it.fileId }) { result ->
                            SubtitleResultRow(
                                result = result,
                                downloadState = state.downloadStates[result.fileId] ?: DownloadState.IDLE,
                                downloadError = state.downloadErrors[result.fileId],
                                translateEnabled = translateEnabled,
                                onDownload = {
                                    viewModel.downloadAndSave(result.fileId, result.languageCode, result.fileName)
                                },
                                onTranslate = {
                                    viewModel.trackTranslateClick(result.fileId, result.languageCode)
                                    onTranslate(result.fileId, result.fileName, result.languageCode)
                                },
                                onTranslateUnavailable = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Translation is currently unavailable")
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SubtitleResultRow(
    result: SubtitleSearchResult,
    downloadState: DownloadState,
    downloadError: String?,
    translateEnabled: Boolean,
    onDownload: () -> Unit,
    onTranslate: () -> Unit,
    onTranslateUnavailable: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SubtyBg),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            // Language + HI badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SubtyText(
                        result.languageCode.uppercase(),
                        fontSize = 11,
                        weight = FontWeight.ExtraBold,
                        color = SubtyMocha,
                        letterSpacing = 0.1f,
                    )
                    SourcePill(result.source)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (result.isHearingImpaired) {
                        Box(
                            modifier = Modifier
                                .border(1.dp, SubtyBorderDim)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            SubtyText("HI", fontSize = 8, weight = FontWeight.Bold, color = SubtyText3)
                        }
                    }
                    if (result.isTrusted) {
                        Box(
                            modifier = Modifier
                                .border(1.dp, SubtyMocha)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            SubtyText("✓", fontSize = 8, weight = FontWeight.Bold, color = SubtyMocha)
                        }
                    }
                }
            }

            // File name
            SubtyText(
                result.fileName,
                fontSize = 12,
                color = SubtyText2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            // Stats
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SubtitleStat(
                    icon = Icons.Default.CloudDownload,
                    text = result.downloadCount.toString(),
                    contentDescription = "Downloads",
                )
                result.rating?.takeIf { it > 0.0 }?.let { r ->
                    SubtyText(
                        "★ ${"%.1f".format(r)}",
                        fontSize = 10,
                        color = SubtyText3,
                    )
                }
                result.uploadedAt?.let {
                    SubtyText(it.take(10), fontSize = 10, color = SubtyText3)
                }
                result.uploaderName?.let {
                    SubtyText(
                        "by $it", fontSize = 10, color = SubtyText3,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SubtyButton(
                    text = when (downloadState) {
                        DownloadState.DOWNLOADING -> "Saving…"
                        DownloadState.DONE -> "Saved ✓"
                        DownloadState.ERROR -> "Retry"
                        else -> "Download"
                    },
                    onClick = onDownload,
                    style = SubtyButtonStyle.FILLED,
                    enabled = downloadState == DownloadState.IDLE || downloadState == DownloadState.ERROR,
                    loading = downloadState == DownloadState.DOWNLOADING,
                    small = true,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .size(38.dp)
                        .border(1.dp, SubtyBorder)
                        .alpha(if (translateEnabled) 1f else 0.55f)
                        .clickable {
                            if (translateEnabled) onTranslate() else onTranslateUnavailable()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.GTranslate,
                        contentDescription = "Translate",
                        tint = SubtyText2,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            downloadError?.let { err ->
                SubtyText(err, fontSize = 10, color = SubtyError)
            }
        }
        SubtyDivider()
    }
}

@Composable
private fun SourcePill(source: SubtitleSource) {
    val borderColor = when (source) {
        SubtitleSource.OPEN_SUBTITLES -> SubtyMocha.copy(alpha = 0.55f)
        SubtitleSource.SUBDL -> SubtyWhite.copy(alpha = 0.24f)
        SubtitleSource.SCREWZIRA -> Color(0xFF4E8B6A)
    }
    val textColor = when (source) {
        SubtitleSource.OPEN_SUBTITLES -> SubtyMocha
        SubtitleSource.SUBDL -> SubtyText2
        SubtitleSource.SCREWZIRA -> Color(0xFF8FD0AA)
    }

    Box(
        modifier = Modifier
            .border(1.dp, borderColor)
            .padding(horizontal = 7.dp, vertical = 3.dp),
    ) {
        SubtyText(
            text = source.label,
            fontSize = 8,
            weight = FontWeight.Bold,
            color = textColor,
            uppercase = true,
        )
    }
}

@Composable
private fun ResultsContextHeader(
    title: String,
    state: ResultsUiState,
    onLanguageSelected: (String?) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SubtyBg2)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(width = 74.dp, height = 108.dp)
                    .border(1.dp, SubtyBorder)
                    .background(SubtyBg3),
                contentAlignment = Alignment.Center,
            ) {
                if (state.posterUrl != null) {
                    AsyncImage(
                        model = state.posterUrl,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    SubtyText(
                        title.take(1).uppercase(),
                        fontSize = 28,
                        weight = FontWeight.ExtraBold,
                        color = SubtyText3,
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                SubtyText(
                    title,
                    fontSize = 21,
                    weight = FontWeight.ExtraBold,
                    color = SubtyText1,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                if (state.isSeries) {
                    SubtyText(
                        "Season ${state.currentSeason ?: 1}  •  Episode ${state.currentEpisode ?: 1}",
                        fontSize = 11,
                        color = SubtyText3,
                        uppercase = true,
                        letterSpacing = 0.04f,
                    )
                }

                SubtyText(
                    "${state.filteredResults.size} subtitles found",
                    fontSize = 11,
                    color = SubtyText3,
                    uppercase = true,
                    letterSpacing = 0.04f,
                )

                SubtyText(
                    "Searching: ${formatSearchLanguages(state.searchLanguages)}",
                    fontSize = 12,
                    color = SubtyText2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        ResultsLanguageFilterStrip(
            state = state,
            onLanguageSelected = onLanguageSelected,
        )
    }
    SubtyDividerDim()
}

@Composable
private fun ResultsLanguageFilterStrip(
    state: ResultsUiState,
    onLanguageSelected: (String?) -> Unit,
) {
    val languages = state.results.map { it.languageCode }.distinct().sorted()
    if (languages.isEmpty()) {
        SubtyText(
            "Language options will appear when results load",
            fontSize = 11,
            color = SubtyText3,
        )
        return
    }

    var langMenuExpanded by remember { mutableStateOf(false) }
    val showOverflowMenu = languages.size > 5
    val inlineLanguages = if (showOverflowMenu) languages.take(5) else languages
    val menuLanguages = if (showOverflowMenu) languages.drop(5) else emptyList()

    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        SubtyText(
            "Edit result language",
            fontSize = 10,
            color = SubtyText3,
            uppercase = true,
            letterSpacing = 0.05f,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                item {
                    SubtyLangChip(
                        "All",
                        selected = state.languageFilter == null,
                        onClick = { onLanguageSelected(null) },
                    )
                }
                items(inlineLanguages) { lang ->
                    SubtyLangChip(
                        lang.uppercase(),
                        selected = state.languageFilter == lang,
                        onClick = { onLanguageSelected(lang) },
                    )
                }
            }

            if (showOverflowMenu) {
                Box(
                    modifier = Modifier
                        .background(if (langMenuExpanded) SubtyBg3 else SubtyBg)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { langMenuExpanded = true }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "More languages",
                        tint = SubtyText2,
                        modifier = Modifier.size(24.dp),
                    )
                    DropdownMenu(
                        expanded = langMenuExpanded,
                        onDismissRequest = { langMenuExpanded = false },
                    ) {
                        menuLanguages.forEach { lang ->
                            DropdownMenuItem(
                                text = { SubtyText(lang.uppercase(), fontSize = 13, color = SubtyText1) },
                                onClick = {
                                    onLanguageSelected(lang)
                                    langMenuExpanded = false
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubtitleStatText(text: String) {
    SubtyText(text, fontSize = 10, color = SubtyText3)
}

@Composable
private fun SubtitleStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    contentDescription: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = SubtyText3,
            modifier = Modifier.size(13.dp),
        )
        SubtyText(text, fontSize = 10, color = SubtyText3)
    }
}

private fun formatSearchLanguages(value: String?): String {
    return value
        ?.split(",", " ")
        ?.map { it.trim() }
        ?.filter { it.isNotBlank() }
        ?.joinToString(", ") { it.uppercase() }
        ?.ifBlank { null }
        ?: "All subtitle languages"
}

@Composable
private fun SeriesEpisodeSearchControls(
    state: ResultsUiState,
    onToggleExpanded: () -> Unit,
    onSeasonSelected: (Int) -> Unit,
    onEpisodeSelected: (Int) -> Unit,
    onRefresh: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SubtyBg2)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            SubtyText(
                text = "S${state.currentSeason ?: 1} E${state.currentEpisode ?: 1}",
                fontSize = 13,
                weight = FontWeight.ExtraBold,
                color = SubtyText1,
            )
            SubtyButton(
                text = if (state.seasonEpisodeExpanded) "Done" else "Change episode",
                onClick = onToggleExpanded,
                style = SubtyButtonStyle.OUTLINE,
                small = true,
            )
        }

        if (state.seasonEpisodeExpanded) {
            val seasonCount = state.maxSeasons?.takeIf { it > 0 } ?: 12
            val episodeCount = state.maxEpisodes?.takeIf { it > 0 } ?: 24

            SubtyLabel("Season")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                items((1..min(seasonCount, 24)).toList()) { season ->
                    SubtyChip(
                        text = season.toString(),
                        selected = state.currentSeason == season,
                        onClick = { onSeasonSelected(season) },
                    )
                }
            }

            SubtyLabel("Episode")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                items((1..min(episodeCount, 32)).toList()) { episode ->
                    SubtyChip(
                        text = episode.toString(),
                        selected = state.currentEpisode == episode,
                        onClick = { onEpisodeSelected(episode) },
                    )
                }
            }

            SubtyButton(
                text = "Search this episode",
                onClick = onRefresh,
                style = SubtyButtonStyle.FILLED,
                small = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 8.dp),
            )
        }
    }
    SubtyDividerDim()
}
