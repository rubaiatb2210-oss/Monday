package dev.monday.data.repository

import dev.monday.data.database.dao.MetricDao
import dev.monday.data.database.entity.MetricEntity
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetricsRepository @Inject constructor(
    private val metricDao: MetricDao
) {
    companion object {
        const val METRIC_NOTIFICATIONS = "notifications_received"
        const val METRIC_AI_CALLS = "ai_calls"
        const val METRIC_COMMANDS = "commands_executed"
        const val METRIC_RESPONSE_TIME = "response_time_ms"
        const val METRIC_RULE_HITS = "rule_hits"
        const val METRIC_AI_HITS = "ai_hits"
        const val METRIC_SKILL_PREFIX = "skill_"
    }

    private val todayStart: Long
        get() = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    suspend fun increment(metricType: String, value: Float = 1f) {
        metricDao.insert(MetricEntity(metricType = metricType, value = value))
    }

    suspend fun recordResponseTime(durationMs: Long) {
        increment(METRIC_RESPONSE_TIME, durationMs.toFloat())
    }

    suspend fun recordSkillUsage(skillId: String) {
        increment("$METRIC_SKILL_PREFIX$skillId")
    }

    suspend fun getTodayCount(metricType: String): Float =
        metricDao.sumSince(metricType, todayStart) ?: 0f

    suspend fun getTodayAverage(metricType: String): Float =
        metricDao.avgSince(metricType, todayStart) ?: 0f

    suspend fun getTodaySummary(): MetricsSummary {
        return MetricsSummary(
            notificationsToday = getTodayCount(METRIC_NOTIFICATIONS).toInt(),
            aiCallsToday = getTodayCount(METRIC_AI_CALLS).toInt(),
            commandsToday = getTodayCount(METRIC_COMMANDS).toInt(),
            avgResponseTimeMs = getTodayAverage(METRIC_RESPONSE_TIME),
            ruleHits = getTodayCount(METRIC_RULE_HITS).toInt(),
            aiHits = getTodayCount(METRIC_AI_HITS).toInt()
        )
    }

    suspend fun cleanup(olderThanDays: Int = 30) {
        val cutoff = LocalDate.now()
            .minusDays(olderThanDays.toLong())
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        metricDao.deleteOlderThan(cutoff)
    }
}

data class MetricsSummary(
    val notificationsToday: Int,
    val aiCallsToday: Int,
    val commandsToday: Int,
    val avgResponseTimeMs: Float,
    val ruleHits: Int,
    val aiHits: Int
)
