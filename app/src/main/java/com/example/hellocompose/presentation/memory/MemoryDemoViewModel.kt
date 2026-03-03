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
            DemoStep("Очищаем историю чата и память (чистый старт)"),
            DemoStep("Отправляем агенту: «Меня зовут Андрей»", isChatStep = true),
            DemoStep("Спрашиваем: «Как меня зовут?»", isChatStep = true, isBeforeStep = true,
                memoryBadge = "💬 из истории чата"),
            DemoStep("Очищаем историю чата"),
            DemoStep("Снова: «Как меня зовут?»", isChatStep = true, isAfterStep = true,
                memoryBadge = "❌ агент не помнит")
        )
        setSteps(initialSteps)

        // Шаг 0: сброс
        runStep(0) {
            agent.reset()
            memoryRepo.clearByType(MemoryType.WORKING)
        }
        // Шаг 1: представляемся
        runStep(1) {
            agent.chat("Меня зовут Андрей")
        }
        // Шаг 2: проверяем ДО
        val beforeAnswer = runStep(2) {
            agent.chat("Как меня зовут?").answer
        }
        _state.update { it.copy(beforeResponse = beforeAnswer) }

        // Шаг 3: очищаем историю
        runStep(3) {
            agent.reset()
        }
        // Шаг 4: проверяем ПОСЛЕ
        val afterAnswer = runStep(4) {
            agent.chat("Как меня зовут?").answer
        }
        _state.update { it.copy(afterResponse = afterAnswer,
            conclusion = DemoScenario.SHORT_TERM.conclusion) }
    }

    // ── Сценарий 2: Рабочая ───────────────────────────────────────────────────

    private suspend fun runWorking() {
        val initialSteps = listOf(
            DemoStep("Очищаем историю и рабочую память (чистый старт)"),
            DemoStep("Добавляем в рабочую память: task.project = Трекер привычек"),
            DemoStep("Спрашиваем: «Что я разрабатываю?»", isChatStep = true, isBeforeStep = true,
                memoryBadge = "🔧 из рабочей памяти"),
            DemoStep("Очищаем рабочую память"),
            DemoStep("Снова: «Что я разрабатываю?»", isChatStep = true, isAfterStep = true,
                memoryBadge = "❌ агент не помнит")
        )
        setSteps(initialSteps)

        // Шаг 0: сброс
        runStep(0) {
            agent.reset()
            memoryRepo.clearByType(MemoryType.WORKING)
        }
        // Шаг 1: добавляем в рабочую
        runStep(1) {
            memoryRepo.save(MemoryType.WORKING, "task.project", "Трекер привычек")
            refreshSystemPrompt()
        }
        // Шаг 2: проверяем ДО
        val beforeAnswer = runStep(2) {
            agent.chat("Что я сейчас разрабатываю?").answer
        }
        _state.update { it.copy(beforeResponse = beforeAnswer) }

        // Шаг 3: очищаем рабочую
        runStep(3) {
            memoryRepo.clearByType(MemoryType.WORKING)
            refreshSystemPrompt()
        }
        // Шаг 4: проверяем ПОСЛЕ
        val afterAnswer = runStep(4) {
            agent.chat("Что я сейчас разрабатываю?").answer
        }
        _state.update { it.copy(afterResponse = afterAnswer,
            conclusion = DemoScenario.WORKING.conclusion) }
    }

    // ── Сценарий 3: Долговременная ────────────────────────────────────────────

    private suspend fun runLongTerm() {
        val initialSteps = listOf(
            DemoStep("Очищаем историю и долговременную память (чистый старт)"),
            DemoStep("Добавляем в долговременную: profile.name = Андрей"),
            DemoStep("Очищаем историю чата (память не трогаем)"),
            DemoStep("Спрашиваем: «Как меня зовут?»", isChatStep = true, isAfterStep = true,
                memoryBadge = "🧠 из долговременной памяти")
        )
        setSteps(initialSteps)

        // Шаг 0: сброс
        runStep(0) {
            agent.reset()
            memoryRepo.clearByType(MemoryType.LONG_TERM)
        }
        // Шаг 1: добавляем в долговременную
        runStep(1) {
            memoryRepo.save(MemoryType.LONG_TERM, "profile.name", "Андрей")
            refreshSystemPrompt()
        }
        // Шаг 2: очищаем историю
        runStep(2) {
            agent.reset()
        }
        // Шаг 3: ключевой момент — агент знает без истории
        val answer = runStep(3) {
            agent.chat("Как меня зовут?").answer
        }
        _state.update { it.copy(afterResponse = answer,
            conclusion = DemoScenario.LONG_TERM.conclusion) }
    }

    // ── Утилиты ───────────────────────────────────────────────────────────────

    private fun setSteps(steps: List<DemoStep>) {
        _state.update { it.copy(steps = steps) }
    }

    /** Устанавливает шаг в RUNNING, выполняет блок, устанавливает DONE. Возвращает результат. */
    private suspend fun <T> runStep(index: Int, block: suspend () -> T): T {
        updateStepStatus(index, StepStatus.RUNNING)
        return try {
            val result = block()
            updateStepStatus(index, StepStatus.DONE)
            result
        } catch (e: Exception) {
            updateStepStatus(index, StepStatus.DONE)
            throw e
        }
    }

    private fun updateStepStatus(index: Int, status: StepStatus, response: String? = null) {
        _state.update { s ->
            val updated = s.steps.toMutableList()
            if (index < updated.size) {
                val old = updated[index]
                updated[index] = old.copy(
                    status = status,
                    agentResponse = response ?: old.agentResponse
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
