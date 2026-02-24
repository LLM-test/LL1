package com.example.hellocompose.domain.agent

/** Результат работы агента: финальный ответ + список шагов (вызовы инструментов). */
data class AgentResult(
    val answer: String,
    val steps: List<AgentStep> = emptyList(),
    val isError: Boolean = false
)
