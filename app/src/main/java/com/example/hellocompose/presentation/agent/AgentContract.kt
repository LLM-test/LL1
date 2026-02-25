package com.example.hellocompose.presentation.agent

import com.example.hellocompose.domain.agent.AgentStep
import com.example.hellocompose.domain.agent.Agent
import com.example.hellocompose.domain.agent.TokenInfo
import java.util.concurrent.atomic.AtomicLong

private val idCounter = AtomicLong(0)
private fun nextId() = idCounter.incrementAndGet()

/**
 * Накопительная статистика токенов за всю сессию.
 *
 * - [lastPromptTokens] — prompt_tokens последнего запроса.
 *   Показывает, сколько токенов «занято» в контекстном окне прямо сейчас
 *   (каждый новый запрос включает всю предыдущую историю).
 * - [totalCostUsd] — суммарная стоимость всех запросов в сессии.
 * - [totalExchanges] — количество завершённых обменов пользователь↔агент.
 */
data class SessionStats(
    val lastPromptTokens: Int = 0,
    val totalCostUsd: Double = 0.0,
    val totalExchanges: Int = 0
) {
    /** Процент заполнения контекстного окна (0.0 … 1.0). */
    val contextUsedPercent: Float
        get() = (lastPromptTokens.toFloat() / Agent.CONTEXT_LIMIT).coerceAtMost(1f)

    /** Достигнуто >80% контекста — время предупредить пользователя. */
    val isNearLimit: Boolean get() = contextUsedPercent > 0.8f
}

data class AgentState(
    val messages: List<AgentMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val sessionStats: SessionStats = SessionStats()
)

sealed class AgentMessage {
    abstract val id: Long

    data class User(
        val text: String,
        override val id: Long = nextId()
    ) : AgentMessage()

    data class Assistant(
        val text: String = "",
        val steps: List<AgentStep> = emptyList(),
        val isLoading: Boolean = false,
        val isError: Boolean = false,
        val tokenInfo: TokenInfo? = null,   // null пока загружается
        override val id: Long = nextId()
    ) : AgentMessage()
}

sealed class AgentIntent {
    data class TypeMessage(val text: String) : AgentIntent()
    object SendMessage : AgentIntent()
    object ClearHistory : AgentIntent()
}

sealed class AgentEffect {
    object ScrollToBottom : AgentEffect()
}
