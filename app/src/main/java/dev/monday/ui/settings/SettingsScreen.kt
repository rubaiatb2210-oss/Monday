package dev.monday.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.monday.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDevMode: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val isDeveloperMode by viewModel.isDeveloperMode.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    val context = LocalContext.current

    var showApiKeyDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MondayNavy)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 12.dp, top = 52.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Filled.ArrowBack, "Back", tint = MondayTextSecondary)
            }
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineLarge,
                color = MondayTextPrimary
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // AI Provider section
            item {
                SectionHeader("AI Provider")
            }
            item {
                SettingsItem(
                    icon = Icons.Filled.Key,
                    title = "Gemini API Key",
                    subtitle = if (apiKey.isNotBlank()) "Configured ✓" else "Not configured",
                    subtitleColor = if (apiKey.isNotBlank()) MondayGreen else MondayAmber,
                    onClick = { showApiKeyDialog = true }
                )
            }

            // Notifications section
            item {
                SectionHeader("Notifications")
            }
            item {
                SettingsItem(
                    icon = Icons.Filled.Notifications,
                    title = "Notification Access",
                    subtitle = "Required for timeline and filtering",
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                )
            }

            // Developer section
            item {
                SectionHeader("Developer")
            }
            item {
                SettingsSwitch(
                    icon = Icons.Filled.Code,
                    title = "Developer Mode",
                    subtitle = "Show event logs, metrics, and pipeline traces",
                    checked = isDeveloperMode,
                    onCheckedChange = { viewModel.toggleDeveloperMode() }
                )
            }
            if (isDeveloperMode) {
                item {
                    SettingsItem(
                        icon = Icons.Filled.BugReport,
                        title = "Open Developer Console",
                        subtitle = "View events, metrics, and AI decisions",
                        onClick = onNavigateToDevMode
                    )
                }
            }

            // About
            item {
                SectionHeader("About")
            }
            item {
                SettingsItem(
                    icon = Icons.Filled.Info,
                    title = "Monday v1.0.0-alpha01",
                    subtitle = "Your Personal Executive Assistant",
                    onClick = {}
                )
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    // API Key Dialog
    if (showApiKeyDialog) {
        ApiKeyDialog(
            currentKey = apiKey,
            onDismiss = { showApiKeyDialog = false },
            onSave = { key ->
                viewModel.saveApiKey(key)
                showApiKeyDialog = false
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MondayBlue,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    subtitleColor: androidx.compose.ui.graphics.Color = MondayTextTertiary,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MondayNavySurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MondayTextSecondary, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = MondayTextPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = subtitleColor)
            }
            Icon(
                Icons.Filled.ChevronRight, null,
                tint = MondayTextMuted, modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SettingsSwitch(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MondayNavySurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MondayTextSecondary, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = MondayTextPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MondayTextTertiary)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MondayBlue,
                    checkedTrackColor = MondayBlue.copy(alpha = 0.3f),
                    uncheckedThumbColor = MondayTextMuted,
                    uncheckedTrackColor = MondayNavyElevated
                )
            )
        }
    }
}

@Composable
private fun ApiKeyDialog(
    currentKey: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var keyText by remember { mutableStateOf(currentKey) }
    var showKey by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MondayNavyCard,
        title = {
            Text("Gemini API Key", color = MondayTextPrimary)
        },
        text = {
            Column {
                Text(
                    "Enter your Gemini API key for AI features.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MondayTextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = keyText,
                    onValueChange = { keyText = it },
                    label = { Text("API Key") },
                    singleLine = true,
                    visualTransformation = if (showKey) VisualTransformation.None
                        else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(
                                if (showKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = "Toggle visibility",
                                tint = MondayTextTertiary
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MondayBlue,
                        unfocusedBorderColor = MondayNavyElevated,
                        cursorColor = MondayBlue,
                        focusedTextColor = MondayTextPrimary,
                        unfocusedTextColor = MondayTextPrimary,
                        focusedLabelColor = MondayBlue,
                        unfocusedLabelColor = MondayTextTertiary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(keyText) }) {
                Text("Save", color = MondayBlue)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MondayTextTertiary)
            }
        }
    )
}
