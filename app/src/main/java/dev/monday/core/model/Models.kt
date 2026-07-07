package dev.monday.core.model

import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Input source for commands entering the pipeline.
 */
enum class InputSource {
    VOICE,
    TEXT,
    WIDGET,
    NOTIFICATION,
    QUICK_SETTINGS,
    SYSTEM
}

/**
 * Priority thresholds for notification scoring.
 * Score is 0.0 (ignore) to 1.0 (critical).
 */
object PriorityThresholds {
    const val SPEAK_ALOUD = 0.9f
    const val SHOW_TIMELINE = 0.5f
    const val BATCH_DIGEST = 0.2f
    // Below BATCH_DIGEST = ignore / log only
}

/**
 * Time-of-day classification for context awareness.
 */
enum class TimeOfDay {
    MORNING,    // 5:00 - 11:59
    AFTERNOON,  // 12:00 - 16:59
    EVENING,    // 17:00 - 20:59
    NIGHT       // 21:00 - 4:59
}

/**
 * Network connectivity state.
 */
enum class Connectivity {
    WIFI,
    CELLULAR,
    OFFLINE
}

/**
 * Action execution status — full state machine.
 */
enum class ActionStatus {
    PENDING,
    EXECUTING,
    COMPLETED,
    FAILED,
    RETRYING,
    CANCELLED
}

/**
 * System event types.
 */
enum class SystemEventType {
    BATTERY_LOW,
    BATTERY_CHARGING,
    CONNECTIVITY_CHANGED,
    BOOT_COMPLETED,
    APP_OPENED,
    APP_BACKGROUNDED
}

/**
 * Conversation message role.
 */
enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}
