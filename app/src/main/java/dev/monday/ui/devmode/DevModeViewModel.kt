package dev.monday.ui.devmode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.monday.data.database.entity.CommandHistoryEntity
import dev.monday.data.database.entity.EventEntity
import dev.monday.data.repository.CommandRepository
import dev.monday.data.repository.MetricsRepository
import dev.monday.data.repository.MetricsSummary
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DevModeViewModel @Inject constructor(
    private val commandRepository: CommandRepository,
    private val metricsRepository: MetricsRepository
) : ViewModel() {

    val recentCommands: StateFlow<List<CommandHistoryEntity>> =
        commandRepository.getRecent(100)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _metrics = MutableStateFlow<MetricsSummary?>(null)
    val metrics: StateFlow<MetricsSummary?> = _metrics.asStateFlow()

    init {
        refreshMetrics()
    }

    fun refreshMetrics() {
        viewModelScope.launch {
            _metrics.value = metricsRepository.getTodaySummary()
        }
    }
}
