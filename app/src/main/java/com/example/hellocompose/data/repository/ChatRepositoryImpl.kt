package com.example.hellocompose.data.repository

import com.example.hellocompose.data.api.ApiConstants
import com.example.hellocompose.data.api.DeepSeekApiService
import com.example.hellocompose.data.api.dto.ChatRequestDto
import com.example.hellocompose.data.api.dto.MessageDto
import com.example.hellocompose.domain.model.ChatMessage
import com.example.hellocompose.domain.model.ChatSettings
import com.example.hellocompose.domain.repository.ChatRepository

class ChatRepositoryImpl(
    private val apiService: DeepSeekApiService
) : ChatRepository {

    override suspend fun sendMessage(
        conversationHistory: List<ChatMessage>,
        settings: ChatSettings
    ): ChatMessage {
        val apiMessages = buildList {
            // Системный промт — первое сообщение
            if (settings.systemPrompt.isNotBlank()) {
                add(MessageDto(role = "system", content = settings.systemPrompt))
            }
            // История сообщений (без ERROR)
            conversationHistory
                .filter { it.role != ChatMessage.Role.ERROR }
                .forEach { msg ->
                    add(
                        MessageDto(
                            role = when (msg.role) {
                                ChatMessage.Role.USER -> "user"
                                ChatMessage.Role.ASSISTANT -> "assistant"
                                ChatMessage.Role.ERROR -> error("unreachable")
                            },
                            content = msg.content
                        )
                    )
                }
        }

        val request = ChatRequestDto(
            model = ApiConstants.MODEL,
            messages = apiMessages,
            temperature = settings.temperature,
            maxTokens = settings.maxTokens,
            topP = settings.topP,
            frequencyPenalty = settings.frequencyPenalty,
            presencePenalty = settings.presencePenalty,
            stop = settings.stopSequences.ifEmpty { null }
        )

        val response = apiService.chatCompletions(request)
        val assistantMessage = response.choices.firstOrNull()?.message
            ?: throw IllegalStateException("Empty response from DeepSeek API")

        return ChatMessage(
            role = ChatMessage.Role.ASSISTANT,
            content = assistantMessage.content.orEmpty()
        )
    }
}
