package dev.monday.domain.skill

import dev.monday.core.model.ContextSnapshot
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registry of all available skills.
 * Skills are injected via Hilt multi-binding (@IntoSet).
 */
@Singleton
class SkillRegistry @Inject constructor(
    private val skills: Set<@JvmSuppressWildcards Skill>
) {
    /**
     * Find the best matching skill for the given input.
     * Returns null if no skill has confidence > threshold.
     */
    fun findBestMatch(input: String, context: ContextSnapshot, threshold: Float = 0.3f): SkillMatch? {
        return skills
            .map { skill ->
                val confidence = skill.canHandle(input)
                SkillMatch(
                    skill = skill,
                    confidence = confidence,
                    params = if (confidence > threshold) skill.extractParams(input, context) else emptyMap()
                )
            }
            .filter { it.confidence > threshold }
            .maxByOrNull { it.confidence }
    }

    /**
     * Find all skills that might handle the input, sorted by confidence.
     */
    fun findMatches(input: String, context: ContextSnapshot, threshold: Float = 0.2f): List<SkillMatch> {
        return skills
            .map { skill ->
                val confidence = skill.canHandle(input)
                SkillMatch(
                    skill = skill,
                    confidence = confidence,
                    params = if (confidence > threshold) skill.extractParams(input, context) else emptyMap()
                )
            }
            .filter { it.confidence > threshold }
            .sortedByDescending { it.confidence }
    }

    fun getSkill(id: String): Skill? =
        skills.find { it.id == id }

    fun allSkills(): List<Skill> =
        skills.toList()
}
