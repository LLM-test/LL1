package com.example.hellocompose.presentation.agent

import com.example.hellocompose.domain.agent.Agent
import com.example.hellocompose.domain.agent.AgentStep
import com.example.hellocompose.domain.agent.ContextStrategy
import com.example.hellocompose.domain.agent.TokenInfo
import java.util.concurrent.atomic.AtomicLong

private val idCounter = AtomicLong(0)
private fun nextId() = idCounter.incrementAndGet()

// ── Session / Context stats (Day 8 / Day 9) ───────────────────────────────────

data class SessionStats(
    val lastPromptTokens: Int = 0,
    val totalCostUsd: Double = 0.0,
    val totalExchanges: Int = 0
) {
    val contextUsedPercent: Float
        get() = (lastPromptTokens.toFloat() / Agent.CONTEXT_LIMIT).coerceAtMost(1f)
    val isNearLimit: Boolean get() = contextUsedPercent > 0.8f
}

data class ContextStats(
    val compressedCount: Int = 0,
    val recentCount: Int = 0,
    val isSummaryActive: Boolean = false,
    val summaryLength: Int = 0
) {
    val totalMessages: Int get() = compressedCount + recentCount
    val compressionRatio: Float
        get() = if (totalMessages == 0) 0f else compressedCount.toFloat() / totalMessages
}

// ── Day 10: Strategy state ────────────────────────────────────────────────────

/** Информация об одной ветке диалога (Branching). */
data class BranchInfo(
    val id: String,
    val name: String,
    val messageCount: Int,
    val isActive: Boolean
)

/**
 * Состояние текущей стратегии управления контекстом.
 * Обновляется в реальном времени после каждого обмена.
 */
data class StrategyState(
    val active: ContextStrategy = ContextStrategy.SlidingWindow(),

    // SlidingWindow
    val totalMessages: Int = 0,     // всего сообщений в истории
    val windowMessages: Int = 0,    // сколько реально попадёт в запрос

    // StickyFacts
    val facts: Map<String, String> = emptyMap(),
    val isExtractingFacts: Boolean = false,

    // Branching
    val hasCheckpoint: Boolean = false,
    val checkpointSize: Int = 0,
    val branches: List<BranchInfo> = emptyList(),
    val activeBranchId: String = "main"
)

// ── Main state ────────────────────────────────────────────────────────────────

data class AgentState(
    val messages: List<AgentMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val sessionStats: SessionStats = SessionStats(),
    val contextStats: ContextStats = ContextStats(),
    val strategyState: StrategyState = StrategyState(),
    val isDemoRunning: Boolean = false,
    val demoStep: Int = 0,
    val demoTotal: Int = 0
)

// ── Messages ──────────────────────────────────────────────────────────────────

sealed class AgentMessage {
    abstract val id: Long

    data class User(
        val text: String,
        val branchId: String = "main",
        override val id: Long = nextId()
    ) : AgentMessage()

    data class Assistant(
        val text: String = "",
        val steps: List<AgentStep> = emptyList(),
        val isLoading: Boolean = false,
        val isError: Boolean = false,
        val tokenInfo: TokenInfo? = null,
        val branchId: String = "main",
        override val id: Long = nextId()
    ) : AgentMessage()
}

// ── Intents ───────────────────────────────────────────────────────────────────

sealed class AgentIntent {
    data class TypeMessage(val text: String) : AgentIntent()
    object SendMessage : AgentIntent()
    object ClearHistory : AgentIntent()

    // Day 10
    data class ChangeStrategy(val strategy: ContextStrategy) : AgentIntent()
    object SaveCheckpoint : AgentIntent()
    data class CreateBranch(val name: String) : AgentIntent()
    data class SwitchBranch(val branchId: String) : AgentIntent()

    // Demo auto-send
    data class RunDemo(val messages: List<String>) : AgentIntent()
    object StopDemo : AgentIntent()
}

// ── Effects ───────────────────────────────────────────────────────────────────

sealed class AgentEffect {
    object ScrollToBottom : AgentEffect()
    data class ShowToast(val message: String) : AgentEffect()
}
