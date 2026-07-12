package dev.monday.domain.command

import dev.monday.core.model.*
import dev.monday.data.ai.AiRouter
import dev.monday.data.repository.NotificationRepository
import dev.monday.domain.prompt.ChatTurn
import dev.monday.data.repository.CommandRepository
import dev.monday.data.repository.MetricsRepository
import dev.monday.domain.context.ContextEngine
import dev.monday.domain.prompt.PromptRegistry
import dev.monday.domain.skill.SkillResult
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Command Pipeline — Monday's signature feature.
 *
 * Every interaction follows the same lifecycle:
 *
 *   Input → Command → Intent → Decision → Action → Result → Memory
 *
 * This makes logging, debugging, undo, and future automation trivial.
 */
@Singleton
class CommandPipeline @Inject constructor(
    private val commandEngine: CommandEngine,
    private val contextEngine: ContextEngine,
    private val aiRouter: AiRouter,
    private val promptRegistry: PromptRegistry,
    private val commandRepository: CommandRepository,
    private val metricsRepository: MetricsRepository,
    private val notificationRepository: NotificationRepository
) {
    /**
     * Process a command through the full pipeline.
     */
    suspend fun process(input: CommandInput): CommandResult {
        val startTime = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        val context = contextEngine.snapshot()

        // Step 1: Parse command into intent
        val parseResult = commandEngine.parse(input)

        // Step 2: Route based on parse result
        val result = when (parseResult) {
            is ParseResult.Resolved -> {
                // Deterministic execution via Skill
                executeSkill(id, input, parseResult, context, startTime)
            }
            is ParseResult.NeedsAi -> {
                // Route to AI
                executeAi(id, input, parseResult, context, startTime)
            }
            is ParseResult.Ambiguous -> {
                // Take the best match for now
                // Future: ask user to clarify
                val bestMatch = parseResult.candidates.first()
                executeSkill(
                    id, input,
                    ParseResult.Resolved(bestMatch),
                    context, startTime
                )
            }
        }

        // Step 3: Record in history (Memory)
        commandRepository.record(result)
        metricsRepository.increment(MetricsRepository.METRIC_COMMANDS)
        metricsRepository.recordResponseTime(result.durationMs)

        return result
    }

    private suspend fun executeSkill(
        id: String,
        input: CommandInput,
        parseResult: ParseResult.Resolved,
        context: ContextSnapshot,
        startTime: Long
    ): CommandResult {
        val match = parseResult.match
        val intent = ResolvedIntent(
            skillId = match.skill.id,
            action = match.skill.name,
            params = match.params,
            confidence = match.confidence
        )

        val decision = Decision(
            shouldExecute = true,
            requiresConfirmation = match.skill.requiresConfirmation,
            explanation = "Matched skill '${match.skill.name}' with ${(match.confidence * 100).toInt()}% confidence",
            priorityScore = match.confidence
        )

        // Execute the skill
        val skillResult = match.skill.execute(match.params, context)
        metricsRepository.increment(MetricsRepository.METRIC_RULE_HITS)
        metricsRepository.recordSkillUsage(match.skill.id)

        val executionResult = when (skillResult) {
            is SkillResult.Success -> ExecutionResult.Success(
                message = skillResult.message,
                data = skillResult.spokenResponse
            )
            is SkillResult.Error -> ExecutionResult.Failure(
                error = skillResult.message,
                retryable = skillResult.retryable
            )
            is SkillResult.NeedsAi -> {
                // Skill deferred to AI — route through
                return executeAiFromSkill(id, input, skillResult, intent, decision, context, startTime)
            }
        }

        return CommandResult(
            id = id,
            input = input,
            intent = intent,
            decision = decision,
            executionResult = executionResult,
            durationMs = System.currentTimeMillis() - startTime
        )
    }

    private suspend fun executeAi(
        id: String,
        input: CommandInput,
        parseResult: ParseResult.NeedsAi,
        context: ContextSnapshot,
        startTime: Long
    ): CommandResult {
        val intent = ResolvedIntent(
            skillId = "conversation",
            action = "AI Conversation",
            confidence = 0.5f
        )

        val decision = Decision(
            shouldExecute = true,
            explanation = "No deterministic skill matched — routing to AI",
            priorityScore = 0.5f
        )

        return try {
            val recentNotifications = notificationRepository.getRecentList(15)
            val notificationContext = if (recentNotifications.isEmpty()) "" else {
                "\n\nRecent Notifications:\n" + recentNotifications.joinToString("\n") { "[${it.packageName}] ${it.title}: ${it.text}" }
            }
            val fullContext = contextEngine.toPromptContext() + notificationContext

            val recentCommands = commandRepository.getRecentList(6)
            val history = recentCommands.reversed().flatMap {
                listOf(
                    ChatTurn("user", it.inputText),
                    ChatTurn("model", it.resultMessage ?: "")
                )
            }

            val prompt = promptRegistry.build("conversation", mapOf(
                "context" to fullContext,
                "input" to parseResult.originalText
            )).copy(history = history)

            val aiResponse = aiRouter.route(prompt)
            metricsRepository.increment(MetricsRepository.METRIC_AI_CALLS)
            metricsRepository.increment(MetricsRepository.METRIC_AI_HITS)

            CommandResult(
                id = id,
                input = input,
                intent = intent,
                decision = decision,
                executionResult = ExecutionResult.Spoken(aiResponse.text),
                durationMs = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            CommandResult(
                id = id,
                input = input,
                intent = intent,
                decision = decision,
                executionResult = ExecutionResult.Failure(
                    error = e.message ?: "AI processing failed",
                    retryable = true
                ),
                durationMs = System.currentTimeMillis() - startTime
            )
        }
    }

    private suspend fun executeAiFromSkill(
        id: String,
        input: CommandInput,
        skillResult: SkillResult.NeedsAi,
        intent: ResolvedIntent,
        decision: Decision,
        context: ContextSnapshot,
        startTime: Long
    ): CommandResult {
        return try {
            val recentNotifications = notificationRepository.getRecentList(15)
            val notificationContext = if (recentNotifications.isEmpty()) "" else {
                "\n\nRecent Notifications:\n" + recentNotifications.joinToString("\n") { "[${it.packageName}] ${it.title}: ${it.text}" }
            }
            val fullContext = contextEngine.toPromptContext() + notificationContext

            val recentCommands = commandRepository.getRecentList(6)
            val history = recentCommands.reversed().flatMap {
                listOf(
                    ChatTurn("user", it.inputText),
                    ChatTurn("model", it.resultMessage ?: "")
                )
            }

            val variables = mutableMapOf(
                "context" to fullContext,
                "input" to skillResult.prompt
            )
            variables.putAll(skillResult.context)

            val prompt = promptRegistry.build("conversation", variables).copy(history = history)
            val aiResponse = aiRouter.route(prompt)
            metricsRepository.increment(MetricsRepository.METRIC_AI_CALLS)
            metricsRepository.increment(MetricsRepository.METRIC_AI_HITS)

            CommandResult(
                id = id,
                input = input,
                intent = intent,
                decision = decision.copy(explanation = "${decision.explanation} → routed to AI"),
                executionResult = ExecutionResult.Spoken(aiResponse.text),
                durationMs = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            CommandResult(
                id = id,
                input = input,
                intent = intent,
                decision = decision,
                executionResult = ExecutionResult.Failure(
                    error = e.message ?: "AI processing failed",
                    retryable = true
                ),
                durationMs = System.currentTimeMillis() - startTime
            )
        }
    }
}
