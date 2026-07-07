package dev.monday.core.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.monday.data.database.MondayDatabase
import dev.monday.data.database.dao.*
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MondayDatabase {
        return Room.databaseBuilder(
            context,
            MondayDatabase::class.java,
            "monday_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides fun provideNotificationDao(db: MondayDatabase): NotificationDao = db.notificationDao()
    @Provides fun provideReminderDao(db: MondayDatabase): ReminderDao = db.reminderDao()
    @Provides fun provideCommandHistoryDao(db: MondayDatabase): CommandHistoryDao = db.commandHistoryDao()
    @Provides fun provideActionDao(db: MondayDatabase): ActionDao = db.actionDao()
    @Provides fun provideEventDao(db: MondayDatabase): EventDao = db.eventDao()
    @Provides fun provideAiMemoryDao(db: MondayDatabase): AiMemoryDao = db.aiMemoryDao()
    @Provides fun provideConversationDao(db: MondayDatabase): ConversationDao = db.conversationDao()
    @Provides fun provideConversationMessageDao(db: MondayDatabase): ConversationMessageDao = db.conversationMessageDao()
    @Provides fun provideMetricDao(db: MondayDatabase): MetricDao = db.metricDao()
    @Provides fun provideSettingDao(db: MondayDatabase): SettingDao = db.settingDao()
}
