package dev.monday.data.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.monday.core.event.EventBus
import dev.monday.core.event.MondayEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Speech-to-Text (STT) and Text-to-Speech (TTS) for Monday.
 *
 * Push-to-talk model:
 * 1. User activates listening (button press)
 * 2. SpeechRecognizer captures and transcribes
 * 3. Result emitted as VoiceCommand event
 * 4. Monday responds via TTS
 */
@Singleton
class SpeechManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val eventBus: EventBus
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    private val _listeningState = MutableStateFlow(ListeningState.IDLE)
    val listeningState: StateFlow<ListeningState> = _listeningState.asStateFlow()

    private val _speakingState = MutableStateFlow(false)
    val speakingState: StateFlow<Boolean> = _speakingState.asStateFlow()

    private val _lastTranscription = MutableStateFlow<String?>(null)
    val lastTranscription: StateFlow<String?> = _lastTranscription.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun initialize() {
        // Initialize TTS
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _speakingState.value = true
                    }
                    override fun onDone(utteranceId: String?) {
                        _speakingState.value = false
                    }
                    @Deprecated("Deprecated")
                    override fun onError(utteranceId: String?) {
                        _speakingState.value = false
                    }
                })
                ttsReady = true
            }
        }

        // Initialize Speech Recognizer
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createRecognitionListener())
            }
        }
    }

    fun startListening() {
        if (_listeningState.value == ListeningState.LISTENING) return
        if (speechRecognizer == null) {
            _errorMessage.value = "Speech recognition not available"
            return
        }

        // Stop TTS if speaking
        if (_speakingState.value) {
            tts?.stop()
            _speakingState.value = false
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        _listeningState.value = ListeningState.LISTENING
        _errorMessage.value = null
        _lastTranscription.value = null
        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        _listeningState.value = ListeningState.IDLE
    }

    fun speak(text: String) {
        if (!ttsReady) return
        val utteranceId = UUID.randomUUID().toString()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stopSpeaking() {
        tts?.stop()
        _speakingState.value = false
    }

    fun shutdown() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        tts?.stop()
        tts?.shutdown()
        tts = null
        ttsReady = false
    }

    private fun createRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _listeningState.value = ListeningState.LISTENING
        }

        override fun onBeginningOfSpeech() {
            _listeningState.value = ListeningState.LISTENING
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Could expose for waveform visualization
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _listeningState.value = ListeningState.PROCESSING
        }

        override fun onError(error: Int) {
            val message = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                SpeechRecognizer.ERROR_SERVER -> "Server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
                else -> "Recognition error ($error)"
            }
            _errorMessage.value = message
            _listeningState.value = ListeningState.IDLE
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull()

            if (text != null) {
                _lastTranscription.value = null
                scope.launch {
                    eventBus.emit(MondayEvent.VoiceCommand(text))
                }
            } else {
                _lastTranscription.value = null
            }
            _listeningState.value = ListeningState.IDLE
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull()
            if (text != null) {
                _lastTranscription.value = text
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
}

enum class ListeningState {
    IDLE,
    LISTENING,
    PROCESSING
}
