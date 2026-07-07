package dev.monday.domain.skill

import dev.monday.core.model.ContextSnapshot
import javax.inject.Inject

/**
 * Skill: Fallback for conversational AI requests.
 *
 * This skill has the lowest confidence and acts as the catch-all.
 * It routes everything it receives to the AI Router.
 *
 * Handles: "summarize today's announcements", "plan my study schedule",
 *          "explain today's OS lecture", open-ended questions
 */
class ConversationSkill @Inject constructor() : Skill {
    override val id = "conversation"
    override val name = "Conversation"
    override val description = "AI-powered conversation and reasoning"

    override fun canHandle(input: String): Float {
        // Always returns a low confidence — other skills should win if they match.
        // But if nothing else matches, this catches it.
        return 0.1f
    }

    override fun extractParams(input: String, context: ContextSnapshot): Map<String, Any> {
        return mapOf("prompt" to input)
    }

    override suspend fun execute(params: Map<String, Any>, context: ContextSnapshot): SkillResult {
        val prompt = params["prompt"] as? String ?: return SkillResult.Error("No prompt provided")
        // Signal to the pipeline that AI is needed
        return SkillResult.NeedsAi(
            prompt = prompt,
            context = mapOf(
                "timeOfDay" to context.timeOfDay.name,
                "dayOfWeek" to context.dayOfWeek.name
            )
        )
    }
}
