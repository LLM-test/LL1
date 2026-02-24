package com.example.hellocompose.domain.agent

/** Один шаг агента — вызов инструмента с аргументами и результатом. */
data class AgentStep(
    val toolName: String,
    val arguments: String,
    val result: String
)
