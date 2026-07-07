package dev.monday.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.monday.data.repository.NotificationRepository
import dev.monday.data.repository.ReminderRepository
import dev.monday.domain.briefing.BriefingGenerator
import dev.monday.domain.briefing.BriefingResult
import dev.monday.domain.context.ContextEngine
import dev.monday.core.model.TimeOfDay
import dev.monday.data.database.entity.ReminderEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val briefingGenerator: BriefingGenerator,
    private val contextEngine: ContextEngine,
    notificationRepository: NotificationRepository,
    reminderRepository: ReminderRepository
) : ViewModel() {

    private val _briefing = MutableStateFlow<BriefingResult?>(null)
    val briefing: StateFlow<BriefingResult?> = _briefing.asStateFlow()

    private val _isLoadingBriefing = MutableStateFlow(false)
    val isLoadingBriefing: StateFlow<Boolean> = _isLoadingBriefing.asStateFlow()

    val unreadCount: StateFlow<Int> = notificationRepository.getUnreadCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val upcomingReminders: StateFlow<List<ReminderEntity>> = reminderRepository.getActive()
        .map { it.take(5) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val timeOfDay: TimeOfDay
        get() = contextEngine.classifyTimeOfDay()

    val greeting: String
        get() = when (timeOfDay) {
            TimeOfDay.MORNING -> "Good morning"
            TimeOfDay.AFTERNOON -> "Good afternoon"
            TimeOfDay.EVENING -> "Good evening"
            TimeOfDay.NIGHT -> "Good night"
        }

    init {
        refreshBriefing()
    }

    fun refreshBriefing() {
        viewModelScope.launch {
            _isLoadingBriefing.value = true
            try {
                _briefing.value = briefingGenerator.generate()
            } catch (e: Exception) {
                // Silently handle — template briefing should never fail
            }
            _isLoadingBriefing.value = false
        }
    }
}
