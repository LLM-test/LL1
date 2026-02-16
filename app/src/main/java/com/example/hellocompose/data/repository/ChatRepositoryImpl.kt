package com.example.hellocompose.data.repository

import com.example.hellocompose.data.api.ApiConstants
import com.example.hellocompose.data.api.DeepSeekApiService
import com.example.hellocompose.data.api.dto.ChatRequestDto
import com.example.hellocompose.data.api.dto.MessageDto
import com.example.hellocompose.domain.model.ChatMessage
import com.example.hellocompose.domain.repository.ChatRepository

class ChatRepositoryImpl(
    private val apiService: DeepSeekApiService
) : ChatRepository {

    override suspend fun sendMessage(conversationHistory: List<ChatMessage>): ChatMessage {
        val request = ChatRequestDto(
            model = ApiConstants.MODEL,
            messages = conversationHistory
                .filter { it.role != ChatMessage.Role.ERROR }
                .map { msg ->
                    MessageDto(
                        role = when (msg.role) {
                            ChatMessage.Role.USER -> "user"
                            ChatMessage.Role.ASSISTANT -> "assistant"
                            ChatMessage.Role.ERROR -> error("unreachable")
                        },
                        content = msg.content
                    )
                }
        )

        val response = apiService.chatCompletions(request)
        val assistantMessage = response.choices.firstOrNull()?.message
            ?: throw IllegalStateException("Empty response from DeepSeek API")

        return ChatMessage(
            role = ChatMessage.Role.ASSISTANT,
            content = assistantMessage.content
        )
    }
}
