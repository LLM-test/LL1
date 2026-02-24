package com.example.hellocompose.presentation.agent

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hellocompose.data.api.dto.MessageDto
import com.example.hellocompose.domain.agent.Agent
import com.example.hellocompose.domain.agent.AgentStep
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AgentViewModel(private val agent: Agent) : ViewModel() {

    private val _state = MutableStateFlow(AgentState())
    val state: StateFlow<AgentState> = _state.asStateFlow()

    private val _effect = Channel<AgentEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        // Загружаем и отображаем сохранённую историю при старте
        viewModelScope.launch {
            val history = agent.getHistory()
            val uiMessages = history.toUiMessages()
            if (uiMessages.isNotEmpty()) {
                _state.update { it.copy(messages = uiMessages) }
                _effect.send(AgentEffect.ScrollToBottom)
                Log.d("AgentViewModel", "Restored ${uiMessages.size} UI messages from history")
            }
        }
    }

    fun handleIntent(intent: AgentIntent) {
        when (intent) {
            is AgentIntent.TypeMessage -> _state.update { it.copy(inputText = intent.text) }
            is AgentIntent.SendMessage -> sendMessage()
            is AgentIntent.ClearHistory -> {
                viewModelScope.launch {
                    agent.reset()
                    _state.update { it.copy(messages = emptyList(), inputText = "") }
                }
            }
        }
    }

    private fun sendMessage() {
        val text = _state.value.inputText.trim()
        if (text.isBlank() || _state.value.isLoading) return

        val userMsg = AgentMessage.User(text = text)
        val loadingMsg = AgentMessage.Assistant(isLoading = true)

        _state.update {
            it.copy(
                messages = it.messages + userMsg + loadingMsg,
                inputText = "",
                isLoading = true
            )
        }

        viewModelScope.launch {
            _effect.send(AgentEffect.ScrollToBottom)

            val result = agent.chat(text)

            _state.update { state ->
                val updated = state.messages.dropLast(1) + AgentMessage.Assistant(
                    text = result.answer,
                    steps = result.steps,
                    isLoading = false,
                    isError = result.isError
                )
                state.copy(messages = updated, isLoading = false)
            }

            _effect.send(AgentEffect.ScrollToBottom)
            Log.d("AgentViewModel", "Answer received, steps=${result.steps.size}")
        }
    }
}

/**
 * Конвертирует сырую историю API-сообщений в UI-модели для отображения.
 *
 * Алгоритм:
 * - user      → AgentMessage.User
 * - assistant (с tool_calls) → запоминает вызовы инструментов
 * - tool      → формирует AgentStep из имени + аргументов + результата
 * - assistant (без tool_calls) → AgentMessage.Assistant с накопленными шагами
 * - system    → пропускается
 */
private fun List<MessageDto>.toUiMessages(): List<AgentMessage> {
    val result = mutableListOf<AgentMessage>()
    val pendingSteps = mutableListOf<AgentStep>()
    // id → (toolName, arguments) из последнего assistant tool_calls
    val pendingToolCalls = mutableMapOf<String, Pair<String, String>>()

    for (msg in this) {
        when (msg.role) {
            "system" -> {}

            "user" -> {
                pendingSteps.clear()
                pendingToolCalls.clear()
                result.add(AgentMessage.User(text = msg.content ?: ""))
            }

            "assistant" -> {
                if (!msg.toolCalls.isNullOrEmpty()) {
                    // Запоминаем вызовы — результаты придут в следующих "tool" сообщениях
                    msg.toolCalls.forEach { call ->
                        pendingToolCalls[call.id] = call.function.name to call.function.arguments
                    }
                } else {
                    // Финальный ответ — создаём карточку с накопленными шагами
                    result.add(
                        AgentMessage.Assistant(
                            text = msg.content ?: "",
                            steps = pendingSteps.toList()
                        )
                    )
                    pendingSteps.clear()
                    pendingToolCalls.clear()
                }
            }

            "tool" -> {
                val (toolName, arguments) = pendingToolCalls[msg.toolCallId]
                    ?: ("unknown_tool" to "{}")
                pendingSteps.add(
                    AgentStep(
                        toolName = toolName,
                        arguments = arguments,
                        result = msg.content ?: ""
                    )
                )
            }
        }
    }
    return result
}
