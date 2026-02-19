package com.example.hellocompose.domain.model

/**
 * Один раунд сравнения температур:
 * один вопрос пользователя → три ответа с разными temperature.
 */
data class TemperatureRound(
    val id: Long = System.currentTimeMillis(),
    val question: String,
    val responses: List<TemperatureResponse> = emptyList()
)

/**
 * Ответ AI при конкретном значении temperature.
 */
data class TemperatureResponse(
    val temperature: Float,
    val content: String = "",
    val isLoading: Boolean = false,
    val isError: Boolean = false
)
