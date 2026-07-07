package dev.monday

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import dev.monday.core.event.EventBus
import dev.monday.data.notification.MondayNotificationListenerHost
import dev.monday.data.repository.MetricsRepository
import dev.monday.data.repository.NotificationRepository
import dev.monday.domain.rule.RuleEngine
import javax.inject.Inject

@HiltAndroidApp
class MondayApplication : Application(), Configuration.Provider, MondayNotificationListenerHost {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    override lateinit var eventBus: EventBus

    @Inject
    override lateinit var notificationRepository: NotificationRepository

    @Inject
    override lateinit var ruleEngine: RuleEngine

    @Inject
    override lateinit var metricsRepository: MetricsRepository

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
