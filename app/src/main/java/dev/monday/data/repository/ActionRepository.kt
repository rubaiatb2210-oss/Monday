package dev.monday.data.repository

import dev.monday.core.model.ActionStatus
import dev.monday.data.database.dao.ActionDao
import dev.monday.data.database.entity.ActionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActionRepository @Inject constructor(
    private val actionDao: ActionDao
) {
    suspend fun enqueue(type: String, payloadJson: String): Long {
        return actionDao.insert(
            ActionEntity(type = type, payloadJson = payloadJson)
        )
    }

    suspend fun getPending(): List<ActionEntity> =
        actionDao.getPending()

    suspend fun getExecutable(): List<ActionEntity> =
        actionDao.getExecutable()

    fun getRecent(limit: Int = 50): Flow<List<ActionEntity>> =
        actionDao.getRecent(limit)

    suspend fun updateStatus(id: Long, status: ActionStatus) {
        actionDao.updateStatus(id, status.name)
    }

    suspend fun markRetry(id: Long, error: String) {
        actionDao.markRetry(id, ActionStatus.RETRYING.name, error)
    }

    suspend fun cancel(id: Long) {
        actionDao.cancel(id)
    }
}
