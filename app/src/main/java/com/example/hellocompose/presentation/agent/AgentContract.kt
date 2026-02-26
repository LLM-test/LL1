package com.example.hellocompose.presentation.agent

import com.example.hellocompose.domain.agent.Agent
import com.example.hellocompose.domain.agent.AgentStep
import com.example.hellocompose.domain.agent.TokenInfo
import java.util.concurrent.atomic.AtomicLong

private val idCounter = AtomicLong(0)
private fun nextId() = idCounter.incrementAndGet()

/**
 * Накопительная статистика токенов за всю сессию.
 */
data class SessionStats(
    val lastPromptTokens: Int = 0,
    val totalCostUsd: Double = 0.0,
    val totalExchanges: Int = 0
) {
    val contextUsedPercent: Float
        get() = (lastPromptTokens.toFloat() / Agent.CONTEXT_LIMIT).coerceAtMost(1f)

    val isNearLimit: Boolean get() = contextUsedPercent > 0.8f
}

/**
 * Статистика сжатия контекста.
 *
 * [compressedCount] — сколько сообщений покрыто summary (уже не передаются verbatim).
 * [recentCount]     — сколько последних сообщений передаётся «как есть».
 * [isSummaryActive] — есть ли активное резюме.
 * [summaryLength]   — длина текста резюме в символах.
 */
data class ContextStats(
    val compressedCount: Int = 0,
    val recentCount: Int = 0,
    val isSummaryActive: Boolean = false,
    val summaryLength: Int = 0
) {
    /** Сколько всего сообщений в истории (compressed + recent). */
    val totalMessages: Int get() = compressedCount + recentCount

    /** Процент сжатия: сколько от общей истории покрыто summary. */
    val compressionRatio: Float
        get() = if (totalMessages == 0) 0f
        else compressedCount.toFloat() / totalMessages
}

data class AgentState(
    val messages: List<AgentMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val sessionStats: SessionStats = SessionStats(),
    val contextStats: ContextStats = ContextStats()
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
        val tokenInfo: TokenInfo? = null,
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
