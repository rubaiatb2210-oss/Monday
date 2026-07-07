package dev.monday.data.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import dev.monday.core.event.EventBus
import dev.monday.core.event.MondayEvent
import dev.monday.data.database.entity.NotificationEntity
import dev.monday.data.repository.MetricsRepository
import dev.monday.data.repository.NotificationRepository
import dev.monday.domain.rule.RuleEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Notification Listener Service — captures all device notifications.
 *
 * Scores each notification via the Rule Engine, persists it, and
 * emits events to the EventBus.
 *
 * Note: This is a system-managed service. Hilt injection isn't
 * directly supported here, so we use the application's service locator.
 */
class MondayNotificationListener : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // These are resolved lazily from the application
    private val eventBus: EventBus by lazy {
        (application as? MondayNotificationListenerHost)?.eventBus
            ?: throw IllegalStateException("Application must implement MondayNotificationListenerHost")
    }
    private val notificationRepository: NotificationRepository by lazy {
        (application as? MondayNotificationListenerHost)?.notificationRepository
            ?: throw IllegalStateException("Application must implement MondayNotificationListenerHost")
    }
    private val ruleEngine: RuleEngine by lazy {
        (application as? MondayNotificationListenerHost)?.ruleEngine
            ?: throw IllegalStateException("Application must implement MondayNotificationListenerHost")
    }
    private val metricsRepository: MetricsRepository by lazy {
        (application as? MondayNotificationListenerHost)?.metricsRepository
            ?: throw IllegalStateException("Application must implement MondayNotificationListenerHost")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return

        // Skip our own notifications
        if (sbn.packageName == packageName) return

        serviceScope.launch {
            processNotification(sbn)
        }
    }

    private suspend fun processNotification(sbn: StatusBarNotification) {
        val extras = sbn.notification?.extras
        val title = extras?.getCharSequence("android.title")?.toString()
        val text = extras?.getCharSequence("android.text")?.toString()
        val packageName = sbn.packageName

        // Check for duplicates
        if (ruleEngine.isDuplicate(title, text)) return

        // Score the notification
        val score = ruleEngine.scoreNotification(packageName, title, text)

        // Build JSON payload for future-proofing
        val payloadJson = buildJsonObject {
            put("packageName", packageName)
            put("title", title ?: "")
            put("text", text ?: "")
            put("key", sbn.key)
            put("postTime", sbn.postTime)
            put("category", sbn.notification?.category ?: "")
        }.toString()

        // Persist
        notificationRepository.insert(
            NotificationEntity(
                packageName = packageName,
                title = title,
                text = text,
                timestamp = sbn.postTime,
                priorityScore = score,
                category = sbn.notification?.category,
                payloadJson = payloadJson
            )
        )

        // Emit event
        eventBus.tryEmit(
            MondayEvent.NotificationReceived(
                packageName = packageName,
                title = title,
                text = text,
                priorityScore = score,
                category = sbn.notification?.category
            )
        )

        // Track metric
        metricsRepository.increment(MetricsRepository.METRIC_NOTIFICATIONS)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Optional: track notification dismissals for future analytics
    }
}

/**
 * Interface for the Application to provide dependencies to the
 * NotificationListenerService (which can't use Hilt directly).
 */
interface MondayNotificationListenerHost {
    val eventBus: EventBus
    val notificationRepository: NotificationRepository
    val ruleEngine: RuleEngine
    val metricsRepository: MetricsRepository
}
