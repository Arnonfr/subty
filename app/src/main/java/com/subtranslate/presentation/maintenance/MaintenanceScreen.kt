package com.subtranslate.presentation.maintenance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.subtranslate.presentation.theme.*

@Composable
fun MaintenanceScreen(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SubtyBg)
            .padding(40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SubtyText(
            text = "⏳",
            fontSize = 48,
        )
        Spacer(Modifier.height(32.dp))
        SubtyPageTitle(
            text = "Be right back",
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        SubtyText(
            text = message,
            fontSize = 14,
            color = SubtyText2,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
