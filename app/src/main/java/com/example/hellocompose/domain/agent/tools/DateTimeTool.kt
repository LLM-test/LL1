package com.example.hellocompose.domain.agent.tools

import com.example.hellocompose.data.api.dto.FunctionDefinitionDto
import com.example.hellocompose.data.api.dto.ParametersDto
import com.example.hellocompose.data.api.dto.ToolDefinitionDto
import com.example.hellocompose.domain.agent.AgentTool
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Инструмент: возвращает текущую дату и время устройства. */
class DateTimeTool : AgentTool {

    override val name = "get_current_datetime"

    override val definition = ToolDefinitionDto(
        function = FunctionDefinitionDto(
            name = name,
            description = "Возвращает текущую дату и время на устройстве пользователя.",
            parameters = ParametersDto()
        )
    )

    override suspend fun execute(argumentsJson: String): String {
        val fmt = SimpleDateFormat("d MMMM yyyy, HH:mm:ss", Locale("ru"))
        return "Текущая дата и время: ${fmt.format(Date())}"
    }
}
