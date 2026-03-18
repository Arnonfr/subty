package com.subtranslate.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.subtranslate.domain.model.HistoryItem
import com.subtranslate.presentation.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
    val items by viewModel.history.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SubtyBg),
    ) {
        SubtyPageTitle(
            "History",
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 4.dp),
        )
        SubtyLabel(
            "Your downloaded subtitles",
            modifier = Modifier.padding(start = 24.dp, bottom = 20.dp),
        )

        if (items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                SubtyText("No downloads yet", color = SubtyText3)
            }
        } else {
            SubtyDivider()
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(items, key = { it.id }) { item ->
                    HistoryRow(item = item, onDelete = { viewModel.delete(item.id) })
                    SubtyDividerDim()
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(item: HistoryItem, onDelete: () -> Unit) {
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SubtyBg)
            .padding(start = 24.dp, top = 14.dp, bottom = 14.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            SubtyText(
                item.movieTitle,
                fontSize = 14,
                weight = FontWeight.SemiBold,
                color = SubtyText1,
            )
            SubtyText(
                buildString {
                    append(item.originalLanguage.uppercase())
                    item.translatedLanguage?.let { append(" → ${it.uppercase()}") }
                    append("  ·  ${item.format.uppercase()}")
                },
                fontSize = 11,
                color = SubtyMocha,
                letterSpacing = 0.04f,
            )
            SubtyText(
                sdf.format(Date(item.downloadedAt)),
                fontSize = 10,
                color = SubtyText3,
            )
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
