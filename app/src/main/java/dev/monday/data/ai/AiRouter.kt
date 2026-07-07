package dev.monday.data.ai

import dev.monday.domain.prompt.PromptPayload
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI Router — tiered escalation from deterministic to cloud AI.
 *
 * Tier 0: Kotlin (deterministic, handled by CommandEngine/Skills)
 * Tier 1: Gemini Nano (runtime check, likely unavailable on Xperia)
 * Tier 2: Gemini Flash (primary cloud AI)
 * Tier 3: Groq (fallback, stub)
 */
@Singleton
class AiRouter @Inject constructor(
    private val geminiProvider: GeminiProvider
) {
    private val providers: List<AiProvider> by lazy {
        listOf(geminiProvider)
        // Future: add GroqProvider, OpenRouterProvider, etc.
    }

    /**
     * Route a prompt through the tiered AI system.
     * Tries each provider in tier order until one succeeds.
     */
    suspend fun route(prompt: PromptPayload): AiResponse {
        for (provider in providers.sortedBy { it.tier }) {
            if (!provider.isAvailable()) continue
            return try {
                provider.generate(prompt)
            } catch (e: Exception) {
                // Try next provider
                continue
            }
        }
        throw IllegalStateException("No AI provider available. Please configure your API key in Settings.")
    }

    /**
     * Check if any AI provider is available.
     */
    suspend fun isAiAvailable(): Boolean {
        return providers.any { it.isAvailable() }
    }

    /**
     * Get the name of the currently active (highest priority available) provider.
     */
    suspend fun activeProviderName(): String? {
        return providers.sortedBy { it.tier }.firstOrNull { it.isAvailable() }?.name
    }
}
