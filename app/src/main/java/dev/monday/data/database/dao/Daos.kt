package dev.monday.data.database.dao

import androidx.room.*
import dev.monday.data.database.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity): Long

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int = 100): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE priorityScore >= :minScore ORDER BY timestamp DESC")
    fun getByMinScore(minScore: Float): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE isRead = 0 ORDER BY priorityScore DESC")
    fun getUnread(): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0 AND priorityScore >= :minScore")
    fun getUnreadCount(minScore: Float = 0.5f): Flow<Int>

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markRead(id: Long)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllRead()

    @Query("""
        SELECT * FROM notifications 
        WHERE title = :title AND text = :text 
        AND timestamp > :sinceTimestamp
    """)
    suspend fun findDuplicates(title: String?, text: String?, sinceTimestamp: Long): List<NotificationEntity>

    @Query("DELETE FROM notifications WHERE timestamp < :beforeTimestamp")
    suspend fun deleteOlderThan(beforeTimestamp: Long)
}

@Dao
interface ReminderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ReminderEntity): Long

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Delete
    suspend fun delete(reminder: ReminderEntity)

    @Query("SELECT * FROM reminders WHERE isCompleted = 0 ORDER BY triggerTime ASC")
    fun getActive(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isCompleted = 0 AND triggerTime <= :now ORDER BY triggerTime ASC")
    fun getOverdue(now: Long = System.currentTimeMillis()): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isCompleted = 0 AND triggerTime > :now ORDER BY triggerTime ASC LIMIT 1")
    suspend fun getNext(now: Long = System.currentTimeMillis()): ReminderEntity?

    @Query("""
        SELECT * FROM reminders 
        WHERE isCompleted = 0 
        AND triggerTime BETWEEN :dayStart AND :dayEnd 
        ORDER BY triggerTime ASC
    """)
    fun getForDay(dayStart: Long, dayEnd: Long): Flow<List<ReminderEntity>>

    @Query("UPDATE reminders SET isCompleted = 1 WHERE id = :id")
    suspend fun markCompleted(id: Long)

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: Long): ReminderEntity?
}

@Dao
interface CommandHistoryDao {
    @Insert
    suspend fun insert(command: CommandHistoryEntity): Long

    @Query("SELECT * FROM command_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int = 50): Flow<List<CommandHistoryEntity>>

    @Query("SELECT * FROM command_history WHERE inputSource = :source ORDER BY timestamp DESC LIMIT :limit")
    fun getBySource(source: String, limit: Int = 20): Flow<List<CommandHistoryEntity>>

    @Query("DELETE FROM command_history WHERE timestamp < :beforeTimestamp")
    suspend fun deleteOlderThan(beforeTimestamp: Long)
}

@Dao
interface ActionDao {
    @Insert
    suspend fun insert(action: ActionEntity): Long

    @Update
    suspend fun update(action: ActionEntity)

