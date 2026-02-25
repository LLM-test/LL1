package com.example.hellocompose.domain.agent

/**
 * Статистика токенов за один обмен (один вызов chat()).
 * prompt_tokens уже включает всю историю диалога — именно по нему
 * измеряем заполнение контекстного окна модели.
 */
data class TokenInfo(
    val promptTokens: Int = 0,      // токены запроса (история + текущий вопрос)
    val completionTokens: Int = 0,  // токены ответа
    val costUsd: Double = 0.0       // стоимость в USD
) {
    val totalTokens: Int get() = promptTokens + completionTokens
}

/** Результат работы агента: финальный ответ + список шагов (вызовы инструментов). */
data class AgentResult(
    val answer: String,
    val steps: List<AgentStep> = emptyList(),
    val isError: Boolean = false,
    val tokenInfo: TokenInfo = TokenInfo()
)
