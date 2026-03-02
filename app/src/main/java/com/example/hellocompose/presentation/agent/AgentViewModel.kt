package com.example.hellocompose.presentation.agent

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hellocompose.data.api.dto.MessageDto
import com.example.hellocompose.domain.agent.Agent
import com.example.hellocompose.domain.agent.AgentStep
import com.example.hellocompose.domain.agent.ContextStrategy
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AgentViewModel(private val agent: Agent) : ViewModel() {

    private val _state = MutableStateFlow(AgentState())
    val state: StateFlow<AgentState> = _state.asStateFlow()

    private val _effect = Channel<AgentEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        viewModelScope.launch {
            val history  = agent.getHistory()
            val uiMsgs   = history.toUiMessages()
            val ctxStats = agent.getContextStats().toUiStats()
            val strategyState = buildStrategyState()
            if (uiMsgs.isNotEmpty()) {
                _state.update { it.copy(messages = uiMsgs, contextStats = ctxStats, strategyState = strategyState) }
                _effect.send(AgentEffect.ScrollToBottom)
            } else {
                _state.update { it.copy(strategyState = strategyState) }
            }
            Log.d("AgentVM", "Init: ${uiMsgs.size} messages, strategy=${agent.activeStrategy.displayName}")
        }
    }

    // ── Intent handler ────────────────────────────────────────────────────────

    fun handleIntent(intent: AgentIntent) {
        when (intent) {
            is AgentIntent.TypeMessage    -> _state.update { it.copy(inputText = intent.text) }
            is AgentIntent.SendMessage    -> sendMessage()
            is AgentIntent.ClearHistory   -> clearHistory()
            is AgentIntent.ChangeStrategy -> changeStrategy(intent.strategy)
            is AgentIntent.SaveCheckpoint -> saveCheckpoint()
            is AgentIntent.CreateBranch   -> createBranch(intent.name)
            is AgentIntent.SwitchBranch   -> switchBranch(intent.branchId)
            is AgentIntent.RunDemo        -> runDemo(intent.messages)
            is AgentIntent.StopDemo       -> stopDemo()
        }
    }

    // ── Send message ──────────────────────────────────────────────────────────

    private fun sendMessage() {
        val text = _state.value.inputText.trim()
        if (text.isBlank() || _state.value.isLoading) return
        viewModelScope.launch { sendMessageCore(text) }
    }

    /** Suspend-ядро отправки — используется и при ручном вводе, и в демо-режиме. */
    private suspend fun sendMessageCore(text: String) {
        val branchId   = agent.activeBranchId
        val userMsg    = AgentMessage.User(text = text, branchId = branchId)
        val loadingMsg = AgentMessage.Assistant(isLoading = true, branchId = branchId)

        _state.update {
            it.copy(messages = it.messages + userMsg + loadingMsg, inputText = "", isLoading = true)
        }
        _effect.send(AgentEffect.ScrollToBottom)

        val result       = agent.chat(text)
        val ctxStats     = agent.getContextStats().toUiStats()
        val strategyState = buildStrategyState()

        _state.update { state ->
            val updatedStats = state.sessionStats.copy(
                lastPromptTokens = result.tokenInfo.promptTokens,
                totalCostUsd     = state.sessionStats.totalCostUsd + result.tokenInfo.costUsd,
                totalExchanges   = state.sessionStats.totalExchanges + 1
            )
            val updatedMsgs = state.messages.dropLast(1) + AgentMessage.Assistant(
                text      = result.answer,
                steps     = result.steps,
                isLoading = false,
                isError   = result.isError,
                tokenInfo = result.tokenInfo,
                branchId  = branchId
            )
            state.copy(
                messages      = updatedMsgs,
                isLoading     = false,
                sessionStats  = updatedStats,
                contextStats  = ctxStats,
                strategyState = strategyState
            )
        }
        _effect.send(AgentEffect.ScrollToBottom)
    }

    // ── Demo (auto-send) ──────────────────────────────────────────────────────

    private var demoJob: Job? = null

    private fun runDemo(messages: List<String>) {
        if (_state.value.isDemoRunning || _state.value.isLoading) return
        demoJob = viewModelScope.launch {
            _state.update { it.copy(isDemoRunning = true, demoStep = 0, demoTotal = messages.size) }
            try {
                messages.forEachIndexed { i, msg ->
                    _state.update { it.copy(demoStep = i + 1) }
                    sendMessageCore(msg)
                    if (i < messages.lastIndex) delay(600)
                }
                _effect.send(AgentEffect.ShowToast("Автотест завершён ✓"))
            } finally {
                _state.update { it.copy(isDemoRunning = false, demoStep = 0, demoTotal = 0) }
            }
        }
    }

    private fun stopDemo() {
        demoJob?.cancel()
        demoJob = null
        _state.update { it.copy(isDemoRunning = false, demoStep = 0, demoTotal = 0, isLoading = false) }
    }

    // ── Clear ─────────────────────────────────────────────────────────────────

    private fun clearHistory() {
        viewModelScope.launch {
            agent.reset()
            _state.update {
                it.copy(
                    messages = emptyList(),
                    inputText = "",
                    sessionStats = SessionStats(),
                    contextStats = ContextStats(),
                    strategyState = buildStrategyState()
                )
            }
        }
    }

    // ── Strategy ──────────────────────────────────────────────────────────────

    private fun changeStrategy(strategy: ContextStrategy) {
        viewModelScope.launch {
            agent.setStrategy(strategy)
            // Сбрасываем UI — история остаётся, но факты/ветки очищены
            _state.update {
                it.copy(
                    messages = emptyList(),
                    sessionStats = SessionStats(),
                    strategyState = buildStrategyState()
                )
            }
            _effect.send(AgentEffect.ShowToast("Стратегия: ${strategy.displayName}"))
            Log.d("AgentVM", "Strategy changed to ${strategy.displayName}")
        }
    }

    // ── Branching ─────────────────────────────────────────────────────────────

    private fun saveCheckpoint() {
        agent.saveCheckpoint()
        _state.update { it.copy(strategyState = buildStrategyState()) }
        viewModelScope.launch {
            _effect.send(AgentEffect.ShowToast("Чекпоинт сохранён (${agent.checkpointHistory.size} сообщ.)"))
        }
    }

    private fun createBranch(name: String) {
        val branch = agent.createBranch(name)
        // Показываем только сообщения текущей ветки
        val branchMsgs = agent.getActiveBranchHistory().toUiMessages(branch.id)
        _state.update { it.copy(messages = branchMsgs, strategyState = buildStrategyState()) }
        viewModelScope.launch {
            _effect.send(AgentEffect.ShowToast("Ветка '${branch.name}' создана"))
            _effect.send(AgentEffect.ScrollToBottom)
        }
    }

    private fun switchBranch(branchId: String) {
        agent.switchBranch(branchId)
        // Перестраиваем UI для новой ветки
        val msgs = agent.getActiveBranchHistory().toUiMessages(branchId)
        _state.update { it.copy(messages = msgs, strategyState = buildStrategyState()) }
        viewModelScope.launch { _effect.send(AgentEffect.ScrollToBottom) }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildStrategyState(): StrategyState {
        val s = agent.activeStrategy
        return StrategyState(
            active = s,
            totalMessages = agent.history.size,
            windowMessages = when (s) {
                is ContextStrategy.SlidingWindow -> minOf(s.windowSize, agent.getActiveBranchHistory().size)
                is ContextStrategy.StickyFacts   -> minOf(s.recentWindow, agent.history.size)
                is ContextStrategy.Branching     -> agent.getActiveBranchHistory().size
            },
            facts = agent.facts.toMap(),
            isExtractingFacts = false,
            hasCheckpoint = agent.checkpointHistory.isNotEmpty(),
            checkpointSize = agent.checkpointHistory.size,
            branches = agent.branches.map { b ->
                BranchInfo(
                    id = b.id,
                    name = b.name,
                    messageCount = (agent.checkpointHistory + b.messages).size,
                    isActive = b.id == agent.activeBranchId
                )
            },
            activeBranchId = agent.activeBranchId
        )
    }

    private fun Agent.ContextStats.toUiStats() = ContextStats(
        compressedCount = compressedCount,
        recentCount = recentCount,
        isSummaryActive = isSummaryActive,
        summaryLength = summaryLength
    )

    // Expose history size for buildStrategyState
    private val Agent.history: List<MessageDto>
        get() = runCatching { getActiveBranchHistory() }.getOrElse { emptyList() }
}

