package com.example.hellocompose.domain.agent

import android.util.Log
import com.example.hellocompose.data.api.ModelComparisonApiService
import com.example.hellocompose.data.api.dto.ChatRequestDto
import com.example.hellocompose.data.api.dto.MessageDto
import com.example.hellocompose.data.repository.AgentHistoryRepository

/**
 * Агент с поддержкой трёх стратегий управления контекстом (День 10):
 *
 *  • [ContextStrategy.SlidingWindow] — последние N сообщений verbatim, остальное отброшено
 *  • [ContextStrategy.StickyFacts]  — ключевые факты + последние N сообщений
 *  • [ContextStrategy.Branching]    — чекпоинт + независимые ветки
 *
 * День 9 (Summary/maybeCompress) сохранён, но не вызывается при Day-10 стратегиях.
 */
class Agent(
    private val apiService: ModelComparisonApiService,
    private val tools: List<AgentTool>,
    private val historyRepository: AgentHistoryRepository
) {
    companion object {
        const val CONTEXT_LIMIT = 131_072
        private const val PRICE_INPUT  = 0.14 / 1_000_000.0
        private const val PRICE_OUTPUT = 0.28 / 1_000_000.0

        // Day 9 (Summary)
        const val RECENT_WINDOW  = 6
        const val COMPRESS_EVERY = 6
    }

    // ── Базовая история (персистентна в Room) ─────────────────────────────────
    private val history = mutableListOf<MessageDto>()
    private var historyLoaded = false

    // ── Day 9: Summary ────────────────────────────────────────────────────────
    private var summary = ""
    private var coveredCount = 0

    // ── Day 10: Стратегия ──────────────────────────────────────────────────────
    var activeStrategy: ContextStrategy = ContextStrategy.SlidingWindow()
        private set

    // StickyFacts — факты персистентны в Room
    val facts = LinkedHashMap<String, String>()

    // Branching — ветки хранятся только в памяти (in-memory)
    data class BranchData(
        val id: String,
        val name: String,
        val messages: MutableList<MessageDto> = mutableListOf()
    )
    var checkpointHistory: List<MessageDto> = emptyList()
        private set
    val branches = mutableListOf<BranchData>()
    var activeBranchId: String = "main"
        private set

    private var sessionPromptTokens = 0
    private var sessionCompletionTokens = 0

    private val systemPrompt = """
        Ты — умный ассистент с доступом к инструментам.
        Используй инструменты, когда нужно точно ответить на вопрос.
        Отвечай на русском языке, кратко и по делу.
    """.trimIndent()

    // ── Загрузка ───────────────────────────────────────────────────────────────

    private suspend fun ensureHistoryLoaded() {
        if (historyLoaded) return
        val saved = historyRepository.loadHistory()
        history.addAll(saved)
        val (savedSummary, savedCoveredCount) = historyRepository.loadContext()
        summary = savedSummary
        coveredCount = savedCoveredCount
        val savedFacts = historyRepository.loadFacts()
        facts.putAll(savedFacts)
        historyLoaded = true
        Log.d("Agent", "Loaded: ${saved.size} msgs, coveredCount=$coveredCount, facts=${facts.size}")
    }

    suspend fun getHistory(): List<MessageDto> {
        ensureHistoryLoaded()
        return history.toList()
    }

    // ── Статистика (Day 9 совместимость) ──────────────────────────────────────

    data class ContextStats(
        val compressedCount: Int = 0,
        val recentCount: Int = 0,
        val isSummaryActive: Boolean = false,
        val summaryLength: Int = 0
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

    // ── Управление стратегией ─────────────────────────────────────────────────

    /** Переключает стратегию, сбрасывая strategy-specific state (факты, ветки). */
    suspend fun setStrategy(strategy: ContextStrategy) {
        ensureHistoryLoaded()
        activeStrategy = strategy
        facts.clear()
        historyRepository.clearFacts()
        checkpointHistory = emptyList()
        branches.clear()
        activeBranchId = "main"
        Log.d("Agent", "Strategy → ${strategy.displayName}")
    }

    // ── Branching API ──────────────────────────────────────────────────────────

    /** Сохраняет текущую историю как чекпоинт, создаёт ветку "main". */
    fun saveCheckpoint() {
        checkpointHistory = history.toList()
        branches.clear()
        branches.add(BranchData(id = "main", name = "Основная"))
        activeBranchId = "main"
        Log.d("Agent", "Checkpoint saved at ${checkpointHistory.size} messages")
    }

    /** Создаёт новую ветку от чекпоинта и делает её активной. */
    fun createBranch(name: String): BranchData {
        val branch = BranchData(id = "branch_${System.currentTimeMillis()}", name = name)
        branches.add(branch)
        activeBranchId = branch.id
        Log.d("Agent", "Branch created: '${branch.name}' id=${branch.id}")
        return branch
    }

    /** Переключается на указанную ветку. */
    fun switchBranch(branchId: String) {
        activeBranchId = branchId
        Log.d("Agent", "Switched to branch: $branchId")
    }

    /** Полная история активной ветки (для UI и построения контекста). */
    fun getActiveBranchHistory(): List<MessageDto> {
        if (activeStrategy !is ContextStrategy.Branching || checkpointHistory.isEmpty()) {
            return history.toList()
        }
        val branchMsgs = branches.find { it.id == activeBranchId }?.messages ?: emptyList()
        return checkpointHistory + branchMsgs
    }

    // ── Сброс ──────────────────────────────────────────────────────────────────

    suspend fun reset() {
        history.clear()
        summary = ""; coveredCount = 0
        facts.clear()
        checkpointHistory = emptyList(); branches.clear(); activeBranchId = "main"
        historyLoaded = true
        sessionPromptTokens = 0; sessionCompletionTokens = 0
        historyRepository.clearHistory()
        Log.d("Agent", "Full reset")
    }

    // ── Добавление в историю ───────────────────────────────────────────────────

    private suspend fun addToHistory(message: MessageDto) {
        if (activeStrategy is ContextStrategy.Branching && checkpointHistory.isNotEmpty()) {
            // В режиме веток — в текущую ветку (in-memory, без Room)
            branches.find { it.id == activeBranchId }?.messages?.add(message)
        } else {
            history.add(message)
            historyRepository.saveMessage(message)
        }
    }

    // ── Построение контекста по стратегии ─────────────────────────────────────

    private fun buildContextMessages(): List<MessageDto> {
        val msgs = mutableListOf<MessageDto>()
        msgs.add(MessageDto(role = "system", content = systemPrompt))

        when (val s = activeStrategy) {

            is ContextStrategy.SlidingWindow -> {
                val src = getActiveBranchHistory()
                val window = src.takeLast(s.windowSize)
                msgs.addAll(window)
                Log.d("Agent", "SlidingWindow: ${window.size}/${src.size} msgs")
            }

            is ContextStrategy.StickyFacts -> {
                if (facts.isNotEmpty()) {
                    val factsText = facts.entries.joinToString("\n") { (k, v) -> "• $k: $v" }
                    msgs.add(MessageDto(
                        role = "system",
                        content = "📌 Ключевые факты о пользователе и контексте:\n$factsText"
                    ))
                }
                val recent = history.takeLast(s.recentWindow)
                msgs.addAll(recent)
                Log.d("Agent", "StickyFacts: ${facts.size} facts + ${recent.size} msgs")
            }

            is ContextStrategy.Branching -> {
                val full = getActiveBranchHistory()
                msgs.addAll(full)
                Log.d("Agent", "Branching: branch=$activeBranchId, ${full.size} msgs")
            }
        }
        return msgs
    }

    // ── Day 9: Summary (не вызывается при Day-10 стратегиях) ─────────────────

    private suspend fun maybeCompress() {
        while (history.size - coveredCount > RECENT_WINDOW + COMPRESS_EVERY) {
            val searchEnd = minOf(coveredCount + COMPRESS_EVERY + 4, history.size - RECENT_WINDOW)
            val cutPoint = findTurnBoundary(coveredCount + 1, searchEnd)
            if (cutPoint <= coveredCount) break
            val batch = history.subList(coveredCount, cutPoint)
            summary = generateSummary(summary, batch)
            coveredCount = cutPoint
            historyRepository.saveContext(summary, coveredCount)
        }
    }

    private fun findTurnBoundary(startFrom: Int, maxEnd: Int): Int {
        for (i in startFrom until maxEnd) {
            val msg = history[i]
            if (msg.role == "assistant" && msg.toolCalls.isNullOrEmpty() && !msg.content.isNullOrBlank())
                return i + 1
        }
        return coveredCount
    }

    private suspend fun generateSummary(existingSummary: String, messages: List<MessageDto>): String {
        val prompt = buildString {
            if (existingSummary.isNotBlank()) appendLine("Существующее резюме:\n$existingSummary\n")
            appendLine("Сообщения:")
            messages.forEach { when (it.role) {
                "user"      -> appendLine("Пользователь: ${it.content}")
                "assistant" -> if (!it.content.isNullOrBlank()) appendLine("Ассистент: ${it.content}")
                "tool"      -> appendLine("Инструмент: ${it.content}")
            }}
            append("Создай краткое резюме (до 150 слов). Только текст.")
        }
        return try {
            apiService.chatCompletions(ChatRequestDto(
                model = "deepseek-chat",
                messages = listOf(MessageDto(role = "user", content = prompt)),
                maxTokens = 400, temperature = 0.2f
            )).choices.first().message.content ?: existingSummary
        } catch (e: Exception) { existingSummary }
    }

    // ── StickyFacts: извлечение фактов ────────────────────────────────────────

    private suspend fun extractAndUpdateFacts(userMessage: String, assistantResponse: String) {
        val currentFacts = if (facts.isEmpty()) "нет"
            else facts.entries.joinToString("\n") { (k, v) -> "$k: $v" }

        val prompt = """
            Обнови список ключевых фактов на основе нового обмена.

            Текущие факты:
            $currentFacts

            Новый обмен:
            Пользователь: $userMessage
            Ассистент: $assistantResponse

            Верни ТОЛЬКО список фактов, каждый на новой строке в формате:
            ключ: значение

            Включай только конкретные факты (имена, числа, решения, предпочтения, договорённости).
            Не добавляй пояснений. Если нет новых фактов — повтори текущие.
        """.trimIndent()

        try {
            val content = apiService.chatCompletions(ChatRequestDto(
                model = "deepseek-chat",
                messages = listOf(MessageDto(role = "user", content = prompt)),
                maxTokens = 300, temperature = 0.1f
            )).choices.first().message.content ?: return

            val newFacts = LinkedHashMap<String, String>()
            content.lines().forEach { line ->
                val idx = line.indexOf(": ")
                if (idx > 0) {
                    val key   = line.substring(0, idx).trim().lowercase()
                    val value = line.substring(idx + 2).trim()
                    if (key.isNotBlank() && value.isNotBlank()) newFacts[key] = value
                }
            }
            if (newFacts.isNotEmpty()) {
                facts.clear(); facts.putAll(newFacts)
                historyRepository.clearFacts()
                newFacts.forEach { (k, v) -> historyRepository.saveFact(k, v) }
                Log.d("Agent", "Facts updated: ${facts.size}")
            }
        } catch (e: Exception) {
            Log.e("Agent", "Facts extraction failed: ${e.message}")
        }
    }

    // ── Основной диалог ────────────────────────────────────────────────────────

    suspend fun chat(userMessage: String): AgentResult {
        ensureHistoryLoaded()

        val messages = buildContextMessages().toMutableList()
        val userMsg  = MessageDto(role = "user", content = userMessage)
        messages.add(userMsg)
        addToHistory(userMsg)

        val toolDefs = tools.map { it.definition }.takeIf { it.isNotEmpty() }
        val steps    = mutableListOf<AgentStep>()

        var lastPromptTokens      = 0
        var totalCompletionTokens = 0
        var totalCostAccumulator  = 0.0

        return runCatching {
            var iterations  = 0
            var finalAnswer = ""

            while (iterations < 5) {
                val response = apiService.chatCompletions(ChatRequestDto(
                    model = "deepseek-chat",
                    messages = messages.toList(),
                    temperature = 0.7f,
                    maxTokens = 1000,
                    tools = toolDefs
                ))

                response.usage?.let { u ->
                    lastPromptTokens       = u.promptTokens
                    totalCompletionTokens += u.completionTokens
                    totalCostAccumulator  += u.promptTokens * PRICE_INPUT + u.completionTokens * PRICE_OUTPUT
                }

                val choice = response.choices.first()
                val assistantMsg = choice.message
                messages.add(assistantMsg)

                if (choice.finishReason == "tool_calls" && !assistantMsg.toolCalls.isNullOrEmpty()) {
                    addToHistory(assistantMsg)
                    for (toolCall in assistantMsg.toolCalls) {
                        val tool   = tools.find { it.name == toolCall.function.name }
                        val result = tool?.execute(toolCall.function.arguments)
                            ?: "Инструмент '${toolCall.function.name}' не найден"
                        steps.add(AgentStep(toolCall.function.name, toolCall.function.arguments, result))
                        val toolMsg = MessageDto(role = "tool", content = result, toolCallId = toolCall.id)
                        messages.add(toolMsg)
                        addToHistory(toolMsg)
                    }
                    iterations++
                } else {
                    finalAnswer = assistantMsg.content ?: ""
                    addToHistory(assistantMsg)
                    break
                }
            }

            if (iterations >= 5)
                return@runCatching AgentResult(answer = "Превышен лимит итераций.", steps = steps)

            sessionPromptTokens    = lastPromptTokens
            sessionCompletionTokens += totalCompletionTokens

            val tokenInfo = TokenInfo(
                promptTokens      = lastPromptTokens,
                completionTokens  = totalCompletionTokens,
                costUsd           = totalCostAccumulator
            )

            // Обновляем факты после успешного обмена (только StickyFacts)
            if (activeStrategy is ContextStrategy.StickyFacts && finalAnswer.isNotBlank()) {
                extractAndUpdateFacts(userMessage, finalAnswer)
            }

            Log.d("Agent", "Done: ${activeStrategy.displayName}, " +
                "prompt=$lastPromptTokens, compl=$totalCompletionTokens, steps=${steps.size}")

            AgentResult(answer = finalAnswer, steps = steps, tokenInfo = tokenInfo)

        }.getOrElse { error ->
            Log.e("Agent", "Error: ${error.message}")
            val isOverflow = error.message?.contains("context_length", ignoreCase = true) == true
            AgentResult(
                answer = if (isOverflow) "⛔ Контекст переполнен! Очистите историю кнопкой 🗑."
                         else error.message ?: "Неизвестная ошибка",
                isError = true
            )
        }
    }
}
