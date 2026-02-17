package com.example.hellocompose.domain.model

data class ChatSettings(
    val systemPrompt: String = "",
    val temperature: Float = 1.0f,
    val maxTokens: Int = 4096,
    val topP: Float = 1.0f,
    val frequencyPenalty: Float = 0.0f,
    val presencePenalty: Float = 0.0f,
    val stopSequences: List<String> = emptyList()
)
