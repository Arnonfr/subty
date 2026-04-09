package com.subtranslate.presentation.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.PaddingValues
import androidx.hilt.navigation.compose.hiltViewModel
import com.subtranslate.presentation.theme.*
import com.subtranslate.util.GOOGLE_TRANSLATE_LANGUAGES

private val MODELS = listOf(
    "google"                         to "Google Translate",
    "gemini-3.1-flash-lite-preview"  to "Gemini 3.1 Flash Lite (fastest)",
    "gemini-2.5-flash"               to "Gemini 2.5 Flash (stable)",
)

private val FORMATS = listOf("srt" to "SRT", "vtt" to "VTT", "ass" to "ASS")

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SubtyBg)
            .verticalScroll(rememberScrollState()),
    ) {
        SubtyPageTitle(
            "Settings",
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 4.dp),
        )

        Spacer(Modifier.height(20.dp))

        // ── Translation ───────────────────────────────────────────────────────
        SettingsSectionHeader("Translation", Modifier.padding(horizontal = 24.dp))
        SubtyDivider()
        SubtyLanguagePickerRow(
            label = "Default target language",
            selected = state.defaultTargetLang,
            options = GOOGLE_TRANSLATE_LANGUAGES,
            onSelect = viewModel::onTargetLangChange,
        )
        SubtyDividerDim()
        SettingsDropdown(
            label = "Model",
            selected = state.translationModel,
            options = MODELS,
            onSelect = viewModel::onModelChange,
        )
        SubtyDividerDim()
        SubtyToggleRow(
            label = "Auto-translate on download",
            description = "Start translation immediately after downloading",
            checked = state.autoTranslate,
            onCheckedChange = viewModel::onAutoTranslateChange,
        )
        SubtyDivider()

        Spacer(Modifier.height(24.dp))

        // ── API Keys ──────────────────────────────────────────────────────────
        SettingsSectionHeader("API Keys", Modifier.padding(horizontal = 24.dp))
        SubtyDivider()
        ApiKeyRow(
            label = "Google Translate API Key",
            value = state.googleTranslateApiKey,
            placeholder = "Enter your Google Translate API key",
            onValueChange = viewModel::onGoogleTranslateApiKeyChange,
            getLinkUrl = "https://console.cloud.google.com/apis/library/translate.googleapis.com",
            getLinkLabel = "Get free API key →",
            context = context,
        )
        SubtyDivider()
        ApiKeyRow(
            label = "Gemini API Key",
            value = state.geminiApiKey,
            placeholder = "Enter your Gemini API key",
            onValueChange = viewModel::onGeminiApiKeyChange,
            getLinkUrl = "https://aistudio.google.com/app/apikey",
            getLinkLabel = "Get free API key →",
            context = context,
        )
        SubtyDivider()

        Spacer(Modifier.height(24.dp))

        // ── Save ──────────────────────────────────────────────────────────────
        SettingsSectionHeader("Save", Modifier.padding(horizontal = 24.dp))
        SubtyDivider()
        SettingsDropdown(
            label = "Preferred format",
            selected = state.preferredSaveFormat,
            options = FORMATS,
            onSelect = viewModel::onSaveFormatChange,
        )
        SubtyDividerDim()
        SubtyToggleRow(
            label = "Auto-save translated files",
            description = "Save to Downloads automatically when done",
            checked = state.autoSave,
            onCheckedChange = viewModel::onAutoSaveChange,
        )
        SubtyDivider()

        Spacer(Modifier.height(24.dp))

        // ── Display ───────────────────────────────────────────────────────────
        SettingsSectionHeader("Display", Modifier.padding(horizontal = 24.dp))
        SubtyDivider()
        SubtyToggleRow(
            label = "Dark mode",
            description = "Switch between dark and light appearance",
            checked = state.isDarkTheme,
            onCheckedChange = viewModel::onDarkThemeChange,
        )
        SubtyDividerDim()
        SubtyToggleRow(
            label = "Show movie posters",
            description = "Display cover art in search results",
            checked = state.showPosters,
            onCheckedChange = viewModel::onShowPostersChange,
        )
        SubtyDividerDim()
        SubtyToggleRow(
            label = "Compact results",
            description = "Smaller cards, more results on screen",
            checked = state.compactResults,
            onCheckedChange = viewModel::onCompactResultsChange,
        )
        SubtyDivider()

        Spacer(Modifier.height(28.dp))

        SubtyButton(
            text = "Save",
            onClick = viewModel::save,
            style = SubtyButtonStyle.FILLED,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        )

        if (state.saved) {
            Spacer(Modifier.height(12.dp))
            SubtySuccessBanner(
                "Settings saved",
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsSectionHeader(text: String, modifier: Modifier = Modifier) {
    SubtyText(
        text = text.uppercase(),
        modifier = modifier.padding(bottom = 10.dp),
        fontSize = 11,
        weight = FontWeight.Bold,
        color = SubtyMocha,
    )
}

@Composable
private fun ApiKeyRow(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    getLinkUrl: String,
    getLinkLabel: String,
    context: android.content.Context,
) {
    var showKey by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SubtyText(label, fontSize = 13, weight = FontWeight.Medium, color = SubtyText1)
        SubtyTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                SubtyText(
                    text = if (showKey) "Hide" else "Show",
                    fontSize = 11,
                    weight = FontWeight.Bold,
                    color = SubtyMocha,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { showKey = !showKey },
                )
            },
        )
        SubtyText(
            text = getLinkLabel,
            fontSize = 11,
            weight = FontWeight.Bold,
            color = SubtyMocha,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(getLinkUrl))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(intent) }
            },
        )
    }
}

@Composable
private fun SettingsDropdown(
    label: String,
    selected: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { expanded = true }
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                SubtyLabel(label)
                Spacer(Modifier.height(4.dp))
                SubtyText(
                    options.find { it.first == selected }?.second ?: selected,
                    fontSize = 14,
                    weight = FontWeight.Medium,
                    color = SubtyText1,
                )
            }
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = SubtyText3,
                modifier = Modifier.size(20.dp),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(SubtyBg2)
                .border(1.dp, SubtyBorderDim),
        ) {
            options.forEach { (code, name) ->
                DropdownMenuItem(
                    text = {
                        SubtyText(
                            name,
                            fontSize = 14,
                            color = if (code == selected) SubtyMocha else SubtyText1,
                            weight = if (code == selected) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    onClick = { onSelect(code); expanded = false },
                    modifier = Modifier.background(SubtyBg2),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                )
            }
        }
    }
}
