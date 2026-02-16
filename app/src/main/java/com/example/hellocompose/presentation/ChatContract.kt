package com.example.hellocompose.presentation

import com.example.hellocompose.domain.model.ChatMessage

data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false
)

sealed interface ChatIntent {
    data class TypeMessage(val text: String) : ChatIntent
    data object SendMessage : ChatIntent
}

sealed interface ChatEffect {
    data object ScrollToBottom : ChatEffect
}
