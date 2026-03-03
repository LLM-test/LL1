package com.example.hellocompose.domain.profile

/**
 * Профиль пользователя — единственная запись (id = 1).
 * Инжектируется в system prompt при каждом запросе к агенту.
 */
data class UserProfile(
    val id: Long = 1L,
    val name: String = "",
    val expertise: String = "",
    val responseStyle: String = "",
    val responseFormat: String = "",
    val preferredLanguage: String = "",
    val constraints: String = ""
) {
    /** true — профиль пустой, блок не добавляется в system prompt */
    val isEmpty: Boolean get() = name.isBlank() && expertise.isBlank() &&
            responseStyle.isBlank() && responseFormat.isBlank() &&
            preferredLanguage.isBlank() && constraints.isBlank()
}

// ── Пресеты ───────────────────────────────────────────────────────────────────

object ProfilePresets {

    val JUNIOR = UserProfile(
        expertise        = "Junior Developer",
        responseStyle    = "подробный и понятный",
        responseFormat   = "с пошаговыми объяснениями и примерами",
        preferredLanguage = "Русский",
        constraints      = "объясняй каждый шаг, не пропускай детали"
    )

    val SENIOR = UserProfile(
        expertise        = "Senior Android Developer (Kotlin)",
        responseStyle    = "краткий и технический",
        responseFormat   = "с примерами кода на Kotlin",
        preferredLanguage = "Русский",
        constraints      = "не объясняй базовые концепции, пропускай очевидное"
    )

    val STUDENT = UserProfile(
        expertise        = "студент Computer Science",
        responseStyle    = "академический",
        responseFormat   = "теория + практика, структурированно",
        preferredLanguage = "Русский",
        constraints      = "ссылайся на термины и концепции из CS"
    )
}
