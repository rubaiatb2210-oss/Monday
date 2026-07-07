package dev.monday.core.event

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central event bus for Monday.
 *
 * Every module publishes events here. Any module can subscribe.
 * Uses SharedFlow with no replay (events are consumed once)
 * and a generous buffer to prevent suspension under load.
 */
@Singleton
class EventBus @Inject constructor() {

    private val _events = MutableSharedFlow<MondayEvent>(
        replay = 0,
        extraBufferCapacity = 64
    )

    /** Subscribe to all events. */
    val events: SharedFlow<MondayEvent> = _events.asSharedFlow()

    /** Publish an event to all subscribers. */
    suspend fun emit(event: MondayEvent) {
        _events.emit(event)
    }

    /** Try to publish without suspending. Returns false if buffer is full. */
    fun tryEmit(event: MondayEvent): Boolean {
        return _events.tryEmit(event)
    }
}
