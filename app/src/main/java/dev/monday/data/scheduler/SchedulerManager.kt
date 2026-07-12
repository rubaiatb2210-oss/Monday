package dev.monday.data.scheduler

import android.content.Context
import androidx.work.*
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.monday.worker.SyncWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages WorkManager scheduling for background sync.
 */
@Singleton
class SchedulerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val SYNC_WORK_NAME = "monday_periodic_sync"
    }

    /**
     * Schedule periodic background sync.
     * Default: every 15 minutes, requires network, battery not low.
     */
    fun schedulePeriodicSync(intervalMinutes: Long = 15) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            intervalMinutes, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                1, TimeUnit.MINUTES
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    /**
     * Run a one-time sync immediately.
     */
    fun syncNow() {
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        WorkManager.getInstance(context).enqueue(syncRequest)
    }

    /**
     * Cancel periodic sync.
     */
    fun cancelPeriodicSync() {
        WorkManager.getInstance(context).cancelUniqueWork(SYNC_WORK_NAME)
    }

    /**
     * Schedule an exact reminder using AlarmManager.
     */
    fun scheduleExactReminder(reminderId: Long, triggerTimeMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val intent = android.content.Intent(context, dev.monday.worker.ReminderReceiver::class.java).apply {
            putExtra("EXTRA_REMINDER_ID", reminderId)
        }
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    android.app.AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    android.app.AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // Android 14+ requires SCHEDULE_EXACT_ALARM permission, fallback if missing
            alarmManager.set(android.app.AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
        }
    }
}
