package dev.monday.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.monday.data.repository.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val isDeveloperMode: StateFlow<Boolean> = settingsRepository.isDeveloperMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val aiProvider: StateFlow<String> = settingsRepository.aiProvider
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "gemini_flash")

    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    init {
        viewModelScope.launch {
            _apiKey.value = settingsRepository.get("gemini_api_key") ?: ""
        }
    }

    fun toggleDeveloperMode() {
        viewModelScope.launch {
            settingsRepository.setDeveloperMode(!isDeveloperMode.value)
        }
    }

    fun saveApiKey(key: String) {
        viewModelScope.launch {
            settingsRepository.set("gemini_api_key", key)
            _apiKey.value = key
        }
    }
}
