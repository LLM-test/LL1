package com.example.hellocompose.domain.model

data class QuizConfig(
    val topic: QuizTopic = QuizTopic.RANDOM,
    val difficulty: QuizDifficulty = QuizDifficulty.MEDIUM,
    val questionCount: Int = 5
)

enum class QuizTopic(val displayName: String, val prompt: String) {
    RANDOM("Случайная", "на абсолютно разные темы (наука, история, география, культура, спорт, технологии)"),
    SCIENCE("Наука", "по науке (физика, химия, биология, астрономия)"),
    HISTORY("История", "по мировой истории"),
    GEOGRAPHY("География", "по географии (страны, столицы, реки, горы)"),
    MOVIES("Кино", "по кино и сериалам"),
    TECHNOLOGY("Технологии", "по IT и технологиям"),
    SPORTS("Спорт", "по спорту");
}

enum class QuizDifficulty(val displayName: String, val prompt: String) {
    EASY("Лёгкий", "простые, для начинающих"),
    MEDIUM("Средний", "средней сложности"),
    HARD("Сложный", "сложные, для эрудитов");
}
