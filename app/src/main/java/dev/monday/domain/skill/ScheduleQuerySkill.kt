package dev.monday.domain.skill

import dev.monday.core.model.ContextSnapshot
import dev.monday.data.repository.ReminderRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Skill: Query schedule and reminders.
 *
 * Handles: "what's next", "today's schedule", "show today's assignments",
 *          "what do I have today", "any reminders"
 */
class ScheduleQuerySkill @Inject constructor(
    private val reminderRepository: ReminderRepository
) : Skill {
    override val id = "schedule_query"
    override val name = "Schedule Query"
    override val description = "Query today's schedule and upcoming reminders"

    private val patterns = listOf(
        Regex("""what'?s?\s+(?:my\s+)?(?:next|schedule|today|coming\s+up)""", RegexOption.IGNORE_CASE),
        Regex("""(?:show|list|any)\s+(?:today'?s?\s+)?(?:schedule|assignments?|reminders?|tasks?|events?)""", RegexOption.IGNORE_CASE),
        Regex("""(?:do\s+I\s+have|what\s+do\s+I\s+have)\s+(?:today|next|coming)""", RegexOption.IGNORE_CASE),
        Regex("""(?:next|upcoming)\s+(?:class|meeting|event|reminder|assignment)""", RegexOption.IGNORE_CASE)
    )

    override fun canHandle(input: String): Float {
        return when {
            patterns.any { it.containsMatchIn(input) } -> 0.85f
            input.lowercase().let {
                it.contains("schedule") || it.contains("what's next") || it.contains("today")
            } -> 0.5f
            else -> 0f
        }
    }

    override fun extractParams(input: String, context: ContextSnapshot): Map<String, Any> {
        return mapOf("query" to input)
    }

    override suspend fun execute(params: Map<String, Any>, context: ContextSnapshot): SkillResult {
        val next = reminderRepository.getNext()

        return if (next != null) {
            val triggerTime = Instant.ofEpochMilli(next.triggerTime)
                .atZone(ZoneId.systemDefault())
                .toLocalTime()
            val timeStr = triggerTime.format(DateTimeFormatter.ofPattern("h:mm a"))

            SkillResult.Success(
                message = "Next up: \"${next.title}\" at $timeStr",
                spokenResponse = "Your next reminder is ${next.title} at $timeStr"
            )
        } else {
            SkillResult.Success(
                message = "Nothing scheduled. You're free!",
                spokenResponse = "You have nothing coming up. You're free."
            )
        }
    }
}
