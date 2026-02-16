package com.example.hellocompose.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatResponseDto(
    @SerialName("choices")
    val choices: List<ChoiceDto>
)

@Serializable
data class ChoiceDto(
    @SerialName("message")
    val message: MessageDto
)
