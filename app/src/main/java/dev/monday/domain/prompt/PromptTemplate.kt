package dev.monday.domain.prompt

/**
 * A versioned prompt template for AI interactions.
 * Every prompt is identified by an id and version.
 * When you change a prompt, bump the version.
 * History ties each AI call to the exact prompt version used.
 */
data class PromptTemplate(
    val id: String,
    val version: Int,
    val systemPrompt: String,
    val userTemplate: String
)

/**
 * A single conversational turn for history.
 */
data class ChatTurn(
    val role: String,
    val text: String
)

/**
 * Ready-to-send prompt payload with variables substituted.
 */
data class PromptPayload(
    val templateId: String,
    val templateVersion: Int,
    val systemPrompt: String,
    val userPrompt: String,
    val history: List<ChatTurn> = emptyList()
)