// ── toUiMessages ──────────────────────────────────────────────────────────────

private fun List<MessageDto>.toUiMessages(branchId: String = "main"): List<AgentMessage> {
    val result = mutableListOf<AgentMessage>()
    val pendingSteps = mutableListOf<AgentStep>()
    val pendingCalls = mutableMapOf<String, Pair<String, String>>()

    for (msg in this) {
        when (msg.role) {
            "system" -> {}

            "user" -> {
                pendingSteps.clear(); pendingCalls.clear()
                result.add(AgentMessage.User(text = msg.content ?: "", branchId = branchId))
            }

            "assistant" -> {
                if (!msg.toolCalls.isNullOrEmpty()) {
                    msg.toolCalls.forEach { call ->
                        pendingCalls[call.id] = call.function.name to call.function.arguments
                    }
                } else {
                    result.add(AgentMessage.Assistant(
                        text = msg.content ?: "",
                        steps = pendingSteps.toList(),
                        branchId = branchId,
                        tokenInfo = null
                    ))
                    pendingSteps.clear(); pendingCalls.clear()
                }
            }

            "tool" -> {
                val (toolName, arguments) = pendingCalls[msg.toolCallId] ?: ("unknown_tool" to "{}")
                pendingSteps.add(AgentStep(toolName, arguments, msg.content ?: ""))
            }
        }
    }
    return result
}
