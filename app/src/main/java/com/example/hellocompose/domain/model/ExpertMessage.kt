package com.example.hellocompose.domain.model

/**
 * Одна «запись» в диалоге экспертного чата.
 * Содержит вопрос пользователя и ответы от каждого активного эксперта.
 */
data class ExpertMessage(
    val id: Long = System.currentTimeMillis(),
    val question: String,
    val responses: List<ExpertResponse> = emptyList()
)

/**
 * Ответ конкретного эксперта на вопрос пользователя.
 */
data class ExpertResponse(
    val character: ExpertCharacter,
    val content: String,
    val isLoading: Boolean = false,
    val isError: Boolean = false
)
