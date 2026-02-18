package com.example.hellocompose.domain.model

import com.example.hellocompose.data.api.dto.QuizResponseDto

data class ChatMessage(
    val role: Role,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    // Для quiz-режима — распарсенный DTO; null для обычных сообщений
    val quizData: QuizResponseDto? = null,
    // Ключ варианта, который выбрал пользователь (A/B/C/D); null — ещё не отвечал
    val selectedOption: String? = null
) {
    enum class Role { USER, ASSISTANT, ERROR }
}
