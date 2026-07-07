package dev.monday.ui.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.monday.core.model.CommandInput
import dev.monday.core.model.CommandResult
import dev.monday.core.model.ExecutionResult
import dev.monday.core.model.InputSource
import dev.monday.data.speech.ListeningState
import dev.monday.data.speech.SpeechManager
import dev.monday.domain.command.CommandPipeline
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VoiceExchange(
    val userText: String,
    val mondayResponse: String?,
    val isProcessing: Boolean = false,
    val isError: Boolean = false
)

@HiltViewModel
class VoiceViewModel @Inject constructor(
    private val speechManager: SpeechManager,
    private val commandPipeline: CommandPipeline
) : ViewModel() {

    val listeningState: StateFlow<ListeningState> = speechManager.listeningState
    val speakingState: StateFlow<Boolean> = speechManager.speakingState
    val errorMessage: StateFlow<String?> = speechManager.errorMessage

    private val _exchanges = MutableStateFlow<List<VoiceExchange>>(emptyList())
    val exchanges: StateFlow<List<VoiceExchange>> = _exchanges.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    init {
        speechManager.initialize()

        // Listen for transcriptions
        viewModelScope.launch {
            speechManager.lastTranscription.filterNotNull().collect { text ->
                processVoiceCommand(text)
            }
        }
    }

    fun toggleListening() {
        when (listeningState.value) {
            ListeningState.IDLE -> speechManager.startListening()
            ListeningState.LISTENING -> speechManager.stopListening()
            ListeningState.PROCESSING -> { /* wait */ }
        }
    }

    fun stopSpeaking() {
        speechManager.stopSpeaking()
    }

    private fun processVoiceCommand(text: String) {
        viewModelScope.launch {
            // Add user message
            val exchange = VoiceExchange(userText = text, mondayResponse = null, isProcessing = true)
            _exchanges.update { it + exchange }
            _isProcessing.value = true

            try {
                val result = commandPipeline.process(
                    CommandInput(text = text, source = InputSource.VOICE)
                )

                val responseText = extractResponseText(result)

                // Update exchange with response
                _exchanges.update { list ->
                    list.toMutableList().apply {
                        val idx = lastIndexOf(exchange)
                        if (idx >= 0) set(idx, exchange.copy(
                            mondayResponse = responseText,
                            isProcessing = false
                        ))
                    }
                }

                // Speak the response
                if (responseText != null) {
                    speechManager.speak(responseText)
                }
            } catch (e: Exception) {
                _exchanges.update { list ->
                    list.toMutableList().apply {
                        val idx = lastIndexOf(exchange)
                        if (idx >= 0) set(idx, exchange.copy(
                            mondayResponse = "Something went wrong: ${e.message}",
                            isProcessing = false,
                            isError = true
                        ))
                    }
                }
            }
            _isProcessing.value = false
        }
    }

    private fun extractResponseText(result: CommandResult): String? {
        return when (val exec = result.executionResult) {
            is ExecutionResult.Success -> {
                (exec.data as? String) ?: exec.message
            }
            is ExecutionResult.Spoken -> exec.text
            is ExecutionResult.Failure -> "Sorry, ${exec.error}"
            is ExecutionResult.NeedsConfirmation -> exec.description
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.shutdown()
    }
}
