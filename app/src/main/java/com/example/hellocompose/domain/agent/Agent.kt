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
 * День 8: подсчёт токенов — per-turn и накопительно по сессии.
 *
 * Цикл:
 *   1. Lazy-загрузка истории из БД при первом обращении
 *   2. Отправка запроса в LLM с инструментами
 *   3. Если tool_calls → выполнить инструменты → сохранить → повторить
 *   4. Финальный ответ → сохранить → вернуть результат + TokenInfo
 */
class Agent(
    private val apiService: ModelComparisonApiService,
    private val tools: List<AgentTool>,
    private val historyRepository: AgentHistoryRepository
) {
    companion object {
        /** Контекстное окно deepseek-chat: 128K токенов. */
        const val CONTEXT_LIMIT = 131_072

        /** Цена deepseek-chat: $0.14 за млн входных токенов. */
        private const val PRICE_INPUT = 0.14 / 1_000_000.0

        /** Цена deepseek-chat: $0.28 за млн выходных токенов. */
        private const val PRICE_OUTPUT = 0.28 / 1_000_000.0
    }

    private val history = mutableListOf<MessageDto>()
    private var historyLoaded = false

    // Накопительная статистика за сессию (сбрасывается при reset)
    private var sessionPromptTokens = 0
    private var sessionCompletionTokens = 0

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

    /** Статистика токенов накопительно за текущую сессию. */
    fun getSessionTokens(): Pair<Int, Int> = sessionPromptTokens to sessionCompletionTokens

    /** Очищает историю в памяти, в БД и сбрасывает статистику сессии. */
    suspend fun reset() {
        history.clear()
        historyLoaded = true
        sessionPromptTokens = 0
        sessionCompletionTokens = 0
        historyRepository.clearHistory()
        Log.d("Agent", "History and session stats cleared")
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

        // Накапливаем токены по всем итерациям цикла агента (tool_calls)
        var turnPromptTokens = 0
        var turnCompletionTokens = 0

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

                // Суммируем токены каждого API-вызова (в цикле tool_calls их может быть несколько)
                response.usage?.let { usage ->
                    turnPromptTokens += usage.promptTokens
                    turnCompletionTokens += usage.completionTokens
                    Log.d("Agent", "API call tokens: prompt=${usage.promptTokens}, completion=${usage.completionTokens}")
                }

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

                    // Обновляем накопительную статистику сессии
                    sessionPromptTokens += turnPromptTokens
                    sessionCompletionTokens += turnCompletionTokens

                    val cost = turnPromptTokens * PRICE_INPUT + turnCompletionTokens * PRICE_OUTPUT
                    val tokenInfo = TokenInfo(
                        promptTokens = turnPromptTokens,
                        completionTokens = turnCompletionTokens,
                        costUsd = cost
                    )

                    Log.d(
                        "Agent",
                        "Turn done: prompt=$turnPromptTokens, completion=$turnCompletionTokens, " +
                            "cost=\$${String.format("%.6f", cost)}, steps=${steps.size}"
                    )

                    return@runCatching AgentResult(answer = answer, steps = steps, tokenInfo = tokenInfo)
                }
            }
            AgentResult(answer = "Превышен лимит итераций агента.", steps = steps)
        }.getOrElse { error ->
            Log.e("Agent", "Error: ${error.message}")
            // Проверяем, является ли ошибка переполнением контекста
            val isContextOverflow = error.message?.contains("context_length", ignoreCase = true) == true ||
                error.message?.contains("maximum context", ignoreCase = true) == true
            val errorMsg = if (isContextOverflow) {
                "⛔ Контекст переполнен! Диалог превысил лимит модели (128K токенов).\n" +
                    "Очистите историю кнопкой 🗑 и начните новый диалог."
            } else {
                error.message ?: "Неизвестная ошибка"
            }
            AgentResult(answer = errorMsg, isError = true)
        }
    }
}
