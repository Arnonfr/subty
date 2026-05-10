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
    onSelected: () -> Unit,
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
            text = "${state.results.size} titles found",
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
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy((-1).dp),
                ) {
                    items(state.results, key = { it.id }) { feature ->
                        TitleBrowserRow(
                            feature = feature,
                            onClick = {
                                viewModel.selectFeature(feature)
                                onSelected()
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
        // Poster thumbnail
        Box(
            modifier = Modifier
                .size(width = 48.dp, height = 68.dp)
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
                    fontSize = 18,
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
            Spacer(Modifier.height(4.dp))
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

        // Type badge
        Box(
            modifier = Modifier
                .border(1.dp, if (isTv) SubtyMocha else SubtyBorderDim)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            SubtyText(
                if (isTv) "TV" else "Film",
                fontSize = 11,
                weight = FontWeight.SemiBold,
                color = if (isTv) SubtyMocha else SubtyText3,
            )
        }
    }
}
