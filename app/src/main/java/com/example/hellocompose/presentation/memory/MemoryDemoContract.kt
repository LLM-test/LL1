package com.example.hellocompose.presentation.memory

// ── Сценарии ─────────────────────────────────────────────────────────────────

enum class DemoScenario(
    val tabIndex: Int,
    val icon: String,
    val title: String,
    val subtitle: String,
    val conclusion: String
) {
    SHORT_TERM(
        tabIndex = 0,
        icon     = "💬",
        title    = "Краткосрочная",
        subtitle = "Живёт только в истории чата",
        conclusion = "После очистки истории агент забыл имя — краткосрочная " +
                "память хранится только в RAM и не переживает сброс."
    ),
    WORKING(
        tabIndex = 1,
        icon     = "🔧",
        title    = "Рабочая",
        subtitle = "Текущая задача — в system prompt",
        conclusion = "После удаления рабочей памяти агент перестал знать задачу — " +
                "рабочая память инжектируется в system prompt и исчезает при ручной очистке."
    ),
    LONG_TERM(
        tabIndex = 2,
        icon     = "🧠",
        title    = "Долговременная",
        subtitle = "Переживает сброс истории",
        conclusion = "Даже после полного сброса истории агент знает имя из " +
                "долговременной памяти — она всегда в system prompt."
    )
}

// ── Шаг сценария ─────────────────────────────────────────────────────────────

enum class StepStatus { PENDING, RUNNING, DONE }

/**
 * Один шаг guided demo.
 *
 * @param description  Что происходит на этом шаге (показывается пользователю).
 * @param isChatStep   true → шаг делает запрос к агенту, false → операция с памятью/историей.
 * @param isBeforeStep true → ответ попадает в [MemoryDemoState.beforeResponse].
 * @param isAfterStep  true → ответ попадает в [MemoryDemoState.afterResponse].
 * @param memoryBadge  Аннотация слоя памяти для этого ответа (null → шаг без ответа).
 */
data class DemoStep(
    val description: String,
    val status: StepStatus = StepStatus.PENDING,
    val agentResponse: String? = null,
    val isChatStep: Boolean = false,
    val isBeforeStep: Boolean = false,
    val isAfterStep: Boolean = false,
    val memoryBadge: String? = null        // напр. "💬 из истории", "🧠 из долговременной", "❌ не помнит"
)

// ── State ─────────────────────────────────────────────────────────────────────

data class MemoryDemoState(
    val activeTab: Int = 0,
    val steps: List<DemoStep> = emptyList(),
    val isRunning: Boolean = false,
    val systemPromptMemoryBlock: String = "",   // живой блок "=== ПАМЯТЬ АССИСТЕНТА ==="
    val beforeResponse: String? = null,
    val afterResponse: String? = null,
    val conclusion: String? = null              // показывается после завершения сценария
)

// ── Intents ───────────────────────────────────────────────────────────────────

sealed class MemoryDemoIntent {
    data class SelectTab(val index: Int) : MemoryDemoIntent()
    object RunScenario : MemoryDemoIntent()
    object StopScenario : MemoryDemoIntent()
    object ResetScenario : MemoryDemoIntent()
}

// ── Effects ───────────────────────────────────────────────────────────────────

sealed class MemoryDemoEffect {
    data class ShowToast(val message: String) : MemoryDemoEffect()
}
