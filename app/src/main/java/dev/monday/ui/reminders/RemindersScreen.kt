package dev.monday.ui.reminders

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.monday.data.database.entity.ReminderEntity
import dev.monday.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun RemindersScreen(
    viewModel: RemindersViewModel = hiltViewModel()
) {
    val activeReminders by viewModel.activeReminders.collectAsState()
    val overdueReminders by viewModel.overdueReminders.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MondayNavy)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 12.dp, top = 60.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Reminders",
                style = MaterialTheme.typography.displayMedium,
                color = MondayTextPrimary
            )
        }

        if (activeReminders.isEmpty() && overdueReminders.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MondayGreen.copy(alpha = 0.5f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No active reminders",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MondayTextTertiary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Say \"remind me…\" to create one",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MondayTextMuted
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Overdue section
                if (overdueReminders.isNotEmpty()) {
                    item {
                        Text(
                            text = "Overdue",
                            style = MaterialTheme.typography.titleSmall,
                            color = MondayRed,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                        )
                    }
                    items(overdueReminders) { reminder ->
                        ReminderItem(
                            reminder = reminder,
                            isOverdue = true,
                            onComplete = { viewModel.complete(reminder.id) },
                            onDelete = { viewModel.delete(reminder) }
                        )
                    }
                }

                // Active section
                if (activeReminders.isNotEmpty()) {
                    item {
                        Text(
                            text = "Upcoming",
                            style = MaterialTheme.typography.titleSmall,
                            color = MondayBlue,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                        )
                    }
                    items(activeReminders) { reminder ->
                        ReminderItem(
                            reminder = reminder,
                            isOverdue = false,
                            onComplete = { viewModel.complete(reminder.id) },
                            onDelete = { viewModel.delete(reminder) }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun ReminderItem(
    reminder: ReminderEntity,
    isOverdue: Boolean,
    onComplete: () -> Unit,
    onDelete: () -> Unit
) {
    val timeStr = remember(reminder.triggerTime) {
        Instant.ofEpochMilli(reminder.triggerTime)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("MMM d, h:mm a"))
    }

    val accentColor = if (isOverdue) MondayRed else MondayBlue

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MondayNavySurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox
            IconButton(
                onClick = onComplete,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.RadioButtonUnchecked,
                    contentDescription = "Complete",
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MondayTextPrimary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = timeStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = accentColor
                    )
                    if (isOverdue) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MondayRed.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "OVERDUE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MondayRed,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Delete",
                    tint = MondayTextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
