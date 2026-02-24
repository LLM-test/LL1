package com.example.hellocompose.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatResponseDto(
    @SerialName("choices")
    val choices: List<ChoiceDto>,
    @SerialName("usage")
    val usage: UsageDto? = null
)

@Serializable
data class UsageDto(
    @SerialName("prompt_tokens")
    val promptTokens: Int = 0,
    @SerialName("completion_tokens")
    val completionTokens: Int = 0,
    @SerialName("total_tokens")
    val totalTokens: Int = 0
)

@Serializable
data class ChoiceDto(
    @SerialName("message")
    val message: MessageDto,
    @SerialName("finish_reason")
    val finishReason: String? = null
)
