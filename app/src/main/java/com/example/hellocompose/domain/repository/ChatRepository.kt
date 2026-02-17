package com.example.hellocompose.domain.repository

import com.example.hellocompose.domain.model.ChatMessage
import com.example.hellocompose.domain.model.ChatSettings

interface ChatRepository {
    suspend fun sendMessage(
        conversationHistory: List<ChatMessage>,
        settings: ChatSettings
    ): ChatMessage
}
