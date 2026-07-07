package dev.monday.domain.prompt

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registry of all versioned AI prompts.
 *
 * Every prompt Monday uses is defined here with a version number.
 * When you tweak a prompt, bump the version. History traces
 * every AI call back to the exact prompt version that generated it.
 */
@Singleton
class PromptRegistry @Inject constructor() {

    private val templates = mapOf(
        "conversation" to PromptTemplate(
            id = "conversation",
            version = 1,
            systemPrompt = """You are Monday, a personal executive assistant AI.

You are direct, helpful, and efficient. You speak concisely — no filler, no over-explaining.

Your personality:
- Professional but warm
- Concise — prefer short, clear answers
- Proactive — suggest next steps when relevant
- Honest — say "I don't know" rather than guessing

You have access to the user's:
- Current time and context
- Reminders and schedule
- Recent notifications

Always respond in a way that reduces cognitive load.
Do NOT use markdown formatting in spoken responses.
Keep spoken responses under 3 sentences unless the user asks for detail.""",
            userTemplate = """Context:
{context}

User: {input}"""
        ),

        "daily_briefing" to PromptTemplate(
            id = "daily_briefing",
            version = 1,
            systemPrompt = """You are Monday, generating a daily briefing for your user.

Create a concise, spoken-style briefing covering:
1. Most important items first
2. Upcoming deadlines or reminders
3. Any items that need attention

Keep it under 5 sentences. Be direct. No greetings (the app handles that).
Do NOT use markdown, bullet points, or formatting — this will be spoken aloud.""",
            userTemplate = """Today's data:
{briefing_data}

Generate a natural, spoken briefing."""
        ),

        "summarizer" to PromptTemplate(
            id = "summarizer",
            version = 1,
            systemPrompt = """You are Monday, summarizing information for your user.

Rules:
- Be concise — 2-4 sentences max
- Lead with the most important point
- Skip pleasantries and filler
- If the content is trivial, say so briefly""",
            userTemplate = """Summarize this:
{content}"""
        ),

        "planner" to PromptTemplate(
            id = "planner",
            version = 1,
            systemPrompt = """You are Monday, helping your user plan their time.

Rules:
- Consider their current schedule and deadlines
- Prioritize by urgency, then importance
- Suggest specific time blocks when possible
- Be realistic about time estimates
- Keep the plan actionable and clear""",
            userTemplate = """Context:
{context}

Current schedule:
{schedule}

User request: {input}"""
        )
    )

    fun get(id: String): PromptTemplate =
        templates[id] ?: throw IllegalArgumentException("Unknown prompt template: $id")

    fun build(id: String, variables: Map<String, String>): PromptPayload {
        val template = get(id)
        var userPrompt = template.userTemplate
        variables.forEach { (key, value) ->
            userPrompt = userPrompt.replace("{$key}", value)
        }
        return PromptPayload(
            templateId = template.id,
            templateVersion = template.version,
            systemPrompt = template.systemPrompt,
            userPrompt = userPrompt
        )
    }

    fun allTemplates(): Map<String, PromptTemplate> = templates
}
