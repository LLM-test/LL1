package com.example.hellocompose.presentation.profile

// ── Сценарии ─────────────────────────────────────────────────────────────────

enum class ProfileDemoScenario(
    val tabIndex: Int,
    val icon: String,
    val title: String,
    val subtitle: String,
    val question: String,
    val conclusion: String
) {
    NO_PROFILE_VS_JUNIOR(
        tabIndex  = 0,
        icon      = "🧑‍🎓",
        title     = "Профиль Junior",
        subtitle  = "Как профиль делает ответы подробнее",
        question  = "Что такое ViewModel в Android?",
        conclusion = "С профилем Junior агент добавил пошаговые объяснения и примеры кода — " +
                     "ответ стал значительно подробнее. Без профиля — краткий ответ по умолчанию."
    ),
    JUNIOR_VS_SENIOR(
        tabIndex  = 1,
        icon      = "⚡",
        title     = "Junior → Senior",
        subtitle  = "Один вопрос — два стиля",
        question  = "Что такое ViewModel в Android?",
        conclusion = "Один и тот же вопрос — два разных ответа. Senior-профиль убирает базовые " +
                     "объяснения и переходит сразу к техническим деталям."
    ),
    WITH_CONSTRAINTS(
        tabIndex  = 2,
        icon      = "🔒",
        title     = "Ограничения",
        subtitle  = "Контроль формата ответа через constraints",
        question  = "Напиши функцию reverseString на Kotlin",
        conclusion = "Ограничение 'только код без объяснений' полностью изменило формат ответа — " +
                     "вместо разбора с комментариями агент вернул чистый код."
    )
}

// ── Шаг сценария ─────────────────────────────────────────────────────────────

enum class ProfileStepStatus { PENDING, RUNNING, DONE }

/**
 * Один шаг guided demo профиля.
 *
 * @param description   Что происходит на этом шаге.
 * @param userMessage   Текст запроса агенту (null → шаг без запроса).
 * @param agentResponse Ответ агента (null → ещё не получен).
 * @param profileAction Описание операции с профилем (null → шаг без операции).
 * @param isChatStep    true → шаг делает запрос к агенту.
 * @param isBeforeStep  true → ответ попадает в [ProfileDemoState.beforeResponse].
 * @param isAfterStep   true → ответ попадает в [ProfileDemoState.afterResponse].
 * @param profileBadge  Аннотация пресета/режима для этого ответа.
 */
data class ProfileDemoStep(
    val description: String,
    val status: ProfileStepStatus = ProfileStepStatus.PENDING,
    val userMessage: String? = null,
    val agentResponse: String? = null,
    val profileAction: String? = null,
    val isChatStep: Boolean = false,
    val isBeforeStep: Boolean = false,
    val isAfterStep: Boolean = false,
    val profileBadge: String? = null
)

// ── State ─────────────────────────────────────────────────────────────────────

data class ProfileDemoState(
    val activeTab: Int = 0,
    val steps: List<ProfileDemoStep> = emptyList(),
    val isRunning: Boolean = false,
    val systemPromptPreview: String = "",
    val beforeResponse: String? = null,
    val afterResponse: String? = null,
    val conclusion: String? = null
)

// ── Intents ───────────────────────────────────────────────────────────────────

sealed class ProfileDemoIntent {
    data class SelectTab(val index: Int) : ProfileDemoIntent()
    object RunScenario   : ProfileDemoIntent()
    object StopScenario  : ProfileDemoIntent()
    object ResetScenario : ProfileDemoIntent()
}
