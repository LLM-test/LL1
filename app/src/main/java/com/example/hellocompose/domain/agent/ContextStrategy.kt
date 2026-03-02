package com.example.hellocompose.domain.agent

/**
 * Стратегия управления контекстом агента.
 *
 * День 10: три стратегии — Sliding Window, Sticky Facts, Branching.
 * Каждая определяет, как формируется список сообщений перед отправкой в API.
 */
sealed class ContextStrategy {
    abstract val displayName: String
    abstract val icon: String
    abstract val shortDescription: String

    /**
     * Скользящее окно: передаём только последние [windowSize] сообщений.
     * Старые отбрасываются полностью — дёшево, но теряет детали.
     */
    data class SlidingWindow(val windowSize: Int = 20) : ContextStrategy() {
        override val displayName = "Sliding Window"
        override val icon = "🪟"
        override val shortDescription = "Последние $windowSize сообщений"
    }

    /**
     * Факты + короткое окно: LLM извлекает ключевые факты из каждого обмена
     * и накапливает их в отдельном блоке. В контекст передаётся:
     * [system prompt] + [блок фактов] + [последние recentWindow сообщений].
     * Экономичнее скользящего окна при длинных диалогах.
     */
    data class StickyFacts(val recentWindow: Int = 10) : ContextStrategy() {
        override val displayName = "Sticky Facts"
        override val icon = "📌"
        override val shortDescription = "Факты + последние $recentWindow сообщений"
    }

    /**
     * Ветки: пользователь сохраняет checkpoint и создаёт независимые ветки.
     * Каждая ветка разделяет историю до checkpoint, но имеет свою историю после.
     * Передаёт полную историю активной ветки (без усечения).
     */
    object Branching : ContextStrategy() {
        override val displayName = "Branching"
        override val icon = "🌿"
        override val shortDescription = "Чекпоинты и независимые ветки"
    }
}
