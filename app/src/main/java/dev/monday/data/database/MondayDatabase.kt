package dev.monday.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.monday.data.database.dao.*
import dev.monday.data.database.entity.*

@Database(
    entities = [
        NotificationEntity::class,
        ReminderEntity::class,
        CommandHistoryEntity::class,
        ActionEntity::class,
        EventEntity::class,
        AiMemoryEntity::class,
        ConversationEntity::class,
        ConversationMessageEntity::class,
        MetricEntity::class,
        SettingEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class MondayDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao
    abstract fun reminderDao(): ReminderDao
    abstract fun commandHistoryDao(): CommandHistoryDao
    abstract fun actionDao(): ActionDao
    abstract fun eventDao(): EventDao
    abstract fun aiMemoryDao(): AiMemoryDao
    abstract fun conversationDao(): ConversationDao
    abstract fun conversationMessageDao(): ConversationMessageDao
    abstract fun metricDao(): MetricDao
    abstract fun settingDao(): SettingDao
}
