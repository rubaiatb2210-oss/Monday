package dev.monday.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.monday.data.database.entity.ReminderEntity
import dev.monday.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val briefing by viewModel.briefing.collectAsState()
    val isLoadingBriefing by viewModel.isLoadingBriefing.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()
    val upcomingReminders by viewModel.upcomingReminders.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MondayNavy),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // Header
        item {
            DashboardHeader(
                greeting = viewModel.greeting,
                onSettingsClick = onNavigateToSettings
            )
        }

        // Briefing Card
        item {
            BriefingCard(
                briefingText = briefing?.text,
                isAiGenerated = briefing?.isAiGenerated ?: false,
                isLoading = isLoadingBriefing,
                onRefresh = { viewModel.refreshBriefing() }
            )
        }

        // Quick Stats
        item {
            QuickStatsRow(
                unreadCount = unreadCount,
                reminderCount = upcomingReminders.size
            )
        }

        // Upcoming Reminders
        if (upcomingReminders.isNotEmpty()) {
            item {
                Text(
                    text = "Upcoming",
                    style = MaterialTheme.typography.titleMedium,
                    color = MondayTextPrimary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }

            items(upcomingReminders) { reminder ->
                ReminderCard(reminder = reminder)
            }
        }
    }
}

@Composable
private fun DashboardHeader(
    greeting: String,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, top = 60.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = greeting,
                style = MaterialTheme.typography.displayMedium,
                color = MondayTextPrimary
            )
            Text(
                text = "Here's your briefing",
                style = MaterialTheme.typography.bodyLarge,
                color = MondayTextSecondary
            )
        }
        IconButton(onClick = onSettingsClick) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = "Settings",
                tint = MondayTextSecondary
            )
        }
    }
}

@Composable
private fun BriefingCard(
    briefingText: String?,
    isAiGenerated: Boolean,
    isLoading: Boolean,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MondayNavyCard)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MondayBlue.copy(alpha = 0.08f),
                            MondayPurple.copy(alpha = 0.05f)
                        )
                    )
                )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = MondayBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Daily Briefing",
                            style = MaterialTheme.typography.titleSmall,
                            color = MondayBlue
                        )
                        if (isAiGenerated) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MondayBlue.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "AI",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MondayBlueBright,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.size(32.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MondayBlue
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Refresh",
                                tint = MondayTextTertiary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = briefingText ?: "Loading your briefing…",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (briefingText != null) MondayTextPrimary else MondayTextTertiary
                )
            }
        }
    }
}

@Composable
private fun QuickStatsRow(unreadCount: Int, reminderCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatChip(
            icon = Icons.Filled.Notifications,
            label = "$unreadCount unread",
            color = if (unreadCount > 0) MondayAmber else MondayTextTertiary,
            modifier = Modifier.weight(1f)
        )
        StatChip(
            icon = Icons.Filled.Schedule,
            label = "$reminderCount reminders",
            color = if (reminderCount > 0) MondayBlue else MondayTextTertiary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MondayNavySurface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = color
            )
        }
    }
}

@Composable
private fun ReminderCard(reminder: ReminderEntity) {
    val timeStr = remember(reminder.triggerTime) {
        Instant.ofEpochMilli(reminder.triggerTime)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
            .format(DateTimeFormatter.ofPattern("h:mm a"))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MondayNavySurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MondayBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.NotificationsActive,
                    contentDescription = null,
                    tint = MondayBlue,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MondayTextPrimary
                )
                if (reminder.description != null) {
                    Text(
                        text = reminder.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MondayTextTertiary,
                        maxLines = 1
                    )
                }
            }

            Text(
                text = timeStr,
                style = MaterialTheme.typography.labelMedium,
                color = MondayBlue
            )
        }
    }
}
