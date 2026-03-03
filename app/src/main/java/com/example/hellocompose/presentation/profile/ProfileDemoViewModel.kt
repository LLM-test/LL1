package com.example.hellocompose.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hellocompose.domain.agent.Agent
import com.example.hellocompose.domain.profile.ProfilePresets
import com.example.hellocompose.domain.profile.ProfileRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileDemoViewModel(
    private val agent: Agent,
    private val profileRepo: ProfileRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileDemoState())
    val state: StateFlow<ProfileDemoState> = _state.asStateFlow()

    private var demoJob: Job? = null

    init { refreshSystemPrompt() }

    fun handleIntent(intent: ProfileDemoIntent) {
        when (intent) {
            is ProfileDemoIntent.SelectTab    -> selectTab(intent.index)
            is ProfileDemoIntent.RunScenario  -> runScenario()
            is ProfileDemoIntent.StopScenario -> stopScenario()
            is ProfileDemoIntent.ResetScenario -> resetScenario()
        }
    }

    // ── Tab ───────────────────────────────────────────────────────────────────

    private fun selectTab(index: Int) {
        _state.update {
            it.copy(
                activeTab      = index,
                steps          = emptyList(),
                isRunning      = false,
                beforeResponse = null,
                afterResponse  = null,
                conclusion     = null
            )
        }
        refreshSystemPrompt()
    }

    private fun refreshSystemPrompt() {
        viewModelScope.launch {
            val preview = agent.getSystemPromptPreview()
            _state.update { it.copy(systemPromptPreview = preview) }
        }
    }

    // ── Run ───────────────────────────────────────────────────────────────────

    private fun runScenario() {
        if (_state.value.isRunning) return
        val scenario = ProfileDemoScenario.entries[_state.value.activeTab]
        demoJob = viewModelScope.launch {
            _state.update { it.copy(isRunning = true, beforeResponse = null, afterResponse = null, conclusion = null) }
            try {
                when (scenario) {
                    ProfileDemoScenario.NO_PROFILE_VS_JUNIOR -> runNoProfileVsJunior()
                    ProfileDemoScenario.JUNIOR_VS_SENIOR     -> runJuniorVsSenior()
                    ProfileDemoScenario.WITH_CONSTRAINTS     -> runWithConstraints()
                }
            } finally {
                _state.update { it.copy(isRunning = false) }
                refreshSystemPrompt()
            }
        }
    }

    // ── Сценарий 0: Без профиля → Junior ─────────────────────────────────────

    private suspend fun runNoProfileVsJunior() {
        val q = ProfileDemoScenario.NO_PROFILE_VS_JUNIOR.question
        setSteps(listOf(
            ProfileDemoStep("Очищаем профиль (чистый старт)",
                profileAction = "clearProfile() + agent.reset() — профиль убран из system prompt"),
            ProfileDemoStep("Вопрос без профиля", isChatStep = true, isBeforeStep = true,
                userMessage = q, profileBadge = "🚫 без профиля"),
            ProfileDemoStep("Применяем пресет 👶 Junior",
                profileAction = "setProfile(JUNIOR) — блок === ПРОФИЛЬ ПОЛЬЗОВАТЕЛЯ === добавлен"),
            ProfileDemoStep("Тот же вопрос с профилем Junior", isChatStep = true, isAfterStep = true,
                userMessage = q, profileBadge = "👶 Junior")
        ))

        // Шаг 0: сброс
        runProfileStep(0, "clearProfile() выполнен — профиль пустой, блок не инжектируется") {
            profileRepo.clearProfile()
            agent.reset()
            refreshSystemPrompt()
        }
        // Шаг 1: вопрос без профиля
        val beforeAnswer = runChatStep(1) { agent.chat(q).answer }
        _state.update { it.copy(beforeResponse = beforeAnswer) }

        // Шаг 2: применяем Junior
        runProfileStep(2, "setProfile(JUNIOR) — профиль Junior активирован в system prompt") {
            profileRepo.setProfile(ProfilePresets.JUNIOR)
            agent.reset()
            refreshSystemPrompt()
        }
        // Шаг 3: тот же вопрос с профилем
        val afterAnswer = runChatStep(3) { agent.chat(q).answer }
        _state.update { it.copy(afterResponse = afterAnswer,
            conclusion = ProfileDemoScenario.NO_PROFILE_VS_JUNIOR.conclusion) }
    }

    // ── Сценарий 1: Junior → Senior ───────────────────────────────────────────

    private suspend fun runJuniorVsSenior() {
        val q = ProfileDemoScenario.JUNIOR_VS_SENIOR.question
        setSteps(listOf(
            ProfileDemoStep("Применяем пресет 👶 Junior",
                profileAction = "setProfile(JUNIOR)"),
            ProfileDemoStep("Вопрос с профилем Junior", isChatStep = true, isBeforeStep = true,
                userMessage = q, profileBadge = "👶 Junior"),
            ProfileDemoStep("Переключаемся на пресет 👨‍💻 Senior",
                profileAction = "setProfile(SENIOR)"),
            ProfileDemoStep("Тот же вопрос с профилем Senior", isChatStep = true, isAfterStep = true,
                userMessage = q, profileBadge = "👨‍💻 Senior")
        ))

        // Шаг 0: Junior
        runProfileStep(0, "setProfile(JUNIOR) — Junior активирован") {
            profileRepo.setProfile(ProfilePresets.JUNIOR)
            agent.reset()
            refreshSystemPrompt()
        }
        // Шаг 1: вопрос с Junior
        val beforeAnswer = runChatStep(1) { agent.chat(q).answer }
        _state.update { it.copy(beforeResponse = beforeAnswer) }

        // Шаг 2: Senior
        runProfileStep(2, "setProfile(SENIOR) — Senior активирован в system prompt") {
            profileRepo.setProfile(ProfilePresets.SENIOR)
            agent.reset()
            refreshSystemPrompt()
        }
        // Шаг 3: тот же вопрос с Senior
        val afterAnswer = runChatStep(3) { agent.chat(q).answer }
        _state.update { it.copy(afterResponse = afterAnswer,
            conclusion = ProfileDemoScenario.JUNIOR_VS_SENIOR.conclusion) }
    }

    // ── Сценарий 2: Без ограничений → С ограничением ─────────────────────────

    private suspend fun runWithConstraints() {
        val q = ProfileDemoScenario.WITH_CONSTRAINTS.question
        setSteps(listOf(
            ProfileDemoStep("Senior без ограничений",
                profileAction = "setProfile(SENIOR, constraints = \"\")"),
            ProfileDemoStep("Вопрос без ограничений", isChatStep = true, isBeforeStep = true,
                userMessage = q, profileBadge = "⚙️ без ограничений"),
            ProfileDemoStep("Добавляем ограничение: только код",
                profileAction = "setProfile(SENIOR, constraints = \"только код без объяснений\")"),
            ProfileDemoStep("Тот же вопрос с ограничением", isChatStep = true, isAfterStep = true,
                userMessage = q, profileBadge = "🔒 только код")
        ))

        // Шаг 0: Senior без ограничений
        runProfileStep(0, "SENIOR без ограничений — обычный ответ с объяснениями") {
            profileRepo.setProfile(ProfilePresets.SENIOR.copy(constraints = ""))
            agent.reset()
            refreshSystemPrompt()
        }
        // Шаг 1: вопрос без ограничений
        val beforeAnswer = runChatStep(1) { agent.chat(q).answer }
        _state.update { it.copy(beforeResponse = beforeAnswer) }

        // Шаг 2: добавляем ограничение
        runProfileStep(2, "Ограничение добавлено — агент должен отвечать только кодом") {
            profileRepo.setProfile(ProfilePresets.SENIOR.copy(
                constraints = "отвечай только кодом без каких-либо текстовых объяснений, никаких комментариев в тексте"
            ))
            agent.reset()
            refreshSystemPrompt()
        }
        // Шаг 3: тот же вопрос с ограничением
        val afterAnswer = runChatStep(3) { agent.chat(q).answer }
        _state.update { it.copy(afterResponse = afterAnswer,
            conclusion = ProfileDemoScenario.WITH_CONSTRAINTS.conclusion) }
    }

    // ── Утилиты ───────────────────────────────────────────────────────────────

    private fun setSteps(steps: List<ProfileDemoStep>) {
        _state.update { it.copy(steps = steps) }
    }

    /** Чат-шаг: сохраняет ответ агента в ProfileDemoStep.agentResponse. */
    private suspend fun runChatStep(index: Int, block: suspend () -> String): String {
        updateStep(index, ProfileStepStatus.RUNNING)
        return try {
            val answer = block()
            updateStep(index, ProfileStepStatus.DONE, agentResponse = answer)
            answer
        } catch (e: Exception) {
            updateStep(index, ProfileStepStatus.DONE)
            throw e
        }
    }

    /** Шаг с операцией над профилем: сохраняет описание в ProfileDemoStep.profileAction. */
    private suspend fun runProfileStep(index: Int, action: String, block: suspend () -> Unit) {
        updateStep(index, ProfileStepStatus.RUNNING)
        try {
            block()
            updateStep(index, ProfileStepStatus.DONE, profileAction = action)
        } catch (e: Exception) {
            updateStep(index, ProfileStepStatus.DONE, profileAction = action)
            throw e
        }
    }

    private fun updateStep(
        index: Int,
        status: ProfileStepStatus,
        agentResponse: String? = null,
        profileAction: String? = null
    ) {
        _state.update { s ->
            val updated = s.steps.toMutableList()
            if (index < updated.size) {
                val old = updated[index]
                updated[index] = old.copy(
                    status        = status,
                    agentResponse = agentResponse ?: old.agentResponse,
                    profileAction = profileAction  ?: old.profileAction
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
