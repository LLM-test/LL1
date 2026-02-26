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
        viewModelScope.launch {
            val history = agent.getHistory()
            val uiMessages = history.toUiMessages()
            val ctxStats = agent.getContextStats().toUiStats()
            if (uiMessages.isNotEmpty()) {
                _state.update { it.copy(messages = uiMessages, contextStats = ctxStats) }
                _effect.send(AgentEffect.ScrollToBottom)
                Log.d("AgentViewModel", "Restored ${uiMessages.size} UI messages, $ctxStats")
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
                    _state.update {
                        it.copy(
                            messages = emptyList(),
                            inputText = "",
                            sessionStats = SessionStats(),
                            contextStats = ContextStats()
                        )
                    }
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
            val ctxStats = agent.getContextStats().toUiStats()

            _state.update { state ->
                val updatedStats = state.sessionStats.copy(
                    lastPromptTokens = result.tokenInfo.promptTokens,
                    totalCostUsd = state.sessionStats.totalCostUsd + result.tokenInfo.costUsd,
                    totalExchanges = state.sessionStats.totalExchanges + 1
                )
                val updatedMessages = state.messages.dropLast(1) + AgentMessage.Assistant(
                    text = result.answer,
                    steps = result.steps,
                    isLoading = false,
                    isError = result.isError,
                    tokenInfo = result.tokenInfo
                )
                state.copy(
                    messages = updatedMessages,
                    isLoading = false,
                    sessionStats = updatedStats,
                    contextStats = ctxStats
                )
            }

            _effect.send(AgentEffect.ScrollToBottom)
            Log.d("AgentViewModel",
                "Answer: steps=${result.steps.size}, prompt=${result.tokenInfo.promptTokens}, " +
                    "completion=${result.tokenInfo.completionTokens}, ctx=$ctxStats")
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun Agent.ContextStats.toUiStats() = ContextStats(
        compressedCount = compressedCount,
        recentCount = recentCount,
        isSummaryActive = isSummaryActive,
        summaryLength = summaryLength
    )
}

/**
 * Конвертирует сырую историю API-сообщений в UI-модели.
 * tokenInfo не восстанавливается из Room (не хранится) — будет null у старых сообщений.
 */
private fun List<MessageDto>.toUiMessages(): List<AgentMessage> {
    val result = mutableListOf<AgentMessage>()
    val pendingSteps = mutableListOf<AgentStep>()
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
                    msg.toolCalls.forEach { call ->
                        pendingToolCalls[call.id] = call.function.name to call.function.arguments
                    }
                } else {
                    result.add(
                        AgentMessage.Assistant(
                            text = msg.content ?: "",
                            steps = pendingSteps.toList(),
                            tokenInfo = null
                        )
                    )
                    pendingSteps.clear()
                    pendingToolCalls.clear()
                }
            }

            "tool" -> {
                val (toolName, arguments) = pendingToolCalls[msg.toolCallId]
                    ?: ("unknown_tool" to "{}")
                pendingSteps.add(AgentStep(toolName, arguments, msg.content ?: ""))
            }
        }
    }
    return result
}
