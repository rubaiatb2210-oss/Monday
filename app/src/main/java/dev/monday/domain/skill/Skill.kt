package dev.monday.domain.skill

import dev.monday.core.model.ContextSnapshot

/**
 * A pluggable capability that Monday can execute.
 *
 * To add a new skill:
 * 1. Implement this interface
 * 2. Add @Inject constructor
 * 3. Bind into the Skill set via Hilt @IntoSet
 *
 * No architecture changes needed. Just one new file + DI binding.
 */
interface Skill {
    /** Unique identifier for this skill. */
    val id: String

    /** Human-readable name. */
    val name: String

    /** What this skill does, shown in Developer Mode. */
    val description: String

    /** Whether this skill requires user confirmation before executing. */
    val requiresConfirmation: Boolean
        get() = false

    /**
     * How confident this skill is that it can handle the given input.
     * Returns 0.0 (cannot handle) to 1.0 (exact match).
     */
    fun canHandle(input: String): Float

    /**
     * Extract parameters from the input text.
     * Only called if canHandle returned > 0.
     */
    fun extractParams(input: String, context: ContextSnapshot): Map<String, Any>

    /**
     * Execute the skill with the given parameters.
     */
    suspend fun execute(params: Map<String, Any>, context: ContextSnapshot): SkillResult
}

/**
 * Result of skill execution.
 */
sealed class SkillResult {
    /** Skill executed successfully. */
    data class Success(
        val message: String,
        val spokenResponse: String? = null,
        val data: Any? = null
    ) : SkillResult()

    /** Skill execution failed. */
    data class Error(
        val message: String,
        val retryable: Boolean = false
    ) : SkillResult()

    /** Skill needs AI to complete the request. */
    data class NeedsAi(
        val prompt: String,
        val context: Map<String, String> = emptyMap()
    ) : SkillResult()
}

/**
 * A match between user input and a skill.
 */
data class SkillMatch(
    val skill: Skill,
    val confidence: Float,
    val params: Map<String, Any>
)
