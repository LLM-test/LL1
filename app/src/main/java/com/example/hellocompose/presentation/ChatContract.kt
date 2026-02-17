package com.example.hellocompose.presentation

import com.example.hellocompose.domain.model.ChatMessage
import com.example.hellocompose.domain.model.ChatSettings
import com.example.hellocompose.domain.model.QuizConfig

data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val settings: ChatSettings = ChatSettings(),
    val quizActive: Boolean = false
)

sealed interface ChatIntent {
    data class TypeMessage(val text: String) : ChatIntent
    data object SendMessage : ChatIntent
    data class UpdateSettings(val settings: ChatSettings) : ChatIntent
    data class StartQuiz(val config: QuizConfig) : ChatIntent
    data class SelectQuizOption(val optionKey: String, val optionText: String) : ChatIntent
}

sealed interface ChatEffect {
    data object ScrollToBottom : ChatEffect
}
