package dev.monday.ui.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.monday.data.database.entity.ReminderEntity
import dev.monday.data.repository.ReminderRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository
) : ViewModel() {

    val activeReminders: StateFlow<List<ReminderEntity>> =
        reminderRepository.getActive()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val overdueReminders: StateFlow<List<ReminderEntity>> =
        reminderRepository.getOverdue()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun complete(id: Long) {
        viewModelScope.launch { reminderRepository.complete(id) }
    }

    fun delete(reminder: ReminderEntity) {
        viewModelScope.launch { reminderRepository.delete(reminder) }
    }

    fun createReminder(title: String, triggerTime: Long) {
        viewModelScope.launch {
            reminderRepository.create(title = title, triggerTime = triggerTime)
        }
    }
}
