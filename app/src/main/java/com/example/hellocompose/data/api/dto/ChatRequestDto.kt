package com.example.hellocompose.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatRequestDto(
    @SerialName("model")
    val model: String,
    @SerialName("messages")
    val messages: List<MessageDto>,
    @SerialName("temperature")
    val temperature: Float? = null,
    @SerialName("max_tokens")
    val maxTokens: Int? = null,
    @SerialName("top_p")
    val topP: Float? = null,
    @SerialName("frequency_penalty")
    val frequencyPenalty: Float? = null,
    @SerialName("presence_penalty")
    val presencePenalty: Float? = null,
    @SerialName("stop")
    val stop: List<String>? = null
)

@Serializable
data class MessageDto(
    @SerialName("role")
    val role: String,
    @SerialName("content")
    val content: String
)
