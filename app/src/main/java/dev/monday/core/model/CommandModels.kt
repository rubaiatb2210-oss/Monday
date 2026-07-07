package dev.monday.core.model

import java.time.Instant

/**
 * Raw input entering the Command Pipeline from any source.
 */
data class CommandInput(
    val text: String,
    val source: InputSource,
    val timestamp: Instant = Instant.now(),
    val metadata: Map<String, String> = emptyMap()
)

/**
 * The fully resolved intent from a command.
 */
data class ResolvedIntent(
    val skillId: String,
    val action: String,
    val params: Map<String, Any> = emptyMap(),
    val confidence: Float = 1.0f
)

/**
 * Decision made by the pipeline about how to handle a command.
 */
data class Decision(
    val shouldExecute: Boolean,
    val requiresConfirmation: Boolean = false,
    val explanation: String = "",
    val priorityScore: Float = 0.5f
)

/**
 * Result of executing through the full pipeline.
 */
data class CommandResult(
    val id: String,
    val input: CommandInput,
    val intent: ResolvedIntent?,
    val decision: Decision,
    val executionResult: ExecutionResult,
    val durationMs: Long,
    val timestamp: Instant = Instant.now()
)

/**
 * Outcome of executing an action.
 */
sealed class ExecutionResult {
    data class Success(val message: String, val data: Any? = null) : ExecutionResult()
    data class Failure(val error: String, val retryable: Boolean = false) : ExecutionResult()
    data class NeedsConfirmation(val description: String) : ExecutionResult()
    data class Spoken(val text: String) : ExecutionResult()
}

/**
 * Context snapshot provided to skills and AI.
 */
data class ContextSnapshot(
    val currentTime: Instant,
    val timeOfDay: TimeOfDay,
    val dayOfWeek: java.time.DayOfWeek,
    val batteryLevel: Int,
    val isCharging: Boolean,
    val connectivity: Connectivity
)
