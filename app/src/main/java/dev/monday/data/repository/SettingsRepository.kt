package dev.monday.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.monday.data.database.dao.SettingDao
import dev.monday.data.database.entity.SettingEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "monday_preferences")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingDao: SettingDao
) {
    companion object {
        // DataStore keys for frequently accessed settings
        val KEY_DEVELOPER_MODE = booleanPreferencesKey("developer_mode")
        val KEY_AI_PROVIDER = stringPreferencesKey("ai_provider")
        val KEY_VOICE_ENABLED = booleanPreferencesKey("voice_enabled")
        val KEY_TTS_ENABLED = booleanPreferencesKey("tts_enabled")
        val KEY_SYNC_INTERVAL_MINUTES = intPreferencesKey("sync_interval_minutes")
        val KEY_NOTIFICATION_ACCESS_GRANTED = booleanPreferencesKey("notification_access_granted")
    }

    // DataStore preferences (fast, in-memory)
    val isDeveloperMode: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_DEVELOPER_MODE] ?: false }

    val aiProvider: Flow<String> = context.dataStore.data
        .map { it[KEY_AI_PROVIDER] ?: "gemini_flash" }

    val syncIntervalMinutes: Flow<Int> = context.dataStore.data
        .map { it[KEY_SYNC_INTERVAL_MINUTES] ?: 15 }

    suspend fun setDeveloperMode(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DEVELOPER_MODE] = enabled }
    }

    suspend fun setAiProvider(provider: String) {
        context.dataStore.edit { it[KEY_AI_PROVIDER] = provider }
    }

    suspend fun setSyncInterval(minutes: Int) {
        context.dataStore.edit { it[KEY_SYNC_INTERVAL_MINUTES] = minutes }
    }

    // Room-backed settings (for complex/arbitrary key-value pairs)
    suspend fun set(key: String, value: String) {
        settingDao.set(SettingEntity(key = key, value = value))
    }

    suspend fun get(key: String): String? =
        settingDao.get(key)?.value

    fun getAll(): Flow<List<SettingEntity>> =
        settingDao.getAll()
}
