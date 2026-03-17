package com.subtranslate.presentation.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.subtranslate.domain.model.HistoryItem
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
    val items by viewModel.history.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "History",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp)
        )

        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("No downloads yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items, key = { it.id }) { item ->
                    HistoryItemCard(item = item, onDelete = { viewModel.delete(item.id) })
                }
            }
        }
    }
}

@Composable
fun HistoryItemCard(item: HistoryItem, onDelete: () -> Unit) {
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.movieTitle, style = MaterialTheme.typography.titleMedium)
                Text(
                    buildString {
                        append(item.originalLanguage.uppercase())
                        item.translatedLanguage?.let { append(" → ${it.uppercase()}") }
                        append(" · ${item.format.uppercase()}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    sdf.format(Date(item.downloadedAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
