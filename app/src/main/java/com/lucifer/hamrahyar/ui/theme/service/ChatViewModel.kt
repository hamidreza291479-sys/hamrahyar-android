package com.lucifer.hamrahyar.ui.theme.service

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lucifer.hamrahyar.ui.theme.data.model.FormRequestDto
import com.lucifer.hamrahyar.ui.theme.data.model.FormResponseDto
import com.lucifer.hamrahyar.ui.theme.domain.model.ChatMessage
import com.lucifer.hamrahyar.ui.theme.domain.repository.OnlineServiceRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val formRequests: List<FormRequestDto> = emptyList(),
    val formResponses: List<FormResponseDto> = emptyList(),
    val isLoading: Boolean = false,
    val realtimeStatus: String = "DISCONNECTED",
    val error: String? = null
)

class ChatViewModel(
    private val repository: OnlineServiceRepository,
    private val conversationId: String,
    private val profileId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState(isLoading = true))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        if (conversationId.isBlank()) {
            _uiState.update { it.copy(isLoading = false, error = "Conversation ID is missing") }
            return
        }

        viewModelScope.launch {
            repository.observeRealtimeStatus()
                .onEach { status ->
                    _uiState.update { it.copy(realtimeStatus = status) }
                }
                .launchIn(this)

            repository.getMessages(conversationId, profileId)
                .onEach { messages ->
                    _uiState.update { it.copy(messages = messages, isLoading = false) }
                }
                .catch { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
                .launchIn(this)

            repository.observeFormRequests(conversationId)
                .onEach { forms ->
                    val responses = forms.mapNotNull { req ->
                        val resp = req.response
                        if (resp is kotlinx.serialization.json.JsonObject) {
                            FormResponseDto(
                                id = null,
                                requestId = req.id,
                                orderId = "",
                                data = resp,
                                createdAt = req.submittedAt ?: req.createdAt
                            )
                        } else null
                    }
                    _uiState.update { it.copy(formRequests = forms, formResponses = responses) }
                }
                .catch { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
                .launchIn(this)
        }
    }
    
    fun submitForm(requestId: String, orderId: String, data: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = repository.submitFormResponse(requestId, orderId, data)
            onResult(result)
        }
    }
}

class ChatViewModelFactory(
    private val repository: OnlineServiceRepository,
    private val conversationId: String,
    private val profileId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ChatViewModel(repository, conversationId, profileId) as T
    }
}
