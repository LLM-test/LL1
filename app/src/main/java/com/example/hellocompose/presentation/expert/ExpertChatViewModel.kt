package com.example.hellocompose.presentation.expert

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hellocompose.domain.model.ChatMessage
import com.example.hellocompose.domain.model.ChatSettings
import com.example.hellocompose.domain.model.ExpertCharacter
import com.example.hellocompose.domain.model.ExpertCharacters
import com.example.hellocompose.domain.model.ExpertMessage
import com.example.hellocompose.domain.model.ExpertResponse
import com.example.hellocompose.domain.usecase.SendMessageUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ExpertChatViewModel(
    private val sendMessageUseCase: SendMessageUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ExpertChatState())
    val state: StateFlow<ExpertChatState> = _state.asStateFlow()

    private val _effect = Channel<ExpertChatEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun handleIntent(intent: ExpertChatIntent) {
        when (intent) {
            is ExpertChatIntent.TypeMessage -> _state.update { it.copy(inputText = intent.text) }
            is ExpertChatIntent.SendMessage -> sendMessage()
            is ExpertChatIntent.ToggleCharacter -> toggleCharacter(intent.character)
            is ExpertChatIntent.ConfirmCharacters -> confirmCharacters()
            is ExpertChatIntent.ResetCharacters -> _state.update {
                it.copy(
                    showCharacterPicker = true,
                    selectedCharacters = emptyList(),
                    messages = emptyList()
                )
            }
        }
    }

    private fun toggleCharacter(character: ExpertCharacter) {
        _state.update { state ->
            val current = state.selectedCharacters
            val updated = if (current.any { it.id == character.id }) {
                current.filter { it.id != character.id }
            } else {
                current + character
            }
            state.copy(selectedCharacters = updated)
        }
    }

    private fun confirmCharacters() {
        if (_state.value.selectedCharacters.isEmpty()) return
        _state.update { it.copy(showCharacterPicker = false) }
    }

    private fun sendMessage() {
        val text = _state.value.inputText.trim()
        if (text.isBlank()) return
        val characters = _state.value.selectedCharacters
        if (characters.isEmpty()) return

        // Создаём запись в истории с loading-заглушками для каждого эксперта
        val loadingResponses = characters.map { character ->
            ExpertResponse(character = character, content = "", isLoading = true)
        }
        val expertMessage = ExpertMessage(
            question = text,
            responses = loadingResponses
        )
        val messageId = expertMessage.id

        _state.update {
            it.copy(
                messages = it.messages + expertMessage,
                inputText = "",
                isAnyLoading = true
            )
        }

        viewModelScope.launch {
            _effect.send(ExpertChatEffect.ScrollToBottom)

            // Параллельно запускаем запросы ко всем экспертам
            val deferredResponses = characters.map { character ->
                async {
                    val userMsg = ChatMessage(
                        role = ChatMessage.Role.USER,
                        content = text
                    )
                    val settings = ChatSettings(
                        systemPrompt = character.systemPrompt,
                        temperature = 0.8f,
                        maxTokens = character.maxTokens
                    )
                    val result = sendMessageUseCase(
                        conversationHistory = listOf(userMsg),
                        settings = settings
                    )
                    character to result
                }
            }

            // По мере завершения каждого запроса — обновляем конкретного эксперта в State
            // Используем awaitAll чтобы дождаться всех и обновить isAnyLoading
            val results = deferredResponses.map { it.await() }

            _state.update { state ->
                val updatedMessages = state.messages.map { msg ->
                    if (msg.id != messageId) return@map msg
                    val updatedResponses = results.map { (character, result) ->
                        result.fold(
                            onSuccess = { chatMsg ->
                                ExpertResponse(
                                    character = character,
                                    content = chatMsg.content,
                                    isLoading = false,
                                    isError = false
                                )
                            },
                            onFailure = { error ->
                                Log.e("ExpertChat", "Error from ${character.id}: ${error.message}")
                                ExpertResponse(
                                    character = character,
                                    content = error.message ?: "Не удалось получить ответ",
                                    isLoading = false,
                                    isError = true
                                )
                            }
                        )
                    }
                    msg.copy(responses = updatedResponses)
                }
                state.copy(messages = updatedMessages, isAnyLoading = false)
            }

            _effect.send(ExpertChatEffect.ScrollToBottom)
        }
    }
}
