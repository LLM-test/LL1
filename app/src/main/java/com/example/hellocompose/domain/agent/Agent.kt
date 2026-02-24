package com.example.hellocompose.domain.agent

import com.example.hellocompose.data.api.ModelComparisonApiService
import com.example.hellocompose.data.api.dto.ChatRequestDto
import com.example.hellocompose.data.api.dto.MessageDto

/**
 * Агент — отдельная сущность, инкапсулирующая логику диалога с LLM.
 *
 * Поддерживает многошаговый цикл:
 *   1. Отправляет сообщение пользователя в LLM вместе с описаниями инструментов
 *   2. Если LLM вызывает инструмент — выполняет его и отправляет результат обратно
 *   3. Повторяет до финального ответа (finish_reason = "stop")
 */
class Agent(
    private val apiService: ModelComparisonApiService,
    private val tools: List<AgentTool>
) {
    private val history = mutableListOf<MessageDto>()

    private val systemPrompt = """
        Ты — умный ассистент с доступом к инструментам.
        Используй инструменты, когда нужно точно ответить на вопрос.
        Отвечай на русском языке, кратко и по делу.
    """.trimIndent()

    fun reset() = history.clear()

    suspend fun chat(userMessage: String): AgentResult {
        val messages = mutableListOf<MessageDto>()
        messages.add(MessageDto(role = "system", content = systemPrompt))
        messages.addAll(history)
        messages.add(MessageDto(role = "user", content = userMessage))
        history.add(MessageDto(role = "user", content = userMessage))

        val toolDefs = tools.map { it.definition }.takeIf { it.isNotEmpty() }
        val steps = mutableListOf<AgentStep>()

        return runCatching {
            var iterations = 0
            while (iterations < 5) {
                val request = ChatRequestDto(
                    model = "deepseek-chat",
                    messages = messages.toList(),
                    temperature = 0.7f,
                    maxTokens = 1000,
                    tools = toolDefs
                )
                val response = apiService.chatCompletions(request)
                val choice = response.choices.first()
                val assistantMessage = choice.message
                messages.add(assistantMessage)

                if (choice.finishReason == "tool_calls" && !assistantMessage.toolCalls.isNullOrEmpty()) {
                    history.add(assistantMessage)
                    for (toolCall in assistantMessage.toolCalls) {
                        val tool = tools.find { it.name == toolCall.function.name }
                        val result = tool?.execute(toolCall.function.arguments)
                            ?: "Инструмент '${toolCall.function.name}' не найден"
                        steps.add(AgentStep(toolCall.function.name, toolCall.function.arguments, result))
                        val toolMessage = MessageDto(
                            role = "tool",
                            content = result,
                            toolCallId = toolCall.id
                        )
                        messages.add(toolMessage)
                        history.add(toolMessage)
                    }
                    iterations++
                } else {
                    val answer = assistantMessage.content ?: ""
                    history.add(assistantMessage)
                    return@runCatching AgentResult(answer = answer, steps = steps)
                }
            }
            AgentResult(answer = "Превышен лимит итераций агента.", steps = steps)
        }.getOrElse { error ->
            AgentResult(answer = error.message ?: "Неизвестная ошибка", isError = true)
        }
    }
}