    @Query("SELECT * FROM actions WHERE status = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPending(): List<ActionEntity>

    @Query("SELECT * FROM actions WHERE status IN ('PENDING', 'RETRYING') ORDER BY createdAt ASC")
    suspend fun getExecutable(): List<ActionEntity>

    @Query("SELECT * FROM actions ORDER BY createdAt DESC LIMIT :limit")
    fun getRecent(limit: Int = 50): Flow<List<ActionEntity>>

    @Query("UPDATE actions SET status = :status, updatedAt = :now WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, now: Long = System.currentTimeMillis())

    @Query("""
        UPDATE actions 
        SET status = :status, retryCount = retryCount + 1, 
            errorMessage = :error, updatedAt = :now 
        WHERE id = :id
    """)
    suspend fun markRetry(id: Long, status: String, error: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE actions SET status = 'CANCELLED', updatedAt = :now WHERE id = :id AND status NOT IN ('COMPLETED')")
    suspend fun cancel(id: Long, now: Long = System.currentTimeMillis())
}

@Dao
interface EventDao {
    @Insert
    suspend fun insert(event: EventEntity): Long

    @Query("SELECT * FROM events WHERE processed = 0 ORDER BY timestamp ASC")
    suspend fun getUnprocessed(): List<EventEntity>

    @Query("UPDATE events SET processed = 1 WHERE id = :id")
    suspend fun markProcessed(id: Long)

    @Query("SELECT * FROM events ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int = 100): Flow<List<EventEntity>>

    @Query("DELETE FROM events WHERE timestamp < :beforeTimestamp")
    suspend fun deleteOlderThan(beforeTimestamp: Long)
}

@Dao
interface AiMemoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(memory: AiMemoryEntity): Long

    @Query("SELECT * FROM ai_memory WHERE type = :type ORDER BY confidence DESC")
    suspend fun getByType(type: String): List<AiMemoryEntity>

    @Query("SELECT * FROM ai_memory WHERE confidence >= :minConfidence ORDER BY lastUsedAt DESC")
    suspend fun getRelevant(minConfidence: Float = 0.5f): List<AiMemoryEntity>

    @Query("UPDATE ai_memory SET lastUsedAt = :now WHERE id = :id")
    suspend fun touchLastUsed(id: Long, now: Long = System.currentTimeMillis())

    @Query("SELECT * FROM ai_memory ORDER BY lastUsedAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 20): List<AiMemoryEntity>
}

@Dao
interface ConversationDao {
    @Insert
    suspend fun insert(conversation: ConversationEntity): Long

    @Update
    suspend fun update(conversation: ConversationEntity)

    @Delete
    suspend fun delete(conversation: ConversationEntity)

    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun getAll(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE isPinned = 1 ORDER BY updatedAt DESC")
    fun getPinned(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE isBookmarked = 1 ORDER BY updatedAt DESC")
    fun getBookmarked(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getById(id: Long): ConversationEntity?

    @Query("UPDATE conversations SET isPinned = :pinned, updatedAt = :now WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE conversations SET isBookmarked = :bookmarked, updatedAt = :now WHERE id = :id")
    suspend fun setBookmarked(id: Long, bookmarked: Boolean, now: Long = System.currentTimeMillis())
}

@Dao
interface ConversationMessageDao {
    @Insert
    suspend fun insert(message: ConversationMessageEntity): Long

    @Query("SELECT * FROM conversation_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessages(conversationId: Long): Flow<List<ConversationMessageEntity>>

    @Query("SELECT * FROM conversation_messages WHERE conversationId = :conversationId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastMessage(conversationId: Long): ConversationMessageEntity?

    @Query("DELETE FROM conversation_messages WHERE conversationId = :conversationId")
    suspend fun deleteForConversation(conversationId: Long)

    @Query("""
        SELECT * FROM conversation_messages 
        WHERE content LIKE '%' || :query || '%' 
        ORDER BY timestamp DESC LIMIT :limit
    """)
    suspend fun search(query: String, limit: Int = 50): List<ConversationMessageEntity>
}

@Dao
interface MetricDao {
    @Insert
    suspend fun insert(metric: MetricEntity): Long

    @Query("""
        SELECT SUM(value) FROM metrics 
        WHERE metricType = :type AND timestamp >= :since
    """)
    suspend fun sumSince(type: String, since: Long): Float?

    @Query("""
        SELECT AVG(value) FROM metrics 
        WHERE metricType = :type AND timestamp >= :since
    """)
    suspend fun avgSince(type: String, since: Long): Float?

    @Query("""
        SELECT COUNT(*) FROM metrics 
        WHERE metricType = :type AND timestamp >= :since
    """)
    suspend fun countSince(type: String, since: Long): Int

    @Query("DELETE FROM metrics WHERE timestamp < :beforeTimestamp")
    suspend fun deleteOlderThan(beforeTimestamp: Long)
}

@Dao
interface SettingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(setting: SettingEntity)

    @Query("SELECT * FROM settings WHERE `key` = :key")
    suspend fun get(key: String): SettingEntity?

    @Query("SELECT * FROM settings")
    fun getAll(): Flow<List<SettingEntity>>

    @Query("DELETE FROM settings WHERE `key` = :key")
    suspend fun delete(key: String)
}
