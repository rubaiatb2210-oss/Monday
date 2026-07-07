package dev.monday.domain.skill

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.monday.core.model.ContextSnapshot
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

/**
 * Skill: Set alarms using Android's AlarmClock intent.
 *
 * Handles: "set alarm for 8 PM", "wake me up at 6:30", "alarm 7 AM"
 */
class AlarmSkill @Inject constructor(
    @ApplicationContext private val context: Context
) : Skill {
    override val id = "alarm"
    override val name = "Alarm"
    override val description = "Set alarms via Android AlarmClock"

    private val patterns = listOf(
        Regex("""(?:set\s+(?:an?\s+)?alarm|wake\s+me\s+up|alarm)\s+(?:for\s+|at\s+)?(\d{1,2})(?::(\d{2}))?\s*(am|pm|a\.m\.|p\.m\.)?""", RegexOption.IGNORE_CASE),
        Regex("""(\d{1,2})(?::(\d{2}))?\s*(am|pm|a\.m\.|p\.m\.)\s+alarm""", RegexOption.IGNORE_CASE)
    )

    override fun canHandle(input: String): Float {
        val lower = input.lowercase()
        if (lower.contains("alarm") || lower.contains("wake me up")) {
            return if (patterns.any { it.containsMatchIn(input) }) 0.95f else 0.6f
        }
        return 0f
    }

    override fun extractParams(input: String, context: ContextSnapshot): Map<String, Any> {
        for (pattern in patterns) {
            val match = pattern.find(input) ?: continue
            val hour = match.groupValues[1].toIntOrNull() ?: continue
            val minute = match.groupValues[2].toIntOrNull() ?: 0
            val ampm = match.groupValues[3].lowercase().replace(".", "")

            val adjustedHour = when {
                ampm == "pm" && hour < 12 -> hour + 12
                ampm == "am" && hour == 12 -> 0
                ampm.isEmpty() && hour <= 12 -> {
                    // Heuristic: if current time is afternoon and hour <= 12, assume PM
                    val currentHour = LocalTime.now().hour
                    if (currentHour >= 12 && hour < 12) hour + 12 else hour
                }
                else -> hour
            }

            return mapOf("hour" to adjustedHour, "minute" to minute)
        }
        return emptyMap()
    }

    override suspend fun execute(params: Map<String, Any>, context: ContextSnapshot): SkillResult {
        val hour = params["hour"] as? Int ?: return SkillResult.Error("Could not parse alarm time")
        val minute = params["minute"] as? Int ?: 0

        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            this.context.startActivity(intent)

            val timeStr = String.format(Locale.US, "%d:%02d %s",
                if (hour > 12) hour - 12 else if (hour == 0) 12 else hour,
                minute,
                if (hour >= 12) "PM" else "AM"
            )
            SkillResult.Success(
                message = "Alarm set for $timeStr",
                spokenResponse = "Alarm set for $timeStr"
            )
        } catch (e: Exception) {
            SkillResult.Error("Failed to set alarm: ${e.message}")
        }
    }
}
