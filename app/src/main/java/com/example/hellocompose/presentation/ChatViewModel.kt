package com.example.hellocompose.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hellocompose.domain.model.ChatMessage
import com.example.hellocompose.domain.usecase.SendMessageUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val sendMessageUseCase: SendMessageUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    private val _effect = Channel<ChatEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun handleIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.TypeMessage -> {
                _state.update { it.copy(inputText = intent.text) }
            }

            is ChatIntent.SendMessage -> sendMessage()
        }
    }

    private fun sendMessage() {
        val currentText = _state.value.inputText.trim()
        if (currentText.isBlank()) return

        val userMessage = ChatMessage(
            role = ChatMessage.Role.USER,
            content = currentText
        )

        _state.update {
            it.copy(
                messages = it.messages + userMessage,
                inputText = "",
                isLoading = true
            )
        }

        viewModelScope.launch {
            _effect.send(ChatEffect.ScrollToBottom)
            val result = sendMessageUseCase(_state.value.messages)
            result.onSuccess { assistantMessage ->
                _state.update {
                    it.copy(
                        messages = it.messages + assistantMessage,
                        isLoading = false
                    )
                }
                _effect.send(ChatEffect.ScrollToBottom)
            }.onFailure { error ->
                Log.e("ChatViewModel", "error=$error")
                val errorMessage = ChatMessage(
                    role = ChatMessage.Role.ERROR,
                    content = error.message ?: "Не удалось получить ответ"
                )
                _state.update {
                    it.copy(
                        messages = it.messages + errorMessage,
                        isLoading = false
                    )
                }
                _effect.send(ChatEffect.ScrollToBottom)
            }
        }
    }
}
