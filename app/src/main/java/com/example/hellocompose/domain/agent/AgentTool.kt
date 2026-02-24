package com.example.hellocompose.domain.agent

import com.example.hellocompose.data.api.dto.ToolDefinitionDto

/** Интерфейс инструмента агента. Каждый инструмент умеет описать себя и выполниться. */
interface AgentTool {
    val name: String
    val definition: ToolDefinitionDto
    suspend fun execute(argumentsJson: String): String
}
