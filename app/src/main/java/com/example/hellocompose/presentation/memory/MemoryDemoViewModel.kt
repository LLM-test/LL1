package com.example.hellocompose.presentation.memory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hellocompose.domain.agent.Agent
import com.example.hellocompose.domain.memory.MemoryRepository
import com.example.hellocompose.domain.memory.MemoryType
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MemoryDemoViewModel(
    private val agent: Agent,
    private val memoryRepo: MemoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MemoryDemoState())
    val state: StateFlow<MemoryDemoState> = _state.asStateFlow()

    private val _effect = Channel<MemoryDemoEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var demoJob: Job? = null

    init { refreshSystemPrompt() }

    fun handleIntent(intent: MemoryDemoIntent) {
        when (intent) {
            is MemoryDemoIntent.SelectTab    -> selectTab(intent.index)
            is MemoryDemoIntent.RunScenario  -> runScenario()
            is MemoryDemoIntent.StopScenario -> stopScenario()
            is MemoryDemoIntent.ResetScenario -> resetScenario()
        }
    }

    // ── Tab ───────────────────────────────────────────────────────────────────

    private fun selectTab(index: Int) {
        _state.update { it.copy(
            activeTab       = index,
            steps           = emptyList(),
            isRunning       = false,
            beforeResponse  = null,
            afterResponse   = null,
            conclusion      = null
        )}
        refreshSystemPrompt()
    }

    private fun refreshSystemPrompt() {
        viewModelScope.launch {
            val preview = agent.getSystemPromptPreview()
            _state.update { it.copy(systemPromptMemoryBlock = preview) }
        }
    }

    // ── Run ───────────────────────────────────────────────────────────────────

    private fun runScenario() {
        if (_state.value.isRunning) return
        val scenario = DemoScenario.entries[_state.value.activeTab]
        demoJob = viewModelScope.launch {
            _state.update { it.copy(isRunning = true, beforeResponse = null, afterResponse = null, conclusion = null) }
            try {
                when (scenario) {
                    DemoScenario.SHORT_TERM -> runShortTerm()
                    DemoScenario.WORKING    -> runWorking()
                    DemoScenario.LONG_TERM  -> runLongTerm()
                }
            } finally {
                _state.update { it.copy(isRunning = false) }
                refreshSystemPrompt()
            }
        }
    }

    // ── Сценарий 1: Краткосрочная ─────────────────────────────────────────────

    private suspend fun runShortTerm() {
        val initialSteps = listOf(
            DemoStep("Очищаем историю и всю память (чистый старт)",
                memoryAction = "agent.reset() + clearByType(WORKING) + clearByType(LONG_TERM)"),
            DemoStep("Представляемся агенту", isChatStep = true,
                userMessage = "Меня зовут Андрей"),
            DemoStep("Проверяем: агент помнит из истории?", isChatStep = true, isBeforeStep = true,
                userMessage = "Как меня зовут?",
                memoryBadge = "💬 из истории чата"),
            DemoStep("Очищаем историю чата",
                memoryAction = "agent.reset() → история стёрта из RAM"),
            DemoStep("Проверяем снова: помнит ли без истории?", isChatStep = true, isAfterStep = true,
                userMessage = "Как меня зовут?",
                memoryBadge = "❌ агент не помнит")
        )
        setSteps(initialSteps)

        // Шаг 0: сброс (очищаем ВСЕ слои, чтобы прошлые сценарии не мешали)
        runMemoryStep(0, "agent.reset() + clearByType(WORKING) + clearByType(LONG_TERM)") {
            agent.reset()
            memoryRepo.clearByType(MemoryType.WORKING)
            memoryRepo.clearByType(MemoryType.LONG_TERM)
            refreshSystemPrompt()
        }
        // Шаг 1: представляемся
        val introAnswer = runChatStep(1) { agent.chat("Меня зовут Андрей").answer }
        // Шаг 2: проверяем ДО
        val beforeAnswer = runChatStep(2) { agent.chat("Как меня зовут?").answer }
        _state.update { it.copy(beforeResponse = beforeAnswer) }

        // Шаг 3: очищаем историю
        runMemoryStep(3, "agent.reset() выполнен — история очищена, долговременная память цела") {
            agent.reset()
        }
        // Шаг 4: проверяем ПОСЛЕ
        val afterAnswer = runChatStep(4) { agent.chat("Как меня зовут?").answer }
        _state.update { it.copy(afterResponse = afterAnswer,
            conclusion = DemoScenario.SHORT_TERM.conclusion) }
    }

    // ── Сценарий 2: Рабочая ───────────────────────────────────────────────────

    private suspend fun runWorking() {
        val initialSteps = listOf(
            DemoStep("Очищаем историю и всю память (чистый старт)",
                memoryAction = "agent.reset() + clearByType(WORKING) + clearByType(LONG_TERM)"),
            DemoStep("Сохраняем в рабочую память",
                memoryAction = "save(WORKING, \"task.project\", \"Трекер привычек\")"),
            DemoStep("Проверяем: агент видит рабочую память?", isChatStep = true, isBeforeStep = true,
                userMessage = "Что я сейчас разрабатываю?",
                memoryBadge = "🔧 из рабочей памяти"),
            DemoStep("Очищаем рабочую память",
                memoryAction = "clearByType(WORKING) → блок [РАБОЧАЯ ПАМЯТЬ] убран из system prompt"),
            DemoStep("Проверяем снова: помнит ли без рабочей памяти?", isChatStep = true, isAfterStep = true,
                userMessage = "Что я сейчас разрабатываю?",
                memoryBadge = "❌ агент не помнит")
        )
        setSteps(initialSteps)

        // Шаг 0: сброс (очищаем ВСЕ слои, чтобы прошлые сценарии не мешали)
        runMemoryStep(0, "agent.reset() + clearByType(WORKING) + clearByType(LONG_TERM)") {
            agent.reset()
            memoryRepo.clearByType(MemoryType.WORKING)
            memoryRepo.clearByType(MemoryType.LONG_TERM)
            refreshSystemPrompt()
        }
        // Шаг 1: добавляем в рабочую
        runMemoryStep(1, "Сохранено: task.project = \"Трекер привычек\"") {
            memoryRepo.save(MemoryType.WORKING, "task.project", "Трекер привычек")
            refreshSystemPrompt()
        }
        // Шаг 2: проверяем ДО
        val beforeAnswer = runChatStep(2) { agent.chat("Что я сейчас разрабатываю?").answer }
        _state.update { it.copy(beforeResponse = beforeAnswer) }

        // Шаг 3: очищаем рабочую
        runMemoryStep(3, "Рабочая память очищена — блок [РАБОЧАЯ ПАМЯТЬ] больше не инжектируется") {
            memoryRepo.clearByType(MemoryType.WORKING)
            refreshSystemPrompt()
        }
        // Шаг 4: проверяем ПОСЛЕ
        val afterAnswer = runChatStep(4) { agent.chat("Что я сейчас разрабатываю?").answer }
        _state.update { it.copy(afterResponse = afterAnswer,
            conclusion = DemoScenario.WORKING.conclusion) }
    }

    // ── Сценарий 3: Долговременная ────────────────────────────────────────────

    private suspend fun runLongTerm() {
        val initialSteps = listOf(
            DemoStep("Очищаем историю и всю память (чистый старт)",
                memoryAction = "agent.reset() + clearByType(WORKING) + clearByType(LONG_TERM)"),
            DemoStep("Сохраняем в долговременную память",
                memoryAction = "save(LONG_TERM, \"profile.name\", \"Андрей\")"),
            DemoStep("Очищаем историю чата (долговременную НЕ трогаем)",
                memoryAction = "agent.reset() → только история, память цела"),
            DemoStep("Проверяем: агент знает имя без истории?", isChatStep = true, isAfterStep = true,
                userMessage = "Как меня зовут?",
                memoryBadge = "🧠 из долговременной памяти")
        )
        setSteps(initialSteps)

        // Шаг 0: сброс (очищаем ВСЕ слои, чтобы прошлые сценарии не мешали)
        runMemoryStep(0, "agent.reset() + clearByType(WORKING) + clearByType(LONG_TERM)") {
            agent.reset()
            memoryRepo.clearByType(MemoryType.WORKING)
            memoryRepo.clearByType(MemoryType.LONG_TERM)
            refreshSystemPrompt()
        }
        // Шаг 1: добавляем в долговременную
        runMemoryStep(1, "Сохранено: profile.name = \"Андрей\"") {
            memoryRepo.save(MemoryType.LONG_TERM, "profile.name", "Андрей")
            refreshSystemPrompt()
        }
        // Шаг 2: очищаем историю
        runMemoryStep(2, "История очищена — долговременная память осталась в system prompt") {
            agent.reset()
        }
        // Шаг 3: ключевой момент — агент знает без истории
        val answer = runChatStep(3) { agent.chat("Как меня зовут?").answer }
        _state.update { it.copy(afterResponse = answer,
            conclusion = DemoScenario.LONG_TERM.conclusion) }
    }

    // ── Утилиты ───────────────────────────────────────────────────────────────

    private fun setSteps(steps: List<DemoStep>) {
        _state.update { it.copy(steps = steps) }
    }

    /** Универсальный шаг (без ответа/операции). */
    private suspend fun <T> runStep(index: Int, block: suspend () -> T): T {
        updateStep(index, StepStatus.RUNNING)
        return try {
            val result = block()
            updateStep(index, StepStatus.DONE)
            result
        } catch (e: Exception) {
            updateStep(index, StepStatus.DONE)
            throw e
        }
    }

    /** Чат-шаг: сохраняет ответ агента прямо в DemoStep.agentResponse. */
    private suspend fun runChatStep(index: Int, block: suspend () -> String): String {
        updateStep(index, StepStatus.RUNNING)
        return try {
            val answer = block()
            updateStep(index, StepStatus.DONE, agentResponse = answer)
            answer
        } catch (e: Exception) {
            updateStep(index, StepStatus.DONE)
            throw e
        }
    }

    /** Шаг с операцией над памятью: сохраняет описание операции в DemoStep.memoryAction. */
    private suspend fun runMemoryStep(index: Int, action: String, block: suspend () -> Unit) {
        updateStep(index, StepStatus.RUNNING)
        try {
            block()
            updateStep(index, StepStatus.DONE, memoryAction = action)
        } catch (e: Exception) {
            updateStep(index, StepStatus.DONE, memoryAction = action)
            throw e
        }
    }

    private fun updateStep(
        index: Int,
        status: StepStatus,
        agentResponse: String? = null,
        memoryAction: String? = null
    ) {
        _state.update { s ->
            val updated = s.steps.toMutableList()
            if (index < updated.size) {
                val old = updated[index]
                updated[index] = old.copy(
                    status        = status,
                    agentResponse = agentResponse ?: old.agentResponse,
                    memoryAction  = memoryAction  ?: old.memoryAction
                )
            }
            s.copy(steps = updated)
        }
    }

    private fun stopScenario() {
        demoJob?.cancel()
        demoJob = null
        _state.update { it.copy(isRunning = false) }
    }

    private fun resetScenario() {
        stopScenario()
        _state.update { it.copy(
            steps          = emptyList(),
            beforeResponse = null,
            afterResponse  = null,
            conclusion     = null
        )}
    }
}
