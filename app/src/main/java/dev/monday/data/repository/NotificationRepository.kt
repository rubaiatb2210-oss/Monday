package dev.monday.data.repository

import dev.monday.data.database.dao.NotificationDao
import dev.monday.data.database.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val notificationDao: NotificationDao
) {
    fun getRecent(limit: Int = 100): Flow<List<NotificationEntity>> =
        notificationDao.getRecent(limit)

    fun getByMinScore(minScore: Float): Flow<List<NotificationEntity>> =
        notificationDao.getByMinScore(minScore)

    fun getUnread(): Flow<List<NotificationEntity>> =
        notificationDao.getUnread()

    fun getUnreadCount(minScore: Float = 0.5f): Flow<Int> =
        notificationDao.getUnreadCount(minScore)

    suspend fun insert(notification: NotificationEntity): Long =
        notificationDao.insert(notification)

    suspend fun markRead(id: Long) =
        notificationDao.markRead(id)

    suspend fun markAllRead() =
        notificationDao.markAllRead()

    suspend fun findDuplicates(title: String?, text: String?, withinMs: Long = 5 * 60 * 1000): List<NotificationEntity> {
        val since = System.currentTimeMillis() - withinMs
        return notificationDao.findDuplicates(title, text, since)
    }

    suspend fun cleanup(olderThanMs: Long = 7 * 24 * 60 * 60 * 1000L) {
        val cutoff = System.currentTimeMillis() - olderThanMs
        notificationDao.deleteOlderThan(cutoff)
    }
}
