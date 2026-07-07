package dev.monday.data.ai

import dev.monday.domain.prompt.PromptPayload

/**
 * Interface for AI providers.
 * Implementations: GeminiProvider (Tier 2), GroqProvider (Tier 3 stub).
 */
interface AiProvider {
    val name: String
    val tier: Int

    suspend fun generate(prompt: PromptPayload): AiResponse
    suspend fun isAvailable(): Boolean
}

data class AiResponse(
    val text: String,
    val provider: String,
    val tier: Int,
    val promptId: String,
    val promptVersion: Int,
    val tokensUsed: Int? = null,
    val latencyMs: Long = 0
)
