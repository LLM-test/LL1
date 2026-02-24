package com.example.hellocompose.presentation.agent

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hellocompose.domain.agent.Agent
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

    fun handleIntent(intent: AgentIntent) {
        when (intent) {
            is AgentIntent.TypeMessage -> _state.update { it.copy(inputText = intent.text) }
            is AgentIntent.SendMessage -> sendMessage()
            is AgentIntent.ClearHistory -> {
                agent.reset()
                _state.update { it.copy(messages = emptyList(), inputText = "") }
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
