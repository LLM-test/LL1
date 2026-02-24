package com.example.hellocompose.presentation.agent

import com.example.hellocompose.domain.agent.AgentStep
import java.util.concurrent.atomic.AtomicLong

private val idCounter = AtomicLong(0)
private fun nextId() = idCounter.incrementAndGet()

data class AgentState(
    val messages: List<AgentMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false
)

sealed class AgentMessage {
    abstract val id: Long

    data class User(
        val text: String,
        override val id: Long = nextId()
    ) : AgentMessage()

    data class Assistant(
        val text: String = "",
        val steps: List<AgentStep> = emptyList(),
        val isLoading: Boolean = false,
        val isError: Boolean = false,
        override val id: Long = nextId()
    ) : AgentMessage()
}

sealed class AgentIntent {
    data class TypeMessage(val text: String) : AgentIntent()
    object SendMessage : AgentIntent()
    object ClearHistory : AgentIntent()
}

sealed class AgentEffect {
    object ScrollToBottom : AgentEffect()
}
