package dev.monday.domain.briefing

import dev.monday.data.ai.AiRouter
import dev.monday.data.repository.NotificationRepository
import dev.monday.data.repository.ReminderRepository
import dev.monday.domain.context.ContextEngine
import dev.monday.domain.prompt.PromptRegistry
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Daily Briefing Generator.
 *
 * Assembles a briefing from local data and optionally enhances it
 * with AI. Degrades gracefully to template-based summaries when
 * AI is unavailable.
 */
@Singleton
class BriefingGenerator @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val reminderRepository: ReminderRepository,
    private val contextEngine: ContextEngine,
    private val aiRouter: AiRouter,
    private val promptRegistry: PromptRegistry
) {
    /**
     * Generate a daily briefing.
     * Tries AI first, falls back to template.
     */
    suspend fun generate(): BriefingResult {
        val data = gatherData()

        return if (aiRouter.isAiAvailable()) {
            try {
                generateAiBriefing(data)
            } catch (e: Exception) {
                // Graceful degradation
                generateTemplateBriefing(data)
            }
        } else {
            generateTemplateBriefing(data)
        }
    }

    private suspend fun gatherData(): BriefingData {
        val unreadCount = notificationRepository.getUnreadCount(0.5f).first()
        val todayReminders = reminderRepository.getForToday().first()
        val overdueReminders = reminderRepository.getOverdue().first()
        val nextReminder = reminderRepository.getNext()
        val importantNotifications = notificationRepository.getByMinScore(0.7f).first().take(5)

        return BriefingData(
            unreadImportantCount = unreadCount,
            todayReminderCount = todayReminders.size,
            overdueCount = overdueReminders.size,
            todayReminders = todayReminders.map { it.title },
            overdueItems = overdueReminders.map { it.title },
            nextReminderTitle = nextReminder?.title,
            nextReminderTime = nextReminder?.triggerTime?.let {
                Instant.ofEpochMilli(it)
                    .atZone(ZoneId.systemDefault())
                    .toLocalTime()
                    .format(DateTimeFormatter.ofPattern("h:mm a"))
            },
            importantNotifications = importantNotifications.map {
                "${it.title ?: "Notification"}: ${it.text ?: ""}"
            }
        )
    }

    private suspend fun generateAiBriefing(data: BriefingData): BriefingResult {
        val briefingDataStr = buildString {
            appendLine("Unread important notifications: ${data.unreadImportantCount}")
            appendLine("Today's reminders (${data.todayReminderCount}):")
            data.todayReminders.forEach { appendLine("  - $it") }
            if (data.overdueCount > 0) {
                appendLine("Overdue items (${data.overdueCount}):")
                data.overdueItems.forEach { appendLine("  - $it") }
            }
            if (data.nextReminderTitle != null) {
                appendLine("Next up: ${data.nextReminderTitle} at ${data.nextReminderTime}")
            }
            if (data.importantNotifications.isNotEmpty()) {
                appendLine("Important notifications:")
                data.importantNotifications.forEach { appendLine("  - $it") }
            }
        }

        val prompt = promptRegistry.build("daily_briefing", mapOf(
            "briefing_data" to briefingDataStr
        ))

        val response = aiRouter.route(prompt)
        return BriefingResult(
            text = response.text,
            isAiGenerated = true,
            data = data
        )
    }

    private fun generateTemplateBriefing(data: BriefingData): BriefingResult {
        val text = buildString {
            // Greeting is handled by the UI based on time of day

            when {
                data.overdueCount > 0 && data.unreadImportantCount > 0 -> {
                    append("You have ${data.overdueCount} overdue item${if (data.overdueCount > 1) "s" else ""}")
                    append(" and ${data.unreadImportantCount} important notification${if (data.unreadImportantCount > 1) "s" else ""}. ")
                }
                data.overdueCount > 0 -> {
                    append("You have ${data.overdueCount} overdue item${if (data.overdueCount > 1) "s" else ""} that need${if (data.overdueCount == 1) "s" else ""} attention. ")
                }
                data.unreadImportantCount > 0 -> {
                    append("You have ${data.unreadImportantCount} important notification${if (data.unreadImportantCount > 1) "s" else ""}. ")
                }
            }

            if (data.todayReminderCount > 0) {
                append("${data.todayReminderCount} reminder${if (data.todayReminderCount > 1) "s" else ""} today. ")
            }

            if (data.nextReminderTitle != null) {
                append("Next up: ${data.nextReminderTitle} at ${data.nextReminderTime}. ")
            }

            if (isEmpty()) {
                append("Nothing urgent right now. You're all caught up.")
            }
        }

        return BriefingResult(
            text = text.trim(),
            isAiGenerated = false,
            data = data
        )
    }
}

data class BriefingData(
    val unreadImportantCount: Int,
    val todayReminderCount: Int,
    val overdueCount: Int,
    val todayReminders: List<String>,
    val overdueItems: List<String>,
    val nextReminderTitle: String?,
    val nextReminderTime: String?,
    val importantNotifications: List<String>
)

data class BriefingResult(
    val text: String,
    val isAiGenerated: Boolean,
    val data: BriefingData
)
