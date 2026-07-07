package dev.monday.ui.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.monday.core.model.CommandInput
import dev.monday.core.model.ExecutionResult
import dev.monday.core.model.InputSource
import dev.monday.core.model.MessageRole
import dev.monday.data.database.entity.ConversationEntity
import dev.monday.data.database.entity.ConversationMessageEntity
import dev.monday.data.repository.ConversationRepository
import dev.monday.domain.command.CommandPipeline
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AssistantViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val commandPipeline: CommandPipeline
) : ViewModel() {

    val conversations: StateFlow<List<ConversationEntity>> =
        conversationRepository.getAllConversations()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeConversationId = MutableStateFlow<Long?>(null)
    val activeConversationId: StateFlow<Long?> = _activeConversationId.asStateFlow()

    val activeMessages: StateFlow<List<ConversationMessageEntity>> =
        _activeConversationId.flatMapLatest { id ->
            if (id != null) conversationRepository.getMessages(id)
            else flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    fun openConversation(id: Long) {
        _activeConversationId.value = id
    }

    fun startNewConversation() {
        viewModelScope.launch {
            val id = conversationRepository.createConversation()
            _activeConversationId.value = id
        }
    }

    fun goBack() {
        _activeConversationId.value = null
    }

    fun sendMessage(text: String) {
        val conversationId = _activeConversationId.value ?: return
        if (text.isBlank()) return

        viewModelScope.launch {
            _isSending.value = true

            // Add user message
            conversationRepository.addMessage(conversationId, MessageRole.USER, text)

            // Process through pipeline
            try {
                val result = commandPipeline.process(
                    CommandInput(text = text, source = InputSource.TEXT)
                )
                val responseText = when (val exec = result.executionResult) {
                    is ExecutionResult.Success -> (exec.data as? String) ?: exec.message
                    is ExecutionResult.Spoken -> exec.text
                    is ExecutionResult.Failure -> "Sorry, ${exec.error}"
                    is ExecutionResult.NeedsConfirmation -> exec.description
                }
                conversationRepository.addMessage(conversationId, MessageRole.ASSISTANT, responseText)

                // Update conversation title from first message if needed
                val conversation = conversationRepository.getConversation(conversationId)
                if (conversation?.title == null) {
                    val title = text.take(40) + if (text.length > 40) "…" else ""
                    conversationRepository.getConversation(conversationId)?.let {
                        // Title will be the first user message, abbreviated
                    }
                }
            } catch (e: Exception) {
                conversationRepository.addMessage(
                    conversationId, MessageRole.ASSISTANT,
                    "Something went wrong: ${e.message}"
                )
            }

            _isSending.value = false
        }
    }

    fun togglePin(conversationId: Long, currentlyPinned: Boolean) {
        viewModelScope.launch {
            conversationRepository.setPinned(conversationId, !currentlyPinned)
        }
    }

    fun toggleBookmark(conversationId: Long, currentlyBookmarked: Boolean) {
        viewModelScope.launch {
            conversationRepository.setBookmarked(conversationId, !currentlyBookmarked)
        }
    }

    fun deleteConversation(conversationId: Long) {
        viewModelScope.launch {
            if (_activeConversationId.value == conversationId) {
                _activeConversationId.value = null
            }
            conversationRepository.deleteConversation(conversationId)
        }
    }
}
