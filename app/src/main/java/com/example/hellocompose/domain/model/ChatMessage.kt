package com.example.hellocompose.domain.model

import com.example.hellocompose.data.api.dto.QuizResponseDto

data class ChatMessage(
    val role: Role,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    // Для quiz-режима — распарсенный DTO; null для обычных сообщений
    val quizData: QuizResponseDto? = null
) {
    // Вычисляемое свойство для совместимости с QuizOptionsBubble
    val options: List<String>?
        get() = (quizData as? QuizResponseDto.Question)
            ?.options?.toList()
            ?.map { (key, value) -> "$key: $value" }

    enum class Role { USER, ASSISTANT, ERROR }
}
