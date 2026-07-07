package dev.monday.data.repository

import dev.monday.data.database.dao.ReminderDao
import dev.monday.data.database.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepository @Inject constructor(
    private val reminderDao: ReminderDao
) {
    fun getActive(): Flow<List<ReminderEntity>> =
        reminderDao.getActive()

    fun getOverdue(): Flow<List<ReminderEntity>> =
        reminderDao.getOverdue()

    fun getForToday(): Flow<List<ReminderEntity>> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val dayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val dayEnd = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return reminderDao.getForDay(dayStart, dayEnd)
    }

    suspend fun getNext(): ReminderEntity? =
        reminderDao.getNext()

    suspend fun getById(id: Long): ReminderEntity? =
        reminderDao.getById(id)

    suspend fun create(title: String, triggerTime: Long, description: String? = null): Long {
        return reminderDao.insert(
            ReminderEntity(
                title = title,
                description = description,
                triggerTime = triggerTime
            )
        )
    }

    suspend fun complete(id: Long) =
        reminderDao.markCompleted(id)

    suspend fun update(reminder: ReminderEntity) =
        reminderDao.update(reminder)

    suspend fun delete(reminder: ReminderEntity) =
        reminderDao.delete(reminder)
}
