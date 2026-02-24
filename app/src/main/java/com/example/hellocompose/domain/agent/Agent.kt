package com.example.hellocompose.domain.agent

import android.util.Log
import com.example.hellocompose.data.api.ModelComparisonApiService
import com.example.hellocompose.data.api.dto.ChatRequestDto
import com.example.hellocompose.data.api.dto.MessageDto
import com.example.hellocompose.data.repository.AgentHistoryRepository

/**
 * Агент — отдельная сущность, инкапсулирующая логику диалога с LLM.
 *
 * День 7: история диалога сохраняется в Room и восстанавливается при перезапуске.
 * Цикл:
 *   1. Lazy-загрузка истории из БД при первом обращении
 *   2. Отправка запроса в LLM с инструментами
 *   3. Если tool_calls → выполнить инструменты → сохранить → повторить
 *   4. Финальный ответ → сохранить → вернуть результат
 */
class Agent(
    private val apiService: ModelComparisonApiService,
    private val tools: List<AgentTool>,
    private val historyRepository: AgentHistoryRepository
) {
    private val history = mutableListOf<MessageDto>()
    private var historyLoaded = false

    private val systemPrompt = """
        Ты — умный ассистент с доступом к инструментам.
        Используй инструменты, когда нужно точно ответить на вопрос.
        Отвечай на русском языке, кратко и по делу.
    """.trimIndent()

    /** Загружает историю из Room при первом вызове (lazy). */
    private suspend fun ensureHistoryLoaded() {
        if (historyLoaded) return
        val saved = historyRepository.loadHistory()
        history.addAll(saved)
        historyLoaded = true
        Log.d("Agent", "Loaded ${saved.size} messages from DB")
    }

    /** Возвращает текущую историю диалога (загружает из БД если ещё не загружена). */
    suspend fun getHistory(): List<MessageDto> {
        ensureHistoryLoaded()
        return history.toList()
    }

    /** Очищает историю в памяти и в базе данных. */
    suspend fun reset() {
        history.clear()
        historyLoaded = true // помечаем как загруженную (пустую)
        historyRepository.clearHistory()
        Log.d("Agent", "History cleared")
    }

    /** Сохраняет сообщение в память и в Room. */
    private suspend fun addToHistory(message: MessageDto) {
        history.add(message)
        historyRepository.saveMessage(message)
    }

    suspend fun chat(userMessage: String): AgentResult {
        ensureHistoryLoaded()

        val messages = mutableListOf<MessageDto>()
        messages.add(MessageDto(role = "system", content = systemPrompt))
        messages.addAll(history)

        val userMsg = MessageDto(role = "user", content = userMessage)
        messages.add(userMsg)
        addToHistory(userMsg)

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
                    addToHistory(assistantMessage)
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
                        addToHistory(toolMessage)
                    }
                    iterations++
                } else {
                    val answer = assistantMessage.content ?: ""
                    addToHistory(assistantMessage)
                    Log.d("Agent", "Final answer after ${steps.size} tool calls")
                    return@runCatching AgentResult(answer = answer, steps = steps)
                }
            }
            AgentResult(answer = "Превышен лимит итераций агента.", steps = steps)
        }.getOrElse { error ->
            Log.e("Agent", "Error: ${error.message}")
            AgentResult(answer = error.message ?: "Неизвестная ошибка", isError = true)
        }
    }
}
