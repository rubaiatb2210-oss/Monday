package dev.monday.domain.skill

import dev.monday.core.model.ContextSnapshot
import dev.monday.data.repository.ReminderRepository
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Skill: Create reminders.
 *
 * Handles: "remind me in 30 minutes", "remind me at 5 PM to call mom",
 *          "don't forget to submit the assignment"
 */
class ReminderSkill @Inject constructor(
    private val reminderRepository: ReminderRepository
) : Skill {
    override val id = "reminder"
    override val name = "Reminder"
    override val description = "Create and manage reminders"

    private val triggerPatterns = listOf(
        Regex("""(?:remind\s+me|don'?t\s+forget|reminder)""", RegexOption.IGNORE_CASE)
    )

    private val timePatterns = listOf(
        // "in X minutes/hours"
        Regex("""in\s+(\d+)\s+(minute|min|hour|hr|second|sec)s?""", RegexOption.IGNORE_CASE),
        // "at X:XX AM/PM"
        Regex("""at\s+(\d{1,2})(?::(\d{2}))?\s*(am|pm|a\.m\.|p\.m\.)?""", RegexOption.IGNORE_CASE),
        // "tomorrow"
        Regex("""tomorrow""", RegexOption.IGNORE_CASE),
        // "after class" — contextual, deferred
        Regex("""after\s+class""", RegexOption.IGNORE_CASE)
    )

    override fun canHandle(input: String): Float {
        val lower = input.lowercase()
        return when {
            triggerPatterns.any { it.containsMatchIn(lower) } -> 0.9f
            lower.contains("remind") -> 0.7f
            else -> 0f
        }
    }

    override fun extractParams(input: String, context: ContextSnapshot): Map<String, Any> {
        val params = mutableMapOf<String, Any>()

        // Extract the reminder content (what to remind about)
        val contentMatch = Regex("""(?:remind\s+me\s+(?:to\s+|about\s+)?|don'?t\s+forget\s+(?:to\s+)?)(.+?)(?:\s+(?:in|at|tomorrow|after)\s+|$)""", RegexOption.IGNORE_CASE)
            .find(input)
        params["title"] = contentMatch?.groupValues?.get(1)?.trim() ?: input

        // Extract time
        for (pattern in timePatterns) {
            val match = pattern.find(input) ?: continue
            when {
                // "in X minutes/hours"
                match.groupValues.size >= 3 && match.groupValues[2].isNotEmpty() -> {
                    val amount = match.groupValues[1].toLongOrNull() ?: continue
                    val unit = match.groupValues[2].lowercase()
                    val ms = when {
                        unit.startsWith("min") -> TimeUnit.MINUTES.toMillis(amount)
                        unit.startsWith("hour") || unit.startsWith("hr") -> TimeUnit.HOURS.toMillis(amount)
                        unit.startsWith("sec") -> TimeUnit.SECONDS.toMillis(amount)
                        else -> continue
                    }
                    params["triggerTime"] = System.currentTimeMillis() + ms
                    break
                }
                // "at X:XX"
                match.groupValues[0].lowercase().startsWith("at") -> {
                    val hour = match.groupValues[1].toIntOrNull() ?: continue
                    val minute = match.groupValues[2].toIntOrNull() ?: 0
                    val ampm = match.groupValues.getOrElse(3) { "" }.lowercase().replace(".", "")
                    val adjustedHour = when {
                        ampm == "pm" && hour < 12 -> hour + 12
                        ampm == "am" && hour == 12 -> 0
                        else -> hour
                    }
                    val today = LocalDate.now()
                    var triggerTime = today.atTime(adjustedHour, minute)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                    // If time already passed today, schedule for tomorrow
                    if (triggerTime < System.currentTimeMillis()) {
                        triggerTime += TimeUnit.DAYS.toMillis(1)
                    }
                    params["triggerTime"] = triggerTime
                    break
                }
                // "tomorrow"
                match.groupValues[0].lowercase().contains("tomorrow") -> {
                    params["triggerTime"] = LocalDate.now().plusDays(1)
                        .atTime(9, 0)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                    break
                }
                // "after class" — default to 1 hour from now
                match.groupValues[0].lowercase().contains("after class") -> {
                    params["triggerTime"] = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1)
                    break
                }
            }
        }

        // Default: 1 hour from now
        if (!params.containsKey("triggerTime")) {
            params["triggerTime"] = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1)
        }

        return params
    }

    override suspend fun execute(params: Map<String, Any>, context: ContextSnapshot): SkillResult {
        val title = params["title"] as? String ?: "Reminder"
        val triggerTime = (params["triggerTime"] as? Number)?.toLong()
            ?: return SkillResult.Error("Could not determine reminder time")

        return try {
            val id = reminderRepository.create(title = title, triggerTime = triggerTime)
            val remaining = triggerTime - System.currentTimeMillis()
            val minutesAway = TimeUnit.MILLISECONDS.toMinutes(remaining)

            val timeDescription = when {
                minutesAway < 1 -> "less than a minute"
                minutesAway < 60 -> "$minutesAway minutes"
                minutesAway < 120 -> "about an hour"
                else -> "${minutesAway / 60} hours"
            }

            SkillResult.Success(
                message = "Reminder created: \"$title\" in $timeDescription",
                spokenResponse = "I'll remind you about $title in $timeDescription"
            )
        } catch (e: Exception) {
            SkillResult.Error("Failed to create reminder: ${e.message}")
        }
    }
}
