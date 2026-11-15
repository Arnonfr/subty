package com.subtranslate.presentation.titlebrowser

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
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
    onSelected: (FeatureDto) -> Unit,
    viewModel: TitleBrowserViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(query) { viewModel.load(query) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SubtyBg),
    ) {
        SubtyTopBar(title = "\"$query\"", onBack = onBack)
        SubtyDivider()

        SubtyLabel(
            text = "${state.displayResults.size} titles found",
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        )

        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SubtyMocha, strokeWidth = 1.5.dp)
                }
            }
            state.error != null -> {
                SubtyErrorBanner(
                    state.error!!,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    items(state.displayResults, key = { it.id }) { feature ->
                        TitleBrowserRow(
                            feature = feature,
                            onClick = {
                                viewModel.selectFeature(feature) { onSelected(feature) }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TitleBrowserRow(
    feature: FeatureDto,
    onClick: () -> Unit,
) {
    val title = feature.attributes.title ?: feature.attributes.originalTitle ?: ""
    val year = feature.attributes.year?.toString() ?: ""
    val isTv = feature.type == "tv" || feature.attributes.featureType?.lowercase() == "tvshow"
    val seasons = feature.attributes.seasonsCount
    val posterUrl = feature.attributes.imgUrl

    Column(modifier = Modifier.fillMaxWidth().background(SubtyBg)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                )
                .padding(horizontal = 4.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(width = 40.dp, height = 58.dp)
                    .background(SubtyBg3)
                    .border(1.dp, SubtyBorderDim),
                contentAlignment = Alignment.Center,
            ) {
                if (posterUrl != null) {
                    AsyncImage(
                        model = posterUrl,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    SubtyText(
                        title.take(1).uppercase(),
                        fontSize = 16,
                        weight = FontWeight.Bold,
                        color = SubtyMocha,
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                SubtyText(
                    title,
                    fontSize = 14,
                    weight = FontWeight.SemiBold,
                    color = SubtyText1,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                SubtyText(
                    buildString {
                        if (year.isNotEmpty()) { append(year); append(" · ") }
                        if (isTv) {
                            append("Series")
                            if (seasons != null && seasons > 0) append(" · $seasons seasons")
                        } else {
                            append("Movie")
                        }
                    },
                    fontSize = 12,
                    color = SubtyText3,
                )
            }

            SubtyText(
                if (isTv) "Series" else "Movie",
                fontSize = 11,
                weight = FontWeight.SemiBold,
                color = if (isTv) SubtyAccent else SubtyText3,
            )
        }
        SubtyDividerDim()
    }
}
