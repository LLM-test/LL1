package com.example.hellocompose.presentation.temperature

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hellocompose.domain.model.ChatMessage
import com.example.hellocompose.domain.model.ChatSettings
import com.example.hellocompose.domain.model.TemperatureResponse
import com.example.hellocompose.domain.model.TemperatureRound
import com.example.hellocompose.domain.usecase.SendMessageUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TemperatureChatViewModel(
    private val sendMessageUseCase: SendMessageUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(TemperatureChatState())
    val state: StateFlow<TemperatureChatState> = _state.asStateFlow()

    private val _effect = Channel<TemperatureChatEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    // Три фиксированных значения температуры — суть задания
    private val temperatures = listOf(0f, 0.7f, 1.2f)

    private val systemPrompt = """
Ты — полезный ассистент. Отвечай развёрнуто и содержательно на вопрос пользователя.
    """.trimIndent()

    fun handleIntent(intent: TemperatureChatIntent) {
        when (intent) {
            is TemperatureChatIntent.TypeMessage -> _state.update { it.copy(inputText = intent.text) }
            is TemperatureChatIntent.SendMessage -> sendMessage()
            is TemperatureChatIntent.ClearHistory -> _state.update {
                it.copy(rounds = emptyList(), inputText = "")
            }
        }
    }

    private fun sendMessage() {
        val text = _state.value.inputText.trim()
        if (text.isBlank() || _state.value.isAnyLoading) return

        // Создаём раунд с loading-заглушками для каждой температуры
        val loadingResponses = temperatures.map { temp ->
            TemperatureResponse(temperature = temp, isLoading = true)
        }
        val round = TemperatureRound(question = text, responses = loadingResponses)
        val roundId = round.id

        _state.update {
            it.copy(
                rounds = it.rounds + round,
                inputText = "",
                isAnyLoading = true
            )
        }

        viewModelScope.launch {
            _effect.send(TemperatureChatEffect.ScrollToBottom)

            // Параллельно отправляем запрос с каждой температурой
            val deferred = temperatures.map { temp ->
                async {
                    val userMsg = ChatMessage(
                        role = ChatMessage.Role.USER,
                        content = text
                    )
                    val settings = ChatSettings(
                        systemPrompt = systemPrompt,
                        temperature = temp,
                        maxTokens = 300
                    )
                    val result = sendMessageUseCase(
                        conversationHistory = listOf(userMsg),
                        settings = settings
                    )
                    temp to result
                }
            }

            val results = deferred.map { it.await() }

            _state.update { state ->
                val updatedRounds = state.rounds.map { r ->
                    if (r.id != roundId) return@map r
                    val updatedResponses = results.map { (temp, result) ->
                        result.fold(
                            onSuccess = { msg ->
                                TemperatureResponse(
                                    temperature = temp,
                                    content = msg.content,
                                    isLoading = false
                                )
                            },
                            onFailure = { error ->
                                Log.e("TempChat", "Error at temp=$temp: ${error.message}")
                                TemperatureResponse(
                                    temperature = temp,
                                    content = error.message ?: "Ошибка",
                                    isLoading = false,
                                    isError = true
                                )
                            }
                        )
                    }
                    r.copy(responses = updatedResponses)
                }
                state.copy(rounds = updatedRounds, isAnyLoading = false)
            }

            _effect.send(TemperatureChatEffect.ScrollToBottom)
        }
    }
}
