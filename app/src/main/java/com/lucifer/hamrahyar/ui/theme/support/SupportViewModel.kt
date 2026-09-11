package com.lucifer.hamrahyar.ui.theme.support

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lucifer.hamrahyar.ui.theme.domain.model.SupportChannel
import com.lucifer.hamrahyar.ui.theme.domain.repository.OnlineServiceRepository
import kotlinx.coroutines.launch

class SupportViewModel(private val repository: OnlineServiceRepository) : ViewModel() {

    var uiState by mutableStateOf<SupportUiState>(SupportUiState.Loading)
        private set

    init {
        loadSupportChannels()
    }

    fun loadSupportChannels() {
        uiState = SupportUiState.Loading
        viewModelScope.launch {
            repository.getSupportChannels()
                .onSuccess { channels ->
                    uiState = SupportUiState.Success(channels)
                }
                .onFailure { error ->
                    uiState = SupportUiState.Error(error.message ?: "Unknown error")
                }
        }
    }
}

sealed class SupportUiState {
    object Loading : SupportUiState()
    data class Success(val channels: List<SupportChannel>) : SupportUiState()
    data class Error(val message: String) : SupportUiState()
}
