package com.example.hellocompose.domain.memory

/**
 * Три слоя памяти ассистента (День 11):
 *
 *  • [WORKING]   — рабочая: текущая задача, стек, цель. Очищается вручную.
 *  • [LONG_TERM] — долговременная: профиль, решения, знания. Никогда не теряется.
 *
 * SHORT_TERM (краткосрочная) — это сам список сообщений чата; он живёт в RAM
 * и не хранится в этой таблице (управляется AgentHistoryRepository).
 */
enum class MemoryType(
    val displayName: String,
    val icon: String,
    val description: String
) {
    WORKING(
        displayName = "Рабочая",
        icon        = "🔧",
        description = "Текущая задача — очищается вручную"
    ),
    LONG_TERM(
        displayName = "Долговременная",
        icon        = "🧠",
        description = "Профиль и знания — доступны всегда"
    )
}
