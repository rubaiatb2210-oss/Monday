package dev.monday.core.event

import dev.monday.core.model.CommandResult
import dev.monday.core.model.SystemEventType
import java.time.Instant

/**
 * All events flowing through Monday's nervous system.
 * Every module publishes events, any module can subscribe.
 */
sealed class MondayEvent {
    abstract val timestamp: Instant

    /** A device notification was received and scored. */
    data class NotificationReceived(
        val packageName: String,
        val title: String?,
        val text: String?,
        val priorityScore: Float,
        val category: String?,
        override val timestamp: Instant = Instant.now()
    ) : MondayEvent()

    /** A voice command was transcribed. */
    data class VoiceCommand(
        val text: String,
        override val timestamp: Instant = Instant.now()
    ) : MondayEvent()

    /** A command completed the full pipeline. */
    data class CommandCompleted(
        val result: CommandResult,
        override val timestamp: Instant = Instant.now()
    ) : MondayEvent()

    /** A reminder triggered at its scheduled time. */
    data class ReminderTriggered(
        val reminderId: Long,
        val title: String,
        override val timestamp: Instant = Instant.now()
    ) : MondayEvent()

    /** A background sync completed. */
    data class SyncCompleted(
        val source: String,
        val itemCount: Int,
        override val timestamp: Instant = Instant.now()
    ) : MondayEvent()

    /** Device context changed (battery, connectivity, etc). */
    data class ContextChanged(
        val type: String,
        val value: String,
        override val timestamp: Instant = Instant.now()
    ) : MondayEvent()

    /** System-level event. */
    data class SystemEvent(
        val type: SystemEventType,
        override val timestamp: Instant = Instant.now()
    ) : MondayEvent()
}
