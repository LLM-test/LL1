package com.example.hellocompose.domain.usecase

import com.example.hellocompose.domain.model.ChatMessage
import com.example.hellocompose.domain.model.ChatSettings
import com.example.hellocompose.domain.repository.ChatRepository

class SendMessageUseCase(private val repository: ChatRepository) {

    suspend operator fun invoke(
        conversationHistory: List<ChatMessage>,
        settings: ChatSettings
    ): Result<ChatMessage> {
        return try {
            val response = repository.sendMessage(conversationHistory, settings)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
