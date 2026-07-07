package dev.monday.data.repository

import dev.monday.core.model.CommandResult
import dev.monday.core.model.ExecutionResult
import dev.monday.core.model.InputSource
import dev.monday.data.database.dao.CommandHistoryDao
import dev.monday.data.database.entity.CommandHistoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommandRepository @Inject constructor(
    private val commandHistoryDao: CommandHistoryDao
) {
    fun getRecent(limit: Int = 50): Flow<List<CommandHistoryEntity>> =
        commandHistoryDao.getRecent(limit)

    fun getBySource(source: InputSource, limit: Int = 20): Flow<List<CommandHistoryEntity>> =
        commandHistoryDao.getBySource(source.name, limit)

    suspend fun record(result: CommandResult) {
        val status = when (result.executionResult) {
            is ExecutionResult.Success -> "SUCCESS"
            is ExecutionResult.Failure -> "FAILURE"
            is ExecutionResult.NeedsConfirmation -> "NEEDS_CONFIRMATION"
            is ExecutionResult.Spoken -> "SPOKEN"
        }
        val message = when (val r = result.executionResult) {
            is ExecutionResult.Success -> r.message
            is ExecutionResult.Failure -> r.error
            is ExecutionResult.NeedsConfirmation -> r.description
            is ExecutionResult.Spoken -> r.text
        }
        commandHistoryDao.insert(
            CommandHistoryEntity(
                inputText = result.input.text,
                inputSource = result.input.source.name,
                resolvedSkillId = result.intent?.skillId,
                resolvedAction = result.intent?.action,
                decision = result.decision.explanation,
                resultStatus = status,
                resultMessage = message,
                durationMs = result.durationMs
            )
        )
    }

    suspend fun cleanup(olderThanMs: Long = 30L * 24 * 60 * 60 * 1000) {
        val cutoff = System.currentTimeMillis() - olderThanMs
        commandHistoryDao.deleteOlderThan(cutoff)
    }
}
