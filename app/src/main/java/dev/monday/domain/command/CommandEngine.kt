package dev.monday.domain.command

import dev.monday.core.model.CommandInput
import dev.monday.core.model.ContextSnapshot
import dev.monday.domain.context.ContextEngine
import dev.monday.domain.skill.SkillMatch
import dev.monday.domain.skill.SkillRegistry
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Command Engine — converts raw input into structured skill matches.
 *
 * This is deterministic-first: it tries the SkillRegistry before
 * falling back to AI. Works for all input sources (voice, text,
 * widget, quick settings, notification buttons).
 */
@Singleton
class CommandEngine @Inject constructor(
    private val skillRegistry: SkillRegistry,
    private val contextEngine: ContextEngine
) {
    /**
     * Parse an input into the best matching skill.
     */
    fun parse(input: CommandInput): ParseResult {
        val context = contextEngine.snapshot()
        val bestMatch = skillRegistry.findBestMatch(input.text, context)

        return when {
            bestMatch == null -> ParseResult.NeedsAi(input.text)
            bestMatch.confidence >= 0.7f -> ParseResult.Resolved(bestMatch)
            else -> {
                // Multiple possible matches — check for ambiguity
                val allMatches = skillRegistry.findMatches(input.text, context, threshold = 0.3f)
                if (allMatches.size > 1 && allMatches[0].confidence - allMatches[1].confidence < 0.2f) {
                    ParseResult.Ambiguous(allMatches.take(3))
                } else {
                    ParseResult.Resolved(bestMatch)
                }
            }
        }
    }
}

/**
 * Result of command parsing.
 */
sealed class ParseResult {
    /** A skill was found with high confidence. */
    data class Resolved(val match: SkillMatch) : ParseResult()

    /** No skill matches — route to AI. */
    data class NeedsAi(val originalText: String) : ParseResult()

    /** Multiple skills match with similar confidence. */
    data class Ambiguous(val candidates: List<SkillMatch>) : ParseResult()
}
