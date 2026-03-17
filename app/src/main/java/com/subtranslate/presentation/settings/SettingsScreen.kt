package com.subtranslate.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.titleLarge)

        // OpenSubtitles section
        Card {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("OpenSubtitles", style = MaterialTheme.typography.titleMedium)
                SecretField(
                    label = "API Key",
                    value = state.osApiKey,
                    onValueChange = viewModel::onOsApiKeyChange
                )
                OutlinedTextField(
                    value = state.osUsername,
                    onValueChange = viewModel::onOsUsernameChange,
                    label = { Text("Username (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                SecretField(
                    label = "Password (optional)",
                    value = state.osPassword,
                    onValueChange = viewModel::onOsPasswordChange
                )
            }
        }

        // Anthropic section
        Card {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Anthropic / Claude", style = MaterialTheme.typography.titleMedium)
                SecretField(
                    label = "API Key",
                    value = state.anthropicKey,
                    onValueChange = viewModel::onAnthropicKeyChange
                )
            }
        }

        // Defaults section
        Card {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Defaults", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = state.defaultSourceLang,
                    onValueChange = viewModel::onSourceLangChange,
                    label = { Text("Source language code (e.g. en)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.defaultTargetLang,
                    onValueChange = viewModel::onTargetLangChange,
                    label = { Text("Target language code (e.g. he)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        Button(
            onClick = viewModel::save,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Settings")
        }

        if (state.saved) {
            Text("Saved!", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun SecretField(label: String, value: String, onValueChange: (String) -> Unit) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
            }
        }
    )
}
