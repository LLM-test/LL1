package com.example.hellocompose.domain.usecase

import com.example.hellocompose.data.api.ModelComparisonApiService
import com.example.hellocompose.data.api.dto.ChatRequestDto
import com.example.hellocompose.data.api.dto.MessageDto
import com.example.hellocompose.domain.model.ModelComparisonResponse

/**
 * Отправляет вопрос и три ответа модели-судье (deepseek-chat),
 * которая сравнивает их качество и выносит вердикт.
 */
class JudgeUseCase(private val deepSeekService: ModelComparisonApiService) {

    suspend operator fun invoke(
        question: String,
        responses: List<ModelComparisonResponse>
    ): Result<String> = runCatching {
        // Скрываем имена моделей — слепой тест
        val answersText = responses.mapIndexed { i, r ->
            "Ответ ${i + 1}:\n${r.content}"
        }.joinToString("\n\n")

        val prompt = """
Вопрос пользователя: «$question»

Три анонимных языковых модели ответили на этот вопрос. Ты не знаешь, какая модель что написала.

$answersText

Оцени каждый ответ по четырём критериям, выставив баллы от 1 до 10:
— Точность (насколько ответ фактически верен)
— Полнота (насколько полно раскрыта тема)
— Ясность (насколько понятно и чётко изложено)
— Краткость (оптимальный объём без воды)

Формат ответа — строго такой:

Ответ 1
  Точность: X/10
  Полнота: X/10
  Ясность: X/10
  Краткость: X/10
  Итого: X/40

Ответ 2
  Точность: X/10
  Полнота: X/10
  Ясность: X/10
  Краткость: X/10
  Итого: X/40

Ответ 3
  Точность: X/10
  Полнота: X/10
  Ясность: X/10
  Краткость: X/10
  Итого: X/40

🏆 Победитель: Ответ X — [одно предложение почему]

🔍 Моя догадка: Ответ 1 — [предположение что за модель и почему], Ответ 2 — [предположение], Ответ 3 — [предположение]
        """.trimIndent()

        val request = ChatRequestDto(
            model = "deepseek-reasoner",
            messages = listOf(
                MessageDto(role = "system", content = "Ты — объективный эксперт по оценке качества текстовых ответов языковых моделей."),
                MessageDto(role = "user", content = prompt)
            ),
            temperature = null,
            maxTokens = 2000
        )

        val response = deepSeekService.chatCompletions(request)
        response.choices.firstOrNull()?.message?.content
            ?: error("Судья не дал ответа")
    }
}
