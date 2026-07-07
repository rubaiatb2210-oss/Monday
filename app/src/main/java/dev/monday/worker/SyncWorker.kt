package dev.monday.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.monday.core.event.EventBus
import dev.monday.core.event.MondayEvent
import dev.monday.data.repository.NotificationRepository

/**
 * Periodic background sync worker.
 *
 * Phase 1: Cleans up old data and emits sync events.
 * Future phases: Gmail, Calendar, Classroom, Slack, Discord sync.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val notificationRepository: NotificationRepository,
    private val eventBus: EventBus
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // Clean up old notifications (> 7 days)
            notificationRepository.cleanup()

            // Emit sync completion
            eventBus.emit(
                MondayEvent.SyncCompleted(
                    source = "system",
                    itemCount = 0
                )
            )

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
