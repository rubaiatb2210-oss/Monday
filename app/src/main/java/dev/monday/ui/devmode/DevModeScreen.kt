package dev.monday.ui.devmode

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.monday.data.database.entity.CommandHistoryEntity
import dev.monday.data.repository.MetricsSummary
import dev.monday.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun DevModeScreen(
    onNavigateBack: () -> Unit,
    viewModel: DevModeViewModel = hiltViewModel()
) {
    val recentCommands by viewModel.recentCommands.collectAsState()
    val metrics by viewModel.metrics.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

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
                text = "Developer Mode",
                style = MaterialTheme.typography.headlineMedium,
                color = MondayTextPrimary,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { viewModel.refreshMetrics() }) {
                Icon(Icons.Filled.Refresh, "Refresh", tint = MondayTextSecondary)
            }
        }

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MondayNavyLight,
            contentColor = MondayBlue,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = MondayBlue
                )
            }
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text("Metrics", modifier = Modifier.padding(12.dp),
                    color = if (selectedTab == 0) MondayBlue else MondayTextTertiary)
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Text("Pipeline", modifier = Modifier.padding(12.dp),
                    color = if (selectedTab == 1) MondayBlue else MondayTextTertiary)
            }
        }

        when (selectedTab) {
            0 -> MetricsTab(metrics)
            1 -> PipelineTab(recentCommands)
        }
    }
}

@Composable
private fun MetricsTab(metrics: MetricsSummary?) {
    if (metrics == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MondayBlue)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Today's Activity",
                style = MaterialTheme.typography.titleSmall,
                color = MondayBlue,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard("Notifications", "${metrics.notificationsToday}", MondayAmber, Modifier.weight(1f))
                MetricCard("AI Calls", "${metrics.aiCallsToday}", MondayPurple, Modifier.weight(1f))
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard("Commands", "${metrics.commandsToday}", MondayBlue, Modifier.weight(1f))
                MetricCard("Avg Response", "${metrics.avgResponseTimeMs.toInt()}ms", MondayGreen, Modifier.weight(1f))
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard("Rule Hits", "${metrics.ruleHits}", MondayTextSecondary, Modifier.weight(1f))
                MetricCard("AI Hits", "${metrics.aiHits}", MondayTextSecondary, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MondayNavySurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge,
                color = color
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MondayTextTertiary
            )
        }
    }
}

@Composable
private fun PipelineTab(commands: List<CommandHistoryEntity>) {
    if (commands.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No pipeline traces yet", color = MondayTextTertiary)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(commands) { command ->
            PipelineTraceItem(command)
        }
    }
}

@Composable
private fun PipelineTraceItem(command: CommandHistoryEntity) {
    val timeStr = remember(command.timestamp) {
        Instant.ofEpochMilli(command.timestamp)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("HH:mm:ss"))
    }
    val statusColor = when (command.resultStatus) {
        "SUCCESS", "SPOKEN" -> MondayGreen
        "FAILURE" -> MondayRed
        else -> MondayAmber
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MondayNavySurface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    ),
                    color = MondayTextTertiary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = statusColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = command.resultStatus,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${command.durationMs}ms",
                        style = MaterialTheme.typography.labelSmall,
                        color = MondayTextMuted
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = command.inputText,
                style = MaterialTheme.typography.bodyMedium,
                color = MondayTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (command.resolvedSkillId != null) {
                Text(
                    text = "→ ${command.resolvedSkillId} (${command.inputSource})",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    ),
                    color = MondayBlue
                )
            }
            if (command.resultMessage != null) {
                Text(
                    text = command.resultMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MondayTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
