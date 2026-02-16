package com.example.hellocompose.domain.repository

import com.example.hellocompose.domain.model.ChatMessage

interface ChatRepository {
    suspend fun sendMessage(conversationHistory: List<ChatMessage>): ChatMessage
}
