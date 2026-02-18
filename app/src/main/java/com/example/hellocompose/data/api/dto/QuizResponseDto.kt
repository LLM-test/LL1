package com.example.hellocompose.data.api.dto

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

/**
 * JSON-схема ответа AI для режима викторины.
 * Поле "type" является дискриминатором для выбора подтипа.
 *
 * Вопрос:
 * { "type": "question", "question_number": 1, "total": 5, "question": "...",
 *   "options": { "A": "...", "B": "...", "C": "...", "D": "..." },
 *   "correct": "B", "explanation": "..." }
 *
 * Итог:
 * { "type": "final", "score": 4, "total": 5, "comment": "..." }
 *
 * Ответ пользователя оценивается локально (сравниваем с полем correct).
 * AI больше не возвращает type=answer — только type=question или type=final.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed class QuizResponseDto {

    @Serializable
    @SerialName("question")
    data class Question(
        @SerialName("question_number") val questionNumber: Int,
        @SerialName("total") val total: Int,
        @SerialName("question") val question: String,
        @SerialName("options") val options: Options,
        @SerialName("correct") val correct: String,
        @SerialName("explanation") val explanation: String = ""
    ) : QuizResponseDto()

    @Serializable
    @SerialName("final")
    data class Final(
        @SerialName("score") val score: Int,
        @SerialName("total") val total: Int,
        @SerialName("comment") val comment: String
    ) : QuizResponseDto()

    @Serializable
    data class Options(
        @SerialName("A") val a: String,
        @SerialName("B") val b: String,
        @SerialName("C") val c: String,
        @SerialName("D") val d: String
    ) {
        fun toList(): List<Pair<String, String>> = listOf("A" to a, "B" to b, "C" to c, "D" to d)
    }
}
