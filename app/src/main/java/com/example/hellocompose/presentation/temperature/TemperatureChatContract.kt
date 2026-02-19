package com.example.hellocompose.presentation.temperature

import com.example.hellocompose.domain.model.TemperatureRound

data class TemperatureChatState(
    val rounds: List<TemperatureRound> = emptyList(),
    val inputText: String = "",
    val isAnyLoading: Boolean = false
)

sealed interface TemperatureChatIntent {
    data class TypeMessage(val text: String) : TemperatureChatIntent
    data object SendMessage : TemperatureChatIntent
    data object ClearHistory : TemperatureChatIntent
}

sealed interface TemperatureChatEffect {
    data object ScrollToBottom : TemperatureChatEffect
}
