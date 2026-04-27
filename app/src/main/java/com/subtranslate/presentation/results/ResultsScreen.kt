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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.subtranslate.domain.model.SubtitleSearchResult
import com.subtranslate.presentation.theme.*
import java.net.URLEncoder

@Composable
fun ResultsScreen(
    query: String,
    onTranslate: (fileId: Int, fileName: String, languageCode: String) -> Unit,
    onBack: () -> Unit,
    viewModel: ResultsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(query) { viewModel.search(query) }

    val snackbarHostState = remember { SnackbarHostState() }
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
            SubtyTopBar(title = query, onBack = onBack)

            // ── Movie header ──────────────────────────────────────────────────────
            state.posterUrl?.let { url ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 44.dp, height = 64.dp)
                            .border(1.dp, SubtyBorder)
                            .background(SubtyBg3),
                    ) {
                        AsyncImage(
                            model = url,
                            contentDescription = query,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Column {
                        SubtyText(
                            query,
                            fontSize = 16,
                            weight = FontWeight.ExtraBold,
                            color = SubtyText1,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(4.dp))
                        SubtyText(
                            "${state.filteredResults.size} subtitles found",
                            fontSize = 10,
                            color = SubtyText3,
                            uppercase = true,
                            letterSpacing = 0.04f,
                        )
                    }
                }
                SubtyDivider()
            }

            // ── Language filter bar ───────────────────────────────────────────────
            val languages = state.results.map { it.languageCode }.distinct().sorted()
            if (languages.isNotEmpty()) {
                var langMenuExpanded by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SubtyBg),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Scrollable language pills
                    LazyRow(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(start = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        item {
                            SubtyLangChip(
                                "All",
                                selected = state.languageFilter == null,
                                onClick = { viewModel.filterByLanguage(null) },
                            )
                        }
                        items(languages) { lang ->
                            SubtyLangChip(
                                lang.uppercase(),
                                selected = state.languageFilter == lang,
                                onClick = { viewModel.filterByLanguage(lang) },
                            )
                        }
                    }

                    // Dropdown search button
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
                            modifier = Modifier.size(22.dp),
                        )
                        DropdownMenu(
                            expanded = langMenuExpanded,
                            onDismissRequest = { langMenuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { SubtyText("All", fontSize = 13, color = SubtyText1) },
                                onClick = { viewModel.filterByLanguage(null); langMenuExpanded = false },
                            )
                            languages.forEach { lang ->
                                DropdownMenuItem(
                                    text = { SubtyText(lang.uppercase(), fontSize = 13, color = SubtyText1) },
                                    onClick = { viewModel.filterByLanguage(lang); langMenuExpanded = false },
                                )
                            }
                        }
                    }
                }
                SubtyDivider()
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
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.filteredResults, key = { it.fileId }) { result ->
                            SubtitleResultRow(
                                result = result,
                                downloadState = state.downloadStates[result.fileId] ?: DownloadState.IDLE,
                                downloadError = state.downloadErrors[result.fileId],
                                onDownload = {
                                    viewModel.downloadAndSave(result.fileId, result.languageCode, result.fileName)
                                },
                                onTranslate = {
                                    onTranslate(result.fileId, result.fileName, result.languageCode)
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
    onDownload: () -> Unit,
    onTranslate: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SubtyBg),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Language + HI badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SubtyText(
                    result.languageCode.uppercase(),
                    fontSize = 11,
                    weight = FontWeight.ExtraBold,
                    color = SubtyMocha,
                    letterSpacing = 0.1f,
                )
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
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SubtyText(
                    "↓ ${result.downloadCount}",
                    fontSize = 10,
                    color = SubtyText3,
                )
                result.rating?.let { r ->
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
            ) {
                SubtyButton(
                    text = when (downloadState) {
                        DownloadState.DOWNLOADING -> "Saving…"
                        DownloadState.DONE -> "Saved ✓"
                        DownloadState.ERROR -> "Retry"
                        else -> "Download"
                    },
                    onClick = onDownload,
                    style = SubtyButtonStyle.OUTLINE,
                    enabled = downloadState == DownloadState.IDLE || downloadState == DownloadState.ERROR,
                    loading = downloadState == DownloadState.DOWNLOADING,
                    small = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width((-1).dp))
                SubtyButton(
                    text = "Translate",
                    onClick = onTranslate,
                    style = SubtyButtonStyle.MOCHA,
                    small = true,
                    modifier = Modifier.weight(1f),
                )
            }

            downloadError?.let { err ->
                SubtyText(err, fontSize = 10, color = SubtyError)
            }
        }
        SubtyDivider()
    }
}
