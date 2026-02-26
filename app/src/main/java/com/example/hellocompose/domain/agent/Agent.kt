package com.example.hellocompose.domain.agent

import android.util.Log
import com.example.hellocompose.data.api.ModelComparisonApiService
import com.example.hellocompose.data.api.dto.ChatRequestDto
import com.example.hellocompose.data.api.dto.MessageDto
import com.example.hellocompose.data.repository.AgentHistoryRepository

/**
 * Агент — отдельная сущность, инкапсулирующая логику диалога с LLM.
 *
 * День 7: история сохраняется в Room.
 * День 8: подсчёт токенов.
 * День 9: сжатие контекста.
 *   - Последние [RECENT_WINDOW] сообщений всегда передаются verbatim.
 *   - Когда некомпрессированных сообщений становится > RECENT_WINDOW + COMPRESS_EVERY,
 *     старый пакет ([COMPRESS_EVERY] сообщений) заменяется summary.
 *   - Summary хранится в Room и восстанавливается при перезапуске.
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

        /** Сколько последних сообщений всегда передаётся verbatim. */
        const val RECENT_WINDOW = 6

        /** Пакет сжатия: каждые N сообщений превращаются в summary. */
        const val COMPRESS_EVERY = 6
    }

    private val history = mutableListOf<MessageDto>()
    private var historyLoaded = false

    /** Сжатое резюме старой части диалога. */
    private var summary: String = ""

    /** Сколько сообщений из начала истории уже покрыто summary. */
    private var coveredCount: Int = 0

    // Накопительная статистика за сессию
    private var sessionPromptTokens = 0
    private var sessionCompletionTokens = 0

    private val systemPrompt = """
        Ты — умный ассистент с доступом к инструментам.
        Используй инструменты, когда нужно точно ответить на вопрос.
        Отвечай на русском языке, кратко и по делу.
    """.trimIndent()

    // ── Инициализация ─────────────────────────────────────────────────────────

    private suspend fun ensureHistoryLoaded() {
        if (historyLoaded) return
        val saved = historyRepository.loadHistory()
        history.addAll(saved)
        val (savedSummary, savedCoveredCount) = historyRepository.loadContext()
        summary = savedSummary
        coveredCount = savedCoveredCount
        historyLoaded = true
        Log.d("Agent", "Loaded ${saved.size} messages, coveredCount=$coveredCount, " +
            "hasSummary=${summary.isNotBlank()}")
    }

    suspend fun getHistory(): List<MessageDto> {
        ensureHistoryLoaded()
        return history.toList()
    }

    // ── Статистика контекста ──────────────────────────────────────────────────

    data class ContextStats(
        val compressedCount: Int = 0,   // сообщений покрыто summary
        val recentCount: Int = 0,       // сообщений передаётся verbatim
        val isSummaryActive: Boolean = false,
        val summaryLength: Int = 0      // длина текста резюме (символов)
    )

    suspend fun getContextStats(): ContextStats {
        ensureHistoryLoaded()
        return ContextStats(
            compressedCount = coveredCount,
            recentCount = history.size - coveredCount,
            isSummaryActive = summary.isNotBlank(),
            summaryLength = summary.length
        )
    }

    // ── Сброс ─────────────────────────────────────────────────────────────────

    suspend fun reset() {
        history.clear()
        summary = ""
        coveredCount = 0
        historyLoaded = true
        sessionPromptTokens = 0
        sessionCompletionTokens = 0
        historyRepository.clearHistory()
        Log.d("Agent", "History, summary and session stats cleared")
    }

    // ── История ───────────────────────────────────────────────────────────────

    private suspend fun addToHistory(message: MessageDto) {
        history.add(message)
        historyRepository.saveMessage(message)
    }

    // ── Сжатие контекста ─────────────────────────────────────────────────────

    /**
     * Сжимает старые сообщения в summary пока
     * некомпрессированных > [RECENT_WINDOW] + [COMPRESS_EVERY].
     *
     * Один вызов сжимает ровно [COMPRESS_EVERY] сообщений.
     * Цикл обрабатывает несколько пакетов подряд (например при загрузке длинной истории из Room).
     */
    private suspend fun maybeCompress() {
        while (history.size - coveredCount > RECENT_WINDOW + COMPRESS_EVERY) {
            val batch = history.subList(coveredCount, coveredCount + COMPRESS_EVERY)
            Log.d("Agent", "Compressing messages [$coveredCount..${coveredCount + COMPRESS_EVERY - 1}]")
            summary = generateSummary(summary, batch)
            coveredCount += COMPRESS_EVERY
            historyRepository.saveContext(summary, coveredCount)
            Log.d("Agent", "Compression done: coveredCount=$coveredCount, summaryLen=${summary.length}")
        }
    }

    /**
     * Генерирует обновлённое резюме на основе существующего + новой порции сообщений.
     * Использует LLM с низкой температурой для стабильного пересказа.
     */
    private suspend fun generateSummary(existingSummary: String, messages: List<MessageDto>): String {
        val prompt = buildString {
            if (existingSummary.isNotBlank()) {
                appendLine("Существующее резюме диалога:")
                appendLine(existingSummary)
                appendLine()
            }
            appendLine("Сообщения для включения в резюме:")
            messages.forEach { msg ->
                when (msg.role) {
                    "user" -> appendLine("Пользователь: ${msg.content}")
                    "assistant" -> if (!msg.content.isNullOrBlank()) appendLine("Ассистент: ${msg.content}")
                    "tool" -> appendLine("Инструмент вернул: ${msg.content}")
                }
            }
            append("\nСоздай краткое резюме диалога (до 150 слов). " +
                "Сохрани ключевые факты, вопросы и ответы. Только текст резюме, без предисловий.")
        }

        val request = ChatRequestDto(
            model = "deepseek-chat",
            messages = listOf(MessageDto(role = "user", content = prompt)),
            maxTokens = 400,
            temperature = 0.2f
        )

        return try {
            val result = apiService.chatCompletions(request).choices.first().message.content
            Log.d("Agent", "Summary generated: ${result?.length} chars")
            result ?: existingSummary
        } catch (e: Exception) {
            Log.e("Agent", "Summary generation failed: ${e.message}")
            existingSummary // при ошибке оставляем старое резюме
        }
    }

    // ── Основной диалог ───────────────────────────────────────────────────────

    suspend fun chat(userMessage: String): AgentResult {
        ensureHistoryLoaded()
        maybeCompress()  // сжимаем старую историю перед отправкой запроса

        // Строим список сообщений: [system] + [summary?] + [recent messages]
        val messages = mutableListOf<MessageDto>()
        messages.add(MessageDto(role = "system", content = systemPrompt))

        if (summary.isNotBlank()) {
            messages.add(
                MessageDto(
                    role = "system",
                    content = "📝 Краткое изложение предыдущего диалога:\n$summary"
                )
            )
        }

        // Только recent-сообщения (не покрытые summary)
        messages.addAll(history.drop(coveredCount))

        val userMsg = MessageDto(role = "user", content = userMessage)
        messages.add(userMsg)
        addToHistory(userMsg)

        val toolDefs = tools.map { it.definition }.takeIf { it.isNotEmpty() }
        val steps = mutableListOf<AgentStep>()

        var lastPromptTokens = 0
        var totalCompletionTokens = 0
        var totalCostAccumulator = 0.0

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

                response.usage?.let { usage ->
                    lastPromptTokens = usage.promptTokens
                    totalCompletionTokens += usage.completionTokens
                    totalCostAccumulator += usage.promptTokens * PRICE_INPUT +
                        usage.completionTokens * PRICE_OUTPUT
                    Log.d("Agent", "API call: prompt=${usage.promptTokens}, completion=${usage.completionTokens}")
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

                    sessionPromptTokens = lastPromptTokens
                    sessionCompletionTokens += totalCompletionTokens

                    val tokenInfo = TokenInfo(
                        promptTokens = lastPromptTokens,
                        completionTokens = totalCompletionTokens,
                        costUsd = totalCostAccumulator
                    )

                    Log.d("Agent",
                        "Turn done: prompt=$lastPromptTokens, completion=$totalCompletionTokens, " +
                            "cost=\$${String.format("%.6f", totalCostAccumulator)}, steps=${steps.size}, " +
                            "compressed=$coveredCount, recent=${history.size - coveredCount}")

                    return@runCatching AgentResult(answer = answer, steps = steps, tokenInfo = tokenInfo)
                }
            }
            AgentResult(answer = "Превышен лимит итераций агента.", steps = steps)
        }.getOrElse { error ->
            Log.e("Agent", "Error: ${error.message}")
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
