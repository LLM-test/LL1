package com.example.hellocompose.domain.usecase

import com.example.hellocompose.data.api.ModelComparisonApiService
import com.example.hellocompose.data.api.dto.ChatRequestDto
import com.example.hellocompose.data.api.dto.MessageDto
import com.example.hellocompose.domain.model.ModelComparisonResponse

/**
 * Отправляет вопрос и три ответа модели-судье (deepseek-reasoner),
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

        // Список участников в перемешанном порядке, чтобы судья не мог угадать по порядку
        val modelNames = responses.map { it.modelConfig.displayName }.shuffled()
        val modelListText = modelNames.joinToString(", ")

        val prompt = """
Вопрос пользователя: «$question»

В тесте участвовали три языковые модели: $modelListText.
Их ответы представлены ниже в случайном порядке — ты не знаешь, кто написал какой ответ.

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

🔍 Моя догадка: Ответ 1 — [название модели и краткое объяснение], Ответ 2 — [название модели и краткое объяснение], Ответ 3 — [название модели и краткое объяснение]
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
